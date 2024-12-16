package com.example.blooddono.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.blooddono.R;
import com.example.blooddono.models.DonationSite;
import com.example.blooddono.models.User;
import com.example.blooddono.repositories.DonationSiteRepository;
import com.example.blooddono.repositories.UserRepository;

public class SiteDetailFragment extends Fragment {
    private TextView ownerNameText;
    private TextView siteNameText;
    private TextView descriptionText;
    private TextView addressText;
    private DonationSiteRepository siteRepository;
    private UserRepository userRepository;

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
}