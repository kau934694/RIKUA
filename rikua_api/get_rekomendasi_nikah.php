<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET, POST");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

$host     = "localhost";
$username = "root";
$password = "";
$database = "rikua";

$conn = new mysqli($host, $username, $password, $database);

if ($conn->connect_error) {
    echo json_encode([
        "status" => false,
        "message" => "Koneksi database gagal: " . $conn->connect_error
    ]);
    exit();
}

$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'GET') {
    // Mengembalikan data informasi panduan & syarat khusus Rekomendasi Nikah
    $response = [
        "status" => true,
        "title" => "Rekomendasi Nikah - KUA Warungpring",
        "subtitle" => "Untuk akad nikah di luar kecamatan tempat tinggal",
        "description" => "Panduan praktis penerbitan surat pengantar rekomendasi bagi warga ber-KTP Warungpring.",
        "last_updated" => "1 Oktober 2026",
        "requirements" => [
            "Surat pengantar nikah dari kantor desa/kelurahan setempat (Model N1)",
            "Fotokopi KTP dan Kartu Keluarga (KK) calon pengantin",
            "Pas foto 2x3 dan 3x4 latar belakang biru (2 lembar)",
            "Fotokopi KTP orang tua / wali nikah"
        ],
        "steps" => [
            "Pengajuan berkas di KUA asal (KUA Warungpring) secara langsung atau online.",
            "Verifikasi dokumen dan penerbitan surat rekomendasi nikah resmi oleh petugas.",
            "Pendaftaran di KUA tempat pelaksanaan akad nikah tujuan."
        ],
        "cost" => "Gratis Rp 0,-"
    ];

    echo json_encode($response, JSON_PRETTY_PRINT);

} elseif ($method === 'POST') {
    // Menerima input pengajuan form rekomendasi nikah dari Android
    $data = json_decode(file_get_contents("php://input"), true);

    if (!empty($data['nik_pemohon']) && !empty($data['nama_lengkap']) && !empty($data['no_hp_kontak']) && !empty($data['kecamatan_tujuan'])) {
        $kode_tiket       = "RIKUA-REK-" . strtoupper(substr(uniqid(), -6));
        $nik              = $conn->real_escape_string($data['nik_pemohon']);
        $nama             = $conn->real_escape_string($data['nama_lengkap']);
        $no_hp            = $conn->real_escape_string($data['no_hp_kontak']);
        $kecamatan_tujuan = $conn->real_escape_string($data['kecamatan_tujuan']);
        $desa_asal        = isset($data['desa_asal']) ? $conn->real_escape_string($data['desa_asal']) : 'Warungpring';

        $sql = "INSERT INTO rekomendasi_nikah 
                (kode_tiket, nik_pemohon, nama_lengkap, no_hp_kontak, desa_asal, kecamatan_tujuan, status) 
                VALUES ('$kode_tiket', '$nik', '$nama', '$no_hp', '$desa_asal', '$kecamatan_tujuan', 'Pending')";

        if ($conn->query($sql) === TRUE) {
            echo json_encode([
                "status" => true,
                "message" => "Pengajuan rekomendasi nikah berhasil disimpan.",
                "kode_tiket" => $kode_tiket
            ]);
        } else {
            echo json_encode([
                "status" => false,
                "message" => "Gagal menyimpan ke database: " . $conn->error
            ]);
        }
    } else {
        echo json_encode([
            "status" => false,
            "message" => "Data tidak lengkap. Mohon isi NIK, Nama Lengkap, No HP, dan Kecamatan Tujuan."
        ]);
    }
}

$conn->close();
?>