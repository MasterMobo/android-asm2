package com.example.blooddono.fragments;

import android.app.ProgressDialog;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.blooddono.R;
import com.example.blooddono.models.DonationSite;
import com.example.blooddono.repositories.DonationSiteRepository;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;

public class SitesMapFragment extends Fragment implements OnMapReadyCallback {
    private GoogleMap mMap;
    private DonationSiteRepository repository;
    private ProgressDialog loadingDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sites_map, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Initialize repository
        repository = new DonationSiteRepository();

        // Initialize loading dialog
        loadingDialog = new ProgressDialog(requireContext());
        loadingDialog.setMessage("Loading donation sites...");
        loadingDialog.setCancelable(false);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Load and display sites
        loadSites();
    }

    private void loadSites() {
        loadingDialog.show();

        repository.getAllDonationSites(new DonationSiteRepository.OnCompleteListener<List<DonationSite>>() {
            @Override
            public void onSuccess(List<DonationSite> sites) {
                loadingDialog.dismiss();
                displaySitesOnMap(sites);
            }

            @Override
            public void onError(Exception e) {
                loadingDialog.dismiss();
                Toast.makeText(requireContext(),
                        "Error loading sites: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void displaySitesOnMap(List<DonationSite> sites) {
        if (sites.isEmpty()) {
            Toast.makeText(requireContext(), "No donation sites found", Toast.LENGTH_SHORT).show();
            return;
        }

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        // Add markers for each site
        for (DonationSite site : sites) {
            LatLng position = new LatLng(site.getLatitude(), site.getLongitude());

            // Add marker
            mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(site.getName())
                    .snippet(site.getAddress()));

            // Include this position in bounds
            boundsBuilder.include(position);
        }

        // Calculate bounds with padding
        LatLngBounds bounds = boundsBuilder.build();
        int padding = 100; // pixels
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);

        // Move camera to show all markers
        try {
            mMap.animateCamera(cameraUpdate);
        } catch (Exception e) {
            // Handle edge case where bounds are invalid
            if (!sites.isEmpty()) {
                DonationSite firstSite = sites.get(0);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(firstSite.getLatitude(), firstSite.getLongitude()),
                        12
                ));
            }
        }
    }
}