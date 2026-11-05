package com.example.rikua;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.ChipGroup;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServicesActivity extends AppCompatActivity {

    private static final String TAG = "Layanan";
    private static final String URL_LAYANAN = "http://192.168.1.4/rikua_api/get_layanan.php";
    private static final String KODE_SYARAT_NIKAH = "syarat-nikah";

    private BottomNavigationView bottomNavigation;
    private RecyclerView rvServices;
    private EditText etSearch;
    private ImageButton btnClearSearch;
    private ChipGroup chipGroupFilters;

    private ServiceAdapter adapter;
    private final List<ServiceItem> allServices = new ArrayList<>();
    private String selectedCategory = null;
    private String searchQuery = "";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_services);

        setupWindowAndStatusBar();
        initViews();
        setupMenuDrawer();
        setupToolbar();
        setupServiceList();
        setupSearch();
        setupFilterChips();
        setupClickListeners();
        setupBottomNavigation();

        loadServices();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private void setupWindowAndStatusBar() {
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary_forest));

        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(false);
        insetsController.setAppearanceLightNavigationBars(true);
    }
    private void setupMenuDrawer() {
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        View btnMenuDrawer = findViewById(R.id.btn_menu_drawer);

        if (btnMenuDrawer != null && drawerLayout != null) {
            btnMenuDrawer.setOnClickListener(v -> {
                drawerLayout.openDrawer(androidx.core.view.GravityCompat.END);
            });
        }
    }
    private void initViews() {
        bottomNavigation = findViewById(R.id.bottom_navigation);
        rvServices = findViewById(R.id.rv_services);
        etSearch = findViewById(R.id.et_search);
        btnClearSearch = findViewById(R.id.btn_clear_search);
        chipGroupFilters = findViewById(R.id.chip_group_filters);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> navigateTo(MainActivity.class));
        }
    }

    private void setupServiceList() {
        adapter = new ServiceAdapter(this::bukaDetailLayanan);
        rvServices.setLayoutManager(new LinearLayoutManager(this));
        rvServices.setAdapter(adapter);
    }

    private void bukaDetailLayanan(ServiceItem item) {
        String slug = kodeLayananFor(item);
        if (slug == null) {
            showToast(item.getTitle());
            return;
        }

        if (slug.contains("rekomendasi")) {
            Intent intent = new Intent(this, RekomendasiNikahActivity.class);
            intent.putExtra(RekomendasiNikahActivity.EXTRA_LAYANAN, item);
            intent.putExtra(RekomendasiNikahActivity.EXTRA_KODE_LAYANAN, slug);
            intent.putExtra(RekomendasiNikahActivity.EXTRA_JUDUL_LAYANAN, item.getTitle());
            startActivity(intent);
            return;
        }

        if (slug.contains("isbat")) {
            Intent intent = new Intent(this, IsbatNikahActivity.class);
            intent.putExtra(IsbatNikahActivity.EXTRA_LAYANAN, item);
            intent.putExtra(IsbatNikahActivity.EXTRA_KODE_LAYANAN, slug);
            intent.putExtra(IsbatNikahActivity.EXTRA_JUDUL_LAYANAN, item.getTitle());
            startActivity(intent);
            return;
        }

        if (slug.contains("duplikat")) {
            Intent intent = new Intent(this, DuplikatBukuNikahActivity.class);
            intent.putExtra(DuplikatBukuNikahActivity.EXTRA_LAYANAN, item);
            intent.putExtra(DuplikatBukuNikahActivity.EXTRA_KODE_LAYANAN, slug);
            intent.putExtra(DuplikatBukuNikahActivity.EXTRA_JUDUL_LAYANAN, item.getTitle());
            startActivity(intent);
            return;
        }

        if (slug.contains("wakaf")) {
            Intent intent = new Intent(this, WakafActivity.class);
            intent.putExtra(WakafActivity.EXTRA_LAYANAN, item);
            intent.putExtra(WakafActivity.EXTRA_KODE_LAYANAN, slug);
            intent.putExtra(WakafActivity.EXTRA_JUDUL_LAYANAN, item.getTitle());
            startActivity(intent);
            return;
        }

        if (slug.contains("taukil")) {
            Intent intent = new Intent(this, TaukilNikahActivity.class);
            intent.putExtra(TaukilNikahActivity.EXTRA_LAYANAN, item);
            intent.putExtra(TaukilNikahActivity.EXTRA_KODE_LAYANAN, slug);
            intent.putExtra(TaukilNikahActivity.EXTRA_JUDUL_LAYANAN, item.getTitle());
            startActivity(intent);
            return;
        }

        if (slug.contains("wali")) {
            Intent intent = new Intent(this, WaliNikahActivity.class);
            intent.putExtra(WaliNikahActivity.EXTRA_LAYANAN, item);
            intent.putExtra(WaliNikahActivity.EXTRA_KODE_LAYANAN, slug);
            intent.putExtra(WaliNikahActivity.EXTRA_JUDUL_LAYANAN, item.getTitle());
            startActivity(intent);
            return;
        }

        if (slug.contains("pengumuman")) {
            Intent intent = new Intent(this, PengumumanNikahActivity.class);
            intent.putExtra(PengumumanNikahActivity.EXTRA_LAYANAN, item);
            intent.putExtra(PengumumanNikahActivity.EXTRA_KODE_LAYANAN, slug);
            intent.putExtra(PengumumanNikahActivity.EXTRA_JUDUL_LAYANAN, item.getTitle());
            startActivity(intent);
            return;
        }

        Intent intent = new Intent(this, SyaratNikahActivity.class);
        intent.putExtra(SyaratNikahActivity.EXTRA_LAYANAN, item);
        intent.putExtra(SyaratNikahActivity.EXTRA_KODE_LAYANAN, slug);
        intent.putExtra(SyaratNikahActivity.EXTRA_JUDUL_LAYANAN, item.getTitle());
        startActivity(intent);
    }

    private String kodeLayananFor(ServiceItem item) {
        String slug = item.getSlug().trim().toLowerCase();
        if (!slug.isEmpty()) {
            return slug;
        }
        String judul = item.getTitle() == null ? "" : item.getTitle().toLowerCase();
        if (judul.contains("rekomendasi")) return "rekomendasi-nikah";
        if (judul.contains("isbat")) return "isbat-nikah";
        if (judul.contains("duplikat")) return "duplikat-buku-nikah";
        if (judul.contains("wakaf")) return "wakaf-kemasjidan";
        if (judul.contains("taukil")) return "taukil-wali-bil-kitabah";
        if (judul.contains("wali")) return "wali-nikah";
        if (judul.contains("pengumuman")) return "pengumuman-kehendak-nikah";
        return judul.contains("syarat") ? KODE_SYARAT_NIKAH : null;
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                searchQuery = s.toString();
                btnClearSearch.setVisibility(searchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                applyFilter();
            }
        });

        btnClearSearch.setOnClickListener(v -> etSearch.setText(""));
    }

    private void setupFilterChips() {
        chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            int id = checkedIds.isEmpty() ? R.id.chip_all : checkedIds.get(0);
            if (id == R.id.chip_pernikahan) {
                selectedCategory = ServiceItem.CATEGORY_PERNIKAHAN;
            } else if (id == R.id.chip_administrasi) {
                selectedCategory = ServiceItem.CATEGORY_ADMINISTRASI;
            } else if (id == R.id.chip_wakaf) {
                selectedCategory = ServiceItem.CATEGORY_WAKAF;
            } else {
                selectedCategory = null;
            }
            applyFilter();
        });
    }

    private void setupClickListeners() {
        View btnTanya = findViewById(R.id.btn_tanya_petugas);
        if (btnTanya != null) {
            btnTanya.setOnClickListener(v -> navigateTo(ConsultationActivity.class));
        }
    }

    private void setupBottomNavigation() {
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.menu_services);
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.menu_home) {
                    navigateTo(MainActivity.class);
                    return true;
                } else if (itemId == R.id.menu_services) {
                    return true;
                } else if (itemId == R.id.menu_consultation) {
                    navigateTo(ConsultationActivity.class);
                    return true;
                } else if (itemId == R.id.menu_facilities) {
                    navigateTo(FacilitiesActivity.class);
                    return true;
                } else if (itemId == R.id.menu_forms) {
                    navigateTo(BlankoActivity.class);
                    return true;
                }
                return false;
            });
        }
    }

    private void loadServices() {
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(URL_LAYANAN).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                int code = conn.getResponseCode();
                if (code != HttpURLConnection.HTTP_OK) {
                    throw new Exception("HTTP " + code);
                }

                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                }
                Log.d(TAG, "Response: " + sb);

                JSONObject root = new JSONObject(sb.toString());
                if (!"success".equals(root.optString("status"))) {
                    throw new Exception(root.optString("message", "Respons server tidak valid"));
                }

                JSONArray arr = root.getJSONArray("data");
                List<ServiceItem> result = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    result.add(parseItem(arr.getJSONObject(i), i));
                }

                runOnUiThread(() -> {
                    allServices.clear();
                    allServices.addAll(result);
                    applyFilter();
                    if (result.isEmpty()) {
                        showToast("Belum ada data layanan");
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Gagal memuat layanan", e);
                runOnUiThread(() -> showToast("Gagal memuat layanan: " + e.getMessage()));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    private ServiceItem parseItem(JSONObject o, int index) {
        String id = firstNonEmpty(o, String.valueOf(index), "id_layanan", "id");
        String title = firstNonEmpty(o, "-", "nama_layanan", "judul", "nama", "title");
        String desc = firstNonEmpty(o, "", "deskripsi", "keterangan", "description");
        String badge = firstNonEmpty(o, null, "badge", "label", "estimasi");
        String kategori = normalizeCategory(firstNonEmpty(o, "", "kategori", "category"));
        String slug = firstNonEmpty(o, "", "kode_layanan", "slug", "kode");

        String iconName = getIconNameBySlug(slug, title);
        int iconRes = getResources().getIdentifier(iconName, "drawable", getPackageName());

        int iconTint;

        // Jika file ada di res/drawable, beri iconTint = 0 agar warna asli foto/PNG muncul
        if (iconRes != 0) {
            iconTint = 0;
        } else {
            iconRes = R.drawable.ic_verified;
            iconTint = R.color.primary_forest;
        }

        int iconBgRes = R.drawable.bg_icon_rounded_green;
        int badgeBgRes = R.drawable.bg_trust_strip;
        int badgeTextColor = R.color.gold_dark;

        return new ServiceItem(id, title, desc, badge, kategori, slug,
                iconRes, iconBgRes, badgeBgRes, badgeTextColor, iconTint);
    }

    private String getIconNameBySlug(String slug, String title) {
        String s = slug != null ? slug.toLowerCase() : "";
        String t = title != null ? title.toLowerCase() : "";

        if (s.contains("syarat") || t.contains("syarat")) return "syarat_nikah";
        if (s.contains("rekomendasi") || t.contains("rekomendasi")) return "rekomendasi_nikah";
        if (s.contains("isbat") || t.contains("isbat")) return "isbat_nikah";
        if (s.contains("duplikat") || t.contains("duplikat")) return "duplikat_buku_nikah";
        if (s.contains("wakaf") || t.contains("wakaf")) return "waqaf";
        if (s.contains("taukil") || t.contains("taukil")) return "taukil_nikah";
        if (s.contains("wali") || t.contains("wali")) return "wali_nikah";
        if (s.contains("pengumuman") || t.contains("pengumuman")) return "pengumuman_nikah";

        return "ic_verified";
    }

    private String firstNonEmpty(JSONObject o, String fallback, String... keys) {
        for (String k : keys) {
            if (o.has(k) && !o.isNull(k)) {
                String v = o.optString(k, "").trim();
                if (!v.isEmpty()) return v;
            }
        }
        return fallback;
    }

    private String normalizeCategory(String raw) {
        String c = raw == null ? "" : raw.toLowerCase().trim();
        if (c.contains("nikah") || c.contains("rujuk")) return ServiceItem.CATEGORY_PERNIKAHAN;
        if (c.contains("wakaf")) return ServiceItem.CATEGORY_WAKAF;
        return ServiceItem.CATEGORY_ADMINISTRASI;
    }

    private void applyFilter() {
        List<ServiceItem> filtered = new ArrayList<>();
        for (ServiceItem s : allServices) {
            boolean categoryOk = selectedCategory == null || selectedCategory.equals(s.getCategory());
            if (categoryOk && s.matches(searchQuery)) {
                filtered.add(s);
            }
        }
        adapter.submitList(filtered);
    }

    private void navigateTo(Class<?> targetActivity) {
        Intent intent = new Intent(this, targetActivity);
        startActivity(intent);
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}