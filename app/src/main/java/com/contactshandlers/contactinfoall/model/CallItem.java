package com.contactshandlers.contactinfoall.model;

import android.net.Uri;
import android.telecom.Call;

public class CallItem {
    private Call call;
    private String phoneNumber;
    private Uri photoUri;
    private int state;
    private long duration;
    private boolean isConference;

    public CallItem(Call call, String phoneNumber) {
        this.call = call;
        this.phoneNumber = phoneNumber;
        this.state = call.getState();
        this.duration = 0;
        this.isConference = call.getDetails().hasProperty(Call.Details.PROPERTY_CONFERENCE);
    }

    public Call getCall() {
        return call;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public Uri getPhotoUri() {
        return photoUri;
    }

    public void setPhotoUri(Uri photoUri) {
        this.photoUri = photoUri;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public boolean isConference() {
        return isConference;
    }

    public void setConference(boolean conference) {
        isConference = conference;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CallItem callItem = (CallItem) obj;
        return phoneNumber != null ? phoneNumber.equals(callItem.phoneNumber) : callItem.phoneNumber == null;
    }

    @Override
    public int hashCode() {
        return phoneNumber != null ? phoneNumber.hashCode() : 0;
    }
}