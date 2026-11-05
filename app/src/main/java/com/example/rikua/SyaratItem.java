package com.example.rikua;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Model satu poin persyaratan layanan (satu kartu accordion) pada
 * {@link SyaratNikahActivity}.
 *
 * Dipetakan dari kolom tabel {@code syarat_layanan} lewat endpoint
 * get_syarat_nikah.php. Implements Serializable mengikuti konvensi project
 * (passing data antar Activity memakai Serializable + fallback String ID).
 */
public class SyaratItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Nilai kolom {@code kategori} tabel syarat_layanan. */
    public static final String KATEGORI_UMUM = "umum";
    public static final String KATEGORI_KHUSUS = "khusus";

    private final int id;
    private final String kategori;
    private final String judul;
    private final String subTeks;
    private final List<String> dokumen;
    private final boolean wajib;
    private final String catatan;

    /** Status expand accordion — disimpan di model agar tetap konsisten saat scroll. */
    private boolean expanded;
    /** Status centang checklist warga (disimpan sementara di memory + SharedPreferences). */
    private boolean checked;

    public SyaratItem(int id,
                      String kategori,
                      String judul,
                      String subTeks,
                      List<String> dokumen,
                      boolean wajib,
                      String catatan) {
        this.id = id;
        this.kategori = kategori == null ? KATEGORI_UMUM : kategori;
        this.judul = judul == null ? "" : judul;
        this.subTeks = subTeks == null ? "" : subTeks;
        this.dokumen = dokumen == null ? new ArrayList<>() : dokumen;
        this.wajib = wajib;
        this.catatan = catatan == null ? "" : catatan;
    }

    public int getId() { return id; }
    public String getKategori() { return kategori; }
    public String getJudul() { return judul; }
    public String getSubTeks() { return subTeks; }
    public List<String> getDokumen() { return dokumen; }
    public boolean isWajib() { return wajib; }
    public String getCatatan() { return catatan; }

    public boolean isExpanded() { return expanded; }
    public void setExpanded(boolean expanded) { this.expanded = expanded; }

    public boolean isChecked() { return checked; }
    public void setChecked(boolean checked) { this.checked = checked; }
}
