package com.contactshandlers.contactinfoall.helper;

import android.content.Context;

import com.contactshandlers.contactinfoall.service.DailyNotification;

public class NotificationManagerHelper {
    
    private Context context;
    private DailyNotification dailyNotification;
    
    public NotificationManagerHelper(Context context) {
        this.context = context;
        this.dailyNotification = new DailyNotification(context);
    }

    public void initializeOnAppStart() {
        dailyNotification.initializeNotificationScheduling();
    }
}