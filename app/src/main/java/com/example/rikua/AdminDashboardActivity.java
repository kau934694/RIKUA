package com.example.rikua;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.rikua.databinding.ActivityAdminDashboardBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Dashboard petugas KUA: ringkasan kerja, antrean berkas perlu diperiksa,
 * kuota akad 7 hari, dan aksi cepat. Hanya untuk sesi dengan peran admin.
 */
public class AdminDashboardActivity extends AppCompatActivity {
    private static final String TAG = "AdminDashboard";

    private static final int MAKS_ANTREAN_TAMPIL = 4;
    private static final int MAKS_HARI_KUOTA = 7;

    private ActivityAdminDashboardBinding binding;
    private SessionManager sessionManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Locale localeId = Locale.forLanguageTag("id-ID");

    private AdminAntreanAdapter antreanAdapter;
    private AdminKuotaAdapter kuotaAdapter;

    // Membedakan "data memang kosong" dan "gagal memuat" (pola MainActivity).
    private boolean gagalMemuat = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (!penjagaSesiAdmin()) {
            return;
        }

        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        isiHeader();
        siapkanDaftar();
        siapkanAksi();

        muatDashboard();
    }

    /**
     * Penjaga sesi: belum login, peran bukan admin, atau kedaluwarsa
     * menghapus sesi dan mengembalikan petugas ke login admin.
     * Mengembalikan false bila activity langsung diselesaikan.
     */
    private boolean penjagaSesiAdmin() {
        String role = sessionManager.getRole();
        boolean admin = role != null && "admin".equalsIgnoreCase(role.trim());
        boolean tokenPresent = sessionManager.isLoggedIn();
        boolean sessionValid = sessionManager.isSessionValid();
        Log.i(TAG, "Session guard: tokenPresent=" + tokenPresent
                + ", sessionValid=" + sessionValid
                + ", roleAdmin=" + admin);
        if (sessionValid && admin) {
            return true;
        }

        Log.w(TAG, "Redirecting to admin login: "
                + (!tokenPresent ? "token_missing"
                : !sessionValid ? "session_invalid" : "role_not_admin"));
        sessionManager.clearSession();
        Toast.makeText(this, R.string.admin_akun_ditolak, Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, AdminLoginActivity.class));
        finish();
        return false;
    }

    private void isiHeader() {
        String nama = nullToEmpty(sessionManager.getNamaLengkap());
        if (nama.isEmpty()) {
            nama = nullToEmpty(sessionManager.getEmail());
        }
        if (nama.isEmpty()) {
            nama = getString(R.string.admin_title_dashboard);
        }

        binding.tvNamaPetugas.setText(nama);
        binding.tvInisial.setText(inisial(nama));
        if (binding.tvInisial.getText().toString().isEmpty()) {
            binding.tvInisial.setVisibility(View.GONE);
        }

        binding.tvTanggalHariIni.setText(baruSatuBarisTanggal());
    }

    private String inisial(String nama) {
        String[] bagian = nama.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String b : bagian) {
            if (b.isEmpty()) continue;
            sb.append(Character.toUpperCase(b.charAt(0)));
            if (sb.length() == 2) break;
        }
        return sb.toString();
    }

    // Satu baris: tanggal Masehi, ditambah Hijriah pada API 26 ke atas.
    private String baruSatuBarisTanggal() {
        String masehi = new SimpleDateFormat("EEEE, d MMMM yyyy", localeId)
                .format(new Date());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                java.time.LocalDate sekarang = java.time.LocalDate.now();
                java.time.format.DateTimeFormatter format =
                        java.time.format.DateTimeFormatter
                                .ofPattern("d MMMM yyyy", localeId)
                                .withChronology(
                                        java.time.chrono.HijrahChronology.INSTANCE);
                return getString(R.string.admin_tanggal_gabung,
                        masehi, format.format(sekarang));
            } catch (Exception e) {
                // Kronologi Hijriah tidak tersedia: tampilkan Masehi saja.
            }
        }
        return masehi;
    }

    private void siapkanDaftar() {
        antreanAdapter = new AdminAntreanAdapter();
        binding.rvAntrean.setLayoutManager(new LinearLayoutManager(this));
        binding.rvAntrean.setAdapter(antreanAdapter);

        kuotaAdapter = new AdminKuotaAdapter();
        binding.rvKuota.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvKuota.setAdapter(kuotaAdapter);
    }

    private void siapkanAksi() {
        binding.btnNotifikasi.setOnClickListener(v -> Toast.makeText(this,
                R.string.admin_notif_segera_hadir, Toast.LENGTH_SHORT).show());

        binding.btnKeluar.setOnClickListener(v -> konfirmasiKeluar());

        View.OnClickListener segeraHadir = v -> Toast.makeText(this,
                R.string.admin_segera_hadir, Toast.LENGTH_SHORT).show();
        binding.cardRingkasDaftar.setOnClickListener(segeraHadir);
        binding.cardRingkasDokumen.setOnClickListener(segeraHadir);
        binding.cardRingkasAkad.setOnClickListener(segeraHadir);
        binding.cardRingkasKonsul.setOnClickListener(segeraHadir);
        binding.btnLihatSemua.setOnClickListener(segeraHadir);
        binding.aksiSarana.setOnClickListener(v ->
                startActivity(new Intent(this, AdminSaranaActivity.class)));
        binding.aksiBlanko.setOnClickListener(segeraHadir);
        binding.aksiPengumuman.setOnClickListener(segeraHadir);
        binding.aksiKonsultasi.setOnClickListener(segeraHadir);

        binding.btnSimkah.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(ApiConfig.URL_SIMKAH_PETUGAS)));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.daf_simkah_unavailable,
                        Toast.LENGTH_SHORT).show();
            }
        });

        binding.bottomNavAdmin.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.menu_admin_dasbor) {
                return true;
            }
            if (item.getItemId() == R.id.menu_admin_pendaftaran) {
                startActivity(new Intent(this, AdminPendaftaranActivity.class));
                binding.bottomNavAdmin.setSelectedItemId(R.id.menu_admin_dasbor);
                return true;
            }
            if (item.getItemId() == R.id.menu_admin_sarana) {
                startActivity(new Intent(this, AdminSaranaActivity.class));
                binding.bottomNavAdmin.setSelectedItemId(R.id.menu_admin_dasbor);
                return true;
            }
            Toast.makeText(this, R.string.admin_segera_hadir,
                    Toast.LENGTH_SHORT).show();
            binding.bottomNavAdmin.setSelectedItemId(R.id.menu_admin_dasbor);
            return false;
        });
        binding.bottomNavAdmin.setSelectedItemId(R.id.menu_admin_dasbor);

        binding.btnCobaLagi.setOnClickListener(v -> muatDashboard());
    }

    private void konfirmasiKeluar() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.admin_keluar_judul)
                .setMessage(R.string.admin_keluar_pesan)
                .setNegativeButton(R.string.admin_keluar_batal,
                        (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.admin_keluar_konfirmasi, (dialog, which) -> {
                    sessionManager.clearSession();
                    Intent intent = new Intent(this, AdminLoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .show();
    }

    private void tampilkanKeadaanMuat() {
        binding.progressDashboard.setVisibility(View.VISIBLE);
        binding.layoutError.setVisibility(View.GONE);
        binding.layoutKonten.setVisibility(View.GONE);
    }

    private void muatDashboard() {
        tampilkanKeadaanMuat();

        executor.execute(() -> {
            HttpURLConnection connection = null;
            String galat = null;
            JSONObject data = null;
            boolean ditolak = false;
            boolean aksesDitolak = false;

            try {
                connection = (HttpURLConnection)
                        new URL(ApiConfig.ADMIN_DASHBOARD).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setRequestProperty("Accept", "application/json");
                String token = sessionManager.getToken();
                boolean tokenPresent = token != null && !token.trim().isEmpty();
                if (tokenPresent) {
                    connection.setRequestProperty(
                            "Authorization", "Bearer " + token);
                }

                int kode = connection.getResponseCode();
                Log.i(TAG, "Dashboard API response HTTP " + kode
                        + ", bearerTokenSent=" + tokenPresent);
                if (kode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    ditolak = true;
                } else {
                    if (kode == HttpURLConnection.HTTP_FORBIDDEN) {
                        aksesDitolak = true;
                        Log.w(TAG, "Dashboard API returned HTTP 403");
                    }
                    InputStream is = (kode >= 200 && kode < 300)
                            ? connection.getInputStream()
                            : connection.getErrorStream();
                    String body = bacaStream(is);

                    if (kode != HttpURLConnection.HTTP_OK) {
                        galat = "Server membalas kode " + kode;
                        Log.e(TAG, "Dashboard API returned HTTP " + kode);
                    } else {
                        JSONObject root = new JSONObject(body);
                        if (!root.optBoolean("success", false)) {
                            galat = root.optString("message",
                                    "Data dashboard belum tersedia");
                        } else {
                            data = root;
                        }
                    }
                }
            } catch (Exception e) {
                galat = "Tidak bisa terhubung ke server";
                android.util.Log.e("AdminDashboard", "Gagal memuat dashboard", e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            final JSONObject fData = data;
            final String fGalat = galat;
            final boolean finalDitolak = ditolak;
            final boolean finalAksesDitolak = aksesDitolak;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed() || binding == null) {
                    return;
                }
                if (finalDitolak) {
                    sesiDitolak();
                } else if (fGalat == null) {
                    gagalMemuat = false;
                    tampilkanData(fData);
                } else {
                    gagalMemuat = true;
                    if (finalAksesDitolak) {
                        Toast.makeText(this, R.string.admin_akses_ditolak,
                                Toast.LENGTH_LONG).show();
                    }
                    tampilkanKegagalan();
                }
            });
        });
    }

    private void sesiDitolak() {
        Log.w(TAG, "Dashboard API rejected bearer token; clearing session");
        sessionManager.clearSession();
        Toast.makeText(this, R.string.admin_akun_ditolak, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, AdminLoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void tampilkanKegagalan() {
        binding.progressDashboard.setVisibility(View.GONE);
        binding.layoutKonten.setVisibility(View.GONE);
        binding.layoutError.setVisibility(View.VISIBLE);
    }

    private void tampilkanData(JSONObject root) {
        binding.progressDashboard.setVisibility(View.GONE);
        binding.layoutError.setVisibility(View.GONE);
        binding.layoutKonten.setVisibility(View.VISIBLE);

        JSONObject ringkasan = root.optJSONObject("ringkasan");
        if (ringkasan != null) {
            binding.tvRingkasDaftar.setText(
                    String.valueOf(ringkasan.optInt("pendaftaran_baru", 0)));
            binding.tvRingkasDokumen.setText(
                    String.valueOf(ringkasan.optInt("dokumen_perlu_diperiksa", 0)));
            binding.tvRingkasAkad.setText(
                    String.valueOf(ringkasan.optInt("akad_bulan_ini", 0)));
            binding.tvRingkasKonsul.setText(
                    String.valueOf(ringkasan.optInt("konsultasi_aktif", 0)));
        }

        // Antrean: maksimal empat baris; keadaan kosong ditampilkan di kartu.
        List<AdminAntreanItem> antrean = new ArrayList<>();
        JSONArray arrAntrean = root.optJSONArray("antrean");
        if (arrAntrean != null) {
            for (int i = 0; i < arrAntrean.length()
                    && antrean.size() < MAKS_ANTREAN_TAMPIL; i++) {
                JSONObject o = arrAntrean.optJSONObject(i);
                if (o == null) continue;
                antrean.add(new AdminAntreanItem(
                        o.optString("id_pendaftaran", ""),
                        o.optString("kode_tiket", "-"),
                        o.optString("nama_calon", "-"),
                        formatTanggalAkad(o.optString("tanggal_akad", "")),
                        o.optInt("dokumen_terunggah", 0),
                        o.optInt("dokumen_total", 0),
                        o.optString("status", "belum_diunggah")));
            }
        }
        antreanAdapter.submitList(antrean);
        boolean kosong = antrean.isEmpty();
        binding.rvAntrean.setVisibility(kosong ? View.GONE : View.VISIBLE);
        binding.tvKosongAntrean.setVisibility(kosong ? View.VISIBLE : View.GONE);
        binding.btnLihatSemua.setVisibility(kosong ? View.GONE : View.VISIBLE);

        // Kuota 7 hari.
        List<AdminKuotaItem> kuotaList = new ArrayList<>();
        JSONArray arrKuota = root.optJSONArray("kuota");
        String hariIni = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .format(Calendar.getInstance().getTime());
        if (arrKuota != null) {
            for (int i = 0; i < arrKuota.length()
                    && kuotaList.size() < MAKS_HARI_KUOTA; i++) {
                JSONObject o = arrKuota.optJSONObject(i);
                if (o == null) continue;
                String tanggal = o.optString("tanggal", "");
                int terisi = o.optInt("terisi", 0);
                int kuota = o.optInt("kuota", 0);
                boolean penuh = kuota > 0 && terisi >= kuota;
                kuotaList.add(new AdminKuotaItem(
                        labelHari(tanggal),
                        labelTanggal(tanggal),
                        penuh ? getString(R.string.admin_penuh)
                                : terisi + "/" + kuota,
                        penuh,
                        hariIni.equals(tanggal)));
            }
        }
        kuotaAdapter.submitList(kuotaList);
        binding.rvKuota.setVisibility(
                kuotaList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // "2026-11-22 08:00:00" -> "22 Nov 2026, 08:00"; gagal baca tetap tampil apa adanya.
    private String formatTanggalAkad(String mentah) {
        if (mentah == null || mentah.trim().isEmpty()) {
            return "-";
        }
        String nilai = mentah.trim();
        try {
            Date parsed = null;
            if (nilai.length() >= 19) {
    parsed = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            .parse(nilai.substring(0, 19));

    if (parsed != null) {
        return new SimpleDateFormat("d MMM yyyy, HH:mm", localeId)
                .format(parsed);
    }
}
            if (nilai.length() >= 10) {
                parsed = new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        .parse(nilai.substring(0, 10));
                if (parsed != null) {
                    return new SimpleDateFormat("d MMM yyyy", localeId)
                            .format(parsed);
                }
            }
        } catch (Exception ignored) {
            // jatuh ke pengembalian nilai mentah
        }
        return nilai;
    }

    private String labelHari(String tanggal) {
        Date parsed = parseTanggalMentah(tanggal);
        return parsed == null ? ""
                : new SimpleDateFormat("EEE", localeId).format(parsed);
    }

    private String labelTanggal(String tanggal) {
        Date parsed = parseTanggalMentah(tanggal);
        return parsed == null ? tanggal
                : new SimpleDateFormat("d", localeId).format(parsed);
    }

    private Date parseTanggalMentah(String tanggal) {
        if (tanggal == null || tanggal.length() < 10) {
            return null;
        }
        try {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    .parse(tanggal.substring(0, 10));
        } catch (Exception e) {
            return null;
        }
    }

    private String nullToEmpty(String nilai) {
        return nilai == null ? "" : nilai;
    }

    private String bacaStream(InputStream is) throws java.io.IOException {
        if (is == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String baris;
            while ((baris = reader.readLine()) != null) {
                sb.append(baris);
            }
        }
        return sb.toString();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        binding = null;
        super.onDestroy();
    }
}
