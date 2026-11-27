package app;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterException;
import java.text.MessageFormat;

import static app.Theme.f;
import static app.Theme.fb;
import static app.Theme.quality;
import static app.Ui.*;

/** Komponen tampilan kustom: tombol, kartu, dialog modern, ikon vektor, cetak tabel, dan baris daftar. */
final class Components {
    private Components() {
    }

    /** Mencetak JTable ke printer atau Simpan sebagai PDF dengan header dan footer rapi. */
    static void printTable(Component parent, JTable table, String title, String subtitle) {
        try {
            MessageFormat header = new MessageFormat(title);
            MessageFormat footer = new MessageFormat(subtitle + "  |  Halaman {0}");
            boolean done = table.print(JTable.PrintMode.FIT_WIDTH, header, footer, true, null, true);
            if (done) {
                AppDialog.show(parent, AppDialog.SUCCESS, "Cetak Selesai", "Dokumen berhasil dikirim ke printer / PDF.");
            }
        } catch (PrinterException ex) {
            AppDialog.show(parent, AppDialog.ERROR, "Gagal Mencetak", "Terjadi kesalahan saat mencetak: " + ex.getMessage());
        }
    }

    /** Membuat dialog modal kustom tanpa bingkai OS agar serasi dengan tema gelap & terang. */
    static JDialog modalDialog(Window owner, String badge, String title, String subtitle,
                                Color tone, JComponent content, JPanel footer, int width, int height) {
        JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);

        DialogBody body = new DialogBody(new BorderLayout(0, 0), tone);

