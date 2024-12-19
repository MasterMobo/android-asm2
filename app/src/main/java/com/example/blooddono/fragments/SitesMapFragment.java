package com.example.blooddono.fragments;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.blooddono.R;
import com.example.blooddono.models.DonationSite;
import com.example.blooddono.repositories.DonationSiteRepository;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SitesMapFragment extends Fragment implements OnMapReadyCallback {
    private static final float DEFAULT_ZOOM = 15f;

    private GoogleMap mMap;
    private DonationSiteRepository repository;
    private ProgressDialog loadingDialog;
    private Map<Marker, String> markerToSiteId = new HashMap<>();
    private View currentInfoWindow;
    private FusedLocationProviderClient fusedLocationClient;
    private Marker userLocationMarker;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private boolean initialLocationSet = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sites_map, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Initialize permission launcher
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        enableMyLocation();
                    }
                }
        );

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

        // Set custom info window adapter
        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                // Skip info window for user location marker
                if (marker.equals(userLocationMarker)) {
                    return null;
                }

                View view = getLayoutInflater().inflate(R.layout.map_info_window, null);
                TextView titleText = view.findViewById(R.id.titleText);
                TextView snippetText = view.findViewById(R.id.snippetText);
                Button detailsButton = view.findViewById(R.id.detailsButton);

                titleText.setText(marker.getTitle());
                snippetText.setText(marker.getSnippet());

                currentInfoWindow = view;
                return view;
            }
        });

        mMap.setOnInfoWindowClickListener(marker -> {
            if (!marker.equals(userLocationMarker) && currentInfoWindow != null) {
                String siteId = markerToSiteId.get(marker);
                if (siteId != null) {
                    navigateToSiteDetails(siteId);
                }
            }
        });

        // Enable location layer and get user location
        enableMyLocation();

        // Load and display sites
        loadSites();
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            updateUserLocation();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void updateUserLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
            if (location != null) {
                LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                // Update or create user location marker
                if (userLocationMarker != null) {
                    userLocationMarker.setPosition(userLatLng);
                } else {
                    userLocationMarker = mMap.addMarker(new MarkerOptions()
                            .position(userLatLng)
                            .title("Your Location")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                }

                // Move camera to user location if it's the first time
                if (!initialLocationSet) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, DEFAULT_ZOOM));
                    initialLocationSet = true;
                }
            }
        });
    }

    private void navigateToSiteDetails(String siteId) {
        Bundle args = new Bundle();
        args.putString("siteId", siteId);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_sitesMapFragment_to_siteDetailFragment, args);
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
        markerToSiteId.clear();

        // Add markers for each site
        for (DonationSite site : sites) {
            LatLng position = new LatLng(site.getLatitude(), site.getLongitude());

            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(site.getName())
                    .snippet(site.getAddress()));

            if (marker != null) {
                markerToSiteId.put(marker, site.getId());
            }

            boundsBuilder.include(position);
        }

        // Only adjust bounds to show all sites if user location hasn't been set yet
        if (!initialLocationSet) {
            try {
                LatLngBounds bounds = boundsBuilder.build();
                int padding = 100;
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                mMap.animateCamera(cameraUpdate);
            } catch (Exception e) {
                if (!sites.isEmpty()) {
                    DonationSite firstSite = sites.get(0);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(firstSite.getLatitude(), firstSite.getLongitude()),
                            DEFAULT_ZOOM
                    ));
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mMap != null) {
            updateUserLocation();
        }
    }
}