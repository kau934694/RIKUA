package com.example.rikua;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class CalonApi {

    public static final String URL_CALON =
            "http://192.168.1.5/rikua_api/get_calon_pengantin.php";

    private static final Handler UI = new Handler(Looper.getMainLooper());

    public interface SimpanCallback {
        void onSukses(int idPendaftaran);
        void onGagal(String pesan);
    }

    public interface AmbilCallback {
        void onSukses(JSONObject data);
        void onGagal(String pesan);
    }

    // POST: step 2 = calon suami, step 3 = calon istri
    public static void simpan(int step, int idPendaftaran, String nama, String nik,
                              String tempat, String tanggal, String agama,
                              String pekerjaan, String pendidikan, String alamat,
                              String telepon, String status, boolean anakPertama,
                              final SimpanCallback cb) {

        final JSONObject body = new JSONObject();
        try {
            body.put("step", step);
            if (idPendaftaran > 0) body.put("id_pendaftaran", idPendaftaran);
            body.put("nama_lengkap", nama);
            body.put("nik", nik);
            body.put("tempat_lahir", tempat);
            body.put("tanggal_lahir", tanggal);
            body.put("agama", agama);
            body.put("pekerjaan", pekerjaan);
            body.put("pendidikan_terakhir", pendidikan);
            body.put("alamat", alamat);
            body.put("no_hp", telepon.replaceFirst("^0+", ""));
            body.put("status_perkawinan", status);
            if (step == 3) body.put("anak_pertama", anakPertama ? "1" : "0");
        } catch (Exception e) {
            cb.onGagal("Gagal menyiapkan data.");
            return;
        }

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(URL_CALON).openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }

                final JSONObject json = new JSONObject(bacaRespons(conn));

                if (json.optBoolean("status", false)) {
                    final int id = json.optInt("id_pendaftaran", 0);

                    if (id <= 0) {
                        UI.post(() -> cb.onGagal(
                                "Server tidak mengembalikan ID pendaftaran yang valid."));
                        return;
                    }

                    UI.post(() -> cb.onSukses(id));
                } else {
                    final String pesan = bangunPesan(json);
                    UI.post(() -> cb.onGagal(pesan));
                }
            } catch (Exception e) {
                UI.post(() -> cb.onGagal(
                        "Tidak bisa terhubung ke server. Periksa jaringan/IP laptop."));
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    // GET: ambil data suami + istri
    public static void ambil(int idPendaftaran, final AmbilCallback cb) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(URL_CALON + "?id_pendaftaran=" + idPendaftaran);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                final JSONObject json = new JSONObject(bacaRespons(conn));

                if (json.optBoolean("status", false)) {
                    UI.post(() -> cb.onSukses(json));
                } else {
                    final String pesan = bangunPesan(json);
                    UI.post(() -> cb.onGagal(pesan));
                }
            } catch (Exception e) {
                UI.post(() -> cb.onGagal("Tidak bisa terhubung ke server."));
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private static String bacaRespons(HttpURLConnection conn) throws Exception {
        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300)
                ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) throw new Exception("Respons kosong, kode " + code);

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private static String bangunPesan(JSONObject json) {
        StringBuilder sb = new StringBuilder(json.optString("message", "Terjadi kesalahan."));
        JSONObject errors = json.optJSONObject("errors");
        if (errors != null) {
            Iterator<String> it = errors.keys();
            while (it.hasNext()) {
                sb.append("\n• ").append(errors.optString(it.next()));
            }
        }
        return sb.toString();
    }
}