        JPanel headerWrap = p(new BorderLayout(0, 12));
        headerWrap.setBorder(BorderFactory.createEmptyBorder(20, 24, 0, 24));
        headerWrap.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));

        final Point[] dragOffset = {null};
        headerWrap.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragOffset[0] = e.getPoint();
            }
        });
        headerWrap.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragOffset[0] != null) {
                    Point loc = dialog.getLocation();
                    dialog.setLocation(loc.x + e.getX() - dragOffset[0].x, loc.y + e.getY() - dragOffset[0].y);
                }
            }
        });

        JPanel topRow = p(new BorderLayout(12, 0));

        JLabel badgeLbl = new JLabel(badge) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                quality(g2);
                g2.setColor(new Color(tone.getRed(), tone.getGreen(), tone.getBlue(), 34));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.setColor(new Color(tone.getRed(), tone.getGreen(), tone.getBlue(), 110));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badgeLbl.setFont(fb(10));
        badgeLbl.setForeground(tone);
        badgeLbl.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        JPanel badgeHold = p(new FlowLayout(FlowLayout.LEFT, 0, 0));
        badgeHold.add(badgeLbl);

        JButton closeBtn = new JButton() {
            private boolean hover;

            {
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setOpaque(false);
                setPreferredSize(new Dimension(32, 32));
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
                if (hover) {
                    g2.setColor(new Color(239, 68, 68, 45));
                    g2.fillRoundRect(0, 0, w, h, 10, 10);
                }
                g2.setColor(hover ? new Color(248, 113, 113) : Theme.muted());
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = w / 2;
                int cy = h / 2;
                int r = 5;
                g2.drawLine(cx - r, cy - r, cx + r, cy + r);
                g2.drawLine(cx + r, cy - r, cx - r, cy + r);
                g2.dispose();
            }
        };
        closeBtn.setToolTipText("Tutup (Esc)");
        closeBtn.addActionListener(e -> dialog.dispose());

        topRow.add(badgeHold, BorderLayout.WEST);
        topRow.add(closeBtn, BorderLayout.EAST);

        JPanel titleBox = p(new GridLayout(0, 1, 0, 4));
        titleBox.add(label(title, fb(19), "title"));
        if (subtitle != null && !subtitle.isEmpty()) {
            titleBox.add(label(subtitle, f(12), tone == Theme.GOLD ? "gold" : "muted"));
        }

        JPanel headContent = p(new BorderLayout(0, 8));
        headContent.add(topRow, BorderLayout.NORTH);
        headContent.add(titleBox, BorderLayout.CENTER);

        headerWrap.add(headContent, BorderLayout.CENTER);
        headerWrap.add(new HLine(), BorderLayout.SOUTH);

        JPanel centerWrap = p(new BorderLayout());
        centerWrap.setBorder(BorderFactory.createEmptyBorder(14, 24, footer == null ? 22 : 14, 24));
        centerWrap.add(content, BorderLayout.CENTER);

        body.add(headerWrap, BorderLayout.NORTH);
        body.add(centerWrap, BorderLayout.CENTER);

        if (footer != null) {
            JPanel southWrap = p(new BorderLayout(0, 14));
            southWrap.setBorder(BorderFactory.createEmptyBorder(0, 24, 20, 24));
            southWrap.add(new HLine(), BorderLayout.NORTH);
            southWrap.add(footer, BorderLayout.CENTER);
            body.add(southWrap, BorderLayout.SOUTH);
        }

        dialog.getRootPane().registerKeyboardAction(
            e -> dialog.dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        dialog.setContentPane(body);
        Theme.apply(body);
        dialog.setSize(width, height);
        dialog.setLocationRelativeTo(owner);
        return dialog;
    }

    /** Ikon vektor murni Graphics2D agar tajam dan bebas kotak kosong. */
    static class VectorIcon implements Icon {
        private final String type;
        private final int size;
        private final Color color;

        VectorIcon(String type, int size, Color color) {
            this.type = type;
            this.size = size;
            this.color = color;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            g2.setColor(color != null ? color : Theme.text());
            g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int s = size;

            switch (type) {
                case "info": {
                    g2.drawOval(x + 1, y + 1, s - 2, s - 2);
                    int m = x + s / 2;
                    g2.fillOval(m - 1, y + 4, 2, 2);
                    g2.drawLine(m, y + 7, m, y + s - 4);
                    break;
                }
                case "table": {
                    g2.drawRoundRect(x + 1, y + 2, s - 2, s - 4, 3, 3);
                    g2.drawLine(x + 1, y + 6, x + s - 1, y + 6);
                    g2.drawLine(x + 1, y + 10, x + s - 1, y + 10);
                    g2.drawLine(x + s / 2, y + 6, x + s / 2, y + s - 2);
                    break;
                }
                case "plus": {
                    g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int m = s / 2;
                    g2.drawLine(x + m, y + 2, x + m, y + s - 2);
                    g2.drawLine(x + 2, y + m, x + s - 2, y + m);
                    break;
                }
                case "pencil": {
                    int[] px = {x + s - 5, x + s - 2, x + 6, x + 2, x + 3};
                    int[] py = {y + 2, y + 5, y + s - 3, y + s - 2, y + s - 6};
                    g2.drawPolygon(px, py, 5);
                    g2.drawLine(x + s - 7, y + 4, x + s - 4, y + 7);
                    break;
                }
                case "trash": {
                    g2.drawLine(x + 2, y + 4, x + s - 2, y + 4);
                    g2.drawRect(x + s / 2 - 2, y + 2, 4, 2);
                    g2.drawRect(x + 4, y + 4, s - 8, s - 6);
                    g2.drawLine(x + s / 2 - 2, y + 7, x + s / 2 - 2, y + s - 4);
                    g2.drawLine(x + s / 2 + 2, y + 7, x + s / 2 + 2, y + s - 4);
                    break;
                }
                case "search": {
                    g2.drawOval(x + 2, y + 2, s - 6, s - 6);
                    g2.drawLine(x + s - 5, y + s - 5, x + s - 2, y + s - 2);
                    break;
                }
                case "refresh": {
                    g2.drawArc(x + 2, y + 2, s - 4, s - 4, 35, 290);
                    g2.drawLine(x + s - 3, y + 2, x + s - 3, y + 6);
                    g2.drawLine(x + s - 7, y + 6, x + s - 3, y + 6);
                    break;
                }
                case "print": {
                    g2.drawRect(x + 4, y + 1, s - 8, 4);
                    g2.drawRoundRect(x + 1, y + 5, s - 2, s - 8, 3, 3);
                    g2.setColor(Theme.surface());
                    g2.fillRect(x + 4, y + s - 6, s - 8, 5);
                    g2.setColor(color != null ? color : Theme.text());
                    g2.drawRect(x + 4, y + s - 6, s - 8, 5);
                    break;
                }
                default:
                    break;
            }
            g2.dispose();
        }
    }

    static class Backdrop extends JPanel {
        Backdrop(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(Theme.bg());
            g2.fillRect(0, 0, w, h);
            int alpha = Theme.dark ? 70 : 28;
            g2.setPaint(new RadialGradientPaint(
                new Point2D.Float(w * 0.12f, 0f), 560f,
                new float[]{0f, 1f},
                new Color[]{new Color(37, 99, 235, alpha), new Color(37, 99, 235, 0)}
            ));
            g2.fillRect(0, 0, w, h);
            if (Theme.dark) {
                g2.setPaint(new RadialGradientPaint(
                    new Point2D.Float(w, h), 420f,
                    new float[]{0f, 1f},
                    new Color[]{new Color(250, 160, 40, 34), new Color(250, 160, 40, 0)}
                ));
                g2.fillRect(0, 0, w, h);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class EdgePanel extends JPanel {
        private final boolean sidebarFill;
        private final boolean rightEdge;

        EdgePanel(LayoutManager layout, boolean sidebarFill, boolean rightEdge) {
            super(layout);
            this.sidebarFill = sidebarFill;
            this.rightEdge = rightEdge;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(sidebarFill ? Theme.sidebar() : Theme.bg());
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Theme.border());
            if (rightEdge) {
                g.fillRect(getWidth() - 1, 0, 1, getHeight());
            } else {
                g.fillRect(0, getHeight() - 1, getWidth(), 1);
            }
            super.paintComponent(g);
        }
    }

    static class RoundPanel extends JPanel {
        private final int arc;
        private final boolean gradient;

        RoundPanel(int arc, boolean gradient) {
            this.arc = arc;
            this.gradient = gradient;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            int w = getWidth();
            int h = getHeight();
            if (gradient) {
                g2.setPaint(new GradientPaint(0, 0, Theme.heroA(), w, h, Theme.heroB()));
            } else {
                g2.setColor(Theme.surface());
            }
            g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
            g2.setColor(Theme.border());
            g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class AppButton extends JButton {
        private final boolean primary;
        private boolean hover;
        private boolean danger;
        private boolean compact;

        void setDanger(boolean danger) {
            this.danger = danger;
            repaint();
        }

        AppButton compact() {
            compact = true;
            setFont(fb(12));
            return this;
        }

        AppButton(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setFont(fb(13));
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
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(d.width + (compact ? 16 : 24), compact ? 34 : 42);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            int w = getWidth();
            int h = getHeight();
            Color fill = primary
                ? (hover ? new Color(59, 115, 246) : Theme.ACCENT)
                : (hover ? Theme.accentSoft() : Theme.input());
            if (danger) {
                fill = new Color(239, 68, 68, hover ? 70 : 38);
            }
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, w, h, 12, 12);
            if (!primary) {
                g2.setColor(danger ? new Color(239, 68, 68, 140) : Theme.border());
                g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);
            }
            g2.setColor(primary ? Color.WHITE : (danger ? new Color(248, 113, 113) : Theme.text()));
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(getText(), (w - fm.stringWidth(getText())) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    static class NavButton extends JButton {
        final String key;
        private final int icon;
        boolean active;
        boolean collapsed;
        boolean group;
        boolean expanded = true;
        boolean childActive;
        boolean child;
        private boolean hover;

        NavButton(String text, int icon, String key) {
            super(text);
            this.icon = icon;
            this.key = key;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
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
        public Dimension getPreferredSize() {
            return new Dimension(100, 40);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            int w = getWidth();
            int h = getHeight();
            if (active) {
                g2.setColor(Theme.accentSoft());
                g2.fillRoundRect(0, 0, w, h, 10, 10);
                g2.setColor(Theme.ACCENT);
                g2.fillRoundRect(0, 6, 3, h - 12, 3, 3);
            } else if (hover) {
                g2.setColor(Theme.hover());
                g2.fillRoundRect(0, 0, w, h, 10, 10);
            }
            g2.setColor(active || childActive ? Theme.accentText() : Theme.muted());
            int ix = collapsed ? (w - 18) / 2 : (child ? 28 : 14);
            int iy = (h - 18) / 2;
            if (icon == 4) {
                g2.fillRoundRect(ix, iy + 9, 4, 9, 2, 2);
                g2.fillRoundRect(ix + 7, iy + 3, 4, 15, 2, 2);
                g2.fillRoundRect(ix + 14, iy + 12, 4, 6, 2, 2);
            } else if (icon == 5) {
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawArc(ix + 1, iy + 1, 16, 16, 20, 140);
                g2.drawArc(ix + 1, iy + 1, 16, 16, 200, 140);
                g2.fillPolygon(new int[]{ix + 13, ix + 20, ix + 19}, new int[]{iy + 1, iy + 3, iy + 9}, 3);
                g2.fillPolygon(new int[]{ix + 5, ix - 2, ix - 1}, new int[]{iy + 17, iy + 15, iy + 9}, 3);
            } else if (icon == 0) {
                g2.fillArc(ix, iy + 1, 17, 17, 90, 270);
                g2.fillArc(ix + 3, iy - 2, 16, 16, 0, 85);
            } else if (icon == 1) {
                g2.fillRoundRect(ix, iy + 1, 8, 5, 3, 3);
                g2.fillRoundRect(ix, iy + 4, 18, 13, 4, 4);
            } else if (icon == 3) {
                g2.fillRoundRect(ix, iy + 2, 18, 16, 4, 4);
                g2.setColor(active ? Theme.accentSoft() : Theme.sidebar());
                g2.fillRect(ix + 2, iy + 8, 14, 8);
                g2.setColor(active ? Theme.accentText() : Theme.muted());
                g2.fillRoundRect(ix + 4, iy - 1, 3, 5, 2, 2);
                g2.fillRoundRect(ix + 11, iy - 1, 3, 5, 2, 2);
                g2.fillRect(ix + 4, iy + 10, 4, 3);
                g2.fillRect(ix + 10, iy + 10, 4, 3);
            } else {
                g2.fillOval(ix + 5, iy, 8, 8);
                g2.fillArc(ix + 1, iy + 10, 16, 16, 0, 180);
            }
            if (!collapsed) {
                g2.setFont(active ? fb(12.5f) : f(12.5f));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), child ? 54 : 42, (h - fm.getHeight()) / 2 + fm.getAscent());
                if (group) {
                    g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int ax = w - 22;
                    int ay = h / 2;
                    if (expanded) {
                        g2.drawPolyline(new int[]{ax - 4, ax, ax + 4}, new int[]{ay - 2, ay + 2, ay - 2}, 3);
                    } else {
                        g2.drawPolyline(new int[]{ax - 2, ax + 2, ax - 2}, new int[]{ay - 4, ay, ay + 4}, 3);
                    }
                }
            }
            g2.dispose();
        }
    }

    static class IconButton extends JButton {
        private String type;
        private boolean hover;

        IconButton(String type) {
            this.type = type;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setPreferredSize(new Dimension(40, 40));
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

        void setType(String type) {
            this.type = type;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            int w = getWidth();
            int h = getHeight();
            int cx = w / 2;
            int cy = h / 2;
            if (hover) {
                g2.setColor(Theme.hover());
                g2.fillRoundRect(0, 0, w, h, 12, 12);
            }
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (type) {
                case "sun":
                    g2.setColor(Theme.GOLD);
                    g2.fill(new Ellipse2D.Double(cx - 4, cy - 4, 8, 8));
                    g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    for (int i = 0; i < 8; i++) {
                        double a = i * Math.PI / 4;
                        g2.drawLine(
                            (int) Math.round(cx + Math.cos(a) * 7), (int) Math.round(cy + Math.sin(a) * 7),
                            (int) Math.round(cx + Math.cos(a) * 10), (int) Math.round(cy + Math.sin(a) * 10)
                        );
                    }
                    break;
                case "moon":
                    Area moon = new Area(new Ellipse2D.Double(cx - 8, cy - 8, 16, 16));
                    moon.subtract(new Area(new Ellipse2D.Double(cx - 2, cy - 10, 15, 15)));
                    g2.setColor(Theme.accentText());
                    g2.fill(moon);
                    break;
                case "chevL":
                    g2.setColor(Theme.muted());
                    g2.drawPolyline(new int[]{cx + 3, cx - 3, cx + 3}, new int[]{cy - 6, cy, cy + 6}, 3);
                    break;
                case "chevR":
                    g2.setColor(Theme.muted());
                    g2.drawPolyline(new int[]{cx - 3, cx + 3, cx - 3}, new int[]{cy - 6, cy, cy + 6}, 3);
                    break;
                case "sound":
                case "mute":
                    g2.setColor(Theme.text());
                    g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.fillPolygon(new int[]{cx - 9, cx - 5, cx - 1, cx - 1, cx - 5, cx - 9},
                        new int[]{cy - 3, cy - 3, cy - 7, cy + 7, cy + 3, cy + 3}, 6);
                    if ("sound".equals(type)) {
                        g2.draw(new Arc2D.Double(cx - 5, cy - 4, 8, 8, -50, 100, Arc2D.OPEN));
                        g2.draw(new Arc2D.Double(cx - 5, cy - 8, 16, 16, -50, 100, Arc2D.OPEN));
                    } else {
                        g2.drawLine(cx + 3, cy - 4, cx + 10, cy + 4);
                        g2.drawLine(cx + 10, cy - 4, cx + 3, cy + 4);
                    }
                    break;
                default:
                    g2.setColor(Theme.muted());
                    g2.drawPolyline(new int[]{cx - 5, cx, cx + 5}, new int[]{cy - 2, cy + 3, cy - 2}, 3);
                    break;
            }
            g2.dispose();
        }
    }

    static class Divider extends JComponent {
        Divider() {
            setPreferredSize(new Dimension(1, 30));
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(Theme.border());
            g.fillRect(0, 0, 1, getHeight());
        }
    }

    static class Pill extends JLabel {
        Pill(String text) {
            super(text);
            setFont(fb(11));
            setForeground(Theme.GOLD);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(7, 30, 7, 16));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            int h = getHeight();
            g2.setColor(new Color(250, 204, 21, 40));
            g2.fillRoundRect(0, 0, getWidth() - 1, h - 1, h, h);
            g2.setColor(Theme.GOLD);
            g2.fill(new Ellipse2D.Double(14, h / 2.0 - 4, 8, 8));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class AvatarIcon implements Icon {
        String initials;
        Color background;
        BufferedImage photo;
        private final int size;
        private BufferedImage cache;
        private BufferedImage cacheSource;

        AvatarIcon(String initials, int size, Color background, Color foreground) {
            this(initials, size, background, foreground, null);
        }

        AvatarIcon(String initials, int size, Color background, Color foreground, BufferedImage photo) {
            this.initials = initials;
            this.size = size;
            this.background = background;
            this.photo = photo;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        private BufferedImage circle(BufferedImage src) {
            if (cache != null && cacheSource == src) {
                return cache;
            }
            BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            quality(g);
            g.fill(new Ellipse2D.Double(0, 0, size, size));
            g.setComposite(AlphaComposite.SrcIn);
            g.drawImage(src.getScaledInstance(size, size, Image.SCALE_SMOOTH), 0, 0, null);
            g.dispose();
            cache = out;
            cacheSource = src;
            return out;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            Shape clip = new Ellipse2D.Double(x, y, size, size);
            if (photo != null) {
                g2.drawImage(circle(photo), x, y, null);
                g2.setColor(new Color(0, 0, 0, 40));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new Ellipse2D.Double(x + 0.5, y + 0.5, size - 1, size - 1));
                g2.dispose();
                return;
            }
            g2.setClip(clip);
            g2.setColor(Theme.input());
            g2.fill(clip);
            g2.setColor(Theme.muted());
            int headD = Math.max(8, (int) Math.round(size * 0.34));
            int headX = x + (size - headD) / 2;
            int headY = y + (int) Math.round(size * 0.22);
            g2.fillOval(headX, headY, headD, headD);
            int bodyW = (int) Math.round(size * 0.66);
            int bodyH = (int) Math.round(size * 0.46);
            int bodyX = x + (size - bodyW) / 2;
            int bodyY = y + (int) Math.round(size * 0.60);
            g2.fillOval(bodyX, bodyY, bodyW, bodyH);
            g2.setClip(null);
            g2.setColor(Theme.border());
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new Ellipse2D.Double(x + 0.5, y + 0.5, size - 1, size - 1));
            g2.dispose();
        }
    }

    static class PhotoPicker extends JPanel {
        private BufferedImage photo;
        private final AvatarIcon icon = new AvatarIcon("", 42, new Color(147, 197, 253), new Color(15, 23, 42));
        private final JLabel preview = new JLabel(icon);
        private final JLabel fileName = new JLabel("Belum ada file dipilih");
        private final AppButton removeButton = new AppButton("Hapus", false).compact();

        PhotoPicker() {
            super(new BorderLayout(12, 0));
            setOpaque(false);

            AppButton choose = new AppButton("Pilih File", false).compact();
            choose.addActionListener(e -> {
                BufferedImage img = choosePhoto(this);
                if (img != null) {
                    setPhoto(img);
                    fileName.setText(Ui.lastPhotoName());
                }
            });
            fileName.setFont(f(12));
            fileName.putClientProperty("role", "muted");
            removeButton.setDanger(true);
            removeButton.addActionListener(e -> setPhoto(null));
            removeButton.setVisible(false);

            JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            actionRow.setOpaque(false);
            actionRow.add(choose);
            actionRow.add(fileName);
            actionRow.add(removeButton);

            JPanel center = new JPanel(new GridBagLayout());
            center.setOpaque(false);
            GridBagConstraints gc = new GridBagConstraints();
            gc.anchor = GridBagConstraints.WEST;
            center.add(actionRow, gc);

            add(preview, BorderLayout.WEST);
            add(center, BorderLayout.CENTER);
        }

        void bindName(javax.swing.text.JTextComponent field) {
        }

        BufferedImage getPhoto() {
            return photo;
        }

        void setPhoto(BufferedImage photo) {
            this.photo = photo;
            icon.photo = photo;
            boolean hasPhoto = (photo != null);
            removeButton.setVisible(hasPhoto);
            fileName.setText(hasPhoto ? "Foto profil terpilih" : "Belum ada file dipilih");
            preview.repaint();
            revalidate();
            repaint();
        }

        void refresh() {
            icon.photo = photo;
            preview.repaint();
        }
    }

    static class TimeIcon extends JComponent {
        TimeIcon() {
            setPreferredSize(new Dimension(42, 42));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            int part = daypart();
            if (part == 3) {
                g2.setColor(new Color(253, 230, 138, 40));
                g2.fill(new Ellipse2D.Double(cx - 17, cy - 17, 34, 34));
                java.awt.geom.Area moon = new java.awt.geom.Area(new Ellipse2D.Double(cx - 10, cy - 10, 20, 20));
                moon.subtract(new java.awt.geom.Area(new Ellipse2D.Double(cx - 3, cy - 13, 19, 19)));
                g2.setColor(new Color(253, 230, 138));
                g2.fill(moon);
                g2.setColor(new Color(253, 245, 200));
                g2.fill(new Ellipse2D.Double(cx + 6, cy - 12, 3, 3));
                g2.fill(new Ellipse2D.Double(cx + 11, cy - 3, 2.4, 2.4));
                g2.fill(new Ellipse2D.Double(cx + 4, cy + 7, 2, 2));
            } else {
                Color sun = part == 0 ? new Color(250, 204, 21)
                    : (part == 1 ? new Color(255, 186, 28) : new Color(251, 146, 60));
                g2.setColor(new Color(sun.getRed(), sun.getGreen(), sun.getBlue(), 45));
                g2.fill(new Ellipse2D.Double(cx - 18, cy - 18, 36, 36));
                g2.setColor(sun);
                g2.fill(new Ellipse2D.Double(cx - 7, cy - 7, 14, 14));
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    g2.drawLine(
                        (int) Math.round(cx + Math.cos(a) * 11), (int) Math.round(cy + Math.sin(a) * 11),
                        (int) Math.round(cx + Math.cos(a) * 15), (int) Math.round(cy + Math.sin(a) * 15)
                    );
                }
            }
            g2.dispose();
        }
    }

    static class EyeIcon implements Icon {
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
            g2.setColor(Theme.muted());
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawArc(x + 1, y + 3, 18, 10, 0, 180);
            g2.drawArc(x + 1, y + 3, 18, 10, 180, 180);
            g2.fillOval(x + 8, y + 6, 5, 5);
            if (!(c instanceof AbstractButton) || !((AbstractButton) c).isSelected()) {
                g2.drawLine(x + 3, y + 14, x + 17, y + 2);
            }
            g2.dispose();
        }
    }

    static class DialogBody extends JPanel {
        private final Color tone;

        DialogBody(LayoutManager layout, Color tone) {
            super(layout);
            this.tone = tone;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            int w = getWidth();
            int h = getHeight();
            g2.setPaint(new GradientPaint(0, 0, Theme.heroA(), w, h, Theme.surface()));
            g2.fillRect(0, 0, w, h);
            g2.setPaint(new GradientPaint(0, 0, tone, w, 0, Theme.GOLD));
            g2.fillRect(0, 0, w, 5);
            g2.setColor(Theme.border());
            g2.drawRect(0, 0, w - 1, h - 1);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class HLine extends JComponent {
        HLine() {
            setPreferredSize(new Dimension(1, 1));
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(Theme.border());
            g.fillRect(0, 0, getWidth(), 1);
        }
    }

    static class PersonCell extends JPanel implements ListCellRenderer<String> {
        private final boolean teacher;
        private final JLabel avatar = new JLabel();
        private final JLabel name = new JLabel();
        private final JLabel sub = new JLabel();
        private boolean selected;

        PersonCell(boolean teacher) {
            super(new BorderLayout(14, 0));
            this.teacher = teacher;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
            name.setFont(fb(13));
            sub.setFont(f(11));
            JPanel text = new JPanel(new GridLayout(0, 1, 0, 1));
            text.setOpaque(false);
            text.add(name);
            text.add(sub);
            add(avatar, BorderLayout.WEST);
            add(text, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            String display = value;
            String detail = "Akun guru";
            BufferedImage photo = null;
            if (teacher) {
                TeacherRecord t = DataStore.TEACHERS.get(value);
                if (t != null) {
                    display = t.name.isEmpty() ? value : t.name;
                    detail = value + "  \u00B7  " + t.subjectsText();
                    photo = t.photo;
                }
            } else {
                StudentRecord s = DataStore.STUDENT_ACCOUNTS.get(value);
                if (s != null) {
                    display = s.name;
                    detail = "Kelas " + s.studentClass + "  \u00B7  " + s.username;
                    photo = s.photo;
                }
            }
            if (photo != null) {
                avatar.setVisible(true);
                avatar.setIcon(new AvatarIcon("", 38, avatarColor(display), new Color(15, 23, 42), photo));
            } else {
                avatar.setIcon(null);
                avatar.setVisible(false);
            }
            name.setText(display);
            sub.setText(detail);
            name.setForeground(isSelected ? Theme.accentText() : Theme.text());
            sub.setForeground(Theme.muted());
            selected = isSelected;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (selected) {
                Graphics2D g2 = (Graphics2D) g.create();
                quality(g2);
                g2.setColor(Theme.accentSoft());
                g2.fillRoundRect(3, 3, getWidth() - 6, getHeight() - 6, 14, 14);
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}