package app;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import app.Components.*;
import static app.Theme.f;
import static app.Theme.fb;
import static app.Theme.quality;
import static app.Ui.*;

/**
 * Operator: memantau progres tiap guru dalam menilai ujian, jumlah siswa yang dinilai,
 * dan jadwal mengajarnya. Satu baris untuk setiap mata pelajaran yang diampu guru.
 */
final class ProgressPage extends Page {
    private static final Color RED = new Color(239, 68, 68);
    private static final Color YELLOW = new Color(234, 179, 8);
    private static final Color GREEN = new Color(34, 197, 94);

    private final DefaultTableModel tableModel = readOnlyModel(
        "Guru", "Mata Pelajaran", "Jadwal", "Siswa Dinilai", "Progres Penilaian");
    private final List<int[]> progress = new ArrayList<>(); // {terisi, total} per baris
    private final JLabel[] tileValue = new JLabel[3];
    private JTable table;

    ProgressPage(AppShell app) {
        super(app);
    }

    @Override
    void onShow() {
        reload();
    }

    @Override
    JPanel view() {
        String[] captions = {"Guru terdaftar", "Mapel selesai dinilai", "Rata-rata progres"};
        JPanel tiles = p(new GridLayout(1, 3, 16, 0));
        for (int i = 0; i < 3; i++) {
            tileValue[i] = label("0", fb(26), "accent");
            RoundPanel tile = card(new BorderLayout(0, 4), 20, 18, false);
            tile.add(label(captions[i], f(12), "muted"), BorderLayout.NORTH);
            tile.add(tileValue[i], BorderLayout.CENTER);
            tiles.add(tile);
        }

        table = styledTable(tableModel, new int[]{190, 160, 190, 120, 220});
        table.getColumnModel().getColumn(4).setCellRenderer(new BarRenderer());

        JPanel legend = p(new FlowLayout(FlowLayout.LEFT, 16, 0));
        legend.add(new Legend(RED, "Belum ada nilai"));
        legend.add(new Legend(YELLOW, "Sebagian"));
        legend.add(new Legend(GREEN, "Selesai"));

        RoundPanel tableCard = card(new BorderLayout(0, 14), 20, 20, false);
        tableCard.add(legend, BorderLayout.NORTH);
        tableCard.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel body = p(new BorderLayout(0, 16));
        body.add(tiles, BorderLayout.NORTH);
        body.add(tableCard, BorderLayout.CENTER);
        return pageShell("Progres Guru",
            "Pantau progres guru dalam menilai ujian, siswa yang dinilai, dan jadwal mengajar", body);
    }

    private void reload() {
        tableModel.setRowCount(0);
        progress.clear();
        int doneRows = 0;
        int countedRows = 0;
        double percentSum = 0;

        for (TeacherRecord t : DataStore.TEACHERS.values()) {
            String shown = t.name.isEmpty() ? t.username : t.name;
            List<StudentRecord> students = DataStore.studentsOf(t);
            if (t.subjects.isEmpty()) {
                tableModel.addRow(new Object[]{shown, "Belum diatur", scheduleText(t, null), students.size() + " siswa", ""});
                progress.add(new int[]{0, 0});
                continue;
            }
            boolean first = true;
            for (String subject : t.subjects) {
                int filled = 0;
                for (StudentRecord s : students) {
                    if (s.gradeFor(subject) != null) {
                        filled++;
                    }
                }
                int total = students.size();
                tableModel.addRow(new Object[]{first ? shown : "", subject, scheduleText(t, subject),
                    filled + " dari " + total + " siswa", ""});
                progress.add(new int[]{filled, total});
                first = false;
                if (total > 0) {
                    countedRows++;
                    percentSum += 100.0 * filled / total;
                    if (filled == total) {
                        doneRows++;
                    }
                }
            }
        }
        tileValue[0].setText(String.valueOf(DataStore.TEACHERS.size()));
        tileValue[1].setText(doneRows + " dari " + countedRows);
        tileValue[2].setText(countedRows == 0 ? "0%" : Math.round(percentSum / countedRows) + "%");
    }

    /** Ringkasan jadwal: jumlah pertemuan dan kelasnya; subject null berarti semua mapel guru. */
    private static String scheduleText(TeacherRecord t, String subject) {
        int count = 0;
        Set<String> classes = new TreeSet<>();
        for (ScheduleRecord r : DataStore.SCHEDULE) {
            if (t.username.equals(r.teacherUsername) && (subject == null || subject.equalsIgnoreCase(r.subject))) {
                count++;
                classes.add(r.studentClass);
            }
        }
        if (count == 0) {
            return "Belum ada jadwal";
        }
        return count + " jadwal \u00B7 Kelas " + String.join(", ", classes);
    }

    /** Bilah progres berwarna (merah, kuning, hijau) dengan persentase. */
    private final class BarRenderer extends JComponent implements TableCellRenderer {
        private int filled;
        private int total;
        private Color background = Color.WHITE;

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            int[] pr = row >= 0 && row < progress.size() ? progress.get(row) : new int[]{0, 0};
            filled = pr[0];
            total = pr[1];
            background = isSelected ? t.getSelectionBackground() : t.getBackground();
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            g2.setColor(background);
            g2.fillRect(0, 0, getWidth(), getHeight());
            if (total == 0) {
                g2.dispose();
                return;
            }
            double ratio = (double) filled / total;
            Color tone = filled == 0 ? RED : (filled == total ? GREEN : YELLOW);
            String label = Math.round(ratio * 100) + "%";
            g2.setFont(fb(12));
            int textW = g2.getFontMetrics().stringWidth(label);
            int x = 14;
            int barW = Math.max(40, getWidth() - x - textW - 24);
            int h = 10;
            int y = (getHeight() - h) / 2;
            g2.setColor(Theme.border());
            g2.fillRoundRect(x, y, barW, h, h, h);
            if (filled > 0) {
                g2.setColor(tone);
                g2.fillRoundRect(x, y, Math.max(h, (int) Math.round(barW * ratio)), h, h, h);
            }
            g2.setColor(filled == 0 ? RED : Theme.text());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(label, x + barW + 10, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    private static final class Legend extends JComponent {
        private final Color color;
        private final String text;

        Legend(Color color, String text) {
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
