
package com.example.rikua;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "rikua_auth";

    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_NAMA = "nama_lengkap";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ROLE = "role";
    private static final String KEY_EXPIRES_AT = "expires_at";

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );
    }

    public void saveSession(
            String token,
            String userId,
            String namaLengkap,
            String email,
            String role,
            String expiresAt
    ) {
        preferences.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_USER_ID, userId)
                .putString(KEY_NAMA, namaLengkap)
                .putString(KEY_EMAIL, email)
                .putString(KEY_ROLE, role)
                .putString(KEY_EXPIRES_AT, expiresAt)
                .apply();
    }

    public boolean isLoggedIn() {
        String token = getToken();
        return token != null && !token.trim().isEmpty();
    }

    public String getToken() {
        return preferences.getString(KEY_TOKEN, null);
    }

    public String getUserId() {
        return preferences.getString(KEY_USER_ID, null);
    }

    public String getNamaLengkap() {
        return preferences.getString(KEY_NAMA, null);
    }

    public String getEmail() {
        return preferences.getString(KEY_EMAIL, null);
    }

    public String getRole() {
        return preferences.getString(KEY_ROLE, null);
    }

    public String getExpiresAt() {
        return preferences.getString(KEY_EXPIRES_AT, null);
    }

    public void clearSession() {
        preferences.edit().clear().apply();
    }
}
