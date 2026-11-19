package com.example.rikua;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.rikua.databinding.ActivityRegisterBinding;

/**
 * Halaman pendaftaran akun baru. Tahap ini hanya tampilan dan validasi kolom,
 * tanpa menyimpan akun atau menghubungi server.
 */
public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        binding.tvLogin.setOnClickListener(v -> finish());

        binding.btnRegister.setOnClickListener(v -> {
            if (validasi()) {
                Toast.makeText(this, R.string.auth_register_pending, Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean validasi() {
        String[] kolom = {
                binding.etFullname.getText().toString().trim(),
                binding.etUsername.getText().toString().trim(),
                binding.etEmail.getText().toString().trim(),
                binding.etPassword.getText().toString(),
                binding.etConfirm.getText().toString()};
        boolean valid = true;

        binding.tilFullname.setError(null);
        binding.tilUsername.setError(null);
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirm.setError(null);

        if (kolom[0].isEmpty()) {
            binding.tilFullname.setError(getString(R.string.auth_error_required));
            valid = false;
        }
        if (kolom[1].isEmpty()) {
            binding.tilUsername.setError(getString(R.string.auth_error_required));
            valid = false;
        }
        if (kolom[2].isEmpty()) {
            binding.tilEmail.setError(getString(R.string.auth_error_required));
            valid = false;
        }
        if (kolom[3].isEmpty()) {
            binding.tilPassword.setError(getString(R.string.auth_error_required));
            valid = false;
        }
        if (kolom[4].isEmpty()) {
            binding.tilConfirm.setError(getString(R.string.auth_error_required));
            valid = false;
        } else if (!kolom[4].equals(kolom[3])) {
            binding.tilConfirm.setError(getString(R.string.auth_error_confirm));
            valid = false;
        }
        return valid;
    }

    @Override
    protected void onDestroy() {
        binding = null;
        super.onDestroy();
    }
}
