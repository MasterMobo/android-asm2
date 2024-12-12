package com.example.blooddono.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.blooddono.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class LocationPickerActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private Marker currentMarker;
    private Button confirmButton;
    private double initialLatitude;
    private double initialLongitude;
    private boolean hasInitialLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_picker);

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
        confirmButton.setEnabled(hasInitialLocation); // Enable button if we have initial location

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
        } else {
            initialPosition = new LatLng(10.729313489742879, 106.69588800185203); // Default location (Sydney)
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialPosition, 15));

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
