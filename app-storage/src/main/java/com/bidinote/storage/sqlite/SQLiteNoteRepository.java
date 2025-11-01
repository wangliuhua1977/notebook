package com.bidinote.storage.sqlite;

import com.bidinote.core.model.NoteBlock;
import com.bidinote.core.model.NoteEdge;
import com.bidinote.core.model.NoteNode;
import com.bidinote.core.repository.NoteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * SQLite 实现，负责与 FTS5 联动。
 */
public class SQLiteNoteRepository implements NoteRepository {
    private static final Logger log = LoggerFactory.getLogger(SQLiteNoteRepository.class);
    private final Path databasePath;

    public SQLiteNoteRepository(Path databasePath) {
        this.databasePath = databasePath;
        try {
            Files.createDirectories(databasePath.getParent());
            try (Connection connection = getConnection()) {
                new DatabaseInitializer().init(connection);
            }
        } catch (Exception e) {
            throw new IllegalStateException("初始化数据库失败", e);
        }
    }

    private Connection getConnection() throws SQLException {
        String url = "jdbc:sqlite:" + databasePath;
        Connection connection = DriverManager.getConnection(url);
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys=OFF");
            stmt.execute("PRAGMA busy_timeout=5000");
        }
        return connection;
    }

    @Override
    public Optional<NoteNode> findNodeById(String id) {
        String sql = "SELECT id, title, aliases, tags, updated_at FROM note_node WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapNode(rs));
                }
            }
        } catch (SQLException e) {
            log.error("查询节点失败", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<NoteNode> findNodeByTitle(String title) {
        String sql = "SELECT id, title, aliases, tags, updated_at FROM note_node WHERE title = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapNode(rs));
                }
            }
        } catch (SQLException e) {
            log.error("查询节点失败", e);
        }
        return Optional.empty();
    }

    @Override
    public Map<String, NoteNode> loadAllNodes() {
        Map<String, NoteNode> map = new LinkedHashMap<>();
        String sql = "SELECT id, title, aliases, tags, updated_at FROM note_node";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                NoteNode node = mapNode(rs);
                map.put(node.getId(), node);
            }
        } catch (SQLException e) {
            log.error("加载节点失败", e);
        }
        return map;
    }

    @Override
    public List<NoteBlock> findBlocksByPageId(String pageId) {
        List<NoteBlock> blocks = new ArrayList<>();
        String sql = "SELECT id, page_id, text, anchor, order_no FROM note_block WHERE page_id = ? ORDER BY order_no";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pageId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    blocks.add(new NoteBlock(
                            rs.getString("id"),
                            rs.getString("page_id"),
                            rs.getString("text"),
                            rs.getString("anchor"),
                            rs.getInt("order_no")));
                }
            }
        } catch (SQLException e) {
            log.error("查询块失败", e);
        }
        return blocks;
    }

    @Override
    public List<NoteEdge> findEdgesByPageId(String pageId) {
        return queryEdges("SELECT src_block_id, src_page_id, dst_page_id, dst_block_id, type, props, created_at FROM note_edge WHERE src_page_id = ?", pageId);
    }

    @Override
    public List<NoteEdge> findEdgesByTarget(String pageId) {
        return queryEdges("SELECT src_block_id, src_page_id, dst_page_id, dst_block_id, type, props, created_at FROM note_edge WHERE dst_page_id = ?", pageId);
    }

    private List<NoteEdge> queryEdges(String sql, String pageId) {
        List<NoteEdge> edges = new ArrayList<>();
        try (Connection conn = getConnection(); PreparedStatement ps = pageId == null ? conn.prepareStatement(sql) : conn.prepareStatement(sql)) {
            if (pageId != null) {
                ps.setString(1, pageId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    edges.add(new NoteEdge(
                            rs.getString("src_block_id"),
                            rs.getString("src_page_id"),
                            rs.getString("dst_page_id"),
                            rs.getString("dst_block_id"),
                            rs.getString("type"),
                            rs.getString("props"),
                            Instant.parse(Optional.ofNullable(rs.getString("created_at")).orElse(Instant.now().toString()))));
                }
            }
        } catch (SQLException e) {
            log.error("查询边失败", e);
        }
        return edges;
    }

    @Override
    public void saveNode(NoteNode node) {
        String sql = "REPLACE INTO note_node(id, title, aliases, tags, updated_at) VALUES(?,?,?,?,?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, node.getId());
            ps.setString(2, node.getTitle());
            ps.setString(3, JsonUtils.toJsonArray(node.getAliases()));
            ps.setString(4, JsonUtils.toJsonArray(node.getTags()));
            ps.setString(5, node.getUpdatedAt().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("保存节点失败", e);
        }
    }

    @Override
    public void saveBlocks(String pageId, List<NoteBlock> blocks) {
        String deleteSql = "DELETE FROM note_block WHERE page_id = ?";
        String insertSql = "INSERT INTO note_block(id, page_id, text, anchor, order_no) VALUES(?,?,?,?,?)";
        String insertFts = "INSERT INTO note_block_fts(rowid, text) VALUES ((SELECT rowid FROM note_block WHERE id = ?), ?)";
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement delete = conn.prepareStatement(deleteSql)) {
                delete.setString(1, pageId);
                delete.executeUpdate();
            }
            try (Statement cleanup = conn.createStatement()) {
                cleanup.execute("DELETE FROM note_block_fts WHERE rowid NOT IN (SELECT rowid FROM note_block)");
            }
            try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
                for (NoteBlock block : blocks) {
                    insert.setString(1, block.getId());
                    insert.setString(2, block.getPageId());
                    insert.setString(3, block.getText());
                    insert.setString(4, block.getAnchor());
                    insert.setInt(5, block.getOrderNo());
                    insert.addBatch();
                }
                insert.executeBatch();
            }
            try (PreparedStatement fts = conn.prepareStatement(insertFts)) {
                for (NoteBlock block : blocks) {
                    fts.setString(1, block.getId());
                    fts.setString(2, block.getText());
                    fts.addBatch();
                }
                fts.executeBatch();
            }
            conn.commit();
        } catch (SQLException e) {
            log.error("保存块失败", e);
        }
    }

    @Override
    public void saveEdges(String pageId, List<NoteEdge> edges) {
        String deleteSql = "DELETE FROM note_edge WHERE src_page_id = ?";
        String insertSql = "INSERT INTO note_edge(src_block_id, src_page_id, dst_page_id, dst_block_id, type, props, created_at) VALUES(?,?,?,?,?,?,?)";
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement delete = conn.prepareStatement(deleteSql)) {
                delete.setString(1, pageId);
                delete.executeUpdate();
            }
            try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
                for (NoteEdge edge : edges) {
                    insert.setString(1, edge.getSrcBlockId());
                    insert.setString(2, edge.getSrcPageId());
                    insert.setString(3, edge.getDstPageId());
                    insert.setString(4, edge.getDstBlockId());
                    insert.setString(5, edge.getType());
                    insert.setString(6, edge.getProps());
                    insert.setString(7, edge.getCreatedAt().toString());
                    insert.addBatch();
                }
                insert.executeBatch();
            }
            conn.commit();
        } catch (SQLException e) {
            log.error("保存边失败", e);
        }
    }

    @Override
    public void removeEdgesBySource(String pageId) {
        String sql = "DELETE FROM note_edge WHERE src_page_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pageId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("删除边失败", e);
        }
    }

    @Override
    public List<NoteEdge> loadAllEdges() {
        return queryEdges("SELECT src_block_id, src_page_id, dst_page_id, dst_block_id, type, props, created_at FROM note_edge", null);
    }

    @Override
    public List<SearchHit> search(String keyword) {
        List<SearchHit> hits = new ArrayList<>();
        String sql = "SELECT note_block.page_id, note_block.id, snippet(note_block_fts, 0, '<b>', '</b>', '...', 20) AS snippet, bm25(note_block_fts) AS score " +
                "FROM note_block_fts JOIN note_block ON note_block_fts.rowid = note_block.rowid WHERE note_block_fts MATCH ? ORDER BY score";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, keyword + "*");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    hits.add(new SearchHit(rs.getString("page_id"), rs.getString("id"), rs.getString("snippet"), rs.getDouble("score")));
                }
            }
        } catch (SQLException e) {
            log.error("全文检索失败", e);
        }
        return hits;
    }

    private NoteNode mapNode(ResultSet rs) throws SQLException {
        NoteNode node = new NoteNode(rs.getString("id"), rs.getString("title"));
        Set<String> aliases = JsonUtils.fromJsonArray(rs.getString("aliases"));
        aliases.forEach(node::addAlias);
        node.setTags(JsonUtils.fromJsonArray(rs.getString("tags")));
        node.setUpdatedAt(Instant.parse(Optional.ofNullable(rs.getString("updated_at")).orElse(Instant.now().toString())));
        return node;
    }
}
