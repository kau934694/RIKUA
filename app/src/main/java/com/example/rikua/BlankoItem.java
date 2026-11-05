package com.example.rikua;

import java.io.Serializable;

/**
 * Model representatif untuk satu blanko/formulir resmi KUA (N1–N6).
 * Digunakan oleh {@link BlankoAdapter} untuk merender daftar unduhan blanko.
 */
public class BlankoItem implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String KATEGORI_WAJIB = "Wajib";
    public static final String KATEGORI_KONDISIONAL = "Kondisional";

    private final String id;
    private final String kode;          // "N1", "N2", dst.
    private final String judul;         // Nama surat / dokumen
    private final String deskripsi;     // Penjelasan singkat
    private final String fileSize;      // "120 KB"
    private final String kategori;      // "Wajib" / "Kondisional"
    private final String fileUrl;       // URL lengkap PDF dari server

    public BlankoItem(String id, String kode, String judul, String deskripsi,
                      String fileSize, String kategori, String fileUrl) {
        this.id = id;
        this.kode = kode;
        this.judul = judul;
        this.deskripsi = deskripsi;
        this.fileSize = fileSize;
        this.kategori = kategori;
        this.fileUrl = fileUrl;
    }

    public String getId() { return id; }
    public String getKode() { return kode; }
    public String getJudul() { return judul; }
    public String getDeskripsi() { return deskripsi; }
    public String getFileSize() { return fileSize; }
    public String getKategori() { return kategori; }
    public String getFileUrl() { return fileUrl; }

    public boolean isWajib() {
        return KATEGORI_WAJIB.equalsIgnoreCase(kategori);
    }
}
