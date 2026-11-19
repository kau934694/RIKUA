package com.example.rikua;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.ActivityNotFoundException;
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
import androidx.core.graphics.Insets;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SyaratNikahActivity — Halaman "Syarat Nikah" RIKUA KUA Kec. Randudongkal.
 */
public class SyaratNikahActivity extends AppCompatActivity {

    private static final String TAG = "SyaratNikah";

    /** Endpoint data — host API Laragon yang sama dengan layar lain. */
    private static final String API_URL =
            "http://192.168.1.8/rikua_api/get_syarat_nikah.php";
    /** Slug default (kolom {@code kode_layanan}) bila Intent tidak mengirim slug. */
    private static final String KODE_LAYANAN = "syarat-nikah";

    /** Extra utama: objek layanan (Serializable) yang diklik di daftar layanan. */
    public static final String EXTRA_LAYANAN = "extra_layanan";
    /** Extra fallback: slug kode_layanan untuk request API. */
    public static final String EXTRA_KODE_LAYANAN = "extra_kode_layanan";
    /** Extra fallback: judul layanan untuk teks sementara sebelum data tiba. */
    public static final String EXTRA_JUDUL_LAYANAN = "extra_judul_layanan";

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
    private View progressLoading;
    private LinearLayout layoutContent;
    private LinearLayout layoutError;
    private TextView tvErrorMessage;

    // ---------- Views: header & estimasi ----------
    private TextView tvLayananJudul;
    private TextView tvLayananDeskripsi;

    // ---------- Views: progres checklist ----------


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

