package com.example.blooddono.models;

public class User {
    public static final String ROLE_DONOR = "donor";
    public static final String ROLE_SITE_MANAGER = "site_manager";

    private String uid;
    private String email;
    private String role;

    public User() {
        // Required empty constructor for Firebase
    }

    public User(String uid, String email, String role) {
        this.uid = uid;
        this.email = email;
        this.role = role;
    }

    // Getters and setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
