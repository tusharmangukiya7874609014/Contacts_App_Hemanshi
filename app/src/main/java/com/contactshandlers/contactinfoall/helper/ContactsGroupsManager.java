package com.contactshandlers.contactinfoall.helper;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

import com.contactshandlers.contactinfoall.model.Account;
import com.contactshandlers.contactinfoall.model.ContactGroupItem;
import com.contactshandlers.contactinfoall.model.ContactsGroups;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ContactsGroupsManager {
    private ContentResolver contentResolver;

    public ContactsGroupsManager(Context context) {
        this.contentResolver = context.getContentResolver();
    }

    public int addContactsToGroup(String groupId, List<ContactGroupItem> contacts) {
        if (groupId == null || contacts == null || contacts.isEmpty()) {
            return 0;
        }

        int addedCount = 0;

        try {
            String actualGroupId = groupId.contains(",") ? groupId.split(",")[0] : groupId;

            for (ContactGroupItem contact : contacts) {
                if (addContactToGroup(actualGroupId, contact.getId())) {
                    addedCount++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return addedCount;
    }

    private boolean addContactToGroup(String groupId, String contactId) {
        try {
            if (isContactInGroup(groupId, contactId)) {
                return true;
            }

            android.content.ContentValues values = new android.content.ContentValues();
            values.put(ContactsContract.CommonDataKinds.GroupMembership.RAW_CONTACT_ID, getRawContactId(contactId));
            values.put(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID, groupId);
            values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE);

            android.net.Uri result = contentResolver.insert(ContactsContract.Data.CONTENT_URI, values);

            return result != null;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isContactInGroup(String groupId, String contactId) {
        String[] projection = {ContactsContract.Data.CONTACT_ID};
        String selection = ContactsContract.Data.MIMETYPE + " = ? AND " +
                ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID + " = ? AND " +
                ContactsContract.Data.CONTACT_ID + " = ?";
        String[] selectionArgs = {
                ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE,
                groupId,
                contactId
        };

        android.database.Cursor cursor = null;
        try {
            cursor = contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null
            );

            return cursor != null && cursor.getCount() > 0;

        } catch (Exception e) {
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private String getRawContactId(String contactId) {
        String[] projection = {ContactsContract.RawContacts._ID};
        String selection = ContactsContract.RawContacts.CONTACT_ID + " = ?";
        String[] selectionArgs = {contactId};

        android.database.Cursor cursor = null;
        try {
            cursor = contentResolver.query(
                    ContactsContract.RawContacts.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    ContactsContract.RawContacts._ID + " ASC LIMIT 1"
            );

            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts._ID));
            } else {
                return null;
            }

        } catch (Exception e) {
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public boolean removeContactFromGroup(String groupId, String contactId) {
        try {
            String actualGroupId = groupId.contains(",") ? groupId.split(",")[0] : groupId;

            String selection = ContactsContract.Data.MIMETYPE + " = ? AND " +
                    ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID + " = ? AND " +
                    ContactsContract.Data.CONTACT_ID + " = ?";
            String[] selectionArgs = {
                    ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE,
                    actualGroupId,
                    contactId
            };

            int rowsDeleted = contentResolver.delete(
                    ContactsContract.Data.CONTENT_URI,
                    selection,
                    selectionArgs
            );

            return rowsDeleted > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<ContactsGroups> getAllContactGroups() {
        Map<String, ContactsGroups> groupMap = new HashMap<>();

        String[] projection = {
                ContactsContract.Groups._ID,
                ContactsContract.Groups.TITLE,
                ContactsContract.Groups.SYSTEM_ID,
                ContactsContract.Groups.ACCOUNT_NAME,
                ContactsContract.Groups.ACCOUNT_TYPE,
                ContactsContract.Groups.NOTES,
                ContactsContract.Groups.SHOULD_SYNC
        };

        String selection = ContactsContract.Groups.DELETED + " = 0";
        String sortOrder = ContactsContract.Groups.TITLE + " ASC";

        Cursor cursor = null;
        try {
            cursor = contentResolver.query(
                    ContactsContract.Groups.CONTENT_URI,
                    projection,
                    selection,
                    null,
                    sortOrder
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String groupTitle = cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.TITLE));
                    String groupId = cursor.getString(cursor.getColumnIndex(ContactsContract.Groups._ID));
                    String systemId = cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.SYSTEM_ID));

                    if (shouldHideGroup(groupTitle, systemId)) {
                        continue;
                    }

                    String accountName = cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.ACCOUNT_NAME));
                    String accountType = cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.ACCOUNT_TYPE));
                    String notes = cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.NOTES));

                    String normalizedTitle = groupTitle != null ? groupTitle.trim() : "";

                    if (normalizedTitle.isEmpty()) {
                        normalizedTitle = "Unnamed";
                    }

                    String groupKey;
                    if (systemId == null) {
                        groupKey = normalizedTitle.toLowerCase() + "|null|" + groupId;
                    } else {
                        String normalizedSystemId = systemId.trim();
                        groupKey = normalizedTitle.toLowerCase() + "|" + normalizedSystemId;
                    }

                    if (groupMap.containsKey(groupKey)) {
                        ContactsGroups existingGroup = groupMap.get(groupKey);

                        String existingIds = existingGroup.getId();
                        existingGroup.setId(existingIds + "," + groupId);
                        existingGroup.setIsMerged(true);

                        if ("com.google".equals(accountType) && !"com.google".equals(existingGroup.getAccountType())) {
                            existingGroup.setAccountName(accountName);
                            existingGroup.setAccountType(accountType);
                        }

                        if (notes != null && !notes.trim().isEmpty()) {
                            String existingNotes = existingGroup.getNotes();
                            if (existingNotes == null || existingNotes.trim().isEmpty()) {
                                existingGroup.setNotes(notes);
                            } else if (!existingNotes.contains(notes)) {
                                existingGroup.setNotes(existingNotes + "; " + notes);
                            }
                        }
                    } else {
                        ContactsGroups group = new ContactsGroups();
                        group.setId(groupId);
                        group.setTitle(normalizedTitle);
                        group.setAccountName(accountName);
                        group.setAccountType(accountType);
                        group.setNotes(notes);
                        group.setIsMerged(false);

                        groupMap.put(groupKey, group);
                    }

                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        for (ContactsGroups group : groupMap.values()) {
            int contactCount = calculateUniqueContactCountForGroup(group.getId());
            group.setContactCount(contactCount);
        }

        List<ContactsGroups> groups = new ArrayList<>(groupMap.values());
        groups.sort((g1, g2) -> {
            String title1 = g1.getTitle() != null ? g1.getTitle() : "";
            String title2 = g2.getTitle() != null ? g2.getTitle() : "";
            return title1.compareToIgnoreCase(title2);
        });

        return groups;
    }

    private boolean shouldHideGroup(String groupTitle, String systemId) {
        if (groupTitle == null) {
            return false;
        }

        String titleLower = groupTitle.toLowerCase().trim();

        if (titleLower.equals("starred in android")) {
            return true;
        }

        if (titleLower.equals("my contacts") && systemId != null) {
            return true;
        }

        return false;
    }

    private int calculateUniqueContactCountForGroup(String groupIds) {
        if (groupIds == null || groupIds.trim().isEmpty()) {
            return 0;
        }

        Set<String> uniqueContactIds = new HashSet<>();
        String[] groupIdArray = groupIds.split(",");

        for (String groupId : groupIdArray) {
            groupId = groupId.trim();

            String[] projection = {ContactsContract.Data.CONTACT_ID};
            String selection = ContactsContract.Data.MIMETYPE + " = ? AND " +
                    ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID + " = ?";
            String[] selectionArgs = {
                    ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE,
                    groupId
            };

            Cursor cursor = null;
            try {
                cursor = contentResolver.query(
                        ContactsContract.Data.CONTENT_URI,
                        projection,
                        selection,
                        selectionArgs,
                        null
                );

                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID));
                        if (contactId != null) {
                            uniqueContactIds.add(contactId);
                        }
                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        return uniqueContactIds.size();
    }

    public List<ContactGroupItem> getContactsInGroup(String groupIds) {
        List<ContactGroupItem> contacts = new ArrayList<>();
        Map<String, ContactGroupItem> contactMap = new HashMap<>();

        if (groupIds == null || groupIds.trim().isEmpty()) {
            return contacts;
        }

        String[] groupIdArray = groupIds.split(",");

        for (String groupId : groupIdArray) {
            groupId = groupId.trim();

            String[] projection = {
                    ContactsContract.Data.CONTACT_ID,
                    ContactsContract.Data.DISPLAY_NAME,
                    ContactsContract.Data.PHOTO_URI
            };

            String selection = ContactsContract.Data.MIMETYPE + " = ? AND " +
                    ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID + " = ?";
            String[] selectionArgs = {
                    ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE,
                    groupId
            };

            Cursor cursor = null;
            try {
                cursor = contentResolver.query(
                        ContactsContract.Data.CONTENT_URI,
                        projection,
                        selection,
                        selectionArgs,
                        ContactsContract.Data.DISPLAY_NAME + " ASC"
                );

                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID));

                        if (!contactMap.containsKey(contactId)) {
                            ContactGroupItem contact = new ContactGroupItem();
                            contact.setId(contactId);
                            contact.setDisplayName(cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME)));
                            contact.setPhoto(cursor.getString(cursor.getColumnIndex(ContactsContract.Data.PHOTO_URI)));
                            contact.setGroupId(groupIds);

                            populateContactDetails(contact);
                            contactMap.put(contactId, contact);
                        }
                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        contacts.addAll(contactMap.values());

        contacts.sort((c1, c2) -> {
            String name1 = c1.getDisplayName() != null ? c1.getDisplayName() : "";
            String name2 = c2.getDisplayName() != null ? c2.getDisplayName() : "";
            return name1.compareToIgnoreCase(name2);
        });

        return contacts;
    }

    private void populateContactDetails(ContactGroupItem contact) {
        String phoneNumber = getContactPhone(contact.getId());
        contact.setPhoneNumber(phoneNumber);

        String email = getContactEmail(contact.getId());
        contact.setEmail(email);
    }

    private String getContactPhone(String contactId) {
        String phone = "";
        String[] projection = {ContactsContract.CommonDataKinds.Phone.NUMBER};
        String selection = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?";
        String[] selectionArgs = {contactId};

        Cursor cursor = null;
        try {
            cursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    ContactsContract.CommonDataKinds.Phone.IS_PRIMARY + " DESC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                phone = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return phone != null ? phone : "";
    }

    private String getContactEmail(String contactId) {
        String email = "";
        String[] projection = {ContactsContract.CommonDataKinds.Email.ADDRESS};
        String selection = ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?";
        String[] selectionArgs = {contactId};

        Cursor cursor = null;
        try {
            cursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    ContactsContract.CommonDataKinds.Email.IS_PRIMARY + " DESC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                email = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return email != null ? email : "";
    }

    public List<Account> getAvailableAccounts() {
        List<Account> accounts = new ArrayList<>();

        String[] projection = {
                ContactsContract.Groups.ACCOUNT_NAME,
                ContactsContract.Groups.ACCOUNT_TYPE
        };

        String selection = ContactsContract.Groups.DELETED + " = 0";

        Cursor cursor = null;
        Map<String, Account> accountMap = new HashMap<>();

        try {
            cursor = contentResolver.query(
                    ContactsContract.Groups.CONTENT_URI,
                    projection,
                    selection,
                    null,
                    ContactsContract.Groups.ACCOUNT_NAME + " ASC"
            );

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String accountName = cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.ACCOUNT_NAME));
                    String accountType = cursor.getString(cursor.getColumnIndex(ContactsContract.Groups.ACCOUNT_TYPE));

                    if (accountName != null && accountType != null) {
                        String key = accountName + "|" + accountType;
                        if (!accountMap.containsKey(key)) {
                            Account account = new Account(accountName, accountType);
                            accountMap.put(key, account);
                        }
                    }
                } while (cursor.moveToNext());
            }

            boolean hasPhoneStorage = false;
            for (Account acc : accountMap.values()) {
                if ("com.android.localphone".equals(acc.getType())) {
                    hasPhoneStorage = true;
                    break;
                }
            }

            if (!hasPhoneStorage) {
                Account phoneAccount = new Account(null, "com.android.localphone",
                        "Phone Storage");
                accountMap.put("phone|com.android.localphone", phoneAccount);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        accounts.addAll(accountMap.values());

        accounts.sort((a1, a2) -> {
            String name1 = a1.getDisplayName() != null ? a1.getDisplayName() : "";
            String name2 = a2.getDisplayName() != null ? a2.getDisplayName() : "";
            return name1.compareToIgnoreCase(name2);
        });

        return accounts;
    }

    public boolean createContactGroup(String groupName, Account account) {
        if (groupName == null || groupName.trim().isEmpty()) {
            return false;
        }

        if (account == null) {
            return false;
        }

        try {
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(ContactsContract.Groups.TITLE, groupName.trim());
            values.put(ContactsContract.Groups.GROUP_VISIBLE, 1);
            values.put(ContactsContract.Groups.SHOULD_SYNC, 1);

            if ("com.android.localphone".equals(account.getType())) {
                values.putNull(ContactsContract.Groups.ACCOUNT_NAME);
                values.putNull(ContactsContract.Groups.ACCOUNT_TYPE);
            } else {
                values.put(ContactsContract.Groups.ACCOUNT_NAME, account.getName());
                values.put(ContactsContract.Groups.ACCOUNT_TYPE, account.getType());
            }

            android.net.Uri result = contentResolver.insert(ContactsContract.Groups.CONTENT_URI, values);

            return result != null;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateGroupName(String groupId, String newName) {
        if (groupId == null || newName == null || newName.trim().isEmpty()) {
            return false;
        }

        try {
            String actualGroupId = groupId.contains(",") ? groupId.split(",")[0] : groupId;

            android.content.ContentValues values = new android.content.ContentValues();
            values.put(ContactsContract.Groups.TITLE, newName.trim());

            String selection = ContactsContract.Groups._ID + " = ?";
            String[] selectionArgs = {actualGroupId};

            int rowsUpdated = contentResolver.update(
                    ContactsContract.Groups.CONTENT_URI,
                    values,
                    selection,
                    selectionArgs
            );

            return rowsUpdated > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteGroup(String groupId) {
        if (groupId == null) {
            return false;
        }

        try {
            String actualGroupId = groupId.contains(",") ? groupId.split(",")[0] : groupId;
            return deleteGroupPermanently(actualGroupId);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean deleteGroupPermanently(String groupId) {
        try {
            android.net.Uri groupUri = ContactsContract.Groups.CONTENT_URI.buildUpon()
                    .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                    .build();

            String selection = ContactsContract.Groups._ID + " = ?";
            String[] selectionArgs = {groupId};

            int rowsDeleted = contentResolver.delete(groupUri, selection, selectionArgs);

            if (rowsDeleted > 0) {
                return true;
            }

            android.net.Uri permanentDeleteUri = ContactsContract.Groups.CONTENT_URI.buildUpon()
                    .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                    .appendQueryParameter("account_name", "")
                    .appendQueryParameter("account_type", "")
                    .build();

            rowsDeleted = contentResolver.delete(permanentDeleteUri, selection, selectionArgs);

            if (rowsDeleted > 0) {
                return true;
            }

            android.content.ContentValues values = new android.content.ContentValues();
            values.put(ContactsContract.Groups.DELETED, 1);

            int updated = contentResolver.update(
                    ContactsContract.Groups.CONTENT_URI,
                    values,
                    selection,
                    selectionArgs
            );

            return updated > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}