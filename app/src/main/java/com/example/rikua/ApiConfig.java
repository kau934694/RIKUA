
package com.example.rikua;

public final class ApiConfig {

    private ApiConfig() {
        // Mencegah pembuatan objek
    }

    public static final String BASE_URL =
            "http://192.168.1.6/RIKUA_auth_backend/";

    public static final String GOOGLE_LOGIN =
            BASE_URL + "auth/google_login.php";
            public static final String GOOGLE_WEB_CLIENT_ID =
        "1074875207002-q9mghcmk7eejovqam50289rk9hmsqk79.apps.googleusercontent.com";

        public static final String ADMIN_LOGIN =
        BASE_URL + "auth/admin_login.php";

    // Ringkasan kerja petugas: jumlah, antrean berkas, dan kuota akad.
    public static final String ADMIN_DASHBOARD =
            BASE_URL + "admin_dashboard.php";

    // Portal SIMKAH tempat petugas melakukan input resmi.
    public static final String URL_SIMKAH_PETUGAS =
            "https://simkah4.kemenag.go.id/";
}
