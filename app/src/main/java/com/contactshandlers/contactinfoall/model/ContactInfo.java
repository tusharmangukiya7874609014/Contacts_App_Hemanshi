package com.contactshandlers.contactinfoall.model;

public class ContactInfo {
    private String contactId;
    private String name;

    public ContactInfo(String contactId, String name) {
        this.contactId = contactId;
        this.name = name;
    }

    public String getContactId() {
        return contactId;
    }

    public String getName() {
        return name;
    }
}
