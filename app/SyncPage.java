package app;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.Components.*;
import static app.Theme.f;
import static app.Theme.fb;
import static app.Theme.quality;
import static app.Ui.*;

/**
 * Operator: sinkronisasi data induk dari file Dapodik (daftar_pd & daftar_gtk) atau CSV.
 * Username otomatis berformat <nisn>_almasho dan password berformat <urutan>+<nisn>.
 */
final class SyncPage extends Page {
    private static final String STUDENT_TEMPLATE = "Nama;Kelas;Alamat;Telepon;NISN";
    private static final String TEACHER_TEMPLATE = "Nama;Mata Pelajaran;NUPTK";

    private static final int NEW = 0;
    private static final int UPDATED = 1;
    private static final int SKIPPED = 2;

    private static final class Result {
        final int row;
        final String name;
        final int kind;
        final String note;

        Result(int row, String name, int kind, String note) {
            this.row = row;
            this.name = name;
            this.kind = kind;
            this.note = note;
        }
    }

    private final JComboBox<String> typeInput = new JComboBox<>(new String[]{"Data Siswa", "Data Guru"});
    private final JLabel fileName = new JLabel("Belum ada file dipilih");
    private final JLabel columnsHint = new JLabel();
    private JLabel summaryLabel;
    private AppButton syncButton;
    private File chosen;

    private final DefaultTableModel tableModel = readOnlyModel("Baris", "Nama", "Hasil & Akun");
    private final List<Result> results = new ArrayList<>();

    SyncPage(AppShell app) {
        super(app);
    }

    @Override
    void reset() {
        chosen = null;
        fileName.setText("Belum ada file dipilih");
        tableModel.setRowCount(0);
        results.clear();
        summaryLabel.setText(" ");
    }

