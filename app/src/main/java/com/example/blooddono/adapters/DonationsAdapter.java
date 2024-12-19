package com.example.blooddono.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;
import com.example.blooddono.R;
import com.example.blooddono.dialogs.BloodCollectionDialog;
import com.example.blooddono.models.Donation;
import com.example.blooddono.repositories.DonationRepository;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DonationsAdapter extends RecyclerView.Adapter<DonationsAdapter.ViewHolder> {
    private final List<Donation> donations = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final Context context;
    private boolean showConfirmButton = false;
    private final DonationRepository donationRepository;

    public DonationsAdapter(Context context) {
        this.context = context;
        this.donationRepository = new DonationRepository();
    }

    public interface OnDonationStatusChangedListener {
        void onDonationStatusChanged();
    }

    private OnDonationStatusChangedListener statusChangedListener;

    public void setOnDonationStatusChangedListener(OnDonationStatusChangedListener listener) {
        this.statusChangedListener = listener;
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

        Button confirmButton = holder.itemView.findViewById(R.id.confirmButton);
        if (showConfirmButton && !Donation.STATUS_COMPLETED.equals(donation.getStatus())) {
            confirmButton.setVisibility(View.VISIBLE);
            confirmButton.setOnClickListener(v -> {
                BloodCollectionDialog dialog = new BloodCollectionDialog(
                        context,
                        donation.getBloodTypes(),
                        collectedAmounts -> {
                            updateDonationStatus(donation.getId(), collectedAmounts);
                        });
                dialog.show();
            });
        } else {
            confirmButton.setVisibility(View.GONE);
        }

        // Show collected amounts if completed
        if (Donation.STATUS_COMPLETED.equals(donation.getStatus()) &&
                donation.getCollectedAmounts() != null) {
            StringBuilder amounts = new StringBuilder("Collected: ");
            for (Map.Entry<String, Double> entry :
                    donation.getCollectedAmounts().entrySet()) {
                amounts.append(entry.getKey())
                        .append(": ")
                        .append(entry.getValue())
                        .append("mL, ");
            }
            // Remove last comma and space
            amounts.setLength(amounts.length() - 2);
            holder.collectedAmountsText.setText(amounts.toString());
            holder.collectedAmountsText.setVisibility(View.VISIBLE);
        } else {
            holder.collectedAmountsText.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return donations.size();
    }

    public List<Donation> getDonations() {
        return new ArrayList<>(donations);
    }
    public void setDonations(List<Donation> newDonations) {
        donations.clear();
        donations.addAll(newDonations);
        notifyDataSetChanged();
    }

    public void setShowConfirmButton(boolean show) {
        this.showConfirmButton = show;
        notifyDataSetChanged();
    }

    private void updateDonationStatus(String donationId,
                                      Map<String, Double> collectedAmounts) {
        donationRepository.updateDonationStatus(
                donationId,
                Donation.STATUS_COMPLETED,
                collectedAmounts,
                new DonationRepository.OnCompleteListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Toast.makeText(context,
                                "Donation confirmed successfully",
                                Toast.LENGTH_SHORT).show();
                        if (statusChangedListener != null) {
                            statusChangedListener.onDonationStatusChanged();
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(context,
                                "Error confirming donation: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView donorNameText;
        final TextView statusText;
        final ChipGroup bloodTypeChipGroup;
        final TextView dateText;
        final TextView collectedAmountsText;
        final Button confirmButton;

        ViewHolder(View view) {
            super(view);
            donorNameText = view.findViewById(R.id.donorNameText);
            statusText = view.findViewById(R.id.statusText);
            bloodTypeChipGroup = view.findViewById(R.id.bloodTypeChipGroup);
            dateText = view.findViewById(R.id.dateText);
            collectedAmountsText = view.findViewById(R.id.collectedAmountsText);
            confirmButton = view.findViewById(R.id.confirmButton);
        }
    }
}