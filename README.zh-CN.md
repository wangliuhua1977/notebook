# BidiNote

BidiNote 是一款运行在 Windows 11 的桌面双向链接笔记软件，使用 Java 22 + Swing 实现，核心能力覆盖 Markdown WYSIWYM 编辑、双向链接、图谱视图、DSL 查询、FTS 全文检索与批量导入导出。

## 模块划分

- **app-core**：领域模型、Markdown 切块、链接/提及解析、图谱与查询服务。
- **app-storage**：SQLite 存储实现，建表 SQL 含 FTS5 索引与常用索引。
- **app-ui**：Swing 桌面界面、命令面板、自动补全、图谱面板、示例数据工具。

## 快速开始

```powershell
mvn -q -U -DskipTests package
pwsh -NoProfile -File .\scripts\run.ps1
pwsh -NoProfile -File .\tools\seed.ps1
pwsh -NoProfile -File .\scripts\pack.ps1
```

## 主要功能

- `[[页面]]` / `[[页面#小节]]` / `[[页面^块ID]]` 链接解析与反向链接即时展示。
- `![[页面^块ID]]` 转录支持「跟随更新」与「冻结快照」属性。
- 未链接提及检测（标题/别名词典 + 大小写/全半角归一），支持批量链接化。
- 重命名保持链路稳定，旧名自动写入 aliases，UI 异步刷新。
- RSyntaxTextArea Markdown 编辑 + flexmark 预览，实现即时 WYSIWYM。
- 命令面板（Ctrl+P）、主题浅色/深色切换、Undo/Redo 200+。
- JGraphX 图谱：锚点展开、多层级过滤、PNG 导出。
- Link Query DSL：`type=note AND out(type="quotes")->tag="A/B测试"`。
- SQLite FTS5 检索标题/正文/别名/标签，结果高亮与跳转。
- Seed 脚本生成 50+ 示例页面、300+ 边，验证链路及图谱。

## 数据模型

```
note_node(id TEXT PK, title TEXT, aliases TEXT[], tags TEXT[], updated_at TEXT)
note_block(id TEXT PK, page_id TEXT FK, text TEXT, anchor TEXT, order_no INT)
note_edge(src_block_id TEXT, src_page_id TEXT, dst_page_id TEXT, dst_block_id TEXT, type TEXT, props JSON, created_at TEXT)
note_block_fts(content=note_block, text)
```

索引：`idx_note_edge_dst`、`idx_note_edge_src`、`idx_note_edge_type`、`idx_note_block_order`。

## 资源与文档

- [docs/architecture.puml](docs/architecture.puml)：模块/类图
- [docs/sequence.puml](docs/sequence.puml)：保存流程时序
- [docs/parser-flow.puml](docs/parser-flow.puml)：解析流程图

## 许可证

MIT（示例项目，可按需调整）。
