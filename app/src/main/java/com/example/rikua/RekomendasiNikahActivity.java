package com.example.rikua;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

/**
 * RekomendasiNikahActivity — Halaman "Rekomendasi Nikah" RIKUA KUA Kec. Randudongkal.
 *
 * Panduan bagi warga ber-KTP Randudongkal yang ingin melangsungkan akad nikah
 * di luar kecamatan (KUA tujuan berbeda dari KUA domisili). Konten halaman
 * bersifat statis (prosedur N2 seragam nasional), sehingga tidak ada fetch API —
 * seluruh interaksi difokuskan pada navigasi dan aksi bawah layar.
 *
 * Struktur tampilan (lihat activity_rekomendasi_nikah.xml):
 *   1. Hero badge resmi + kartu info (ikon transfer_within_a_station)
 *   2. Section "Kapan Diperlukan?"  : kartu border-l-4 primary + catatan SIMKAH
 *   3. Section "Yang Perlu Disiapkan": checklist 4 berkas utama
 *   4. Section "Langkah"             : timeline 3 langkah bergaris putus-putus
 *   5. Banner jaminan institusi "Gratis Rp 0,-"
 *   6. Sticky actions: Unduh Blanko, Konsultasi, tautan bantuan petugas
 *   7. Bottom navigation persisten (5 tab, "Layanan" aktif dengan indikator pill)
 */
public class RekomendasiNikahActivity extends AppCompatActivity {

    /** Extra utama: objek layanan (Serializable) — konvensi intent project. */
    public static final String EXTRA_LAYANAN = "extra_layanan";
    /** Extra fallback: slug kode_layanan dari daftar layanan. */
    public static final String EXTRA_KODE_LAYANAN = "extra_kode_layanan";
    /** Extra fallback: judul layanan untuk teks sementara di toolbar. */
    public static final String EXTRA_JUDUL_LAYANAN = "extra_judul_layanan";

    /** Slug layanan yang diarahkan ke halaman ini dari daftar Layanan. */
    public static final String KODE_LAYANAN = "rekomendasi-nikah";

    /** Admin KUA untuk konsultasi rekomendasi (format 62…). */
    private static final String ADMIN_WHATSAPP = "6287787080948";

    private DrawerLayout drawerLayout;

    // =======================================================================
    // Lifecycle
    // =======================================================================

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rekomendasi_nikah);

        setupWindowAndStatusBar();
        initViews();
        setupToolbar();
        setupActionButtons();
        setupBottomNavigation();

        // Judul toolbar ikut menyesuaikan layanan yang diklik user bila dikirim.
        bacaExtraLayanan();
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

        // Panel sticky bawah (aksi + bottom nav) naik menghindari gesture bar.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layout_bottom_sticky), (view, insets) -> {
            androidx.core.graphics.Insets bars =
                    insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(view.getPaddingLeft(), view.getPaddingTop(),
                    view.getPaddingRight(), bars.bottom);
            return insets;
        });
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);

        NavigationView navView = findViewById(R.id.nav_drawer);
        navView.setNavigationItemSelectedListener(item -> {
            drawerLayout.closeDrawer(GravityCompat.END);
            handleNavigation(item.getItemId());
            return true;
        });
    }

    /** Panah kiri menutup activity; burger icon membuka navigation drawer. */
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_open_drawer) {
                toggleDrawer();
                return true;
            }
            return false;
        });
    }

    private void toggleDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            drawerLayout.openDrawer(GravityCompat.END);
        }
    }

    /**
     * Aksi sticky bawah:
     *   - Unduh Blanko  -> BlankoActivity (formulir N1 s.d. N6)
     *   - Konsultasi    -> ConsultationActivity (daftar petugas)
     *   - Tautan bantuan-> WhatsApp resmi admin KUA dengan pesan siap kirim
     */
    private void setupActionButtons() {
        findViewById(R.id.btn_unduh_blanko).setOnClickListener(v ->
                startActivity(new Intent(this, BlankoActivity.class)));

        findViewById(R.id.btn_konsultasi).setOnClickListener(v ->
                startActivity(new Intent(this, ConsultationActivity.class)));
    }

    /** Buka WhatsApp admin dengan pesan pra-isi konsultasi rekomendasi nikah. */
    private void bukaWhatsAppKonsultasi() {
        try {
            String pesan = Uri.encode(getString(R.string.rekom_wa_message));
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://wa.me/" + ADMIN_WHATSAPP + "?text=" + pesan));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, R.string.rekom_wa_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Baca Intent mengikuti konvensi project: objek layanan (Serializable) sebagai
     * sumber utama, fallback ke extra String. Judul dikirim pengantar dipakai
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
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
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
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }
}
