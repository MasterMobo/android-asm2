package com.example.blooddono.repositories;

import android.util.Log;

import com.example.blooddono.models.DayHours;
import com.example.blooddono.models.DonationSite;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DonationSiteRepository {
    private static final String COLLECTION_NAME = "donationSites";
    private final FirebaseFirestore db;

    // Interface for handling callbacks
    public interface OnCompleteListener<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    public DonationSiteRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Add a new donation site to Firestore
     */
    public void addDonationSite(DonationSite site, OnCompleteListener<String> listener) {
        // Add owner ID before saving
        site.setOwnerId(FirebaseAuth.getInstance().getCurrentUser().getUid());

        Map<String, Object> siteData = new HashMap<>();
        siteData.put("ownerId", site.getOwnerId());
        siteData.put("name", site.getName());
        siteData.put("description", site.getDescription());
        siteData.put("latitude", site.getLatitude());
        siteData.put("longitude", site.getLongitude());
        siteData.put("address", site.getAddress());
        siteData.put("type", site.getType());
        siteData.put("hoursType", site.getHoursType());
        siteData.put("neededBloodTypes", site.getNeededBloodTypes());
        siteData.put("createdAt", FieldValue.serverTimestamp());

        // Only add dates if it's a limited time site
        if (DonationSite.TYPE_LIMITED.equals(site.getType())) {
            siteData.put("startDate", site.getStartDate());
            siteData.put("endDate", site.getEndDate());
        }

        if (DonationSite.HOURS_SPECIFIC.equals(site.getHoursType())) {
            siteData.put("operatingHours", site.getOperatingHours());
        }

        db.collection(COLLECTION_NAME)
                .add(siteData)
                .addOnSuccessListener(documentReference -> listener.onSuccess(documentReference.getId()))
                .addOnFailureListener(listener::onError);
    }

    /**
     * Get a donation site by ID
     */
    public void getDonationSite(String siteId, OnCompleteListener<DonationSite> listener) {
        db.collection(COLLECTION_NAME)
                .document(siteId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        DonationSite site = documentToDonationSite(document);
                        listener.onSuccess(site);
                    } else {
                        listener.onError(new Exception("Site not found"));
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    /**
     * Get all active donation sites
     * For limited time sites, only return those within their date range
     */
    public void getAllDonationSites(OnCompleteListener<List<DonationSite>> listener) {
        long currentTime = System.currentTimeMillis();

        db.collection(COLLECTION_NAME)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DonationSite> sites = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        DonationSite site = documentToDonationSite(document);
                        sites.add(site);
                    }
                    listener.onSuccess(sites);
                })
                .addOnFailureListener(listener::onError);
    }

    /**
     * Delete a donation site
     */
    public void deleteDonationSite(String siteId, OnCompleteListener<Void> listener) {
        db.collection(COLLECTION_NAME)
                .document(siteId)
                .delete()
                .addOnSuccessListener(aVoid -> listener.onSuccess(null))
                .addOnFailureListener(listener::onError);
    }

    /**
     * Update a donation site
     */
    public void updateDonationSite(String siteId, DonationSite site, OnCompleteListener<Void> listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", site.getName());
        updates.put("description", site.getDescription());
        updates.put("latitude", site.getLatitude());
        updates.put("longitude", site.getLongitude());
        updates.put("address", site.getAddress());
        updates.put("type", site.getType());
        updates.put("hoursType", site.getHoursType());
        updates.put("updatedAt", FieldValue.serverTimestamp());

        // Only update dates if it's a limited time site
        if (DonationSite.TYPE_LIMITED.equals(site.getType())) {
            updates.put("startDate", site.getStartDate());
            updates.put("endDate", site.getEndDate());
        } else {
            // Remove date fields if switching to permanent type
            updates.put("startDate", FieldValue.delete());
            updates.put("endDate", FieldValue.delete());
        }

        if (DonationSite.HOURS_SPECIFIC.equals(site.getHoursType())) {
            updates.put("operatingHours", site.getOperatingHours());
        } else {
            updates.put("operatingHours", FieldValue.delete());
        }

        db.collection(COLLECTION_NAME)
                .document(siteId)
                .update(updates)
                .addOnSuccessListener(aVoid -> listener.onSuccess(null))
                .addOnFailureListener(listener::onError);
    }

    /**
     * Get all donation sites owned by a user, including inactive ones
     */
    public void getDonationSitesByOwner(String ownerId, OnCompleteListener<List<DonationSite>> listener) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("ownerId", ownerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DonationSite> sites = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        DonationSite site = documentToDonationSite(document);
                        site.setId(document.getId());
                        sites.add(site);
                    }
                    listener.onSuccess(sites);
                })
                .addOnFailureListener(listener::onError);
    }

    public void addVolunteer(String siteId, String volunteerId, OnCompleteListener<Void> listener) {
        db.collection(COLLECTION_NAME)
                .document(siteId)
                .update("volunteerIds", FieldValue.arrayUnion(volunteerId))
                .addOnSuccessListener(aVoid -> listener.onSuccess(null))
                .addOnFailureListener(listener::onError);
    }

    private DonationSite documentToDonationSite(DocumentSnapshot document) {
        String type = document.getString("type");
        if (type == null) {
            type = DonationSite.TYPE_PERMANENT; // Default for backward compatibility
        }

        String hoursType = document.getString("hoursType");
        if (hoursType == null) {
            hoursType = DonationSite.HOURS_24_7; // Default for backward compatibility
        }

        // Convert Firestore Map to operating hours Map
        Map<String, DayHours> operatingHours = new HashMap<>();
        Map<String, Object> hoursData = (Map<String, Object>) document.get("operatingHours");
        if (hoursData != null) {
            for (Map.Entry<String, Object> entry : hoursData.entrySet()) {
                Map<String, Object> dayData = (Map<String, Object>) entry.getValue();
                DayHours dayHours = new DayHours(
                        (boolean) dayData.get("open"),
                        (String) dayData.get("openTime"),
                        (String) dayData.get("closeTime")
                );
                operatingHours.put(entry.getKey(), dayHours);
            }
        }

        // Get blood types
        List<String> neededBloodTypes = new ArrayList<>();
        Object bloodTypesObj = document.get("neededBloodTypes");
        if (bloodTypesObj instanceof List) {
            neededBloodTypes = (List<String>) bloodTypesObj;
        }

        // Get volunteer IDs
        List<String> volunteerIds = new ArrayList<>();
        Object volunteerIdsObj = document.get("volunteerIds");
        if (volunteerIdsObj instanceof List) {
            volunteerIds = (List<String>) volunteerIdsObj;
        }

        DonationSite site = new DonationSite(
                document.getString("name"),
                document.getString("description"),
                document.getDouble("latitude"),
                document.getDouble("longitude"),
                document.getString("address"),
                type,
                document.getLong("startDate"),
                document.getLong("endDate"),
                hoursType,
                operatingHours,
                neededBloodTypes
        );
        site.setOwnerId(document.getString("ownerId"));
        site.setId(document.getId());
        site.setVolunteerIds(volunteerIds);
        return site;
    }
}