package com.example.rikua;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.rikua.databinding.ActivityDuplikatBukuNikahBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * DuplikatBukuNikahActivity — Halaman "Duplikat Buku Nikah" RIKUA KUA Kec. Randudongkal.
 *
 * Panduan penggantian buku nikah yang hilang atau rusak: surat kehilangan dari
 * Polsek, berkas KTP/KK + pas foto, verifikasi register akta nikah di KUA
 * penerbit awal, lalu penerbitan duplikat (gratis, tanpa pungli). Konten
 * prosedural bersifat statis (seragam nasional), sehingga halaman tidak
 * melakukan fetch API.
 *
 * Implementasi memakai View Binding (ActivityDuplikatBukuNikahBinding) — tanpa
 * findViewById, sesuai standar project.
 *
 * Struktur tampilan (lihat activity_duplikat_buku_nikah.xml):
 *   1. Section banner : badge resmi, tanggal pembaruan, judul, deskripsi,
 *                      strip biaya "Rp 0" + ringkasan alur
 *   2. Checklist dokumen : badge "4 Dokumen" + 4 row interaktif (klik = siap)
 *   3. Timeline 4 langkah : garis dashed, langkah-4 highlight emas (Selesai)
 *   4. Banner anti-pungli : garansi gratis + kartu jam layanan + tombol Hubungi
 *   5. Sticky dock    : Lihat Syarat Nikah (outline) + Konsultasi (filled)
 *   6. Bottom navigation persisten (5 tab, "Layanan" aktif)
 */
public class DuplikatBukuNikahActivity extends AppCompatActivity {

    /** Extra utama: objek layanan (Serializable) — konvensi intent project. */
    public static final String EXTRA_LAYANAN = "extra_layanan";
    /** Extra fallback: slug kode_layanan dari daftar layanan. */
    public static final String EXTRA_KODE_LAYANAN = "extra_kode_layanan";
    /** Extra fallback: judul layanan untuk teks sementara di toolbar. */
    public static final String EXTRA_JUDUL_LAYANAN = "extra_judul_layanan";

    /** Slug layanan yang diarahkan ke halaman ini dari daftar Layanan. */
    public static final String KODE_LAYANAN = "duplikat-buku-nikah";

    /** Admin KUA untuk konsultasi duplikat (format 62…). */
    private static final String ADMIN_WHATSAPP = "6287787080948";

    private ActivityDuplikatBukuNikahBinding binding;

    /** State centang checklist dokumen (indeks 0..3 == dokumen 1..4). */
    private final boolean[] dokumenSiap = new boolean[4];

    // =======================================================================
    // Lifecycle
    // =======================================================================

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDuplikatBukuNikahBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupWindowAndStatusBar();
        setupToolbar();
        setupDrawer();
        setupChecklist();
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
    // Checklist dokumen — klik row menandai "siap"
    // =======================================================================

    /**
     * Interaksi checklist: mengetuk salah satu dari 4 row dokumen akan
     * menandai/membatalkan status "siap" — ikon centang berubah warna dan
     * judul menebal; saat semua dokumen siap tampil toast apresiasi.
     */
    private void setupChecklist() {
        binding.rowDok1.setOnClickListener(v -> toggleDokumen(0));
        binding.rowDok2.setOnClickListener(v -> toggleDokumen(1));
        binding.rowDok3.setOnClickListener(v -> toggleDokumen(2));
        binding.rowDok4.setOnClickListener(v -> toggleDokumen(3));
        terapkanVisualSemuaDokumen();
    }

    private void toggleDokumen(int index) {
        dokumenSiap[index] = !dokumenSiap[index];
        terapkanVisualDokumen(index);

        if (dokumenSemuaSiap()) {
            Toast.makeText(this, R.string.dup_checklist_selesai, Toast.LENGTH_SHORT).show();
        }
    }

    /** Terapkan visual satu row: tint ikon centang + gaya teks judul. */
    private void terapkanVisualDokumen(int index) {
        ImageView icon;
        TextView judul;
        switch (index) {
            case 1:  icon = binding.ivCheck2; judul = binding.tvDok2; break;
            case 2:  icon = binding.ivCheck3; judul = binding.tvDok3; break;
            case 3:  icon = binding.ivCheck4; judul = binding.tvDok4; break;
            default: icon = binding.ivCheck1; judul = binding.tvDok1; break;
        }

        boolean siap = dokumenSiap[index];
        icon.setImageResource(R.drawable.ic_check_circle);
        icon.setColorFilter(ContextCompat.getColor(this,
                siap ? R.color.md3_primary : R.color.md3_outline_variant_40));
        judul.setTypeface(Typeface.create("sans-serif-medium",
                siap ? Typeface.BOLD : Typeface.NORMAL));
    }

    private void terapkanVisualSemuaDokumen() {
        for (int i = 0; i < dokumenSiap.length; i++) {
            terapkanVisualDokumen(i);
        }
    }

    private boolean dokumenSemuaSiap() {
        for (boolean siap : dokumenSiap) {
            if (!siap) return false;
        }
        return true;
    }

    // =======================================================================
    // Sticky action buttons
    // =======================================================================

    /**
     * Aksi tombol:
     *   - Lihat Syarat Nikah -> SyaratNikahActivity (checklist berkas nikah)
     *   - Konsultasi         -> ConsultationActivity (daftar petugas)
     *   - Hubungi (jam layanan) -> WhatsApp resmi admin KUA pesan siap kirim
     */
    private void setupActionButtons() {
        binding.btnLihatSyarat.setOnClickListener(v ->
                startActivity(new Intent(this, SyaratNikahActivity.class)));

        binding.btnKonsultasi.setOnClickListener(v ->
                startActivity(new Intent(this, ConsultationActivity.class)));

        binding.btnHubungi.setOnClickListener(v -> bukaWhatsAppKonsultasi());
    }

    /** Buka WhatsApp admin dengan pesan pra-isi konsultasi duplikat buku nikah. */
    private void bukaWhatsAppKonsultasi() {
        try {
            String pesan = Uri.encode(getString(R.string.dup_wa_message));
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
