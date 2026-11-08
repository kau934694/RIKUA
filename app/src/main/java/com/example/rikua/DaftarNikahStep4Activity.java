package com.example.rikua;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.rikua.databinding.ActivityDaftarNikahStep4Binding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.regex.Pattern;

/**
 * DaftarNikahStep4Activity — Halaman "Daftar Nikah: Wali, Saksi & Rencana Akad" (Langkah 4 dari 6) RIKUA KUA Kec. Randudongkal.
 */
public class DaftarNikahStep4Activity extends AppCompatActivity {

    private static final String URL_SIMKAH = "https://simkah.kemenag.go.id";
    
    // Endpoint API PHP yang konsisten
    private static final String API_URL = "http://192.168.1.5/rikua_api/get_langkah_4_wali.php";

    private int idPendaftaran = 0;
    private String kodeTiketAktif = "";

    private static final Pattern POLA_NIK = Pattern.compile("\\d{16}");
    private static final Pattern POLA_TANGGAL = Pattern.compile("\\d{2}/\\d{2}/\\d{4}");
    private static final Pattern POLA_WAKTU = Pattern.compile("\\d{2}:\\d{2}");

    private static final int LOKASI_KUA = 0;
    private static final int LOKASI_LUAR = 1;

    private ActivityDaftarNikahStep4Binding binding;
    private int lokasiAkadTerpilih = LOKASI_KUA;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDaftarNikahStep4Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Ambil id_pendaftaran yang diteruskan dari Langkah 3
        idPendaftaran = getIntent().getIntExtra("id_pendaftaran", 0);

        // Migrasi OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding != null && binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.END);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });

