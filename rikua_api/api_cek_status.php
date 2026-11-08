<?php
ini_set('display_errors', 0);
error_reporting(E_ALL);
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');

function keluar($arr) {
    $json = json_encode($arr, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT | JSON_INVALID_UTF8_SUBSTITUTE);
    echo ($json === false)
        ? json_encode(['status' => 'error', 'message' => 'JSON error: ' . json_last_error_msg()])
        : $json;
    exit;
}

$host     = 'localhost';
$dbname   = 'rikua';
$username = 'root';
$password = '';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    $kode_tiket = trim($_GET['kode_tiket'] ?? '');
    $tgl_raw    = trim($_GET['tanggal_lahir'] ?? '');

    if ($kode_tiket === '' || $tgl_raw === '') {
        keluar(["status" => "error",
                "message" => "Parameter kode_tiket dan tanggal_lahir wajib diisi."]);
    }

    // Terima dd/mm/yyyy (dari aplikasi) maupun yyyy-mm-dd
    $d = DateTime::createFromFormat('d/m/Y', $tgl_raw) ?: DateTime::createFromFormat('Y-m-d', $tgl_raw);
    if (!$d) {
        keluar(["status" => "error", "message" => "Format tanggal lahir tidak valid."]);
    }
    $tanggal_lahir = $d->format('Y-m-d');

    $stmt = $pdo->prepare("
        SELECT p.id_pendaftaran,
               p.kode_tiket,
               p.tanggal_daftar,
               p.tanggal_akad,
               p.lokasi_akad,
               p.status AS status_pendaftaran
        FROM trx_pendaftaran_nikah p
        JOIN trx_calon_pengantin c ON c.id_pendaftaran = p.id_pendaftaran
        WHERE p.kode_tiket = ? AND c.tanggal_lahir = ?
        LIMIT 1
    ");
    $stmt->execute([$kode_tiket, $tanggal_lahir]);
    $pendaftaran = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$pendaftaran) {
        keluar(["status" => "not_found",
                "message" => "Data pendaftaran tidak ditemukan untuk tiket dan tanggal lahir tersebut."]);
    }

    // Nama kedua calon (berguna untuk judul kartu hasil)
    $pendaftaran['nama_suami'] = null;
    $pendaftaran['nama_istri'] = null;
    $st = $pdo->prepare("SELECT peran, nama_lengkap FROM trx_calon_pengantin WHERE id_pendaftaran = ?");
    $st->execute([$pendaftaran['id_pendaftaran']]);
    foreach ($st->fetchAll(PDO::FETCH_ASSOC) as $r) {
        if ($r['peran'] === 'suami') $pendaftaran['nama_suami'] = $r['nama_lengkap'];
        if ($r['peran'] === 'istri') $pendaftaran['nama_istri'] = $r['nama_lengkap'];
    }

    // Catatan petugas terbaru dari dokumen (opsional; gagal tidak menggagalkan hasil)
    $pendaftaran['catatan_petugas'] = null;
    $pendaftaran['nama_petugas']    = null;
    $pendaftaran['jabatan_petugas'] = null;
    try {
        $st = $pdo->prepare("
            SELECT d.catatan, pt.nama_petugas, pt.jabatan
            FROM trx_dokumen_pendaftaran d
            LEFT JOIN petugas pt ON pt.id_petugas = d.id_petugas
            WHERE d.id_pendaftaran = ? AND d.catatan IS NOT NULL AND d.catatan <> ''
            ORDER BY d.id_dokumen DESC
            LIMIT 1
        ");
        $st->execute([$pendaftaran['id_pendaftaran']]);
        if ($c = $st->fetch(PDO::FETCH_ASSOC)) {
            $pendaftaran['catatan_petugas'] = $c['catatan'];
            $pendaftaran['nama_petugas']    = $c['nama_petugas'];
            $pendaftaran['jabatan_petugas'] = $c['jabatan'];
        }
    } catch (Exception $e) {
        // tabel/kolom petugas berbeda: abaikan, catatan dibiarkan kosong
        error_log('api_cek_status catatan: ' . $e->getMessage());
    }

    keluar(["status" => "success", "data" => $pendaftaran]);

} catch (Exception $e) {
    keluar(["status" => "error", "message" => $e->getMessage()]);
}