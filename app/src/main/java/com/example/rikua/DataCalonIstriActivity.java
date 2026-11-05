package com.example.rikua;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
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

import com.example.rikua.databinding.ActivityDataCalonIstriBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;
import java.util.regex.Pattern;

/**
 * DataCalonIstriActivity — Halaman "Daftar Nikah: Data Calon Istri"
 * (Langkah 3 dari 6) RIKUA KUA Kec. Randudongkal.
 *
 * Form identitas calon pengantin wanita dengan validasi sederhana di sisi
 * perangkat (belum dikirim ke server):
 *   1. Stepper & draf   : label "Langkah 3 dari 6", progress 50% (3/6),
 *                         label "Sebelumnya: Calon Suami" / "Berikutnya:
 *                         Wali Nikah", tombol Simpan Draf (Snackbar).
 *   2. Header info      : badge "Validasi Data Kemenag & Disdukcapil" +
 *                         judul "Identitas Calon Pengantin Wanita".
 *   3. Field wajib (*)  : nama (endIcon centang), NIK 16 digit, tempat &
 *                         tanggal lahir (DatePicker, format HH/BB/TTTT),
 *                         agama (dropdown, default Islam), pekerjaan,
 *                         pendidikan (dropdown), alamat multiline,
 *                         telepon prefix +62.
 *   4. Status pernikahan: 3 kartu radio custom — Perawan (default, badge
 *                         "Utama"), Janda cerai hidup, Janda cerai mati.
 *   5. Switch card      : "Anak Pertama?" — teks dinamis Ya (Sulung) /
 *                         Bukan (Bungsu/Tengah).
 *   6. Banner bantuan   : Tanya Petugas → Konsultasi.
 *   7. Sticky dock      : "Lanjut: Data Wali Nikah" (validasi lalu Intent
 *                         Langkah 4), bottom nav 5 tab dengan tab
 *                         "Daftar Nikah" aktif, drawer 12 menu.
 *
 * Implementasi memakai View Binding (ActivityDataCalonIstriBinding) — tanpa
 * findViewById, sesuai standar project.
 */
public class DataCalonIstriActivity extends AppCompatActivity {

    /** Pendaftaran mandiri lewat portal SIMKAH Kemenag (menu drawer). */
    private static final String API_URL = "http://192.168.1.4/rikua_api/get_calon_pengantin.php";
    private static final String URL_SIMKAH = "https://simkah.kemenag.go.id";

    /** NIK: tepat 16 digit angka. */
    private static final Pattern POLA_NIK = Pattern.compile("\\d{16}");
    /** Tanggal lahir: HH/BB/TTTT. */
    private static final Pattern POLA_TANGGAL = Pattern.compile("\\d{2}/\\d{2}/\\d{4}");
    /** Nomor telepon: 9–13 digit (tanpa kode negara, sudah ada prefix +62). */
    private static final Pattern POLA_TELEPON = Pattern.compile("\\d{9,13}");

    /** Index status pernikahan wanita, dipakai untuk highlight kartu radio. */
    private static final int STATUS_PERAWAN = 0;
    private static final int STATUS_JANDA_HIDUP = 1;
    private static final int STATUS_JANDA_MATI = 2;

    private ActivityDataCalonIstriBinding binding;
    private int idPendaftaran = 0;
private String statusTerpilih = "perawan";

    // =======================================================================
    // Lifecycle
    // =======================================================================

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDataCalonIstriBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        idPendaftaran = getIntent().getIntExtra("id_pendaftaran", 0);

