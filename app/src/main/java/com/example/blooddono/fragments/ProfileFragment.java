package com.example.blooddono.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.blooddono.R;
import com.example.blooddono.activities.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {
    private TextView emailText;
    private Button logoutButton;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        emailText = view.findViewById(R.id.emailText);
        logoutButton = view.findViewById(R.id.logoutButton);

        // Set email text
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            emailText.setText(currentUser.getEmail());
        }

        // Set logout button click listener
        logoutButton.setOnClickListener(v -> handleLogout());
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