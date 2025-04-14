package com.ajinkya.cleantogether;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ajinkya.cleantogether.Adapters.WasteReportAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.ajinkya.cleantogether.Models.WasteReport;
import java.util.ArrayList;
import java.util.List;

public class AdminActivity extends AppCompatActivity {
    private RecyclerView reportsRecyclerView;
    private WasteReportAdapter adapter;
    private FirebaseFirestore db;
    private List<WasteReport> reports;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvNoReports;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.adminToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Admin Dashboard");
        }

        initializeViews();
        if (!areViewsInitialized()) {
            Log.e("AdminActivity", "Views not initialized properly");
            Toast.makeText(this, "Error: Views not initialized", Toast.LENGTH_SHORT).show();
            return;
        }
        setupRecyclerView();
        setupSwipeRefresh();
        loadReports();
    }

    private void initializeViews() {
        db = FirebaseFirestore.getInstance();
        reports = new ArrayList<>();
        reportsRecyclerView = findViewById(R.id.reportsRecyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        tvNoReports = findViewById(R.id.tvNoReports);
        FloatingActionButton refreshFab = findViewById(R.id.refreshFab);
        refreshFab.setOnClickListener(v -> loadReports());
    }

    private void setupRecyclerView() {
        reportsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WasteReportAdapter(reports, new WasteReportAdapter.OnReportActionListener() {
            @Override
            public void onMarkReal(WasteReport report) {
                updateReportStatus(report, "real");
            }

            @Override
            public void onMarkFake(WasteReport report) {
                updateReportStatus(report, "fake");
            }
        });
        reportsRecyclerView.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(this::loadReports);
        swipeRefreshLayout.setColorSchemeResources(
                R.color.blue,
                R.color.green,
                R.color.orange
        );
    }

    private void updateUI() {
        // Ensure we're on the main thread
        runOnUiThread(() -> {
            try {
                // Update adapter
                adapter.notifyDataSetChanged();

                // Update visibility
                if (reports == null || reports.isEmpty()) {
                    tvNoReports.setVisibility(View.VISIBLE);
                    reportsRecyclerView.setVisibility(View.GONE);
                    Log.d("AdminActivity", "No reports to display");
                } else {
                    tvNoReports.setVisibility(View.GONE);
                    reportsRecyclerView.setVisibility(View.VISIBLE);
                    Log.d("AdminActivity", "Displaying " + reports.size() + " reports");
                }
            } catch (Exception e) {
                Log.e("AdminActivity", "Error updating UI", e);
                Toast.makeText(AdminActivity.this,
                        "Error updating display: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean areViewsInitialized() {
        return reportsRecyclerView != null &&
                swipeRefreshLayout != null &&
                tvNoReports != null &&
                reports != null;
    }

    private void loadReports() {
        if (!areViewsInitialized()) {
            Log.e("AdminActivity", "Views not initialized properly");
            Toast.makeText(this, "Error: Views not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        swipeRefreshLayout.setRefreshing(true);
        Log.d("AdminActivity", "Starting to load reports...");

        db.collection("waste_reports")
                .whereEqualTo("verificationStatus", "pending")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    reports.clear();
                    Log.d("AdminActivity", "Successfully retrieved " + queryDocumentSnapshots.size() + " documents");
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        WasteReport report = document.toObject(WasteReport.class);
                        if (report != null) {
                            if (!isValidReport(report)){
                                continue;
                            }
                            report.setDocumentId(document.getId());
                            reports.add(report);
                            Log.d("AdminActivity", "Added report with ID: " + document.getId());
                        } else {
                            Log.w("AdminActivity", "Failed to parse document: " + document.getId());
                        }
                    }
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    Log.e("AdminActivity", "Error loading reports", e);
                    Toast.makeText(this, "Error loading reports: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                })
                .addOnCompleteListener(task -> {
                    swipeRefreshLayout.setRefreshing(false);
                });
    }

    private boolean isValidReport(WasteReport report) {
        return report.getImageUrl() != null &&
                report.getUserId() != null &&
                report.getVerificationStatus() != null &&
                report.getTimestamp() != null;
    }

    private void updateReportStatus(WasteReport report, String status) {
        if (report == null || report.getDocumentId() == null) {
            Toast.makeText(this, "Invalid report data", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("waste_reports")
                .document(report.getDocumentId())
                .update(
                        "verificationStatus", status,
                        "verifiedAt", Timestamp.now()
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Report marked as " + status, Toast.LENGTH_SHORT).show();
                    loadReports();
                })
                .addOnFailureListener(e -> {
                    Log.e("AdminActivity", "Error updating report status", e);
                    Toast.makeText(this, "Error updating status: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        // Navigate to AuthActivity
        Intent intent = new Intent(this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}