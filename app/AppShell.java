package app;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import app.Components.*;
import static app.Theme.f;
import static app.Theme.fb;
import static app.Theme.quality;
import static app.Ui.*;

/** Jendela utama SINA: sidebar penuh dengan sub-menu Data Guru tertutup (panah kanan) saat awal dibuka. */
final class AppShell {
    private JFrame frame;
    private final CardLayout rootCards = new CardLayout();
    private final JPanel root = new JPanel(rootCards);
    private final CardLayout pageCards = new CardLayout();
    private final JPanel pages = new JPanel(pageCards);
    private final Map<String, Page> registry = new LinkedHashMap<>();
    private LoginScreen login;

    private EdgePanel sidebar;
    private JLabel logoLabel;
    private JLabel appTitleLabel;
    private JLabel appDescLabel;
    private JLabel schoolSubLabel;
    private JPanel quickBar;
    private final List<TkaQuickButton> quickButtons = new ArrayList<>();
    private JPanel navList;
    private IconButton collapseButton;
    // Sidebar terbuka penuh saat awal aplikasi dibuka
    private boolean collapsed = false;
    private final List<NavButton> navButtons = new ArrayList<>();

    private static final class NavEntry {
        final NavButton button;
        final List<NavButton> children = new ArrayList<>();

        NavEntry(NavButton button) {
            this.button = button;
        }
    }

    private final List<NavEntry> navEntries = new ArrayList<>();
    private final List<JComponent> sectionHeaders = new ArrayList<>();

    private JLabel greetingLabel;
    private TimeIcon timeIcon;
    private String greetingName = "";
    private JLabel subGreetingLabel;
    private JLabel userNameLabel;
    private JLabel userRoleLabel;
    private JLabel avatarLabel;
    private AvatarIcon avatarIcon;
    private IconButton themeButton;
    private IconButton soundButton;
    private JPanel userChip;

    private String activeUsername = "";
    private String activeRole = "";

    JFrame frame() {
        return frame;
    }

    String role() {
        return activeRole;
    }

    String username() {
        return activeUsername;
    }

    void start() {
        frame = new JFrame("SINA - Sistem Informasi Nilai & Akademik | SMK Al Malikus Sholeh");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        pages.setOpaque(false);
        root.putClientProperty("role", "bg");
        login = new LoginScreen(this);
        root.add(login.view(), "login");
        root.add(buildShell(), "app");
        frame.setContentPane(root);

        Theme.apply(root);
        rootCards.show(root, "login");

        frame.setSize(1240, 750);
        frame.setMinimumSize(new Dimension(1000, 640));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.getRootPane().setDefaultButton(login.defaultButton());
    }

    private JPanel buildShell() {
        JPanel shell = p(new BorderLayout());
        sidebar = buildSidebar();

        register("dashboard", new DashboardPage(this));
        register("teachers", new TeachersPage(this));
        register("students", new StudentsPage(this));
        register("schedule", new SchedulePage(this));
        register("grades", new GradesPage(this));
        register("progress", new ProgressPage(this));
        register("sync", new SyncPage(this));
        register("mydata", new MyDataPage(this));
        register("myschedule", new StudentSchedulePage(this));

        Backdrop content = new Backdrop(new BorderLayout());
        content.add(pages, BorderLayout.CENTER);

        JPanel main = p(new BorderLayout());
        main.add(buildTopBar(), BorderLayout.NORTH);
        main.add(content, BorderLayout.CENTER);

        shell.add(sidebar, BorderLayout.WEST);
        shell.add(main, BorderLayout.CENTER);
        return shell;
    }

    private void register(String key, Page page) {
        registry.put(key, page);
        pages.add(page.view(), key);
    }

    void enter(String role, String username) {
        activeUsername = username;
        activeRole = role;
        refreshUser();

        // Sidebar tampil penuh, dan grup Data Guru mulai dalam keadaan tertutup (panah kanan '>')
        collapsed = false;
        applySidebarState();
        buildNav(role);

        frame.getRootPane().setDefaultButton(null);
        rootCards.show(root, "app");
        showPage("dashboard");
        frame.revalidate();
        frame.repaint();
    }

