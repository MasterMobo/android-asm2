package com.example.blooddono.models;

public class DonationSite {
    public static final String TYPE_PERMANENT = "permanent";
    public static final String TYPE_LIMITED = "limited";

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

    public DonationSite() {
        // Required empty constructor for Firebase
    }

    public DonationSite(String name, String description, double latitude, double longitude, String address,
                        String type, Long startDate, Long endDate) {
        this.name = name;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.type = type;
        this.startDate = startDate != null ? startDate : 0;
        this.endDate = endDate != null ? endDate : 0;
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