        setupWindowAndStatusBar();
        setupToolbar();
        setupDrawer();
        setupDropdowns();
        setupTanggalLahir();
        setupStatusPernikahan();
        setupSwitchAnakPertama();
        setupClearErrorOnInput();
        setupActionButtons();
        setupBottomNavigation();
    }

    @Override
    protected void onDestroy() {
        binding = null; // hindari kebocoran view melalui binding statis
        super.onDestroy();
    }

    // =======================================================================
    // Setup tampilan
    // =======================================================================

    /** Selaraskan warna status bar dengan primary MD3 + inset navigasi bawah. */
    private void setupWindowAndStatusBar() {
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.md3_primary));

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(true);

        // Panel sticky bawah (dock + bottom nav) naik menghindari gesture bar.
        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutBottomSticky, (view, insets) -> {
            androidx.core.graphics.Insets bars =
                    insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(view.getPaddingLeft(), view.getPaddingTop(),
                    view.getPaddingRight(), bars.bottom);
            return insets;
        });
    }

    /** Panah kiri kembali ke Langkah 2; burger icon membuka navigation drawer. */
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

    // =======================================================================
    // Komponen form
    // =======================================================================

    /**
     * Dropdown Agama & Pendidikan Terakhir memakai ExposedDropdownMenu
     * (padanan MD3 dari Spinner) dengan item dari arrays project.
     * Agama terisi default item pertama ("Islam") sesuai regulasi pengisian.
     */
    private void setupDropdowns() {
        ArrayAdapter<CharSequence> pendidikanAdapter = ArrayAdapter.createFromResource(
                this, R.array.pendidikan_options, android.R.layout.simple_list_item_1);
        binding.dropdownPendidikan.setAdapter(pendidikanAdapter);
    }

    /** Field tanggal lahir read-only; klik memunculkan DatePicker lalu
      *  menulis hasil dalam format HH/BB/TTTT. */
    private void setupTanggalLahir() {
        binding.etTanggalLahir.setOnClickListener(v -> tampilkanDatePicker());
        binding.tilTanggalLahir.setEndIconOnClickListener(v -> tampilkanDatePicker());
    }

    private void tampilkanDatePicker() {
        Calendar now = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> binding.etTanggalLahir.setText(
                        String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)),
                now.get(Calendar.YEAR) - 25, now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMaxDate(now.getTimeInMillis());
        dialog.show();
    }

    /** Kartu status pernikahan: RadioButton di luar grup → ekslusivitas
      *  dikelola manual; kartu terpilih diberi stroke + background highlight. */
    private void setupStatusPernikahan() {
        binding.cardStatusPerawan.setOnClickListener(v -> pilihStatus(STATUS_PERAWAN));
        binding.cardStatusJandaHidup.setOnClickListener(v -> pilihStatus(STATUS_JANDA_HIDUP));
        binding.cardStatusJandaMati.setOnClickListener(v -> pilihStatus(STATUS_JANDA_MATI));
        pilihStatus(STATUS_PERAWAN); // default Terpilih: Perawan (badge "Utama")
    }

    private void pilihStatus(int index) {
        statusTerpilih = index == STATUS_JANDA_HIDUP ? "janda_cerai_hidup"
        : index == STATUS_JANDA_MATI ? "janda_cerai_mati" : "perawan";
        binding.rbPerawan.setChecked(index == STATUS_PERAWAN);
        binding.rbJandaHidup.setChecked(index == STATUS_JANDA_HIDUP);
        binding.rbJandaMati.setChecked(index == STATUS_JANDA_MATI);

        terangkanKartu(binding.cardStatusPerawan, index == STATUS_PERAWAN);
        terangkanKartu(binding.cardStatusJandaHidup, index == STATUS_JANDA_HIDUP);
        terangkanKartu(binding.cardStatusJandaMati, index == STATUS_JANDA_MATI);
    }

    /** Kartu terpilih: background hijau soft + stroke primary 1.5dp;
      *  kartu biasa: putih + border tipis. */
    private void terangkanKartu(MaterialCardView card, boolean terpilih) {
        int bg = ContextCompat.getColor(this,
                terpilih ? R.color.md3_radio_selected_bg : R.color.md3_surface_card);
        int stroke = ContextCompat.getColor(this,
                terpilih ? R.color.md3_primary : R.color.md3_outline_variant_40);
        card.setCardBackgroundColor(bg);
        card.setStrokeColor(ColorStateList.valueOf(stroke));
        card.setStrokeWidth(dp(terpilih ? 1.5f : 1f));
    }

    /** Switch "Anak Pertama?" — teks keterangan berubah dinamis mengikuti
      *  keadaan switch; klik kartu turut men-toggle switch. */
    private void setupSwitchAnakPertama() {
        binding.switchAnakPertama.setOnCheckedChangeListener((button, checked) ->
                binding.tvAnakStatus.setText(checked
                        ? R.string.dai_anak_ya : R.string.dai_anak_bukan));
        // Kartu bisa diklik sebagai area toggle yang lebih luas.
        binding.cardAnakPertama.setOnClickListener(v ->
                binding.switchAnakPertama.toggle());
    }

    /** Error hilang begitu user mulai memperbaiki field. */
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
        layout.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().trim().isEmpty()) {
                    layout.setError(null);
                }
            }
        });
    }
    private void kirimKeServer() {
    if (idPendaftaran <= 0) {
        Snackbar.make(binding.getRoot(),
                "Data calon suami belum tersimpan. Kembali ke Langkah 2.",
                Snackbar.LENGTH_LONG).show();
        return;
    }
    binding.btnLanjutWali.setEnabled(false);

    CalonApi.simpan(3, idPendaftaran,
            binding.etNama.getText().toString().trim(),
            binding.etNik.getText().toString().trim(),
            binding.etTempatLahir.getText().toString().trim(),
            binding.etTanggalLahir.getText().toString().trim(),
           "Islam",
            binding.etPekerjaan.getText().toString().trim(),
            binding.dropdownPendidikan.getText().toString().trim(),
            binding.etAlamat.getText().toString().trim(),
            binding.etTelepon.getText().toString().trim(),
            statusTerpilih,
            binding.switchAnakPertama.isChecked(),
            new CalonApi.SimpanCallback() {
                @Override
                public void onSukses(int id) {
                    if (binding == null) return;
                    binding.btnLanjutWali.setEnabled(true);
                    Intent i = new Intent(DataCalonIstriActivity.this,
                            DaftarNikahStep4Activity.class);
                    i.putExtra("id_pendaftaran", id);
                    startActivity(i);
                }

                @Override
                public void onGagal(String pesan) {
                    if (binding == null) return;
                    binding.btnLanjutWali.setEnabled(true);
                    Snackbar.make(binding.getRoot(), pesan, Snackbar.LENGTH_LONG).show();
                }
            });
}

    // =======================================================================
    // Aksi tombol
    // =======================================================================

    private void setupActionButtons() {
        binding.btnSimpanDraf.setOnClickListener(v ->
                Snackbar.make(binding.getRoot(), R.string.dai_draf_saved,
                        Snackbar.LENGTH_LONG).show());

        binding.btnLanjutWali.setOnClickListener(v -> {
    if (validasiForm()) {
        kirimKeServer();
    }
});
        binding.btnTanyaPetugas.setOnClickListener(v ->
                startActivity(new Intent(this, ConsultationActivity.class)));
    }

    // =======================================================================
    // Validasi sederhana (client-side, field wajib bertanda *)
    // =======================================================================

    /**
     * Periksa kelengkapan & format field wajib. Menandai TextInputLayout
     * yang bermasalah dengan error merah (#ba1a1a) dan mengembalikan false
     * agar navigasi ke Langkah 4 ditunda sampai data benar.
     */
    private boolean validasiForm() {
        boolean valid = true;

        // Nama — wajib terisi.
        String nama = teks(binding.etNama.getText().toString());
        if (nama.isEmpty()) {
            binding.tilNama.setError(getString(R.string.s2_err_nama));
            valid = false;
        } else {
            binding.tilNama.setError(null);
        }

        // NIK — tepat 16 digit.
        String nik = binding.etNik.getText().toString().trim();
        if (!POLA_NIK.matcher(nik).matches()) {
            binding.tilNik.setError(getString(R.string.s2_err_nik));
            valid = false;
        } else {
            binding.tilNik.setError(null);
        }

        // Tempat lahir — wajib.
        String tempat = teks(binding.etTempatLahir.getText().toString());
        if (tempat.isEmpty()) {
            binding.tilTempatLahir.setError(getString(R.string.s2_err_tempat));
            valid = false;
        } else {
            binding.tilTempatLahir.setError(null);
        }

        // Tanggal lahir — wajib dengan format HH/BB/TTTT.
        String tanggal = binding.etTanggalLahir.getText().toString().trim();
        if (!POLA_TANGGAL.matcher(tanggal).matches()) {
            binding.tilTanggalLahir.setError(getString(R.string.s2_err_tanggal));
            valid = false;
        } else {
            binding.tilTanggalLahir.setError(null);
        }
        // Pekerjaan — wajib.
        if (binding.etPekerjaan.getText().toString().trim().isEmpty()) {
            binding.tilPekerjaan.setError(getString(R.string.s2_err_pekerjaan));
            valid = false;
        } else {
            binding.tilPekerjaan.setError(null);
        }

        // Pendidikan — wajib dipilih.
        if (binding.dropdownPendidikan.getText().toString().trim().isEmpty()) {
            binding.tilPendidikan.setError(getString(R.string.s2_err_wajib_lengkap));
            valid = false;
        } else {
            binding.tilPendidikan.setError(null);
        }

        // Alamat — wajib.
        if (binding.etAlamat.getText().toString().trim().isEmpty()) {
            binding.tilAlamat.setError(getString(R.string.s2_err_alamat));
            valid = false;
        } else {
            binding.tilAlamat.setError(null);
        }

        // Telepon — 9–13 digit.
        String telepon = binding.etTelepon.getText().toString().trim();
        if (!POLA_TELEPON.matcher(telepon).matches()) {
            binding.tilTelepon.setError(getString(R.string.s2_err_telepon));
            valid = false;
        } else {
            binding.tilTelepon.setError(null);
        }

        // Status pernikahan — default Perawan selalu terpilih; tidak bisa kosong.
        if (!valid) {
            Snackbar.make(binding.getRoot(), R.string.s2_err_wajib_lengkap,
                    Snackbar.LENGTH_LONG).show();
        }
        return valid;
    }

    private String teks(String nilai) {
        return nilai == null ? "" : nilai.trim();
    }

    private int dp(float nilaiDp) {
        return Math.round(getResources().getDisplayMetrics().density * nilaiDp);
    }

    // =======================================================================
    // Navigasi
    // =======================================================================

    /** Bottom nav persisten — tab ke-3 "Daftar Nikah" tetap aktif di alur ini. */
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = binding.bottomNavigation;
        bottomNav.setSelectedItemId(R.id.menu_daftar_nikah);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_daftar_nikah) {
                return true; // masih dalam alur daftar nikah — tetap di sini
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

    /** Routing bersama bottom navigation dan navigation drawer (12 menu). */
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
                Toast.makeText(this, R.string.daf_simkah_unavailable,
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            // daftar_nikah (sudah di alur) / cek_status / faq / profil / login
            Toast.makeText(this, R.string.daf_feature_soon, Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateTo(Class<?> target) {
        Intent intent = new Intent(this, target);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    // =======================================================================
    // Back stack
    // =======================================================================

    /** Drawer terbuka punya prioritas pertama saat tombol back ditekan. */
    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
            binding.drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }
}
