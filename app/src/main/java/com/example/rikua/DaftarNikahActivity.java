package com.example.rikua;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.rikua.databinding.ActivityDaftarNikahBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

/**
 * DaftarNikahActivity — Halaman "Daftar Nikah: Pengantar" (Langkah 1 dari 6)
 * RIKUA KUA Kec. Randudongkal.
 *
 * Pintu masuk alur pendaftaran nikah online: ringkasan proses, checklist
 * dokumen (N1, KTP/KK, akta/ijazah, pas foto latar biru, N4/N5), persetujuan
 * kebijakan privasi, dan aksi utama menuju Langkah 2 (Data Calon Suami & Istri)
 * atau portal mandiri SIMKAH Kemenag.
 *
 * Interaktifitas kunci:
 *   - Tombol "Mulai Mendaftar" disabled sampai checkbox persetujuan dicentang
 *     (state-list warna daf_btn_mulai_* menangani transisi visual otomatis).
 *   - "Simpan Draf" mengonfirmasi penyimpanan sementara lewat Snackbar.
 *   - Stepper app bar menampilkan progres Langkah 1 dari 6 (LinearProgress 1/6).
 *   - Drawer 12 menu dengan item "Daftar Nikah" ter-highlight; bottom nav 5 tab
 *     dengan tab ke-3 "Daftar Nikah" aktif.
 *
 * Implementasi memakai View Binding (ActivityDaftarNikahBinding) — tanpa
 * findViewById, sesuai standar project.
 */
public class DaftarNikahActivity extends AppCompatActivity {

    /** Portal resmi pendaftaran nikah mandiri Kementerian Agama. */
    private static final String URL_SIMKAH = "https://simkah.kemenag.go.id";

    /** Total langkah alur pendaftaran (pengantar s.d. selesaikan akad). */
    private static final int TOTAL_LANGKAH = 6;

    private ActivityDaftarNikahBinding binding;

