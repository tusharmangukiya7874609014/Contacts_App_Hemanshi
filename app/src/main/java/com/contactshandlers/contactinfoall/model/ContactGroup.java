package com.contactshandlers.contactinfoall.model;

import java.util.List;

public class ContactGroup {
    private String letter;
    private List<ContactData> contacts;

    public ContactGroup(String letter, List<ContactData> contacts) {
        this.letter = letter;
        this.contacts = contacts;
    }

    public String getLetter() {
        return letter;
    }

    public List<ContactData> getContacts() {
        return contacts;
    }
}
