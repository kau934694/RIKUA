
package com.example.rikua;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.example.rikua.databinding.ActivityLoginBinding;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

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

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private CredentialManager credentialManager;
    private SessionManager sessionManager;
    private ExecutorService networkExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        credentialManager = CredentialManager.create(getApplicationContext());
        sessionManager = new SessionManager(this);
        networkExecutor = Executors.newSingleThreadExecutor();

        binding.tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        binding.btnAdmin.setOnClickListener(v ->
                startActivity(new Intent(this, AdminLoginActivity.class)));

        binding.btnLogin.setOnClickListener(v -> {
            if (validasi()) {
                Toast.makeText(
                        this,
                        R.string.auth_login_pending,
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        binding.btnGoogle.setOnClickListener(v -> masukDenganGoogle());
    }

    private void masukDenganGoogle() {
        binding.btnGoogle.setEnabled(false);

        GetGoogleIdOption googleIdOption =
                new GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(ApiConfig.GOOGLE_WEB_CLIENT_ID)
                        .setAutoSelectEnabled(false)
                        .build();

        GetCredentialRequest request =
                new GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                null,
                ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {

                    @Override
                    public void onResult(GetCredentialResponse result) {
                        Credential credential = result.getCredential();

                        if (credential instanceof CustomCredential
                                && credential.getType().equals(
                                GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {

                            try {
                                GoogleIdTokenCredential googleCredential =
                                        GoogleIdTokenCredential.createFrom(
                                                credential.getData()
                                        );

                                String idToken = googleCredential.getIdToken();

                                kirimTokenKeServer(idToken);

                            } catch (Exception e) {
    binding.btnGoogle.setEnabled(true);
    Toast.makeText(
            LoginActivity.this,
            "Gagal membaca akun Google: " + e.getMessage(),
            Toast.LENGTH_LONG
    ).show();
}

                        } else {
                            binding.btnGoogle.setEnabled(true);
                            Toast.makeText(
                                    LoginActivity.this,
                                    "Jenis akun Google tidak dikenali.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        binding.btnGoogle.setEnabled(true);
                        Toast.makeText(
                                LoginActivity.this,
                                "Login Google dibatalkan atau gagal: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void kirimTokenKeServer(String idToken) {
        networkExecutor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(ApiConfig.GOOGLE_LOGIN);
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

                JSONObject requestBody = new JSONObject();
                requestBody.put("id_token", idToken);

                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(
                            requestBody.toString().getBytes(StandardCharsets.UTF_8)
                    );
                }

                int responseCode = connection.getResponseCode();

                InputStream inputStream = responseCode >= 200
                        && responseCode < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                StringBuilder response = new StringBuilder();

                if (inputStream != null) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(
                                    inputStream,
                                    StandardCharsets.UTF_8
                            ))) {

                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                    }
                }

                JSONObject jsonResponse =
                        new JSONObject(response.toString());

                if (responseCode >= 200
                        && responseCode < 300
                        && jsonResponse.optBoolean("success")) {

                    JSONObject user = jsonResponse.getJSONObject("user");

                    String token = jsonResponse.getString("token");
                    String userId = user.optString("id", "");
                    String nama = user.optString("nama_lengkap", "");
                    String email = user.optString("email", "");
                    String role = user.optString("role", "");
                    String expiresAt = jsonResponse.optString("expires_at", "");

                    sessionManager.saveSession(
                            token,
                            userId,
                            nama,
                            email,
                            role,
                            expiresAt
                    );

                    runOnUiThread(this::bukaDashboard);

                } else {
                    String message = jsonResponse.optString(
                            "message",
                            "Login Google gagal."
                    );

                    runOnUiThread(() -> {
                        binding.btnGoogle.setEnabled(true);
                        Toast.makeText(
                                LoginActivity.this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    });
                }

            } catch (Exception e) {
                runOnUiThread(() -> {
                    binding.btnGoogle.setEnabled(true);
                    Toast.makeText(
                            LoginActivity.this,
                            "Tidak dapat menghubungi server: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void bukaDashboard() {
        Toast.makeText(
                this,
                "Login berhasil. Selamat datang!",
                Toast.LENGTH_SHORT
        ).show();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private boolean validasi() {
        String identitas = binding.etIdentity.getText().toString().trim();
        String sandi = binding.etPassword.getText().toString();

        boolean valid = true;

        binding.tilIdentity.setError(null);
        binding.tilPassword.setError(null);

        if (identitas.isEmpty()) {
            binding.tilIdentity.setError(
                    getString(R.string.auth_error_required)
            );
            valid = false;
        }

        if (sandi.isEmpty()) {
            binding.tilPassword.setError(
                    getString(R.string.auth_error_required)
            );
            valid = false;
        }

        return valid;
    }

    @Override
    protected void onDestroy() {
        if (networkExecutor != null) {
            networkExecutor.shutdown();
        }

        binding = null;
        super.onDestroy();
    }
}
