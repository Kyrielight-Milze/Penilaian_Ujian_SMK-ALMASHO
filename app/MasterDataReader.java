package app;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Membaca tabel dari file Excel (.xlsx) atau CSV tanpa library tambahan. Hasilnya daftar baris berisi teks. */
final class MasterDataReader {
    private MasterDataReader() {
    }

    static List<String[]> read(File file) throws Exception {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".xlsx")) {
            return readXlsx(file);
        }
        if (name.endsWith(".csv") || name.endsWith(".txt")) {
            return readCsv(file);
        }
        throw new IOException("Format file tidak didukung. Gunakan .xlsx atau .csv.");
    }

    // ===================== CSV =====================
    private static List<String[]> readCsv(File file) throws IOException {
        String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }
        int lineEnd = text.indexOf('\n');
        String first = lineEnd < 0 ? text : text.substring(0, lineEnd);
        char delimiter = ',';
        int best = count(first, ',');
        if (count(first, ';') > best) {
            delimiter = ';';
            best = count(first, ';');
        }
        if (count(first, '\t') > best) {
            delimiter = '\t';
        }

        List<String[]> rows = new ArrayList<>();
        List<String> cells = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    cell.append(ch);
                }
            } else if (ch == '"') {
                quoted = true;
            } else if (ch == delimiter) {
                cells.add(cell.toString().trim());
                cell.setLength(0);
            } else if (ch == '\n' || ch == '\r') {
                if (ch == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                cells.add(cell.toString().trim());
                cell.setLength(0);
                rows.add(cells.toArray(new String[0]));
                cells.clear();
            } else {
                cell.append(ch);
            }
        }
        if (cell.length() > 0 || !cells.isEmpty()) {
            cells.add(cell.toString().trim());
            rows.add(cells.toArray(new String[0]));
        }
        return rows;
    }

    private static int count(String text, char ch) {
        int n = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == ch) {
                n++;
            }
        }
        return n;
    }

    // ===================== XLSX =====================
    private static Document parse(InputStream in) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(in);
    }

    private static List<String[]> readXlsx(File file) throws Exception {
        try (ZipFile zip = new ZipFile(file)) {
            List<String> shared = new ArrayList<>();
            ZipEntry ss = zip.getEntry("xl/sharedStrings.xml");
            if (ss != null) {
                try (InputStream in = zip.getInputStream(ss)) {
                    NodeList items = parse(in).getElementsByTagName("si");
                    for (int i = 0; i < items.getLength(); i++) {
                        shared.add(allText((Element) items.item(i)));
                    }
                }
            }
            ZipEntry sheet = zip.getEntry("xl/worksheets/sheet1.xml");
            if (sheet == null) {
                var entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry e = entries.nextElement();
                    if (e.getName().startsWith("xl/worksheets/") && e.getName().endsWith(".xml")) {
                        sheet = e;
                        break;
                    }
                }
            }
            if (sheet == null) {
                throw new IOException("File Excel tidak berisi lembar kerja.");
            }

            List<String[]> rows = new ArrayList<>();
            try (InputStream in = zip.getInputStream(sheet)) {
                NodeList rowNodes = parse(in).getElementsByTagName("row");
                for (int i = 0; i < rowNodes.getLength(); i++) {
                    Element row = (Element) rowNodes.item(i);
                    List<String> cells = new ArrayList<>();
                    for (Node n = row.getFirstChild(); n != null; n = n.getNextSibling()) {
                        if (!(n instanceof Element) || !"c".equals(n.getNodeName())) {
                            continue;
                        }
                        Element c = (Element) n;
                        int col = columnIndex(c.getAttribute("r"));
                        while (col >= 0 && cells.size() < col) {
                            cells.add("");
                        }
                        cells.add(cellText(c, shared));
                    }
                    // nomor baris Excel dipertahankan supaya laporan "Baris n" cocok dengan yang tampak di Excel
                    int rowNo = row.getAttribute("r").isEmpty() ? rows.size() + 1 : Integer.parseInt(row.getAttribute("r"));
                    while (rows.size() < rowNo - 1) {
                        rows.add(new String[0]);
                    }
                    rows.add(cells.toArray(new String[0]));
                }
            }
            return rows;
        }
    }

    private static String cellText(Element c, List<String> shared) {
        String type = c.getAttribute("t");
        if ("inlineStr".equals(type)) {
            return allText(c).trim();
        }
        NodeList v = c.getElementsByTagName("v");
        if (v.getLength() == 0) {
            return "";
        }
        String raw = v.item(0).getTextContent().trim();
        if ("s".equals(type)) {
            try {
                return shared.get(Integer.parseInt(raw)).trim();
            } catch (Exception e) {
                return "";
            }
        }
        if (type.isEmpty() || "n".equals(type)) {
            return cleanNumber(raw);
        }
        return raw;
    }

    /** Angka dari Excel ("1.234E9", "10.0") dijadikan teks bilangan biasa. */
    private static String cleanNumber(String raw) {
        try {
            BigDecimal d = new BigDecimal(raw);
            return d.stripTrailingZeros().scale() <= 0 ? d.toBigInteger().toString() : d.toPlainString();
        } catch (NumberFormatException e) {
            return raw;
        }
    }

    private static String allText(Element e) {
        StringBuilder sb = new StringBuilder();
        NodeList ts = e.getElementsByTagName("t");
        for (int i = 0; i < ts.getLength(); i++) {
            Node parent = ts.item(i).getParentNode();
            if (parent != null && "rPh".equals(parent.getNodeName())) {
                continue; // teks furigana/fonetik dilewati
            }
            sb.append(ts.item(i).getTextContent());
        }
        return sb.toString();
    }

    /** "C5" menjadi 2 (kolom ke-3, mulai dari 0). Mengembalikan -1 bila tidak ada referensi. */
    private static int columnIndex(String ref) {
        if (ref == null || ref.isEmpty()) {
            return -1;
        }
        int col = 0;
        for (int i = 0; i < ref.length(); i++) {
            char ch = Character.toUpperCase(ref.charAt(i));
            if (ch < 'A' || ch > 'Z') {
                break;
            }
            col = col * 26 + (ch - 'A' + 1);
        }
        return col - 1;
    }
}
