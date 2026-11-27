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
import static app.Theme.f;
import static app.Theme.fb;
import static app.Ui.*;

/** Operator: tampilan tabel dengan simbol vektor, dialog modern, dan nama otomatis kapital. */
final class StudentsPage extends Page {
    private final JCheckBox selectAllBox = new JCheckBox();
    private JButton rekapBtn;
    private final JComboBox<String> classFilter = new JComboBox<>(new String[]{"Semua Kelas", "10", "11", "12"});
    private final JTextField searchInput = new JTextField(16);
    private final JComboBox<Integer> pageSizeBox = new JComboBox<>(new Integer[]{10, 20, 50, 100});
    private final JTextField pageInput = new JTextField("1", 3);
    private final JLabel totalPageLabel = label("dari 1", f(12), "muted");
    private final JLabel infoCountLabel = label("Menampilkan data 0-0 dari 0", f(12), "muted");

    private final StudentTableModel tableModel = new StudentTableModel();
    private final JTable table = new JTable(tableModel);
    private List<StudentRecord> filteredList = new ArrayList<>();
    private int currentPage = 1;

    StudentsPage(AppShell app) {
        super(app);
    }

    private static void forceUppercase(JTextField field) {
        AbstractDocument doc = (AbstractDocument) field.getDocument();
        doc.setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr)
                    throws BadLocationException {
                if (text != null) {
                    text = text.toUpperCase();
                }
                super.insertString(fb, offset, text, attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                    throws BadLocationException {
                if (text != null) {
                    text = text.toUpperCase();
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
        classFilter.setSelectedIndex(0);
    }

    @Override
    JPanel view() {
        RoundPanel mainCard = card(new BorderLayout(0, 0), 14, 0, false);

        JPanel cardHeader = p(new BorderLayout());
        cardHeader.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.border()),
            BorderFactory.createEmptyBorder(12, 18, 12, 18)
        ));
        JLabel titleLbl = label("Data Peserta / Siswa", fb(14), "title");
        titleLbl.setIcon(new VectorIcon("table", 15, Theme.text()));
        titleLbl.setIconTextGap(8);
        cardHeader.add(titleLbl, BorderLayout.WEST);
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

        rekapBtn = makeIconTextBtn("Rekap", new VectorIcon("table", 14, Theme.text()), "Rekap Data Terpilih");
        rekapBtn.setPreferredSize(new Dimension(115, 32));
        rekapBtn.addActionListener(e -> handleRekapAction());

        JButton addBtn = makeSymbolBtn(new VectorIcon("plus", 15, Color.WHITE), "Tambah Data Siswa", new Color(22, 163, 74));
        addBtn.addActionListener(e -> openStudentFormDialog(null));

        JButton editBtn = makeSymbolBtn(new VectorIcon("pencil", 15, Color.WHITE), "Edit Data Siswa Terpilih", new Color(234, 179, 8));
        editBtn.addActionListener(e -> handleEditSelected());

        JButton deleteBtn = makeSymbolBtn(new VectorIcon("trash", 15, Color.WHITE), "Hapus Data Siswa Terpilih", new Color(220, 38, 38));
        deleteBtn.addActionListener(e -> handleDeleteSelected());

        leftTools.add(selectAllBox);
        leftTools.add(rekapBtn);
        leftTools.add(addBtn);
        leftTools.add(editBtn);
        leftTools.add(deleteBtn);

        JPanel rightTools = p(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        classFilter.setFont(f(12));
        classFilter.setPreferredSize(new Dimension(125, 32));
        classFilter.addActionListener(e -> {
            currentPage = 1;
            applyFilterAndRender();
        });

        searchInput.setFont(f(12));
        searchInput.setPreferredSize(new Dimension(175, 32));
        searchInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                currentPage = 1;
                applyFilterAndRender();
            }
        });

        JButton searchBtn = makeIconTextBtn("Cari", new VectorIcon("search", 13, Theme.text()), "Cari Siswa");
        searchBtn.setPreferredSize(new Dimension(78, 32));
        searchBtn.addActionListener(e -> {
            currentPage = 1;
            applyFilterAndRender();
        });

        rightTools.add(classFilter);
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

        return pageShell("Data Siswa", "Biodata siswa beserta akun login (kelas 10, 11, atau 12)", mainCard);
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
        List<StudentRecord> checked = tableModel.getCheckedRecords();
        if (!checked.isEmpty()) {
            showRekapDialog(checked, "Rekap Data Siswa Terpilih (" + checked.size() + " Siswa)");
        } else {
            JPopupMenu menu = new JPopupMenu();
            menu.setBackground(Theme.surface());
            long k10 = DataStore.STUDENTS.stream().filter(s -> "10".equals(s.studentClass)).count();
            long k11 = DataStore.STUDENTS.stream().filter(s -> "11".equals(s.studentClass)).count();
            long k12 = DataStore.STUDENTS.stream().filter(s -> "12".equals(s.studentClass)).count();
            menu.add(new JMenuItem("Total Semua Siswa: " + DataStore.STUDENTS.size()));
            menu.add(new JMenuItem("Kelas 10: " + k10 + " | Kelas 11: " + k11 + " | Kelas 12: " + k12));
            menu.addSeparator();
            JMenuItem allItem = new JMenuItem("Buka Tabel Rekap Semua Siswa...");
            allItem.addActionListener(ev -> showRekapDialog(filteredList, "Rekap Semua Data Siswa (" + filteredList.size() + " Siswa)"));
            menu.add(allItem);
            menu.show(rekapBtn, 0, rekapBtn.getHeight() + 4);
        }
    }

    private void showRekapDialog(List<StudentRecord> list, String title) {
        final JDialog[] dialogRef = new JDialog[1];

        long k10 = list.stream().filter(s -> "10".equals(s.studentClass)).count();
        long k11 = list.stream().filter(s -> "11".equals(s.studentClass)).count();
        long k12 = list.stream().filter(s -> "12".equals(s.studentClass)).count();

        JPanel summaryBar = p(new FlowLayout(FlowLayout.LEFT, 12, 4));
        summaryBar.add(label("Total Terpilih: " + list.size() + " siswa", fb(13), "title"));
        summaryBar.add(label("\u00B7  Kelas 10: " + k10, f(12), "muted"));
        summaryBar.add(label("\u00B7  Kelas 11: " + k11, f(12), "muted"));
        summaryBar.add(label("\u00B7  Kelas 12: " + k12, f(12), "muted"));

        String[] cols = {"No", "Nama Lengkap", "Kelas", "Username", "Email", "No. Telepon", "Alamat"};
        Object[][] data = new Object[list.size()][cols.length];
        for (int i = 0; i < list.size(); i++) {
            StudentRecord s = list.get(i);
            data[i][0] = i + 1;
            data[i][1] = s.name;
            data[i][2] = "Kelas " + s.studentClass;
            data[i][3] = s.username;
            data[i][4] = s.email;
            data[i][5] = s.phone;
            data[i][6] = s.address;
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
        rekapTable.getColumnModel().getColumn(2).setMaxWidth(85);

        DefaultTableCellRenderer rekapRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean sel,
                                                           boolean foc, int row, int col) {
                super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                setBackground(sel ? Theme.accentSoft() : Theme.surface());
                setForeground(sel ? Theme.accentText() : Theme.text());
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                setHorizontalAlignment(col == 0 || col == 2 ? SwingConstants.CENTER : SwingConstants.LEFT);
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
            sb.append(title).append("\n");
            sb.append("Kelas 10: ").append(k10).append(" | Kelas 11: ").append(k11).append(" | Kelas 12: ").append(k12).append("\n\n");
            for (int i = 0; i < list.size(); i++) {
                StudentRecord s = list.get(i);
                sb.append(i + 1).append(". ").append(s.name)
                  .append(" (Kelas ").append(s.studentClass).append(") - ")
                  .append(s.username).append(" | ").append(s.phone).append("\n");
            }
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.toString()), null);
            AppDialog.show(dialogRef[0], AppDialog.SUCCESS, "Tersalin", "Rekap data terpilih berhasil disalin ke clipboard.");
        });

        AppButton closeBtn = new AppButton("Tutup", true);
        closeBtn.addActionListener(e -> dialogRef[0].dispose());

        JPanel footer = p(new GridLayout(1, 2, 12, 0));
        footer.add(copyBtn);
        footer.add(closeBtn);

        dialogRef[0] = modalDialog(
            app.frame(),
            "REKAPITULASI DATA SISWA",
            title,
            "Ringkasan biodata peserta didik berdasarkan pilihan",
            new Color(16, 185, 129),
            content,
            footer,
            780,
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

        JTableHeader header = table.getTableHeader();
        header.setFont(fb(12));
        header.setPreferredSize(new Dimension(0, 38));
        header.setReorderingAllowed(false);

        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setMaxWidth(45);
        cm.getColumn(1).setMaxWidth(48);
        cm.getColumn(2).setMaxWidth(58);
        cm.getColumn(3).setPreferredWidth(180);
        cm.getColumn(4).setMaxWidth(85);
        cm.getColumn(5).setPreferredWidth(160);
        cm.getColumn(6).setPreferredWidth(165);
        cm.getColumn(7).setPreferredWidth(165);
        cm.getColumn(8).setPreferredWidth(115);

        cm.getColumn(2).setCellRenderer((tbl, value, isSelected, hasFocus, row, col) -> {
            JPanel cell = new JPanel(new GridBagLayout());
            cell.setBackground(isSelected ? Theme.accentSoft() : Theme.surface());
            if (value instanceof java.awt.image.BufferedImage) {
                JLabel pic = new JLabel(new AvatarIcon("", 30, Theme.ACCENT, Color.WHITE,
                    (java.awt.image.BufferedImage) value));
                cell.add(pic);
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
                if (column == 1 || column == 4) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }
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
                    StudentRecord s = tableModel.getRecordAt(table.getSelectedRow());
                    if (s != null) {
                        showStudentDialog(s);
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
        List<StudentRecord> checked = tableModel.getCheckedRecords();
        StudentRecord target = null;
        if (!checked.isEmpty()) {
            target = checked.get(0);
        } else if (table.getSelectedRow() >= 0) {
            target = tableModel.getRecordAt(table.getSelectedRow());
        }
        if (target == null) {
            warn("Centang atau pilih salah satu baris siswa terlebih dahulu.", "Pilih Siswa");
            return;
        }
        openStudentFormDialog(target);
    }

    private void handleDeleteSelected() {
        List<StudentRecord> checked = tableModel.getCheckedRecords();
        if (checked.isEmpty() && table.getSelectedRow() >= 0) {
            StudentRecord sel = tableModel.getRecordAt(table.getSelectedRow());
            if (sel != null) {
                checked.add(sel);
            }
        }
        if (checked.isEmpty()) {
            warn("Centang atau pilih data siswa yang akan dihapus.", "Hapus Siswa");
            return;
        }
        String msg = checked.size() == 1
            ? "Hapus data " + checked.get(0).name + "? Akun login siswa ini juga akan dihapus."
            : "Hapus " + checked.size() + " data siswa terpilih beserta akun loginnya?";
        if (!AppDialog.confirm(app.frame(), "Hapus Data Siswa", msg, "Hapus")) {
            return;
        }
        for (StudentRecord s : checked) {
            DataStore.removeStudent(s);
        }
        selectAllBox.setSelected(false);
        onShow();
    }

    private void openStudentFormDialog(StudentRecord editing) {
        boolean isEdit = (editing != null);
        final JDialog[] dialogRef = new JDialog[1];

        PhotoPicker picker = new PhotoPicker();
        JTextField namaField = new JTextField(isEdit ? editing.name.toUpperCase() : "");
        forceUppercase(namaField);

        JComboBox<String> kelasBox = new JComboBox<>(new String[]{"10", "11", "12"});
        if (isEdit) {
            kelasBox.setSelectedItem(editing.studentClass);
        }
        JTextField alamatField = new JTextField(isEdit ? editing.address : "");
        JTextField emailField = new JTextField(isEdit ? editing.email : "");
        JTextField teleponField = new JTextField(isEdit ? editing.phone : "");
        JTextField userField = new JTextField(isEdit ? editing.username : "");
        JPasswordField passField = new JPasswordField();

        if (isEdit) {
            picker.setPhoto(editing.photo);
            userField.setEditable(false);
            userField.setToolTipText("Username tidak dapat diubah");
        }

        JPanel form = p(new GridBagLayout());
        addRow(form, 0, "Foto profil", picker);
        addRow(form, 1, "Nama lengkap", namaField);
        addRow(form, 2, "Kelas", kelasBox);
        addRow(form, 3, "Alamat", alamatField);
        addRow(form, 4, "Email", emailField);
        addRow(form, 5, "Nomor Telepon", teleponField);
        addRow(form, 6, "Username (Gmail)", userField);
        addRow(form, 7, isEdit ? "Password baru (opsional)" : "Password siswa", createPasswordControl(passField));

        JScrollPane scroll = formScroll(form);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        AppButton cancelBtn = new AppButton("Batal", false);
        cancelBtn.addActionListener(e -> dialogRef[0].dispose());

        AppButton saveBtn = new AppButton(isEdit ? "Simpan Perubahan" : "Simpan Data Siswa", true);
        saveBtn.addActionListener(e -> {
            String nama = namaField.getText().trim().toUpperCase();
            String kelas = String.valueOf(kelasBox.getSelectedItem());
            String alamat = alamatField.getText().trim();
            String email = emailField.getText().trim();
            String telepon = teleponField.getText().trim();
            String user = userField.getText().trim();
            String pass = new String(passField.getPassword());

            if (isEdit) {
                if (nama.isEmpty() || alamat.isEmpty() || email.isEmpty() || telepon.isEmpty()) {
                    warn("Nama, alamat, email, dan nomor telepon wajib diisi.", "Data belum lengkap");
                    return;
                }
                editing.name = nama;
                editing.studentClass = kelas;
                editing.address = alamat;
                editing.email = email;
                editing.phone = telepon;
                editing.photo = picker.getPhoto();
                if (!pass.isEmpty()) {
                    editing.password = pass;
                }
                DataStore.updateStudent(editing);
                dialogRef[0].dispose();
                onShow();
                AppDialog.show(app.frame(), AppDialog.SUCCESS, "Berhasil", "Perubahan data siswa berhasil disimpan.");
            } else {
                if (nama.isEmpty() || alamat.isEmpty() || email.isEmpty() || telepon.isEmpty()
                    || user.isEmpty() || pass.isEmpty()) {
                    warn("Semua biodata dan akses login siswa harus diisi.", "Data belum lengkap");
                    return;
                }
                if (!isGmail(user)) {
                    warn("Username harus berupa alamat Gmail, misalnya nama@gmail.com.", "Username tidak valid");
                    return;
                }
                if (DataStore.isUsernameTaken(user)) {
                    warn("Username tersebut sudah digunakan.", "Akun duplikat");
                    return;
                }
                StudentRecord student = new StudentRecord(nama, kelas, alamat, email, telepon, user, pass);
                student.photo = picker.getPhoto();
                DataStore.addStudent(student);
                dialogRef[0].dispose();
                onShow();
                AppDialog.show(app.frame(), AppDialog.SUCCESS, "Berhasil", "Data siswa dan akun login berhasil dibuat.");
            }
        });

        JPanel btnRow = p(new GridLayout(1, 2, 12, 0));
        btnRow.add(cancelBtn);
        btnRow.add(saveBtn);

        dialogRef[0] = modalDialog(
            app.frame(),
            isEdit ? "EDIT DATA SISWA" : "DATA SISWA BARU",
            isEdit ? "Ubah Biodata & Akun Siswa" : "Tambah Data Siswa",
            isEdit ? EDIT_HINT : "Isi biodata lengkap beserta akun login Gmail siswa",
            isEdit ? Theme.GOLD : Theme.ACCENT,
            scroll,
            btnRow,
            540,
            610
        );
        dialogRef[0].setVisible(true);
    }

    private void applyFilterAndRender() {
        String selectedClass = String.valueOf(classFilter.getSelectedItem());
        String key = searchInput.getText().trim().toLowerCase();

        filteredList = new ArrayList<>();
        for (StudentRecord s : DataStore.STUDENTS) {
            boolean matchClass = "Semua Kelas".equals(selectedClass) || s.studentClass.equals(selectedClass);
            boolean matchKey = key.isEmpty()
                || s.name.toLowerCase().contains(key)
                || s.username.toLowerCase().contains(key)
                || s.email.toLowerCase().contains(key)
                || s.address.toLowerCase().contains(key);
            if (matchClass && matchKey) {
                filteredList.add(s);
            }
        }
        renderPage();
    }

    private int getPageSize() {
        Integer sz = (Integer) pageSizeBox.getSelectedItem();
        return sz != null ? sz : 20;
    }

    private int getTotalPages() {
        int sz = getPageSize();
        return Math.max(1, (int) Math.ceil((double) filteredList.size() / sz));
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

        List<StudentRecord> slice = (total == 0) ? new ArrayList<>() : filteredList.subList(start, end);
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

    private void showStudentDialog(StudentRecord s) {
        JPanel info = p(new BorderLayout(0, 4));
        info.add(detailRows(
            new String[]{"Username", "Password"},
            new JComponent[]{valueLabel(s.username, 250), secretRow(s.password)}), BorderLayout.NORTH);
        info.add(studentInfoRows(s, 250), BorderLayout.CENTER);
        int action = PersonDialog.show(app.frame(), studentHeader(s, 56), info);
        if (action == PersonDialog.EDIT) {
            openStudentFormDialog(s);
        } else if (action == PersonDialog.DELETE) {
            if (AppDialog.confirm(app.frame(), "Hapus Data Siswa",
                "Hapus data " + s.name + "? Akun login siswa ini juga akan dihapus.", "Hapus")) {
                DataStore.removeStudent(s);
                onShow();
            }
        }
    }

    private static final class StudentTableModel extends AbstractTableModel {
        private final String[] cols = {
            "Cek", "No", "Foto", "Nama Lengkap", "Kelas", "Alamat", "Email", "Username", "No. Telepon"
        };
        private List<StudentRecord> rows = new ArrayList<>();
        private List<Boolean> checks = new ArrayList<>();
        private int startNumber = 1;

        void setData(List<StudentRecord> data, int startNumber) {
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

        List<StudentRecord> getCheckedRecords() {
            List<StudentRecord> out = new ArrayList<>();
            for (int i = 0; i < rows.size(); i++) {
                if (checks.get(i)) {
                    out.add(rows.get(i));
                }
            }
            return out;
        }

        StudentRecord getRecordAt(int row) {
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
            StudentRecord s = rows.get(row);
            switch (col) {
                case 0:
                    return checks.get(row);
                case 1:
                    return String.valueOf(startNumber + row);
                case 2:
                    return s.photo;
                case 3:
                    return s.name;
                case 4:
                    return s.studentClass;
                case 5:
                    return s.address;
                case 6:
                    return s.email;
                case 7:
                    return s.username;
                case 8:
                    return s.phone;
                default:
                    return "";
            }
        }
    }
}