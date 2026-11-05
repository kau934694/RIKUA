
package com.example.rikua;

/**
 * Model satu dokumen pada Langkah 5 RIKUA.
 */
public class DokumenItem {

    public static final int STATUS_TERUNGGAH = 0;
    public static final int STATUS_PERLU_PERBAIKAN = 1;
    public static final int STATUS_BELUM = 2;
    public static final int STATUS_TIDAK_DIPERLUKAN = 3;

    private final int idJenisDokumen;
    private int idDokumen;

    private final String judul;
    private final String deskripsi;

    private String pesanPetugas;
    private int status;

    private String namaFile;
    private String filePath;
    private String statusServer = "belum_diunggah";

    // Status asli sebelum dokumen dinyatakan tidak diperlukan.
    private int statusSebelumnya = STATUS_BELUM;

    private boolean diperlukan = true;

    /**
     * Constructor dengan ID jenis dokumen dari database.
     */
    public DokumenItem(
            int idJenisDokumen,
            String judul,
            String deskripsi,
            String pesanPetugas,
            int status
    ) {
        this.idJenisDokumen = idJenisDokumen;
        this.judul = judul;
        this.deskripsi = deskripsi;
        this.pesanPetugas = pesanPetugas;
        this.status = status;
        this.statusSebelumnya = status;
    }

    /**
     * Constructor lama untuk kompatibilitas.
     */
    public DokumenItem(
            String judul,
            String deskripsi,
            String pesanPetugas,
            int status
    ) {
        this(0, judul, deskripsi, pesanPetugas, status);
    }

    public int getIdJenisDokumen() {
        return idJenisDokumen;
    }

    public int getIdDokumen() {
        return idDokumen;
    }

    public String getJudul() {
        return judul;
    }

    public String getDeskripsi() {
        return deskripsi;
    }

    public String getPesanPetugas() {
        return pesanPetugas;
    }

    public int getStatus() {
        return status;
    }

    public String getNamaFile() {
        return namaFile;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getStatusServer() {
        return statusServer;
    }

    public boolean isDiperlukan() {
        return diperlukan;
    }

    /**
     * Memetakan status dokumen dari database.
     */
    public void setStatusDariServer(
            String statusServer,
            int idDokumen,
            String filePath,
            String namaFile,
            String catatan
    ) {
        this.statusServer = statusServer == null
                ? "belum_diunggah"
                : statusServer.trim();

        this.idDokumen = idDokumen;
        this.filePath = filePath;
        this.namaFile = namaFile;
        this.pesanPetugas = catatan;

        int statusDatabase;

        switch (this.statusServer) {
            case "terunggah":
            case "diverifikasi":
                statusDatabase = STATUS_TERUNGGAH;
                break;

            case "perlu_perbaikan":
                statusDatabase = STATUS_PERLU_PERBAIKAN;
                break;

            case "belum_diunggah":
            default:
                statusDatabase = STATUS_BELUM;
                break;
        }

        statusSebelumnya = statusDatabase;

        if (diperlukan) {
            status = statusDatabase;
        } else {
            status = STATUS_TIDAK_DIPERLUKAN;
        }
    }

    /**
     * Mengaktifkan atau menonaktifkan kebutuhan dokumen
     * berdasarkan kondisi calon pengantin.
     */
    public void setDiperlukan(boolean diperlukan) {

        if (this.diperlukan == diperlukan) {
            return;
        }

        if (!diperlukan) {
            statusSebelumnya = status;
            status = STATUS_TIDAK_DIPERLUKAN;

        } else {
            status = statusSebelumnya;
        }

        this.diperlukan = diperlukan;
    }

    /**
     * Dipanggil setelah server mengonfirmasi upload berhasil.
     */
    public void tandaiTerunggah(String namaFile) {
        this.status = STATUS_TERUNGGAH;
        this.statusSebelumnya = STATUS_TERUNGGAH;
        this.statusServer = "terunggah";
        this.namaFile = namaFile;
    }

    /**
     * Tandai terunggah dengan metadata server.
     */
    public void tandaiTerunggah(
            String namaFile,
            String filePath,
            int idDokumen
    ) {
        tandaiTerunggah(namaFile);

        this.filePath = filePath;
        this.idDokumen = idDokumen;
    }
}
