package com.example.blooddono.views;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentActivity;

import com.example.blooddono.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationPreviewView extends FrameLayout implements OnMapReadyCallback {
    private GoogleMap previewMap;
    private Marker previewMarker;
    private TextView locationStatusText;
    private TextView addressText;
    private CardView mapCard;
    private LatLng selectedLocation;
    private String selectedAddress;
    private Geocoder geocoder;

    public LocationPreviewView(Context context) {
        super(context);
        init();
    }

    public LocationPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Initialize geocoder
        geocoder = new Geocoder(getContext(), Locale.getDefault());

        // Inflate the layout
        LayoutInflater.from(getContext()).inflate(R.layout.view_location_preview, this, true);

        locationStatusText = findViewById(R.id.locationStatusText);
        addressText = findViewById(R.id.addressText);
        mapCard = findViewById(R.id.previewMapCard);

        // Initialize map
        SupportMapFragment mapFragment = new SupportMapFragment();
        ((FragmentActivity) getContext()).getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.mapContainer, mapFragment)
                .commit();
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        previewMap = googleMap;
        previewMap.getUiSettings().setAllGesturesEnabled(false);
        previewMap.getUiSettings().setMapToolbarEnabled(false);

        if (selectedLocation != null) {
            updatePreviewMap();
        }
    }

    public void setLocation(LatLng location) {
        selectedLocation = location;
        updateLocationStatus();
        updatePreviewMap();
        updateAddress();
    }

    public LatLng getLocation() {
        return selectedLocation;
    }

    public String getAddress() {
        return selectedAddress;
    }

    private void updatePreviewMap() {
        if (previewMap != null && selectedLocation != null) {
            if (previewMarker != null) {
                previewMarker.remove();
            }
            previewMarker = previewMap.addMarker(new MarkerOptions()
                    .position(selectedLocation));
            previewMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 15));
        }
    }

    private void updateLocationStatus() {
        if (selectedLocation == null) {
            locationStatusText.setText("Location not selected");
            locationStatusText.setVisibility(View.VISIBLE);
            addressText.setVisibility(View.GONE);
            mapCard.setVisibility(View.GONE);
        } else {
            locationStatusText.setVisibility(View.GONE);
            addressText.setVisibility(View.VISIBLE);
            mapCard.setVisibility(View.VISIBLE);
        }
    }

    private void updateAddress() {
        if (selectedLocation == null) {
            addressText.setText("");
            return;
        }

        // Show loading state
        addressText.setText("Loading address...");

        // Run geocoding in background thread
        new Thread(() -> {
            try {
                List<Address> addresses = geocoder.getFromLocation(
                        selectedLocation.latitude,
                        selectedLocation.longitude,
                        1
                );

                // Update UI on main thread
                post(() -> {
                    if (!addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        StringBuilder addressStr = new StringBuilder();

                        // Build address string
                        for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                            if (i > 0) addressStr.append(", ");
                            addressStr.append(address.getAddressLine(i));
                        }

                        selectedAddress = addressStr.toString();
                        addressText.setText(selectedAddress);

                    } else {
                        addressText.setText("Address not found");
                    }
                });
            } catch (IOException e) {
                post(() -> addressText.setText("Could not get address"));
            }
        }).start();
    }

}