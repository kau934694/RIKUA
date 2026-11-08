<?php

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
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

// Tangani permintaan OPTIONS jika diperlukan.
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

// =====================================================
// KONEKSI DATABASE
// =====================================================

$host = 'localhost';
$dbname = 'rikua';
$username = 'root';
$password = '';

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

    // =================================================
    // 1. POST: KIRIM PENDAFTARAN DARI ANDROID STEP 6
    // =================================================

    if ($_SERVER['REQUEST_METHOD'] === 'POST') {

        // Ambil ID dari body POST, bukan dari GET.
        $idPost = filter_var(
            $_POST['id_pendaftaran'] ?? null,
            FILTER_VALIDATE_INT
        );

        $tiketPost = trim(
            $_POST['kode_tiket'] ?? ''
        );

        $persetujuan = $_POST['persetujuan_data'] ?? '';

        // Validasi ID.
        if (
            $idPost === false ||
            $idPost === null ||
            $idPost <= 0
        ) {
            keluar([
                'status' => 'error',
                'message' => 'ID pendaftaran tidak valid.'
            ], 400);
        }

        // Validasi kode tiket dan persetujuan.
        if (
            $tiketPost === '' ||
            $persetujuan !== '1'
        ) {
            keluar([
                'status' => 'error',
                'message' => 'Kode tiket atau persetujuan data tidak valid.'
            ], 400);
        }

        // ---------------------------------------------
        // Pastikan pendaftaran dan kode tiket cocok.
        // ---------------------------------------------

        $stmt = $pdo->prepare(
            "SELECT id_pendaftaran, kode_tiket
             FROM trx_pendaftaran_nikah
             WHERE id_pendaftaran = ?
             LIMIT 1"
        );

        $stmt->execute([$idPost]);
        $data = $stmt->fetch();

        if (!$data) {
            keluar([
                'status' => 'error',
                'message' => 'Data pendaftaran tidak ditemukan.'
            ], 404);
        }

        $tiketDatabase = trim(
            (string) ($data['kode_tiket'] ?? '')
        );

        if (
            $tiketDatabase === '' ||
            !hash_equals($tiketDatabase, $tiketPost)
        ) {
            keluar([
                'status' => 'error',
                'message' => 'Kode tiket tidak sesuai dengan data pendaftaran.'
            ], 400);
        }

        // ---------------------------------------------
        // Simpan status dan persetujuan.
        //
        // PENTING:
        // Pastikan kolom status mengizinkan nilai
        // 'dikirim' sesuai struktur database.
        // ---------------------------------------------

        $stmt = $pdo->prepare(
            "UPDATE trx_pendaftaran_nikah
             SET status = 'dikirim',
                 persetujuan_data = 1
             WHERE id_pendaftaran = ?
               AND kode_tiket = ?"
        );

        $stmt->execute([
            $idPost,
            $tiketPost
        ]);

        // ---------------------------------------------
        // Verifikasi ulang data setelah UPDATE.
        // ---------------------------------------------

        $stmt = $pdo->prepare(
            "SELECT
                id_pendaftaran,
                kode_tiket,
                status,
                persetujuan_data
             FROM trx_pendaftaran_nikah
             WHERE id_pendaftaran = ?
               AND kode_tiket = ?
             LIMIT 1"
        );

        $stmt->execute([
            $idPost,
            $tiketPost
        ]);

        $tersimpan = $stmt->fetch();

        if (
            !$tersimpan ||
            $tersimpan['status'] !== 'dikirim' ||
            (int) $tersimpan['persetujuan_data'] !== 1
        ) {
            keluar([
                'status' => 'error',
                'message' => 'Status pendaftaran belum berhasil dikonfirmasi.'
            ], 500);
        }

        // ---------------------------------------------
        // Respons sukses yang diperlukan Android.
        // ---------------------------------------------

        keluar([
            'status' => 'success',
            'message' => 'Pendaftaran berhasil dikirim.',
            'id_pendaftaran' => (int) $tersimpan['id_pendaftaran'],
            'kode_tiket' => $tersimpan['kode_tiket']
        ]);
    }

    // Tolak metode selain GET dan POST.
    if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
        keluar([
            'status' => 'error',
            'message' => 'Metode permintaan tidak didukung.'
        ], 405);
    }

    // =================================================
    // 2. GET: MUAT DATA UNTUK TINJAUAN STEP 6
    // =================================================

    $id = filter_input(
        INPUT_GET,
        'id_pendaftaran',
        FILTER_VALIDATE_INT
    );

    if (
        $id === false ||
        $id === null ||
        $id <= 0
    ) {
        keluar([
            'status' => 'error',
            'message' => 'Parameter id_pendaftaran wajib diisi.'
        ], 400);
    }

    // ---------------------------------------------
    // Ambil pendaftaran utama.
    // ---------------------------------------------

    $stmt = $pdo->prepare(
        "SELECT *
         FROM trx_pendaftaran_nikah
         WHERE id_pendaftaran = ?
         LIMIT 1"
    );

    $stmt->execute([$id]);
    $pendaftaran = $stmt->fetch();

    if (!$pendaftaran) {
        keluar([
            'status' => 'not_found',
            'message' => 'Pendaftaran tidak ditemukan.'
        ], 404);
    }

    // ---------------------------------------------
    // Ambil calon suami dan calon istri.
    // ---------------------------------------------

    $stmt = $pdo->prepare(
        "SELECT *
         FROM trx_calon_pengantin
         WHERE id_pendaftaran = ?"
    );

    $stmt->execute([$id]);
    $calon = $stmt->fetchAll();

    $suami = null;
    $istri = null;

    foreach ($calon as $orang) {

        $peran = strtolower(
            trim($orang['peran'] ?? '')
        );

        if (
            $peran === 'suami' ||
            $peran === 'calon_suami'
        ) {
            $suami = $orang;

        } elseif (
            $peran === 'istri' ||
            $peran === 'calon_istri'
        ) {
            $istri = $orang;
        }
    }

    if (!$suami && !$istri) {
        keluar([
            'status' => 'error',
            'message' => 'Data calon pengantin tidak ditemukan untuk pendaftaran ini.'
        ], 404);
    }

    // ---------------------------------------------
    // Ambil wali dan saksi.
    // ---------------------------------------------

    $pihak = [];

    try {

        $stmt = $pdo->prepare(
            "SELECT *
             FROM trx_pihak_terkait
             WHERE id_pendaftaran = ?"
        );

        $stmt->execute([$id]);
        $pihak = $stmt->fetchAll();

    } catch (PDOException $e) {

        error_log(
            'Tinjau pihak terkait: ' . $e->getMessage()
        );
    }

    // ---------------------------------------------
    // Hitung dokumen terverifikasi.
    // ---------------------------------------------

    $jumlahTerverifikasi = 0;

    try {

        $stmt = $pdo->prepare(
            "SELECT *
             FROM trx_dokumen_pendaftaran
             WHERE id_pendaftaran = ?"
        );

        $stmt->execute([$id]);
        $dokumenRows = $stmt->fetchAll();

        foreach ($dokumenRows as $dok) {

            foreach (
                [
                    'status_verifikasi',
                    'status_dokumen',
                    'status'
                ] as $kolom
            ) {

                if (
                    isset($dok[$kolom]) &&
                    in_array(
                        strtolower(
                            trim((string) $dok[$kolom])
                        ),
                        [
                            'terverifikasi',
                            'valid',
                            'verified',
                            'lengkap'
                        ],
                        true
                    )
                ) {
                    $jumlahTerverifikasi++;
                    break;
                }
            }
        }

    } catch (PDOException $e) {

        error_log(
            'Tinjau dokumen: ' . $e->getMessage()
        );
    }

    // ---------------------------------------------
    // Respons data untuk Android Step 6.
    // ---------------------------------------------

    keluar([
        'status' => 'success',
        'id_pendaftaran' => (int) $id,
        'calon_suami' => $suami,
        'calon_istri' => $istri,
        'pihak_terkait' => $pihak,
        'pendaftaran' => $pendaftaran,
        'dokumen' => [
            'terverifikasi' => $jumlahTerverifikasi
        ]
    ]);

} catch (PDOException $e) {

    error_log(
        'langkah_6_tinjau database: ' . $e->getMessage()
    );

    keluar([
        'status' => 'error',
        'message' => 'Terjadi kesalahan database saat memproses pendaftaran.'
    ], 500);

} catch (Throwable $e) {

    error_log(
        'langkah_6_tinjau: ' . $e->getMessage()
    );

    keluar([
        'status' => 'error',
        'message' => 'Terjadi kesalahan saat memproses permintaan.'
    ], 500);
}