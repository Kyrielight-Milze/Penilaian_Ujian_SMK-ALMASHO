package app;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import app.Components.*;
import static app.Theme.f;
import static app.Theme.fb;
import static app.Ui.*;

/** Siswa: melihat jadwal pelajaran kelasnya (hanya lihat, tidak bisa diubah). */
final class StudentSchedulePage extends Page {
    private final DefaultTableModel tableModel = readOnlyModel("Hari", "Jam", "Mata Pelajaran", "Guru Pengampu");
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
        for (ScheduleRecord r : mine) {
            tableModel.addRow(new Object[]{r.day, r.timeRange(), r.subject, r.teacherName()});
        }
        subtitle.setText("Jadwal pelajaran kelas " + me.studentClass);
        countLabel.setText(mine.size() + " jadwal");
    }

    @Override
    JPanel view() {
        JTable table = styledTable(tableModel, new int[]{90, 120, 230, 230});
        countLabel = label("0 jadwal", f(12), "muted");
        JPanel head = p(new BorderLayout());
        head.add(label("Jadwal kelas Anda", fb(16), "title"), BorderLayout.WEST);
        head.add(countLabel, BorderLayout.EAST);

        RoundPanel tableCard = card(new BorderLayout(0, 14), 20, 22, false);
        tableCard.add(head, BorderLayout.NORTH);
        tableCard.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel page = pageShell("Jadwal Pelajaran", "Jadwal pelajaran kelas Anda", tableCard);
        // subtitle (label kedua di kepala halaman) diperbarui saat halaman dibuka
        JPanel headPanel = (JPanel) ((BorderLayout) page.getLayout()).getLayoutComponent(BorderLayout.NORTH);
        subtitle = (JLabel) headPanel.getComponent(1);
        return page;
    }
}
