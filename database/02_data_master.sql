-- phpMyAdmin SQL Dump
-- version 5.2.0
-- https://www.phpmyadmin.net/
--
-- Host: localhost:3306
-- Generation Time: Nov 08, 2026 at 03:53 AM
-- Server version: 8.0.30
-- PHP Version: 8.2.29

SET FOREIGN_KEY_CHECKS=0;
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

--
-- Dumping data for table `blanko`
--

INSERT INTO `blanko` (`id_blanko`, `kode_formulir`, `nama_formulir`, `deskripsi`, `ukuran_file`, `kategori_syarat`, `file_pdf`, `dibuat_pada`, `diperbarui_pada`) VALUES
(1, 'N1', 'Surat Pengantar Nikah', 'Diterbitkan dari kantor desa / kelurahan domisili calon pengantin.', '145 KB', 'Wajib', 'n1.pdf', '2026-10-11 16:20:41', '2026-10-11 16:20:41'),
(2, 'N2', 'Permohonan Kehendak Nikah', 'Surat resmi permohonan kehendak nikah ke KUA Randudongkal', '130 KB', 'Wajib', 'n2.pdf', '2026-10-11 16:20:41', '2026-10-11 16:50:10'),
(3, 'N3', 'Isbat Nikah', 'Formulir permohonan pengesahan nikah ke Pengadilan Agama.', '160 KB', 'Kondisional', 'n3.pdf', '2026-10-11 16:20:41', '2026-10-11 16:20:41'),
(4, 'N4', 'Persetujuan Calon Pengantin', 'Surat pernyataan kerelaan dan kesepakatan kedua mempelai.', '115 KB', 'Wajib', 'n4.pdf', '2026-10-11 16:20:41', '2026-10-11 16:20:41'),
(5, 'N5', 'Persetujuan Orang Tua', 'Surat izin orang tua/wali bagi calon pengantin berusia di bawah 21 tahun.', '125 KB', '< 21 Tahun', 'n5.pdf', '2026-10-11 16:20:41', '2026-10-11 16:20:41'),
(6, 'N6', 'Surat Keterangan Kematian', 'Keterangan kematian suami/istri terdahulu untuk status duda/janda.', '140 KB', 'Duda/Janda', 'n6.pdf', '2026-10-11 16:20:41', '2026-10-11 16:20:41');

--
-- Dumping data for table `detail_syarat`
--

INSERT INTO `detail_syarat` (`id`, `kategori_id`, `judul_syarat`, `keterangan`, `urutan`) VALUES
(1, 1, 'Surat pengantar nikah dari kelurahan/desa (Model N1)', 'Diterbitkan oleh Balai Desa setempat', 1),
(2, 1, 'Permohonan kehendak nikah (Model N2 dan N4)', 'Persetujuan kedua mempelai', 2),
(3, 1, 'Fotokopi KTP dan Kartu Keluarga (KK) calon pengantin', 'Masing-masing 2 lembar jelas (tidak buram)', 3),
(4, 1, 'Fotokopi akta kelahiran', 'Atau surat kenal lahir dari desa', 4),
(5, 1, 'Fotokopi ijazah terakhir', 'Sebagai pencocokan ejaan nama resmi', 5),
(6, 1, 'Pas foto 2x3 (4 lembar) dan 4x6 (2 lembar) latar belakang biru', 'Pakaian rapi, berkerah/berbusana muslimah', 6),
(7, 1, 'Surat keterangan sehat dari Puskesmas/Faskes', 'Pemeriksaan catin di Puskesmas Warungpring', 7),
(8, 1, 'Bukti imunisasi Tetanus Toksoid (TT) calon istri', 'Kartu vaksinasi atau surat dokter', 8),
(9, 1, 'Fotokopi KTP dan KK orang tua / wali nikah', 'Bagi wali nasab utama (Ayah kandung)', 9),
(10, 1, 'Fotokopi KTP dua orang saksi nikah', 'Saksi laki-laki dewasa beragama Islam', 10),
(11, 1, 'Surat pernyataan jejaka/perawan bermaterai', 'Materai Rp 10.000 ditandatangani pemohon', 11),
(12, 2, 'Surat rekomendasi nikah (jika akad di luar kecamatan tempat tinggal)', 'Diterbitkan oleh KUA asal pemohon', 1),
(13, 2, 'Surat izin orang tua (Model N5) jika berusia di bawah 21 tahun', 'Sesuai ketentuan pasal 6 ayat (2) UU Perkawinan', 2),
(14, 2, 'Dispensasi kawin dari Pengadilan Agama jika pria/wanita di bawah usia 19 tahun', 'Keputusan resmi dari PA Pemalang', 3),
(15, 2, 'Akta cerai asli atau surat keterangan kematian (N6) bagi duda/janda', 'Wajib lampiran asli dan fotokopi 2 rangkap', 4),
(16, 2, 'Surat izin komandan/atasan bagi anggota TNI/Polri', 'Surat izin sah dari kesatuan', 5),
(17, 2, 'Penetapan izin poligami dari Pengadilan Agama', 'Surat putusan inkrah pengadilan', 6);

