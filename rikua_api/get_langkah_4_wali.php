<?php
ini_set('log_errors', '1');
ini_set('error_log', __DIR__ . '/error_langkah4.log');
error_reporting(E_ALL);

set_exception_handler(function ($e) {
    error_log(
        'EXCEPTION: ' . $e->getMessage()
        . ' di ' . $e->getFile()
        . ':' . $e->getLine()
    );

    http_response_code(500);
    header('Content-Type: application/json; charset=utf-8');

    echo json_encode([
        'status' => 'error',
        'message' => 'Terjadi kesalahan internal server'
    ]);
    exit;
});
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

// Koneksi Database
$host = 'localhost';
$dbname = 'rikua';
$username = 'root';
$password = '';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch (PDOException $e) {
    echo json_encode([
        "status" => "error",
        "message" => "Koneksi database gagal: " . $e->getMessage()
    ]);
    exit;
}

// Tangkap id_pendaftaran dari POST, GET, atau Session
$id_pendaftaran = $_POST['id_pendaftaran'] ?? $_GET['id_pendaftaran'] ?? $_SESSION['id_pendaftaran'] ?? 0;

// Handle form submission (Metode POST)
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    try {
        if (empty($id_pendaftaran)) {
            throw new Exception("ID Pendaftaran tidak valid atau kosong.");
        }

        // 1. Sanitasi dan Normalisasi Format Tanggal & Waktu Akad (Mencegah SQLSTATE[22007])
      $tanggal_akad_raw = trim($_POST['tanggal_akad'] ?? '');
$waktu_akad_raw   = trim($_POST['waktu_akad'] ?? '');
if ($tanggal_akad_raw === '' || $waktu_akad_raw === '') {
    throw new Exception("Tanggal dan waktu akad wajib diisi.");
}
$dt  = DateTime::createFromFormat('d/m/Y H:i', $tanggal_akad_raw . ' ' . $waktu_akad_raw);
$err = DateTime::getLastErrors();
if (!$dt || ($err && ($err['warning_count'] > 0 || $err['error_count'] > 0))) {
    throw new Exception("Format tanggal/waktu akad tidak valid.");
}
$tanggal_akad = $dt->format('Y-m-d H:i:s');

        $lokasi_akad = $_POST['lokasi_akad'] ?? 'KUA';
        $alamat_akad = $_POST['alamat_akad'] ?? '';
        
        // Ambil atau generate kode_tiket jika dikirim dari request atau sudah ada di DB
        $kode_tiket = $_POST['kode_tiket'] ?? null;
        if (empty($kode_tiket)) {
            $stmtCek = $pdo->prepare("SELECT kode_tiket FROM trx_pendaftaran_nikah WHERE id_pendaftaran = ?");
            $stmtCek->execute([$id_pendaftaran]);
            $existing = $stmtCek->fetch(PDO::FETCH_ASSOC);
            $kode_tiket = $existing['kode_tiket'] ?? ("RKA-" . date('Y') . "-" . str_pad($id_pendaftaran, 5, "0", STR_PAD_LEFT));
        }
        
        // Gabungkan lokasi jika di luar KUA
        $lokasi_lengkap = ($lokasi_akad === 'luar_kua') 
            ? "Di Luar KUA: " . $alamat_akad 
            : "Balai Nikah Kantor KUA Warungpring";

        // Konversi nilai boolean/flag kuesioner kondisi khusus
        $akad_luar_kecamatan = isset($_POST['akad_luar_kecamatan']) && ($_POST['akad_luar_kecamatan'] === 'ya' || $_POST['akad_luar_kecamatan'] == '1') ? 1 : 0;
        $usia_di_bawah_21 = isset($_POST['usia_di_bawah_21']) && ($_POST['usia_di_bawah_21'] === 'ya' || $_POST['usia_di_bawah_21'] == '1') ? 1 : 0;
        $usia_di_bawah_19 = isset($_POST['usia_di_bawah_19']) && ($_POST['usia_di_bawah_19'] === 'ya' || $_POST['usia_di_bawah_19'] == '1') ? 1 : 0;
        $anggota_tni_polri = isset($_POST['anggota_tni_polri']) && ($_POST['anggota_tni_polri'] === 'ya' || $_POST['anggota_tni_polri'] == '1') ? 1 : 0;
        $poligami = isset($_POST['poligami']) && ($_POST['poligami'] === 'ya' || $_POST['poligami'] == '1') ? 1 : 0;
        $daftar_kurang_10_hari = isset($_POST['daftar_kurang_10_hari']) && ($_POST['daftar_kurang_10_hari'] === 'ya' || $_POST['daftar_kurang_10_hari'] == '1') ? 1 : 0;
   $cek = $pdo->prepare("SELECT 1 FROM trx_pendaftaran_nikah WHERE id_pendaftaran = ?");
   $cek->execute([$id_pendaftaran]);
   if (!$cek->fetchColumn()) {
       throw new Exception("Pendaftaran id $id_pendaftaran tidak ditemukan.");
   }
        // Mulai Transaksi Database untuk Menjamin Konsistensi Data
        $pdo->beginTransaction();

        // Update trx_pendaftaran_nikah
        
      
       
        // Update trx_pendaftaran_nikah
        $stmt = $pdo->prepare("
            UPDATE trx_pendaftaran_nikah
            SET
                kode_tiket = ?,
                tanggal_akad = ?,
                lokasi_akad = ?,
                akad_luar_kecamatan = ?,
                usia_di_bawah_21 = ?,
                usia_di_bawah_19 = ?,
                anggota_tni_polri = ?,
                poligami = ?,
                daftar_kurang_10_hari = ?
            WHERE id_pendaftaran = ?
        ");

        $stmt->execute([
            $kode_tiket,
            $tanggal_akad,
            $lokasi_lengkap,
            $akad_luar_kecamatan,
            $usia_di_bawah_21,
            $usia_di_bawah_19,
            $anggota_tni_polri,
            $poligami,
            $daftar_kurang_10_hari,
            $id_pendaftaran
        ]);

        // Simpan Data Wali & Saksi ke trx_pihak_terkait (Reset data lama untuk pendaftaran ini)
        $pdo->prepare("DELETE FROM trx_pihak_terkait WHERE id_pendaftaran = ?")->execute([$id_pendaftaran]);

        $stmtPihak = $pdo->prepare("INSERT INTO trx_pihak_terkait (id_pendaftaran, peran, nama_lengkap, nik, hubungan) VALUES (?, ?, ?, ?, ?)");

        if (!empty($_POST['nama_wali'])) {
            $stmtPihak->execute([$id_pendaftaran, 'wali', $_POST['nama_wali'], $_POST['nik_wali'] ?? '', $_POST['hubungan_wali'] ?? 'Wali Nikah']);
        }
        if (!empty($_POST['nama_saksi_1'])) {
            $stmtPihak->execute([$id_pendaftaran, 'saksi_1', $_POST['nama_saksi_1'], $_POST['nik_saksi_1'] ?? '', 'Saksi Pertama']);
        }
        if (!empty($_POST['nama_saksi_2'])) {
            $stmtPihak->execute([$id_pendaftaran, 'saksi_2', $_POST['nama_saksi_2'], $_POST['nik_saksi_2'] ?? '', 'Saksi Kedua']);
        }

        // Commit transaksi jika semua query sukses
        $pdo->commit();

        echo json_encode([
            "status" => "success",
            "message" => "Data langkah 4 berhasil disimpan",
            "id_pendaftaran" => $id_pendaftaran,
            "kode_tiket" => $kode_tiket
        ]);

    
    } catch (Throwable $e) {
        // Rollback transaksi jika terjadi error
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }

        // Catat detail error untuk debugging
        error_log(
            'Langkah 4: ' . $e->getMessage() .
            ' di ' . $e->getFile() .
            ':' . $e->getLine()
        );

        http_response_code(500);
        echo json_encode([
            "status" => "error",
            "message" => "Gagal menyimpan data langkah 4."
        ]);
    }
    exit;
}


