package com.ajinkya.cleantogether.Models;

import com.google.firebase.Timestamp;

import com.google.firebase.firestore.Exclude;

import java.util.Date;

public class WasteReport {
    private String documentId;
    private String userId;
    private String imageUrl;
    private double latitude;
    private double longitude;
    private String wasteType; // "organic" or "inorganic"
    private Date timestamp;
    private String address;
    private String verificationStatus; // "pending", "real", "fake"
    private String cleaningStatus; // "pending", "cleaned"
    private String assignedDriverId;
    private Timestamp verifiedAt;
    private Timestamp cleanedAt;

    public WasteReport() {
        // Required empty constructor for Firebase
    }


    public WasteReport(String userId, String imageUrl, double latitude, double longitude,
                       String wasteType, String address) {
        this.userId = userId;
        this.imageUrl = imageUrl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.wasteType = wasteType;
        this.timestamp = new Date();
        this.address = address;
        this.verificationStatus = "pending";
        this.cleaningStatus = "pending";
        this.assignedDriverId = null;
        this.verifiedAt = null;
        this.cleanedAt = null;
    }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }


    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getWasteType() { return wasteType; }
    public void setWasteType(String wasteType) { this.wasteType = wasteType; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
        if (verificationStatus.equals("real") || verificationStatus.equals("fake")) {
            this.verifiedAt = Timestamp.now();
        }
    }

    public String getCleaningStatus() { return cleaningStatus; }
    public void setCleaningStatus(String cleaningStatus) {
        this.cleaningStatus = cleaningStatus;
        if (cleaningStatus.equals("cleaned")) {
            this.cleanedAt = Timestamp.now();
        }
    }

    public String getAssignedDriverId() { return assignedDriverId; }
    public void setAssignedDriverId(String assignedDriverId) { this.assignedDriverId = assignedDriverId; }

    public Timestamp getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Timestamp verifiedAt) { this.verifiedAt = verifiedAt; }

    public Timestamp getCleanedAt() { return cleanedAt; }
    public void setCleanedAt(Timestamp cleanedAt) { this.cleanedAt = cleanedAt; }
}
