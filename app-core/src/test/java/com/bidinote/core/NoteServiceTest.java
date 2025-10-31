package com.bidinote.core;

import com.bidinote.core.graph.GraphExpansionService;
import com.bidinote.core.graph.LinkQueryService;
import com.bidinote.core.model.NoteNode;
import com.bidinote.core.repository.NoteRepository.SearchHit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class NoteServiceTest {
    private InMemoryNoteRepository repository;
    private NoteService service;
    private NoteNode pageA;
    private NoteNode pageB;

    @BeforeEach
    void setup() {
        repository = new InMemoryNoteRepository();
        service = new NoteService(repository);
        pageA = new NoteNode("A", "页面A");
        pageB = new NoteNode("B", "页面B");
        repository.saveNode(pageA);
        repository.saveNode(pageB);
    }

    @Test
    void parsePageLinkCreatesEdgeAndBacklink() {
        service.save(pageA, "这是一个链接 [[页面B]]。");
        assertEquals(1, repository.loadAllEdges().size());
        assertEquals("页面B", repository.loadAllNodes().get("B").getTitle());
        assertEquals(1, service.reverseLinks("B").size());
    }

    @Test
    void unlinkedMentionDetected() {
        NoteService.SaveResult result = service.save(pageA, "这里提到页面B但没有链接。");
        assertFalse(result.mentions().isEmpty());
        assertEquals("页面B", repository.findNodeById(result.mentions().get(0).noteId()).get().getTitle());
    }

    @Test
    void renameKeepsAlias() {
        service.rename(pageB, "页面B-2025");
        assertTrue(repository.findNodeById("B").get().getAliases().contains("页面B"));
        service.save(pageA, "再次链接 [[页面B-2025]]");
        assertEquals(1, repository.loadAllEdges().size());
    }

    @Test
    void searchHitsFromBlocks() {
        service.save(pageA, "内容包含测试词汇");
        List<SearchHit> hits = service.search("测试");
        assertEquals(1, hits.size());
    }

    @Test
    void graphExpansionReturnsSubgraph() {
        service.save(pageA, "链接 [[页面B]]");
        GraphExpansionService.GraphResult result = service.expandGraph("A", 1, 0);
        assertEquals(2, result.nodes().size());
        assertEquals(1, result.edges().size());
    }

    @Test
    void linkQueryFiltersByTag() {
        pageA.setTags(Set.of("tag1"));
        pageB.setTags(Set.of("tag2"));
        repository.saveNode(pageA);
        repository.saveNode(pageB);
        service.save(pageA, "链接 [[页面B]]");
        LinkQueryService.QueryResult result = service.query("tag=\"tag1\"");
        assertEquals(1, result.nodes().size());
    }
}
