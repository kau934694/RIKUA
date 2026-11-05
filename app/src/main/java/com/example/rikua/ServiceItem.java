package com.example.rikua;

import java.io.Serializable;

/**
 * Model representatif untuk satu item layanan KUA.
 * Digunakan oleh {@link ServiceAdapter} untuk merender daftar layanan
 * serta difilter secara real-time berdasarkan {@link #category} dan pencarian teks.
 *
 * Implements Serializable agar objeknya bisa dibawa utuh lewat Intent ke halaman
 * detail persyaratan ({@link SyaratNikahActivity}), sesuai konvensi project.
 */
public class ServiceItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Enum kategori layanan untuk keperluan filter chip. */
    public static final String CATEGORY_PERNIKAHAN = "pernikahan";
    public static final String CATEGORY_ADMINISTRASI = "administrasi";
    public static final String CATEGORY_WAKAF = "wakaf";

    private final String id;
    private final String title;
    private final String description;
    private final String badge;
    private final String category;
    /**
     * Slug {@code kode_layanan} (mis. "syarat-nikah") — dipakai sebagai parameter
     * endpoint detail layanan. Kosong bila server belum mengirim kolom slug.
     */
    private final String slug;
    private final int iconRes;
    private final int iconBgRes;
    private final int badgeBgRes;
    private final int badgeTextColorRes;
    private final int iconTintRes;

    public ServiceItem(String id,
                       String title,
                       String description,
                       String badge,
                       String category,
                       String slug,
                       int iconRes,
                       int iconBgRes,
                       int badgeBgRes,
                       int badgeTextColorRes,
                       int iconTintRes) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.badge = badge;
        this.category = category;
        this.slug = slug == null ? "" : slug;
        this.iconRes = iconRes;
        this.iconBgRes = iconBgRes;
        this.badgeBgRes = badgeBgRes;
        this.badgeTextColorRes = badgeTextColorRes;
        this.iconTintRes = iconTintRes;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getBadge() { return badge; }
    public String getCategory() { return category; }
    public String getSlug() { return slug; }
    public int getIconRes() { return iconRes; }
    public int getIconBgRes() { return iconBgRes; }
    public int getBadgeBgRes() { return badgeBgRes; }
    public int getBadgeTextColorRes() { return badgeTextColorRes; }
    public int getIconTintRes() { return iconTintRes; }

    /**
     * Pencocokan sederhana untuk query pencarian (case-insensitive)
     * pada gabungan judul, deskripsi, badge, dan kategori.
     */
    public boolean matches(String query) {
        if (query == null || query.isEmpty()) return true;
        String q = query.toLowerCase().trim();
        return title.toLowerCase().contains(q)
                || description.toLowerCase().contains(q)
                || (badge != null && badge.toLowerCase().contains(q))
                || category.toLowerCase().contains(q);
    }
}
