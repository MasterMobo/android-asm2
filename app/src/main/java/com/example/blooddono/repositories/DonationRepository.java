package com.example.blooddono.repositories;

import com.example.blooddono.models.Donation;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DonationRepository {
    private static final String COLLECTION_NAME = "donations";
    private final FirebaseFirestore db;

    public DonationRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void createDonation(Donation donation, OnCompleteListener<String> listener) {
        db.collection(COLLECTION_NAME)
                .add(donation)
                .addOnSuccessListener(documentReference -> {
                    String donationId = documentReference.getId();
                    documentReference.update("id", donationId)
                            .addOnSuccessListener(aVoid -> listener.onSuccess(donationId))
                            .addOnFailureListener(listener::onError);
                })
                .addOnFailureListener(listener::onError);
    }

    public void getDonationsBySite(String siteId, OnCompleteListener<List<Donation>> listener) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("siteId", siteId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Donation> donations = new ArrayList<>();
                    for (var doc : queryDocumentSnapshots) {
                        Donation donation = doc.toObject(Donation.class);
                        donations.add(donation);
                    }
                    // Sort the list in memory instead
                    donations.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                    listener.onSuccess(donations);
                })
                .addOnFailureListener(listener::onError);
    }

    public void getDonationsByDonor(String donorId, OnCompleteListener<List<Donation>> listener) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("donorId", donorId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Donation> donations = new ArrayList<>();
                    for (var doc : queryDocumentSnapshots) {
                        Donation donation = doc.toObject(Donation.class);
                        donations.add(donation);
                    }
                    listener.onSuccess(donations);
                })
                .addOnFailureListener(listener::onError);
    }

    public void updateDonationStatus(String donationId, String status,
                                     Map<String, Double> collectedAmounts,
                                     OnCompleteListener<Void> listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("completedAt", status.equals(Donation.STATUS_COMPLETED) ?
                System.currentTimeMillis() : null);
        updates.put("collectedAmounts", collectedAmounts);

        db.collection(COLLECTION_NAME)
                .document(donationId)
                .update(updates)
                .addOnSuccessListener(aVoid -> listener.onSuccess(null))
                .addOnFailureListener(listener::onError);
    }

    public interface OnCompleteListener<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }
}