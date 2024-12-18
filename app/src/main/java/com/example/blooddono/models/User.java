package com.example.blooddono.models;

public class User {
    public static final String ROLE_DONOR = "donor";
    public static final String ROLE_SITE_MANAGER = "site_manager";

    private String uid;
    private String email;
    private String fullName;
    private String role;

    public User() {
        // Required empty constructor for Firebase
    }

    public User(String uid, String email, String fullName, String role) {
        this.uid = uid;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }

    // Getters and setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
