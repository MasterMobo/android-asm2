package com.example.blooddono;

import android.app.Application;
import com.example.blooddono.utils.NotificationUtils;

public class BloodDonoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize notification channels
        NotificationUtils.createNotificationChannels(this);
    }
}