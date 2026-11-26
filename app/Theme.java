package app;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.JTableHeader;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.prefs.Preferences;

/** Warna tema terang/gelap, font (Poppins bila tersedia), dan penerapan tema ke komponen. */
final class Theme {
    private Theme() {
    }

    // ===================== WARNA =====================
    private static final Preferences PREFS = Preferences.userNodeForPackage(Theme.class);

    /** Tema aktif. Dibaca dari penyimpanan, jadi pilihan terakhir tetap ada walau aplikasi ditutup. */
    static boolean dark = PREFS.getBoolean("dark", true);

    /** Suara aktif/senyap. Disimpan juga, supaya pilihan terakhir tetap ada. */
    static boolean sound = PREFS.getBoolean("sound", true);

    static void setSound(boolean value) {
        sound = value;
        try {
            PREFS.putBoolean("sound", value);
            PREFS.flush();
        } catch (Exception ignored) {
            // abaikan bila penyimpanan tidak tersedia
        }
    }

    /** Ganti tema sekaligus menyimpannya. */
    static void setDark(boolean value) {
        dark = value;
        try {
            PREFS.putBoolean("dark", value);
            PREFS.flush();
        } catch (Exception ignored) {
            // penyimpanan preferensi tidak tersedia; tema tetap berlaku selama aplikasi berjalan
        }
    }

    static final Color ACCENT = new Color(37, 99, 235);
    static final Color GOLD = new Color(250, 204, 21);

    static Color bg()         { return dark ? new Color(11, 17, 33)    : new Color(240, 244, 250); }
    static Color sidebar()    { return dark ? new Color(15, 23, 42)    : Color.WHITE; }
    static Color surface()    { return dark ? new Color(20, 29, 54)    : Color.WHITE; }
    static Color input()      { return dark ? new Color(13, 20, 40)    : new Color(236, 240, 247); }
    static Color inputBorder(){ return dark ? new Color(52, 68, 108)   : new Color(150, 164, 192); }
    static Color border()     { return dark ? new Color(38, 51, 84)    : new Color(220, 227, 238); }
    static Color text()       { return dark ? new Color(240, 245, 255) : new Color(22, 32, 56); }
    static Color muted()      { return dark ? new Color(148, 163, 194) : new Color(100, 116, 145); }
    static Color accentSoft() { return dark ? new Color(30, 50, 98)    : new Color(223, 234, 255); }
    static Color accentText() { return dark ? new Color(96, 165, 250)  : new Color(37, 99, 235); }
    static Color hover()      { return dark ? new Color(24, 35, 64)    : new Color(237, 242, 250); }
    static Color heroA()      { return dark ? new Color(27, 44, 90)    : new Color(226, 237, 255); }
    static Color heroB()      { return dark ? new Color(16, 25, 50)    : Color.WHITE; }

    // ===================== FONT (Poppins bila tersedia) =====================
    static Font baseRegular;
    static Font baseBold;

    static Font f(float size)  { return baseRegular.deriveFont(size); }
    static Font fb(float size) { return baseBold.deriveFont(size); }
    static Font fi(float size) { return baseRegular.deriveFont(Font.ITALIC, size); }

    static Font tryLoad(String path) {
        try {
            File file = new File(path);
            if (!file.exists()) {
                return null;
            }
            Font font = Font.createFont(Font.TRUETYPE_FONT, file);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
            return font;
        } catch (Exception e) {
            return null;
        }
    }

    static String pickFamily(String... candidates) {
        Set<String> installed = new HashSet<>();
        Collections.addAll(installed,
            GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String name : candidates) {
            if (installed.contains(name)) {
                return name;
            }
        }
        return Font.SANS_SERIF;
    }

    static void initFonts() {
        baseRegular = tryLoad("fonts/Poppins-Regular.ttf");
        baseBold = tryLoad("fonts/Poppins-SemiBold.ttf");
        if (baseRegular == null) {
            String family = pickFamily("Poppins", "Segoe UI", "Inter", "Helvetica Neue", "Ubuntu", "Noto Sans");
            baseRegular = new Font(family, Font.PLAIN, 13);
            baseBold = new Font(family, Font.BOLD, 13);
        } else if (baseBold == null) {
            baseBold = baseRegular.deriveFont(Font.BOLD);
        }
        for (Object key : Collections.list(UIManager.getDefaults().keys())) {
            if (UIManager.get(key) instanceof FontUIResource) {
                UIManager.put(key, new FontUIResource(f(13)));
            }
        }
    }

