# RIKUA (Rekan Informasi KUA)

Aplikasi Android untuk layanan informasi dan pra-pendaftaran administrasi pernikahan di KUA Kecamatan Randudongkal, Kabupaten Pemalang. RIKUA tidak menggantikan SIMKAH Kemenag: aplikasi ini membantu masyarakat menyiapkan dan mengunggah berkas, sedangkan petugas memeriksa dokumen lalu menginput ke SIMKAH.

## Fitur

- Login dan registrasi pengguna (email/password dan Google Sign-In), login khusus administrator, serta logout dari menu drawer dengan konfirmasi
- Informasi layanan KUA: syarat nikah, rekomendasi nikah luar kecamatan, isbat nikah, duplikat buku nikah, wali nikah, taukil wali, wakaf
- Unduh blanko N1 sampai N6
- Direktori sarana keagamaan (masjid, musholla, pesantren, TPQ, madrasah)
- Konsultasi dengan petugas lewat WhatsApp
- Pendaftaran nikah 6 langkah dengan unggah dokumen dinamis
- Cek status pendaftaran memakai kode tiket
- Pengumuman kehendak nikah

## Teknologi

| Bagian | Teknologi |
|---|---|
| Aplikasi | Android native (Java, XML, ViewBinding), minSdk 24 |
| Autentikasi | Google Sign-In (Credential Manager) + token sesi via backend `RIKUA_auth_backend` |
| Backend | PHP (REST sederhana, respons JSON) |
| Database | MySQL |
| Lingkungan lokal | XAMPP (Apache + MySQL) |

## Struktur repositori

```
RIKUA/
├── app/         Kode aplikasi Android
├── rikua_api/   Backend PHP (endpoint API dan folder uploads)
├── database/    Skema dan data master MySQL
└── asset/       Aset desain
```

Backend autentikasi (`RIKUA_auth_backend/`, dipakai `ApiConfig.java`) merupakan proyek PHP terpisah yang tidak ikut di repo ini; skema tabel user/adminnya tidak didistribusikan di sini.

## Cara menjalankan

### 1. Siapkan database

Buat database lalu impor dua file SQL, strukturnya dulu:

```bash
mysql -u root -p -e "CREATE DATABASE rikua CHARACTER SET utf8mb4"
mysql -u root -p rikua < database/01_struktur.sql
mysql -u root -p rikua < database/02_data_master.sql
```

Atau lewat phpMyAdmin: buat database `rikua`, lalu impor `01_struktur.sql` dan `02_data_master.sql` secara berurutan.

### 2. Jalankan backend

1. Salin folder `rikua_api` ke `C:\xampp\htdocs\rikua_api`.
2. Buat folder `uploads/dokumen` di dalamnya untuk menampung berkas pendaftar.
3. Letakkan juga `RIKUA_auth_backend` di `htdocs` agar login berjalan (lihat `ApiConfig.java`).
4. Jalankan Apache dan MySQL dari XAMPP.
5. Uji di peramban: `http://localhost/rikua_api/get_layanan.php` harus menampilkan JSON.

### 3. Hubungkan aplikasi

Alamat server di kode Android masih ditulis langsung. Ganti `192.168.1.6` dengan IP komputer Anda (cek dengan `ipconfig`) di semua file Java, dan tambahkan IP yang sama ke `app/src/main/res/xml/network_security_config.xml`. Gunakan fitur Find and Replace di Android Studio (Ctrl+Shift+R). Ponsel dan komputer harus berada di jaringan Wi-Fi yang sama.

Setelah itu, buka proyek di Android Studio, tunggu sinkronisasi Gradle, lalu jalankan di emulator atau perangkat.

## Tabel database

| Jenis | Tabel |
|---|---|
| Transaksi | `trx_pendaftaran_nikah`, `trx_calon_pengantin`, `trx_pihak_terkait`, `trx_dokumen_pendaftaran` |
| Master | `mst_desa`, `mst_jenis_dokumen`, `mst_sarana_keagamaan` |
| Konten | `layanan_kua`, `kategori_syarat`, `detail_syarat`, `langkah_pendaftaran`, `rekomendasi_nikah`, `blanko`, `petugas` |

Data transaksi dan data petugas tidak disertakan di repositori. Skema `trx_*` ada di `01_struktur.sql`, sedangkan `02_data_master.sql` hanya berisi data konten. Relasi antar tabel (ERD) dan pemetaan tabel ke endpoint API didokumentasikan di `progres.md` bagian Database.

## Keterbatasan

- Aplikasi masih berjalan di jaringan lokal lewat HTTP tanpa enkripsi. Versi produksi wajib memakai HTTPS.
- Backend memakai akun MySQL `root` tanpa kata sandi (hanya untuk pengembangan lokal). Versi produksi perlu akun database khusus dengan kata sandi.
- Login pengguna dan login admin sudah tersedia, tetapi panel pengelolaan konten untuk admin belum dibangun. Data konten masih dikelola langsung lewat database.
- Pendaftaran di aplikasi berfungsi sebagai pra-pendaftaran. Input resmi tetap dilakukan petugas di SIMKAH Kemenag.