package com.example.blooddono.fragments;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blooddono.R;
import com.example.blooddono.models.Donation;
import com.example.blooddono.models.DonationDrive;
import com.example.blooddono.models.User;
import com.example.blooddono.repositories.DonationDriveRepository;
import com.example.blooddono.repositories.DonationRepository;
import com.example.blooddono.repositories.UserRepository;
import com.example.blooddono.utils.PDFGenerator;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SuperUserDrivesFragment extends Fragment {
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyStateText;
    private Spinner sortSpinner;
    private DonationDriveRepository driveRepository;
    private DonationRepository donationRepository;
    private UserRepository userRepository;
    private DrivesAdapter drivesAdapter;
    private List<DonationDrive> allDrives = new ArrayList<>();
    private FloatingActionButton downloadAllFab;
    private ProgressDialog progressDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_super_user_drives, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        sortSpinner = view.findViewById(R.id.sortSpinner);
        downloadAllFab = view.findViewById(R.id.downloadAllFab);
        downloadAllFab.setOnClickListener(v -> handleDownloadAllReports());

        // Initialize repository
        driveRepository = new DonationDriveRepository();
        userRepository = new UserRepository();
        donationRepository = new DonationRepository();

        // Setup RecyclerView
        drivesAdapter = new DrivesAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(drivesAdapter);

        // Setup sort spinner
        setupSortSpinner();

        // Load drives
        loadAllDrives();
    }

    private void setupSortSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.drive_sort_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(adapter);

        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sortDrives(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadAllDrives() {
        driveRepository.getAllDrives(new DonationDriveRepository.OnCompleteListener<List<DonationDrive>>() {
            @Override
            public void onSuccess(List<DonationDrive> drives) {
                if (drives.isEmpty()) {
                    progressBar.setVisibility(View.GONE);
                    allDrives = drives;
                    updateDisplay();
                    return;
                }

                AtomicInteger counter = new AtomicInteger(drives.size());

                for (DonationDrive drive : drives) {
                    userRepository.getUser(drive.getOwnerId(),
                            new UserRepository.OnCompleteListener<User>() {
                                @Override
                                public void onSuccess(User user) {
                                    drive.setOwnerName(user.getFullName());
                                    if (counter.decrementAndGet() == 0) {
                                        progressBar.setVisibility(View.GONE);
                                        allDrives = drives; // Use original list since we modified the objects
                                        updateDisplay();
                                    }
                                }

                                @Override
                                public void onError(Exception e) {
                                    drive.setOwnerName("Unknown"); // Set fallback name on error
                                    if (counter.decrementAndGet() == 0) {
                                        progressBar.setVisibility(View.GONE);
                                        allDrives = drives;
                                        updateDisplay();
                                    }
                                }
                            });
                }
            }

            @Override
            public void onError(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(),
                        "Error loading drives: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                updateDisplay();
            }
        });
    }

    private void sortDrives(int sortOption) {
        List<DonationDrive> sortedDrives = new ArrayList<>(allDrives);

        switch (sortOption) {
            case 0: // Date (Newest First)
                Collections.sort(sortedDrives,
                        (a, b) -> Long.compare(b.getStartDate(), a.getStartDate()));
                break;
            case 1: // Date (Oldest First)
                Collections.sort(sortedDrives,
                        (a, b) -> Long.compare(a.getStartDate(), b.getStartDate()));
                break;
            case 2: // Status (Active First)
                Collections.sort(sortedDrives,
                        (a, b) -> Boolean.compare(b.isActive(), a.isActive()));
                break;
            case 3: // Most Donations
                Collections.sort(sortedDrives,
                        (a, b) -> Integer.compare(b.getTotalDonations(), a.getTotalDonations()));
                break;
        }

        drivesAdapter.setDrives(sortedDrives);
    }

    private void updateDisplay() {
        if (allDrives.isEmpty()) {
            emptyStateText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            sortDrives(sortSpinner.getSelectedItemPosition());
        }
    }

    private class DrivesAdapter extends RecyclerView.Adapter<DrivesAdapter.ViewHolder> {
        private List<DonationDrive> drives = new ArrayList<>();
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_all_drives, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            DonationDrive drive = drives.get(position);

            // Set basic info
            holder.nameText.setText(drive.getName());
            String ownerDisplay = "Managed by: " +
                    (drive.getOwnerName() != null ? drive.getOwnerName() : "Unknown");
            holder.ownerText.setText(ownerDisplay);

            // Set dates
            String dateRange = String.format("%s - %s",
                    dateFormat.format(new Date(drive.getStartDate())),
                    dateFormat.format(new Date(drive.getEndDate())));
            holder.datesText.setText(dateRange);

            // Set status
            holder.statusText.setText(drive.isActive() ? "Active" : "Completed");
            holder.statusText.setTextColor(getResources().getColor(
                    drive.isActive() ? android.R.color.holo_green_dark : android.R.color.darker_gray));

            // Set statistics
            StringBuilder stats = new StringBuilder();
            stats.append(String.format("Total Donations: %d\n", drive.getTotalDonations()));

            // Add blood collection summary
            stats.append("Blood Collected:\n");
            Map<String, Double> collections = drive.getTotalCollectedAmounts();
            if (collections != null && !collections.isEmpty()) {
                for (Map.Entry<String, Double> entry : collections.entrySet()) {
                    stats.append(String.format("%s: %.1f mL\n", entry.getKey(), entry.getValue()));
                }
            } else {
                stats.append("No collections recorded");
            }

            holder.statsText.setText(stats.toString());

            // Set completion date if applicable
            if (!drive.isActive() && drive.getCompletedAt() > 0) {
                holder.completedText.setVisibility(View.VISIBLE);
                holder.completedText.setText("Completed: " +
                        dateFormat.format(new Date(drive.getCompletedAt())));
            } else {
                holder.completedText.setVisibility(View.GONE);
            }

            // Set click listener
            holder.itemView.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putString("driveId", drive.getId());
                Navigation.findNavController(v)
                        .navigate(R.id.action_superUserDrivesFragment_to_driveDetailFragment, args);
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
            final TextView statusText;
            final TextView statsText;
            final TextView ownerText; // Add this
            final TextView completedText;

            ViewHolder(View view) {
                super(view);
                nameText = view.findViewById(R.id.driveNameText);
                datesText = view.findViewById(R.id.driveDatesText);
                statusText = view.findViewById(R.id.statusText);
                statsText = view.findViewById(R.id.statsText);
                ownerText = view.findViewById(R.id.ownerText); // Add this
                completedText = view.findViewById(R.id.completedText);
            }
        }
    }

    private void handleDownloadAllReports() {
        // Show loading dialog
        progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Generating reports...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // First get all drives
        driveRepository.getAllDrives(new DonationDriveRepository.OnCompleteListener<List<DonationDrive>>() {
            @Override
            public void onSuccess(List<DonationDrive> drives) {
                // Create counter to track completion of owner info fetching
                final AtomicInteger ownerCounter = new AtomicInteger(drives.size());
                final List<Exception> ownerErrors = new ArrayList<>();

                // First fetch all owner information
                for (DonationDrive drive : drives) {
                    userRepository.getUser(drive.getOwnerId(),
                            new UserRepository.OnCompleteListener<User>() {
                                @Override
                                public void onSuccess(User user) {
                                    drive.setOwnerName(user.getFullName());
                                    if (ownerCounter.decrementAndGet() == 0) {
                                        // Now generate reports with owner information
                                        generateReportsForDrives(drives);
                                    }
                                }

                                @Override
                                public void onError(Exception e) {
                                    ownerErrors.add(e);
                                    drive.setOwnerName("Unknown");
                                    if (ownerCounter.decrementAndGet() == 0) {
                                        generateReportsForDrives(drives);
                                    }
                                }
                            });
                }
            }

            @Override
            public void onError(Exception e) {
                progressDialog.dismiss();
                showErrorDialog("Failed to fetch drives: " + e.getMessage());
            }
        });
    }

    private void generateReportsForDrives(List<DonationDrive> drives) {
        // Create counter to track completion of report generation
        final AtomicInteger counter = new AtomicInteger(drives.size());
        final List<File> generatedFiles = new ArrayList<>();
        final List<Exception> errors = new ArrayList<>();

        // Process each drive
        for (DonationDrive drive : drives) {
            // Get donations for this drive
            donationRepository.getDonationsByDrive(drive.getId(),
                    new DonationRepository.OnCompleteListener<List<Donation>>() {
                        @Override
                        public void onSuccess(List<Donation> donations) {
                            PDFGenerator.generateDriveReport(requireContext(),
                                    drive, donations,
                                    new PDFGenerator.OnPDFGeneratedListener() {
                                        @Override
                                        public void onSuccess(File pdfFile) {
                                            generatedFiles.add(pdfFile);
                                            checkCompletion(counter, generatedFiles, errors);
                                        }

                                        @Override
                                        public void onError(Exception e) {
                                            errors.add(e);
                                            checkCompletion(counter, generatedFiles, errors);
                                        }
                                    });
                        }

                        @Override
                        public void onError(Exception e) {
                            errors.add(e);
                            checkCompletion(counter, generatedFiles, errors);
                        }
                    });
        }
    }
    private void checkCompletion(AtomicInteger counter, List<File> generatedFiles, List<Exception> errors) {
        if (counter.decrementAndGet() == 0) {
            progressDialog.dismiss();

            // Show completion dialog
            requireActivity().runOnUiThread(() -> {
                if (!errors.isEmpty()) {
                    showErrorDialog("Some reports failed to generate. " +
                            errors.size() + " errors occurred.");
                } else {
                    showSuccessDialog(generatedFiles);
                }
            });
        }
    }

    private void showSuccessDialog(List<File> files) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Reports Generated")
                .setMessage(files.size() + " PDF reports have been saved to:\n" +
                        files.get(0).getParentFile().getPath())
                .setPositiveButton("Share", (dialog, which) -> {
                    sharePDFFiles(files);
                })
                .setNeutralButton("OK", null)
                .show();
    }

    private void showErrorDialog(String message) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void sharePDFFiles(List<File> files) {
        // Create ArrayList of URIs for sharing
        ArrayList<Uri> uris = new ArrayList<>();
        for (File file : files) {
            Uri contentUri = FileProvider.getUriForFile(requireContext(),
                    "com.example.blooddono.fileprovider",
                    file);
            uris.add(contentUri);
        }

        // Create share intent for multiple files
        Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        shareIntent.setType("application/pdf");
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Start share activity
        startActivity(Intent.createChooser(shareIntent, "Share PDF Reports"));
    }
}