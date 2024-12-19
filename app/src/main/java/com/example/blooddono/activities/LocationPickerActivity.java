package com.example.blooddono.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.blooddono.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class LocationPickerActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final float DEFAULT_ZOOM = 15f;

    private GoogleMap mMap;
    private Marker currentMarker;
    private Button confirmButton;
    private double initialLatitude;
    private double initialLongitude;
    private boolean hasInitialLocation;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private boolean locationPermissionGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_picker);

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize permission launcher
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    locationPermissionGranted = isGranted;
                    if (isGranted && mMap != null) {
                        getUserLocation();
                    }
                }
        );

        // Get initial location if it exists
        hasInitialLocation = getIntent().hasExtra("latitude") && getIntent().hasExtra("longitude");
        if (hasInitialLocation) {
            initialLatitude = getIntent().getDoubleExtra("latitude", 0);
            initialLongitude = getIntent().getDoubleExtra("longitude", 0);
        }

        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        confirmButton = findViewById(R.id.confirmButton);
        confirmButton.setEnabled(false); // Will be enabled when a location is selected

        confirmButton.setOnClickListener(v -> {
            if (currentMarker != null) {
                LatLng position = currentMarker.getPosition();
                Intent resultIntent = new Intent();
                resultIntent.putExtra("latitude", position.latitude);
                resultIntent.putExtra("longitude", position.longitude);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });

        // Check location permission
        checkLocationPermission();
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void getUserLocation() {
        if (!locationPermissionGranted) {
            return;
        }

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null && !hasInitialLocation) {
                LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                if (currentMarker != null) {
                    currentMarker.remove();
                }
                currentMarker = mMap.addMarker(new MarkerOptions()
                        .position(userLatLng)
                        .title("Selected Location"));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, DEFAULT_ZOOM));
                confirmButton.setEnabled(true);
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Set initial camera position and marker
        LatLng initialPosition;
        if (hasInitialLocation) {
            initialPosition = new LatLng(initialLatitude, initialLongitude);
            currentMarker = mMap.addMarker(new MarkerOptions()
                    .position(initialPosition)
                    .title("Selected Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialPosition, DEFAULT_ZOOM));
            confirmButton.setEnabled(true);
        } else {
            // Try to get user's location
            getUserLocation();
        }

        // Enable zoom controls
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Set up map click listener
        mMap.setOnMapClickListener(latLng -> {
            if (currentMarker != null) {
                currentMarker.remove();
            }
            currentMarker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Selected Location"));
            confirmButton.setEnabled(true);
        });
    }
}