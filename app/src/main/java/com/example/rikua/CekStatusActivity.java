package com.example.rikua;

import android.app.DatePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Window;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.rikua.databinding.ActivityCekStatusBinding;
import com.example.rikua.databinding.ItemStatusTimelineBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * CekStatusActivity — Layar "Cek Status Pendaftaran" (pelacakan berkas) RIKUA
 * KUA Kec. Randudongkal.
 *
 * Alur: pengguna memasukkan nomor tiket (RKA-YYYY-NNNNN) + tanggal lahir calon
 * pengantin, lalu aplikasi mengirim GET ke backend PHP
 * ({@value #API_URL} dengan parameter kode_tiket & tanggal_lahir), mem-parse
 * JSON di thread latar, dan merender hasil secara dinamis: identitas pasangan,
 * badge status aktif, timeline vertikal 5 tahap (Diterima → Sedang Diperiksa →
 * Perlu Perbaikan → Lengkap → Dijadwalkan) dengan penyorotan tahap berjalan,
 * serta kartu catatan petugas. Bila status "perlu_perbaikan", tombol
 * "Unggah Ulang Dokumen" mengarahkan kembali ke Langkah 5 (form unggah berkas).
 *
 * Mengikuti konvensi pra-integrasi backend proyek: saat server tidak terjangkau
 * atau data tidak ditemukan, ditampilkan contoh hasil pelacakan (demo) agar
 * layar tetap representatif, disertai Snackbar informatif. Parsing JSON null-safe
 * dan pembaruan UI selalu melewati pemeriksaan {@code binding != null} di main
 * thread (hindari kebocoran pasca-destroy).
 *
 * Implementasi memakai View Binding (ActivityCekStatusBinding).
 */
public class CekStatusActivity extends AppCompatActivity {

    private static final String URL_SIMKAH = "https://simkah.kemenag.go.id";

    /** Endpoint pelacakan status berkas (parameter kode_tiket & tanggal_lahir). */
    private static final String API_URL = "http://192.168.1.23/rikua_api/api_cek_status.php";

    /** Admin KUA Randudongkal untuk konsultasi status (format 62…). */
    private static final String ADMIN_WHATSAPP = "6287787080948";

    private static final Pattern POLA_TANGGAL = Pattern.compile("\\d{2}/\\d{2}/\\d{4}");

    // State dot timeline
    private static final int TAHAP_SELESAI = 0;
    private static final int TAHAP_AKTIF = 1;
    private static final int TAHAP_MENUNGGU = 2;

    // Kode status backend → indeks tahap aktif pada timeline (0..4)
    private static final String ST_DITERIMA = "diterima";
    private static final String ST_DIPERIKSA = "diperiksa";
    private static final String ST_PERBAIKAN = "perlu_perbaikan";
    private static final String ST_LENGKAP = "lengkap";
    private static final String ST_JADWAL = "dijadwalkan";

    private ActivityCekStatusBinding binding;
    private String tiketTerakhir = "";

    // =======================================================================
    // Lifecycle
    // =======================================================================

@Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCekStatusBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Penanganan Back Press Modern menggunakan OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding != null && binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.END);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });

        setupWindowAndStatusBar();
        setupToolbar();
        setupDrawer();
        setupTanggalLahir();
        setupClearErrorOnInput();
        setupActions();
        setupBottomNavigation();

        praIsiDariIntent();
    } // <--- Pastikan kurung kurawal penutup onCreate ini ada di sini!penutup method onCreate() yang sah

    @Override
    protected void onDestroy() {
        binding = null;
        super.onDestroy();
    }

    /** Bila dipanggil dari modal sukses Langkah 6, tiket ikut terisi otomatis. */
    private void praIsiDariIntent() {
        String tiket = getIntent().getStringExtra("kode_tiket");
        if (tiket != null && !tiket.trim().isEmpty()) {
            binding.etKodeTiket.setText(tiket);
            binding.etKodeTiket.setSelection(binding.etKodeTiket.getText().length());
        }
    }

    // =======================================================================
    // Setup tampilan
    // =======================================================================

    private void setupWindowAndStatusBar() {
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.md3_primary));

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(true);

        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutBottomSticky, (view, insets) -> {
            androidx.core.graphics.Insets bars =
                    insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(view.getPaddingLeft(), view.getPaddingTop(),
                    view.getPaddingRight(), bars.bottom);
            return insets;
        });
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_open_drawer) {
                toggleDrawer();
                return true;
            }
            return false;
        });
    }

    private void setupDrawer() {
        binding.navDrawer.setNavigationItemSelectedListener(item -> {
            binding.drawerLayout.closeDrawer(GravityCompat.END);
            handleNavigation(item.getItemId());
            return true;
        });
    }

    private void toggleDrawer() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
            binding.drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            binding.drawerLayout.openDrawer(GravityCompat.END);
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = binding.bottomNavigation;
        bottomNav.setSelectedItemId(R.id.menu_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_lainnya) {
                toggleDrawer();
                bottomNav.setSelectedItemId(R.id.menu_home);
                return false;
            }
            handleNavigation(id);
            return true;
        });
    }

    // =======================================================================
    // Input tanggal lahir & pembersihan error
    // =======================================================================

    private void setupTanggalLahir() {
        binding.etTanggalLahir.setOnClickListener(v -> tampilkanDatePicker());
    }

    private void tampilkanDatePicker() {
        Calendar now = Calendar.getInstance();
        Calendar initial = Calendar.getInstance();
        initial.add(Calendar.YEAR, -22); // asumsi dewasa; bisa diubah pengguna

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    binding.etTanggalLahir.setText(
                            String.format(Locale.US, "%02d/%02d/%04d", dayOfMonth, month + 1, year));
                    binding.tilTanggalLahir.setError(null);
                },
                initial.get(Calendar.YEAR), initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMaxDate(now.getTimeInMillis());
        dialog.show();
    }

    private void setupClearErrorOnInput() {
        binding.etKodeTiket.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().trim().isEmpty()) binding.tilKodeTiket.setError(null);
            }
        });
    }

    // =======================================================================
    // Aksi
    // =======================================================================

    private void setupActions() {
        binding.btnCekStatus.setOnClickListener(v -> jalankanPencarian());
        binding.btnWa.setOnClickListener(v -> bukaWhatsApp());
        binding.btnUnggahUlang.setOnClickListener(v -> bukaLangkahDokumen());
    }

    /** Validasi input lalu kirim pencarian status ke backend (thread latar). */
    private void jalankanPencarian() {
        String tiket = binding.etKodeTiket.getText().toString().trim();
        String tanggal = binding.etTanggalLahir.getText().toString().trim();

        boolean valid = true;
        if (tiket.isEmpty()) {
            binding.tilKodeTiket.setError(getString(R.string.cs_err_kode_tiket));
            valid = false;
        } else {
            binding.tilKodeTiket.setError(null);
        }
        if (!POLA_TANGGAL.matcher(tanggal).matches()) {
            binding.tilTanggalLahir.setError(getString(R.string.cs_err_tanggal_lahir));
            valid = false;
        } else {
            binding.tilTanggalLahir.setError(null);
        }
        if (!valid) return;

        tiketTerakhir = tiket;
        binding.btnCekStatus.setEnabled(false);
        Snackbar.make(binding.getRoot(), R.string.cs_loading, Snackbar.LENGTH_SHORT).show();
        new Thread(() -> cariStatus(tiket, tanggal)).start();
    }

    /**
     * GET {@code api_cek_status.php?kode_tiket=..&tanggal_lahir=..}. Pada respons
     * sukses (data ditemukan) render langsung dari server; saat tidak ditemukan /
     * jaringan gagal tampilkan contoh data agar pelacakan tetap terlihat.
     */
    private void cariStatus(String tiket, String tanggal) {
        HttpURLConnection conn = null;
        String hasilJson = null;
        boolean koneksiOK = false;
        try {
            String url = API_URL + "?kode_tiket=" + URLEncoder.encode(tiket, "UTF-8")
                    + "&tanggal_lahir=" + URLEncoder.encode(tanggal, "UTF-8");
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.connect();

            koneksiOK = conn.getResponseCode() == HttpURLConnection.HTTP_OK;
            if (koneksiOK) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                }
                hasilJson = sb.toString();
            }
        } catch (Exception e) {
            koneksiOK = false;
        } finally {
            if (conn != null) conn.disconnect();
        }

        final boolean ok = koneksiOK;
        final String json = hasilJson;
        runOnUiThread(() -> {
            if (binding == null) return;
            binding.btnCekStatus.setEnabled(true);

            if (ok && json != null && terapkanRespons(json)) {
                return; // sukses render dari server
            }
            // Tidak ditemukan / offline → contoh hasil pelacakan.
            binding.layoutKosong.setVisibility(android.view.View.VISIBLE);
            binding.cardHasil.setVisibility(android.view.View.GONE);

            Snackbar.make(binding.getRoot(),
                    ok ? R.string.cs_not_found : R.string.cs_demo_fallback,
                    Snackbar.LENGTH_LONG).show();   
        });
    }

    /** Parse & render respons; false bila status != success / data kosong. */
