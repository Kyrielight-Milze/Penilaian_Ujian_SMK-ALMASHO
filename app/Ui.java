package app;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Calendar;
import java.util.regex.Pattern;

import app.Components.*;
import static app.Theme.f;
import static app.Theme.fb;

/** Fungsi bantu pembuat tampilan yang dipakai banyak halaman. */
final class Ui {
    private Ui() {
    }

    // ===================== HELPER UI =====================
    static JPanel p(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    static JLabel label(String text, Font font, String role) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.putClientProperty("role", role);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    static RoundPanel card(LayoutManager layout, int arc, int pad, boolean gradient) {
        RoundPanel panel = new RoundPanel(arc, gradient);
        if (layout != null) {
            panel.setLayout(layout);
        }
        panel.setBorder(BorderFactory.createEmptyBorder(pad, pad, pad, pad));
        return panel;
    }

    /** Panel isi scroll yang lebarnya mengikuti jendela, jadi formulir tidak pernah terpotong di kanan. */
    private static final class TrackingBody extends JPanel implements Scrollable {
        TrackingBody() {
            super(new BorderLayout());
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 18;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(visible.height - 40, 40);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /** Scroll tanpa bingkai untuk formulir: isi menyesuaikan lebar, bergulir ke bawah bila terlalu tinggi. */
    static JScrollPane formScroll(JComponent content) {
        TrackingBody body = new TrackingBody();
        body.add(content, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(body);
        sp.putClientProperty("role", "clear");
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return sp;
    }

    static JPanel wrapNorth(JComponent component) {
        JPanel wrap = p(new BorderLayout());
        wrap.add(component, BorderLayout.NORTH);
        return wrap;
    }

    static JPanel labeled(String text, JComponent field) {
        JPanel panel = p(new BorderLayout(0, 6));
        panel.add(label(text, f(12), "muted"), BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    static void addRow(JPanel panel, int row, String text, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 0, 5, 16);
        panel.add(label(text, f(13), "muted"), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 0, 5, 0);
        panel.add(field, c);
    }

    static JPanel pageShell(String title, String subtitle, JComponent body) {
        JPanel head = p(new GridLayout(0, 1, 0, 2));
        head.add(label(title, fb(22), "title"));
        head.add(label(subtitle, f(13), "muted"));
        JPanel page = p(new BorderLayout(0, 18));
        page.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        page.add(head, BorderLayout.NORTH);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    static Icon logoIcon(int size) {
        ImageIcon icon = new ImageIcon("logo_SMK_ALMASHO.png");
        if (icon.getIconWidth() > 0) {
            return new ImageIcon(icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH));
        }
        return new AvatarIcon("SM", size, Theme.ACCENT, Color.WHITE);
    }

    /** Bagian hari: 0 pagi, 1 siang, 2 sore, 3 malam (termasuk dini hari). */
    static int daypart() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 4) return 3;
        if (hour < 11) return 0;
        if (hour < 15) return 1;
        if (hour < 18) return 2;
        return 3;
    }

    static String greeting() {
        switch (daypart()) {
            case 0: return "Selamat pagi";
            case 1: return "Selamat siang";
            case 2: return "Selamat sore";
            default: return "Selamat malam";
        }
    }

    /** Teks petunjuk saat mengubah data yang usernamenya tidak boleh diganti. */
    static final String EDIT_HINT = "Username tidak bisa diubah. Isi password hanya bila ingin menggantinya.";

    private static final Pattern GMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@gmail\\.com$");

    static boolean isGmail(String text) {
        return GMAIL.matcher(text).matches();
    }

    static String initials(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    static JPanel createPasswordControl(JPasswordField field) {
        JPanel control = p(new BorderLayout(4, 0));
        JToggleButton toggle = new JToggleButton(new EyeIcon());
        toggle.setToolTipText("Tampilkan atau sembunyikan sandi");
        toggle.setMargin(new Insets(2, 6, 2, 6));
        toggle.setContentAreaFilled(false);
        toggle.setBorderPainted(false);
        toggle.setFocusPainted(false);
        toggle.setOpaque(false);
        toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        char echo = field.getEchoChar();
        toggle.addActionListener(e -> field.setEchoChar(toggle.isSelected() ? (char) 0 : echo));
        control.add(field, BorderLayout.CENTER);
        control.add(toggle, BorderLayout.EAST);
        return control;
    }

    static String formatStudentDetails(StudentRecord s) {
        StringBuilder d = new StringBuilder()
            .append("Nama: ").append(s.name)
            .append("\nKelas: ").append(s.studentClass)
            .append("\nAlamat: ").append(s.address)
            .append("\nEmail: ").append(s.email)
            .append("\nNomor Telepon: ").append(s.phone)
            .append("\n\nNilai UTS:");
        if (s.grades.isEmpty()) {
            d.append("\nBelum ada nilai UTS.");
        } else {
            for (GradeRecord g : s.grades) {
                d.append("\n").append(g.subject).append(": ").append(g.score);
            }
        }
        return d.toString();
    }

    static void makeDraggable(JDialog dialog, JComponent handle) {
        Point[] origin = {null};
        MouseAdapter drag = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                origin[0] = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (origin[0] != null) {
                    Point p = dialog.getLocation();
                    dialog.setLocation(p.x + e.getX() - origin[0].x, p.y + e.getY() - origin[0].y);
                }
            }
        };
        handle.addMouseListener(drag);
        handle.addMouseMotionListener(drag);
    }

    static String esc(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static String formatScore(double score) {
        return score == Math.rint(score) ? String.valueOf((int) score) : String.format("%.1f", score);
    }

    static Color avatarColor(String name) {
        Color[] palette = {
            new Color(147, 197, 253), new Color(134, 239, 172), new Color(253, 224, 71),
            new Color(252, 165, 165), new Color(196, 181, 253), new Color(94, 234, 212)
        };
        return palette[Math.abs(name.hashCode()) % palette.length];
    }

    static JPanel personHeader(String name, String sub, BufferedImage photo, int size) {
        JPanel text = p(new GridLayout(0, 1, 0, 2));
        text.add(label(name, fb(18), "title"));
        text.add(label(sub, f(12), "muted"));
        JPanel header = p(new BorderLayout(16, 0));
        header.add(new JLabel(new AvatarIcon(initials(name), size, avatarColor(name), new Color(15, 23, 42), photo)),
            BorderLayout.WEST);
        header.add(text, BorderLayout.CENTER);
        return header;
    }

    static JPanel studentHeader(StudentRecord s, int size) {
        return personHeader(s.name, "Kelas " + s.studentClass + "  \u00B7  " + s.username, s.photo, size);
    }

    static JLabel valueLabel(String text, int width) {
        return label("<html><div style='width:" + width + "px'>" + esc(text) + "</div></html>", f(13), "title");
    }

    /** Baris keterangan: kolom label di kiri, komponen nilai di kanan. */
    static JPanel detailRows(String[] labels, JComponent[] values) {
        JPanel g = p(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        for (int i = 0; i < labels.length; i++) {
            c.gridx = 0;
            c.gridy = i;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(6, 0, 6, 20);
            g.add(label(labels[i], f(12), "muted"), c);
            c.gridx = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(6, 0, 6, 0);
            g.add(values[i], c);
        }
        return g;
    }

    /** Teks rahasia (mis. password) yang bisa ditampilkan lewat ikon mata. */
    static JPanel secretRow(String secret) {
        StringBuilder dots = new StringBuilder();
        for (int i = 0; i < Math.min(Math.max(secret.length(), 6), 14); i++) {
            dots.append('\u2022');
        }
        JLabel value = label(dots.toString(), f(13), "title");
        JToggleButton toggle = new JToggleButton(new EyeIcon());
        toggle.setToolTipText("Tampilkan atau sembunyikan password");
        toggle.setMargin(new Insets(0, 4, 0, 4));
        toggle.setContentAreaFilled(false);
        toggle.setBorderPainted(false);
        toggle.setFocusPainted(false);
        toggle.setOpaque(false);
        toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggle.addActionListener(e -> value.setText(toggle.isSelected() ? secret : dots.toString()));
        JPanel row = p(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.add(value);
        row.add(toggle);
        return row;
    }

    // ===================== FOTO PROFIL =====================
    private static String lastPhotoDir;
    private static String lastPhotoName = "";

    /** Nama file foto yang terakhir dipilih lewat dialog (untuk ditampilkan di samping tombol Pilih File). */
    static String lastPhotoName() {
        return lastPhotoName;
    }

    private static boolean isImageName(String name) {
        String n = name.toLowerCase();
        return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png")
            || n.endsWith(".gif") || n.endsWith(".bmp");
    }

    /**
     * Dialog pilih gambar bawaan sistem. Di Windows dipanggil dialog "Open" modern (sama seperti di
     * Chrome/Explorer, ada tampilan thumbnail); di sistem lain atau bila gagal dipakai FileDialog Java.
     * Hasilnya dipotong persegi dari tengah dan diperkecil. Null bila dibatalkan.
     */
    static BufferedImage choosePhoto(Component owner) {
        Window w = owner == null ? null : SwingUtilities.getWindowAncestor(owner);
        if (w == null && owner instanceof Window) {
            w = (Window) owner;
        }
        java.io.File file = null;
        boolean handled = false;
        if (System.getProperty("os.name", "").toLowerCase().contains("win") && SwingUtilities.isEventDispatchThread()) {
            String[] result = pickWithWindowsDialog(w);
            if (result != null) {
                handled = true;
                if (result[0] != null) {
                    file = new java.io.File(result[0]);
                }
            }
        }
        if (!handled) {
            file = pickWithAwtDialog(w);
        }
        if (file == null) {
            return null;
        }
        lastPhotoDir = file.getParent();
        lastPhotoName = file.getName();
        try {
            if (!isImageName(file.getName())) {
                throw new java.io.IOException("bukan gambar");
            }
            BufferedImage src = javax.imageio.ImageIO.read(file);
            if (src == null) {
                throw new java.io.IOException("bukan gambar");
            }
            return squarePhoto(src, 256);
        } catch (Exception | OutOfMemoryError ex) {
            AppDialog.show(owner, AppDialog.ERROR, "Foto tidak valid",
                "File tidak dapat dibaca sebagai gambar. Pilih file JPG atau PNG.");
            return null;
        }
    }

    private static java.io.File pickWithAwtDialog(Window w) {
        FileDialog fd;
        if (w instanceof Dialog) {
            fd = new FileDialog((Dialog) w, "Pilih foto profil", FileDialog.LOAD);
        } else {
            fd = new FileDialog(w instanceof Frame ? (Frame) w : null, "Pilih foto profil", FileDialog.LOAD);
        }
        fd.setFile("*.jpg;*.jpeg;*.png;*.gif;*.bmp");
        fd.setFilenameFilter((dir, name) -> isImageName(name));
        if (lastPhotoDir != null) {
            fd.setDirectory(lastPhotoDir);
        }
        fd.setVisible(true);
        return fd.getFile() == null ? null : new java.io.File(fd.getDirectory(), fd.getFile());
    }

    /**
     * Membuka dialog Open modern Windows lewat PowerShell (System.Windows.Forms.OpenFileDialog).
     * Mengembalikan {path} bila dipilih, {null} bila dibatalkan, atau null bila PowerShell gagal dijalankan.
     * Memakai SecondaryLoop supaya pemanggil tetap berjalan berurutan tanpa membekukan antarmuka.
     */
    private static String[] pickWithWindowsDialog(Window owner) {
        String initial = lastPhotoDir != null && new java.io.File(lastPhotoDir).isDirectory()
            ? lastPhotoDir : System.getProperty("user.home") + "\\Pictures";
        String script =
            "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8\n"
            + "Add-Type -AssemblyName System.Windows.Forms\n"
            + "$o = New-Object System.Windows.Forms.Form\n"
            + "$o.TopMost = $true\n"
            + "$d = New-Object System.Windows.Forms.OpenFileDialog\n"
            + "$d.Title = 'Pilih foto profil'\n"
            + "$d.Filter = 'Gambar (*.jpg;*.jpeg;*.png;*.gif;*.bmp)|*.jpg;*.jpeg;*.png;*.gif;*.bmp|Semua File (*.*)|*.*'\n"
            + "$d.CheckFileExists = $true\n"
            + "$d.InitialDirectory = '" + initial.replace("'", "''") + "'\n"
            + "if ($d.ShowDialog($o) -eq [System.Windows.Forms.DialogResult]::OK) "
            + "{ Write-Output ('OK|' + $d.FileName) } else { Write-Output 'CANCEL' }\n"
            + "$o.Dispose()\n";
        String encoded = java.util.Base64.getEncoder().encodeToString(
            script.getBytes(java.nio.charset.StandardCharsets.UTF_16LE));

        String[] outcome = {null};
        boolean[] ok = {false};
        SecondaryLoop loop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
        Thread worker = new Thread(() -> {
            try {
                Process proc = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-STA",
                    "-WindowStyle", "Hidden", "-ExecutionPolicy", "Bypass", "-EncodedCommand", encoded)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
                proc.getOutputStream().close();
                String text = new String(proc.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                proc.waitFor();
                for (String line : text.split("\\r?\\n")) {
                    line = line.replace("\uFEFF", "").trim();
                    if (line.startsWith("OK|")) {
                        outcome[0] = line.substring(3);
                        ok[0] = true;
                    } else if (line.equals("CANCEL")) {
                        ok[0] = true;
                    }
                }
            } catch (Exception ignored) {
                // PowerShell tidak tersedia: pakai dialog cadangan
            } finally {
                SwingUtilities.invokeLater(loop::exit);
            }
        }, "windows-file-dialog");
        worker.setDaemon(true);
        if (owner != null) {
            owner.setEnabled(false);
        }
        try {
            worker.start();
            loop.enter();
        } finally {
            if (owner != null) {
                owner.setEnabled(true);
                owner.toFront();
            }
        }
        return ok[0] ? new String[]{outcome[0]} : null;
    }

    static BufferedImage squarePhoto(BufferedImage src, int size) {
        int side = Math.min(src.getWidth(), src.getHeight());
        BufferedImage cur = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g0 = cur.createGraphics();
        g0.drawImage(src, -(src.getWidth() - side) / 2, -(src.getHeight() - side) / 2, null);
        g0.dispose();
        int w = side;
        while (w > size) {
            w = Math.max(size, w / 2);
            BufferedImage next = new BufferedImage(w, w, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = next.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(cur, 0, 0, w, w, null);
            g.dispose();
            cur = next;
        }
        return cur;
    }

    static JPanel studentInfoRows(StudentRecord s, int valueWidth) {
        return studentInfoRows(s, valueWidth, true);
    }

    /** Biodata siswa; withGrades=false menghilangkan daftar nilai (dipakai halaman yang menampilkan tabel nilai). */
    static JPanel studentInfoRows(StudentRecord s, int valueWidth, boolean withGrades) {
        JPanel g = p(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        int row = 0;
        String[][] rows = {
            {"Kelas", s.studentClass},
            {"Alamat", s.address},
            {"Email", s.email},
            {"Nomor Telepon", s.phone}
        };
        for (String[] r : rows) {
            c.gridx = 0;
            c.gridy = row;
            c.gridwidth = 1;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.insets = new Insets(6, 0, 6, 20);
            g.add(label(r[0], f(12), "muted"), c);
            c.gridx = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(6, 0, 6, 0);
            g.add(label("<html><div style='width:" + valueWidth + "px'>" + esc(r[1]) + "</div></html>", f(13), "title"), c);
            row++;
        }
        if (!withGrades) {
            return g;
        }

        c.gridx = 0;
        c.gridy = row++;
        c.gridwidth = 2;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(12, 0, 12, 0);
        g.add(new HLine(), c);

        c.gridy = row++;
        c.insets = new Insets(0, 0, 6, 0);
        g.add(label("Nilai UTS", fb(14), "title"), c);

        if (s.grades.isEmpty()) {
            c.gridy = row++;
            c.insets = new Insets(4, 0, 4, 0);
            g.add(label("Belum ada nilai UTS.", f(13), "muted"), c);
        } else {
            for (GradeRecord gr : s.grades) {
                c.gridx = 0;
                c.gridy = row;
                c.gridwidth = 1;
                c.weightx = 0;
                c.fill = GridBagConstraints.NONE;
                c.anchor = GridBagConstraints.WEST;
                c.insets = new Insets(5, 0, 5, 20);
                g.add(label(gr.subject, f(13), "title"), c);
                c.gridx = 1;
                c.weightx = 1;
                c.anchor = GridBagConstraints.EAST;
                c.insets = new Insets(5, 0, 5, 0);
                g.add(label(formatScore(gr.score), fb(13), "accent"), c);
                row++;
            }
        }
        return g;
    }

    // ===================== TABEL =====================
    /** Gaya tabel seragam: baris lega, garis tipis antar baris, isi tidak bisa diedit. */
    static JTable styledTable(javax.swing.table.TableModel model, int[] widths) {
        JTable table = new JTable(model) {
            @Override
            public String getToolTipText(java.awt.event.MouseEvent e) {
                int r = rowAtPoint(e.getPoint());
                int c = columnAtPoint(e.getPoint());
                Object v = r >= 0 && c >= 0 ? getValueAt(r, c) : null;
                return v == null || v.toString().isEmpty() ? null : v.toString();
            }
        };
        table.setRowHeight(40);
        table.setShowGrid(false);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.getTableHeader().setReorderingAllowed(false);
        table.setFont(f(13));
        javax.swing.table.TableCellRenderer headerBase = table.getTableHeader().getDefaultRenderer();
        javax.swing.table.DefaultTableCellRenderer cells = new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus,
                                                           int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, false, row, col);
                setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 8));
                return this;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) {
            javax.swing.table.TableColumn col = table.getColumnModel().getColumn(i);
            if (widths != null && i < widths.length) {
                col.setPreferredWidth(widths[i]);
            }
            col.setCellRenderer(cells);
            col.setHeaderRenderer((t, value, sel, focus, r, c) -> {
                Component comp = headerBase.getTableCellRendererComponent(t, value, sel, focus, r, c);
                if (comp instanceof JLabel) {
                    JLabel l = (JLabel) comp;
                    l.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 8));
                    l.setHorizontalAlignment(SwingConstants.LEFT);
                    l.setOpaque(true);
                    l.setBackground(Theme.surface());
                    l.setForeground(Theme.muted());
                    l.setFont(fb(12));
                }
                return comp;
            });
        }
        return table;
    }

    /** Model tabel yang selnya tidak bisa diedit. */
    static javax.swing.table.DefaultTableModel readOnlyModel(String... columns) {
        return new javax.swing.table.DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
                
            }
        };
    }
}
