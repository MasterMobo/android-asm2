package com.example.blooddono;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.blooddono.activities.LocationPickerActivity;
import com.example.blooddono.activities.LoginActivity;
import com.example.blooddono.models.DonationSite;
import com.example.blooddono.models.User;
import com.example.blooddono.repositories.DonationSiteRepository;
import com.example.blooddono.repositories.UserRepository;
import com.example.blooddono.views.LocationPreviewView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNav;
    private NavController navController;
    private FirebaseAuth mAuth;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase and repository
        mAuth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();

        // Initialize bottom navigation
        bottomNav = findViewById(R.id.bottom_navigation);

        // Setup navigation controller with nav graph
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        // Setup ActionBar with NavController
        NavigationUI.setupActionBarWithNavController(this, navController);

        // Connect bottom nav with nav controller
        NavigationUI.setupWithNavController(bottomNav, navController);

        // Check user role and setup appropriate navigation
        setupNavigationForUserRole();
    }

    private void setupNavigationForUserRole() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userRepository.getUser(currentUser.getUid(), new UserRepository.OnCompleteListener<User>() {
                @Override
                public void onSuccess(User user) {
                    // Set the appropriate menu based on user role
                    if (user.getRole().equals(User.ROLE_DONOR)) {
                        bottomNav.getMenu().clear();
                        bottomNav.inflateMenu(R.menu.donor_nav_menu);
                    } else {
                        bottomNav.getMenu().clear();
                        bottomNav.inflateMenu(R.menu.manager_nav_menu);
                    }

                    // Connect bottom nav with nav controller
                    NavigationUI.setupWithNavController(bottomNav, navController);
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(MainActivity.this,
                            "Error loading user profile", Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                    logout();
                }
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in
        if (mAuth.getCurrentUser() == null) {
            // Not signed in, return to login
            logout();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }

    private void logout() {
        // Not signed in, return to login
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}