package com.contactshandlers.contactinfoall.model;

public class ContactsGroups {
    private String id;
    private String title;
    private String accountName;
    private String accountType;
    private String notes;
    private int contactCount;
    private boolean isMerged;

    public ContactsGroups() {
        this.isMerged = false;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public int getContactCount() {
        return contactCount;
    }

    public void setContactCount(int contactCount) {
        this.contactCount = contactCount;
    }

    public boolean isMerged() {
        return isMerged;
    }

    public void setIsMerged(boolean merged) {
        this.isMerged = merged;
    }

    @Override
    public String toString() {
        return "ContactsGroups{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", accountName='" + accountName + '\'' +
                ", accountType='" + accountType + '\'' +
                ", contactCount=" + contactCount +
                ", isMerged=" + isMerged +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContactsGroups that = (ContactsGroups) o;
        return java.util.Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id);
    }
}