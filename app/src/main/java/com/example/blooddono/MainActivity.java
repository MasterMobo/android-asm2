package com.example.blooddono;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.example.blooddono.activities.LoginActivity;
import com.example.blooddono.models.DonationDrive;
import com.example.blooddono.models.User;
import com.example.blooddono.repositories.DonationDriveRepository;
import com.example.blooddono.repositories.UserRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNav;
    private NavController navController;
    private FirebaseAuth mAuth;
    private UserRepository userRepository;
    private DonationDriveRepository driveRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize repositories
        mAuth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();
        driveRepository = new DonationDriveRepository();

        // Initialize bottom navigation
        bottomNav = findViewById(R.id.bottom_navigation);

        // Setup navigation controller with nav graph
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        // Setup ActionBar with NavController
        NavigationUI.setupActionBarWithNavController(this, navController);

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
                    switch (user.getRole()) {
                        case User.ROLE_DONOR:
                            bottomNav.getMenu().clear();
                            bottomNav.inflateMenu(R.menu.donor_nav_menu);
                            // Set start destination to Discover for donors
                            navController.getGraph().setStartDestination(R.id.discoverFragment);
                            break;

                        case User.ROLE_SITE_MANAGER:
                            bottomNav.getMenu().clear();
                            bottomNav.inflateMenu(R.menu.manager_nav_menu);
                            // Set start destination to My Sites for managers
                            navController.getGraph().setStartDestination(R.id.mySitesFragment);
                            // Ensure active drive exists for site manager
                            ensureActiveDriveForManager(user.getUid());
                            break;

                        case User.ROLE_SUPER_USER:
                            bottomNav.getMenu().clear();
                            bottomNav.inflateMenu(R.menu.super_user_nav_menu);
                            // Set start destination to Map for super users
                            navController.getGraph().setStartDestination(R.id.discoverFragment);
                            break;
                    }

                    // Connect bottom nav with nav controller
                    NavigationUI.setupWithNavController(bottomNav, navController);

                    // Navigate to start destination
                    navController.navigate(navController.getGraph().getStartDestination());
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

    private void ensureActiveDriveForManager(String managerId) {
        driveRepository.ensureActiveDriveExists(managerId,
                new DonationDriveRepository.OnCompleteListener<DonationDrive>() {
                    @Override
                    public void onSuccess(DonationDrive drive) {
                        // Drive exists or was created successfully
                        Log.d("MainActivity", "Active drive ensured: " + drive.getName());
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("MainActivity", "Error ensuring active drive: " + e.getMessage());
                    }
                });
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