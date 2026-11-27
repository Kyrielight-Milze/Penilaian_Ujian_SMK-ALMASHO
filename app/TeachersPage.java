package app;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

import app.Components.*;
import static app.Components.modalDialog;
import static app.Components.printTable;
import static app.Theme.f;
import static app.Theme.fb;
import static app.Ui.*;

/** Operator: kelola akun guru dengan petunjuk klik 2x, info selengkapnya bersih, dan fitur cetak. */
final class TeachersPage extends Page {
    private final JCheckBox selectAllBox = new JCheckBox();
    private JButton rekapBtn;
    private final JTextField searchInput = new JTextField(18);
    private final JComboBox<Integer> pageSizeBox = new JComboBox<>(new Integer[]{10, 20, 50, 100});
    private final JTextField pageInput = new JTextField("1", 3);
    private final JLabel totalPageLabel = label("dari 1", f(12), "muted");
    private final JLabel infoCountLabel = label("Menampilkan data 0-0 dari 0", f(12), "muted");

    private final TeacherTableModel tableModel = new TeacherTableModel();
    private final JTable table = new JTable(tableModel);
    private List<TeacherRecord> filteredList = new ArrayList<>();
    private int currentPage = 1;

    TeachersPage(AppShell app) {
        super(app);
    }

    private static String formatTeacherName(String raw) {
        if (raw == null) {
            return "";
        }
        int comma = raw.indexOf(',');
        if (comma < 0) {
            return raw.toUpperCase();
        }
        return raw.substring(0, comma).toUpperCase() + raw.substring(comma);
    }

