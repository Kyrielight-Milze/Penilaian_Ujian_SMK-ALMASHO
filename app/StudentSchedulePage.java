package app;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import app.Components.*;
import static app.Theme.f;
import static app.Theme.fb;
import static app.Theme.fi;
import static app.Ui.*;

/** Siswa: melihat jadwal ujian kelasnya (tanpa nama guru; tampil "Belum diset operator" jika kosong). */
final class StudentSchedulePage extends Page {
    private static final String UNSET_OPERATOR = "Belum diset operator";
    private final DefaultTableModel tableModel = readOnlyModel("Hari", "Jam Ujian", "Mata Pelajaran");
    private JLabel countLabel;
    private JLabel subtitle;

    StudentSchedulePage(AppShell app) {
        super(app);
    }

    @Override
    void onShow() {
        StudentRecord me = DataStore.STUDENT_ACCOUNTS.get(app.username());
        tableModel.setRowCount(0);
        if (me == null) {
            return;
        }
        List<ScheduleRecord> mine = new ArrayList<>();
        for (ScheduleRecord r : DataStore.SCHEDULE) {
            if (r.studentClass.equals(me.studentClass)) {
                mine.add(r);
            }
        }
        mine.sort((a, b) -> {
            int d = SchedulePage.dayIndex(a.day) - SchedulePage.dayIndex(b.day);
            return d != 0 ? d : Integer.compare(a.startMinute, b.startMinute);
        });
        if (mine.isEmpty()) {
            tableModel.addRow(new Object[]{"-", UNSET_OPERATOR, UNSET_OPERATOR});
        } else {
            for (ScheduleRecord r : mine) {
                tableModel.addRow(new Object[]{r.day, r.timeRange(), r.subject.toUpperCase()});
            }
        }
        subtitle.setText("Jadwal ujian resmi Kelas " + me.studentClass);
        countLabel.setText(mine.isEmpty() ? UNSET_OPERATOR : mine.size() + " jadwal ujian");
    }

    @Override
    JPanel view() {
        JTable table = styledTable(tableModel, new int[]{110, 150, 320});
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, false, r, c);
                String str = String.valueOf(v);
                boolean unset = UNSET_OPERATOR.equals(str) || "-".equals(str);
                setFont(unset ? fi(12) : (c == 2 ? fb(12.5f) : f(12.5f)));
                setForeground(unset ? Theme.muted() : Theme.text());
                return this;
            }
        });

        countLabel = label("0 jadwal ujian", f(12), "muted");
        JPanel head = p(new BorderLayout());
        head.add(label("Jadwal Ujian Kelas Anda", fb(16), "title"), BorderLayout.WEST);
        head.add(countLabel, BorderLayout.EAST);

        RoundPanel tableCard = card(new BorderLayout(0, 14), 20, 22, false);
        tableCard.add(head, BorderLayout.NORTH);
        tableCard.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel page = pageShell("Jadwal Ujian", "Jadwal ujian kelas Anda", tableCard);
        JPanel headPanel = (JPanel) ((BorderLayout) page.getLayout()).getLayoutComponent(BorderLayout.NORTH);
        subtitle = (JLabel) headPanel.getComponent(1);
        return page;
    }
}