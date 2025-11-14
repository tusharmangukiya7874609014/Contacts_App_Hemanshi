package com.contactshandlers.contactinfoall.model;

import java.io.Serializable;
import java.util.List;

public class DuplicateGroup implements Serializable {
    private String groupKey;
    private List<Contact> contacts;
    private int duplicateCount;
    private String groupType;
    private String accountType;

    public DuplicateGroup(String groupKey, List<Contact> contacts, String groupType) {
        this.groupKey = groupKey;
        this.contacts = contacts;
        this.duplicateCount = contacts.size();
        this.groupType = groupType;
    }

    public DuplicateGroup(String groupKey, List<Contact> contacts, String groupType, String accountType) {
        this.groupKey = groupKey;
        this.contacts = contacts;
        this.duplicateCount = contacts.size();
        this.groupType = groupType;
        this.accountType = accountType;
    }

    public String getGroupKey() { return groupKey; }
    public void setGroupKey(String groupKey) { this.groupKey = groupKey; }

    public List<Contact> getContacts() { return contacts; }
    public void setContacts(List<Contact> contacts) {
        this.contacts = contacts;
        this.duplicateCount = contacts.size();
    }

    public int getDuplicateCount() { return duplicateCount; }
    public void setDuplicateCount(int duplicateCount) { this.duplicateCount = duplicateCount; }

    public String getGroupType() { return groupType; }
    public void setGroupType(String groupType) { this.groupType = groupType; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
}