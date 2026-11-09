package com.example.rikua;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;    
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ConsultationActivity extends AppCompatActivity
        implements OfficerAdapter.OnActionListener {

    private static final int CURRENT_TAB_INDEX = 2; // Konsultasi = 2

    private DrawerLayout drawerLayout;
    private OfficerAdapter adapter;
    private RecyclerView rvOfficers;

    private static final String WA_BASE = "https://wa.me/";
    private static final String API_URL = "http://192.168.1.5/rikua_api/get_petugas.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consultation);

        setupWindowAndStatusBar();
        setupToolbar();
        initViews();
        setupRecyclerView();
        setupBottomNavigation();

        fetchOfficersFromDatabase();
    }

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

        NavigationView navView = findViewById(R.id.nav_drawer);
        navView.setNavigationItemSelectedListener(item -> {
            drawerLayout.closeDrawer(GravityCompat.END);
            handleNavigation(item.getItemId());
            return true;
        });
    }

    private void setupRecyclerView() {
        rvOfficers = findViewById(R.id.rv_officers);
        adapter = new OfficerAdapter(this);
        rvOfficers.setLayoutManager(new LinearLayoutManager(this));
        rvOfficers.setAdapter(adapter);
        rvOfficers.setNestedScrollingEnabled(false);
    }

    private void fetchOfficersFromDatabase() {
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
                    List<OfficerItem> officers = new ArrayList<>();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);

                        String nama = obj.getString("nama");
                        String jabatan = obj.getString("jabatan");
                        String jamLayanan = obj.getString("jam_layanan");
                        String bidangKeahlianStr = obj.getString("bidang_keahlian");
                        String nomorWa = obj.getString("nomor_wa");
                        String statusAktif = obj.getString("status");

                        boolean isAvailable = statusAktif.equalsIgnoreCase("Tersedia");
                        String[] expertiseArray = bidangKeahlianStr.split(", ");

                        int avatarBg = R.drawable.bg_avatar_teal;
                        if (i == 1) avatarBg = R.drawable.bg_avatar_gold;
                        else if (i == 2) avatarBg = R.drawable.bg_avatar_purple;
                        else if (i >= 3) avatarBg = R.drawable.bg_avatar_gray;

                        String initials = getInitials(nama);

                        officers.add(new OfficerItem(
                                "id_" + i,
                                nama,
                                jabatan,
                                jamLayanan,
                                expertiseArray,
                                nomorWa,
                                isAvailable,
                                avatarBg,
                                initials
                        ));
                    }

                    runOnUiThread(() -> adapter.submitList(officers));
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("DB_ERROR", "Gagal memuat data petugas: " + e.getMessage());
                runOnUiThread(() -> showToast("Gagal terhubung ke database server"));
            }
        }).start();
    }

    private String getInitials(String name) {
        String[] parts = name.replace(".", "").trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        } else if (parts.length == 1 && parts[0].length() >= 2) {
            return parts[0].substring(0, 2).toUpperCase();
        }
        return "KU";
    }

    @Override
    public void onWhatsAppClick(OfficerItem officer) {
        String topic = officer.getExpertise().length > 0 ? officer.getExpertise()[0] : "layanan KUA";
        String message = getString(R.string.consultation_wa_message, officer.getName(), topic);

        String phoneClean = officer.getPhoneDigits().replaceAll("[^0-9]", "");
        String url = WA_BASE + phoneClean + "?text=" + Uri.encode(message);

        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            showToast(getString(R.string.consultation_wa_unavailable));
        }
    }

    @Override
    public void onPhoneClick(OfficerItem officer) {
        String phoneClean = officer.getPhoneDigits().replaceAll("[^0-9]", "");
        Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneClean));
        try {
            startActivity(dial);
        } catch (ActivityNotFoundException e) {
            showToast(getString(R.string.consultation_dial_unavailable));
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.menu_consultation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_consultation) {
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
            // current screen
        } else if (itemId == R.id.menu_facilities) {
            navigateTo(FacilitiesActivity.class, 3);
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
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}