package com.contactshandlers.contactinfoall.model;

import java.util.List;

public class ContactGroupData {
    private String groupLetter;
    private List<ContactGroupItem> contacts;
    private int contactCount;

    public ContactGroupData(String groupLetter, List<ContactGroupItem> contacts) {
        this.groupLetter = groupLetter;
        this.contacts = contacts;
        this.contactCount = contacts != null ? contacts.size() : 0;
    }

    public String getGroupLetter() {
        return groupLetter;
    }

    public void setGroupLetter(String groupLetter) {
        this.groupLetter = groupLetter;
    }

    public List<ContactGroupItem> getContacts() {
        return contacts;
    }

    public void setContacts(List<ContactGroupItem> contacts) {
        this.contacts = contacts;
        this.contactCount = contacts != null ? contacts.size() : 0;
    }

    public int getContactCount() {
        return contactCount;
    }

    public void setContactCount(int contactCount) {
        this.contactCount = contactCount;
    }
}