        muatDataDariDatabase();
        setupWindowAndStatusBar();
        setupToolbar();
        setupDrawer();
        setupDropdownWali();
        setupRencanaAkad();
        setupLokasiAkad();
        setupClearErrorOnInput();
        setupActionButtons();
        setupBottomNavigation();
    }

    @Override
    protected void onDestroy() {
        binding = null;
        super.onDestroy();
    }

    private void muatDataDariDatabase() {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(API_URL + "?id_pendaftaran=" + idPendaftaran);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.connect();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream())
                    );
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    org.json.JSONObject json = new org.json.JSONObject(sb.toString());
                    if (json.optBoolean("status") || "success".equals(json.optString("status"))) {
                        kodeTiketAktif = json.optString("kode_tiket", "");

                        runOnUiThread(() -> {
                            if (!kodeTiketAktif.isEmpty() && binding != null) {
                                binding.tvKodeTiketValue.setText(kodeTiketAktif);
                            }
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private void setupWindowAndStatusBar() {
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.md3_primary));

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(true);

        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutBottomSticky, (view, insets) -> {
            androidx.core.graphics.Insets bars =
                    insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(view.getPaddingLeft(), view.getPaddingTop(),
                    view.getPaddingRight(), bars.bottom);
            return insets;
        });
    }

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

    private void setupDropdownWali() {
        ArrayAdapter<CharSequence> waliAdapter = ArrayAdapter.createFromResource(
                this, R.array.wali_hubungan_options, android.R.layout.simple_list_item_1);
        binding.dropdownHubunganWali.setAdapter(waliAdapter);
    }

    private void setupRencanaAkad() {
        binding.etTanggalAkad.setOnClickListener(v -> tampilkanDatePickerAkad());
        binding.etWaktuAkad.setOnClickListener(v -> tampilkanTimePickerAkad());
    }

    private void tampilkanDatePickerAkad() {
        Calendar now = Calendar.getInstance();
        Calendar initial = (Calendar) now.clone();
        initial.add(Calendar.DAY_OF_YEAR, 10);

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    binding.etTanggalAkad.setText(
                            String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year));
                    binding.tilTanggalAkad.setError(null);
                },
                initial.get(Calendar.YEAR), initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMinDate(now.getTimeInMillis());
        dialog.show();
    }

    private void tampilkanTimePickerAkad() {
        Calendar now = Calendar.getInstance();
        TimePickerDialog dialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    binding.etWaktuAkad.setText(
                            String.format("%02d:%02d", hourOfDay, minute));
                    binding.tilWaktuAkad.setError(null);
                },
                now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true);
        dialog.show();
    }

    private void setupLokasiAkad() {
        binding.cardLokasiKua.setOnClickListener(v -> pilihLokasiAkad(LOKASI_KUA));
        binding.cardLokasiLuar.setOnClickListener(v -> pilihLokasiAkad(LOKASI_LUAR));
        pilihLokasiAkad(LOKASI_KUA);
    }

    private void pilihLokasiAkad(int index) {
        lokasiAkadTerpilih = index;
        boolean kua = index == LOKASI_KUA;

        binding.rbLokasiKua.setChecked(kua);
        binding.rbLokasiLuar.setChecked(!kua);
        terangkanKartu(binding.cardLokasiKua, kua);
        terangkanKartu(binding.cardLokasiLuar, !kua);

        binding.tilAlamatAkad.setVisibility(kua ? View.GONE : View.VISIBLE);
        if (kua) {
            binding.tilAlamatAkad.setError(null);
        }
    }

    private void terangkanKartu(MaterialCardView card, boolean terpilih) {
        int bg = ContextCompat.getColor(this,
                terpilih ? R.color.md3_radio_selected_bg : R.color.md3_surface_card);
        int stroke = ContextCompat.getColor(this,
                terpilih ? R.color.md3_primary : R.color.md3_outline_variant_40);
        card.setCardBackgroundColor(bg);
        card.setStrokeColor(ColorStateList.valueOf(stroke));
        card.setStrokeWidth(dp(terpilih ? 1.5f : 1f));
    }

    private void setupClearErrorOnInput() {
        bersihkanErrorSaatKetik(binding.tilNamaWali);
        bersihkanErrorSaatKetik(binding.tilNikWali);
        bersihkanErrorSaatKetik(binding.tilNamaSaksi1);
        bersihkanErrorSaatKetik(binding.tilNikSaksi1);
        bersihkanErrorSaatKetik(binding.tilNamaSaksi2);
        bersihkanErrorSaatKetik(binding.tilNikSaksi2);
        bersihkanErrorSaatKetik(binding.tilAlamatAkad);
    }

    private void bersihkanErrorSaatKetik(final TextInputLayout layout) {
        if (layout.getEditText() != null) {
            layout.getEditText().addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    if (!s.toString().trim().isEmpty()) {
                        layout.setError(null);
                    }
                }
            });
        }
    }

    // Ubah ke false kalau sudah rilis. Saat true, kegagalan server/koneksi
// tetap membuka Step 5 supaya alur UI bisa diuji tanpa backend.
private static final boolean MODE_UJI_TANPA_SERVER = false;

private boolean sedangMengirim = false;

private void setupActionButtons() {
    binding.btnSimpanDraf.setOnClickListener(v -> kirimDataKeServer("draf", false));

    binding.btnLanjutDokumen.setOnClickListener(v -> {
        if (validasiForm()) {
            kirimDataKeServer("terverifikasi", true);
        }
    });

    binding.tvBantuanPetugas.setOnClickListener(v ->
            startActivity(new Intent(this, ConsultationActivity.class)));
}

private void setSedangMengirim(boolean kirim) {
    sedangMengirim = kirim;
    if (binding == null) return;
    binding.btnLanjutDokumen.setEnabled(!kirim);
    binding.btnSimpanDraf.setEnabled(!kirim);
    binding.btnLanjutDokumen.setAlpha(kirim ? 0.6f : 1f);
}

