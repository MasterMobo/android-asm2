package com.example.blooddono.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blooddono.R;
import com.example.blooddono.models.Donation;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DonationsAdapter extends RecyclerView.Adapter<DonationsAdapter.ViewHolder> {
    private final List<Donation> donations = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final Context context;

    public DonationsAdapter(Context context) {
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_donation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Donation donation = donations.get(position);

        holder.donorNameText.setText(donation.getDonorName());
        holder.dateText.setText(dateFormat.format(new Date(donation.getCreatedAt())));

        // Set status
        holder.statusText.setText(donation.getStatus().toUpperCase(Locale.getDefault()));
        if (Donation.STATUS_COMPLETED.equals(donation.getStatus())) {
            holder.statusText.setBackgroundColor(context.getResources().getColor(android.R.color.holo_green_dark));
        } else {
            holder.statusText.setBackgroundColor(context.getResources().getColor(android.R.color.holo_blue_dark));
        }

        // Add blood type chips
        holder.bloodTypeChipGroup.removeAllViews();
        for (String bloodType : donation.getBloodTypes()) {
            Chip chip = new Chip(context);
            chip.setText(bloodType);
            chip.setClickable(false);
            chip.setChipBackgroundColorResource(R.color.design_default_color_primary);
            chip.setTextColor(Color.WHITE);
            holder.bloodTypeChipGroup.addView(chip);
        }
    }

    @Override
    public int getItemCount() {
        return donations.size();
    }

    public void setDonations(List<Donation> newDonations) {
        donations.clear();
        donations.addAll(newDonations);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView donorNameText;
        final TextView statusText;
        final ChipGroup bloodTypeChipGroup;
        final TextView dateText;

        ViewHolder(View view) {
            super(view);
            donorNameText = view.findViewById(R.id.donorNameText);
            statusText = view.findViewById(R.id.statusText);
            bloodTypeChipGroup = view.findViewById(R.id.bloodTypeChipGroup);
            dateText = view.findViewById(R.id.dateText);
        }
    }
}