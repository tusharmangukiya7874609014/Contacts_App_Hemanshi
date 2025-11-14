package com.contactshandlers.contactinfoall.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "emergency_contacts")
public class EmergencyContact {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "contact_id")
    public String contactId;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "phone")
    public String phone;

    @ColumnInfo(name = "photo_uri")
    public String photoUri;

    @ColumnInfo(name = "is_emergency")
    public boolean isEmergency;

    public EmergencyContact(String contactId, String name, String phone, String photoUri, boolean isEmergency) {
        this.contactId = contactId;
        this.name = name;
        this.phone = phone;
        this.photoUri = photoUri;
        this.isEmergency = isEmergency;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPhotoUri() {
        return photoUri;
    }

    public void setPhotoUri(String photoUri) {
        this.photoUri = photoUri;
    }

    public boolean isEmergency() {
        return isEmergency;
    }

    public void setEmergency(boolean emergency) {
        isEmergency = emergency;
    }
}
