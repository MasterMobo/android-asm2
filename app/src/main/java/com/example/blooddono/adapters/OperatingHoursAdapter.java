package com.example.blooddono.adapters;

import android.app.TimePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.blooddono.R;
import com.example.blooddono.models.DayHours;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class OperatingHoursAdapter extends RecyclerView.Adapter<OperatingHoursAdapter.ViewHolder> {
    private final String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    private final Map<String, DayHours> operatingHours;
    private final Context context;

    public OperatingHoursAdapter(Context context) {
        this.context = context;
        this.operatingHours = new HashMap<>();
        // Initialize with default values
        for (String day : days) {
            operatingHours.put(day, new DayHours(false, "09:00", "17:00"));
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.operating_hours_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String day = days[position];
        DayHours hours = operatingHours.get(day);

        holder.dayName.setText(day);
        holder.dayCheckbox.setChecked(hours.isOpen());
        holder.openTimeButton.setText(hours.getOpenTime());
        holder.closeTimeButton.setText(hours.getCloseTime());

        // Update visibility based on checkbox
        holder.hoursLayout.setVisibility(hours.isOpen() ? View.VISIBLE : View.GONE);

        // Set up listeners
        holder.dayCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            hours.setOpen(isChecked);
            holder.hoursLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        holder.openTimeButton.setOnClickListener(v -> showTimePicker(hours, true, holder.openTimeButton));
        holder.closeTimeButton.setOnClickListener(v -> showTimePicker(hours, false, holder.closeTimeButton));
    }

    private void showTimePicker(DayHours hours, boolean isOpenTime, Button button) {
        String currentTime = isOpenTime ? hours.getOpenTime() : hours.getCloseTime();
        String[] timeParts = currentTime.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        TimePickerDialog picker = new TimePickerDialog(context,
                (view, hourOfDay, minute1) -> {
                    String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute1);
                    if (isOpenTime) {
                        hours.setOpenTime(time);
                    } else {
                        hours.setCloseTime(time);
                    }
                    button.setText(time);
                }, hour, minute, true);
        picker.show();
    }

    @Override
    public int getItemCount() {
        return days.length;
    }

    public Map<String, DayHours> getOperatingHours() {
        return operatingHours;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dayName;
        CheckBox dayCheckbox;
        LinearLayout hoursLayout;
        Button openTimeButton;
        Button closeTimeButton;

        ViewHolder(View view) {
            super(view);
            dayName = view.findViewById(R.id.dayName);
            dayCheckbox = view.findViewById(R.id.dayCheckbox);
            hoursLayout = view.findViewById(R.id.hoursLayout);
            openTimeButton = view.findViewById(R.id.openTimeButton);
            closeTimeButton = view.findViewById(R.id.closeTimeButton);
        }
    }
}
