package com.contactshandlers.contactinfoall.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Contact implements Serializable {
    private String id;
    private String name;
    private String phoneNumber;
    private String email;
    private String photo;
    private String accountName;
    private String accountType;
    private List<String> phoneNumbers;
    private List<String> emails;

    public Contact() {
        phoneNumbers = new ArrayList<>();
        emails = new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public List<String> getPhoneNumbers() { return phoneNumbers; }
    public void setPhoneNumbers(List<String> phoneNumbers) { this.phoneNumbers = phoneNumbers; }

    public List<String> getEmails() { return emails; }
    public void setEmails(List<String> emails) { this.emails = emails; }

    public String getDisplayAccountName() {
        return getAccountTypeDisplay();
    }

    public String getAccountTypeDisplay() {
        if (accountType == null || accountType.isEmpty()) return "Device";

        switch (accountType.toLowerCase()) {
            case "com.android.contacts":
            case "com.android.localphone":
            case "local":
            case "phone":
                return "Device";
            case "com.google":
                return accountName != null && accountName.contains("@") ? accountName : "Google";
            case "com.whatsapp":
                return "WhatsApp";
            case "org.telegram.messenger":
            case "org.telegram.plus":
            case "telegram":
                return "Telegram";
            case "com.android.sim":
                return "SIM";
            case "com.viber.voip":
                return "Viber";
            case "com.facebook.orca":
                return "Messenger";
            case "com.skype.raider":
                return "Skype";
            case "com.linkedin.android":
                return "LinkedIn";
            case "com.twitter.android":
                return "Twitter";
            default:
                if (accountName != null) {
                    if (accountName.contains("telegram") || accountName.contains("Telegram")) {
                        return "Telegram";
                    } else if (accountName.contains("whatsapp") || accountName.contains("WhatsApp")) {
                        return "WhatsApp";
                    } else if (accountName.contains("@")) {
                        return accountName;
                    }
                }

                return "Device";
        }
    }
}