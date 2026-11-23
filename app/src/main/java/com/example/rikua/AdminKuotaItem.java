package com.example.rikua;

/**
 * Satu sel kuota akad harian pada strip 7 hari dashboard admin.
 * Label hari/tanggal sudah diformat Activity agar adapter tetap murni tampil.
 */
public class AdminKuotaItem {

    private final String labelHari;
    private final String labelTanggal;
    private final String labelTerisi;
    private final boolean penuh;
    private final boolean hariIni;

    public AdminKuotaItem(String labelHari, String labelTanggal, String labelTerisi,
                          boolean penuh, boolean hariIni) {
        this.labelHari = labelHari;
        this.labelTanggal = labelTanggal;
        this.labelTerisi = labelTerisi;
        this.penuh = penuh;
        this.hariIni = hariIni;
    }

    public String getLabelHari() {
        return labelHari;
    }

    public String getLabelTanggal() {
        return labelTanggal;
    }

    public String getLabelTerisi() {
        return labelTerisi;
    }

    public boolean isPenuh() {
        return penuh;
    }

    public boolean isHariIni() {
        return hariIni;
    }
}
