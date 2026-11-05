package com.example.rikua;

/**
 * Model representatif untuk satu petugas pelayanan KUA pada layar Konsultasi.
 * Digunakan oleh {@link OfficerAdapter} untuk merender kartu petugas beserta
 * status ketersediaan, bidang keahlian, dan kanal kontak (WhatsApp / telepon).
 */
public class OfficerItem {

    private final String id;
    private final String name;
    private final String role;
    private final String serviceHours;
    private final String[] expertise;
    /** Nomor telepon dalam format internasional tanpa prefix "+" (mis. 62812...). */
    private final String phoneDigits;
    private final boolean available;
    private final int avatarBgRes;
    /** Inisial yang ditampilkan pada avatar lingkaran (mis. "HS"). */
    private final String initials;

    public OfficerItem(String id,
                       String name,
                       String role,
                       String serviceHours,
                       String[] expertise,
                       String phoneDigits,
                       boolean available,
                       int avatarBgRes,
                       String initials) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.serviceHours = serviceHours;
        this.expertise = expertise;
        this.phoneDigits = phoneDigits;
        this.available = available;
        this.avatarBgRes = avatarBgRes;
        this.initials = initials;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public String getServiceHours() { return serviceHours; }
    public String[] getExpertise() { return expertise; }
    public String getPhoneDigits() { return phoneDigits; }
    public boolean isAvailable() { return available; }
    public int getAvatarBgRes() { return avatarBgRes; }
    public String getInitials() { return initials; }
}
