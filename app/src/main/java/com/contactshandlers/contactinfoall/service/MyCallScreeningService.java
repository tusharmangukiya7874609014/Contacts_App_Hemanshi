package com.contactshandlers.contactinfoall.service;

import android.os.Build;
import android.telecom.Call;
import android.telecom.CallScreeningService;
import android.telecom.Connection;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.Q)
public class MyCallScreeningService extends CallScreeningService {

    @Override
    public void onScreenCall(@NonNull Call.Details callDetails) {

        boolean isIncoming = callDetails.getCallDirection() == Call.Details.DIRECTION_INCOMING;

        if (isIncoming) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (callDetails.getCallerNumberVerificationStatus() == Connection.VERIFICATION_STATUS_FAILED) {
                    respondToCall(callDetails, new CallResponse.Builder()
                            .setRejectCall(true)
                            .setDisallowCall(true)
                            .setSilenceCall(true)
                            .setSkipNotification(true)
                            .setSkipCallLog(true)
                            .build());
                } else {
                    CallResponse.Builder response = new CallResponse.Builder();
                    response.setRejectCall(false);
                    response.setDisallowCall(false);
                    response.setSilenceCall(false);
                    response.setSkipCallLog(false);
                    response.setSkipNotification(false);
                    respondToCall(callDetails, response.build());
                }
            } else {
                CallResponse.Builder response = new CallResponse.Builder();
                response.setRejectCall(false);
                response.setDisallowCall(false);
                response.setSilenceCall(false);
                response.setSkipCallLog(false);
                response.setSkipNotification(false);
                respondToCall(callDetails, response.build());
            }
        }
    }
}
