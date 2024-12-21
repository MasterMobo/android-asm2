package com.example.blooddono.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.blooddono.MainActivity;
import com.example.blooddono.R;
import com.example.blooddono.models.Donation;

public class NotificationUtils {
    private static final String DONATION_CHANNEL_ID = "donation_notifications";
    private static final int NOTIFICATION_ID = 1;

    public static void createNotificationChannels(Context context) {
        // Create notification channels for Android 8.0 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel donationChannel = new NotificationChannel(
                    DONATION_CHANNEL_ID,
                    "Donation Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            donationChannel.setDescription("Notifications for new blood donations");

            // Register the channel with the system
            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(donationChannel);
        }
    }

    public static void sendDonationNotification(Context context, Donation donation) {
        // Create an explicit intent for the MainActivity
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Create pending intent to launch when notification is tapped
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, DONATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)  // You'll need to add this icon
                .setContentTitle("New Blood Donation Registration")
                .setContentText("Donor " + donation.getDonorName() + " has registered to donate " +
                        String.join(", ", donation.getBloodTypes()))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // Show the notification
        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(NOTIFICATION_ID, builder.build());
            Log.d("NotificationUtils", "Notification sent!");
        } catch (SecurityException e) {
            // Handle case where notification permission is not granted
            Log.e("NotificationUtils", e.getMessage());
            e.printStackTrace();
        }
    }
}