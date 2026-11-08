<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
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
$conn->set_charset("utf8mb4");

// ---------------------------------------------------------------- helper
function kirim($arr, $kode = 200) {
    http_response_code($kode);
    echo json_encode($arr, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
    exit();
}

function ambil($data, $k) {
    return isset($data[$k]) ? trim((string)$data[$k]) : '';
}

/** dd/mm/yyyy -> yyyy-mm-dd, null jika tidak valid */
function tglSql($s) {
    $d = DateTime::createFromFormat('d/m/Y', $s);
    return ($d && $d->format('d/m/Y') === $s) ? $d->format('Y-m-d') : null;
}

function satuBaris($conn, $sql, $id) {
    $st = $conn->prepare($sql);
    $st->bind_param("i", $id);
    $st->execute();
    $row = $st->get_result()->fetch_assoc();
    $st->close();
    return $row ?: null;
}

/**
 * Status perkawinan disimpan netral (belum_menikah / cerai_hidup / cerai_mati).
 * Label dari layar (jejaka, perawan, duda_cerai_hidup, janda_cerai_mati, dst.) ikut diterima.
 */
function petaStatus($s) {
    $peta = [
        'belum_menikah'     => 'belum_menikah',
        'jejaka'            => 'belum_menikah',
        'perawan'           => 'belum_menikah',
        'cerai_hidup'       => 'cerai_hidup',
        'duda_cerai_hidup'  => 'cerai_hidup',
        'janda_cerai_hidup' => 'cerai_hidup',
        'cerai_mati'        => 'cerai_mati',
        'duda_cerai_mati'   => 'cerai_mati',
        'janda_cerai_mati'  => 'cerai_mati',
    ];
    return isset($peta[$s]) ? $peta[$s] : null;
}

$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'OPTIONS') {
    exit();
}

// ================================================================ GET
// Contoh: get_calon_pengantin.php?id_pendaftaran=1
if ($method === 'GET') {
    $id = isset($_GET['id_pendaftaran']) ? (int)$_GET['id_pendaftaran'] : 0;

    $pendaftaran = satuBaris($conn, "SELECT id_pendaftaran, status FROM trx_pendaftaran_nikah WHERE id_pendaftaran = ?", $id);
    if (!$pendaftaran) {
        kirim(["status" => false, "message" => "Data pendaftaran tidak ditemukan."], 404);
    }

    $st = $conn->prepare("SELECT * FROM trx_calon_pengantin WHERE id_pendaftaran = ?");
    $st->bind_param("i", $id);
    $st->execute();
    $calon = [];
    $res = $st->get_result();
    while ($r = $res->fetch_assoc()) {
        $calon[$r['peran']] = $r;
    }
    $st->close();

    kirim([
        "status"      => true,
        "pendaftaran" => $pendaftaran,
        "suami"       => isset($calon['suami']) ? $calon['suami'] : null,
        "istri"       => isset($calon['istri']) ? $calon['istri'] : null
    ]);
}

