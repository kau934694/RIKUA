package com.example.rikua;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter untuk merender daftar layanan pada {@link ServicesActivity}.
 */
public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(ServiceItem item);
    }

    private final List<ServiceItem> items = new ArrayList<>();
    private final OnItemClickListener clickListener;

    public ServiceAdapter(OnItemClickListener clickListener) {
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_service, parent, false);
        return new ServiceViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void submitList(List<ServiceItem> filtered) {
        items.clear();
        if (filtered != null) {
            items.addAll(filtered);
        }
        notifyDataSetChanged();
    }

    public static List<ServiceItem> copyOf(List<ServiceItem> source) {
        return new ArrayList<>(source);
    }

    class ServiceViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView card;
        private final FrameLayout iconFrame;
        private final ImageView icon;
        private final TextView title;
        private final TextView desc;
        private final TextView badge;
        private final ImageView chevron;

        ServiceViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card_service_item);
            iconFrame = itemView.findViewById(R.id.icon_frame);
            icon = itemView.findViewById(R.id.iv_service_icon);
            title = itemView.findViewById(R.id.tv_service_title);
            desc = itemView.findViewById(R.id.tv_service_desc);
            badge = itemView.findViewById(R.id.tv_service_badge);
            chevron = itemView.findViewById(R.id.iv_chevron);
        }

        void bind(ServiceItem item) {
    title.setText(item.getTitle());
    desc.setText(item.getDescription());

    if (item.getBadge() != null && !item.getBadge().isEmpty()) {
        badge.setVisibility(View.VISIBLE);
        badge.setText(item.getBadge());
        badge.setBackgroundResource(item.getBadgeBgRes());
        badge.setTextColor(ContextCompat.getColor(badge.getContext(), item.getBadgeTextColorRes()));
    } else {
        badge.setVisibility(View.GONE);
    }

    iconFrame.setBackgroundResource(item.getIconBgRes());
    icon.setImageResource(item.getIconRes());
    
    // Bersihkan tint agar warna asli PNG muncul murni
    icon.clearColorFilter();
    icon.setImageTintList(null);

    card.setOnClickListener(v -> {
        if (clickListener != null) {
            clickListener.onItemClick(item);
        }
    });

    chevron.setColorFilter(ContextCompat.getColor(chevron.getContext(), R.color.text_tertiary));
}
    }
}