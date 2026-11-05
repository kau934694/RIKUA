package com.example.rikua;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Kelompok syarat satu kategori ("Untuk Semua Pendaftar" / "Kondisi Tertentu")
 * beserta header section accordion pada {@link SyaratNikahActivity}.
 *
 * Dipetakan dari elemen array {@code data.syarat[]} respons get_syarat_nikah.php.
 */
public class SyaratSection implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String kategori;
    private final String label;
    private final List<SyaratItem> items;

    public SyaratSection(String kategori, String label, List<SyaratItem> items) {
        this.kategori = kategori == null ? SyaratItem.KATEGORI_UMUM : kategori;
        this.label = label == null ? "Syarat" : label;
        this.items = items == null ? new ArrayList<>() : items;
    }

    public String getKategori() { return kategori; }
    public String getLabel() { return label; }
    public List<SyaratItem> getItems() { return items; }

    public int getItemCount() { return items.size(); }

    /** Nomor badge yang ditampilkan di header section (jumlah poin syarat). */
    public String getJumlahLabel() {
        return String.valueOf(items.size());
    }
}
