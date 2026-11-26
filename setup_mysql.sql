-- Jalankan sekali di server MySQL/MariaDB (lewat phpMyAdmin tab SQL, atau klien mysql).
-- Setelah itu isi database.properties dengan user dan password di bawah ini.

CREATE DATABASE IF NOT EXISTS sekolah CHARACTER SET utf8mb4;

-- Ganti GANTI_PASSWORD_KUAT dengan kata sandi yang kuat.
-- '%' berarti boleh dari komputer mana pun di jaringan. Lebih aman dibatasi ke jaringan sekolah,
-- misalnya 'pendataan'@'192.168.1.%'.
CREATE USER IF NOT EXISTS 'pendataan'@'%' IDENTIFIED BY 'GANTI_PASSWORD_KUAT';

-- Hanya hak yang dibutuhkan aplikasi, dan hanya pada database sekolah.
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX ON sekolah.* TO 'pendataan'@'%';
FLUSH PRIVILEGES;

-- Catatan MySQL 8 (bukan MariaDB/XAMPP): driver yang disertakan butuh autentikasi lama. Pakai ini
-- sebagai ganti baris CREATE USER di atas:
--   CREATE USER IF NOT EXISTS 'pendataan'@'%' IDENTIFIED WITH mysql_native_password BY 'GANTI_PASSWORD_KUAT';
