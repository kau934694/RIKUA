package com.example.rikua;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
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
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * BlankoActivity — Halaman "Unduh Blanko" RIKUA KUA Kec. Randudongkal.
 *
 * Menampilkan daftar formulir resmi N1–N6 yang diambil dari API PHP
 * via HttpURLConnection pada background thread.
 */
public class BlankoActivity extends AppCompatActivity
        implements BlankoAdapter.OnDownloadClickListener {

    private static final String API_URL = "http://192.168.1.5/rikua_api/get_blanko.php";
    private static final String BASE_FILE_URL = "http:// 192.168.1.5/rikua_api/uploads/blanko/";

    private final List<BlankoItem> blankoList = new ArrayList<>();
    private BlankoAdapter adapter;
    private RecyclerView rvBlanko;
    private LinearProgressIndicator progressLoading;
    private LinearLayout layoutError;
    private TextView tvErrorMessage;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blanko);

        setupWindowAndStatusBar();
        setupToolbar();
        initViews();
        setupRecyclerView();
        setupBottomNavigation();
        setupActionButtons();
        fetchBlankoData();
    }

    // ==================== WINDOW / STATUS BAR ====================
    private void setupWindowAndStatusBar() {
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.consult_primary));
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

    // ==================== INIT VIEWS ====================
    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        rvBlanko = findViewById(R.id.rv_blanko);
        progressLoading = findViewById(R.id.progress_loading);
        layoutError = findViewById(R.id.layout_error);
        tvErrorMessage = findViewById(R.id.tv_error_message);

        NavigationView navView = findViewById(R.id.nav_drawer);
        navView.setNavigationItemSelectedListener(item -> {
            drawerLayout.closeDrawer(GravityCompat.END);
            handleDrawerNavigation(item.getItemId());
            return true;
        });

        // Retry button
        findViewById(R.id.btn_retry).setOnClickListener(v -> fetchBlankoData());
    }

    // ==================== RECYCLER ====================
    private void setupRecyclerView() {
        adapter = new BlankoAdapter(this);
        rvBlanko.setLayoutManager(new LinearLayoutManager(this));
        rvBlanko.setAdapter(adapter);
    }

    // ==================== NETWORK FETCH ====================
    /**
     * Mengambil data blanko dari endpoint PHP menggunakan HttpURLConnection
     * pada background thread, lalu update UI via runOnUiThread().
     */
    private void fetchBlankoData() {
    showLoading(true);

    new Thread(() -> {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(API_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.connect();

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new Exception("Server merespon dengan kode: " + responseCode);
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Parse JSON
            JSONObject jsonResponse = new JSONObject(response.toString());
            String status = jsonResponse.optString("status", "error");

            if (!status.equals("success")) {
                String msg = jsonResponse.optString("message", "Data tidak tersedia.");
                throw new Exception(msg);
            }

            JSONArray dataArray = jsonResponse.optJSONArray("data");
            if (dataArray == null || dataArray.length() == 0) {
                throw new Exception("Tidak ada data blanko tersedia.");
            }

            List<BlankoItem> fetched = new ArrayList<>();
            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject obj = dataArray.getJSONObject(i);

                String id = obj.optString("id", String.valueOf(i + 1));
                String kode = obj.optString("kode", "N" + (i + 1));
                String judul = obj.optString("judul", "Blanko " + kode);
                String deskripsi = obj.optString("deskripsi", "Formulir resmi KUA model " + kode);
                String fileSize = obj.optString("file_size", "-");
                String kategori = obj.optString("kategori", "Wajib");

                // Langsung ambil URL Google Drive dari JSON tanpa penggabungan BASE_FILE_URL
                String fileUrl = obj.optString("file_url", "").trim();

                fetched.add(new BlankoItem(id, kode, judul, deskripsi,
                        fileSize, kategori, fileUrl));
            }

            // Update UI di Main Thread
            runOnUiThread(() -> {
                showLoading(false);
                blankoList.clear();
                blankoList.addAll(fetched);
                adapter.submitList(blankoList);
                rvBlanko.setVisibility(View.VISIBLE);
                layoutError.setVisibility(View.GONE);
            });

        } catch (Exception e) {
            final String errorMsg = e.getMessage() != null
                    ? e.getMessage() : "Gagal terhubung ke server. Periksa koneksi internet Anda.";

            runOnUiThread(() -> {
                showLoading(false);
                rvBlanko.setVisibility(View.GONE);
                layoutError.setVisibility(View.VISIBLE);
                tvErrorMessage.setText(errorMsg);
            });
        } finally {
            if (conn != null) conn.disconnect();
        }
    }).start();
}

    // ==================== DOWNLOAD ACTION ====================
 @Override
public void onDownloadClick(BlankoItem item) {
    String urlString = item.getFileUrl();
    if (urlString == null || urlString.isEmpty()) {
        showToast("File belum tersedia untuk: " + item.getJudul());
        return;
    }

    try {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlString));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    } catch (Exception e) {
        showToast("Tidak dapat membuka link Google Drive.");
    }
}

    // ==================== BOTTOM NAVIGATION ====================
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.menu_forms);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_forms) {
                return true; // Already here
            } else if (id == R.id.menu_home) {
                navigateTo(MainActivity.class);
                return true;
            } else if (id == R.id.menu_services) {
                navigateTo(ServicesActivity.class);
                return true;
            } else if (id == R.id.menu_consultation) {
                navigateTo(ConsultationActivity.class);
                return true;
            } else if (id == R.id.menu_facilities) {
                navigateTo(FacilitiesActivity.class);
                return true;
            }
            return false;
        });
    }

    private void navigateTo(Class<?> target) {
        Intent intent = new Intent(this, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    // ==================== DRAWER NAV ====================
    private void handleDrawerNavigation(int itemId) {
        if (itemId == R.id.menu_home) {
            navigateTo(MainActivity.class);
        } else if (itemId == R.id.menu_services) {
            navigateTo(ServicesActivity.class);
        } else if (itemId == R.id.menu_consultation) {
            navigateTo(ConsultationActivity.class);
        } else if (itemId == R.id.menu_facilities) {
            navigateTo(FacilitiesActivity.class);
        } else if (itemId == R.id.menu_forms) {
            // Already on blanko
        }
    }

    // ==================== ACTION BUTTONS ====================
    private void setupActionButtons() {
        // Lanjut ke Daftar Nikah
        findViewById(R.id.btn_daftar_nikah).setOnClickListener(v ->
                startActivity(new Intent(this, DaftarNikahActivity.class)));

        // Tanya Petugas via WhatsApp
        findViewById(R.id.btn_wa_konsultasi).setOnClickListener(v -> openWhatsApp());
    }

    private void openWhatsApp() {
        try {
            String phone = "6287787080948";
            String msg = "Halo Admin RIKUA KUA Randudongkal, saya butuh bantuan mengisi blanko N...";
            String url = "https://api.whatsapp.com/send?phone=" + phone + "&text=" + Uri.encode(msg);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            showToast("Menghubungi petugas KUA Randudongkal...");
        }
    }

    // ==================== HELPERS ====================
    private void showLoading(boolean loading) {
        progressLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            layoutError.setVisibility(View.GONE);
            rvBlanko.setVisibility(View.GONE);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
