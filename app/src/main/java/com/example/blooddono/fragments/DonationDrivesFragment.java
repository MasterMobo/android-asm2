package com.example.blooddono.fragments;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blooddono.R;
import com.example.blooddono.models.DonationDrive;
import com.example.blooddono.repositories.DonationDriveRepository;
import com.google.android.material.card.MaterialCardView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
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
    private RecyclerView pastDrivesRecyclerView;
    private PastDrivesAdapter pastDrivesAdapter;
    private Button completeCurrentDriveButton;

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
        completeCurrentDriveButton = view.findViewById(R.id.completeCurrentDriveButton);
        completeCurrentDriveButton.setOnClickListener(v -> handleCompleteCurrentDrive());

        repository = new DonationDriveRepository();

        setupPastDrivesList(view);
        loadCurrentDrive();

        currentDriveCard.setOnClickListener(v -> {
            if (currentDrive != null) {
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

    private void setupPastDrivesList(View view) {
        pastDrivesRecyclerView = view.findViewById(R.id.pastDrivesRecyclerView);
        pastDrivesAdapter = new PastDrivesAdapter();
        pastDrivesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        pastDrivesRecyclerView.setAdapter(pastDrivesAdapter);
        loadPastDrives();
    }

    private void loadPastDrives() {
        repository.getPastDrives(new DonationDriveRepository.OnCompleteListener<List<DonationDrive>>() {
            @Override
            public void onSuccess(List<DonationDrive> drives) {
                pastDrivesAdapter.setDrives(drives);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(requireContext(),
                        "Error loading past drives: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleCompleteCurrentDrive() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Complete Drive")
                .setMessage("Are you sure you want to complete the current drive?\n\n" +
                        "This will:\n" +
                        "• Complete all pending donations with 0 mL collected\n" +
                        "• Create a new drive automatically\n\n" +
                        "This action cannot be undone.")
                .setPositiveButton("Complete", (dialog, which) -> {
                    ProgressDialog progressDialog = new ProgressDialog(requireContext());
                    progressDialog.setMessage("Completing drive...");
                    progressDialog.show();

                    repository.completeDrive(currentDrive.getId(),
                            new DonationDriveRepository.OnCompleteListener<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    progressDialog.dismiss();
                                    Toast.makeText(requireContext(),
                                            "Drive completed successfully",
                                            Toast.LENGTH_SHORT).show();
                                    loadCurrentDrive();
                                    loadPastDrives();
                                }

                                @Override
                                public void onError(Exception e) {
                                    progressDialog.dismiss();
                                    Toast.makeText(requireContext(),
                                            "Error completing drive: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Past drives adapter inner class
    private class PastDrivesAdapter extends RecyclerView.Adapter<PastDrivesAdapter.ViewHolder> {
        private List<DonationDrive> drives = new ArrayList<>();

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_past_drive, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            DonationDrive drive = drives.get(position);
            holder.nameText.setText(drive.getName());

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String dateRange = String.format("%s - %s",
                    sdf.format(drive.getStartDate()),
                    sdf.format(drive.getEndDate()));
            holder.datesText.setText(dateRange);

            holder.statsText.setText(String.format("Total Donations: %d\nTotal Sites: %d",
                    drive.getTotalDonations(), drive.getTotalSites()));

            // Handle click to view details
            holder.itemView.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("driveId", drive.getId());
                Navigation.findNavController(v)
                        .navigate(R.id.action_donationDrivesFragment_to_driveDetailFragment, args);
            });
        }

        @Override
        public int getItemCount() {
            return drives.size();
        }

        public void setDrives(List<DonationDrive> drives) {
            this.drives = drives;
            notifyDataSetChanged();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView nameText;
            final TextView datesText;
            final TextView statsText;

            ViewHolder(View view) {
                super(view);
                nameText = view.findViewById(R.id.driveNameText);
                datesText = view.findViewById(R.id.driveDatesText);
                statsText = view.findViewById(R.id.driveStatsText);
            }
        }
    }
}