private void kirimDataKeServer(String statusSimpan, boolean lanjutKeStep5) {
    if (sedangMengirim) return;

    // 1) Ambil semua nilai di UI thread (binding tidak aman dibaca dari thread lain)
    final String postData;
    try {
        postData = "id_pendaftaran=" + idPendaftaran
                + "&status_simpan=" + URLEncoder.encode(statusSimpan, "UTF-8")
                + "&kode_tiket=" + enc(kodeTiketAktif)
                + "&tanggal_akad=" + enc(binding.etTanggalAkad.getText().toString())
                + "&waktu_akad=" + enc(binding.etWaktuAkad.getText().toString())
                + "&lokasi_akad=" + (lokasiAkadTerpilih == LOKASI_KUA ? "kua" : "luar_kua")
                + "&alamat_akad=" + enc(binding.etAlamatAkad.getText().toString())
                + "&nama_wali=" + enc(binding.etNamaWali.getText().toString())
                + "&nik_wali=" + enc(binding.etNikWali.getText().toString())
                + "&hubungan_wali=" + enc(binding.dropdownHubunganWali.getText().toString())
                + "&nama_saksi_1=" + enc(binding.etNamaSaksi1.getText().toString())
                + "&nik_saksi_1=" + enc(binding.etNikSaksi1.getText().toString())
                + "&nama_saksi_2=" + enc(binding.etNamaSaksi2.getText().toString())
                + "&nik_saksi_2=" + enc(binding.etNikSaksi2.getText().toString());
    } catch (java.io.UnsupportedEncodingException e) {
        return; // UTF-8 selalu ada, tidak akan terjadi
    }

    setSedangMengirim(true);

    new Thread(() -> {
        HttpURLConnection conn = null;
        boolean sukses = false;
        String pesan;

        try {
            conn = (HttpURLConnection) new URL(API_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded; charset=UTF-8");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(postData.getBytes("UTF-8"));
            }

            int kode = conn.getResponseCode();
            // Baca errorStream juga, supaya pesan error PHP tidak hilang
            InputStream is = (kode >= 200 && kode < 300)
                    ? conn.getInputStream() : conn.getErrorStream();
            String body = bacaStream(is);

            if (kode != HttpURLConnection.HTTP_OK) {
                pesan = "Server membalas kode " + kode;
                android.util.Log.e("Step4", "HTTP " + kode + " : " + body);
            } else {
                try {
                    org.json.JSONObject json = new org.json.JSONObject(body);
                    boolean ok = json.optBoolean("status", false)
                            || "success".equals(json.optString("status"));
                    if (ok) {
                        sukses = true;
                        pesan = "Berhasil simpan data";
                        String tiket = json.optString("kode_tiket", "");
                        if (!tiket.isEmpty()) kodeTiketAktif = tiket;
                    } else {
                        pesan = json.optString("message", "Server menolak data");
                    }
                } catch (org.json.JSONException je) {
                    // Biasanya PHP mengeluarkan warning/notice HTML sebelum JSON
                    pesan = "Respons server bukan JSON valid";
                    android.util.Log.e("Step4", "Body: " + body);
                }
            }
        } catch (java.net.SocketTimeoutException e) {
            pesan = "Koneksi timeout. Periksa jaringan/IP server.";
        } catch (java.io.IOException e) {
            pesan = "Gagal terhubung ke server. Periksa Laragon/IP.";
            android.util.Log.e("Step4", "IO error", e);
        } catch (Exception e) {
            pesan = "Terjadi kesalahan: " + e.getMessage();
            android.util.Log.e("Step4", "Error", e);
        } finally {
            if (conn != null) conn.disconnect();
        }

        final boolean fSukses = sukses;
        final String fPesan = pesan;
        runOnUiThread(() -> {
            if (isFinishing() || binding == null) return;
            setSedangMengirim(false);

            if (!kodeTiketAktif.isEmpty()) {
                binding.tvKodeTiketValue.setText(kodeTiketAktif);
            }

            if (fSukses) {
                if (lanjutKeStep5) {
                    bukaStep5();
                } else {
                    Snackbar.make(binding.getRoot(), R.string.s4_draf_saved,
                            Snackbar.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, fPesan, Toast.LENGTH_LONG).show();
                if (lanjutKeStep5 && MODE_UJI_TANPA_SERVER) {
                    bukaStep5();
                }
            }
        });
    }).start();
}

private void bukaStep5() {
    Intent intent = new Intent(this, DaftarNikahStep5Activity.class);
    intent.putExtra("id_pendaftaran", idPendaftaran);
    intent.putExtra("kode_tiket", kodeTiketAktif);
    startActivity(intent);
    finish();
}

private String enc(String nilai) throws java.io.UnsupportedEncodingException {
    return URLEncoder.encode(nilai == null ? "" : nilai.trim(), "UTF-8");
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

    private boolean validasiForm() {
        boolean valid = true;

        if (teks(binding.etNamaWali.getText().toString()).isEmpty()) {
            binding.tilNamaWali.setError(getString(R.string.s2_err_nama));
            valid = false;
        } else {
            binding.tilNamaWali.setError(null);
        }

        if (!POLA_NIK.matcher(binding.etNikWali.getText().toString().trim()).matches()) {
            binding.tilNikWali.setError(getString(R.string.s2_err_nik));
            valid = false;
        } else {
            binding.tilNikWali.setError(null);
        }

        if (binding.dropdownHubunganWali.getText().toString().trim().isEmpty()) {
            binding.tilHubunganWali.setError(getString(R.string.s4_err_hubungan));
            valid = false;
        } else {
            binding.tilHubunganWali.setError(null);
        }

        if (teks(binding.etNamaSaksi1.getText().toString()).isEmpty()) {
            binding.tilNamaSaksi1.setError(getString(R.string.s2_err_nama));
            valid = false;
        } else {
            binding.tilNamaSaksi1.setError(null);
        }

        if (!POLA_NIK.matcher(binding.etNikSaksi1.getText().toString().trim()).matches()) {
            binding.tilNikSaksi1.setError(getString(R.string.s2_err_nik));
            valid = false;
        } else {
            binding.tilNikSaksi1.setError(null);
        }

        if (teks(binding.etNamaSaksi2.getText().toString()).isEmpty()) {
            binding.tilNamaSaksi2.setError(getString(R.string.s2_err_nama));
            valid = false;
        } else {
            binding.tilNamaSaksi2.setError(null);
        }

        if (!POLA_NIK.matcher(binding.etNikSaksi2.getText().toString().trim()).matches()) {
            binding.tilNikSaksi2.setError(getString(R.string.s2_err_nik));
            valid = false;
        } else {
            binding.tilNikSaksi2.setError(null);
        }

        if (!POLA_TANGGAL.matcher(binding.etTanggalAkad.getText().toString().trim()).matches()) {
            binding.tilTanggalAkad.setError(getString(R.string.s4_err_tanggal_akad));
            valid = false;
        } else {
            binding.tilTanggalAkad.setError(null);
        }

        if (!POLA_WAKTU.matcher(binding.etWaktuAkad.getText().toString().trim()).matches()) {
            binding.tilWaktuAkad.setError(getString(R.string.s4_err_waktu_akad));
            valid = false;
        } else {
            binding.tilWaktuAkad.setError(null);
        }

        if (lokasiAkadTerpilih == LOKASI_LUAR && teks(binding.etAlamatAkad.getText().toString()).isEmpty()) {
            binding.tilAlamatAkad.setError(getString(R.string.s4_err_alamat_luar));
            valid = false;
        } else {
            binding.tilAlamatAkad.setError(null);
        }

        if (!valid) {
            Snackbar.make(binding.getRoot(), R.string.s2_err_wajib_lengkap, Snackbar.LENGTH_LONG).show();
        }
        return valid;
    }

    private String teks(String nilai) {
        return nilai == null ? "" : nilai.trim();
    }

    private int dp(float nilaiDp) {
        return Math.round(getResources().getDisplayMetrics().density * nilaiDp);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = binding.bottomNavigation;
        bottomNav.setSelectedItemId(R.id.menu_daftar_nikah);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_daftar_nikah) return true;
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
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL_SIMKAH)));
            } catch (Exception e) {
                Toast.makeText(this, R.string.daf_simkah_unavailable, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, R.string.daf_feature_soon, Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateTo(Class<?> target) {
        Intent intent = new Intent(this, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}