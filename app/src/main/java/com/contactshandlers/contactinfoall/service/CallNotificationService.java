package com.contactshandlers.contactinfoall.service;

import android.Manifest;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.telecom.Call;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.helper.CallListHelper;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.helper.IncomingCallDialog;
import com.contactshandlers.contactinfoall.helper.SharedPreferencesManager;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.helper.XiaomiPermissionManager;
import com.contactshandlers.contactinfoall.receiver.CallActionReceiver;
import com.contactshandlers.contactinfoall.ui.activity.CallBackActivity;
import com.contactshandlers.contactinfoall.ui.activity.IncomingCallActivity;
import com.contactshandlers.contactinfoall.ui.activity.MainActivity;
import com.contactshandlers.contactinfoall.ui.activity.OutgoingCallActivity;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class CallNotificationService extends Service {
    private static final String CHANNEL_ID = "call_notification_channel";
    public static final int NOTIFICATION_ID = 1;
    private MediaPlayer ringtonePlayer;
    private Vibrator vibrator;
    private Handler autoEndHandler = new Handler();
    private long callStartTime = 0;
    private boolean isCallAnswered = false;
    private String lastAction = null;
    private long lastNotificationTime = 0;
    private long lastConferenceNotificationTime = 0;
    private static final long NOTIFICATION_THROTTLE_MS = 1000;
    private Handler handler = new Handler(Looper.getMainLooper());
    private IncomingCallDialog dialog;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    private boolean canDrawOverlays() {
        return Settings.canDrawOverlays(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        String callerNumber = intent.getStringExtra(Constants.PHONE_NUMBER);
        String action = intent.getStringExtra(Constants.TITLE);
        boolean isCallReceived = intent.getBooleanExtra("IS_CALL_RECEIVED", false);
        long providedConnectTime = intent.getLongExtra("CONNECT_TIME", -1);
        boolean isSwapOperation = intent.getBooleanExtra("IS_SWAP_OPERATION", false);

        if (!isSwapOperation) {
            long currentTime = System.currentTimeMillis();
            if (action != null) {
                if (!action.equals("ACTION_CONFERENCE_NOTIFICATION")) {
                    if (action.equals(lastAction) &&
                            (currentTime - lastNotificationTime) < NOTIFICATION_THROTTLE_MS) {
                        return START_STICKY;
                    }
                    lastAction = action;
                    lastNotificationTime = currentTime;
                } else {
                    if (action.equals(lastAction) &&
                            (currentTime - lastConferenceNotificationTime) < NOTIFICATION_THROTTLE_MS) {
                        return START_STICKY;
                    }
                    lastAction = action;
                    lastConferenceNotificationTime = currentTime;
                }
            }
        }

        try {
            if (action != null) {
                startForeground(NOTIFICATION_ID, createInitialNotification());
            }
        } catch (Exception e) {
            e.printStackTrace();
            stopSelf();
            return START_NOT_STICKY;
        }

        if (callerNumber != null && !callerNumber.isEmpty()) {
            String callerName = Utils.getContactNameByNumber(this, callerNumber);

            Context context = this;
            if (action != null && !action.isEmpty()) {
                switch (action) {
                    case "INCOMING_CALL":
                        autoEndHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                startService(new Intent(context, CallNotificationService.class)
                                        .putExtra(Constants.TITLE, "MISSED_CALL"));
                            }
                        }, 30000);
                        showIncomingCall(callerName != null ? callerName : callerNumber, callerNumber);
                        Uri customRingtone = getRingtoneUriForNumber(this, callerNumber);

                        try {
                            if (canDrawOverlays()) {
                                dialog = new IncomingCallDialog(this, callerNumber);
                                if (dialog.getWindow() != null) {
                                    dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
                                    dialog.show();
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        playRingtone(customRingtone);
                        break;
                    case "ACTION_UPDATE_NOTIFICATION":
                        if (isSwapOperation && providedConnectTime > 0) {
                            isCallAnswered = true;
                            callStartTime = providedConnectTime;
                        } else if (isCallReceived && !isCallAnswered) {
                            isCallAnswered = true;
                            callStartTime = providedConnectTime > 0 ? providedConnectTime : System.currentTimeMillis();
                        } else if (isCallReceived && providedConnectTime > 0) {
                            boolean shouldUpdateTime = false;

                            if (callStartTime <= 0 || Math.abs(callStartTime - providedConnectTime) > 2000) {
                                shouldUpdateTime = true;
                            }

                            if (shouldUpdateTime) {
                                callStartTime = providedConnectTime;
                            }
                        }

                        updateNotificationToHangUp(callerName != null ? callerName : callerNumber, callerNumber);
                        stopRingtone();
                        break;
                    case "OUTGOING_CALL":
                        if (isCallReceived && !isCallAnswered) {
                            isCallAnswered = true;
                            callStartTime = providedConnectTime > 0 ? providedConnectTime : System.currentTimeMillis();
                        } else if (isCallReceived && providedConnectTime > 0) {
                            if (callStartTime <= 0 || Math.abs(callStartTime - providedConnectTime) > 2000) {
                                callStartTime = providedConnectTime;
                            }
                        }
                        OutGoingNotificationToHangUp(callerName != null ? callerName : callerNumber, callerNumber);
                        break;
                    case "ACTION_STOP_RINGTONE":
                        stopRingtone();
                        break;
                    case "ACTION_SILENT_RINGTONE":
                        silentRingtone(callerName != null ? callerName : callerNumber, callerNumber);
                        break;
                    case "MISSED_CALL":
                        stopRingtone();
                        isCallAnswered = false;
                        callStartTime = 0;
                        stopService(new Intent(context, CallNotificationService.class));
                        handler.postDelayed(() -> {
                            if (SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_CALL_BACK_SCREEN, true)) {
                                Intent callbackIntent = new Intent(context, CallBackActivity.class);
                                callbackIntent.putExtra(Constants.PHONE_NUMBER, callerNumber);
                                callbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                context.startActivity(callbackIntent);
                            }
                            showMissedCallNotification(callerName != null ? callerName : callerNumber);
                        }, 500);

                        break;
                }
            }
        } else {
            if (action != null && !action.isEmpty()) {
                List<Call> uniqueCalls = CallListHelper.getUniqueCallList();
                long earliestConnectTime = Long.MAX_VALUE;
                boolean hasActiveCall = false;
                for (Call call : uniqueCalls) {
                    if (call.getState() == Call.STATE_ACTIVE) {
                        hasActiveCall = true;
                        long connectTime = call.getDetails().getConnectTimeMillis();
                        if (connectTime > 0 && connectTime < earliestConnectTime) {
                            earliestConnectTime = connectTime;
                        }
                    }
                }

                if (action.equals("ACTION_CONFERENCE_NOTIFICATION")) {
                    isCallAnswered = isCallReceived || hasActiveCall;

                    if (isCallAnswered && hasActiveCall && earliestConnectTime != Long.MAX_VALUE) {
                        callStartTime = earliestConnectTime;
                    } else if (isCallAnswered && !hasActiveCall) {
                        callStartTime = System.currentTimeMillis();
                    }

                    int participantCount = intent.getIntExtra("PARTICIPANT_COUNT", CallListHelper.mergedCallsList.size());

                    showConferenceCallNotification(participantCount);
                }
            }
        }

        return START_STICKY;
    }

    private Notification createInitialNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_contact)
                .setContentText("Initializing call...")
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
    }

    private void playRingtone(Uri ringtoneUri) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            int ringerMode = audioManager.getRingerMode();

            if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                if (ringtoneUri == null) {
                    ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                }

                try {
                    ringtonePlayer = new MediaPlayer();
                    ringtonePlayer.setAudioStreamType(AudioManager.STREAM_RING);
                    ringtonePlayer.setDataSource(this, ringtoneUri);
                    ringtonePlayer.setLooping(true);
                    ringtonePlayer.prepare();
                    ringtonePlayer.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if ((ringerMode == AudioManager.RINGER_MODE_VIBRATE || ringerMode == AudioManager.RINGER_MODE_NORMAL)) {
                vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null && vibrator.hasVibrator()) {
                    long[] pattern = {0, 500, 1000};
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
                }
            }
        }
    }

    public Uri getRingtoneUriForNumber(Context context, String number) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        Uri ringtoneUri = null;

        Cursor cursor = context.getContentResolver().query(
                uri,
                new String[]{ContactsContract.Contacts.CUSTOM_RINGTONE},
                null,
                null,
                null
        );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                String ringtone = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.CUSTOM_RINGTONE));
                if (ringtone != null) {
                    ringtoneUri = Uri.parse(ringtone);
                }
            }
            cursor.close();
        }

        return ringtoneUri;
    }

    private void stopRingtone() {
        autoEndHandler.removeCallbacksAndMessages(null);
        if (ringtonePlayer != null) {
            if (ringtonePlayer.isPlaying()) ringtonePlayer.stop();
            ringtonePlayer.release();
            ringtonePlayer = null;
        }
        if (vibrator != null) vibrator.cancel();
        if (dialog != null) dialog.dismiss();
    }

    private void silentRingtone(String callerName, String callerNumber) {
        if (ringtonePlayer != null) {
            if (ringtonePlayer.isPlaying()) ringtonePlayer.stop();
            ringtonePlayer.release();
            ringtonePlayer = null;
        }
        if (vibrator != null) vibrator.cancel();
        if (dialog != null) dialog.dismiss();
        showIncomingCallNotificationForScreenOff(callerName, callerNumber);
    }

    private void createNotificationChannel() {
        NotificationChannel incomingChannel = new NotificationChannel(
                CHANNEL_ID + "_incoming",
                "Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
        );
        incomingChannel.setDescription("Incoming call notifications");
        incomingChannel.setShowBadge(true);
        incomingChannel.enableLights(true);
        incomingChannel.enableVibration(true);
        incomingChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        incomingChannel.setBypassDnd(true);

        NotificationChannel ongoingChannel = new NotificationChannel(
                CHANNEL_ID,
                "Call Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        ongoingChannel.setDescription("Ongoing call notifications");
        ongoingChannel.setShowBadge(true);
        ongoingChannel.enableLights(false);
        ongoingChannel.enableVibration(false);
        ongoingChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);

        notificationManager.createNotificationChannel(incomingChannel);
        notificationManager.createNotificationChannel(ongoingChannel);
    }

    public void showIncomingCall(String callerName, String callerNumber) {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

        boolean isScreenOff = powerManager != null && !powerManager.isInteractive();
        boolean isLocked = keyguardManager != null && keyguardManager.isDeviceLocked();
        boolean isInBackground = isAppInBackground();

        if (isScreenOff || isLocked) {
            if (XiaomiPermissionManager.isXiaomiDevice()) {
                wakeAndUnlockScreen();
                showIncomingCallNotificationOnly(callerName, callerNumber);
            } else {
                showIncomingCallNotificationForScreenOff(callerName, callerNumber);
            }
            try {
                Intent intent = new Intent(this, IncomingCallActivity.class);
                intent.putExtra(Constants.PHONE_NUMBER, callerNumber);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } else {
            showIncomingCallNotificationOnly(callerName, callerNumber);
        }
    }

    private void wakeAndUnlockScreen() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK |
                        PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.ON_AFTER_RELEASE,
                "myapp:WakeLock");
        wakeLock.acquire(3000);

        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager != null) {
            KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("myapp:KeyguardLock");
            keyguardLock.disableKeyguard();
        }
    }


    private boolean isAppInBackground() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) return true;

        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) return true;

        final String packageName = getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    appProcess.processName.equals(packageName)) {
                return false;
            }
        }
        return true;
    }

    private void updateNotificationToHangUp(String callerName, String callerNumber) {
        CallListHelper.CallMode callMode = CallListHelper.getCurrentCallMode();
        boolean isMultiCall = callMode == CallListHelper.CallMode.MULTI_CALL;

        Call activeCall = CallListHelper.getActiveCallForNotification();

        if (isMultiCall && activeCall != null && activeCall.getDetails() != null &&
                activeCall.getDetails().getHandle() != null) {

            String activeNumber = activeCall.getDetails().getHandle().getSchemeSpecificPart();

            if (!callerNumber.equals(activeNumber) && activeCall.getState() == Call.STATE_ACTIVE) {
                callerNumber = activeNumber;
                callerName = Utils.getContactNameByNumber(this, callerNumber);
                if (callerName == null || callerName.isEmpty()) {
                    callerName = activeNumber;
                }
            }
        }

        Person.Builder personBuilder = new Person.Builder()
                .setName(callerName)
                .setImportant(true);

        Uri photoUri = getContactPhotoUri(callerNumber);

        if (photoUri != null) {
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);
                if (bitmap != null) {
                    Bitmap circularBitmap = getCircularBitmap(bitmap);
                    IconCompat icon = IconCompat.createWithBitmap(circularBitmap);
                    personBuilder.setIcon(icon);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Person caller = personBuilder.build();

        Intent hangUpIntent = new Intent(this, CallActionReceiver.class);
        hangUpIntent.setAction("ACTION_HANGUP_CALL");
        hangUpIntent.putExtra(Constants.PHONE_NUMBER, callerNumber);
        PendingIntent hangUpPendingIntent = PendingIntent.getBroadcast(this, 2, hangUpIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent ongoingCallIntent = new Intent(this, OutgoingCallActivity.class);
        ongoingCallIntent.putExtra(Constants.PHONE_NUMBER, callerNumber);
        ongoingCallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                this,
                3,
                ongoingCallIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = createNotificationBuilder(caller, contentPendingIntent, hangUpPendingIntent);

        boolean isCallOnHold = false;
        boolean isCallActive = false;
        long connectTime = 0;

        List<Call> calls = CallListHelper.getUniqueCallList();
        Call targetCall = null;

        for (Call call : calls) {
            if (call != null && call.getDetails() != null && call.getDetails().getHandle() != null) {
                String callNumber = call.getDetails().getHandle().getSchemeSpecificPart();
                if (callerNumber.equals(callNumber)) {
                    targetCall = call;
                    break;
                }
            }
        }

        if (targetCall != null) {
            isCallOnHold = targetCall.getState() == Call.STATE_HOLDING;
            isCallActive = targetCall.getState() == Call.STATE_ACTIVE;

            if (isCallActive) {
                connectTime = targetCall.getDetails().getConnectTimeMillis();
                if (connectTime > 0 && (!isCallAnswered || callStartTime == 0)) {
                    isCallAnswered = true;
                    callStartTime = connectTime;
                }
            }

            if (isCallOnHold) {
                builder.setContentText("On Hold");
                builder.setUsesChronometer(false);
            } else if (isCallActive && isCallAnswered && callStartTime > 0) {
                builder.setWhen(callStartTime)
                        .setUsesChronometer(true)
                        .setChronometerCountDown(false);
            } else {
                builder.setContentText("Call in progress");
                builder.setUsesChronometer(false);
            }
        }

        startForeground(NOTIFICATION_ID, builder.build());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(this, OutgoingCallActivity.class);
            intent.putExtra(Constants.PHONE_NUMBER, callerNumber);
            intent.putExtra(Constants.IS_INCOMING, true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    private void OutGoingNotificationToHangUp(String callerName, String callerNumber) {
        CallListHelper.CallMode callMode = CallListHelper.getCurrentCallMode();
        boolean isMultiCall = callMode == CallListHelper.CallMode.MULTI_CALL;

        Call activeCall = CallListHelper.getActiveCallForNotification();

        if (isMultiCall && activeCall != null && activeCall.getDetails() != null &&
                activeCall.getDetails().getHandle() != null) {

            String activeNumber = activeCall.getDetails().getHandle().getSchemeSpecificPart();

            if (!callerNumber.equals(activeNumber) && activeCall.getState() == Call.STATE_ACTIVE) {
                callerNumber = activeNumber;
                callerName = Utils.getContactNameByNumber(this, callerNumber);
                if (callerName == null || callerName.isEmpty()) {
                    callerName = activeNumber;
                }
            }
        }

        Person.Builder personBuilder = new Person.Builder()
                .setName(callerName)
                .setImportant(true);

        Uri photoUri = getContactPhotoUri(callerNumber);

        if (photoUri != null) {
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);
                if (bitmap != null) {
                    Bitmap circularBitmap = getCircularBitmap(bitmap);
                    IconCompat icon = IconCompat.createWithBitmap(circularBitmap);
                    personBuilder.setIcon(icon);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Person caller = personBuilder.build();

        Intent hangUpIntent = new Intent(this, CallActionReceiver.class);
        hangUpIntent.setAction("ACTION_HANGUP_CALL");
        hangUpIntent.putExtra(Constants.PHONE_NUMBER, callerNumber);
        PendingIntent hangUpPendingIntent = PendingIntent.getBroadcast(this, 2, hangUpIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent ongoingCallIntent = new Intent(this, OutgoingCallActivity.class);
        ongoingCallIntent.putExtra(Constants.PHONE_NUMBER, callerNumber);
        ongoingCallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                this,
                3,
                ongoingCallIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = createNotificationBuilder(caller, contentPendingIntent, hangUpPendingIntent);

        boolean isCallOnHold = false;
        boolean isCallActive = false;
        long connectTime = 0;

        if (isMultiCall) {
            List<Call> calls = CallListHelper.getUniqueCallList();
            Call targetCall = null;

            for (Call call : calls) {
                if (call != null && call.getDetails() != null && call.getDetails().getHandle() != null) {
                    String callNumber = call.getDetails().getHandle().getSchemeSpecificPart();
                    if (callerNumber.equals(callNumber)) {
                        targetCall = call;
                        break;
                    }
                }
            }

            if (targetCall != null) {
                isCallOnHold = targetCall.getState() == Call.STATE_HOLDING;
                isCallActive = targetCall.getState() == Call.STATE_ACTIVE;

                if (isCallActive) {
                    connectTime = targetCall.getDetails().getConnectTimeMillis();
                    if (connectTime > 0 && (!isCallAnswered || callStartTime == 0)) {
                        isCallAnswered = true;
                        callStartTime = connectTime;
                    }
                }

            }
        } else if (callerNumber != null && !callerNumber.equals("Conference")) {
            if (activeCall != null && activeCall.getDetails() != null &&
                    activeCall.getDetails().getHandle() != null &&
                    callerNumber.equals(activeCall.getDetails().getHandle().getSchemeSpecificPart())) {

                isCallOnHold = activeCall.getState() == Call.STATE_HOLDING;
                isCallActive = activeCall.getState() == Call.STATE_ACTIVE;

                if (isCallActive) {
                    connectTime = activeCall.getDetails().getConnectTimeMillis();
                    if (connectTime > 0 && (!isCallAnswered || callStartTime == 0)) {
                        isCallAnswered = true;
                        callStartTime = connectTime;
                    }
                }
            } else {
                List<Call> calls = CallListHelper.getUniqueCallList();
                for (Call call : calls) {
                    if (call != null && call.getDetails() != null && call.getDetails().getHandle() != null) {
                        String callNumber = call.getDetails().getHandle().getSchemeSpecificPart();
                        if (callerNumber.equals(callNumber)) {
                            isCallOnHold = call.getState() == Call.STATE_HOLDING;
                            isCallActive = call.getState() == Call.STATE_ACTIVE;

                            if (isCallActive) {
                                connectTime = call.getDetails().getConnectTimeMillis();
                                if (connectTime > 0 && (!isCallAnswered || callStartTime == 0)) {
                                    isCallAnswered = true;
                                    callStartTime = connectTime;
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }

        if (isCallOnHold) {
            builder.setContentText("On Hold");
            builder.setUsesChronometer(false);
        } else if (isCallActive && isCallAnswered && callStartTime > 0) {
            builder.setWhen(callStartTime)
                    .setUsesChronometer(true)
                    .setChronometerCountDown(false);
        } else if (!isCallActive && !isCallOnHold) {
            builder.setContentText("Dialing...");
            builder.setUsesChronometer(false);
        }

        startForeground(NOTIFICATION_ID, builder.build());
    }

    private void showIncomingCallNotificationForScreenOff(String callerName, String callerNumber) {
        Person.Builder personBuilder = new Person.Builder()
                .setName(callerName)
                .setImportant(true);

        Uri photoUri = getContactPhotoUri(callerNumber);
        if (photoUri != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);
                if (bitmap != null) {
                    Bitmap circularBitmap = getCircularBitmap(bitmap);
                    IconCompat icon = IconCompat.createWithBitmap(circularBitmap);
                    personBuilder.setIcon(icon);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Person caller = personBuilder.build();

        Intent answerIntent = new Intent(this, CallActionReceiver.class);
        answerIntent.setAction("ACTION_ANSWER_CALL");
        answerIntent.putExtra(Constants.PHONE_NUMBER, callerNumber);
        PendingIntent answerPendingIntent = PendingIntent.getBroadcast(this, 0, answerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent declineIntent = new Intent(this, CallActionReceiver.class);
        declineIntent.setAction("ACTION_DECLINE_CALL");
        declineIntent.putExtra(Constants.PHONE_NUMBER, callerNumber);
        PendingIntent declinePendingIntent = PendingIntent.getBroadcast(this, 1, declineIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_contact)
                .setContentTitle("Incoming Call")
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .setAutoCancel(false)
                .setStyle(NotificationCompat.CallStyle.forIncomingCall(caller, declinePendingIntent, answerPendingIntent))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setVibrate(new long[0]);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void showIncomingCallNotificationOnly(String callerName, String callerNumber) {
        Person.Builder personBuilder = new Person.Builder()
                .setName(callerName)
                .setImportant(true);

        Uri photoUri = getContactPhotoUri(callerNumber);
        if (photoUri != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);
                if (bitmap != null) {
                    Bitmap circularBitmap = getCircularBitmap(bitmap);
                    IconCompat icon = IconCompat.createWithBitmap(circularBitmap);
                    personBuilder.setIcon(icon);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Person caller = personBuilder.build();

        Intent answerIntent = new Intent(this, CallActionReceiver.class);
        answerIntent.setAction("ACTION_ANSWER_CALL");
        answerIntent.putExtra(Constants.PHONE_NUMBER, callerNumber);
        PendingIntent answerPendingIntent = PendingIntent.getBroadcast(this, 0, answerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent declineIntent = new Intent(this, CallActionReceiver.class);
        declineIntent.setAction("ACTION_DECLINE_CALL");
        declineIntent.putExtra(Constants.PHONE_NUMBER, callerNumber);
        PendingIntent declinePendingIntent = PendingIntent.getBroadcast(this, 1, declineIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent fullScreenIntent = new Intent(this, IncomingCallActivity.class);
        fullScreenIntent.putExtra(Constants.PHONE_NUMBER, callerNumber);
        fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(this, 2, fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID + "_incoming")
                .setSmallIcon(R.drawable.ic_contact)
                .setContentTitle("Incoming Call")
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setAutoCancel(false)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setStyle(NotificationCompat.CallStyle.forIncomingCall(caller, declinePendingIntent, answerPendingIntent))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVibrate(new long[0]);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private NotificationCompat.Builder createNotificationBuilder(Person caller, PendingIntent contentPendingIntent, PendingIntent hangUpPendingIntent) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_contact)
                .setContentText("Ongoing Call")
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .setContentIntent(contentPendingIntent)
                .setStyle(NotificationCompat.CallStyle.forOngoingCall(caller, hangUpPendingIntent))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true);

        if (isCallAnswered && callStartTime > 0) {
            builder.setWhen(callStartTime)
                    .setUsesChronometer(true)
                    .setChronometerCountDown(false);
        } else {
            builder.setWhen(0)
                    .setUsesChronometer(false)
                    .setChronometerCountDown(false);
        }

        return builder;
    }

    private void showConferenceCallNotification(int participantCount) {
        Person caller = new Person.Builder()
                .setName("Conference Call")
                .setImportant(true)
                .build();

        Intent hangUpIntent = new Intent(this, CallActionReceiver.class);
        hangUpIntent.setAction("ACTION_HANGUP_CALL");
        PendingIntent hangUpPendingIntent = PendingIntent.getBroadcast(this, 2, hangUpIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent ongoingCallIntent = new Intent(this, OutgoingCallActivity.class);
        ongoingCallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                this, 3, ongoingCallIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_contact)
                .setContentText("Conference Call (" + participantCount + " participants)")
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
                .setContentIntent(contentPendingIntent)
                .setStyle(NotificationCompat.CallStyle.forOngoingCall(caller, hangUpPendingIntent))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true);

        boolean allCallsOnHold = true;
        boolean hasActiveCall = false;
        long earliestConnectTime = Long.MAX_VALUE;

        List<Call> calls = CallListHelper.getUniqueCallList();

        if (!calls.isEmpty()) {
            for (Call call : calls) {
                if (call.getState() == Call.STATE_ACTIVE) {
                    allCallsOnHold = false;
                    hasActiveCall = true;

                    if (call.getDetails() != null) {
                        long connectTime = call.getDetails().getConnectTimeMillis();
                        if (connectTime > 0 && connectTime < earliestConnectTime) {
                            earliestConnectTime = connectTime;
                        }
                    }
                }
            }
        } else {
            allCallsOnHold = false;
        }

        if (allCallsOnHold) {
            builder.setContentText("Conference On Hold (" + participantCount + " participants)");
            builder.setUsesChronometer(false);
        } else if (hasActiveCall && earliestConnectTime != Long.MAX_VALUE) {
            if (!isCallAnswered || callStartTime == 0 || callStartTime > earliestConnectTime) {
                isCallAnswered = true;
                callStartTime = earliestConnectTime;
            }

            builder.setWhen(callStartTime)
                    .setUsesChronometer(true)
                    .setChronometerCountDown(false);
        } else if (isCallAnswered && callStartTime > 0) {
            builder.setWhen(callStartTime)
                    .setUsesChronometer(true)
                    .setChronometerCountDown(false);
        } else {
            builder.setContentText("Conference Call (" + participantCount + " participants)");
            builder.setUsesChronometer(false);
        }

        startForeground(NOTIFICATION_ID, builder.build());
    }

    private void showMissedCallNotification(String callerName) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 100, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_missed)
                .setContentTitle("Missed Call")
                .setContentText(callerName)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private Uri getContactPhotoUri(String phoneNumber) {
        Uri photoUri = null;
        try {
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                Uri lookupUri = Uri.withAppendedPath(
                        android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                        Uri.encode(phoneNumber));

                String[] projection = new String[]{
                        android.provider.ContactsContract.PhoneLookup._ID,
                        android.provider.ContactsContract.PhoneLookup.PHOTO_URI
                };

                try (Cursor cursor = getContentResolver().query(
                        lookupUri, projection, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        String photoUriString = cursor.getString(
                                cursor.getColumnIndexOrThrow(android.provider.ContactsContract.PhoneLookup.PHOTO_URI));
                        if (photoUriString != null) {
                            photoUri = Uri.parse(photoUriString);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return photoUri;
    }

    public static Bitmap getCircularBitmap(Bitmap bitmap) {
        int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(output);
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, size, size);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, null, rect, paint);

        return output;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        autoEndHandler.removeCallbacksAndMessages(null);
        isCallAnswered = false;
        callStartTime = 0;
        lastAction = null;
        lastNotificationTime = 0;
        lastConferenceNotificationTime = 0;
        if (dialog != null) dialog.dismiss();
        super.onDestroy();
    }
}