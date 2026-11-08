<?php
header("Content-Type: application/json; charset=UTF-8");
$conn = new mysqli("localhost", "root", "", "rikua");

if ($conn->connect_error) {
    die(json_encode(["status" => "error", "message" => "Koneksi gagal"]));
}

// Menggunakan JOIN agar nama desa dan data lengkap sesuai ERD terbaca
$query = "SELECT s.*, d.nama_desa FROM mst_sarana_keagamaan s 
          JOIN mst_desa d ON s.id_desa = d.id_desa";
$result = $conn->query($query);

$data = array();
if ($result) {
    while($row = $result->fetch_assoc()) {
        $data[] = array(
            "id" => $row['id_sarana'],
            "jenis" => $row['jenis'],
            "nama_sarana" => $row['nama_sarana'],
            "alamat" => $row['alamat'],
            "desa" => $row['nama_desa'],
            "nama_pengurus" => $row['nama_pengurus'],
            "kontak" => $row['kontak'],
            "jumlah_jamaah" => $row['jumlah_anggota'],
            "gambar" => $row['foto'],
            "latitude" => $row['latitude'],
            "longitude" => $row['longitude']
        );
    }
    echo json_encode(["status" => "success", "data" => $data]);
} else {
    echo json_encode(["status" => "error", "message" => $conn->error]);
}

$conn->close();
?>