private boolean terapkanRespons(String json) {
    try {
        JSONObject root = new JSONObject(json);

        if (!"success".equalsIgnoreCase(root.optString("status"))) {
            return false;
        }

        JSONObject data = root.optJSONObject("data");
        if (data == null) {
            return false;
        }

        String namaSuami = data.optString("nama_suami", "").trim();
        String namaIstri = data.optString("nama_istri", "").trim();

        if (namaSuami.isEmpty() && namaIstri.isEmpty()) {
            return false;
        }

        String pasangan = namaSuami;
        if (!namaIstri.isEmpty()) {
            pasangan = pasangan.isEmpty()
                    ? namaIstri
                    : pasangan + " & " + namaIstri;
        }

        tampilkanHasil();

        binding.tvHasilPasangan.setText(pasangan);
        binding.tvHasilTiket.setText(
                data.optString("kode_tiket", tiketTerakhir));
        binding.tvHasilDaftar.setText(
                data.optString("tanggal_daftar", "—"));

        String statusKode = data.optString(
                "status_pendaftaran", ST_DITERIMA);

        terapkanBadge(statusKode);
        bangunTimeline(null, indexDariStatus(statusKode), statusKode);

        JSONObject catatanData = new JSONObject();
        catatanData.put(
                "pemeriksa", data.optString("nama_petugas", ""));
        catatanData.put(
                "catatan", data.optString("catatan_petugas", ""));

        terapkanCatatanPetugas(catatanData, statusKode);

        return true;

    } catch (Exception e) {
        return false;
    }
}
// =======================================================================
    // Render hasil & timeline
    // =======================================================================

    private void tampilkanHasil() {
        binding.layoutKosong.setVisibility(android.view.View.GONE);
        binding.cardHasil.setVisibility(android.view.View.VISIBLE);
    }

    /** Kembalikan indeks timeline 0..4 dari kode status. */
    private int indexDariStatus(String statusKode) {
        switch (statusKode) {
            case ST_DIPERIKSA: return 1;
            case ST_PERBAIKAN: return 2;
            case ST_LENGKAP: return 3;
            case ST_JADWAL: return 4;
            case ST_DITERIMA:
            default: return 0;
        }
    }

    /**
     * Susun 5 baris timeline. Definisi tiap tahap diambil dari array server
     * (opsional) atau fallback lokal; state tiap baris = selesai (idx&lt;aktif),
     * aktif (idx==aktif), menunggu (idx&gt;aktif).
     */
    private void bangunTimeline(@Nullable JSONArray dariServer, int aktifIdx, String statusKode) {
        binding.containerTimeline.removeAllViews();

        int[][] namaRes = {
                {R.string.cs_stage_diterima, R.string.cs_stage_diterima_desc, R.drawable.ic_check_circle},
                {R.string.cs_stage_diperiksa, R.string.cs_stage_diperiksa_desc, R.drawable.ic_search},
                {R.string.cs_stage_perbaikan, R.string.cs_stage_perbaikan_desc, R.drawable.ic_warning},
                {R.string.cs_stage_lengkap, R.string.cs_stage_lengkap_desc, R.drawable.ic_verified},
                {R.string.cs_stage_jadwal, R.string.cs_stage_jadwal_desc, R.drawable.ic_calendar},
        };
        int total = namaRes.length;

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < total; i++) {
            ItemStatusTimelineBinding row =
                    ItemStatusTimelineBinding.inflate(inflater, binding.containerTimeline, false);

            int state = i < aktifIdx ? TAHAP_SELESAI
                    : (i == aktifIdx ? TAHAP_AKTIF : TAHAP_MENUNGGU);

            String judul = getString(namaRes[i][0]);
            String deskripsi = getString(namaRes[i][1]);
            String waktu = "";

            if (dariServer != null && i < dariServer.length()) {
                JSONObject tahap = dariServer.optJSONObject(i);
                if (tahap != null) {
                    judul = tahap.optString("nama", judul);
                    deskripsi = tahap.optString("deskripsi", deskripsi);
                    waktu = tahap.optString("waktu", "");
                }
            }

            row.tlTitle.setText(judul);
            row.tlSubtitle.setText(deskripsi);
            if (!waktu.isEmpty()) {
                row.tlTime.setText(waktu);
                row.tlTime.setVisibility(android.view.View.VISIBLE);
            }

            // Segmen penghubung: awal tak punya garis atas, akhir tak punya garis bawah
            row.tlSegTop.setVisibility(i == 0 ? android.view.View.INVISIBLE : android.view.View.VISIBLE);
            row.tlSegBottom.setVisibility(i == total - 1
                    ? android.view.View.INVISIBLE : android.view.View.VISIBLE);

            terapkanStateRow(row, state, namaRes[i][2]);
            binding.containerTimeline.addView(row.getRoot());
        }

        // Status "ditolak" → tahap aktif tak berlaku; tandai perbaikan sebagai aktif merah
        if (ST_PERBAIKAN.equals(statusKode)) {
            binding.containerTimeline.requestLayout();
        }
    }

    /** Set dot bg, ikon, & warna teks sesuai state baris timeline. */
    private void terapkanStateRow(ItemStatusTimelineBinding row, int state, int ikonTahap) {
        int dotBg;
        int ikon;
        int tintIkon;
        int warnaJudul;
        int warnaSub;

        if (state == TAHAP_SELESAI) {
            dotBg = R.drawable.bg_status_dot_done;
            ikon = R.drawable.ic_check_circle;
            tintIkon = R.color.md3_on_primary;
            warnaJudul = R.color.md3_on_surface;
            warnaSub = R.color.text_tertiary;
        } else if (state == TAHAP_AKTIF) {
            dotBg = R.drawable.bg_status_dot_active;
            ikon = ikonTahap;
            tintIkon = R.color.md3_on_primary;
            warnaJudul = R.color.s5_amber_text;
            warnaSub = R.color.md3_on_surface_variant;
        } else { // MENUNGGU
            dotBg = R.drawable.bg_status_dot_todo;
            ikon = ikonTahap;
            tintIkon = R.color.md3_outline_soft;
            warnaJudul = R.color.text_tertiary;
            warnaSub = R.color.text_tertiary;
        }

        row.tlDot.setBackgroundResource(dotBg);
        row.tlIcon.setImageResource(ikon);
        row.tlIcon.setColorFilter(ContextCompat.getColor(this, tintIkon));
        row.tlTitle.setTextColor(ContextCompat.getColor(this, warnaJudul));
        row.tlSubtitle.setTextColor(ContextCompat.getColor(this, warnaSub));
    }

    /** Atur label + warna badge status mengikuti kode backend. */
    private void terapkanBadge(String statusKode) {
        int labelRes;
        int bgRes;
        int teksRes;
        switch (statusKode) {
            case ST_DIPERIKSA:
                labelRes = R.string.cs_badge_diperiksa; bgRes = R.drawable.bg_badge_sky; teksRes = R.color.badge_sky_text; break;
            case ST_PERBAIKAN:
                labelRes = R.string.cs_badge_perlu_perbaikan; bgRes = R.drawable.bg_s5_badge_amber; teksRes = R.color.s5_amber_text; break;
            case ST_LENGKAP:
                labelRes = R.string.cs_badge_lengkap; bgRes = R.drawable.bg_s5_badge_emerald; teksRes = R.color.s5_emerald_badge_text; break;
            case ST_JADWAL:
                labelRes = R.string.cs_badge_dijadwalkan; bgRes = R.drawable.bg_s5_badge_emerald; teksRes = R.color.s5_emerald_badge_text; break;
            case ST_DITERIMA:
                labelRes = R.string.cs_badge_diterima; bgRes = R.drawable.bg_badge_blue; teksRes = R.color.badge_blue_text; break;
            default:
                labelRes = R.string.cs_badge_diterima; bgRes = R.drawable.bg_badge_gray; teksRes = R.color.status_offline_text; break;
        }
        binding.tvBadgeStatus.setText(getString(labelRes));
        binding.tvBadgeStatus.setBackgroundResource(bgRes);
        binding.tvBadgeStatus.setTextColor(ContextCompat.getColor(this, teksRes));
    }

    /** Isi kartu catatan petugas; tombol unggah ulang hanya untuk perlu perbaikan. */
    private void terapkanCatatanPetugas(JSONObject data, String statusKode) {
        String pemeriksa = data.optString("pemeriksa", "");
        String catatan = data.optString("catatan", "");

        boolean adaCatatan = !pemeriksa.trim().isEmpty() || !catatan.trim().isEmpty();
        binding.cardCatatan.setVisibility(adaCatatan
                ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.tvPemeriksa.setText(pemeriksa);
        binding.tvCatatan.setText(catatan);

        boolean perluPerbaikan = ST_PERBAIKAN.equals(statusKode);
        binding.btnUnggahUlang.setVisibility(perluPerbaikan
                ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    /** Data contoh agar pelacakan tetap tampil saat server belum tersedia. */
    private void renderContoh() {
        tampilkanHasil();
        binding.tvHasilPasangan.setText("Ahmad Fauzi & Siti Nurhaliza");
        binding.tvHasilTiket.setText(
                tiketTerakhir.isEmpty() ? "RKA-2026-00123" : tiketTerakhir);
        binding.tvHasilDaftar.setText("12 Januari 2026");
        terapkanBadge(ST_PERBAIKAN);
        bangunTimeline(null, 2, ST_PERBAIKAN);

        binding.cardCatatan.setVisibility(android.view.View.VISIBLE);
        binding.tvPemeriksa.setText("Zaenal Arifin, S.Ud");
        binding.tvCatatan.setText("Pindai KTP istri buram dan NIK tidak terbaca. "
                + "Mohon unggah ulang dengan resolusi lebih tinggi (maks 2 MB, PDF/JPG/PNG).");
        binding.btnUnggahUlang.setVisibility(android.view.View.VISIBLE);

        android.view.animation.ScaleAnimation masuk =
                new android.view.animation.ScaleAnimation(
                        0.96f, 1f, 0.96f, 1f,
                        android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                        android.view.animation.Animation.RELATIVE_TO_SELF, 0f);
        masuk.setDuration(220);
        binding.cardHasil.startAnimation(masuk);
    }

    // =======================================================================
    // Navigasi & aksi luar
    // =======================================================================

    /** Aksi "Unggah Ulang Dokumen" → kembali ke form unggah berkas (Langkah 5). */
    private void bukaLangkahDokumen() {
        Intent intent = new Intent(this, DaftarNikahStep5Activity.class);
        intent.putExtra("kode_tiket", tiketTerakhir);
        startActivity(intent);
    }

    private void bukaWhatsApp() {
        try {
            String pesan = getString(R.string.cs_wa_message,
                    tiketTerakhir.isEmpty() ? "-" : tiketTerakhir);
            Uri uri = Uri.parse("https://wa.me/" + ADMIN_WHATSAPP + "?text=" + Uri.encode(pesan));
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.cs_wa_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    /** Routing bersama bottom navigation dan navigation drawer. */
    private void handleNavigation(int itemId) {
        if (itemId == R.id.menu_home) {
            navigateTo(MainActivity.class);
        } else if (itemId == R.id.menu_services) {
            navigateTo(ServicesActivity.class);
        } else if (itemId == R.id.menu_forms) {
            navigateTo(BlankoActivity.class);
        } else if (itemId == R.id.menu_facilities) {
            navigateTo(FacilitiesActivity.class);
        } else if (itemId == R.id.menu_consultation) {
            navigateTo(ConsultationActivity.class);
        } else if (itemId == R.id.menu_pengumuman) {
            navigateTo(PengumumanNikahActivity.class);
        } else if (itemId == R.id.menu_daftar_nikah) {
            navigateTo(DaftarNikahActivity.class);
        } else if (itemId == R.id.menu_cek_status) {
            // sudah berada di layar cek status — tidak ada navigasi
        } else if (itemId == R.id.menu_simkah) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL_SIMKAH)));
            } catch (Exception e) {
                Toast.makeText(this, R.string.daf_simkah_unavailable, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, R.string.daf_feature_soon, Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateTo(Class<?> target) {
        Intent intent = new Intent(this, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}