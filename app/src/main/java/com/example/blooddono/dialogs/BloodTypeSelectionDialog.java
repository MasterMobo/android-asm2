package com.example.blooddono.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blooddono.R;
import com.example.blooddono.models.DonationSite;
import java.util.ArrayList;
import java.util.List;

public class BloodTypeSelectionDialog extends Dialog {
    private final List<String> availableBloodTypes;
    private final List<String> selectedBloodTypes = new ArrayList<>();
    private final OnBloodTypesSelectedListener listener;

    public interface OnBloodTypesSelectedListener {
        void onBloodTypesSelected(List<String> selectedBloodTypes);
    }

    public BloodTypeSelectionDialog(Context context, List<String> availableBloodTypes,
                                    OnBloodTypesSelectedListener listener) {
        super(context);
        this.availableBloodTypes = availableBloodTypes;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_blood_type_selection);

        RecyclerView recyclerView = findViewById(R.id.bloodTypesRecyclerView);
        Button confirmButton = findViewById(R.id.confirmButton);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        BloodTypeAdapter adapter = new BloodTypeAdapter(availableBloodTypes, selectedBloodTypes);
        recyclerView.setAdapter(adapter);

        confirmButton.setOnClickListener(v -> {
            if (selectedBloodTypes.isEmpty()) {
                Toast.makeText(getContext(), "Please select at least one blood type",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            listener.onBloodTypesSelected(new ArrayList<>(selectedBloodTypes));
            dismiss();
        });
    }

    private static class BloodTypeAdapter extends RecyclerView.Adapter<BloodTypeAdapter.ViewHolder> {
        private final List<String> bloodTypes;
        private final List<String> selectedBloodTypes;

        BloodTypeAdapter(List<String> bloodTypes, List<String> selectedBloodTypes) {
            this.bloodTypes = bloodTypes;
            this.selectedBloodTypes = selectedBloodTypes;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_blood_type, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            String bloodType = bloodTypes.get(position);
            holder.checkBox.setText(bloodType);
            holder.checkBox.setChecked(selectedBloodTypes.contains(bloodType));
            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedBloodTypes.add(bloodType);
                } else {
                    selectedBloodTypes.remove(bloodType);
                }
            });
        }

        @Override
        public int getItemCount() {
            return bloodTypes.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final CheckBox checkBox;

            ViewHolder(View view) {
                super(view);
                checkBox = (CheckBox) view;
            }
        }
    }
}