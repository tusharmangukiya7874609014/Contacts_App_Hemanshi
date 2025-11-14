package com.contactshandlers.contactinfoall.model;

public class CallLogItem {
    private String name;
    private String number;
    private String callType;
    private String duration;
    private String time;
    private int count;

    public CallLogItem(String name, String number, String callType, String duration, String time, int count) {
        this.name = name;
        this.number = number;
        this.callType = callType;
        this.duration = duration;
        this.time = time;
        this.count = count;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }

    public String getCallType() { return callType; }
    public void setCallType(String callType) { this.callType = callType; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
}