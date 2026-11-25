package com.example.rikua;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AdminPendaftaranAdapter
        extends RecyclerView.Adapter<AdminPendaftaranAdapter.PendaftaranViewHolder> {

    interface OnDetailClickListener {
        void onDetailClick(AdminPendaftaranItem item);
    }

    private final List<AdminPendaftaranItem> items = new ArrayList<>();
    private final OnDetailClickListener detailClickListener;
    private final Locale localeId = Locale.forLanguageTag("id-ID");

    AdminPendaftaranAdapter(OnDetailClickListener detailClickListener) {
        this.detailClickListener = detailClickListener;
    }

    public void submitList(List<AdminPendaftaranItem> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PendaftaranViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_pendaftaran, parent, false);
        return new PendaftaranViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PendaftaranViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class PendaftaranViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvKodeTiket;
        private final TextView tvStatus;
        private final TextView tvTanggalDaftar;
        private final TextView tvNamaSuami;
        private final TextView tvNamaIstri;
        private final TextView tvJadwalAkad;
        private final TextView tvLokasiAkad;
        private final MaterialButton btnDetail;

        PendaftaranViewHolder(@NonNull View itemView) {
            super(itemView);
            tvKodeTiket = itemView.findViewById(R.id.tv_kode_tiket);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvTanggalDaftar = itemView.findViewById(R.id.tv_tanggal_daftar);
            tvNamaSuami = itemView.findViewById(R.id.tv_nama_suami);
            tvNamaIstri = itemView.findViewById(R.id.tv_nama_istri);
            tvJadwalAkad = itemView.findViewById(R.id.tv_jadwal_akad);
            tvLokasiAkad = itemView.findViewById(R.id.tv_lokasi_akad);
            btnDetail = itemView.findViewById(R.id.btn_detail);
        }

        void bind(AdminPendaftaranItem item) {
            tvKodeTiket.setText(item.getKodeTiket());
            tvStatus.setText(item.getStatusLabel());
            tvTanggalDaftar.setText(tvTanggalDaftar.getContext().getString(
                    R.string.admin_pendaftaran_diajukan,
                    tampilkan(item.formatTanggalDaftar(localeId))));
            tvNamaSuami.setText(tampilkan(item.getNamaSuami()));
            tvNamaIstri.setText(tampilkan(item.getNamaIstri()));
            tvJadwalAkad.setText(tvJadwalAkad.getContext().getString(
                    R.string.admin_pendaftaran_jadwal,
                    tampilkan(item.formatTanggalAkad(localeId)),
                    tampilkan(item.formatWaktuAkad(localeId))));
            tvLokasiAkad.setText(tampilkan(item.getLokasiAkad()));
            terapkanWarnaStatus(item.getStatus());
            btnDetail.setOnClickListener(v -> detailClickListener.onDetailClick(item));
        }

        private String tampilkan(String value) {
            return value == null || value.trim().isEmpty()
                    ? itemView.getContext().getString(
                            R.string.admin_pendaftaran_belum_tersedia)
                    : value.trim();
        }

        private void terapkanWarnaStatus(String status) {
            int background;
            int foreground;
            if ("dikirim".equalsIgnoreCase(status)) {
                background = R.color.badge_teal_bg;
                foreground = R.color.badge_teal_text;
            } else if ("draf".equalsIgnoreCase(status)) {
                background = R.color.badge_amber_bg;
                foreground = R.color.badge_amber_text;
            } else {
                background = R.color.status_offline_bg;
                foreground = R.color.status_offline_text;
            }
            tvStatus.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(tvStatus.getContext(), background)));
            tvStatus.setTextColor(ContextCompat.getColor(tvStatus.getContext(), foreground));
        }
    }
}
