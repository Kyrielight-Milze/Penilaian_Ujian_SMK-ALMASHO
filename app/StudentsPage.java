package app;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

import app.Components.*;
import static app.Theme.f;
import static app.Theme.fb;
import static app.Ui.*;

/** Operator: tampilan tabel ala Web TKA dengan simbol tambah, edit, hapus, dan filter. */
final class StudentsPage extends Page {
    private final JCheckBox selectAllBox = new JCheckBox();
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

    @Override
    void onShow() {
        selectAllBox.setSelected(false);
        applyFilterAndRender();
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

        // 1. Header Judul Card ala Web TKA
        JPanel cardHeader = p(new BorderLayout());
        cardHeader.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.border()),
            BorderFactory.createEmptyBorder(12, 18, 12, 18)
        ));
        cardHeader.add(label("\u25A4  Data Peserta / Siswa", fb(14), "title"), BorderLayout.WEST);
        mainCard.add(cardHeader, BorderLayout.NORTH);

        // 2. Toolbar Atas (Checkbox | Rekap | + | Edit | Hapus || Filter | Cari) + Tabel
        JPanel centerArea = p(new BorderLayout());

        JPanel toolbar = p(new BorderLayout(10, 6));
        toolbar.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JPanel leftTools = p(new FlowLayout(FlowLayout.LEFT, 8, 0));
        selectAllBox.setOpaque(false);
        selectAllBox.setToolTipText("Pilih Semua");
        selectAllBox.addActionListener(e -> tableModel.setAllChecked(selectAllBox.isSelected()));

        JButton rekapBtn = makeSymbolBtn("\u25A4 Rekap \u25BE", "Rekap Jumlah Siswa", false, null);
        rekapBtn.setPreferredSize(new Dimension(92, 32));
        rekapBtn.addActionListener(e -> showRekapPopup(rekapBtn));

        JButton addBtn = makeSymbolBtn("+", "Tambah Data Siswa", true, new Color(22, 163, 74));
        addBtn.setFont(fb(16));
        addBtn.addActionListener(e -> openStudentFormDialog(null));

        JButton editBtn = makeSymbolBtn("\u270E", "Edit Data Siswa Terpilih", true, new Color(234, 179, 8));
        editBtn.setFont(fb(15));
        editBtn.addActionListener(e -> handleEditSelected());

        JButton deleteBtn = makeSymbolBtn("\uD83D\uDDD1", "Hapus Data Siswa Terpilih", true, new Color(220, 38, 38));
        deleteBtn.setFont(f(14));
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

        JButton searchBtn = makeSymbolBtn("\uD83D\uDD0D Cari", "Cari Siswa", false, null);
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
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Theme.border()));
        tableScroll.getViewport().setBackground(Theme.surface());
        centerArea.add(tableScroll, BorderLayout.CENTER);

        mainCard.add(centerArea, BorderLayout.CENTER);

        // 3. Footer Paginasi ala Web TKA
        JPanel footerBar = p(new BorderLayout());
        footerBar.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));

        JPanel leftPage = p(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton firstBtn = makePageBtn("|<");
        JButton prevBtn = makePageBtn("<");
        JButton nextBtn = makePageBtn(">");
        JButton lastBtn = makePageBtn(">|");
        JButton reloadBtn = makePageBtn("\u21BB");

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
        cm.getColumn(0).setMaxWidth(45);      // Cek
        cm.getColumn(1).setMaxWidth(48);      // No
        cm.getColumn(2).setMaxWidth(58);      // Foto
        cm.getColumn(3).setPreferredWidth(180); // Nama Lengkap
        cm.getColumn(4).setMaxWidth(85);      // Kelas
        cm.getColumn(5).setPreferredWidth(160); // Alamat
        cm.getColumn(6).setPreferredWidth(165); // Email
        cm.getColumn(7).setPreferredWidth(165); // Username
        cm.getColumn(8).setPreferredWidth(115); // Telepon

        // Renderer Kolom Foto: JIKA PP KOSONG JANGAN GAMBAR HURUF DEPAN (TA/IM)
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

        // Renderer teks kolom lain mengikuti tema terang/gelap
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

        // Klik ganda pada baris membuka detail lengkap siswa
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

    private JButton makeSymbolBtn(String text, String tooltip, boolean colored, Color bg) {
        JButton btn = new JButton(text);
        btn.setToolTipText(tooltip);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(34, 32));
        if (colored && bg != null) {
            btn.setBackground(bg);
            btn.setForeground(Color.WHITE);
            btn.setBorder(BorderFactory.createLineBorder(bg));
        } else {
            btn.setBackground(Theme.input());
            btn.setForeground(Theme.text());
            btn.setFont(fb(12));
            btn.setBorder(BorderFactory.createLineBorder(Theme.border()));
        }
        return btn;
    }

    private JButton makePageBtn(String text) {
        JButton btn = new JButton(text);
        btn.setFont(f(11));
        btn.setFocusPainted(false);
        btn.setMargin(new Insets(3, 8, 3, 8));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void showRekapPopup(Component invoker) {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(Theme.surface());
        long k10 = DataStore.STUDENTS.stream().filter(s -> "10".equals(s.studentClass)).count();
        long k11 = DataStore.STUDENTS.stream().filter(s -> "11".equals(s.studentClass)).count();
        long k12 = DataStore.STUDENTS.stream().filter(s -> "12".equals(s.studentClass)).count();
        menu.add(new JMenuItem("Total Semua Siswa: " + DataStore.STUDENTS.size()));
        menu.addSeparator();
        menu.add(new JMenuItem("Kelas 10: " + k10 + " siswa"));
        menu.add(new JMenuItem("Kelas 11: " + k11 + " siswa"));
        menu.add(new JMenuItem("Kelas 12: " + k12 + " siswa"));
        menu.show(invoker, 0, invoker.getHeight() + 4);
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

    // ===================== MODAL DIALOG TAMBAH / EDIT SISWA =====================
    private void openStudentFormDialog(StudentRecord editing) {
        boolean isEdit = (editing != null);
        JDialog dialog = new JDialog(app.frame(),
            isEdit ? "Ubah Data Siswa" : "Tambah Data Siswa",
            Dialog.ModalityType.APPLICATION_MODAL);

        PhotoPicker picker = new PhotoPicker();
        JTextField namaField = new JTextField(isEdit ? editing.name : "");
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
            userField.setForeground(Theme.muted());
        }

        JPanel form = p(new GridBagLayout());
        addRow(form, 0, "Foto profil", picker);
        addRow(form, 1, "Nama", namaField);
        addRow(form, 2, "Kelas", kelasBox);
        addRow(form, 3, "Alamat", alamatField);
        addRow(form, 4, "Email", emailField);
        addRow(form, 5, "Nomor Telepon", teleponField);
        addRow(form, 6, "Username (Gmail)", userField);
        addRow(form, 7, isEdit ? "Password baru (opsional)" : "Password siswa", createPasswordControl(passField));

        AppButton saveBtn = new AppButton(isEdit ? "Simpan Perubahan" : "Simpan Data Siswa", true);
        AppButton cancelBtn = new AppButton("Batal", false);
        cancelBtn.addActionListener(e -> dialog.dispose());

        saveBtn.addActionListener(e -> {
            String nama = namaField.getText().trim();
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
                dialog.dispose();
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
                dialog.dispose();
                onShow();
                AppDialog.show(app.frame(), AppDialog.SUCCESS, "Berhasil", "Data siswa dan akun login berhasil dibuat.");
            }
        });

        JPanel btnRow = p(new GridLayout(1, 2, 10, 0));
        btnRow.add(cancelBtn);
        btnRow.add(saveBtn);

        JPanel head = p(new GridLayout(0, 1, 0, 2));
        head.add(label(isEdit ? "Ubah biodata dan akun siswa" : "Input biodata dan akun siswa", fb(16), "title"));
        if (isEdit) {
            head.add(label(EDIT_HINT, f(12), "gold"));
        }

        JPanel body = p(new BorderLayout(0, 14));
        body.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        body.add(head, BorderLayout.NORTH);
        body.add(formScroll(form), BorderLayout.CENTER);
        body.add(btnRow, BorderLayout.SOUTH);

        dialog.setContentPane(body);
        Theme.apply(body);
        dialog.setSize(520, 540);
        dialog.setLocationRelativeTo(app.frame());
        dialog.setVisible(true);
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

    // ===================== TABLE MODEL SISWA =====================
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