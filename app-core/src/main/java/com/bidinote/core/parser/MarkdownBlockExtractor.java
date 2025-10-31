package com.bidinote.core.parser;

import com.bidinote.core.model.NoteBlock;
import com.bidinote.core.util.UlidHelper;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ext.tables.TableBlock;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 使用 flexmark 将 Markdown 内容切分为块。每个段落/标题都会拥有一个持久化的块 ID。
 */
public class MarkdownBlockExtractor {
    private final Parser parser;

    public MarkdownBlockExtractor() {
        this(Parser.builder().build());
    }

    public MarkdownBlockExtractor(Parser parser) {
        this.parser = Objects.requireNonNull(parser, "parser");
    }

    public static class Result {
        private final List<NoteBlock> blocks;
        private final Map<String, String> generatedIdMap;

        public Result(List<NoteBlock> blocks, Map<String, String> generatedIdMap) {
            this.blocks = blocks;
            this.generatedIdMap = generatedIdMap;
        }

        public List<NoteBlock> getBlocks() {
            return blocks;
        }

        public Map<String, String> getGeneratedIdMap() {
            return generatedIdMap;
        }
    }

    public Result extract(String pageId, String markdown) {
        Document document = parser.parse(markdown == null ? "" : markdown);
        List<NoteBlock> blocks = new ArrayList<>();
        Map<String, String> generated = new ConcurrentHashMap<>();
        traverse(document, pageId, blocks, generated, new int[]{0});
        return new Result(blocks, generated);
    }

    private void traverse(Node node, String pageId, List<NoteBlock> blocks, Map<String, String> generated, int[] orderHolder) {
        for (Node current = node.getFirstChild(); current != null; current = current.getNext()) {
            if (current instanceof Heading heading) {
                String anchor = heading.getText().toString();
                String blockId = getOrCreateBlockId(heading, generated);
                blocks.add(new NoteBlock(blockId, pageId, heading.getChars().toString(), anchor, orderHolder[0]++));
            } else if (current instanceof Paragraph paragraph) {
                String blockId = getOrCreateBlockId(paragraph, generated);
                blocks.add(new NoteBlock(blockId, pageId, paragraph.getChars().toString(), null, orderHolder[0]++));
            } else if (current instanceof TableBlock tableBlock) {
                String blockId = getOrCreateBlockId(tableBlock, generated);
                blocks.add(new NoteBlock(blockId, pageId, tableBlock.getChars().toString(), null, orderHolder[0]++));
            }
            traverse(current, pageId, blocks, generated, orderHolder);
        }
    }

    private String getOrCreateBlockId(Node node, Map<String, String> map) {
        String key = Integer.toHexString(System.identityHashCode(node));
        return map.computeIfAbsent(key, k -> UlidHelper.newUlid());
    }
}
