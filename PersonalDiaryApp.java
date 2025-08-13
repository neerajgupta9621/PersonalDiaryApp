import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class PersonalDiaryApp extends JFrame {

    // UI
    private final JSpinner dateSpinner;
    private final JTextArea editor;
    private final JLabel statusLabel;

    // State
    private boolean dirty = false; // unsaved changes?
    private final Path diaryDir = Paths.get("diary");
    private final DateTimeFormatter fileFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final SimpleDateFormat spinnerFmt = new SimpleDateFormat("EEE, dd MMM yyyy");

    public PersonalDiaryApp() {
        super("Personal Diary");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        // ----- Top Bar -----
        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(new EmptyBorder(12, 12, 8, 12));
        top.setBackground(new Color(245, 248, 252));

        JLabel title = new JLabel("Personal Diary");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(32, 64, 112));
        top.add(title, BorderLayout.WEST);

        // Date spinner
        SpinnerDateModel dm = new SpinnerDateModel(new Date(), null, null, java.util.Calendar.DAY_OF_MONTH);
        dateSpinner = new JSpinner(dm);
        JSpinner.DateEditor de = new JSpinner.DateEditor(dateSpinner, "EEE, dd MMM yyyy");
        dateSpinner.setEditor(de);
        dateSpinner.setPreferredSize(new Dimension(220, 32));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JButton prevBtn = new JButton("◀");
        JButton nextBtn = new JButton("▶");
        styleButton(prevBtn);
        styleButton(nextBtn);
        JButton newBtn = new JButton("New");
        JButton saveBtn = new JButton("Save  ⌘/Ctrl+S");
        stylePrimary(saveBtn);
        right.add(prevBtn);
        right.add(dateSpinner);
        right.add(nextBtn);
        right.add(newBtn);
        right.add(saveBtn);
        top.add(right, BorderLayout.EAST);

        // ----- Editor -----
        editor = new JTextArea();
        editor.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        editor.setLineWrap(true);
        editor.setWrapStyleWord(true);
        editor.setMargin(new Insets(12, 16, 12, 16));
        JScrollPane scroll = new JScrollPane(editor);
        scroll.setBorder(new EmptyBorder(0, 12, 0, 12));

        // Subtle background
        editor.setBackground(new Color(255, 255, 255));

        // ----- Status Bar -----
        JPanel status = new JPanel(new BorderLayout());
        status.setBorder(new EmptyBorder(6, 12, 8, 12));
        status.setBackground(new Color(240, 244, 248));
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        status.add(statusLabel, BorderLayout.WEST);

        // ----- Menu -----
        setJMenuBar(buildMenuBar());

        // Layout
        getContentPane().setLayout(new BorderLayout(0, 8));
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(scroll, BorderLayout.CENTER);
        getContentPane().add(status, BorderLayout.SOUTH);
        getContentPane().setBackground(new Color(235, 241, 248));

        // Listeners
        editor.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { markDirty(); }
            public void removeUpdate(DocumentEvent e) { markDirty(); }
            public void changedUpdate(DocumentEvent e) { markDirty(); }
        });

        saveBtn.addActionListener(e -> saveCurrent());
        newBtn.addActionListener(e -> newEntry());
        prevBtn.addActionListener(e -> shiftDay(-1));
        nextBtn.addActionListener(e -> shiftDay(1));
        dateSpinner.addChangeListener(e -> {
            if (confirmDiscardIfDirty()) {
                loadForCurrentDate();
            } else {
                // no-op: user cancelled, keep current text/date
            }
        });

        // Window close confirmation
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (confirmDiscardIfDirty()) {
                    dispose();
                    System.exit(0);
                }
            }
        });

        // Shortcuts
        editor.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_S,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "save");
        editor.getActionMap().put("save", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { saveCurrent(); }
        });
        editor.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "find");
        editor.getActionMap().put("find", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { showFindDialog(); }
        });

        // Ensure diary directory
        try { Files.createDirectories(diaryDir); } catch (IOException ignored) {}

        // Initial load (today)
        loadForCurrentDate();
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu file = new JMenu("File");
        JMenuItem miNew = new JMenuItem("New (clear)"); miNew.addActionListener(e -> newEntry());
        JMenuItem miSave = new JMenuItem("Save"); miSave.addActionListener(e -> saveCurrent());
        JMenuItem miOpenFolder = new JMenuItem("Open Diary Folder");
        miOpenFolder.addActionListener(e -> openDiaryFolder());
        JMenuItem miExit = new JMenuItem("Exit"); miExit.addActionListener(e -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
        file.add(miNew); file.add(miSave); file.addSeparator(); file.add(miOpenFolder); file.addSeparator(); file.add(miExit);

        JMenu edit = new JMenu("Edit");
        JMenuItem miFind = new JMenuItem("Find…"); miFind.addActionListener(e -> showFindDialog());
        JMenuItem miWordCount = new JMenuItem("Word Count"); miWordCount.addActionListener(e -> showWordCount());
        edit.add(miFind); edit.add(miWordCount);

        JMenu view = new JMenu("View");
        JMenuItem miPrev = new JMenuItem("Previous Day"); miPrev.addActionListener(e -> shiftDay(-1));
        JMenuItem miNext = new JMenuItem("Next Day"); miNext.addActionListener(e -> shiftDay(1));
        view.add(miPrev); view.add(miNext);

        bar.add(file); bar.add(edit); bar.add(view);
        return bar;
    }

    private void styleButton(JButton b) {
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
    }
    private void stylePrimary(JButton b) {
        styleButton(b);
        b.setBackground(new Color(32, 122, 231));
        b.setForeground(Color.WHITE);
    }

    // ----- Core helpers -----
    private void markDirty() {
        dirty = true;
        updateStatus();
    }

    private void updateStatus() {
        String dateStr = spinnerFmt.format((Date) dateSpinner.getValue());
        int chars = editor.getText().length();
        int words = wordCount(editor.getText());
        statusLabel.setText((dirty ? "● Unsaved  |  " : "✔ Saved  |  ") + dateStr + "  |  " + words + " words, " + chars + " chars");
    }

    private void newEntry() {
        if (!confirmDiscardIfDirty()) return;
        editor.setText("");
        dirty = false;
        updateStatus();
    }

    private void shiftDay(int delta) {
        if (!confirmDiscardIfDirty()) return;
        Date d = (Date) dateSpinner.getValue();
        LocalDate ld = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().plusDays(delta);
        dateSpinner.setValue(Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        loadForCurrentDate();
    }

    private boolean confirmDiscardIfDirty() {
        if (!dirty) return true;
        int opt = JOptionPane.showConfirmDialog(this, "You have unsaved changes.\\nSave before continuing?", "Unsaved Changes",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt == JOptionPane.CANCEL_OPTION) return false;
        if (opt == JOptionPane.YES_OPTION) saveCurrent();
        return opt != JOptionPane.CANCEL_OPTION;
    }

    private void saveCurrent() {
        try {
            Path f = fileForCurrentDate();
            Files.createDirectories(diaryDir);
            Files.write(f, editor.getText().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            dirty = false;
            updateStatus();
            toast("Saved: " + f.getFileName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to save:\\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadForCurrentDate() {
        Path f = fileForCurrentDate();
        try {
            String text = Files.exists(f) ? Files.readString(f, StandardCharsets.UTF_8) : "";
            editor.setText(text);
            editor.setCaretPosition(0);
            dirty = false;
            updateStatus();
        } catch (IOException ex) {
            editor.setText("");
            dirty = false;
            updateStatus();
            JOptionPane.showMessageDialog(this, "Failed to load:\\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Path fileForCurrentDate() {
        Date d = (Date) dateSpinner.getValue();
        LocalDate ld = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        String name = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(ld) + ".txt";
        return diaryDir.resolve(name);
    }

    private void openDiaryFolder() {
        try {
            Desktop.getDesktop().open(diaryDir.toFile());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Cannot open folder:\\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ----- Find dialog -----
    private void showFindDialog() {
        JDialog dlg = new JDialog(this, "Find", true);
        dlg.setSize(360, 120);
        dlg.setLocationRelativeTo(this);
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBorder(new EmptyBorder(12,12,12,12));
        JTextField query = new JTextField();
        JButton next = new JButton("Find Next");
        JButton prev = new JButton("Find Previous");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(prev); buttons.add(next);
        p.add(new JLabel("Find text:"), BorderLayout.NORTH);
        p.add(query, BorderLayout.CENTER);
        p.add(buttons, BorderLayout.SOUTH);
        dlg.setContentPane(p);

        ActionListener findNext = e -> {
            String q = query.getText();
            String text = editor.getText();
            if (q.isEmpty() || text.isEmpty()) return;
            int start = editor.getCaretPosition();
            int idx = text.toLowerCase().indexOf(q.toLowerCase(), start);
            if (idx == -1 && start != 0) idx = text.toLowerCase().indexOf(q.toLowerCase(), 0);
            if (idx >= 0) {
                editor.requestFocus();
                editor.select(idx, idx + q.length());
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        };
        ActionListener findPrev = e -> {
            String q = query.getText();
            String text = editor.getText();
            if (q.isEmpty() || text.isEmpty()) return;
            int start = Math.max(0, editor.getCaretPosition() - 1);
            int idx = text.toLowerCase().lastIndexOf(q.toLowerCase(), start);
            if (idx == -1 && start != text.length()) idx = text.toLowerCase().lastIndexOf(q.toLowerCase(), text.length());
            if (idx >= 0) {
                editor.requestFocus();
                editor.select(idx, idx + q.length());
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        };

        next.addActionListener(findNext);
        prev.addActionListener(findPrev);
        query.addActionListener(findNext);

        dlg.setVisible(true);
    }

    private void showWordCount() {
        String t = editor.getText();
        JOptionPane.showMessageDialog(this,
                "Words: " + wordCount(t) + "\\nCharacters: " + t.length(),
                "Word Count", JOptionPane.INFORMATION_MESSAGE);
    }

    private int wordCount(String s) {
        String trimmed = s.trim();
        if (trimmed.isEmpty()) return 0;
        return trimmed.split("\\s+").length;
    }

    private void toast(String msg) {
        statusLabel.setText(statusLabel.getText() + "   |   " + msg);
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new PersonalDiaryApp().setVisible(true));
    }
}
