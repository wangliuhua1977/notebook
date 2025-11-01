package com.bidinote.storage.sqlite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * 初始化 SQLite 数据库结构，包括 FTS5。
 */
public class DatabaseInitializer {
    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);
    private static final String[] SQLS = new String[]{
            "PRAGMA journal_mode=WAL;",
            "CREATE TABLE IF NOT EXISTS note_node (id TEXT PRIMARY KEY, title TEXT NOT NULL, aliases TEXT, tags TEXT, updated_at TEXT);",
            "CREATE TABLE IF NOT EXISTS note_block (id TEXT PRIMARY KEY, page_id TEXT NOT NULL, text TEXT, anchor TEXT, order_no INTEGER);",
            "CREATE TABLE IF NOT EXISTS note_edge (src_block_id TEXT, src_page_id TEXT NOT NULL, dst_page_id TEXT NOT NULL, dst_block_id TEXT, type TEXT, props TEXT, created_at TEXT);",
            "CREATE INDEX IF NOT EXISTS idx_note_edge_dst ON note_edge(dst_page_id);",
            "CREATE INDEX IF NOT EXISTS idx_note_edge_src ON note_edge(src_page_id);",
            "CREATE INDEX IF NOT EXISTS idx_note_edge_type ON note_edge(type);",
            "CREATE INDEX IF NOT EXISTS idx_note_block_order ON note_block(page_id, order_no);",
            "CREATE VIRTUAL TABLE IF NOT EXISTS note_block_fts USING fts5(text, content='note_block', content_rowid='rowid');"
    };

    public void init(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            for (String sql : SQLS) {
                log.debug("执行建表 SQL: {}", sql);
                stmt.execute(sql);
            }
        }
    }
}