--
-- Dumping data for table `kategori_syarat`
--

INSERT INTO `kategori_syarat` (`id`, `nama_kategori`, `deskripsi`, `ikon`, `warna_bg`, `warna_teks`) VALUES
(1, 'Untuk semua pendaftar', '11 berkas wajib dilengkapi', 'checklist', 'bg-[#0F6B5C]/10', 'text-primary'),
(2, 'Jika berlaku untuk Anda', 'Berkas tambahan kondisi tertentu', 'assignment_add', 'bg-[#C9A24B]/15', 'text-[#795902]');

--
-- Dumping data for table `langkah_pendaftaran`
--

INSERT INTO `langkah_pendaftaran` (`id`, `nomor_langkah`, `judul`, `deskripsi`, `is_highlight`) VALUES
(1, 1, 'Surat Pengantar Desa', 'Urus surat pengantar (N1) di kantor desa/kelurahan setempat sesuai domisili KTP Anda.', 0),
(2, 2, 'Pemeriksaan Kesehatan', 'Periksa kesehatan dan imunisasi di Puskesmas Warungpring untuk memperoleh sertifikat layak nikah.', 0),
(3, 3, 'Pendaftaran Berkas', 'Daftarkan berkas secara online melalui Simkah / RIKUA atau langsung ke KUA Warungpring (min. 10 hari kerja sebelum akad).', 0),
(4, 4, 'Pemeriksaan & Bimbingan', 'Pemeriksaan berkas dan verifikasi oleh penghulu KUA Warungpring bersama calon pengantin & wali.', 0),
(5, 5, 'Pelaksanaan Akad Nikah', 'Pelaksanaan akad nikah sesuai jadwal dan lokasi yang disepakati (di Balai Nikah KUA atau di luar KUA).', 1);

--
-- Dumping data for table `layanan_kua`
--

INSERT INTO `layanan_kua` (`id`, `nama_layanan`, `kategori`, `badge_info`, `deskripsi`) VALUES
(1, 'Syarat Nikah', 'Pernikahan', 'Lengkap', 'Dokumen dan prosedur pendaftaran nikah resmi'),
(2, 'Rekomendasi Nikah', 'Pernikahan', 'Surat Pengantar', 'Pengantar untuk pelaksanaan akad di luar kecamatan'),
(3, 'Isbat Nikah', 'Administrasi & Dokumen', 'Pengadilan Agama', 'Pengesahan nikah lewat Pengadilan Agama'),
(4, 'Duplikat Buku Nikah', 'Administrasi & Dokumen', 'Umum', 'Penggantian buku nikah yang hilang atau rusak'),
(5, 'Wakaf', 'Kemasjidan & Wakaf', 'Sosial Keagamaan', 'Ikrar wakaf dan bimbingan nadzir'),
(6, 'Wali Nikah', 'Pernikahan', 'Urutan WaliNikah', 'Urutan Lengkap Wali dan Wali Nikah'),
(7, 'Taukil Nikah', 'Pernikahan', 'PErnikahan', 'Bila Wali Berhalangan Hadir'),
(8, 'Pengumuman Kehendak Nikah', 'Pernikahan', 'Nikah', 'Rencana Akad di Wilayah Ini');

