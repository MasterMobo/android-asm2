package com.example.blooddono.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.example.blooddono.R;
import com.example.blooddono.models.Donation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BloodCollectionDialog extends Dialog {
    private final List<String> bloodTypes;
    private final OnCollectionConfirmedListener listener;
    private final Map<String, EditText> bloodTypeInputs = new HashMap<>();

    public interface OnCollectionConfirmedListener {
        void onCollectionConfirmed(Map<String, Double> collectedAmounts);
    }

    public BloodCollectionDialog(@NonNull Context context,
                                 List<String> bloodTypes,
                                 OnCollectionConfirmedListener listener) {
        super(context);
        this.bloodTypes = bloodTypes;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_blood_collection);

        LinearLayout inputContainer = findViewById(R.id.bloodTypeInputContainer);
        Button confirmButton = findViewById(R.id.confirmButton);

        // Add input fields for each blood type
        for (String bloodType : bloodTypes) {
            View inputView = LayoutInflater.from(getContext())
                    .inflate(R.layout.blood_collection_input_item, inputContainer, false);

            TextView bloodTypeLabel = inputView.findViewById(R.id.bloodTypeLabel);
            EditText amountInput = inputView.findViewById(R.id.amountInput);

            bloodTypeLabel.setText(bloodType);
            bloodTypeInputs.put(bloodType, amountInput);

            inputContainer.addView(inputView);
        }

        confirmButton.setOnClickListener(v -> {
            Map<String, Double> amounts = new HashMap<>();
            boolean hasError = false;

            // Validate and collect amounts
            for (Map.Entry<String, EditText> entry : bloodTypeInputs.entrySet()) {
                String bloodType = entry.getKey();
                EditText input = entry.getValue();
                String amountStr = input.getText().toString().trim();

                if (amountStr.isEmpty()) {
                    input.setError("Required");
                    hasError = true;
                    continue;
                }

                try {
                    double amount = Double.parseDouble(amountStr);
                    if (amount <= 0) {
                        input.setError("Must be greater than 0");
                        hasError = true;
                        continue;
                    }
                    amounts.put(bloodType, amount);
                } catch (NumberFormatException e) {
                    input.setError("Invalid number");
                    hasError = true;
                }
            }

            if (!hasError) {
                listener.onCollectionConfirmed(amounts);
                dismiss();
            }
        });
    }
}