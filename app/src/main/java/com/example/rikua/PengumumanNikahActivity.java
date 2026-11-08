package com.example.rikua;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.rikua.databinding.ActivityPengumumanNikahBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * PengumumanNikahActivity — Halaman "Pengumuman Kehendak Nikah" RIKUA.
 *
 * Papan pengumuman read-only. Datanya diambil dari server (get_pengumuman.php):
 * nama calon pengantin, waktu & tempat akad, dan kode tiket. Pencarian dan chip
 * filter bekerja di sisi aplikasi pada data yang sudah dimuat.
 */
public class PengumumanNikahActivity extends AppCompatActivity {

    /** Extra utama: objek layanan (Serializable) — konvensi intent project. */
    public static final String EXTRA_LAYANAN = "extra_layanan";
    /** Extra fallback: slug kode_layanan dari daftar layanan. */
    public static final String EXTRA_KODE_LAYANAN = "extra_kode_layanan";
    /** Extra fallback: judul layanan untuk teks sementara di toolbar. */
    public static final String EXTRA_JUDUL_LAYANAN = "extra_judul_layanan";

    /** Slug layanan yang diarahkan ke halaman ini dari daftar Layanan. */
    public static final String KODE_LAYANAN = "pengumuman-kehendak-nikah";

    /** Admin KUA untuk sanggahan/konsultasi (format 62…). */
    private static final String ADMIN_WHATSAPP = "6287787080948";

    /** Endpoint daftar pengumuman. */
    private static final String API_URL = "http://192.168.1.5/rikua_api/get_pengumuman.php";

    /**
     * Saat true, pendaftaran berstatus 'draf' ikut ditampilkan (untuk uji coba).
     * Ubah ke false untuk rilis: hanya yang sudah dikirim dan akadnya belum lewat.
     */
    private static final boolean TAMPILKAN_DRAF_UNTUK_UJI = true;

    /** Mode chip filter jadwal. */
    private static final int FILTER_ALL = 0;
    private static final int FILTER_BULAN_INI = 1;
    private static final int FILTER_DESA_WARUNGPRING = 2;
    private static final int FILTER_DESA_CIBUYUR = 3;

    private static final String DESA_WARUNGPRING = "Desa Warungpring";
    private static final String DESA_CIBUYUR = "Desa Cibuyur";

    private ActivityPengumumanNikahBinding binding;
    private PengumumanAdapter adapter;

    /** Seluruh pengumuman hasil muat dari server. */
    private final List<PengumumanItem> semuaJadwal = new ArrayList<>();
    private final List<PengumumanItem> jadwalTersaring = new ArrayList<>();

    private String kueriCari = "";
    private int modeFilter = FILTER_ALL;
    private ObjectAnimator pulseAnimator;

    // =======================================================================
    // Lifecycle
    // =======================================================================

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPengumumanNikahBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupWindowAndStatusBar();
        setupToolbar();
        setupDrawer();
        setupJadwalList();
        setupSearch();
        setupFilterChips();
        setupActionButtons();
        setupBottomNavigation();

        // Judul toolbar ikut menyesuaikan layanan yang diklik user bila dikirim.
        bacaExtraLayanan();

