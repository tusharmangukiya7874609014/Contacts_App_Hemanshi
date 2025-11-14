package com.contactshandlers.contactinfoall.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "reminders")
public class Reminder {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private long timestamp;
    private String contactName;
    @ColumnInfo(name = "phoneNumber")
    private String phoneNumber;
    private String message;
    private long reminderTime;
    private String color;

    public Reminder(long timestamp, String contactName, String phoneNumber, String message, long reminderTime, String color) {
        this.timestamp = timestamp;
        this.contactName = contactName;
        this.phoneNumber = phoneNumber;
        this.message = message;
        this.reminderTime = reminderTime;
        this.color = color;
    }

    public long getTimestamp() { return timestamp; }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getContactName() { return contactName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getMessage() { return message; }
    public long getReminderTime() { return reminderTime; }
    public String getColor() { return color; }
}
