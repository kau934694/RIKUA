<?php
header('Content-Type: application/json; charset=utf-8');
ini_set('display_errors', 0);   // supaya warning PHP tidak merusak JSON
mysqli_report(MYSQLI_REPORT_OFF);

$conn = new mysqli('localhost', 'root', '', 'rikua');
if ($conn->connect_error) {
    echo json_encode(['status' => 'error', 'message' => 'Koneksi database gagal']);
    exit;
}
$conn->set_charset('utf8mb4');

$syarat = [];
$counter = 1;

$resKat = $conn->query("SELECT * FROM kategori_syarat ORDER BY id ASC");
while ($kat = $resKat->fetch_assoc()) {
    $katId = (int)$kat['id'];
    $items = [];

    $resDet = $conn->query("SELECT * FROM detail_syarat WHERE kategori_id = $katId ORDER BY urutan ASC");
    while ($det = $resDet->fetch_assoc()) {
        $items[] = [
            'id_syarat'        => isset($det['id']) ? (int)$det['id'] : $counter,
            'kategori'         => '',   // diisi di bawah
            'judul_syarat'     => $det['judul_syarat'],
            'sub_teks'         => $det['keterangan'],
            'dokumen'          => [],
            'wajib'            => 1,
            'catatan_tambahan' => ''
        ];
        $counter++;
    }

    // nama kategori mengandung "khusus" -> section khusus, selain itu umum
    $jenis = (stripos($kat['nama_kategori'], 'khusus') !== false) ? 'khusus' : 'umum';
    foreach ($items as &$it) { $it['kategori'] = $jenis; }
    unset($it);

    $syarat[] = [
        'kategori' => $jenis,
        'label'    => $kat['nama_kategori'],
        'items'    => $items
    ];
}

$langkah = [];
$resLangkah = $conn->query("SELECT * FROM langkah_pendaftaran ORDER BY nomor_langkah ASC");
while ($l = $resLangkah->fetch_assoc()) {
    $langkah[] = [
        'nomor_langkah' => (int)$l['nomor_langkah'],
        'judul_langkah' => $l['judul'],
        'deskripsi'     => $l['deskripsi'],
        'lokasi'        => '',
        'estimasi_durasi' => ''
    ];
}

echo json_encode([
    'status' => 'success',
    'data' => [
        'layanan' => [
            'id' => 1,
            'kode' => 'syarat-nikah',
            'judul' => 'Syarat Nikah',
            'deskripsi' => 'Silakan lengkapi berkas persyaratan administrasi berikut sebelum mendaftar di KUA Randudongkal.',
            'biaya_teks' => 'Rp 0',
            'biaya_gratis' => true,
            'biaya_catatan' => 'Biaya nikah di KUA pada jam kerja: Rp 0 (Gratis)',
            'estimasi_durasi' => '',
            'terakhir_diperbarui' => '1 Okt 2026'
        ],
        'syarat' => $syarat,
        'langkah' => $langkah
    ]
], JSON_UNESCAPED_UNICODE);

$conn->close();