    void refreshUser() {
        String name = DataStore.displayName(activeRole, activeUsername);
        greetingName = name.trim().split("\\s+")[0];
        updateGreeting();
        subGreetingLabel.setText("SMK Al Malikus Sholeh \u00B7 " + activeRole);
        userNameLabel.setText(name);
        userRoleLabel.setText(activeRole);
        avatarIcon.initials = "";
        avatarIcon.photo = DataStore.photoOf(activeRole, activeUsername);
        avatarLabel.repaint();
    }

    private void updateGreeting() {
        if (activeRole.isEmpty()) {
            return;
        }
        greetingLabel.setText(greeting() + ", " + greetingName);
        timeIcon.repaint();
    }

    void showPage(String key) {
        DataStore.reload();
        for (NavEntry entry : navEntries) {
            entry.button.active = entry.button.key.equals(key);
            boolean childOn = false;
            for (NavButton c : entry.children) {
                c.active = c.key.equals(key);
                childOn |= c.active;
            }
            if (childOn) {
                entry.button.expanded = true;
            }
            entry.button.childActive = childOn;
        }
        rebuildNavList();
        Page page = registry.get(key);
        if (page != null) {
            page.onShow();
        }
        pageCards.show(pages, key);
    }

    private void confirmLogout() {
        if (!AppDialog.confirm(frame, "Konfirmasi Logout", "Anda yakin ingin keluar dari akun ini?", "Logout")) {
            return;
        }
        for (Page page : registry.values()) {
            page.reset();
        }
        login.reset();
        activeUsername = "";
        activeRole = "";
        rootCards.show(root, "login");
        frame.getRootPane().setDefaultButton(login.defaultButton());
    }

    // ===================== SIDEBAR SINA =====================
    private EdgePanel buildSidebar() {
        EdgePanel sb = new EdgePanel(new BorderLayout(0, 0), true, true);
        sb.setPreferredSize(new Dimension(collapsed ? 78 : 255, 0));

        JPanel topTkaHeader = p(new BorderLayout(0, 0));

        JPanel brandBox = new JPanel();
        brandBox.setOpaque(false);
        brandBox.setLayout(new BoxLayout(brandBox, BoxLayout.Y_AXIS));
        brandBox.setBorder(BorderFactory.createEmptyBorder(14, 8, 12, 8));

        logoLabel = new JLabel(logoIcon(collapsed ? 38 : 62));
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        appTitleLabel = label("SINA", fb(15), "title");
        appTitleLabel.setForeground(new Color(41, 128, 185));
        appTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        appTitleLabel.setVisible(!collapsed);

        appDescLabel = label("Sistem Informasi Nilai & Akademik", f(10), "muted");
        appDescLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        appDescLabel.setVisible(!collapsed);

        schoolSubLabel = label("SMK AL MALIKUS SHOLEH", fb(9.5f), "muted");
        schoolSubLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        schoolSubLabel.setVisible(!collapsed);

        brandBox.add(logoLabel);
        brandBox.add(Box.createVerticalStrut(4));
        brandBox.add(appTitleLabel);
        brandBox.add(Box.createVerticalStrut(1));
        brandBox.add(appDescLabel);
        brandBox.add(Box.createVerticalStrut(2));
        brandBox.add(schoolSubLabel);

        quickBar = new JPanel(new GridLayout(collapsed ? 3 : 1, collapsed ? 1 : 3, 2, 2)) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(44, 62, 80));
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        quickBar.setOpaque(false);
        quickBar.setBorder(BorderFactory.createEmptyBorder(5, 6, 5, 6));