--
-- Dumping data for table `mst_desa`
--

INSERT INTO `mst_desa` (`id_desa`, `nama_desa`) VALUES
(1, 'Randudongkal'),
(2, 'Banjaranyar'),
(3, 'Gembyang'),
(4, 'Gongseng'),
(5, 'Kalimas'),
(6, 'Kalitorong'),
(7, 'Karangmoncol'),
(8, 'Kecepit'),
(9, 'Kejene'),
(10, 'Kreyo'),
(11, 'Lodaya'),
(12, 'Mangli'),
(13, 'Mejagong'),
(14, 'Penusupan'),
(15, 'Rembul'),
(16, 'Semaya'),
(17, 'Semingkir');

--
-- Dumping data for table `mst_jenis_dokumen`
--

INSERT INTO `mst_jenis_dokumen` (`id_jenis_dokumen`, `nama_dokumen`, `kategori`, `kondisi_kode`, `id_blanko`, `wajib`, `urutan`, `aktif`) VALUES
(1, 'Surat Pengantar Nikah (N1)', 'utama', NULL, 1, 1, 1, 1),
(2, 'Permohonan Kehendak Nikah (N2)', 'utama', NULL, 2, 1, 2, 1),
(3, 'Isbat Nikah (N3)', 'utama', 'isbat_nikah', 3, 0, 3, 1),
(4, 'Persetujuan Calon Pengantin (N4)', 'utama', NULL, 4, 1, 4, 1),
(5, 'KTP Calon Suami', 'utama', NULL, NULL, 1, 5, 1),
(6, 'KTP Calon Istri', 'utama', NULL, NULL, 1, 6, 1),
(7, 'Kartu Keluarga Calon Suami', 'utama', NULL, NULL, 1, 7, 1),
(8, 'Kartu Keluarga Calon Istri', 'utama', NULL, NULL, 1, 8, 1),
(9, 'Akta Kelahiran Calon Suami', 'utama', NULL, NULL, 1, 9, 1),
(10, 'Pas Foto Calon Pengantin', 'utama', NULL, NULL, 1, 10, 1),
(11, 'Persetujuan Orang Tua (N5)', 'tambahan', 'usia_di_bawah_21', 5, 0, 11, 1),
(12, 'Surat Dispensasi Nikah', 'tambahan', 'dispensasi_nikah', NULL, 0, 12, 1),
(13, 'Rekomendasi Nikah Luar Kecamatan', 'tambahan', 'luar_kecamatan', NULL, 0, 13, 1),
(14, 'Surat Keterangan Kematian (N6)', 'tambahan', 'duda_janda', 6, 0, 14, 1);

--
-- Dumping data for table `mst_sarana_keagamaan`
--

INSERT INTO `mst_sarana_keagamaan` (`id_sarana`, `jenis`, `nama_sarana`, `alamat`, `id_desa`, `nama_pengurus`, `kontak`, `jumlah_anggota`, `foto`, `latitude`, `longitude`, `id_petugas`, `updated_at`) VALUES
(1, 'madrasah', 'Madrasah Diniyah Al Ihsan', 'Jl. Mangli Raya No. 12, Desa Mangli', 12, 'K.H. M. Thoha', '081234567890', 1200, 'madrasah.jpg', '-7.1234560', '109.3456780', 1, '2026-10-12 04:16:53'),
(2, 'masjid', 'Masjid Besar Baitul Muttaqin', 'Jl. Raya Randudongkal No. 04', 1, 'H. Ahmad Fauzan, S.Pd.I.', '081328904432', 1450, 'masjid.jpeg', '-7.1256780', '109.3489000', 1, '2026-10-09 17:24:53'),
(3, 'tpq', 'TPQ Asyifa 1', 'Dusun I, Desa Randudongkal', 1, 'Ustadz H. M. Nur', '081987654321', 350, '', '-7.1289000', '109.3512300', 1, '2026-10-09 17:24:53');
SET FOREIGN_KEY_CHECKS=1;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
