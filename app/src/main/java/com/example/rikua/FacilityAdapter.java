package com.example.rikua;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

public class FacilityAdapter extends RecyclerView.Adapter<FacilityAdapter.FacilityViewHolder> {

    public interface OnItemClickListener {
        void onFacilityClick(FacilityItem item);
    }

    private final List<FacilityItem> items = new ArrayList<>();
    private final OnItemClickListener clickListener;

    public FacilityAdapter(OnItemClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void submitList(List<FacilityItem> filtered) {
        items.clear();
        if (filtered != null) {
            items.addAll(filtered);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FacilityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_facility, parent, false);
        return new FacilityViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull FacilityViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class FacilityViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView card;
        private final View accentStrip;
        private final ShapeableImageView thumbIcon;
        private final TextView name;
        private final TextView categoryBadge;
        private final TextView location;
        private final TextView detail1;
        private final TextView detail2;

        FacilityViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card_facility_item);
            accentStrip = itemView.findViewById(R.id.view_accent_strip);
            thumbIcon = itemView.findViewById(R.id.iv_thumb_icon);
            name = itemView.findViewById(R.id.tv_facility_name);
            categoryBadge = itemView.findViewById(R.id.tv_facility_category);
            location = itemView.findViewById(R.id.tv_facility_location);
            detail1 = itemView.findViewById(R.id.tv_facility_detail1);
            detail2 = itemView.findViewById(R.id.tv_facility_detail2);
        }

        void bind(FacilityItem item) {
            name.setText(item.getName());
            location.setText(item.getLocation());
            detail1.setText(item.getDetail1());

            // Badge Kategori
            categoryBadge.setText(item.getCategoryLabel());
            categoryBadge.setBackgroundResource(item.getBadgeBgRes());
            categoryBadge.setTextColor(ContextCompat.getColor(
                    categoryBadge.getContext(), item.getBadgeTextColorRes()));

            // Strip Akses Kiri
            accentStrip.setBackgroundColor(ContextCompat.getColor(
                    accentStrip.getContext(), item.getAccentColorRes()));

            // Handling Pembuatan Gambar dengan Glide
            if (item.getImageUrl() != null && !item.getImageUrl().trim().isEmpty()) {
                thumbIcon.clearColorFilter();
                thumbIcon.setPadding(0, 0, 0, 0);

                Log.d("GLIDE_DEBUG", "Memuat foto: " + item.getImageUrl());

                Glide.with(itemView.getContext())
                        .load(item.getImageUrl())
                        .centerCrop()
                        .placeholder(item.getThumbBgRes())
                        .error(item.getThumbIconRes())
                        .into(thumbIcon);
            } else {
                thumbIcon.setImageResource(item.getThumbIconRes());
                thumbIcon.setBackgroundResource(item.getThumbBgRes());
                thumbIcon.setColorFilter(ContextCompat.getColor(
                        thumbIcon.getContext(), item.getThumbTintRes()));
                
                int paddingPx = (int) (12 * itemView.getContext().getResources().getDisplayMetrics().density);
                thumbIcon.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
            }

            // Detail 2 (Keterangan)
            if (item.getDetail2() != null && !item.getDetail2().isEmpty()) {
                detail2.setVisibility(View.VISIBLE);
                detail2.setText(item.getDetail2());
            } else {
                detail2.setVisibility(View.GONE);
            }

            card.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onFacilityClick(item);
                }
            });
        }
    }
}