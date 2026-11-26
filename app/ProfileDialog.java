package app;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import app.Components.*;
import static app.Theme.f;
import static app.Theme.fb;
import static app.Ui.*;

/** Profil pengguna yang sedang login: lihat dan ubah foto; operator juga bisa mengisi namanya. */
final class ProfileDialog {
    private ProfileDialog() {
    }

    static void show(AppShell app) {
        String role = app.role();
        String username = app.username();
        String name = DataStore.displayName(role, username);
        BufferedImage[] photo = {DataStore.photoOf(role, username)};
        boolean[] saved = {false};

        JDialog dialog = new JDialog(app.frame(), Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        DialogBody body = new DialogBody(new BorderLayout(0, 16), Theme.ACCENT);
        body.setBorder(BorderFactory.createEmptyBorder(30, 28, 20, 28));

        // ---- kepala: foto besar + nama
        AvatarIcon big = new AvatarIcon(initials(name), 84, avatarColor(name), new Color(15, 23, 42), photo[0]);
        JLabel bigLabel = new JLabel(big);
        JPanel headText = p(new GridLayout(0, 1, 0, 2));
        headText.add(label(name, fb(18), "title"));
        headText.add(label(role + "  \u00B7  " + username, f(12), "muted"));
        JPanel header = p(new BorderLayout(18, 0));
        header.add(bigLabel, BorderLayout.WEST);
        header.add(headText, BorderLayout.CENTER);

        // ---- tombol foto
        JLabel fileName = label(photo[0] == null ? "Belum ada file dipilih" : "Foto tersimpan", f(12), "muted");
        fileName.setPreferredSize(new Dimension(150, 20));
        AppButton choose = new AppButton("Pilih File", false);
        choose.addActionListener(e -> {
            BufferedImage img = choosePhoto(dialog.getRootPane());
            if (img != null) {
                photo[0] = img;
                big.photo = img;
                bigLabel.repaint();
                fileName.setText(lastPhotoName());
            }
        });
        AppButton remove = new AppButton("Hapus Foto", false);
        remove.addActionListener(e -> {
            photo[0] = null;
            big.photo = null;
            bigLabel.repaint();
            fileName.setText("Belum ada file dipilih");
        });
        JPanel photoButtons = p(new FlowLayout(FlowLayout.LEFT, 10, 0));
        photoButtons.add(choose);
        photoButtons.add(fileName);
        photoButtons.add(remove);

        // ---- isi sesuai peran
        JTextField operatorName = new JTextField(name);
        JComponent info;
        if ("Operator".equals(role)) {
            info = detailRows(
                new String[]{"Nama operator", "Username"},
                new JComponent[]{operatorName, valueLabel(username, 250)});
        } else if ("Guru".equals(role)) {
            TeacherRecord t = DataStore.TEACHERS.get(username);
            info = detailRows(
                new String[]{"Username", "Mata pelajaran"},
                new JComponent[]{valueLabel(username, 250),
                    valueLabel(t == null ? "Belum diatur operator" : t.subjectsText(), 250)});
        } else {
            StudentRecord s = DataStore.STUDENT_ACCOUNTS.get(username);
            info = s != null ? studentInfoRows(s, 250) : p(new GridLayout());
        }

        JPanel center = p(new BorderLayout(0, 14));
        center.add(photoButtons, BorderLayout.NORTH);
        center.add(info, BorderLayout.CENTER);
        JScrollPane scroll = new JScrollPane(wrapNorth(center));
        scroll.putClientProperty("role", "clear");
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        Dimension d = center.getPreferredSize();
        scroll.setPreferredSize(new Dimension(Math.max(d.width, 400) + 20, Math.min(d.height + 4, 320)));

        // ---- tombol bawah
        AppButton close = new AppButton("Tutup", false);
        close.addActionListener(e -> dialog.dispose());
        AppButton save = new AppButton("Simpan Profil", true);
        save.addActionListener(e -> {
            if ("Operator".equals(role)) {
                String n = operatorName.getText().trim();
                if (n.isEmpty()) {
                    AppDialog.show(dialog.getRootPane(), AppDialog.WARN, "Data belum lengkap", "Nama operator wajib diisi.");
                    return;
                }
                DataStore.operatorName = n;
                DataStore.operatorPhoto = photo[0];
                DataStore.saveOperator();
            } else if ("Guru".equals(role)) {
                TeacherRecord t = DataStore.TEACHERS.get(username);
                if (t != null) {
                    t.photo = photo[0];
                    DataStore.updateTeacher(t);
                }
            } else {
                StudentRecord s = DataStore.STUDENT_ACCOUNTS.get(username);
                if (s != null) {
                    s.photo = photo[0];
                    DataStore.updateStudent(s);
                }
            }
            saved[0] = true;
            dialog.dispose();
        });
        JPanel buttons = p(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.add(close);
        buttons.add(save);

        body.add(header, BorderLayout.NORTH);
        body.add(scroll, BorderLayout.CENTER);
        body.add(buttons, BorderLayout.SOUTH);
        Theme.apply(body);
        makeDraggable(dialog, body);

        dialog.setContentPane(body);
        dialog.getRootPane().setDefaultButton(save);
        dialog.getRootPane().registerKeyboardAction(
            e -> dialog.dispose(),
            KeyStroke.getKeyStroke("ESCAPE"),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        dialog.pack();
        try {
            dialog.setShape(new java.awt.geom.RoundRectangle2D.Double(
                0, 0, dialog.getWidth(), dialog.getHeight(), 24, 24));
        } catch (Exception ignored) {
            // platform tidak mendukung bentuk jendela
        }
        dialog.setLocationRelativeTo(app.frame());
        AppDialog.animateIn(dialog);
        dialog.setVisible(true);

        if (saved[0]) {
            app.refreshUser();
            AppDialog.show(app.frame(), AppDialog.SUCCESS, "Berhasil", "Profil berhasil diperbarui.");
        }
    }
}
