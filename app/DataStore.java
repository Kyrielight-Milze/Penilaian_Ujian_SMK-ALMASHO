package app;

import java.awt.image.BufferedImage;
import javax.swing.JOptionPane;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Pusat data aplikasi. Data dimuat dari database (Database.java) saat aplikasi dibuka dan disimpan
 * kembali setiap kali ada perubahan, jadi tetap ada walau aplikasi ditutup.
 * Halaman cukup memanggil add/update/remove di sini setelah mengubah data.
 */
final class DataStore {
    private DataStore() {
    }

    private static boolean loadErrorShown;

    /** Membuka database dan memuat semua data. Mengembalikan false bila gagal (pesan sudah ditampilkan). */
    static boolean init() {
        try {
            Database.open();
            applySnapshot(Database.loadSnapshot());
            return true;
        } catch (SQLException e) {
            String detail = String.valueOf(e.getMessage());
            DbConfig cfg = Database.config();
            String where = cfg == null ? "database" : cfg.describe();
            String text;
            if (detail.contains("No suitable driver")) {
                text = "Driver database tidak ditemukan.\nPastikan folder lib/ (berisi file .jar) ada di folder project, "
                    + "lalu jalankan ulang.";
            } else if (cfg != null && cfg.mysql) {
                text = "Tidak dapat tersambung ke " + where + ".\n\nPeriksa:\n"
                    + "- MySQL/XAMPP sudah dinyalakan\n"
                    + "- host, port, user, dan password di file database.properties sudah benar\n"
                    + "- komputer ini bisa menjangkau server (firewall port 3306)\n\nRincian: " + detail;
            } else {
                text = "Database tidak dapat dibuka:\n" + detail
                    + "\n\nBila aplikasi masih terbuka di jendela lain, tutup dulu lalu coba lagi.";
            }
            JOptionPane.showMessageDialog(null, text, "Database", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * Memuat ulang data dari database supaya perubahan dari komputer lain ikut terlihat.
     * Objek yang sudah ada diperbarui di tempat, bukan diganti. Bila gagal, data lama tetap dipakai.
     */
    static boolean reload() {
        try {
            applySnapshot(Database.loadSnapshot());
            loadErrorShown = false;
            return true;
        } catch (SQLException e) {
            if (!loadErrorShown) {
                loadErrorShown = true;
                JOptionPane.showMessageDialog(null,
                    "Data terbaru tidak dapat dimuat dari server, jadi yang tampil mungkin belum yang terbaru.\n"
                        + "Periksa sambungan ke database.\n\nRincian: " + e.getMessage(),
                    "Database", JOptionPane.WARNING_MESSAGE);
            }
            return false;
        }
    }

    private static void applySnapshot(Database.Snapshot snap) throws SQLException {
        if (snap.operatorName != null && !snap.operatorName.trim().isEmpty()) {
            operatorName = snap.operatorName;
        }
        operatorPhoto = snap.operatorPhoto;

        // ----- guru -----
        for (TeacherRecord f : snap.teachers.values()) {
            TeacherRecord cur = TEACHERS.get(f.username);
            boolean photoChanged;
            if (cur == null) {
                cur = f;
                TEACHERS.put(f.username, cur);
                photoChanged = f.photoStamp != 0;
            } else {
                cur.name = f.name;
                cur.password = f.password;
                cur.subjects.clear();
                cur.subjects.addAll(f.subjects);
                photoChanged = cur.photoStamp != f.photoStamp;
                cur.photoStamp = f.photoStamp;
            }
            if (photoChanged) {
                cur.photo = f.photoStamp == 0 ? null : Database.loadTeacherPhoto(cur.username);
            }
        }
        TEACHERS.keySet().retainAll(snap.teachers.keySet());

        // ----- siswa -----
        List<StudentRecord> ordered = new ArrayList<>();
        Set<String> seen = new java.util.HashSet<>();
        for (StudentRecord f : snap.students) {
            StudentRecord cur = STUDENT_ACCOUNTS.get(f.username);
            boolean photoChanged;
            if (cur == null) {
                cur = f;
                STUDENT_ACCOUNTS.put(f.username, cur);
                photoChanged = f.photoStamp != 0;
            } else {
                cur.name = f.name;
                cur.studentClass = f.studentClass;
                cur.address = f.address;
                cur.email = f.email;
                cur.phone = f.phone;
                cur.password = f.password;
                cur.grades.clear();
                cur.grades.addAll(f.grades);
                photoChanged = cur.photoStamp != f.photoStamp;
                cur.photoStamp = f.photoStamp;
            }
            if (photoChanged) {
                cur.photo = f.photoStamp == 0 ? null : Database.loadStudentPhoto(cur.username);
            }
            ordered.add(cur);
            seen.add(cur.username);
        }
        STUDENT_ACCOUNTS.keySet().retainAll(seen);
        STUDENTS.clear();
        STUDENTS.addAll(ordered);

        // ----- jadwal (dicocokkan lewat id) -----
        Map<Long, ScheduleRecord> existing = new LinkedHashMap<>();
        for (ScheduleRecord r : SCHEDULE) {
            if (r.id > 0) {
                existing.put(r.id, r);
            }
        }
        List<ScheduleRecord> merged = new ArrayList<>();
        for (ScheduleRecord f : snap.schedule) {
            ScheduleRecord cur = existing.get(f.id);
            if (cur == null) {
                merged.add(f);
            } else {
                cur.day = f.day;
                cur.startMinute = f.startMinute;
                cur.endMinute = f.endMinute;
                cur.studentClass = f.studentClass;
                cur.subject = f.subject;
                cur.teacherUsername = f.teacherUsername;
                merged.add(cur);
            }
        }
        SCHEDULE.clear();
        SCHEDULE.addAll(merged);
    }

    /** Menjalankan satu operasi database; kegagalan ditampilkan sebagai pesan, tidak menutup aplikasi. */
    private interface Op {
        void run() throws SQLException;
    }

    private static void db(Op op) {
        try {
            op.run();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Data gagal disimpan ke database:\n" + e.getMessage(),
                "Database", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===================== SIMPAN PERUBAHAN =====================
    static void saveOperator() {
        db(() -> Database.saveOperator(operatorName, operatorPhoto));
    }

    static void addTeacher(TeacherRecord t) {
        TEACHERS.put(t.username, t);
        db(() -> Database.saveTeacher(t));
    }

    /** Simpan perubahan guru yang datanya sudah diubah langsung pada objeknya. */
    static void updateTeacher(TeacherRecord t) {
        db(() -> Database.saveTeacher(t));
    }

    static void removeTeacher(TeacherRecord t) {
        TEACHERS.remove(t.username);
        for (ScheduleRecord r : SCHEDULE) {
            if (t.username.equals(r.teacherUsername)) {
                r.teacherUsername = null;
            }
        }
        db(() -> Database.deleteTeacher(t.username));
    }

    static void addStudent(StudentRecord s) {
        STUDENTS.add(s);
        STUDENT_ACCOUNTS.put(s.username, s);
        db(() -> Database.saveStudent(s));
    }

    static void updateStudent(StudentRecord s) {
        db(() -> Database.saveStudent(s));
    }

    static void removeStudent(StudentRecord s) {
        STUDENT_ACCOUNTS.remove(s.username);
        STUDENTS.remove(s);
        db(() -> Database.deleteStudent(s.username));
    }

    /** Menyimpan nilai. Mengembalikan true bila nilai lama diperbarui, false bila nilai baru. */
    static boolean saveGrade(StudentRecord s, String subject, double score) {
        boolean updated = s.setGrade(subject, score);
        db(() -> Database.saveGrade(s.username, subject, score));
        return updated;
    }

    static void addSchedule(ScheduleRecord r) {
        SCHEDULE.add(r);
        db(() -> r.id = Database.saveSchedule(r));
    }

    static void updateSchedule(ScheduleRecord r) {
        db(() -> Database.saveSchedule(r));
    }

    static void removeSchedule(ScheduleRecord r) {
        SCHEDULE.remove(r);
        if (r.id > 0) {
            db(() -> Database.deleteSchedule(r.id));
        }
    }

    // ===================== TURUNAN DATA =====================
    /** Kelas yang diajar seorang guru menurut jadwal; kosong bila guru belum punya jadwal. */
    static Set<String> classesTaught(TeacherRecord t) {
        Set<String> out = new TreeSet<>();
        for (ScheduleRecord r : SCHEDULE) {
            if (t.username.equals(r.teacherUsername)) {
                out.add(r.studentClass);
            }
        }
        return out;
    }

    /** Siswa yang dinilai guru: siswa di kelas yang ada di jadwalnya; semua siswa bila belum ada jadwal. */
    static List<StudentRecord> studentsOf(TeacherRecord t) {
        Set<String> classes = classesTaught(t);
        List<StudentRecord> out = new ArrayList<>();
        for (StudentRecord s : STUDENTS) {
            if (classes.isEmpty() || classes.contains(s.studentClass)) {
                out.add(s);
            }
        }
        return out;
    }

    /** Status nilai seorang siswa untuk daftar mapel: 0 belum diisi, 1 belum lengkap, 2 sudah terisi. */
    static int gradeStatus(StudentRecord s, List<String> subjects) {
        int filled = 0;
        for (String subject : subjects) {
            if (s.gradeFor(subject) != null) {
                filled++;
            }
        }
        if (filled == 0) {
            return 0;
        }
        return filled == subjects.size() ? 2 : 1;
    }

    static final String OPERATOR_USERNAME = "ftonyastro@gmail.com";
    static final String OPERATOR_PASSWORD = "fatoniyasashii664";

    /** Nama dan foto operator (bisa diubah operator lewat menu Profil). */
    static String operatorName = "Operator";
    static BufferedImage operatorPhoto;

    static final List<StudentRecord> STUDENTS = new ArrayList<>();
    static final Map<String, StudentRecord> STUDENT_ACCOUNTS = new LinkedHashMap<>();
    static final Map<String, TeacherRecord> TEACHERS = new LinkedHashMap<>();
    /** Jadwal pelajaran yang diatur operator (menu Jadwal Pelajaran). */
    static final List<ScheduleRecord> SCHEDULE = new ArrayList<>();

    /** Daftar mata pelajaran unik dari jadwal, urut abjad. Dipakai sebagai saran cepat di Akun Guru. */
    static List<String> scheduleSubjects() {
        List<String> out = new ArrayList<>();
        for (ScheduleRecord r : SCHEDULE) {
            boolean exists = false;
            for (String s : out) {
                if (s.equalsIgnoreCase(r.subject)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                out.add(r.subject);
            }
        }
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    static boolean isUsernameTaken(String username) {
        return TEACHERS.containsKey(username)
            || STUDENT_ACCOUNTS.containsKey(username)
            || OPERATOR_USERNAME.equals(username);
    }

    /** Mengembalikan "Operator", "Guru", atau "Siswa"; null jika akun/kata sandi salah. */
    static String authenticate(String username, String password) {
        if (OPERATOR_USERNAME.equals(username) && OPERATOR_PASSWORD.equals(password)) {
            return "Operator";
        }
        TeacherRecord teacher = TEACHERS.get(username);
        if (teacher != null && password.equals(teacher.password)) {
            return "Guru";
        }
        StudentRecord student = STUDENT_ACCOUNTS.get(username);
        if (student != null && password.equals(student.password)) {
            return "Siswa";
        }
        return null;
    }

    /** Nama yang ditampilkan untuk pengguna yang sedang login. */
    static String displayName(String role, String username) {
        if ("Operator".equals(role)) {
            return operatorName;
        }
        if ("Guru".equals(role)) {
            TeacherRecord t = TEACHERS.get(username);
            if (t != null && !t.name.isEmpty()) {
                return t.name;
            }
        } else if ("Siswa".equals(role)) {
            StudentRecord s = STUDENT_ACCOUNTS.get(username);
            if (s != null) {
                return s.name;
            }
        }
        int at = username.indexOf('@');
        return at > 0 ? username.substring(0, at) : username;
    }

    static BufferedImage photoOf(String role, String username) {
        if ("Operator".equals(role)) {
            return operatorPhoto;
        }
        if ("Guru".equals(role)) {
            TeacherRecord t = TEACHERS.get(username);
            return t == null ? null : t.photo;
        }
        StudentRecord s = STUDENT_ACCOUNTS.get(username);
        return s == null ? null : s.photo;
    }

    /** Memecah "Matematika, Fisika" menjadi daftar mapel: dirapikan, huruf awal kapital, tanpa duplikat. */
    static List<String> parseSubjects(String text) {
        List<String> out = new ArrayList<>();
        for (String part : text.split("[,;\\n]")) {
            String s = part.trim().replaceAll("\\s+", " ");
            if (s.isEmpty()) {
                continue;
            }
            StringBuilder sb = new StringBuilder();
            for (String word : s.split(" ")) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
            String subject = sb.toString();
            boolean duplicate = false;
            for (String existing : out) {
                if (existing.equalsIgnoreCase(subject)) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                out.add(subject);
            }
        }
        return out;
    }
}
