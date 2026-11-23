package com.example.rikua;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter antrean berkas "perlu diperiksa" pada {@link AdminDashboardActivity}.
 */
public class AdminAntreanAdapter
        extends RecyclerView.Adapter<AdminAntreanAdapter.AntreanViewHolder> {

    private final List<AdminAntreanItem> items = new ArrayList<>();

    public void submitList(List<AdminAntreanItem> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AntreanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_antrean, parent, false);
        return new AntreanViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AntreanViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class AntreanViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvKode;
        private final TextView tvBadge;
        private final TextView tvNama;
        private final TextView tvTanggal;
        private final TextView tvDokumen;

        AntreanViewHolder(@NonNull View itemView) {
            super(itemView);
            tvKode = itemView.findViewById(R.id.tv_kode_tiket);
            tvBadge = itemView.findViewById(R.id.tv_badge_status);
            tvNama = itemView.findViewById(R.id.tv_nama_calon);
            tvTanggal = itemView.findViewById(R.id.tv_tanggal_akad);
            tvDokumen = itemView.findViewById(R.id.tv_dokumen_count);
        }

        void bind(AdminAntreanItem item) {
            tvKode.setText(item.getKodeTiket());
            tvNama.setText(item.getNamaCalon());
            tvTanggal.setText(item.getTanggalAkad());
            tvDokumen.setText(tvDokumen.getContext().getString(
                    R.string.admin_dokumen_count,
                    item.getDokumenTerunggah(),
                    item.getDokumenTotal()));

            // Lencana status: teks Indonesia + drawable badge yang sudah ada,
            // status tidak pernah hanya warna.
            TextView badge = tvBadge;
            String status = item.getStatus() == null ? "" : item.getStatus();
            int drawable;
            int textColor;
            int label;
            switch (status) {
                case "terunggah":
                    drawable = R.drawable.bg_badge_blue;
                    textColor = R.color.badge_blue_text;
                    label = R.string.admin_status_terunggah;
                    break;
                case "perlu_perbaikan":
                    drawable = R.drawable.bg_badge_amber;
                    textColor = R.color.badge_amber_text;
                    label = R.string.admin_status_perlu_perbaikan;
                    break;
                case "diverifikasi":
                    drawable = R.drawable.bg_badge_emerald;
                    textColor = R.color.badge_emerald_text;
                    label = R.string.admin_status_diverifikasi;
                    break;
                case "belum_diunggah":
                default:
                    drawable = R.drawable.bg_badge_gray;
                    textColor = R.color.status_offline_text;
                    label = R.string.admin_status_belum_diunggah;
                    break;
            }
            badge.setText(label);
            badge.setBackgroundResource(drawable);
            badge.setTextColor(ContextCompat.getColor(badge.getContext(), textColor));
        }
    }
}
