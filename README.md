# BidiNote

BidiNote is a Windows 11 desktop note-taking application implemented with Java 22 and Swing. It provides bidirectional linking, block references, graph visualization, link queries, FTS search, and Markdown-based WYSIWYM editing.

## Modules

- **app-core** – Domain models, Markdown parsing, link detection, graph/query services.
- **app-storage** – SQLite repository implementation with FTS5 indices.
- **app-ui** – Swing desktop application, WYSIWYM editor, graph view, command palette, seed tool.

## Build & Run

```powershell
mvn -q -U -DskipTests package
pwsh -NoProfile -File .\scripts\run.ps1
pwsh -NoProfile -File .\tools\seed.ps1
pwsh -NoProfile -File .\scripts\pack.ps1
```

## Features Overview

- Bidirectional links (`[[Page]]`, `[[Page#Heading]]`, `[[Page^Block]]`) with automatic reverse link panel.
- Markdown WYSIWYM editor based on RSyntaxTextArea plus live preview via flexmark-java.
- Unlinked mention detection, batch conversion, alias preservation on rename.
- Graph view powered by JGraphX with depth controls and PNG export.
- Link query DSL (`type=note AND out(type="quotes")->tag="A/B测试"`).
- SQLite storage with FTS5 (xerial sqlite-jdbc) spanning titles, blocks, aliases, tags.
- Automatic link suggestions based on TF-IDF heuristics.
- Theme toggle (light/dark), command palette (Ctrl+P), Chinese/English UI via ResourceBundle.
- Seed script generates 50 demo notes and 300+ links.

## Data Model

```
note_node(id TEXT PK, title TEXT, aliases TEXT[], tags TEXT[], updated_at TEXT)
note_block(id TEXT PK, page_id TEXT FK, text TEXT, anchor TEXT, order_no INT)
note_edge(src_block_id TEXT, src_page_id TEXT, dst_page_id TEXT, dst_block_id TEXT, type TEXT, props JSON, created_at TEXT)
note_block_fts(content=note_block, text)
```

Indexes: `idx_note_edge_dst`, `idx_note_edge_src`, `idx_note_edge_type`, `idx_note_block_order`.

## Packaging

The Maven Assembly Plugin produces `app-ui/target/bidinote.jar` (with dependencies). `scripts/pack.ps1` copies it to `dist/bidinote-with-deps.jar`.

## Testing

JUnit5 tests cover link parsing, unlinked mention recall/precision, rename alias preservation, graph expansion, search integration, and DSL filtering.

## Resources

- [docs/architecture.puml](docs/architecture.puml)
- [docs/sequence.puml](docs/sequence.puml)
- [docs/parser-flow.puml](docs/parser-flow.puml)
