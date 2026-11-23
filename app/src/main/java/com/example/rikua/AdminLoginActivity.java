
package com.example.rikua;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

    private static final String TAG = "AdminLogin";

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
                Log.i(TAG, "Login response HTTP " + responseCode);

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
                Log.i(TAG, "Login response success=" + success);

                if (responseCode == 200 && success) {
                    JSONObject user = json.getJSONObject("user");

                    String role = user.optString("role", "").trim();

                    if (!"admin".equalsIgnoreCase(role)) {
                        Log.w(TAG, "Login rejected: role is not admin");
                        tampilkanHasil(false,
                                "Akun ini bukan akun administrator.");
                        return;
                    }

                    String token = json.optString("token", "").trim();
                    String userId = user.optString("id", "").trim();
                    String nama = user.optString("nama_lengkap", "");
                    String email = user.optString("email", "");
                    String expiresAt = json.optString("expires_at", "");
                    boolean tokenPresent = !token.isEmpty();
                    boolean userIdPresent = !userId.isEmpty();
                    boolean expiryValid = SessionExpiry.isFuture(
                            expiresAt, System.currentTimeMillis());
                    Log.i(TAG, "Login fields: tokenPresent=" + tokenPresent
                            + ", userIdPresent=" + userIdPresent
                            + ", roleAdmin=true, expiryValid=" + expiryValid);

                    if (!tokenPresent || !userIdPresent || !expiryValid) {
                        Log.w(TAG, "Login rejected: incomplete or invalid session");
                        tampilkanHasil(false,
                                getString(R.string.auth_session_expiry_invalid));
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
                    Log.i(TAG, "Session saved: loggedIn="
                            + sessionManager.isLoggedIn()
                            + ", sessionValid=" + sessionManager.isSessionValid()
                            + ", roleAdmin="
                            + "admin".equalsIgnoreCase(sessionManager.getRole()));

                    tampilkanHasil(true,
                            "Login administrator berhasil!");

                } else {
                    String message = json.optString(
                            "message",
                            "Login gagal. Periksa kembali akun Anda."
                    );

                    Log.w(TAG, "Login rejected by server: HTTP " + responseCode
                            + ", success=" + success);
                    tampilkanHasil(false, message);
                }

            } catch (Exception e) {
                Log.e(TAG, "Login request failed: "
                        + e.getClass().getSimpleName());
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

                // Bersihkan tumpukan task: Back tidak kembali ke login admin.
                Intent intent = new Intent(
                        AdminLoginActivity.this,
                        AdminDashboardActivity.class
                );
                intent.setFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                );
                Log.i(TAG, "Launching AdminDashboardActivity");
                startActivity(intent);
                finish();
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
