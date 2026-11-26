package app;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.Components.*;
import static app.Theme.f;
import static app.Theme.fb;
import static app.Ui.*;

/**
 * Operator: atur jadwal pelajaran. Urutan input: guru pengampu, mata pelajaran (otomatis sesuai
 * mapel guru yang dipilih di Akun Guru), hari, kelas, lalu jam. Hasilnya tampil dalam bentuk tabel.
 */
final class SchedulePage extends Page {
    static final String[] DAYS = {"Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu"};
    private static final String NO_TEACHER = "Belum ditentukan";
    private static final Pattern TIME = Pattern.compile("^([01]?\\d|2[0-3])[:.]([0-5]\\d)$");

    private ScheduleRecord editing;
    private JLabel formTitle;
    private JLabel formHint;
    private JLabel countLabel;
    private AppButton saveButton;
    private AppButton cancelButton;
    private boolean updating;

    private final JComboBox<String> teacherInput = new JComboBox<>();
    private final JComboBox<String> subjectInput = new JComboBox<>();
    private final JComboBox<String> dayInput = new JComboBox<>(DAYS);
    private final JComboBox<String> classInput = new JComboBox<>(new String[]{"10", "11", "12"});
    private final JTextField startInput = new JTextField();
    private final JTextField endInput = new JTextField();
    private final List<String> teacherUsernames = new ArrayList<>();
    private final List<String> subjectOptions = new ArrayList<>();

    private final DefaultTableModel tableModel = readOnlyModel("Hari", "Jam", "Kelas", "Mata Pelajaran", "Guru");
    private final List<ScheduleRecord> rows = new ArrayList<>();
    private JTable table;

    SchedulePage(AppShell app) {
        super(app);
    }

    @Override
    void onShow() {
        loadTeachers(null);
        reloadSubjects(null);
        reloadTable();
    }

    @Override
    void reset() {
        exitEditMode();
    }

