<?php

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

function keluar($data, $kode = 200)
{
    http_response_code($kode);

    echo json_encode(
        $data,
        JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT
    );

    exit;
}

// Tangani OPTIONS.
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

// Endpoint ini hanya menerima POST.
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    keluar([
        'status' => 'error',
        'message' => 'Gunakan metode POST.'
    ], 405);
}

// =====================================================
// KONFIGURASI
// =====================================================

$host = 'localhost';
$dbname = 'rikua';
$username = 'root';
$password = '';

$batasUkuran = 2 * 1024 * 1024; // 2 MB

// =====================================================
// VALIDASI INPUT
// =====================================================

$idPendaftaran = filter_var(
    $_POST['id_pendaftaran'] ?? null,
    FILTER_VALIDATE_INT
);

$idJenisDokumen = filter_var(
    $_POST['id_jenis_dokumen'] ?? null,
    FILTER_VALIDATE_INT
);

$kodeTiket = trim($_POST['kode_tiket'] ?? '');

if (
    $idPendaftaran === false ||
    $idPendaftaran === null ||
    $idPendaftaran <= 0
) {
    keluar([
        'status' => 'error',
        'message' => 'ID pendaftaran tidak valid.'
    ], 400);
}

if (
    $idJenisDokumen === false ||
    $idJenisDokumen === null ||
    $idJenisDokumen <= 0
) {
    keluar([
        'status' => 'error',
        'message' => 'Jenis dokumen tidak valid.'
    ], 400);
}

if ($kodeTiket === '') {
    keluar([
        'status' => 'error',
        'message' => 'Kode tiket wajib diisi.'
    ], 400);
}

// =====================================================
// VALIDASI FILE
// Nama field file yang akan digunakan Android: file
// =====================================================

if (!isset($_FILES['file'])) {
    keluar([
        'status' => 'error',
        'message' => 'File dokumen belum diterima server.'
    ], 400);
}

$file = $_FILES['file'];

if ($file['error'] !== UPLOAD_ERR_OK) {
    $pesan = 'Upload file gagal.';

    if ($file['error'] === UPLOAD_ERR_INI_SIZE ||
        $file['error'] === UPLOAD_ERR_FORM_SIZE) {
        $pesan = 'Ukuran file melebihi batas server.';
    } elseif ($file['error'] === UPLOAD_ERR_NO_FILE) {
        $pesan = 'Pilih file terlebih dahulu.';
    }

    keluar([
        'status' => 'error',
        'message' => $pesan,
        'upload_error' => $file['error']
    ], 400);
}

if (!is_uploaded_file($file['tmp_name'])) {
    keluar([
        'status' => 'error',
        'message' => 'File upload tidak valid.'
    ], 400);
}

$ukuranByte = (int) $file['size'];

if ($ukuranByte <= 0) {
    keluar([
        'status' => 'error',
        'message' => 'File kosong atau tidak valid.'
    ], 400);
}

if ($ukuranByte > $batasUkuran) {
    keluar([
        'status' => 'error',
        'message' => 'Ukuran file maksimal 2 MB.'
    ], 413);
}

// Tentukan tipe file berdasarkan isi file,
// bukan hanya ekstensi nama file.
$finfo = new finfo(FILEINFO_MIME_TYPE);
$mime = $finfo->file($file['tmp_name']);

$tipeDiizinkan = [
    'application/pdf' => 'pdf',
    'image/jpeg' => 'jpg',
    'image/png' => 'png'
];

if (!isset($tipeDiizinkan[$mime])) {
    keluar([
        'status' => 'error',
        'message' => 'Format file harus PDF, JPG, JPEG, atau PNG.'
    ], 415);
}

$ekstensi = $tipeDiizinkan[$mime];

// =====================================================
// KONEKSI DAN VALIDASI DATABASE
// =====================================================

$pdo = null;
$pathBaru = null;
$pathLama = null;
$transaksiAktif = false;

