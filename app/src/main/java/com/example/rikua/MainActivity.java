package com.example.rikua;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Button;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import android.content.Intent;
import com.google.android.material.navigation.NavigationView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import java.util.List;
import java.util.ArrayList;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class MainActivity extends AppCompatActivity {

    // ====== KONFIGURASI: sesuaikan dengan data KUA Randudongkal ======
    private DrawerLayout drawerLayout;
    private static final String WHATSAPP_NUMBER = "6287787080948"; // format internasional tanpa + atau 0 di depan
    private static final String PHONE_NUMBER = "+6287787080948";
    private static final String MAPS_QUERY = "KUA Randudongkal, Pemalang";
    private static final String WHATSAPP_MESSAGE = "Assalamu'alaikum, saya ingin bertanya terkait layanan KUA Randudongkal.";
    private static final String URL_SIMKAH = "https://simkah.kemenag.go.id";

    /** Endpoint kuota akad. Ganti IP bila IP laptop berubah. */
    private static final String API_KUOTA = "http://192.168.1.4/rikua_api/get_kuota.php";

    /**
     * Saat true, pendaftaran berstatus 'draf' ikut dihitung (untuk uji coba).
     * Ubah ke false untuk rilis.
     */
    private static final boolean HITUNG_DRAF_UNTUK_UJI = true;

    private static final String[] NAMA_BULAN = {
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"};

    private BottomNavigationView bottomNavigation;
    private ObjectAnimator pulseAnimator;

    private TextView tvAnnouncementTitle;
    private TextView tvAnnouncementDesc;
    // Pencarian global
    private EditText etGlobalSearch;
    private View cardSearchResults;
    private LinearLayout layoutSearchResults;
    // Data fasilitas untuk pencarian global
private static final String URL_SARANA =
        "http://192.168.1.4/rikua_api/get_sarana.php";

private static final String URL_GAMBAR_SARANA =
        "http://192.168.1.4/rikua_api/uploads/";

private final List<FacilityItem> daftarFasilitasGlobal =
        new ArrayList<>();

private volatile boolean fasilitasSudahDimuat = false;
private volatile boolean sedangMemuatFasilitas = false;

private final List<ServiceItem> daftarLayananGlobal = new ArrayList<>();
private volatile boolean layananGlobalSudahDimuat = false;
private volatile boolean sedangMemuatLayananGlobal = false;

    /** Isi dialog kuota; null bila data belum berhasil dimuat. */
    private String detailKuota = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        drawerLayout = findViewById(R.id.drawer_layout);

        View btnMenu = findViewById(R.id.btn_menu);
        btnMenu.setOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.END)
        );
NavigationView navView = findViewById(R.id.nav_drawer);

navView.setNavigationItemSelectedListener(item -> {
    Toast.makeText(
            MainActivity.this,
            "Menu dipilih: " + item.getTitle(),
            Toast.LENGTH_SHORT
    ).show();

    handleNavigation(item.getItemId());
    drawerLayout.closeDrawer(GravityCompat.END);
    return true;
});
        bindAnnouncement();
        bindTopBar();
        bindServiceGrid();
        bindInfoCards();
        bindBottomNavigation();
        startStatusPulse();
    }
