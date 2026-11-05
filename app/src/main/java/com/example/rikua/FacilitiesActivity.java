package com.example.rikua;
import com.example.rikua.FacilityItem;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

/**
 * FacilitiesActivity — Halaman "Sarana Keagamaan" RIKUA KUA Kec. Randudongkal.
 *
 * Direktori sarana keagamaan (masjid, musholla, pesantren, TPQ, madrasah diniyah)
 * dengan fitur:
 * - Live text search filtering (TextWatcher)
 * - Category chip filter (horizontal carousel, single selection)
 * - Village dropdown filter (Spinner)
 * - Navigation drawer (trigger burger icon di kanan toolbar)
 * - Bottom navigation bar
 */
public class FacilitiesActivity extends AppCompatActivity 
        implements FacilityAdapter.OnItemClickListener {
private static final String API_URL = "http://192.168.1.4/rikua_api/get_sarana.php";

    // State filter
    private String currentCategory = null; // null = "Semua"
    private String currentVillage = null;  // null = "Semua Desa"
    private String currentQuery = "";

    // Data
    private final List<FacilityItem> allFacilities = new ArrayList<>();
    private final List<FacilityItem> filteredFacilities = new ArrayList<>();

    // Views
    private EditText etSearch;
    private ImageButton btnClearSearch;
    private LinearLayout layoutEmptyState;
    private RecyclerView rvFacilities;
    private ChipGroup chipGroupFilters;
    private Spinner spinnerVillage;
    private TextView tvResultCount;
    private FacilityAdapter adapter;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_facilities);

        setupWindowAndStatusBar();
        setupToolbar();
        initViews();
        loadFacilityData();
        setupRecyclerView();
        setupSearchFilter();
        setupCategoryChips();
        setupVillageSpinner();
        setupBottomNavigation();
        setupHelpButton();

        applyFilters();
    }

    // ==================== WINDOW / STATUS BAR ====================
    private void setupWindowAndStatusBar() {
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.primary_forest));
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(true);

        View bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (view, insets) -> {
                androidx.core.graphics.Insets bars =
                        insets.getInsets(WindowInsetsCompat.Type.systemBars());
                view.setPadding(view.getPaddingLeft(), view.getPaddingTop(),
                        view.getPaddingRight(), bars.bottom);
                return insets;
            });
        }
    }

    // ==================== TOOLBAR ====================
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
            } else {
                finish();
            }
        });
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

    // ==================== INIT VIEWS ====================
    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        etSearch = findViewById(R.id.et_search);
        btnClearSearch = findViewById(R.id.btn_clear_search);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        rvFacilities = findViewById(R.id.rv_facilities);
        chipGroupFilters = findViewById(R.id.chip_group_filters);
        spinnerVillage = findViewById(R.id.spinner_village);
        tvResultCount = findViewById(R.id.tv_result_count);

        NavigationView navView = findViewById(R.id.nav_drawer);
        navView.setNavigationItemSelectedListener(item -> {
            drawerLayout.closeDrawer(GravityCompat.END);
            handleNavigation(item.getItemId());
            return true;
        });
    }

    // ==================== DATA ====================
  // ==================== DATA ====================
    private void loadFacilityData() {
        new Thread(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parsing JSON dari respons PHP
                JSONObject jsonResponse = new JSONObject(response.toString());
                String status = jsonResponse.getString("status");

                if (status.equals("success")) {
                    JSONArray jsonArray = jsonResponse.getJSONArray("data");
                    List<FacilityItem> fetchedList = new ArrayList<>();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);

                        // Ambil data aman menggunakan optString/optInt agar tidak crash
                        String id = String.valueOf(obj.optInt("id", 0));
                        String namaFileGambar = obj.optString("gambar", "");
                        
                        // PERBAIKAN 1: Sesuaikan IP gambar dengan IP server laptop (192.168.1.4)
                        String imageUrl = "";
                        if (!namaFileGambar.isEmpty()) {
                            imageUrl = "http://192.168.1.4/rikua_api/uploads/" + namaFileGambar;
                        }

                        String namaSarana = obj.optString("nama_sarana", "-");
                        String jenis = obj.optString("jenis", "-");
                        String desa = obj.optString("desa", "-");
                        int jumlahJamaah = obj.optInt("jumlah_jamaah", 0);
                        String keterangan = obj.optString("keterangan", obj.optString("alamat", "-"));

                        // PERBAIKAN 2: Ambil koordinat latitude & longitude untuk fitur peta
                        double latitude = obj.optDouble("latitude", 0.0);
                        double longitude = obj.optDouble("longitude", 0.0);

                        String detail1 = "Jumlah Jamaah: " + jumlahJamaah + " orang";
                        String detail2 = "Keterangan: " + keterangan;

                        // Buat objek FacilityItem
                        FacilityItem item = new FacilityItem(
                                id,
                                namaSarana,
                                jenis,
                                (namaSarana + " " + desa).toLowerCase(),
                                keterangan, 
                                desa,
                                detail1,
                                detail2,
                                imageUrl,
                                R.color.consult_primary,
                                R.drawable.bg_badge_emerald,
                                R.color.badge_emerald_text,
                                R.drawable.ic_mosque,
                                R.drawable.bg_icon_rounded_green,
                                R.color.primary_forest
                        );

                        // PERBAIKAN 3: Masukkan koordinat latitude & longitude ke model
                        item.setLatitude(latitude);
                        item.setLongitude(longitude);

                        fetchedList.add(item);
                    }

                    // Perbarui data dan UI di Main Thread
                    runOnUiThread(() -> {
                        allFacilities.clear();
                        allFacilities.addAll(fetchedList);
                        applyFilters(); 
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> showToast("Gagal memuat data sarana dari database"));
            }
        }).start();
    }
    // ==================== RECYCLER ====================
    private void setupRecyclerView() {
        adapter = new FacilityAdapter(this);
        rvFacilities.setLayoutManager(new LinearLayoutManager(this));
        rvFacilities.setAdapter(adapter);
        rvFacilities.setNestedScrollingEnabled(false);
    }

    // ==================== SEARCH ====================
    private void setupSearchFilter() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString().trim();
                btnClearSearch.setVisibility(
                        currentQuery.isEmpty() ? View.GONE : View.VISIBLE);
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            etSearch.requestFocus();
        });
    }

    // ==================== CATEGORY CHIPS ====================
    private void setupCategoryChips() {
        chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                currentCategory = null;
            } else {
                int id = checkedIds.get(0);
                if (id == R.id.chip_masjid) {
                    currentCategory = FacilityItem.CAT_MASJID;
                } else if (id == R.id.chip_musholla) {
                    currentCategory = FacilityItem.CAT_MUSHOLLA;
                } else if (id == R.id.chip_pesantren) {
                    currentCategory = FacilityItem.CAT_PESANTREN;
                } else if (id == R.id.chip_tpq) {
                    currentCategory = FacilityItem.CAT_TPQ;
                } else if (id == R.id.chip_madin) {
                    currentCategory = FacilityItem.CAT_MADIN;
                } else {
                    currentCategory = null; // chip_all
                }
            }
            applyFilters();
        });
    }

    // ==================== VILLAGE SPINNER ====================
    private void setupVillageSpinner() {
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                this, R.array.facilities_villages, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVillage.setAdapter(spinnerAdapter);

        spinnerVillage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Position 0 = "Semua Desa" -> no village filter
                currentVillage = (position == 0) ? null : parent.getItemAtPosition(position).toString();
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // ==================== FILTER LOGIC ====================
    /**
     * Menerapkan filter gabungan (kategori + desa + query teks) pada dataset.
     */
    private void applyFilters() {
        filteredFacilities.clear();
        for (FacilityItem item : allFacilities) {
            if (!item.matchesCategory(currentCategory)) continue;
            if (!item.matchesVillage(currentVillage)) continue;
            if (!item.matchesQuery(currentQuery)) continue;
            filteredFacilities.add(item);
        }

        adapter.submitList(filteredFacilities);
        tvResultCount.setText(getString(R.string.facilities_result_count, filteredFacilities.size()));

        boolean isEmpty = filteredFacilities.isEmpty();
        layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvFacilities.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    // ==================== BOTTOM NAVIGATION ====================
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.menu_facilities);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_facilities) {
                return true; // Already here
            }
            handleNavigation(id);
            return true;
        });
    }

    /** Routing bersama untuk bottom navigation maupun drawer item. */
    private void handleNavigation(int itemId) {
        if (itemId == R.id.menu_home) {
            navigateTo(MainActivity.class);
        } else if (itemId == R.id.menu_services) {
            navigateTo(ServicesActivity.class);
        } else if (itemId == R.id.menu_consultation) {
            navigateTo(ConsultationActivity.class);
        } else if (itemId == R.id.menu_facilities) {
            // current screen
       } else if (itemId == R.id.menu_forms) {
    Intent intent = new Intent(this, BlankoActivity.class);
    startActivity(intent);
}
        else {
            showToast("Menu: " + itemId);
        }
    }

    private void navigateTo(Class<?> targetActivity) {
        Intent intent = new Intent(this, targetActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    // ==================== HELP / CTA BUTTON ====================
    private void setupHelpButton() {
        findViewById(R.id.btn_konsultasi_pendataan).setOnClickListener(v -> openWhatsAppPendataan());
    }

    private void openWhatsAppPendataan() {
        try {
            String phone = "6281234567890";
            String msg = "Halo Admin RIKUA KUA Randudongkal, saya ingin konsultasi pendataan / pembaruan sarana keagamaan...";
            String url = "https://wa.me/" + phone + "?text=" + Uri.encode(msg);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            showToast("Menghubungi petugas KUA Randudongkal...");
        }
    }

    // ==================== FACILITY ITEM CLICK ====================
    @Override
    public void onFacilityClick(FacilityItem item) {
        // Bawa objek FacilityItem (Serializable) ke halaman detail.
        Intent intent = new Intent(this, FacilityDetailActivity.class);
        intent.putExtra(FacilityDetailActivity.EXTRA_FACILITY, item);
        // Extra cadangan berbasis String (bila tak ingin mengirim objek utuh).
        intent.putExtra(FacilityDetailActivity.EXTRA_FACILITY_ID, item.getId());
        intent.putExtra("extra_name", item.getName());
        intent.putExtra("extra_category", item.getCategoryLabel());
        intent.putExtra("extra_location", item.getLocation());
        intent.putExtra("extra_image", item.getImageUrl());
        startActivity(intent);
    }

    // ==================== UTILITIES ====================
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
