package app;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;

import app.Components.*;
import static app.Theme.f;
import static app.Theme.fb;
import static app.Theme.quality;

/** Dialog bertema (pengganti JOptionPane) dengan ikon, aksen warna, dan tombol gaya aplikasi. */
final class AppDialog {
    static final int INFO = 0;
    static final int SUCCESS = 1;
    static final int WARN = 2;
    static final int ERROR = 3;
    static final int QUESTION = 4;

    private AppDialog() {
    }

    static void show(Component owner, int type, String title, String message) {
        open(owner, type, title, message, null, "OK");
    }

    static boolean confirm(Component owner, String title, String message, String yesText) {
        return open(owner, QUESTION, title, message, "Batal", yesText);
    }

    static void animateIn(JDialog dialog) {
        animateIn(dialog, null);
    }

    /**
     * Animasi muncul: dialog naik pelan sambil memudar masuk (±250 ms).
     * Panggil setelah posisi dialog ditentukan dan sebelum setVisible(true).
     */
    static void animateIn(JDialog dialog, java.util.function.Consumer<Float> onProgress) {
        final Point end = dialog.getLocation();
        final int rise = 26;
        boolean canFade;
        try {
            dialog.setOpacity(0f);
            canFade = true;
        } catch (Exception e) {
            canFade = false; // platform tidak mendukung transparansi jendela
        }
        final boolean fade = canFade;
        dialog.setLocation(end.x, end.y + rise);
        if (onProgress != null) {
            onProgress.accept(0f);
        }
        final long start = System.currentTimeMillis();
        final Timer timer = new Timer(12, null);
        timer.addActionListener(e -> {
            float k = Math.min(1f, (System.currentTimeMillis() - start) / 250f);
            float ease = 1f - (1f - k) * (1f - k) * (1f - k);
            if (dialog.isDisplayable()) {
                dialog.setLocation(end.x, end.y + Math.round(rise * (1f - ease)));
                if (fade) {
                    try {
                        dialog.setOpacity(Math.max(0.02f, ease));
                    } catch (Exception ignored) {
                        // abaikan
                    }
                }
            }
            if (onProgress != null) {
                onProgress.accept(k);
            }
            if (k >= 1f || !dialog.isDisplayable()) {
                timer.stop();
            }
        });
        timer.start();
    }

    private static Color accent(int type) {
        switch (type) {
            case SUCCESS: return new Color(34, 197, 94);
            case WARN:    return Theme.GOLD;
            case ERROR:   return new Color(239, 68, 68);
            case QUESTION:return new Color(168, 85, 247);
            default:      return new Color(59, 130, 246);
        }
    }

    private static boolean open(Component owner, int type, String title, String message,
                                String cancelText, String okText) {
        Window parent = owner == null ? null : SwingUtilities.getWindowAncestor(owner);
        if (parent == null && owner instanceof Window) {
            parent = (Window) owner;
        }
        JDialog dialog = new JDialog(parent, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        boolean[] result = {false};
        Color tone = accent(type);

        JPanel body = new JPanel(new BorderLayout(18, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                quality(g2);
                int w = getWidth();
                int h = getHeight();
                g2.setPaint(new GradientPaint(0, 0, Theme.heroA(), w, h, Theme.surface()));
                g2.fillRect(0, 0, w, h);
                g2.setPaint(new GradientPaint(0, 0, tone, w, 0, Theme.ACCENT));
                g2.fillRect(0, 0, w, 5);
                g2.setColor(Theme.border());
                g2.drawRect(0, 0, w - 1, h - 1);
                g2.dispose();
            }
        };
        body.setBorder(BorderFactory.createEmptyBorder(30, 26, 20, 26));

        float[] pop = {0f};
        JComponent badge = new JComponent() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(54, 54);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                quality(g2);
                // lencana "membal" saat dialog muncul
                double k = pop[0] - 1;
                double scale = Math.max(0.01, 1 + 2.70158 * k * k * k + 1.70158 * k * k);
                g2.translate(27, 27);
                g2.scale(scale, scale);
                g2.translate(-27, -27);
                g2.setColor(new Color(tone.getRed(), tone.getGreen(), tone.getBlue(), 45));
                g2.fill(new Ellipse2D.Double(0, 0, 54, 54));
                g2.setColor(tone);
                g2.fill(new Ellipse2D.Double(8, 8, 38, 38));
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                if (type == SUCCESS) {
                    g2.drawPolyline(new int[]{18, 25, 37}, new int[]{28, 35, 20}, 3);
                } else if (type == WARN || type == ERROR) {
                    g2.drawLine(27, 17, 27, 29);
                    g2.fill(new Ellipse2D.Double(25, 34, 4, 4));
                } else if (type == QUESTION) {
                    g2.setFont(fb(22f));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString("?", (54 - fm.stringWidth("?")) / 2, (54 - fm.getHeight()) / 2 + fm.getAscent());
                } else {
                    g2.drawLine(27, 25, 27, 37);
                    g2.fill(new Ellipse2D.Double(25, 15, 4, 4));
                }
                g2.dispose();
            }
        };
        JPanel badgeWrap = new JPanel(new BorderLayout());
        badgeWrap.setOpaque(false);
        badgeWrap.add(badge, BorderLayout.NORTH);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(fb(17));
        titleLabel.setForeground(Theme.text());

        JTextArea text = new JTextArea(message);
        text.setEditable(false);
        text.setFocusable(false);
        text.setOpaque(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setFont(f(13));
        text.setForeground(Theme.muted());
        text.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        text.setSize(new Dimension(330, 10));
        Dimension pref = text.getPreferredSize();
        text.setPreferredSize(new Dimension(330, pref.height));

        JPanel textBox = new JPanel(new BorderLayout(0, 8));
        textBox.setOpaque(false);
        textBox.add(titleLabel, BorderLayout.NORTH);
        textBox.add(text, BorderLayout.CENTER);

        AppButton ok = new AppButton(okText, true);
        ok.addActionListener(e -> {
            result[0] = true;
            dialog.dispose();
        });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        if (cancelText != null) {
            AppButton cancel = new AppButton(cancelText, false);
            cancel.addActionListener(e -> dialog.dispose());
            buttons.add(cancel);
        }
        buttons.add(ok);

        body.add(badgeWrap, BorderLayout.WEST);
        body.add(textBox, BorderLayout.CENTER);
        body.add(buttons, BorderLayout.SOUTH);

        // geser dialog dengan mouse (karena tanpa title bar)
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
        body.addMouseListener(drag);
        body.addMouseMotionListener(drag);

        dialog.setContentPane(body);
        dialog.getRootPane().setDefaultButton(ok);
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
        dialog.setLocationRelativeTo(parent);
        animateIn(dialog, k -> {
            pop[0] = k;
            badge.repaint();
        });
        dialog.setVisible(true);
        return result[0];
    }
}
