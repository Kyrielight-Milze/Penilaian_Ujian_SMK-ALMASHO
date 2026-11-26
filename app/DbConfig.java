package app;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Pengaturan database, dibaca dari file database.properties di folder project.
 * Bila file tidak ada, aplikasi memakai database H2 lokal (data/ di folder project).
 */
final class DbConfig {
    final boolean mysql;
    final String host;
    final int port;
    final String name;
    final String user;
    final String password;

    private DbConfig(boolean mysql, String host, int port, String name, String user, String password) {
        this.mysql = mysql;
        this.host = host;
        this.port = port;
        this.name = name;
        this.user = user;
        this.password = password;
    }

    static DbConfig load() {
        Properties p = new Properties();
        File file = new File("database.properties");
        if (file.exists()) {
            try (Reader in = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                p.load(in);
            } catch (Exception ignored) {
                // file tidak terbaca: pakai bawaan
            }
        }
        boolean mysql = "mysql".equalsIgnoreCase(p.getProperty("type", "h2").trim());
        int port = 3306;
        try {
            port = Integer.parseInt(p.getProperty("port", "3306").trim());
        } catch (NumberFormatException ignored) {
            // pakai 3306
        }
        return new DbConfig(mysql,
            p.getProperty("host", "localhost").trim(),
            port,
            p.getProperty("name", "sekolah").trim(),
            p.getProperty("user", "root").trim(),
            p.getProperty("password", ""));
    }

    String url(boolean withDatabase) {
        return "jdbc:mariadb://" + host + ":" + port + "/" + (withDatabase ? name : "")
            + "?useUnicode=true&connectTimeout=5000&tcpKeepAlive=true";
    }

    String describe() {
        return mysql ? "MySQL " + host + ":" + port + "/" + name : "database lokal (H2)";
    }
}
