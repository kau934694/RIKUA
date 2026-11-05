package com.example.rikua;

import java.io.Serializable;

/**
 * Data umum sebuah layanan (tabel {@code layanan}) untuk header halaman
 * {@link SyaratNikahActivity}: judul, deskripsi, biaya, dan tanggal pembaruan.
 */
public class LayananInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int id;
    private final String kode;
    private final String judul;
    private final String deskripsi;
    private final String biayaTeks;
    private final boolean gratis;
    private final String biayaCatatan;
    private final String estimasiDurasi;
    private final String gambarUrl;
    private final String terakhirDiperbarui;

    public LayananInfo(int id,
                       String kode,
                       String judul,
                       String deskripsi,
                       String biayaTeks,
                       boolean gratis,
                       String biayaCatatan,
                       String estimasiDurasi,
                       String gambarUrl,
                       String terakhirDiperbarui) {
        this.id = id;
        this.kode = kode == null ? "" : kode;
        this.judul = judul == null ? "Layanan" : judul;
        this.deskripsi = deskripsi == null ? "" : deskripsi;
        this.biayaTeks = biayaTeks == null ? "Rp 0" : biayaTeks;
        this.gratis = gratis;
        this.biayaCatatan = biayaCatatan == null ? "" : biayaCatatan;
        this.estimasiDurasi = estimasiDurasi == null ? "" : estimasiDurasi;
        this.gambarUrl = gambarUrl == null ? "" : gambarUrl;
        this.terakhirDiperbarui = terakhirDiperbarui == null ? "" : terakhirDiperbarui;
    }

    public int getId() { return id; }
    public String getKode() { return kode; }
    public String getJudul() { return judul; }
    public String getDeskripsi() { return deskripsi; }
    public String getBiayaTeks() { return biayaTeks; }
    public boolean isGratis() { return gratis; }
    public String getBiayaCatatan() { return biayaCatatan; }
    public String getEstimasiDurasi() { return estimasiDurasi; }
    public String getGambarUrl() { return gambarUrl; }
    public String getTerakhirDiperbarui() { return terakhirDiperbarui; }
}