private void handleNavigation(int itemId) {

    if (itemId == R.id.menu_home) {
        drawerLayout.closeDrawer(GravityCompat.END);

    } else if (itemId == R.id.menu_services) {
        startActivity(new Intent(this, ServicesActivity.class));

    } else if (itemId == R.id.menu_consultation) {
        startActivity(new Intent(this, ConsultationActivity.class));

    } else if (itemId == R.id.menu_facilities) {
        startActivity(new Intent(this, FacilitiesActivity.class));

    } else if (itemId == R.id.menu_forms) {
        startActivity(new Intent(this, BlankoActivity.class));
    }
}
    @Override
    protected void onResume() {
        super.onResume();
        // Bulan & data kuota diperbarui setiap Beranda ditampilkan (realtime)
        perbaruiJudulKuota();
        muatKuota();
    }

    // ------------------------------------------------------------
    // Kuota pendaftaran nikah (judul bulan realtime + data server)
    // ------------------------------------------------------------
    private void bindAnnouncement() {
        tvAnnouncementTitle = findViewById(R.id.tv_announcement_title);
        tvAnnouncementDesc = findViewById(R.id.tv_announcement_desc);

        perbaruiJudulKuota();
        tvAnnouncementDesc.setText("Memuat data kuota…");

        // "Lihat Kuota" membuka daftar kuota per tanggal (bukan langsung ke pendaftaran)
        
findViewById(R.id.btn_lihat_kuota).setOnClickListener(v -> {
    Intent intent = new Intent(MainActivity.this,
            JadwalPernikahanActivity.class);
    startActivity(intent);
});

    }

    private String labelBulanSekarang() {
        Calendar c = Calendar.getInstance();
        return NAMA_BULAN[c.get(Calendar.MONTH)] + " " + c.get(Calendar.YEAR);
    }

    private void perbaruiJudulKuota() {
        tvAnnouncementTitle.setText("Kuota Pendaftaran Nikah — " + labelBulanSekarang());
    }

    /** Ambil ringkasan kuota bulan berjalan dari server (thread latar). */
    private void muatKuota() {
        Calendar c = Calendar.getInstance();
        final String bulan = String.format(Locale.US, "%04d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1);

        new Thread(() -> {
            HttpURLConnection conn = null;
            String ringkas = null;
            String detail = null;
            String galat = null;

            try {
                String alamat = API_KUOTA + "?bulan=" + bulan
                        + (HITUNG_DRAF_UNTUK_UJI ? "&semua=1" : "");
                conn = (HttpURLConnection) new URL(alamat).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                int kode = conn.getResponseCode();
                InputStream is = (kode >= 200 && kode < 300)
                        ? conn.getInputStream() : conn.getErrorStream();
                String body = bacaStream(is);

                if (kode != HttpURLConnection.HTTP_OK) {
                    galat = "Server membalas kode " + kode;
                    android.util.Log.e("Kuota", "HTTP " + kode + " : " + body);
                } else {
                    JSONObject root = new JSONObject(body);
                    if (!"success".equals(root.optString("status"))) {
                        galat = root.optString("message", "Gagal memuat kuota");
                    } else {
                        int total = root.optInt("total_akad", 0);
                        int perHari = root.optInt("kuota_per_hari", 0);
                        ringkas = total + " akad terjadwal bulan ini · maks "
                                + perHari + " akad per hari";
                        detail = susunDetail(root, perHari);
                    }
                }
            } catch (Exception e) {
                galat = "Tidak bisa terhubung ke server";
                android.util.Log.e("Kuota", "Gagal memuat kuota", e);
            } finally {
                if (conn != null) conn.disconnect();
            }

            final String fRingkas = ringkas;
            final String fDetail = detail;
            final String fGalat = galat;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (fGalat == null) {
                    detailKuota = fDetail;
                    tvAnnouncementDesc.setText(fRingkas);
                } else {
                    detailKuota = null;
                    tvAnnouncementDesc.setText(
                            "Data kuota belum bisa dimuat. Periksa koneksi ke server.");
                }
            });
        }).start();
    }

    private String susunDetail(JSONObject root, int perHari) throws org.json.JSONException {
        StringBuilder sb = new StringBuilder();
        sb.append("Maksimal ").append(perHari).append(" akad per hari.\n\n");

        JSONArray arr = root.optJSONArray("hari");
        if (arr == null || arr.length() == 0) {
            sb.append("Belum ada tanggal yang terisi. Semua tanggal masih tersedia.");
        } else {
            sb.append("Tanggal yang sudah terisi:\n");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                sb.append("• ").append(o.optString("label"));
                if (o.optBoolean("penuh")) {
                    sb.append(" — PENUH");
                } else {
                    sb.append(" — terisi ").append(o.optInt("terisi"))
                            .append(", sisa ").append(o.optInt("sisa"));
                }
                sb.append('\n');
            }
            sb.append("\nTanggal yang tidak tercantum masih tersedia.");
        }
        return sb.toString();
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

    private void tampilkanKuota() {
        if (detailKuota == null) {
            Toast.makeText(this, "Data kuota belum bisa dimuat. Mencoba lagi…",
                    Toast.LENGTH_SHORT).show();
            muatKuota();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Kuota " + labelBulanSekarang())
                .setMessage(detailKuota)
                .setPositiveButton("Daftar Nikah", (d, w) -> bukaMenu(R.id.menu_daftar_nikah))
                .setNegativeButton("Tutup", null)
                .show();
    }

    // ------------------------------------------------------------
    // Top bar & pencarian
    // ------------------------------------------------------------
private void bindTopBar() {
    etGlobalSearch = findViewById(R.id.et_global_search);
    cardSearchResults = findViewById(R.id.card_search_results);
    layoutSearchResults = findViewById(R.id.layout_search_results);

    etGlobalSearch.setOnClickListener(v -> {
        etGlobalSearch.requestFocus();
    });

    etGlobalSearch.setOnEditorActionListener((textView, actionId, event) -> {
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            jalankanPencarian(etGlobalSearch.getText().toString());
            return true;
        }
        return false;
    });

    etGlobalSearch.addTextChangedListener(new TextWatcher() {
        @Override
        public void beforeTextChanged(
                CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(
                CharSequence s, int start, int before, int count) {
            jalankanPencarian(s.toString());
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    });

    findViewById(R.id.btn_filter_search)
            .setOnClickListener(v -> tampilkanFilterKategori());
}
private void jalankanPencarian(String query) {
    String kataKunci = query.trim();

    layoutSearchResults.removeAllViews();

    if (kataKunci.isEmpty()) {
        cardSearchResults.setVisibility(View.GONE);
        return;
    }

    cardSearchResults.setVisibility(View.VISIBLE);

    if (!fasilitasSudahDimuat) {
        TextView loading = new TextView(this);
        loading.setText("Memuat data fasilitas...");
        loading.setTextSize(14);
        loading.setPadding(dp(12), dp(12), dp(12), dp(12));
        layoutSearchResults.addView(loading);

        muatFasilitasUntukPencarian();
        return;
    }

    tampilkanHasilFasilitas(kataKunci);
}
private void tampilkanHasilFasilitas(String query) {
    List<FacilityItem> hasil = new ArrayList<>();

    for (FacilityItem item : daftarFasilitasGlobal) {
        if (item.matchesQuery(query)) {
            hasil.add(item);
        }

        if (hasil.size() >= 5) {
            break;
        }
    }

    if (hasil.isEmpty()) {
        TextView kosong = new TextView(this);
        kosong.setText("Fasilitas tidak ditemukan.");
        kosong.setTextSize(14);
        kosong.setPadding(dp(12), dp(12), dp(12), dp(12));
        layoutSearchResults.addView(kosong);
        return;
    }

    TextView judul = new TextView(this);
    judul.setText("FASILITAS KEAGAMAAN");
    judul.setTextSize(12);
    judul.setTypeface(null, android.graphics.Typeface.BOLD);
    judul.setTextColor(Color.GRAY);
    judul.setPadding(dp(12), dp(8), dp(12), dp(8));

    layoutSearchResults.addView(judul);

    for (FacilityItem item : hasil) {
        TextView baris = new TextView(this);
        baris.setText(
                item.getName() + "\n"
                        + item.getCategoryLabel() + " • "
                        + item.getLocation()
        );
        baris.setTextSize(14);
        baris.setTextColor(Color.BLACK);
        baris.setPadding(dp(12), dp(12), dp(12), dp(12));
        baris.setBackgroundResource(
                android.R.drawable.list_selector_background
        );

        baris.setOnClickListener(v -> bukaDetailFasilitas(item));

        layoutSearchResults.addView(baris);
    }
}
private void bukaDetailFasilitas(FacilityItem item) {
    Intent intent = new Intent(
            MainActivity.this,
            FacilityDetailActivity.class
    );

    intent.putExtra(
            FacilityDetailActivity.EXTRA_FACILITY,
            item
    );

    intent.putExtra(
            FacilityDetailActivity.EXTRA_FACILITY_ID,
            item.getId()
    );

    intent.putExtra("extra_name", item.getName());
    intent.putExtra("extra_category", item.getCategoryLabel());
    intent.putExtra("extra_location", item.getLocation());
    intent.putExtra("extra_image", item.getImageUrl());

    startActivity(intent);
}

private void tampilkanHasilPencarian(String query) {
    if (layoutSearchResults == null) return;

    layoutSearchResults.removeAllViews();
    String q = query.toLowerCase(Locale.ROOT);
    int jumlah = 0;

    if (!layananGlobalSudahDimuat || !fasilitasSudahDimuat) {
        TextView loading = buatTeksPencarian("Memuat data pencarian…", true);
        layoutSearchResults.addView(loading);
    }

    List<ServiceItem> layananCocok = new ArrayList<>();
    for (ServiceItem item : daftarLayananGlobal) {
        String teks = (item.getTitle() + " " + item.getSlug())
                .toLowerCase(Locale.ROOT);
        if (teks.contains(q)) layananCocok.add(item);
    }

    List<FacilityItem> fasilitasCocok = new ArrayList<>();
    for (FacilityItem item : daftarFasilitasGlobal) {
        if (item.matchesQuery(query)) fasilitasCocok.add(item);
    }

    if (!layananCocok.isEmpty()) {
        tambahJudulGrup("Layanan");
        for (ServiceItem item : layananCocok) {
            tambahBarisHasil(item.getTitle(), "Ketuk untuk membuka layanan",
                    () -> bukaDetailLayananDariFilter(item));
            jumlah++;
        }
    }

    if (!fasilitasCocok.isEmpty()) {
        tambahJudulGrup("Fasilitas Keagamaan");
        for (FacilityItem item : fasilitasCocok) {
            tambahBarisHasil(item.getName(),
                    item.getCategoryLabel() + " • " + item.getLocation(),
                    () -> bukaDetailFasilitasDariPencarian(item));
            jumlah++;
        }
    }

    if (jumlah == 0 && layananGlobalSudahDimuat && fasilitasSudahDimuat) {
        layoutSearchResults.addView(
                buatTeksPencarian("Tidak ada hasil untuk ‘" + query + "’. ", false));
    }
}

private TextView buatTeksPencarian(String teks, boolean tebal) {
    TextView view = new TextView(this);
    view.setText(teks);
    view.setTextSize(14);
    view.setTextColor(Color.DKGRAY);
    view.setPadding(dp(12), dp(12), dp(12), dp(12));
    if (tebal) view.setTypeface(null, android.graphics.Typeface.BOLD);
    return view;
}

private void tambahJudulGrup(String judul) {
    TextView heading = buatTeksPencarian(judul, true);
    heading.setTextColor(androidx.core.content.ContextCompat.getColor(
            this, R.color.primary_forest));
    layoutSearchResults.addView(heading);
}

private void tambahBarisHasil(String judul, String deskripsi, Runnable aksi) {
    LinearLayout baris = new LinearLayout(this);
    baris.setOrientation(LinearLayout.VERTICAL);
    baris.setPadding(dp(12), dp(10), dp(12), dp(10));
    baris.setBackgroundResource(android.R.drawable.list_selector_background);

    TextView nama = buatTeksPencarian(judul, true);
    nama.setPadding(0, 0, 0, dp(3));
    TextView detail = buatTeksPencarian(
            deskripsi == null ? "" : deskripsi, false);
    detail.setTextSize(12);
    detail.setPadding(0, 0, 0, 0);

    baris.addView(nama);
    baris.addView(detail);
    baris.setOnClickListener(v -> aksi.run());
    layoutSearchResults.addView(baris);
}

private void muatFasilitasUntukPencarian() {
    if (fasilitasSudahDimuat || sedangMemuatFasilitas) return;
    sedangMemuatFasilitas = true;

    new Thread(() -> {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(URL_SARANA).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new Exception("HTTP " + conn.getResponseCode());
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
            }

            JSONObject root = new JSONObject(response.toString());
            if (!"success".equals(root.optString("status"))) {
                throw new Exception("Respons server fasilitas tidak valid");
            }

            JSONArray array = root.optJSONArray("data");
            List<FacilityItem> hasil = new ArrayList<>();
            if (array != null) {
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    String id = String.valueOf(obj.optInt("id", 0));
                    String gambar = obj.optString("gambar", "");
                    String imageUrl = gambar.isEmpty() ? "" : URL_GAMBAR_SARANA + gambar;
                    String nama = obj.optString("nama_sarana", "-");
                    String jenis = obj.optString("jenis", "-");
                    String desa = obj.optString("desa", "-");
                    int jumlahJamaah = obj.optInt("jumlah_jamaah", 0);
                    String keterangan = obj.optString("keterangan",
                            obj.optString("alamat", "-"));

                    FacilityItem item = new FacilityItem(
                            id, nama, jenis,
                            (nama + " " + desa).toLowerCase(Locale.ROOT),
                            keterangan, desa,
                            "Jumlah Jamaah: " + jumlahJamaah + " orang",
                            "Keterangan: " + keterangan,
                            imageUrl,
                            R.color.consult_primary,
                            R.drawable.bg_badge_emerald,
                            R.color.badge_emerald_text,
                            R.drawable.ic_mosque,
                            R.drawable.bg_icon_rounded_green,
                            R.color.primary_forest
                    );
                    item.setLatitude(obj.optDouble("latitude", 0.0));
                    item.setLongitude(obj.optDouble("longitude", 0.0));
                    hasil.add(item);
                }
            }

            runOnUiThread(() -> {
                daftarFasilitasGlobal.clear();
                daftarFasilitasGlobal.addAll(hasil);
                fasilitasSudahDimuat = true;
                sedangMemuatFasilitas = false;
                perbaruiHasilPencarianTerakhir();
            });
        } catch (Exception e) {
            android.util.Log.e("PencarianGlobal", "Gagal memuat fasilitas", e);
            runOnUiThread(() -> {
                sedangMemuatFasilitas = false;
                Toast.makeText(this, "Gagal memuat data fasilitas",
                        Toast.LENGTH_SHORT).show();
                perbaruiHasilPencarianTerakhir();
            });
        } finally {
            if (conn != null) conn.disconnect();
        }
    }).start();
}

