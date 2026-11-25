package com.example.rikua;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdminSaranaActivity extends AppCompatActivity {
    private static final int MAX_FOTO_BYTES = 5 * 1024 * 1024;
    private static final String URL_FOTO_SARANA =
            "http://192.168.1.6/rikua_api/uploads/";

    private com.example.rikua.databinding.ActivityAdminSaranaBinding binding;
    private SessionManager sessionManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<AdminSaranaItem> semuaSarana = new ArrayList<>();
    private final List<String> jenisData = new ArrayList<>();
    private final List<DesaItem> desaData = new ArrayList<>();
    private AdminSaranaAdapter adapter;
    private String jenisTerpilih;
    private Integer desaTerpilih;
    private String fotoBase64Terpilih;
    private boolean hapusFotoTerpilih;
    private ImageView fotoPreviewView;
    private TextView fotoStatusView;
    private MaterialButton fotoRemoveView;

    private final ActivityResultLauncher<String> pilihFotoLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) {
                    return;
                }
                if (!formatFotoDidukung(uri)) {
                    Toast.makeText(this, R.string.admin_sarana_foto_format,
                            Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    byte[] dataFoto = bacaFoto(uri);
                    Bitmap gambar = decodeFoto(dataFoto);
                    if (gambar == null) {
                        throw new Exception(getString(R.string.admin_sarana_foto_format));
                    }
                    byte[] fotoTerkompresi;
                    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                        if (!gambar.compress(Bitmap.CompressFormat.JPEG, 82, output)) {
                            throw new Exception(getString(R.string.admin_sarana_foto_gagal_proses));
                        }
                        fotoTerkompresi = output.toByteArray();
                    }
                    if (fotoTerkompresi.length > MAX_FOTO_BYTES) {
                        throw new Exception(getString(R.string.admin_sarana_foto_ukuran));
                    }
                    fotoBase64Terpilih = Base64.encodeToString(
                            fotoTerkompresi, Base64.NO_WRAP);
                    hapusFotoTerpilih = false;
                    if (fotoPreviewView != null) {
                        fotoPreviewView.setImageBitmap(gambar);
                        fotoPreviewView.setPadding(0, 0, 0, 0);
                        fotoPreviewView.setImageTintList(null);
                        fotoPreviewView.setColorFilter(null);
                    }
                    if (fotoStatusView != null) {
                        fotoStatusView.setText(R.string.admin_sarana_foto_terpilih);
                    }
                    if (fotoRemoveView != null) {
                        fotoRemoveView.setVisibility(View.VISIBLE);
                    }
                } catch (Exception e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(this);
        if (!penjagaSesiAdmin()) {
            return;
        }

        binding = com.example.rikua.databinding.ActivityAdminSaranaBinding.inflate(
                getLayoutInflater());
        setContentView(binding.getRoot());

        siapkanDaftar();
        siapkanPencarian();
        siapkanNavigasi();
        binding.btnKembali.setOnClickListener(v -> finish());
        binding.btnCobaLagi.setOnClickListener(v -> muatSarana());
        binding.fabTambahSarana.setOnClickListener(v -> bukaForm(null));
        muatSarana();
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
        adapter = new AdminSaranaAdapter(new AdminSaranaAdapter.SaranaActionListener() {
            @Override
            public void onEdit(AdminSaranaItem item) {
                bukaForm(item);
            }

            @Override
            public void onDelete(AdminSaranaItem item) {
                konfirmasiHapus(item);
            }
        });
        binding.rvSarana.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSarana.setAdapter(adapter);
    }

    private void siapkanPencarian() {
        binding.etCari.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                terapkanFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void siapkanNavigasi() {
        binding.bottomNavAdmin.setSelectedItemId(R.id.menu_admin_sarana);
        binding.bottomNavAdmin.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.menu_admin_sarana) {
                return true;
            }
            if (item.getItemId() == R.id.menu_admin_dasbor) {
                finish();
                return true;
            }
            if (item.getItemId() == R.id.menu_admin_pendaftaran) {
                startActivity(new Intent(this, AdminPendaftaranActivity.class));
                finish();
                return true;
            }
            Toast.makeText(this, R.string.admin_segera_hadir, Toast.LENGTH_SHORT).show();
            binding.bottomNavAdmin.setSelectedItemId(R.id.menu_admin_sarana);
            return false;
        });
    }

    private void muatSarana() {
        tampilkanMuat();
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(ApiConfig.ADMIN_GET_SARANA)
                        .openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Authorization",
                        "Bearer " + sessionManager.getToken());

                int code = connection.getResponseCode();
                String response = bacaRespons(connection, code >= 400);
                JSONObject json = new JSONObject(response);
                if (code == 401) {
                    runOnUiThread(this::kelolaSesiTidakValid);
                    return;
                }
                if (code == 403) {
                    runOnUiThread(() -> tampilkanAksesDitolak(
                            json.optString("message", getString(R.string.admin_akses_ditolak))));
                    return;
                }
                if (code < 200 || code >= 300 || !json.optBoolean("success", false)) {
                    throw new Exception(json.optString("message",
                            getString(R.string.admin_sarana_gagal)));
                }

                List<AdminSaranaItem> hasil = parseSarana(json.optJSONArray("data"));
                jenisData.clear();
                JSONArray jenis = json.optJSONArray("jenis");
                if (jenis != null) {
                    for (int i = 0; i < jenis.length(); i++) {
                        jenisData.add(jenis.optString(i));
                    }
                }
                desaData.clear();
                JSONArray desa = json.optJSONArray("desa");
                if (desa != null) {
                    for (int i = 0; i < desa.length(); i++) {
                        JSONObject row = desa.optJSONObject(i);
                        if (row != null) {
                            desaData.add(new DesaItem(
                                    row.optInt("id_desa"),
                                    row.optString("nama_desa")));
                        }
                    }
                }

                runOnUiThread(() -> tampilkanHasil(hasil));
            } catch (Exception e) {
                runOnUiThread(() -> tampilkanError(e.getMessage()));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private List<AdminSaranaItem> parseSarana(JSONArray array) {
        List<AdminSaranaItem> hasil = new ArrayList<>();
        if (array == null) {
            return hasil;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject row = array.optJSONObject(i);
            if (row == null) {
                continue;
            }
            Double latitude = row.isNull("latitude")
                    ? null : row.optDouble("latitude");
            Double longitude = row.isNull("longitude")
                    ? null : row.optDouble("longitude");
            hasil.add(new AdminSaranaItem(
                    row.optInt("id_sarana"),
                    row.optString("jenis"),
                    row.optString("nama_sarana"),
                    row.optString("alamat"),
                    row.optInt("id_desa"),
                    row.optString("nama_desa"),
                    row.optString("nama_pengurus"),
                    row.optString("kontak"),
                    row.optInt("jumlah_anggota"),
                    row.optString("foto"),
                    latitude,
                    longitude));
        }
        return hasil;
    }

    private void tampilkanHasil(List<AdminSaranaItem> hasil) {
        semuaSarana.clear();
        semuaSarana.addAll(hasil);
        siapkanSpinnerJenis();
        siapkanSpinnerDesa();
        binding.layoutLoading.setVisibility(View.GONE);
        binding.layoutError.setVisibility(View.GONE);
        binding.layoutEmpty.setVisibility(View.GONE);
        binding.rvSarana.setVisibility(View.VISIBLE);
        binding.tvJumlahSarana.setText(getString(
                R.string.admin_sarana_jumlah_total, semuaSarana.size()));
        terapkanFilter();
    }

    private void siapkanSpinnerJenis() {
        List<String> pilihan = new ArrayList<>();
        pilihan.add(getString(R.string.admin_sarana_semua_jenis));
        for (String jenis : jenisData) {
            pilihan.add(labelJenis(jenis));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, pilihan);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFilterJenis.setAdapter(adapter);
        binding.spinnerFilterJenis.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view,
                                               int position, long id) {
                        jenisTerpilih = position == 0 ? null : jenisData.get(position - 1);
                        terapkanFilter();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
    }

    private void siapkanSpinnerDesa() {
        List<String> pilihan = new ArrayList<>();
        pilihan.add(getString(R.string.admin_sarana_semua_desa));
        for (DesaItem desa : desaData) {
            pilihan.add(desa.nama);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, pilihan);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFilterDesa.setAdapter(adapter);
        binding.spinnerFilterDesa.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view,
                                               int position, long id) {
                        desaTerpilih = position == 0 ? null : desaData.get(position - 1).id;
                        terapkanFilter();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
    }

    private void terapkanFilter() {
        String query = binding.etCari.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<AdminSaranaItem> hasil = new ArrayList<>();
        for (AdminSaranaItem item : semuaSarana) {
            boolean cocokJenis = jenisTerpilih == null
                    || jenisTerpilih.equalsIgnoreCase(item.getJenis());
            boolean cocokDesa = desaTerpilih == null
                    || desaTerpilih == item.getIdDesa();
            boolean cocokCari = query.isEmpty()
                    || item.getNamaSarana().toLowerCase(Locale.ROOT).contains(query)
                    || item.getAlamat().toLowerCase(Locale.ROOT).contains(query)
                    || item.getNamaDesa().toLowerCase(Locale.ROOT).contains(query);
            if (cocokJenis && cocokDesa && cocokCari) {
                hasil.add(item);
            }
        }
        adapter.submitList(hasil);
        binding.layoutEmpty.setVisibility(hasil.isEmpty() ? View.VISIBLE : View.GONE);
        binding.rvSarana.setVisibility(hasil.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void bukaForm(AdminSaranaItem item) {
        View form = LayoutInflater.from(this).inflate(
                R.layout.dialog_admin_sarana, binding.getRoot(), false);
        TextView tvFormTitle = form.findViewById(R.id.tv_form_title);
        EditText etNama = form.findViewById(R.id.et_nama_sarana);
        EditText etAlamat = form.findViewById(R.id.et_alamat_sarana);
        EditText etPengurus = form.findViewById(R.id.et_pengurus_sarana);
        EditText etKontak = form.findViewById(R.id.et_kontak_sarana);
        EditText etJumlah = form.findViewById(R.id.et_jumlah_sarana);
        EditText etKoordinat = form.findViewById(R.id.et_koordinat_sarana);
        Spinner spJenis = form.findViewById(R.id.spinner_jenis_sarana);
        Spinner spDesa = form.findViewById(R.id.spinner_desa_sarana);
        ImageView ivFoto = form.findViewById(R.id.iv_preview_foto);
        ivFoto.setClipToOutline(true);
        TextView tvFoto = form.findViewById(R.id.tv_status_foto);
        MaterialButton btnPilihFoto = form.findViewById(R.id.btn_pilih_foto);
        MaterialButton btnHapusFoto = form.findViewById(R.id.btn_hapus_foto);
        MaterialButton btnBatal = form.findViewById(R.id.btn_batal_form);
        MaterialButton btnSimpan = form.findViewById(R.id.btn_simpan_form);
        ProgressBar progressSimpan = form.findViewById(R.id.progress_simpan_form);
        View btnTutup = form.findViewById(R.id.btn_tutup_form);

        terapkanTandaWajib(form, R.id.tv_label_nama_sarana);
        terapkanTandaWajib(form, R.id.tv_label_jenis_sarana);
        terapkanTandaWajib(form, R.id.tv_label_desa_sarana);
        terapkanTandaWajib(form, R.id.tv_label_alamat_sarana);
        terapkanTandaWajib(form, R.id.tv_label_pengurus_sarana);
        terapkanTandaWajib(form, R.id.tv_label_kontak_sarana);

        fotoPreviewView = ivFoto;
        fotoStatusView = tvFoto;
        fotoRemoveView = btnHapusFoto;
        fotoBase64Terpilih = null;
        hapusFotoTerpilih = false;

        List<String> pilihanJenis = new ArrayList<>();
        pilihanJenis.add(getString(R.string.admin_sarana_pilih_jenis));
        for (String jenis : jenisData) {
            pilihanJenis.add(labelJenis(jenis));
        }
        ArrayAdapter<String> jenisAdapter = new ArrayAdapter<>(
                this, R.layout.item_spinner_admin_sarana, pilihanJenis);
        jenisAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown_admin_sarana);
        spJenis.setAdapter(jenisAdapter);

        List<String> namaDesa = new ArrayList<>();
        namaDesa.add(getString(R.string.admin_sarana_pilih_desa));
        for (DesaItem desa : desaData) {
            namaDesa.add(desa.nama);
        }
        ArrayAdapter<String> desaAdapter = new ArrayAdapter<>(
                this, R.layout.item_spinner_admin_sarana, namaDesa);
        desaAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown_admin_sarana);
        spDesa.setAdapter(desaAdapter);

        if (item != null) {
            tvFormTitle.setText(R.string.admin_sarana_edit_judul);
            etNama.setText(item.getNamaSarana());
            etAlamat.setText(item.getAlamat());
            etPengurus.setText(item.getNamaPengurus());
            etKontak.setText(item.getKontak());
            if (item.getJumlahAnggota() > 0) {
                etJumlah.setText(String.valueOf(item.getJumlahAnggota()));
            }
            if (item.getLatitude() != null && item.getLongitude() != null) {
                etKoordinat.setText(String.format(Locale.US, "%.7f, %.7f",
                        item.getLatitude(), item.getLongitude()));
            }
            tampilkanFotoSaatIni(item, ivFoto, tvFoto, btnHapusFoto);
            pilihJenis(spJenis, item.getJenis());
            pilihDesa(spDesa, item.getIdDesa());
        }

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(form);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.getBehavior().setFitToContents(true);
        dialog.getBehavior().setSkipCollapsed(true);
        btnSimpan.setText(item == null
                ? R.string.admin_sarana_simpan_data
                : R.string.admin_sarana_simpan_perubahan);

        String[] nilaiAwal = {
                etNama.getText().toString(),
                etAlamat.getText().toString(),
                etPengurus.getText().toString(),
                etKontak.getText().toString(),
                etJumlah.getText().toString(),
                etKoordinat.getText().toString()
        };
        int jenisAwal = spJenis.getSelectedItemPosition();
        int desaAwal = spDesa.getSelectedItemPosition();
        Runnable tutupForm = () -> {
            boolean berubah = !nilaiAwal[0].equals(etNama.getText().toString())
                    || !nilaiAwal[1].equals(etAlamat.getText().toString())
                    || !nilaiAwal[2].equals(etPengurus.getText().toString())
                    || !nilaiAwal[3].equals(etKontak.getText().toString())
                    || !nilaiAwal[4].equals(etJumlah.getText().toString())
                    || !nilaiAwal[5].equals(etKoordinat.getText().toString())
                    || jenisAwal != spJenis.getSelectedItemPosition()
                    || desaAwal != spDesa.getSelectedItemPosition()
                    || fotoBase64Terpilih != null || hapusFotoTerpilih;
            if (!berubah) {
                dialog.dismiss();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle(R.string.admin_sarana_perubahan_judul)
                    .setMessage(R.string.admin_sarana_perubahan_pesan)
                    .setNegativeButton(R.string.admin_sarana_tetap_edit, null)
                    .setPositiveButton(R.string.admin_sarana_buang_perubahan,
                            (confirmation, which) -> dialog.dismiss())
                    .show();
        };
        btnTutup.setOnClickListener(v -> tutupForm.run());
        btnBatal.setOnClickListener(v -> tutupForm.run());
        dialog.setOnKeyListener((d, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                tutupForm.run();
                return true;
            }
            return false;
        });
        dialog.setOnShowListener(ignored -> {
            dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
            dialog.getBehavior().setPeekHeight(getResources().getDisplayMetrics().heightPixels);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }
        });
        dialog.setOnDismissListener(ignored -> {
            fotoPreviewView = null;
            fotoStatusView = null;
            fotoRemoveView = null;
        });
        btnPilihFoto.setOnClickListener(v -> pilihFotoLauncher.launch("image/*"));
        btnHapusFoto.setOnClickListener(v -> {
            if (fotoBase64Terpilih != null) {
                fotoBase64Terpilih = null;
                hapusFotoTerpilih = false;
                tampilkanFotoSaatIni(item, ivFoto, tvFoto, btnHapusFoto);
            } else {
                hapusFotoTerpilih = item != null
                        && item.getFoto() != null && !item.getFoto().trim().isEmpty();
                ivFoto.setImageResource(R.drawable.ic_upload);
                ivFoto.setPadding(30, 30, 30, 30);
                ivFoto.setImageTintList(ColorStateList.valueOf(getColor(R.color.admin_teal)));
                ivFoto.setColorFilter(getColor(R.color.admin_teal));
                tvFoto.setText(hapusFotoTerpilih
                        ? R.string.admin_sarana_foto_dihapus
                        : R.string.admin_sarana_foto_belum);
                btnHapusFoto.setVisibility(View.GONE);
            }
        });
        btnSimpan.setOnClickListener(v -> {
            String nama = etNama.getText().toString().trim();
            String alamat = etAlamat.getText().toString().trim();
            String pengurus = etPengurus.getText().toString().trim();
            String kontak = etKontak.getText().toString().trim();
            String jumlahText = etJumlah.getText().toString().trim();
            String koordinatText = etKoordinat.getText().toString().trim();
            DesaItem desa = pilihanDesa(spDesa);
            int posisiJenis = spJenis.getSelectedItemPosition();
            if (nama.isEmpty()) {
                etNama.setError(getString(R.string.admin_sarana_validasi));
                etNama.requestFocus();
                return;
            }
            if (posisiJenis <= 0 || posisiJenis > jenisData.size()) {
                Toast.makeText(this, R.string.admin_sarana_validasi, Toast.LENGTH_LONG).show();
                return;
            }
            if (desa == null) {
                Toast.makeText(this, R.string.admin_sarana_validasi, Toast.LENGTH_LONG).show();
                return;
            }
            if (alamat.isEmpty()) {
                etAlamat.setError(getString(R.string.admin_sarana_validasi));
                etAlamat.requestFocus();
                return;
            }
            if (pengurus.isEmpty()) {
                etPengurus.setError(getString(R.string.admin_sarana_validasi));
                etPengurus.requestFocus();
                return;
            }
            if (!nomorTeleponValid(kontak)) {
                etKontak.setError(getString(R.string.admin_sarana_kontak_invalid));
                etKontak.requestFocus();
                return;
            }
            int jumlah = 0;
            if (!jumlahText.isEmpty()) {
                try {
                    jumlah = Integer.parseInt(jumlahText);
                    if (jumlah <= 0) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException e) {
                    etJumlah.setError(getString(R.string.admin_sarana_jumlah_invalid));
                    etJumlah.requestFocus();
                    return;
                }
            }
            Object[] koordinat;
            try {
                koordinat = parseKoordinat(koordinatText);
            } catch (IllegalArgumentException e) {
                etKoordinat.setError(getString(R.string.admin_sarana_gps_invalid));
                etKoordinat.requestFocus();
                return;
            }

            JSONObject body = new JSONObject();
            try {
                if (item != null) {
                    body.put("id_sarana", item.getId());
                }
                body.put("nama_sarana", nama);
                body.put("jenis", jenisData.get(posisiJenis - 1));
                body.put("id_desa", desa.id);
                body.put("alamat", alamat);
                body.put("nama_pengurus", pengurus);
                body.put("kontak", kontak);
                body.put("jumlah_anggota", jumlah);
                body.put("latitude", koordinat[0]);
                body.put("longitude", koordinat[1]);
                if (fotoBase64Terpilih != null) {
                    body.put("foto_base64", fotoBase64Terpilih);
                    body.put("foto_nama", "sarana.jpg");
                }
                if (hapusFotoTerpilih) {
                    body.put("hapus_foto", true);
                }
            } catch (Exception e) {
                Toast.makeText(this, "Data sarana tidak dapat disiapkan: "
                        + e.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }
            aturStatusSimpan(btnSimpan, progressSimpan, true, item != null);
            kirimPerubahan(item == null ? "POST" : "PUT",
                    item == null ? ApiConfig.ADMIN_CREATE_SARANA
                            : ApiConfig.ADMIN_UPDATE_SARANA,
                    body,
                    item == null ? "Sarana berhasil ditambahkan."
                            : "Sarana berhasil diperbarui.",
                    dialog, btnSimpan, progressSimpan, item != null);
        });
        dialog.show();
    }

    private void tampilkanFotoSaatIni(@Nullable AdminSaranaItem item, ImageView preview,
                                      TextView status, MaterialButton tombolHapus) {
        String namaFoto = item == null ? null : item.getFoto();
        if (namaFoto != null && !namaFoto.trim().isEmpty()) {
            String foto = namaFoto.trim();
            String fotoUrl = foto.startsWith("http://") || foto.startsWith("https://")
                    ? foto : URL_FOTO_SARANA + Uri.encode(foto, "/");
            preview.setImageTintList(null);
            preview.setColorFilter(null);
            Glide.with(this)
                    .load(fotoUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_upload)
                    .error(R.drawable.ic_upload)
                    .into(preview);
            preview.setPadding(0, 0, 0, 0);
            status.setText(R.string.admin_sarana_foto_lama);
            tombolHapus.setVisibility(View.VISIBLE);
            return;
        }
        preview.setImageResource(R.drawable.ic_upload);
        preview.setPadding(30, 30, 30, 30);
        preview.setImageTintList(ColorStateList.valueOf(getColor(R.color.admin_teal)));
        preview.setColorFilter(getColor(R.color.admin_teal));
        status.setText(R.string.admin_sarana_foto_belum);
        tombolHapus.setVisibility(View.GONE);
    }

    private void pilihJenis(Spinner spinner, String jenis) {
        for (int i = 0; i < jenisData.size(); i++) {
            if (jenisData.get(i).equalsIgnoreCase(jenis)) {
                spinner.setSelection(i + 1);
                return;
            }
        }
    }

    private void pilihDesa(Spinner spinner, int idDesa) {
        for (int i = 0; i < desaData.size(); i++) {
            if (desaData.get(i).id == idDesa) {
                spinner.setSelection(i + 1);
                return;
            }
        }
    }

    private DesaItem pilihanDesa(Spinner spinner) {
        int position = spinner.getSelectedItemPosition() - 1;
        return position >= 0 && position < desaData.size()
                ? desaData.get(position) : null;
    }

    private void kirimPerubahan(String method, String endpoint, JSONObject body,
                                String pesanSukses, BottomSheetDialog dialog,
                                MaterialButton btnSimpan, ProgressBar progressSimpan,
                                boolean modeEdit) {
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(endpoint).openConnection();
                connection.setRequestMethod(method);
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Authorization",
                        "Bearer " + sessionManager.getToken());
                byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(bytes);
                }

                int code = connection.getResponseCode();
                JSONObject json = new JSONObject(bacaRespons(connection, code >= 400));
                if (code == 401) {
                    runOnUiThread(this::kelolaSesiTidakValid);
                    return;
                }
                if (code == 403) {
                    runOnUiThread(() -> {
                        aturStatusSimpan(btnSimpan, progressSimpan, false, modeEdit);
                        Toast.makeText(this,
                                json.optString("message", getString(R.string.admin_akses_ditolak)),
                                Toast.LENGTH_LONG).show();
                    });
                    return;
                }
                if (code < 200 || code >= 300 || !json.optBoolean("success", false)) {
                    throw new Exception(json.optString("message",
                            "Perubahan sarana gagal disimpan."));
                }
                runOnUiThread(() -> {
                    dialog.dismiss();
                    Toast.makeText(this, pesanSukses, Toast.LENGTH_SHORT).show();
                    muatSarana();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    aturStatusSimpan(btnSimpan, progressSimpan, false, modeEdit);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void konfirmasiHapus(AdminSaranaItem item) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.admin_sarana_hapus_judul)
                .setMessage(getString(R.string.admin_sarana_hapus_pesan, item.getNamaSarana()))
                .setNegativeButton(R.string.admin_sarana_batal, null)
                .setPositiveButton(R.string.admin_sarana_hapus,
                        (dialog, which) -> hapusSarana(item))
                .show();
    }

    private void hapusSarana(AdminSaranaItem item) {
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                JSONObject body = new JSONObject();
                body.put("id_sarana", item.getId());
                connection = (HttpURLConnection) new URL(ApiConfig.ADMIN_DELETE_SARANA)
                        .openConnection();
                connection.setRequestMethod("DELETE");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Authorization",
                        "Bearer " + sessionManager.getToken());
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = connection.getResponseCode();
                JSONObject json = new JSONObject(bacaRespons(connection, code >= 400));
                if (code == 401) {
                    runOnUiThread(this::kelolaSesiTidakValid);
                    return;
                }
                if (code == 403) {
                    runOnUiThread(() -> Toast.makeText(this,
                            json.optString("message", getString(R.string.admin_akses_ditolak)),
                            Toast.LENGTH_LONG).show());
                    return;
                }
                if (code < 200 || code >= 300 || !json.optBoolean("success", false)) {
                    throw new Exception(json.optString("message",
                            "Sarana gagal dihapus."));
                }
                runOnUiThread(() -> {
                    Toast.makeText(this, "Sarana berhasil dihapus.", Toast.LENGTH_SHORT).show();
                    muatSarana();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, e.getMessage(),
                        Toast.LENGTH_LONG).show());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void tampilkanMuat() {
        binding.layoutLoading.setVisibility(View.VISIBLE);
        binding.layoutError.setVisibility(View.GONE);
        binding.layoutEmpty.setVisibility(View.GONE);
        binding.rvSarana.setVisibility(View.GONE);
    }

    private void tampilkanError(String message) {
        binding.layoutLoading.setVisibility(View.GONE);
        binding.layoutEmpty.setVisibility(View.GONE);
        binding.rvSarana.setVisibility(View.GONE);
        binding.layoutError.setVisibility(View.VISIBLE);
        binding.tvError.setText(message == null || message.trim().isEmpty()
                ? getString(R.string.admin_sarana_gagal) : message);
    }

    private void tampilkanAksesDitolak(String message) {
        binding.layoutLoading.setVisibility(View.GONE);
        binding.layoutEmpty.setVisibility(View.GONE);
        binding.rvSarana.setVisibility(View.GONE);
        binding.layoutError.setVisibility(View.VISIBLE);
        binding.tvError.setText(message);
    }

    private void kelolaSesiTidakValid() {
        sessionManager.clearSession();
        Toast.makeText(this, R.string.admin_akun_ditolak, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, AdminLoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String labelJenis(String jenis) {
        if (jenis == null || jenis.trim().isEmpty()) {
            return "-";
        }
        String value = jenis.trim();
        if ("tpq".equalsIgnoreCase(value)) {
            return "TPQ";
        }
        return value.substring(0, 1).toUpperCase(Locale.forLanguageTag("id-ID"))
                + value.substring(1);
    }

    private Bitmap ubahUkuranFoto(Bitmap bitmap, int lebarMaksimum) {
        int lebar = bitmap.getWidth();
        int tinggi = bitmap.getHeight();
        if (lebar <= lebarMaksimum && tinggi <= lebarMaksimum) {
            return bitmap;
        }
        float skala = Math.min(
                (float) lebarMaksimum / lebar,
                (float) lebarMaksimum / tinggi);
        int lebarBaru = Math.round(lebar * skala);
        int tinggiBaru = Math.round(tinggi * skala);
        Bitmap hasil = Bitmap.createScaledBitmap(
                bitmap, lebarBaru, tinggiBaru, true);
        if (hasil != bitmap) {
            bitmap.recycle();
        }
        return hasil;
    }

    private void terapkanTandaWajib(View form, int idLabel) {
        TextView label = form.findViewById(idLabel);
        SpannableString teks = new SpannableString(label.getText() + " *");
        teks.setSpan(new ForegroundColorSpan(getColor(R.color.badge_rose_text)),
                teks.length() - 1, teks.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        label.setText(teks);
    }

    private boolean nomorTeleponValid(String nomor) {
        if (nomor == null || !nomor.matches("[0-9+() .-]{8,20}")) {
            return false;
        }
        String digit = nomor.replaceAll("[^0-9]", "");
        return digit.length() >= 9 && digit.length() <= 15;
    }

    private void aturStatusSimpan(MaterialButton button, ProgressBar progress,
                                  boolean menyimpan, boolean modeEdit) {
        button.setEnabled(!menyimpan);
        progress.setVisibility(menyimpan ? View.VISIBLE : View.GONE);
        button.setText(menyimpan ? R.string.admin_sarana_menyimpan
                : modeEdit ? R.string.admin_sarana_simpan_perubahan
                : R.string.admin_sarana_simpan_data);
        button.setIcon(menyimpan ? null
                : AppCompatResources.getDrawable(this, R.drawable.ic_save));
    }

    private boolean formatFotoDidukung(Uri uri) {
        String mime = getContentResolver().getType(uri);
        if (mime != null) {
            return "image/jpeg".equalsIgnoreCase(mime)
                    || "image/png".equalsIgnoreCase(mime);
        }
        String name = uri.getLastPathSegment();
        return name != null && (name.toLowerCase(Locale.ROOT).endsWith(".jpg")
                || name.toLowerCase(Locale.ROOT).endsWith(".jpeg")
                || name.toLowerCase(Locale.ROOT).endsWith(".png"));
    }

    private byte[] bacaFoto(Uri uri) throws Exception {
        try (InputStream stream = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (stream == null) {
                throw new Exception("Foto tidak dapat dibaca.");
            }
            byte[] buffer = new byte[8192];
            int total = 0;
            int jumlahBaca;
            while ((jumlahBaca = stream.read(buffer)) != -1) {
                total += jumlahBaca;
                if (total > MAX_FOTO_BYTES) {
                    throw new Exception(getString(R.string.admin_sarana_foto_ukuran));
                }
                output.write(buffer, 0, jumlahBaca);
            }
            return output.toByteArray();
        }
    }

    @Nullable
    private Bitmap decodeFoto(byte[] dataFoto) {
        BitmapFactory.Options batas = new BitmapFactory.Options();
        batas.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(dataFoto, 0, dataFoto.length, batas);
        if (batas.outWidth <= 0 || batas.outHeight <= 0) {
            return null;
        }
        BitmapFactory.Options opsi = new BitmapFactory.Options();
        opsi.inSampleSize = 1;
        while (batas.outWidth / opsi.inSampleSize > 1600
                || batas.outHeight / opsi.inSampleSize > 1600) {
            opsi.inSampleSize *= 2;
        }
        Bitmap bitmap = BitmapFactory.decodeByteArray(dataFoto, 0, dataFoto.length, opsi);
        return bitmap == null ? null : ubahUkuranFoto(bitmap, 1600);
    }

    private Object[] parseKoordinat(String nilai) {
        if (nilai == null || nilai.trim().isEmpty()) {
            return new Object[]{JSONObject.NULL, JSONObject.NULL};
        }
        String[] bagian = nilai.split(",");
        if (bagian.length != 2) {
            throw new IllegalArgumentException("Invalid coordinate format");
        }
        try {
            double latitude = Double.parseDouble(bagian[0].trim());
            double longitude = Double.parseDouble(bagian[1].trim());
            if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                    || latitude < -90 || latitude > 90
                    || longitude < -180 || longitude > 180) {
                throw new IllegalArgumentException("Coordinate out of range");
            }
            return new Object[]{latitude, longitude};
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid coordinate number", e);
        }
    }

    private String bacaRespons(HttpURLConnection connection, boolean error) throws Exception {
        InputStream stream = error ? connection.getErrorStream() : connection.getInputStream();
        if (stream == null) {
            return "{}";
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private static class DesaItem {
        final int id;
        final String nama;

        DesaItem(int id, String nama) {
            this.id = id;
            this.nama = nama;
        }
    }
}
