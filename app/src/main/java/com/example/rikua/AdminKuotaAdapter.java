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
 * Adapter strip kuota akad 7 hari pada {@link AdminDashboardActivity}.
 */
public class AdminKuotaAdapter
        extends RecyclerView.Adapter<AdminKuotaAdapter.KuotaViewHolder> {

    private final List<AdminKuotaItem> items = new ArrayList<>();

    public void submitList(List<AdminKuotaItem> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public KuotaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_kuota_hari, parent, false);
        return new KuotaViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull KuotaViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class KuotaViewHolder extends RecyclerView.ViewHolder {
        private final View kolom;
        private final TextView tvHari;
        private final TextView tvTanggal;
        private final TextView tvTerisi;

        KuotaViewHolder(@NonNull View itemView) {
            super(itemView);
            kolom = itemView.findViewById(R.id.kolom_kuota);
            tvHari = itemView.findViewById(R.id.tv_hari);
            tvTanggal = itemView.findViewById(R.id.tv_tanggal);
            tvTerisi = itemView.findViewById(R.id.tv_terisi);
        }

        void bind(AdminKuotaItem item) {
            tvHari.setText(item.getLabelHari());
            tvTanggal.setText(item.getLabelTanggal());
            tvTerisi.setText(item.getLabelTerisi());

            if (item.isPenuh()) {
                tvTerisi.setTextColor(ContextCompat.getColor(
                        tvTerisi.getContext(), R.color.badge_amber_text));
            } else {
                tvTerisi.setTextColor(ContextCompat.getColor(
                        tvTerisi.getContext(), R.color.text_secondary));
            }

            // Hari ini ditandai garis tepi, bukan hanya warna teks.
            kolom.setBackgroundResource(item.isHariIni()
                    ? R.drawable.bg_admin_kuota_hari_today
                    : R.drawable.bg_admin_kuota_hari);
        }
    }
}
