
package com.example.rikua;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rikua.databinding.ItemUploadDokumenBinding;

import java.util.List;

/**
 * Adapter dokumen Langkah 5 RIKUA.
 *
 * Mendukung empat status:
 * 1. Terunggah
 * 2. Perlu Perbaikan
 * 3. Belum Diunggah
 * 4. Tidak Diperlukan
 */
public class DokumenUploadAdapter
        extends RecyclerView.Adapter<DokumenUploadAdapter.DokumenViewHolder> {

    public interface OnDokumenActionListener {
        void onPilihFile(int position);
        void onAmbilFoto(int position);
    }

    private final List<DokumenItem> items;
    private final OnDokumenActionListener listener;

    public DokumenUploadAdapter(
            List<DokumenItem> items,
            OnDokumenActionListener listener
    ) {
        this.items = items;
        this.listener = listener;
    }

    public DokumenItem getItem(int position) {
        return items.get(position);
    }

    @NonNull
    @Override
    public DokumenViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        ItemUploadDokumenBinding binding =
                ItemUploadDokumenBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                );

        return new DokumenViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(
            @NonNull DokumenViewHolder holder,
            int position
    ) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // =====================================================
    // VIEW HOLDER
    // =====================================================

    static class DokumenViewHolder extends RecyclerView.ViewHolder {

        private final ItemUploadDokumenBinding binding;

        DokumenViewHolder(ItemUploadDokumenBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(
                DokumenItem item,
                OnDokumenActionListener listener
        ) {
            android.content.Context ctx =
                    binding.getRoot().getContext();

            String btnText;
            final boolean aksiAktif =
        item.getStatus() != DokumenItem.STATUS_TIDAK_DIPERLUKAN;

            // Reset tampilan agar tidak terjadi state tertinggal
            // akibat RecyclerView menggunakan ulang ViewHolder.

            binding.tvJudulDokumen.setText(item.getJudul());

            binding.layoutErrorBox.setVisibility(View.GONE);

            binding.btnKamera.setVisibility(View.VISIBLE);

            binding.btnUnggahFile.setEnabled(true);
            binding.btnKamera.setEnabled(true);

            binding.btnUnggahFile.setAlpha(1f);
            binding.btnKamera.setAlpha(1f);

            // =================================================
            // STATUS DOKUMEN
            // =================================================

            switch (item.getStatus()) {

                // ---------------------------------------------
                // 1. DOKUMEN TIDAK DIPERLUKAN
                // ---------------------------------------------

                case DokumenItem.STATUS_TIDAK_DIPERLUKAN:

                    binding.tvInfoFile.setText(
                            "Dokumen tidak diperlukan "
                                    + "berdasarkan kondisi pendaftaran."
                    );

                    binding.tvBadgeStatus.setText(
                            "Tidak Diperlukan"
                    );

                    binding.tvBadgeStatus.setBackgroundResource(
                            R.drawable.bg_badge_gray
                    );

                    binding.tvBadgeStatus.setTextColor(
                            ContextCompat.getColor(
                                    ctx,
                                    R.color.status_offline_text
                            )
                    );

                    btnText = "Tidak Diperlukan";

                    // Nonaktifkan seluruh aksi upload.

                    binding.btnKamera.setEnabled(false);
                    binding.btnUnggahFile.setEnabled(false);

                    binding.btnKamera.setAlpha(0.4f);
                    binding.btnUnggahFile.setAlpha(0.4f);

                    break;

                // ---------------------------------------------
                // 2. DOKUMEN SUDAH TERUNGGAH / DIVERIFIKASI
                // ---------------------------------------------

                case DokumenItem.STATUS_TERUNGGAH:

                    binding.tvInfoFile.setText(
                            ctx.getString(
                                    R.string.s5_file_terunggah,
                                    item.getNamaFile() == null
                                            ? "-"
                                            : item.getNamaFile()
                            )
                    );

                    binding.tvBadgeStatus.setText(
                            R.string.s5_status_terunggah
                    );

                    binding.tvBadgeStatus.setBackgroundResource(
                            R.drawable.bg_s5_badge_emerald
                    );

                    binding.tvBadgeStatus.setTextColor(
                            ContextCompat.getColor(
                                    ctx,
                                    R.color.s5_emerald_badge_text
                            )
                    );

                    // File sudah ada, kamera tidak diperlukan.
                    binding.btnKamera.setVisibility(View.GONE);

                    // File tetap dapat diganti.
                    btnText = ctx.getString(
                            R.string.s5_btn_ganti_file
                    );

                    break;

                // ---------------------------------------------
                // 3. DOKUMEN PERLU PERBAIKAN
                // ---------------------------------------------

                case DokumenItem.STATUS_PERLU_PERBAIKAN:

                    binding.tvInfoFile.setText(
                            item.getDeskripsi()
                    );

                    binding.tvBadgeStatus.setText(
                            R.string.s5_status_perlu_perbaikan
                    );

                    binding.tvBadgeStatus.setBackgroundResource(
                            R.drawable.bg_s5_badge_amber
                    );

                    binding.tvBadgeStatus.setTextColor(
                            ContextCompat.getColor(
                                    ctx,
                                    R.color.s5_amber_text
                            )
                    );

                    binding.layoutErrorBox.setVisibility(
                            View.VISIBLE
                    );

                    binding.tvPesanError.setText(
                            item.getPesanPetugas() == null
                                    ? "Dokumen perlu diperbaiki."
                                    : item.getPesanPetugas()
                    );

                    btnText = ctx.getString(
                            R.string.s5_btn_unggah_ulang
                    );

                    break;

                // ---------------------------------------------
                // 4. BELUM DIUNGGAH
                // ---------------------------------------------

                case DokumenItem.STATUS_BELUM:
                default:

                    binding.tvInfoFile.setText(
                            item.getDeskripsi()
                    );

                    binding.tvBadgeStatus.setText(
                            R.string.s5_status_belum
                    );

                    binding.tvBadgeStatus.setBackgroundResource(
                            R.drawable.bg_badge_gray
                    );

                    binding.tvBadgeStatus.setTextColor(
                            ContextCompat.getColor(
                                    ctx,
                                    R.color.status_offline_text
                            )
                    );

                    btnText = ctx.getString(
                            R.string.s5_btn_unggah
                    );

                    break;
            }

            // =================================================
            // TOMBOL UNGGAH
            // =================================================

            binding.btnUnggahFile.setText(btnText);

            binding.btnUnggahFile.setEnabled(aksiAktif);

            // =================================================
            // STROKE KARTU DOKUMEN
            // =================================================

            int strokeNormal = ContextCompat.getColor(
                    ctx,
                    R.color.md3_outline_variant_40
            );

            int strokeAmber = ContextCompat.getColor(
                    ctx,
                    R.color.s5_amber_border
            );

            boolean perluPerbaikan =
                    item.getStatus()
                            == DokumenItem.STATUS_PERLU_PERBAIKAN;

            binding.cardDokumen.setStrokeColor(
                    ColorStateList.valueOf(
                            perluPerbaikan
                                    ? strokeAmber
                                    : strokeNormal
                    )
            );

            binding.cardDokumen.setStrokeWidth(
                    dpToPx(
                            ctx,
                            perluPerbaikan ? 1.6f : 1f
                    )
            );

            // =================================================
            // LISTENER TOMBOL UNGGAH
            // =================================================

            binding.btnUnggahFile.setOnClickListener(v -> {

                if (!aksiAktif || listener == null) {
                    return;
                }

                int position = getBindingAdapterPosition();

                if (position == RecyclerView.NO_POSITION) {
                    return;
                }

                listener.onPilihFile(position);
            });

            // =================================================
            // LISTENER TOMBOL KAMERA
            // =================================================

            binding.btnKamera.setOnClickListener(v -> {

                if (!aksiAktif || listener == null) {
                    return;
                }

                int position = getBindingAdapterPosition();

                if (position == RecyclerView.NO_POSITION) {
                    return;
                }

                listener.onAmbilFoto(position);
            });
        }

        private static int dpToPx(
                android.content.Context ctx,
                float dp
        ) {
            return Math.round(
                    dp * ctx.getResources()
                            .getDisplayMetrics().density
            );
        }
    }
}
