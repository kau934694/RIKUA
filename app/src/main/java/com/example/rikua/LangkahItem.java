package com.example.rikua;

import java.io.Serializable;

/**
 * Model satu tahapan alur pendaftaran (node timeline) pada {@link SyaratNikahActivity}.
 * Dipetakan dari kolom tabel {@code langkah_layanan}.
 */
public class LangkahItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int nomor;
    private final String judul;
    private final String deskripsi;
    private final String lokasi;
    private final String estimasi;

    public LangkahItem(int nomor, String judul, String deskripsi, String lokasi, String estimasi) {
        this.nomor = nomor;
        this.judul = judul == null ? "" : judul;
        this.deskripsi = deskripsi == null ? "" : deskripsi;
        this.lokasi = lokasi == null ? "" : lokasi;
        this.estimasi = estimasi == null ? "" : estimasi;
    }

    public int getNomor() { return nomor; }
    public String getJudul() { return judul; }
    public String getDeskripsi() { return deskripsi; }
    public String getLokasi() { return lokasi; }
    public String getEstimasi() { return estimasi; }
}
