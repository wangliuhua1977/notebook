package com.bidinote.core.graph;

import com.bidinote.core.model.NoteEdge;
import com.bidinote.core.model.NoteNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 图谱展开：以指定页面为锚点，扩展 N 层并按度数过滤。
 */
public class GraphExpansionService {
    public record GraphResult(List<NoteNode> nodes, List<NoteEdge> edges) {
    }

    public GraphResult expand(NoteNode anchor, List<NoteNode> allNodes, List<NoteEdge> allEdges, int depth, int degreeThreshold) {
        Objects.requireNonNull(anchor, "anchor");
        Map<String, NoteNode> byId = allNodes.stream().collect(Collectors.toMap(NoteNode::getId, n -> n));
        Set<String> visited = new HashSet<>();
        Set<String> selectedNodeIds = new HashSet<>();
        List<NoteEdge> selectedEdges = new ArrayList<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(anchor.getId());
        visited.add(anchor.getId());
        int currentDepth = 0;
        while (!queue.isEmpty() && currentDepth <= depth) {
            int levelSize = queue.size();
            for (int i = 0; i < levelSize; i++) {
                String nodeId = queue.poll();
                selectedNodeIds.add(nodeId);
                List<NoteEdge> neighbours = allEdges.stream()
                        .filter(edge -> edge.getSrcPageId().equals(nodeId) || edge.getDstPageId().equals(nodeId))
                        .collect(Collectors.toList());
                if (neighbours.size() >= degreeThreshold) {
                    selectedEdges.addAll(neighbours);
                    for (NoteEdge edge : neighbours) {
                        String targetId = edge.getSrcPageId().equals(nodeId) ? edge.getDstPageId() : edge.getSrcPageId();
                        if (!visited.contains(targetId)) {
                            visited.add(targetId);
                            queue.add(targetId);
                        }
                    }
                }
            }
            currentDepth++;
        }
        List<NoteNode> nodes = selectedNodeIds.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return new GraphResult(nodes, selectedEdges);
    }
}