    private static void forceUppercaseBeforeComma(JTextField field) {
        AbstractDocument doc = (AbstractDocument) field.getDocument();
        doc.setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr)
                    throws BadLocationException {
                if (text != null) {
                    String current = fb.getDocument().getText(0, fb.getDocument().getLength());
                    int commaIdx = current.indexOf(',');
                    if (commaIdx < 0 || offset <= commaIdx) {
                        text = formatTeacherName(text);
                    }
                }
                super.insertString(fb, offset, text, attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (text != null) {
                    String current = fb.getDocument().getText(0, fb.getDocument().getLength());
                    int commaIdx = current.indexOf(',');
                    if (commaIdx < 0 || offset <= commaIdx) {
                        text = formatTeacherName(text);
                    }
                }
                super.replace(fb, offset, length, text, attrs);
            }
        });
    }

    @Override
    void onShow() {
        selectAllBox.setSelected(false);
        applyFilterAndRender();
        updateRekapButtonLabel();
    }

    @Override
    void reset() {
        currentPage = 1;
        searchInput.setText("");
    }

    @Override
    JPanel view() {
        RoundPanel mainCard = card(new BorderLayout(0, 0), 14, 0, false);

        JPanel cardHeader = p(new BorderLayout());
        cardHeader.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.border()),
            BorderFactory.createEmptyBorder(12, 18, 12, 18)
        ));
        JLabel titleLbl = label("Data Akun Guru", fb(14), "title");
        titleLbl.setIcon(new VectorIcon("table", 15, Theme.text()));
        titleLbl.setIconTextGap(8);

        JLabel hintLbl = label("Petunjuk: Klik 2 kali pada baris data guru untuk melihat info selengkapnya", f(11.5f), "accent");
        hintLbl.setIcon(new VectorIcon("info", 14, Theme.accentText()));
        hintLbl.setIconTextGap(6);

        cardHeader.add(titleLbl, BorderLayout.WEST);
        cardHeader.add(hintLbl, BorderLayout.EAST);
        mainCard.add(cardHeader, BorderLayout.NORTH);

        JPanel centerArea = p(new BorderLayout());

        JPanel toolbar = p(new BorderLayout(10, 6));
        toolbar.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JPanel leftTools = p(new FlowLayout(FlowLayout.LEFT, 8, 0));
        selectAllBox.setOpaque(false);
        selectAllBox.setToolTipText("Pilih Semua");
        selectAllBox.addActionListener(e -> {
            tableModel.setAllChecked(selectAllBox.isSelected());
            updateRekapButtonLabel();
        });

        rekapBtn = makeIconTextBtn("Rekap", new VectorIcon("table", 14, Theme.text()), "Rekap & Cetak Akun Guru");
        rekapBtn.setPreferredSize(new Dimension(115, 32));
        rekapBtn.addActionListener(e -> handleRekapAction());

        JButton addBtn = makeSymbolBtn(new VectorIcon("plus", 15, Color.WHITE), "Tambah Akun Guru", new Color(22, 163, 74));
        addBtn.addActionListener(e -> openTeacherFormDialog(null));

        JButton editBtn = makeSymbolBtn(new VectorIcon("pencil", 15, Color.WHITE), "Edit Akun Guru Terpilih", new Color(234, 179, 8));
        editBtn.addActionListener(e -> handleEditSelected());

        JButton deleteBtn = makeSymbolBtn(new VectorIcon("trash", 15, Color.WHITE), "Hapus Akun Guru Terpilih", new Color(220, 38, 38));
        deleteBtn.addActionListener(e -> handleDeleteSelected());

        leftTools.add(selectAllBox);
        leftTools.add(rekapBtn);
        leftTools.add(addBtn);
        leftTools.add(editBtn);
        leftTools.add(deleteBtn);

        JPanel rightTools = p(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        searchInput.setFont(f(12));
        searchInput.setPreferredSize(new Dimension(200, 32));
        searchInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                currentPage = 1;
                applyFilterAndRender();
            }
        });

        JButton searchBtn = makeIconTextBtn("Cari", new VectorIcon("search", 13, Theme.text()), "Cari Guru");
        searchBtn.setPreferredSize(new Dimension(78, 32));
        searchBtn.addActionListener(e -> {
            currentPage = 1;
            applyFilterAndRender();
        });

        rightTools.add(searchInput);
        rightTools.add(searchBtn);

        toolbar.add(leftTools, BorderLayout.WEST);
        toolbar.add(rightTools, BorderLayout.EAST);
        centerArea.add(toolbar, BorderLayout.NORTH);

        configureTable();
        tableModel.addTableModelListener(e -> updateRekapButtonLabel());

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Theme.border()));
        tableScroll.getViewport().setBackground(Theme.surface());
        centerArea.add(tableScroll, BorderLayout.CENTER);

        mainCard.add(centerArea, BorderLayout.CENTER);

        JPanel footerBar = p(new BorderLayout());
        footerBar.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));

        JPanel leftPage = p(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton firstBtn = makePageBtn("|<", null);
        JButton prevBtn = makePageBtn("<", null);
        JButton nextBtn = makePageBtn(">", null);
        JButton lastBtn = makePageBtn(">|", null);
        JButton reloadBtn = makePageBtn("", new VectorIcon("refresh", 13, Theme.text()));
        reloadBtn.setToolTipText("Muat ulang data");

        pageInput.setHorizontalAlignment(SwingConstants.CENTER);
        pageInput.setFont(f(12));
        pageInput.setPreferredSize(new Dimension(42, 26));
        pageInput.addActionListener(e -> {
            try {
                int p = Integer.parseInt(pageInput.getText().trim());
                currentPage = Math.max(1, Math.min(p, getTotalPages()));
            } catch (NumberFormatException ignored) {
            }
            renderPage();
        });

        firstBtn.addActionListener(e -> {
            currentPage = 1;
            renderPage();
        });
        prevBtn.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                renderPage();
            }
        });
        nextBtn.addActionListener(e -> {
            if (currentPage < getTotalPages()) {
                currentPage++;
                renderPage();
            }
        });
        lastBtn.addActionListener(e -> {
            currentPage = getTotalPages();
            renderPage();
        });
        reloadBtn.addActionListener(e -> {
            DataStore.reload();
            onShow();
        });

        leftPage.add(firstBtn);
        leftPage.add(prevBtn);
        leftPage.add(pageInput);
        leftPage.add(totalPageLabel);
        leftPage.add(nextBtn);
        leftPage.add(lastBtn);
        leftPage.add(reloadBtn);
        leftPage.add(Box.createHorizontalStrut(8));
        leftPage.add(infoCountLabel);

        JPanel rightPage = p(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        pageSizeBox.setSelectedItem(20);
        pageSizeBox.setFont(f(12));
        pageSizeBox.addActionListener(e -> {
            currentPage = 1;
            renderPage();
        });
        rightPage.add(label("Jumlah Per Halaman", f(12), "muted"));
        rightPage.add(pageSizeBox);

        footerBar.add(leftPage, BorderLayout.WEST);
        footerBar.add(rightPage, BorderLayout.EAST);
        mainCard.add(footerBar, BorderLayout.SOUTH);

        return pageShell("Akun Guru", "Kelola akun login guru dan mata pelajaran yang diampu", mainCard);
    }

    private void updateRekapButtonLabel() {
        if (rekapBtn == null) {
            return;
        }
        int checkedCount = tableModel.getCheckedRecords().size();
        int rowCount = tableModel.getRowCount();
        selectAllBox.setSelected(rowCount > 0 && checkedCount == rowCount);
        if (checkedCount > 0) {
            rekapBtn.setText("Rekap (" + checkedCount + ")");
            rekapBtn.setBackground(Theme.accentSoft());
            rekapBtn.setForeground(Theme.accentText());
        } else {
            rekapBtn.setText("Rekap");
            rekapBtn.setBackground(Theme.input());
            rekapBtn.setForeground(Theme.text());
        }
    }

    private void handleRekapAction() {
        List<TeacherRecord> checked = tableModel.getCheckedRecords();
        if (!checked.isEmpty()) {
            showRekapDialog(checked, "Rekap Data Guru Terpilih (" + checked.size() + " Guru)");
        } else {
            JPopupMenu menu = new JPopupMenu();
            menu.setBackground(Theme.surface());
            menu.add(new JMenuItem("Total Semua Guru: " + DataStore.TEACHERS.size() + " guru"));
            menu.addSeparator();
            JMenuItem allItem = new JMenuItem("Buka & Cetak Tabel Rekap Semua Guru...");
            allItem.addActionListener(ev -> showRekapDialog(filteredList, "Rekap Semua Akun Guru (" + filteredList.size() + " Guru)"));
            menu.add(allItem);
            menu.show(rekapBtn, 0, rekapBtn.getHeight() + 4);
        }
    }

    private void showRekapDialog(List<TeacherRecord> list, String title) {
        final JDialog[] dialogRef = new JDialog[1];

        int totalMapel = 0;
        for (TeacherRecord t : list) {
            totalMapel += t.subjects.size();
        }

        JPanel summaryBar = p(new FlowLayout(FlowLayout.LEFT, 12, 4));
        summaryBar.add(label("Total Guru: " + list.size() + " guru", fb(13), "title"));
        summaryBar.add(label("\u00B7  Total Mata Pelajaran Diampu: " + totalMapel + " mapel", f(12), "muted"));

        String[] cols = {"No", "Nama Guru", "Username (Gmail)", "Mata Pelajaran", "Jumlah"};
        Object[][] data = new Object[list.size()][cols.length];
        for (int i = 0; i < list.size(); i++) {
            TeacherRecord t = list.get(i);
            data[i][0] = i + 1;
            data[i][1] = t.name.isEmpty() ? t.username : t.name;
            data[i][2] = t.username;
            data[i][3] = t.subjectsText();
            data[i][4] = t.subjects.size() + " mapel";
        }

        JTable rekapTable = new JTable(new DefaultTableModel(data, cols) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        rekapTable.setRowHeight(34);
        rekapTable.setFont(f(12));
        rekapTable.setShowVerticalLines(false);
        rekapTable.setFillsViewportHeight(true);
        rekapTable.getTableHeader().setFont(fb(12));
        rekapTable.getTableHeader().setPreferredSize(new Dimension(0, 34));
        rekapTable.getColumnModel().getColumn(0).setMaxWidth(45);
        rekapTable.getColumnModel().getColumn(4).setMaxWidth(95);

        DefaultTableCellRenderer rekapRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean sel,
                                                           boolean foc, int row, int col) {
                super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                setBackground(sel ? Theme.accentSoft() : Theme.surface());
                setForeground(sel ? Theme.accentText() : Theme.text());
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                setHorizontalAlignment(col == 0 || col == 4 ? SwingConstants.CENTER : SwingConstants.LEFT);
                return this;
            }
        };
        for (int c = 0; c < rekapTable.getColumnCount(); c++) {
            rekapTable.getColumnModel().getColumn(c).setCellRenderer(rekapRenderer);
        }

        JScrollPane scroll = new JScrollPane(rekapTable);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.border()));
        scroll.getViewport().setBackground(Theme.surface());

        JPanel content = p(new BorderLayout(0, 10));
        content.add(summaryBar, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);

        AppButton copyBtn = new AppButton("Salin Rekap", false);
        copyBtn.addActionListener(e -> {
            StringBuilder sb = new StringBuilder();
            sb.append(title).append("\n\n");
            for (int i = 0; i < list.size(); i++) {
                TeacherRecord t = list.get(i);
                String nama = t.name.isEmpty() ? t.username : t.name;
                sb.append(i + 1).append(". ").append(nama)
                  .append(" (").append(t.username).append(") - Mapel: ")
                  .append(t.subjectsText()).append("\n");
            }
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.toString()), null);
            AppDialog.show(dialogRef[0], AppDialog.SUCCESS, "Tersalin", "Rekap data guru terpilih berhasil disalin ke clipboard.");
        });

        AppButton printBtn = new AppButton("Cetak / PDF", true);
        printBtn.addActionListener(e -> printTable(dialogRef[0], rekapTable, title, "SINA - Rekapitulasi Akun Guru"));

        AppButton closeBtn = new AppButton("Tutup", false);
        closeBtn.addActionListener(e -> dialogRef[0].dispose());

        JPanel footer = p(new GridLayout(1, 3, 12, 0));
        footer.add(copyBtn);
        footer.add(printBtn);
        footer.add(closeBtn);

        dialogRef[0] = modalDialog(
            app.frame(),
            "REKAPITULASI DATA GURU",
            title,
            "Ringkasan akun dan distribusi mata pelajaran yang diampu",
            new Color(16, 185, 129),
            content,
            footer,
            760,
            470
        );
        dialogRef[0].setVisible(true);
    }

    private void configureTable() {
        table.setRowHeight(44);
        table.setFont(f(12.5f));
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        table.setToolTipText("Klik 2 kali pada baris untuk melihat info selengkapnya");

        JTableHeader header = table.getTableHeader();
        header.setFont(fb(12));
        header.setPreferredSize(new Dimension(0, 38));
        header.setReorderingAllowed(false);

        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setMaxWidth(45);
        cm.getColumn(1).setMaxWidth(48);
        cm.getColumn(2).setMaxWidth(58);
        cm.getColumn(3).setPreferredWidth(210);
        cm.getColumn(4).setPreferredWidth(210);
        cm.getColumn(5).setPreferredWidth(280);

        cm.getColumn(2).setCellRenderer((tbl, value, isSelected, hasFocus, row, col) -> {
            JPanel cell = new JPanel(new GridBagLayout());
            cell.setBackground(isSelected ? Theme.accentSoft() : Theme.surface());
            if (value instanceof java.awt.image.BufferedImage) {
                cell.add(new JLabel(new AvatarIcon("", 30, Theme.ACCENT, Color.WHITE,
                    (java.awt.image.BufferedImage) value)));
            } else {
                JLabel dash = new JLabel("-");
                dash.setForeground(Theme.muted());
                cell.add(dash);
            }
            return cell;
        });

        DefaultTableCellRenderer textRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                setBackground(isSelected ? Theme.accentSoft() : Theme.surface());
                setForeground(isSelected ? Theme.accentText() : Theme.text());
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                setHorizontalAlignment(column == 1 ? SwingConstants.CENTER : SwingConstants.LEFT);
                return this;
            }
        };
        for (int i = 1; i < cm.getColumnCount(); i++) {
            if (i != 2) {
                cm.getColumn(i).setCellRenderer(textRenderer);
            }
        }

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    TeacherRecord t = tableModel.getRecordAt(table.getSelectedRow());
                    if (t != null) {
                        showTeacherDialog(t);
                    }
                }
            }
        });
    }

    private JButton makeSymbolBtn(Icon icon, String tooltip, Color bg) {
        JButton btn = new JButton(icon);
        btn.setToolTipText(tooltip);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(36, 32));
        btn.setBackground(bg);
        btn.setBorder(BorderFactory.createLineBorder(bg));
        return btn;
    }

    private JButton makeIconTextBtn(String text, Icon icon, String tooltip) {
        JButton btn = new JButton(text, icon);
        btn.setToolTipText(tooltip);
        btn.setFocusPainted(false);
        btn.setIconTextGap(6);
        btn.setFont(fb(12));
        btn.setBackground(Theme.input());
        btn.setForeground(Theme.text());
        btn.setBorder(BorderFactory.createLineBorder(Theme.border()));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton makePageBtn(String text, Icon icon) {
        JButton btn = icon != null ? new JButton(icon) : new JButton(text);
        btn.setFont(f(11));
        btn.setFocusPainted(false);
        btn.setMargin(new Insets(3, 8, 3, 8));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void handleEditSelected() {
        List<TeacherRecord> checked = tableModel.getCheckedRecords();
        TeacherRecord target = null;
        if (!checked.isEmpty()) {
            target = checked.get(0);
        } else if (table.getSelectedRow() >= 0) {
            target = tableModel.getRecordAt(table.getSelectedRow());
        }
        if (target == null) {
            warn("Centang atau pilih salah satu baris guru terlebih dahulu.", "Pilih Guru");
            return;
        }
        openTeacherFormDialog(target);
    }

    private void handleDeleteSelected() {
        List<TeacherRecord> checked = tableModel.getCheckedRecords();
        if (checked.isEmpty() && table.getSelectedRow() >= 0) {
            TeacherRecord sel = tableModel.getRecordAt(table.getSelectedRow());
            if (sel != null) {
                checked.add(sel);
            }
        }
        if (checked.isEmpty()) {
            warn("Centang atau pilih akun guru yang akan dihapus.", "Hapus Guru");
            return;
        }
        String msg = checked.size() == 1
            ? "Hapus akun " + (checked.get(0).name.isEmpty() ? checked.get(0).username : checked.get(0).name) + "?"
            : "Hapus " + checked.size() + " akun guru terpilih?";
        if (!AppDialog.confirm(app.frame(), "Hapus Akun Guru", msg, "Hapus")) {
            return;
        }
        for (TeacherRecord t : checked) {
            DataStore.removeTeacher(t);
        }
        selectAllBox.setSelected(false);
        onShow();
    }

    private void openTeacherFormDialog(TeacherRecord editing) {
        boolean isEdit = (editing != null);
        final JDialog[] dialogRef = new JDialog[1];

        PhotoPicker picker = new PhotoPicker();
        JTextField nameField = new JTextField(isEdit ? formatTeacherName(editing.name) : "");
        nameField.setToolTipText("Contoh: AHMAD SYARIFUL ANAM, S.Pd.");
        forceUppercaseBeforeComma(nameField);

        JTextField userField = new JTextField(isEdit ? editing.username : "");
        JPasswordField passField = new JPasswordField();
        SubjectPicker subjectPicker = new SubjectPicker();
        subjectPicker.refreshSuggestions();

        if (isEdit) {
            picker.setPhoto(editing.photo);
            userField.setEditable(false);
            userField.setToolTipText("Username tidak dapat diubah");
            subjectPicker.setSubjects(editing.subjects);
        }

        JPanel form = p(new GridBagLayout());
        addRow(form, 0, "Foto profil", picker);
        addRow(form, 1, "Nama & gelar guru", nameField);
        addRow(form, 2, "Username (Gmail)", userField);
        addRow(form, 3, isEdit ? "Password baru (opsional)" : "Password guru", createPasswordControl(passField));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(12, 0, 4, 0);
        form.add(label("Mata pelajaran yang diampu", fb(12.5f), "title"), c);
        c.gridy = 5;
        c.insets = new Insets(2, 0, 4, 0);
        form.add(subjectPicker, c);

        JScrollPane scroll = formScroll(form);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        AppButton cancelBtn = new AppButton("Batal", false);
        cancelBtn.addActionListener(e -> dialogRef[0].dispose());

        AppButton saveBtn = new AppButton(isEdit ? "Simpan Perubahan" : "Tambah Akun Guru", true);
        saveBtn.addActionListener(e -> {
            String nama = formatTeacherName(nameField.getText().trim());
            String user = userField.getText().trim();
            String pass = new String(passField.getPassword());
            List<String> subjects = subjectPicker.getSubjects();

            if (isEdit) {
                if (nama.isEmpty()) {
                    warn("Nama guru wajib diisi.", "Data belum lengkap");
                    return;
                }
                editing.name = nama;
                if (!pass.isEmpty()) {
                    editing.password = pass;
                }
                editing.subjects.clear();
                editing.subjects.addAll(subjects);
                editing.photo = picker.getPhoto();
                DataStore.updateTeacher(editing);
                dialogRef[0].dispose();
                onShow();
                success("Perubahan data guru berhasil disimpan.");
            } else {
                if (nama.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                    warn("Nama, username, dan password guru wajib diisi.", "Data belum lengkap");
                    return;
                }
                if (!isGmail(user)) {
                    warn("Username harus berupa alamat Gmail, misalnya nama@gmail.com.", "Username tidak valid");
                    return;
                }
                if (DataStore.isUsernameTaken(user)) {
                    warn("Username tersebut sudah terdaftar.", "Akun duplikat");
                    return;
                }
                TeacherRecord teacher = new TeacherRecord(nama, user, pass);
                teacher.subjects.addAll(subjects);
                teacher.photo = picker.getPhoto();
                DataStore.addTeacher(teacher);
                dialogRef[0].dispose();
                onShow();
                success("Akun guru berhasil ditambahkan.");
            }
        });

        JPanel btnRow = p(new GridLayout(1, 2, 12, 0));
        btnRow.add(cancelBtn);
        btnRow.add(saveBtn);

        dialogRef[0] = modalDialog(
            app.frame(),
            isEdit ? "EDIT AKUN GURU" : "AKUN GURU BARU",
            isEdit ? "Ubah Data Guru" : "Tambah Akun Guru",
            isEdit ? EDIT_HINT : "Pisahkan nama dan gelar dengan koma (misal: AHMAD, S.Pd.)",
            isEdit ? Theme.GOLD : Theme.ACCENT,
            scroll,
            btnRow,
            540,
            585
        );
        dialogRef[0].setVisible(true);
    }

    private void applyFilterAndRender() {
        String key = searchInput.getText().trim().toLowerCase();
        filteredList = new ArrayList<>();
        for (TeacherRecord t : DataStore.TEACHERS.values()) {
            boolean matchKey = key.isEmpty()
                || t.name.toLowerCase().contains(key)
                || t.username.toLowerCase().contains(key)
                || t.subjectsText().toLowerCase().contains(key);
            if (matchKey) {
                filteredList.add(t);
            }
        }
        renderPage();
    }

    private int getPageSize() {
        Integer sz = (Integer) pageSizeBox.getSelectedItem();
        return sz != null ? sz : 20;
    }

    private int getTotalPages() {
        return Math.max(1, (int) Math.ceil((double) filteredList.size() / getPageSize()));
    }

    private void renderPage() {
        int total = filteredList.size();
        int sz = getPageSize();
        int totalPages = getTotalPages();
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }
        int start = (currentPage - 1) * sz;
        int end = Math.min(start + sz, total);

        List<TeacherRecord> slice = (total == 0) ? new ArrayList<>() : filteredList.subList(start, end);
        tableModel.setData(slice, start + 1);
        updateRekapButtonLabel();

        pageInput.setText(String.valueOf(currentPage));
        totalPageLabel.setText("dari " + totalPages);
        if (total == 0) {
            infoCountLabel.setText("Menampilkan data 0-0 dari 0");
        } else {
            infoCountLabel.setText("Menampilkan data " + (start + 1) + "-" + end + " dari " + total);
        }
    }

    /** Info selengkapnya saat klik 2 kali: tanpa tombol Ubah, Tutup, dan Hapus di bawah. */
    private void showTeacherDialog(TeacherRecord t) {
        String shownName = t.name.isEmpty() ? t.username : t.name;
        JPanel content = p(new BorderLayout(0, 14));
        content.add(personHeader(shownName, "Guru  \u00B7  " + t.username, t.photo, 58), BorderLayout.NORTH);
        content.add(detailRows(
            new String[]{"Username", "Password", "Mata pelajaran"},
            new JComponent[]{
                valueLabel(t.username, 270),
                secretRow(t.password),
                valueLabel(t.subjectsText(), 270)
            }), BorderLayout.CENTER);

        JDialog dlg = modalDialog(
            app.frame(),
            "INFO SELENGKAPNYA",
            "Detail Akun Guru",
            "Tekan tombol silang (\u00D7) di kanan atas atau Esc untuk menutup",
            Theme.ACCENT,
            content,
            null,
            490,
            345
        );
        dlg.setVisible(true);
    }

    private static final class TeacherTableModel extends AbstractTableModel {
        private final String[] cols = {"Cek", "No", "Foto", "Nama Guru", "Username (Gmail)", "Mata Pelajaran"};
        private List<TeacherRecord> rows = new ArrayList<>();
        private List<Boolean> checks = new ArrayList<>();
        private int startNumber = 1;

        void setData(List<TeacherRecord> data, int startNumber) {
            this.rows = new ArrayList<>(data);
            this.checks = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                checks.add(false);
            }
            this.startNumber = startNumber;
            fireTableDataChanged();
        }

        void setAllChecked(boolean checked) {
            for (int i = 0; i < checks.size(); i++) {
                checks.set(i, checked);
            }
            fireTableDataChanged();
        }

        List<TeacherRecord> getCheckedRecords() {
            List<TeacherRecord> out = new ArrayList<>();
            for (int i = 0; i < rows.size(); i++) {
                if (checks.get(i)) {
                    out.add(rows.get(i));
                }
            }
            return out;
        }

        TeacherRecord getRecordAt(int row) {
            return (row >= 0 && row < rows.size()) ? rows.get(row) : null;
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return cols.length;
        }

        @Override
        public String getColumnName(int col) {
            return cols[col];
        }

        @Override
        public Class<?> getColumnClass(int col) {
            if (col == 0) {
                return Boolean.class;
            }
            if (col == 2) {
                return java.awt.image.BufferedImage.class;
            }
            return String.class;
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col == 0;
        }

        @Override
        public void setValueAt(Object aValue, int row, int col) {
            if (col == 0 && aValue instanceof Boolean) {
                checks.set(row, (Boolean) aValue);
                fireTableCellUpdated(row, col);
            }
        }

        @Override
        public Object getValueAt(int row, int col) {
            TeacherRecord t = rows.get(row);
            switch (col) {
                case 0:
                    return checks.get(row);
                case 1:
                    return String.valueOf(startNumber + row);
                case 2:
                    return t.photo;
                case 3:
                    return t.name.isEmpty() ? t.username : t.name;
                case 4:
                    return t.username;
                case 5:
                    return t.subjectsText();
                default:
                    return "";
            }
        }
    }
}