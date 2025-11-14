package com.contactshandlers.contactinfoall.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.room.ReminderDatabase;
import com.contactshandlers.contactinfoall.ui.activity.MainActivity;

public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String message = intent.getStringExtra(Constants.REMINDER_MESSAGE);
        String title = intent.getStringExtra(Constants.TITLE);
        long reminderId = intent.getLongExtra(Constants.REMINDER_ID, -1);

        showNotification(context, title, message);

        if (reminderId != -1) {
            deleteReminderFromDatabase(context, reminderId);
        }
    }

    private void showNotification(Context context, String title, String message) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel(
                "reminder_channel",
                "Reminder",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Reminders for contacts");
        notificationManager.createNotificationChannel(channel);

        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.putExtra(Constants.OPEN_FRAGMENT, 1);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(),
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "reminder_channel")
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setSmallIcon(R.drawable.ic_reminder)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void deleteReminderFromDatabase(Context context, long reminderId) {
        new Thread(() -> {
            try {
                ReminderDatabase db = ReminderDatabase.getInstance(context);
                db.reminderDao().deleteReminderById(reminderId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}