package com.example.blooddono.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.blooddono.MainActivity;
import com.example.blooddono.R;
import com.example.blooddono.models.User;
import com.example.blooddono.repositories.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseUser;

import android.app.ProgressDialog;
import android.app.AlertDialog;
import android.util.Log;

public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "RegisterActivity";
    private EditText emailInput;
    private EditText fullNameInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private RadioGroup roleRadioGroup;
    private Button registerButton;
    private TextView loginLink;
    private FirebaseAuth mAuth;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase Auth and repository
        mAuth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();

        // Initialize views
        emailInput = findViewById(R.id.emailInput);
        fullNameInput = findViewById(R.id.fullNameInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        roleRadioGroup = findViewById(R.id.roleRadioGroup);
        registerButton = findViewById(R.id.registerButton);
        loginLink = findViewById(R.id.loginLink);

        // Set click listeners
        registerButton.setOnClickListener(v -> handleRegister());
        loginLink.setOnClickListener(v -> finish());
    }

    private void handleRegister() {
        String email = emailInput.getText().toString().trim();
        String fullName = fullNameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        // Get selected role
        int selectedRoleId = roleRadioGroup.getCheckedRadioButtonId();
        if (selectedRoleId == -1) {
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            return;
        }
        String role = selectedRoleId == R.id.donorRadio ? User.ROLE_DONOR : User.ROLE_SITE_MANAGER;

        // Validate inputs
        if (email.isEmpty()) {
            emailInput.setError("Email is required");
            return;
        }

        if (fullName.isEmpty()) {
            fullNameInput.setError("Full name is required");
            return;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Password is required");
            return;
        }

        if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Passwords don't match");
            return;
        }

        // Show loading dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating account...");
        progressDialog.show();

        // Create user with Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Create user in Firestore
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        User user = new User(firebaseUser.getUid(), email, fullName, role);

                        userRepository.createUser(user, new UserRepository.OnCompleteListener<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                progressDialog.dismiss();
                                new AlertDialog.Builder(RegisterActivity.this)
                                        .setTitle("Success")
                                        .setMessage("Account created successfully!")
                                        .setPositiveButton("OK", (dialog, which) -> {
                                            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                            finish();
                                        })
                                        .show();
                            }

                            @Override
                            public void onError(Exception e) {
                                progressDialog.dismiss();
                                new AlertDialog.Builder(RegisterActivity.this)
                                        .setTitle("Error")
                                        .setMessage("Failed to create user profile: " + e.getMessage())
                                        .setPositiveButton("OK", null)
                                        .show();
                            }
                        });
                    } else {
                        progressDialog.dismiss();
                        // Handle specific registration errors
                        String errorMessage;
                        if (task.getException() instanceof FirebaseAuthWeakPasswordException) {
                            errorMessage = "Password is too weak. Please choose a stronger password.";
                        } else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            errorMessage = "Invalid email format. Please enter a valid email address.";
                        } else if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            errorMessage = "An account already exists with this email address.";
                        } else if (task.getException() instanceof FirebaseNetworkException) {
                            errorMessage = "Network error. Please check your internet connection and try again.";
                        } else {
                            errorMessage = "Registration failed. Please try again later.";
                        }

                        new AlertDialog.Builder(RegisterActivity.this)
                                .setTitle("Registration Failed")
                                .setMessage(errorMessage)
                                .setPositiveButton("OK", null)
                                .show();
                    }
                });
    }
}