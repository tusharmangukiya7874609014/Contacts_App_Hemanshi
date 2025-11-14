package com.contactshandlers.contactinfoall.helper;

import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog;
import android.telephony.PhoneNumberUtils;

import com.contactshandlers.contactinfoall.model.CallHistory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CallHistoryHelper {

    public static List<CallHistory> getCallLogsForContactWithPagination(Context context, String phoneNumber, int offset, int limit) {
        List<CallHistory> callLogList = new ArrayList<>();
        int matchedCount = 0;
        int skippedCount = 0;

        Cursor cursor = context.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                new String[]{
                        CallLog.Calls.NUMBER,
                        CallLog.Calls.TYPE,
                        CallLog.Calls.DATE,
                        CallLog.Calls.DURATION
                },
                null,
                null,
                CallLog.Calls.DATE + " DESC"
        );

        if (cursor != null) {
            while (cursor.moveToNext() && matchedCount < limit) {
                String number = cursor.getString(0);

                if (PhoneNumberUtils.compare(number, phoneNumber)) {
                    if (skippedCount < offset) {
                        skippedCount++;
                        continue;
                    }

                    int callType = cursor.getInt(1);
                    long date = cursor.getLong(2);
                    long duration = cursor.getLong(3);

                    String formattedDate = formatDate(date);
                    String formattedDuration = formatDuration(duration);
                    String callTypeString = getCallTypeString(callType);

                    callLogList.add(new CallHistory(callTypeString, formattedDate, formattedDuration, duration));
                    matchedCount++;
                }
            }
            cursor.close();
        }

        return callLogList;
    }

    public static List<CallHistory> getCallLogsForContact(Context context, String phoneNumber, int limit) {
        List<CallHistory> callLogList = new ArrayList<>();
        int recordCount = 0;

        Cursor cursor = context.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                new String[]{
                        CallLog.Calls.NUMBER,
                        CallLog.Calls.TYPE,
                        CallLog.Calls.DATE,
                        CallLog.Calls.DURATION
                },
                null,
                null,
                CallLog.Calls.DATE + " DESC"
        );

        if (cursor != null) {
            while (cursor.moveToNext() && recordCount < limit) {
                String number = cursor.getString(0);

                if (PhoneNumberUtils.compare(number, phoneNumber)) {
                    int callType = cursor.getInt(1);
                    long date = cursor.getLong(2);
                    long duration = cursor.getLong(3);

                    String formattedDate = formatDate(date);
                    String formattedDuration = formatDuration(duration);
                    String callTypeString = getCallTypeString(callType);

                    callLogList.add(new CallHistory(callTypeString, formattedDate, formattedDuration, duration));
                    recordCount++;
                }
            }
            cursor.close();
        }

        return callLogList;
    }

    private static String getCallTypeString(int callType) {
        switch (callType) {
            case CallLog.Calls.INCOMING_TYPE:
                return "Incoming";
            case CallLog.Calls.OUTGOING_TYPE:
                return "Outgoing";
            case CallLog.Calls.MISSED_TYPE:
                return "Missed Call";
            case CallLog.Calls.REJECTED_TYPE:
                return "Rejected";
            default:
                return "Unknown";
        }
    }

    private static String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a");
        return sdf.format(new Date(timestamp));
    }

    private static String formatDuration(long duration) {
        if (duration == 0) return "";

        long hours = duration / 3600;
        long minutes = (duration % 3600) / 60;
        long seconds = duration % 60;

        StringBuilder formattedTime = new StringBuilder();

        if (hours > 0) {
            formattedTime.append(hours).append("h ");
        }
        if (minutes > 0) {
            formattedTime.append(minutes).append("m ");
        }
        if (seconds > 0 || formattedTime.length() == 0) {
            formattedTime.append(seconds).append("s");
        }

        return formattedTime.toString().trim();
    }
}