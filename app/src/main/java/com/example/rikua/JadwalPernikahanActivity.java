
package com.example.rikua;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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
import java.util.List;

public class JadwalPernikahanActivity extends AppCompatActivity {

    private static final String API_URL =
            "http://192.168.1.4/rikua_api/get_pengumuman.php?semua=1";

    private ProgressBar progressJadwal;
    private TextView tvStatusJadwal;
    private RecyclerView rvJadwalPernikahan;
    private JadwalPernikahanAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jadwal_pernikahan);

        progressJadwal = findViewById(R.id.progress_jadwal);
        tvStatusJadwal = findViewById(R.id.tv_status_jadwal);
        rvJadwalPernikahan = findViewById(R.id.rv_jadwal_pernikahan);

        adapter = new JadwalPernikahanAdapter();

        rvJadwalPernikahan.setLayoutManager(
                new LinearLayoutManager(this));
        rvJadwalPernikahan.setAdapter(adapter);

        muatJadwal();
    }

    private void muatJadwal() {
        progressJadwal.setVisibility(View.VISIBLE);
        tvStatusJadwal.setText("Memuat jadwal pernikahan...");

        new Thread(() -> {
            HttpURLConnection conn = null;
            List<JadwalPernikahanItem> hasil = new ArrayList<>();
            String error = null;

            try {
                URL url = new URL(API_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int kode = conn.getResponseCode();

                InputStream stream = (kode >= 200 && kode < 300)
                        ? conn.getInputStream()
                        : conn.getErrorStream();

                StringBuilder body = new StringBuilder();

                if (stream != null) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(stream, "UTF-8"))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            body.append(line);
                        }
                    }
                }

                if (kode != HttpURLConnection.HTTP_OK) {
                    error = "Server merespons dengan kode " + kode;
                } else {
                    JSONObject root = new JSONObject(body.toString());

                    if (!"success".equals(root.optString("status"))) {
                        error = root.optString(
                                "message", "Gagal mengambil data jadwal.");
                    } else {
                        JSONArray data = root.optJSONArray("data");

                        if (data != null) {
                            for (int i = 0; i < data.length(); i++) {
                                JSONObject obj = data.getJSONObject(i);

                                hasil.add(new JadwalPernikahanItem(
                                        obj.optString("nama", "-"),
                                        obj.optString("jadwal", "-"),
                                        obj.optString("lokasi", "-")
                                ));
                            }
                        }
                    }
                }

            } catch (Exception e) {
                error = "Tidak dapat mengambil jadwal. Periksa koneksi.";
                android.util.Log.e(
                        "JadwalPernikahan", "Gagal memuat API", e);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }

            final String pesanError = error;
            final List<JadwalPernikahanItem> dataHasil = hasil;

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                progressJadwal.setVisibility(View.GONE);

                if (pesanError != null) {
                    tvStatusJadwal.setText(pesanError);
                    android.util.Log.e(
                            "JadwalPernikahan", pesanError);
                    return;
                }

                adapter.setData(dataHasil);

                if (dataHasil.isEmpty()) {
                    tvStatusJadwal.setText(
                            "Belum ada jadwal pernikahan.");
                    tvStatusJadwal.setVisibility(View.VISIBLE);
                } else {
                    tvStatusJadwal.setVisibility(View.GONE);
                }
            });
        }).start();
    }
}
