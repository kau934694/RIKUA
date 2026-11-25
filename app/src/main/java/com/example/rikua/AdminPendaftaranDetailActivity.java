package com.example.rikua;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.rikua.databinding.ActivityAdminPendaftaranDetailBinding;

public class AdminPendaftaranDetailActivity extends AppCompatActivity {
    public static final String EXTRA_TIKET = "admin_pendaftaran_tiket";
    public static final String EXTRA_STATUS = "admin_pendaftaran_status";
    public static final String EXTRA_TANGGAL_DAFTAR = "admin_pendaftaran_tanggal_daftar";
    public static final String EXTRA_NAMA_SUAMI = "admin_pendaftaran_nama_suami";
    public static final String EXTRA_NAMA_ISTRI = "admin_pendaftaran_nama_istri";
    public static final String EXTRA_TANGGAL_AKAD = "admin_pendaftaran_tanggal_akad";
    public static final String EXTRA_WAKTU_AKAD = "admin_pendaftaran_waktu_akad";
    public static final String EXTRA_LOKASI = "admin_pendaftaran_lokasi";

    private ActivityAdminPendaftaranDetailBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (!penjagaSesiAdmin()) {
            return;
        }

        binding = ActivityAdminPendaftaranDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnKembali.setOnClickListener(v -> finish());

        String tiket = nilaiExtra(EXTRA_TIKET);
        if (tiket.isEmpty()) {
            Toast.makeText(this, R.string.admin_pendaftaran_detail_tidak_valid,
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.tvKodeTiket.setText(tiket);
        binding.tvStatus.setText(labelStatus(nilaiExtra(EXTRA_STATUS)));
        binding.tvTanggalDaftar.setText(nilaiAtauKosong(EXTRA_TANGGAL_DAFTAR));
        binding.tvNamaSuami.setText(nilaiAtauKosong(EXTRA_NAMA_SUAMI));
        binding.tvNamaIstri.setText(nilaiAtauKosong(EXTRA_NAMA_ISTRI));
        binding.tvTanggalAkad.setText(nilaiAtauKosong(EXTRA_TANGGAL_AKAD));
        binding.tvWaktuAkad.setText(nilaiAtauKosong(EXTRA_WAKTU_AKAD));
        binding.tvLokasiAkad.setText(nilaiAtauKosong(EXTRA_LOKASI));
        terapkanWarnaStatus(nilaiExtra(EXTRA_STATUS));
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

    private String nilaiExtra(String key) {
        String value = getIntent().getStringExtra(key);
        return value == null ? "" : value.trim();
    }

    private String nilaiAtauKosong(String key) {
        String value = nilaiExtra(key);
        return value.isEmpty()
                ? getString(R.string.admin_pendaftaran_belum_tersedia) : value;
    }

    private String labelStatus(String status) {
        if (status.isEmpty()) {
            return getString(R.string.admin_pendaftaran_status_tidak_tersedia);
        }
        String label = status.replace('_', ' ');
        return label.substring(0, 1).toUpperCase(java.util.Locale.forLanguageTag("id-ID"))
                + label.substring(1);
    }

    private void terapkanWarnaStatus(String status) {
        int background;
        int foreground;
        if ("dikirim".equalsIgnoreCase(status)) {
            background = R.color.badge_teal_bg;
            foreground = R.color.badge_teal_text;
        } else if ("draf".equalsIgnoreCase(status)) {
            background = R.color.badge_amber_bg;
            foreground = R.color.badge_amber_text;
        } else {
            background = R.color.status_offline_bg;
            foreground = R.color.status_offline_text;
        }
        binding.tvStatus.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(this, background)));
        binding.tvStatus.setTextColor(ContextCompat.getColor(this, foreground));
    }

    @Override
    protected void onDestroy() {
        binding = null;
        super.onDestroy();
    }
}
