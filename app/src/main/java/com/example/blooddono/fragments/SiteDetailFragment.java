package com.example.blooddono.fragments;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blooddono.R;
import com.example.blooddono.adapters.DonationsAdapter;
import com.example.blooddono.dialogs.BloodTypeSelectionDialog;
import com.example.blooddono.models.DayHours;
import com.example.blooddono.models.Donation;
import com.example.blooddono.models.DonationSite;
import com.example.blooddono.models.User;
import com.example.blooddono.repositories.DonationRepository;
import com.example.blooddono.repositories.DonationSiteRepository;
import com.example.blooddono.repositories.UserRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class SiteDetailFragment extends Fragment {
    private TextView ownerNameText;
    private TextView siteNameText;
    private TextView descriptionText;
    private TextView addressText;
    private DonationSiteRepository siteRepository;
    private UserRepository userRepository;
    private DonationRepository donationRepository;
    private TextView availabilityText;
    private TextView hoursTypeText;
    private LinearLayout operatingHoursLayout;
    private MaterialCardView datesCard;
    private MaterialCardView hoursCard;
    private ChipGroup bloodTypeChipGroup;
    private MaterialButton donateButton;
    private DonationSite currentSite;
    private RecyclerView donationsRecyclerView;
    private TextView noDonationsText;
    private DonationsAdapter donationsAdapter;
    private MaterialButton volunteerButton;
    private TextView volunteersText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_site_detail, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize repositories
        siteRepository = new DonationSiteRepository();
        userRepository = new UserRepository();
        donationRepository = new DonationRepository();

        // Initialize views
        ownerNameText = view.findViewById(R.id.ownerNameText);
        siteNameText = view.findViewById(R.id.siteNameText);
        descriptionText = view.findViewById(R.id.descriptionText);
        addressText = view.findViewById(R.id.addressText);
        availabilityText = view.findViewById(R.id.availabilityText);
        hoursTypeText = view.findViewById(R.id.hoursTypeText);
        operatingHoursLayout = view.findViewById(R.id.operatingHoursLayout);
        datesCard = view.findViewById(R.id.datesCard);
        hoursCard = view.findViewById(R.id.hoursCard);
        bloodTypeChipGroup = view.findViewById(R.id.bloodTypeChipGroup);
        donateButton = view.findViewById(R.id.donateButton);
        donationsRecyclerView = view.findViewById(R.id.donationsRecyclerView);
        noDonationsText = view.findViewById(R.id.noDonationsText);
        volunteerButton = view.findViewById(R.id.volunteerButton);
        volunteersText = view.findViewById(R.id.volunteersText);

        // Setup donations RecyclerView
        donationsAdapter = new DonationsAdapter(requireContext());
        donationsAdapter.setOnDonationStatusChangedListener(() -> {
            // Reload donations when status changes
            String siteId = getArguments().getString("siteId");
            if (siteId != null) {
                loadDonations(siteId);
            }
        });
        donationsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        donationsRecyclerView.setAdapter(donationsAdapter);

        // Get site ID from arguments and load details
        String siteId = getArguments().getString("siteId");
        if (siteId != null) {
            loadSiteDetails(siteId);
            loadDonations(siteId);
        }
    }

    private void loadSiteDetails(String siteId) {
        siteRepository.getDonationSite(siteId, new DonationSiteRepository.OnCompleteListener<DonationSite>() {
            @Override
            public void onSuccess(DonationSite site) {
                currentSite = site;
                // Set site details
                siteNameText.setText(site.getName());
                descriptionText.setText(site.getDescription());
                addressText.setText(site.getAddress());

                displayAvailability(site);
                displayOperatingHours(site);
                displayBloodTypes(site);
                displayVolunteers(site.getVolunteerIds());

                // Load owner details
                loadOwnerDetails(site.getOwnerId());

                // Check if current user is the site owner
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                userRepository.getUser(currentUserId, new UserRepository.OnCompleteListener<User>() {
                    @Override
                    public void onSuccess(User user) {
                        boolean isOwner = user.getUid().equals(site.getOwnerId());
                        donationsAdapter.setShowConfirmButton(isOwner);

                        if (User.ROLE_DONOR.equals(user.getRole())) {
                            donateButton.setVisibility(View.VISIBLE);
                            donateButton.setOnClickListener(v -> handleDonation(user));

                            volunteerButton.setVisibility(View.GONE);
                        } else {
                            donateButton.setVisibility(View.GONE);

                            if (site.getOwnerId().equals(currentUserId)) {
                                volunteerButton.setVisibility(View.GONE);
                                return;
                            }

                            if (site.getVolunteerIds() != null && site.getVolunteerIds().contains(currentUserId)) {
                                volunteerButton.setVisibility(View.GONE);
                                return;
                            }

                            volunteerButton.setVisibility(View.VISIBLE);
                            volunteerButton.setOnClickListener(v -> handleVolunteer());
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        // Handle error if needed
                        donationsAdapter.setShowConfirmButton(false);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(requireContext(),
                        "Error loading site details: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void loadOwnerDetails(String ownerId) {
        userRepository.getUser(ownerId, new UserRepository.OnCompleteListener<User>() {
            @Override
            public void onSuccess(User user) {
                ownerNameText.setText(user.getFullName()); // Now using full name instead of email
            }

            @Override
            public void onError(Exception e) {
                Log.e("SiteDetailFragment", e.getMessage());
                ownerNameText.setText("Unknown Owner");
            }
        });
    }

    private void handleDonation(User donor) {
        if (currentSite == null) return;

        // Create and show blood type selection dialog
        BloodTypeSelectionDialog dialog = new BloodTypeSelectionDialog(
                requireContext(),
                currentSite.getNeededBloodTypes(),
                selectedBloodTypes -> {
                    // Create donation record
                    Donation donation = new Donation(
                            donor.getUid(),
                            donor.getFullName(),
                            currentSite.getId(),
                            currentSite.getName(),
                            selectedBloodTypes
                    );

                    // Show loading dialog
                    ProgressDialog progressDialog = new ProgressDialog(requireContext());
                    progressDialog.setMessage("Registering donation...");
                    progressDialog.show();

                    // Save donation to Firebase
                    donationRepository.createDonation(donation, new DonationRepository.OnCompleteListener<String>() {
                        @Override
                        public void onSuccess(String donationId) {
                            progressDialog.dismiss();
                            showSuccessDialog();
                            // Reload donations to show the new one
                            loadDonations(currentSite.getId());
                        }

                        @Override
                        public void onError(Exception e) {
                            progressDialog.dismiss();
                            showErrorDialog(e.getMessage());
                        }
                    });
                }
        );
        dialog.show();
    }

    private void showSuccessDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Success")
                .setMessage("Your donation registration has been submitted successfully!")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showErrorDialog(String message) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Error")
                .setMessage("Failed to register donation: " + message)
                .setPositiveButton("OK", null)
                .show();
    }

    private void displayAvailability(DonationSite site) {
        if (DonationSite.TYPE_PERMANENT.equals(site.getType())) {
            availabilityText.setText("Permanently available");
            datesCard.setVisibility(View.VISIBLE);
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String startDate = sdf.format(new Date(site.getStartDate()));
            String endDate = sdf.format(new Date(site.getEndDate()));

            // Check if site is currently active
            long currentTime = System.currentTimeMillis();
            boolean isActive = currentTime >= site.getStartDate() && currentTime <= site.getEndDate();

            String status = isActive ? "Active" : "Inactive";
            String dateRange = String.format("Available from %s to %s\nStatus: %s",
                    startDate, endDate, status);

            availabilityText.setText(dateRange);
            datesCard.setVisibility(View.VISIBLE);
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

            // Days of the week in order
            String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday",
                    "Friday", "Saturday", "Sunday"};

            // Add each day's hours
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
        } else {
            TextView noBloodTypesText = new TextView(requireContext());
            noBloodTypesText.setText("No specific blood types specified");
            noBloodTypesText.setTextColor(Color.GRAY);
            bloodTypeChipGroup.addView(noBloodTypesText);
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

    private void loadDonations(String siteId) {
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Refreshing donations...");
        progressDialog.show();

        donationRepository.getDonationsBySite(siteId, new DonationRepository.OnCompleteListener<List<Donation>>() {
            @Override
            public void onSuccess(List<Donation> donations) {
                progressDialog.dismiss();
                if (donations.isEmpty()) {
                    noDonationsText.setVisibility(View.VISIBLE);
                    donationsRecyclerView.setVisibility(View.GONE);
                } else {
                    noDonationsText.setVisibility(View.GONE);
                    donationsRecyclerView.setVisibility(View.VISIBLE);
                    donationsAdapter.setDonations(donations);
                }
            }

            @Override
            public void onError(Exception e) {
                progressDialog.dismiss();
                Log.e("SiteDetailFragment", e.getMessage());
                Toast.makeText(requireContext(),
                        "Error loading donations: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                noDonationsText.setVisibility(View.VISIBLE);
                donationsRecyclerView.setVisibility(View.GONE);
            }
        });
    }

    private void displayVolunteers(List<String> volunteerIds) {
        if (volunteerIds == null || volunteerIds.isEmpty()) {
            volunteersText.setVisibility(View.VISIBLE);
            volunteersText.setText("No volunteers yet");
            return;
        }

        StringBuilder volunteerNames = new StringBuilder();
        AtomicInteger counter = new AtomicInteger(volunteerIds.size());

        for (String volunteerId : volunteerIds) {
            userRepository.getUser(volunteerId, new UserRepository.OnCompleteListener<User>() {
                @Override
                public void onSuccess(User user) {
                    volunteerNames.append(user.getFullName()).append("\n");
                    if (counter.decrementAndGet() == 0) {
                        volunteersText.setText(volunteerNames.toString());
                        volunteersText.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (counter.decrementAndGet() == 0) {
                        volunteersText.setText(volunteerNames.toString());
                        volunteersText.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
    }

    private void handleVolunteer() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        siteRepository.addVolunteer(currentSite.getId(), currentUserId,
                new DonationSiteRepository.OnCompleteListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Toast.makeText(requireContext(), "Registered as volunteer successfully",
                                Toast.LENGTH_SHORT).show();
                        // Reload the site details to refresh the UI
                        loadSiteDetails(currentSite.getId());
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(requireContext(), "Error registering as volunteer: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}