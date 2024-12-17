package com.example.blooddono.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.blooddono.R;
import com.example.blooddono.models.DayHours;
import com.example.blooddono.models.DonationSite;
import com.example.blooddono.models.User;
import com.example.blooddono.repositories.DonationSiteRepository;
import com.example.blooddono.repositories.UserRepository;
import com.google.android.material.card.MaterialCardView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SiteDetailFragment extends Fragment {
    private TextView ownerNameText;
    private TextView siteNameText;
    private TextView descriptionText;
    private TextView addressText;
    private DonationSiteRepository siteRepository;
    private UserRepository userRepository;
    private TextView availabilityText;
    private TextView hoursTypeText;
    private LinearLayout operatingHoursLayout;
    private MaterialCardView datesCard;
    private MaterialCardView hoursCard;

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

        // Get site ID from arguments
        String siteId = getArguments().getString("siteId");
        if (siteId != null) {
            loadSiteDetails(siteId);
        }
    }

    private void loadSiteDetails(String siteId) {
        siteRepository.getDonationSite(siteId, new DonationSiteRepository.OnCompleteListener<DonationSite>() {
            @Override
            public void onSuccess(DonationSite site) {
                // Set site details
                siteNameText.setText(site.getName());
                descriptionText.setText(site.getDescription());
                addressText.setText(site.getAddress());

                // Display availability and hours
                displayAvailability(site);
                displayOperatingHours(site);

                // Load owner details
                loadOwnerDetails(site.getOwnerId());
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
                ownerNameText.setText(user.getEmail()); // Using email as name for now
            }

            @Override
            public void onError(Exception e) {
                ownerNameText.setText("Unknown Owner");
            }
        });
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
}