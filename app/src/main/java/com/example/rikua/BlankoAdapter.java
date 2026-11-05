package com.example.rikua;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter RecyclerView untuk daftar blanko N1–N6 pada {@link BlankoActivity}.
 */
public class BlankoAdapter extends RecyclerView.Adapter<BlankoAdapter.BlankoViewHolder> {

    public interface OnDownloadClickListener {
        void onDownloadClick(BlankoItem item);
    }

    private final List<BlankoItem> items = new ArrayList<>();
    private final OnDownloadClickListener downloadListener;

    public BlankoAdapter(OnDownloadClickListener downloadListener) {
        this.downloadListener = downloadListener;
    }

    public void submitList(List<BlankoItem> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BlankoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_blanko, parent, false);
        return new BlankoViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull BlankoViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class BlankoViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView card;
        private final TextView tvKode;
        private final TextView tvJudul;
        private final TextView tvDeskripsi;
        private final TextView tvSize;
        private final TextView tvKategori;
        private final MaterialButton btnUnduh;

        BlankoViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card_blanko_item);
            tvKode = itemView.findViewById(R.id.tv_blanko_kode);
            tvJudul = itemView.findViewById(R.id.tv_blanko_judul);
            tvDeskripsi = itemView.findViewById(R.id.tv_blanko_deskripsi);
            tvSize = itemView.findViewById(R.id.tv_blanko_size);
            tvKategori = itemView.findViewById(R.id.tv_blanko_kategori);
            btnUnduh = itemView.findViewById(R.id.btn_unduh_pdf);
        }

        void bind(BlankoItem item) {
            tvKode.setText(item.getKode());
            tvJudul.setText(item.getJudul());
            tvDeskripsi.setText(item.getDeskripsi());
            tvSize.setText("PDF \u2022 " + item.getFileSize());

            // Badge kategori: Wajib (emerald) / Kondisional (amber)
            tvKategori.setText(item.getKategori());
            if (item.isWajib()) {
                tvKategori.setBackgroundResource(R.drawable.bg_badge_emerald);
                tvKategori.setTextColor(ContextCompat.getColor(
                        tvKategori.getContext(), R.color.status_green_dark));
            } else {
                tvKategori.setBackgroundResource(R.drawable.bg_badge_amber);
                tvKategori.setTextColor(ContextCompat.getColor(
                        tvKategori.getContext(), R.color.badge_amber_text));
            }

            btnUnduh.setOnClickListener(v -> {
                if (downloadListener != null) {
                    downloadListener.onDownloadClick(item);
                }
            });
        }
    }
}
