package app;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.Components.*;
import static app.Theme.f;
import static app.Theme.fb;
import static app.Theme.fi;
import static app.Theme.quality;
import static app.Ui.*;

/**
 * Operator: halaman Jadwal Ujian dengan dua mode submenu:
 * 1) inputMode = true  -> Submenu "Input Jadwal" (Atur slot jam ujian dulu, lalu pilih jam lewat dropdown).
 * 2) inputMode = false -> Submenu "Jadwal Ujian" (Hasil matriks Kelas 10, 11, 12 tanpa nama guru).
 */
final class SchedulePage extends Page {
    static final String[] DAYS = {"Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu"};
    private static final String[] CLASSES = {"10", "11", "12"};
    private static final String[] ROMANS = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII"};
    private static final String NO_TEACHER = "Belum ditentukan";
    private static final String UNSET_OPERATOR = "Belum diset operator";
    private static final Pattern TIME = Pattern.compile("^([01]?\\d|2[0-3])[:.]([0-5]\\d)$");
    private static final Preferences SLOT_PREFS = Preferences.userNodeForPackage(SchedulePage.class);

    static final class TimeSlot {
        final int startMinute;
        final int endMinute;

        TimeSlot(int startMinute, int endMinute) {
            this.startMinute = startMinute;
            this.endMinute = endMinute;
        }

        String range() {
            return ScheduleRecord.time(startMinute) + "\u2013" + ScheduleRecord.time(endMinute);
        }
    }

    private final boolean inputMode;

    // State pengaturan slot jam ujian
    private final List<TimeSlot> timeSlots = new ArrayList<>();
    private final DefaultTableModel slotModel = readOnlyModel("Sesi", "Waktu Ujian");
    private JTable slotTable;
    private final JTextField slotStartInput = new JTextField();
    private final JTextField slotEndInput = new JTextField();

    // State form input jadwal ujian
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
    private final JComboBox<String> classInput = new JComboBox<>(CLASSES);
    private final JComboBox<String> slotSelectBox = new JComboBox<>();
    private final List<String> teacherUsernames = new ArrayList<>();
    private final List<String> subjectOptions = new ArrayList<>();

    // Tabel daftar inputan (untuk submenu Input Jadwal) & tabel matriks output (untuk submenu Jadwal Ujian)
    private final DefaultTableModel inputListModel = readOnlyModel("Hari", "Jam", "Kelas", "Mata Pelajaran", "Guru Pengampu");
    private final List<ScheduleRecord> inputRows = new ArrayList<>();
    private JTable inputListTable;

    private final MatrixModel matrixModel = new MatrixModel();
    private JTable matrixTable;

    SchedulePage(AppShell app, boolean inputMode) {
        super(app);
        this.inputMode = inputMode;
        loadSavedSlots();
    }

    @Override
    void onShow() {
        loadSavedSlots();
        syncSlotsFromSchedules();
        if (inputMode) {
            refreshSlotUI();
            loadTeachers(null);
            reloadSubjects(null);
            reloadInputList();
        } else {
            reloadMatrix();
        }
    }

    @Override
    void reset() {
        exitEditMode();
    }

    @Override
    JPanel view() {
        return inputMode ? buildInputView() : buildOutputMatrixView();
    }

