package app;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Titik masuk aplikasi. Jalankan kelas ini. */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {
                // pakai tampilan bawaan
            }
            Theme.initFonts();
            if (!DataStore.init()) {
                System.exit(1);
                return;
            }
            new AppShell().start();
        });
    }
}