private void muatLayananUntukPencarian() {
    if (layananGlobalSudahDimuat || sedangMemuatLayananGlobal) return;
    sedangMemuatLayananGlobal = true;

    new Thread(() -> {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(URL_LAYANAN).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new Exception("HTTP " + conn.getResponseCode());
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
            }

            JSONObject root = new JSONObject(response.toString());
            if (!"success".equals(root.optString("status"))) {
                throw new Exception("Respons server layanan tidak valid");
            }

            JSONArray array = root.optJSONArray("data");
            List<ServiceItem> hasil = new ArrayList<>();
            if (array != null) {
                for (int i = 0; i < array.length(); i++) {
                    hasil.add(parseLayananFilter(array.getJSONObject(i), i));
                }
            }

            runOnUiThread(() -> {
                daftarLayananGlobal.clear();
                daftarLayananGlobal.addAll(hasil);
                layananGlobalSudahDimuat = true;
                sedangMemuatLayananGlobal = false;
                perbaruiHasilPencarianTerakhir();
            });
        } catch (Exception e) {
            android.util.Log.e("PencarianGlobal", "Gagal memuat layanan", e);
            runOnUiThread(() -> {
                sedangMemuatLayananGlobal = false;
                Toast.makeText(this, "Gagal memuat data layanan",
                        Toast.LENGTH_SHORT).show();
                perbaruiHasilPencarianTerakhir();
            });
        } finally {
            if (conn != null) conn.disconnect();
        }
    }).start();
}

