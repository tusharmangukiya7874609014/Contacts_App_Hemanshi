package com.contactshandlers.contactinfoall.service;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.DisconnectCause;
import android.telecom.InCallService;

import androidx.core.content.ContextCompat;

import com.contactshandlers.contactinfoall.helper.CallListHelper;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.helper.SharedPreferencesManager;
import com.contactshandlers.contactinfoall.ui.activity.OutgoingCallActivity;

import java.util.HashMap;
import java.util.Map;

public class MyInCallService extends InCallService {

    public static Call currentCall;
    public static InCallService inCallService;
    private static Map<String, CallInfo> callTracker = new HashMap<>();
    private Handler handler = new Handler(Looper.getMainLooper());

    private static class CallInfo {
        String phoneNumber;
        boolean wasRinging = false;
        boolean wasActive = false;
        long timestamp;

        CallInfo(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            this.timestamp = System.currentTimeMillis();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        inCallService = this;
    }

    @Override
    public void onDestroy() {
        inCallService = null;
        super.onDestroy();
    }

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        String callId = call.toString();
        String phoneNumber = getPhoneNumber(call);

        if (SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_UNKNOWN, false)) {
            if (phoneNumber == null || phoneNumber.equals("unknown") || phoneNumber.equals("private") || phoneNumber.isEmpty()) {
                call.disconnect();
                return;
            }
        }

        currentCall = call;
        inCallService = this;

        if (!checkPermissions()) {
            return;
        }

        CallInfo callInfo = new CallInfo(phoneNumber);
        callTracker.put(callId, callInfo);

        if (call.getDetails().hasProperty(Call.Details.PROPERTY_CONFERENCE)) {
            handleConferenceCall(call);
        } else {
            call.registerCallback(new Call.Callback() {
                @Override
                public void onStateChanged(Call call, int state) {
                    super.onStateChanged(call, state);
                    handleCallStateChange(call, state, callId);
                }
            });

            handleCallStateChange(call, call.getState(), callId);
        }
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED;
    }

    private String getPhoneNumber(Call call) {
        if (call != null && call.getDetails() != null && call.getDetails().getHandle() != null) {
            return call.getDetails().getHandle().getSchemeSpecificPart();
        }
        return null;
    }

    private void handleConferenceCall(Call call) {

        call.registerCallback(new Call.Callback() {
            @Override
            public void onStateChanged(Call call, int state) {
                super.onStateChanged(call, state);
                handleConferenceCallState(call, state);
            }

            @Override
            public void onDetailsChanged(Call call, Call.Details details) {
                super.onDetailsChanged(call, details);
                safeUpdateUI();
            }
        });

        synchronized (CallListHelper.class) {
            if (!CallListHelper.isInConferenceMode()) {
                CallListHelper.clearAllCalls();
            }
            CallListHelper.addCall(call);
            CallListHelper.setConferenceMode(true);
        }
    }

    private void handleCallStateChange(Call call, int state, String callId) {
        String phoneNumber = getPhoneNumber(call);
        CallInfo callInfo = callTracker.get(callId);

        if (callInfo == null || phoneNumber == null) {
            return;
        }

        switch (state) {
            case Call.STATE_DIALING:
            case Call.STATE_CONNECTING:
                callInfo.wasActive = true;
                CallListHelper.addCall(call);

                Intent intent = new Intent(this, OutgoingCallActivity.class);
                intent.putExtra(Constants.PHONE_NUMBER, phoneNumber);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

                handler.postDelayed(() -> {
                    Intent serviceIntent = new Intent(MyInCallService.this, CallNotificationService.class);
                    serviceIntent.putExtra(Constants.PHONE_NUMBER, phoneNumber);
                    serviceIntent.putExtra(Constants.TITLE, "OUTGOING_CALL");
                    startService(serviceIntent);
                }, 100);
                break;

            case Call.STATE_RINGING:
                callInfo.wasRinging = true;

                handler.postDelayed(() -> {
                    Intent serviceIntent = new Intent(MyInCallService.this, CallNotificationService.class);
                    serviceIntent.putExtra(Constants.PHONE_NUMBER, phoneNumber);
                    serviceIntent.putExtra(Constants.TITLE, "INCOMING_CALL");
                    startService(serviceIntent);
                }, 100);
                break;

            case Call.STATE_ACTIVE:
                callInfo.wasActive = true;
                if (CallListHelper.getUniqueCallList().size() >= 2) {
                    OutgoingCallActivity.updateButtonStates();
                }
                CallListHelper.addCall(call);
                safeUpdateUI();
                break;

            case Call.STATE_DISCONNECTED:
                DisconnectCause disconnectCause = call.getDetails().getDisconnectCause();
                int code = disconnectCause.getCode();
                boolean missedCall = callInfo.wasRinging && !callInfo.wasActive
                        && (code == DisconnectCause.MISSED || code == DisconnectCause.REMOTE);
                if (missedCall) {
                    Intent serviceIntent = new Intent(MyInCallService.this, CallNotificationService.class);
                    serviceIntent.putExtra(Constants.PHONE_NUMBER, phoneNumber);
                    serviceIntent.putExtra(Constants.TITLE, "MISSED_CALL");
                    startService(serviceIntent);
                }

                synchronized (CallListHelper.class) {
                    CallListHelper.removeCall(call, 5);
                    if (CallListHelper.getUniqueCallList().isEmpty()) {
                        CallListHelper.clearAllCalls();
                    }
                }


                handler.postDelayed(() -> callTracker.remove(callId), 1000);

                safeUpdateUI();
                break;
        }
    }

    private void handleConferenceCallState(Call call, int state) {
        switch (state) {
            case Call.STATE_ACTIVE:
                CallListHelper.addCall(call);
                safeUpdateUI();
                break;
            case Call.STATE_DISCONNECTED:
                synchronized (CallListHelper.class) {
                    CallListHelper.removeCall(call, 4);
                    if (CallListHelper.getUniqueCallList().isEmpty()) {
                        CallListHelper.clearAllCalls();
                        CallListHelper.setConferenceMode(false);
                    }
                }
                safeUpdateUI();
                break;
        }
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        if (currentCall == call) {
            currentCall = null;
        }
        safeUpdateUI();
    }

    private void safeUpdateUI() {
        try {
            OutgoingCallActivity.changeView();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void holdCall(Call mCall) {
        if (mCall != null && inCallService != null) {
            mCall.hold();
        }
    }

    public static void unholdCall(Call mCall) {
        if (mCall != null && inCallService != null) {
            mCall.unhold();
        }
    }

    public static void muteCall(boolean isMuted) {
        if (inCallService != null) {
            inCallService.setMuted(isMuted);
        }
    }

    public static void speakerCall(boolean isSpeakerOn) {
        if (inCallService != null) {
            int route = isSpeakerOn ? CallAudioState.ROUTE_SPEAKER : CallAudioState.ROUTE_EARPIECE;
            inCallService.setAudioRoute(route);
        }
    }
}