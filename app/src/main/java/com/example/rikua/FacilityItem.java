package com.example.rikua;

import java.io.Serializable;

/**
 * Model representatif untuk satu sarana keagamaan pada direktori {@link FacilitiesActivity}.
 */
public class FacilityItem implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String CAT_MASJID = "masjid";
    public static final String CAT_MUSHOLLA = "musholla";
    public static final String CAT_PESANTREN = "pesantren";
    public static final String CAT_TPQ = "tpq";
    public static final String CAT_MADIN = "madin";

    private final String id;
    private final String name;
    private String categoryLabel;   
    private String categoryTokens;  
    private final String location;   
    private String villageKey;      
    private String detail1; // Kapasitas jamaah
    private String detail2; // Keterangan/Status tanah      
    private final String imageUrl;        // URL lengkap gambar dari server

    // Sumber daya tampilan
    private int accentColorRes;
    private int badgeBgRes;
    private int badgeTextColorRes;
    private int thumbIconRes;
    private int thumbBgRes;
    private int thumbTintRes;

    // Atribut tambahan sesuai ERD untuk Halaman Detail
    private String alamatLengkap;
    private String namaPengurus;
    private String kontakPengurus;
    private double latitude;
    private double longitude;

    // 1. Konstruktor 1: Versi 15 Parameter (Dipakai oleh FacilitiesActivity saat parsing JSON)
    public FacilityItem(String id,
                        String name,
                        String categoryLabel,
                        String categoryTokens,
                        String location,
                        String villageKey,
                        String detail1,
                        String detail2,
                        String imageUrl,
                        int accentColorRes,
                        int badgeBgRes,
                        int badgeTextColorRes,
                        int thumbIconRes,
                        int thumbBgRes,
                        int thumbTintRes) {
        this.id = id;
        this.name = name;
        this.categoryLabel = categoryLabel;
        this.categoryTokens = categoryTokens;
        this.location = location;
        this.villageKey = villageKey;
        this.detail1 = detail1;
        this.detail2 = detail2;
        this.imageUrl = imageUrl;
        this.accentColorRes = accentColorRes;
        this.badgeBgRes = badgeBgRes;
        this.badgeTextColorRes = badgeTextColorRes;
        this.thumbIconRes = thumbIconRes;
        this.thumbBgRes = thumbBgRes;
        this.thumbTintRes = thumbTintRes;
    }

    // 2. Konstruktor 2: Versi Sederhana / 7 Parameter (Cadangan jika dipanggil di bagian kode lain)
    public FacilityItem(String id, String name, String location, String detail1, String detail2, String imageUrl, String categoryKey) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.detail1 = detail1;
        this.detail2 = detail2;
        this.imageUrl = imageUrl;
        this.categoryLabel = categoryKey;
        this.categoryTokens = categoryKey;
        this.villageKey = "";
        this.accentColorRes = 0;
        this.badgeBgRes = 0;
        this.badgeTextColorRes = 0;
        this.thumbIconRes = 0;
        this.thumbBgRes = 0;
        this.thumbTintRes = 0;
    }

    // Standard Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategoryLabel() { return categoryLabel; }
    public String getCategoryTokens() { return categoryTokens; }
    public String getLocation() { return location; }
    public String getVillageKey() { return villageKey; }
    public String getDetail1() { return detail1; }
    public String getDetail2() { return detail2; }
    public String getImageUrl() { return imageUrl; }

    public int getAccentColorRes() { return accentColorRes; }
    public int getBadgeBgRes() { return badgeBgRes; }
    public int getBadgeTextColorRes() { return badgeTextColorRes; }
    public int getThumbIconRes() { return thumbIconRes; }
    public int getThumbBgRes() { return thumbBgRes; }
    public int getThumbTintRes() { return thumbTintRes; }

    // Getters & Setters untuk Atribut Detail ERD
    public String getAlamatLengkap() { return alamatLengkap; }
    public void setAlamatLengkap(String alamatLengkap) { this.alamatLengkap = alamatLengkap; }

    public String getNamaPengurus() { return namaPengurus; }
    public void setNamaPengurus(String namaPengurus) { this.namaPengurus = namaPengurus; }

    public String getKontakPengurus() { return kontakPengurus; }
    public void setKontakPengurus(String kontakPengurus) { this.kontakPengurus = kontakPengurus; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    // Helper Methods
    public boolean matchesCategory(String categoryKey) {
        if (categoryKey == null || categoryKey.isEmpty()) return true;
        return (" " + categoryTokens.toLowerCase() + " ").contains(" " + categoryKey.toLowerCase() + " ");
    }

    public boolean matchesVillage(String village) {
        if (village == null || village.isEmpty()) return true;
        return villageKey.equalsIgnoreCase(village.trim());
    }

    public boolean matchesQuery(String query) {
        if (query == null || query.isEmpty()) return true;
        String q = query.toLowerCase().trim();
        return name.toLowerCase().contains(q)
                || location.toLowerCase().contains(q)
                || categoryLabel.toLowerCase().contains(q)
                || categoryTokens.toLowerCase().contains(q)
                || (detail1 != null && detail1.toLowerCase().contains(q))
                || (detail2 != null && detail2.toLowerCase().contains(q));
    }
}