    @Override
    JPanel view() {
        teacherInput.addActionListener(e -> {
            if (!updating) {
                reloadSubjects(null);
            }
        });

        JLabel timeHint = label("Format 24 jam", f(11), "muted");
        JPanel timeRow = p(new GridLayout(1, 2, 10, 0));
        timeRow.add(startInput);
        timeRow.add(endInput);

        JPanel form = p(new GridBagLayout());
        addRow(form, 0, "Guru pengampu", teacherInput);
        addRow(form, 1, "Mata pelajaran", subjectInput);
        addRow(form, 2, "Hari", dayInput);
        addRow(form, 3, "Kelas", classInput);
        addRow(form, 4, "Jam", timeRow);
        addRow(form, 5, "", timeHint);

        saveButton = new AppButton("Tambah Jadwal", true);
        saveButton.addActionListener(e -> save());
        cancelButton = new AppButton("Reset", false);
        cancelButton.addActionListener(e -> exitEditMode());
        JPanel buttons = p(new GridLayout(1, 2, 10, 0));
        buttons.add(saveButton);
        buttons.add(cancelButton);

        formTitle = label("Tambah jadwal pelajaran", fb(16), "title");
        formHint = label(" ", f(12), "gold");
        formHint.setVisible(false);
        JPanel formHead = p(new BorderLayout(0, 2));
        formHead.add(formTitle, BorderLayout.NORTH);
        formHead.add(formHint, BorderLayout.CENTER);

        RoundPanel formCard = card(new BorderLayout(0, 14), 20, 22, false);
        formCard.add(formHead, BorderLayout.NORTH);
        formCard.add(formScroll(form), BorderLayout.CENTER);
        formCard.add(buttons, BorderLayout.SOUTH);

        table = styledTable(tableModel, new int[]{70, 100, 55, 125, 115});
        table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0 && row < rows.size()) {
                    showDialog(rows.get(row));
                    table.clearSelection();
                }
            }
        });

        countLabel = label("0 jadwal", f(12), "muted");
        JPanel listHead = p(new BorderLayout());
        listHead.add(label("Jadwal terdaftar", fb(16), "title"), BorderLayout.WEST);
        listHead.add(countLabel, BorderLayout.EAST);
        JPanel listTop = p(new GridLayout(0, 1, 0, 2));
        listTop.add(listHead);
        listTop.add(label("Klik baris jadwal untuk detail, ubah, atau hapus.", f(12), "muted"));

        RoundPanel listCard = card(new BorderLayout(0, 14), 20, 22, false);
        listCard.add(listTop, BorderLayout.NORTH);
        listCard.add(new JScrollPane(table), BorderLayout.CENTER);

        formCard.setPreferredSize(new Dimension(100, 100));
        listCard.setPreferredSize(new Dimension(100, 100));
        JPanel body = p(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 0.38;
        c.insets = new Insets(0, 0, 0, 16);
        body.add(formCard, c);
        c.gridx = 1;
        c.weightx = 0.62;
        c.insets = new Insets(0, 0, 0, 0);
        body.add(listCard, c);
        return pageShell("Jadwal Pelajaran",
            "Pilih guru pengampu, lalu atur mata pelajaran, hari, kelas, dan jam", body);
    }

    // ===================== DATA =====================
    private void loadTeachers(String selectUsername) {
        updating = true;
        Object keep = selectUsername != null ? selectUsername
            : (teacherInput.getSelectedIndex() > 0 ? teacherUsernames.get(teacherInput.getSelectedIndex() - 1) : null);
        teacherInput.removeAllItems();
        teacherUsernames.clear();
        teacherInput.addItem(NO_TEACHER);
        for (TeacherRecord t : DataStore.TEACHERS.values()) {
            teacherUsernames.add(t.username);
            teacherInput.addItem(t.name.isEmpty() ? t.username : t.name);
        }
        int idx = keep == null ? -1 : teacherUsernames.indexOf(keep);
        teacherInput.setSelectedIndex(idx >= 0 ? idx + 1 : 0);
        updating = false;
    }

    /**
     * Mengisi pilihan mata pelajaran sesuai guru yang dipilih (dari Akun Guru).
     * Bila guru belum ditentukan, yang tampil adalah gabungan mapel semua guru.
     */
    private void reloadSubjects(String selectSubject) {
        updating = true;
        subjectOptions.clear();
        int tIdx = teacherInput.getSelectedIndex();
        if (tIdx > 0 && tIdx - 1 < teacherUsernames.size()) {
            TeacherRecord t = DataStore.TEACHERS.get(teacherUsernames.get(tIdx - 1));
            if (t != null) {
                subjectOptions.addAll(t.subjects);
            }
        } else {
            TreeSet<String> all = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            for (TeacherRecord t : DataStore.TEACHERS.values()) {
                all.addAll(t.subjects);
            }
            subjectOptions.addAll(all);
        }
        if (selectSubject != null && !containsIgnoreCase(subjectOptions, selectSubject)) {
            subjectOptions.add(selectSubject); // jadwal lama tetap bisa dibuka walau mapel guru sudah berubah
        }
        subjectInput.removeAllItems();
        if (subjectOptions.isEmpty()) {
            subjectInput.addItem(tIdx > 0 ? "Guru ini belum punya mata pelajaran" : "Belum ada mata pelajaran");
            subjectInput.setEnabled(false);
        } else {
            for (String s : subjectOptions) {
                subjectInput.addItem(s);
            }
            subjectInput.setEnabled(true);
            if (selectSubject != null) {
                subjectInput.setSelectedItem(selectSubject);
            }
        }
        updating = false;
    }

    private static boolean containsIgnoreCase(List<String> list, String value) {
        for (String s : list) {
            if (s.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private void reloadTable() {
        DataStore.SCHEDULE.sort((a, b) -> {
            int d = dayIndex(a.day) - dayIndex(b.day);
            if (d != 0) {
                return d;
            }
            if (a.startMinute != b.startMinute) {
                return Integer.compare(a.startMinute, b.startMinute);
            }
            return a.studentClass.compareTo(b.studentClass);
        });
        tableModel.setRowCount(0);
        rows.clear();
        for (ScheduleRecord r : DataStore.SCHEDULE) {
            tableModel.addRow(new Object[]{r.day, r.timeRange(), r.studentClass, r.subject, r.teacherName()});
            rows.add(r);
        }
        countLabel.setText(DataStore.SCHEDULE.size() + " jadwal");
    }

    static int dayIndex(String day) {
        for (int i = 0; i < DAYS.length; i++) {
            if (DAYS[i].equals(day)) {
                return i;
            }
        }
        return DAYS.length;
    }

    private static int parseTime(String text) {
        Matcher m = TIME.matcher(text.trim());
        if (!m.matches()) {
            return -1;
        }
        return Integer.parseInt(m.group(1)) * 60 + Integer.parseInt(m.group(2));
    }

    // ===================== SIMPAN / UBAH / HAPUS =====================
    private void save() {
        String day = (String) dayInput.getSelectedItem();
        String kelas = (String) classInput.getSelectedItem();
        int start = parseTime(startInput.getText());
        int end = parseTime(endInput.getText());
        int tIdx = teacherInput.getSelectedIndex();
        String teacher = tIdx > 0 ? teacherUsernames.get(tIdx - 1) : null;

        if (subjectOptions.isEmpty() || subjectInput.getSelectedIndex() < 0) {
            warn(tIdx > 0
                ? "Guru ini belum punya mata pelajaran. Atur dulu di menu Akun Guru."
                : "Belum ada mata pelajaran. Atur dulu mapel guru di menu Akun Guru.", "Data belum lengkap");
            return;
        }
        String subject = subjectOptions.get(subjectInput.getSelectedIndex());
        if (start < 0 || end < 0) {
            warn("Jam harus berformat 24 jam, misalnya 07:00 dan 08:30.", "Jam tidak valid");
            return;
        }
        if (end <= start) {
            warn("Jam selesai harus lebih akhir dari jam mulai.", "Jam tidak valid");
            return;
        }

        for (ScheduleRecord r : DataStore.SCHEDULE) {
            if (r == editing || !r.day.equals(day) || !(start < r.endMinute && r.startMinute < end)) {
                continue;
            }
            if (r.studentClass.equals(kelas)) {
                warn("Bentrok dengan " + r.subject + " (kelas " + r.studentClass + ", "
                    + r.timeRange() + ") pada hari " + day + ".", "Jadwal bentrok");
                return;
            }
            if (teacher != null && teacher.equals(r.teacherUsername)) {
                warn(r.teacherName() + " sudah mengajar " + r.subject + " di kelas " + r.studentClass
                    + " pada " + r.timeRange() + " hari " + day + ".", "Guru bentrok");
                return;
            }
        }

        if (editing != null) {
            editing.day = day;
            editing.studentClass = kelas;
            editing.startMinute = start;
            editing.endMinute = end;
            editing.subject = subject;
            editing.teacherUsername = teacher;
            DataStore.updateSchedule(editing);
            exitEditMode();
            reloadTable();
            success("Perubahan jadwal berhasil disimpan.");
            return;
        }
        DataStore.addSchedule(new ScheduleRecord(day, start, end, kelas, subject, teacher));
        reloadTable();
        // pertahankan guru/hari/kelas supaya input jadwal berikutnya lebih cepat
        startInput.setText(ScheduleRecord.time(end));
        endInput.setText("");
        endInput.requestFocusInWindow();
        success("Jadwal berhasil ditambahkan.");
    }

    private void startEdit(ScheduleRecord r) {
        editing = r;
        loadTeachers(r.teacherUsername);
        reloadSubjects(r.subject);
        dayInput.setSelectedItem(r.day);
        classInput.setSelectedItem(r.studentClass);
        startInput.setText(ScheduleRecord.time(r.startMinute));
        endInput.setText(ScheduleRecord.time(r.endMinute));
        formTitle.setText("Ubah jadwal");
        formHint.setText("Perubahan berlaku setelah Anda menekan Simpan Perubahan.");
        formHint.setVisible(true);
        saveButton.setText("Simpan Perubahan");
        cancelButton.setText("Batal");
        startInput.requestFocusInWindow();
    }

    private void exitEditMode() {
        editing = null;
        startInput.setText("");
        endInput.setText("");
        dayInput.setSelectedIndex(0);
        classInput.setSelectedIndex(0);
        if (teacherInput.getItemCount() > 0) {
            updating = true;
            teacherInput.setSelectedIndex(0);
            updating = false;
        }
        reloadSubjects(null);
        formTitle.setText("Tambah jadwal pelajaran");
        formHint.setText(" ");
        formHint.setVisible(false);
        saveButton.setText("Tambah Jadwal");
        cancelButton.setText("Reset");
    }

    private void confirmDelete(ScheduleRecord r) {
        boolean yes = AppDialog.confirm(app.frame(), "Hapus Jadwal",
            "Hapus jadwal " + r.subject + " kelas " + r.studentClass + " (" + r.day + ", " + r.timeRange() + ")?",
            "Hapus");
        if (!yes) {
            return;
        }
        DataStore.removeSchedule(r);
        if (editing == r) {
            exitEditMode();
        }
        reloadTable();
    }

    private void showDialog(ScheduleRecord r) {
        JPanel header = personHeader(r.subject, r.day + "  \u00B7  " + r.timeRange(), null, 56);
        JPanel info = detailRows(
            new String[]{"Hari", "Jam", "Kelas", "Mata pelajaran", "Guru pengampu"},
            new JComponent[]{
                valueLabel(r.day, 250), valueLabel(r.timeRange(), 250),
                valueLabel("Kelas " + r.studentClass, 250),
                valueLabel(r.subject, 250), valueLabel(r.teacherName(), 250)
            });
        int action = PersonDialog.show(app.frame(), header, info);
        if (action == PersonDialog.EDIT) {
            startEdit(r);
        } else if (action == PersonDialog.DELETE) {
            confirmDelete(r);
        }
    }
}
