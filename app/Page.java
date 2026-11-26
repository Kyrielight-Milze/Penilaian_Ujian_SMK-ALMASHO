package app;

import javax.swing.*;

/** Dasar semua halaman. Halaman baru cukup mewarisi kelas ini lalu didaftarkan di AppShell. */
abstract class Page {
    protected final AppShell app;

    Page(AppShell app) {
        this.app = app;
    }

    /** Membangun tampilan halaman (dipanggil sekali). */
    abstract JPanel view();

    /** Dipanggil setiap kali halaman dibuka (muat ulang data di sini). */
    void onShow() {
    }

    /** Dipanggil saat logout (kosongkan form di sini). */
    void reset() {
    }

    protected void warn(String message, String title) {
        AppDialog.show(app.frame(), AppDialog.WARN, title, message);
    }

    protected void error(String message, String title) {
        AppDialog.show(app.frame(), AppDialog.ERROR, title, message);
    }

    protected void success(String message) {
        AppDialog.show(app.frame(), AppDialog.SUCCESS, "Berhasil", message);
    }
}
