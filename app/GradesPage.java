package app;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import app.Components.*;
import static app.Theme.f;
import static app.Theme.fb;
import static app.Theme.quality;
import static app.Ui.*;

/**
 * Guru: tabel nilai UTS siswa per mata pelajaran. Nama siswa diberi warna status:
 * merah = belum diset guru pengampu, kuning = belum lengkap, hijau = sudah terisi.
 */
final class GradesPage extends Page {
    private static final Color RED = new Color(239, 68, 68);
    private static final Color YELLOW = new Color(234, 179, 8);
    private static final Color GREEN = new Color(34, 197, 94);
    private static final String EMPTY = "Belum diset guru pengampu";

    private final CardLayout cards = new CardLayout();
    private final JPanel switcher = new JPanel(cards);

    private final DefaultTableModel tableModel = readOnlyModel();
    private final JTable studentTable = new JTable(tableModel);
    private final List<StudentRecord> rows = new ArrayList<>();
    private final List<Integer> statuses = new ArrayList<>();
    private List<String> subjects = new ArrayList<>();

    private final JComboBox<String> subjectBox = new JComboBox<>();
    private final JTextField studentField = new JTextField();
    private final JTextField scoreInput = new JTextField();
    private JLabel statusLabel;
    private JLabel scopeLabel;
    private JLabel summaryLabel;
    private AppButton saveButton;
    private boolean updating;

    GradesPage(AppShell app) {
        super(app);
    }

    @Override
    void onShow() {
        refreshView();
    }

    @Override
    void reset() {
        scoreInput.setText("");
        studentField.setText("");
    }

