package com.example.blooddono.models;

public class DayHours {
    private boolean isOpen;
    private String openTime;  // Format: "HH:mm"
    private String closeTime; // Format: "HH:mm"

    public DayHours() {
        // Required empty constructor for Firebase
    }

    public DayHours(boolean isOpen, String openTime, String closeTime) {
        this.isOpen = isOpen;
        this.openTime = openTime;
        this.closeTime = closeTime;
    }

    // Getters and setters
    public boolean isOpen() { return isOpen; }
    public void setOpen(boolean open) { isOpen = open; }
    public String getOpenTime() { return openTime; }
    public void setOpenTime(String openTime) { this.openTime = openTime; }
    public String getCloseTime() { return closeTime; }
    public void setCloseTime(String closeTime) { this.closeTime = closeTime; }
}
