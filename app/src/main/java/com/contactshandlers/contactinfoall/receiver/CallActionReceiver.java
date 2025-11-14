package com.contactshandlers.contactinfoall.receiver;

import static android.content.Context.TELECOM_SERVICE;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.telecom.Call;
import android.telecom.TelecomManager;

import androidx.core.app.ActivityCompat;

import com.contactshandlers.contactinfoall.helper.CallListHelper;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.helper.SharedPreferencesManager;
import com.contactshandlers.contactinfoall.service.CallNotificationService;
import com.contactshandlers.contactinfoall.ui.activity.CallBackActivity;
import com.contactshandlers.contactinfoall.ui.activity.OutgoingCallActivity;

import java.lang.reflect.Method;
import java.util.List;

public class CallActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        assert action != null;
        switch (action) {
            case "ACTION_ANSWER_CALL": {
                String phoneNumber = intent.getStringExtra(Constants.PHONE_NUMBER);
                TelecomManager telecomManager = (TelecomManager) context.getSystemService(TELECOM_SERVICE);
                if (telecomManager != null && ActivityCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                    telecomManager.acceptRingingCall();
                    Intent updateIntent = new Intent(context, CallNotificationService.class);
                    updateIntent.putExtra(Constants.PHONE_NUMBER, phoneNumber);
                    updateIntent.putExtra(Constants.TITLE, "ACTION_UPDATE_NOTIFICATION");
                    updateIntent.putExtra("IS_CALL_RECEIVED", true);
                    try {
                        context.startForegroundService(updateIntent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                Intent closeIntent = new Intent("ACTION_CLOSE_CALL_UI");
                context.sendBroadcast(closeIntent);
                break;
            }
            case "ACTION_DECLINE_CALL": {
                String phoneNumber = intent.getStringExtra(Constants.PHONE_NUMBER);
                Intent intent1 = new Intent(context, CallNotificationService.class);
                intent1.putExtra(Constants.PHONE_NUMBER, phoneNumber);
                intent1.putExtra(Constants.TITLE, "ACTION_STOP_RINGTONE");
                try {
                    context.startForegroundService(intent1);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Call incomingCall = null;
                List<Call> uniqueCalls = CallListHelper.getUniqueCallList();
                for (Call call : uniqueCalls) {
                    if (call.getDetails() != null &&
                            call.getDetails().getHandle() != null &&
                            call.getDetails().getHandle().getSchemeSpecificPart().equals(phoneNumber)) {
                        incomingCall = call;
                        break;
                    }
                }

                if (incomingCall != null) {
                    try {
                        incomingCall.reject(false, null);
                        CallListHelper.removeCall(incomingCall, 3);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    TelecomManager telecomManager = (TelecomManager) context.getSystemService(TELECOM_SERVICE);
                    if (telecomManager != null) {
                        try {
                            Method endCallMethod = telecomManager.getClass().getDeclaredMethod("endCall");
                            endCallMethod.setAccessible(true);
                            endCallMethod.invoke(telecomManager);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                Call activeCall = null;
                for (Call call : CallListHelper.getUniqueCallList()) {
                    if (call.getState() == Call.STATE_ACTIVE) {
                        activeCall = call;
                        break;
                    }
                }

                if (activeCall == null) {
                    for (Call call : CallListHelper.getUniqueCallList()) {
                        if (call.getState() == Call.STATE_HOLDING) {
                            activeCall = call;
                            try {
                                call.unhold();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        }
                    }
                }

                boolean hasActiveCalls = activeCall != null;
                if (!hasActiveCalls) {
                    context.stopService(new Intent(context, CallNotificationService.class));
                } else {
                    String activeNumber = activeCall.getDetails().getHandle().getSchemeSpecificPart();

                    Intent updateIntent = new Intent(context, CallNotificationService.class);
                    updateIntent.putExtra(Constants.TITLE, "ACTION_UPDATE_NOTIFICATION");
                    updateIntent.putExtra(Constants.PHONE_NUMBER, activeNumber);
                    updateIntent.putExtra("IS_CALL_RECEIVED", activeCall.getState() == Call.STATE_ACTIVE);
                    try {
                        context.startForegroundService(updateIntent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_CALL_BACK_SCREEN, true)) {
                    Intent callbackIntent = new Intent(context, CallBackActivity.class);
                    callbackIntent.putExtra(Constants.PHONE_NUMBER, phoneNumber);
                    callbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    context.startActivity(callbackIntent);
                }

                Intent closeIntent = new Intent("ACTION_CLOSE_CALL_UI");
                context.sendBroadcast(closeIntent);
                break;
            }
            case "ACTION_HANGUP_CALL": {
                TelecomManager telecomManager = (TelecomManager) context.getSystemService(TELECOM_SERVICE);
                if (telecomManager != null && telecomManager.isInCall() && ActivityCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                    telecomManager.endCall();
                }
                if (CallListHelper.getUniqueCallList().isEmpty()) {
                    context.stopService(new Intent(context, CallNotificationService.class));
                    OutgoingCallActivity.finishActivity();
                }
                break;
            }
        }
    }
}