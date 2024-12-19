package com.example.blooddono.repositories;

import com.example.blooddono.models.DonationDrive;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DonationDriveRepository {
    private static final String COLLECTION_NAME = "donationDrives";
    private final FirebaseFirestore db;

    public DonationDriveRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void getDriveById(String driveId, OnCompleteListener<DonationDrive> listener) {
        db.collection(COLLECTION_NAME)
                .document(driveId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        DonationDrive drive = documentSnapshot.toObject(DonationDrive.class);
                        if (drive != null) {
                            drive.setId(documentSnapshot.getId());
                        }
                        listener.onSuccess(drive);
                    } else {
                        listener.onError(new Exception("Drive not found"));
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    public void createDrive(DonationDrive drive, OnCompleteListener<String> listener) {
        // First deactivate any currently active drive
        deactivateCurrentDrive(new OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                // Then create the new drive
                db.collection(COLLECTION_NAME)
                        .add(drive)
                        .addOnSuccessListener(documentReference -> {
                            String driveId = documentReference.getId();
                            documentReference.update("id", driveId)
                                    .addOnSuccessListener(aVoid -> listener.onSuccess(driveId))
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

    private void deactivateCurrentDrive(OnCompleteListener<Void> listener) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        var batch = db.batch();
                        for (var doc : queryDocumentSnapshots) {
                            batch.update(doc.getReference(), "active", false);
                        }
                        batch.commit()
                                .addOnSuccessListener(aVoid -> listener.onSuccess(null))
                                .addOnFailureListener(listener::onError);
                    } else {
                        listener.onSuccess(null);
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    public void getCurrentDrive(OnCompleteListener<DonationDrive> listener) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DonationDrive drive = queryDocumentSnapshots.getDocuments()
                                .get(0).toObject(DonationDrive.class);
                        if (drive != null) {
                            drive.setId(queryDocumentSnapshots.getDocuments().get(0).getId());
                        }
                        listener.onSuccess(drive);
                    } else {
                        listener.onSuccess(null);
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    public void updateDriveStats(String driveId, Map<String, Double> newAmounts,
                                 OnCompleteListener<Void> listener) {
        db.collection(COLLECTION_NAME)
                .document(driveId)
                .update("totalCollectedAmounts", newAmounts,
                        "totalDonations", FieldValue.increment(1))
                .addOnSuccessListener(aVoid -> listener.onSuccess(null))
                .addOnFailureListener(listener::onError);
    }

    public void ensureActiveDriveExists(OnCompleteListener<DonationDrive> listener) {
        getCurrentDrive(new OnCompleteListener<DonationDrive>() {
            @Override
            public void onSuccess(DonationDrive drive) {
                if (drive == null) {
                    // Create a new drive if none exists
                    createDefaultDrive(listener);
                } else {
                    listener.onSuccess(drive);
                }
            }

            @Override
            public void onError(Exception e) {
                listener.onError(e);
            }
        });
    }

    private void createDefaultDrive(OnCompleteListener<DonationDrive> listener) {
        // Create a drive starting today and ending in 3 months
        Calendar startDate = Calendar.getInstance();
        Calendar endDate = Calendar.getInstance();
        endDate.add(Calendar.MONTH, 3);

        DonationDrive newDrive = new DonationDrive(
                "Blood Donation Drive " + new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(new Date()),
                startDate.getTimeInMillis(),
                endDate.getTimeInMillis()
        );

        createDrive(newDrive, new OnCompleteListener<String>() {
            @Override
            public void onSuccess(String driveId) {
                getDriveById(driveId, listener);
            }

            @Override
            public void onError(Exception e) {
                listener.onError(e);
            }
        });
    }

    public void completeDrive(String driveId, OnCompleteListener<Void> listener) {
        // First deactivate the current drive
        db.collection(COLLECTION_NAME)
                .document(driveId)
                .update("active", false,
                        "completedAt", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    // Then create a new drive
                    createDefaultDrive(new OnCompleteListener<DonationDrive>() {
                        @Override
                        public void onSuccess(DonationDrive drive) {
                            listener.onSuccess(null);
                        }

                        @Override
                        public void onError(Exception e) {
                            listener.onError(e);
                        }
                    });
                })
                .addOnFailureListener(listener::onError);
    }

    public void getPastDrives(OnCompleteListener<List<DonationDrive>> listener) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("active", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DonationDrive> drives = new ArrayList<>();
                    for (var doc : querySnapshot) {
                        DonationDrive drive = doc.toObject(DonationDrive.class);
                        drive.setId(doc.getId());
                        drives.add(drive);
                    }
                    listener.onSuccess(drives);
                })
                .addOnFailureListener(listener::onError);    }

    public interface OnCompleteListener<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }
}