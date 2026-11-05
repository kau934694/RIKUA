package com.example.rikua;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.rikua.databinding.ActivityWaliNikahBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * WaliNikahActivity — Halaman "Wali Nikah" RIKUA KUA Kec. Randudongkal.
 *
 * Panduan tertib wali nasab (7 urutan silsilah, ayah kandung sebagai prioritas
 * utama) dan kondisi digunakannya wali hakim (putus nasab, mafqud, berhalangan
 * jauh, adhal). Konten prosedural bersifat statis (kompilasi hukum Islam &
 * KHI), sehingga halaman tidak melakukan fetch API.
 *
 * Implementasi memakai View Binding (ActivityWaliNikahBinding) — tanpa
 * findViewById, sesuai standar project.
 *
 * Struktur tampilan (lihat activity_wali_nikah.xml):
 *   1. Kartu headline   : badge resmi, tanggal pembaruan, judul & deskripsi
 *   2. Wali nasab 1–7   : poin-1 kartu emas "PRIORITAS UTAMA" + row 2–7 putih
 *   3. Accordion        : "Kapan Memakai Wali Hakim?" expand/collapse animasi
 *                         height (4 kondisi + catatan petugas KUA)
 *   4. Notice amber     : pesan konsultasi silsilah sebelum mendaftar
 *   5. Sticky dock      : Syarat Nikah (outline) + Konsultasi (filled) +
 *                         tautan bantuan petugas, bottom nav 5 tab (Layanan aktif)
 */
public class WaliNikahActivity extends AppCompatActivity {

    /** Extra utama: objek layanan (Serializable) — konvensi intent project. */
    public static final String EXTRA_LAYANAN = "extra_layanan";
    /** Extra fallback: slug kode_layanan dari daftar layanan. */
    public static final String EXTRA_KODE_LAYANAN = "extra_kode_layanan";
    /** Extra fallback: judul layanan untuk teks sementara di toolbar. */
    public static final String EXTRA_JUDUL_LAYANAN = "extra_judul_layanan";

    /** Slug layanan yang diarahkan ke halaman ini dari daftar Layanan. */
    public static final String KODE_LAYANAN = "wali-nikah";

    /** Admin KUA untuk konsultasi wali (format 62…). */
    private static final String ADMIN_WHATSAPP = "6287787080948";

    private ActivityWaliNikahBinding binding;

    /** State accordion: false = collapsed (default jawaban disembunyikan XML). */
    private boolean hakimTerbuka = false;
    private ValueAnimator hakimAnimator;

    // =======================================================================
    // Lifecycle
    // =======================================================================

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWaliNikahBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupWindowAndStatusBar();
        setupToolbar();
        setupDrawer();
        setupHakimAccordion();
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
    // Accordion wali hakim — expand/collapse jawaban dengan animasi height
    // =======================================================================

    private void setupHakimAccordion() {
        binding.hakimHeader.setOnClickListener(v -> {
            if (hakimTerbuka) {
                collapseHakim();
            } else {
                expandHakim();
            }
        });
    }

    /** Buka jawaban: animasi tinggi 0 → tinggi konten + rotasi chevron. */
    private void expandHakim() {
        hakimTerbuka = true;
        binding.ivHakimChevron.animate().rotation(180f)
                .setDuration(220).setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        LinearLayout body = binding.hakimBody;
        body.setVisibility(View.VISIBLE);
        body.measure(View.MeasureSpec.makeMeasureSpec(
                        binding.hakimCard.getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        animateBodyHeight(body, 0, body.getMeasuredHeight());
    }

    /** Tutup jawaban: animasi tinggi konten → 0, lalu kembalikan chevron. */
    private void collapseHakim() {
        hakimTerbuka = false;
        binding.ivHakimChevron.animate().rotation(0f)
                .setDuration(220).setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        animateBodyHeight(binding.hakimBody, binding.hakimBody.getHeight(), 0);
    }

    /**
     * Animasikan tinggi vertikal body accordion. Pakai AnimatorListenerAdapter
     * (bukan Kotlin doOnEnd) — sesuai standar Java project; saat collapse
     * selesai body dikembalikan ke wrap_content + GONE agar tidak memakan ruang.
     */
    private void animateBodyHeight(final LinearLayout body, int from, int to) {
        if (hakimAnimator != null && hakimAnimator.isRunning()) {
            hakimAnimator.cancel();
        }
        hakimAnimator = ValueAnimator.ofInt(from, to);
        hakimAnimator.setDuration(240);
        hakimAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        hakimAnimator.addUpdateListener(animation -> {
            int tinggi = (int) animation.getAnimatedValue();
            LinearLayout.LayoutParams lp =
                    (LinearLayout.LayoutParams) body.getLayoutParams();
            lp.height = tinggi;
            body.setLayoutParams(lp);
        });
        hakimAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                LinearLayout.LayoutParams lp =
                        (LinearLayout.LayoutParams) body.getLayoutParams();
                lp.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                if (!hakimTerbuka) {
                    body.setVisibility(View.GONE);
                }
                body.setLayoutParams(lp);
            }
        });
        hakimAnimator.start();
    }

    // =======================================================================
    // Sticky action buttons
    // =======================================================================

    /**
     * Aksi tombol:
     *   - Syarat Nikah -> SyaratNikahActivity (checklist berkas nikah)
     *   - Konsultasi   -> ConsultationActivity (daftar petugas)
     *   - Tautan bantuan -> WhatsApp resmi admin KUA dengan pesan siap kirim
     */
    private void setupActionButtons() {
        binding.btnSyaratNikah.setOnClickListener(v ->
                startActivity(new Intent(this, SyaratNikahActivity.class)));

        binding.btnKonsultasi.setOnClickListener(v ->
                startActivity(new Intent(this, ConsultationActivity.class)));

        binding.tvBantuanPetugas.setOnClickListener(v -> bukaWhatsAppKonsultasi());
    }

    /** Buka WhatsApp admin dengan pesan pra-isi konsultasi penentuan wali. */
    private void bukaWhatsAppKonsultasi() {
        try {
            String pesan = Uri.encode(getString(R.string.wali_wa_message));
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