        applyFilters();
        muatPengumuman();
    }

    @Override
    protected void onDestroy() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel(); // hentikan animasi live-status sebelum view dilepas
        }
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

    /** Titik hijau "live status" berkedip halus (alpha 1.0 ↔ 0.25, terus-menerus). */

    // =======================================================================
    // Daftar jadwal + muat data server + pencarian/filter
    // =======================================================================

    /** Siapkan RecyclerView; isinya diisi setelah data server selesai dimuat. */
    private void setupJadwalList() {
        adapter = new PengumumanAdapter();
        binding.rvPengumuman.setLayoutManager(new LinearLayoutManager(this));
        binding.rvPengumuman.setAdapter(adapter);
    }

    /** Ambil daftar pengumuman dari server di thread latar. */
    private void muatPengumuman() {
        new Thread(() -> {
            HttpURLConnection conn = null;
            List<PengumumanItem> hasil = new ArrayList<>();
            String pesanError = null;

            try {
                String alamat = API_URL + (TAMPILKAN_DRAF_UNTUK_UJI ? "?semua=1" : "");
                conn = (HttpURLConnection) new URL(alamat).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                int kode = conn.getResponseCode();
                InputStream is = (kode >= 200 && kode < 300)
                        ? conn.getInputStream() : conn.getErrorStream();
                String body = bacaStream(is);

                if (kode != HttpURLConnection.HTTP_OK) {
                    pesanError = "Server membalas kode " + kode;
                    android.util.Log.e("Pengumuman", "HTTP " + kode + " : " + body);
                } else {
                    JSONObject root = new JSONObject(body);
                    if (!"success".equals(root.optString("status"))) {
                        pesanError = root.optString("message", "Gagal memuat pengumuman");
                    } else {
                        JSONArray arr = root.optJSONArray("data");
                        if (arr != null) {
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject o = arr.getJSONObject(i);
                                boolean terverifikasi =
                                        "terverifikasi".equals(o.optString("status_tampil"));
                                hasil.add(new PengumumanItem(
                                        o.optString("nama", "-"),
                                        o.optString("jadwal", "-"),
                                        o.optString("lokasi", "-"),
                                        o.optString("desa", ""),
                                        "Kode tiket: " + o.optString("kode_tiket", "-"),
                                        terverifikasi
                                                ? PengumumanItem.STATUS_TERVERIFIKASI
                                                : PengumumanItem.STATUS_PUBLIKASI,
                                        o.optBoolean("bulan_ini", false)));
                            }
                        }
                    }
                }
            } catch (java.net.SocketTimeoutException e) {
                pesanError = "Koneksi timeout. Periksa jaringan/IP server.";
            } catch (java.io.IOException e) {
                pesanError = "Tidak bisa terhubung ke server. Periksa jaringan/IP laptop.";
                android.util.Log.e("Pengumuman", "IO error", e);
            } catch (Exception e) {
                pesanError = "Data pengumuman tidak bisa dibaca.";
                android.util.Log.e("Pengumuman", "Parse error", e);
            } finally {
                if (conn != null) conn.disconnect();
            }

            final String fPesan = pesanError;
            runOnUiThread(() -> {
                if (isFinishing() || binding == null) return;
                semuaJadwal.clear();
                semuaJadwal.addAll(hasil);
                applyFilters();
                if (fPesan != null) {
                    Toast.makeText(this, fPesan, Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private String bacaStream(InputStream is) throws java.io.IOException {
        if (is == null) return "";
        try (BufferedReader r = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    /** Pencarian real-time: nama calon pengantin atau desa. */
    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                kueriCari = s.toString().trim();
                binding.btnClearSearch.setVisibility(
                        kueriCari.isEmpty() ? View.GONE : View.VISIBLE);
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.btnClearSearch.setOnClickListener(v -> binding.etSearch.setText(""));
    }

    /** Chip filter tunggal: Semua Jadwal | Bulan Ini | Desa Warungpring | Cibuyur. */
    private void setupFilterChips() {
        binding.chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                modeFilter = FILTER_ALL; // user mengetuk chip aktif -> tidak ada filter
                group.check(R.id.chip_all);
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.chip_month) {
                modeFilter = FILTER_BULAN_INI;
            } else if (id == R.id.chip_randudongkal) {
                modeFilter = FILTER_DESA_WARUNGPRING;
            } else if (id == R.id.chip_cibuyur) {
                modeFilter = FILTER_DESA_CIBUYUR;
            } else {
                modeFilter = FILTER_ALL;
            }
            applyFilters();
        });
    }

    /**
     * Filter gabungan (chip mode + teks pencarian) pada data yang sudah dimuat,
     * lalu perbarui adapter dan tampilan empty-state bila hasil nihil.
     */
    private void applyFilters() {
        jadwalTersaring.clear();
        for (PengumumanItem item : semuaJadwal) {
            if (!item.matchesQuery(kueriCari)) continue;
            switch (modeFilter) {
                case FILTER_BULAN_INI:
                    if (!item.isThisMonth()) continue;
                    break;
                case FILTER_DESA_WARUNGPRING:
                    if (!item.matchesVillage(DESA_WARUNGPRING)) continue;
                    break;
                case FILTER_DESA_CIBUYUR:
                    if (!item.matchesVillage(DESA_CIBUYUR)) continue;
                    break;
                default:
                    break; // FILTER_ALL — tanpa penyaringan tambahan
            }
            jadwalTersaring.add(item);
        }
        adapter.submitList(jadwalTersaring);
        binding.layoutEmptyState.setVisibility(
                jadwalTersaring.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // =======================================================================
    // Sticky action buttons
    // =======================================================================

    /**
     * Aksi tombol:
     *   - Lihat Syarat Nikah -> SyaratNikahActivity (checklist berkas nikah)
     *   - Tautan bantuan     -> WhatsApp resmi admin KUA dengan pesan siap kirim
     */
    private void setupActionButtons() {
        binding.btnLihatSyarat.setOnClickListener(v ->
                startActivity(new Intent(this, SyaratNikahActivity.class)));
    }

    /** Buka WhatsApp admin dengan pesan pra-isi sanggahan/pertanyaan pengumuman. */
  

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
        if (binding != null && binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
            binding.drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }
}