<?php
header("Content-Type: application/json; charset=UTF-8");
$conn = new mysqli("localhost", "root", "", "rikua");

if ($conn->connect_error) {
    die(json_encode(["status" => "error", "message" => "Koneksi database gagal"]));
}

// Mendeteksi protokol dan host/IP server secara otomatis
$protocol = isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http";
$host = $_SERVER['HTTP_HOST']; // Otomatis mendeteksi IP atau localhost beserta port jika ada
$baseUrl = $protocol . "://" . $host . "/rikua_api/uploads/blanko/";

$result = $conn->query("SELECT * FROM blanko ORDER BY kode_formulir ASC");
$data = array();

if ($result) {
    while($row = $result->fetch_assoc()) {
        $data[] = array(
            "id" => $row['id_blanko'],
            "kode" => $row['kode_formulir'],
            "nama" => $row['nama_formulir'],
            "deskripsi" => $row['deskripsi'],
            "ukuran" => $row['ukuran_file'],
            "syarat" => $row['kategori_syarat'],
            // Menggunakan base URL dinamis agar tidak perlu hardcode IP lagi
            "file_url" => $baseUrl . trim($row['file_pdf'])
        );
    }
    echo json_encode(["status" => "success", "data" => $data]);
} else {
    echo json_encode(["status" => "error", "message" => $conn->error]);
}

$conn->close();
?>