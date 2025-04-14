package com.ajinkya.cleantogether.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ajinkya.cleantogether.R;
import com.ajinkya.cleantogether.Models.WasteReport;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private RecyclerView recyclerView;
    private WasteReportAdapter adapter;
    private List<WasteReport> reportList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        Toolbar toolbar = view.findViewById(R.id.userToolbar);
        if (getActivity() instanceof AppCompatActivity) {
            ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
            ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle("Your Reports");
        }


        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.userRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh);


        reportList = new ArrayList<>();
        adapter = new WasteReportAdapter(reportList);
        recyclerView.setAdapter(adapter);

        // Load user's complaints
        loadUserComplaints();

        return view;
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(this::loadUserComplaints);
        swipeRefreshLayout.setColorSchemeResources(
                R.color.blue,
                R.color.green,
                R.color.orange
        );
    }

    private void loadUserComplaints() {
        String userId = auth.getCurrentUser().getUid();

        db.collection("waste_reports")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    reportList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        WasteReport report = document.toObject(WasteReport.class);
                        report.setDocumentId(document.getId());
                        reportList.add(report);
                    }
                    adapter.notifyDataSetChanged();

                    if (reportList.isEmpty()) {
                        Toast.makeText(getContext(), "No complaints found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading complaints: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private static class WasteReportAdapter extends RecyclerView.Adapter<WasteReportAdapter.ViewHolder> {
        private final List<WasteReport> reports;

        public WasteReportAdapter(List<WasteReport> reports) {
            this.reports = reports;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_user_ws_report, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            WasteReport report = reports.get(position);
            holder.bind(report);
        }

        @Override
        public int getItemCount() {
            return reports.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            private final ImageView wasteImageView;
            private final TextView addressText;
            private final TextView statusText;
            private final TextView wasteTypeText;
            private final TextView timestampText;

            public ViewHolder(View itemView) {
                super(itemView);
                wasteImageView = itemView.findViewById(R.id.wasteImageView);
                addressText = itemView.findViewById(R.id.addressText);
                statusText = itemView.findViewById(R.id.statusText);
                wasteTypeText = itemView.findViewById(R.id.wasteTypeText);
                timestampText = itemView.findViewById(R.id.timestampText);
            }

            public void bind(WasteReport report) {
                Glide.with(itemView.getContext())
                        .load(report.getImageUrl())
                        .into(wasteImageView);
                addressText.setText(report.getAddress());
                statusText.setText(String.format("Status: %s", report.getCleaningStatus()));
                wasteTypeText.setText(String.format("Type: %s", report.getWasteType()));
                timestampText.setText(android.text.format.DateFormat.format("MMM dd, yyyy hh:mm a", report.getTimestamp()));
            }
        }
    }
}