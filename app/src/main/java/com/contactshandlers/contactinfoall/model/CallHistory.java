package com.contactshandlers.contactinfoall.model;

public class CallHistory {
    private String callType;
    private String dateTime;
    private String duration;
    private long durationInSeconds;

    public CallHistory(String callType, String dateTime, String duration, long durationInSeconds) {
        this.callType = callType;
        this.dateTime = dateTime;
        this.duration = duration;
        this.durationInSeconds = durationInSeconds;
    }

    public String getCallType() {
        return callType;
    }

    public void setCallType(String callType) {
        this.callType = callType;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public long getDurationInSeconds() {
        return durationInSeconds;
    }

    public void setDurationInSeconds(long durationInSeconds) {
        this.durationInSeconds = durationInSeconds;
    }
}
