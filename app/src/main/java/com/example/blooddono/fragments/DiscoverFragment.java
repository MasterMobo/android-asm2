package com.example.blooddono.fragments;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.Button;
import android.widget.ProgressBar;

import com.example.blooddono.R;
import com.example.blooddono.models.DonationSite;
import com.example.blooddono.repositories.DonationSiteRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import android.graphics.Color;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DiscoverFragment extends Fragment {
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyStateText;
    private EditText searchInput;
    private MaterialButton bloodTypeFilterButton;
    private MaterialButton dateFilterButton;
    private MaterialButton clearFiltersButton;
    private DonationSiteRepository repository;
    private SitesAdapter adapter;
    private List<DonationSite> allSites = new ArrayList<>();
    private Set<String> selectedBloodTypes = new HashSet<>();
    private Long selectedDate = null;

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
        bloodTypeFilterButton = view.findViewById(R.id.bloodTypeFilterButton);
        dateFilterButton = view.findViewById(R.id.dateFilterButton);
        clearFiltersButton = view.findViewById(R.id.clearFiltersButton);

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
                filterSites();
            }
        });

        // Setup filter buttons
        bloodTypeFilterButton.setOnClickListener(v -> showBloodTypeFilterDialog());
        dateFilterButton.setOnClickListener(v -> showDateFilterDialog());
        clearFiltersButton.setOnClickListener(v -> clearFilters());

        // Load sites
        loadSites();
    }

    private void showBloodTypeFilterDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_blood_type_filter);

        GridLayout bloodTypeGrid = dialog.findViewById(R.id.bloodTypeGrid);
        Button applyButton = dialog.findViewById(R.id.applyButton);

        // Add checkboxes for each blood type
        Set<String> tempSelectedTypes = new HashSet<>(selectedBloodTypes);
        for (String bloodType : DonationSite.BLOOD_TYPES) {
            CheckBox checkBox = new CheckBox(requireContext());
            checkBox.setText(bloodType);
            checkBox.setChecked(selectedBloodTypes.contains(bloodType));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    tempSelectedTypes.add(bloodType);
                } else {
                    tempSelectedTypes.remove(bloodType);
                }
            });
            bloodTypeGrid.addView(checkBox);
        }

        applyButton.setOnClickListener(v -> {
            selectedBloodTypes = tempSelectedTypes;
            updateFilterButtonsAppearance();
            filterSites();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showDateFilterDialog() {
        Calendar calendar = Calendar.getInstance();
        if (selectedDate != null) {
            calendar.setTimeInMillis(selectedDate);
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCal = Calendar.getInstance();
                    selectedCal.set(year, month, dayOfMonth);
                    selectedDate = selectedCal.getTimeInMillis();
                    updateFilterButtonsAppearance();
                    filterSites();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void clearFilters() {
        selectedBloodTypes.clear();
        selectedDate = null;
        updateFilterButtonsAppearance();
        filterSites();
    }

    private void updateFilterButtonsAppearance() {
        boolean hasFilters = !selectedBloodTypes.isEmpty() || selectedDate != null;
        clearFiltersButton.setVisibility(hasFilters ? View.VISIBLE : View.GONE);

        // Update blood type filter button appearance
        if (!selectedBloodTypes.isEmpty()) {
            bloodTypeFilterButton.setStrokeColor(getResources().getColorStateList(R.color.design_default_color_primary));
            bloodTypeFilterButton.setStrokeWidth(4);
        } else {
            bloodTypeFilterButton.setStrokeColor(getResources().getColorStateList(R.color.design_default_color_primary));
            bloodTypeFilterButton.setStrokeWidth(1);
        }

        // Update date filter button appearance
        if (selectedDate != null) {
            dateFilterButton.setStrokeColor(getResources().getColorStateList(R.color.design_default_color_primary));
            dateFilterButton.setStrokeWidth(4);
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
            dateFilterButton.setText(sdf.format(new Date(selectedDate)));
        } else {
            dateFilterButton.setStrokeColor(getResources().getColorStateList(R.color.design_default_color_primary));
            dateFilterButton.setStrokeWidth(1);
            dateFilterButton.setText("Date");
        }
    }

    private void filterSites() {
        String query = searchInput.getText().toString().toLowerCase(Locale.getDefault());
        List<DonationSite> filteredSites = new ArrayList<>();

        for (DonationSite site : allSites) {
            // Apply text search filter
            boolean matchesSearch = query.isEmpty() ||
                    site.getName().toLowerCase(Locale.getDefault()).contains(query) ||
                    site.getDescription().toLowerCase(Locale.getDefault()).contains(query);

            // Apply blood type filter
            boolean matchesBloodType = selectedBloodTypes.isEmpty() ||
                    site.getNeededBloodTypes().stream().anyMatch(selectedBloodTypes::contains);

            // Apply date filter
            boolean matchesDate = true;
            if (selectedDate != null) {
                if (DonationSite.TYPE_LIMITED.equals(site.getType())) {
                    matchesDate = selectedDate >= site.getStartDate() && selectedDate <= site.getEndDate();
                }
                // Permanent sites are always available, so they match any date
            }

            if (matchesSearch && matchesBloodType && matchesDate) {
                filteredSites.add(site);
            }
        }

        adapter.setSites(filteredSites);
        updateEmptyState(filteredSites.isEmpty());
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            String searchText = searchInput.getText().toString();
            if (searchText.isEmpty() && selectedBloodTypes.isEmpty() && selectedDate == null) {
                emptyStateText.setText("No donation sites available");
            } else {
                emptyStateText.setText("No sites match your filters");
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
                filterSites(); // This will apply any active filters and update the UI
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

            // Set availability text
            StringBuilder availabilityText = new StringBuilder();
            if (DonationSite.TYPE_PERMANENT.equals(site.getType())) {
                availabilityText.append("Permanently Available");
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                String startDate = sdf.format(new Date(site.getStartDate()));
                String endDate = sdf.format(new Date(site.getEndDate()));
                availabilityText.append("Available from ").append(startDate)
                        .append(" to ").append(endDate);
            }
            holder.availabilityText.setText(availabilityText);

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
                    boolean isSelected = selectedBloodTypes.contains(bloodType);
                    chip.setChipBackgroundColorResource(isSelected ?
                            R.color.design_default_color_secondary :
                            R.color.design_default_color_primary);
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
            TextView availabilityText;
            ChipGroup bloodTypeChipGroup;

            SiteViewHolder(View itemView) {
                super(itemView);
                nameText = itemView.findViewById(R.id.nameText);
                descriptionText = itemView.findViewById(R.id.descriptionText);
                addressText = itemView.findViewById(R.id.addressText);
                statusText = itemView.findViewById(R.id.statusText);
                availabilityText = itemView.findViewById(R.id.availabilityText);
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