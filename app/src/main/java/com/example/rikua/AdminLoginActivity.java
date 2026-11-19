
package com.example.rikua;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.rikua.databinding.ActivityAdminLoginBinding;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdminLoginActivity extends AppCompatActivity {

    private ActivityAdminLoginBinding binding;
    private SessionManager sessionManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityAdminLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.tvBackToLogin.setOnClickListener(v -> finish());

        binding.btnAdminLogin.setOnClickListener(v -> {
            if (validasi()) {
                prosesLogin();
            }
        });
    }

    private boolean validasi() {
        String identitas = binding.etIdentity.getText().toString().trim();
        String sandi = binding.etPassword.getText().toString();

        binding.tilIdentity.setError(null);
        binding.tilPassword.setError(null);

        boolean valid = true;

        if (identitas.isEmpty()) {
            binding.tilIdentity.setError(
                    getString(R.string.auth_error_required));
            valid = false;
        }

        if (sandi.isEmpty()) {
            binding.tilPassword.setError(
                    getString(R.string.auth_error_required));
            valid = false;
        }

        return valid;
    }

    private void prosesLogin() {
        String identity = binding.etIdentity.getText().toString().trim();
        String password = binding.etPassword.getText().toString();

        aturLoading(true);

        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(ApiConfig.ADMIN_LOGIN);
                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setDoOutput(true);
                connection.setRequestProperty(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                );
                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                JSONObject request = new JSONObject();
                request.put("identity", identity);
                request.put("password", password);

                byte[] requestBody = request.toString()
                        .getBytes(StandardCharsets.UTF_8);

                try (OutputStream output = connection.getOutputStream()) {
                    output.write(requestBody);
                    output.flush();
                }

                int responseCode = connection.getResponseCode();

                InputStream input = responseCode >= 200
                        && responseCode < 400
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                StringBuilder response = new StringBuilder();

                if (input != null) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(
                                    input,
                                    StandardCharsets.UTF_8
                            ))) {

                        String line;

                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                    }
                }

                JSONObject json = new JSONObject(response.toString());

                boolean success = json.optBoolean("success", false);

                if (responseCode == 200 && success) {
                    JSONObject user = json.getJSONObject("user");

                    String role = user.optString("role", "");

                    if (!"admin".equalsIgnoreCase(role)) {
                        tampilkanHasil(false,
                                "Akun ini bukan akun administrator.");
                        return;
                    }

                    String token = json.optString("token", "");
                    String userId = user.optString("id", "");
                    String nama = user.optString("nama_lengkap", "");
                    String email = user.optString("email", "");
                    String expiresAt = json.optString("expires_at", "");

                    if (token.isEmpty() || userId.isEmpty()) {
                        tampilkanHasil(false,
                                "Respons server tidak lengkap.");
                        return;
                    }

                    sessionManager.saveSession(
                            token,
                            userId,
                            nama,
                            email,
                            role,
                            expiresAt
                    );

                    tampilkanHasil(true,
                            "Login administrator berhasil!");

                } else {
                    String message = json.optString(
                            "message",
                            "Login gagal. Periksa kembali akun Anda."
                    );

                    tampilkanHasil(false, message);
                }

            } catch (Exception e) {
                tampilkanHasil(false,
                        "Tidak dapat terhubung ke server. "
                                + "Periksa Laragon dan koneksi Wi-Fi.");
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void tampilkanHasil(boolean berhasil, String pesan) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed() || binding == null) {
                return;
            }

            aturLoading(false);

            Toast.makeText(
                    AdminLoginActivity.this,
                    pesan,
                    Toast.LENGTH_LONG
            ).show();

            if (berhasil) {
                binding.etPassword.setText("");
            }
        });
    }

    private void aturLoading(boolean loading) {
        binding.btnAdminLogin.setEnabled(!loading);
        binding.etIdentity.setEnabled(!loading);
        binding.etPassword.setEnabled(!loading);

        binding.btnAdminLogin.setText(
                loading ? "Memproses..." : "Login Admin"
        );
    }

    @Override
    protected void onDestroy() {
        if (binding != null) {
            binding = null;
        }

        executor.shutdownNow();
        super.onDestroy();
    }
}