// ================================================================ POST
// Body JSON, "step" = 2 (calon suami) atau 3 (calon istri).
// Langkah 2 tanpa id_pendaftaran akan membuat pendaftaran draf baru.
if ($method === 'POST') {
    $data = json_decode(file_get_contents("php://input"), true);
    if (!is_array($data)) {
        kirim(["status" => false, "message" => "Body harus berupa JSON."], 400);
    }

    $step = (int)ambil($data, 'step');
    $id   = (int)ambil($data, 'id_pendaftaran');

    if ($step !== 2 && $step !== 3) {
        kirim(["status" => false, "message" => "step harus 2 (calon suami) atau 3 (calon istri)."], 422);
    }
    $peran = $step === 2 ? 'suami' : 'istri';

    // ---- validasi
    $err = [];
    $nama       = ambil($data, 'nama_lengkap');
    $nik        = ambil($data, 'nik');
    $tempat     = ambil($data, 'tempat_lahir');
    $tanggal    = tglSql(ambil($data, 'tanggal_lahir'));
    $agama      = ambil($data, 'agama') ?: 'Islam';
    $pekerjaan  = ambil($data, 'pekerjaan');
    $pendidikan = ambil($data, 'pendidikan_terakhir');
    $alamat     = ambil($data, 'alamat');
    $noHp       = ambil($data, 'no_hp');
    $status     = petaStatus(ambil($data, 'status_perkawinan'));

    if ($nama === '')                          $err['nama_lengkap']        = 'Nama wajib diisi';
    if (!preg_match('/^\d{16}$/', $nik))       $err['nik']                 = 'NIK harus 16 digit';
    if ($tempat === '')                        $err['tempat_lahir']        = 'Tempat lahir wajib diisi';
    if ($tanggal === null)                     $err['tanggal_lahir']       = 'Format tanggal HH/BB/TTTT';
    if ($pekerjaan === '')                     $err['pekerjaan']           = 'Pekerjaan wajib diisi';
    if ($pendidikan === '')                    $err['pendidikan_terakhir'] = 'Pendidikan wajib dipilih';
    if ($alamat === '')                        $err['alamat']              = 'Alamat wajib diisi';
    if (!preg_match('/^\d{9,13}$/', $noHp))    $err['no_hp']               = 'Telepon 9-13 digit';
    if ($status === null)                      $err['status_perkawinan']   = 'Status perkawinan tidak valid';
    if ($err) {
        kirim(["status" => false, "message" => "Data belum benar.", "errors" => $err], 422);
    }
    $noHp = '+62' . $noHp;

    // anak_pertama hanya untuk calon istri; suami = NULL
    $anak = null;
    if ($peran === 'istri') {
        $v = ambil($data, 'anak_pertama');
        $anak = ($v === '1' || $v === 'true') ? 1 : 0;
    }

    // ---- pendaftaran induk
    if ($id) {
        if (!satuBaris($conn, "SELECT id_pendaftaran FROM trx_pendaftaran_nikah WHERE id_pendaftaran = ?", $id)) {
            kirim(["status" => false, "message" => "id_pendaftaran tidak ditemukan."], 404);
        }
    } elseif ($step === 2) {
        $conn->query("INSERT INTO trx_pendaftaran_nikah (status) VALUES ('draf')");
        $id = $conn->insert_id;
    } else {
        kirim(["status" => false, "message" => "id_pendaftaran wajib diisi. Selesaikan Langkah 2 dulu."], 422);
    }

    // ---- simpan calon (upsert per id_pendaftaran + peran)
    $st = $conn->prepare("INSERT INTO trx_calon_pengantin
        (id_pendaftaran, peran, nama_lengkap, nik, tempat_lahir, tanggal_lahir, agama, pekerjaan,
         pendidikan_terakhir, alamat, no_hp, status_perkawinan, anak_pertama)
        VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
        ON DUPLICATE KEY UPDATE
          nama_lengkap=VALUES(nama_lengkap), nik=VALUES(nik), tempat_lahir=VALUES(tempat_lahir),
          tanggal_lahir=VALUES(tanggal_lahir), agama=VALUES(agama), pekerjaan=VALUES(pekerjaan),
          pendidikan_terakhir=VALUES(pendidikan_terakhir), alamat=VALUES(alamat), no_hp=VALUES(no_hp),
          status_perkawinan=VALUES(status_perkawinan), anak_pertama=VALUES(anak_pertama)");
    $st->bind_param("isssssssssssi", $id, $peran, $nama, $nik, $tempat, $tanggal, $agama,
        $pekerjaan, $pendidikan, $alamat, $noHp, $status, $anak);

    if (!$st->execute()) {
        kirim(["status" => false, "message" => "Gagal menyimpan ke database: " . $st->error], 500);
    }
    $st->close();

    kirim([
        "status"         => true,
        "message"        => "Data calon " . $peran . " berhasil disimpan.",
        "id_pendaftaran" => $id,
        "peran"          => $peran
    ]);
}

$conn->close();
?>