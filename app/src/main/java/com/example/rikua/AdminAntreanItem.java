package com.example.rikua;

/**
 * Satu baris antrean berkas "perlu diperiksa" pada dashboard admin.
 */
public class AdminAntreanItem {

    private final String idPendaftaran;
    private final String kodeTiket;
    private final String namaCalon;
    private final String tanggalAkad;
    private final int dokumenTerunggah;
    private final int dokumenTotal;
    private final String status;

    public AdminAntreanItem(String idPendaftaran, String kodeTiket, String namaCalon,
                            String tanggalAkad, int dokumenTerunggah, int dokumenTotal,
                            String status) {
        this.idPendaftaran = idPendaftaran;
        this.kodeTiket = kodeTiket;
        this.namaCalon = namaCalon;
        this.tanggalAkad = tanggalAkad;
        this.dokumenTerunggah = dokumenTerunggah;
        this.dokumenTotal = dokumenTotal;
        this.status = status;
    }

    public String getIdPendaftaran() {
        return idPendaftaran;
    }

    public String getKodeTiket() {
        return kodeTiket;
    }

    public String getNamaCalon() {
        return namaCalon;
    }

    public String getTanggalAkad() {
        return tanggalAkad;
    }

    public int getDokumenTerunggah() {
        return dokumenTerunggah;
    }

    public int getDokumenTotal() {
        return dokumenTotal;
    }

    /** Nilai enum trx_dokumen_pendaftaran.status. */
    public String getStatus() {
        return status;
    }
}
