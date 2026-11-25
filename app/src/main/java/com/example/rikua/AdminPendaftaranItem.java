package com.example.rikua;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class AdminPendaftaranItem {
    private final long idPendaftaran;
    private final String kodeTiket;
    private final String status;
    private final String tanggalDaftar;
    private final String tanggalAkad;
    private final String lokasiAkad;
    private final String namaSuami;
    private final String namaIstri;

    public AdminPendaftaranItem(
            long idPendaftaran,
            String kodeTiket,
            String status,
            String tanggalDaftar,
            String tanggalAkad,
            String lokasiAkad,
            String namaSuami,
            String namaIstri
    ) {
        this.idPendaftaran = idPendaftaran;
        this.kodeTiket = kodeTiket;
        this.status = status;
        this.tanggalDaftar = tanggalDaftar;
        this.tanggalAkad = tanggalAkad;
        this.lokasiAkad = lokasiAkad;
        this.namaSuami = namaSuami;
        this.namaIstri = namaIstri;
    }

    public static AdminPendaftaranItem fromJson(JSONObject json) throws JSONException {
        long id = json.optLong("id_pendaftaran", -1);
        String ticket = value(json, "kode_tiket");
        String status = value(json, "status");
        if (id <= 0 || ticket.isEmpty() || status.isEmpty()) {
            throw new JSONException("Data pendaftaran tidak lengkap");
        }

        return new AdminPendaftaranItem(
                id,
                ticket,
                status,
                value(json, "tanggal_daftar"),
                value(json, "tanggal_akad"),
                value(json, "lokasi_akad"),
                value(json, "nama_suami"),
                value(json, "nama_istri")
        );
    }

    private static String value(JSONObject json, String key) {
        Object value = json.opt(key);
        return value == null || JSONObject.NULL.equals(value)
                ? "" : value.toString().trim();
    }

    public long getIdPendaftaran() {
        return idPendaftaran;
    }

    public String getKodeTiket() {
        return kodeTiket;
    }

    public String getStatus() {
        return status;
    }

    public String getTanggalDaftar() {
        return tanggalDaftar;
    }

    public String getTanggalAkad() {
        return tanggalAkad;
    }

    public String getLokasiAkad() {
        return lokasiAkad;
    }

    public String getNamaSuami() {
        return namaSuami;
    }

    public String getNamaIstri() {
        return namaIstri;
    }

    public String getStatusLabel() {
        String label = status.replace('_', ' ');
        if (label.isEmpty()) {
            return label;
        }
        return label.substring(0, 1).toUpperCase(Locale.forLanguageTag("id-ID"))
                + label.substring(1);
    }

    public boolean matchesFilters(
            String query,
            String statusFilter,
            String dateFrom,
            String dateTo
    ) {
        String normalizedQuery = query == null
                ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (!normalizedQuery.isEmpty()
                && !kodeTiket.toLowerCase(Locale.ROOT).contains(normalizedQuery)
                && !namaSuami.toLowerCase(Locale.ROOT).contains(normalizedQuery)
                && !namaIstri.toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
            return false;
        }

        if (statusFilter != null && !status.equalsIgnoreCase(statusFilter)) {
            return false;
        }

        if (dateFrom == null && dateTo == null) {
            return true;
        }

        String submissionDate = tanggalDaftar.length() >= 10
                ? tanggalDaftar.substring(0, 10) : "";
        if (submissionDate.isEmpty()) {
            return false;
        }
        return (dateFrom == null || submissionDate.compareTo(dateFrom) >= 0)
                && (dateTo == null || submissionDate.compareTo(dateTo) <= 0);
    }

    public String formatTanggalDaftar(Locale locale) {
        return formatTanggal(tanggalDaftar, "d MMM yyyy", locale);
    }

    public String formatTanggalAkad(Locale locale) {
        return formatTanggal(tanggalAkad, "d MMM yyyy", locale);
    }

    public String formatWaktuAkad(Locale locale) {
        if (tanggalAkad.length() < 19) {
            return "";
        }
        return formatTanggal(tanggalAkad, "HH:mm", locale);
    }

    private String formatTanggal(String value, String outputPattern, Locale locale) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        String pattern = value.length() >= 19
                ? "yyyy-MM-dd HH:mm:ss" : "yyyy-MM-dd";
        int length = pattern.length() == 10 ? 10 : 19;
        if (value.length() < length) {
            return "";
        }

        try {
            SimpleDateFormat parser = new SimpleDateFormat(pattern, Locale.US);
            parser.setLenient(false);
            Date parsed = parser.parse(value.substring(0, length));
            if (parsed == null) {
                return "";
            }
            return new SimpleDateFormat(outputPattern, locale).format(parsed);
        } catch (ParseException e) {
            return "";
        }
    }
}
