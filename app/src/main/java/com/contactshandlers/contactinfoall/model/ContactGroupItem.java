package com.contactshandlers.contactinfoall.model;

import android.os.Parcel;
import android.os.Parcelable;

public class ContactGroupItem implements Parcelable {
    private String id;
    private String displayName;
    private String phoneNumber;
    private String email;
    private String photo;
    private String groupId;

    public ContactGroupItem() {}

    protected ContactGroupItem(Parcel in) {
        id = in.readString();
        displayName = in.readString();
        phoneNumber = in.readString();
        email = in.readString();
        photo = in.readString();
        groupId = in.readString();
    }

    public static final Creator<ContactGroupItem> CREATOR = new Creator<ContactGroupItem>() {
        @Override
        public ContactGroupItem createFromParcel(Parcel in) {
            return new ContactGroupItem(in);
        }

        @Override
        public ContactGroupItem[] newArray(int size) {
            return new ContactGroupItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(displayName);
        dest.writeString(phoneNumber);
        dest.writeString(email);
        dest.writeString(photo);
        dest.writeString(groupId);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    @Override
    public String toString() {
        return "ContactGroupItem{" +
                "id='" + id + '\'' +
                ", displayName='" + displayName + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}