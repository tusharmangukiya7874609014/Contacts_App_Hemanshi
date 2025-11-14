package com.contactshandlers.contactinfoall.room;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.contactshandlers.contactinfoall.model.RecentAddedContact;
import com.contactshandlers.contactinfoall.model.RecentViewedContact;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ContactRepository {
    private final ContactDao contactDao;
    private final ExecutorService executorService;

    public ContactRepository(Application application) {
        ContactDatabase db = ContactDatabase.getInstance(application);
        contactDao = db.contactDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public void insertRecentAdded(RecentAddedContact contact) {
        executorService.execute(() -> contactDao.insertRecentAdded(contact));
    }

    public LiveData<List<RecentAddedContact>> getRecentAdded() {
        return contactDao.getRecentAdded();
    }

    public void deleteRecentAdded() {
        executorService.execute(contactDao::deleteAllRecentAdded);
    }

    public RecentAddedContact getRecentAddedById(String contactId) {
        return contactDao.getRecentAddedById(contactId);
    }

    public void insertRecentViewed(RecentViewedContact contact) {
        executorService.execute(() -> contactDao.insertRecentViewed(contact));
    }

    public LiveData<List<RecentViewedContact>> getRecentViewed() {
        return contactDao.getRecentViewed();
    }

    public void deleteRecentViewed() {
        executorService.execute(contactDao::deleteAllRecentViewed);
    }

    public RecentViewedContact getRecentViewedById(String contactId) {
        return contactDao.getRecentViewedById(contactId);
    }
}