private void perbaruiHasilPencarianTerakhir() {
    if (etGlobalSearch == null || isFinishing() || isDestroyed()) return;
    String query = etGlobalSearch.getText().toString().trim();
    if (!query.isEmpty()) tampilkanHasilPencarian(query);
}

private void bukaDetailFasilitasDariPencarian(FacilityItem item) {
    Intent intent = new Intent(this, FacilityDetailActivity.class);
    intent.putExtra(FacilityDetailActivity.EXTRA_FACILITY, item);
    intent.putExtra(FacilityDetailActivity.EXTRA_FACILITY_ID, item.getId());
    intent.putExtra("extra_name", item.getName());
    intent.putExtra("extra_category", item.getCategoryLabel());
    intent.putExtra("extra_location", item.getLocation());
    intent.putExtra("extra_image", item.getImageUrl());
    startActivity(intent);
}

private int dp(int value) {
    return (int) (value * getResources().getDisplayMetrics().density);
}
private static final String URL_LAYANAN =
        "http://192.168.1.4/rikua_api/get_layanan.php";

private final List<ServiceItem> daftarLayananFilter = new ArrayList<>();

private final ExecutorService filterExecutor =
        Executors.newSingleThreadExecutor();

    // ------------------------------------------------------------
    // Grid layanan: tiap kartu memakai ID menu yang sama dengan navbar
    // ------------------------------------------------------------
    private void bindServiceGrid() {
        findViewById(R.id.btn_lihat_semua_layanan)
                .setOnClickListener(v -> bukaMenu(R.id.menu_services));
        findViewById(R.id.card_daftar_nikah)
                .setOnClickListener(v -> bukaMenu(R.id.menu_daftar_nikah));
        findViewById(R.id.card_blangko)
                .setOnClickListener(v -> bukaMenu(R.id.menu_forms));
        findViewById(R.id.card_konsultasi)
                .setOnClickListener(v -> bukaMenu(R.id.menu_consultation));
        findViewById(R.id.card_sarana)
                .setOnClickListener(v -> bukaMenu(R.id.menu_facilities));
    }

    // ------------------------------------------------------------
    // Kartu informasi: lokasi, WhatsApp, telepon
    // ------------------------------------------------------------
    private void bindInfoCards() {
        findViewById(R.id.btn_petunjuk_arah).setOnClickListener(v -> openMaps());
        findViewById(R.id.btn_whatsapp).setOnClickListener(v -> openWhatsApp());
        findViewById(R.id.btn_telepon).setOnClickListener(v -> openDialer());
    }

   private void openMaps() {
    Uri uri = Uri.parse("https://www.google.com/maps/place/KUA+KEC.+RANDUDONGKAL/@-7.0986391,109.3140101,843m/data=!3m2!1e3!4b1!4m6!3m5!1s0x2e6fe9681f12c5b3:0x2ba6aa1285f8c025!8m2!3d-7.0986391!4d109.316585!16s%2Fg%2F11srgyt91f?entry=ttu&g_ep=EgoyMDI2MTAyOC4wIKXMDSoASAFQAw%3D%3D");

    startExternal(
            new Intent(Intent.ACTION_VIEW, uri),
            "Aplikasi peta tidak ditemukan"
    );
}

    private void openWhatsApp() {
        Uri uri = Uri.parse("https://wa.me/" + WHATSAPP_NUMBER + "?text=" + Uri.encode(WHATSAPP_MESSAGE));
        startExternal(new Intent(Intent.ACTION_VIEW, uri), "WhatsApp tidak ditemukan");
    }

    private void openDialer() {
        // ACTION_DIAL tidak memerlukan izin CALL_PHONE
        Uri uri = Uri.parse("tel:" + PHONE_NUMBER);
        startExternal(new Intent(Intent.ACTION_DIAL, uri), "Aplikasi telepon tidak ditemukan");
    }

    private void startExternal(Intent intent, String errorMessage) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        }
    }

    // ------------------------------------------------------------
    // Bottom navigation: Beranda | Layanan | Konsultasi | Sarana | Blanko
    // Dipetakan lewat ID item menu (bukan urutan).
    // ------------------------------------------------------------
    private void bindBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottom_navigation);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_home) {
                return true; // sudah di Beranda
            }
            if (id == R.id.menu_lainnya) {
                openScreen("Menu Lainnya");
                return false;
            }
            bukaMenu(id);
            return false; // Beranda tetap terpilih saat kembali
        });
    }
    private void tampilkanFilterKategori() {
    BottomSheetDialog dialog = new BottomSheetDialog(this);

    LinearLayout container = new LinearLayout(this);
    container.setOrientation(LinearLayout.VERTICAL);
    container.setPadding(dp(24), dp(20), dp(24), dp(24));
    container.setBackgroundColor(Color.WHITE);

    TextView title = new TextView(this);
    title.setText("Pilih Layanan");
    title.setTextSize(20);
    title.setTextColor(Color.BLACK);
    title.setTypeface(null, android.graphics.Typeface.BOLD);

    TextView loading = new TextView(this);
    loading.setText("Memuat daftar layanan...");
    loading.setTextSize(14);
    loading.setTextColor(Color.GRAY);
    loading.setPadding(0, dp(20), 0, dp(20));

    container.addView(title);
    container.addView(loading);

    dialog.setContentView(container);
    dialog.show();

    filterExecutor.execute(() -> {
        HttpURLConnection conn = null;

        try {
            conn = (HttpURLConnection)
                    new URL(URL_LAYANAN).openConnection();

            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new Exception("HTTP " + conn.getResponseCode());
            }

            StringBuilder sb = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            JSONObject root = new JSONObject(sb.toString());

            if (!"success".equals(root.optString("status"))) {
                throw new Exception("Respons server tidak valid");
            }

            JSONArray array = root.getJSONArray("data");
            List<ServiceItem> hasil = new ArrayList<>();

            for (int i = 0; i < array.length(); i++) {
                hasil.add(parseLayananFilter(array.getJSONObject(i), i));
            }

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed() || !dialog.isShowing()) {
                    return;
                }

                if (hasil.isEmpty()) {
                    loading.setText("Belum ada layanan tersedia.");
                    return;
                }

                tampilkanPilihanLayanan(
                        dialog,
                        container,
                        title,
                        hasil
                );
            });

        } catch (Exception e) {
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed() || !dialog.isShowing()) {
                    return;
                }

                loading.setText("Gagal memuat layanan: " + e.getMessage());
            });

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    });
}
private void tampilkanPilihanLayanan(
        BottomSheetDialog dialog,
        LinearLayout container,
        TextView title,
        List<ServiceItem> layanan
) {
    container.removeAllViews();
    container.addView(title);

    TextView subtitle = new TextView(this);
    subtitle.setText("Pilih salah satu layanan yang ingin dibuka");
    subtitle.setTextSize(13);
    subtitle.setTextColor(Color.GRAY);

    container.addView(subtitle);

    RadioGroup radioGroup = new RadioGroup(this);
    radioGroup.setOrientation(RadioGroup.VERTICAL);
    radioGroup.setPadding(0, dp(12), 0, dp(12));

    for (int i = 0; i < layanan.size(); i++) {
        ServiceItem item = layanan.get(i);

        RadioButton radioButton = new RadioButton(this);
        radioButton.setId(View.generateViewId());
        radioButton.setText(item.getTitle());
        radioButton.setTextSize(15);
        radioButton.setTextColor(Color.BLACK);
        radioButton.setPadding(0, dp(8), 0, dp(8));

        radioGroup.addView(radioButton);
    }

    android.widget.Button btnTerapkan =
        new android.widget.Button(this);
    btnTerapkan.setText("Terapkan Filter");
    btnTerapkan.setAllCaps(false);
// Kondisi awal: tombol abu-abu dan nonaktif
btnTerapkan.setEnabled(false);
btnTerapkan.setBackgroundTintList(
        android.content.res.ColorStateList.valueOf(
                Color.parseColor("#BDBDBD")
        )
);

// Tombol berubah hijau setelah layanan dipilih
radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
    boolean sudahDipilih = checkedId != -1;

    btnTerapkan.setEnabled(sudahDipilih);

    int warna = sudahDipilih
            ? androidx.core.content.ContextCompat.getColor(
                    this, R.color.primary_forest)
            : Color.parseColor("#BDBDBD");

    btnTerapkan.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(warna)
    );
});
    btnTerapkan.setOnClickListener(v -> {
        int checkedId = radioGroup.getCheckedRadioButtonId();

        if (checkedId == -1) {
            Toast.makeText(
                    this,
                    "Pilih layanan terlebih dahulu",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        int posisi = radioGroup.indexOfChild(
                radioGroup.findViewById(checkedId)
        );

        ServiceItem itemDipilih = layanan.get(posisi);

        dialog.dismiss();
        bukaDetailLayananDariFilter(itemDipilih);
    });

    container.addView(radioGroup);
    container.addView(btnTerapkan);
}
private ServiceItem parseLayananFilter(JSONObject o, int index) {
    String id = firstNonEmpty(o, String.valueOf(index),
            "id_layanan", "id");

    String title = firstNonEmpty(o, "-",
            "nama_layanan", "judul", "nama", "title");

    String desc = firstNonEmpty(o, "",
            "deskripsi", "keterangan", "description");

    String badge = firstNonEmpty(o, null,
            "badge", "label", "estimasi");

    String kategori = normalizeCategory(
            firstNonEmpty(o, "", "kategori", "category")
    );

    String slug = firstNonEmpty(o, "",
            "kode_layanan", "slug", "kode");

    return new ServiceItem(
            id,
            title,
            desc,
            badge,
            kategori,
            slug,
            R.drawable.ic_verified,
            R.drawable.bg_icon_rounded_green,
            R.drawable.bg_trust_strip,
            R.color.gold_dark,
            R.color.primary_forest
    );
}

private String firstNonEmpty(
        JSONObject o,
        String fallback,
        String... keys
) {
    for (String key : keys) {
        if (o.has(key) && !o.isNull(key)) {
            String value = o.optString(key, "").trim();

            if (!value.isEmpty()) {
                return value;
            }
        }
    }

    return fallback;
}

private String normalizeCategory(String raw) {
    String category = raw == null ? "" : raw.toLowerCase().trim();

    if (category.contains("nikah") || category.contains("rujuk")) {
        return ServiceItem.CATEGORY_PERNIKAHAN;
    }

    if (category.contains("wakaf")) {
        return ServiceItem.CATEGORY_WAKAF;
    }

    return ServiceItem.CATEGORY_ADMINISTRASI;
}
private void bukaDetailLayananDariFilter(ServiceItem item) {
    String slug = item.getSlug() == null
            ? ""
            : item.getSlug().trim().toLowerCase();

    if (slug.isEmpty()) {
        String judul = item.getTitle() == null
                ? ""
                : item.getTitle().toLowerCase();

        if (judul.contains("rekomendasi")) {
            slug = "rekomendasi-nikah";
        } else if (judul.contains("isbat")) {
            slug = "isbat-nikah";
        } else if (judul.contains("duplikat")) {
            slug = "duplikat-buku-nikah";
        } else if (judul.contains("wakaf")) {
            slug = "wakaf-kemasjidan";
        } else if (judul.contains("taukil")) {
            slug = "taukil-wali-bil-kitabah";
        } else if (judul.contains("wali")) {
            slug = "wali-nikah";
        } else if (judul.contains("pengumuman")) {
            slug = "pengumuman-kehendak-nikah";
        } else if (judul.contains("syarat")) {
            slug = "syarat-nikah";
        }
    }

    Intent intent;

    if (slug.contains("rekomendasi")) {
        intent = new Intent(this, RekomendasiNikahActivity.class);
        intent.putExtra(RekomendasiNikahActivity.EXTRA_LAYANAN, item);
        intent.putExtra(RekomendasiNikahActivity.EXTRA_KODE_LAYANAN, slug);
        intent.putExtra(RekomendasiNikahActivity.EXTRA_JUDUL_LAYANAN, item.getTitle());

    } else if (slug.contains("isbat")) {
        intent = new Intent(this, IsbatNikahActivity.class);
        intent.putExtra(IsbatNikahActivity.EXTRA_LAYANAN, item);
        intent.putExtra(IsbatNikahActivity.EXTRA_KODE_LAYANAN, slug);
        intent.putExtra(IsbatNikahActivity.EXTRA_JUDUL_LAYANAN, item.getTitle());

    } else if (slug.contains("duplikat")) {
        intent = new Intent(this, DuplikatBukuNikahActivity.class);
        intent.putExtra(DuplikatBukuNikahActivity.EXTRA_LAYANAN, item);
        intent.putExtra(DuplikatBukuNikahActivity.EXTRA_KODE_LAYANAN, slug);
        intent.putExtra(DuplikatBukuNikahActivity.EXTRA_JUDUL_LAYANAN, item.getTitle());

    } else if (slug.contains("wakaf")) {
        intent = new Intent(this, WakafActivity.class);
        intent.putExtra(WakafActivity.EXTRA_LAYANAN, item);
        intent.putExtra(WakafActivity.EXTRA_KODE_LAYANAN, slug);
        intent.putExtra(WakafActivity.EXTRA_JUDUL_LAYANAN, item.getTitle());

    } else if (slug.contains("taukil")) {
        intent = new Intent(this, TaukilNikahActivity.class);
        intent.putExtra(TaukilNikahActivity.EXTRA_LAYANAN, item);
        intent.putExtra(TaukilNikahActivity.EXTRA_KODE_LAYANAN, slug);
        intent.putExtra(TaukilNikahActivity.EXTRA_JUDUL_LAYANAN, item.getTitle());

    } else if (slug.contains("wali")) {
        intent = new Intent(this, WaliNikahActivity.class);
        intent.putExtra(WaliNikahActivity.EXTRA_LAYANAN, item);
        intent.putExtra(WaliNikahActivity.EXTRA_KODE_LAYANAN, slug);
        intent.putExtra(WaliNikahActivity.EXTRA_JUDUL_LAYANAN, item.getTitle());

    } else if (slug.contains("pengumuman")) {
        intent = new Intent(this, PengumumanNikahActivity.class);
        intent.putExtra(PengumumanNikahActivity.EXTRA_LAYANAN, item);
        intent.putExtra(PengumumanNikahActivity.EXTRA_KODE_LAYANAN, slug);
        intent.putExtra(PengumumanNikahActivity.EXTRA_JUDUL_LAYANAN, item.getTitle());

    } else {
        intent = new Intent(this, SyaratNikahActivity.class);
        intent.putExtra(SyaratNikahActivity.EXTRA_LAYANAN, item);
        intent.putExtra(SyaratNikahActivity.EXTRA_KODE_LAYANAN, slug);
        intent.putExtra(SyaratNikahActivity.EXTRA_JUDUL_LAYANAN, item.getTitle());
    }

    startActivity(intent);
}   


    // ------------------------------------------------------------
    // Satu tempat untuk semua tujuan menu (ID sama dengan file menu)
    // ------------------------------------------------------------
    private void bukaMenu(int id) {
        if (id == R.id.menu_services) {
            buka(ServicesActivity.class);
        } else if (id == R.id.menu_daftar_nikah) {
            buka(DaftarNikahActivity.class);
        } else if (id == R.id.menu_cek_status) {
            buka(CekStatusActivity.class);
        } else if (id == R.id.menu_forms) {
            buka(BlankoActivity.class);
        } else if (id == R.id.menu_facilities) {
            buka(FacilitiesActivity.class);
        } else if (id == R.id.menu_consultation) {
            buka(ConsultationActivity.class);
        } else if (id == R.id.menu_pengumuman) {
            buka(PengumumanNikahActivity.class);
        } else if (id == R.id.menu_simkah) {
            startExternal(new Intent(Intent.ACTION_VIEW, Uri.parse(URL_SIMKAH)),
                    getString(R.string.daf_simkah_unavailable));
        } else if (id == R.id.menu_faq
                || id == R.id.menu_profil
                || id == R.id.menu_login) {
            // Halaman ini memang belum dibuat
            Toast.makeText(this, R.string.daf_feature_soon, Toast.LENGTH_SHORT).show();
        }
    }

    private void buka(Class<?> tujuan) {
        startActivity(new Intent(this, tujuan));
    }

    // ------------------------------------------------------------
    // Animasi indikator status "Buka Sekarang"
    // ------------------------------------------------------------
    private void startStatusPulse() {
        /*View indicator = findViewById(R.id.indicator_status_pulse);
        pulseAnimator = ObjectAnimator.ofFloat(indicator, View.ALPHA, 1f, 0.3f);
        pulseAnimator.setDuration(900);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseAnimator.start();*/
    }

    @Override
    protected void onDestroy() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }
        super.onDestroy();
        filterExecutor.shutdownNow();
    }

    // ------------------------------------------------------------
    // Placeholder untuk fitur yang memang belum dibuat
    // ------------------------------------------------------------
    private void openScreen(String name) {
        Toast.makeText(this, name + " segera hadir", Toast.LENGTH_SHORT).show();
    }
}