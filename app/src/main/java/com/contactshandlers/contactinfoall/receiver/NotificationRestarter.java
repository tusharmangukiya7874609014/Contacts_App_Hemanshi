package com.contactshandlers.contactinfoall.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.contactshandlers.contactinfoall.helper.NotificationManagerHelper;

public class NotificationRestarter extends BroadcastReceiver {

    private NotificationManagerHelper notificationHelper;

    @Override
    public void onReceive(Context context, Intent intent) {
        notificationHelper = new NotificationManagerHelper(context);
        notificationHelper.initializeOnAppStart();
    }
}
