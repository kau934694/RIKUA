package com.example.rikua;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import com.example.rikua.databinding.ActivityIsbatNikahBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * IsbatNikahActivity — Halaman "Isbat Nikah" RIKUA KUA Kec. Randudongkal.
 *
 * Panduan pencatatan nikah (isbat) bagi pasangan yang pernikahannya belum
 * tercatat di KUA: permohonan ke Pengadilan Agama Pemalang, sidang penetapan,
 * lalu pencatatan + penerbitan Buku Nikah di KUA. Konten prosedural bersifat
 * statis (seragam nasional), sehingga halaman tidak melakukan fetch API.
 *
 * Implementasi memakai View Binding (ActivityIsbatNikahBinding) — tanpa
 * findViewById, sesuai spesifikasi permintaan fitur ini.
 *
 * Struktur tampilan (lihat activity_isbat_nikah.xml):
 *   1. Section banner : badge resmi, tanggal pembaruan, judul, ringkasan alur
 *   2. Kapan Diperlukan? : 3 kartu poin penjelasan
 *   3. Yang Perlu Disiapkan : checklist 4 berkas (klik = tandai siap)
 *   4. Alur Proses 4 Langkah : timeline, langkah-4 highlight sekunder emas
 *   5. Notice biaya  : "Bebas Biaya (Rp 0,-)" + footnote biaya sidang PA
 *   6. FAQ accordion : expand/collapse jawaban dengan animasi height
 *   7. Floating dock : Unduh Blanko N3 + Konsultasi + tautan bantuan petugas
 *   8. Bottom navigation persisten (5 tab, "Layanan" aktif)
 */
public class IsbatNikahActivity extends AppCompatActivity {

    /** Extra utama: objek layanan (Serializable) — konvensi intent project. */
    public static final String EXTRA_LAYANAN = "extra_layanan";
    /** Extra fallback: slug kode_layanan dari daftar layanan. */
    public static final String EXTRA_KODE_LAYANAN = "extra_kode_layanan";
    /** Extra fallback: judul layanan untuk teks sementara di toolbar. */
    public static final String EXTRA_JUDUL_LAYANAN = "extra_judul_layanan";

    /** Slug layanan yang diarahkan ke halaman ini dari daftar Layanan. */
    public static final String KODE_LAYANAN = "isbat-nikah";

    /** Admin KUA untuk konsultasi isbat (format 62…). */
    private static final String ADMIN_WHATSAPP = "6287787080948";

    private ActivityIsbatNikahBinding binding;

    /** State centang checklist berkas (indeks 0..3 == berkas 1..4). */
    private final boolean[] berkasSiap = new boolean[4];

    // =======================================================================
    // Lifecycle
    // =======================================================================

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityIsbatNikahBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupWindowAndStatusBar();
        setupToolbar();
        setupDrawer();
        setupChecklist();
        setupFaqAccordion();
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
    // Checklist berkas — klik row menandai "siap"
    // =======================================================================

    /**
     * Interaksi checklist: mengetuk salah satu dari 4 row berkas akan
     * menandai/membatalkan status "siap" — ikon centang berubah warna,
     * judul menebal, dan saat semua berkas siap tampil toast apresiasi.
     */
    private void setupChecklist() {
        binding.rowBerkas1.setOnClickListener(v -> toggleBerkas(0));
        binding.rowBerkas2.setOnClickListener(v -> toggleBerkas(1));
        binding.rowBerkas3.setOnClickListener(v -> toggleBerkas(2));
        binding.rowBerkas4.setOnClickListener(v -> toggleBerkas(3));
        terapkanVisualSemuaBerkas();
    }

    private void toggleBerkas(int index) {
        berkasSiap[index] = !berkasSiap[index];
        terapkanVisualBerkas(index);

        if (berkasSemuaSiap()) {
            Toast.makeText(this, R.string.isbat_checklist_selesai, Toast.LENGTH_SHORT).show();
        }
    }

    /** Terapkan visual satu row: tint ikon centang + gaya teks judul. */
    private void terapkanVisualBerkas(int index) {
        ImageView icon;
        TextView judul;
        switch (index) {
            case 1:  icon = binding.ivCheck2; judul = binding.tvBerkas2; break;
            case 2:  icon = binding.ivCheck3; judul = binding.tvBerkas3; break;
            case 3:  icon = binding.ivCheck4; judul = binding.tvBerkas4; break;
            default: icon = binding.ivCheck1; judul = binding.tvBerkas1; break;
        }

        boolean siap = berkasSiap[index];
        icon.setImageResource(R.drawable.ic_check_circle);
        icon.setColorFilter(ContextCompat.getColor(this,
                siap ? R.color.md3_primary : R.color.md3_outline_variant_40));
        judul.setTypeface(Typeface.create("sans-serif-medium",
                siap ? Typeface.BOLD : Typeface.NORMAL));
    }

