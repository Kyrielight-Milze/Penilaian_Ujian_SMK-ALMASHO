package app;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
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
import static app.Ui.*;

/**
 * Operator: sinkronisasi data induk. Data siswa atau guru diimpor dari file Excel (.xlsx) atau CSV,
 * misalnya hasil ekspor Dapodik. Data yang sudah ada (username/Gmail sama) diperbarui, yang baru ditambahkan.
 */
final class SyncPage extends Page {
    private static final String DEFAULT_PASSWORD = "12345678";
    private static final String STUDENT_TEMPLATE = "Nama;Kelas;Email;Alamat;Telepon;NISN";
    private static final String TEACHER_TEMPLATE = "Nama;Email;Mata Pelajaran;NUPTK";

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
    private File chosen;

    private final DefaultTableModel tableModel = readOnlyModel("Baris", "Nama", "Hasil");
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

        JLabel intro = label("<html><div style='width:250px'>Impor data induk dari file Excel (.xlsx) atau CSV, "
            + "misalnya hasil ekspor Dapodik. Data yang sudah ada diperbarui, data baru ditambahkan.</div></html>",
            f(12), "muted");
        JPanel hintBox = p(new BorderLayout(0, 4));
        hintBox.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        hintBox.add(label("Kolom yang dibaca", fb(12), "title"), BorderLayout.NORTH);
        hintBox.add(columnsHint, BorderLayout.CENTER);
        JPanel formBody = p(new BorderLayout(0, 10));
        formBody.add(intro, BorderLayout.NORTH);
        formBody.add(form, BorderLayout.CENTER);
        formBody.add(hintBox, BorderLayout.SOUTH);

        AppButton sync = new AppButton("Sinkronkan", true);
        sync.addActionListener(e -> runSync());
        AppButton template = new AppButton("Unduh Template", false);
        template.addActionListener(e -> saveTemplate());
        JPanel buttons = p(new GridLayout(1, 2, 10, 0));
        buttons.add(sync);
        buttons.add(template);

        RoundPanel formCard = card(new BorderLayout(0, 14), 20, 22, false);
        formCard.add(label("Sinkronisasi data induk", fb(16), "title"), BorderLayout.NORTH);
        formCard.add(formScroll(formBody), BorderLayout.CENTER);
        formCard.add(buttons, BorderLayout.SOUTH);

