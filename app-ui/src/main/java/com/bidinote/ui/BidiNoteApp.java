package com.bidinote.ui;

import com.bidinote.core.NoteService;
import com.bidinote.core.model.NoteBlock;
import com.bidinote.core.model.NoteEdge;
import com.bidinote.core.model.NoteNode;
import com.bidinote.core.repository.NoteRepository;
import com.bidinote.core.repository.NoteRepository.SearchHit;
import com.bidinote.storage.sqlite.SQLiteNoteRepository;
import com.fifesoft.rsyntaxtextarea.RSyntaxTextArea;
import com.fifesoft.rsyntaxtextarea.SyntaxConstants;
import com.fifesoft.rtextarea.RTextScrollPane;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * BidiNote 主应用入口。
 */
public class BidiNoteApp extends JFrame {
    private static final Logger log = LoggerFactory.getLogger(BidiNoteApp.class);
    private final AppConfig config = new AppConfig();
    private Localization i18n;
    private NoteService noteService;
    private NoteRepository repository;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Parser markdownParser = Parser.builder().build();
    private final HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();

    private DefaultListModel<NoteNode> noteListModel = new DefaultListModel<>();
    private JList<NoteNode> noteList;
    private RSyntaxTextArea editor;
    private JEditorPane preview;
    private JTextArea backlinkArea;
    private JTextArea mentionArea;
    private JTextArea propertyArea;
    private JTextField searchField;
    private JLabelStatus statusBar;
    private NoteNode currentNode;
    private Timer saveTimer;
    private JPopupMenu autoCompleteMenu;
    private JTabbedPane rightTabs;
    private DefaultListModel<String> queryResultModel = new DefaultListModel<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BidiNoteApp app = new BidiNoteApp();
            app.start();
        });
    }

    public void start() {
        config.load();
        i18n = new Localization(config.getLocale());
        new ThemeManager().apply(config.getTheme());
        repository = new SQLiteNoteRepository(Paths.get(config.getDbPath()));
        noteService = new NoteService(repository);
        initUi();
        loadNotes();
    }

    private void initUi() {
        setTitle("BidiNote");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(1400, 900));
        setLayout(new BorderLayout());

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton commandButton = new JButton(i18n.get("command_palette"));
        commandButton.addActionListener(e -> openCommandPalette());
        JButton themeButton = new JButton(i18n.get("toggle_theme"));
        themeButton.addActionListener(e -> toggleTheme());
        topBar.add(commandButton);
        topBar.add(themeButton);
        add(topBar, BorderLayout.NORTH);

        noteList = new JList<>(noteListModel);
        noteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        noteList.addListSelectionListener(noteSelectionListener());
        searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(200, 28));
        searchField.addActionListener(this::handleSearch);
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(searchField, BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(noteList), BorderLayout.CENTER);

        editor = new RSyntaxTextArea();
        editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_MARKDOWN);
        editor.setCodeFoldingEnabled(true);
        editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                scheduleSave();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                scheduleSave();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                scheduleSave();
            }
        });
        editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyChar() == '[') {
                    triggerAutoComplete();
                }
            }
        });
        RTextScrollPane editorScroll = new RTextScrollPane(editor);

        preview = new JEditorPane();
        preview.setContentType("text/html");
        preview.setEditable(false);
        JSplitPane editorSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorScroll, new JScrollPane(preview));
        editorSplit.setResizeWeight(0.6);

        backlinkArea = new JTextArea();
        backlinkArea.setEditable(false);
        mentionArea = new JTextArea();
        mentionArea.setEditable(false);
        propertyArea = new JTextArea();
        propertyArea.setEditable(false);
        rightTabs = new JTabbedPane();
        rightTabs.addTab(i18n.get("backlinks"), new JScrollPane(backlinkArea));
        rightTabs.addTab(i18n.get("mentions"), new JScrollPane(mentionArea));
        rightTabs.addTab(i18n.get("properties"), new JScrollPane(propertyArea));

        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, editorSplit, rightTabs);
        centerSplit.setResizeWeight(0.7);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, centerSplit);
        mainSplit.setResizeWeight(0.2);
        add(mainSplit, BorderLayout.CENTER);

        statusBar = new JLabelStatus();
        add(statusBar, BorderLayout.SOUTH);

        setupCommandPaletteShortcut();

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void setupCommandPaletteShortcut() {
        getRootPane().getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_DOWN_MASK), "commandPalette");
        getRootPane().getActionMap().put("commandPalette", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openCommandPalette();
            }
        });
    }

    private void openCommandPalette() {
        JDialog dialog = new JDialog(this, i18n.get("command_palette"), true);
        DefaultListModel<NoteNode> model = new DefaultListModel<>();
        repository.loadAllNodes().values().forEach(model::addElement);
        JList<NoteNode> list = new JList<>(model);
        JTextField filter = new JTextField();
        filter.getDocument().addDocumentListener(new SimpleDocumentListener(() -> {
            String keyword = filter.getText().toLowerCase(Locale.ROOT);
            model.clear();
            repository.loadAllNodes().values().stream()
                    .filter(node -> node.getTitle().toLowerCase(Locale.ROOT).contains(keyword))
                    .forEach(model::addElement);
        }));
        list.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                NoteNode selected = list.getSelectedValue();
                if (selected != null) {
                    selectNode(selected);
                    dialog.dispose();
                }
            }
        });
        dialog.setLayout(new BorderLayout());
        dialog.add(filter, BorderLayout.NORTH);
        dialog.add(new JScrollPane(list), BorderLayout.CENTER);
        dialog.setSize(400, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void toggleTheme() {
        String newTheme = "light".equals(config.getTheme()) ? "dark" : "light";
        config.setTheme(newTheme);
        config.save();
        JOptionPane.showMessageDialog(this, i18n.get("restart_apply"));
    }

    private void handleSearch(ActionEvent event) {
        String keyword = searchField.getText();
        executor.submit(() -> {
            List<SearchHit> hits = noteService.search(keyword);
            SwingUtilities.invokeLater(() -> {
                noteListModel.clear();
                for (SearchHit hit : hits) {
                    repository.findNodeById(hit.pageId()).ifPresent(noteListModel::addElement);
                }
                statusBar.setText(i18n.get("search_result") + hits.size());
            });
        });
    }

    private ListSelectionListener noteSelectionListener() {
        return e -> {
            if (!e.getValueIsAdjusting()) {
                NoteNode selected = noteList.getSelectedValue();
                if (selected != null) {
                    selectNode(selected);
                }
            }
        };
    }

    private void selectNode(NoteNode node) {
        currentNode = node;
        executor.submit(() -> {
            List<NoteBlock> blocks = repository.findBlocksByPageId(node.getId());
            String markdown = blocks.stream().map(NoteBlock::getText).collect(Collectors.joining("\n\n"));
            updatePreview(markdown);
            List<NoteEdge> edges = noteService.reverseLinks(node.getId());
            SwingUtilities.invokeLater(() -> {
                editor.setText(markdown);
                renderBacklinks(edges);
                renderProperties(node);
                statusBar.setText(i18n.get("loaded") + node.getTitle());
            });
        });
    }

    private void renderBacklinks(List<NoteEdge> edges) {
        StringBuilder linked = new StringBuilder(i18n.get("linked_mentions"));
        StringBuilder unlinked = new StringBuilder(i18n.get("unlinked_mentions"));
        edges.forEach(edge -> {
            repository.findNodeById(edge.getSrcPageId()).ifPresent(source -> linked.append("\n").append(source.getTitle()));
        });
        backlinkArea.setText(linked.toString());
        mentionArea.setText(unlinked.toString());
    }

    private void renderProperties(NoteNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append(i18n.get("title")).append(':').append(node.getTitle()).append('\n');
        sb.append(i18n.get("aliases")).append(':').append(String.join(",", node.getAliases())).append('\n');
        sb.append(i18n.get("tags")).append(':').append(String.join(",", node.getTags())).append('\n');
        propertyArea.setText(sb.toString());
    }

    private void scheduleSave() {
        if (currentNode == null) {
            return;
        }
        if (saveTimer != null) {
            saveTimer.stop();
        }
        saveTimer = new Timer(500, e -> saveCurrent());
        saveTimer.setRepeats(false);
        saveTimer.start();
    }

    private void saveCurrent() {
        if (currentNode == null) {
            return;
        }
        String markdown = editor.getText();
        statusBar.setText(i18n.get("saving"));
        CompletableFuture.supplyAsync(() -> noteService.save(currentNode, markdown), executor)
                .thenAccept(result -> SwingUtilities.invokeLater(() -> {
                    updatePreview(markdown);
                    renderBacklinks(result.edges());
                    mentionArea.setText(result.mentions().stream()
                            .map(m -> m.matchedText() + " -> " + repository.findNodeById(m.noteId()).map(NoteNode::getTitle).orElse("?"))
                            .collect(Collectors.joining("\n")));
                    statusBar.setText(i18n.get("saved"));
                }))
                .exceptionally(ex -> {
                    log.error("保存失败", ex);
                    SwingUtilities.invokeLater(() -> statusBar.setText(i18n.get("save_failed")));
                    return null;
                });
    }

    private void updatePreview(String markdown) {
        Node document = markdownParser.parse(markdown);
        String html = htmlRenderer.render(document);
        preview.setText(html);
    }

    private void loadNotes() {
        noteListModel.clear();
        repository.loadAllNodes().values().forEach(noteListModel::addElement);
    }

    private void triggerAutoComplete() {
        if (currentNode == null) {
            return;
        }
        String text = editor.getText();
        int caret = editor.getCaretPosition();
        if (caret < 2 || !text.substring(Math.max(0, caret - 2), caret).equals("[[")) {
            return;
        }
        if (autoCompleteMenu != null && autoCompleteMenu.isVisible()) {
            autoCompleteMenu.setVisible(false);
        }
        autoCompleteMenu = new JPopupMenu();
        repository.loadAllNodes().values().stream()
                .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
                .limit(10)
                .forEach(node -> {
                    JMenuItem item = new JMenuItem(node.getTitle());
                    item.addActionListener(e -> insertAtCaret(node.getTitle() + "]]"));
                    autoCompleteMenu.add(item);
                });
        try {
            Point p = editor.modelToView(caret).getLocation();
            autoCompleteMenu.show(editor, p.x, p.y + 20);
        } catch (BadLocationException e) {
            log.warn("自动补全定位失败", e);
        }
    }

    private void insertAtCaret(String text) {
        try {
            Document doc = editor.getDocument();
            doc.insertString(editor.getCaretPosition(), text, null);
        } catch (BadLocationException e) {
            log.error("插入自动补全失败", e);
        }
    }

    private static class JLabelStatus extends javax.swing.JLabel {
        public JLabelStatus() {
            super("Ready");
            setBorder(new EmptyBorder(4, 10, 4, 10));
        }
    }

    private static class SimpleDocumentListener implements DocumentListener {
        private final Runnable runnable;

        public SimpleDocumentListener(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            runnable.run();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            runnable.run();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            runnable.run();
        }
    }
}
