package com.example.blooddono.repositories;

import android.util.Log;

import com.example.blooddono.models.Donation;
import com.example.blooddono.models.DonationDrive;
import com.example.blooddono.models.DonationSite;
import com.example.blooddono.utils.NotificationUtils;
import com.google.firebase.firestore.FieldValue;
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
        // Get the current user's site manager
        String siteId = donation.getSiteId();
        DonationSiteRepository siteRepo = new DonationSiteRepository();

        siteRepo.getDonationSite(siteId, new DonationSiteRepository.OnCompleteListener<DonationSite>() {
            @Override
            public void onSuccess(DonationSite site) {
                // Ensure an active drive exists for this site's owner
                String siteOwnerId = site.getOwnerId();
                driveRepository.ensureActiveDriveExists(siteOwnerId,
                        new DonationDriveRepository.OnCompleteListener<DonationDrive>() {
                            @Override
                            public void onSuccess(DonationDrive drive) {
                                // Use a transaction to create donation and update drive statistics
                                db.runTransaction(transaction -> {
                                    // Create donation document
                                    var donationRef = db.collection(COLLECTION_NAME).document();
                                    donation.setId(donationRef.getId());
                                    donation.setDriveId(drive.getId());

                                    // Get drive reference
                                    var driveRef = db.collection("donationDrives").document(drive.getId());

                                    // Write donation
                                    transaction.set(donationRef, donation);

                                    // Increment total donations in drive
                                    transaction.update(driveRef, "totalDonations", FieldValue.increment(1));

                                    // Send notification after successful write
                                    NotificationUtils.sendDonationNotification(db.getApp().getApplicationContext(), donation);

                                    return donationRef.getId();
                                }).addOnSuccessListener(donationId -> {
                                    listener.onSuccess(donationId.toString());
                                }).addOnFailureListener(listener::onError);
                            }

                            @Override
                            public void onError(Exception e) {
                                listener.onError(e);
                            }
                        });
            }

            @Override
            public void onError(Exception e) {
                listener.onError(e);
            }
        });
    }    public void getDonationsByDrive(String driveId, OnCompleteListener<List<Donation>> listener) {
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
                        Log.e("DonationRepository", "Donation not associated with a drive");
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

                    // Update drive totals only for blood collection amounts
                    if (status.equals(Donation.STATUS_COMPLETED)) {
                        Map<String, Double> currentTotals =
                                (Map<String, Double>) driveDoc.get("totalCollectedAmounts");
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

                        transaction.update(driveRef, "totalCollectedAmounts", currentTotals);
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