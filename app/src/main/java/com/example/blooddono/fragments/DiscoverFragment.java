package com.example.blooddono.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.ProgressBar;
import com.example.blooddono.R;
import com.example.blooddono.models.DonationSite;
import com.example.blooddono.repositories.DonationSiteRepository;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DiscoverFragment extends Fragment {
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyStateText;
    private EditText searchInput;
    private DonationSiteRepository repository;
    private SitesAdapter adapter;
    private List<DonationSite> allSites = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_discover, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        searchInput = view.findViewById(R.id.searchInput);

        // Initialize repository
        repository = new DonationSiteRepository();

        // Setup RecyclerView
        adapter = new SitesAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // Setup search functionality
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                filterSites(s.toString());
            }
        });

        // Load sites
        loadSites();
    }

    private void filterSites(String query) {
        if (query.isEmpty()) {
            adapter.setSites(allSites);
            updateEmptyState(allSites.isEmpty());
            return;
        }

        List<DonationSite> filteredSites = new ArrayList<>();
        String lowercaseQuery = query.toLowerCase(Locale.getDefault());

        for (DonationSite site : allSites) {
            if (site.getName().toLowerCase(Locale.getDefault()).contains(lowercaseQuery) ||
                    site.getDescription().toLowerCase(Locale.getDefault()).contains(lowercaseQuery)) {
                filteredSites.add(site);
            }
        }

        adapter.setSites(filteredSites);
        updateEmptyState(filteredSites.isEmpty());
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            String searchText = searchInput.getText().toString();
            if (searchText.isEmpty()) {
                emptyStateText.setText("No donation sites available");
            } else {
                emptyStateText.setText("No matches found for \"" + searchText + "\"");
            }
            emptyStateText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void loadSites() {
        progressBar.setVisibility(View.VISIBLE);
        repository.getAllDonationSites(new DonationSiteRepository.OnCompleteListener<List<DonationSite>>() {
            @Override
            public void onSuccess(List<DonationSite> sites) {
                progressBar.setVisibility(View.GONE);
                allSites = sites;
                String currentQuery = searchInput.getText().toString();
                if (currentQuery.isEmpty()) {
                    adapter.setSites(sites);
                    updateEmptyState(sites.isEmpty());
                } else {
                    filterSites(currentQuery);
                }
            }

            @Override
            public void onError(Exception e) {
                progressBar.setVisibility(View.GONE);
                emptyStateText.setText("Error loading sites: " + e.getMessage());
                emptyStateText.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        });
    }

    private class SitesAdapter extends RecyclerView.Adapter<SitesAdapter.SiteViewHolder> {
        private List<DonationSite> sites = new ArrayList<>();

        @Override
        public SiteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_discover_site, parent, false);
            return new SiteViewHolder(view);
        }

        @Override
        public void onBindViewHolder(SiteViewHolder holder, int position) {
            DonationSite site = sites.get(position);
            holder.nameText.setText(site.getName());

            // Set description (limit to 100 characters)
            String description = site.getDescription();
            if (description.length() > 100) {
                description = description.substring(0, 97) + "...";
            }
            holder.descriptionText.setText(description);

            holder.addressText.setText(site.getAddress());

            // Set opening status
            String status;
            if (DonationSite.TYPE_LIMITED.equals(site.getType())) {
                long currentTime = System.currentTimeMillis();
                if (currentTime >= site.getStartDate() && currentTime <= site.getEndDate()) {
                    if (DonationSite.HOURS_24_7.equals(site.getHoursType())) {
                        status = "Open 24/7";
                    } else {
                        status = "Open (Limited Time)";
                    }
                } else {
                    status = "Closed";
                }
            } else {
                if (DonationSite.HOURS_24_7.equals(site.getHoursType())) {
                    status = "Open 24/7";
                } else {
                    status = "Open (Check Hours)";
                }
            }
            holder.statusText.setText(status);

            // Set status text color based on status
            int color = status.contains("Open") ?
                    getResources().getColor(android.R.color.holo_green_dark) :
                    getResources().getColor(android.R.color.holo_red_dark);
            holder.statusText.setTextColor(color);

            // Add blood types
            holder.bloodTypeChipGroup.removeAllViews();
            if (site.getNeededBloodTypes() != null && !site.getNeededBloodTypes().isEmpty()) {
                for (String bloodType : site.getNeededBloodTypes()) {
                    Chip chip = new Chip(holder.itemView.getContext());
                    chip.setText(bloodType);
                    chip.setClickable(false);
                    chip.setChipBackgroundColorResource(R.color.design_default_color_primary);
                    chip.setTextColor(Color.WHITE);
                    holder.bloodTypeChipGroup.addView(chip);
                }
            }

        }

        @Override
        public int getItemCount() {
            return sites.size();
        }

        public void setSites(List<DonationSite> sites) {
            this.sites = sites;
            notifyDataSetChanged();
        }

        class SiteViewHolder extends RecyclerView.ViewHolder {
            TextView nameText;
            TextView descriptionText;
            TextView addressText;
            TextView statusText;
            ChipGroup bloodTypeChipGroup;

            SiteViewHolder(View itemView) {
                super(itemView);
                nameText = itemView.findViewById(R.id.nameText);
                descriptionText = itemView.findViewById(R.id.descriptionText);
                addressText = itemView.findViewById(R.id.addressText);
                statusText = itemView.findViewById(R.id.statusText);
                bloodTypeChipGroup = itemView.findViewById(R.id.bloodTypeChipGroup);

                itemView.setOnClickListener(v -> {
                    DonationSite site = sites.get(getAdapterPosition());
                    Bundle args = new Bundle();
                    args.putString("siteId", site.getId());
                    Navigation.findNavController(itemView)
                            .navigate(R.id.action_discoverFragment_to_siteDetailFragment, args);
                });
            }
        }
    }
}