    @Override
    JPanel view() {
        studentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        studentTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        studentTable.setRowHeight(42);
        studentTable.setShowGrid(false);
        studentTable.setShowHorizontalLines(true);
        studentTable.setIntercellSpacing(new Dimension(0, 1));
        studentTable.setFillsViewportHeight(true);
        studentTable.getTableHeader().setReorderingAllowed(false);
        studentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelected();
            }
        });
        studentTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = studentTable.columnAtPoint(e.getPoint());
                if (col >= 1 && col - 1 < subjectBox.getItemCount() && subjectBox.getSelectedIndex() != col - 1) {
                    subjectBox.setSelectedIndex(col - 1);
                }
            }
        });

        subjectBox.addActionListener(e -> loadSelected());
        studentField.setEditable(false);
        limitScoreInput(scoreInput);

        saveButton = new AppButton("Simpan Nilai UTS", true);
        saveButton.addActionListener(e -> saveGrade());
        statusLabel = label(" ", f(12), "muted");

        JPanel fields = p(new GridLayout(1, 3, 12, 0));
        fields.add(labeled("Mata Pelajaran", subjectBox));
        fields.add(labeled("Siswa terpilih", studentField));
        fields.add(labeled("Nilai UTS (1-100)", scoreInput));
        JPanel saveWrap = p(new BorderLayout());
        saveWrap.add(saveButton, BorderLayout.SOUTH);
        JPanel formRow = p(new BorderLayout(12, 0));
        formRow.add(fields, BorderLayout.CENTER);
        formRow.add(saveWrap, BorderLayout.EAST);
        JPanel bottom = p(new BorderLayout(0, 10));
        bottom.add(statusLabel, BorderLayout.NORTH);
        bottom.add(formRow, BorderLayout.CENTER);

        scopeLabel = label(" ", f(12), "muted");
        summaryLabel = label(" ", f(12), "muted");
        JPanel legend = p(new FlowLayout(FlowLayout.LEFT, 16, 0));
        legend.add(new LegendItem(RED, "Belum diset guru pengampu"));
        legend.add(new LegendItem(YELLOW, "Belum lengkap"));
        legend.add(new LegendItem(GREEN, "Sudah terisi"));
        JPanel legendRow = p(new BorderLayout());
        legendRow.add(legend, BorderLayout.WEST);
        legendRow.add(summaryLabel, BorderLayout.EAST);
        JPanel top = p(new GridLayout(0, 1, 0, 6));
        top.add(scopeLabel);
        top.add(legendRow);

        RoundPanel tableCard = card(new BorderLayout(0, 14), 20, 20, false);
        tableCard.add(top, BorderLayout.NORTH);
        tableCard.add(new JScrollPane(studentTable), BorderLayout.CENTER);
        tableCard.add(bottom, BorderLayout.SOUTH);

        JLabel noticeTitle = label("Mata pelajaran belum diset operator", fb(18), "title");
        noticeTitle.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel noticeText = label("Operator belum menetapkan mata pelajaran untuk akun Anda.", f(13), "muted");
        noticeText.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel noticeText2 = label("Hubungi operator, lalu buka kembali menu ini.", f(13), "muted");
        noticeText2.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel noticeInner = p(new GridLayout(0, 1, 0, 6));
        noticeInner.add(noticeTitle);
        noticeInner.add(noticeText);
        noticeInner.add(noticeText2);
        RoundPanel notice = card(new GridBagLayout(), 20, 40, false);
        notice.add(noticeInner);

        switcher.setOpaque(false);
        switcher.add(tableCard, "content");
        switcher.add(notice, "notice");

        return pageShell("Nilai UTS Siswa",
            "Pilih siswa pada tabel, lalu isi nilai UTS (1-100) untuk mata pelajaran Anda", switcher);
    }

    private static void limitScoreInput(JTextField field) {
        ((PlainDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr)
                throws BadLocationException {
                replace(fb, offset, 0, text, attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attr)
                throws BadLocationException {
                String current = fb.getDocument().getText(0, fb.getDocument().getLength());
                String next = current.substring(0, offset) + (text == null ? "" : text)
                    + current.substring(offset + length);
                if (next.isEmpty()) {
                    super.replace(fb, offset, length, text, attr);
                    return;
                }
                if (!next.matches("[1-9]\\d{0,2}") || Integer.parseInt(next) > 100) {
                    Toolkit.getDefaultToolkit().beep();
                    return;
                }
                super.replace(fb, offset, length, text, attr);
            }
        });
    }

    private void refreshView() {
        TeacherRecord teacher = DataStore.TEACHERS.get(app.username());
        subjects = teacher == null ? new ArrayList<>() : new ArrayList<>(teacher.subjects);
        if (subjects.isEmpty()) {
            cards.show(switcher, "notice");
            return;
        }
        cards.show(switcher, "content");

        Set<String> classes = DataStore.classesTaught(teacher);
        scopeLabel.setText(classes.isEmpty()
            ? "Menampilkan semua siswa karena jadwal ujian Anda belum diset operator."
            : "Menampilkan siswa kelas " + String.join(", ", classes) + " sesuai jadwal ujian Anda.");

        int keepRow = studentTable.getSelectedRow();
        String keepSubject = (String) subjectBox.getSelectedItem();
        updating = true;

        String[] columns = new String[subjects.size() + 1];
        columns[0] = "Nama Siswa";
        for (int i = 0; i < subjects.size(); i++) {
            columns[i + 1] = subjects.get(i);
        }
        tableModel.setColumnIdentifiers(columns);
        tableModel.setRowCount(0);
        rows.clear();
        statuses.clear();
        int done = 0;
        for (StudentRecord s : DataStore.studentsOf(teacher)) {
            Object[] row = new Object[columns.length];
            row[0] = s.name + "  \u00B7  " + s.studentClass;
            for (int i = 0; i < subjects.size(); i++) {
                GradeRecord g = s.gradeFor(subjects.get(i));
                row[i + 1] = g == null ? EMPTY : formatScore(g.score);
            }
            int status = DataStore.gradeStatus(s, subjects);
            if (status == 2) {
                done++;
            }
            tableModel.addRow(row);
            rows.add(s);
            statuses.add(status);
        }
        summaryLabel.setText(done + " dari " + rows.size() + " siswa sudah terisi");
        styleColumns();

        subjectBox.removeAllItems();
        for (String subject : subjects) {
            subjectBox.addItem(subject);
        }
        if (keepSubject != null && subjects.contains(keepSubject)) {
            subjectBox.setSelectedItem(keepSubject);
        }
        updating = false;

        if (keepRow >= 0 && keepRow < rows.size()) {
            studentTable.setRowSelectionInterval(keepRow, keepRow);
        } else {
            studentTable.clearSelection();
        }
        loadSelected();
    }

    private void styleColumns() {
        TableCellRenderer headerBase = studentTable.getTableHeader().getDefaultRenderer();
        for (int i = 0; i < studentTable.getColumnCount(); i++) {
            TableColumn column = studentTable.getColumnModel().getColumn(i);
            boolean first = i == 0;
            column.setPreferredWidth(first ? 280 : 165);
            column.setCellRenderer(first ? new NameRenderer() : new ScoreRenderer());
            column.setHeaderRenderer((table, value, sel, focus, r, c) -> {
                Component comp = headerBase.getTableCellRendererComponent(table, value, sel, focus, r, c);
                if (comp instanceof JLabel) {
                    JLabel l = (JLabel) comp;
                    l.setBorder(BorderFactory.createEmptyBorder(0, first ? 14 : 0, 0, 8));
                    l.setHorizontalAlignment(first ? SwingConstants.LEFT : SwingConstants.CENTER);
                    l.setOpaque(true);
                    l.setBackground(Theme.surface());
                    l.setForeground(Theme.muted());
                    l.setFont(fb(12));
                }
                return comp;
            });
        }
    }

    private void loadSelected() {
        if (updating) {
            return;
        }
        int row = studentTable.getSelectedRow();
        String subject = (String) subjectBox.getSelectedItem();
        if (row < 0 || row >= rows.size() || subject == null) {
            studentField.setText("");
            scoreInput.setText("");
            statusLabel.setText("Pilih siswa pada tabel untuk mengisi atau mengubah nilai.");
            saveButton.setText("Simpan Nilai UTS");
            return;
        }
        StudentRecord s = rows.get(row);
        GradeRecord g = s.gradeFor(subject);
        studentField.setText(s.name);
        if (g == null) {
            scoreInput.setText("");
            statusLabel.setText("Nilai " + subject + " untuk " + s.name + ": Belum diset guru pengampu.");
            saveButton.setText("Simpan Nilai UTS");
        } else {
            scoreInput.setText(formatScore(g.score));
            statusLabel.setText("Sudah dinilai (" + formatScore(g.score) + "). Ubah angkanya lalu simpan untuk memperbarui.");
            saveButton.setText("Perbarui Nilai UTS");
        }
    }

    private void saveGrade() {
        int row = studentTable.getSelectedRow();
        if (row < 0 || row >= rows.size()) {
            warn("Pilih data siswa pada tabel terlebih dahulu.", "Siswa belum dipilih");
            return;
        }
        String subject = (String) subjectBox.getSelectedItem();
        if (subject == null) {
            warn("Mata pelajaran belum dipilih.", "Data belum lengkap");
            return;
        }
        int score;
        try {
            score = Integer.parseInt(scoreInput.getText().trim());
            if (score < 1 || score > 100) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            error("Nilai UTS harus berupa angka dari 1 sampai 100.", "Nilai tidak valid");
            return;
        }
        boolean updated = DataStore.saveGrade(rows.get(row), subject, score);
        refreshView();
        success(updated ? "Nilai UTS berhasil diperbarui." : "Nilai UTS berhasil disimpan.");
    }

    private static Color tone(int status) {
        return status == 0 ? RED : (status == 1 ? YELLOW : GREEN);
    }

    private final class NameRenderer extends JComponent implements TableCellRenderer {
        private String text = "";
        private int status;
        private Color background = Color.WHITE;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            text = String.valueOf(value);
            status = row >= 0 && row < statuses.size() ? statuses.get(row) : 0;
            background = isSelected ? table.getSelectionBackground() : table.getBackground();
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            g2.setColor(background);
            g2.fillRect(0, 0, getWidth(), getHeight());
            Color tone = tone(status);
            g2.setColor(new Color(tone.getRed(), tone.getGreen(), tone.getBlue(), Theme.dark ? 55 : 45));
            g2.fillRoundRect(8, 5, getWidth() - 16, getHeight() - 10, 14, 14);
            g2.setColor(tone);
            g2.fillOval(20, getHeight() / 2 - 5, 10, 10);
            g2.setColor(Theme.text());
            g2.setFont(fb(13));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, 38, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    private static final class ScoreRenderer extends DefaultTableCellRenderer {
        ScoreRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
            boolean empty = EMPTY.equals(value);
            setFont(empty ? f(11.5f) : fb(15));
            setForeground(empty ? Theme.muted() : Theme.accentText());
            return this;
        }
    }

    private static final class LegendItem extends JComponent {
        private final Color color;
        private final String text;

        LegendItem(Color color, String text) {
            this.color = color;
            this.text = text;
            setPreferredSize(new Dimension(24 + getFontMetrics(f(12)).stringWidth(text), 20));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            g2.setColor(color);
            g2.fillOval(0, getHeight() / 2 - 5, 10, 10);
            g2.setColor(Theme.muted());
            g2.setFont(f(12));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, 16, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }
}