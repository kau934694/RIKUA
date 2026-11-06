package com.example.rikua;

import android.content.ActivityNotFoundException;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SyaratNikahActivity — Halaman "Syarat Nikah" RIKUA KUA Kec. Randudongkal.
 *
 * Redesign Material Design 3 (primary #0F6B5C, background #F6F8F7, kartu putih
 * border #E5E9E7, aksen gold #795902/#C9A24B). Seluruh konten syarat & langkah
 * tetap dinamis dari database (tabel {@code layanan}, {@code syarat_layanan},
 * {@code langkah_layanan}) lewat endpoint {@code get_syarat_nikah.php}, sehingga
 * perubahan di panel admin langsung tampak tanpa update aplikasi.
 *
 * Struktur tampilan (lihat activity_syarat_nikah.xml):
 *   1. Summary card : badge "Resmi Kemenag", tanggal pembaruan, judul,
 *                     deskripsi, chip estimasi, strip "Biaya Nikah di KUA: Rp 0"
 *   2. Progress     : bar checklist persiapan berkas (persisten per layanan)
 *   3. Accordion    : section "Untuk Semua Pendaftar" (berkas wajib) +
 *                     "Jika Berlaku untuk Anda" (berkas kondisi tertentu)
 *   4. Timeline     : "Langkah Pendaftaran" bernomor dari tabel langkah_layanan
 *   5. Notice       : banner bantuan konsultasi petugas hari kerja 08.00–15.00
 *   6. Aksi sticky  : Daftar Nikah (SIMKAH), Unduh Blanko, tautan konsultasi,
 *                     bottom nav 5 tab dengan tab Layanan aktif
 *
 * Interaksi accordion ditangani langsung di Activity (bukan adapter) karena
 * jumlah section sedikit dan tiap kartu punya state expand/centang yang harus
 * bertahan saat user membuka-tutup berulang.
 */
public class SyaratNikahActivity extends AppCompatActivity {

    private static final String TAG = "SyaratNikah";

    /** Endpoint data — host API Laragon yang sama dengan layar lain. */
    private static final String API_URL =
            "http://192.168.1.4/rikua_api/get_syarat_nikah.php";
    /** Slug default (kolom {@code kode_layanan}) bila Intent tidak mengirim slug. */
    private static final String KODE_LAYANAN = "syarat-nikah";

    /** Extra utama: objek layanan (Serializable) yang diklik di daftar layanan. */
    public static final String EXTRA_LAYANAN = "extra_layanan";
    /** Extra fallback: slug kode_layanan untuk request API. */
    public static final String EXTRA_KODE_LAYANAN = "extra_kode_layanan";
    /** Extra fallback: judul layanan untuk teks sementara sebelum data tiba. */
    public static final String EXTRA_JUDUL_LAYANAN = "extra_judul_layanan";
    /** Portal pendaftaran nikah online Kementerian Agama. */
    private static final String SIMKAH_URL = "https://simkah.kemenag.go.id";
    /** Admin KUA untuk konsultasi persyaratan (format 62…). */
    private static final String ADMIN_WHATSAPP = "6287787080948";

    private static final String PREFS_NAME = "syarat_nikah_checklist";
    private static final long ANIMASI_ACCORDION_MS = 220L;

    /** Peta id syarat -> root view accordion, agar bisa di-expand dari luar. */
    private final Map<Integer, AccordionHolder> holders = new HashMap<>();

    // ---------- Data ----------
    private final List<SyaratSection> sections = new ArrayList<>();
    private LayananInfo layanan;
    private SharedPreferences prefs;

    /** Slug layanan yang sedang ditampilkan; dapat diganti lewat Intent. */
    private String kodeLayanan = KODE_LAYANAN;

    // ---------- Views: state muat ----------
    private LinearProgressIndicator progressLoading;
    private LinearLayout layoutContent;
    private LinearLayout layoutError;
    private TextView tvErrorMessage;

    // ---------- Views: header & biaya ----------
    private TextView tvLayananJudul;
    private TextView tvLayananDeskripsi;
    private TextView tvEstimasi;
    private TextView tvBiaya;
    private TextView tvBiayaCatatan;
    private TextView tvBadgeGratis;

    // ---------- Views: progres checklist ----------
    private TextView tvProgressLabel;
    private LinearProgressIndicator progressChecklist;

    // ---------- Views: section accordion ----------
    private View sectionUmum;
    private View sectionKhusus;
    private LinearLayout containerUmum;
    private LinearLayout containerKhusus;
    private LinearLayout containerLangkah;

    private DrawerLayout drawerLayout;

    // =======================================================================
    // Lifecycle
    // =======================================================================

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_syarat_nikah);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        setupWindowAndStatusBar();
        initViews();
        setupToolbar();
        setupSectionHeaders();
        setupActionButtons();
        setupBottomNavigation();

        // Kenali layanan mana yang diminta sebelum data diambil dari server.
        bacaExtraLayanan();

        loadSyaratData();
    }

    /**
     * Baca Intent mengikuti konvensi project: objek layanan (Serializable) sebagai
     * sumber utama, lalu fallback ke extra String bila pengirim hanya membawa slug.
     * Judul dipakai sebagai teks sementara agar toolbar tidak kosong saat memuat.
     */
    private void bacaExtraLayanan() {
        Intent intent = getIntent();
        if (intent == null) return;

        String judulSementara = null;
        Object raw = intent.getSerializableExtra(EXTRA_LAYANAN);
        if (raw instanceof ServiceItem) {
            judulSementara = ((ServiceItem) raw).getTitle();
        }

        String slug = intent.getStringExtra(EXTRA_KODE_LAYANAN);
        if (slug != null && slug.matches("^[a-z0-9\\-]{3,60}$")) {
            kodeLayanan = slug;
        }

        if (judulSementara == null || judulSementara.isEmpty()) {
            judulSementara = intent.getStringExtra(EXTRA_JUDUL_LAYANAN);
        }
        if (judulSementara != null && !judulSementara.isEmpty()) {
            tvLayananJudul.setText(judulSementara);
            setTitle(judulSementara);
        }
    }

    // =======================================================================
    // Setup tampilan
    // =======================================================================

    /** Selaraskan warna status bar & navigasi dengan primary MD3 #0F6B5C. */
    private void setupWindowAndStatusBar() {
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.syarat_primary));

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(true);

        // Panel sticky (aksi + bottom nav) naik menghindari gesture navigation bar.
        View bottomSticky = findViewById(R.id.layout_bottom_sticky);
        if (bottomSticky != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomSticky, (view, insets) -> {
                androidx.core.graphics.Insets bars =
                        insets.getInsets(WindowInsetsCompat.Type.systemBars());
                view.setPadding(view.getPaddingLeft(), view.getPaddingTop(),
                        view.getPaddingRight(), bars.bottom);
                return insets;
            });
        }
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);

        progressLoading = findViewById(R.id.progress_loading);
        layoutContent = findViewById(R.id.layout_content);
        layoutError = findViewById(R.id.layout_error);
        tvErrorMessage = findViewById(R.id.tv_error_message);

        tvLayananJudul = findViewById(R.id.tv_layanan_judul);
        tvLayananDeskripsi = findViewById(R.id.tv_layanan_deskripsi);
        tvEstimasi = findViewById(R.id.tv_estimasi);
        tvBiaya = findViewById(R.id.tv_biaya);
        tvBiayaCatatan = findViewById(R.id.tv_biaya_catatan);
        tvBadgeGratis = findViewById(R.id.tv_badge_gratis);

        tvProgressLabel = findViewById(R.id.tv_progress_label);
        progressChecklist = findViewById(R.id.progress_checklist);

        // Header section di-<include> dua kali; id include jadi anchor root-nya.
        sectionUmum = findViewById(R.id.section_umum);
        sectionKhusus = findViewById(R.id.section_khusus);

        containerUmum = findViewById(R.id.container_syarat_umum);
        containerKhusus = findViewById(R.id.container_syarat_khusus);
        containerLangkah = findViewById(R.id.container_langkah);

        findViewById(R.id.btn_retry).setOnClickListener(v -> loadSyaratData());

        NavigationView navView = findViewById(R.id.nav_drawer);
        navView.setNavigationItemSelectedListener(item -> {
            drawerLayout.closeDrawer(GravityCompat.END);
            handleNavigation(item.getItemId());
            return true;
        });
    }

    /** Back arrow menutup activity; burger icon membuka navigation drawer. */
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

    /** Judul, hint jumlah, ikon, dan tombol "Buka/Tutup semua" per kategori. */
    private void setupSectionHeaders() {
        bindSectionHeader(sectionUmum,
                getString(R.string.syarat_section_umum),
                getString(R.string.syarat_section_umum_hint),
                R.drawable.ic_check_circle,
                containerUmum);

        bindSectionHeader(sectionKhusus,
                getString(R.string.syarat_section_khusus),
                getString(R.string.syarat_section_khusus_hint),
                R.drawable.ic_description,
                containerKhusus);
    }

    /**
     * Pasang judul tetap section + hint jumlah ("11 berkas wajib dilengkapi"),
     * ikon kategori, dan listener "Buka/Tutup semua". Sub-judul diisi saat data
     * tiba, karena label aslinya dikirim server (kolom kategori).
     */
    private void bindSectionHeader(@Nullable View sectionRoot, String judul,
                                   String hintJumlah, int iconRes,
                                   LinearLayout container) {
        if (sectionRoot == null) return;

        TextView tvJudul = sectionRoot.findViewById(R.id.tv_section_title);
        if (tvJudul != null) tvJudul.setText(judul);

        TextView tvHint = sectionRoot.findViewById(R.id.tv_section_hint);
        if (tvHint != null) tvHint.setText(hintJumlah);

        ImageView ivIcon = sectionRoot.findViewById(R.id.iv_section_icon);
        if (ivIcon != null) ivIcon.setImageResource(iconRes);

        TextView btnToggle = sectionRoot.findViewById(R.id.btn_toggle_all);
        if (btnToggle != null) {
            btnToggle.setOnClickListener(v -> toggleSemuaSection(container, btnToggle));
        }
    }

    // =======================================================================
    // Pengambilan data (background thread + parsing JSON aman)
    // =======================================================================

    private void loadSyaratData() {
        showLoading(true);

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(API_URL + "?kode=" + Uri.encode(kodeLayanan));
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                int code = conn.getResponseCode();
                if (code != HttpURLConnection.HTTP_OK) {
                    throw new Exception("Server membalas dengan kode HTTP " + code);
                }

                StringBuilder body = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        body.append(line);
                    }
                }

                final Payload parsed = parsePayload(body.toString());

                runOnUiThread(() -> {
                    showLoading(false);
                    tampilkanKonten(parsed);
                });

            } catch (Exception e) {
                final String pesan = e.getMessage() == null
                        ? getString(R.string.syarat_error_default)
                        : e.getMessage();
                runOnUiThread(() -> {
                    showLoading(false);
                    tampilkanError(pesan);
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    /**
     * Kantong sementara hasil parsing agar hanya satu kali pass di background thread.
     */
    private static final class Payload {
        LayananInfo layanan;
        final List<SyaratSection> sections = new ArrayList<>();
        final List<LangkahItem> langkah = new ArrayList<>();
    }

    /**
     * Ubah respons JSON get_syarat_nikah.php menjadi model.
     * Seluruh pembacaan memakai opt*() agar kolom yang hilang tidak memicu crash.
     */
    private Payload parsePayload(String json) throws Exception {
        JSONObject root = new JSONObject(json);
        if (!"success".equalsIgnoreCase(root.optString("status", "error"))) {
            throw new Exception(root.optString("message", "Data layanan tidak tersedia."));
        }

        JSONObject data = root.optJSONObject("data");
        if (data == null) {
            throw new Exception("Struktur data tidak valid.");
        }

        Payload p = new Payload();
        p.layanan = parseLayanan(data.optJSONObject("layanan"));

        // ---- Syarat per kategori (accordion) ----
        JSONArray arrSyarat = data.optJSONArray("syarat");
        if (arrSyarat != null) {
            for (int i = 0; i < arrSyarat.length(); i++) {
                JSONObject grup = arrSyarat.optJSONObject(i);
                if (grup == null) continue;

                String kategori = grup.optString("kategori", SyaratItem.KATEGORI_UMUM);
                String label = grup.optString("label", getString(R.string.syarat_toolbar_title));

                List<SyaratItem> items = new ArrayList<>();
                JSONArray arrItems = grup.optJSONArray("items");
                if (arrItems != null) {
                    for (int j = 0; j < arrItems.length(); j++) {
                        items.add(parseSyarat(arrItems.optJSONObject(j), kategori, j));
                    }
                }
                p.sections.add(new SyaratSection(kategori, label, items));
            }
        }

        // ---- Langkah alur pendaftaran (timeline) ----
        JSONArray arrLangkah = data.optJSONArray("langkah");
        if (arrLangkah != null) {
            for (int i = 0; i < arrLangkah.length(); i++) {
                JSONObject o = arrLangkah.optJSONObject(i);
                if (o == null) continue;

                int nomor = o.optInt("nomor_langkah", i + 1);
                p.langkah.add(new LangkahItem(
                        nomor,
                        o.optString("judul_langkah", "Langkah " + nomor),
                        o.optString("deskripsi", ""),
                        o.optString("lokasi", ""),
                        o.optString("estimasi_durasi", "")));
            }
        }

        return p;
    }

    private LayananInfo parseLayanan(JSONObject o) {
        if (o == null) {
            return new LayananInfo(0, kodeLayanan,
                    getString(R.string.syarat_toolbar_title),
                    getString(R.string.syarat_hero_desc_default),
                    getString(R.string.syarat_biaya_rp0), true, "", "", "", "");
        }

        float biaya = (float) o.optDouble("biaya", 0d);
        boolean gratis = o.optBoolean("biaya_gratis", biaya <= 0f);
        String biayaTeks = o.optString("biaya_teks", getString(R.string.syarat_biaya_rp0));

        return new LayananInfo(
                o.optInt("id", 0),
                o.optString("kode", kodeLayanan),
                o.optString("judul", getString(R.string.syarat_toolbar_title)),
                o.optString("deskripsi", ""),
                biayaTeks,
                gratis,
                o.optString("biaya_catatan", ""),
                o.optString("estimasi_durasi", ""),
                o.optString("gambar", ""),
                o.optString("terakhir_diperbarui", ""));
    }

    private SyaratItem parseSyarat(JSONObject o, String kategori, int indexFallback) {
        if (o == null) {
            return new SyaratItem(indexFallback, kategori, "", "", null, true, "");
        }

        List<String> dokumen = new ArrayList<>();
        JSONArray arr = o.optJSONArray("dokumen");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                String d = arr.optString(i, "").trim();
                if (!d.isEmpty()) dokumen.add(d);
            }
        }
        // Kompatibel dengan bentuk string ber-pipe bila server belum mengirim array.
        if (dokumen.isEmpty()) {
            String raw = o.optString("dokumen_diminta", "");
            if (!raw.trim().isEmpty()) {
                for (String d : raw.split("\\|")) {
                    if (!d.trim().isEmpty()) dokumen.add(d.trim());
                }
            }
        }

        return new SyaratItem(
                o.optInt("id_syarat", indexFallback + 1),
                o.optString("kategori", kategori),
                o.optString("judul_syarat", ""),
                o.optString("sub_teks", ""),
                dokumen,
                o.optInt("wajib", 1) == 1,
                o.optString("catatan_tambahan", ""));
    }

    // =======================================================================
    // Render konten
    // =======================================================================

    private void tampilkanKonten(Payload p) {
        layanan = p.layanan;
        sections.clear();
        sections.addAll(p.sections);

        bindHeader();
        bindSyarat(p);
        bindLangkah(p);
        updateChecklistProgress();

        layoutError.setVisibility(View.GONE);
        layoutContent.setVisibility(View.VISIBLE);
    }

    /** Hero + kartu biaya: seluruh teks diambil dari baris tabel {@code layanan}. */
    private void bindHeader() {
        tvLayananJudul.setText(layanan.getJudul());
        setTitle(layanan.getJudul());

        if (!layanan.getDeskripsi().isEmpty()) {
            tvLayananDeskripsi.setText(layanan.getDeskripsi());
        }

        tvBiaya.setText(layanan.getBiayaTeks());

        if (layanan.isGratis()) {
            tvBadgeGratis.setVisibility(View.VISIBLE);
            tvBadgeGratis.setText(R.string.syarat_badge_gratis);
        } else {
            // Layanan berbiaya: badge "GRATIS" diganti catatan resmi agar tidak menyesatkan.
            tvBadgeGratis.setText(R.string.syarat_institution_badge);
        }

        if (!layanan.getBiayaCatatan().isEmpty()) {
            // Catatan biaya dari server menimpa suffix statis "(Gratis) | Hari Kerja".
            tvBiayaCatatan.setText(layanan.getBiayaCatatan());
        }
        tvBiayaCatatan.setVisibility(View.VISIBLE);

        /** Bangun kartu accordion syarat pada container yang sesuai kategorinya. */
    }
    private void bindSyarat(@NonNull Payload p) {
        holders.clear();
        containerUmum.removeAllViews();
        containerKhusus.removeAllViews();

        for (SyaratSection section : p.sections) {
            boolean khusus = SyaratItem.KATEGORI_KHUSUS.equals(section.getKategori());
            LinearLayout container = khusus ? containerKhusus : containerUmum;
            View header = khusus ? sectionKhusus : sectionUmum;
            boolean adaIsi = section.getItemCount() > 0;

            if (header != null) {
                header.setVisibility(adaIsi ? View.VISIBLE : View.GONE);

                TextView tvCount = header.findViewById(R.id.tv_section_count);
                if (tvCount != null) tvCount.setText(section.getJumlahLabel());

                // Sub-judul section memakai label asli dari server (kolom kategori).
                TextView tvSubjudul = header.findViewById(R.id.tv_section_subjudul);
                if (tvSubjudul != null) {
                    tvSubjudul.setText(section.getLabel());
                    tvSubjudul.setVisibility(View.VISIBLE);
                }
            }
            container.setVisibility(adaIsi ? View.VISIBLE : View.GONE);

            for (SyaratItem item : section.getItems()) {
                renderAccordion(container, item);
            }
        }

        updateToggleAllLabel(containerUmum);
        updateToggleAllLabel(containerKhusus);
    }

    /**
     * Satu kartu syarat: isi nilai, pulihkan status centang/expand dari
     * SharedPreferences + model, lalu pasang listener expand-collapse.
     */
    private void renderAccordion(@NonNull LinearLayout container, @NonNull SyaratItem item) {
        View root = LayoutInflater.from(this)
                .inflate(R.layout.item_syarat_accordion, container, false);

        ImageView ivCheck = root.findViewById(R.id.iv_check);
        ImageView ivChevron = root.findViewById(R.id.iv_chevron);
        TextView tvJudul = root.findViewById(R.id.tv_judul_syarat);
        TextView tvBadge = root.findViewById(R.id.tv_badge_wajib);
        final LinearLayout body = root.findViewById(R.id.layout_accordion_body);
        TextView tvSubTeks = root.findViewById(R.id.tv_sub_teks);
        TextView tvDokumenLabel = root.findViewById(R.id.tv_dokumen_label);
        ChipGroup chipGroup = root.findViewById(R.id.chipgroup_dokumen);
        TextView tvCatatan = root.findViewById(R.id.tv_catatan);
        View header = root.findViewById(R.id.layout_accordion_header);

        tvJudul.setText(item.getJudul());

        // Badge status syarat (wajib / sesuai kondisi) — warna mengikuti latar badge.
        if (item.isWajib()) {
            tvBadge.setText(R.string.syarat_badge_wajib);
            tvBadge.setBackgroundResource(R.drawable.bg_badge_blue);
            tvBadge.setTextColor(ContextCompat.getColor(this, R.color.badge_blue_text));
        } else {
            tvBadge.setText(R.string.syarat_badge_kondisi);
            tvBadge.setBackgroundResource(R.drawable.bg_badge_amber);
            tvBadge.setTextColor(ContextCompat.getColor(this, R.color.badge_amber_text));
        }

        // Isi body accordion
        if (item.getSubTeks().isEmpty()) {
            tvSubTeks.setVisibility(View.GONE);
        } else {
            tvSubTeks.setVisibility(View.VISIBLE);
            tvSubTeks.setText(item.getSubTeks());
        }

        if (item.getDokumen().isEmpty()) {
            tvDokumenLabel.setVisibility(View.GONE);
            chipGroup.setVisibility(View.GONE);
        } else {
            tvDokumenLabel.setVisibility(View.VISIBLE);
            chipGroup.setVisibility(View.VISIBLE);
            chipGroup.removeAllViews();

            for (String dok : item.getDokumen()) {
                Chip chip = (Chip) LayoutInflater.from(this)
                        .inflate(R.layout.chip_expertise, chipGroup, false);
                chip.setText(dok);
                chip.setCheckable(false);
                chipGroup.addView(chip);
            }
        }

        if (item.getCatatan().isEmpty()) {
            tvCatatan.setVisibility(View.GONE);
        } else {
            tvCatatan.setVisibility(View.VISIBLE);
            tvCatatan.setText(item.getCatatan());
        }

        // ---- Pulihkan status centang checklist (persisten antar sesi) ----
        item.setChecked(prefs.getBoolean(keyCheck(item.getId()), false));
        terapkanVisualCek(ivCheck, item.isChecked());

        // ---- Pasang listener ----
        header.setOnClickListener(v -> toggleAccordion(item));

        // Ketuk lama header = tanyakan syarat ini ke petugas via WhatsApp.
        header.setOnLongClickListener(v -> {
            bukaWhatsAppTentangSyarat(item.getJudul());
            return true;
        });

        ivCheck.setOnClickListener(v -> {
            boolean baru = !item.isChecked();
            item.setChecked(baru);
            prefs.edit().putBoolean(keyCheck(item.getId()), baru).apply();
            terapkanVisualCek(ivCheck, baru);
            updateChecklistProgress();
        });

        // Tag id syarat dipakai untuk memetakan kartu -> model saat toggle massal.
        root.setTag(item.getId());

        AccordionHolder holder = new AccordionHolder(body, ivChevron, container);
        holders.put(item.getId(), holder);

        // Expand mengikuti status model (mis. kartu dibuka sebelum rotasi layar).
        if (item.isExpanded()) {
            tampilkanBodyTanpaAnimasi(item, body, ivChevron);
        } else {
            body.setVisibility(View.GONE);
            ivChevron.setRotation(0f);
        }

        container.addView(root);
    }

    /**
     * Penegang referensi view milik satu kartu accordion.
     * {@code container} menyimpan section tempat kartu berada, agar tombol
     * "Buka/Tutup semua" hanya memperbarui label pada section yang benar.
     */
    private static final class AccordionHolder {
        final LinearLayout body;
        final ImageView chevron;
        final LinearLayout container;

        AccordionHolder(LinearLayout body, ImageView chevron, LinearLayout container) {
            this.body = body;
            this.chevron = chevron;
            this.container = container;
        }
    }

    // =======================================================================
    // Logika expand / collapse accordion
    // =======================================================================

    /**
     * Buka-tutup body accordion dengan animasi tinggi (height) agar halus seperti
     * transisi web, bukan perubahan visibilitas mendadak.
     */
    private void toggleAccordion(@NonNull SyaratItem item) {
        AccordionHolder holder = holders.get(item.getId());
        if (holder == null) return;

        final boolean expand = !item.isExpanded();
        item.setExpanded(expand);

        if (expand) {
            holder.body.setVisibility(View.VISIBLE);
            animateTinggi(holder.body, 0, tinggiSebenarnya(holder.body), null);
            putarChevron(holder.chevron, 180f);
        } else {
            animateTinggi(holder.body, tinggiSebenarnya(holder.body), 0,
                    () -> holder.body.setVisibility(View.GONE));
            putarChevron(holder.chevron, 0f);
        }

        updateToggleAllLabel(holder.container);
    }

    /** Buka/tutup seluruh kartu dalam satu section lewat tombol header. */
    private void toggleSemuaSection(@NonNull LinearLayout container, @NonNull TextView btnToggle) {
        // Acuan = kondisi saat ini; bila semua sudah terbuka, aksi berikutnya menutup.
        final boolean targetExpand = !semuaTerbuka(container);

        for (View child : daftarAnak(container)) {
            // Cari holder milik kartu ini lewat id syarat yang tersimpan di tag view.
            Object tag = child.getTag();
            if (!(tag instanceof Integer)) continue;

            SyaratItem item = cariItem((Integer) tag);
            if (item == null || item.isExpanded() == targetExpand) continue;

            toggleAccordion(item);
        }

        // Label disinkronkan ulang setelah semua animasi state selesai dipasang.
        updateToggleAllLabel(container);
    }

    private boolean semuaTerbuka(@NonNull LinearLayout container) {
        boolean ada = false;
        for (SyaratItem item : daftarItemContainer(container)) {
            ada = true;
            if (!item.isExpanded()) return false;
        }
        return ada;
    }

    /** Perbarui label tombol "Buka semua" / "Tutup semua" per section. */
    private void updateToggleAllLabel(@Nullable LinearLayout container) {
        if (container == null) return;

        boolean umum = container == containerUmum;
        View header = umum ? sectionUmum : sectionKhusus;
        if (header == null) return;

        TextView btnToggle = header.findViewById(R.id.btn_toggle_all);
        if (btnToggle == null) return;

        // Bila semua kartu tertutup, tawarkan "Buka semua"; selain itu "Tutup semua".
        boolean semuaTertutup = !sedikitnyaSatuTerbuka(container);
        btnToggle.setText(semuaTertutup
                ? R.string.syarat_buka_semua
                : R.string.syarat_tutup_semua);
    }

    private boolean sedikitnyaSatuTerbuka(@NonNull LinearLayout container) {
        for (SyaratItem item : daftarItemContainer(container)) {
            if (item.isExpanded()) return true;
        }
        return false;
    }

    /** Kumpulkan item syarat yang render-nya berada di dalam container tertentu. */
    @NonNull
    private List<SyaratItem> daftarItemContainer(@NonNull LinearLayout container) {
        List<SyaratItem> hasil = new ArrayList<>();
        for (View child : daftarAnak(container)) {
            Object tag = child.getTag();
            if (tag instanceof Integer) {
                SyaratItem item = cariItem((Integer) tag);
                if (item != null) hasil.add(item);
            }
        }
        return hasil;
    }

    @NonNull
    private List<View> daftarAnak(@NonNull ViewGroup group) {
        List<View> anak = new ArrayList<>();
        for (int i = 0; i < group.getChildCount(); i++) {
            anak.add(group.getChildAt(i));
        }
        return anak;
    }

    @Nullable
    private SyaratItem cariItem(int idSyarat) {
        for (SyaratSection section : sections) {
            for (SyaratItem item : section.getItems()) {
                if (item.getId() == idSyarat) return item;
            }
        }
        return null;
    }

    /**
     * Ukur tinggi konten body secara aktual (mengakomodasi chip yang wrap dan
     * teks multi-baris) sebelum animasi dijalankan.
     */
    private int tinggiSebenarnya(@NonNull View view) {
        int widthSpec = View.MeasureSpec.makeMeasureSpec(
                Math.max(view.getWidth(), getContentWidth()), View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(widthSpec, heightSpec);
        return view.getMeasuredHeight();
    }

    /** Lebar konten efektif = lebar layar dikurangi margin horizontal kartu. */
    private int getContentWidth() {
        int margin = (int) (36 * getResources().getDisplayMetrics().density);
        return getResources().getDisplayMetrics().widthPixels - margin;
    }

    private void animateTinggi(@NonNull final View view, int dari, int ke) {
        animateTinggi(view, dari, ke, null);
    }

    /**
     * Animasi tinggi body accordion.
     * @param selesai opsional, dijalankan setelah animasi tuntas (mis. sembunyikan body)
     */
    private void animateTinggi(@NonNull final View view, int dari, int ke,
                               @Nullable final Runnable selesai) {
        ValueAnimator animator = ValueAnimator.ofInt(dari, Math.max(ke, 0));
        animator.setDuration(ANIMASI_ACCORDION_MS);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(a -> {
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            lp.height = (int) a.getAnimatedValue();
            view.setLayoutParams(lp);
        });
        // Setelah animasi, kembalikan tinggi ke wrap_content supaya isi dinamis
        // (chip wrap / teks multi-baris hasil update server) tetap terukur benar.
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                ViewGroup.LayoutParams lp = view.getLayoutParams();
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                view.setLayoutParams(lp);
                if (selesai != null) selesai.run();
            }
        });
        animator.start();
    }

    private void putarChevron(@NonNull ImageView chevron, float derajat) {
        chevron.animate()
                .rotation(derajat)
                .setDuration(ANIMASI_ACCORDION_MS)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    /** Buka body tanpa animasi (dipakai saat render ulang / state dipulihkan). */
    private void tampilkanBodyTanpaAnimasi(@NonNull SyaratItem item,
                                           @NonNull LinearLayout body,
                                           @NonNull ImageView chevron) {
        body.setVisibility(View.VISIBLE);
        ViewGroup.LayoutParams lp = body.getLayoutParams();
        lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        body.setLayoutParams(lp);
        chevron.setRotation(180f);
        item.setExpanded(true);
    }

    // =======================================================================
    // Timeline alur pendaftaran
    // =======================================================================

    private void bindLangkah(@NonNull Payload p) {
        containerLangkah.removeAllViews();

        TextView tvSubtitle = findViewById(R.id.tv_langkah_subtitle);
        if (p.langkah.isEmpty()) {
            tvSubtitle.setVisibility(View.GONE);
            containerLangkah.setVisibility(View.GONE);
            return;
        }

        tvSubtitle.setVisibility(View.VISIBLE);
        tvSubtitle.setText(getString(R.string.syarat_langkah_subtitle, p.langkah.size()));
        containerLangkah.setVisibility(View.VISIBLE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < p.langkah.size(); i++) {
            LangkahItem item = p.langkah.get(i);
            View root = inflater.inflate(R.layout.item_langkah_timeline, containerLangkah, false);

            ((TextView) root.findViewById(R.id.tv_nomor_langkah)).setText(String.valueOf(item.getNomor()));

            TextView tvJudul = root.findViewById(R.id.tv_judul_langkah);
            TextView tvDeskripsi = root.findViewById(R.id.tv_deskripsi_langkah);
            TextView tvLokasi = root.findViewById(R.id.tv_lokasi_langkah);
            TextView tvEstimasi = root.findViewById(R.id.tv_estimasi_langkah);
            View garis = root.findViewById(R.id.view_timeline_line);

            tvJudul.setText(item.getJudul());

            if (item.getDeskripsi().isEmpty()) {
                tvDeskripsi.setVisibility(View.GONE);
            } else {
                tvDeskripsi.setVisibility(View.VISIBLE);
                tvDeskripsi.setText(item.getDeskripsi());
            }

            tvLokasi.setText(item.getLokasi());
            tvLokasi.setVisibility(item.getLokasi().isEmpty() ? View.GONE : View.VISIBLE);

            tvEstimasi.setText(item.getEstimasi());
            tvEstimasi.setVisibility(item.getEstimasi().isEmpty() ? View.GONE : View.VISIBLE);

            // Langkah terakhir tidak butuh garis penghubung ke bawah.
            garis.setVisibility(i == p.langkah.size() - 1 ? View.INVISIBLE : View.VISIBLE);

            containerLangkah.addView(root);
        }
    }

    // =======================================================================
    // Checklist progres persiapan berkas
    // =======================================================================

    private String keyCheck(int idSyarat) {
        // Sertakan kode layanan agar centang tidak tertukar antar layanan.
        return "syarat_" + kodeLayanan + "_" + idSyarat;
    }

    private void terapkanVisualCek(@NonNull ImageView ivCheck, boolean cek) {
        ivCheck.setColorFilter(ContextCompat.getColor(
                this, cek ? R.color.syarat_primary : R.color.text_tertiary));
    }

    /** Perbarui teks + bar progres "x dari y syarat sudah disiapkan". */
    private void updateChecklistProgress() {
        int total = 0;
        int siap = 0;

        for (SyaratSection section : sections) {
            for (SyaratItem item : section.getItems()) {
                total++;
                if (item.isChecked()) siap++;
            }
        }

        progressChecklist.setMax(Math.max(total, 1));
        progressChecklist.setProgress(total == 0 ? 0 : Math.round(100f * siap / total));

        if (total == 0) {
            tvProgressLabel.setText(R.string.syarat_progress_kosong);
        } else if (siap == 0) {
            tvProgressLabel.setText(R.string.syarat_progress_kosong);
        } else {
            tvProgressLabel.setText(getString(R.string.syarat_progress_format, siap, total));
        }
    }

    // =======================================================================
    // Tombol aksi bawah
    // =======================================================================

    private void setupActionButtons() {
        findViewById(R.id.btn_daftar_nikah).setOnClickListener(v -> bukaDaftarNikah());
        findViewById(R.id.btn_unduh_blanko).setOnClickListener(v ->
                navigateTo(BlankoActivity.class));

        // Sticky redesign: tautan bantuan & tombol "Tanya Petugas" membuka konsultasi.
        findViewById(R.id.btn_notice_konsultasi).setOnClickListener(v ->
                startActivity(new Intent(this, ConsultationActivity.class)));
        findViewById(R.id.card_bantuan_harian).setOnClickListener(v ->
                startActivity(new Intent(this, ConsultationActivity.class)));
    }

    /** Daftar nikah online — dibuka di browser pada portal SIMKAH Kemenag. */
   /** Buka alur pendaftaran nikah (Langkah 1) di dalam aplikasi. */
private void bukaDaftarNikah() {
    startActivity(new Intent(this, DaftarNikahActivity.class));
}

    /**
     * Konsultasi satu syarat ke petugas KUA lewat WhatsApp.
     * @param judulSyarat judul syarat yang sedang ditanyakan
     */
    private void bukaWhatsAppTentangSyarat(String judulSyarat) {
        try {
            String pesan = getString(R.string.syarat_wa_message, judulSyarat);
            String url = "https://wa.me/" + ADMIN_WHATSAPP + "?text=" + Uri.encode(pesan);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            showToast(getString(R.string.consultation_wa_unavailable));
        }
    }
    /** Buka WhatsApp langsung ke nomor petugas KUA dengan pesan awal. */
private void bukaWhatsAppPetugas() {
    try {
        String pesan = getString(R.string.syarat_wa_umum);
        String url = "https://wa.me/" + ADMIN_WHATSAPP + "?text=" + Uri.encode(pesan);
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    } catch (ActivityNotFoundException e) {
        showToast(getString(R.string.consultation_wa_unavailable));
    }
}

    // =======================================================================
    // Navigasi & state
    // =======================================================================

    /**
     * Bottom nav persisten 5 tab (Beranda | Layanan* | Daftar Nikah | Konsultasi | Lainnya).
     * "Layanan" tetap tab aktif (halang balik ke daftar layanan saat tab sama
     * ditekan ulang); "Daftar Nikah" membuka portal SIMKAH dan "Lainnya" membuka
     * drawer tanpa memindahkan highlight.
     */
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.menu_services);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_services) {
                navigateTo(ServicesActivity.class);
                return true;
            }
            if (id == R.id.menu_daftar_nikah) {
                bukaDaftarNikah();
                bottomNav.setSelectedItemId(R.id.menu_services);
                return false;
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
            bukaDaftarNikah();
        } else if (itemId == R.id.menu_lainnya) {
            showToast(getString(R.string.syarat_feature_soon));
        }
    }

    private void navigateTo(Class<?> target) {
        Intent intent = new Intent(this, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean loading) {
        progressLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            layoutError.setVisibility(View.GONE);
            layoutContent.setVisibility(View.GONE);
        }
    }

    private void tampilkanError(String pesan) {
        sections.clear();
        holders.clear();
        layoutContent.setVisibility(View.GONE);
        layoutError.setVisibility(View.VISIBLE);
        tvErrorMessage.setText(pesan);
    }

    private void showToast(String pesan) {
        Toast.makeText(this, pesan, Toast.LENGTH_SHORT).show();
    }
}
