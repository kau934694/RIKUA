package com.example.rikua;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Window;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rikua.databinding.ActivityDaftarNikahStep5Binding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class DaftarNikahStep5Activity extends AppCompatActivity {

    private static final String URL_SIMKAH =
            "https://simkah.kemenag.go.id";

    private static final String API_DAFTAR_DOKUMEN =
            "http://192.168.1.5/rikua_api/get_jenis_dokumen.php";

    private static final String API_UPLOAD_DOKUMEN =
            "http://192.168.1.5/rikua_api/upload_dokumen.php";

    private static final long MAKS_UKURAN_BYTE =
            2L * 1024L * 1024L;

    private static final String DIR_FOTO_KAMERA = "photos";

    private int idPendaftaran = 0;
    private String kodeTiket = "";

    private ActivityDaftarNikahStep5Binding binding;

    private final List<DokumenItem> dokumenUtama =
            new ArrayList<>();

    private final List<DokumenItem> dokumenTambahan =
            new ArrayList<>();

    private final Map<Integer, Boolean> wajibPerJenis =
            new HashMap<>();

    private DokumenUploadAdapter adapterUtama;
    private DokumenUploadAdapter adapterTambahan;
    private boolean dokumenDimuat = false;

    private DokumenUploadAdapter adapterTarget;
    private int posisiTarget = -1;

    private ActivityResultLauncher<Intent> pickFileLauncher;
    private ActivityResultLauncher<Uri> takePhotoLauncher;

    private File berkasFoto;

    // =========================================================
    // LIFECYCLE
    // =========================================================

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDaftarNikahStep5Binding.inflate(
                getLayoutInflater()
        );

        setContentView(binding.getRoot());

        idPendaftaran = getIntent().getIntExtra(
                "id_pendaftaran", 0
        );

        String tiket = getIntent().getStringExtra("kode_tiket");
        kodeTiket = tiket != null ? tiket : "";

        registerLaunchers();
        setupWindowAndStatusBar();
        setupToolbar();
        setupDrawer();
        setupDocumentLists();
        setupActionButtons();
        setupBottomNavigation();

        perbaruiRingkasanKelengkapan();
    }

    @Override
    protected void onDestroy() {
        binding = null;
        adapterTarget = null;
        super.onDestroy();
    }

    // =========================================================
    // FILE PICKER DAN KAMERA
    // =========================================================

    private void registerLaunchers() {

        pickFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK
                            && result.getData() != null
                            && result.getData().getData() != null) {

                        prosesBerkasTerpilih(
                                result.getData().getData()
                        );
                    } else {
                        resetTarget();
                    }
                }
        );

        takePhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (Boolean.TRUE.equals(success)
                            && berkasFoto != null
                            && berkasFoto.exists()) {

                        prosesFotoKamera();
                    } else {
                        resetTarget();
                    }
                }
        );
    }

    private void mulaiPilihBerkas(
            DokumenUploadAdapter adapter,
            int position
    ) {
        if (position < 0 || position >= adapter.getItemCount()) {
            tampilkanPesan("Baris dokumen tidak valid.");
            return;
        }

        adapterTarget = adapter;
        posisiTarget = position;

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");

        intent.putExtra(
                Intent.EXTRA_MIME_TYPES,
                new String[]{
                        "application/pdf",
                        "image/jpeg",
                        "image/png"
                }
        );

        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);

        try {
            pickFileLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            resetTarget();
            tampilkanPesan(
                    getString(R.string.s5_picker_unavailable)
            );
        }
    }

    private void mulaiFotoKamera(
            DokumenUploadAdapter adapter,
            int position
    ) {
        if (position < 0 || position >= adapter.getItemCount()) {
            tampilkanPesan("Baris dokumen tidak valid.");
            return;
        }

        adapterTarget = adapter;
        posisiTarget = position;

        try {
            File dir = new File(
                    getCacheDir(),
                    DIR_FOTO_KAMERA
            );

            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException(
                        "Folder foto tidak dapat dibuat."
                );
            }

            berkasFoto = new File(
                    dir,
                    "DOK_" + System.currentTimeMillis() + ".jpg"
            );

            Uri uriFoto = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    berkasFoto
            );

            takePhotoLauncher.launch(uriFoto);

        } catch (
                IOException |
                IllegalArgumentException |
                ActivityNotFoundException e
        ) {
            resetTarget();

            tampilkanPesan(
                    getString(R.string.s5_camera_failed)
            );
        }
    }

    // =========================================================
    // VALIDASI DAN PROSES FILE
    // =========================================================

    private void prosesBerkasTerpilih(Uri uri) {
        if (!targetValid()) {
            resetTarget();
            return;
        }

        String namaFile = queryDisplayName(uri);
        long ukuran = queryUkuran(uri);

        if (ukuran < 0) {
            tampilkanPesan(
                    "Ukuran file tidak dapat dibaca."
            );
            resetTarget();
            return;
        }

        if (ukuran == 0) {
            tampilkanPesan("File kosong.");
            resetTarget();
            return;
        }

        if (ukuran > MAKS_UKURAN_BYTE) {
            gagalUkuran(ukuran);
            return;
        }

        mulaiUpload(uri, null, namaFile, ukuran);
    }

    private void prosesFotoKamera() {
        if (!targetValid()
                || berkasFoto == null
                || !berkasFoto.exists()) {

            resetTarget();
            return;
        }

        File foto = berkasFoto;
        long ukuran = foto.length();

        if (ukuran <= 0) {
            tampilkanPesan(
                    "Foto kosong atau gagal dibuat."
            );
            resetTarget();
            return;
        }

        if (ukuran > MAKS_UKURAN_BYTE) {
            gagalUkuran(ukuran);
            return;
        }

        mulaiUpload(
                null,
                foto,
                foto.getName(),
                ukuran
        );
    }

    private boolean targetValid() {
        return adapterTarget != null
                && posisiTarget >= 0
                && posisiTarget < adapterTarget.getItemCount();
    }

    private void gagalUkuran(long ukuranByte) {
        Snackbar.make(
                binding.getRoot(),
                getString(
                        R.string.s5_err_ukuran,
                        formatUkuran(ukuranByte)
                ),
                Snackbar.LENGTH_LONG
        ).show();

        resetTarget();
    }

    private void resetTarget() {
        adapterTarget = null;
        posisiTarget = -1;
        berkasFoto = null;
    }

    // =========================================================
    // UPLOAD FILE KE SERVER
    // =========================================================

    private void mulaiUpload(
            Uri uri,
            @Nullable File fileLokal,
            String namaFile,
            long ukuran
    ) {
        if (!targetValid()) {
            resetTarget();
            return;
        }

        if (idPendaftaran <= 0 || kodeTiket.trim().isEmpty()) {
            tampilkanPesan(
                    "ID pendaftaran atau kode tiket tidak valid."
            );
            resetTarget();
            return;
        }

        DokumenUploadAdapter adapter = adapterTarget;
        int position = posisiTarget;
        DokumenItem item = adapter.getItem(position);
        if (!item.isDiperlukan()) {
    tampilkanPesan(
            "Dokumen ini tidak diperlukan untuk pendaftaran kamu."
    );

    resetTarget();
    return;
}

        int idJenisDokumen = item.getIdJenisDokumen();

        if (idJenisDokumen <= 0) {
            tampilkanPesan(
                    "ID jenis dokumen tidak valid. Muat ulang daftar dokumen."
            );
            resetTarget();
            return;
        }

        resetTarget();

        Snackbar.make(
                binding.getRoot(),
                "Mengunggah " + namaFile + "...",
                Snackbar.LENGTH_LONG
        ).show();

        new Thread(() -> {
            try {
                byte[] isiFile = bacaIsiFile(
                        uri,
                        fileLokal
                );

                String mime = tentukanMime(
                        uri,
                        namaFile
                );

                JSONObject respons = kirimFileKeServer(
                        isiFile,
                        namaFile,
                        mime,
                        idJenisDokumen
                );

                int idDokumen = respons.optInt(
                        "id_dokumen", 0
                );

                String filePath = respons.optString(
                        "file_path", ""
                );

                String statusDokumen = respons.optString(
                        "status_dokumen", ""
                );

                if (idDokumen <= 0
                        || filePath.trim().isEmpty()
                        || !"terunggah".equals(statusDokumen)) {
                    throw new IOException(
                            "Respons server belum mengonfirmasi file tersimpan."
                    );
                }

                runOnUiThread(() -> {
                    if (binding == null) return;

                    item.tandaiTerunggah(
                            namaFile,
                            filePath,
                            idDokumen
                    );

                    adapter.notifyItemChanged(position);

                    perbaruiRingkasanKelengkapan();

                    Snackbar.make(
                            binding.getRoot(),
                            "Dokumen berhasil diunggah: " + namaFile,
                            Snackbar.LENGTH_LONG
                    ).show();

                    if (fileLokal != null && fileLokal.exists()) {
                        // Hapus foto sementara hanya setelah server
                        // mengonfirmasi bahwa file berhasil disimpan.
                        //noinspection ResultOfMethodCallIgnored
                        fileLokal.delete();
                    }
                });

            } catch (Exception e) {
                android.util.Log.e(
                        "STEP5_UPLOAD",
                        "Upload dokumen gagal",
                        e
                );

                runOnUiThread(() -> {
                    if (binding == null) return;

                    Snackbar.make(
                            binding.getRoot(),
                            "Upload gagal: "
                                    + (e.getMessage() != null
                                    ? e.getMessage()
                                    : "Kesalahan tidak diketahui"),
                            Snackbar.LENGTH_LONG
                    ).show();
                });
            }
        }).start();
    }

    private byte[] bacaIsiFile(
            @Nullable Uri uri,
            @Nullable File fileLokal
    ) throws IOException {

        InputStream sumber;

        if (fileLokal != null) {
            sumber = new FileInputStream(fileLokal);
        } else if (uri != null) {
            sumber = getContentResolver().openInputStream(uri);
        } else {
            throw new IOException("Sumber file tidak tersedia.");
        }

        if (sumber == null) {
            throw new IOException("File tidak dapat dibaca.");
        }

        try (
                InputStream in = sumber;
                ByteArrayOutputStream out =
                        new ByteArrayOutputStream()
        ) {
            byte[] buffer = new byte[8192];
            int jumlah;
            long total = 0;

            while ((jumlah = in.read(buffer)) != -1) {
                total += jumlah;

                if (total > MAKS_UKURAN_BYTE) {
                    throw new IOException(
                            "Ukuran file melebihi batas 2 MB."
                    );
                }

                out.write(buffer, 0, jumlah);
            }

            if (total == 0) {
                throw new IOException("File kosong.");
            }

            return out.toByteArray();
        }
    }

    private String tentukanMime(
            @Nullable Uri uri,
            String namaFile
    ) throws IOException {

        String mime = null;

        if (uri != null) {
            mime = getContentResolver().getType(uri);
        }

        if (mime != null) {
            int semicolon = mime.indexOf(';');

            if (semicolon >= 0) {
                mime = mime.substring(0, semicolon);
            }

            mime = mime.trim().toLowerCase(Locale.ROOT);
        }

        String nama = namaFile.toLowerCase(Locale.ROOT);

        if (mime == null
                || mime.isEmpty()
                || "application/octet-stream".equals(mime)) {

            if (nama.endsWith(".pdf")) {
                mime = "application/pdf";
            } else if (
                    nama.endsWith(".jpg")
                    || nama.endsWith(".jpeg")
            ) {
                mime = "image/jpeg";
            } else if (nama.endsWith(".png")) {
                mime = "image/png";
            }
        }

        if (!"application/pdf".equals(mime)
                && !"image/jpeg".equals(mime)
                && !"image/png".equals(mime)) {

            throw new IOException(
                    "Format file harus PDF, JPG, JPEG, atau PNG."
            );
        }

        return mime;
    }

    private JSONObject kirimFileKeServer(
            byte[] isiFile,
            String namaFile,
            String mime,
            int idJenisDokumen
    ) throws Exception {

        String boundary = "----RIKUA-" + UUID.randomUUID();

        Map<String, String> fields = new HashMap<>();

        fields.put(
                "id_pendaftaran",
                String.valueOf(idPendaftaran)
        );

        fields.put("kode_tiket", kodeTiket);

        fields.put(
                "id_jenis_dokumen",
                String.valueOf(idJenisDokumen)
        );

        byte[] body = buatBodyMultipart(
                boundary,
                fields,
                namaFile,
                mime,
                isiFile
        );

        HttpURLConnection conn = null;

        try {
            conn = (HttpURLConnection)
                    new URL(API_UPLOAD_DOKUMEN).openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(20000);

            conn.setRequestProperty(
                    "Content-Type",
                    "multipart/form-data; boundary=" + boundary
            );

            conn.setRequestProperty(
                    "Accept",
                    "application/json"
            );

            conn.setFixedLengthStreamingMode(body.length);

            try (OutputStream out = conn.getOutputStream()) {
                out.write(body);
                out.flush();
            }

            int httpCode = conn.getResponseCode();

            InputStream stream =
                    httpCode >= 200 && httpCode < 300
                            ? conn.getInputStream()
                            : conn.getErrorStream();

            if (stream == null) {
                throw new IOException(
                        "Server tidak memberikan respons. HTTP "
                                + httpCode
                );
            }

            StringBuilder sb = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, "UTF-8"))) {

                String line;

                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            String teksRespons = sb.toString();

            android.util.Log.d(
                    "STEP5_UPLOAD",
                    "HTTP " + httpCode
                            + ", respons: " + teksRespons
            );

            JSONObject json;

            try {
                json = new JSONObject(teksRespons);
            } catch (Exception e) {
                throw new IOException(
                        "Respons upload bukan JSON yang valid. HTTP "
                                + httpCode
                );
            }

            if (httpCode < 200 || httpCode >= 300) {
                throw new IOException(
                        json.optString(
                                "message",
                                "Upload ditolak server. HTTP " + httpCode
                        )
                );
            }

            if (!"success".equals(json.optString("status"))) {
                throw new IOException(
                        json.optString(
                                "message",
                                "Server belum mengonfirmasi upload."
                        )
                );
            }

            if (json.optInt("id_pendaftaran", -1)
                    != idPendaftaran) {
                throw new IOException(
                        "ID pendaftaran pada respons server tidak cocok."
                );
            }

            if (json.optInt("id_jenis_dokumen", -1)
                    != idJenisDokumen) {
                throw new IOException(
                        "Jenis dokumen pada respons server tidak cocok."
                );
            }

            return json;

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private byte[] buatBodyMultipart(
            String boundary,
            Map<String, String> fields,
            String namaFile,
            String mime,
            byte[] isiFile
    ) throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        for (Map.Entry<String, String> field : fields.entrySet()) {
            tulisString(
                    out,
                    "--" + boundary + "\r\n"
                            + "Content-Disposition: form-data; name=\""
                            + field.getKey()
                            + "\"\r\n\r\n"
                            + field.getValue()
                            + "\r\n"
            );
        }

        String namaAman = namaFile
                .replace("\\", "_")
                .replace("\"", "_")
                .replace("\r", "_")
                .replace("\n", "_");

        tulisString(
                out,
                "--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"file\"; filename=\""
                        + namaAman
                        + "\"\r\n"
                        + "Content-Type: " + mime + "\r\n\r\n"
        );

        out.write(isiFile);

        tulisString(
                out,
                "\r\n--" + boundary + "--\r\n"
        );

        return out.toByteArray();
    }

    private void tulisString(
            ByteArrayOutputStream out,
            String teks
    ) throws IOException {

        out.write(
                teks.getBytes(StandardCharsets.UTF_8)
        );
    }

    // =========================================================
    // METADATA FILE
    // =========================================================

    private String queryDisplayName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(
                uri,
                new String[]{OpenableColumns.DISPLAY_NAME},
                null,
                null,
                null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME
                );

                if (index >= 0 && !cursor.isNull(index)) {
                    String nama = cursor.getString(index);

                    if (nama != null && !nama.trim().isEmpty()) {
                        return nama;
                    }
                }
            }
        } catch (Exception ignored) {
            // Gunakan fallback.
        }

        String last = uri.getLastPathSegment();

        return last != null
                ? last
                : "berkas_dokumen";
    }

    private long queryUkuran(Uri uri) {
        try (Cursor cursor = getContentResolver().query(
                uri,
                new String[]{OpenableColumns.SIZE},
                null,
                null,
                null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(
                        OpenableColumns.SIZE
                );

                if (index >= 0 && !cursor.isNull(index)) {
                    return cursor.getLong(index);
                }
            }
        } catch (Exception ignored) {
            // Lanjutkan dengan membaca stream.
        }

        try (InputStream in =
                     getContentResolver().openInputStream(uri)) {

            if (in == null) return -1;

            long total = 0;
            byte[] buffer = new byte[8192];
            int read;

            while ((read = in.read(buffer)) != -1) {
                total += read;

                if (total > MAKS_UKURAN_BYTE) {
                    return total;
                }
            }

            return total;

        } catch (IOException e) {
            return -1;
        }
    }

    private String formatUkuran(long byteCount) {
        if (byteCount < 0) return "?";

        if (byteCount >= 1024L * 1024L) {
            return String.format(
                    Locale.US,
                    "%.1f MB",
                    byteCount / (1024f * 1024f)
            );
        }

        return String.format(
                Locale.US,
                "%.0f KB",
                byteCount / 1024f
        );
    }

    // =========================================================
    // DAFTAR DOKUMEN DARI DATABASE
    // =========================================================

    private void setupDocumentLists() {
        dokumenDimuat = false;

        dokumenUtama.clear();
        dokumenTambahan.clear();
        wajibPerJenis.clear();

        adapterUtama = siapkanRecyclerView(
                binding.rvDokumenUtama,
                dokumenUtama
        );

        adapterTambahan = siapkanRecyclerView(
                binding.rvDokumenTambahan,
                dokumenTambahan
        );

        muatDaftarDokumenDariServer();
    }

    