    static void quality(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    // ===================== KOTAK INPUT & SCROLLBAR =====================
    /** Bingkai kotak input: garis jelas saat diam, garis aksen tebal saat sedang diisi. */
    static Border fieldBorder(boolean focused) {
        return BorderFactory.createCompoundBorder(
            new LineBorder(focused ? ACCENT : inputBorder(), focused ? 2 : 1),
            BorderFactory.createEmptyBorder(focused ? 7 : 8, focused ? 9 : 10, focused ? 7 : 8, focused ? 9 : 10)
        );
    }

    /** Memasang scrollbar tipis yang memudar. Aman dipanggil berulang kali. */
    static void installModernScroll(JScrollPane sp) {
        if (sp.getClientProperty("modernScroll") == null) {
            sp.putClientProperty("modernScroll", Boolean.TRUE);
            for (JScrollBar bar : new JScrollBar[]{sp.getVerticalScrollBar(), sp.getHorizontalScrollBar()}) {
                bar.setUI(new ModernScrollBarUI());
                bar.setOpaque(false);
                bar.setUnitIncrement(18);
            }
            MouseMotionAdapter hover = new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    ModernScrollBarUI.poke(sp.getVerticalScrollBar());
                    ModernScrollBarUI.poke(sp.getHorizontalScrollBar());
                }
            };
            sp.addMouseMotionListener(hover);
            sp.getViewport().addMouseMotionListener(hover);
            Component view = sp.getViewport().getView();
            if (view != null) {
                view.addMouseMotionListener(hover);
            }
            ModernScrollBarUI.poke(sp.getVerticalScrollBar());
        }
        // Jalur vertikal selalu disediakan supaya tata letak tidak bergeser saat lebar halaman berubah.
        sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    }

    /** Menampilkan kembali semua scrollbar sesaat (dipakai setelah ukuran area berubah). */
    static void pokeScrollBars(Component c) {
        if (c instanceof JScrollPane) {
            ModernScrollBarUI.poke(((JScrollPane) c).getVerticalScrollBar());
            ModernScrollBarUI.poke(((JScrollPane) c).getHorizontalScrollBar());
        }
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                pokeScrollBars(child);
            }
        }
    }

    // ===================== PENERAPAN TEMA =====================
    static void apply(Component c) {
        if (c instanceof JComponent && Boolean.TRUE.equals(((JComponent) c).getClientProperty("noTheme"))) {
            return; // bagian ini punya warna sendiri (mis. halaman login)
        }
        Object role = (c instanceof JComponent) ? ((JComponent) c).getClientProperty("role") : null;

        if ("bg".equals(role)) {
            c.setBackground(bg());
        }

        if (c instanceof JLabel) {
            Color col = text();
            if ("muted".equals(role)) col = muted();
            else if ("gold".equals(role)) col = GOLD;
            else if ("accent".equals(role)) col = accentText();
            c.setForeground(col);
        } else if (c instanceof JScrollPane) {
            JScrollPane sp = (JScrollPane) c;
            if ("clear".equals(role)) {
                sp.setBorder(null);
                sp.setOpaque(false);
                sp.getViewport().setOpaque(false);
            } else {
                sp.setBorder(new LineBorder(inputBorder(), 1));
                sp.setBackground(input());
                sp.getViewport().setBackground(input());
            }
            installModernScroll(sp);
        } else if (c instanceof JSplitPane) {
            c.setBackground(surface());
        } else if (c instanceof JTable) {
            JTable t = (JTable) c;
            t.setBackground(input());
            t.setForeground(text());
            t.setGridColor(border());
            t.setSelectionBackground(accentSoft());
            t.setSelectionForeground(text());
        } else if (c instanceof JTableHeader) {
            c.setBackground(surface());
            c.setForeground(muted());
            c.setFont(fb(12));
        } else if (c instanceof JTextComponent) {
            JTextComponent tc = (JTextComponent) c;
            tc.setBackground(input());
            tc.setForeground(text());
            tc.setCaretColor(text());
            if ("plain".equals(role)) {
                tc.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
            } else {
                tc.setBorder(fieldBorder(tc.hasFocus()));
                if (tc.getClientProperty("focusHook") == null) {
                    tc.putClientProperty("focusHook", Boolean.TRUE);
                    tc.addFocusListener(new FocusAdapter() {
                        @Override
                        public void focusGained(FocusEvent e) {
                            tc.setBorder(fieldBorder(true));
                        }

                        @Override
                        public void focusLost(FocusEvent e) {
                            tc.setBorder(fieldBorder(false));
                        }
                    });
                }
            }
        } else if (c instanceof JList) {
            c.setBackground(input());
            c.setForeground(text());
            ((JList<?>) c).setSelectionBackground(accentSoft());
            ((JList<?>) c).setSelectionForeground(accentText());
        } else if (c instanceof JComboBox) {
            c.setBackground(input());
            c.setForeground(text());
            ((JComboBox<?>) c).setBorder(new LineBorder(inputBorder(), 1));
        }

        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                apply(child);
            }
        }
    }
}
