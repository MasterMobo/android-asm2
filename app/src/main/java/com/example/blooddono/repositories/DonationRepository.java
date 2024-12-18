package com.example.blooddono.repositories;

import android.util.Log;

import com.example.blooddono.models.Donation;
import com.example.blooddono.models.DonationDrive;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DonationRepository {
    private static final String COLLECTION_NAME = "donations";
    private final FirebaseFirestore db;
    private final DonationDriveRepository driveRepository;

    public DonationRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.driveRepository = new DonationDriveRepository();
    }

    public void createDonation(Donation donation, OnCompleteListener<String> listener) {
        // First ensure an active drive exists
        driveRepository.ensureActiveDriveExists(new DonationDriveRepository.OnCompleteListener<DonationDrive>() {
            @Override
            public void onSuccess(DonationDrive drive) {
                // Add drive ID to donation
                donation.setDriveId(drive.getId());

                // Create donation in Firestore
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

            @Override
            public void onError(Exception e) {
                listener.onError(e);
            }
        });
    }


    public void getDonationsByDrive(String driveId, OnCompleteListener<List<Donation>> listener) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("driveId", driveId)
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
        db.runTransaction((Transaction.Function<Void>) transaction -> {
                    // Get the donation
                    var donationRef = db.collection(COLLECTION_NAME).document(donationId);
                    var donationDoc = transaction.get(donationRef);

                    String driveId = donationDoc.getString("driveId");
                    if (driveId == null) {
                        Log.e("DonationRepository","Donation not associated with a drive");
                        return null;
                    }

                    // Get the drive
                    var driveRef = db.collection("donationDrives").document(driveId);
                    var driveDoc = transaction.get(driveRef);

                    // Update donation
                    Map<String, Object> donationUpdates = new HashMap<>();
                    donationUpdates.put("status", status);
                    donationUpdates.put("collectedAmounts", collectedAmounts);
                    if (status.equals(Donation.STATUS_COMPLETED)) {
                        donationUpdates.put("completedAt", System.currentTimeMillis());
                    }
                    transaction.update(donationRef, donationUpdates);

                    // Update drive totals
                    if (status.equals(Donation.STATUS_COMPLETED)) {
                        Map<String, Double> currentTotals = (Map<String, Double>) driveDoc.get("totalCollectedAmounts");
                        if (currentTotals == null) {
                            currentTotals = new HashMap<>();
                        }

                        // Update totals for each blood type
                        for (Map.Entry<String, Double> entry : collectedAmounts.entrySet()) {
                            String bloodType = entry.getKey();
                            Double amount = entry.getValue();
                            Double currentAmount = currentTotals.getOrDefault(bloodType, 0.0);
                            currentTotals.put(bloodType, currentAmount + amount);
                        }

                        Map<String, Object> driveUpdates = new HashMap<>();
                        driveUpdates.put("totalCollectedAmounts", currentTotals);
                        driveUpdates.put("totalDonations",
                                (driveDoc.getLong("totalDonations") != null ?
                                        driveDoc.getLong("totalDonations") : 0) + 1);
                        transaction.update(driveRef, driveUpdates);
                    }

                    return null;
                }).addOnSuccessListener(aVoid -> listener.onSuccess(null))
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
                    // Sort the list in memory
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

    public interface OnCompleteListener<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }
}