package com.example.blooddono.models;

import java.util.List;

public class Donation {
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_COMPLETED = "completed";

    private String id;
    private String donorId;
    private String donorName;
    private String siteId;
    private String siteName;
    private List<String> bloodTypes;
    private String status;
    private long createdAt;
    private long completedAt;

    public Donation() {
        // Required empty constructor for Firebase
    }

    public Donation(String donorId, String donorName, String siteId, String siteName,
                    List<String> bloodTypes) {
        this.donorId = donorId;
        this.donorName = donorName;
        this.siteId = siteId;
        this.siteName = siteName;
        this.bloodTypes = bloodTypes;
        this.status = STATUS_PENDING;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDonorId() { return donorId; }
    public void setDonorId(String donorId) { this.donorId = donorId; }
    public String getDonorName() { return donorName; }
    public void setDonorName(String donorName) { this.donorName = donorName; }
    public String getSiteId() { return siteId; }
    public void setSiteId(String siteId) { this.siteId = siteId; }
    public String getSiteName() { return siteName; }
    public void setSiteName(String siteName) { this.siteName = siteName; }
    public List<String> getBloodTypes() { return bloodTypes; }
    public void setBloodTypes(List<String> bloodTypes) { this.bloodTypes = bloodTypes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }
}