// Ambil data existing (Metode GET)
try {
    if (empty($id_pendaftaran)) {
        echo json_encode([
            "status" => "error",
            "message" => "ID Pendaftaran tidak ditemukan."
        ]);
        exit;
    }

    $stmt = $pdo->prepare("SELECT * FROM trx_pendaftaran_nikah WHERE id_pendaftaran = ?");
    $stmt->execute([$id_pendaftaran]);
    $pendaftaran = $stmt->fetch(PDO::FETCH_ASSOC);

    $stmtPihak = $pdo->prepare("SELECT * FROM trx_pihak_terkait WHERE id_pendaftaran = ?");
    $stmtPihak->execute([$id_pendaftaran]);
    $pihakList = [];
    while ($row = $stmtPihak->fetch(PDO::FETCH_ASSOC)) {
        $pihakList[$row['peran']] = $row;
    }

    echo json_encode([
        "status" => "success",
        "id_pendaftaran" => $id_pendaftaran,
        "kode_tiket" => $pendaftaran['kode_tiket'] ?? "",
        "pendaftaran" => $pendaftaran ?: [],
        "pihak_terkait" => $pihakList
    ], JSON_PRETTY_PRINT);

} catch (Exception $e) {
    echo json_encode([
        "status" => "error",
        "message" => $e->getMessage()
    ]);
}
?>