package com.example.blooddono.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.media.RingtoneManager;
import android.net.Uri;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.blooddono.MainActivity;
import com.example.blooddono.R;
import com.example.blooddono.models.Donation;
import com.example.blooddono.models.DonationSite;
import com.example.blooddono.models.User;

public class NotificationUtils {
    public static final String DONATION_CHANNEL_ID = "donation_notifications";
    private static final int NOTIFICATION_ID = 1;

    public static void createNotificationChannels(Context context) {
        // Create notification channels for Android 8.0 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel donationChannel = new NotificationChannel(
                    DONATION_CHANNEL_ID,
                    "Donation Notifications",
                    NotificationManager.IMPORTANCE_HIGH  // Changed to HIGH importance
            );
            donationChannel.setDescription("Notifications for new blood donations");
            donationChannel.enableVibration(true);
            donationChannel.enableLights(true);
            donationChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

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

        // Get default notification sound
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, DONATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("New Blood Donation Registration")
                .setContentText("Donor " + donation.getDonorName() + " has registered to donate " +
                        String.join(", ", donation.getBloodTypes()))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Donor " + donation.getDonorName() + " has registered to donate " +
                                String.join(", ", donation.getBloodTypes())))
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Set high priority
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setVibrate(new long[]{0, 500, 200, 500}) // Vibration pattern
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent);

        // Show the notification
        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(NOTIFICATION_ID, builder.build());
            Log.d("NotificationUtils", "Notification sent!");
        } catch (SecurityException e) {
            // Handle case where notification permission is not granted
            Log.e("NotificationUtils", "Error sending notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void sendVolunteerNotification(Context context, DonationSite site, User volunteer) {
        createNotificationChannels(context); // Ensure channel exists

        // Create an explicit intent for the MainActivity
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, DONATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("New Volunteer Registration")
                .setContentText(volunteer.getFullName() + " has registered as a volunteer at " + site.getName())
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(volunteer.getFullName() + " has registered as a volunteer at " + site.getName()))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setVibrate(new long[]{0, 500, 200, 500})
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
            Log.d("NotificationUtils", "Volunteer notification sent!");
        } catch (SecurityException e) {
            Log.e("NotificationUtils", "Error sending volunteer notification: " + e.getMessage());
            e.printStackTrace();
        }
    }
}