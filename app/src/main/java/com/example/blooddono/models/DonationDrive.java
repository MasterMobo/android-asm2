package com.example.blooddono.models;

import java.util.HashMap;
import java.util.Map;

public class DonationDrive {
    private String id;
    private String name;
    private long startDate;
    private long endDate;
    private boolean isActive;
    private Map<String, Double> totalCollectedAmounts; // Blood type -> Total amount
    private int totalSites;
    private int totalDonations;
    private long completedAt;

    public DonationDrive() {
        // Required empty constructor for Firebase
        this.totalCollectedAmounts = new HashMap<>();
    }

    public DonationDrive(String name, long startDate, long endDate) {
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isActive = true;
        this.totalCollectedAmounts = new HashMap<>();
        this.totalSites = 0;
        this.totalDonations = 0;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getStartDate() { return startDate; }
    public void setStartDate(long startDate) { this.startDate = startDate; }
    public long getEndDate() { return endDate; }
    public void setEndDate(long endDate) { this.endDate = endDate; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public Map<String, Double> getTotalCollectedAmounts() { return totalCollectedAmounts; }
    public void setTotalCollectedAmounts(Map<String, Double> totalCollectedAmounts) {
        this.totalCollectedAmounts = totalCollectedAmounts;
    }
    public int getTotalSites() { return totalSites; }
    public void setTotalSites(int totalSites) { this.totalSites = totalSites; }
    public int getTotalDonations() { return totalDonations; }
    public void setTotalDonations(int totalDonations) { this.totalDonations = totalDonations; }
    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }
}

