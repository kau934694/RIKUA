-- phpMyAdmin SQL Dump
-- version 5.2.0
-- https://www.phpmyadmin.net/
--
-- Host: localhost:3306
-- Generation Time: Nov 08, 2026 at 03:51 AM
-- Server version: 8.0.30
-- PHP Version: 8.2.29

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `rikua`
--

-- --------------------------------------------------------

--
-- Table structure for table `blanko`
--

CREATE TABLE IF NOT EXISTS `blanko` (
  `id_blanko` int NOT NULL AUTO_INCREMENT,
  `kode_formulir` varchar(10) NOT NULL,
  `nama_formulir` varchar(150) NOT NULL,
  `deskripsi` text,
  `ukuran_file` varchar(20) DEFAULT '120 KB',
  `kategori_syarat` varchar(50) DEFAULT 'Wajib',
  `file_pdf` varchar(255) NOT NULL,
  `dibuat_pada` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `diperbarui_pada` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_blanko`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `detail_syarat`
--

CREATE TABLE IF NOT EXISTS `detail_syarat` (
  `id` int NOT NULL AUTO_INCREMENT,
  `kategori_id` int NOT NULL,
  `judul_syarat` varchar(255) NOT NULL,
  `keterangan` text NOT NULL,
  `urutan` int DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `kategori_id` (`kategori_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `kategori_syarat`
--

CREATE TABLE IF NOT EXISTS `kategori_syarat` (
  `id` int NOT NULL AUTO_INCREMENT,
  `nama_kategori` varchar(150) NOT NULL,
  `deskripsi` text,
  `ikon` varchar(50) DEFAULT 'checklist',
  `warna_bg` varchar(50) DEFAULT 'bg-[#0F6B5C]/10',
  `warna_teks` varchar(50) DEFAULT 'text-primary',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `langkah_pendaftaran`
--

CREATE TABLE IF NOT EXISTS `langkah_pendaftaran` (
  `id` int NOT NULL AUTO_INCREMENT,
  `nomor_langkah` int NOT NULL,
  `judul` varchar(150) NOT NULL,
  `deskripsi` text NOT NULL,
  `is_highlight` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `layanan_kua`
--

CREATE TABLE IF NOT EXISTS `layanan_kua` (
  `id` int NOT NULL AUTO_INCREMENT,
  `nama_layanan` varchar(150) NOT NULL,
  `kategori` varchar(50) NOT NULL,
  `badge_info` varchar(50) NOT NULL,
  `deskripsi` text NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `mst_desa`
--

CREATE TABLE IF NOT EXISTS `mst_desa` (
  `id_desa` int NOT NULL AUTO_INCREMENT,
  `nama_desa` varchar(80) NOT NULL,
  PRIMARY KEY (`id_desa`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `mst_jenis_dokumen`
--

CREATE TABLE IF NOT EXISTS `mst_jenis_dokumen` (
  `id_jenis_dokumen` int NOT NULL AUTO_INCREMENT,
  `nama_dokumen` varchar(150) NOT NULL,
  `kategori` enum('utama','tambahan','khusus') DEFAULT 'utama',
  `kondisi_kode` varchar(40) DEFAULT NULL,
  `id_blanko` int DEFAULT NULL,
  `wajib` tinyint(1) DEFAULT '1',
  `urutan` smallint DEFAULT '0',
  `aktif` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id_jenis_dokumen`),
  KEY `id_blanko` (`id_blanko`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `mst_sarana_keagamaan`
--

CREATE TABLE IF NOT EXISTS `mst_sarana_keagamaan` (
  `id_sarana` int NOT NULL AUTO_INCREMENT,
  `jenis` enum('masjid','musholla','tpq','madrasah','pesantren') NOT NULL,
  `nama_sarana` varchar(150) NOT NULL,
  `alamat` text NOT NULL,
  `id_desa` int NOT NULL,
  `nama_pengurus` varchar(100) DEFAULT NULL,
  `kontak` varchar(20) DEFAULT NULL,
  `jumlah_anggota` int NOT NULL DEFAULT '0',
  `foto` varchar(255) DEFAULT NULL,
  `latitude` decimal(10,7) DEFAULT NULL,
  `longitude` decimal(10,7) DEFAULT NULL,
  `id_petugas` int DEFAULT NULL,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_sarana`),
  KEY `fk_sarana_desa` (`id_desa`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `petugas`
--

CREATE TABLE IF NOT EXISTS `petugas` (
  `id` int NOT NULL AUTO_INCREMENT,
  `nama` varchar(100) NOT NULL,
  `jabatan` varchar(100) NOT NULL,
  `jam_layanan` varchar(50) NOT NULL,
  `bidang_keahlian` text NOT NULL,
  `nomor_wa` varchar(20) NOT NULL,
  `status` varchar(50) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `rekomendasi_nikah`
--

CREATE TABLE IF NOT EXISTS `rekomendasi_nikah` (
  `id_rekomendasi` int NOT NULL AUTO_INCREMENT,
  `kode_tiket` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nik_pemohon` char(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nama_lengkap` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `no_hp_kontak` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `desa_asal` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'Warungpring',
  `kecamatan_tujuan` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tanggal_pengajuan` datetime DEFAULT CURRENT_TIMESTAMP,
  `status` enum('Pending','Diproses','Selesai','Ditolak') COLLATE utf8mb4_unicode_ci DEFAULT 'Pending',
  `catatan_petugas` text COLLATE utf8mb4_unicode_ci,
  `id_petugas` int DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_rekomendasi`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `trx_calon_pengantin`
--

CREATE TABLE IF NOT EXISTS `trx_calon_pengantin` (
  `id_calon` int NOT NULL AUTO_INCREMENT,
  `id_pendaftaran` int NOT NULL,
  `peran` enum('suami','istri') NOT NULL,
  `nama_lengkap` varchar(100) NOT NULL,
  `nik` char(16) NOT NULL,
  `tempat_lahir` varchar(60) NOT NULL,
  `tanggal_lahir` date NOT NULL,
  `agama` varchar(20) NOT NULL DEFAULT 'Islam',
  `pekerjaan` varchar(80) NOT NULL,
  `pendidikan_terakhir` varchar(40) NOT NULL,
  `alamat` text NOT NULL,
  `no_hp` varchar(20) NOT NULL,
  `status_perkawinan` enum('belum_menikah','cerai_hidup','cerai_mati') NOT NULL,
  `anak_pertama` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id_calon`),
  UNIQUE KEY `uq_pendaftaran_peran` (`id_pendaftaran`,`peran`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `trx_dokumen_pendaftaran`
--

CREATE TABLE IF NOT EXISTS `trx_dokumen_pendaftaran` (
  `id_dokumen` int NOT NULL AUTO_INCREMENT,
  `id_pendaftaran` int NOT NULL,
  `id_jenis_dokumen` int NOT NULL,
  `file_path` varchar(255) DEFAULT NULL,
  `ukuran_kb` int DEFAULT '0',
  `status` enum('belum_diunggah','terunggah','perlu_perbaikan','diverifikasi') DEFAULT 'belum_diunggah',
  `catatan` text,
  `id_petugas` int DEFAULT NULL,
  `uploaded_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `diperiksa_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id_dokumen`),
  KEY `id_pendaftaran` (`id_pendaftaran`),
  KEY `id_jenis_dokumen` (`id_jenis_dokumen`),
  KEY `id_petugas` (`id_petugas`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `trx_pendaftaran_nikah`
--

CREATE TABLE IF NOT EXISTS `trx_pendaftaran_nikah` (
  `id_pendaftaran` int NOT NULL AUTO_INCREMENT,
  `kode_tiket` varchar(30) DEFAULT NULL,
  `tanggal_daftar` datetime DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'draf',
  `tanggal_akad` datetime DEFAULT NULL,
  `lokasi_akad` varchar(255) DEFAULT NULL,
  `akad_luar_kecamatan` tinyint(1) NOT NULL DEFAULT '0',
  `usia_di_bawah_21` tinyint(1) NOT NULL DEFAULT '0',
  `usia_di_bawah_19` tinyint(1) NOT NULL DEFAULT '0',
  `anggota_tni_polri` tinyint(1) NOT NULL DEFAULT '0',
  `poligami` tinyint(1) NOT NULL DEFAULT '0',
  `daftar_kurang_10_hari` tinyint(1) NOT NULL DEFAULT '0',
  `persetujuan_data` tinyint(1) NOT NULL DEFAULT '0',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_pendaftaran`),
  UNIQUE KEY `kode_tiket` (`kode_tiket`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `trx_pihak_terkait`
--

CREATE TABLE IF NOT EXISTS `trx_pihak_terkait` (
  `id_pihak` int NOT NULL AUTO_INCREMENT,
  `id_pendaftaran` int NOT NULL,
  `peran` varchar(50) NOT NULL COMMENT 'Contoh: wali, saksi_1, saksi_2',
  `nama_lengkap` varchar(100) NOT NULL,
  `nik` char(16) NOT NULL,
  `hubungan` varchar(50) DEFAULT NULL COMMENT 'Hubungan nasab wali atau keterangan saksi',
  PRIMARY KEY (`id_pihak`),
  KEY `fk_pendaftaran_pihak` (`id_pendaftaran`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `detail_syarat`
--
ALTER TABLE `detail_syarat`
  ADD CONSTRAINT `detail_syarat_ibfk_1` FOREIGN KEY (`kategori_id`) REFERENCES `kategori_syarat` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `mst_jenis_dokumen`
--
ALTER TABLE `mst_jenis_dokumen`
  ADD CONSTRAINT `mst_jenis_dokumen_ibfk_1` FOREIGN KEY (`id_blanko`) REFERENCES `blanko` (`id_blanko`) ON DELETE SET NULL;

--
-- Constraints for table `mst_sarana_keagamaan`
--
ALTER TABLE `mst_sarana_keagamaan`
  ADD CONSTRAINT `fk_sarana_desa` FOREIGN KEY (`id_desa`) REFERENCES `mst_desa` (`id_desa`) ON DELETE CASCADE;

--
-- Constraints for table `trx_calon_pengantin`
--
ALTER TABLE `trx_calon_pengantin`
  ADD CONSTRAINT `fk_calon_pendaftaran` FOREIGN KEY (`id_pendaftaran`) REFERENCES `trx_pendaftaran_nikah` (`id_pendaftaran`) ON DELETE CASCADE;

--
-- Constraints for table `trx_dokumen_pendaftaran`
--
ALTER TABLE `trx_dokumen_pendaftaran`
  ADD CONSTRAINT `trx_dokumen_pendaftaran_ibfk_1` FOREIGN KEY (`id_pendaftaran`) REFERENCES `trx_pendaftaran_nikah` (`id_pendaftaran`) ON DELETE CASCADE,
  ADD CONSTRAINT `trx_dokumen_pendaftaran_ibfk_2` FOREIGN KEY (`id_jenis_dokumen`) REFERENCES `mst_jenis_dokumen` (`id_jenis_dokumen`) ON DELETE RESTRICT,
  ADD CONSTRAINT `trx_dokumen_pendaftaran_ibfk_3` FOREIGN KEY (`id_petugas`) REFERENCES `petugas` (`id`) ON DELETE SET NULL;

--
-- Constraints for table `trx_pihak_terkait`
--
ALTER TABLE `trx_pihak_terkait`
  ADD CONSTRAINT `fk_pendaftaran_pihak` FOREIGN KEY (`id_pendaftaran`) REFERENCES `trx_pendaftaran_nikah` (`id_pendaftaran`) ON DELETE CASCADE ON UPDATE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
