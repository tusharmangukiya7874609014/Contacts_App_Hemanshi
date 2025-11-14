package com.contactshandlers.contactinfoall.room;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.contactshandlers.contactinfoall.model.BinContactEntity;

import java.util.List;
import java.util.concurrent.Executors;

public class BinContactRepository {
    private BinContactDao dao;
    private LiveData<List<BinContactEntity>> allContacts;

    public BinContactRepository(Application application) {
        BinContactDatabase db = BinContactDatabase.getInstance(application);
        dao = db.binContactDao();
        allContacts = dao.getAll();
    }

    public LiveData<List<BinContactEntity>> getAllContacts() {
        return allContacts;
    }

    public void insert(BinContactEntity contact) {
        Executors.newSingleThreadExecutor().execute(() -> dao.insert(contact));
    }

    public void delete(BinContactEntity contact) {
        Executors.newSingleThreadExecutor().execute(() -> dao.delete(contact));
    }

    public void clearAll() {
        Executors.newSingleThreadExecutor().execute(dao::clearAll);
    }

    public void deleteOlderThan30Days() {
        long thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000;
        Executors.newSingleThreadExecutor().execute(() -> dao.deleteOlderThan(thirtyDaysAgo));
    }
}
