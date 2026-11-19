
package com.example.rikua;

public final class ApiConfig {

    private ApiConfig() {
        // Mencegah pembuatan objek
    }

    public static final String BASE_URL =
            "http://192.168.1.8/RIKUA_auth_backend/";

    public static final String GOOGLE_LOGIN =
            BASE_URL + "auth/google_login.php";
            public static final String GOOGLE_WEB_CLIENT_ID =
        "1074875207002-q9mghcmk7eejovqam50289rk9hmsqk79.apps.googleusercontent.com";
        
        public static final String ADMIN_LOGIN =
        BASE_URL + "auth/admin_login.php";
}
