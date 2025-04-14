// WasteReportAdapter.java
package com.ajinkya.cleantogether.Adapters;

import android.util.Log;
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

public class WasteReportAdapter extends RecyclerView.Adapter<WasteReportAdapter.ViewHolder> {
    private List<WasteReport> reports;
    private OnReportActionListener listener;

    public interface OnReportActionListener {
        void onMarkReal(WasteReport report);
        void onMarkFake(WasteReport report);
    }

    public WasteReportAdapter(List<WasteReport> reports, OnReportActionListener listener) {
        this.reports = reports;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_waste_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WasteReportAdapter.ViewHolder holder, int position) {
        WasteReport report = reports.get(position);

        if (report.getImageUrl() != null) {
            if (holder.wasteImageView != null) {
                Glide.with(holder.itemView.getContext())
                        .load(report.getImageUrl())
                        .into(holder.wasteImageView);
            } else {
                Log.e("WasteReportAdapter", "ImageView is null for position: " + position);
            }
        } else {
            Log.e("WasteReportAdapter", "Image URL is null for position: " + position);
        }

        if (holder.addressTextView != null) {
            holder.addressTextView.setText(report.getAddress());
        } else {
            Log.e("WasteReportAdapter", "Address TextView is null for position: " + position);
        }

        if (holder.wasteTypeTextView != null) {
            holder.wasteTypeTextView.setText("Type: " + report.getWasteType());
        } else {
            Log.e("WasteReportAdapter", "Waste Type TextView is null for position: " + position);
        }

        holder.markRealButton.setOnClickListener(v -> listener.onMarkReal(report));
        holder.markFakeButton.setOnClickListener(v -> listener.onMarkFake(report));
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView wasteImageView;
        TextView addressTextView;
        TextView wasteTypeTextView;
        Button markRealButton;
        Button markFakeButton;

        ViewHolder(View itemView) {
            super(itemView);
            wasteImageView = itemView.findViewById(R.id.wasteImageView);
            addressTextView = itemView.findViewById(R.id.addressText);
            wasteTypeTextView = itemView.findViewById(R.id.wasteTypeText);
            markRealButton = itemView.findViewById(R.id.markRealButton);
            markFakeButton = itemView.findViewById(R.id.markFakeButton);
        }
    }
}