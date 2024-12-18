package com.example.blooddono.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
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
import java.util.Map;

public class DriveDonationsAdapter extends RecyclerView.Adapter<DriveDonationsAdapter.ViewHolder> {
    private final List<Donation> donations = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy, HH:mm", Locale.getDefault());
    private final Context context;

    public DriveDonationsAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_drive_donation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Donation donation = donations.get(position);

        // Set donor name and site
        holder.donorNameText.setText(donation.getDonorName());
        holder.siteNameText.setText(donation.getSiteName());

        // Set date
        holder.dateText.setText(dateFormat.format(new Date(donation.getCreatedAt())));

        // Set status with appropriate color
        holder.statusText.setText(donation.getStatus().toUpperCase(Locale.getDefault()));
        int statusColor;
        if (Donation.STATUS_COMPLETED.equals(donation.getStatus())) {
            statusColor = context.getResources().getColor(android.R.color.holo_green_dark);
        } else {
            statusColor = context.getResources().getColor(android.R.color.holo_blue_dark);
        }
        holder.statusText.setTextColor(statusColor);

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

        // Show collected amounts if completed
        if (Donation.STATUS_COMPLETED.equals(donation.getStatus()) &&
                donation.getCollectedAmounts() != null) {
            StringBuilder amounts = new StringBuilder("Collected amounts:\n");
            for (Map.Entry<String, Double> entry : donation.getCollectedAmounts().entrySet()) {
                amounts.append(entry.getKey())
                        .append(": ")
                        .append(String.format(Locale.getDefault(), "%.1f", entry.getValue()))
                        .append(" mL\n");
            }
            holder.collectedAmountsText.setText(amounts.toString());
            holder.collectedAmountsText.setVisibility(View.VISIBLE);
        } else {
            holder.collectedAmountsText.setVisibility(View.GONE);
        }

        // Show completion date if completed
        if (Donation.STATUS_COMPLETED.equals(donation.getStatus()) && donation.getCompletedAt() > 0) {
            String completedDate = "Completed: " +
                    dateFormat.format(new Date(donation.getCompletedAt()));
            holder.completedDateText.setText(completedDate);
            holder.completedDateText.setVisibility(View.VISIBLE);
        } else {
            holder.completedDateText.setVisibility(View.GONE);
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

    public List<Donation> getDonations() {
        return new ArrayList<>(donations);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView donorNameText;
        final TextView siteNameText;
        final TextView dateText;
        final TextView statusText;
        final ChipGroup bloodTypeChipGroup;
        final TextView collectedAmountsText;
        final TextView completedDateText;

        ViewHolder(View view) {
            super(view);
            donorNameText = view.findViewById(R.id.donorNameText);
            siteNameText = view.findViewById(R.id.siteNameText);
            dateText = view.findViewById(R.id.dateText);
            statusText = view.findViewById(R.id.statusText);
            bloodTypeChipGroup = view.findViewById(R.id.bloodTypeChipGroup);
            collectedAmountsText = view.findViewById(R.id.collectedAmountsText);
            completedDateText = view.findViewById(R.id.completedDateText);
        }
    }
}