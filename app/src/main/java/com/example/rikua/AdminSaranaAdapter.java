package com.example.rikua;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminSaranaAdapter
        extends RecyclerView.Adapter<AdminSaranaAdapter.SaranaViewHolder> {

    interface SaranaActionListener {
        void onEdit(AdminSaranaItem item);
        void onDelete(AdminSaranaItem item);
    }

    private final List<AdminSaranaItem> items = new ArrayList<>();
    private final SaranaActionListener listener;

    AdminSaranaAdapter(SaranaActionListener listener) {
        this.listener = listener;
    }

    void submitList(List<AdminSaranaItem> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SaranaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_sarana, parent, false);
        return new SaranaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SaranaViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class SaranaViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvNama;
        private final TextView tvJenis;
        private final TextView tvDesa;
        private final TextView tvAlamat;
        private final MaterialButton btnEdit;
        private final MaterialButton btnHapus;

        SaranaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNama = itemView.findViewById(R.id.tv_nama_sarana);
            tvJenis = itemView.findViewById(R.id.tv_jenis_sarana);
            tvDesa = itemView.findViewById(R.id.tv_desa_sarana);
            tvAlamat = itemView.findViewById(R.id.tv_alamat_sarana);
            btnEdit = itemView.findViewById(R.id.btn_edit_sarana);
            btnHapus = itemView.findViewById(R.id.btn_hapus_sarana);
        }

        void bind(AdminSaranaItem item) {
            tvNama.setText(item.getNamaSarana());
            tvJenis.setText(labelJenis(item.getJenis()));
            tvDesa.setText(item.getNamaDesa());
            tvAlamat.setText(item.getAlamat());
            btnEdit.setOnClickListener(v -> listener.onEdit(item));
            btnHapus.setOnClickListener(v -> listener.onDelete(item));
        }

        private String labelJenis(String jenis) {
            if (jenis == null || jenis.trim().isEmpty()) {
                return "-";
            }
            String value = jenis.trim();
            return value.substring(0, 1).toUpperCase(Locale.forLanguageTag("id-ID"))
                    + value.substring(1);
        }
    }
}
