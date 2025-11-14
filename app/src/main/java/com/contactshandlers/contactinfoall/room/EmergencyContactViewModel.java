package com.contactshandlers.contactinfoall.room;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.contactshandlers.contactinfoall.model.EmergencyContact;

import java.util.List;

public class EmergencyContactViewModel extends AndroidViewModel {

    private EmergencyContactRepository repository;
    private LiveData<List<EmergencyContact>> allContacts;

    public EmergencyContactViewModel(@NonNull Application application) {
        super(application);
        repository = new EmergencyContactRepository(application);
        allContacts = repository.getAllContacts();
    }

    public void insert(EmergencyContact contact) {
        repository.insert(contact);
    }

    public void delete(String contactId) {
        repository.delete(contactId);
    }

    public LiveData<List<EmergencyContact>> getAllContacts() {
        return allContacts;
    }
}
