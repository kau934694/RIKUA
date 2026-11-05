package com.example.rikua;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter daftar "Pengumuman Kehendak Nikah" — merender kartu jadwal akad dari
 * dataset lokal. Mendukung pembaruan in-place lewat {@link #submitList(List)}
 * saat pencarian / chip filter berubah (pola sama seperti {@link FacilityAdapter}).
 */
public class PengumumanAdapter extends RecyclerView.Adapter<PengumumanAdapter.JadwalViewHolder> {

    private final List<PengumumanItem> items = new ArrayList<>();

    /** Ganti konten list dengan hasil filter terbaru. */
    public void submitList(List<PengumumanItem> filtered) {
        items.clear();
        if (filtered != null) {
            items.addAll(filtered);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public JadwalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pengumuman_jadwal, parent, false);
        return new JadwalViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull JadwalViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class JadwalViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout statusBadge;
        private final ImageView statusIcon;
        private final TextView statusLabel;
        private final TextView village;
        private final TextView coupleNames;
        private final TextView schedule;
        private final TextView location;
        private final TextView fileInfo;

        JadwalViewHolder(@NonNull View itemView) {
            super(itemView);
            statusBadge = itemView.findViewById(R.id.layout_status_badge);
            statusIcon = itemView.findViewById(R.id.iv_status_icon);
            statusLabel = itemView.findViewById(R.id.tv_status_label);
            village = itemView.findViewById(R.id.tv_village);
            coupleNames = itemView.findViewById(R.id.tv_couple_names);
            schedule = itemView.findViewById(R.id.tv_schedule);
            location = itemView.findViewById(R.id.tv_location);
            fileInfo = itemView.findViewById(R.id.tv_file_info);
        }

        void bind(PengumumanItem item) {
            coupleNames.setText(item.getCoupleNames());
            schedule.setText(item.getSchedule());
            location.setText(item.getLocation());
            village.setText(item.getVillage());
            fileInfo.setText(item.getFileInfo());

            // Badge status: hijau terverifikasi vs amber uji publik.
            boolean terverifikasi =
                    item.getStatus() == PengumumanItem.STATUS_TERVERIFIKASI;
            statusBadge.setBackgroundResource(
                    terverifikasi ? R.drawable.bg_badge_emerald : R.drawable.bg_badge_amber);
            statusIcon.setImageResource(
                    terverifikasi ? R.drawable.ic_check_circle : R.drawable.ic_schedule);
            int colorRes = terverifikasi
                    ? R.color.badge_emerald_text : R.color.badge_amber_text;
            statusIcon.setColorFilter(ContextCompat.getColor(
                    statusIcon.getContext(), colorRes));
            statusLabel.setText(terverifikasi
                    ? R.string.peng_status_terverifikasi
                    : R.string.peng_status_publikasi);
            statusLabel.setTextColor(ContextCompat.getColor(
                    statusLabel.getContext(), colorRes));
        }
    }
}
