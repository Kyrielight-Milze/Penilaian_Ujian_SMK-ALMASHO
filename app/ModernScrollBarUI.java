package app;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Scrollbar tipis dan membulat tanpa tombol panah. Batang geser muncul saat halaman digulir
 * atau kursor mendekat, lalu memudar menjadi transparan bila dibiarkan.
 */
final class ModernScrollBarUI extends BasicScrollBarUI {
    private static final int IDLE_MS = 900;

    private float alpha = 0f;
    private long lastActive = 0;
    private boolean hovering;
    private final Timer timer = new Timer(28, e -> tick());

    static void poke(JScrollBar bar) {
        if (bar.getUI() instanceof ModernScrollBarUI) {
            ((ModernScrollBarUI) bar.getUI()).poke();
        }
    }

    void poke() {
        lastActive = System.currentTimeMillis();
        if (!timer.isRunning()) {
            timer.start();
        }
    }

    private void tick() {
        boolean active = hovering || isDragging || System.currentTimeMillis() - lastActive < IDLE_MS;
        float target = active ? 1f : 0f;
        alpha += (target - alpha) * 0.22f;
        if (Math.abs(target - alpha) < 0.03f) {
            alpha = target;
        }
        if (scrollbar != null) {
            scrollbar.repaint();
        }
        if (!active && alpha == 0f) {
            timer.stop();
        }
    }

    @Override
    protected void installListeners() {
        super.installListeners();
        scrollbar.addAdjustmentListener(e -> poke());
        scrollbar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovering = true;
                poke();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovering = false;
                poke();
            }
        });
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        return scrollbar.getOrientation() == JScrollBar.VERTICAL ? new Dimension(12, 48) : new Dimension(48, 12);
    }

    @Override
    protected Dimension getMinimumThumbSize() {
        return new Dimension(22, 22);
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return emptyButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return emptyButton();
    }

    private static JButton emptyButton() {
        JButton b = new JButton();
        Dimension zero = new Dimension(0, 0);
        b.setPreferredSize(zero);
        b.setMinimumSize(zero);
        b.setMaximumSize(zero);
        b.setFocusable(false);
        return b;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        // trek dibiarkan transparan
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
        if (r.isEmpty() || !scrollbar.isEnabled() || alpha <= 0.01f) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.quality(g2);
        boolean strong = hovering || isDragging;
        int thick = strong ? 8 : 6;
        int x;
        int y;
        int w;
        int h;
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            w = thick;
            h = r.height - 4;
            x = r.x + (r.width - thick) / 2;
            y = r.y + 2;
        } else {
            w = r.width - 4;
            h = thick;
            x = r.x + 2;
            y = r.y + (r.height - thick) / 2;
        }
        Color base = Theme.muted();
        int a = Math.round(alpha * (strong ? 210 : 150));
        g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), a));
        g2.fillRoundRect(x, y, w, h, thick, thick);
        g2.dispose();
    }
}