        quickButtons.clear();
        TkaQuickButton bHome = new TkaQuickButton("Dasbor", "home", () -> showPage("dashboard"));
        TkaQuickButton bGear = new TkaQuickButton("Pengaturan", "gear", () -> ProfileDialog.show(this));
        TkaQuickButton bExit = new TkaQuickButton("Keluar", "exit", this::confirmLogout);
        quickButtons.add(bHome);
        quickButtons.add(bGear);
        quickButtons.add(bExit);
        for (TkaQuickButton qb : quickButtons) {
            qb.collapsed = collapsed;
            qb.setPreferredSize(new Dimension(collapsed ? 60 : 70, collapsed ? 32 : 50));
            qb.setToolTipText(qb.getText());
            quickBar.add(qb);
        }

        topTkaHeader.add(brandBox, BorderLayout.CENTER);
        topTkaHeader.add(quickBar, BorderLayout.SOUTH);

        navList = p(new GridLayout(0, 1, 0, 4));
        navList.setBorder(BorderFactory.createEmptyBorder(8, 10, 10, 10));
        JPanel navWrap = p(new BorderLayout());
        navWrap.add(navList, BorderLayout.NORTH);

        JScrollPane navScroll = new JScrollPane(navWrap);
        navScroll.setBorder(null);
        navScroll.setOpaque(false);
        navScroll.getViewport().setOpaque(false);
        navScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        collapseButton = new IconButton(collapsed ? "chevR" : "chevL");
        collapseButton.setToolTipText(collapsed ? "Perluas sidebar" : "Perkecil sidebar");
        collapseButton.setPreferredSize(new Dimension(40, 34));
        collapseButton.addActionListener(e -> toggleSidebar());
        JPanel bottom = p(new BorderLayout());
        bottom.setBorder(BorderFactory.createEmptyBorder(4, 10, 10, 10));
        bottom.add(collapseButton, BorderLayout.CENTER);

