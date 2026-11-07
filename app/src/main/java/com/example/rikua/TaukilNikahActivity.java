package com.example.rikua;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

import com.example.rikua.databinding.ActivityTaukilNikahBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * TaukilNikahActivity — Halaman "Taukil Wali bil Kitabah" RIKUA KUA Kec. Randudongkal.
 *
 * Panduan pelimpahan wewenang wali nasab secara tertulis (surat kuasa taukil)
 * kepada wakil wali bila wali berhalangan hadir saat akad: jarak jauh, sakit,
 * tugas luar daerah/negeri, atau perwakilan ijab kabul. Konten prosedural
 * bersifat statis (berbasis KHI & praktik kepenghuluan), sehingga halaman tidak
 * melakukan fetch API.
 *
 * Implementasi memakai View Binding (ActivityTaukilNikahBinding) — tanpa
 * findViewById, sesuai standar project.
 *
 * Struktur tampilan (lihat activity_taukil_nikah.xml):
 *   1. Hero / summary card : badge resmi, diperbarui, label "Panduan
 *                            Kepenghuluan" (emas), judul, subjudul, penjelasan
 *   2. Grid 2x2            : 4 kartu kondisi "Kapan Diperlukan?"
 *   3. Dokumen 1–3         : row bernomor surat taukil, KTP/KK wali, KTP wakil
 *   4. Timeline 3 langkah  : garis dashed, langkah-3 highlight emas (akad)
 *   5. Alert box           : konsultasi gratis tanpa pungutan + badge Rp 0
 *   6. Sticky dock         : Lihat Wali Nikah (outline) + Konsultasi (filled)
 *                             + tautan bantuan petugas, bottom nav 5 tab
 */
public class TaukilNikahActivity extends AppCompatActivity {

    /** Extra utama: objek layanan (Serializable) — konvensi intent project. */
    public static final String EXTRA_LAYANAN = "extra_layanan";
    /** Extra fallback: slug kode_layanan dari daftar layanan. */
    public static final String EXTRA_KODE_LAYANAN = "extra_kode_layanan";
    /** Extra fallback: judul layanan untuk teks sementara di toolbar. */
    public static final String EXTRA_JUDUL_LAYANAN = "extra_judul_layanan";

    /** Slug layanan yang diarahkan ke halaman ini dari daftar Layanan. */
    public static final String KODE_LAYANAN = "taukil-wali-bil-kitabah";

    /** Admin KUA untuk konsultasi taukil (format 62…). */
    private static final String ADMIN_WHATSAPP = "6287787080948";

    private ActivityTaukilNikahBinding binding;

    // =======================================================================
    // Lifecycle
    // =======================================================================

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTaukilNikahBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupWindowAndStatusBar();
        setupToolbar();
        setupDrawer();
        setupActionButtons();
        setupBottomNavigation();

        // Judul toolbar ikut menyesuaikan layanan yang diklik user bila dikirim.
        bacaExtraLayanan();
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

    // =======================================================================
    // Sticky action buttons
    // =======================================================================

    /**
     * Aksi tombol:
     *   - Lihat Wali Nikah -> WaliNikahActivity (tertib wali nasab & wali hakim)
     *   - Konsultasi       -> ConsultationActivity (daftar petugas)
     *   - Tautan bantuan   -> WhatsApp resmi admin KUA dengan pesan siap kirim
     */
    private void setupActionButtons() {
        binding.btnLihatWali.setOnClickListener(v ->
                startActivity(new Intent(this, WaliNikahActivity.class)));

        binding.btnKonsultasi.setOnClickListener(v ->
                startActivity(new Intent(this, ConsultationActivity.class)));
    }
    /**
     * Baca Intent mengikuti konvensi project: objek layanan (Serializable) sebagai
     * sumber utama, fallback ke extra String. Judul dari pengantar dipakai
     * sementara agar toolbar mencerminkan layanan yang diklik user.
     */
    private void bacaExtraLayanan() {
        Intent intent = getIntent();
        if (intent == null) return;

        String judulSementara = null;
        Object raw = intent.getSerializableExtra(EXTRA_LAYANAN);
        if (raw instanceof ServiceItem) {
            judulSementara = ((ServiceItem) raw).getTitle();
        }
        if (judulSementara == null || judulSementara.isEmpty()) {
            judulSementara = intent.getStringExtra(EXTRA_JUDUL_LAYANAN);
        }
        if (judulSementara != null && !judulSementara.isEmpty()) {
            setTitle(judulSementara);
        }
    }

    // =======================================================================
    // Navigasi
    // =======================================================================

    /** Bottom nav persisten — tab "Layanan" berada pada state aktif (highlight pill). */
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = binding.bottomNavigation;
        bottomNav.setSelectedItemId(R.id.menu_services);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_services) {
                // Tab sudah aktif di konteks halaman layanan ini; kembali ke daftar.
                navigateTo(ServicesActivity.class);
                return true;
            }
            if (id == R.id.menu_daftar_nikah) {
                navigateTo(DaftarNikahActivity.class);
                return true;
            }
            if (id == R.id.menu_lainnya) {
                toggleDrawer();
                bottomNav.setSelectedItemId(R.id.menu_services);
                return false;
            }
            handleNavigation(id);
            return true;
        });
    }

    /** Routing bersama bottom navigation dan navigation drawer. */
    private void handleNavigation(int itemId) {
        if (itemId == R.id.menu_home) {
            navigateTo(MainActivity.class);
        } else if (itemId == R.id.menu_services) {
            navigateTo(ServicesActivity.class);
        } else if (itemId == R.id.menu_consultation) {
            navigateTo(ConsultationActivity.class);
        } else if (itemId == R.id.menu_facilities) {
            navigateTo(FacilitiesActivity.class);
        } else if (itemId == R.id.menu_forms) {
            navigateTo(BlankoActivity.class);
        } else if (itemId == R.id.menu_daftar_nikah) {
            navigateTo(DaftarNikahActivity.class);
        } else if (itemId == R.id.menu_lainnya) {
            Toast.makeText(this, R.string.rekom_feature_soon, Toast.LENGTH_SHORT).show();
        }
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
