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
import static app.Components.modalDialog;
import static app.Theme.f;
import static app.Theme.fb;
import static app.Theme.fi;
import static app.Theme.quality;

/** Halaman masuk SINA dengan tata letak bersih, animasi teks rapi, dan ukuran kartu login semula. */
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

    private static boolean dk()          { return Theme.dark; }
    private static Color ink()           { return Theme.text(); }
    private static Color slate()         { return Theme.muted(); }
    private static Color soft()          { return dk() ? new Color(100, 116, 145) : new Color(148, 163, 184); }
    private static Color line()          { return Theme.border(); }
    private static Color navy()          { return dk() ? Theme.ACCENT : new Color(28, 63, 96); }
    private static Color navyHover()     { return dk() ? new Color(59, 130, 246) : new Color(38, 82, 124); }
    private static Color navyPressed()   { return dk() ? new Color(29, 78, 216) : new Color(18, 43, 68); }
    private static Color tagline()       { return dk() ? Theme.GOLD : new Color(194, 125, 0); }
    private static Color rememberText()  { return dk() ? new Color(203, 213, 225) : new Color(71, 85, 105); }
    private static Color errorFill()     { return dk() ? new Color(69, 26, 36) : new Color(254, 236, 236); }
    private static Color errorInk()      { return dk() ? new Color(254, 202, 202) : new Color(127, 29, 29); }
    private static Color infoFill()      { return dk() ? new Color(16, 26, 50) : new Color(245, 248, 253); }
    private static Color red()           { return RED; }
    private static Color sky()           { return SKY; }

    private final List<Runnable> themed = new ArrayList<>();
    private HeroBackdrop screen;

    LoginScreen(AppShell app) {
        super(app);
    }

    private void themed(Runnable r) {
        themed.add(r);
        r.run();
    }

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

        screen = new HeroBackdrop(logoImage);
        screen.putClientProperty("noTheme", Boolean.TRUE);

        // Panel kiri: teks beranimasi dengan jarak & spasi lega
        SinaCarouselPanel carousel = new SinaCarouselPanel(this::showMoreModal);

        // Panel kanan: kotak login ukuran semula (455px)
        card = new Card();

        JLabel logo = new JLabel(logoIcon(74));
        logo.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel title = text("Sistem Informasi Nilai & Akademik", fb(20), LoginScreen::ink, SwingConstants.CENTER);
        JLabel school = text("SMK Al Malikus Sholeh", f(13), LoginScreen::slate, SwingConstants.CENTER);
        JLabel tagline = text("Berotak Teknologi, Berhati Qur'ani", fi(13), LoginScreen::tagline, SwingConstants.CENTER);

        JToggleButton eye = new JToggleButton(new LoginEye());
        eye.setToolTipText("Tampilkan atau sembunyikan sandi");
        eye.setContentAreaFilled(false);
        eye.setBorderPainted(false);
        eye.setFocusPainted(false);
        eye.setOpaque(false);
        eye.setPreferredSize(new Dimension(44, 46));
        eye.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        char echo = passwordInput.getEchoChar();
        eye.addActionListener(e -> passwordInput.setEchoChar(eye.isSelected() ? (char) 0 : echo));

        IconField userField = new IconField(usernameInput, 0, null);
        IconField passField = new IconField(passwordInput, 1, eye);
        themed(userField::refresh);
        themed(passField::refresh);

        remember.setOpaque(false);
        remember.setFocusPainted(false);
        remember.setFont(f(12.5f));
        themed(() -> remember.setForeground(rememberText()));
        remember.setIcon(new CheckIcon(false));
        remember.setSelectedIcon(new CheckIcon(true));
        remember.setIconTextGap(8);
        remember.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel forgot = text("Lupa kata sandi?", fb(12.5f), LoginScreen::slate, SwingConstants.RIGHT);
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

        errorText = new WrapLabel(f(12.5f), fb(12.5f), LoginScreen::errorInk, 336);
        errorBanner = new Banner(LoginScreen::errorFill, LoginScreen::red, "!", errorText);
        errorBanner.setVisible(false);

        buttonLogin = new LoginButton("Masuk");
        buttonLogin.addActionListener(e -> doLogin());

        WrapLabel infoText = new WrapLabel(f(12), fb(12), LoginScreen::slate, 332);
        infoText.setText("Akun guru dan siswa dikelola oleh **Operator** sekolah. Hubungi operator untuk akses masuk.");
        JPanel info = new Banner(LoginScreen::infoFill, LoginScreen::sky, "i", infoText);

        int row = 0;
        put(card, logo, row++, 12);
        put(card, title, row++, 4);
        put(card, school, row++, 4);
        put(card, tagline, row++, 20);
        put(card, text("Nama pengguna", fb(12.5f), LoginScreen::ink, SwingConstants.LEFT), row++, 6);
        put(card, userField, row++, 14);
        put(card, text("Kata sandi", fb(12.5f), LoginScreen::ink, SwingConstants.LEFT), row++, 6);
        put(card, passField, row++, 12);
        put(card, options, row++, 14);
        put(card, errorBanner, row++, 12);
        put(card, buttonLogin, row++, 16);
        put(card, info, row, 0);

        DocumentListener clear = new SimpleDoc(this::hideError);
        usernameInput.getDocument().addDocumentListener(clear);
        passwordInput.getDocument().addDocumentListener(clear);

        String saved = PREFS.get("username", "");
        if (!saved.isEmpty()) {
            usernameInput.setText(saved);
            remember.setSelected(true);
        }

        // Susun 2 kolom simetris penuh tanpa header atas
        JPanel split = new JPanel(new GridBagLayout());
        split.setOpaque(false);
        split.setBorder(BorderFactory.createEmptyBorder(24, 52, 24, 52));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = 0;
        gc.gridx = 0;
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        gc.fill = GridBagConstraints.BOTH;
        gc.insets = new Insets(0, 0, 0, 44);
        split.add(carousel, gc);

        gc.gridx = 1;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        gc.anchor = GridBagConstraints.CENTER;
        gc.fill = GridBagConstraints.NONE;
        gc.insets = new Insets(0, 0, 0, 0);
        split.add(card, gc);

        screen.add(split, BorderLayout.CENTER);
        return screen;
    }

    /** Popup informasi lengkap saat tombol "Selengkapnya" diklik. */
    private void showMoreModal() {
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));

        JLabel p1 = new JLabel(
            "<html><div style='line-height:1.6;'>"
            + "<b>Sistem Informasi Nilai &amp; Akademik (SINA)</b> merupakan layanan administrasi akademik "
            + "terpadu milik <b>SMK Al Malikus Sholeh</b> yang dirancang untuk menyatukan pengelolaan akun pendidik, "
            + "biodata peserta didik kelas 10 hingga 12, sinkronisasi tarik data Dapodik, pengaturan jadwal pelajaran, "
            + "serta rekapitulasi nilai Ujian Tengah Semester (UTS) secara terstandar dan akurat.</div></html>"
        );
        p1.setFont(f(13));
        p1.setForeground(ink());
        p1.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(p1);
        body.add(Box.createVerticalStrut(18));

        JLabel h2 = new JLabel("Tujuan Utama Penerapan SINA");
        h2.setFont(fb(14.5f));
        h2.setForeground(Theme.ACCENT);
        h2.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(h2);
        body.add(Box.createVerticalStrut(10));

        String[] points = {
            "Memperoleh informasi capaian akademik peserta didik yang terstandar untuk evaluasi pembelajaran.",
            "Menjamin keterpaduan data induk guru dan peserta didik melalui fitur tarik data Dapodik yang rapi.",
            "Mendorong peningkatan efektivitas pendidik dalam menyusun jadwal pelajaran serta pelaporan nilai UTS.",
            "Memberikan transparansi informasi perkembangan belajar kepada guru, wali kelas, dan peserta didik."
        };
        for (String pt : points) {
            JLabel item = new JLabel("<html><div style='line-height:1.5;'>\u2713 &nbsp;" + pt + "</div></html>");
            item.setFont(f(12.5f));
            item.setForeground(slate());
            item.setBorder(BorderFactory.createEmptyBorder(0, 6, 8, 0));
            item.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(item);
        }

        body.add(Box.createVerticalStrut(10));
        JLabel h3 = new JLabel("Alur Penggunaan Sistem");
        h3.setFont(fb(14.5f));
        h3.setForeground(Theme.ACCENT);
        h3.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(h3);
        body.add(Box.createVerticalStrut(8));

        JLabel flow = new JLabel(
            "<html><div style='line-height:1.55;'>"
            + "1. <b>Operator Sekolah</b> melakukan tarik data Dapodik, pemetaan mata pelajaran guru, dan jadwal kelas.<br>"
            + "2. <b>Bapak/Ibu Guru</b> masuk menggunakan akun terdaftar untuk menginput dan memperbarui nilai UTS.<br>"
            + "3. <b>Peserta Didik</b> dapat memantau biodata akademik serta jadwal pelajaran secara langsung.</div></html>"
        );
        flow.setFont(f(12.5f));
        flow.setForeground(ink());
        flow.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(flow);

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        JDialog dlg = modalDialog(
            app.frame(),
            "TENTANG SINA",
            "Sistem Informasi Nilai & Akademik",
            "SMK Al Malikus Sholeh  \u00B7  Portal Administrasi Akademik",
            Theme.ACCENT,
            scroll,
            null,
            620,
            470
        );
        dlg.setVisible(true);
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
        DataStore.reload();
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

    // ===================== HERO CAROUSEL 2 SLIDE DENGAN JARAK TEKS RAPI =====================
    private static final class SinaCarouselPanel extends JPanel {
        private static final String[] TUJUAN_POINTS = {
            "Memperoleh informasi capaian akademik peserta didik yang terstandar untuk evaluasi pembelajaran.",
            "Menjamin keterpaduan data induk guru dan peserta didik melalui fitur tarik data Dapodik yang rapi.",
            "Mendorong peningkatan efektivitas pendidik dalam menyusun jadwal pelajaran serta pelaporan nilai UTS.",
            "Memberikan informasi capaian belajar kepada guru, wali kelas, dan peserta didik secara transparan."
        };

        private int slideIndex = 0;
        private float progress = 1.0f;
        private boolean moreHover = false;
        private final Rectangle[] indicatorRects = new Rectangle[2];
        private Rectangle moreBtnRect = new Rectangle();

        SinaCarouselPanel(Runnable onMoreClick) {
            setOpaque(false);

            Timer autoSwitch = new Timer(6500, e -> switchTo((slideIndex + 1) % 2));
            autoSwitch.start();

            Timer animTimer = new Timer(28, e -> {
                if (progress < 1.0f) {
                    progress = Math.min(1.0f, progress + 0.045f);
                    repaint();
                }
            });
            animTimer.start();

            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    boolean onMore = moreBtnRect.contains(e.getPoint());
                    boolean onInd = (indicatorRects[0] != null && indicatorRects[0].contains(e.getPoint()))
                        || (indicatorRects[1] != null && indicatorRects[1].contains(e.getPoint()));
                    if (onMore != moreHover) {
                        moreHover = onMore;
                        repaint();
                    }
                    setCursor(Cursor.getPredefinedCursor((onMore || onInd) ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                }
            });

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (moreBtnRect.contains(e.getPoint())) {
                        onMoreClick.run();
                        return;
                    }
                    for (int i = 0; i < indicatorRects.length; i++) {
                        if (indicatorRects[i] != null && indicatorRects[i].contains(e.getPoint())) {
                            switchTo(i);
                            return;
                        }
                    }
                }
            });
        }

        private void switchTo(int target) {
            slideIndex = target;
            progress = 0.0f;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            int w = getWidth();
            int h = getHeight();
            int padX = 12;
            int maxW = Math.max(300, w - 24);

            float ease = (float) (1.0 - Math.pow(1.0 - progress, 3));
            int slideShift = (int) Math.round((1.0f - ease) * 44);

            // Posisikan blok konten tepat di tengah vertikal layar
            int contentHeight = 420;
            int y = Math.max(40, (h - contentHeight) / 2);

if (slideIndex == 0) {
                // ---------- SLIDE 1: APA ITU SINA? ----------
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.1f, ease)));
                g2.setFont(fb(26));
                g2.setColor(ink());
                y = drawWrapped(g2, "Apa Itu Sistem Informasi Nilai & Akademik?", padX + slideShift, y, maxW, 36);
                y += 28;

                g2.setFont(f(14));
                g2.setColor(ink());
                String p1 = "Sistem Informasi Nilai & Akademik (SINA) dilatarbelakangi oleh perlunya pelaporan "
                    + "capaian akademik peserta didik yang terstandar di SMK Al Malikus Sholeh. Tidak tersedianya "
                    + "pencatatan nilai dan jadwal yang terintegrasi dapat menyulitkan rekapitulasi hasil belajar siswa.";
                y = drawWrapped(g2, p1, padX + slideShift, y, maxW, 24);
                y += 42;

                String p2 = "Melalui integrasi tarik data Dapodik, distribusi mata pelajaran guru, dan pengelolaan nilai UTS "
                    + "terpusat, SINA menjamin objektivitas, kecepatan, serta akurasi administrasi akademik sekolah.";
                y = drawWrapped(g2, p2, padX + slideShift, y, maxW, 24);
                y += 42;
            } else {
                // ---------- SLIDE 2: TUJUAN SINA (4 POIN CENTANG DENGAN SPASI LEGA) ----------
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.1f, ease)));
                g2.setFont(fb(26));
                g2.setColor(ink());
                y = drawWrapped(g2, "Tujuan Sistem Informasi Nilai & Akademik", padX + slideShift, y, maxW, 36);
                y += 28;

                for (int i = 0; i < TUJUAN_POINTS.length; i++) {
                    float itemStart = i * 0.16f;
                    float itemAlpha = Math.max(0f, Math.min(1f, (progress - itemStart) / 0.36f));
                    int itemShift = (int) Math.round((1.0f - itemAlpha) * 26);

                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.05f, itemAlpha)));
                    int cx = padX + itemShift;
                    int cy = y - 13;

                    // Ikon lingkaran centang rapi
                    g2.setColor(dk() ? new Color(255, 255, 255, 235) : Theme.ACCENT);
                    g2.fillOval(cx, cy, 20, 20);
                    g2.setColor(dk() ? new Color(28, 63, 96) : Color.WHITE);
                    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawPolyline(new int[]{cx + 5, cx + 9, cx + 15}, new int[]{cy + 10, cy + 14, cy + 7}, 3);

                    g2.setFont(f(14));
                    g2.setColor(ink());
                    y = drawWrapped(g2, TUJUAN_POINTS[i], cx + 32, y, maxW - 36, 23);
                    y += 22;
                }
                y += 14;
            }

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

            // 2 Garis Indikator Slide di Tengah
            int barW = 36;
            int barH = 4;
            int gap = 10;
            int totalIndW = 2 * barW + gap;
            int ix = padX + (maxW - totalIndW) / 2;
            for (int i = 0; i < 2; i++) {
                indicatorRects[i] = new Rectangle(ix - 4, y - 8, barW + 8, barH + 16);
                g2.setColor(i == slideIndex ? (dk() ? Color.WHITE : Theme.ACCENT) : line());
                g2.fillRoundRect(ix, y, barW, barH, barH, barH);
                ix += barW + gap;
            }
            y += 26;

            // Tombol "➔ Selengkapnya"
            int btnH = 46;
            moreBtnRect = new Rectangle(padX, y, maxW, btnH);
            g2.setColor(moreHover
                ? (dk() ? new Color(255, 255, 255, 28) : Theme.accentSoft())
                : (dk() ? new Color(255, 255, 255, 14) : new Color(255, 255, 255, 200)));
            g2.fillRoundRect(moreBtnRect.x, moreBtnRect.y, moreBtnRect.width, moreBtnRect.height, 12, 12);
            g2.setColor(moreHover ? Theme.ACCENT : (dk() ? new Color(255, 255, 255, 90) : line()));
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(moreBtnRect.x, moreBtnRect.y, moreBtnRect.width, moreBtnRect.height, 12, 12);

            g2.setFont(fb(13.5f));
            String btnText = "Selengkapnya";
            FontMetrics fm = g2.getFontMetrics();
            int r = 8;
            int contentW = r * 2 + 10 + fm.stringWidth(btnText);
            int bx = moreBtnRect.x + (moreBtnRect.width - contentW) / 2;
            int by = moreBtnRect.y + btnH / 2;

            g2.setColor(ink());
            g2.fillOval(bx, by - r, r * 2, r * 2);
            g2.setColor(dk() ? Theme.bg() : Color.WHITE);
            g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(bx + 4, by, bx + 12, by);
            g2.drawPolyline(new int[]{bx + 9, bx + 12, bx + 9}, new int[]{by - 3, by, by + 3}, 3);

            g2.setColor(ink());
            g2.drawString(btnText, bx + r * 2 + 10, by - fm.getHeight() / 2 + fm.getAscent());

            g2.dispose();
        }

        private int drawWrapped(Graphics2D g2, String text, int x, int startY, int maxWidth, int lineHeight) {
            FontMetrics fm = g2.getFontMetrics();
            StringBuilder lineBuf = new StringBuilder();
            int y = startY;
            for (String word : text.split(" ")) {
                String test = lineBuf.length() == 0 ? word : lineBuf + " " + word;
                if (fm.stringWidth(test) > maxWidth && lineBuf.length() > 0) {
                    g2.drawString(lineBuf.toString(), x, y);
                    y += lineHeight;
                    lineBuf = new StringBuilder(word);
                } else {
                    lineBuf = new StringBuilder(test);
                }
            }
            if (lineBuf.length() > 0) {
                g2.drawString(lineBuf.toString(), x, y);
            }
            return y;
        }
    }

    // ===================== LATAR & KARTU LOGIN UKURAN SEMULA =====================
    private static final class HeroBackdrop extends JPanel {
        private final Image logo;

        HeroBackdrop(Image logo) {
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
                g2.setPaint(new GradientPaint(0, 0, new Color(18, 36, 56), w, h, new Color(13, 22, 38)));
            } else {
                g2.setPaint(new GradientPaint(0, 0, new Color(224, 236, 250), w, h, new Color(242, 247, 255)));
            }
            g2.fillRect(0, 0, w, h);
            if (logo != null) {
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Theme.dark ? 0.05f : 0.06f));
                g2.drawImage(logo, (int) (w * 0.04), (int) (h * 0.14), 210, 210, null);
                g2.drawImage(logo, (int) (w * 0.42), (int) (h * 0.64), 180, 180, null);
            }
            g2.dispose();
        }
    }

    private static final class Card extends JPanel {
        Card() {
            super(new GridBagLayout());
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(26, 38, 28, 38));
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(455, d.height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            int x = 10;
            int y = 6;
            int w = getWidth() - 20;
            int h = getHeight() - 18;
            for (int k = 8; k >= 1; k--) {
                g2.setColor(Theme.dark ? new Color(0, 0, 0, 22) : new Color(30, 41, 90, 5));
                g2.fillRoundRect(x - k, y - k + 4, w + 2 * k, h + 2 * k, 26 + 2 * k, 26 + 2 * k);
            }
            g2.setColor(Theme.surface());
            g2.fillRoundRect(x, y, w, h, 26, 26);
            g2.setColor(Theme.dark ? Theme.border() : new Color(225, 232, 242));
            g2.drawRoundRect(x, y, w, h, 26, 26);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class IconField extends JPanel {
        private final JTextField field;
        private boolean focused;

        IconField(JTextField field, int icon, JComponent trailing) {
            super(new BorderLayout());
            setOpaque(false);
            setPreferredSize(new Dimension(100, 46));
            setMinimumSize(new Dimension(100, 46));

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
                    return new Dimension(46, 46);
                }

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    quality(g2);
                    int cx = 24;
                    int cy = getHeight() / 2;
                    g2.setColor(focused ? Theme.ACCENT : soft());
                    g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    if (icon == 0) {
                        g2.draw(new Ellipse2D.Double(cx - 4.5, cy - 8, 9, 9));
                        g2.draw(new Arc2D.Double(cx - 7.5, cy + 1.5, 15, 14, 0, 180, Arc2D.OPEN));
                    } else {
                        g2.drawRoundRect(cx - 6, cy - 1, 13, 10, 4, 4);
                        g2.draw(new Arc2D.Double(cx - 4, cy - 8.5, 8, 9.5, 0, 180, Arc2D.OPEN));
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
            g2.fillRoundRect(0, 0, w - 1, h - 1, 12, 12);
            if (focused) {
                g2.setColor(Theme.ACCENT);
                g2.setStroke(new BasicStroke(1.5f));
            } else {
                g2.setColor(line());
                g2.setStroke(new BasicStroke(1f));
            }
            g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class Banner extends JPanel {
        private final Supplier<Color> fill;
        private final Supplier<Color> tone;

        Banner(Supplier<Color> fill, Supplier<Color> tone, String glyph, JComponent content) {
            super(new BorderLayout(10, 0));
            this.fill = fill;
            this.tone = tone;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(11, 13, 11, 13));
            JComponent icon = new JComponent() {
                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(20, 20);
                }

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    quality(g2);
                    g2.setColor(tone.get());
                    g2.setStroke(new BasicStroke(1.7f));
                    g2.draw(new Ellipse2D.Double(1, 1, 17, 17));
                    g2.setFont(fb(11));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(glyph, 1 + (17 - fm.stringWidth(glyph)) / 2f + 0.5f,
                        1 + (17 - fm.getHeight()) / 2f + fm.getAscent());
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
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.setColor(new Color(t.getRed(), t.getGreen(), t.getBlue(), 45));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }
    }

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

    private static final class LoginButton extends JButton {
        private boolean hover;

        LoginButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setFont(fb(14.5f));
            setPreferredSize(new Dimension(100, 46));
            setMinimumSize(new Dimension(100, 46));
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
            g2.fillRoundRect(0, 0, w, h, 12, 12);
            g2.setColor(Color.WHITE);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(getText());
            g2.drawString(getText(), (w - textWidth) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
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
            return 18;
        }

        @Override
        public int getIconHeight() {
            return 18;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            if (checked) {
                g2.setColor(navy());
                g2.fillRoundRect(x + 1, y + 1, 16, 16, 5, 5);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawPolyline(new int[]{x + 4, x + 8, x + 13}, new int[]{y + 9, y + 13, y + 6}, 3);
            } else {
                g2.setColor(Theme.input());
                g2.fillRoundRect(x + 1, y + 1, 16, 16, 5, 5);
                g2.setColor(dk() ? new Color(71, 85, 119) : new Color(176, 186, 202));
                g2.setStroke(new BasicStroke(1.3f));
                g2.drawRoundRect(x + 1, y + 1, 16, 16, 5, 5);
            }
            g2.dispose();
        }
    }

    private static final class LoginEye implements Icon {
        @Override
        public int getIconWidth() {
            return 20;
        }

        @Override
        public int getIconHeight() {
            return 16;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            g2.setColor(soft());
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Arc2D.Double(x + 1, y + 2, 18, 11, 0, 180, Arc2D.OPEN));
            g2.draw(new Arc2D.Double(x + 1, y + 2, 18, 11, 180, 180, Arc2D.OPEN));
            g2.fill(new Ellipse2D.Double(x + 7.5, y + 5, 5, 5));
            if (!(c instanceof AbstractButton) || !((AbstractButton) c).isSelected()) {
                g2.drawLine(x + 3, y + 14, x + 17, y + 2);
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