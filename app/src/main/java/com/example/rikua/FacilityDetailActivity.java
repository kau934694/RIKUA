package com.example.rikua;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * FacilityDetailActivity — Halaman "Detail Sarana Keagamaan" RIKUA KUA Kec. Randudongkal.
 *
 * Menerima {@link FacilityItem} (Serializable) dari {@link FacilitiesActivity} saat kartu
 * fasilitas diklik, lalu menampilkan informasi lengkap sarana: gambar header (Glide),
 * kategori, SIMAS Kemenag, status, alamat, kapasitas, legalitas tanah, luas bangunan,
 * tahun berdiri, pengurus, kontak, dan arah kiblat.
 *
 * Aksi fungsional:
 * - Tombol "Lihat di Peta"  -> Intent VIEW ke Google Maps (koordinat / alamat).
 * - Tombol "Konsultasi"      -> Intent VIEW ke WhatsApp admin KUA Randudongkal.
 * - Tombol back toolbar      -> finish().
 */
public class FacilityDetailActivity extends AppCompatActivity {

    /** Kunci extra: objek FacilityItem (Serializable). */
    public static final String EXTRA_FACILITY = "extra_facility";
    /** Kunci cadangan: id fasilitas (String) bila objek tidak tersedia. */
    public static final String EXTRA_FACILITY_ID = "extra_facility_id";

    // Basis endpoint detail (Laragon). Mengikuti host API direktori sarana.
    private static final String DETAIL_API_URL =
            "http://192.168.1.5/rikua_api/get_sarana_detail.php";
    // Nomor WhatsApp admin KUA Randudongkal (konsultasi kemasjidan & wakaf).
    private static final String ADMIN_WHATSAPP = "6281234567890";

    private static final String PLACEHOLDER = "—";

    // Data
    private FacilityItem facility;
    private String latitude;
    private String longitude;

