package app;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/** Data satu siswa beserta akun login, foto profil, dan daftar nilainya. */
final class StudentRecord {
    String name;
    String studentClass;
    String address;
    String email;
    String phone;
    final String username;
    String password;
    BufferedImage photo;
    /** Penanda versi foto di database; dipakai untuk memuat ulang foto hanya bila berubah. */
    long photoStamp;
    final List<GradeRecord> grades = new ArrayList<>();

    StudentRecord(String name, String studentClass, String address, String email,
                  String phone, String username, String password) {
        this.name = name;
        this.studentClass = studentClass;
        this.address = address;
        this.email = email;
        this.phone = phone;
        this.username = username;
        this.password = password;
    }

    /** Nilai untuk satu mata pelajaran (tanpa membedakan huruf besar/kecil); null bila belum ada. */
    GradeRecord gradeFor(String subject) {
        for (GradeRecord g : grades) {
            if (g.subject.equalsIgnoreCase(subject)) {
                return g;
            }
        }
        return null;
    }

    /** Menyimpan nilai. Mengembalikan true bila nilai lama diperbarui, false bila nilai baru ditambahkan. */
    boolean setGrade(String subject, double score) {
        for (int i = 0; i < grades.size(); i++) {
            if (grades.get(i).subject.equalsIgnoreCase(subject)) {
                grades.set(i, new GradeRecord(grades.get(i).subject, score));
                return true;
            }
        }
        grades.add(new GradeRecord(subject, score));
        return false;
    }
}
