package com.example.rikua;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.rikua.databinding.ActivityDaftarNikahStep6Binding;
import com.example.rikua.databinding.BottomsheetSuksesStep6Binding;
import com.example.rikua.databinding.ItemReviewRowBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class DaftarNikahStep6Activity extends AppCompatActivity {

    private static final String URL_SIMKAH =
            "https://simkah.kemenag.go.id";

    private static final String API_URL =
            "http://192.168.1.5/rikua_api/langkah_6_tinjau.php";

    private ActivityDaftarNikahStep6Binding binding;

    private String kodeTiket = "";
    private int idPendaftaran = 0;
    private int dokumenTerverifikasi = 0;
    private int jumlahFieldKosong = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDaftarNikahStep6Binding.inflate(
                getLayoutInflater()
        );
        setContentView(binding.getRoot());

        setupWindowAndStatusBar();
        setupToolbar();
        setupDrawer();
        setupPernyataanGating();
        setupEditButtons();
        setupActionButtons();
        setupBottomNavigation();

        muatData();
    }

    @Override
    protected void onDestroy() {
        binding = null;
        super.onDestroy();
    }

    // =========================================================
    // MEMUAT DATA PENDAFTARAN
    // =========================================================

    private void muatData() {
        idPendaftaran = getIntent().getIntExtra(
                "id_pendaftaran", 0
        );

        String tiket = getIntent().getStringExtra("kode_tiket");
        kodeTiket = tiket != null ? tiket : "";

        if (idPendaftaran <= 0) {
            tampilkanErrorData(
                    "ID pendaftaran tidak valid. Kembali ke langkah sebelumnya."
            );
            return;
        }

        new Thread(this::ambilDariServer).start();
    }

    private void ambilDariServer() {
        HttpURLConnection conn = null;

        try {
            String urlApi = API_URL
                    + "?id_pendaftaran="
                    + URLEncoder.encode(
                            String.valueOf(idPendaftaran), "UTF-8"
                    );

            conn = (HttpURLConnection)
                    new URL(urlApi).openConnection();

            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int responseCode = conn.getResponseCode();

            InputStream stream = responseCode >= 200
                    && responseCode < 300
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            if (stream == null) {
                throw new Exception("Respons server kosong.");
            }

            StringBuilder sb = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, "UTF-8"))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            JSONObject root = new JSONObject(sb.toString());

            if (responseCode < 200 || responseCode >= 300
                    || !"success".equals(
                            root.optString("status"))) {

                String pesan = root.optString(
                        "message",
                        "Data pendaftaran gagal dimuat."
                );

                tampilkanErrorData(pesan);
                return;
            }

            if (root.optInt("id_pendaftaran", -1)
                    != idPendaftaran) {

                tampilkanErrorData(
                        "ID pendaftaran dari server tidak sesuai."
                );
                return;
            }

            runOnUiThread(() -> {
                if (binding != null) {
                    terapkanDataServer(root);
                }
            });

        } catch (Exception e) {
            tampilkanErrorData(
                    "Gagal mengambil data. Periksa koneksi dan server."
            );
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void tampilkanErrorData(String pesan) {
        runOnUiThread(() -> {
            if (binding == null) return;

            Snackbar.make(
                    binding.getRoot(),
                    pesan,
                    Snackbar.LENGTH_LONG
            ).show();
        });
    }

    // =========================================================
    // MEMETAKAN DATA JSON KE TAMPILAN
    // =========================================================

    private void terapkanDataServer(JSONObject root) {
        JSONObject suami = root.optJSONObject("calon_suami");
        JSONObject istri = root.optJSONObject("calon_istri");
        JSONObject daftar = root.optJSONObject("pendaftaran");
        JSONArray pihak = root.optJSONArray("pihak_terkait");
        JSONObject dokumen = root.optJSONObject("dokumen");

        isiRow(binding.rowSuamiNama,
                R.string.s6_lbl_nama,
                nilai(suami, "nama_lengkap"));

        isiRow(binding.rowSuamiNik,
                R.string.s6_lbl_nik,
                nilai(suami, "nik"));

        isiRow(binding.rowSuamiStatus,
                R.string.s6_lbl_status_perkawinan,
                nilai(suami, "status_perkawinan"));

        isiRow(binding.rowSuamiHp,
                R.string.s6_lbl_hp,
                nilai(suami, "no_hp"));

        isiRow(binding.rowIstriNama,
                R.string.s6_lbl_nama,
                nilai(istri, "nama_lengkap"));

        isiRow(binding.rowIstriNik,
                R.string.s6_lbl_nik,
                nilai(istri, "nik"));

        isiRow(binding.rowIstriStatus,
                R.string.s6_lbl_status_perkawinan,
                nilai(istri, "status_perkawinan"));

        isiRow(binding.rowIstriKelahiran,
                R.string.s6_lbl_kelahiran,
                nilai(istri, "urutan_kelahiran"));

        String waliNama = "";
        String waliHubungan = "";
        String saksi1 = "";
        String saksi2 = "";

        if (pihak != null) {
            for (int i = 0; i < pihak.length(); i++) {
                JSONObject orang = pihak.optJSONObject(i);
                if (orang == null) continue;

                String peran = orang.optString("peran", "");
                String nama = orang.optString("nama_lengkap", "");

                switch (peran) {
                    case "wali":
                        waliNama = nama;
                        waliHubungan = orang.optString(
                                "hubungan", ""
                        );
                        break;

                    case "saksi_1":
                        saksi1 = nama;
                        break;

                    case "saksi_2":
                        saksi2 = nama;
                        break;
                }
            }
        }

        isiRow(binding.rowWaliNama,
                R.string.s6_lbl_wali, waliNama);

        isiRow(binding.rowWaliHubungan,
                R.string.s6_lbl_hubungan_wali, waliHubungan);

        isiRow(binding.rowSaksi1,
                R.string.s6_lbl_saksi1, saksi1);

        isiRow(binding.rowSaksi2,
                R.string.s6_lbl_saksi2, saksi2);

        isiRow(binding.rowAkadTanggal,
                R.string.s6_lbl_tanggal,
                nilai(daftar, "tanggal_akad"));

        isiRow(binding.rowAkadWaktu,
                R.string.s6_lbl_waktu,
                nilai(daftar, "waktu_akad"));

        isiRow(binding.rowAkadLokasi,
                R.string.s6_lbl_lokasi,
                nilai(daftar, "lokasi_akad"));

        isiRow(binding.rowAkadTarif,
                R.string.s6_lbl_tarif,
                nilai(daftar, "tarif_pnbp"));

        dokumenTerverifikasi = dokumen != null
                ? dokumen.optInt("terverifikasi", 0)
                : 0;

        binding.tvDokumenCount.setText(
                getString(
                        R.string.s6_dokumen_count,
                        dokumenTerverifikasi
                )
        );

        String tiket = daftar != null
                ? daftar.optString("kode_tiket", "")
                : "";

        if (!tiket.isEmpty()) {
            kodeTiket = tiket;
        }

        hitungKelengkapan();
    }

    private void isiRow(
            ItemReviewRowBinding row,
            int labelRes,
            @Nullable String nilai) {

        row.tvReviewLabel.setText(getString(labelRes));

        String v = nilai == null ? "" : nilai.trim();

        row.tvReviewValue.setText(
                v.isEmpty()
                        ? getString(R.string.s6_val_kosong)
                        : v
        );
    }

    private String nilai(
            @Nullable JSONObject obj,
            String key) {

        return obj == null ? "" : obj.optString(key, "");
    }

    private void hitungKelengkapan() {
        jumlahFieldKosong = 0;

        jumlahFieldKosong += kosongDiRow(binding.rowSuamiNama);
        jumlahFieldKosong += kosongDiRow(binding.rowIstriNama);
        jumlahFieldKosong += kosongDiRow(binding.rowWaliNama);

        boolean lengkap = jumlahFieldKosong == 0;

        binding.tvStatusKelengkapan.setText(
                lengkap
                        ? getString(R.string.s6_status_lengkap)
                        : getString(
                                R.string.s6_status_kurang,
                                jumlahFieldKosong
                        )
        );

        binding.icStatusKelengkapan.setColorFilter(
                ContextCompat.getColor(
                        this,
                        lengkap
                                ? R.color.md3_primary
                                : R.color.s5_amber_text
                )
        );
    }

    private int kosongDiRow(ItemReviewRowBinding row) {
        String v = row.tvReviewValue.getText()
                .toString().trim();

        return v.isEmpty()
                || v.equals(getString(R.string.s6_val_kosong))
                ? 1 : 0;
    }

    // =========================================================
    // TAMPILAN DAN NAVIGASI
    // =========================================================

    private void setupWindowAndStatusBar() {
        Window window = getWindow();

        window.setStatusBarColor(
                ContextCompat.getColor(this, R.color.md3_primary)
        );

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(
                        window, window.getDecorView()
                );

        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(true);

        ViewCompat.setOnApplyWindowInsetsListener(
                binding.layoutBottomSticky,
                (view, insets) -> {

                    androidx.core.graphics.Insets bars =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                            );

                    view.setPadding(
                            view.getPaddingLeft(),
                            view.getPaddingTop(),
                            view.getPaddingRight(),
                            bars.bottom
                    );

                    return insets;
                }
        );
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(
                v -> finish()
        );

        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_open_drawer) {
                toggleDrawer();
                return true;
            }
            return false;
        });
    }

    private void setupDrawer() {
        binding.navDrawer.setCheckedItem(R.id.menu_daftar_nikah);

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

    private void setupPernyataanGating() {
        binding.btnKirimPendaftaran.setEnabled(false);

        binding.cbPernyataan.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    binding.btnKirimPendaftaran.setEnabled(isChecked);

                    binding.tvPernyataanHint.setVisibility(
                            isChecked ? View.GONE : View.VISIBLE
                    );
                }
        );
    }

    private void setupEditButtons() {
        binding.btnUbahSuami.setOnClickListener(
                v -> kembaliKe(DaftarNikahStep2Activity.class)
        );

        binding.btnUbahIstri.setOnClickListener(
                v -> kembaliKe(DataCalonIstriActivity.class)
        );

        binding.btnUbahWali.setOnClickListener(
                v -> kembaliKe(DaftarNikahStep4Activity.class)
        );

        binding.btnUbahAkad.setOnClickListener(
                v -> kembaliKe(DaftarNikahStep4Activity.class)
        );

        binding.btnUbahDokumen.setOnClickListener(
                v -> kembaliKe(DaftarNikahStep5Activity.class)
        );
    }

    private void setupActionButtons() {
        binding.btnSimpanDraf.setOnClickListener(v ->
                Snackbar.make(
                        binding.getRoot(),
                        R.string.s6_draf_saved,
                        Snackbar.LENGTH_LONG
                ).show()
        );

        binding.btnKembaliDokumen.setOnClickListener(
                v -> kembaliKe(DaftarNikahStep5Activity.class)
        );

        binding.btnKirimPendaftaran.setOnClickListener(v -> {
            if (!binding.cbPernyataan.isChecked()) {
                Snackbar.make(
                        binding.getRoot(),
                        R.string.s6_err_belum_setuju,
                        Snackbar.LENGTH_LONG
                ).show();
                return;
            }

            kirimPendaftaran();
        });
    }

    // =========================================================
    // PENGIRIMAN PENDAFTARAN
    // =========================================================

    private void kirimPendaftaran() {
        if (idPendaftaran <= 0) {
            tampilkanErrorData("ID pendaftaran tidak valid.");
            return;
        }

        binding.btnKirimPendaftaran.setEnabled(false);

        Snackbar.make(
                binding.getRoot(),
                R.string.s6_sending,
                Snackbar.LENGTH_SHORT
        ).show();

        new Thread(() -> {
            String tiketServer = postKirimKeServer();

            runOnUiThread(() -> {
                if (binding == null) return;

                if (tiketServer.isEmpty()) {
                    binding.btnKirimPendaftaran.setEnabled(
                            binding.cbPernyataan.isChecked()
                    );

                    tampilkanErrorData(
                            "Pendaftaran belum dapat dikirim. "
                                    + "Server tidak mengonfirmasi penyimpanan."
                    );
                    return;
                }

                kodeTiket = tiketServer;
                tampilkanModalSukses();
            });
        }).start();
    }

    private String postKirimKeServer() {
    HttpURLConnection conn = null;

    try {
        conn = (HttpURLConnection)
                new URL(API_URL).openConnection();

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        conn.setRequestProperty(
                "Content-Type",
                "application/x-www-form-urlencoded; charset=UTF-8"
        );
        conn.setRequestProperty(
                "Accept",
                "application/json"
        );

        String body =
                "id_pendaftaran="
                + URLEncoder.encode(
                        String.valueOf(idPendaftaran), "UTF-8"
                )
                + "&kode_tiket="
                + URLEncoder.encode(kodeTiket, "UTF-8")
                + "&status="
                + URLEncoder.encode("dikirim", "UTF-8")
                + "&persetujuan_data=1";

        android.util.Log.d(
                "STEP6_KIRIM",
                "Mengirim POST. HTTP endpoint: " + API_URL
                        + ", ID: " + idPendaftaran
                        + ", kode tiket tersedia: "
                        + !kodeTiket.trim().isEmpty()
        );

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes("UTF-8"));
            os.flush();
        }

        int responseCode = conn.getResponseCode();

        InputStream stream = responseCode >= 200
                && responseCode < 300
                ? conn.getInputStream()
                : conn.getErrorStream();

        if (stream == null) {
            android.util.Log.e(
                    "STEP6_KIRIM",
                    "HTTP " + responseCode
                            + ", server tidak memberikan response body."
            );
            return "";
        }

        StringBuilder sb = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, "UTF-8"))) {

            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        String response = sb.toString();

        android.util.Log.d(
                "STEP6_KIRIM",
                "HTTP " + responseCode
                        + ", respons server: " + response
        );

        if (responseCode < 200 || responseCode >= 300) {
            android.util.Log.e(
                    "STEP6_KIRIM",
                    "Server mengembalikan HTTP error: "
                            + responseCode
            );
            return "";
        }

        JSONObject json = new JSONObject(response);

        String status = json.optString("status", "");
        int idRespons = json.optInt("id_pendaftaran", -1);
        String tiketRespons = json.optString("kode_tiket", "");

        if (!"success".equals(status)) {
            android.util.Log.e(
                    "STEP6_KIRIM",
                    "Nilai status respons bukan success: " + status
            );
            return "";
        }

        if (idRespons != idPendaftaran) {
            android.util.Log.e(
                    "STEP6_KIRIM",
                    "ID tidak cocok. ID aplikasi: " + idPendaftaran
                            + ", ID server: " + idRespons
            );
            return "";
        }

        if (tiketRespons.trim().isEmpty()) {
            android.util.Log.e(
                    "STEP6_KIRIM",
                    "Respons sukses, tetapi kode_tiket kosong."
            );
            return "";
        }

        return tiketRespons;

    } catch (Exception e) {
        android.util.Log.e(
                "STEP6_KIRIM",
                "Terjadi kesalahan saat mengirim pendaftaran.",
                e
        );
        return "";

    } finally {
        if (conn != null) {
            conn.disconnect();
        }
    }
}

    // =========================================================
    // MODAL SUKSES
    // =========================================================

    private void tampilkanModalSukses() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);

        BottomsheetSuksesStep6Binding sheet =
                BottomsheetSuksesStep6Binding.inflate(
                        LayoutInflater.from(this)
                );

        dialog.setContentView(sheet.getRoot());
        sheet.tvTiketKode.setText(kodeTiket);

        sheet.btnSalinTiket.setOnClickListener(
                v -> salinTiket(
                        sheet.tvTiketKode.getText().toString()
                )
        );

        sheet.btnUnduhPdf.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        R.string.s6_unduh_soon,
                        Toast.LENGTH_SHORT
                ).show()
        );

        sheet.btnCekStatus.setOnClickListener(v -> {
            dialog.dismiss();

            Intent intent = new Intent(
                    this, CekStatusActivity.class
            );

            intent.putExtra("kode_tiket", kodeTiket);
            startActivity(intent);
        });

        sheet.btnSelesai.setOnClickListener(v -> {
            dialog.dismiss();
            navigateTo(MainActivity.class);
        });

        dialog.show();
    }

    private void salinTiket(String tiket) {
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(
                        Context.CLIPBOARD_SERVICE
                );

        if (clipboard != null) {
            clipboard.setPrimaryClip(
                    ClipData.newPlainText(
                            "Kode Tiket RIKUA", tiket
                    )
            );

            Toast.makeText(
                    this,
                    getString(R.string.s6_tiket_disalin, tiket),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // =========================================================
    // NAVIGASI
    // =========================================================

    private void kembaliKe(Class<?> target) {
        Intent intent = new Intent(this, target);
        intent.putExtra("id_pendaftaran", idPendaftaran);
        intent.putExtra("kode_tiket", kodeTiket);
        startActivity(intent);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = binding.bottomNavigation;

        bottomNav.setSelectedItemId(R.id.menu_daftar_nikah);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.menu_daftar_nikah) {
                return true;
            }

            if (id == R.id.menu_lainnya) {
                toggleDrawer();
                bottomNav.setSelectedItemId(R.id.menu_daftar_nikah);
                return false;
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
        } else if (itemId == R.id.menu_forms) {
            navigateTo(BlankoActivity.class);
        } else if (itemId == R.id.menu_facilities) {
            navigateTo(FacilitiesActivity.class);
        } else if (itemId == R.id.menu_consultation) {
            navigateTo(ConsultationActivity.class);
        } else if (itemId == R.id.menu_pengumuman) {
            navigateTo(PengumumanNikahActivity.class);
        } else if (itemId == R.id.menu_simkah) {
            try {
                startActivity(new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(URL_SIMKAH)
                ));
            } catch (Exception e) {
                Toast.makeText(
                        this,
                        R.string.daf_simkah_unavailable,
                        Toast.LENGTH_SHORT
                ).show();
            }
        } else {
            Toast.makeText(
                    this,
                    R.string.daf_feature_soon,
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void navigateTo(Class<?> target) {
        Intent intent = new Intent(this, target);

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (binding != null
                && binding.drawerLayout.isDrawerOpen(
                        GravityCompat.END)) {

            binding.drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }
}