    @Override
    JPanel view() {
        fileName.setFont(f(12));
        fileName.putClientProperty("role", "muted");
        columnsHint.setFont(f(12));
        columnsHint.putClientProperty("role", "muted");
        typeInput.addActionListener(e -> updateHint());
        updateHint();

        AppButton choose = new AppButton("Pilih File", false).compact();
        choose.addActionListener(e -> chooseFile());
        JPanel fileRow = p(new BorderLayout(10, 0));
        fileRow.add(choose, BorderLayout.WEST);
        fileRow.add(fileName, BorderLayout.CENTER);

        JPanel form = p(new GridBagLayout());
        addRow(form, 0, "Jenis data", typeInput);
        addRow(form, 1, "File", fileRow);

        JLabel intro = label("<html><div style='width:250px'>Impor langsung file Excel (.xlsx) hasil unduhan "
            + "Dapodik (daftar_pd / daftar_gtk) atau file CSV. Username otomatis <b>&lt;nisn&gt;_almasho</b> dan "
            + "password <b>&lt;urutan&gt;+&lt;nisn&gt;</b>.</div></html>",
            f(12), "muted");
        JPanel hintBox = p(new BorderLayout(0, 4));
        hintBox.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        hintBox.add(label("Aturan akun otomatis", fb(12), "title"), BorderLayout.NORTH);
        hintBox.add(columnsHint, BorderLayout.CENTER);
        JPanel formBody = p(new BorderLayout(0, 10));
        formBody.add(intro, BorderLayout.NORTH);
        formBody.add(form, BorderLayout.CENTER);
        formBody.add(hintBox, BorderLayout.SOUTH);

        syncButton = new AppButton("Sinkronkan", true);
        syncButton.addActionListener(e -> runSync());
        AppButton template = new AppButton("Unduh Template", false);
        template.addActionListener(e -> saveTemplate());
        JPanel buttons = p(new GridLayout(1, 2, 10, 0));
        buttons.add(syncButton);
        buttons.add(template);

        RoundPanel formCard = card(new BorderLayout(0, 14), 20, 22, false);
        formCard.add(label("Sinkronisasi data induk", fb(16), "title"), BorderLayout.NORTH);
        formCard.add(formScroll(formBody), BorderLayout.CENTER);
        formCard.add(buttons, BorderLayout.SOUTH);

        JTable table = styledTable(tableModel, new int[]{60, 165, 255});
        DefaultTableCellRenderer resultRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus,
                                                           int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, false, row, col);
                setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 8));
                int kind = row >= 0 && row < results.size() ? results.get(row).kind : NEW;
                setFont(fb(11.5f));
                setForeground(kind == NEW ? new Color(34, 197, 94)
                    : (kind == UPDATED ? Theme.accentText() : new Color(239, 68, 68)));
                return this;
            }
        };
        table.getColumnModel().getColumn(2).setCellRenderer(resultRenderer);

        summaryLabel = label(" ", f(12), "muted");
        JPanel listHead = p(new BorderLayout());
        listHead.add(label("Hasil sinkronisasi", fb(16), "title"), BorderLayout.WEST);
        listHead.add(summaryLabel, BorderLayout.EAST);

        RoundPanel listCard = card(new BorderLayout(0, 14), 20, 22, false);
        listCard.add(listHead, BorderLayout.NORTH);
        listCard.add(new JScrollPane(table), BorderLayout.CENTER);

        formCard.setPreferredSize(new Dimension(100, 100));
        listCard.setPreferredSize(new Dimension(100, 100));
        JPanel body = p(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridx = 0;
        c.weightx = 0.42;
        c.insets = new Insets(0, 0, 0, 16);
        body.add(formCard, c);
        c.gridx = 1;
        c.weightx = 0.58;
        c.insets = new Insets(0, 0, 0, 0);
        body.add(listCard, c);
        return pageShell("Sinkronisasi Data Induk",
            "Perbarui data siswa dan guru sekaligus dari file Excel Dapodik atau CSV", body);
    }

    private boolean isStudent() {
        return typeInput.getSelectedIndex() == 0;
    }

    private void updateHint() {
        columnsHint.setText(isStudent()
            ? "<html><div style='width:250px'><b>Username:</b> &lt;nisn&gt;_almasho<br>"
                + "<b>Password:</b> &lt;urutan&gt;+&lt;nisn&gt; (contoh: 1+3090621505)<br>"
                + "<b>Kolom dibaca:</b> Nama, Rombel Saat Ini / Kelas, Alamat, HP/Telepon, NISN.</div></html>"
            : "<html><div style='width:250px'><b>Username:</b> &lt;nuptk&gt;_almasho<br>"
                + "<b>Password:</b> &lt;urutan&gt;+&lt;nuptk&gt;<br>"
                + "<b>Kolom dibaca:</b> Nama, Bidang Studi / Mata Pelajaran, NUPTK/NIP.</div></html>");
    }

    // ===================== PILIH FILE & TEMPLATE =====================
    private void chooseFile() {
        FileDialog fd = new FileDialog(app.frame(), "Pilih file data induk", FileDialog.LOAD);
        fd.setFilenameFilter((dir, name) -> {
            String n = name.toLowerCase();
            return n.endsWith(".xlsx") || n.endsWith(".csv") || n.endsWith(".txt");
        });
        fd.setVisible(true);
        if (fd.getFile() == null) {
            return;
        }
        chosen = new File(fd.getDirectory(), fd.getFile());
        fileName.setText(chosen.getName());
    }

    private void saveTemplate() {
        FileDialog fd = new FileDialog(app.frame(), "Simpan template", FileDialog.SAVE);
        fd.setFile(isStudent() ? "template_data_siswa.csv" : "template_data_guru.csv");
        fd.setVisible(true);
        if (fd.getFile() == null) {
            return;
        }
        File out = new File(fd.getDirectory(), fd.getFile());
        try {
            String content = "\uFEFF" + (isStudent() ? STUDENT_TEMPLATE : TEACHER_TEMPLATE) + "\r\n";
            Files.write(out.toPath(), content.getBytes(StandardCharsets.UTF_8));
            success("Template disimpan: " + out.getName() + ". Isi datanya lalu pilih file itu untuk disinkronkan.");
        } catch (Exception ex) {
            error("Template gagal disimpan: " + ex.getMessage(), "Gagal menyimpan");
        }
    }

    // ===================== SINKRONISASI DENGAN ANIMASI LOADING =====================
    private void runSync() {
        if (chosen == null) {
            warn("Pilih file data induk terlebih dahulu.", "File belum dipilih");
            return;
        }

        final File fileToSync = chosen;
        final boolean student = isStudent();

        results.clear();
        tableModel.setRowCount(0);
        summaryLabel.setText("Memproses...");
        syncButton.setEnabled(false);

        LoadingOverlay loader = new LoadingOverlay(app.frame(), "Menyinkronkan Data Dapodik",
            "Membaca berkas " + fileToSync.getName() + "...");

        SwingWorker<Void, Result> worker = new SwingWorker<>() {
            private String errorMessage;
            private String errorTitle;
            private boolean emptyWarning;

            @Override
            protected Void doInBackground() {
                List<String[]> table;
                try {
                    table = MasterDataReader.read(fileToSync);
                } catch (Exception ex) {
                    errorTitle = "File tidak valid";
                    errorMessage = "File tidak dapat dibaca: " + ex.getMessage();
                    return null;
                }

                int headerRow = -1;
                for (int i = 0; i < Math.min(table.size(), 20); i++) {
                    for (String c : table.get(i)) {
                        if (norm(c).startsWith("nama")) {
                            headerRow = i;
                            break;
                        }
                    }
                    if (headerRow >= 0) {
                        break;
                    }
                }
                if (headerRow < 0) {
                    errorTitle = "Format tidak sesuai";
                    errorMessage = "Kolom \"Nama\" tidak ditemukan. Pastikan file berasal dari Dapodik atau template SINA.";
                    return null;
                }

                String[] header = table.get(headerRow);
                int cName = col(header, "nama", "namalengkap", "namasiswa", "namaguru", "namapesertadidik", "namapd");
                int cClass = col(header, "kelas", "rombelsaatini", "rombel", "rombonganbelajar", "tingkat");
                int cAddress = col(header, "alamat", "alamatjalan", "alamatlengkap");
                int cHp = col(header, "hp", "nohp", "nomorhp");
                int cTelp = col(header, "telepon", "notelepon", "nomortelepon", "telp");
                int cNisn = col(header, "nisn", "nuptk", "nip", "nipd", "nik");
                int cSubjects = col(header, "matapelajaran", "mapel", "pelajaran", "mengajar",
                    "matapelajaranyangdiampu", "bidangstudi");

                if (student && cClass < 0) {
                    errorTitle = "Format tidak sesuai";
                    errorMessage = "Kolom Kelas atau Rombel Saat Ini tidak ditemukan.";
                    return null;
                }

                int totalRows = Math.max(1, table.size() - (headerRow + 1));
                int sequence = 0;
                for (int i = headerRow + 1; i < table.size(); i++) {
                    String[] r = table.get(i);
                    String name = cell(r, cName);
                    if (name.isEmpty()) {
                        continue; // melewati baris kosong & sub-header Ayah/Ibu/Wali
                    }
                    sequence++;
                    loader.setStatus("Memproses data ke-" + sequence + " dari " + totalRows + " (" + name + ")");

                    String rawId = cell(r, cNisn);
                    String cleanId = rawId.replaceAll("[^a-zA-Z0-9]", "");
                    String idToken = cleanId.isEmpty() ? String.valueOf(sequence) : cleanId;

                    String username = generateUniqueUsername(idToken);
                    String password = sequence + "+" + idToken;

                    String hp = cell(r, cHp);
                    String phone = !hp.isEmpty() ? hp : cell(r, cTelp);

                    Result res = student
                        ? syncStudent(sequence, name, username, password, cell(r, cClass), cell(r, cAddress), phone)
                        : syncTeacher(sequence, name, username, password, cSubjects >= 0 ? cell(r, cSubjects) : null);
                    publish(res);
                }
                if (results.isEmpty() && errorMessage == null) {
                    emptyWarning = true;
                }
                return null;
            }

            @Override
            protected void process(List<Result> chunks) {
                for (Result res : chunks) {
                    results.add(res);
                    tableModel.addRow(new Object[]{res.row, res.name.isEmpty() ? "-" : res.name, res.note});
                }
            }

            @Override
            protected void done() {
                loader.close();
                syncButton.setEnabled(true);

                if (errorMessage != null) {
                    summaryLabel.setText("Gagal");
                    error(errorMessage, errorTitle);
                    return;
                }

                int created = 0;
                int updated = 0;
                int skipped = 0;
                for (Result r : results) {
                    if (r.kind == NEW) {
                        created++;
                    } else if (r.kind == UPDATED) {
                        updated++;
                    } else {
                        skipped++;
                    }
                }
                String summary = created + " baru, " + updated + " diperbarui, " + skipped + " dilewati";
                summaryLabel.setText(summary);
                if (emptyWarning || results.isEmpty()) {
                    warn("Tidak ada baris data yang bisa dibaca dari file ini.", "File kosong");
                } else {
                    success("Sinkronisasi selesai: " + summary + ".");
                }
            }
        };

        worker.execute();
        loader.showDialog();
    }

    private Result syncStudent(int seq, String name, String username, String password,
                               String classText, String address, String phone) {
        String kelas = parseClass(classText);
        if (name.isEmpty()) {
            return new Result(seq, name, SKIPPED, "Dilewati: nama kosong");
        }
        if (kelas == null) {
            return new Result(seq, name, SKIPPED, "Dilewati: kelas tidak dikenali (" + classText + ")");
        }

        StudentRecord existing = findStudentByName(name);
        if (existing != null) {
            existing.name = name;
            existing.studentClass = kelas;
            if (!address.isEmpty()) {
                existing.address = address;
            }
            if (!phone.isEmpty()) {
                existing.phone = phone;
            }
            DataStore.updateStudent(existing);
            return new Result(seq, name, UPDATED,
                "Diperbarui | User: " + existing.username + " | PW: " + existing.password);
        }

        StudentRecord s = new StudentRecord(name, kelas, address, username + "@gmail.com",
            phone, username, password);
        DataStore.addStudent(s);
        return new Result(seq, name, NEW,
            "Baru | User: " + username + " | PW: " + password);
    }

    private Result syncTeacher(int seq, String name, String username, String password, String subjectsText) {
        if (name.isEmpty()) {
            return new Result(seq, name, SKIPPED, "Dilewati: nama kosong");
        }

        TeacherRecord existing = findTeacherByName(name);
        if (existing != null) {
            existing.name = name;
            if (subjectsText != null && !subjectsText.isEmpty()) {
                existing.subjects.clear();
                existing.subjects.addAll(DataStore.parseSubjects(subjectsText));
            }
            DataStore.updateTeacher(existing);
            return new Result(seq, name, UPDATED,
                "Diperbarui | User: " + existing.username + " | PW: " + existing.password);
        }

        TeacherRecord t = new TeacherRecord(name, username, password);
        if (subjectsText != null && !subjectsText.isEmpty()) {
            t.subjects.addAll(DataStore.parseSubjects(subjectsText));
        }
        DataStore.addTeacher(t);
        return new Result(seq, name, NEW,
            "Baru | User: " + username + " | PW: " + password);
    }

    // ===================== PEMBENTUK USERNAME =====================
    /** Username berformat <nisn>_almasho (contoh: 3090621505_almasho). */
    private static String generateUniqueUsername(String idToken) {
        String clean = idToken == null ? "" : idToken.toLowerCase().replaceAll("[^a-z0-9]", "");
        if (clean.isEmpty()) {
            clean = "user";
        }
        String base = clean + "_almasho";
        if (!DataStore.isUsernameTaken(base)) {
            return base;
        }
        int suffix = 2;
        while (DataStore.isUsernameTaken(clean + "_" + suffix + "_almasho")) {
            suffix++;
        }
        return clean + "_" + suffix + "_almasho";
    }

    private static StudentRecord findStudentByName(String name) {
        for (StudentRecord s : DataStore.STUDENTS) {
            if (s.name != null && s.name.trim().equalsIgnoreCase(name.trim())) {
                return s;
            }
        }
        return null;
    }

    private static TeacherRecord findTeacherByName(String name) {
        for (TeacherRecord t : DataStore.TEACHERS.values()) {
            if (t.name != null && t.name.trim().equalsIgnoreCase(name.trim())) {
                return t;
            }
        }
        return null;
    }

    private static String norm(String s) {
        return s == null ? "" : s.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private static int col(String[] header, String... names) {
        for (int i = 0; i < header.length; i++) {
            String h = norm(header[i]);
            for (String n : names) {
                if (h.equals(n)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String cell(String[] row, int index) {
        return index >= 0 && index < row.length && row[index] != null ? row[index].trim() : "";
    }

    private static String parseClass(String text) {
        String t = text == null ? "" : text.trim().toUpperCase();
        Matcher m = Pattern.compile("(^|[^0-9])(1[0-2])([^0-9]|$)").matcher(t);
        if (m.find()) {
            return m.group(2);
        }
        m = Pattern.compile("^(XII|XI|X)([^A-Z]|$)").matcher(t);
        if (m.find()) {
            return m.group(1).equals("XII") ? "12" : (m.group(1).equals("XI") ? "11" : "10");
        }
        return null;
    }

    // ===================== DIALOG ANIMASI LOADING =====================
    private static final class LoadingOverlay {
        private final JDialog dialog;
        private final JLabel statusLabel;
        private final Timer spinTimer;
        private int angle = 0;

        LoadingOverlay(JFrame owner, String titleText, String initialStatus) {
            dialog = new JDialog(owner, true);
            dialog.setUndecorated(true);
            dialog.setBackground(new Color(0, 0, 0, 0));

            JPanel card = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    quality(g2);
                    int w = getWidth();
                    int h = getHeight();
                    g2.setColor(Theme.surface());
                    g2.fillRoundRect(4, 4, w - 8, h - 8, 22, 22);
                    g2.setColor(Theme.border());
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawRoundRect(4, 4, w - 8, h - 8, 22, 22);
                    g2.dispose();
                }
            };
            card.setOpaque(false);
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBorder(BorderFactory.createEmptyBorder(26, 30, 26, 30));

            JComponent spinner = new JComponent() {
                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(56, 56);
                }

                @Override
                public Dimension getMaximumSize() {
                    return new Dimension(56, 56);
                }

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    quality(g2);
                    int d = 42;
                    int x = (getWidth() - d) / 2;
                    int y = (getHeight() - d) / 2;

                    g2.setStroke(new BasicStroke(4.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.setColor(Theme.accentSoft());
                    g2.drawOval(x, y, d, d);

                    g2.setColor(Theme.ACCENT);
                    g2.draw(new Arc2D.Double(x, y, d, d, angle, 115, Arc2D.OPEN));
                    g2.dispose();
                }
            };
            spinner.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel title = new JLabel(titleText, SwingConstants.CENTER);
            title.setFont(fb(15));
            title.setForeground(Theme.text());
            title.setAlignmentX(Component.CENTER_ALIGNMENT);

            statusLabel = new JLabel(initialStatus, SwingConstants.CENTER);
            statusLabel.setFont(f(12));
            statusLabel.setForeground(Theme.muted());
            statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            card.add(spinner);
            card.add(Box.createVerticalStrut(14));
            card.add(title);
            card.add(Box.createVerticalStrut(6));
            card.add(statusLabel);

            dialog.setContentPane(card);
            dialog.setSize(390, 185);
            dialog.setLocationRelativeTo(owner);

            spinTimer = new Timer(24, e -> {
                angle = (angle - 12) % 360;
                spinner.repaint();
            });
        }

        void setStatus(String text) {
            SwingUtilities.invokeLater(() -> statusLabel.setText(text));
        }

        void showDialog() {
            spinTimer.start();
            dialog.setVisible(true);
        }

        void close() {
            spinTimer.stop();
            dialog.dispose();
        }
    }
}