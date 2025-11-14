package com.contactshandlers.contactinfoall.helper;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.telecom.Call;
import com.contactshandlers.contactinfoall.ui.activity.OutgoingCallActivity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class CallListHelper {
    public static final int MAX_CONFERENCE_PARTICIPANTS = 5;
    public static final int MAX_MULTI_CALLS = 2;

    public enum CallMode {
        SINGLE_CALL,
        MULTI_CALL,
        CONFERENCE
    }

    public enum CallState {
        ACTIVE,
        HOLDING,
        DISCONNECTED
    }

    public static List<Call> mergedCallsList = new ArrayList<>();
    private static final List<Call> regularCallList = new CopyOnWriteArrayList<>();
    private static boolean isInConferenceMode = false;
    private static boolean isNewCallInProgress = false;
    private static boolean isSwapInProgress = false;
    private static Call activeCallForNotification = null;
    private static Call lastActiveCall = null;
    private static int lastCallCount = 0;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static WeakReference<CallStateListener> callStateListenerRef;

    public interface CallStateListener {
        void onCallStateChanged(Call call, int state);

        void onSwapComplete();

        void onActiveCallChanged(Call call);

        void onConferenceStateChanged(boolean isConference);
    }

    private static CallStateListener callStateListener;
    private static final Map<String, Integer> disconnectRetryCounts = new ConcurrentHashMap<>();
    private static final List<Call> activeCalls = new CopyOnWriteArrayList<>();
    private static final List<Call> heldCalls = new CopyOnWriteArrayList<>();
    private static Call activeCall = null;
    private static final Object stateLock = new Object();

    public static void initialize(CallStateListener listener) {
        callStateListener = listener;
        callStateListenerRef = new WeakReference<>(listener);
    }

    public static boolean isInConferenceMode() {
        synchronized (stateLock) {
            if (isInConferenceMode) {
                return true;
            }

            if (!mergedCallsList.isEmpty() && mergedCallsList.size() > 1) {
                return true;
            }

            List<Call> uniqueCalls = getUniqueCallList();
            for (Call call : uniqueCalls) {
                if (call != null && call.getDetails() != null &&
                        call.getDetails().hasProperty(Call.Details.PROPERTY_CONFERENCE)) {
                    return true;
                }
            }

            int activeCallCount = 0;
            for (Call call : uniqueCalls) {
                if (call != null && call.getState() == Call.STATE_ACTIVE) {
                    activeCallCount++;
                    if (activeCallCount > 1) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    public static void setConferenceMode(boolean inConference) {
        synchronized (stateLock) {
            if (!inConference && isInConferenceMode) {
                return;
            }

            if (isInConferenceMode != inConference) {
                isInConferenceMode = inConference;

                notifyModeChange();
            }
        }
    }

    public static boolean isNewCallInProgress() {
        return isNewCallInProgress;
    }

    public static void setNewCallInProgress(boolean inProgress) {
        isNewCallInProgress = inProgress;
    }

    public static boolean isCallListEmpty() {
        return regularCallList.isEmpty();
    }

    public static int getCallListSize() {
        return regularCallList.size() ;
    }

    public static List<Call> getRegularCallList() {
        return new ArrayList<>(regularCallList);
    }

    public static int getTotalCallCount() {
        return regularCallList.size();
    }

    public static boolean canAddMoreCalls() {
        if (isInConferenceMode) {
            return getTotalCallCount() < MAX_CONFERENCE_PARTICIPANTS;
        } else {
            return regularCallList.size() < MAX_MULTI_CALLS;
        }
    }

    public static void addCall(Call call) {
        if (call == null) {
            return;
        }

        String number = getCallNumber(call);
        if (number == null) {
            return;
        }

        if (isDuplicateCall(call) && canAddMoreCalls()) {
            regularCallList.add(call);

            if (isInConferenceMode && !isNewCallInProgress && call.getState() == Call.STATE_ACTIVE && !isIncomingCall(call)) {
                mergedCallsList.add(call);
            }

            updateCallState(call, CallState.ACTIVE);

            if (activeCallForNotification == null || call.getState() == Call.STATE_ACTIVE) {
                Call previousActiveCall = activeCallForNotification;
                activeCallForNotification = call;

                if (previousActiveCall != activeCallForNotification) {
                    if (callStateListener != null) {
                        mainHandler.post(() -> callStateListener.onActiveCallChanged(call));
                    }
                }
            } else {
                updateActiveCallForNotification();
            }

            if (callStateListener != null) {
                mainHandler.post(() -> callStateListener.onCallStateChanged(call, call.getState()));
            }

            try {
                Context context = getContextFromOutgoingCallActivity();
                if (context != null) {

                    if (call.getState() == Call.STATE_ACTIVE) {
                        Intent intent = new Intent(context, com.contactshandlers.contactinfoall.service.CallNotificationService.class);
                        intent.putExtra(com.contactshandlers.contactinfoall.helper.Constants.TITLE, "ACTION_UPDATE_NOTIFICATION");
                        intent.putExtra(com.contactshandlers.contactinfoall.helper.Constants.PHONE_NUMBER, number);
                        intent.putExtra("IS_CALL_RECEIVED", true);

                        if (call.getDetails() != null) {
                            long connectTime = call.getDetails().getConnectTimeMillis();
                            if (connectTime > 0) {
                                intent.putExtra("CONNECT_TIME", connectTime);
                            }
                        }

                        context.startService(intent);
                    } else {
                        updateNotificationOnModeChange(context);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void removeCall(Call call, int n) {
        if (call == null) {
            return;
        }

        String removingNumber = getCallNumber(call);
        if (removingNumber == null) {
            return;
        }

        boolean wasActiveCallForNotification = (call == activeCallForNotification);

        if (!isInConferenceMode) {
            regularCallList.removeIf(c -> removingNumber.equals(getCallNumber(c)));
        }

        for(Call call1 : mergedCallsList){
            if(call1.getDetails().getHandle().getSchemeSpecificPart().equals(call.getDetails().getHandle().getSchemeSpecificPart())){
                mergedCallsList.remove(call1);
                break;
            }
        }

        updateCallState(call, CallState.DISCONNECTED);

        if (isInConferenceMode && regularCallList.isEmpty()) {
            setConferenceMode(false);
        }

        Call previousActiveCall = activeCallForNotification;
        updateActiveCallForNotification();

        if (wasActiveCallForNotification || previousActiveCall != activeCallForNotification) {
            try {
                Context context = getContextFromOutgoingCallActivity();
                if (context != null) {

                        Call newActiveCall = getActiveCallForNotification();
                        if (newActiveCall != null && newActiveCall.getDetails() != null &&
                                newActiveCall.getDetails().getHandle() != null) {

                            String phoneNumber = newActiveCall.getDetails().getHandle().getSchemeSpecificPart();

                            Intent intent = new Intent(context, com.contactshandlers.contactinfoall.service.CallNotificationService.class);
                            intent.putExtra(com.contactshandlers.contactinfoall.helper.Constants.TITLE, "ACTION_UPDATE_NOTIFICATION");
                            intent.putExtra(com.contactshandlers.contactinfoall.helper.Constants.PHONE_NUMBER, phoneNumber);
                            intent.putExtra("IS_CALL_RECEIVED", newActiveCall.getState() == Call.STATE_ACTIVE);

                            if (newActiveCall.getState() == Call.STATE_ACTIVE &&
                                    newActiveCall.getDetails() != null) {
                                long connectTime = newActiveCall.getDetails().getConnectTimeMillis();
                                if (connectTime > 0) {
                                    intent.putExtra("CONNECT_TIME", connectTime);
                                }
                            }

                            context.startService(intent);
                        } else {
                            updateNotificationOnModeChange(context);
                        }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        handleConferenceStateAfterRemoval();
    }

    public static void handleConferenceStateAfterRemoval() {
        synchronized (CallListHelper.class) {
            if (isInConferenceMode) {
                List<Call> uniqueCalls = getUniqueCallList();
                if (uniqueCalls.isEmpty()) {
                    notifyModeChange();
                } else if (uniqueCalls.size() == 1) {
                    isInConferenceMode = false;
                    Call remainingCall = uniqueCalls.get(0);
                    if (remainingCall.getState() != Call.STATE_ACTIVE) {
                        remainingCall.unhold();
                        updateCallState(remainingCall, CallState.ACTIVE);
                    }
                    notifyModeChange();
                }

                OutgoingCallActivity.changeView();
            }
        }
    }

    public static void updateCallState(Call call, CallState state) {
        if (call == null) return;

        String callId = getCallId(call);
        if (callId == null) return;

        String number = getCallNumber(call);

        boolean wasActive = (activeCall == call);
        boolean isBecomingActive = (state == CallState.ACTIVE);

        switch (state) {
            case ACTIVE:
                if (!activeCalls.contains(call)) {
                    activeCalls.add(call);
                }
                heldCalls.remove(call);
                activeCall = call;
                break;
            case HOLDING:
                if (!heldCalls.contains(call)) {
                    heldCalls.add(call);
                }
                activeCalls.remove(call);
                if (activeCall == call) {
                    activeCall = !activeCalls.isEmpty() ? activeCalls.get(0) : null;
                }
                break;
            case DISCONNECTED:
                activeCalls.remove(call);
                heldCalls.remove(call);
                if (activeCall == call) {
                    activeCall = !activeCalls.isEmpty() ? activeCalls.get(0) : null;
                }
                break;
        }

        if (isInConferenceMode) {
            if (state == CallState.DISCONNECTED) {
                regularCallList.remove(call);
            }
        }

        if ((wasActive && activeCall != call) || isBecomingActive) {
            updateActiveCallForNotification();

            try {
                Context context = getContextFromOutgoingCallActivity();
                if (context != null) {

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        updateNotificationOnModeChange(context);
                    }, 100);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static String getCallNumber(Call call) {
        if (call != null && call.getDetails() != null &&
                call.getDetails().getHandle() != null) {
            return call.getDetails().getHandle().getSchemeSpecificPart();
        }
        return null;
    }

    public static String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return null;

        String numberWithoutBrackets = phoneNumber.replaceAll("\\[\\d+]$", "");

        String digitsOnly = numberWithoutBrackets.replaceAll("[^0-9]", "");

        while (digitsOnly.startsWith("0") && digitsOnly.length() > 1) {
            digitsOnly = digitsOnly.substring(1);
        }

        if (digitsOnly.startsWith("91")) {
            digitsOnly = digitsOnly.substring(2);
        }

        if (digitsOnly.length() > 10) {
            digitsOnly = digitsOnly.substring(digitsOnly.length() - 10);
        }

        return digitsOnly;
    }

    private static boolean isDuplicateCall(Call newCall) {
        if (newCall == null || newCall.getDetails() == null) return true;

        String newCallNumber = normalizePhoneNumber(getCallNumber(newCall));
        if (newCallNumber == null) return true;

        for (Call existingCall : regularCallList) {
            String existingNumber = normalizePhoneNumber(getCallNumber(existingCall));
            if (newCallNumber.equals(existingNumber)) {
                return false;
            }
        }

        return true;
    }

    public static List<Call> getUniqueCallList() {
        Map<String, Call> normalizedNumberToCall = new LinkedHashMap<>();

        for (Call call : regularCallList) {
            if (call == null) continue;

            String number = getCallNumber(call);
            if (number == null) continue;

            String normalizedNumber = normalizePhoneNumber(number);
            if (normalizedNumber != null) {
                if (!normalizedNumberToCall.containsKey(normalizedNumber)) {
                    normalizedNumberToCall.put(normalizedNumber, call);
                }
            }
        }

        List<Call> result = new ArrayList<>(normalizedNumberToCall.values());

        for (Map.Entry<String, Call> entry : normalizedNumberToCall.entrySet()) {
            Call call = entry.getValue();
        }

        return result;
    }

    public static Call getActiveCallForNotification() {
        return activeCallForNotification;
    }

    public static boolean hasActiveCallChanged() {
        Call currentActiveCall = getActiveCallForNotification();
        boolean hasChanged = currentActiveCall != lastActiveCall;
        lastActiveCall = currentActiveCall;
        return hasChanged;
    }

    public static boolean hasCallListChanged() {
        int currentCallCount = regularCallList.size();
        boolean hasChanged = currentCallCount != lastCallCount;
        lastCallCount = currentCallCount;
        return hasChanged;
    }

    private static boolean isCallbackSafe() {
        CallStateListener listener = callStateListenerRef != null ? callStateListenerRef.get() : null;
        if (listener == null) return false;

        if (listener instanceof OutgoingCallActivity) {
            OutgoingCallActivity activity = (OutgoingCallActivity) listener;
            return !activity.isDestroyed() && !activity.isFinishing();
        }

        return true;
    }

    public static void updateActiveCallForNotification() {
        Call previousActiveCall = activeCallForNotification;

        List<Call> uniqueCalls = getUniqueCallList();

        Call newActiveCall = null;

        if (isSwapInProgress) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }

            for (Call call : uniqueCalls) {
                int state = call.getState();
                if (state == Call.STATE_ACTIVE) {
                    newActiveCall = call;
                    break;
                }
            }
        } else {
            for (Call call : uniqueCalls) {
                if (call.getState() == Call.STATE_ACTIVE) {
                    newActiveCall = call;
                    break;
                }
            }
        }

        if (newActiveCall == null && !uniqueCalls.isEmpty()) {

            for (Call call : uniqueCalls) {
                if (call.getState() == Call.STATE_DIALING || call.getState() == Call.STATE_CONNECTING) {
                    newActiveCall = call;
                    break;
                }
            }

            if (newActiveCall == null) {
                for (Call call : uniqueCalls) {
                    if (call.getState() == Call.STATE_RINGING) {
                        newActiveCall = call;
                        break;
                    }
                }
            }

            if (newActiveCall == null) {
                for (Call call : uniqueCalls) {
                    if (call.getState() == Call.STATE_HOLDING) {
                        newActiveCall = call;
                        break;
                    }
                }
            }

            if (newActiveCall == null && !uniqueCalls.isEmpty()) {
                newActiveCall = uniqueCalls.get(0);
            }
        }

        if (newActiveCall != previousActiveCall) {
            activeCallForNotification = newActiveCall;

            if (isCallbackSafe()) {
                CallStateListener listener = callStateListenerRef.get();
                if (listener != null) {
                    mainHandler.post(() -> {
                        if (isCallbackSafe()) {
                            try {
                                listener.onActiveCallChanged(activeCallForNotification);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        }
    }

    public static void clearAllCalls() {
        regularCallList.clear();
    }

    public static void swapCalls() {
        if (isSwapInProgress) {
            return;
        }

        List<Call> uniqueCalls = getUniqueCallList();
        if (uniqueCalls.size() != 2) {
            return;
        }

        Call activeCall = null;
        Call heldCall = null;

        for (Call call : uniqueCalls) {
            if (call.getState() == Call.STATE_ACTIVE) {
                activeCall = call;
            } else if (call.getState() == Call.STATE_HOLDING) {
                heldCall = call;
            }
        }

        if (activeCall == null || heldCall == null) {
            return;
        }

        isSwapInProgress = true;

        try {
            performSwapOperations(activeCall, heldCall);
        } catch (Exception e) {
            recoverFromSwapError(activeCall, activeCall, heldCall);
        } finally {
            isSwapInProgress = false;
            updateActiveCallForNotification();
        }
    }

    private static void performSwapOperations(Call activeCall, Call heldCall) throws Exception {
        String activeNumber = getCallNumber(activeCall);
        String heldNumber = getCallNumber(heldCall);

        long activeConnectTime = -1;
        long heldConnectTime = -1;

        if (activeCall.getDetails() != null) {
            activeConnectTime = activeCall.getDetails().getConnectTimeMillis();

            if (activeConnectTime > 0) {
                storeConnectTimeForSwap(activeNumber, activeConnectTime);
            }
        }

        if (heldCall.getDetails() != null) {
            heldConnectTime = heldCall.getDetails().getConnectTimeMillis();
        }

        if (heldConnectTime <= 0 && activeConnectTime > 0) {
            heldConnectTime = activeConnectTime;
            storeConnectTimeForSwap(heldNumber, heldConnectTime);
        }

        activeCallForNotification = heldCall;

        activeCall.hold();
        waitForCallState(activeCall, Call.STATE_HOLDING);

        heldCall.unhold();
        waitForCallState(heldCall, Call.STATE_ACTIVE);

        if (activeCall.getState() == Call.STATE_HOLDING &&
                heldCall.getState() == Call.STATE_ACTIVE) {

            activeCallForNotification = heldCall;

            long newActiveConnectTime = -1;
            if (heldCall.getDetails() != null) {
                newActiveConnectTime = heldCall.getDetails().getConnectTimeMillis();
            }

            if (newActiveConnectTime <= 0) {
                newActiveConnectTime = getStoredConnectTimeForSwap(heldNumber);
                if (newActiveConnectTime > 0) {
                }
            }

            activeCalls.remove(activeCall);
            heldCalls.remove(heldCall);
            activeCalls.add(heldCall);
            heldCalls.add(activeCall);

            if (callStateListener != null) {
                mainHandler.post(() -> {
                    callStateListener.onActiveCallChanged(heldCall);

                    callStateListener.onSwapComplete();

                    try {
                        Class<?> outgoingCallActivityClass = Class.forName("com.contactshandlers.contactinfoall.ui.activity.OutgoingCallActivity");
                        java.lang.reflect.Method onSwapCompleteMethod = outgoingCallActivityClass.getMethod("onSwapComplete");
                        onSwapCompleteMethod.invoke(null);
                    } catch (Exception e) {
                        updateActiveCallForNotification();
                    }
                });
            }
        } else {
            throw new IllegalStateException("Final state verification failed");
        }
    }

    private static void waitForCallState(Call call, int targetState) throws Exception {
        int retryCount = 0;
        while (call.getState() != targetState && retryCount < 3) {
            Thread.sleep(500);
            retryCount++;
        }

        if (call.getState() != targetState) {
            throw new IllegalStateException("Failed to change call state to " + targetState);
        }
    }

    private static void recoverFromSwapError(Call previousActiveCall, Call activeCall, Call heldCall) {
        try {
            if (previousActiveCall != null && previousActiveCall.getState() != Call.STATE_ACTIVE) {
                previousActiveCall.unhold();
                Thread.sleep(500);
            }

            Call otherCall = (previousActiveCall == activeCall) ? heldCall : activeCall;
            if (otherCall != null && otherCall.getState() != Call.STATE_HOLDING) {
                otherCall.hold();
                Thread.sleep(500);
            }

            activeCallForNotification = previousActiveCall;

            if (callStateListener != null) {
                callStateListener.onActiveCallChanged(previousActiveCall);
                mainHandler.postDelayed(() -> callStateListener.onSwapComplete(), 500);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static boolean canSwapCall(Call call) {
        if (isSwapInProgress || call == null) {
            return false;
        }

        List<Call> uniqueCalls = getUniqueCallList();
        if (uniqueCalls.size() != 2) {
            return false;
        }

        boolean hasActiveCall = false;
        boolean hasHeldCall = false;
        for (Call c : uniqueCalls) {
            if (c.getState() == Call.STATE_ACTIVE) {
                if (hasActiveCall) return false;
                hasActiveCall = true;
            } else if (c.getState() == Call.STATE_HOLDING) {
                if (hasHeldCall) return false;
                hasHeldCall = true;
            }
        }

        return hasActiveCall && hasHeldCall && call.getState() == Call.STATE_HOLDING;
    }

    public static void handleIncomingCall(Call call) {
        if (call == null) {
            return;
        }

        String number = getCallNumber(call);

        setNewCallInProgress(true);

        registerCallStateCallback(call);

        if (isDuplicateCall(call) && canAddMoreCalls()) {
            regularCallList.add(call);
        }
    }

    public static void handleOutgoingCall(Call call) {
        if (call == null) {
            return;
        }

        String number = getCallNumber(call);

        setNewCallInProgress(true);

        registerCallStateCallback(call);

        if (isDuplicateCall(call) && canAddMoreCalls()) {
            regularCallList.add(call);
            updateCallState(call, CallState.ACTIVE);
        }
    }

    private static void registerCallStateCallback(Call call) {
        call.registerCallback(new Call.Callback() {
            @Override
            public void onStateChanged(Call call, int state) {
                String number = getCallNumber(call);

                switch (state) {
                    case Call.STATE_ACTIVE:
                        updateCallState(call, CallState.ACTIVE);
                        if (callStateListener != null) {
                            callStateListener.onCallStateChanged(call, state);
                        }
                        break;
                    case Call.STATE_HOLDING:
                        updateCallState(call, CallState.HOLDING);
                        if (callStateListener != null) {
                            callStateListener.onCallStateChanged(call, state);
                        }
                        break;
                    case Call.STATE_DISCONNECTED:
                        removeCall(call, 1);
                        if (callStateListener != null) {
                            callStateListener.onCallStateChanged(call, state);
                        }
                        break;
                    case Call.STATE_DIALING:
                    case Call.STATE_RINGING:
                    case Call.STATE_CONNECTING:
                    case Call.STATE_SELECT_PHONE_ACCOUNT:
                        if (callStateListener != null) {
                            callStateListener.onCallStateChanged(call, state);
                        }
                        break;
                }

                updateActiveCallForNotification();

                try {
                    if (call == activeCallForNotification) {
                        Context context = getContextFromOutgoingCallActivity();
                        if (context != null) {
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                updateNotificationOnModeChange(context);
                            }, 100);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void cleanup() {
        synchronized (CallListHelper.class) {
            regularCallList.clear();
            isInConferenceMode = false;
            isNewCallInProgress = false;
            isSwapInProgress = false;
            activeCallForNotification = null;
            lastActiveCall = null;
            lastCallCount = 0;
            callStateListener = null;
            mergedCallsList.clear();
        }
    }

    public static String getCallId(Call call) {
        if (call == null || call.getDetails() == null) {
            return null;
        }

        Uri handle = call.getDetails().getHandle();
        if (handle == null) {
            return null;
        }

        return handle.toString();
    }

    public static void disconnectConferenceParticipant(Call call) {
        if (call == null) {
            return;
        }

        if (!isInConferenceMode) {
            return;
        }

        String callId = getCallId(call);
        if (callId == null) {
            return;
        }

        try {
            disconnectRetryCounts.put(callId, 0);

            call.disconnect();

            if (callStateListener != null) {
                callStateListener.onCallStateChanged(call, Call.STATE_DISCONNECTED);
            }

            call.registerCallback(new Call.Callback() {
                @Override
                public void onStateChanged(Call call, int state) {
                    super.onStateChanged(call, state);
                    if (state == Call.STATE_DISCONNECTED) {
                        handleSuccessfulDisconnect(call, callId);
                    } else if (state == Call.STATE_HOLDING) {
                        handleFailedDisconnect(call, callId);
                    }
                }
            });

        } catch (Exception e) {
            handleFailedDisconnect(call, callId);
        }
    }

    private static void handleSuccessfulDisconnect(Call call, String callId) {
        removeCall(call, 2);
        updateActiveCallForNotification();
    }

    private static void handleFailedDisconnect(Call call, String callId) {
        int retryCount = disconnectRetryCounts.getOrDefault(callId, 0);
        disconnectRetryCounts.put(callId, retryCount + 1);

        mainHandler.postDelayed(() -> disconnectConferenceParticipant(call), 1000);
    }

    public static CallMode getCurrentCallMode() {
        synchronized (stateLock) {
            if (isInConferenceMode) {
                return CallMode.CONFERENCE;
            }

            List<Call> uniqueCalls = getUniqueCallList();
            if (uniqueCalls.isEmpty()) {
                return CallMode.SINGLE_CALL;
            }

            if (isInConferenceCall(uniqueCalls)) {
                return CallMode.CONFERENCE;
            }

            return uniqueCalls.size() > 1 ? CallMode.MULTI_CALL : CallMode.SINGLE_CALL;
        }
    }

    private static boolean isInConferenceCall(List<Call> uniqueCalls) {
        if (uniqueCalls.isEmpty()) {
            return false;
        }

        for (Call call : uniqueCalls) {
            if (call.getDetails() != null &&
                    call.getDetails().hasProperty(Call.Details.PROPERTY_CONFERENCE)) {
                return true;
            }
        }

        int activeCallCount = 0;
        for (Call call : uniqueCalls) {
            if (call.getState() == Call.STATE_ACTIVE) {
                activeCallCount++;
                if (activeCallCount > 1) {
                    return true;
                }
            }
        }

        return false;
    }

    private static void notifyModeChange() {
        if (callStateListener != null) {
            mainHandler.post(() -> {
                List<Call> calls = getUniqueCallList();
                for (Call call : calls) {
                    callStateListener.onCallStateChanged(call, call.getState());
                }
                callStateListener.onConferenceStateChanged(isInConferenceMode);
            });
        }

        try {
            Context context = getContextFromOutgoingCallActivity();
            if (context != null) {
                updateNotificationOnModeChange(context);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Context getContextFromOutgoingCallActivity() {
        try {
            Class<?> outgoingCallActivityClass = Class.forName("com.contactshandlers.contactinfoall.ui.activity.OutgoingCallActivity");
            java.lang.reflect.Field contextField = outgoingCallActivityClass.getDeclaredField("context");
            contextField.setAccessible(true);
            return (Context) contextField.get(null);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isIncomingCall(Call call) {
        if (call == null || call.getDetails() == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return call.getDetails().getCallDirection() == Call.Details.DIRECTION_INCOMING;
        } else {
            return call.getState() == Call.STATE_RINGING;
        }
    }

    public static void updateSwapNotification(Context context, Call heldCall, Call activeCall) {
        if (context == null || heldCall == null) {
            return;
        }

        String heldNumber = getCallNumber(heldCall);
        if (heldNumber == null || heldNumber.isEmpty()) {
            return;
        }

        activeCallForNotification = heldCall;

        long connectTime = -1;

        if (heldCall.getDetails() != null) {
            connectTime = heldCall.getDetails().getConnectTimeMillis();
            if (connectTime > 0) {
            }
        }

        if (connectTime <= 0) {
            if (activeCall != null && activeCall.getDetails() != null) {
                long activeConnectTime = activeCall.getDetails().getConnectTimeMillis();
                if (activeConnectTime > 0) {
                    connectTime = activeConnectTime;
                } else {
                    connectTime = System.currentTimeMillis();
                }
            } else {
                connectTime = System.currentTimeMillis();
            }
        }

        storeConnectTimeForSwap(heldNumber, connectTime);

        Intent intent = new Intent(context, com.contactshandlers.contactinfoall.service.CallNotificationService.class);
        intent.putExtra(com.contactshandlers.contactinfoall.helper.Constants.TITLE, "ACTION_UPDATE_NOTIFICATION");
        intent.putExtra(com.contactshandlers.contactinfoall.helper.Constants.PHONE_NUMBER, heldNumber);
        intent.putExtra("IS_CALL_RECEIVED", true);
        intent.putExtra("CONNECT_TIME", connectTime);
        intent.putExtra("IS_SWAP_OPERATION", true);

        context.startService(intent);

        final long finalConnectTime = connectTime;

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            updateActiveCallForNotification();

            Call actualActiveCall = null;
            for (Call call : getUniqueCallList()) {
                if (call.getState() == Call.STATE_ACTIVE) {
                    actualActiveCall = call;
                    break;
                }
            }

            if (actualActiveCall != null) {
                String actualNumber = getCallNumber(actualActiveCall);

                long storedConnectTime = getStoredConnectTimeForSwap(actualNumber);
                if (storedConnectTime <= 0) {
                    if (actualActiveCall.getDetails() != null) {
                        storedConnectTime = actualActiveCall.getDetails().getConnectTimeMillis();
                    }
                    if (storedConnectTime <= 0) {
                        storedConnectTime = finalConnectTime;
                    }
                }

                Intent verifyIntent = new Intent(context, com.contactshandlers.contactinfoall.service.CallNotificationService.class);
                verifyIntent.putExtra(com.contactshandlers.contactinfoall.helper.Constants.TITLE, "ACTION_UPDATE_NOTIFICATION");
                verifyIntent.putExtra(com.contactshandlers.contactinfoall.helper.Constants.PHONE_NUMBER, actualNumber);
                verifyIntent.putExtra("IS_CALL_RECEIVED", true);
                verifyIntent.putExtra("CONNECT_TIME", storedConnectTime);
                verifyIntent.putExtra("IS_SWAP_OPERATION", true);

                context.startService(verifyIntent);
            }
        }, 800);
    }

    private static final ConcurrentHashMap<String, Long> swapConnectTimes = new ConcurrentHashMap<>();

    private static void storeConnectTimeForSwap(String phoneNumber, long connectTime) {
        if (phoneNumber == null || phoneNumber.isEmpty() || connectTime <= 0) {
            return;
        }

        String normalizedNumber = normalizePhoneNumber(phoneNumber);
        swapConnectTimes.put(normalizedNumber, connectTime);
    }

    public static long getStoredConnectTimeForSwap(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return -1;
        }

        String normalizedNumber = normalizePhoneNumber(phoneNumber);
        Long connectTime = swapConnectTimes.get(normalizedNumber);

        if (connectTime != null) {
            return connectTime;
        }

        return -1;
    }

    public static void updateNotificationOnModeChange(Context context) {
        if (context == null) {
            return;
        }

        CallMode currentMode = getCurrentCallMode();

        List<Call> uniqueCalls = getUniqueCallList();
        if (uniqueCalls.isEmpty()) {
            return;
        }

        updateActiveCallForNotification();

        Intent intent = new Intent(context, com.contactshandlers.contactinfoall.service.CallNotificationService.class);

        switch (currentMode) {
            case CONFERENCE:
                intent.putExtra(Constants.TITLE, "ACTION_CONFERENCE_NOTIFICATION");
                intent.putExtra("IS_CALL_RECEIVED", true);
                intent.putExtra("PARTICIPANT_COUNT", uniqueCalls.size());
                break;

            case MULTI_CALL:
            case SINGLE_CALL:
                Call activeCall = getActiveCallForNotification();
                if (activeCall != null && activeCall.getDetails() != null &&
                        activeCall.getDetails().getHandle() != null) {

                    String phoneNumber = activeCall.getDetails().getHandle().getSchemeSpecificPart();

                    long connectTime = -1;
                    if (activeCall.getState() == Call.STATE_ACTIVE && activeCall.getDetails() != null) {
                        connectTime = activeCall.getDetails().getConnectTimeMillis();
                    }

                    intent.putExtra(Constants.TITLE, "ACTION_UPDATE_NOTIFICATION");
                    intent.putExtra(Constants.PHONE_NUMBER, phoneNumber);
                    intent.putExtra("IS_CALL_RECEIVED", activeCall.getState() == Call.STATE_ACTIVE);

                    if (connectTime > 0) {
                        intent.putExtra("CONNECT_TIME", connectTime);
                    }

                    intent.putExtra("CALL_MODE", currentMode.name());
                } else if (!uniqueCalls.isEmpty()) {
                    Call firstCall = uniqueCalls.get(0);
                    if (firstCall.getDetails() != null && firstCall.getDetails().getHandle() != null) {
                        String phoneNumber = firstCall.getDetails().getHandle().getSchemeSpecificPart();

                        intent.putExtra(Constants.TITLE, "ACTION_UPDATE_NOTIFICATION");
                        intent.putExtra(Constants.PHONE_NUMBER, phoneNumber);
                        intent.putExtra("IS_CALL_RECEIVED", firstCall.getState() == Call.STATE_ACTIVE);
                        intent.putExtra("CALL_MODE", currentMode.name());
                    }
                }
                break;
        }

        context.startService(intent);
    }
}