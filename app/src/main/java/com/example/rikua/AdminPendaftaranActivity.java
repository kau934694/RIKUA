package com.example.rikua;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.rikua.databinding.ActivityAdminPendaftaranBinding;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdminPendaftaranActivity extends AppCompatActivity {
    private static final String TAG = "AdminPendaftaran";

    private ActivityAdminPendaftaranBinding binding;
    private SessionManager sessionManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Locale localeId = Locale.forLanguageTag("id-ID");
    private final List<AdminPendaftaranItem> semuaPendaftaran = new ArrayList<>();

    private AdminPendaftaranAdapter adapter;
    private String statusTerpilih;
    private String tanggalMulai;
    private String tanggalAkhir;
    private int jumlahTotal;
    private boolean sedangMemuat;
    private boolean dataDimuat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (!penjagaSesiAdmin()) {
            return;
        }

        binding = ActivityAdminPendaftaranBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        siapkanDaftar();
        siapkanPencarian();
        siapkanFilterTanggal();
        siapkanNavigasi();
        tampilkanStatusFilter(new ArrayList<>());
        muatPendaftaran();
    }

    private boolean penjagaSesiAdmin() {
        String role = sessionManager.getRole();
        boolean admin = role != null && "admin".equalsIgnoreCase(role.trim());
        if (sessionManager.isSessionValid() && admin) {
            return true;
        }

        sessionManager.clearSession();
        Toast.makeText(this, R.string.admin_akun_ditolak, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, AdminLoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        return false;
    }

    private void siapkanDaftar() {
        adapter = new AdminPendaftaranAdapter(this::bukaDetail);
        binding.rvPendaftaran.setLayoutManager(new LinearLayoutManager(this));
        binding.rvPendaftaran.setAdapter(adapter);
        binding.btnCobaLagi.setOnClickListener(v -> muatPendaftaran());
        binding.btnKembali.setOnClickListener(v -> finish());
    }

    private void siapkanPencarian() {
        binding.etCari.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                terapkanFilter();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });
    }

    private void siapkanFilterTanggal() {
        binding.btnTanggalMulai.setOnClickListener(v -> pilihTanggal(true));
        binding.btnTanggalAkhir.setOnClickListener(v -> pilihTanggal(false));
        binding.btnHapusTanggal.setOnClickListener(v -> {
            tanggalMulai = null;
            tanggalAkhir = null;
            perbaruiLabelTanggal();
            terapkanFilter();
        });
        perbaruiLabelTanggal();
    }

    private void siapkanNavigasi() {
        binding.bottomNavAdmin.setSelectedItemId(R.id.menu_admin_pendaftaran);
        binding.bottomNavAdmin.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.menu_admin_pendaftaran) {
                return true;
            }
            if (item.getItemId() == R.id.menu_admin_dasbor) {
                finish();
                return true;
            }
            Toast.makeText(this, R.string.admin_segera_hadir,
                    Toast.LENGTH_SHORT).show();
            binding.bottomNavAdmin.setSelectedItemId(R.id.menu_admin_pendaftaran);
            return false;
        });
    }

    private void pilihTanggal(boolean pilihMulai) {
        Calendar awal = kalenderUntuk(pilihMulai ? tanggalMulai : tanggalAkhir);
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String tanggal = String.format(Locale.US, "%04d-%02d-%02d",
                            year, month + 1, dayOfMonth);
                    if (pilihMulai) {
                        if (tanggalAkhir != null && tanggal.compareTo(tanggalAkhir) > 0) {
                            Toast.makeText(this, R.string.admin_pendaftaran_rentang_tidak_valid,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        tanggalMulai = tanggal;
                    } else {
                        if (tanggalMulai != null && tanggal.compareTo(tanggalMulai) < 0) {
                            Toast.makeText(this, R.string.admin_pendaftaran_rentang_tidak_valid,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        tanggalAkhir = tanggal;
                    }
                    perbaruiLabelTanggal();
                    terapkanFilter();
                },
                awal.get(Calendar.YEAR),
                awal.get(Calendar.MONTH),
                awal.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private Calendar kalenderUntuk(String tanggal) {
        Calendar calendar = Calendar.getInstance();
        if (tanggal == null) {
            return calendar;
        }
        String[] parts = tanggal.split("-");
        if (parts.length != 3) {
            return calendar;
        }
        try {
            calendar.set(
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]) - 1,
                    Integer.parseInt(parts[2])
            );
        } catch (NumberFormatException ignored) {
            return Calendar.getInstance();
        }
        return calendar;
    }

    private void perbaruiLabelTanggal() {
        binding.btnTanggalMulai.setText(tanggalMulai == null
                ? getString(R.string.admin_pendaftaran_dari_tanggal)
                : formatLabelTanggal(tanggalMulai));
        binding.btnTanggalAkhir.setText(tanggalAkhir == null
                ? getString(R.string.admin_pendaftaran_sampai_tanggal)
                : formatLabelTanggal(tanggalAkhir));
        binding.btnHapusTanggal.setVisibility(
                tanggalMulai == null && tanggalAkhir == null ? View.GONE : View.VISIBLE);
    }

    private String formatLabelTanggal(String tanggal) {
        Calendar calendar = kalenderUntuk(tanggal);
        return new java.text.SimpleDateFormat("d MMM yyyy", localeId)
                .format(calendar.getTime());
    }

    private void siapkanChipListener() {
        binding.chipStatusGroup.setSingleSelection(true);
        binding.chipStatusGroup.setSelectionRequired(true);
        binding.chipStatusGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            Chip checked = group.findViewById(checkedIds.get(0));
            Object tag = checked == null ? null : checked.getTag();
            statusTerpilih = tag instanceof String ? (String) tag : null;
            terapkanFilter();
        });
    }

    private void tampilkanStatusFilter(List<String> statuses) {
        siapkanChipListener();
        ChipGroup group = binding.chipStatusGroup;
        group.removeAllViews();

        int chipSemuaId = tambahChip(group, getString(R.string.admin_pendaftaran_semua), null);
        Set<String> unik = new LinkedHashSet<>(statuses);
        for (String status : unik) {
            if (status != null && !status.trim().isEmpty()) {
                tambahChip(group, labelStatus(status), status);
            }
        }

        boolean statusMasihAda = false;
        if (statusTerpilih != null) {
            for (int i = 1; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (statusTerpilih.equals(child.getTag())) {
                    group.check(child.getId());
                    statusMasihAda = true;
                    break;
                }
            }
        }
        if (!statusMasihAda) {
            statusTerpilih = null;
            group.check(chipSemuaId);
        }
    }

    private int tambahChip(ChipGroup group, String label, String status) {
        Chip chip = new Chip(this);
        chip.setId(View.generateViewId());
        chip.setText(label);
        chip.setTag(status);
        chip.setCheckable(true);
        chip.setClickable(true);
        chip.setEnsureMinTouchTargetSize(true);
        chip.setMinHeight(dp(44));

        int selected = ContextCompat.getColor(this, R.color.primary_forest);
        int unselected = ContextCompat.getColor(this, R.color.surface_white);
        int white = ContextCompat.getColor(this, R.color.surface_white);
        int text = ContextCompat.getColor(this, R.color.text_primary);
        chip.setChipBackgroundColor(new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{selected, unselected}
        ));
        chip.setTextColor(new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{white, text}
        ));
        chip.setChipStrokeColor(ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.primary_forest)));
        chip.setChipStrokeWidth(dp(1));
        chip.setRippleColor(ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.primary_forest_container)));
        group.addView(chip);
        return chip.getId();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String labelStatus(String status) {
        String label = status.replace('_', ' ').trim();
        if (label.isEmpty()) {
            return label;
        }
        return label.substring(0, 1).toUpperCase(localeId) + label.substring(1);
    }

    private void muatPendaftaran() {
        sedangMemuat = true;
        dataDimuat = false;
        tampilkanLoading();
        adapter.submitList(new ArrayList<>());
        semuaPendaftaran.clear();
        jumlahTotal = 0;

        executor.execute(() -> {
            HttpURLConnection connection = null;
            List<AdminPendaftaranItem> hasil = new ArrayList<>();
            List<String> statuses = new ArrayList<>();
            String galat = null;
            boolean sesiDitolak = false;
            boolean aksesDitolak = false;
            int total = 0;

            try {
                connection = (HttpURLConnection)
                        new URL(ApiConfig.ADMIN_PENDAFTARAN).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setRequestProperty("Accept", "application/json");

                String token = sessionManager.getToken();
                boolean tokenPresent = token != null && !token.trim().isEmpty();
                if (tokenPresent) {
                    connection.setRequestProperty("Authorization", "Bearer " + token);
                }

                int kode = connection.getResponseCode();
                Log.i(TAG, "Registration API response HTTP " + kode
                        + ", bearerTokenSent=" + tokenPresent);
                if (kode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    sesiDitolak = true;
                } else if (kode == HttpURLConnection.HTTP_FORBIDDEN) {
                    aksesDitolak = true;
                    galat = getString(R.string.admin_akses_ditolak);
                } else if (kode != HttpURLConnection.HTTP_OK) {
                    galat = getString(R.string.admin_pendaftaran_gagal_memuat);
                    Log.e(TAG, "Registration API returned HTTP " + kode);
                } else {
                    String body = bacaStream(connection.getInputStream());
                    JSONObject root = new JSONObject(body);
                    if (!root.optBoolean("success", false)) {
                        throw new JSONException("API tidak mengembalikan success=true");
                    }
                    JSONArray rows = root.optJSONArray("data");
                    if (rows == null) {
                        throw new JSONException("Array data pendaftaran tidak tersedia");
                    }
                    total = root.optInt("total", -1);
                    if (total != rows.length()) {
                        throw new JSONException("Jumlah data tidak sesuai dengan respons API");
                    }

                    for (int i = 0; i < rows.length(); i++) {
                        JSONObject row = rows.optJSONObject(i);
                        if (row == null) {
                            throw new JSONException("Baris pendaftaran tidak valid");
                        }
                        AdminPendaftaranItem item = AdminPendaftaranItem.fromJson(row);
                        hasil.add(item);
                        if (!statuses.contains(item.getStatus())) {
                            statuses.add(item.getStatus());
                        }
                    }
                }
            } catch (SocketTimeoutException e) {
                galat = getString(R.string.admin_pendaftaran_gagal_memuat);
                Log.e(TAG, "Registration API timed out", e);
            } catch (IOException e) {
                galat = getString(R.string.admin_pendaftaran_gagal_memuat);
                Log.e(TAG, "Registration API connection failed", e);
            } catch (Exception e) {
                galat = getString(R.string.admin_pendaftaran_gagal_memuat);
                Log.e(TAG, "Registration API response could not be read", e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            final List<AdminPendaftaranItem> fHasil = hasil;
            final List<String> fStatuses = statuses;
            final String fGalat = galat;
            final boolean fSesiDitolak = sesiDitolak;
            final boolean fAksesDitolak = aksesDitolak;
            final int fTotal = total;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed() || binding == null) {
                    return;
                }
                sedangMemuat = false;
                if (fSesiDitolak) {
                    tanganiSesiDitolak();
                } else if (fGalat != null) {
                    tampilkanError(fGalat);
                    if (fAksesDitolak) {
                        Log.w(TAG, "Registration API returned HTTP 403");
                    }
                } else {
                    dataDimuat = true;
                    semuaPendaftaran.clear();
                    semuaPendaftaran.addAll(fHasil);
                    jumlahTotal = fTotal;
                    tampilkanStatusFilter(fStatuses);
                    terapkanFilter();
                }
            });
        });
    }

    private void terapkanFilter() {
        if (binding == null || adapter == null || sedangMemuat || !dataDimuat) {
            return;
        }
        String query = binding.etCari.getText() == null
                ? "" : binding.etCari.getText().toString();
        List<AdminPendaftaranItem> hasil = new ArrayList<>();
        for (AdminPendaftaranItem item : semuaPendaftaran) {
            if (item.matchesFilters(query, statusTerpilih, tanggalMulai, tanggalAkhir)) {
                hasil.add(item);
            }
        }

        adapter.submitList(hasil);
        binding.tvJumlahHasil.setText(getString(
                R.string.admin_pendaftaran_jumlah_hasil,
                hasil.size(),
                jumlahTotal
        ));
        boolean kosong = hasil.isEmpty();
        binding.rvPendaftaran.setVisibility(kosong ? View.GONE : View.VISIBLE);
        binding.layoutKosong.setVisibility(kosong ? View.VISIBLE : View.GONE);
        if (kosong) {
            binding.tvKosong.setText(semuaPendaftaran.isEmpty()
                    ? R.string.admin_pendaftaran_kosong
                    : R.string.admin_pendaftaran_tidak_ada_hasil);
        }
        binding.layoutLoading.setVisibility(View.GONE);
        binding.layoutError.setVisibility(View.GONE);
    }

    private void tampilkanLoading() {
        binding.tvJumlahHasil.setText("");
        binding.layoutLoading.setVisibility(View.VISIBLE);
        binding.layoutError.setVisibility(View.GONE);
        binding.layoutKosong.setVisibility(View.GONE);
        binding.rvPendaftaran.setVisibility(View.GONE);
    }

    private void tampilkanError(String pesan) {
        adapter.submitList(new ArrayList<>());
        binding.layoutLoading.setVisibility(View.GONE);
        binding.layoutKosong.setVisibility(View.GONE);
        binding.rvPendaftaran.setVisibility(View.GONE);
        binding.layoutError.setVisibility(View.VISIBLE);
        binding.tvError.setText(pesan);
    }

    private void tanganiSesiDitolak() {
        Log.w(TAG, "Registration API rejected bearer token; clearing session");
        sessionManager.clearSession();
        Toast.makeText(this, R.string.admin_akun_ditolak, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, AdminLoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void bukaDetail(AdminPendaftaranItem item) {
        Intent intent = new Intent(this, AdminPendaftaranDetailActivity.class);
        intent.putExtra(AdminPendaftaranDetailActivity.EXTRA_TIKET, item.getKodeTiket());
        intent.putExtra(AdminPendaftaranDetailActivity.EXTRA_STATUS, item.getStatus());
        intent.putExtra(AdminPendaftaranDetailActivity.EXTRA_TANGGAL_DAFTAR,
                item.formatTanggalDaftar(localeId));
        intent.putExtra(AdminPendaftaranDetailActivity.EXTRA_NAMA_SUAMI, item.getNamaSuami());
        intent.putExtra(AdminPendaftaranDetailActivity.EXTRA_NAMA_ISTRI, item.getNamaIstri());
        intent.putExtra(AdminPendaftaranDetailActivity.EXTRA_TANGGAL_AKAD,
                item.formatTanggalAkad(localeId));
        intent.putExtra(AdminPendaftaranDetailActivity.EXTRA_WAKTU_AKAD,
                item.formatWaktuAkad(localeId));
        intent.putExtra(AdminPendaftaranDetailActivity.EXTRA_LOKASI, item.getLokasiAkad());
        startActivity(intent);
    }

    private String bacaStream(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        return body.toString();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        binding = null;
        super.onDestroy();
    }
}
