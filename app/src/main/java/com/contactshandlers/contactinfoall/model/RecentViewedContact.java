package com.contactshandlers.contactinfoall.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "recent_viewed")
public class RecentViewedContact {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String contactId;
    private String name;
    private long viewedTimestamp;

    public RecentViewedContact(String contactId, String name, long viewedTimestamp) {
        this.contactId = contactId;
        this.name = name;
        this.viewedTimestamp = viewedTimestamp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContactId() {
        return contactId;
    }

    public void setContactId(String contactId) {
        this.contactId = contactId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getViewedTimestamp() {
        return viewedTimestamp;
    }

    public void setViewedTimestamp(long viewedTimestamp) {
        this.viewedTimestamp = viewedTimestamp;
    }
}
