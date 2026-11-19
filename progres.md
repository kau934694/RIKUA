# Progres Proyek RIKUA

Dokumen ini merangkum progres pengerjaan aplikasi Android **RIKUA** (layanan KUA Kec. Randudongkal) selama sesi kerja bersama.

Terakhir diperbarui: November 2026

---

## 1. Ringkasan Proyek

- **Aplikasi**: RIKUA — layanan digital KUA Kecamatan Randudongkal.
- **Tech stack**: Android native, Java + XML, Gradle (build via `build_with_jdk.bat`).
- **Backend**: PHP (`rikua_api`) + skema database, diakses via API lokal `http://192.168.1.8/rikua_api/...`.
- **Identitas desain**: Forest Green + Gold, font serif sebagai suara institusional, latar cream untuk area editorial.

## 2. Fitur / Halaman yang Sudah Dikerjakan

### Fitur fungsional (history commit)
- Pendaftaran Nikah, Rekomendasi Nikah, Syarat Nikah (fitur selesai).
- Isbat Nikah (layout + logika + string diperbarui).
- Jadwal, Pengumuman, Wali Nikah, Wakaf (layout + logika diperbarui).
- Facilities / Sarana Keagamaan + Facility Detail (layout, adapter, manifest).
- Backend PHP `rikua_api` + skema database ditambahkan.
- Animasi transisi antar-activity ditambahkan.
- Network security config diperbarui untuk API lokal.

### Redesign UI (workflow Antislop)
Tiga layar utama dirotasi total secara visual sambil mempertahankan **semua ID XML yang di-bind Java**, listener, Intent, panggilan API, dan navigasi:
- **Beranda** (`activity_main.xml`): semua 19 ID Java-bound dipertahankan; ditambah `progress_kuota`, `btn_kuota_retry`, `card_info`; token skala dimens baru; fokus selector `bg_search_input_state.xml`; style `Widget.Rikua.FlatCard`; perbaikan `nav_drawer_header.xml` (teks content-description salah render sebagai body).
- **Layanan** (`activity_services.xml`, `item_service.xml`): kartu flat hairline, chip filter berwarna custom (`chip_filter_bg/stroke/text`), header kompak. Catatan: keputusan header putih MaterialToolbar **ditolak user dan sudah di-restore** ke banner hijau RIKUA.
- **Konsultasi** (`activity_consultation.xml`, `item_officer.xml`, `chip_expertise.xml`): pil status `bg_status_pill_available/offline`, tile ikon putih, komposisi internal baru.

Halaman lain yang disentuh setelahnya:
- **Facilities**: strip dekoratif `view_accent_strip` dihapus (beserta pemakaiannya di `FacilityAdapter.java` — menghapus ID XML tanpa membersihkan adapter menyebabkan NPE runtime).
- **Blanko**: penyatuan warna `consult_primary` → `primary_forest`; perbaikan kontras badge Wajib (`status_green_dark` 3.44:1 → `badge_emerald_text` 5.01:1) di `BlankoAdapter.java` + `item_blanko.xml`.
- **Facility Detail**: di-refine (bukan redesign) — struktur hero CollapsingToolbarLayout 320dp, blok identitas editorial (nama serif + verifikasi SIMAS emas), dua tombol aksi sejajar (Maps + WhatsApp), spec-sheet putih tunggal 3 grup (Identitas, Kapasitas & Legal, Pengelolaan) dengan baris label-kiri / nilai-kanan, footer strip emas.
- **Rekomendasi Nikah**: hero diwarnai ulang dari gradien hijau ke cream (`surface_cream_light`), teks pindah ke token gelap (kontras 16.2:1 / 8.93:1), label gold_dark (6.13:1), watermark pola tak terlihat dihapus; `bg_rekom_hero.xml` yang jadi yatim dihapus.
- **Isbat Nikah**: arah desain editorial (kertas cream, judul serif, rule emas, timeline hijau solid) — tidak meniru template hero hijau Syarat Nikah.
- **Duplikat Buku Nikah**: identitas visual netral sendiri (putih/off-white, teks charcoal, aksen champagne) — tidak memakai tema hijau.

