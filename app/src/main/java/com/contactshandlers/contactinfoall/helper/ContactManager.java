package com.contactshandlers.contactinfoall.helper;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.content.Context;
import android.text.TextUtils;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.model.Contact;
import com.contactshandlers.contactinfoall.model.DuplicateGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ContactManager {
    private Context context;
    private ContentResolver contentResolver;
    private Map<String, List<Contact>> accountCache = new ConcurrentHashMap<>();
    private long lastCacheTime = 0;
    private static final long CACHE_DURATION = 30000;

    public ContactManager(Context context) {
        this.context = context;
        this.contentResolver = context.getContentResolver();
    }

    public List<DuplicateGroup> getAccountsWithDuplicates(String mergeType) {
        Map<String, List<Contact>> accountMap = getAccountContactsMap();
        List<DuplicateGroup> accountsWithDuplicates = new ArrayList<>();

        for (Map.Entry<String, List<Contact>> entry : accountMap.entrySet()) {
            List<Contact> accountContacts = entry.getValue();
            List<DuplicateGroup> duplicatesInAccount = findDuplicatesInAccountFast(accountContacts, mergeType);

            if (!duplicatesInAccount.isEmpty()) {
                int totalDuplicateContacts = 0;
                for (DuplicateGroup duplicateGroup : duplicatesInAccount) {
                    totalDuplicateContacts += duplicateGroup.getDuplicateCount();
                }

                Contact sampleContact = accountContacts.get(0);
                DuplicateGroup accountGroup = new DuplicateGroup(
                        entry.getKey(),
                        accountContacts,
                        "account",
                        sampleContact.getAccountType()
                );

                accountGroup.setDuplicateCount(totalDuplicateContacts);
                accountsWithDuplicates.add(accountGroup);
            }
        }

        return accountsWithDuplicates;
    }

    public List<DuplicateGroup> findDuplicatesInAccount(List<Contact> accountContacts, String mergeType) {
        return findDuplicatesInAccountFast(accountContacts, mergeType);
    }

    private List<DuplicateGroup> findDuplicatesInAccountFast(List<Contact> contacts, String mergeType) {
        if (context.getString(R.string.mobile).equals(mergeType)) {
            return findDuplicatesByNumberFast(contacts);
        } else {
            return findDuplicatesByNameFast(contacts);
        }
    }

    private List<DuplicateGroup> findDuplicatesByNumberFast(List<Contact> contacts) {
        Map<String, List<Contact>> duplicateMap = new HashMap<>();

        for (Contact contact : contacts) {
            if (!TextUtils.isEmpty(contact.getPhoneNumber())) {
                String cleanNumber = android.telephony.PhoneNumberUtils.normalizeNumber(contact.getPhoneNumber());
                duplicateMap.computeIfAbsent(cleanNumber, k -> new ArrayList<>()).add(contact);
            }
        }

        List<DuplicateGroup> duplicateGroups = new ArrayList<>();
        for (Map.Entry<String, List<Contact>> entry : duplicateMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplicateGroups.add(new DuplicateGroup(entry.getKey(), entry.getValue(), context.getString(R.string.mobile)));
            }
        }

        return duplicateGroups;
    }

    private List<DuplicateGroup> findDuplicatesByNameFast(List<Contact> contacts) {
        Map<String, List<Contact>> duplicateMap = new HashMap<>();

        for (Contact contact : contacts) {
            if (!TextUtils.isEmpty(contact.getName())) {
                String cleanName = contact.getName();
                duplicateMap.computeIfAbsent(cleanName, k -> new ArrayList<>()).add(contact);
            }
        }

        List<DuplicateGroup> duplicateGroups = new ArrayList<>();
        for (Map.Entry<String, List<Contact>> entry : duplicateMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplicateGroups.add(new DuplicateGroup(entry.getKey(), entry.getValue(), context.getString(R.string.name)));
            }
        }

        return duplicateGroups;
    }

    private Map<String, List<Contact>> getAccountContactsMap() {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastCacheTime < CACHE_DURATION && !accountCache.isEmpty()) {
            return accountCache;
        }

        accountCache.clear();
        List<Contact> contacts = getAllContactsOptimized();

        for (Contact contact : contacts) {
            String accountKey = contact.getDisplayAccountName();
            accountCache.computeIfAbsent(accountKey, k -> new ArrayList<>()).add(contact);
        }

        lastCacheTime = currentTime;
        return accountCache;
    }

    private List<Contact> getAllContactsOptimized() {
        List<Contact> contacts = new ArrayList<>();

        try {
            Cursor cursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                            ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
                            ContactsContract.CommonDataKinds.Phone.ACCOUNT_TYPE_AND_DATA_SET,
                    },
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " IS NOT NULL",
                    null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            );

            Map<String, Contact> contactMap = new HashMap<>();

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String contactId = cursor.getString(0);

                    Contact contact = contactMap.get(contactId);
                    if (contact == null) {
                        contact = new Contact();
                        contact.setId(contactId);
                        contact.setName(cursor.getString(1));
                        contact.setPhoto(cursor.getString(3));

                        String accountInfo = cursor.getString(4);
                        if (accountInfo != null) {
                            String[] parts = accountInfo.split("/");
                            if (parts.length >= 2) {
                                contact.setAccountType(parts[0]);
                                contact.setAccountName(parts[1]);
                            }
                        }

                        contact.setPhoneNumbers(new ArrayList<>());
                        contactMap.put(contactId, contact);
                    }

                    String phoneNumber = cursor.getString(2);
                    if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                        contact.getPhoneNumbers().add(phoneNumber.trim());
                        if (contact.getPhoneNumber() == null) {
                            contact.setPhoneNumber(phoneNumber.trim());
                        }
                    }
                }
                cursor.close();
            }

            contacts.addAll(contactMap.values());

        } catch (Exception e) {
            contacts = getAllContactsWithAccountSlow();
        }

        return contacts;
    }

    private List<Contact> getAllContactsWithAccountSlow() {
        List<Contact> contacts = new ArrayList<>();
        return contacts;
    }

    public void clearCache() {
        accountCache.clear();
        lastCacheTime = 0;
    }
}