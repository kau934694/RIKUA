<?php
/**
 * get_kuota.php  (taruh di C:\laragon\www\rikua_api\)
 * Ringkasan kuota akad untuk satu bulan: jumlah akad terjadwal per tanggal dan sisa kuota.
 *
 * Contoh:
 *   get_kuota.php                          -> bulan berjalan, hanya yang BUKAN 'draf'
 *   get_kuota.php?bulan=2026-10            -> bulan tertentu
 *   get_kuota.php?bulan=2026-10&semua=1    -> termasuk 'draf' (untuk uji coba)
 */

ini_set('display_errors', isset($_GET['debug']) ? 1 : 0);
error_reporting(E_ALL);
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');

// ===================== SESUAIKAN =====================
// Batas akad nikah per hari di KUA. Angka ini HANYA contoh; isi sesuai aturan KUA.
$KUOTA_PER_HARI = 5;
// =====================================================

$host     = 'localhost';
$dbname   = 'rikua';
$username = 'root';
$password = '';

$HARI  = ['Minggu', 'Senin', 'Selasa', 'Rabu', 'Kamis', 'Jumat', 'Sabtu'];
$BULAN = ['', 'Januari', 'Februari', 'Maret', 'April', 'Mei', 'Juni', 'Juli',
          'Agustus', 'September', 'Oktober', 'November', 'Desember'];
$BULAN_PENDEK = ['', 'Jan', 'Feb', 'Mar', 'Apr', 'Mei', 'Jun', 'Jul',
                 'Agu', 'Sep', 'Okt', 'Nov', 'Des'];

function keluar($arr) {
    $json = json_encode($arr, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT | JSON_INVALID_UTF8_SUBSTITUTE);
    echo ($json === false)
        ? json_encode(['status' => 'error', 'message' => 'JSON error: ' . json_last_error_msg()])
        : $json;
    exit;
}

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

    // Bulan yang diminta (format YYYY-MM); default: bulan berjalan menurut server
    $bulan = $_GET['bulan'] ?? date('Y-m');
    if (!preg_match('/^\d{4}-(0[1-9]|1[0-2])$/', $bulan)) {
        $bulan = date('Y-m');
    }
    $awal  = new DateTime($bulan . '-01 00:00:00');
    $akhir = (clone $awal)->modify('first day of next month');

    $semua = (($_GET['semua'] ?? '') === '1');

    $where = "p.tanggal_akad >= ? AND p.tanggal_akad < ?
              AND p.kode_tiket IS NOT NULL AND p.kode_tiket <> ''";
    if (!$semua) {
        $where .= " AND p.status <> 'draf'";
    }

    $stmt = $pdo->prepare("
        SELECT DATE(p.tanggal_akad) AS tgl, COUNT(*) AS jml
        FROM trx_pendaftaran_nikah p
        WHERE $where
        GROUP BY DATE(p.tanggal_akad)
        ORDER BY tgl ASC
    ");
    $stmt->execute([$awal->format('Y-m-d H:i:s'), $akhir->format('Y-m-d H:i:s')]);

    $hari  = [];
    $total = 0;
    foreach ($stmt->fetchAll(PDO::FETCH_ASSOC) as $r) {
        $d      = new DateTime($r['tgl']);
        $terisi = (int)$r['jml'];
        $sisa   = max(0, $KUOTA_PER_HARI - $terisi);
        $total += $terisi;

        $hari[] = [
            'tanggal' => $d->format('Y-m-d'),
            'label'   => $HARI[(int)$d->format('w')] . ', ' . (int)$d->format('j') . ' '
                         . $BULAN_PENDEK[(int)$d->format('n')],
            'terisi'  => $terisi,
            'sisa'    => $sisa,
            'penuh'   => ($sisa === 0),
        ];
    }

    keluar([
        'status'        => 'success',
        'bulan'         => $bulan,
        'nama_bulan'    => $BULAN[(int)$awal->format('n')] . ' ' . $awal->format('Y'),
        'kuota_per_hari' => $KUOTA_PER_HARI,
        'total_akad'    => $total,
        'hari'          => $hari,
    ]);

} catch (Exception $e) {
    keluar(['status' => 'error', 'message' => $e->getMessage()]);
}