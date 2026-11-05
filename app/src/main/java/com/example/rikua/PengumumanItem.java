package com.example.rikua;

/**
 * Model satu baris "Pengumuman Kehendak Nikah" — rencana akad yang diumumkan
 * terbuka untuk uji publik. Data bersifat lokal/statis di halaman panduan ini
 * (contoh tampilan), bukan hasil fetch server.
 */
public class PengumumanItem {

    /** Status kartu: sudah terverifikasi petugas KUA. */
    public static final int STATUS_TERVERIFIKASI = 0;
    /** Status kartu: masih dalam masa pengumuman / uji publik. */
    public static final int STATUS_PUBLIKASI = 1;

    private final String coupleNames;   // "Ahmad Fauzi & Siti Aminah"
    private final String schedule;       // "Sabtu, 24 Oktober 2026 • 08.00 WIB"
    private final String location;       // lokasi akad
    private final String village;        // desa domisili calon
    private final String fileInfo;       // nomor berkas / keterangan sanggah
    private final int status;            // STATUS_TERVERIFIKASI | STATUS_PUBLIKASI
    private final boolean thisMonth;     // untuk filter "Bulan Ini"

    public PengumumanItem(String coupleNames, String schedule, String location,
                          String village, String fileInfo, int status, boolean thisMonth) {
        this.coupleNames = coupleNames;
        this.schedule = schedule;
        this.location = location;
        this.village = village;
        this.fileInfo = fileInfo;
        this.status = status;
        this.thisMonth = thisMonth;
    }

    public String getCoupleNames() { return coupleNames; }
    public String getSchedule() { return schedule; }
    public String getLocation() { return location; }
    public String getVillage() { return village; }
    public String getFileInfo() { return fileInfo; }
    public int getStatus() { return status; }
    public boolean isThisMonth() { return thisMonth; }

    /** Kecocokan teks pencarian (nama calon atau desa), abaikan besar-kecil. */
    public boolean matchesQuery(String query) {
        if (query == null || query.trim().isEmpty()) return true;
        String q = query.trim().toLowerCase();
        return coupleNames.toLowerCase().contains(q) || village.toLowerCase().contains(q);
    }

    /** Kecocokan filter desa; {@code null} berarti semua desa. */
    public boolean matchesVillage(String villageFilter) {
        return villageFilter == null || village.equalsIgnoreCase(villageFilter);
    }
}
