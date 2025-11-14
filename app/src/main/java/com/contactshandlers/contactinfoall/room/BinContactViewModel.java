package com.contactshandlers.contactinfoall.room;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.contactshandlers.contactinfoall.model.BinContactEntity;

import java.util.List;

public class BinContactViewModel extends AndroidViewModel {
    private BinContactRepository repository;
    private LiveData<List<BinContactEntity>> allContacts;

    public BinContactViewModel(@NonNull Application application) {
        super(application);
        repository = new BinContactRepository(application);
        allContacts = repository.getAllContacts();
    }

    public LiveData<List<BinContactEntity>> getAllContacts() {
        return allContacts;
    }

    public void insert(BinContactEntity contact) {
        repository.insert(contact);
    }

    public void delete(BinContactEntity contact) {
        repository.delete(contact);
    }

    public void clearAll() {
        repository.clearAll();
    }

    public void autoDeleteOldContacts() {
        repository.deleteOlderThan30Days();
    }

    public int getDaysLeft(BinContactEntity contact) {
        long passedMillis = System.currentTimeMillis() - contact.timestampDeleted;
        int daysPassed = (int) (passedMillis / (1000 * 60 * 60 * 24));
        return Math.max(0, 30 - daysPassed);
    }
}
