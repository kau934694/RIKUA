package com.example.rikua;

public class AdminSaranaItem {
    private final int id;
    private final String jenis;
    private final String namaSarana;
    private final String alamat;
    private final int idDesa;
    private final String namaDesa;
    private final String namaPengurus;
    private final String kontak;
    private final int jumlahAnggota;
    private final String foto;
    private final Double latitude;
    private final Double longitude;

    public AdminSaranaItem(int id, String jenis, String namaSarana, String alamat,
                           int idDesa, String namaDesa, String namaPengurus,
                           String kontak, int jumlahAnggota, String foto,
                           Double latitude, Double longitude) {
        this.id = id;
        this.jenis = jenis;
        this.namaSarana = namaSarana;
        this.alamat = alamat;
        this.idDesa = idDesa;
        this.namaDesa = namaDesa;
        this.namaPengurus = namaPengurus;
        this.kontak = kontak;
        this.jumlahAnggota = jumlahAnggota;
        this.foto = foto;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public int getId() { return id; }
    public String getJenis() { return jenis; }
    public String getNamaSarana() { return namaSarana; }
    public String getAlamat() { return alamat; }
    public int getIdDesa() { return idDesa; }
    public String getNamaDesa() { return namaDesa; }
    public String getNamaPengurus() { return namaPengurus; }
    public String getKontak() { return kontak; }
    public int getJumlahAnggota() { return jumlahAnggota; }
    public String getFoto() { return foto; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
}
