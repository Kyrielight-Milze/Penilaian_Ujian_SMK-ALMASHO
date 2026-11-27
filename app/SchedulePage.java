package app;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.Components.*;
import static app.Theme.f;
import static app.Theme.fb;
import static app.Theme.quality;
import static app.Ui.*;

/**
 * Operator: atur jadwal pelajaran dengan tampilan matriks bertingkat (Hari, Jam, dan Kelas 10, 11, 12).
 */
final class SchedulePage extends Page {
    static final String[] DAYS = {"Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu"};
    private static final String[] CLASSES = {"10", "11", "12"};
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
    private final JComboBox<String> classInput = new JComboBox<>(CLASSES);
    private final JTextField startInput = new JTextField();
    private final JTextField endInput = new JTextField();
    private final List<String> teacherUsernames = new ArrayList<>();
    private final List<String> subjectOptions = new ArrayList<>();

    private final MatrixModel matrixModel = new MatrixModel();
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

        JLabel timeHint = label("Format 24 jam (contoh: 07:00 dan 08:30)", f(11), "muted");
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

        // Tabel Matriks Bertingkat: HARI | JAM | KELAS (10, 11, 12)
        table = new JTable(matrixModel);
        table.setRowHeight(54);
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setCellSelectionEnabled(true);
        table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        table.setDefaultRenderer(Object.class, new MatrixCellRenderer());

