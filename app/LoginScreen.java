package app;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

import app.Components.*;
import static app.Theme.f;
import static app.Theme.fb;
import static app.Theme.fi;
import static app.Theme.quality;

/**
 * Halaman login SINA (kartu di atas latar lembut dengan watermark logo). Warnanya mengikuti tema
 * terang/gelap aplikasi (Theme.dark), jadi selaras dengan halaman di dalam.
 * Peran dikenali otomatis dari akun.
 */
final class LoginScreen extends Page {
    private static final Color RED = new Color(220, 38, 38);
    private static final Color SKY = new Color(14, 165, 233);
    private static final Preferences PREFS = Preferences.userNodeForPackage(LoginScreen.class);

    private final JTextField usernameInput = new JTextField();
    private final JPasswordField passwordInput = new JPasswordField();
    private final JCheckBox remember = new JCheckBox("Ingat saya di perangkat ini");
    private LoginButton buttonLogin;
    private JPanel errorBanner;
    private WrapLabel errorText;
    private Card card;
    private Image logoImage;

    // ----- warna yang mengikuti tema (dibaca ulang setiap kali digambar) -----
    private static boolean dk()          { return Theme.dark; }
    private static Color ink()           { return Theme.text(); }
    private static Color slate()         { return Theme.muted(); }
    private static Color soft()          { return dk() ? new Color(100, 116, 145) : new Color(148, 163, 184); }
    private static Color line()          { return Theme.border(); }
    private static Color navy()          { return dk() ? Theme.ACCENT : new Color(15, 30, 60); }
    private static Color navyHover()     { return dk() ? new Color(59, 130, 246) : new Color(26, 46, 90); }
    private static Color navyPressed()   { return dk() ? new Color(29, 78, 216) : new Color(8, 18, 40); }
    private static Color tagline()       { return dk() ? Theme.GOLD : new Color(194, 125, 0); }
    private static Color rememberText()  { return dk() ? new Color(203, 213, 225) : new Color(71, 85, 105); }
    private static Color errorFill()     { return dk() ? new Color(69, 26, 36) : new Color(254, 236, 236); }
    private static Color errorInk()      { return dk() ? new Color(254, 202, 202) : new Color(127, 29, 29); }
    private static Color infoFill()      { return dk() ? new Color(16, 26, 50) : new Color(249, 251, 254); }
    private static Color red()           { return RED; }
    private static Color sky()           { return SKY; }

    /** Daftar pekerjaan yang dijalankan ulang saat tema berganti. */
    private final List<Runnable> themed = new ArrayList<>();
    private LightBackdrop screen;

    LoginScreen(AppShell app) {
        super(app);
    }

    private void themed(Runnable r) {
        themed.add(r);
        r.run();
    }

    /** Dipanggil saat tema diganti dari dalam aplikasi, supaya halaman login ikut selaras. */
    void refreshTheme() {
        for (Runnable r : themed) {
            r.run();
        }
        if (screen != null) {
            screen.repaint();
        }
    }

