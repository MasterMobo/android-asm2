package com.example.blooddono.models;

public class DonationSite {
    private String name;
    private String description;
    private double latitude;
    private double longitude;
    private String address;

    public DonationSite(String name, String description, double latitude, double longitude, String address) {
        this.name = name;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
    }

    // Getters
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
