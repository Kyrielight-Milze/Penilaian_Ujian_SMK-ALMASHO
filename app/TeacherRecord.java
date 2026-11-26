package app;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/** Data satu guru: akun login, nama, mata pelajaran yang diampu (diatur operator), dan foto profil. */
final class TeacherRecord {
    String name;
    final String username;
    String password;
    final List<String> subjects = new ArrayList<>();
    BufferedImage photo;
    /** Penanda versi foto di database; dipakai untuk memuat ulang foto hanya bila berubah. */
    long photoStamp;

    TeacherRecord(String name, String username, String password) {
        this.name = name;
        this.username = username;
        this.password = password;
    }

    String subjectsText() {
        return subjects.isEmpty() ? "Belum diatur operator" : String.join(", ", subjects);
    }
}
