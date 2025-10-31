package com.bidinote.core.repository;

import com.bidinote.core.model.NoteBlock;
import com.bidinote.core.model.NoteEdge;
import com.bidinote.core.model.NoteNode;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 存储层接口，由 app-storage 实现。
 */
public interface NoteRepository {
    Optional<NoteNode> findNodeById(String id);

    Optional<NoteNode> findNodeByTitle(String title);

    Map<String, NoteNode> loadAllNodes();

    List<NoteBlock> findBlocksByPageId(String pageId);

    List<NoteEdge> findEdgesByPageId(String pageId);

    List<NoteEdge> findEdgesByTarget(String pageId);

    void saveNode(NoteNode node);

    void saveBlocks(String pageId, List<NoteBlock> blocks);

    void saveEdges(String pageId, List<NoteEdge> edges);

    void removeEdgesBySource(String pageId);

    List<NoteEdge> loadAllEdges();

    List<SearchHit> search(String keyword);

    record SearchHit(String pageId, String blockId, String snippet, double score) {
    }
}
