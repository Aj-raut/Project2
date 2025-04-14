package com.ajinkya.cleantogether;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;
import com.ajinkya.cleantogether.Models.WasteReport;
import com.ajinkya.cleantogether.Adapters.DriverWasteReportAdapter;
import java.util.ArrayList;
import java.util.List;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class DriverActivity extends AppCompatActivity implements DriverWasteReportAdapter.OnReportCleanedListener {
    private RecyclerView reportsRecyclerView;
    private DriverWasteReportAdapter adapter;
    private FirebaseFirestore db;
    private List<WasteReport> reports;
    private String driverId;
    private static final String DRIVER_ID = "DRIVER_001";
    private MapView map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(R.layout.activity_driver);

        db = FirebaseFirestore.getInstance();
        reports = new ArrayList<>();
        driverId = getIntent().getStringExtra("DRIVER_ID");
        if (driverId == null) {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null) {
                driverId = auth.getCurrentUser().getUid();
            } else {
                driverId = DRIVER_ID;
            }
        }


        map = findViewById(R.id.mapView);
        map.setMultiTouchControls(true);
        map.getController().setZoom(12.0);

        map.getController().setCenter(new GeoPoint(18.5204, 73.8567)); // Default to Pune

        reportsRecyclerView = findViewById(R.id.assignedReportsRecyclerView);
        reportsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new DriverWasteReportAdapter(reports, this);
        reportsRecyclerView.setAdapter(adapter);

        FloatingActionButton refreshFab = findViewById(R.id.refreshFab);
        refreshFab.setOnClickListener(v -> loadAssignedReports());

        loadAssignedReports();
    }

    private void loadAssignedReports() {
        Query query = db.collection("waste_reports")
                .whereEqualTo("cleaningStatus", "pending")
                .whereEqualTo("verificationStatus", "real")
                .orderBy("timestamp", Query.Direction.DESCENDING);

        if (!DRIVER_ID.equals(driverId)) {
            query = query.whereEqualTo("assignedDriverId", driverId);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    reports.clear();
                    map.getOverlays().clear(); // Clear existing markers

                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        WasteReport report = document.toObject(WasteReport.class);
                        if (report != null) {
                            report.setDocumentId(document.getId());
                            reports.add(report);

                            // Add marker for each report
                            if (report.getLatitude() != 0 && report.getLongitude() != 0) {
                                Marker marker = new Marker(map);
                                marker.setPosition(new GeoPoint(report.getLatitude(), report.getLongitude()));
                                marker.setTitle("Pending Cleanup");
                                marker.setSnippet("Address: " + report.getAddress() + "\nWaste Type: " + report.getWasteType());
                                map.getOverlays().add(marker);

                            }
                        }
                    }

                    map.invalidate(); 
                    adapter.notifyDataSetChanged();

                    if (reports.isEmpty()) {
                        Toast.makeText(this, "No pending reports found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading reports: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onMarkCleaned(WasteReport report) {
        db.collection("waste_reports")
                .document(report.getDocumentId())
                .update("cleaningStatus", "cleaned")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Report marked as cleaned", Toast.LENGTH_SHORT).show();
                    loadAssignedReports();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error updating status: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
    }
}