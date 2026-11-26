package app;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import app.Components.AppButton;
import static app.Theme.f;
import static app.Theme.fb;
import static app.Theme.quality;

/**
 * Pemilih mata pelajaran untuk operator: satu guru bisa mengampu banyak mapel.
 * Mapel tampil sebagai chip yang bisa dihapus dengan tombol x. Tambah lewat kolom ketik
 * (Enter / koma / tombol Tambah) atau klik 
 */
final class SubjectPicker extends JPanel {
    private static final int MAX_SUGGESTIONS = 12;

    private final List<String> subjects = new ArrayList<>();
    private final JTextField input = new JTextField();
    private final JPanel chipBox = new JPanel(new WrapLayout(FlowLayout.LEFT, 6, 6));
    private final JPanel suggestBox = new JPanel(new WrapLayout(FlowLayout.LEFT, 6, 6));
    private final JLabel emptyHint = new JLabel("Belum ada mapel. Ketik nama mapel lalu tekan Enter atau tombol Tambah.");
    private final JLabel countLabel = new JLabel();

    SubjectPicker() {
        super(new BorderLayout(0, 8));
        setOpaque(false);

        chipBox.setOpaque(false);
        suggestBox.setOpaque(false);
        emptyHint.setFont(Theme.fi(12));
        countLabel.setFont(f(11));
        countLabel.putClientProperty("role", "muted");
        emptyHint.putClientProperty("role", "muted");

        // ---- baris ketik + tombol Tambah
        AppButton add = new AppButton("Tambah", false).compact();
        add.addActionListener(e -> commitPending());
        input.putClientProperty("role", "field");
        input.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    e.consume();
                    commitPending();
                } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE
                    && input.getText().isEmpty() && !subjects.isEmpty()) {
                    removeSubject(subjects.get(subjects.size() - 1));
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == ',' || e.getKeyChar() == ';') {
                    e.consume();
                    commitPending();
                }
            }
        });
        input.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                rebuildSuggestions();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                rebuildSuggestions();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                rebuildSuggestions();
            }
        });
        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        inputRow.setOpaque(false);
        inputRow.add(input, BorderLayout.CENTER);
        inputRow.add(add, BorderLayout.EAST);

        // ---- kotak chip terpilih
        JPanel chipCard = new JPanel(new BorderLayout(0, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                quality(g2);
                g2.setColor(Theme.input());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.setColor(Theme.border());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        chipCard.setOpaque(false);
        chipCard.setBorder(BorderFactory.createEmptyBorder(8, 8, 6, 8));
        JPanel chipHead = new JPanel(new BorderLayout());
        chipHead.setOpaque(false);
        chipHead.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        chipHead.add(label("Mapel yang diampu", fb(12), "muted"), BorderLayout.WEST);
        chipHead.add(countLabel, BorderLayout.EAST);
        chipCard.add(chipHead, BorderLayout.NORTH);
        JPanel chipBody = new JPanel(new BorderLayout());
        chipBody.setOpaque(false);
        chipBody.add(chipBox, BorderLayout.CENTER);
        chipCard.add(chipBody, BorderLayout.CENTER);

        JPanel suggestWrap = new JPanel(new BorderLayout(0, 2));
        suggestWrap.setOpaque(false);
        suggestWrap.add(suggestBox, BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(inputRow, BorderLayout.NORTH);
        top.add(chipCard, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        add(suggestWrap, BorderLayout.CENTER);

        rebuildChips();
        rebuildSuggestions();
    }

    private static JLabel label(String text, Font font, String role) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.putClientProperty("role", role);
        return l;
    }

    // ===================== API =====================
    List<String> getSubjects() {
        commitPending();
        return new ArrayList<>(subjects);
    }

    void setSubjects(List<String> list) {
        subjects.clear();
        subjects.addAll(list);
        input.setText("");
        rebuildChips();
        rebuildSuggestions();
    }

    /** Muat ulang saran dari jadwal (dipanggil saat halaman Akun Guru dibuka). */
    void refreshSuggestions() {
        rebuildSuggestions();
    }

    void clear() {
        setSubjects(new ArrayList<>());
    }

    JTextField inputField() {
        return input;
    }

    // ===================== LOGIKA =====================
    /** Masukkan teks yang sedang diketik sebagai mapel (boleh beberapa, dipisah koma). */
    private void commitPending() {
        String text = input.getText();
        if (text.trim().isEmpty()) {
            return;
        }
        input.setText("");
        for (String s : DataStore.parseSubjects(text)) {
            addSubject(s);
        }
        input.requestFocusInWindow();
    }

    private void addSubject(String subject) {
        for (String existing : subjects) {
            if (existing.equalsIgnoreCase(subject)) {
                return;
            }
        }
        subjects.add(subject);
        rebuildChips();
        rebuildSuggestions();
    }

    private void removeSubject(String subject) {
        subjects.remove(subject);
        rebuildChips();
        rebuildSuggestions();
    }

    private void rebuildChips() {
        chipBox.removeAll();
        if (subjects.isEmpty()) {
            chipBox.add(emptyHint);
        } else {
            for (String s : subjects) {
                chipBox.add(new Chip(s, true));
            }
        }
        countLabel.setText(subjects.size() + " mapel");
        refreshTheme();
        relayout();
    }

    private void rebuildSuggestions() {
        suggestBox.removeAll();
        // Saran hanya dari menu Jadwal Pelajaran yang diatur operator.
        Set<String> pool = new LinkedHashSet<>(DataStore.scheduleSubjects());
        String query = input.getText().trim().toLowerCase();
        int shown = 0;
        for (String s : pool) {
            if (shown >= MAX_SUGGESTIONS) {
                break;
            }
            boolean taken = false;
            for (String existing : subjects) {
                if (existing.equalsIgnoreCase(s)) {
                    taken = true;
                    break;
                }
            }
            if (taken || (!query.isEmpty() && !s.toLowerCase().contains(query))) {
                continue;
            }
            suggestBox.add(new Chip(s, false));
            shown++;
        }
        if (shown == 0) {
            String q = input.getText().trim();
            if (!q.isEmpty()) {
                suggestBox.add(label("Tekan Enter untuk menambahkan \u201C" + q + "\u201D.", f(11), "muted"));
            }
        }
        refreshTheme();
        relayout();
    }

    private void refreshTheme() {
        Theme.apply(chipBox);
        Theme.apply(suggestBox);
    }

    private void relayout() {
        revalidate();
        repaint();
        Container c = getParent();
        while (c != null) {
            c.invalidate();
            c = c.getParent();
        }
        Window w = SwingUtilities.getWindowAncestor(this);
        if (w != null) {
            w.validate();
            w.repaint();
        }
    }

    // ===================== CHIP =====================
    private final class Chip extends JComponent {
        private final String text;
        private final boolean selected;
        private boolean hover;
        private boolean hoverClose;

        Chip(String text, boolean selected) {
            this.text = text;
            this.selected = selected;
            setFont(selected ? fb(12) : f(12));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(selected ? "Klik \u00D7 untuk menghapus" : "Tambahkan " + text);
            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    hoverClose = false;
                    repaint();
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    boolean over = selected && e.getX() >= getWidth() - 28;
                    if (over != hoverClose) {
                        hoverClose = over;
                        repaint();
                    }
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (selected) {
                        if (e.getX() >= getWidth() - 28) {
                            removeSubject(text);
                        }
                    } else {
                        addSubject(text);
                    }
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(text) + (selected ? 14 + 28 : 14 + 22), 30);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            quality(g2);
            int w = getWidth();
            int h = getHeight();
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
            if (selected) {
                g2.setColor(Theme.accentSoft());
                g2.fillRoundRect(0, 0, w - 1, h - 1, h, h);
                g2.setColor(new Color(Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(), Theme.ACCENT.getBlue(), 120));
                g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);
                g2.setColor(Theme.accentText());
                g2.drawString(text, 14, ty);
                // tombol x
                int cx = w - 16;
                int cy = h / 2;
                if (hoverClose) {
                    g2.setColor(new Color(239, 68, 68, 60));
                    g2.fillOval(cx - 9, cy - 9, 18, 18);
                }
                g2.setColor(hoverClose ? new Color(248, 113, 113) : Theme.accentText());
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(cx - 3, cy - 3, cx + 3, cy + 3);
                g2.drawLine(cx - 3, cy + 3, cx + 3, cy - 3);
            } else {
                g2.setColor(hover ? Theme.accentSoft() : Theme.surface());
                g2.fillRoundRect(0, 0, w - 1, h - 1, h, h);
                g2.setColor(hover ? Theme.accentText() : Theme.border());
                g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);
                g2.setColor(hover ? Theme.accentText() : Theme.muted());
                g2.drawString("+", 10, ty);
                g2.drawString(text, 10 + fm.stringWidth("+ "), ty);
            }
            g2.dispose();
        }
    }

    // ===================== FLOW LAYOUT YANG BISA TURUN BARIS =====================
    /** FlowLayout yang menghitung tinggi sesuai lebar, jadi chip turun ke baris berikutnya. */
    static final class WrapLayout extends FlowLayout {
        WrapLayout(int align, int hgap, int vgap) {
            super(align, hgap, vgap);
        }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return size(target, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            Dimension d = size(target, false);
            d.width -= getHgap() + 1;
            return d;
        }

        private Dimension size(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getSize().width;
                Container top = target;
                while (top.getSize().width == 0 && top.getParent() != null) {
                    top = top.getParent();
                }
                if (targetWidth == 0) {
                    targetWidth = Math.max(top.getSize().width, 260);
                }
                Insets insets = target.getInsets();
                int maxWidth = targetWidth - (insets.left + insets.right + getHgap() * 2);
                Dimension dim = new Dimension(0, 0);
                int rowW = 0;
                int rowH = 0;
                for (Component m : target.getComponents()) {
                    if (!m.isVisible()) {
                        continue;
                    }
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                    if (rowW + d.width > maxWidth && rowW > 0) {
                        addRow(dim, rowW, rowH);
                        rowW = 0;
                        rowH = 0;
                    }
                    if (rowW != 0) {
                        rowW += getHgap();
                    }
                    rowW += d.width;
                    rowH = Math.max(rowH, d.height);
                }
                addRow(dim, rowW, rowH);
                dim.width += insets.left + insets.right + getHgap() * 2;
                dim.height += insets.top + insets.bottom + getVgap() * 2;
                return dim;
            }
        }

        private void addRow(Dimension dim, int rowW, int rowH) {
            dim.width = Math.max(dim.width, rowW);
            if (dim.height > 0) {
                dim.height += getVgap();
            }
            dim.height += rowH;
        }
    }
}
