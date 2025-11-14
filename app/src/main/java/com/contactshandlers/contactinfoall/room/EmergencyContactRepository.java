package com.contactshandlers.contactinfoall.room;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.contactshandlers.contactinfoall.model.EmergencyContact;

import java.util.List;
import java.util.concurrent.Executors;

public class EmergencyContactRepository {

    private EmergencyContactDao dao;
    private LiveData<List<EmergencyContact>> allContacts;

    public EmergencyContactRepository(Application application) {
        EmergencyContactDatabase db = EmergencyContactDatabase.getInstance(application);
        dao = db.emergencyContactDao();
        allContacts = dao.getAllContacts();
    }

    public void insert(EmergencyContact contact) {
        Executors.newSingleThreadExecutor().execute(() -> dao.insert(contact));
    }

    public void delete(String contactId) {
        Executors.newSingleThreadExecutor().execute(() -> dao.deleteByContactId(contactId));
    }

    public LiveData<List<EmergencyContact>> getAllContacts() {
        return allContacts;
    }
}