    // =======================================================================
    // Lifecycle
    // =======================================================================

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDaftarNikahBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupWindowAndStatusBar();
        setupToolbar();
        setupStepper();
        setupDrawer();
        setupConsentGating();
        setupActionButtons();
        setupBottomNavigation();
    }

    @Override
    protected void onDestroy() {
        binding = null; // hindari kebocoran view melalui binding statis
        super.onDestroy();
    }

    // =======================================================================
    // Setup tampilan
    // =======================================================================

    /** Selaraskan warna status bar dengan primary MD3 + inset navigasi bawah. */
    private void setupWindowAndStatusBar() {
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.md3_primary));

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(true);

        // Panel sticky bawah (dock + bottom nav) naik menghindari gesture bar.
        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutBottomSticky, (view, insets) -> {
            androidx.core.graphics.Insets bars =
                    insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(view.getPaddingLeft(), view.getPaddingTop(),
                    view.getPaddingRight(), bars.bottom);
            return insets;
        });
    }

    /** Panah kiri menutup activity; burger icon membuka navigation drawer. */
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

    /** Konfigurasi stepper — progres sudah didefinisikan XML (1/6), nilai
      *  dijalankan ulang di sini agar tetap konsisten bila total langkah berubah. */
    private void setupStepper() {
        binding.progressLangkah.setMax(TOTAL_LANGKAH);
        binding.progressLangkah.setProgress(1);
    }

    private void setupDrawer() {
        // Halaman ini adalah bagian alur "Daftar Nikah" — beri state aktif.
        binding.navDrawer.setCheckedItem(R.id.menu_daftar_nikah);
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

    // =======================================================================
    // Logika gating persetujuan privasi
    // =======================================================================

    /**
     * Tombol "Mulai Mendaftar" kondisi awal disabled (surface-variant, teks abu,
     * tanpa efek klik). Begitu checkbox persetujuan dicentang, tombol diaktifkan
     * — perubahan warna hijau + teks putih ditangani color state list
     * daf_btn_mulai_bg/_text/_icon, elevation bawaan tombol filled muncul kembali.
     */
    private void setupConsentGating() {
        binding.btnMulaiMendaftar.setEnabled(false);
        binding.cbPersetujuan.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.btnMulaiMendaftar.setEnabled(isChecked);
            binding.tvConsentHint.setVisibility(isChecked ? View.GONE : View.VISIBLE);
        });
    }

    // =======================================================================
    // Aksi tombol
    // =======================================================================

    /**
     * Aksi tombol:
     *   - Simpan Draf      -> konfirmasi Snackbar (draf tersimpan di perangkat)
     *   - Mulai Mendaftar  -> Intent Langkah 2 (Data Calon Suami & Istri)
     *   - Daftar SIMKAH    -> browser eksternal portal simkah.kemenag.go.id
     *   - Lihat Syarat     -> SyaratNikahActivity (panduan berkas)
     *   - Unduh Blanko     -> BlankoActivity (form N1–N9 per desa)
     */
    private void setupActionButtons() {
        binding.btnSimpanDraf.setOnClickListener(v ->
                Snackbar.make(binding.getRoot(), R.string.daf_draft_saved,
                        Snackbar.LENGTH_LONG).show());

        binding.btnMulaiMendaftar.setOnClickListener(v -> lanjutKeLangkah2());

        binding.btnSimkah.setOnClickListener(v -> bukaSimkahKemenag());

        binding.btnLihatSyarat.setOnClickListener(v ->
                startActivity(new Intent(this, SyaratNikahActivity.class)));

        binding.btnUnduhBlanko.setOnClickListener(v ->
                startActivity(new Intent(this, BlankoActivity.class)));
    }

    /** Pindah ke Langkah 2 alur pendaftaran: pengisian data calon mempelai pria. */
    private void lanjutKeLangkah2() {
        startActivity(new Intent(this, DaftarNikahStep2Activity.class));
    }

    /** Buka portal SIMKAH Kemenag lewat browser eksternal (implicit intent). */
    private void bukaSimkahKemenag() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL_SIMKAH)));
        } catch (Exception e) {
            Toast.makeText(this, R.string.daf_simkah_unavailable,
                    Toast.LENGTH_SHORT).show();
        }
    }

    // =======================================================================
    // Navigasi
    // =======================================================================

    /** Bottom nav persisten — tab ke-3 "Daftar Nikah" berada pada state aktif. */
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = binding.bottomNavigation;
        bottomNav.setSelectedItemId(R.id.menu_daftar_nikah);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_daftar_nikah) {
                return true; // sudah di halaman alur daftar nikah — tetap di sini
            }
            if (id == R.id.menu_lainnya) {
                toggleDrawer();
                bottomNav.setSelectedItemId(R.id.menu_daftar_nikah);
                return false;
            }
            handleNavigation(id);
            return true;
        });
    }

    /** Routing bersama bottom navigation dan navigation drawer (12 menu). */
    private void handleNavigation(int itemId) {
        if (itemId == R.id.menu_home) {
            navigateTo(MainActivity.class);
        } else if (itemId == R.id.menu_services) {
            navigateTo(ServicesActivity.class);
        } else if (itemId == R.id.menu_daftar_nikah) {
            // Sudah berada pada alur daftar nikah; tidak ada navigasi.
        } else if (itemId == R.id.menu_cek_status) {
            showFeatureSoon();
        } else if (itemId == R.id.menu_forms) {
            navigateTo(BlankoActivity.class);
        } else if (itemId == R.id.menu_facilities) {
            navigateTo(FacilitiesActivity.class);
        } else if (itemId == R.id.menu_consultation) {
            navigateTo(ConsultationActivity.class);
        } else if (itemId == R.id.menu_pengumuman) {
            navigateTo(PengumumanNikahActivity.class);
        } else if (itemId == R.id.menu_faq
                || itemId == R.id.menu_profil
                || itemId == R.id.menu_login) {
            showFeatureSoon();
        } else if (itemId == R.id.menu_simkah) {
            bukaSimkahKemenag();
        }
    }

    private void showFeatureSoon() {
        Toast.makeText(this, R.string.daf_feature_soon, Toast.LENGTH_SHORT).show();
    }

    private void navigateTo(Class<?> target) {
        Intent intent = new Intent(this, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    // =======================================================================
    // Back stack
    // =======================================================================

    /** Drawer terbuka punya prioritas pertama saat tombol back ditekan. */
    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
            binding.drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }
}