    private void terapkanVisualSemuaBerkas() {
        for (int i = 0; i < berkasSiap.length; i++) {
            terapkanVisualBerkas(i);
        }
    }

    private boolean berkasSemuaSiap() {
        for (boolean siap : berkasSiap) {
            if (!siap) return false;
        }
        return true;
    }

    // =======================================================================
    // FAQ Accordion — expand/collapse jawaban dengan animasi height
    // =======================================================================

    /** State accordion: false = collapsed (default jawaban disembunyikan XML). */
    private boolean faqTerbuka = false;
    private ValueAnimator faqAnimator;

    private void setupFaqAccordion() {
        binding.faqHeader.setOnClickListener(v -> {
            if (faqTerbuka) {
                collapseFaq();
            } else {
                expandFaq();
            }
        });
    }

    /** Buka jawaban FAQ: animasi tinggi 0 → tinggi konten + rotasi chevron. */
    private void expandFaq() {
        faqTerbuka = true;
        binding.ivFaqChevron.animate().rotation(180f)
                .setDuration(220).setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        LinearLayout body = binding.faqBody;
        body.setVisibility(View.VISIBLE);
        body.measure(View.MeasureSpec.makeMeasureSpec(
                        binding.faqCard.getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        animateBodyHeight(body, 0, body.getMeasuredHeight());
    }

    /** Tutup jawaban FAQ: animasi tinggi konten → 0, lalu kembalikan chevron. */
    private void collapseFaq() {
        faqTerbuka = false;
        binding.ivFaqChevron.animate().rotation(0f)
                .setDuration(220).setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        animateBodyHeight(binding.faqBody, binding.faqBody.getHeight(), 0);
    }

    /**
     * Animasikan tinggi vertikal body FAQ. Pakai AnimatorListenerAdapter
     * (bukan Kotlin doOnEnd) — sesuai standar Java project; saat collapse
     * selesai body dikembalikan ke wrap_content + GONE agar tidak memakan ruang.
     */
    private void animateBodyHeight(final LinearLayout body, int from, int to) {
        if (faqAnimator != null && faqAnimator.isRunning()) {
            faqAnimator.cancel();
        }
        faqAnimator = ValueAnimator.ofInt(from, to);
        faqAnimator.setDuration(240);
        faqAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        faqAnimator.addUpdateListener(animation -> {
            int tinggi = (int) animation.getAnimatedValue();
            LinearLayout.LayoutParams lp =
                    (LinearLayout.LayoutParams) body.getLayoutParams();
            lp.height = tinggi;
            body.setLayoutParams(lp);
        });
        faqAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                LinearLayout.LayoutParams lp =
                        (LinearLayout.LayoutParams) body.getLayoutParams();
                if (!faqTerbuka) {
                    lp.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                    body.setVisibility(View.GONE);
                } else {
                    lp.height = LinearLayout.LayoutParams.WRAP_CONTENT;
                }
                body.setLayoutParams(lp);
            }
        });
        faqAnimator.start();
    }

    // =======================================================================
    // Sticky action buttons
    // =======================================================================

    /**
     * Aksi floating dock:
     *   - Unduh Blanko N3 -> BlankoActivity (formulir N1 s.d. N6) + toast petunjuk
     *   - Konsultasi      -> ConsultationActivity (daftar petugas)
     *   - Tautan bantuan  -> WhatsApp resmi admin KUA dengan pesan siap kirim
     */
    private void setupActionButtons() {
        binding.btnUnduhBlanko.setOnClickListener(v -> {
            Toast.makeText(this, R.string.isbat_blanko_info, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, BlankoActivity.class));
        });

        binding.btnKonsultasi.setOnClickListener(v ->
                startActivity(new Intent(this, ConsultationActivity.class)));

        binding.tvBantuanPetugas.setOnClickListener(v -> bukaWhatsAppKonsultasi());
    }

    /** Buka WhatsApp admin dengan pesan pra-isi konsultasi isbat nikah. */
    private void bukaWhatsAppKonsultasi() {
        try {
            String pesan = Uri.encode(getString(R.string.isbat_wa_message));
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
