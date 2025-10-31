package com.bidinote.core;

import com.bidinote.core.graph.GraphExpansionService;
import com.bidinote.core.graph.LinkQueryService;
import com.bidinote.core.mention.AliasDictionary;
import com.bidinote.core.mention.MentionDetector;
import com.bidinote.core.mention.UnlinkedMention;
import com.bidinote.core.model.NoteBlock;
import com.bidinote.core.model.NoteEdge;
import com.bidinote.core.model.NoteNode;
import com.bidinote.core.parser.MarkdownBlockExtractor;
import com.bidinote.core.parser.WikiLink;
import com.bidinote.core.parser.WikiLinkParser;
import com.bidinote.core.repository.NoteRepository;
import com.bidinote.core.suggest.LinkSuggestionService;
import com.bidinote.core.util.UlidHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 核心领域服务：负责保存、解析、重命名与查询。
 */
public class NoteService {
    private static final Logger log = LoggerFactory.getLogger(NoteService.class);
    private final NoteRepository repository;
    private final MarkdownBlockExtractor blockExtractor = new MarkdownBlockExtractor();
    private final WikiLinkParser linkParser = new WikiLinkParser();

    public NoteService(NoteRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public record SaveResult(List<NoteBlock> blocks, List<NoteEdge> edges, List<UnlinkedMention> mentions, List<NoteNode> suggestions) {
    }

    public SaveResult save(NoteNode node, String markdown) {
        log.info("保存页面 {}", node.getTitle());
        node.setUpdatedAt(Instant.now());
        repository.saveNode(node);
        List<NoteBlock> previous = repository.findBlocksByPageId(node.getId());
        MarkdownBlockExtractor.Result result = blockExtractor.extract(node.getId(), markdown);
        List<NoteBlock> blocks = reconcileBlocks(previous, result.getBlocks());
        repository.saveBlocks(node.getId(), blocks);
        List<NoteEdge> edges = buildEdges(node, blocks, markdown);
        repository.saveEdges(node.getId(), edges);
        AliasDictionary dictionary = buildAliasDictionary();
        MentionDetector detector = new MentionDetector(dictionary);
        List<UnlinkedMention> mentions = detector.detect(markdown).stream()
                .filter(mention -> mention.noteId() != null)
                .collect(Collectors.toList());
        LinkSuggestionService suggestionService = new LinkSuggestionService(repository.loadAllNodes());
        List<NoteNode> suggestions = blocks.stream()
                .flatMap(block -> suggestionService.suggest(block.getText(), 5).stream())
                .distinct()
                .limit(5)
                .collect(Collectors.toList());
        return new SaveResult(blocks, edges, mentions, suggestions);
    }

    private List<NoteBlock> reconcileBlocks(List<NoteBlock> previous, List<NoteBlock> current) {
        Map<String, NoteBlock> byAnchor = previous.stream()
                .filter(block -> block.getAnchor() != null)
                .collect(Collectors.toMap(NoteBlock::getAnchor, block -> block, (a, b) -> a, HashMap::new));
        Map<String, NoteBlock> bySnippet = previous.stream()
                .collect(Collectors.toMap(block -> snippet(block.getText()), block -> block, (a, b) -> a, HashMap::new));
        List<NoteBlock> resolved = new ArrayList<>();
        int order = 0;
        for (NoteBlock block : current) {
            NoteBlock match = null;
            if (block.getAnchor() != null) {
                match = byAnchor.remove(block.getAnchor());
            }
            if (match == null) {
                match = bySnippet.remove(snippet(block.getText()));
            }
            String id = match != null ? match.getId() : block.getId();
            if (id == null || id.isBlank()) {
                id = UlidHelper.newUlid();
            }
            resolved.add(new NoteBlock(id, block.getPageId(), block.getText(), block.getAnchor(), order++));
        }
        return resolved;
    }

    private String snippet(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.strip();
        return normalized.length() > 40 ? normalized.substring(0, 40) : normalized;
    }

    private List<NoteEdge> buildEdges(NoteNode node, List<NoteBlock> blocks, String markdown) {
        List<NoteEdge> edges = new ArrayList<>();
        for (NoteBlock block : blocks) {
            for (WikiLink link : linkParser.parse(block.getText())) {
                resolveTarget(link).ifPresent(target -> {
                    String dstBlockId = link.getType() == WikiLink.Type.BLOCK ? link.getAnchor() : null;
                    edges.add(new NoteEdge(block.getId(), node.getId(), target.getId(), dstBlockId,
                            link.isEmbed() ? "embed" : "link",
                            link.isEmbed() ? "{\"mode\":\"follow\"}" : null,
                            Instant.now()));
                });
            }
        }
        for (WikiLink link : linkParser.parse(markdown)) {
            if (link.isEmbed()) {
                resolveTarget(link).ifPresent(target -> edges.add(new NoteEdge(null, node.getId(), target.getId(), link.getAnchor(),
                        "transclusion", "{\"mode\":\"follow\"}", Instant.now())));
            }
        }
        return edges;
    }

    private Optional<NoteNode> resolveTarget(WikiLink link) {
        if (link.getTargetTitle() == null || link.getTargetTitle().isBlank()) {
            return Optional.empty();
        }
        Optional<NoteNode> node = repository.findNodeByTitle(link.getTargetTitle());
        if (node.isPresent()) {
            return node;
        }
        return repository.loadAllNodes().values().stream()
                .filter(candidate -> candidate.getAliases().stream().anyMatch(alias -> alias.equalsIgnoreCase(link.getTargetTitle())))
                .findFirst();
    }

    public void rename(NoteNode node, String newTitle) {
        log.info("重命名 {} -> {}", node.getTitle(), newTitle);
        String oldTitle = node.getTitle();
        node.setTitle(newTitle);
        node.addAlias(oldTitle);
        repository.saveNode(node);
    }

    public GraphExpansionService.GraphResult expandGraph(String nodeId, int depth, int degreeThreshold) {
        NoteNode anchor = repository.findNodeById(nodeId).orElseThrow();
        GraphExpansionService service = new GraphExpansionService();
        return service.expand(anchor, new ArrayList<>(repository.loadAllNodes().values()), repository.loadAllEdges(), depth, degreeThreshold);
    }

    public LinkQueryService.QueryResult query(String expression) {
        LinkQueryService service = new LinkQueryService();
        return service.query(new ArrayList<>(repository.loadAllNodes().values()), repository.loadAllEdges(), expression);
    }

    public List<NoteEdge> reverseLinks(String pageId) {
        return repository.findEdgesByTarget(pageId);
    }

    public List<NoteRepository.SearchHit> search(String keyword) {
        return repository.search(keyword);
    }

    private AliasDictionary buildAliasDictionary() {
        AliasDictionary dictionary = new AliasDictionary();
        repository.loadAllNodes().values().forEach(node -> dictionary.addEntry(node.getId(), node.getTitle(), node.getAliases()));
        return dictionary;
    }
}
