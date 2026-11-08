<?php
ini_set('display_errors', 1);
error_reporting(E_ALL);

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');

$host = 'localhost';
$dbname = 'rikua'; // Pastikan nama database sesuai di phpMyAdmin
$username = 'root';
$password = '';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    $kode_tiket = $_GET['kode_tiket'] ?? '';
    $tanggal_lahir = $_GET['tanggal_lahir'] ?? '';

    if (empty($kode_tiket) || empty($tanggal_lahir)) {
        echo json_encode([
            "status" => "error",
            "message" => "Parameter kode_tiket dan tanggal_lahir wajib diisi di URL (contoh: ?kode_tiket=RKA-2026-00123&tanggal_lahir=1998-04-12)"
        ], JSON_PRETTY_PRINT);
        exit;
    }

    $stmt = $pdo->prepare("
        SELECT 
            p.id_pendaftaran,
            p.kode_tiket,
            p.tanggal_daftar,
            p.tanggal_akad,
            p.lokasi_akad,
            p.status AS status_pendaftaran,
            p.catatan_petugas,
            pt.nama_petugas,
            pt.jabatan AS jabatan_petugas
        FROM trx_pendaftaran_nikah p
        JOIN trx_calon_pengantin c ON p.id_pendaftaran = c.id_pendaftaran
        LEFT JOIN petugas pt ON p.id = pt.id_petugas
        WHERE p.kode_tiket = ? AND c.tanggal_lahir = ?
        LIMIT 1
    ");
    $stmt->execute([trim($kode_tiket), trim($tanggal_lahir)]);
    $pendaftaran = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$pendaftaran) {
        echo json_encode([
            "status" => "not_found",
            "message" => "Data pendaftaran tidak ditemukan untuk tiket dan tanggal lahir tersebut."
        ], JSON_PRETTY_PRINT);
        exit;
    }

    echo json_encode([
        "status" => "success",
        "data" => $pendaftaran
    ], JSON_PRETTY_PRINT);

} catch (Exception $e) {
    echo json_encode([
        "status" => "error",
        "message" => $e->getMessage()
    ], JSON_PRETTY_PRINT);
}
?>
