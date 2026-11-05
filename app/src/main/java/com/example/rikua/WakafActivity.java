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

import com.example.rikua.databinding.ActivityWakafBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * WakafActivity — Halaman "Wakaf & Kemasjidan" RIKUA KUA Kec. Randudongkal.
 *
 * Panduan ikrar wakaf di hadapan PPAIW (Pejabat Pembuat Akta Ikrar Wakaf):
 * berkas wakif/nadzir, alur 4 tahapan hingga terbit AIW/APAIW dan pendaftaran
 * tanah wakaf ke BPN. Konten prosedural bersifat statis (berbasis UU No. 41
 * Tahun 2004), sehingga halaman tidak melakukan fetch API.
 *
 * Implementasi memakai View Binding (ActivityWakafBinding) — tanpa
 * findViewById, sesuai standar project.
 *
 * Struktur tampilan (lihat activity_wakaf.xml):
 *   1. Meta & status banner : titik hijau layanan + tanggal pembaruan
 *   2. Hero / bento card    : ikon masjid, judul, subjudul, peran PPAIW
 *   3. Tentang AIW          : fungsi hukum + strip landasan regulasi
 *   4. Berkas 1–3           : row bernomor (wakif, bukti tanah, nadzir)
 *   5. Timeline 4 langkah   : garis dashed, langkah-4 highlight emas (Selesai)
 *   6. Info biaya           : banner "Layanan Tanpa Pungutan" + badge Rp 0
 *   7. Sticky action tray   : Sarana Keagamaan (outline) + Konsultasi (filled)
 *                              + tautan bantuan petugas, bottom nav 5 tab
 */
public class WakafActivity extends AppCompatActivity {

    /** Extra utama: objek layanan (Serializable) — konvensi intent project. */
    public static final String EXTRA_LAYANAN = "extra_layanan";
    /** Extra fallback: slug kode_layanan dari daftar layanan. */
    public static final String EXTRA_KODE_LAYANAN = "extra_kode_layanan";
    /** Extra fallback: judul layanan untuk teks sementara di toolbar. */
    public static final String EXTRA_JUDUL_LAYANAN = "extra_judul_layanan";

    /** Slug layanan yang diarahkan ke halaman ini dari daftar Layanan. */
    public static final String KODE_LAYANAN = "wakaf-kemasjidan";

    /** Admin KUA untuk konsultasi wakaf (format 62…). */
    private static final String ADMIN_WHATSAPP = "6287787080948";

    private ActivityWakafBinding binding;

    // =======================================================================
    // Lifecycle
    // =======================================================================

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWakafBinding.inflate(getLayoutInflater());
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
     *   - Sarana Keagamaan -> FacilitiesActivity (direktori masjid/musala)
     *   - Konsultasi       -> ConsultationActivity (daftar petugas)
     *   - Tautan bantuan   -> WhatsApp resmi admin KUA dengan pesan siap kirim
     */
    private void setupActionButtons() {
        binding.btnSaranaKeagamaan.setOnClickListener(v ->
                startActivity(new Intent(this, FacilitiesActivity.class)));

        binding.btnKonsultasi.setOnClickListener(v ->
                startActivity(new Intent(this, ConsultationActivity.class)));

        binding.tvBantuanPetugas.setOnClickListener(v -> bukaWhatsAppKonsultasi());
    }

    /** Buka WhatsApp admin dengan pesan pra-isi konsultasi wakaf & kemasjidan. */
    private void bukaWhatsAppKonsultasi() {
        try {
            String pesan = Uri.encode(getString(R.string.wakaf_wa_message));
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://wa.me/" + ADMIN_WHATSAPP + "?text=" + pesan));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, R.string.rekom_wa_unavailable, Toast.LENGTH_SHORT).show();
        }
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