    // Views
    private ImageView ivHeader;
    private TextView tvCategoryBadge;
    private TextView tvName;
    private TextView tvSimas;
    private TextView tvLocationSummary;
    private TextView tvValCategory;
    private TextView tvValAddress;
    private TextView tvValYear;
    private TextView tvValCapacity;
    private TextView tvValBuilding;
    private TextView tvValLand;
    private TextView tvValQibla;
    private TextView tvValTakmir;
    private TextView tvValPhone;
    private TextView tvValEmail;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_facility_detail);

        setupWindowAndStatusBar();
        initViews();
        setupToolbar();
        readIntentData();
        bindBasicData();
        setupActionButtons();
        setupBottomNavigation();

        // Kaya data: coba ambil field detail tambahan dari server.
        loadExtendedDetail();
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

    // ==================== INIT VIEWS ====================
    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        ivHeader = findViewById(R.id.iv_header_image);
        tvCategoryBadge = findViewById(R.id.tv_category_badge);
        tvName = findViewById(R.id.tv_facility_name);
        tvSimas = findViewById(R.id.tv_simas);
        tvLocationSummary = findViewById(R.id.tv_location_summary);

        tvValCategory = findViewById(R.id.tv_val_category);
        tvValAddress = findViewById(R.id.tv_val_address);
        tvValYear = findViewById(R.id.tv_val_year);
        tvValCapacity = findViewById(R.id.tv_val_capacity);
        tvValBuilding = findViewById(R.id.tv_val_building);
        tvValLand = findViewById(R.id.tv_val_land);
        tvValQibla = findViewById(R.id.tv_val_qibla);
        tvValTakmir = findViewById(R.id.tv_val_takmir);
        tvValPhone = findViewById(R.id.tv_val_phone);
        tvValEmail = findViewById(R.id.tv_val_email);
    }

    // ==================== TOOLBAR ====================
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        // Tombol kembali menutup activity.
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    // ==================== READ INTENT ====================
    private void readIntentData() {
        Object obj = getIntent().getSerializableExtra(EXTRA_FACILITY);
        if (obj instanceof FacilityItem) {
            facility = (FacilityItem) obj;
        } else {
            // Fallback: bangun objek minimal dari id bila objek utuh tak terkirim.
            String id = getIntent().getStringExtra(EXTRA_FACILITY_ID);
            String name = getIntent().getStringExtra("extra_name");
            facility = new FacilityItem(
                    id != null ? id : "",
                    name != null ? name : "Sarana Keagamaan",
                    getIntent().getStringExtra("extra_category"),
                    "",
                    getIntent().getStringExtra("extra_location"),
                    "",
                    null, null,
                    getIntent().getStringExtra("extra_image"),
                    R.color.consult_primary,
                    R.drawable.bg_badge_emerald,
                    R.color.badge_emerald_text,
                    R.drawable.ic_mosque,
                    R.drawable.bg_icon_rounded_green,
                    R.color.primary_forest);
        }
    }

    // ==================== BIND BASIC DATA ====================
    private void bindBasicData() {
        if (facility == null) return;

        tvName.setText(facility.getName());
        tvCategoryBadge.setText(facility.getCategoryLabel());
        tvValCategory.setText(facility.getCategoryLabel());

        String location = notEmpty(facility.getLocation()) ? facility.getLocation() : PLACEHOLDER;
        tvLocationSummary.setText(location);
        tvValAddress.setText(location);

        // Kapasitas & keterangan dari list (detail1/detail2) bila field extended belum tiba.
        if (notEmpty(facility.getDetail1())) {
            tvValCapacity.setText(stripPrefix(facility.getDetail1(), "Jumlah Jamaah:"));
        } else {
            tvValCapacity.setText(PLACEHOLDER);
        }

        // Gambar header dinamis via Glide.
        loadHeaderImage(facility.getImageUrl());
    }

    private void loadHeaderImage(String imageUrl) {
        if (notEmpty(imageUrl)) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_mosque)
                    .error(R.drawable.ic_mosque)
                    .centerCrop()
                    .into(ivHeader);
        } else {
            ivHeader.setImageResource(R.drawable.ic_mosque);
            ivHeader.setColorFilter(ContextCompat.getColor(this, R.color.primary_forest));
        }
    }

    // ==================== EXTENDED DETAIL (SERVER) ====================
    /**
     * Mengambil field detail tambahan (SIMAS, legalitas, luas, tahun, pengurus, kontak,
     * arah kiblat, koordinat) dari server. Setiap field opsional — yang kosong memakai
     * placeholder agar halaman tetap informatif dan tidak crash.
     */
    private void loadExtendedDetail() {
        if (facility == null || !notEmpty(facility.getId())) return;

        new Thread(() -> {
            try {
                URL url = new URL(DETAIL_API_URL + "?id=" + Uri.encode(facility.getId()));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);

                int code = conn.getResponseCode();
                if (code != HttpURLConnection.HTTP_OK) return;

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject resp = new JSONObject(sb.toString());
                if (!"success".equalsIgnoreCase(resp.optString("status"))) return;

                final JSONObject d = resp.optJSONObject("data");
                if (d == null) return;

                runOnUiThread(() -> applyExtendedDetail(d));
            } catch (Exception e) {
                // Offline / endpoint belum tersedia: biarkan placeholder dari data list.
                e.printStackTrace();
            }
        }).start();
    }

    private void applyExtendedDetail(JSONObject d) {
        // SIMAS & status
        String simas = d.optString("nomor_simas", d.optString("simas", ""));
        if (notEmpty(simas)) {
            tvSimas.setText(getString(R.string.facility_detail_simas_prefix, simas));
        } else {
            tvSimas.setText(R.string.facility_detail_simas_unknown);
        }

        // Alamat lengkap (prioritas field detail, fallback ke lokasi list)
        setTextIfPresent(tvValAddress, firstNonEmpty(
                d.optString("alamat_lengkap", d.optString("alamat", "")),
                facility.getLocation()));
        setTextIfPresent(tvLocationSummary, firstNonEmpty(
                d.optString("alamat_lengkap", d.optString("alamat", "")),
                facility.getLocation()));

        // Kapasitas jamaah
        setTextIfPresent(tvValCapacity, firstNonEmpty(
                d.optString("kapasitas_jamaah", d.optString("jumlah_jamaah", "")),
                stripPrefix(facility.getDetail1(), "Jumlah Jamaah:")));

        // Legalitas tanah
        setTextIfPresent(tvValLand, d.optString("legalitas_tanah", ""));
        // Luas bangunan
        setTextIfPresent(tvValBuilding, d.optString("luas_bangunan", ""));
        // Tahun berdiri
        setTextIfPresent(tvValYear, d.optString("tahun_berdiri", ""));
        // Ketua takmir / pengurus
        setTextIfPresent(tvValTakmir, firstNonEmpty(
                d.optString("ketua_takmir", d.optString("pengurus", "")),
                stripPrefix(facility.getDetail2(), "Ketua Takmir:")));
        // Kontak
        setTextIfPresent(tvValPhone, d.optString("telepon", d.optString("kontak", "")));
        setTextIfPresent(tvValEmail, d.optString("email", ""));
        // Arah kiblat
        setTextIfPresent(tvValQibla, firstNonEmpty(
                d.optString("arah_kiblat", ""),
                d.optString("kiblat", "")));

        // Kategori (perbarui bila server mengirim label lebih lengkap)
        String category = d.optString("jenis", "");
        if (notEmpty(category)) {
            tvCategoryBadge.setText(category);
            tvValCategory.setText(category);
        }

        // Koordinat untuk Google Maps
        latitude = d.optString("latitude", d.optString("lat", ""));
        longitude = d.optString("longitude", d.optString("lng", ""));
    }

    // ==================== ACTION BUTTONS ====================
    private void setupActionButtons() {
        findViewById(R.id.btn_open_maps).setOnClickListener(v -> openInGoogleMaps());
        findViewById(R.id.btn_consult_wakaf).setOnClickListener(v -> openWhatsAppConsultation());
    }

    /** Membuka Google Maps memakai koordinat bila ada, selain itu berbasis query alamat. */
    private void openInGoogleMaps() {
        try {
            String nameQuery = (facility != null)
                    ? facility.getName() + " " + facility.getLocation()
                    : "KUA Randudongkal";

            Uri gmmUri;
            if (notEmpty(latitude) && notEmpty(longitude)) {
                // Koordinat: zoom ke titik lokasi sarana.
                gmmUri = Uri.parse("geo:" + latitude + "," + longitude
                        + "?q=" + latitude + "," + longitude
                        + "(" + Uri.encode(nameQuery) + ")");
            } else {
                gmmUri = Uri.parse("geo:0,0?q=" + Uri.encode(nameQuery));
            }

            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmUri);
            mapIntent.setPackage("com.google.android.apps.maps");

            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                // Fallback ke browser bila aplikasi Maps tak terpasang.
                String webUri = notEmpty(latitude) && notEmpty(longitude)
                        ? "https://maps.google.com/?q=" + latitude + "," + longitude
                        : "https://maps.google.com/?q=" + Uri.encode(nameQuery);
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webUri)));
            }
        } catch (Exception e) {
            showToast(getString(R.string.facility_detail_maps_unavailable));
        }
    }

    /** Membuka WhatsApp admin KUA Randudongkal dengan pesan otomatis. */
    private void openWhatsAppConsultation() {
        try {
            String name = (facility != null) ? facility.getName() : "";
            String message = getString(R.string.facility_detail_wa_message, name);
            String url = "https://wa.me/" + ADMIN_WHATSAPP + "?text=" + Uri.encode(message);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            showToast(getString(R.string.facility_detail_whatsapp_unavailable));
        }
    }

    // ==================== BOTTOM NAVIGATION ====================
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.menu_facilities);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_facilities) {
                finish(); // kembali ke direktori sarana
                return true;
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
            showToast("Blangko & Formulir — Coming Soon");
        }
    }

    private void navigateTo(Class<?> targetActivity) {
        Intent intent = new Intent(this, targetActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    // ==================== UTILITIES ====================
    private static boolean notEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

    /** Isi TextView hanya bila nilai tersedia; kalau tidak, paksa placeholder. */
    private void setTextIfPresent(TextView tv, String value) {
        tv.setText(notEmpty(value) ? value : PLACEHOLDER);
    }

    private static String firstNonEmpty(String a, String b) {
        return notEmpty(a) ? a : (notEmpty(b) ? b : "");
    }

    /** Buang prefix label (mis. "Jumlah Jamaah:") dari string detail list. */
    private static String stripPrefix(String src, String prefix) {
        if (!notEmpty(src)) return "";
        if (src.toLowerCase().startsWith(prefix.toLowerCase())) {
            return src.substring(prefix.length()).replace(":", "").trim();
        }
        return src;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
