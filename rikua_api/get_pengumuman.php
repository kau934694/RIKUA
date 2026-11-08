<?php

ini_set('display_errors', isset($_GET['debug']) ? 1 : 0);
error_reporting(E_ALL);
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');

$host     = 'localhost';
$dbname   = 'rikua';
$username = 'root';
$password = '';
$STATUS_TERVERIFIKASI = ['diverifikasi', 'terverifikasi', 'disetujui', 'selesai'];

$HARI  = ['Minggu', 'Senin', 'Selasa', 'Rabu', 'Kamis', 'Jumat', 'Sabtu'];
$BULAN = ['', 'Januari', 'Februari', 'Maret', 'April', 'Mei', 'Juni', 'Juli',
          'Agustus', 'September', 'Oktober', 'November', 'Desember'];

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    $semua = (($_GET['semua'] ?? '') === '1');

    $where = "p.kode_tiket IS NOT NULL AND p.kode_tiket <> '' AND p.tanggal_akad IS NOT NULL";
    if (!$semua) {
        $where .= " AND p.status <> 'draf' AND p.tanggal_akad >= CURDATE()";
    }

    $sql = "
        SELECT p.id_pendaftaran, p.kode_tiket, p.tanggal_akad, p.lokasi_akad, p.status,
               (SELECT c.nama_lengkap FROM trx_calon_pengantin c
                 WHERE c.id_pendaftaran = p.id_pendaftaran AND c.peran = 'suami' LIMIT 1) AS nama_suami,
               (SELECT c.nama_lengkap FROM trx_calon_pengantin c
                 WHERE c.id_pendaftaran = p.id_pendaftaran AND c.peran = 'istri' LIMIT 1) AS nama_istri
        FROM trx_pendaftaran_nikah p
        WHERE $where
        ORDER BY p.tanggal_akad ASC
    ";
    $rows = $pdo->query($sql)->fetchAll(PDO::FETCH_ASSOC);

    $bulanIni = date('Y-m');
    $data = [];
    foreach ($rows as $r) {
        $dt = new DateTime($r['tanggal_akad']);

        $suami = trim((string)$r['nama_suami']);
        $istri = trim((string)$r['nama_istri']);
        if ($suami === '' && $istri === '' && !$semua) {
            continue; // belum ada data calon, jangan diumumkan
        }
        $nama = ($suami !== '' ? $suami : 'Calon Suami') . ' & ' . ($istri !== '' ? $istri : 'Calon Istri');

        $jadwal = $HARI[(int)$dt->format('w')] . ', ' . (int)$dt->format('j') . ' '
                . $BULAN[(int)$dt->format('n')] . ' ' . $dt->format('Y')
                . ' • ' . $dt->format('H.i') . ' WIB';

        $data[] = [
            'id_pendaftaran' => (int)$r['id_pendaftaran'],
            'kode_tiket'     => $r['kode_tiket'],
            'nama'           => $nama,
            'jadwal'         => $jadwal,
            'lokasi'         => (string)$r['lokasi_akad'],
            'desa'           => '',
            'status_tampil'  => in_array(strtolower((string)$r['status']), $STATUS_TERVERIFIKASI, true)
                                    ? 'terverifikasi' : 'publikasi',
            'bulan_ini'      => ($dt->format('Y-m') === $bulanIni),
        ];
    }

    $json = json_encode(['status' => 'success', 'jumlah' => count($data), 'data' => $data],
        JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT | JSON_INVALID_UTF8_SUBSTITUTE);

    if ($json === false) {
        // Jangan biarkan respons kosong: kirim alasan yang bisa dibaca aplikasi
        echo json_encode(['status' => 'error', 'message' => 'JSON error: ' . json_last_error_msg()]);
    } else {
        echo $json;
    }

} catch (Exception $e) {
    echo json_encode(['status' => 'error', 'message' => $e->getMessage()], JSON_UNESCAPED_UNICODE);
}