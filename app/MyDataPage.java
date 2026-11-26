package app;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

import app.Components.*;
import static app.Theme.f;
import static app.Theme.fb;
import static app.Ui.*;

/** Siswa: melihat biodata dan nilai UTS miliknya sendiri (nilai tampil dalam tabel). */
final class MyDataPage extends Page {
    private final JPanel bioBody = new JPanel(new BorderLayout(0, 18));
    private final DefaultTableModel gradeModel = readOnlyModel("No", "Mata Pelajaran", "Nilai UTS");
    private JLabel averageLabel;

    MyDataPage(AppShell app) {
        super(app);
    }

    @Override
    void onShow() {
        StudentRecord me = DataStore.STUDENT_ACCOUNTS.get(app.username());
        if (me == null) {
            return;
        }
        bioBody.removeAll();
        bioBody.add(studentHeader(me, 60), BorderLayout.NORTH);
        bioBody.add(wrapNorth(studentInfoRows(me, 240, false)), BorderLayout.CENTER);
        Theme.apply(bioBody);
        bioBody.revalidate();
        bioBody.repaint();

        gradeModel.setRowCount(0);
        double sum = 0;
        int no = 1;
        for (GradeRecord g : me.grades) {
            gradeModel.addRow(new Object[]{no++, g.subject, formatScore(g.score)});
            sum += g.score;
        }
        averageLabel.setText(me.grades.isEmpty() ? "Belum ada nilai UTS."
            : "Rata-rata nilai: " + String.format("%.1f", sum / me.grades.size()));
    }

    @Override
    JPanel view() {
        bioBody.setOpaque(false);
        JPanel holder = p(new BorderLayout());
        holder.add(bioBody, BorderLayout.NORTH);
        JScrollPane bioScroll = new JScrollPane(holder);
        bioScroll.putClientProperty("role", "clear");
        bioScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        RoundPanel bioCard = card(new BorderLayout(0, 12), 20, 24, false);
        bioCard.add(label("Biodata", fb(16), "title"), BorderLayout.NORTH);
        bioCard.add(bioScroll, BorderLayout.CENTER);

        JTable table = styledTable(gradeModel, new int[]{50, 220, 100});
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(center);
        DefaultTableCellRenderer score = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus,
                                                           int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, false, row, col);
                setFont(fb(14));
                setForeground(Theme.accentText());
                return this;
            }
        };
        score.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(2).setCellRenderer(score);

        averageLabel = label(" ", f(12), "muted");
        RoundPanel gradeCard = card(new BorderLayout(0, 12), 20, 24, false);
        gradeCard.add(label("Nilai UTS", fb(16), "title"), BorderLayout.NORTH);
        gradeCard.add(new JScrollPane(table), BorderLayout.CENTER);
        gradeCard.add(averageLabel, BorderLayout.SOUTH);

        JPanel body = p(new GridLayout(1, 2, 16, 0));
        body.add(bioCard);
        body.add(gradeCard);
        return pageShell("Data Saya", "Biodata dan nilai UTS Anda", body);
    }
}