try {

    $pdo = new PDO(
        "mysql:host=$host;dbname=$dbname;charset=utf8mb4",
        $username,
        $password,
        [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC
        ]
    );

    // Pastikan pendaftaran dan kode tiket cocok.
    $stmt = $pdo->prepare(
        "SELECT id_pendaftaran
         FROM trx_pendaftaran_nikah
         WHERE id_pendaftaran = ?
           AND kode_tiket = ?
         LIMIT 1"
    );

    $stmt->execute([
        $idPendaftaran,
        $kodeTiket
    ]);

    if (!$stmt->fetch()) {
        keluar([
            'status' => 'error',
            'message' => 'Pendaftaran atau kode tiket tidak cocok.'
        ], 404);
    }

    // Pastikan jenis dokumen ada dan aktif.
    $stmt = $pdo->prepare(
        "SELECT id_jenis_dokumen
         FROM mst_jenis_dokumen
         WHERE id_jenis_dokumen = ?
           AND aktif = 1
         LIMIT 1"
    );

    $stmt->execute([$idJenisDokumen]);

    if (!$stmt->fetch()) {
        keluar([
            'status' => 'error',
            'message' => 'Jenis dokumen tidak ditemukan atau tidak aktif.'
        ], 404);
    }

    // Cari dokumen terdahulu untuk pendaftaran dan jenis yang sama.
    $stmt = $pdo->prepare(
        "SELECT id_dokumen, file_path
         FROM trx_dokumen_pendaftaran
         WHERE id_pendaftaran = ?
           AND id_jenis_dokumen = ?
         ORDER BY id_dokumen DESC
         LIMIT 1"
    );

    $stmt->execute([
        $idPendaftaran,
        $idJenisDokumen
    ]);

    $dokumenLama = $stmt->fetch();

    if ($dokumenLama) {
        $pathLama = $dokumenLama['file_path'];
    }

    // =================================================
    // SIMPAN FILE KE SERVER
    // =================================================

    $folderRelatif = 'uploads/dokumen/' . $idPendaftaran;
    $folderAbsolut = __DIR__ . '/' . $folderRelatif;

    if (
        !is_dir($folderAbsolut) &&
        !mkdir($folderAbsolut, 0750, true) &&
        !is_dir($folderAbsolut)
    ) {
        throw new RuntimeException(
            'Folder upload tidak dapat dibuat.'
        );
    }

    if (!is_writable($folderAbsolut)) {
        throw new RuntimeException(
            'Folder upload tidak memiliki izin tulis.'
        );
    }

    // Nama file dibuat oleh server agar tidak menggunakan
    // nama file asli sebagai nama file penyimpanan.
    $namaSimpan = sprintf(
        'dok_%d_%d_%s.%s',
        $idPendaftaran,
        $idJenisDokumen,
        bin2hex(random_bytes(8)),
        $ekstensi
    );

    $tujuan = $folderAbsolut . '/' . $namaSimpan;
    $pathBaru = $folderRelatif . '/' . $namaSimpan;

    if (!move_uploaded_file($file['tmp_name'], $tujuan)) {
        throw new RuntimeException(
            'File tidak berhasil dipindahkan ke folder penyimpanan.'
        );
    }

    $ukuranKb = (int) ceil($ukuranByte / 1024);

    // =================================================
    // SIMPAN METADATA FILE KE DATABASE
    // =================================================

    $pdo->beginTransaction();
    $transaksiAktif = true;

    if ($dokumenLama) {

        $stmt = $pdo->prepare(
            "UPDATE trx_dokumen_pendaftaran
             SET file_path = ?,
                 ukuran_kb = ?,
                 status = 'terunggah',
                 catatan = NULL,
                 uploaded_at = CURRENT_TIMESTAMP
             WHERE id_dokumen = ?"
        );

        $stmt->execute([
            $pathBaru,
            $ukuranKb,
            $dokumenLama['id_dokumen']
        ]);

        $idDokumen = (int) $dokumenLama['id_dokumen'];

    } else {

        $stmt = $pdo->prepare(
            "INSERT INTO trx_dokumen_pendaftaran
                (id_pendaftaran, id_jenis_dokumen,
                 file_path, ukuran_kb, status)
             VALUES (?, ?, ?, ?, 'terunggah')"
        );

        $stmt->execute([
            $idPendaftaran,
            $idJenisDokumen,
            $pathBaru,
            $ukuranKb
        ]);

        $idDokumen = (int) $pdo->lastInsertId();
    }

    $pdo->commit();
    $transaksiAktif = false;

    // Setelah data baru berhasil disimpan, hapus file lama
    // jika memang berada di direktori upload dokumen ini.
    if (
        $pathLama &&
        $pathLama !== $pathBaru &&
        strpos($pathLama, 'uploads/dokumen/') === 0
    ) {
        $akarUpload = realpath(
            __DIR__ . '/uploads/dokumen'
        );

        $fileLamaAbsolut = realpath(
            __DIR__ . '/' . $pathLama
        );

        if (
            $akarUpload !== false &&
            $fileLamaAbsolut !== false &&
            strpos(
                $fileLamaAbsolut,
                $akarUpload . DIRECTORY_SEPARATOR
            ) === 0 &&
            is_file($fileLamaAbsolut)
        ) {
            @unlink($fileLamaAbsolut);
        }
    }

    keluar([
        'status' => 'success',
        'message' => 'Dokumen berhasil diunggah.',
        'id_dokumen' => $idDokumen,
        'id_pendaftaran' => (int) $idPendaftaran,
        'id_jenis_dokumen' => (int) $idJenisDokumen,
        'file_path' => $pathBaru,
        'ukuran_kb' => $ukuranKb,
        'status_dokumen' => 'terunggah'
    ]);

} catch (Throwable $e) {

    if ($transaksiAktif && $pdo instanceof PDO &&
        $pdo->inTransaction()) {
        $pdo->rollBack();
    }

    // Jika database gagal setelah file dipindahkan,
    // hapus file baru yang belum tercatat.
    if ($pathBaru !== null) {
        $fileBaruAbsolut = __DIR__ . '/' . $pathBaru;

        if (is_file($fileBaruAbsolut)) {
            @unlink($fileBaruAbsolut);
        }
    }

    error_log(
        'upload_dokumen: ' . $e->getMessage()
    );

    keluar([
        'status' => 'error',
        'message' => 'Upload gagal diproses oleh server.'
    ], 500);
}