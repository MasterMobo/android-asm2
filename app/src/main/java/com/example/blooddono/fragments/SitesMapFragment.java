package com.example.blooddono.fragments;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.blooddono.R;
import com.example.blooddono.models.DayHours;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SitesMapFragment extends Fragment implements OnMapReadyCallback {
    private static final float DEFAULT_ZOOM = 15f;

    private GoogleMap mMap;
    private DonationSiteRepository repository;
    private ProgressDialog loadingDialog;
    private Map<Marker, DonationSite> markerToSite = new HashMap<>();
    private FusedLocationProviderClient fusedLocationClient;
    private Marker userLocationMarker;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private boolean initialLocationSet = false;

    // UI elements for bottom sheet
    private View bottomSheet;
    private TextView siteNameText;
    private TextView addressText;
    private TextView descriptionText;
    private TextView availabilityText;
    private TextView hoursTypeText;
    private LinearLayout operatingHoursLayout;
    private ChipGroup bloodTypeChipGroup;
    private Button detailsButton;
    private Button directionsButton;
    private Polyline currentRoute;
    private DonationSite currentSite;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sites_map, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize bottom sheet views
        bottomSheet = view.findViewById(R.id.bottomSheet);
        siteNameText = bottomSheet.findViewById(R.id.siteNameText);
        addressText = bottomSheet.findViewById(R.id.addressText);
        descriptionText = bottomSheet.findViewById(R.id.descriptionText);
        availabilityText = bottomSheet.findViewById(R.id.availabilityText);
        hoursTypeText = bottomSheet.findViewById(R.id.hoursTypeText);
        operatingHoursLayout = bottomSheet.findViewById(R.id.operatingHoursLayout);
        bloodTypeChipGroup = bottomSheet.findViewById(R.id.bloodTypeChipGroup);
        detailsButton = bottomSheet.findViewById(R.id.detailsButton);
        directionsButton = bottomSheet.findViewById(R.id.directionsButton);
        directionsButton.setOnClickListener(v -> showDirections());

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

    private void showSiteDetails(DonationSite site) {
        // Update basic info
        siteNameText.setText(site.getName());
        addressText.setText(site.getAddress());
        descriptionText.setText(site.getDescription());

        // Display availability
        displayAvailability(site);

        // Display operating hours
        displayOperatingHours(site);

        // Display blood types
        displayBloodTypes(site);

        // Set up details button click listener
        detailsButton.setOnClickListener(v -> navigateToSiteDetails(site.getId()));

        // Show bottom sheet
        bottomSheet.setVisibility(View.VISIBLE);

        // Animate camera to the site
        LatLng position = new LatLng(site.getLatitude(), site.getLongitude());
        mMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                        position,
                        Math.max(mMap.getCameraPosition().zoom, DEFAULT_ZOOM)
                )
        );
    }

    private void showDirections() {
        if (currentSite == null) {
            Toast.makeText(requireContext(),
                    "No site selected", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
            if (location == null) {
                Toast.makeText(requireContext(),
                        "Unable to get current location", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show loading indicator
            ProgressDialog progressDialog = new ProgressDialog(requireContext());
            progressDialog.setMessage("Getting directions...");
            progressDialog.show();

            // Construct the URL for the Directions API
            String origin = location.getLatitude() + "," + location.getLongitude();
            String destination = currentSite.getLatitude() + "," + currentSite.getLongitude();
            String directionsUrl = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin=" + origin +
                    "&destination=" + destination +
                    "&key=" + getString(R.string.map_api_key);

            // Make HTTP request using OkHttp
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(directionsUrl)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    requireActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(requireContext(),
                                "Error getting directions: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        requireActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(requireContext(),
                                    "Error getting directions",
                                    Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }

                    try {
                        String jsonData = response.body().string();
                        JSONObject json = new JSONObject(jsonData);

                        // Parse the route
                        JSONArray routes = json.getJSONArray("routes");
                        if (routes.length() == 0) {
                            requireActivity().runOnUiThread(() -> {
                                progressDialog.dismiss();
                                Toast.makeText(requireContext(),
                                        "No route found",
                                        Toast.LENGTH_SHORT).show();
                            });
                            return;
                        }

                        JSONObject route = routes.getJSONObject(0);
                        JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                        String encodedPath = overviewPolyline.getString("points");

                        // Get route summary
                        JSONArray legs = route.getJSONArray("legs");
                        JSONObject leg = legs.getJSONObject(0);
                        String distance = leg.getJSONObject("distance").getString("text");
                        String duration = leg.getJSONObject("duration").getString("text");

                        // Decode the polyline and draw on map
                        requireActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();

                            // Remove previous route if exists
                            if (currentRoute != null) {
                                currentRoute.remove();
                            }

                            // Decode the polyline
                            List<LatLng> decodedPath = PolyUtil.decode(encodedPath);

                            // Draw the route
                            PolylineOptions polylineOptions = new PolylineOptions()
                                    .addAll(decodedPath)
                                    .width(10)
                                    .color(Color.BLUE);

                            currentRoute = mMap.addPolyline(polylineOptions);

                            // Show route info
                            String routeInfo = "Distance: " + distance + "\nDuration: " + duration;
                            Toast.makeText(requireContext(), routeInfo, Toast.LENGTH_LONG).show();

                            // Zoom map to show entire route
                            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
                            for (LatLng latLng : decodedPath) {
                                boundsBuilder.include(latLng);
                            }
                            LatLngBounds bounds = boundsBuilder.build();
                            int padding = 100;
                            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                            mMap.animateCamera(cu);
                        });

                    } catch (JSONException e) {
                        requireActivity().runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(requireContext(),
                                    "Error parsing directions data",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
        });
    }

    private void displayAvailability(DonationSite site) {
        if (DonationSite.TYPE_PERMANENT.equals(site.getType())) {
            availabilityText.setText("Permanently available");
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String startDate = sdf.format(new Date(site.getStartDate()));
            String endDate = sdf.format(new Date(site.getEndDate()));

            long currentTime = System.currentTimeMillis();
            boolean isActive = currentTime >= site.getStartDate() && currentTime <= site.getEndDate();

            String status = isActive ? "Active" : "Inactive";
            availabilityText.setText(String.format("Available from %s to %s\nStatus: %s",
                    startDate, endDate, status));
        }
    }

    private void displayOperatingHours(DonationSite site) {
        if (DonationSite.HOURS_24_7.equals(site.getHoursType())) {
            hoursTypeText.setText("Open 24 hours a day, 7 days a week");
            operatingHoursLayout.setVisibility(View.GONE);
        } else {
            hoursTypeText.setText("Operating hours by day:");
            operatingHoursLayout.setVisibility(View.VISIBLE);
            operatingHoursLayout.removeAllViews();

            String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday",
                    "Friday", "Saturday", "Sunday"};

            for (String day : days) {
                DayHours hours = site.getOperatingHours().get(day);
                if (hours != null && hours.isOpen()) {
                    View hourView = getLayoutInflater().inflate(
                            R.layout.operating_hours_detail_item,
                            operatingHoursLayout,
                            false
                    );

                    TextView dayText = hourView.findViewById(R.id.dayText);
                    TextView hoursText = hourView.findViewById(R.id.hoursText);

                    dayText.setText(day);
                    hoursText.setText(String.format("%s - %s",
                            formatTime(hours.getOpenTime()),
                            formatTime(hours.getCloseTime()))
                    );

                    operatingHoursLayout.addView(hourView);
                }
            }
        }
    }

    private void displayBloodTypes(DonationSite site) {
        bloodTypeChipGroup.removeAllViews();
        if (site.getNeededBloodTypes() != null && !site.getNeededBloodTypes().isEmpty()) {
            for (String bloodType : site.getNeededBloodTypes()) {
                Chip chip = new Chip(requireContext());
                chip.setText(bloodType);
                chip.setClickable(false);
                chip.setChipBackgroundColorResource(R.color.design_default_color_primary);
                chip.setTextColor(Color.WHITE);
                bloodTypeChipGroup.addView(chip);
            }
        }
    }

    private String formatTime(String time) {
        try {
            SimpleDateFormat input = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat output = new SimpleDateFormat("h:mm a", Locale.getDefault());
            Date date = input.parse(time);
            return date != null ? output.format(date) : time;
        } catch (ParseException e) {
            return time;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Set marker click listener
        mMap.setOnMapClickListener(latLng -> {
            // Hide bottom sheet and clear current site when clicking on map
            bottomSheet.setVisibility(View.GONE);
            currentSite = null;
            if (currentRoute != null) {
                currentRoute.remove();
                currentRoute = null;
            }
        });

        mMap.setOnMarkerClickListener(marker -> {
            if (!marker.equals(userLocationMarker)) {
                DonationSite site = markerToSite.get(marker);
                if (site != null) {
                    currentSite = site; // Store the selected site
                    showSiteDetails(site);
                }
            }
            return true;
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

                if (userLocationMarker != null) {
                    userLocationMarker.setPosition(userLatLng);
                } else {
                    userLocationMarker = mMap.addMarker(new MarkerOptions()
                            .position(userLatLng)
                            .title("Your Location")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                }

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
        markerToSite.clear();

        // Add markers for each site
        for (DonationSite site : sites) {
            LatLng position = new LatLng(site.getLatitude(), site.getLongitude());

            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(site.getName()));

            if (marker != null) {
                markerToSite.put(marker, site);
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


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up references
        currentSite = null;
        if (currentRoute != null) {
            currentRoute.remove();
            currentRoute = null;
        }
    }
}