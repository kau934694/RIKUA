
package com.example.rikua;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class JadwalPernikahanAdapter
        extends RecyclerView.Adapter<JadwalPernikahanAdapter.ViewHolder> {

    private final List<JadwalPernikahanItem> daftar = new ArrayList<>();

    public void setData(List<JadwalPernikahanItem> data) {
        daftar.clear();
        daftar.addAll(data);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nama, jadwal, lokasi;

        ViewHolder(LinearLayout card) {
            super(card);

            nama = new TextView(card.getContext());
            nama.setTextSize(17);
            nama.setTypeface(null, Typeface.BOLD);
            nama.setTextColor(Color.rgb(45, 75, 58));

            jadwal = new TextView(card.getContext());
            jadwal.setTextSize(14);
            jadwal.setTextColor(Color.DKGRAY);

            lokasi = new TextView(card.getContext());
            lokasi.setTextSize(14);
            lokasi.setTextColor(Color.DKGRAY);

            card.addView(nama);
            card.addView(jadwal);
            card.addView(lokasi);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        LinearLayout card = new LinearLayout(parent.getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(parent, 16), dp(parent, 16),
                dp(parent, 16), dp(parent, 16));

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(parent, 14));
        background.setStroke(dp(parent, 1), Color.rgb(225, 230, 225));
        card.setBackground(background);
        card.setElevation(dp(parent, 2));

        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(parent, 4), 0, dp(parent, 12));
        card.setLayoutParams(params);

        ViewHolder holder = new ViewHolder(card);

        LinearLayout.LayoutParams textParams =
                (LinearLayout.LayoutParams) holder.jadwal.getLayoutParams();
        textParams.topMargin = dp(parent, 8);
        holder.jadwal.setLayoutParams(textParams);

        textParams = (LinearLayout.LayoutParams)
                holder.lokasi.getLayoutParams();
        textParams.topMargin = dp(parent, 5);
        holder.lokasi.setLayoutParams(textParams);

        return holder;
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder, int position) {

        JadwalPernikahanItem item = daftar.get(position);

        holder.nama.setText(item.getNama());
        holder.jadwal.setText("Jadwal akad: " + item.getJadwal());
        holder.lokasi.setText("Lokasi: " + item.getLokasi());
    }

    @Override
    public int getItemCount() {
        return daftar.size();
    }

    private int dp(ViewGroup parent, int value) {
        return (int) (value * parent.getResources()
                .getDisplayMetrics().density + 0.5f);
    }
}
