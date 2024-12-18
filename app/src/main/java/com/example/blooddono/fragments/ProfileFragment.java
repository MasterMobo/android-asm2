package com.example.blooddono.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.blooddono.R;
import com.example.blooddono.activities.LoginActivity;
import com.example.blooddono.models.User;
import com.example.blooddono.repositories.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {
    private TextView emailText;
    private TextView fullNameText;
    private Button logoutButton;
    private FirebaseAuth mAuth;
    private UserRepository userRepository;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase Auth and repository
        mAuth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();

        // Initialize views
        emailText = view.findViewById(R.id.emailText);
        fullNameText = view.findViewById(R.id.fullNameText);
        logoutButton = view.findViewById(R.id.logoutButton);

        // Set email text and load user data
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            emailText.setText(currentUser.getEmail());
            loadUserData(currentUser.getUid());
        }

        // Set logout button click listener
        logoutButton.setOnClickListener(v -> handleLogout());
    }

    private void loadUserData(String uid) {
        userRepository.getUser(uid, new UserRepository.OnCompleteListener<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null && isAdded()) {  // Check if fragment is still attached
                    fullNameText.setText(user.getFullName());
                }
            }

            @Override
            public void onError(Exception e) {
                if (isAdded()) {  // Check if fragment is still attached
                    Toast.makeText(requireContext(),
                            "Error loading user data: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void handleLogout() {
        mAuth.signOut();

        // Navigate to login activity
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}