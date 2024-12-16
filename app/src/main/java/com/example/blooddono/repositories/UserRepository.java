package com.example.blooddono.repositories;

import com.example.blooddono.models.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

public class UserRepository {
    private static final String COLLECTION_NAME = "users";
    private final FirebaseFirestore db;

    public interface OnCompleteListener<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    public UserRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void createUser(User user, OnCompleteListener<Void> listener) {
        db.collection(COLLECTION_NAME)
                .document(user.getUid())
                .set(user)
                .addOnSuccessListener(aVoid -> listener.onSuccess(null))
                .addOnFailureListener(listener::onError);
    }

    public void getUser(String uid, OnCompleteListener<User> listener) {
        db.collection(COLLECTION_NAME)
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        listener.onSuccess(user);
                    } else {
                        listener.onError(new Exception("User not found"));
                    }
                })
                .addOnFailureListener(listener::onError);
    }
}