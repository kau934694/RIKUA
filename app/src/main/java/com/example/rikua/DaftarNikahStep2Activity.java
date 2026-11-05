package com.example.rikua;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.rikua.databinding.ActivityDaftarNikahStep2Binding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;
import java.util.regex.Pattern;

/** Halaman Daftar Nikah: Data Calon Suami (langkah 2). */
public class DaftarNikahStep2Activity extends AppCompatActivity {

    private static final String API_URL =
            "http://192.168.1.4/rikua_api/get_calon_pengantin.php";
    private static final Pattern POLA_NIK = Pattern.compile("\\d{16}");
    private static final Pattern POLA_TANGGAL = Pattern.compile("\\d{2}/\\d{2}/\\d{4}");
    private static final Pattern POLA_TELEPON = Pattern.compile("\\d{9,13}");

    private static final int STATUS_JEJAKA = 0;
    private static final int STATUS_DUDA_HIDUP = 1;
    private static final int STATUS_DUDA_MATI = 2;

    private ActivityDaftarNikahStep2Binding binding;
    private int idPendaftaran = 0;
    private String statusTerpilih = "jejaka";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDaftarNikahStep2Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        idPendaftaran = getIntent().getIntExtra("id_pendaftaran", 0);

        setupWindowAndStatusBar();
        setupToolbar();
        setupDrawer();
        setupDropdowns();
        setupTanggalLahir();
        setupStatusPernikahan();
        setupClearErrorOnInput();
        setupActionButtons();
        setupBottomNavigation();
    }

    @Override
    protected void onDestroy() {
        binding = null;
        super.onDestroy();
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

    private void setupDropdowns() {
        ArrayAdapter<CharSequence> pendidikanAdapter = ArrayAdapter.createFromResource(
                this, R.array.pendidikan_options, android.R.layout.simple_list_item_1);
        binding.dropdownPendidikan.setAdapter(pendidikanAdapter);
    }

    private void setupTanggalLahir() {
        binding.etTanggalLahir.setOnClickListener(v -> tampilkanDatePicker());
        binding.tilTanggalLahir.setEndIconOnClickListener(v -> tampilkanDatePicker());
    }

    private void tampilkanDatePicker() {
        Calendar now = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> binding.etTanggalLahir.setText(
                        String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)),
                now.get(Calendar.YEAR) - 25, now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMaxDate(now.getTimeInMillis());
        dialog.show();
    }

    private void setupStatusPernikahan() {
        binding.cardStatusJejaka.setOnClickListener(v -> pilihStatus(STATUS_JEJAKA));
        binding.cardStatusDudaHidup.setOnClickListener(v -> pilihStatus(STATUS_DUDA_HIDUP));
        binding.cardStatusDudaMati.setOnClickListener(v -> pilihStatus(STATUS_DUDA_MATI));
        pilihStatus(STATUS_JEJAKA);
    }

    private void pilihStatus(int index) {
        statusTerpilih = index == STATUS_DUDA_HIDUP ? "duda_cerai_hidup"
                : index == STATUS_DUDA_MATI ? "duda_cerai_mati" : "jejaka";
        binding.rbJejaka.setChecked(index == STATUS_JEJAKA);
        binding.rbDudaHidup.setChecked(index == STATUS_DUDA_HIDUP);
        binding.rbDudaMati.setChecked(index == STATUS_DUDA_MATI);
        terangkanKartu(binding.cardStatusJejaka, index == STATUS_JEJAKA);
        terangkanKartu(binding.cardStatusDudaHidup, index == STATUS_DUDA_HIDUP);
        terangkanKartu(binding.cardStatusDudaMati, index == STATUS_DUDA_MATI);
    }

    private void terangkanKartu(MaterialCardView card, boolean terpilih) {
        int bg = ContextCompat.getColor(this, terpilih
                ? R.color.md3_radio_selected_bg : R.color.md3_surface_card);
        int stroke = ContextCompat.getColor(this, terpilih
                ? R.color.md3_primary : R.color.md3_outline_variant_40);
        card.setCardBackgroundColor(bg);
        card.setStrokeColor(ColorStateList.valueOf(stroke));
        card.setStrokeWidth(dp(terpilih ? 1.5f : 1f));
    }

    private void setupClearErrorOnInput() {
        bersihkanErrorSaatKetik(binding.tilNama);
        bersihkanErrorSaatKetik(binding.tilNik);
        bersihkanErrorSaatKetik(binding.tilTempatLahir);
        bersihkanErrorSaatKetik(binding.tilTanggalLahir);
        bersihkanErrorSaatKetik(binding.tilPekerjaan);
        bersihkanErrorSaatKetik(binding.tilAlamat);
        bersihkanErrorSaatKetik(binding.tilTelepon);
    }

    private void bersihkanErrorSaatKetik(final TextInputLayout layout) {
        if (layout.getEditText() == null) return;
        layout.getEditText().addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                if (!s.toString().trim().isEmpty()) layout.setError(null);
            }
        });
    }

    private void setupActionButtons() {
        binding.btnLanjutIstri.setOnClickListener(v -> {
            if (validasiForm()) kirimKeServer();
        });
        binding.tvBantuanPetugas.setOnClickListener(v ->
                startActivity(new Intent(this, ConsultationActivity.class)));
    }

    private boolean validasiForm() {
        boolean valid = true;
        String nama = teks(binding.etNama.getText().toString());
        if (nama.isEmpty()) {
            binding.tilNama.setError(getString(R.string.s2_err_nama)); valid = false;
        } else binding.tilNama.setError(null);

        String nik = binding.etNik.getText().toString().trim();
        if (!POLA_NIK.matcher(nik).matches()) {
            binding.tilNik.setError(getString(R.string.s2_err_nik)); valid = false;
        } else binding.tilNik.setError(null);

        String tempat = teks(binding.etTempatLahir.getText().toString());
        if (tempat.isEmpty()) {
            binding.tilTempatLahir.setError(getString(R.string.s2_err_tempat)); valid = false;
        } else binding.tilTempatLahir.setError(null);

        String tanggal = binding.etTanggalLahir.getText().toString().trim();
        if (!POLA_TANGGAL.matcher(tanggal).matches()) {
            binding.tilTanggalLahir.setError(getString(R.string.s2_err_tanggal)); valid = false;
        } else binding.tilTanggalLahir.setError(null);

        if (binding.etPekerjaan.getText().toString().trim().isEmpty()) {
            binding.tilPekerjaan.setError(getString(R.string.s2_err_pekerjaan)); valid = false;
        } else binding.tilPekerjaan.setError(null);

        if (binding.dropdownPendidikan.getText().toString().trim().isEmpty()) {
            binding.tilPendidikan.setError(getString(R.string.s2_err_wajib_lengkap)); valid = false;
        } else binding.tilPendidikan.setError(null);

        if (binding.etAlamat.getText().toString().trim().isEmpty()) {
            binding.tilAlamat.setError(getString(R.string.s2_err_alamat)); valid = false;
        } else binding.tilAlamat.setError(null);

        String telepon = binding.etTelepon.getText().toString().trim();
        if (!POLA_TELEPON.matcher(telepon).matches()) {
            binding.tilTelepon.setError(getString(R.string.s2_err_telepon)); valid = false;
        } else binding.tilTelepon.setError(null);

        if (!valid) Snackbar.make(binding.getRoot(), R.string.s2_err_wajib_lengkap,
                Snackbar.LENGTH_LONG).show();
        return valid;
    }

    private String teks(String nilai) {
        return nilai == null ? "" : nilai.trim();
    }

    private void kirimKeServer() {
        binding.btnLanjutIstri.setEnabled(false);
        CalonApi.simpan(2, idPendaftaran,
                binding.etNama.getText().toString().trim(),
                binding.etNik.getText().toString().trim(),
                binding.etTempatLahir.getText().toString().trim(),
                binding.etTanggalLahir.getText().toString().trim(),
                "Islam",
                binding.etPekerjaan.getText().toString().trim(),
                binding.dropdownPendidikan.getText().toString().trim(),
                binding.etAlamat.getText().toString().trim(),
                binding.etTelepon.getText().toString().trim(),
                statusTerpilih, false, new CalonApi.SimpanCallback() {
                    @Override
                    public void onSukses(int id) {
                        if (binding == null) return;
                        binding.btnLanjutIstri.setEnabled(true);
                        if (id <= 0) {
                            Snackbar.make(binding.getRoot(),
                                    "Data tersimpan, tetapi ID pendaftaran tidak valid.",
                                    Snackbar.LENGTH_LONG).show();
                            return;
                        }
                        idPendaftaran = id;
                        Intent intent = new Intent(DaftarNikahStep2Activity.this,
                                DataCalonIstriActivity.class);
                        intent.putExtra("id_pendaftaran", idPendaftaran);
                        startActivity(intent);
                    }

                    @Override
                    public void onGagal(String pesan) {
                        if (binding == null) return;
                        binding.btnLanjutIstri.setEnabled(true);
                        Snackbar.make(binding.getRoot(), pesan, Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private int dp(float nilaiDp) {
        return Math.round(getResources().getDisplayMetrics().density * nilaiDp);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = binding.bottomNavigation;
        bottomNav.setSelectedItemId(R.id.menu_daftar_nikah);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_daftar_nikah) return true;
            if (itemId == R.id.menu_lainnya) {
                toggleDrawer();
                bottomNav.setSelectedItemId(R.id.menu_daftar_nikah);
                return false;
            }
            handleNavigation(itemId);
            return true;
        });
    }

    private void handleNavigation(int itemId) {
        if (itemId == R.id.menu_home) navigateTo(MainActivity.class);
        else if (itemId == R.id.menu_services) navigateTo(ServicesActivity.class);
        else if (itemId == R.id.menu_forms) navigateTo(BlankoActivity.class);
        else if (itemId == R.id.menu_facilities) navigateTo(FacilitiesActivity.class);
        else if (itemId == R.id.menu_consultation) navigateTo(ConsultationActivity.class);
        else if (itemId == R.id.menu_pengumuman) navigateTo(PengumumanNikahActivity.class);
        else Toast.makeText(this, R.string.daf_feature_soon, Toast.LENGTH_SHORT).show();
    }

    private void navigateTo(Class<?> target) {
        Intent intent = new Intent(this, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (binding != null && binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
            binding.drawerLayout.closeDrawer(GravityCompat.END);
        } else super.onBackPressed();
    }
}
