package com.example.rikua;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter untuk merender daftar petugas pelayanan pada {@link ConsultationActivity}.
 * Menangani binding status ketersediaan, inisial avatar, chip bidang keahlian,
 * serta men-delegasikan aksi WhatsApp / telepon ke {@link OnActionListener}.
 */
public class OfficerAdapter extends RecyclerView.Adapter<OfficerAdapter.OfficerViewHolder> {

    /** Callback aksi kontak untuk satu petugas. */
    public interface OnActionListener {
        void onWhatsAppClick(OfficerItem officer);
        void onPhoneClick(OfficerItem officer);
    }

    private final List<OfficerItem> items = new ArrayList<>();
    private final OnActionListener listener;

    public OfficerAdapter(OnActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<OfficerItem> officers) {
        items.clear();
        if (officers != null) {
            items.addAll(officers);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OfficerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_officer, parent, false);
        return new OfficerViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull OfficerViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class OfficerViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView card;
        private final TextView avatar;
        private final ImageView statusDot;
        private final TextView name;
        private final TextView role;
        private final TextView statusBadge;
        private final TextView hours;
        private final ChipGroup chipGroup;
        private final MaterialButton btnWhatsApp;
        private final MaterialButton btnPhone;

        OfficerViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card_officer);
            avatar = itemView.findViewById(R.id.tv_avatar);
            statusDot = itemView.findViewById(R.id.iv_status_dot);
            name = itemView.findViewById(R.id.tv_officer_name);
            role = itemView.findViewById(R.id.tv_officer_role);
            statusBadge = itemView.findViewById(R.id.tv_status_badge);
            hours = itemView.findViewById(R.id.tv_service_hours);
            chipGroup = itemView.findViewById(R.id.chip_group_expertise);
            btnWhatsApp = itemView.findViewById(R.id.btn_whatsapp);
            btnPhone = itemView.findViewById(R.id.btn_telepon);
        }

        void bind(OfficerItem item) {
            avatar.setText(item.getInitials());
            avatar.setBackgroundResource(item.getAvatarBgRes());

            name.setText(item.getName());
            role.setText(item.getRole());
            hours.setText(itemView.getContext().getString(
                    R.string.consultation_hours_prefix, item.getServiceHours()));

            // Status ketersediaan -> dot + badge
            if (item.isAvailable()) {
                statusDot.setImageResource(R.drawable.dot_status_online);
                statusBadge.setText(R.string.consultation_status_available);
                statusBadge.setBackgroundResource(R.drawable.bg_badge_emerald);
                statusBadge.setTextColor(ContextCompat.getColor(
                        statusBadge.getContext(), R.color.status_green_dark));
            } else {
                statusDot.setImageResource(R.drawable.dot_status_offline);
                statusBadge.setText(R.string.consultation_status_offline);
                statusBadge.setBackgroundResource(R.drawable.bg_badge_gray);
                statusBadge.setTextColor(ContextCompat.getColor(
                        statusBadge.getContext(), R.color.status_offline_text));
            }

            // Chip bidang keahlian (dinamis)
            chipGroup.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(chipGroup.getContext());
            for (String topic : item.getExpertise()) {
                Chip chip = (Chip) inflater.inflate(R.layout.chip_expertise, chipGroup, false);
                chip.setText(topic);
                chipGroup.addView(chip);
            }

            btnWhatsApp.setOnClickListener(v -> {
                if (listener != null) listener.onWhatsAppClick(item);
            });
            btnPhone.setOnClickListener(v -> {
                if (listener != null) listener.onPhoneClick(item);
            });
        }
    }
}
