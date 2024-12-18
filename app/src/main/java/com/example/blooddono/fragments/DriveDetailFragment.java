package com.example.blooddono.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blooddono.R;
import com.example.blooddono.adapters.DriveDonationsAdapter;
import com.example.blooddono.models.Donation;
import com.example.blooddono.models.DonationDrive;
import com.example.blooddono.repositories.DonationDriveRepository;
import com.example.blooddono.repositories.DonationRepository;
import com.google.android.material.card.MaterialCardView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class DriveDetailFragment extends Fragment {
    private TextView driveNameText;
    private TextView driveDatesText;
    private TextView totalDonationsText;
    private TextView bloodTypeSummaryText;
    private MaterialCardView summaryCard;
    private RecyclerView donationsRecyclerView;
    private TextView emptyStateText;
    private Spinner sortSpinner;
    private Spinner bloodTypeFilterSpinner;
    private DonationDriveRepository driveRepository;
    private DonationRepository donationRepository;
    private DriveDonationsAdapter donationsAdapter;
    private List<Donation> allDonations = new ArrayList<>();
    private DonationDrive currentDrive;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_drive_detail, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize repositories
        driveRepository = new DonationDriveRepository();
        donationRepository = new DonationRepository();

        // Initialize views
        driveNameText = view.findViewById(R.id.driveNameText);
        driveDatesText = view.findViewById(R.id.driveDatesText);
        totalDonationsText = view.findViewById(R.id.totalDonationsText);
        bloodTypeSummaryText = view.findViewById(R.id.bloodTypeSummaryText);
        summaryCard = view.findViewById(R.id.summaryCard);
        donationsRecyclerView = view.findViewById(R.id.donationsRecyclerView);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        sortSpinner = view.findViewById(R.id.sortSpinner);
        bloodTypeFilterSpinner = view.findViewById(R.id.bloodTypeFilterSpinner);

        // Setup RecyclerView
        donationsAdapter = new DriveDonationsAdapter(requireContext());
        donationsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        donationsRecyclerView.setAdapter(donationsAdapter);

        // Setup spinners
        setupSpinners();

        // Get drive ID from arguments and load data
        String driveId = getArguments().getString("driveId");
        if (driveId != null) {
            loadDriveDetails(driveId);
            loadDriveDonations(driveId);
        }
    }

    private void setupSpinners() {
        // Setup sort options
        ArrayAdapter<CharSequence> sortAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.sort_options, android.R.layout.simple_spinner_item);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(sortAdapter);
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applySortAndFilter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Setup blood type filter options
        List<String> filterOptions = new ArrayList<>();
        filterOptions.add("All Blood Types");
        filterOptions.addAll(Arrays.asList("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"));
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, filterOptions);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bloodTypeFilterSpinner.setAdapter(filterAdapter);
        bloodTypeFilterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applySortAndFilter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadDriveDetails(String driveId) {
        driveRepository.getDriveById(driveId, new DonationDriveRepository.OnCompleteListener<DonationDrive>() {
            @Override
            public void onSuccess(DonationDrive drive) {
                currentDrive = drive;
                displayDriveSummary(drive);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(requireContext(),
                        "Error loading drive details: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadDriveDonations(String driveId) {
        donationRepository.getDonationsByDrive(driveId,
                new DonationRepository.OnCompleteListener<List<Donation>>() {
                    @Override
                    public void onSuccess(List<Donation> donations) {
                        allDonations = donations;
                        applySortAndFilter();
                        updateEmptyState();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(requireContext(),
                                "Error loading donations: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        updateEmptyState();
                    }
                });
    }

    private void displayDriveSummary(DonationDrive drive) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        driveNameText.setText(drive.getName());
        driveDatesText.setText(String.format("%s - %s",
                sdf.format(drive.getStartDate()),
                sdf.format(drive.getEndDate())));

        totalDonationsText.setText(String.format("Total Donations: %d",
                drive.getTotalDonations()));

        // Format blood type summary
        StringBuilder summary = new StringBuilder("Blood Collected:\n");
        for (Map.Entry<String, Double> entry : drive.getTotalCollectedAmounts().entrySet()) {
            summary.append(String.format("%s: %.1f mL\n",
                    entry.getKey(), entry.getValue()));
        }
        bloodTypeSummaryText.setText(summary.toString());
    }

    private void applySortAndFilter() {
        if (allDonations.isEmpty()) return;

        List<Donation> filteredDonations = new ArrayList<>(allDonations);

        // Apply blood type filter
        String selectedBloodType = bloodTypeFilterSpinner.getSelectedItem().toString();
        if (!"All Blood Types".equals(selectedBloodType)) {
            filteredDonations = filteredDonations.stream()
                    .filter(donation -> donation.getBloodTypes().contains(selectedBloodType))
                    .collect(Collectors.toList());
        }

        // Apply sorting
        String sortOption = sortSpinner.getSelectedItem().toString();
        switch (sortOption) {
            case "Date (Newest First)":
                Collections.sort(filteredDonations,
                        (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                break;
            case "Date (Oldest First)":
                Collections.sort(filteredDonations,
                        (a, b) -> Long.compare(a.getCreatedAt(), b.getCreatedAt()));
                break;
            case "Status":
                Collections.sort(filteredDonations,
                        (a, b) -> a.getStatus().compareTo(b.getStatus()));
                break;
            case "Donor Name":
                Collections.sort(filteredDonations,
                        (a, b) -> a.getDonorName().compareTo(b.getDonorName()));
                break;
        }

        donationsAdapter.setDonations(filteredDonations);
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (allDonations.isEmpty()) {
            emptyStateText.setText("No donations in this drive yet");
            emptyStateText.setVisibility(View.VISIBLE);
            donationsRecyclerView.setVisibility(View.GONE);
        } else {
            List<Donation> filteredDonations = donationsAdapter.getDonations();
            if (filteredDonations.isEmpty()) {
                emptyStateText.setText("No donations match the selected filters");
                emptyStateText.setVisibility(View.VISIBLE);
                donationsRecyclerView.setVisibility(View.GONE);
            } else {
                emptyStateText.setVisibility(View.GONE);
                donationsRecyclerView.setVisibility(View.VISIBLE);
            }
        }
    }
}