        JTable table = styledTable(tableModel, new int[]{70, 170, 220});
        DefaultTableCellRenderer resultRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus,
                                                           int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, false, row, col);
                setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 8));
                int kind = row >= 0 && row < results.size() ? results.get(row).kind : NEW;
                setFont(fb(12));
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
            "Perbarui data siswa dan guru sekaligus dari file Excel atau CSV", body);
    }

    private boolean isStudent() {
        return typeInput.getSelectedIndex() == 0;
    }

    private void updateHint() {
        columnsHint.setText(isStudent()
            ? "<html><div style='width:250px'>Nama, Kelas (10/11/12), Email (Gmail, jadi username), Alamat, Telepon, "
                + "NISN (jadi kata sandi awal).</div></html>"
            : "<html><div style='width:250px'>Nama, Email (Gmail, jadi username), Mata Pelajaran "
                + "(pisahkan dengan koma), NUPTK (jadi kata sandi awal).</div></html>");
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
            // titik koma + BOM supaya langsung rapi saat dibuka di Excel
            String content = "\uFEFF" + (isStudent() ? STUDENT_TEMPLATE : TEACHER_TEMPLATE) + "\r\n";
            Files.write(out.toPath(), content.getBytes(StandardCharsets.UTF_8));
            success("Template disimpan: " + out.getName() + ". Isi datanya lalu pilih file itu untuk disinkronkan.");
        } catch (Exception ex) {
            error("Template gagal disimpan: " + ex.getMessage(), "Gagal menyimpan");
        }
    }

    // ===================== SINKRONISASI =====================
    private void runSync() {
        if (chosen == null) {
            warn("Pilih file data induk terlebih dahulu.", "File belum dipilih");
            return;
        }
        List<String[]> table;
        try {
            table = MasterDataReader.read(chosen);
        } catch (Exception ex) {
            error("File tidak dapat dibaca: " + ex.getMessage(), "File tidak valid");
            return;
        }
        int headerRow = -1;
        for (int i = 0; i < Math.min(table.size(), 20); i++) {
            for (String cell : table.get(i)) {
                if (norm(cell).startsWith("nama")) {
                    headerRow = i;
                    break;
                }
            }
            if (headerRow >= 0) {
                break;
            }
        }
        if (headerRow < 0) {
            error("Kolom \"Nama\" tidak ditemukan. Unduh template untuk melihat format yang dibaca.", "Format tidak sesuai");
            return;
        }
        String[] header = table.get(headerRow);
        boolean student = isStudent();
        int cName = col(header, "nama", "namalengkap", "namasiswa", "namaguru", "namapesertadidik", "namapd");
        int cEmail = col(header, "email", "gmail", "emailgmail", "username", "akun");
        int cClass = col(header, "kelas", "rombel", "rombonganbelajar", "tingkat");
        int cAddress = col(header, "alamat", "alamatjalan", "alamatlengkap");
        int cPhone = col(header, "telepon", "notelepon", "nomortelepon", "hp", "nohp", "nomorhp", "telp");
        int cPass = col(header, "password", "katasandi", "sandi");
        int cSecret = col(header, "nisn", "nuptk", "nip", "nik");
        int cSubjects = col(header, "matapelajaran", "mapel", "pelajaran", "mengajar", "matapelajaranyangdiampu");

        if (cEmail < 0) {
            error("Kolom Email (Gmail) tidak ditemukan. Kolom ini dipakai sebagai username.", "Format tidak sesuai");
            return;
        }
        if (student && cClass < 0) {
            error("Kolom Kelas tidak ditemukan.", "Format tidak sesuai");
            return;
        }

        results.clear();
        tableModel.setRowCount(0);
        for (int i = headerRow + 1; i < table.size(); i++) {
            String[] r = table.get(i);
            String name = get(r, cName);
            String email = get(r, cEmail).toLowerCase();
            if (name.isEmpty() && email.isEmpty()) {
                continue; // baris kosong
            }
            String password = !get(r, cPass).isEmpty() ? get(r, cPass)
                : (!get(r, cSecret).isEmpty() ? get(r, cSecret) : null);
            Result res = student
                ? syncStudent(i + 1, name, email, get(r, cClass), get(r, cAddress), get(r, cPhone), password)
                : syncTeacher(i + 1, name, email, cSubjects >= 0 ? get(r, cSubjects) : null, password);
            results.add(res);
            tableModel.addRow(new Object[]{res.row, res.name.isEmpty() ? email : res.name, res.note});
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
        if (results.isEmpty()) {
            warn("Tidak ada baris data yang bisa dibaca dari file ini.", "File kosong");
        } else {
            success("Sinkronisasi selesai: " + summary + ".");
        }
    }

    private Result syncStudent(int row, String name, String email, String classText, String address,
                               String phone, String password) {
        String kelas = parseClass(classText);
        if (name.isEmpty()) {
            return new Result(row, name, SKIPPED, "Dilewati: nama kosong");
        }
        if (!isGmail(email)) {
            return new Result(row, name, SKIPPED, "Dilewati: email bukan Gmail");
        }
        if (kelas == null) {
            return new Result(row, name, SKIPPED, "Dilewati: kelas tidak dikenali");
        }
        StudentRecord existing = DataStore.STUDENT_ACCOUNTS.get(email);
        if (existing != null) {
            existing.name = name;
            existing.studentClass = kelas;
            existing.email = email;
            if (!address.isEmpty()) {
                existing.address = address;
            }
            if (!phone.isEmpty()) {
                existing.phone = phone;
            }
            DataStore.updateStudent(existing);
            return new Result(row, name, UPDATED, "Diperbarui");
        }
        if (DataStore.isUsernameTaken(email)) {
            return new Result(row, name, SKIPPED, "Dilewati: username dipakai akun lain");
        }
        StudentRecord s = new StudentRecord(name, kelas, address, email, phone, email,
            password == null ? DEFAULT_PASSWORD : password);
        DataStore.addStudent(s);
        return new Result(row, name, NEW, "Ditambahkan");
    }

    private Result syncTeacher(int row, String name, String email, String subjectsText, String password) {
        if (name.isEmpty()) {
            return new Result(row, name, SKIPPED, "Dilewati: nama kosong");
        }
        if (!isGmail(email)) {
            return new Result(row, name, SKIPPED, "Dilewati: email bukan Gmail");
        }
        TeacherRecord existing = DataStore.TEACHERS.get(email);
        if (existing != null) {
            existing.name = name;
            if (subjectsText != null) {
                existing.subjects.clear();
                existing.subjects.addAll(DataStore.parseSubjects(subjectsText));
            }
            DataStore.updateTeacher(existing);
            return new Result(row, name, UPDATED, "Diperbarui");
        }
        if (DataStore.isUsernameTaken(email)) {
            return new Result(row, name, SKIPPED, "Dilewati: username dipakai akun lain");
        }
        TeacherRecord t = new TeacherRecord(name, email, password == null ? DEFAULT_PASSWORD : password);
        if (subjectsText != null) {
            t.subjects.addAll(DataStore.parseSubjects(subjectsText));
        }
        DataStore.addTeacher(t);
        return new Result(row, name, NEW, "Ditambahkan");
    }

    // ===================== BANTUAN =====================
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

    private static String get(String[] row, int index) {
        return index >= 0 && index < row.length && row[index] != null ? row[index].trim() : "";
    }

    /** "10", "X RPL 1", "XI TKJ", "12.0" menjadi "10", "11", atau "12"; null bila tidak dikenali. */
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
}
