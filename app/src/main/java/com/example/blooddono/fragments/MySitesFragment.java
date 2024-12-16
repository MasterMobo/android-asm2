package com.example.blooddono.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;


import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.blooddono.R;
import com.example.blooddono.models.DonationSite;
import com.example.blooddono.repositories.DonationSiteRepository;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.List;

public class MySitesFragment extends Fragment {
    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private FloatingActionButton addButton;
    private DonationSiteRepository repository;
    private SitesAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_sites, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        addButton = view.findViewById(R.id.addButton);

        // Initialize repository
        repository = new DonationSiteRepository();

        // Setup RecyclerView
        adapter = new SitesAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // Setup FAB
        addButton.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_mySitesFragment_to_siteRegistrationFragment)
        );

        // Load sites
        loadMySites();
    }

    private void loadMySites() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        repository.getDonationSitesByOwner(currentUserId, new DonationSiteRepository.OnCompleteListener<List<DonationSite>>() {
            @Override
            public void onSuccess(List<DonationSite> sites) {
                if (sites.isEmpty()) {
                    emptyStateText.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyStateText.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    adapter.setSites(sites);
                }
            }

            @Override
            public void onError(Exception e) {
                if(getContext() != null) {
                    Toast.makeText(getContext(),
                            "Error loading sites: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
                emptyStateText.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        });
    }

    // RecyclerView Adapter
    private class SitesAdapter extends RecyclerView.Adapter<SitesAdapter.SiteViewHolder> {
        private List<DonationSite> sites = new ArrayList<>();

        @Override
        public SiteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_donation_site, parent, false);
            return new SiteViewHolder(view);
        }

        @Override
        public void onBindViewHolder(SiteViewHolder holder, int position) {
            DonationSite site = sites.get(position);
            holder.nameText.setText(site.getName());
            holder.addressText.setText(site.getAddress());
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
            TextView addressText;

            // In SitesAdapter.SiteViewHolder
            SiteViewHolder(View itemView) {
                super(itemView);
                nameText = itemView.findViewById(R.id.nameText);
                addressText = itemView.findViewById(R.id.addressText);

                itemView.setOnClickListener(v -> {
                    DonationSite site = sites.get(getAdapterPosition());
                    Bundle args = new Bundle();
                    args.putString("siteId", site.getId());
                    Navigation.findNavController(itemView)
                            .navigate(R.id.action_mySitesFragment_to_siteDetailFragment, args);
                });
            }
        }
    }
}