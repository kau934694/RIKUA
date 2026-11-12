package com.example.rikua;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class JadwalPernikahanActivity extends AppCompatActivity {

    private static final String API_KUOTA = "http://192.168.1.23/rikua_api/get_kuota.php";

    private ProgressBar progressJadwal;
    private TextView tvStatusJadwal;
    private RecyclerView rvJadwalPernikahan;
    private JadwalPernikahanAdapter adapter;

    private String bulanAktif;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jadwal_pernikahan);

        // 1. Ambil parameter bulan dari Intent, jika kosong fallback ke bulan saat ini
        if (getIntent().hasExtra("EXTRA_BULAN")) {
            bulanAktif = getIntent().getStringExtra("EXTRA_BULAN");
        } else {
            Calendar c = Calendar.getInstance();
            bulanAktif = String.format(Locale.US, "%04d-%02d",
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1);
        }

        // 2. Callback tombol kembali dengan animasi slide
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            }
        });

        // 3. Inisialisasi Views
        progressJadwal = findViewById(R.id.progress_jadwal);
        tvStatusJadwal = findViewById(R.id.tv_status_jadwal);
        rvJadwalPernikahan = findViewById(R.id.rv_jadwal_pernikahan);

        adapter = new JadwalPernikahanAdapter();
        rvJadwalPernikahan.setLayoutManager(new LinearLayoutManager(this));
        rvJadwalPernikahan.setAdapter(adapter);

        // 4. Muat data realtime sesuai bulanAktif
        muatJadwal(bulanAktif);
    }

    private void muatJadwal(String bulan) {
        if (progressJadwal != null) progressJadwal.setVisibility(View.VISIBLE);
        if (tvStatusJadwal != null) tvStatusJadwal.setText("Memuat data kuota & jadwal...");

        new Thread(() -> {
            HttpURLConnection conn = null;
            List<JadwalPernikahanItem> listJadwal = new ArrayList<>();
            String errorMessage = null;

            try {
                String alamat = API_KUOTA + "?bulan=" + bulan + "&semua=1";
                conn = (HttpURLConnection) new URL(alamat).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                int kode = conn.getResponseCode();
                InputStream is = (kode >= 200 && kode < 300)
                        ? conn.getInputStream() : conn.getErrorStream();

                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }

                if (kode != HttpURLConnection.HTTP_OK) {
                    errorMessage = "Server error HTTP " + kode;
                } else {
                    JSONObject root = new JSONObject(response.toString());
                    if (!"success".equals(root.optString("status"))) {
                        errorMessage = root.optString("message", "Gagal mengambil jadwal");
                    } else {
                        JSONArray arrayJadwal = root.optJSONArray("jadwal");
                        if (arrayJadwal == null) arrayJadwal = root.optJSONArray("hari");

                        if (arrayJadwal != null) {
                            for (int i = 0; i < arrayJadwal.length(); i++) {
                                JSONObject obj = arrayJadwal.getJSONObject(i);

                                String nama = obj.optString("nama", obj.optString("label", "Pasangan"));
                                String jadwal = obj.optString("jadwal", obj.optString("tanggal", "-"));
                                String lokasi = obj.optString("lokasi", "KUA Kecamatan");

                                listJadwal.add(new JadwalPernikahanItem(nama, jadwal, lokasi));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                errorMessage = "Gagal terhubung ke server";
                android.util.Log.e("JadwalPernikahan", "Error fetching data", e);
            } finally {
                if (conn != null) conn.disconnect();
            }

            final String fError = errorMessage;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (progressJadwal != null) progressJadwal.setVisibility(View.GONE);

                if (fError == null) {
                    if (tvStatusJadwal != null) {
                        tvStatusJadwal.setVisibility(listJadwal.isEmpty() ? View.VISIBLE : View.GONE);
                        if (listJadwal.isEmpty()) {
                            tvStatusJadwal.setText("Belum ada jadwal terdaftar bulan ini.");
                        }
                    }
                    if (adapter != null) {
                        adapter.setData(listJadwal);
                    }
                } else {
                    if (tvStatusJadwal != null) {
                        tvStatusJadwal.setVisibility(View.VISIBLE);
                        tvStatusJadwal.setText(fError);
                    }
                    Toast.makeText(this, fError, Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
}