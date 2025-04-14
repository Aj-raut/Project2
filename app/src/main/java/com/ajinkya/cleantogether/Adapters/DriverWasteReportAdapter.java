package com.ajinkya.cleantogether.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ajinkya.cleantogether.Models.WasteReport;
import com.ajinkya.cleantogether.R;
import com.bumptech.glide.Glide;
import java.util.List;

public class DriverWasteReportAdapter extends RecyclerView.Adapter<DriverWasteReportAdapter.ViewHolder> {
    private List<WasteReport> reports;
    private OnReportCleanedListener listener;

    public interface OnReportCleanedListener {
        void onMarkCleaned(WasteReport report);
    }

    public DriverWasteReportAdapter(List<WasteReport> reports, OnReportCleanedListener listener) {
        this.reports = reports;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_driver_waste_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WasteReport report = reports.get(position);

        Glide.with(holder.itemView.getContext())
                .load(report.getImageUrl())
                .into(holder.wasteImageView);

        holder.addressTextView.setText(report.getAddress());
        holder.wasteTypeTextView.setText("Type: " + report.getWasteType());

        holder.markCleanedButton.setOnClickListener(v -> listener.onMarkCleaned(report));
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView wasteImageView;
        TextView addressTextView;
        TextView wasteTypeTextView;
        Button markCleanedButton;

        ViewHolder(View itemView) {
            super(itemView);
            wasteImageView = itemView.findViewById(R.id.wasteImageView);
            addressTextView = itemView.findViewById(R.id.addressTextView);
            wasteTypeTextView = itemView.findViewById(R.id.wasteTypeTextView);
            markCleanedButton = itemView.findViewById(R.id.markCleanedButton);
        }
    }
}