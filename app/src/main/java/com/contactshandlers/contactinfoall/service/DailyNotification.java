package com.contactshandlers.contactinfoall.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.helper.SharedPreferencesManager;
import com.contactshandlers.contactinfoall.ui.activity.MainActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class DailyNotification {
    private static final String CHANNEL_ID = "daily_channel";
    private static final String CHANNEL_NAME = "Daily Notifications";
    private static final int NOTIFICATION_ID = 100;
    private static final String WORK_TAG = "alternate_day_notification";
    private static final String PREF_NAME = "notification_prefs";
    private static final String KEY_LAST_NOTIFICATION_DATE = "last_notification_date";
    private static final String KEY_NEXT_NOTIFICATION_TIME = "next_notification_time";
    private static final String KEY_FIRST_TIME_SETUP = "first_time_setup";
    private static final String KEY_INSTALLATION_DATE = "installation_date";
    private static final String KEY_NOTIFICATION_DATA_LIST = "notification_data_list";
    private static final String KEY_CURRENT_NOTIFICATION_INDEX = "current_notification_index";
    private static final String KEY_FIREBASE_DATA_FETCHED = "firebase_data_fetched";
    private static final String KEY_LAST_USED_INDEX = "last_used_index";
    private boolean isShowNotification;
    private Context context;
    private SharedPreferences prefs;
    private DatabaseReference databaseReference;

    public DailyNotification(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.databaseReference = FirebaseDatabase.getInstance().getReference("ads_data/notifications");
        isShowNotification = SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_SHOW_NOTIFICATION, false);
    }

    public void initializeNotificationScheduling() {
        if (isShowNotification) {
            long currentTime = System.currentTimeMillis();

            if (prefs.getLong(KEY_INSTALLATION_DATE, 0) == 0) {
                prefs.edit().putLong(KEY_INSTALLATION_DATE, currentTime).apply();
            }

            fetchFirebaseNotificationData();

            boolean isFirstTime = prefs.getBoolean(KEY_FIRST_TIME_SETUP, true);
            long scheduledTime = prefs.getLong(KEY_NEXT_NOTIFICATION_TIME, 0);

            if (isFirstTime) {
                scheduleForTomorrow();
                prefs.edit().putBoolean(KEY_FIRST_TIME_SETUP, false).apply();

            } else if (scheduledTime == 0) {
                scheduleNextNotification();

            } else if (scheduledTime <= currentTime) {
                scheduleNextNotification();

            } else {
                ensureWorkManagerScheduled(scheduledTime);
            }
        }
    }

    private void fetchFirebaseNotificationData() {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> notificationTexts = new ArrayList<>();

                if (dataSnapshot.exists()) {
                    for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                        String text = childSnapshot.getValue(String.class);
                        if (text != null && !text.trim().isEmpty()) {
                            notificationTexts.add(text);
                        }
                    }
                }

                saveNotificationDataToPreferences(notificationTexts);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                List<String> defaultTexts = new ArrayList<>();
                saveNotificationDataToPreferences(defaultTexts);
            }
        });
    }

    private void saveNotificationDataToPreferences(List<String> notificationTexts) {
        if (notificationTexts.isEmpty()) {
            return;
        }

        Gson gson = new Gson();
        String json = gson.toJson(notificationTexts);

        boolean isFirstTime = !prefs.getBoolean(KEY_FIREBASE_DATA_FETCHED, false);
        int currentIndex = isFirstTime ? 0 : prefs.getInt(KEY_CURRENT_NOTIFICATION_INDEX, 0);

        if (currentIndex >= notificationTexts.size()) {
            currentIndex = 0;
        }

        prefs.edit()
                .putString(KEY_NOTIFICATION_DATA_LIST, json)
                .putBoolean(KEY_FIREBASE_DATA_FETCHED, true)
                .putInt(KEY_CURRENT_NOTIFICATION_INDEX, currentIndex)
                .apply();
    }

    private List<String> getNotificationDataFromPreferences() {
        String json = prefs.getString(KEY_NOTIFICATION_DATA_LIST, "");
        if (json.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            Gson gson = new Gson();
            Type type = new TypeToken<List<String>>() {
            }.getType();
            List<String> result = gson.fromJson(json, type);
            return result != null ? result : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String getNextNotificationText() {
        List<String> notificationTexts = getNotificationDataFromPreferences();

        if (notificationTexts.isEmpty()) {
            return "Don't forget to check your contacts!";
        }

        int currentIndex = prefs.getInt(KEY_CURRENT_NOTIFICATION_INDEX, 0);

        if (currentIndex >= notificationTexts.size()) {
            currentIndex = 0;
        }

        String currentText = notificationTexts.get(currentIndex);

        int nextIndex = (currentIndex + 1) % notificationTexts.size();

        prefs.edit()
                .putInt(KEY_CURRENT_NOTIFICATION_INDEX, nextIndex)
                .putInt(KEY_LAST_USED_INDEX, currentIndex)
                .apply();

        return currentText;
    }

    private void scheduleForTomorrow() {
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DAY_OF_YEAR, 1);

        long scheduledTime = setRandomTime(tomorrow);
        saveScheduledTime(scheduledTime);
        scheduleWorkManager(scheduledTime);
    }

    public void scheduleNextNotification() {
        Calendar nextDay = calculateNextAlternateDay();

        long scheduledTime = setRandomTime(nextDay);
        saveScheduledTime(scheduledTime);
        scheduleWorkManager(scheduledTime);
    }

    private Calendar calculateNextAlternateDay() {
        Calendar nextDay = Calendar.getInstance();
        String lastNotificationDate = prefs.getString(KEY_LAST_NOTIFICATION_DATE, "");

        if (!lastNotificationDate.isEmpty()) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date lastDate = dateFormat.parse(lastNotificationDate);

                Calendar lastNotificationCal = Calendar.getInstance();
                assert lastDate != null;
                lastNotificationCal.setTime(lastDate);

                lastNotificationCal.add(Calendar.DAY_OF_YEAR, 2);

                Calendar now = Calendar.getInstance();
                if (lastNotificationCal.before(now)) {
                    nextDay.add(Calendar.DAY_OF_YEAR, 1);
                } else {
                    nextDay = lastNotificationCal;
                }

            } catch (Exception e) {
                nextDay.add(Calendar.DAY_OF_YEAR, 2);
            }
        } else {
            nextDay.add(Calendar.DAY_OF_YEAR, 2);
        }

        return nextDay;
    }

    private long setRandomTime(Calendar calendar) {
        Random random = new Random();
        int randomHour = random.nextInt(24);
        int randomMinute = random.nextInt(60);

        calendar.set(Calendar.HOUR_OF_DAY, randomHour);
        calendar.set(Calendar.MINUTE, randomMinute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTimeInMillis();
    }

    private void saveScheduledTime(long scheduledTime) {
        prefs.edit().putLong(KEY_NEXT_NOTIFICATION_TIME, scheduledTime).apply();
    }

    private void scheduleWorkManager(long scheduledTime) {
        long currentTime = System.currentTimeMillis();
        long delay = scheduledTime - currentTime;

        if (delay <= 0) {
            scheduleNextNotification();
            return;
        }

        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG);

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(false)
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false)
                .build();

        Data inputData = new Data.Builder()
                .putLong("scheduled_time", scheduledTime)
                .putString("work_tag", WORK_TAG)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(AlternateDayWorker.class)
                .setConstraints(constraints)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag(WORK_TAG)
                .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_TAG, ExistingWorkPolicy.REPLACE, workRequest);
    }

    private void ensureWorkManagerScheduled(long scheduledTime) {
        long currentTime = System.currentTimeMillis();
        long delay = scheduledTime - currentTime;

        if (delay > 0) {
            scheduleWorkManager(scheduledTime);
        } else {
            scheduleNextNotification();
        }
    }

    public void cancelAllNotifications() {
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG);
        prefs.edit()
                .remove(KEY_NEXT_NOTIFICATION_TIME)
                .remove(KEY_LAST_NOTIFICATION_DATE)
                .putBoolean(KEY_FIRST_TIME_SETUP, true)
                .apply();
    }

    public static class AlternateDayWorker extends Worker {
        public AlternateDayWorker(@NonNull Context context, @NonNull WorkerParameters params) {
            super(context, params);
        }

        @NonNull
        @Override
        public Result doWork() {
            Context context = getApplicationContext();

            try {
                SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

                if (wasNotificationSentToday(context)) {
                    scheduleNextNotification(context);
                    return Result.success();
                }

                boolean isNotificationEnabled = SharedPreferencesManager.getInstance()
                        .getBooleanValue(Constants.IS_SHOW_NOTIFICATION, false);

                String message = getNextNotificationMessage(context);

                if (message == null) {
                    return Result.retry();
                }

                if (isNotificationEnabled) {
                    showNotification(context, message);
                }

                markNotificationSentToday(context);
                prefs.edit().remove(KEY_NEXT_NOTIFICATION_TIME).apply();
                scheduleNextNotification(context);

                return Result.success();
            } catch (Exception e) {
                return Result.retry();
            }
        }

        private boolean wasNotificationSentToday(Context context) {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String lastDate = prefs.getString(KEY_LAST_NOTIFICATION_DATE, "");
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            return today.equals(lastDate);
        }

        private void markNotificationSentToday(Context context) {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            prefs.edit().putString(KEY_LAST_NOTIFICATION_DATE, today).apply();
        }

        private void scheduleNextNotification(Context context) {
            DailyNotification notification = new DailyNotification(context);
            notification.scheduleNextNotification();
        }

        private String getNextNotificationMessage(Context context) {
            DailyNotification dailyNotification = new DailyNotification(context);
            return dailyNotification.getNextNotificationText();
        }

        private void showNotification(Context context, String message) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }

                NotificationManager notificationManager =
                        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

                if (notificationManager == null) {
                    return;
                }

                createNotificationChannel(notificationManager);

                Intent intent = new Intent(context, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                PendingIntent pendingIntent = PendingIntent.getActivity(
                        context, NOTIFICATION_ID, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification_logo)
                        .setContentTitle(context.getString(R.string.app_name))
                        .setColorized(true)
                        .setColor(context.getColor(R.color.main))
                        .setContentText(message)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_REMINDER)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .setShowWhen(true)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

                notificationManager.notify(NOTIFICATION_ID, builder.build());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void createNotificationChannel(NotificationManager notificationManager) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for alternate days");
            channel.enableVibration(true);
            channel.setShowBadge(true);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(channel);
        }
    }
}