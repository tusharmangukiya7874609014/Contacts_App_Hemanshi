package com.contactshandlers.contactinfoall.helper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.text.TextUtils;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.model.CallLogItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class CallLogHelper {

    public static Map<String, ArrayList<CallLogItem>> getCallLogsWithLimit(Context context, int limit, int offset) {
        Map<String, ArrayList<CallLogItem>> groupedLogs = new LinkedHashMap<>();
        Map<String, String> contactMap = getNormalizedContactMap(context);

        String[] projection = {
                CallLog.Calls.NUMBER,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.TYPE
        };

        Cursor cursor = null;

        try {
            Uri uri = CallLog.Calls.CONTENT_URI.buildUpon()
                    .appendQueryParameter("limit", String.valueOf(limit))
                    .appendQueryParameter("offset", String.valueOf(offset))
                    .build();

            cursor = context.getContentResolver().query(
                    uri,
                    projection,
                    null,
                    null,
                    CallLog.Calls.DATE + " DESC"
            );

            if (cursor != null) {;
                int processedCount = 0;

                while (cursor.moveToNext() && processedCount < limit) {
                    String rawNumber = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER));
                    long dateMillis = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE));
                    int duration = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION));
                    int callType = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE));

                    if (rawNumber == null || rawNumber.trim().isEmpty()) {
                        continue;
                    }

                    String normalizedNumber = normalizeNumber(rawNumber);
                    String name = contactMap.getOrDefault(normalizedNumber, rawNumber);

                    String section = getFormattedDate(dateMillis, context);
                    String time = getFormattedTime(dateMillis, context);
                    String formattedDuration = getFormattedDuration(callType, duration);
                    String callStatus = getCallStatus(callType);

                    CallLogItem callLogItem = new CallLogItem(name, rawNumber, callStatus, formattedDuration, time, 1);

                    if (!groupedLogs.containsKey(section)) {
                        groupedLogs.put(section, new ArrayList<>());
                    }

                    ArrayList<CallLogItem> callList = groupedLogs.get(section);
                    assert callList != null;

                    if (callList.isEmpty() || !canMergeWithLastItem(callList.get(callList.size() - 1), callLogItem)) {
                        callList.add(callLogItem);
                    } else {
                        CallLogItem lastItem = callList.get(callList.size() - 1);
                        lastItem.setCount(lastItem.getCount() + 1);

                        if (callType == CallLog.Calls.INCOMING_TYPE || callType == CallLog.Calls.OUTGOING_TYPE) {
                            String combinedDuration = combineDurations(lastItem.getDuration(), formattedDuration);
                            lastItem.setDuration(combinedDuration);
                        }
                    }

                    processedCount++;
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return groupedLogs;
    }

    public static Map<String, ArrayList<CallLogItem>> getCallLogs(Context context) {
        Map<String, ArrayList<CallLogItem>> groupedLogs = new LinkedHashMap<>();
        Map<String, String> contactMap = getNormalizedContactMap(context);

        String[] projection = {
                CallLog.Calls.NUMBER,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.TYPE
        };

        String sortOrder = CallLog.Calls.DATE + " DESC ";

        Cursor cursor = context.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
        );

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    String rawNumber = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER));
                    long dateMillis = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE));
                    int duration = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION));
                    int callType = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE));

                    if (rawNumber == null || rawNumber.trim().isEmpty()) continue;

                    String normalizedNumber = normalizeNumber(rawNumber);
                    String name = contactMap.getOrDefault(normalizedNumber, rawNumber);

                    String section = getFormattedDate(dateMillis, context);
                    String time = getFormattedTime(dateMillis, context);
                    String formattedDuration = getFormattedDuration(callType, duration);
                    String callStatus = getCallStatus(callType);

                    CallLogItem callLogItem = new CallLogItem(name, rawNumber, callStatus, formattedDuration, time, 1);

                    if (!groupedLogs.containsKey(section)) {
                        groupedLogs.put(section, new ArrayList<>());
                    }

                    ArrayList<CallLogItem> callList = groupedLogs.get(section);
                    assert callList != null;

                    if (callList.isEmpty() || !canMergeWithLastItem(callList.get(callList.size() - 1), callLogItem)) {
                        callList.add(callLogItem);
                    } else {
                        CallLogItem lastItem = callList.get(callList.size() - 1);
                        lastItem.setCount(lastItem.getCount() + 1);

                        if (callType == CallLog.Calls.INCOMING_TYPE || callType == CallLog.Calls.OUTGOING_TYPE) {
                            String combinedDuration = combineDurations(lastItem.getDuration(), formattedDuration);
                            lastItem.setDuration(combinedDuration);
                        }
                    }
                }
            } finally {
                cursor.close();
            }
        }
        return groupedLogs;
    }

    public static Map<String, ArrayList<CallLogItem>> getFilterCallLogs(Context context) {
        Map<String, ArrayList<CallLogItem>> groupedLogs = new LinkedHashMap<>();

        Map<String, String> contactMap = getNormalizedContactMap(context);

        String[] projection = {
                CallLog.Calls.NUMBER,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.TYPE
        };

        Cursor cursor = context.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                CallLog.Calls.DATE + " DESC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String rawNumber = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER));
                long dateMillis = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE));
                int duration = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION));
                int callType = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE));

                if (rawNumber == null || rawNumber.trim().isEmpty()) continue;

                String normalizedNumber = normalizeNumber(rawNumber);
                String name = contactMap.getOrDefault(normalizedNumber, rawNumber);

                String section = getFormattedDate(dateMillis, context);
                String time = getFormattedTime(dateMillis, context);
                String formattedDuration = getFormattedDuration(callType, duration);
                String callStatus = getCallStatus(callType);

                CallLogItem callLogItem = new CallLogItem(name, rawNumber, callStatus, formattedDuration, time, 1);

                if (!groupedLogs.containsKey(section)) {
                    groupedLogs.put(section, new ArrayList<>());
                }

                ArrayList<CallLogItem> callList = groupedLogs.get(section);
                boolean found = false;
                if (callList != null) {
                    for (CallLogItem existingItem : callList) {
                        if (TextUtils.equals(existingItem.getName(), name)) {
                            existingItem.setCount(existingItem.getCount() + 1);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        callList.add(callLogItem);
                    }
                }
            }
            cursor.close();
        }

        return groupedLogs;
    }

    private static boolean canMergeWithLastItem(CallLogItem lastItem, CallLogItem currentItem) {
        return lastItem.getCallType().equals(currentItem.getCallType()) &&
                normalizeNumber(lastItem.getNumber()).equals(normalizeNumber(currentItem.getNumber()));
    }

    private static String combineDurations(String duration1, String duration2) {
        try {
            int seconds1 = parseDurationToSeconds(duration1);
            int seconds2 = parseDurationToSeconds(duration2);

            if (seconds1 == -1 || seconds2 == -1) {
                return duration1;
            }

            return getDuration(seconds1 + seconds2);
        } catch (Exception e) {
            return duration1;
        }
    }

    private static int parseDurationToSeconds(String duration) {
        if (duration == null || duration.equals("Missed Call") || duration.equals("Didn't Connect")) {
            return -1;
        }

        try {
            if (duration.contains(":")) {
                String[] parts = duration.split(":");
                if (parts.length == 3) {
                    int hours = Integer.parseInt(parts[0]);
                    int minutes = Integer.parseInt(parts[1]);
                    int seconds = Integer.parseInt(parts[2]);
                    return hours * 3600 + minutes * 60 + seconds;
                }
            } else if (duration.contains("min")) {
                String[] parts = duration.split(" ");
                int minutes = 0, seconds = 0;
                for (int i = 0; i < parts.length; i++) {
                    if (parts[i].equals("min") && i > 0) {
                        minutes = Integer.parseInt(parts[i - 1]);
                    } else if (parts[i].equals("sec") && i > 0) {
                        seconds = Integer.parseInt(parts[i - 1]);
                    }
                }
                return minutes * 60 + seconds;
            } else if (duration.contains("sec")) {
                String[] parts = duration.split(" ");
                if (parts.length >= 2) {
                    return Integer.parseInt(parts[0]);
                }
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return -1;
    }

    private static Map<String, String> getNormalizedContactMap(Context context) {
        Map<String, String> contactMap = new HashMap<>();

        Cursor cursor = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                },
                null,
                null,
                null
        );

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    String number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    if (number != null && name != null) {
                        contactMap.put(normalizeNumber(number), name);
                    }
                }
            } finally {
                cursor.close();
            }
        }

        return contactMap;
    }

    private static String normalizeNumber(String number) {
        if (number == null) return "";
        return number.replaceAll("[^0-9]", "")
                .replaceFirst("^91", "");
    }

    private static String getFormattedDate(long timestamp, Context context) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault());
        String callDate = sdf.format(new Date(timestamp));

        String today = sdf.format(new Date());
        String yesterday = sdf.format(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000));

        if (callDate.equals(today)) return context.getString(R.string.today);
        if (callDate.equals(yesterday)) return context.getString(R.string.yesterday);
        return callDate;
    }

    private static String getFormattedTime(long millis, Context context) {
        String day = getFormattedDate(millis, context);
        String pattern;
        if (day.equals(context.getString(R.string.today)) || day.equals(context.getString(R.string.yesterday))) {
            pattern = "hh:mm a";
        } else {
            pattern = "EEE, hh:mm a";
        }
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
        return sdf.format(new Date(millis));
    }

    public static String getCallStatus(int callType) {
        switch (callType) {
            case CallLog.Calls.OUTGOING_TYPE:
                return "Outgoing";
            case CallLog.Calls.INCOMING_TYPE:
                return "Incoming";
            case CallLog.Calls.MISSED_TYPE:
                return "Missed Call";
            case CallLog.Calls.REJECTED_TYPE:
                return "Rejected";
            default:
                return "Unknown";
        }
    }

    private static String getFormattedDuration(int type, int duration) {
        if (type == CallLog.Calls.MISSED_TYPE) return "Missed Call";
        if (type == CallLog.Calls.OUTGOING_TYPE || type == CallLog.Calls.INCOMING_TYPE) {
            return (duration == 0) ? "Didn't Connect" : getDuration(duration);
        }
        return "Didn't Connect";
    }

    @SuppressLint("DefaultLocale")
    private static String getDuration(int duration) {
        int hours = duration / 3600;
        int minutes = (duration % 3600) / 60;
        int seconds = duration % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%02d min %02d sec", minutes, seconds);
        } else {
            return String.format("%02d sec", seconds);
        }
    }
}