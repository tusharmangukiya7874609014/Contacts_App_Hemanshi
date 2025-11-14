package com.contactshandlers.contactinfoall.model;

public class Account {
    private String name;
    private String type;
    private String displayName;

    public Account(String name, String type) {
        this.name = name;
        this.type = type;
        this.displayName = generateDisplayName();
    }

    public Account(String name, String type, String displayName) {
        this.name = name;
        this.type = type;
        this.displayName = displayName;
    }

    private String generateDisplayName() {
        if (name == null || name.isEmpty()) {
            return getTypeDisplayName();
        }
        return name;
    }

    private String getTypeDisplayName() {
        switch (type) {
            case "com.google":
                return "Google Account";
            case "com.android.localphone":
                return "Phone Storage (hidden from other apps)";
            case "com.whatsapp":
                return "WhatsApp";
            case "com.telegram":
                return "Telegram";
            case "com.microsoft.exchange":
                return "Exchange";
            default:
                return type != null ? type : "Unknown Account";
        }
    }

    public String getName() { return name; }
    public void setName(String name) { 
        this.name = name; 
        this.displayName = generateDisplayName();
    }

    public String getType() { return type; }
    public void setType(String type) { 
        this.type = type; 
        this.displayName = generateDisplayName();
    }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    @Override
    public String toString() {
        return displayName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Account account = (Account) obj;
        return name.equals(account.name) && type.equals(account.type);
    }

    @Override
    public int hashCode() {
        return (name != null ? name.hashCode() : 0) * 31 + (type != null ? type.hashCode() : 0);
    }
}