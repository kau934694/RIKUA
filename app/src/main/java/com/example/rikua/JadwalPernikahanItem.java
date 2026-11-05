
package com.example.rikua;

public class JadwalPernikahanItem {

    private final String nama;
    private final String jadwal;
    private final String lokasi;

    public JadwalPernikahanItem(
            String nama,
            String jadwal,
            String lokasi
    ) {
        this.nama = nama;
        this.jadwal = jadwal;
        this.lokasi = lokasi;
    }

    public String getNama() {
        return nama;
    }

    public String getJadwal() {
        return jadwal;
    }

    public String getLokasi() {
        return lokasi;
    }
}
