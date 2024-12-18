package com.example.blooddono.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.blooddono.R;
import com.example.blooddono.models.DonationDrive;
import com.example.blooddono.repositories.DonationDriveRepository;
import com.google.android.material.card.MaterialCardView;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

public class DonationDrivesFragment extends Fragment {
    private MaterialCardView currentDriveCard;
    private TextView driveNameText;
    private TextView driveDatesText;
    private TextView totalSitesText;
    private TextView totalDonationsText;
    private TextView bloodTypeSummaryText;
    private DonationDriveRepository repository;
    private DonationDrive currentDrive;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_donation_drives, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        currentDriveCard = view.findViewById(R.id.currentDriveCard);
        driveNameText = view.findViewById(R.id.driveNameText);
        driveDatesText = view.findViewById(R.id.driveDatesText);
        totalSitesText = view.findViewById(R.id.totalSitesText);
        totalDonationsText = view.findViewById(R.id.totalDonationsText);
        bloodTypeSummaryText = view.findViewById(R.id.bloodTypeSummaryText);

        repository = new DonationDriveRepository();

        loadCurrentDrive();

        currentDriveCard.setOnClickListener(v -> {
            if (currentDrive != null) { // Added null check
                Bundle args = new Bundle();
                args.putString("driveId", currentDrive.getId());
                Navigation.findNavController(v)
                        .navigate(R.id.action_donationDrivesFragment_to_driveDetailFragment, args);
            }
        });
    }

    private void loadCurrentDrive() {
        repository.getCurrentDrive(new DonationDriveRepository.OnCompleteListener<DonationDrive>() {
            @Override
            public void onSuccess(DonationDrive drive) {
                if (drive != null) {
                    currentDrive = drive; // Store the current drive
                    displayDrive(drive);
                } else {
                    showNoDriveState();
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(requireContext(),
                        "Error loading drive: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                showNoDriveState();
            }
        });
    }

    private void displayDrive(DonationDrive drive) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        driveNameText.setText(drive.getName());
        driveDatesText.setText(String.format("%s - %s",
                sdf.format(drive.getStartDate()),
                sdf.format(drive.getEndDate())));

        totalSitesText.setText(String.format("Total Sites: %d", drive.getTotalSites()));
        totalDonationsText.setText(String.format("Total Donations: %d", drive.getTotalDonations()));

        StringBuilder summary = new StringBuilder("Blood Collected:\n");
        for (Map.Entry<String, Double> entry : drive.getTotalCollectedAmounts().entrySet()) {
            summary.append(String.format("%s: %.1f mL\n", entry.getKey(), entry.getValue()));
        }
        bloodTypeSummaryText.setText(summary.toString());

        currentDriveCard.setVisibility(View.VISIBLE);
    }

    private void showNoDriveState() {
        // Show appropriate UI when no active drive exists
        currentDriveCard.setVisibility(View.GONE);
        // Optionally show a message or button to create a new drive
        Toast.makeText(requireContext(), "No active donation drive found", Toast.LENGTH_SHORT).show();
    }
}