        sb.add(topTkaHeader, BorderLayout.NORTH);
        sb.add(navScroll, BorderLayout.CENTER);
        sb.add(bottom, BorderLayout.SOUTH);
        return sb;
    }

    private void toggleSidebar() {
        collapsed = !collapsed;
        applySidebarState();
        rebuildNavList();
        sidebar.revalidate();
        if (sidebar.getParent() != null) {
            sidebar.getParent().revalidate();
        }
        sidebar.repaint();
        root.revalidate();
        SwingUtilities.invokeLater(() -> Theme.pokeScrollBars(root));
    }

    private void applySidebarState() {
        sidebar.setPreferredSize(new Dimension(collapsed ? 78 : 255, 0));
        logoLabel.setIcon(logoIcon(collapsed ? 38 : 62));
        appTitleLabel.setVisible(!collapsed);
        appDescLabel.setVisible(!collapsed);
        schoolSubLabel.setVisible(!collapsed);

        quickBar.setLayout(new GridLayout(collapsed ? 3 : 1, collapsed ? 1 : 3, 2, 2));
        for (TkaQuickButton qb : quickButtons) {
            qb.collapsed = collapsed;
            qb.setPreferredSize(new Dimension(collapsed ? 60 : 70, collapsed ? 32 : 50));
            qb.repaint();
        }

        for (JComponent sec : sectionHeaders) {
            sec.setVisible(!collapsed);
        }
        for (NavButton b : navButtons) {
            b.collapsed = collapsed;
            b.setToolTipText(collapsed ? b.getText() : null);
        }
        collapseButton.setType(collapsed ? "chevR" : "chevL");
        collapseButton.setToolTipText(collapsed ? "Perluas sidebar" : "Perkecil sidebar");
    }

    private EdgePanel buildTopBar() {
        EdgePanel bar = new EdgePanel(new BorderLayout(), false, false);
        bar.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));

        greetingLabel = label("Selamat datang", fb(15), "title");
        subGreetingLabel = label("SMK Al Malikus Sholeh", f(12), "muted");
        JPanel greetText = p(new GridLayout(0, 1, 0, 0));
        greetText.add(greetingLabel);
        greetText.add(subGreetingLabel);
        timeIcon = new TimeIcon();
        JPanel greet = p(new BorderLayout(12, 0));
        greet.add(timeIcon, BorderLayout.WEST);
        greet.add(greetText, BorderLayout.CENTER);
        new Timer(30_000, e -> updateGreeting()).start();

        themeButton = new IconButton(Theme.dark ? "sun" : "moon");
        themeButton.setToolTipText("Ganti tema terang/gelap");
        themeButton.addActionListener(e -> toggleTheme());

        soundButton = new IconButton(Theme.sound ? "sound" : "mute");
        soundButton.setToolTipText("Suara aktif/senyap");
        soundButton.addActionListener(e -> {
            Theme.setSound(!Theme.sound);
            soundButton.setType(Theme.sound ? "sound" : "mute");
        });

        Divider divider = new Divider();

        avatarIcon = new AvatarIcon("", 38, new Color(147, 197, 253), new Color(15, 23, 42));
        avatarLabel = new JLabel(avatarIcon);
        userNameLabel = label("Pengguna", fb(13), "title");
        userRoleLabel = label("Peran", f(11), "muted");
        JPanel userText = p(new GridLayout(0, 1, 0, 0));
        userText.add(userNameLabel);
        userText.add(userRoleLabel);

        IconButton arrow = new IconButton("chevD");
        arrow.setPreferredSize(new Dimension(26, 26));
        arrow.addActionListener(e -> showUserMenu());

        userChip = p(new BorderLayout(10, 0));
        userChip.add(avatarLabel, BorderLayout.WEST);
        userChip.add(userText, BorderLayout.CENTER);
        userChip.add(arrow, BorderLayout.EAST);
        userChip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        userChip.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showUserMenu();
            }
        });

        JPanel right = p(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        right.add(soundButton);
        right.add(themeButton);
        right.add(divider);
        right.add(userChip);

        bar.add(greet, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    void toggleTheme() {
        Theme.setDark(!Theme.dark);
        themeButton.setType(Theme.dark ? "sun" : "moon");
        Theme.apply(root);
        login.refreshTheme();
        frame.repaint();
    }

    private void showUserMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(Theme.surface());
        menu.setBorder(new LineBorder(Theme.border(), 1));
        JMenuItem profil = menuItem("Profil");
        profil.addActionListener(e -> ProfileDialog.show(this));
        JMenuItem logout = menuItem("Logout");
        logout.addActionListener(e -> confirmLogout());
        menu.add(profil);
        menu.add(logout);
        menu.show(userChip, 0, userChip.getHeight() + 6);
    }

    private JMenuItem menuItem(String text) {
        JMenuItem item = new JMenuItem(text);
        item.setOpaque(true);
        item.setBackground(Theme.surface());
        item.setForeground(Theme.text());
        item.setFont(f(13));
        item.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 24));
        return item;
    }

    // ===================== MENU PER PERAN =====================
    private void buildNav(String role) {
        navButtons.clear();
        navEntries.clear();
        sectionHeaders.clear();
        if ("Operator".equals(role)) {
            addNav("Dasbor", 0, "dashboard");
            NavEntry guru = addGroup("Data Guru", 2);
            addChild(guru, "Akun Guru", 2, "teachers");
            addChild(guru, "Progres Guru", 4, "progress");
            addNav("Data Siswa", 1, "students");
            addNav("Jadwal Pelajaran", 3, "schedule");
            addNav("Sinkronisasi Data Induk", 5, "sync");
        } else if ("Guru".equals(role)) {
            addNav("Dasbor", 0, "dashboard");
            addNav("Nilai UTS Siswa", 1, "grades");
        } else {
            addNav("Dasbor", 0, "dashboard");
            addNav("Data Saya", 2, "mydata");
            addNav("Jadwal Pelajaran", 3, "myschedule");
        }
        rebuildNavList();
    }

    private NavButton newNavButton(String text, int icon, String key) {
        NavButton button = new NavButton(text, icon, key);
        button.collapsed = collapsed;
        button.setToolTipText(collapsed ? text : null);
        navButtons.add(button);
        return button;
    }

    private void addNav(String text, int icon, String key) {
        NavButton button = newNavButton(text, icon, key);
        button.addActionListener(e -> showPage(key));
        navEntries.add(new NavEntry(button));
    }

    private NavEntry addGroup(String text, int icon) {
        NavButton header = newNavButton(text, icon, "group:" + text);
        header.group = true;
        // Mulai dalam posisi tertutup (tanda panah mengarah ke kanan '>')
        header.expanded = false;
        header.addActionListener(e -> {
            header.expanded = !header.expanded;
            rebuildNavList();
        });
        NavEntry entry = new NavEntry(header);
        navEntries.add(entry);
        return entry;
    }

    private void addChild(NavEntry group, String text, int icon, String key) {
        NavButton button = newNavButton(text, icon, key);
        button.child = true;
        button.addActionListener(e -> showPage(key));
        group.children.add(button);
    }

    private JPanel makeSectionHeader(String title) {
        JPanel sec = p(new BorderLayout());
        sec.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        JLabel lbl = label(title, fb(10), "muted");
        sec.add(lbl, BorderLayout.WEST);
        sec.setVisible(!collapsed);
        sectionHeaders.add(sec);
        return sec;
    }

    private void rebuildNavList() {
        navList.removeAll();
        sectionHeaders.clear();
        if (!collapsed) {
            navList.add(makeSectionHeader("MENU UTAMA"));
        }
        for (NavEntry entry : navEntries) {
            if (entry.children.isEmpty()) {
                navList.add(entry.button);
                continue;
            }
            if (!collapsed) {
                navList.add(entry.button);
            }
            if (collapsed || entry.button.expanded) {
                for (NavButton c : entry.children) {
                    navList.add(c);
                }
            }
        }
        if (!collapsed) {
            navList.add(makeSectionHeader("MANAJEMEN PENGGUNA"));
        }
        NavButton akunBtn = new NavButton("Pengaturan Akun", 2, "account");
        akunBtn.collapsed = collapsed;
        akunBtn.setToolTipText(collapsed ? "Pengaturan Akun" : null);
        akunBtn.addActionListener(e -> ProfileDialog.show(this));
        navList.add(akunBtn);

        navList.revalidate();
        navList.repaint();
    }

    private static final class TkaQuickButton extends JButton {
        private final String iconType;
        boolean collapsed;
        private boolean hover;

        TkaQuickButton(String text, String iconType, Runnable action) {
            super(text);
            this.iconType = iconType;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setForeground(Color.WHITE);
            setFont(f(11));
            setPreferredSize(new Dimension(70, 50));
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
            addActionListener(e -> action.run());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            int w = getWidth();
            int h = getHeight();
            if (hover) {
                g2.setColor(new Color(255, 255, 255, 28));
                g2.fillRoundRect(2, 2, w - 4, h - 4, 8, 8);
            }
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = w / 2;
            int iy = collapsed ? (h - 14) / 2 : 8;
            if ("home".equals(iconType)) {
                g2.drawPolygon(new int[]{cx - 8, cx, cx + 8}, new int[]{iy + 8, iy + 1, iy + 8}, 3);
                g2.drawRect(cx - 5, iy + 8, 10, 7);
            } else if ("gear".equals(iconType)) {
                g2.drawOval(cx - 3, iy + 4, 6, 6);
                g2.drawOval(cx - 7, iy, 14, 14);
            } else {
                g2.drawRect(cx - 7, iy + 1, 8, 13);
                g2.drawLine(cx - 1, iy + 7, cx + 8, iy + 7);
                g2.drawLine(cx + 5, iy + 4, cx + 8, iy + 7);
                g2.drawLine(cx + 5, iy + 10, cx + 8, iy + 7);
            }
            if (!collapsed) {
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (w - fm.stringWidth(getText())) / 2, h - 7);
            }
            g2.dispose();
        }
    }
}