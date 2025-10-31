package com.bidinote.core;

import com.bidinote.core.model.NoteBlock;
import com.bidinote.core.model.NoteEdge;
import com.bidinote.core.model.NoteNode;
import com.bidinote.core.repository.NoteRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

class InMemoryNoteRepository implements NoteRepository {
    Map<String, NoteNode> nodes = new LinkedHashMap<>();
    Map<String, List<NoteBlock>> blocksByPage = new HashMap<>();
    List<NoteEdge> edges = new ArrayList<>();

    @Override
    public Optional<NoteNode> findNodeById(String id) {
        return Optional.ofNullable(nodes.get(id));
    }

    @Override
    public Optional<NoteNode> findNodeByTitle(String title) {
        return nodes.values().stream().filter(node -> node.getTitle().equals(title)).findFirst();
    }

    @Override
    public Map<String, NoteNode> loadAllNodes() {
        return nodes;
    }

    @Override
    public List<NoteBlock> findBlocksByPageId(String pageId) {
        return blocksByPage.getOrDefault(pageId, new ArrayList<>());
    }

    @Override
    public List<NoteEdge> findEdgesByPageId(String pageId) {
        return edges.stream().filter(edge -> edge.getSrcPageId().equals(pageId)).collect(Collectors.toList());
    }

    @Override
    public List<NoteEdge> findEdgesByTarget(String pageId) {
        return edges.stream().filter(edge -> edge.getDstPageId().equals(pageId)).collect(Collectors.toList());
    }

    @Override
    public void saveNode(NoteNode node) {
        nodes.put(node.getId(), node);
    }

    @Override
    public void saveBlocks(String pageId, List<NoteBlock> blocks) {
        blocksByPage.put(pageId, blocks);
    }

    @Override
    public void saveEdges(String pageId, List<NoteEdge> edges) {
        removeEdgesBySource(pageId);
        this.edges.addAll(edges);
    }

    @Override
    public void removeEdgesBySource(String pageId) {
        edges.removeIf(edge -> edge.getSrcPageId().equals(pageId));
    }

    @Override
    public List<NoteEdge> loadAllEdges() {
        return edges;
    }

    @Override
    public List<SearchHit> search(String keyword) {
        return blocksByPage.values().stream()
                .flatMap(List::stream)
                .filter(block -> block.getText().contains(keyword))
                .map(block -> new SearchHit(block.getPageId(), block.getId(), block.getText(), 1.0))
                .collect(Collectors.toList());
    }
}
