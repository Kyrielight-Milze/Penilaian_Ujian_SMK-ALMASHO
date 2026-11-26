package app;

import javax.swing.*;
import java.awt.*;

import app.Components.*;
import static app.Ui.*;

/** Dialog detail seseorang (siswa/guru) dengan tombol Hapus, Tutup, dan Ubah Data. */
final class PersonDialog {
    static final int CLOSE = 0;
    static final int EDIT = 1;
    static final int DELETE = 2;

    private PersonDialog() {
    }

    /** Mengembalikan CLOSE, EDIT, atau DELETE sesuai tombol yang dipilih. */
    static int show(Window owner, JPanel header, JComponent info) {
        JDialog dialog = new JDialog(owner, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        int[] action = {CLOSE};

        DialogBody body = new DialogBody(new BorderLayout(0, 16), Theme.ACCENT);
        body.setBorder(BorderFactory.createEmptyBorder(30, 28, 20, 28));

        JScrollPane scroll = new JScrollPane(wrapNorth(info));
        scroll.putClientProperty("role", "clear");
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        Dimension d = info.getPreferredSize();
        scroll.setPreferredSize(new Dimension(Math.max(d.width, 360) + 20, Math.min(d.height + 4, 300)));

        AppButton edit = new AppButton("Ubah Data", true);
        edit.addActionListener(e -> {
            action[0] = EDIT;
            dialog.dispose();
        });
        AppButton close = new AppButton("Tutup", false);
        close.addActionListener(e -> dialog.dispose());
        AppButton delete = new AppButton("Hapus", false);
        delete.setDanger(true);
        delete.addActionListener(e -> {
            action[0] = DELETE;
            dialog.dispose();
        });

        JPanel right = p(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.add(close);
        right.add(edit);
        JPanel buttons = p(new BorderLayout());
        buttons.add(delete, BorderLayout.WEST);
        buttons.add(right, BorderLayout.EAST);

        body.add(header, BorderLayout.NORTH);
        body.add(scroll, BorderLayout.CENTER);
        body.add(buttons, BorderLayout.SOUTH);
        Theme.apply(body);
        makeDraggable(dialog, body);

        dialog.setContentPane(body);
        dialog.getRootPane().setDefaultButton(edit);
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
        dialog.setLocationRelativeTo(owner);
        AppDialog.animateIn(dialog);
        dialog.setVisible(true);
        return action[0];
    }
}