    @Override
    JPanel view() {
        ImageIcon file = new ImageIcon("logo_SMK_ALMASHO.png");
        logoImage = file.getIconWidth() > 0 ? file.getImage() : null;

        screen = new LightBackdrop(logoImage);
        screen.putClientProperty("noTheme", Boolean.TRUE);

        card = new Card();

        JLabel logo = new JLabel(logoIcon(64));
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel title = text("SINA", fb(28), LoginScreen::ink, SwingConstants.CENTER);
        JLabel subtitle = text("Sistem Informasi Nilai & Akademik", fb(13), LoginScreen::slate, SwingConstants.CENTER);
        JLabel school = text("SMK Al Malikus Sholeh", f(13), LoginScreen::slate, SwingConstants.CENTER);
        JLabel tagline = text("Berotak Teknologi, Berhati Qur'ani", fi(14), LoginScreen::tagline,
            SwingConstants.CENTER);

        // kolom isian
        JToggleButton eye = new JToggleButton(new LoginEye());
        eye.setToolTipText("Tampilkan atau sembunyikan sandi");
        eye.setContentAreaFilled(false);
        eye.setBorderPainted(false);
        eye.setFocusPainted(false);
        eye.setOpaque(false);
        eye.setPreferredSize(new Dimension(46, 50));
        eye.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        char echo = passwordInput.getEchoChar();
        eye.addActionListener(e -> passwordInput.setEchoChar(eye.isSelected() ? (char) 0 : echo));

        IconField userField = new IconField(usernameInput, 0, null);
        IconField passField = new IconField(passwordInput, 1, eye);
        themed(userField::refresh);
        themed(passField::refresh);

        // baris "ingat saya" dan "lupa kata sandi"
        remember.setOpaque(false);
        remember.setFocusPainted(false);
        remember.setFont(f(13));
        themed(() -> remember.setForeground(rememberText()));
        remember.setIcon(new CheckIcon(false));
        remember.setSelectedIcon(new CheckIcon(true));
        remember.setIconTextGap(10);
        remember.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel forgot = text("Lupa kata sandi?", fb(13), LoginScreen::slate, SwingConstants.RIGHT);
        forgot.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgot.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                AppDialog.show(app.frame(), AppDialog.INFO, "Lupa kata sandi",
                    "Hubungi operator sekolah untuk mengatur ulang kata sandi Anda.");
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                forgot.setForeground(ink());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                forgot.setForeground(slate());
            }
        });
        JPanel options = new JPanel(new BorderLayout());
        options.setOpaque(false);
        options.add(remember, BorderLayout.WEST);
        options.add(forgot, BorderLayout.EAST);

        // banner kesalahan (tersembunyi sampai dibutuhkan)
        errorText = new WrapLabel(f(13), fb(13), LoginScreen::errorInk, 340);
        errorBanner = new Banner(LoginScreen::errorFill, LoginScreen::red, "!", errorText);
        errorBanner.setVisible(false);

        buttonLogin = new LoginButton("Masuk");
        buttonLogin.addActionListener(e -> doLogin());

        WrapLabel infoText = new WrapLabel(f(12), fb(12), LoginScreen::slate, 332);
        infoText.setText("Belum punya akun? Akun siswa dan guru dibuat oleh **Operator** sekolah. "
            + "Hubungi operator untuk mendapatkan akses.");
        JPanel info = new Banner(LoginScreen::infoFill, LoginScreen::sky, "i", infoText);

        int row = 0;
        put(card, logo, row++, 6);
        put(card, title, row++, 1);
        put(card, subtitle, row++, 2);
        put(card, school, row++, 4);
        put(card, tagline, row++, 16);
        put(card, text("Nama pengguna", fb(13), LoginScreen::ink, SwingConstants.LEFT), row++, 7);
        put(card, userField, row++, 16);
        put(card, text("Kata sandi", fb(13), LoginScreen::ink, SwingConstants.LEFT), row++, 7);
        put(card, passField, row++, 14);
        put(card, options, row++, 14);
        put(card, errorBanner, row++, 14);
        put(card, buttonLogin, row++, 18);
        put(card, info, row, 0);

        DocumentListener clear = new SimpleDoc(this::hideError);
        usernameInput.getDocument().addDocumentListener(clear);
        passwordInput.getDocument().addDocumentListener(clear);

        String saved = PREFS.get("username", "");
        if (!saved.isEmpty()) {
            usernameInput.setText(saved);
            remember.setSelected(true);
        }

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(card);

        screen.add(center, BorderLayout.CENTER);
        return screen;
    }

    JButton defaultButton() {
        return buttonLogin;
    }

    @Override
    void reset() {
        passwordInput.setText("");
        if (!remember.isSelected()) {
            usernameInput.setText("");
        }
        hideError();
    }

    private void doLogin() {
        String username = usernameInput.getText().trim();
        String password = new String(passwordInput.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            showError("Nama pengguna dan kata sandi wajib diisi.");
            return;
        }
        DataStore.reload(); // ambil akun terbaru dari server sebelum memeriksa login
        String role = DataStore.authenticate(username, password);
        if (role == null) {
            showError("Nama pengguna atau kata sandi salah.");
            return;
        }
        try {
            if (remember.isSelected()) {
                PREFS.put("username", username);
            } else {
                PREFS.remove("username");
            }
        } catch (Exception ignored) {
            // penyimpanan preferensi tidak tersedia; abaikan
        }
        hideError();
        passwordInput.setText("");
        app.enter(role, username);
    }

    private void showError(String message) {
        errorText.setText(message);
        errorBanner.setVisible(true);
        card.revalidate();
        card.repaint();
    }

    private void hideError() {
        if (errorBanner != null && errorBanner.isVisible()) {
            errorBanner.setVisible(false);
            card.revalidate();
            card.repaint();
        }
    }

    private Icon logoIcon(int size) {
        if (logoImage == null) {
            return new AvatarIcon("SM", size, Theme.ACCENT, Color.WHITE);
        }
        return new ImageIcon(logoImage.getScaledInstance(size, size, Image.SCALE_SMOOTH));
    }

    private JLabel text(String value, Font font, Supplier<Color> color, int align) {
        JLabel label = new JLabel(value, align);
        label.setFont(font);
        themed(() -> label.setForeground(color.get()));
        return label;
    }

    private static void put(JPanel panel, Component c, int row, int bottom) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = row;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 0, bottom, 0);
        panel.add(c, gc);
    }

    // ===================== KOMPONEN KHUSUS HALAMAN LOGIN =====================

    /** Latar gradasi lembut dengan watermark logo. */
    private static final class LightBackdrop extends JPanel {
        private final Image logo;

        LightBackdrop(Image logo) {
            super(new BorderLayout());
            this.logo = logo;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            int w = getWidth();
            int h = getHeight();
            if (Theme.dark) {
                g2.setPaint(new GradientPaint(0, 0, Theme.bg(), w, h, new Color(22, 30, 62)));
            } else {
                g2.setPaint(new GradientPaint(0, 0, new Color(232, 240, 255), w, h, new Color(251, 236, 250)));
            }
            g2.fillRect(0, 0, w, h);
            if (logo != null) {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Theme.dark ? 0.06f : 0.07f));
                int[][] marks = {
                    {(int) (w * 0.04), (int) (h * 0.03), 110},
                    {(int) (w * 0.80), (int) (h * 0.04), 230},
                    {-50, (int) (h * 0.62), 210},
                    {(int) (w * 0.78), (int) (h * 0.68), 260},
                    {(int) (w * 0.20), (int) (h * 0.80), 120}
                };
                for (int[] m : marks) {
                    g2.drawImage(logo, m[0], m[1], m[2], m[2], null);
                }
            }
            g2.dispose();
        }
    }

    /** Kartu putih bersudut membulat dengan bayangan lembut. */
    private static final class Card extends JPanel {
        Card() {
            super(new GridBagLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(6 + 26, 10 + 50, 14 + 26, 10 + 50));
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(520, d.height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            int x = 10;
            int y = 6;
            int w = getWidth() - 20;
            int h = getHeight() - 20;
            for (int k = 9; k >= 1; k--) {
                g2.setColor(Theme.dark ? new Color(0, 0, 0, 22) : new Color(30, 41, 90, 5));
                g2.fillRoundRect(x - k, y - k + 5, w + 2 * k, h + 2 * k, 30 + 2 * k, 30 + 2 * k);
            }
            g2.setColor(Theme.surface());
            g2.fillRoundRect(x, y, w, h, 30, 30);
            g2.setColor(Theme.dark ? Theme.border() : new Color(232, 237, 245));
            g2.drawRoundRect(x, y, w, h, 30, 30);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Kolom isian bulat dengan ikon di kiri dan (opsional) tombol di kanan. */
    private static final class IconField extends JPanel {
        private final JTextField field;
        private boolean focused;

        IconField(JTextField field, int icon, JComponent trailing) {
            super(new BorderLayout());
            setOpaque(false);
            setPreferredSize(new Dimension(100, 52));
            setMinimumSize(new Dimension(100, 52));

            this.field = field;
            field.setOpaque(false);
            field.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
            field.setFont(f(14));
            refresh();
            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    focused = true;
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    focused = false;
                    repaint();
                }
            });

            JComponent lead = new JComponent() {
                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(50, 52);
                }

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    quality(g2);
                    int cx = 26;
                    int cy = getHeight() / 2;
                    g2.setColor(focused ? Theme.ACCENT : soft());
                    g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    if (icon == 0) {
                        g2.draw(new Ellipse2D.Double(cx - 4.5, cy - 9, 9, 9));
                        g2.draw(new Arc2D.Double(cx - 8, cy + 1.5, 16, 16, 0, 180, Arc2D.OPEN));
                    } else {
                        g2.drawRoundRect(cx - 7, cy - 1, 14, 10, 4, 4);
                        g2.draw(new Arc2D.Double(cx - 4.5, cy - 9, 9, 10, 0, 180, Arc2D.OPEN));
                        g2.drawLine(cx - 4, cy - 4, cx - 4, cy - 1);
                        g2.drawLine(cx + 5, cy - 4, cx + 5, cy - 1);
                    }
                    g2.dispose();
                }
            };

            add(lead, BorderLayout.WEST);
            add(field, BorderLayout.CENTER);
            if (trailing != null) {
                add(trailing, BorderLayout.EAST);
            }
        }

        /** Terapkan warna teks sesuai tema. */
        void refresh() {
            field.setForeground(ink());
            field.setCaretColor(ink());
            field.setSelectionColor(Theme.accentSoft());
            field.setSelectedTextColor(ink());
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            int w = getWidth();
            int h = getHeight();
            if (Theme.dark) {
                g2.setColor(focused ? Theme.surface() : Theme.input());
            } else {
                g2.setColor(focused ? Color.WHITE : new Color(244, 247, 252));
            }
            g2.fillRoundRect(0, 0, w - 1, h - 1, 16, 16);
            if (focused) {
                g2.setColor(new Color(37, 99, 235, 40));
                g2.setStroke(new BasicStroke(4f));
                g2.drawRoundRect(1, 1, w - 3, h - 3, 16, 16);
                g2.setColor(Theme.ACCENT);
                g2.setStroke(new BasicStroke(1.5f));
            } else {
                g2.setColor(line());
                g2.setStroke(new BasicStroke(1f));
            }
            g2.drawRoundRect(0, 0, w - 1, h - 1, 16, 16);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Kotak informasi/kesalahan: ikon bulat di kiri, teks berbungkus di kanan. */
    private static final class Banner extends JPanel {
        private final Supplier<Color> fill;
        private final Supplier<Color> tone;
        private final String glyph;

        Banner(Supplier<Color> fill, Supplier<Color> tone, String glyph, JComponent content) {
            super(new BorderLayout(12, 0));
            this.fill = fill;
            this.tone = tone;
            this.glyph = glyph;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
            JComponent icon = new JComponent() {
                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(22, 22);
                }

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    quality(g2);
                    g2.setColor(tone.get());
                    g2.setStroke(new BasicStroke(1.8f));
                    g2.draw(new Ellipse2D.Double(1, 1, 19, 19));
                    g2.setFont(fb(12));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(glyph, 1 + (19 - fm.stringWidth(glyph)) / 2f + 0.5f,
                        1 + (19 - fm.getHeight()) / 2f + fm.getAscent());
                    g2.dispose();
                }
            };
            JPanel iconWrap = new JPanel(new BorderLayout());
            iconWrap.setOpaque(false);
            iconWrap.add(icon, BorderLayout.NORTH);
            add(iconWrap, BorderLayout.WEST);
            add(content, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            Color t = tone.get();
            g2.setColor(fill.get());
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.setColor(new Color(t.getRed(), t.getGreen(), t.getBlue(), 45));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Teks yang membungkus baris sendiri. Tanda ** mengapit kata tebal. */
    private static final class WrapLabel extends JComponent {
        private final Font regular;
        private final Font bold;
        private final Supplier<Color> color;
        private final int width;
        private final List<Object[]> words = new ArrayList<>();
        private int height;

        WrapLabel(Font regular, Font bold, Supplier<Color> color, int width) {
            this.regular = regular;
            this.bold = bold;
            this.color = color;
            this.width = width;
            setOpaque(false);
            setText(" ");
        }

        void setText(String markup) {
            words.clear();
            FontMetrics fr = getFontMetrics(regular);
            FontMetrics fbm = getFontMetrics(bold);
            int lineHeight = fr.getHeight() + 3;
            int x = 0;
            int y = fr.getAscent();
            boolean isBold = false;
            for (String part : markup.split("\\*\\*", -1)) {
                FontMetrics fm = isBold ? fbm : fr;
                for (String word : part.split(" ")) {
                    if (word.isEmpty()) {
                        continue;
                    }
                    int w = fm.stringWidth(word);
                    if (x > 0 && x + w > width) {
                        x = 0;
                        y += lineHeight;
                    }
                    words.add(new Object[]{word, x, y, isBold});
                    x += w + fm.charWidth(' ');
                }
                isBold = !isBold;
            }
            height = y + fr.getDescent();
            revalidate();
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(width, height);
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            for (Object[] w : words) {
                boolean isBold = (Boolean) w[3];
                g2.setFont(isBold ? bold : regular);
                g2.setColor(isBold ? ink() : color.get());
                g2.drawString((String) w[0], (Integer) w[1], (Integer) w[2]);
            }
            g2.dispose();
        }
    }

    /** Tombol navy dengan teks dan panah. */
    private static final class LoginButton extends JButton {
        private boolean hover;

        LoginButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setFont(fb(15));
            setPreferredSize(new Dimension(100, 52));
            setMinimumSize(new Dimension(100, 52));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            int w = getWidth();
            int h = getHeight();
            Color fill = getModel().isPressed() ? navyPressed()
                : (hover ? navyHover() : navy());
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, w, h, 16, 16);
            g2.setColor(Color.WHITE);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(getText());
            int total = textWidth + 12 + 18;
            int x = (w - total) / 2;
            g2.drawString(getText(), x, (h - fm.getHeight()) / 2 + fm.getAscent());
            int ax = x + textWidth + 12;
            int cy = h / 2;
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(ax, cy, ax + 16, cy);
            g2.drawPolyline(new int[]{ax + 10, ax + 16, ax + 10}, new int[]{cy - 5, cy, cy + 5}, 3);
            g2.dispose();
        }
    }

    private static final class CheckIcon implements Icon {
        private final boolean checked;

        CheckIcon(boolean checked) {
            this.checked = checked;
        }

        @Override
        public int getIconWidth() {
            return 20;
        }

        @Override
        public int getIconHeight() {
            return 20;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            if (checked) {
                g2.setColor(navy());
                g2.fillRoundRect(x + 1, y + 1, 18, 18, 6, 6);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawPolyline(new int[]{x + 5, x + 9, x + 15}, new int[]{y + 10, y + 14, y + 6}, 3);
            } else {
                g2.setColor(Theme.input());
                g2.fillRoundRect(x + 1, y + 1, 18, 18, 6, 6);
                g2.setColor(dk() ? new Color(71, 85, 119) : new Color(176, 186, 202));
                g2.setStroke(new BasicStroke(1.4f));
                g2.drawRoundRect(x + 1, y + 1, 18, 18, 6, 6);
            }
            g2.dispose();
        }
    }

    private static final class LoginEye implements Icon {
        @Override
        public int getIconWidth() {
            return 22;
        }

        @Override
        public int getIconHeight() {
            return 18;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            g2.setColor(soft());
            g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Arc2D.Double(x + 1, y + 3, 20, 12, 0, 180, Arc2D.OPEN));
            g2.draw(new Arc2D.Double(x + 1, y + 3, 20, 12, 180, 180, Arc2D.OPEN));
            g2.fill(new Ellipse2D.Double(x + 8, y + 6, 6, 6));
            if (!(c instanceof AbstractButton) || !((AbstractButton) c).isSelected()) {
                g2.drawLine(x + 3, y + 16, x + 19, y + 2);
            }
            g2.dispose();
        }
    }

    private static final class SimpleDoc implements DocumentListener {
        private final Runnable action;

        SimpleDoc(Runnable action) {
            this.action = action;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            action.run();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            action.run();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            action.run();
        }
    }
}