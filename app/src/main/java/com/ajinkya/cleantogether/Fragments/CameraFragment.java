package com.ajinkya.cleantogether.Fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.annotation.NonNull;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.FusedLocationProviderClient;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.ajinkya.cleantogether.Models.WasteReport;
import com.ajinkya.cleantogether.R;
import com.cloudinary.Cloudinary;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import com.google.common.util.concurrent.ListenableFuture;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import java.util.HashMap;
import java.util.Map;
import androidx.annotation.Nullable;
import android.location.LocationManager;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;


public class CameraFragment extends Fragment {
    private PreviewView viewFinder;
    private ImageCapture imageCapture;
    private FloatingActionButton captureButton;
    private FloatingActionButton switchCamera;
    private ProcessCameraProvider cameraProvider;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;

    private FirebaseStorage storage;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private FusedLocationProviderClient fusedLocationClient;

    private static final int REQUEST_CAMERA_PERMISSION = 10;
    private static final int REQUEST_LOCATION_PERMISSION = 11;

    private Uri pendingImageUri;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, container, false);

        viewFinder = view.findViewById(R.id.viewFinder);
        captureButton = view.findViewById(R.id.captureButton);
        switchCamera = view.findViewById(R.id.switchCamera);

        storage = FirebaseStorage.getInstance();
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            requestPermissions();
        }

        captureButton.setOnClickListener(v -> takePhoto());
        switchCamera.setOnClickListener(v -> toggleCamera());

        return view;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get(); // Get the camera provider
                bindCameraUseCases(); // Bind the camera use cases
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(getContext(), "Error starting camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }


    private void bindCameraUseCases() {
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error binding camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        File photoFile = new File(requireContext().getCacheDir(),
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".jpg");

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        Uri savedUri = Uri.fromFile(photoFile);
                        getCurrentLocation(savedUri);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exc) {
                        Toast.makeText(getContext(), "Error capturing photo", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation(Uri imageUri) {
        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            pendingImageUri = imageUri; // Store the URI before requesting permissions
            requestLocationPermission();
            return;
        }

        // Check if location is enabled
        LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Enable Location")
                    .setMessage("Please enable location services to tag your waste report with location information")
                    .setPositiveButton("Location Settings", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        // Proceed without location
                        showWasteTypeDialog(imageUri, 0, 0, "Location not available");
                    })
                    .create()
                    .show();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        getAddressFromLocation(location, imageUri);
                    } else {
                        // Try to request new location
                        LocationRequest locationRequest = LocationRequest.create()
                                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                                .setInterval(10000)
                                .setFastestInterval(5000)
                                .setNumUpdates(1);

                        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
                            @Override
                            public void onLocationResult(LocationResult locationResult) {
                                Location location = locationResult.getLastLocation();
                                if (location != null) {
                                    getAddressFromLocation(location, imageUri);
                                } else {
                                    showWasteTypeDialog(imageUri, 0, 0, "Location not available");
                                }
                                fusedLocationClient.removeLocationUpdates(this);
                            }
                        }, Looper.getMainLooper());
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to retrieve location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showWasteTypeDialog(imageUri, 0, 0, "Location not available");
                });
    }

    private void requestLocationPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Location Permission Needed")
                    .setMessage("This app needs the Location permission to tag your waste reports with location information")
                    .setPositiveButton("OK", (dialog, which) -> {
                        requestPermissions(
                                new String[] {
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                },
                                REQUEST_LOCATION_PERMISSION
                        );
                    })
                    .create()
                    .show();
        } else {
            requestPermissions(
                    new String[] {
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQUEST_LOCATION_PERMISSION
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted, retry the last operation
                if (pendingImageUri != null) {
                    getCurrentLocation(pendingImageUri);
                }
            } else {
                Toast.makeText(getContext(),
                        "Location permission denied. Location information won't be available.",
                        Toast.LENGTH_LONG).show();
                if (pendingImageUri != null) {
                    showWasteTypeDialog(pendingImageUri, 0, 0, "Location not available");
                }
            }
        }
    }

    private void getAddressFromLocation(Location location, Uri imageUri) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(), location.getLongitude(), 1);
            String address = addresses != null && !addresses.isEmpty()
                    ? addresses.get(0).getAddressLine(0)
                    : "Unknown location";

            showWasteTypeDialog(imageUri, location.getLatitude(),
                    location.getLongitude(), address);
        } catch (IOException e) {
            showWasteTypeDialog(imageUri, location.getLatitude(),
                    location.getLongitude(), "Unknown location");
        }
    }

    private void showWasteTypeDialog(Uri imageUri, double latitude,
                                     double longitude, String address) {
        String[] wasteTypes = {"Organic Waste", "Inorganic Waste"};

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Waste Type")
                .setItems(wasteTypes, (dialog, which) -> {
                    String wasteType = which == 0 ? "organic" : "inorganic";
                    uploadImage(imageUri, latitude, longitude,
                            wasteType, address);
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Cloudinary
        initCloudinary();
    }

    private void initCloudinary() {
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "djkdlbyoi");  // Replace with your cloud name
            config.put("api_key", "128671898467639");        // Replace with your api key
            config.put("api_secret", "lk3DF7k16PlsE1r0peFPgrDtDJQ");  // Replace with your api secret
            MediaManager.init(requireContext(), config);
        } catch (IllegalStateException e) {
            // MediaManager was already initialized
        }
    }

    private void uploadImage(Uri imageUri, double latitude, double longitude, String wasteType, String address) {
        if (imageUri == null) {
            Toast.makeText(getContext(), "Invalid image URI", Toast.LENGTH_SHORT).show();
            return;
        }

        MediaManager.get().upload(imageUri)
                .unsigned("WasteReport")  // Replace with your Cloudinary upload preset
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        // Upload started
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        // Upload progress
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        saveWasteReport(imageUrl, latitude, longitude, wasteType, address);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Upload failed: " + error.getDescription(), Toast.LENGTH_SHORT).show()
                        );
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        // Upload rescheduled
                    }
                })
                .dispatch();
    }

    private String getRealPathFromURI(Uri uri) {
        if (DocumentsContract.isDocumentUri(getContext(), uri)) {
            String documentId = DocumentsContract.getDocumentId(uri);
            if (documentId.startsWith("raw:")) {
                return documentId.replaceFirst("raw:", "");
            }
        }
        return null;
    }


    private void saveWasteReport(String imageUrl, double latitude,
                                 double longitude, String wasteType, String address) {
        String userId = auth.getCurrentUser().getUid();
        WasteReport report = new WasteReport(userId, imageUrl, latitude, longitude, wasteType, address);

        firestore.collection("waste_reports")
                .add(report)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Report uploaded successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to save report", Toast.LENGTH_SHORT).show();
                });
    }



    private void toggleCamera() {
        lensFacing = (lensFacing == CameraSelector.LENS_FACING_BACK)
                ? CameraSelector.LENS_FACING_FRONT
                : CameraSelector.LENS_FACING_BACK;
        bindCameraUseCases();
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(requireActivity(), new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION
        }, REQUEST_CAMERA_PERMISSION);
    }





}