package com.example.blooddono.models;

import java.util.List;
import java.util.Map;

public class DonationSite {
    public static final String TYPE_PERMANENT = "permanent";
    public static final String TYPE_LIMITED = "limited";
    public static final String HOURS_24_7 = "24/7";
    public static final String HOURS_SPECIFIC = "specific";
    public static final String[] BLOOD_TYPES = {
            "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"
    };

    private String id;
    private String ownerId;
    private String name;
    private String description;
    private double latitude;
    private double longitude;
    private String address;
    private String type; // permanent or limited
    private long startDate; // timestamp for limited type
    private long endDate;   // timestamp for limited type
    private String hoursType; // 24/7 or specific
    private Map<String, DayHours> operatingHours; // Day of week -> operating hours
    private List<String> neededBloodTypes;

    public DonationSite() {
        // Required empty constructor for Firebase
    }

    public DonationSite(String name, String description, double latitude, double longitude,
                        String address, String type, Long startDate, Long endDate,
                        String hoursType, Map<String, DayHours> operatingHours,
                        List<String> neededBloodTypes) {
        this.name = name;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.type = type;
        this.startDate = startDate != null ? startDate : 0;
        this.endDate = endDate != null ? endDate : 0;
        this.hoursType = hoursType;
        this.operatingHours = operatingHours;
        this.neededBloodTypes = neededBloodTypes;
    }

    // Add getters and setters for new fields
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public long getStartDate() { return startDate; }
    public void setStartDate(long startDate) { this.startDate = startDate; }
    public long getEndDate() { return endDate; }
    public void setEndDate(long endDate) { this.endDate = endDate; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getHoursType() { return hoursType; }
    public void setHoursType(String hoursType) { this.hoursType = hoursType; }
    public Map<String, DayHours> getOperatingHours() { return operatingHours; }
    public void setOperatingHours(Map<String, DayHours> operatingHours) {
        this.operatingHours = operatingHours;
    }
    public List<String> getNeededBloodTypes() { return neededBloodTypes; }
    public void setNeededBloodTypes(List<String> neededBloodTypes) {
        this.neededBloodTypes = neededBloodTypes;
    }
    public String getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return "DonationSite{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", address=" + address +
                '}';
    }

}
