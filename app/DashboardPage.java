package app;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

import app.Components.*;
import static app.Theme.f;
import static app.Theme.fb;
import static app.Theme.fi;
import static app.Ui.*;

/** Ringkasan data SINA. Isi kartu statistik menyesuaikan peran pengguna. */
final class DashboardPage extends Page {
    private final JLabel[] statCaption = new JLabel[3];
    private final JLabel[] statValue = new JLabel[3];
    private JLabel heroDescription;

    DashboardPage(AppShell app) {
        super(app);
    }

    @Override
    void onShow() {
        refreshDashboard();
    }

    @Override
    JPanel view() {
        Pill pill = new Pill("SELAMAT DATANG");
        pill.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel heroTitle = label("SINA — Sistem Informasi Nilai & Akademik", fb(26), "title");
        JLabel heroTagline = label("Berotak Teknologi, Berhati Qur'ani", fi(16), "gold");
        heroDescription = label(" ", f(13), "muted");

        RoundPanel hero = card(null, 28, 32, true);
        hero.setLayout(new BoxLayout(hero, BoxLayout.Y_AXIS));
        hero.add(pill);
        hero.add(Box.createVerticalStrut(14));
        hero.add(heroTitle);
        hero.add(Box.createVerticalStrut(4));
        hero.add(heroTagline);
        hero.add(Box.createVerticalStrut(12));
        hero.add(heroDescription);
        hero.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel stats = p(new GridLayout(1, 3, 16, 0));
        for (int i = 0; i < 3; i++) {
            statCaption[i] = label(" ", f(12), "muted");
            statValue[i] = label("0", fb(30), "accent");
            RoundPanel tile = card(new BorderLayout(0, 6), 20, 20, false);
            tile.add(statCaption[i], BorderLayout.NORTH);
            tile.add(statValue[i], BorderLayout.CENTER);
            stats.add(tile);
        }
        stats.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel box = p(null);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.add(hero);
        box.add(Box.createVerticalStrut(16));
        box.add(stats);

        JPanel body = p(new BorderLayout());
        body.add(box, BorderLayout.NORTH);
        return pageShell("Dashboard", "Ringkasan data SINA (Sistem Informasi Nilai & Akademik)", body);
    }

    private void refreshDashboard() {
        int gradeCount = 0;
        Set<String> subjects = new HashSet<>();
        for (StudentRecord s : DataStore.STUDENTS) {
            gradeCount += s.grades.size();
            for (GradeRecord g : s.grades) {
                subjects.add(g.subject.toLowerCase());
            }
        }

        if ("Operator".equals(app.role())) {
            heroDescription.setText("Anda masuk sebagai Operator. Kelola guru, siswa, jadwal, dan sinkronisasi data induk lewat menu di samping.");
            setStat(0, "Siswa terdaftar", String.valueOf(DataStore.STUDENTS.size()));
            setStat(1, "Guru terdaftar", String.valueOf(DataStore.TEACHERS.size()));
            setStat(2, "Nilai UTS terinput", String.valueOf(gradeCount));
        } else if ("Guru".equals(app.role())) {
            TeacherRecord teacher = DataStore.TEACHERS.get(app.username());
            int mine = 0;
            int mySubjects = 0;
            if (teacher != null) {
                mySubjects = teacher.subjects.size();
                for (StudentRecord s : DataStore.STUDENTS) {
                    for (String subject : teacher.subjects) {
                        if (s.gradeFor(subject) != null) {
                            mine++;
                        }
                    }
                }
            }
            heroDescription.setText(mySubjects == 0
                ? "Anda masuk sebagai Guru. Mata pelajaran Anda belum diatur operator, hubungi operator terlebih dahulu."
                : "Anda masuk sebagai Guru. Pilih mata pelajaran dan input nilai UTS siswa lewat menu di samping.");
            setStat(0, "Siswa terdaftar", String.valueOf(DataStore.STUDENTS.size()));
            setStat(1, "Mata pelajaran Anda", String.valueOf(mySubjects));
            setStat(2, "Nilai UTS terinput", String.valueOf(mine));
        } else {
            StudentRecord me = DataStore.STUDENT_ACCOUNTS.get(app.username());
            heroDescription.setText("Anda masuk sebagai Siswa. Lihat biodata dan nilai UTS lewat Data Saya, serta jadwal pelajaran lewat menu Jadwal Pelajaran.");
            if (me != null) {
                double sum = 0;
                for (GradeRecord g : me.grades) {
                    sum += g.score;
                }
                setStat(0, "Kelas", me.studentClass);
                setStat(1, "Nilai UTS", String.valueOf(me.grades.size()));
                setStat(2, "Rata-rata nilai",
                    me.grades.isEmpty() ? "-" : String.format("%.1f", sum / me.grades.size()));
            }
        }
    }

    private void setStat(int index, String caption, String value) {
        statCaption[index].setText(caption);
        statValue[index].setText(value);
    }
}