## 3. Perbaikan Aksesibilitas (WCAG)

Rasio kontras dihitung dengan skrip antislop-human; enam pasangan gagal diperbaiki hingga lulus AA:
- `text_tertiary` #707976 (gagal di putih) → `text_muted` #5B6663.
- Inisial putih di avatar abu/emas (1.83/~2.4) → token digelapkan.
- Hijau WhatsApp #1DA851 + teks putih (3.10) → `whatsapp_brand_dark` #0E7A44 (5.40), sekaligus memperbaiki `activity_blanko.xml`.
- Teks status "tersedia" di pill mint (3.32) → forest di `forest_container` (7.84).

## 4. Penanganan State (Loading / Empty / Error)

- Ditambahkan di Beranda, Layanan, Konsultasi: spinner + empty view + teks berbeda antara "tidak ada hasil" vs "gagal memuat" (flag `gagalMemuat`).
- `ConsultationActivity`: kini melempar error bila respons API tidak success — memperbaiki bug spinner berputar permanen.

## 5. Aturan Desain yang Disepakati

- **Halaman top-level WAJIB mempertahankan banner hijau RIKUA** di atas (`bg_hero_curved` + `bg_islamic_pattern` + logo `iv_app_logo` + "RIKUA" + "KUA Kec. Randudongkal" + `btn_menu_drawer`); dilarang diganti MaterialToolbar putih; status bar `primary_forest`, light status bars false.
- **Drawable shared** (`bg_hero_curved`, `bg_islamic_pattern`, `bg_search_field`, dll.) tidak boleh diedit — dibuat varian lokal per layar.
- Copy UI tanpa hardcoded counts (data kini API-driven), tanpa em-dash/buzzword; indikator status harus disertai teks, bukan warna saja.
- Redesign = komposisi struktural baru, bukan ganti warna tipis; hasil harus terlihat modern dan profesional.

## 6. Verifikasi & Build

- Build: `cmd /c build_with_jdk.bat :app:assembleDebug` → BUILD SUCCESSFUL.
- Lint: `lintDebug` setelah setiap batch edit; temuan pada file yang diubah dibersihkan (TooDeepLayout, SmallSp, HardcodedText, Autofill, UseCompoundDrawables, DisableBaselineAlignment), `tools:ignore` hanya dengan alasan nyata.
- Baseline lint pre-existing yang **bukan regresi**: 13 error (12 GestureBackNavigation dari `onBackPressed` activity lain, 1 NewApi `windowLightNavigationBar` di `themes.xml:23`). Project-wide warnings turun dari 415 → 387.
- Perubahan resource kecil butuh `--rerun-tasks --no-configuration-cache` agar terbukti benar-benar diproses ulang (plain assembleDebug bisa UP-TO-DATE palsu).
- Validasi UI runtime direncanakan via ADB: install APK, uiautomator dump + tap simulasi, cek logcat crash, screenshot.

## 7. Status Saat Ini

Perubahan belum di-commit (working tree berisi redesign Blanko, Duplikat Buku Nikah, Facility (Detail), Rekomendasi, Syarat Nikah, Isbat, Services + resource drawable baru, colors, strings, styles).

### Belum selesai / perlu perhatian
- [ ] Verifikasi device/emulator: rendering, font-scale clipping, perilaku animasi.
- [ ] API base URL masih mengarah ke IP LAN `192.168.1.8` — data tidak bisa load di luar jaringan tersebut.
- [ ] Commit perubahan yang sekarang (menunggu instruksi user).
- [ ] File artefak build sementara di root repo (`build_dup.txt`, `build_dup_btn.txt`, `build_id_inventory.txt`, `build_isbat.txt`) perlu dihapus/di-gitignore sebelum commit.