        // Custom Header 2 Tingkat (HARI, JAM, dan grup KELAS -> KELAS 10 | KELAS 11 | KELAS 12)
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);
        header.setPreferredSize(new Dimension(100, 56));
        header.setDefaultRenderer(new GroupedMatrixHeaderRenderer());

        int[] widths = {88, 102, 155, 155, 155};
        for (int i = 0; i < widths.length; i++) {
            TableColumn col = table.getColumnModel().getColumn(i);
            col.setPreferredWidth(widths[i]);
            if (i < 2) {
                col.setMaxWidth(widths[i] + 25);
                col.setMinWidth(widths[i] - 10);
            }
        }

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int r = table.rowAtPoint(e.getPoint());
                int c = table.columnAtPoint(e.getPoint());
                if (r >= 0 && c >= 2) {
                    ScheduleRecord rec = matrixModel.recordAt(r, c - 2);
                    if (rec != null) {
                        showDialog(rec);
                    } else {
                        // Klik sel kosong langsung mengisi hari, kelas, dan jam ke form
                        MatrixRow mr = matrixModel.rows.get(r);
                        dayInput.setSelectedItem(mr.day);
                        classInput.setSelectedIndex(c - 2);
                        if (mr.startMinute >= 0) {
                            startInput.setText(ScheduleRecord.time(mr.startMinute));
                            endInput.setText(ScheduleRecord.time(mr.endMinute));
                        }
                        startInput.requestFocusInWindow();
                    }
                    table.clearSelection();
                }
            }
        });

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(Theme.border(), 1));

        countLabel = label("0 jadwal", f(12), "muted");
        JPanel listHead = p(new BorderLayout());
        listHead.add(label("Matriks Jadwal Pelajaran (Kelas 10, 11 & 12)", fb(16), "title"), BorderLayout.WEST);
        listHead.add(countLabel, BorderLayout.EAST);
        JPanel listTop = p(new GridLayout(0, 1, 0, 2));
        listTop.add(listHead);
        listTop.add(label("Klik sel mata pelajaran untuk ubah/hapus, atau klik sel '-' untuk mengisi jam & kelas tersebut.", f(12), "muted"));

        RoundPanel listCard = card(new BorderLayout(0, 14), 20, 22, false);
        listCard.add(listTop, BorderLayout.NORTH);
        listCard.add(tableScroll, BorderLayout.CENTER);

        formCard.setPreferredSize(new Dimension(100, 100));
        listCard.setPreferredSize(new Dimension(100, 100));
        JPanel body = p(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 0.34;
        c.insets = new Insets(0, 0, 0, 16);
        body.add(formCard, c);
        c.gridx = 1;
        c.weightx = 0.66;
        c.insets = new Insets(0, 0, 0, 0);
        body.add(listCard, c);
        return pageShell("Jadwal Pelajaran",
            "Susun jadwal pembelajaran per hari dan jam untuk Kelas 10, Kelas 11, dan Kelas 12", body);
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
        matrixModel.rebuild(DataStore.SCHEDULE);
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

    // ===================== MODEL & RENDERER MATRIKS JADWAL =====================
    private static final class MatrixRow {
        final String day;
        final boolean showDayLabel;
        final int slotRoman;
        final int startMinute;
        final int endMinute;
        final ScheduleRecord[] cells = new ScheduleRecord[3]; // index 0 = Kelas 10, 1 = Kelas 11, 2 = Kelas 12

        MatrixRow(String day, boolean showDayLabel, int slotRoman, int startMinute, int endMinute) {
            this.day = day;
            this.showDayLabel = showDayLabel;
            this.slotRoman = slotRoman;
            this.startMinute = startMinute;
            this.endMinute = endMinute;
        }
    }

    private static final class MatrixModel extends AbstractTableModel {
        private static final String[] COLS = {"HARI", "JAM", "KELAS 10", "KELAS 11", "KELAS 12"};
        private static final String[] ROMANS = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII"};
        final List<MatrixRow> rows = new ArrayList<>();

        void rebuild(List<ScheduleRecord> all) {
            rows.clear();
            for (String day : DAYS) {
                Map<String, MatrixRow> slots = new LinkedHashMap<>();
                int idx = 0;
                for (ScheduleRecord r : all) {
                    if (!day.equalsIgnoreCase(r.day)) {
                        continue;
                    }
                    String key = r.startMinute + "-" + r.endMinute;
                    MatrixRow mr = slots.get(key);
                    if (mr == null) {
                        mr = new MatrixRow(day, idx == 0, idx + 1, r.startMinute, r.endMinute);
                        slots.put(key, mr);
                        idx++;
                    }
                    int cIdx = classIndex(r.studentClass);
                    if (cIdx >= 0) {
                        mr.cells[cIdx] = r;
                    }
                }
                if (slots.isEmpty()) {
                    rows.add(new MatrixRow(day, true, 0, -1, -1));
                } else {
                    rows.addAll(slots.values());
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
                    return "-";
                }
                String rom = mr.slotRoman >= 1 && mr.slotRoman <= ROMANS.length
                    ? ROMANS[mr.slotRoman - 1] : String.valueOf(mr.slotRoman);
                return "<html><center><b>" + rom + "</b><br><span style='font-size:9px;'>"
                    + ScheduleRecord.time(mr.startMinute) + "\u2013" + ScheduleRecord.time(mr.endMinute)
                    + "</span></center></html>";
            }
            ScheduleRecord rec = mr.cells[col - 2];
            if (rec == null) {
                return "-";
            }
            return "<html><center><b>" + rec.subject.toUpperCase() + "</b><br><span style='font-size:9.5px;'>"
                + rec.teacherName() + "</span></center></html>";
        }
    }

    /** Header tabel bertingkat (HARI | JAM | KELAS -> KELAS 10, KELAS 11, KELAS 12) persis seperti contoh. */
    private static final class GroupedMatrixHeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JComponent headerCell = new JComponent() {
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
                        // Kolom HARI dan JAM membentang penuh 2 baris ke bawah
                        g2.setColor(bgHead);
                        g2.fillRect(0, 0, w, h);
                        g2.setColor(grid);
                        g2.drawLine(w - 1, 0, w - 1, h);
                        g2.drawLine(0, h - 1, w, h - 1);

                        String title = column == 0 ? "HARI" : "JAM";
                        g2.setColor(Theme.text());
                        g2.drawString(title, (w - fm.stringWidth(title)) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
                    } else {
                        // Baris atas: grup "KELAS" menyatu melintasi kolom 2, 3, dan 4
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

                        // Baris bawah: KELAS 10 | KELAS 11 | KELAS 12
                        g2.setColor(bgSub);
                        g2.fillRect(0, halfH, w, h - halfH);
                        g2.setColor(grid);
                        g2.drawLine(w - 1, halfH, w - 1, h);
                        g2.drawLine(0, h - 1, w, h - 1);

                        g2.setFont(fb(11));
                        FontMetrics fmSub = g2.getFontMetrics();
                        String sub = "KELAS " + (column + 8); // col 2->10, 3->11, 4->12
                        g2.setColor(Theme.ACCENT);
                        g2.drawString(sub, (w - fmSub.stringWidth(sub)) / 2,
                            halfH + ((h - halfH) - fmSub.getHeight()) / 2 + fmSub.getAscent());
                    }
                    g2.dispose();
                }
            };
            return headerCell;
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
            setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

            if (column == 0) {
                setFont(fb(11.5f));
                setBackground(Theme.dark ? new Color(24, 36, 58) : new Color(238, 243, 251));
                setForeground(Theme.text());
            } else if (column == 1) {
                setFont(f(11));
                setBackground(Theme.dark ? new Color(20, 30, 50) : new Color(246, 249, 254));
                setForeground(Theme.muted());
            } else {
                String str = String.valueOf(value);
                boolean empty = "-".equals(str);
                setFont(f(11));
                setBackground(Theme.surface());
                setForeground(empty ? Theme.muted() : Theme.text());
            }
            return this;
        }
    }
}