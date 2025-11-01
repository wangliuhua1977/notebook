package com.bidinote.core.graph;

import com.bidinote.core.model.NoteEdge;
import com.bidinote.core.model.NoteNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 简易 DSL 查询引擎。
 */
public class LinkQueryService {
    private static final Pattern OUT_PATTERN = Pattern.compile("out\\(type=\"([^\"]+)\"\\)->tag=\"([^\"]+)\"");

    public record QueryResult(List<NoteNode> nodes, List<NoteEdge> edges) {
    }

    public QueryResult query(List<NoteNode> nodes, List<NoteEdge> edges, String expression) {
        Objects.requireNonNull(nodes, "nodes");
        Objects.requireNonNull(edges, "edges");
        if (expression == null || expression.isBlank()) {
            return new QueryResult(nodes, edges);
        }
        List<NoteNode> filtered = new ArrayList<>(nodes);
        List<String> tokens = List.of(expression.split("\\s+AND\\s+"));
        for (String token : tokens) {
            token = token.trim();
            if (token.startsWith("type=")) {
                String value = token.substring("type=".length()).replace("\"", "").toLowerCase(Locale.ROOT);
                if (!"note".equals(value)) {
                    filtered.clear();
                }
            } else if (token.startsWith("tag=")) {
                String value = token.substring("tag=".length()).replace("\"", "");
                filtered = filtered.stream()
                        .filter(node -> node.getTags().stream().anyMatch(tag -> tag.equalsIgnoreCase(value)))
                        .collect(Collectors.toCollection(ArrayList::new));
            } else {
                Matcher matcher = OUT_PATTERN.matcher(token);
                if (matcher.matches()) {
                    String edgeType = matcher.group(1);
                    String targetTag = matcher.group(2);
                    Set<String> allowedTargets = nodes.stream()
                            .filter(node -> node.getTags().stream().anyMatch(tag -> tag.equalsIgnoreCase(targetTag)))
                            .map(NoteNode::getId)
                            .collect(Collectors.toSet());
                    Set<String> allowedSources = edges.stream()
                            .filter(edge -> edgeType.equalsIgnoreCase(edge.getType()) && allowedTargets.contains(edge.getDstPageId()))
                            .map(NoteEdge::getSrcPageId)
                            .collect(Collectors.toSet());
                    filtered = filtered.stream()
                            .filter(node -> allowedSources.contains(node.getId()))
                            .collect(Collectors.toCollection(ArrayList::new));
                }
            }
        }
        Set<String> filteredIds = filtered.stream()
                .map(NoteNode::getId)
                .collect(Collectors.toSet());
        List<NoteEdge> filteredEdges = edges.stream()
                .filter(edge -> filteredIds.contains(edge.getSrcPageId()) || filteredIds.contains(edge.getDstPageId()))
                .collect(Collectors.toList());
        return new QueryResult(filtered, filteredEdges);
    }
}
