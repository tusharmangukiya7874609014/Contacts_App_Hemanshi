package com.contactshandlers.contactinfoall.model;

public class ContactData {
    private String id;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String photo;
    private boolean isFavourite;
    private boolean isEmergency;
    private boolean isSelected;
    private long deletedTime;

    public ContactData(String id, String firstName, String lastName, String phoneNumber, String photo, boolean isFavourite, boolean isEmergency) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.photo = photo;
        this.isFavourite = isFavourite;
        this.isEmergency = isEmergency;
    }

    public String getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getPhoto() {
        return photo;
    }

    public boolean isFavourite() {
        return isFavourite;
    }

    public void setFavourite(boolean favourite) {
        isFavourite = favourite;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public long getDeletedTime() {
        return deletedTime;
    }

    public void setDeletedTime(long deletedTime) {
        this.deletedTime = deletedTime;
    }

    public boolean isEmergency() {
        return isEmergency;
    }

    public void setEmergency(boolean emergency) {
        isEmergency = emergency;
    }

    public String getNameFL() {
        if (firstName == null) {
            firstName = "";
        }
        if (lastName == null) {
            lastName = "";
        }
        return firstName + " " + lastName;
    }

    public String getFormattedName(boolean firstNameFirst) {
        if (lastName == null) {
            lastName = "";
            return firstName;
        } else {
            if (firstNameFirst) {
                return firstName + " " + lastName;
            } else {
                if (lastName.isEmpty()){
                    return firstName;
                }
                return lastName + ", " + firstName;
            }
        }
    }
}
