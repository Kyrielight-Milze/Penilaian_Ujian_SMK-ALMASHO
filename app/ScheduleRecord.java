package app;

/** Satu baris jadwal pelajaran yang diatur operator: hari, jam, kelas, mata pelajaran, dan guru pengampu. */
final class ScheduleRecord {
    /** Nomor baris di database (0 bila belum tersimpan). */
    long id;
    String day;
    int startMinute;
    int endMinute;
    String studentClass;
    String subject;
    /** Username guru pengampu; null/kosong bila belum ditentukan. */
    String teacherUsername;

    ScheduleRecord(String day, int startMinute, int endMinute, String studentClass,
                   String subject, String teacherUsername) {
        this.day = day;
        this.startMinute = startMinute;
        this.endMinute = endMinute;
        this.studentClass = studentClass;
        this.subject = subject;
        this.teacherUsername = teacherUsername;
    }

    static String time(int minute) {
        return String.format("%02d:%02d", minute / 60, minute % 60);
    }

    String timeRange() {
        return time(startMinute) + "\u2013" + time(endMinute);
    }

    String teacherName() {
        TeacherRecord t = teacherUsername == null ? null : DataStore.TEACHERS.get(teacherUsername);
        if (t == null) {
            return "Belum ditentukan";
        }
        return t.name.isEmpty() ? t.username : t.name;
    }
}
