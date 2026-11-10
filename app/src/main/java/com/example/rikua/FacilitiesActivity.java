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
import androidx.activity.OnBackPressedCallback;

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

public class FacilitiesActivity extends AppCompatActivity 
        implements FacilityAdapter.OnItemClickListener {

    private static final int CURRENT_TAB_INDEX = 3; // Sarana = 3

    private static final String API_URL = "http://192.168.1.5/rikua_api/get_sarana.php";

    private String currentCategory = null; 
    private String currentVillage = null;  
    private String currentQuery = "";

    private final List<FacilityItem> allFacilities = new ArrayList<>();
    private final List<FacilityItem> filteredFacilities = new ArrayList<>();

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

        // Callback tombol kembali ke MainActivity
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(FacilitiesActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            }
        });

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

    private void setupToolbar() {
        View btnMenu = findViewById(R.id.btn_menu_drawer);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> toggleDrawer());
        }
    }

    private void toggleDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            drawerLayout.openDrawer(GravityCompat.END);
        }
    }

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

                JSONObject jsonResponse = new JSONObject(response.toString());
                String status = jsonResponse.getString("status");

                if (status.equals("success")) {
                    JSONArray jsonArray = jsonResponse.getJSONArray("data");
                    List<FacilityItem> fetchedList = new ArrayList<>();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);

                        String id = String.valueOf(obj.optInt("id", 0));
                        String namaFileGambar = obj.optString("gambar", "");
                        
                        String imageUrl = "";
                        if (!namaFileGambar.isEmpty()) {
                            imageUrl = "http://192.168.1.5/rikua_api/uploads/" + namaFileGambar;
                        }

                        String namaSarana = obj.optString("nama_sarana", "-");
                        String jenis = obj.optString("jenis", "-");
                        String desa = obj.optString("desa", "-");
                        int jumlahJamaah = obj.optInt("jumlah_jamaah", 0);
                        String keterangan = obj.optString("keterangan", obj.optString("alamat", "-"));

                        double latitude = obj.optDouble("latitude", 0.0);
                        double longitude = obj.optDouble("longitude", 0.0);

                        String detail1 = "Jumlah Jamaah: " + jumlahJamaah + " orang";
                        String detail2 = "Keterangan: " + keterangan;

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

                        item.setLatitude(latitude);
                        item.setLongitude(longitude);

                        fetchedList.add(item);
                    }

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

    private void setupRecyclerView() {
        adapter = new FacilityAdapter(this);
        rvFacilities.setLayoutManager(new LinearLayoutManager(this));
        rvFacilities.setAdapter(adapter);
        rvFacilities.setNestedScrollingEnabled(false);
    }

    private void setupSearchFilter() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString().trim();
                btnClearSearch.setVisibility(currentQuery.isEmpty() ? View.GONE : View.VISIBLE);
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
                    currentCategory = null;
                }
            }
            applyFilters();
        });
    }

    private void setupVillageSpinner() {
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                this, R.array.facilities_villages, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVillage.setAdapter(spinnerAdapter);

        spinnerVillage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentVillage = (position == 0) ? null : parent.getItemAtPosition(position).toString();
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

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

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.menu_facilities);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_facilities) {
                return true;
            }
            handleNavigation(id);
            return true;
        });
    }

    private void handleNavigation(int itemId) {
        if (itemId == R.id.menu_home) {
            navigateTo(MainActivity.class, 0);
        } else if (itemId == R.id.menu_services) {
            navigateTo(ServicesActivity.class, 1);
        } else if (itemId == R.id.menu_consultation) {
            navigateTo(ConsultationActivity.class, 2);
        } else if (itemId == R.id.menu_facilities) {
            // current screen
        } else if (itemId == R.id.menu_forms) {
            navigateTo(BlankoActivity.class, 4);
        } else {
            showToast("Menu: " + itemId);
        }
    }

    private void navigateTo(Class<?> targetActivity, int targetIndex) {
        if (targetIndex == CURRENT_TAB_INDEX) return;
        Intent intent = new Intent(this, targetActivity);
        startActivity(intent);
        if (targetIndex > CURRENT_TAB_INDEX) {
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } else {
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
        finish();
    }

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

    @Override
    public void onFacilityClick(FacilityItem item) {
        Intent intent = new Intent(this, FacilityDetailActivity.class);
        intent.putExtra(FacilityDetailActivity.EXTRA_FACILITY, item);
        intent.putExtra(FacilityDetailActivity.EXTRA_FACILITY_ID, item.getId());
        intent.putExtra("extra_name", item.getName());
        intent.putExtra("extra_category", item.getCategoryLabel());
        intent.putExtra("extra_location", item.getLocation());
        intent.putExtra("extra_image", item.getImageUrl());
        startActivity(intent);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}