    // ===================== SUBMENU 1: INPUT JADWAL =====================
    private JPanel buildInputView() {
        // --- KIRI: TAHAP 1 ATUR JAM UJIAN TERLEBIH DAHULU ---
        JPanel slotTimeRow = p(new GridLayout(1, 2, 10, 0));
        slotTimeRow.add(slotStartInput);
        slotTimeRow.add(slotEndInput);

        JPanel slotForm = p(new GridBagLayout());
        addRow(slotForm, 0, "Jam mulai & selesai", slotTimeRow);
        addRow(slotForm, 1, "", label("Contoh: 07:30 dan 09:00 (format 24 jam)", f(11), "muted"));

        AppButton addSlotBtn = new AppButton("Tambah Jam", true).compact();
        addSlotBtn.addActionListener(e -> addTimeSlot());
        AppButton delSlotBtn = new AppButton("Hapus Jam", false).compact();
        delSlotBtn.addActionListener(e -> removeSelectedSlot());

        JPanel slotBtns = p(new GridLayout(1, 2, 8, 0));
        slotBtns.add(addSlotBtn);
        slotBtns.add(delSlotBtn);

        slotTable = styledTable(slotModel, new int[]{65, 140});
        JScrollPane slotScroll = new JScrollPane(slotTable);
        slotScroll.setPreferredSize(new Dimension(100, 155));

        JPanel slotTop = p(new GridLayout(0, 1, 0, 2));
        slotTop.add(label("1. Atur Slot Jam Ujian", fb(15), "title"));
        slotTop.add(label("Tentukan sesi jam terlebih dahulu agar muncul di pilihan bawah.", f(11.5f), "muted"));

        JPanel slotMiddle = p(new BorderLayout(0, 10));
        slotMiddle.add(slotForm, BorderLayout.NORTH);
        slotMiddle.add(slotBtns, BorderLayout.CENTER);
        slotMiddle.add(slotScroll, BorderLayout.SOUTH);

        RoundPanel slotCard = card(new BorderLayout(0, 12), 18, 20, false);
        slotCard.add(slotTop, BorderLayout.NORTH);
        slotCard.add(slotMiddle, BorderLayout.CENTER);

        // --- KANAN: TAHAP 2 INPUT JADWAL UJIAN (PILIH JAM SEPERTI PILIH KELAS) ---
        teacherInput.addActionListener(e -> {
            if (!updating) {
                reloadSubjects(null);
            }
        });

        JPanel form = p(new GridBagLayout());
        addRow(form, 0, "Guru pengampu", teacherInput);
        addRow(form, 1, "Mata pelajaran", subjectInput);
        addRow(form, 2, "Hari", dayInput);
        addRow(form, 3, "Kelas", classInput);
        addRow(form, 4, "Jam ujian", slotSelectBox);

        saveButton = new AppButton("Simpan Jadwal", true);
        saveButton.addActionListener(e -> save());
        cancelButton = new AppButton("Reset", false);
        cancelButton.addActionListener(e -> exitEditMode());
        JPanel buttons = p(new GridLayout(1, 2, 10, 0));
        buttons.add(saveButton);
        buttons.add(cancelButton);

        formTitle = label("2. Input Jadwal Ujian", fb(15), "title");
        formHint = label(" ", f(12), "gold");
        formHint.setVisible(false);
        JPanel formHead = p(new BorderLayout(0, 2));
        formHead.add(formTitle, BorderLayout.NORTH);
        formHead.add(formHint, BorderLayout.CENTER);

        RoundPanel formCard = card(new BorderLayout(0, 12), 18, 20, false);
        formCard.add(formHead, BorderLayout.NORTH);
        formCard.add(formScroll(form), BorderLayout.CENTER);
        formCard.add(buttons, BorderLayout.SOUTH);

        // --- BAWAH: DAFTAR JADWAL YANG SUDAH DIINPUT ---
        inputListTable = styledTable(inputListModel, new int[]{80, 125, 65, 160, 150});
        inputListTable.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        inputListTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int r = inputListTable.rowAtPoint(e.getPoint());
                if (r >= 0 && r < inputRows.size()) {
                    showDialog(inputRows.get(r));
                    inputListTable.clearSelection();
                }
            }
        });

        countLabel = label("0 jadwal", f(12), "muted");
        AppButton toOutputBtn = new AppButton("Lihat Hasil Jadwal Ujian  \u2192", false).compact();
        toOutputBtn.addActionListener(e -> app.showPage("schedule"));

        JPanel rightActions = p(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightActions.add(countLabel);
        rightActions.add(toOutputBtn);

        JPanel listHead = p(new BorderLayout());
        listHead.add(label("Daftar Inputan Jadwal Ujian", fb(15), "title"), BorderLayout.WEST);
        listHead.add(rightActions, BorderLayout.EAST);

        RoundPanel listCard = card(new BorderLayout(0, 10), 18, 18, false);
        listCard.add(listHead, BorderLayout.NORTH);
        listCard.add(new JScrollPane(inputListTable), BorderLayout.CENTER);

        // Gabungkan atas (Slot Jam + Form Input) dan bawah (Tabel Inputan)
        JPanel topGrid = p(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.gridy = 0;
        gc.weighty = 1;
        gc.gridx = 0;
        gc.weightx = 0.42;
        gc.insets = new Insets(0, 0, 0, 16);
        topGrid.add(slotCard, gc);

        gc.gridx = 1;
        gc.weightx = 0.58;
        gc.insets = new Insets(0, 0, 0, 0);
        topGrid.add(formCard, gc);

        JPanel body = p(new BorderLayout(0, 14));
        body.add(topGrid, BorderLayout.NORTH);
        body.add(listCard, BorderLayout.CENTER);

        refreshSlotUI();
        return pageShell("Input Jadwal Ujian",
            "Atur slot jam ujian terlebih dahulu, lalu pilih guru, mata pelajaran, hari, kelas, dan jam ujian", body);
    }

    // ===================== SUBMENU 2: JADWAL UJIAN (HASIL OUTPUT MATRIKS) =====================
    private JPanel buildOutputMatrixView() {
        matrixTable = new JTable(matrixModel);
        matrixTable.setRowHeight(50);
        matrixTable.setShowGrid(true);
        matrixTable.setIntercellSpacing(new Dimension(1, 1));
        matrixTable.setFillsViewportHeight(true);
        matrixTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        matrixTable.setCellSelectionEnabled(true);
        matrixTable.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        matrixTable.setDefaultRenderer(Object.class, new MatrixCellRenderer());

        JTableHeader header = matrixTable.getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);
        header.setPreferredSize(new Dimension(100, 56));
        header.setDefaultRenderer(new GroupedMatrixHeaderRenderer());

        int[] widths = {95, 120, 180, 180, 180};
        for (int i = 0; i < widths.length; i++) {
            TableColumn col = matrixTable.getColumnModel().getColumn(i);
            col.setPreferredWidth(widths[i]);
            if (i < 2) {
                col.setMaxWidth(widths[i] + 30);
                col.setMinWidth(widths[i] - 10);
            }
        }

        matrixTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int r = matrixTable.rowAtPoint(e.getPoint());
                int c = matrixTable.columnAtPoint(e.getPoint());
                if (r >= 0 && c >= 2) {
                    ScheduleRecord rec = matrixModel.recordAt(r, c - 2);
                    if (rec != null) {
                        showDialog(rec);
                    } else {
                        app.showPage("schedule_input");
                    }
                    matrixTable.clearSelection();
                }
            }
        });

        JScrollPane tableScroll = new JScrollPane(matrixTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(Theme.border(), 1));

        countLabel = label("0 jadwal ujian", f(12), "muted");
        AppButton goInputBtn = new AppButton("+ Input Jadwal", true).compact();
        goInputBtn.addActionListener(e -> app.showPage("schedule_input"));

        JPanel headRight = p(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        headRight.add(countLabel);
        headRight.add(goInputBtn);

        JPanel listHead = p(new BorderLayout());
        listHead.add(label("Jadwal Ujian SMK Al Malikus Sholeh (Kelas 10, 11 & 12)", fb(16), "title"), BorderLayout.WEST);
        listHead.add(headRight, BorderLayout.EAST);

        JPanel listTop = p(new GridLayout(0, 1, 0, 2));
        listTop.add(listHead);
        listTop.add(label("Hasil output dari menu Input Jadwal. Klik mata pelajaran untuk detail, atau klik 'Belum diset operator' untuk mengisi.", f(12), "muted"));

        RoundPanel outCard = card(new BorderLayout(0, 14), 20, 22, false);
        outCard.add(listTop, BorderLayout.NORTH);
        outCard.add(tableScroll, BorderLayout.CENTER);

        return pageShell("Jadwal Ujian",
            "Daftar jadwal ujian resmi per hari dan sesi jam untuk Kelas 10, Kelas 11, dan Kelas 12", outCard);
    }

    // ===================== PENGELOLAAN SLOT JAM UJIAN =====================
    private void loadSavedSlots() {
        timeSlots.clear();
        String raw = SLOT_PREFS.get("exam_slots", "450-540;570-660;690-780");
        for (String part : raw.split(";")) {
            String[] p = part.trim().split("-");
            if (p.length == 2) {
                try {
                    int s = Integer.parseInt(p[0]);
                    int e = Integer.parseInt(p[1]);
                    if (e > s) {
                        timeSlots.add(new TimeSlot(s, e));
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        sortSlots();
    }

    private void syncSlotsFromSchedules() {
        boolean added = false;
        for (ScheduleRecord r : DataStore.SCHEDULE) {
            if (!hasSlot(r.startMinute, r.endMinute)) {
                timeSlots.add(new TimeSlot(r.startMinute, r.endMinute));
                added = true;
            }
        }
        if (added) {
            sortSlots();
            saveSlots();
        }
    }

    private boolean hasSlot(int start, int end) {
        for (TimeSlot ts : timeSlots) {
            if (ts.startMinute == start && ts.endMinute == end) {
                return true;
            }
        }
        return false;
    }

    private void sortSlots() {
        timeSlots.sort((a, b) -> a.startMinute != b.startMinute
            ? Integer.compare(a.startMinute, b.startMinute)
            : Integer.compare(a.endMinute, b.endMinute));
    }

    private void saveSlots() {
        StringBuilder sb = new StringBuilder();
        for (TimeSlot ts : timeSlots) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(ts.startMinute).append('-').append(ts.endMinute);
        }
        SLOT_PREFS.put("exam_slots", sb.toString());
    }

    private void addTimeSlot() {
        int s = parseTime(slotStartInput.getText());
        int e = parseTime(slotEndInput.getText());
        if (s < 0 || e < 0) {
            warn("Jam mulai dan selesai harus berformat 24 jam, misalnya 07:30 dan 09:00.", "Jam tidak valid");
            return;
        }
        if (e <= s) {
            warn("Jam selesai harus lebih akhir dari jam mulai.", "Jam tidak valid");
            return;
        }
        if (hasSlot(s, e)) {
            warn("Slot jam " + ScheduleRecord.time(s) + "\u2013" + ScheduleRecord.time(e) + " sudah tersedia.", "Slot ganda");
            return;
        }
        timeSlots.add(new TimeSlot(s, e));
        sortSlots();
        saveSlots();
        slotStartInput.setText("");
        slotEndInput.setText("");
        refreshSlotUI();
        success("Slot jam ujian berhasil ditambahkan.");
    }

    private void removeSelectedSlot() {
        int row = slotTable == null ? -1 : slotTable.getSelectedRow();
        if (row < 0 || row >= timeSlots.size()) {
            warn("Pilih baris slot jam yang ingin dihapus pada tabel di atas.", "Pilih slot jam");
            return;
        }
        timeSlots.remove(row);
        saveSlots();
        refreshSlotUI();
    }

    private void refreshSlotUI() {
        if (slotModel != null) {
            slotModel.setRowCount(0);
            for (int i = 0; i < timeSlots.size(); i++) {
                String rom = i < ROMANS.length ? "Jam " + ROMANS[i] : "Jam " + (i + 1);
                slotModel.addRow(new Object[]{rom, timeSlots.get(i).range()});
            }
        }
        int keep = slotSelectBox.getSelectedIndex();
        slotSelectBox.removeAllItems();
        if (timeSlots.isEmpty()) {
            slotSelectBox.addItem(UNSET_OPERATOR);
            slotSelectBox.setEnabled(false);
        } else {
            for (int i = 0; i < timeSlots.size(); i++) {
                String rom = i < ROMANS.length ? "Jam " + ROMANS[i] : "Jam " + (i + 1);
                slotSelectBox.addItem(rom + "  (" + timeSlots.get(i).range() + ")");
            }
            slotSelectBox.setEnabled(true);
            if (keep >= 0 && keep < slotSelectBox.getItemCount()) {
                slotSelectBox.setSelectedIndex(keep);
            }
        }
    }

    // ===================== DATA GURU, MAPEL, & JADWAL =====================
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
            subjectOptions.add(selectSubject);
        }
        subjectInput.removeAllItems();
        if (subjectOptions.isEmpty()) {
            subjectInput.addItem(UNSET_OPERATOR);
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

    private void sortSchedules() {
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
    }

    private void reloadInputList() {
        sortSchedules();
        inputListModel.setRowCount(0);
        inputRows.clear();
        for (ScheduleRecord r : DataStore.SCHEDULE) {
            inputListModel.addRow(new Object[]{
                r.day, r.timeRange(), "Kelas " + r.studentClass, r.subject, r.teacherName()
            });
            inputRows.add(r);
        }
        if (countLabel != null) {
            countLabel.setText(DataStore.SCHEDULE.size() + " jadwal");
        }
    }

    private void reloadMatrix() {
        sortSchedules();
        matrixModel.rebuild(DataStore.SCHEDULE, timeSlots);
        if (countLabel != null) {
            countLabel.setText(DataStore.SCHEDULE.size() + " jadwal ujian");
        }
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
        int tIdx = teacherInput.getSelectedIndex();
        String teacher = tIdx > 0 ? teacherUsernames.get(tIdx - 1) : null;

        if (timeSlots.isEmpty() || slotSelectBox.getSelectedIndex() < 0) {
            warn("Atur minimal satu slot jam ujian di panel kiri terlebih dahulu.", "Jam ujian belum diatur");
            return;
        }
        if (subjectOptions.isEmpty() || subjectInput.getSelectedIndex() < 0) {
            warn(tIdx > 0
                ? "Guru ini belum punya mata pelajaran. Atur dulu di menu Akun Guru."
                : "Belum ada mata pelajaran. Atur dulu mapel guru di menu Akun Guru.", "Data belum lengkap");
            return;
        }

        TimeSlot chosenSlot = timeSlots.get(slotSelectBox.getSelectedIndex());
        int start = chosenSlot.startMinute;
        int end = chosenSlot.endMinute;
        String subject = subjectOptions.get(subjectInput.getSelectedIndex());

        for (ScheduleRecord r : DataStore.SCHEDULE) {
            if (r == editing || !r.day.equals(day) || !(start < r.endMinute && r.startMinute < end)) {
                continue;
            }
            if (r.studentClass.equals(kelas)) {
                warn("Bentrok dengan " + r.subject + " (Kelas " + r.studentClass + ", "
                    + r.timeRange() + ") pada hari " + day + ".", "Jadwal bentrok");
                return;
            }
            if (teacher != null && teacher.equals(r.teacherUsername)) {
                warn(r.teacherName() + " sudah mengampu " + r.subject + " di Kelas " + r.studentClass
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
            reloadInputList();
            success("Perubahan jadwal ujian berhasil disimpan.");
            return;
        }
        DataStore.addSchedule(new ScheduleRecord(day, start, end, kelas, subject, teacher));
        reloadInputList();
        success("Jadwal ujian berhasil disimpan.");
    }

    private void startEdit(ScheduleRecord r) {
        editing = r;
        loadTeachers(r.teacherUsername);
        reloadSubjects(r.subject);
        dayInput.setSelectedItem(r.day);
        classInput.setSelectedItem(r.studentClass);
        for (int i = 0; i < timeSlots.size(); i++) {
            TimeSlot ts = timeSlots.get(i);
            if (ts.startMinute == r.startMinute && ts.endMinute == r.endMinute) {
                slotSelectBox.setSelectedIndex(i);
                break;
            }
        }
        formTitle.setText("Ubah Jadwal Ujian");
        formHint.setText("Perubahan berlaku setelah Anda menekan Simpan Perubahan.");
        formHint.setVisible(true);
        saveButton.setText("Simpan Perubahan");
        cancelButton.setText("Batal");
    }

    private void exitEditMode() {
        editing = null;
        if (dayInput.getItemCount() > 0) {
            dayInput.setSelectedIndex(0);
        }
        if (classInput.getItemCount() > 0) {
            classInput.setSelectedIndex(0);
        }
        if (slotSelectBox.getItemCount() > 0) {
            slotSelectBox.setSelectedIndex(0);
        }
        if (teacherInput.getItemCount() > 0) {
            updating = true;
            teacherInput.setSelectedIndex(0);
            updating = false;
        }
        reloadSubjects(null);
        if (formTitle != null) {
            formTitle.setText("2. Input Jadwal Ujian");
            formHint.setText(" ");
            formHint.setVisible(false);
            saveButton.setText("Simpan Jadwal");
            cancelButton.setText("Reset");
        }
    }

    private void confirmDelete(ScheduleRecord r) {
        boolean yes = AppDialog.confirm(app.frame(), "Hapus Jadwal Ujian",
            "Hapus jadwal " + r.subject + " Kelas " + r.studentClass + " (" + r.day + ", " + r.timeRange() + ")?",
            "Hapus");
        if (!yes) {
            return;
        }
        DataStore.removeSchedule(r);
        if (editing == r) {
            exitEditMode();
        }
        if (inputMode) {
            reloadInputList();
        } else {
            reloadMatrix();
        }
    }

    private void showDialog(ScheduleRecord r) {
        JPanel header = personHeader(r.subject, r.day + "  \u00B7  " + r.timeRange(), null, 56);
        JPanel info = detailRows(
            new String[]{"Hari", "Jam Ujian", "Kelas", "Mata Pelajaran", "Guru Pengampu"},
            new JComponent[]{
                valueLabel(r.day, 250), valueLabel(r.timeRange(), 250),
                valueLabel("Kelas " + r.studentClass, 250),
                valueLabel(r.subject, 250), valueLabel(r.teacherName(), 250)
            });
        int action = PersonDialog.show(app.frame(), header, info);
        if (action == PersonDialog.EDIT) {
            if (!inputMode) {
                app.showPage("schedule_input");
            }
            startEdit(r);
        } else if (action == PersonDialog.DELETE) {
            confirmDelete(r);
        }
    }

    // ===================== MODEL & RENDERER MATRIKS JADWAL UJIAN =====================
    private static final class MatrixRow {
        final String day;
        final boolean showDayLabel;
        final int slotIndex;
        final int startMinute;
        final int endMinute;
        final ScheduleRecord[] cells = new ScheduleRecord[3]; // 0 = Kelas 10, 1 = Kelas 11, 2 = Kelas 12

        MatrixRow(String day, boolean showDayLabel, int slotIndex, int startMinute, int endMinute) {
            this.day = day;
            this.showDayLabel = showDayLabel;
            this.slotIndex = slotIndex;
            this.startMinute = startMinute;
            this.endMinute = endMinute;
        }
    }

    private static final class MatrixModel extends AbstractTableModel {
        private static final String[] COLS = {"HARI", "JAM", "KELAS 10", "KELAS 11", "KELAS 12"};
        final List<MatrixRow> rows = new ArrayList<>();

        void rebuild(List<ScheduleRecord> all, List<TimeSlot> configuredSlots) {
            rows.clear();
            for (String day : DAYS) {
                Map<String, MatrixRow> slotRows = new LinkedHashMap<>();
                int idx = 0;

                // Tampilkan semua slot jam yang sudah diatur operator terlebih dahulu
                for (TimeSlot ts : configuredSlots) {
                    String key = ts.startMinute + "-" + ts.endMinute;
                    slotRows.put(key, new MatrixRow(day, idx == 0, idx + 1, ts.startMinute, ts.endMinute));
                    idx++;
                }

                // Isi mata pelajaran ke masing-masing kolom kelas
                for (ScheduleRecord r : all) {
                    if (!day.equalsIgnoreCase(r.day)) {
                        continue;
                    }
                    String key = r.startMinute + "-" + r.endMinute;
                    MatrixRow mr = slotRows.get(key);
                    if (mr == null) {
                        mr = new MatrixRow(day, idx == 0, idx + 1, r.startMinute, r.endMinute);
                        slotRows.put(key, mr);
                        idx++;
                    }
                    int cIdx = classIndex(r.studentClass);
                    if (cIdx >= 0) {
                        mr.cells[cIdx] = r;
                    }
                }

                if (slotRows.isEmpty()) {
                    rows.add(new MatrixRow(day, true, 0, -1, -1));
                } else {
                    rows.addAll(slotRows.values());
                }
            }
            fireTableDataChanged();
        }

        private static int classIndex(String cls) {
            if ("10".equals(cls)) return 0;
            if ("11".equals(cls)) return 1;
            if ("12".equals(cls)) return 2;
            return -1;
        }

        ScheduleRecord recordAt(int row, int classIdx) {
            if (row < 0 || row >= rows.size() || classIdx < 0 || classIdx > 2) {
                return null;
            }
            return rows.get(row).cells[classIdx];
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return COLS.length;
        }

        @Override
        public String getColumnName(int col) {
            return COLS[col];
        }

        @Override
        public Object getValueAt(int row, int col) {
            MatrixRow mr = rows.get(row);
            if (col == 0) {
                return mr.showDayLabel ? mr.day.toUpperCase() : "";
            }
            if (col == 1) {
                if (mr.startMinute < 0) {
                    return UNSET_OPERATOR;
                }
                String rom = mr.slotIndex >= 1 && mr.slotIndex <= ROMANS.length
                    ? ROMANS[mr.slotIndex - 1] : String.valueOf(mr.slotIndex);
                return "<html><center><b>" + rom + "</b><br><span style='font-size:9.5px;'>"
                    + ScheduleRecord.time(mr.startMinute) + "\u2013" + ScheduleRecord.time(mr.endMinute)
                    + "</span></center></html>";
            }
            ScheduleRecord rec = mr.cells[col - 2];
            if (rec == null) {
                return UNSET_OPERATOR;
            }
            // Tanpa nama guru — hanya menampilkan Mata Pelajaran secara tegas dan rapi
            return rec.subject.toUpperCase();
        }
    }

    private static final class GroupedMatrixHeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            return new JComponent() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    quality(g2);
                    int w = getWidth();
                    int h = getHeight();
                    int halfH = h / 2;

                    Color bgHead = Theme.dark ? new Color(30, 44, 70) : new Color(224, 232, 244);
                    Color bgSub = Theme.dark ? new Color(24, 36, 58) : new Color(235, 241, 250);
                    Color grid = Theme.border();

                    g2.setFont(fb(11.5f));
                    FontMetrics fm = g2.getFontMetrics();

                    if (column == 0 || column == 1) {
                        g2.setColor(bgHead);
                        g2.fillRect(0, 0, w, h);
                        g2.setColor(grid);
                        g2.drawLine(w - 1, 0, w - 1, h);
                        g2.drawLine(0, h - 1, w, h - 1);

                        String title = column == 0 ? "HARI" : "JAM";
                        g2.setColor(Theme.text());
                        g2.drawString(title, (w - fm.stringWidth(title)) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
                    } else {
                        g2.setColor(bgHead);
                        g2.fillRect(0, 0, w, halfH);
                        g2.setColor(grid);
                        g2.drawLine(0, halfH - 1, w, halfH - 1);
                        if (column == 4) {
                            g2.drawLine(w - 1, 0, w - 1, halfH);
                        }
                        if (column == 3) {
                            g2.setColor(Theme.text());
                            String grp = "K E L A S";
                            g2.drawString(grp, (w - fm.stringWidth(grp)) / 2, (halfH - fm.getHeight()) / 2 + fm.getAscent());
                        }

                        g2.setColor(bgSub);
                        g2.fillRect(0, halfH, w, h - halfH);
                        g2.setColor(grid);
                        g2.drawLine(w - 1, halfH, w - 1, h);
                        g2.drawLine(0, h - 1, w, h - 1);

                        g2.setFont(fb(11));
                        FontMetrics fmSub = g2.getFontMetrics();
                        String sub = "KELAS " + (column + 8);
                        g2.setColor(Theme.ACCENT);
                        g2.drawString(sub, (w - fmSub.stringWidth(sub)) / 2,
                            halfH + ((h - halfH) - fmSub.getHeight()) / 2 + fmSub.getAscent());
                    }
                    g2.dispose();
                }
            };
        }
    }

    private static final class MatrixCellRenderer extends DefaultTableCellRenderer {
        MatrixCellRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, false, false, row, column);
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

            String str = String.valueOf(value);
            boolean unset = UNSET_OPERATOR.equals(str);

            if (column == 0) {
                setFont(fb(11.5f));
                setBackground(Theme.dark ? new Color(24, 36, 58) : new Color(238, 243, 251));
                setForeground(Theme.text());
            } else if (column == 1) {
                setFont(unset ? fi(10.5f) : f(11));
                setBackground(Theme.dark ? new Color(20, 30, 50) : new Color(246, 249, 254));
                setForeground(Theme.muted());
            } else {
                setFont(unset ? fi(11) : fb(11.5f));
                setBackground(Theme.surface());
                setForeground(unset ? Theme.muted() : Theme.text());
            }
            return this;
        }
    }
}