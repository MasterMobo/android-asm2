package com.example.blooddono.repositories;

import com.example.blooddono.models.Donation;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;

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
                .orderBy("createdAt", Query.Direction.DESCENDING)
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

    public void getDonationsByDonor(String donorId, OnCompleteListener<List<Donation>> listener) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("donorId", donorId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
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

    public void updateDonationStatus(String donationId, String status, OnCompleteListener<Void> listener) {
        db.collection(COLLECTION_NAME)
                .document(donationId)
                .update(
                        "status", status,
                        "completedAt", status.equals(Donation.STATUS_COMPLETED) ? System.currentTimeMillis() : null
                )
                .addOnSuccessListener(aVoid -> listener.onSuccess(null))
                .addOnFailureListener(listener::onError);
    }

    public interface OnCompleteListener<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }
}