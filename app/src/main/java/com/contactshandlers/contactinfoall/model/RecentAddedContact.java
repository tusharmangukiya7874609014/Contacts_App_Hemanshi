package com.contactshandlers.contactinfoall.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "recent_added")
public class RecentAddedContact {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String contactId;
    private String name;
    private long addedTimestamp;

    public RecentAddedContact(String contactId, String name, long addedTimestamp) {
        this.contactId = contactId;
        this.name = name;
        this.addedTimestamp = addedTimestamp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContactId() {
        return contactId;
    }

    public void setContactId(String contactId) {
        this.contactId = contactId;
    }

    public long getAddedTimestamp() {
        return addedTimestamp;
    }

    public void setAddedTimestamp(long addedTimestamp) {
        this.addedTimestamp = addedTimestamp;
    }
}
