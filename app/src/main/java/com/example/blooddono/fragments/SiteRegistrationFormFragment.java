package com.example.blooddono.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.blooddono.R;
import com.example.blooddono.activities.LocationPickerActivity;
import com.example.blooddono.models.DonationSite;
import com.example.blooddono.repositories.DonationSiteRepository;
import com.example.blooddono.views.LocationPreviewView;
import com.google.android.gms.maps.model.LatLng;

public class SiteRegistrationFormFragment extends Fragment {
    private static final int PICK_LOCATION_REQUEST = 1;
    private EditText nameInput;
    private EditText descriptionInput;
    private LocationPreviewView locationPreview;
    private Button selectLocationButton;
    private Button submitButton;
    private DonationSiteRepository repository;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_site_registration_form, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize repository
        repository = new DonationSiteRepository();

        // Initialize views
        nameInput = view.findViewById(R.id.nameInput);
        descriptionInput = view.findViewById(R.id.descriptionInput);
        locationPreview = view.findViewById(R.id.locationPreview);
        selectLocationButton = view.findViewById(R.id.selectLocationButton);
        submitButton = view.findViewById(R.id.submitButton);

        selectLocationButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), LocationPickerActivity.class);
            LatLng currentLocation = locationPreview.getLocation();
            if (currentLocation != null) {
                intent.putExtra("latitude", currentLocation.latitude);
                intent.putExtra("longitude", currentLocation.longitude);
            }
            startActivityForResult(intent, PICK_LOCATION_REQUEST);
        });

        submitButton.setOnClickListener(v -> handleSubmit());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_LOCATION_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            double latitude = data.getDoubleExtra("latitude", 0);
            double longitude = data.getDoubleExtra("longitude", 0);
            locationPreview.setLocation(new LatLng(latitude, longitude));
        }
    }

    private void handleSubmit() {
        // Validate inputs
        String name = nameInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        LatLng location = locationPreview.getLocation();
        String address = locationPreview.getAddress();

        if (name.isEmpty()) {
            nameInput.setError("Name is required");
            return;
        }

        if (description.isEmpty()) {
            descriptionInput.setError("Description is required");
            return;
        }

        if (location == null) {
            Toast.makeText(requireContext(), "Please select a location", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create donation site object
        DonationSite site = new DonationSite(
                name,
                description,
                location.latitude,
                location.longitude,
                address
        );

        // Show loading dialog
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Saving donation site...");
        progressDialog.show();

        // Save to database
        repository.addDonationSite(site, new DonationSiteRepository.OnCompleteListener<String>() {
            @Override
            public void onSuccess(String documentId) {
                progressDialog.dismiss();
                Log.d("DonationSite", "New site registered with ID: " + documentId);

                // Show success alert and navigate back to map
                new AlertDialog.Builder(requireContext())
                        .setTitle("Success")
                        .setMessage("Blood donation site registered successfully!")
                        .setPositiveButton("OK", (dialog, which) -> {
                            // Clear form
                            clearForm();
                            // Navigate to map
                            NavController navController = Navigation.findNavController(requireView());
                            navController.navigate(R.id.sitesMapFragment);
                        })
                        .show();
            }

            @Override
            public void onError(Exception e) {
                progressDialog.dismiss();

                // Show error alert
                new AlertDialog.Builder(requireContext())
                        .setTitle("Error")
                        .setMessage("Failed to register donation site: " + e.getMessage())
                        .setPositiveButton("OK", null)
                        .show();

                Log.e("DonationSite", "Error adding document", e);
            }
        });
    }

    private void clearForm() {
        nameInput.setText("");
        descriptionInput.setText("");
        locationPreview.setLocation(null);
    }
}