package com.example.blooddono.repositories;

import android.util.Log;

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
        siteData.put("createdAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_NAME)
                .add(siteData)
                .addOnSuccessListener(documentReference -> {
                    listener.onSuccess(documentReference.getId());
                })
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
     * Get all donation sites
     */
    public void getAllDonationSites(OnCompleteListener<List<DonationSite>> listener) {
        db.collection(COLLECTION_NAME)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DonationSite> sites = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        sites.add(documentToDonationSite(document));
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
        updates.put("updatedAt", FieldValue.serverTimestamp());

        db.collection(COLLECTION_NAME)
                .document(siteId)
                .update(updates)
                .addOnSuccessListener(aVoid -> listener.onSuccess(null))
                .addOnFailureListener(listener::onError);
    }

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
                .addOnFailureListener(e -> {
                    listener.onError(e);
                });
    }


    private DonationSite documentToDonationSite(DocumentSnapshot document) {
        DonationSite site = new DonationSite(
                document.getString("name"),
                document.getString("description"),
                document.getDouble("latitude"),
                document.getDouble("longitude"),
                document.getString("address")
        );
        site.setOwnerId(document.getString("ownerId"));
        return site;
    }
}