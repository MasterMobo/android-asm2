package com.example.blooddono.fragments;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.blooddono.R;
import com.example.blooddono.activities.LocationPickerActivity;
import com.example.blooddono.adapters.OperatingHoursAdapter;
import com.example.blooddono.models.DayHours;
import com.example.blooddono.models.DonationSite;
import com.example.blooddono.repositories.DonationSiteRepository;
import com.example.blooddono.views.LocationPreviewView;
import com.google.android.gms.maps.model.LatLng;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SiteRegistrationFormFragment extends Fragment {
    private static final int PICK_LOCATION_REQUEST = 1;
    private EditText nameInput;
    private EditText descriptionInput;
    private LocationPreviewView locationPreview;
    private Button selectLocationButton;
    private Button submitButton;
    private DonationSiteRepository repository;
    private RadioGroup typeRadioGroup;
    private LinearLayout dateSelectionLayout;
    private Button startDateButton;
    private Button endDateButton;
    private TextView startDateText;
    private TextView endDateText;
    private long selectedStartDate = 0;
    private long selectedEndDate = 0;
    private RadioGroup hoursTypeRadioGroup;
    private RecyclerView operatingHoursRecyclerView;
    private OperatingHoursAdapter operatingHoursAdapter;
    private GridLayout bloodTypeGrid;
    private List<String> selectedBloodTypes = new ArrayList<>();

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
        typeRadioGroup = view.findViewById(R.id.typeRadioGroup);
        dateSelectionLayout = view.findViewById(R.id.dateSelectionLayout);
        startDateButton = view.findViewById(R.id.startDateButton);
        endDateButton = view.findViewById(R.id.endDateButton);
        startDateText = view.findViewById(R.id.startDateText);
        endDateText = view.findViewById(R.id.endDateText);
        hoursTypeRadioGroup = view.findViewById(R.id.hoursTypeRadioGroup);
        operatingHoursRecyclerView = view.findViewById(R.id.operatingHoursRecyclerView);
        bloodTypeGrid = view.findViewById(R.id.bloodTypeGrid);

        operatingHoursAdapter = new OperatingHoursAdapter(requireContext());

        operatingHoursRecyclerView.setAdapter(operatingHoursAdapter);
        operatingHoursRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()) {
            @Override
            public boolean canScrollVertically() {
                return true;
            }
        });
        selectLocationButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), LocationPickerActivity.class);
            LatLng currentLocation = locationPreview.getLocation();
            if (currentLocation != null) {
                intent.putExtra("latitude", currentLocation.latitude);
                intent.putExtra("longitude", currentLocation.longitude);
            }
            startActivityForResult(intent, PICK_LOCATION_REQUEST);
        });

        typeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.limitedRadio) {
                dateSelectionLayout.setVisibility(View.VISIBLE);
            } else {
                dateSelectionLayout.setVisibility(View.GONE);
            }
        });

        // Setup hours type radio group listener
        hoursTypeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            operatingHoursRecyclerView.setVisibility(
                    checkedId == R.id.hoursSpecificRadio ? View.VISIBLE : View.GONE
            );
        });

        submitButton.setOnClickListener(v -> handleSubmit());
        startDateButton.setOnClickListener(v -> showDatePicker(true));
        endDateButton.setOnClickListener(v -> showDatePicker(false));
        setupBloodTypeSelection();
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

    private void showDatePicker(boolean isStartDate) {
        Calendar calendar = Calendar.getInstance();
        if (isStartDate && selectedStartDate > 0) {
            calendar.setTimeInMillis(selectedStartDate);
        } else if (!isStartDate && selectedEndDate > 0) {
            calendar.setTimeInMillis(selectedEndDate);
        }

        DatePickerDialog picker = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCal = Calendar.getInstance();
                    selectedCal.set(year, month, dayOfMonth);

                    if (isStartDate) {
                        selectedStartDate = selectedCal.getTimeInMillis();
                        startDateText.setText(formatDate(selectedStartDate));
                    } else {
                        selectedEndDate = selectedCal.getTimeInMillis();
                        endDateText.setText(formatDate(selectedEndDate));
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Set min date for start date picker
        if (isStartDate) {
            picker.getDatePicker().setMinDate(System.currentTimeMillis());
        }
        // Set min date for end date picker based on selected start date
        else if (selectedStartDate > 0) {
            picker.getDatePicker().setMinDate(selectedStartDate);
        }

        picker.show();
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
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

        // Validate site type and dates
        int selectedTypeId = typeRadioGroup.getCheckedRadioButtonId();
        if (selectedTypeId == -1) {
            Toast.makeText(requireContext(), "Please select a site type", Toast.LENGTH_SHORT).show();
            return;
        }

        String type = selectedTypeId == R.id.permanentRadio ?
                DonationSite.TYPE_PERMANENT : DonationSite.TYPE_LIMITED;

        // Validate dates for limited type
        if (type.equals(DonationSite.TYPE_LIMITED)) {
            if (selectedStartDate == 0 || selectedEndDate == 0) {
                Toast.makeText(requireContext(), "Please select both start and end dates",
                        Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Validate operating hours
        int selectedHoursTypeId = hoursTypeRadioGroup.getCheckedRadioButtonId();
        if (selectedHoursTypeId == -1) {
            Toast.makeText(requireContext(), "Please select operating hours type",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String hoursType = selectedHoursTypeId == R.id.hours24Radio ?
                DonationSite.HOURS_24_7 : DonationSite.HOURS_SPECIFIC;

        // For specific hours, validate that at least one day is selected
        Map<String, DayHours> operatingHours = null;
        if (hoursType.equals(DonationSite.HOURS_SPECIFIC)) {
            operatingHours = operatingHoursAdapter.getOperatingHours();
            boolean hasOpenDay = false;
            for (DayHours hours : operatingHours.values()) {
                if (hours.isOpen()) {
                    hasOpenDay = true;
                    break;
                }
            }
            if (!hasOpenDay) {
                Toast.makeText(requireContext(),
                        "Please select at least one day of operation",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate time ranges
            for (Map.Entry<String, DayHours> entry : operatingHours.entrySet()) {
                DayHours hours = entry.getValue();
                if (hours.isOpen()) {
                    if (!isValidTimeRange(hours.getOpenTime(), hours.getCloseTime())) {
                        Toast.makeText(requireContext(),
                                "Invalid time range for " + entry.getKey(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }
        }

        // Validate blood types
        if (selectedBloodTypes.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Please select at least one blood type needed",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading dialog
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Saving donation site...");
        progressDialog.show();

        // Create donation site object
        DonationSite site = new DonationSite(
                nameInput.getText().toString().trim(),
                descriptionInput.getText().toString().trim(),
                locationPreview.getLocation().latitude,
                locationPreview.getLocation().longitude,
                locationPreview.getAddress(),
                type,
                type.equals(DonationSite.TYPE_LIMITED) ? selectedStartDate : null,
                type.equals(DonationSite.TYPE_LIMITED) ? selectedEndDate : null,
                hoursType,
                operatingHours,
                selectedBloodTypes
        );

        // Save to database
        repository.addDonationSite(site, new DonationSiteRepository.OnCompleteListener<String>() {
            @Override
            public void onSuccess(String documentId) {
                progressDialog.dismiss();
                showSuccessDialog();
            }

            @Override
            public void onError(Exception e) {
                progressDialog.dismiss();
                showErrorDialog(e.getMessage());
            }
        });
    }

    private void setupBloodTypeSelection() {
        for (String bloodType : DonationSite.BLOOD_TYPES) {
            CheckBox checkBox = new CheckBox(requireContext());
            checkBox.setText(bloodType);
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedBloodTypes.add(bloodType);
                } else {
                    selectedBloodTypes.remove(bloodType);
                }
            });
            bloodTypeGrid.addView(checkBox);
        }
    }


    private boolean isValidTimeRange(String openTime, String closeTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date openDate = sdf.parse(openTime);
            Date closeDate = sdf.parse(closeTime);

            // If close time is earlier than open time, consider it invalid
            return openDate != null && closeDate != null &&
                    (closeDate.after(openDate) || closeTime.equals("00:00"));
        } catch (ParseException e) {
            return false;
        }
    }

    private void showSuccessDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Success")
                .setMessage("Blood donation site registered successfully!")
                .setPositiveButton("OK", (dialog, which) -> {
                    // Clear form
                    clearForm();
                    // Navigate back to My Sites fragment
                    NavController navController = Navigation.findNavController(requireView());
                    navController.navigate(R.id.mySitesFragment);
                })
                .show();
    }

    private void showErrorDialog(String message) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Error")
                .setMessage("Failed to register donation site: " + message)
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up any time picker dialogs that might be showing
    }

    private void clearForm() {
        nameInput.setText("");
        descriptionInput.setText("");
        locationPreview.setLocation(null);
    }
}