private void muatDaftarDokumenDariServer() {
    if (idPendaftaran <= 0) {
        tampilkanPesan(
                "ID pendaftaran tidak valid. Kembali ke langkah sebelumnya."
        );
        return;
    }

    android.util.Log.d(
            "STEP5_DOKUMEN",
            "Mulai memuat dokumen. ID pendaftaran: " + idPendaftaran
    );

    new Thread(() -> {
        HttpURLConnection conn = null;

        try {
            String urlApi = API_DAFTAR_DOKUMEN
                    + "?id_pendaftaran="
                    + URLEncoder.encode(
                            String.valueOf(idPendaftaran),
                            "UTF-8"
                    );

            android.util.Log.d(
                    "STEP5_DOKUMEN",
                    "Request URL: " + urlApi
            );

            conn = (HttpURLConnection)
                    new URL(urlApi).openConnection();

            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty(
                    "Accept",
                    "application/json"
            );

            int httpCode = conn.getResponseCode();

            InputStream stream =
                    httpCode >= 200 && httpCode < 300
                            ? conn.getInputStream()
                            : conn.getErrorStream();

            if (stream == null) {
                throw new IOException(
                        "Server tidak memberikan respons. HTTP "
                                + httpCode
                );
            }

            StringBuilder sb = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, "UTF-8"))) {

                String line;

                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            String teksRespons = sb.toString();

            android.util.Log.d(
                    "STEP5_DOKUMEN",
                    "HTTP " + httpCode
                            + ", respons: " + teksRespons
            );

            JSONObject root = new JSONObject(teksRespons);

            if (httpCode < 200 || httpCode >= 300
                    || !"success".equals(
                            root.optString("status"))) {

                throw new IOException(
                        root.optString(
                                "message",
                                "Daftar dokumen gagal dimuat."
                        )
                );
            }

            JSONArray data = root.optJSONArray("data");

            if (data == null) {
                throw new IOException(
                        "Field data tidak ditemukan dalam respons API."
                );
            }

            if (root.optJSONObject("kondisi") == null) {
                throw new IOException(
                        "Field kondisi tidak ditemukan dalam respons API."
                );
            }

            // Semua perubahan UI harus dijalankan di Main Thread.
            runOnUiThread(() -> {
                if (binding == null) return;

                try {
                    terapkanDaftarDokumen(root);

                    android.util.Log.d(
                            "STEP5_DOKUMEN",
                            "Daftar dokumen berhasil diterapkan. Jumlah: "
                                    + data.length()
                    );

                } catch (Exception e) {
                    android.util.Log.e(
                            "STEP5_DOKUMEN",
                            "Gagal menerapkan daftar dokumen",
                            e
                    );

                    dokumenDimuat = false;

                    tampilkanPesan(
                            "Gagal menampilkan dokumen: "
                                    + e.getMessage()
                    );
                }
            });

        } catch (Exception e) {
            android.util.Log.e(
                    "STEP5_DOKUMEN",
                    "Gagal mengambil daftar dokumen dari server",
                    e
            );

            runOnUiThread(() -> {
                if (binding == null) return;

                dokumenDimuat = false;

                tampilkanPesan(
                        "Gagal memuat dokumen: "
                                + (e.getMessage() != null
                                ? e.getMessage()
                                : "Kesalahan jaringan")
                );
            });

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }).start();
}

    private void terapkanDaftarDokumen(JSONObject root) {

    JSONObject kondisi = root.optJSONObject("kondisi");
    JSONArray data = root.optJSONArray("data");

    if (kondisi == null || data == null) {
        throw new IllegalArgumentException(
                "Objek kondisi atau daftar dokumen tidak tersedia."
        );
    }

    dokumenUtama.clear();
    dokumenTambahan.clear();
    wajibPerJenis.clear();

    // Kondisi dari database pendaftaran.
    boolean usiaDiBawah21 =
            kondisi.optBoolean("usia_di_bawah_21", false);

    boolean usiaDiBawah19 =
            kondisi.optBoolean("usia_di_bawah_19", false);

    boolean akadLuarKecamatan =
            kondisi.optBoolean("akad_luar_kecamatan", false);

    boolean ceraiMati =
            kondisi.optBoolean("cerai_mati", false);

    for (int i = 0; i < data.length(); i++) {

        JSONObject row = data.optJSONObject(i);

        if (row == null) continue;

        int idJenis = row.optInt(
                "id_jenis_dokumen", 0
        );

        if (idJenis <= 0) continue;

        String judul = row.optString(
                "nama_dokumen", "Dokumen"
        );

        String kategori = row.optString(
                "kategori", "utama"
        );

        boolean wajibMaster =
                row.optInt("wajib", 1) == 1;

        String kondisiKode = row.isNull("kondisi_kode")
                ? ""
                : row.optString("kondisi_kode", "");

        // Tentukan apakah dokumen diperlukan.
        boolean diperlukan;
        String deskripsi;

        switch (idJenis) {

            case 3:
                // N3 belum memiliki sumber kondisi di API.
                diperlukan = false;
                deskripsi =
                        "Belum diaktifkan sementara karena kondisi "
                        + "Isbat Nikah belum tersedia.";
                break;

            case 11:
                // N5: usia di bawah 21 tahun.
                diperlukan = usiaDiBawah21;
                deskripsi = diperlukan
                        ? "Wajib karena calon pengantin berusia di bawah 21 tahun."
                        : "Tidak diperlukan karena kondisi usia di bawah 21 tahun tidak terpenuhi.";
                break;

            case 12:
                // Dispensasi: usia di bawah 19 tahun.
                diperlukan = usiaDiBawah19;
                deskripsi = diperlukan
                        ? "Wajib karena kondisi usia di bawah 19 tahun terpenuhi."
                        : "Tidak diperlukan karena kondisi usia di bawah 19 tahun tidak terpenuhi.";
                break;

            case 13:
                // Rekomendasi luar kecamatan.
                diperlukan = akadLuarKecamatan;
                deskripsi = diperlukan
                        ? "Wajib karena akad dilakukan di luar kecamatan."
                        : "Tidak diperlukan karena akad tidak dilakukan di luar kecamatan.";
                break;

            case 14:
                // N6: salah satu calon berstatus cerai mati.
                diperlukan = ceraiMati;
                deskripsi = diperlukan
                        ? "Wajib karena salah satu calon berstatus cerai mati."
                        : "Tidak diperlukan karena kondisi cerai mati tidak terpenuhi.";
                break;

            default:
                // Dokumen wajib biasa.
                diperlukan = wajibMaster;
                deskripsi = diperlukan
                        ? "Dokumen wajib diunggah."
                        : "Dokumen tidak diwajibkan untuk pendaftaran ini.";
                break;
        }

        String statusServer = row.isNull("status")
                ? "belum_diunggah"
                : row.optString(
                        "status",
                        "belum_diunggah"
                );

        int idDokumen = row.isNull("id_dokumen")
                ? 0
                : row.optInt("id_dokumen", 0);

        String filePath = row.isNull("uploaded_file_path")
                ? ""
                : row.optString(
                        "uploaded_file_path", ""
                );

        String namaFile = "";

        if (!filePath.isEmpty()) {
            namaFile = new File(filePath).getName();
        }

        String catatan = row.isNull("catatan")
                ? ""
                : row.optString("catatan", "");

        int statusAwal = DokumenItem.STATUS_BELUM;

        if ("perlu_perbaikan".equals(statusServer)) {

            statusAwal =
                    DokumenItem.STATUS_PERLU_PERBAIKAN;

        } else if (
                "terunggah".equals(statusServer)
                || "diverifikasi".equals(statusServer)
        ) {

            statusAwal =
                    DokumenItem.STATUS_TERUNGGAH;
        }

        DokumenItem item = new DokumenItem(
                idJenis,
                judul,
                deskripsi,
                catatan,
                statusAwal
        );

        // Isi status dan informasi unggahan dari database.
        item.setStatusDariServer(
                statusServer,
                idDokumen,
                filePath,
                namaFile,
                catatan
        );

        // Aktifkan atau nonaktifkan dokumen berdasarkan kondisi.
        item.setDiperlukan(diperlukan);

        // Simpan status kewajiban aktual, bukan hanya nilai master.
        wajibPerJenis.put(idJenis, diperlukan);

        if ("utama".equals(kategori)) {
            dokumenUtama.add(item);
        } else {
            dokumenTambahan.add(item);
        }
    }

    adapterUtama.notifyDataSetChanged();
    adapterTambahan.notifyDataSetChanged();

    dokumenDimuat = !dokumenUtama.isEmpty()
            || !dokumenTambahan.isEmpty();

    perbaruiRingkasanKelengkapan();

    if (!dokumenDimuat) {
        tampilkanPesan(
                "Server belum mengembalikan jenis dokumen aktif."
        );
    }
}

    private DokumenUploadAdapter siapkanRecyclerView(
            RecyclerView rv,
            List<DokumenItem> items
    ) {
        rv.setHasFixedSize(false);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setNestedScrollingEnabled(false);

        DokumenUploadAdapter.OnDokumenActionListener listener =
                new DokumenUploadAdapter.OnDokumenActionListener() {

                    @Override
                    public void onPilihFile(int position) {
                        RecyclerView.Adapter<?> currentAdapter =
                                rv.getAdapter();

                        if (currentAdapter instanceof DokumenUploadAdapter) {
                            mulaiPilihBerkas(
                                    (DokumenUploadAdapter) currentAdapter,
                                    position
                            );
                        }
                    }

                    @Override
                    public void onAmbilFoto(int position) {
                        RecyclerView.Adapter<?> currentAdapter =
                                rv.getAdapter();

                        if (currentAdapter instanceof DokumenUploadAdapter) {
                            mulaiFotoKamera(
                                    (DokumenUploadAdapter) currentAdapter,
                                    position
                            );
                        }
                    }
                };

        DokumenUploadAdapter adapter =
                new DokumenUploadAdapter(items, listener);

        rv.setAdapter(adapter);

        return adapter;
    }

    // =========================================================
    // RINGKASAN DOKUMEN
    // =========================================================

    private void perbaruiRingkasanKelengkapan() {

    if (binding == null) return;

    int total = 0;
    int terunggah = 0;

    List<DokumenItem> semuaDokumen = new ArrayList<>();

    semuaDokumen.addAll(dokumenUtama);
    semuaDokumen.addAll(dokumenTambahan);

    for (DokumenItem item : semuaDokumen) {

        if (!item.isDiperlukan()) {
            continue;
        }

        total++;

        if (item.getStatus()
                == DokumenItem.STATUS_TERUNGGAH) {
            terunggah++;
        }
    }

    int persen = total > 0
            ? Math.round(100f * terunggah / total)
            : 0;

    binding.tvJumlahDokumen.setText(
            getString(
                    R.string.s5_summary_count,
                    terunggah,
                    total
            )
    );

    binding.tvPersenDokumen.setText(
            getString(
                    R.string.s5_summary_percent,
                    persen
            )
    );

    binding.progressDokumen.setMax(Math.max(total, 1));
    binding.progressDokumen.setProgress(terunggah);
}

    private int hitungBelumSelesai() {
        int sisa = 0;

        for (DokumenItem item : dokumenUtama) {
            if (wajibBelumSelesai(item)) {
                sisa++;
            }
        }

        for (DokumenItem item : dokumenTambahan) {
            if (wajibBelumSelesai(item)) {
                sisa++;
            }
        }

        return sisa;
    }

    private boolean wajibBelumSelesai(DokumenItem item) {

    return item.isDiperlukan()
            && item.getStatus()
            != DokumenItem.STATUS_TERUNGGAH;
}

    // =========================================================
    // WINDOW, TOOLBAR DAN DRAWER
    // =========================================================

    private void setupWindowAndStatusBar() {
        Window window = getWindow();

        window.setStatusBarColor(
                ContextCompat.getColor(
                        this,
                        R.color.md3_primary
                )
        );

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(
                        window,
                        window.getDecorView()
                );

        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(true);

        ViewCompat.setOnApplyWindowInsetsListener(
                binding.layoutBottomSticky,
                (view, insets) -> {
                    androidx.core.graphics.Insets bars =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                            );

                    view.setPadding(
                            view.getPaddingLeft(),
                            view.getPaddingTop(),
                            view.getPaddingRight(),
                            bars.bottom
                    );

                    return insets;
                }
        );
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(
                v -> finish()
        );

        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_open_drawer) {
                toggleDrawer();
                return true;
            }

            return false;
        });
    }

    private void setupDrawer() {
        binding.navDrawer.setCheckedItem(
                R.id.menu_daftar_nikah
        );

        binding.navDrawer.setNavigationItemSelectedListener(item -> {
            binding.drawerLayout.closeDrawer(
                    GravityCompat.END
            );

            handleNavigation(item.getItemId());

            return true;
        });
    }

    private void toggleDrawer() {
        if (binding.drawerLayout.isDrawerOpen(
                GravityCompat.END
        )) {
            binding.drawerLayout.closeDrawer(
                    GravityCompat.END
            );
        } else {
            binding.drawerLayout.openDrawer(
                    GravityCompat.END
            );
        }
    }

    // =========================================================
    // TOMBOL AKSI STEP 5
    // =========================================================

    private void setupActionButtons() {

        binding.btnSimpanDraf.setOnClickListener(v ->
                Snackbar.make(
                        binding.getRoot(),
                        R.string.s5_draf_saved,
                        Snackbar.LENGTH_LONG
                ).show()
        );

        binding.btnUnduhBlanko.setOnClickListener(v ->
                navigateTo(BlankoActivity.class)
        );

        binding.btnLanjutTinjau.setOnClickListener(v -> {
            if (!dokumenDimuat) {
                tampilkanPesan(
                        "Daftar dokumen belum berhasil dimuat."
                );
                return;
            }

            int sisa = hitungBelumSelesai();

            if (sisa > 0) {
                Snackbar.make(
                        binding.getRoot(),
                        getString(
                                R.string.s5_err_incomplete,
                                sisa
                        ),
                        Snackbar.LENGTH_LONG
                ).show();
                return;
            }

            Intent intent = new Intent(
                    DaftarNikahStep5Activity.this,
                    DaftarNikahStep6Activity.class
            );

            intent.putExtra(
                    "id_pendaftaran",
                    idPendaftaran
            );

            intent.putExtra(
                    "kode_tiket",
                    kodeTiket
            );

            startActivity(intent);
        });

        binding.tvBantuanPetugas.setOnClickListener(v ->
                startActivity(
                        new Intent(
                                DaftarNikahStep5Activity.this,
                                ConsultationActivity.class
                        )
                )
        );
    }

    // =========================================================
    // BOTTOM NAVIGATION
    // =========================================================

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav =
                binding.bottomNavigation;

        bottomNav.setSelectedItemId(
                R.id.menu_daftar_nikah
        );

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.menu_daftar_nikah) {
                return true;
            }

            if (id == R.id.menu_lainnya) {
                toggleDrawer();

                bottomNav.setSelectedItemId(
                        R.id.menu_daftar_nikah
                );

                return false;
            }

            handleNavigation(id);

            return true;
        });
    }

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
                startActivity(
                        new Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(URL_SIMKAH)
                        )
                );
            } catch (Exception e) {
                Toast.makeText(
                        this,
                        R.string.daf_simkah_unavailable,
                        Toast.LENGTH_SHORT
                ).show();
            }

        } else {
            Toast.makeText(
                    this,
                    R.string.daf_feature_soon,
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void navigateTo(Class<?> target) {
        Intent intent = new Intent(this, target);

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        startActivity(intent);
        finish();
    }

    private void tampilkanPesan(String pesan) {
        if (binding == null) return;

        Snackbar.make(
                binding.getRoot(),
                pesan,
                Snackbar.LENGTH_LONG
        ).show();
    }

    // =========================================================
    // BACK STACK
    // =========================================================

    @Override
    public void onBackPressed() {
        if (binding != null
                && binding.drawerLayout.isDrawerOpen(
                        GravityCompat.END
                )) {

            binding.drawerLayout.closeDrawer(
                    GravityCompat.END
            );
        } else {
            super.onBackPressed();
        }
    }
}