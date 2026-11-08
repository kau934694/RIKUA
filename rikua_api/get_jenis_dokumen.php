<?php
/**
 * API: get_jenis_dokumen.php
 * Mengambil dokumen aktif, status unggahan terakhir, dan kondisi
 * persyaratan untuk satu pendaftaran nikah.
 *
 * Contoh:
 * GET /rikua_api/get_jenis_dokumen.php?id_pendaftaran=62
 */

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

function keluar($payload, $httpCode = 200)
{
    http_response_code($httpCode);
    echo json_encode(
        $payload,
        JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES
    );
    exit;
}

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(204);
    exit;
}

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    keluar([
        'status' => 'error',
        'pesan' => 'Metode request tidak diizinkan. Gunakan GET.'
    ], 405);
}

$idPendaftaran = filter_input(INPUT_GET, 'id_pendaftaran', FILTER_VALIDATE_INT);
if ($idPendaftaran === false || $idPendaftaran === null || $idPendaftaran < 1) {
    keluar([
        'status' => 'error',
        'pesan' => 'Parameter id_pendaftaran wajib diisi dengan angka yang valid.'
    ], 400);
}

// Samakan konfigurasi koneksi dengan upload_dokumen.php.
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
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES => false
        ]
    );

    // 1. Ambil kondisi pendaftaran dan status perkawinan kedua calon.
    $sqlKondisi = "
        SELECT
            p.id_pendaftaran,
            p.usia_di_bawah_21,
            p.usia_di_bawah_19,
            p.akad_luar_kecamatan,
            p.anggota_tni_polri,
            p.poligami,
            p.daftar_kurang_10_hari,
            (
                SELECT cp.status_perkawinan
                FROM trx_calon_pengantin cp
                WHERE cp.id_pendaftaran = p.id_pendaftaran
                  AND cp.peran = 'suami'
                ORDER BY cp.id_calon DESC
                LIMIT 1
            ) AS status_perkawinan_suami,
            (
                SELECT cp.status_perkawinan
                FROM trx_calon_pengantin cp
                WHERE cp.id_pendaftaran = p.id_pendaftaran
                  AND cp.peran = 'istri'
                ORDER BY cp.id_calon DESC
                LIMIT 1
            ) AS status_perkawinan_istri
        FROM trx_pendaftaran_nikah p
        WHERE p.id_pendaftaran = :id_pendaftaran
        LIMIT 1
    ";

    $stmtKondisi = $pdo->prepare($sqlKondisi);
    $stmtKondisi->execute([':id_pendaftaran' => $idPendaftaran]);
    $rowKondisi = $stmtKondisi->fetch();

    if (!$rowKondisi) {
        keluar([
            'status' => 'error',
            'pesan' => 'Data pendaftaran tidak ditemukan.',
            'id_pendaftaran' => (int) $idPendaftaran
        ], 404);
    }

    $statusSuami = $rowKondisi['status_perkawinan_suami'] ?? null;
    $statusIstri = $rowKondisi['status_perkawinan_istri'] ?? null;

    $kondisi = [
        'usia_di_bawah_21' => ((int) $rowKondisi['usia_di_bawah_21'] === 1),
        'usia_di_bawah_19' => ((int) $rowKondisi['usia_di_bawah_19'] === 1),
        'akad_luar_kecamatan' => ((int) $rowKondisi['akad_luar_kecamatan'] === 1),
        'anggota_tni_polri' => ((int) $rowKondisi['anggota_tni_polri'] === 1),
        'poligami' => ((int) $rowKondisi['poligami'] === 1),
        'daftar_kurang_10_hari' => ((int) $rowKondisi['daftar_kurang_10_hari'] === 1),
        'status_perkawinan_suami' => $statusSuami,
        'status_perkawinan_istri' => $statusIstri,
        'cerai_mati' => ($statusSuami === 'cerai_mati' || $statusIstri === 'cerai_mati')
    ];

    // 2. Ambil dokumen aktif dan unggahan terakhir untuk pendaftaran ini.
    // Nama kolom template pada tabel blanko adalah file_pdf.
    $sqlDokumen = "
        SELECT
            j.id_jenis_dokumen,
            j.nama_dokumen,
            j.kategori,
            j.kondisi_kode,
            j.id_blanko,
            j.wajib,
            j.urutan,
            j.aktif,
            b.file_pdf AS template_blanko,
            td.id_dokumen,
            td.file_path AS uploaded_file_path,
            td.ukuran_kb,
            td.status,
            td.catatan,
            td.uploaded_at,
            td.diperiksa_at
        FROM mst_jenis_dokumen j
        LEFT JOIN blanko b
            ON b.id_blanko = j.id_blanko
        LEFT JOIN (
            SELECT d.*
            FROM trx_dokumen_pendaftaran d
            INNER JOIN (
                SELECT
                    id_pendaftaran,
                    id_jenis_dokumen,
                    MAX(id_dokumen) AS id_terakhir
                FROM trx_dokumen_pendaftaran
                WHERE id_pendaftaran = :id_pendaftaran_upload
                GROUP BY id_pendaftaran, id_jenis_dokumen
            ) terakhir
                ON terakhir.id_terakhir = d.id_dokumen
        ) td
            ON td.id_jenis_dokumen = j.id_jenis_dokumen
        WHERE j.aktif = 1
        ORDER BY j.urutan ASC, j.id_jenis_dokumen ASC
    ";

    $stmtDokumen = $pdo->prepare($sqlDokumen);
    $stmtDokumen->execute([':id_pendaftaran_upload' => $idPendaftaran]);
    $rowsDokumen = $stmtDokumen->fetchAll();

    $dokumenList = [];
    foreach ($rowsDokumen as $row) {
        $dokumenList[] = [
            'id_jenis_dokumen' => (int) $row['id_jenis_dokumen'],
            'nama_dokumen' => $row['nama_dokumen'],
            'kategori' => $row['kategori'],
            'kondisi_kode' => $row['kondisi_kode'],
            'id_blanko' => $row['id_blanko'] !== null ? (int) $row['id_blanko'] : null,
            'wajib' => (int) $row['wajib'],
            'urutan' => (int) $row['urutan'],
            'aktif' => (int) $row['aktif'],
            'template_blanko' => $row['template_blanko'],
            'id_dokumen' => $row['id_dokumen'] !== null ? (int) $row['id_dokumen'] : null,
            'uploaded_file_path' => $row['uploaded_file_path'],
            'ukuran_kb' => $row['ukuran_kb'] !== null ? (int) $row['ukuran_kb'] : null,
            'status' => $row['status'],
            'catatan' => $row['catatan'],
            'uploaded_at' => $row['uploaded_at'],
            'diperiksa_at' => $row['diperiksa_at']
        ];
    }

    keluar([
        'status' => 'success',
        'id_pendaftaran' => (int) $idPendaftaran,
        'kondisi' => $kondisi,
        'data' => $dokumenList
    ]);

} catch (Throwable $e) {
    // Detail error disimpan di log server, tidak dibuka ke aplikasi.
    error_log('get_jenis_dokumen.php: ' . $e->getMessage());
    keluar([
        'status' => 'error',
        'pesan' => 'Terjadi kesalahan saat mengambil data dokumen.'
    ], 500);
}
?>