        bacaExtraLayanan();
        loadSyaratData();
    }

    /**
     * Baca Intent mengikuti konvensi project: objek layanan (Serializable) sebagai
     * sumber utama, lalu fallback ke extra String bila pengirim hanya membawa slug.
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
            if (tvLayananJudul != null) tvLayananJudul.setText(judulSementara);
            setTitle(judulSementara);
        }
    }

    // =======================================================================
    // Setup tampilan
    // =======================================================================

    private void setupWindowAndStatusBar() {
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.syarat_primary));

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(false);
            controller.setAppearanceLightNavigationBars(true);
        }

        View bottomSticky = findViewById(R.id.layout_bottom_sticky);
        if (bottomSticky != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomSticky, (view, insets) -> {
                Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
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


        sectionUmum = findViewById(R.id.section_umum);
        sectionKhusus = findViewById(R.id.section_khusus);

        containerUmum = findViewById(R.id.container_syarat_umum);
        containerKhusus = findViewById(R.id.container_syarat_khusus);
        containerLangkah = findViewById(R.id.container_langkah);

        View btnRetry = findViewById(R.id.btn_retry);
        if (btnRetry != null) {
            btnRetry.setOnClickListener(v -> loadSyaratData());
        }

        NavigationView navView = findViewById(R.id.nav_drawer);
        if (navView != null) {
            navView.setNavigationItemSelectedListener(item -> {
                if (drawerLayout != null) {
                    drawerLayout.closeDrawer(GravityCompat.END);
                }
                handleNavigation(item.getItemId());
                return true;
            });
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar == null) return;

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
        if (drawerLayout == null) return;
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            drawerLayout.openDrawer(GravityCompat.END);
        }
    }

    private void setupSectionHeaders() {
        bindSectionHeader(sectionUmum,
                "Untuk Semua Pendaftar",
                "Berkas wajib dilengkapi",
                R.drawable.ic_check_circle,
                containerUmum);

        bindSectionHeader(sectionKhusus,
                "Jika Berlaku untuk Anda",
                "Berkas kondisi tertentu",
                R.drawable.ic_description,
                containerKhusus);
    }

    private void bindSectionHeader(@Nullable View sectionRoot, String judul,
                                   String hintJumlah, int iconRes,
                                   @Nullable LinearLayout container) {
        if (sectionRoot == null) return;

        TextView tvJudul = sectionRoot.findViewById(R.id.tv_section_title);
        if (tvJudul != null) tvJudul.setText(judul);

        TextView tvHint = sectionRoot.findViewById(R.id.tv_section_hint);
        if (tvHint != null) tvHint.setText(hintJumlah);

        ImageView ivIcon = sectionRoot.findViewById(R.id.iv_section_icon);
        if (ivIcon != null) ivIcon.setImageResource(iconRes);

        TextView btnToggle = sectionRoot.findViewById(R.id.btn_toggle_all);
        if (btnToggle != null && container != null) {
            btnToggle.setOnClickListener(v -> toggleSemuaSection(container));
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
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        body.append(line);
                    }
                }

                final Payload parsed = parsePayload(body.toString());

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    showLoading(false);
                    tampilkanKonten(parsed);
                });

            } catch (Exception e) {
                final String pesan = (e.getMessage() == null || e.getMessage().isEmpty())
                        ? "Gagal memuat data persyaratan. Periksa koneksi internet Anda."
                        : e.getMessage();
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    showLoading(false);
                    tampilkanError(pesan);
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private static final class Payload {
        LayananInfo layanan;
        final List<SyaratSection> sections = new ArrayList<>();
        final List<LangkahItem> langkah = new ArrayList<>();
    }

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
                String label = grup.optString("label", "Persyaratan");

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

    private LayananInfo parseLayanan(@Nullable JSONObject o) {
        if (o == null) {
            return new LayananInfo(0, kodeLayanan,
                    "Syarat Nikah",
                    "",
                    "Rp 0", true, "", "", "", "");
        }

        float biaya = (float) o.optDouble("biaya", 0d);
        boolean gratis = o.optBoolean("biaya_gratis", biaya <= 0f);
        String biayaTeks = o.optString("biaya_teks", "Rp 0");

        return new LayananInfo(
                o.optInt("id", 0),
                o.optString("kode", kodeLayanan),
                o.optString("judul", "Syarat Nikah"),
                o.optString("deskripsi", ""),
                biayaTeks,
                gratis,
                o.optString("biaya_catatan", ""),
                o.optString("estimasi_durasi", ""),
                o.optString("gambar", ""),
                o.optString("terakhir_diperbarui", ""));
    }

    private SyaratItem parseSyarat(@Nullable JSONObject o, String kategori, int indexFallback) {
        if (o == null) {
            return new SyaratItem(indexFallback, kategori, "", "", new ArrayList<>(), true, "");
        }

        List<String> dokumen = new ArrayList<>();
        JSONArray arr = o.optJSONArray("dokumen");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                String d = arr.optString(i, "").trim();
                if (!d.isEmpty()) dokumen.add(d);
            }
        }

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

        if (layoutError != null) layoutError.setVisibility(View.GONE);
        if (layoutContent != null) layoutContent.setVisibility(View.VISIBLE);
    }

    private void bindHeader() {
        if (layanan == null) return;

        if (tvLayananJudul != null) {
            tvLayananJudul.setText(layanan.getJudul());
        }
        setTitle(layanan.getJudul());

        if (tvLayananDeskripsi != null) {
            if (!layanan.getDeskripsi().isEmpty()) {
                tvLayananDeskripsi.setText(layanan.getDeskripsi());
                tvLayananDeskripsi.setVisibility(View.VISIBLE);
            } else {
                tvLayananDeskripsi.setVisibility(View.GONE);
            }
        }
    }

    private void bindSyarat(@NonNull Payload p) {
        holders.clear();
        if (containerUmum != null) containerUmum.removeAllViews();
        if (containerKhusus != null) containerKhusus.removeAllViews();

        for (SyaratSection section : p.sections) {
            boolean khusus = SyaratItem.KATEGORI_KHUSUS.equals(section.getKategori());
            LinearLayout container = khusus ? containerKhusus : containerUmum;
            View header = khusus ? sectionKhusus : sectionUmum;
            boolean adaIsi = section.getItemCount() > 0;

            if (header != null) {
                header.setVisibility(adaIsi ? View.VISIBLE : View.GONE);

                TextView tvCount = header.findViewById(R.id.tv_section_count);
                if (tvCount != null) tvCount.setText(section.getJumlahLabel());

                TextView tvSubjudul = header.findViewById(R.id.tv_section_subjudul);
                if (tvSubjudul != null) {
                    tvSubjudul.setText(section.getLabel());
                    tvSubjudul.setVisibility(View.VISIBLE);
                }
            }

            if (container != null) {
                container.setVisibility(adaIsi ? View.VISIBLE : View.GONE);
                for (SyaratItem item : section.getItems()) {
                    renderAccordion(container, item);
                }
            }
        }

        updateToggleAllLabel(containerUmum);
        updateToggleAllLabel(containerKhusus);
    }

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

        if (tvJudul != null) tvJudul.setText(item.getJudul());

        if (tvBadge != null) {
            if (item.isWajib()) {
                tvBadge.setText("Wajib");
                tvBadge.setBackgroundResource(R.drawable.bg_badge_emerald);
                tvBadge.setTextColor(ContextCompat.getColor(this, R.color.badge_emerald_text));
            } else {
                tvBadge.setText("Sesuai Kondisi");
                tvBadge.setBackgroundResource(R.drawable.bg_badge_amber);
                tvBadge.setTextColor(ContextCompat.getColor(this, R.color.badge_amber_text));
            }
        }

        if (tvSubTeks != null) {
            if (item.getSubTeks().isEmpty()) {
                tvSubTeks.setVisibility(View.GONE);
            } else {
                tvSubTeks.setVisibility(View.VISIBLE);
                tvSubTeks.setText(item.getSubTeks());
            }
        }

        if (tvDokumenLabel != null && chipGroup != null) {
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
        }

        if (tvCatatan != null) {
            if (item.getCatatan().isEmpty()) {
                tvCatatan.setVisibility(View.GONE);
            } else {
                tvCatatan.setVisibility(View.VISIBLE);
                tvCatatan.setText(item.getCatatan());
            }
        }

        item.setChecked(prefs.getBoolean(keyCheck(item.getId()), false));
        if (ivCheck != null) {
            terapkanVisualCek(ivCheck, item.isChecked());
            ivCheck.setOnClickListener(v -> {
                boolean baru = !item.isChecked();
                item.setChecked(baru);
                prefs.edit().putBoolean(keyCheck(item.getId()), baru).apply();
                terapkanVisualCek(ivCheck, baru);
            });
        }

        if (header != null) {
            header.setOnClickListener(v -> toggleAccordion(item));
            header.setOnLongClickListener(v -> {
                bukaWhatsAppTentangSyarat(item.getJudul());
                return true;
            });
        }

        root.setTag(item.getId());

        if (body != null && ivChevron != null) {
            AccordionHolder holder = new AccordionHolder(body, ivChevron, container);
            holders.put(item.getId(), holder);

            if (item.isExpanded()) {
                tampilkanBodyTanpaAnimasi(item, body, ivChevron);
            } else {
                body.setVisibility(View.GONE);
                ivChevron.setRotation(0f);
            }
        }

        container.addView(root);
    }

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

    private void toggleSemuaSection(@NonNull LinearLayout container) {
        final boolean targetExpand = !semuaTerbuka(container);

        for (View child : daftarAnak(container)) {
            Object tag = child.getTag();
            if (!(tag instanceof Integer)) continue;

            SyaratItem item = cariItem((Integer) tag);
            if (item == null || item.isExpanded() == targetExpand) continue;

            toggleAccordion(item);
        }

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

    private void updateToggleAllLabel(@Nullable LinearLayout container) {
        if (container == null) return;

        boolean umum = container == containerUmum;
        View header = umum ? sectionUmum : sectionKhusus;
        if (header == null) return;

        TextView btnToggle = header.findViewById(R.id.btn_toggle_all);
        if (btnToggle == null) return;

        boolean semuaTertutup = !sedikitnyaSatuTerbuka(container);
        btnToggle.setText(semuaTertutup ? "Buka semua" : "Tutup semua");
    }

    private boolean sedikitnyaSatuTerbuka(@NonNull LinearLayout container) {
        for (SyaratItem item : daftarItemContainer(container)) {
            if (item.isExpanded()) return true;
        }
        return false;
    }

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

    private int tinggiSebenarnya(@NonNull View view) {
        int widthSpec = View.MeasureSpec.makeMeasureSpec(
                Math.max(view.getWidth(), getContentWidth()), View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(widthSpec, heightSpec);
        return view.getMeasuredHeight();
    }

    private int getContentWidth() {
        int margin = (int) (36 * getResources().getDisplayMetrics().density);
        return getResources().getDisplayMetrics().widthPixels - margin;
    }

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
        if (containerLangkah == null) return;
        containerLangkah.removeAllViews();

        TextView tvSubtitle = findViewById(R.id.tv_langkah_subtitle);
        if (p.langkah.isEmpty()) {
            if (tvSubtitle != null) tvSubtitle.setVisibility(View.GONE);
            containerLangkah.setVisibility(View.GONE);
            return;
        }

        if (tvSubtitle != null) {
            tvSubtitle.setVisibility(View.VISIBLE);
            tvSubtitle.setText(p.langkah.size() + " tahapan pendaftaran");
        }
        containerLangkah.setVisibility(View.VISIBLE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < p.langkah.size(); i++) {
            LangkahItem item = p.langkah.get(i);
            View root = inflater.inflate(R.layout.item_langkah_timeline, containerLangkah, false);

            TextView tvNomor = root.findViewById(R.id.tv_nomor_langkah);
            TextView tvJudul = root.findViewById(R.id.tv_judul_langkah);
            TextView tvDeskripsi = root.findViewById(R.id.tv_deskripsi_langkah);
            TextView tvLokasi = root.findViewById(R.id.tv_lokasi_langkah);
            TextView tvEstimasi = root.findViewById(R.id.tv_estimasi_langkah);
            View garis = root.findViewById(R.id.view_timeline_line);

            if (tvNomor != null) tvNomor.setText(String.valueOf(item.getNomor()));
            if (tvJudul != null) tvJudul.setText(item.getJudul());

            if (tvDeskripsi != null) {
                if (item.getDeskripsi().isEmpty()) {
                    tvDeskripsi.setVisibility(View.GONE);
                } else {
                    tvDeskripsi.setVisibility(View.VISIBLE);
                    tvDeskripsi.setText(item.getDeskripsi());
                }
            }

            if (tvLokasi != null) {
                tvLokasi.setText(item.getLokasi());
                tvLokasi.setVisibility(item.getLokasi().isEmpty() ? View.GONE : View.VISIBLE);
            }

            if (tvEstimasi != null) {
                tvEstimasi.setText(item.getEstimasi());
                tvEstimasi.setVisibility(item.getEstimasi().isEmpty() ? View.GONE : View.VISIBLE);
            }

            if (garis != null) {
                garis.setVisibility(i == p.langkah.size() - 1 ? View.INVISIBLE : View.VISIBLE);
            }

            containerLangkah.addView(root);
        }
    }

    // =======================================================================
    // Checklist progres persiapan berkas
    // =======================================================================

    private String keyCheck(int idSyarat) {
        return "syarat_" + kodeLayanan + "_" + idSyarat;
    }

    private void terapkanVisualCek(@NonNull ImageView ivCheck, boolean cek) {
        ivCheck.setColorFilter(ContextCompat.getColor(
                this, cek ? R.color.syarat_primary : R.color.text_tertiary));
    }
    // =======================================================================
    // Tombol aksi bawah
    // =======================================================================

    private void setupActionButtons() {
        View btnDaftar = findViewById(R.id.btn_daftar_nikah);
        if (btnDaftar != null) {
            btnDaftar.setOnClickListener(v -> bukaDaftarNikah());
        }

        View btnBlanko = findViewById(R.id.btn_unduh_blanko);
        if (btnBlanko != null) {
            btnBlanko.setOnClickListener(v -> navigateTo(BlankoActivity.class));
        }

        View btnNotice = findViewById(R.id.btn_notice_konsultasi);
        if (btnNotice != null) {
            btnNotice.setOnClickListener(v ->
                    startActivity(new Intent(this, ConsultationActivity.class)));
        }

        View cardBantuan = findViewById(R.id.card_bantuan_harian);
        if (cardBantuan != null) {
            cardBantuan.setOnClickListener(v ->
                    startActivity(new Intent(this, ConsultationActivity.class)));
        }
    }

    private void bukaDaftarNikah() {
        startActivity(new Intent(this, DaftarNikahActivity.class));
    }

    private void bukaWhatsAppTentangSyarat(String judulSyarat) {
        try {
            String pesan = "Assalamu'alaikum Admin KUA, saya ingin bertanya mengenai syarat: " + judulSyarat;
            String url = "https://wa.me/" + ADMIN_WHATSAPP + "?text=" + Uri.encode(pesan);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            showToast("WhatsApp tidak terpasang di perangkat ini");
        }
    }

    // =======================================================================
    // Navigasi & state
    // =======================================================================

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav == null) return;

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
            showToast("Fitur segera hadir");
        }
    }

    private void navigateTo(Class<?> target) {
        Intent intent = new Intent(this, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean loading) {
        if (progressLoading != null) {
            progressLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (loading) {
            if (layoutError != null) layoutError.setVisibility(View.GONE);
            if (layoutContent != null) layoutContent.setVisibility(View.GONE);
        }
    }

    private void tampilkanError(String pesan) {
        sections.clear();
        holders.clear();
        if (layoutContent != null) layoutContent.setVisibility(View.GONE);
        if (layoutError != null) layoutError.setVisibility(View.VISIBLE);
        if (tvErrorMessage != null) tvErrorMessage.setText(pesan);
    }

    private void showToast(String pesan) {
        Toast.makeText(this, pesan, Toast.LENGTH_SHORT).show();
    }
}