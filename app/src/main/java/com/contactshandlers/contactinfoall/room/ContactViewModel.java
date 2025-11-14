package com.contactshandlers.contactinfoall.room;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.contactshandlers.contactinfoall.model.RecentAddedContact;
import com.contactshandlers.contactinfoall.model.RecentViewedContact;

import java.util.List;

public class ContactViewModel extends AndroidViewModel {
    private final ContactRepository repository;

    public ContactViewModel(Application application) {
        super(application);
        repository = new ContactRepository(application);
    }

    public void insertRecentAdded(RecentAddedContact contact) {
        repository.insertRecentAdded(contact);
    }

    public LiveData<List<RecentAddedContact>> getRecentAdded() {
        return repository.getRecentAdded();
    }

    public void deleteRecentAdded() {
        repository.deleteRecentAdded();
    }

    public RecentAddedContact getRecentAddedById(String contactId) {
        return repository.getRecentAddedById(contactId);
    }

    public void insertRecentViewed(RecentViewedContact contact) {
        repository.insertRecentViewed(contact);
    }

    public LiveData<List<RecentViewedContact>> getRecentViewed() {
        return repository.getRecentViewed();
    }

    public void deleteRecentViewed() {
        repository.deleteRecentViewed();
    }

    public RecentViewedContact getRecentViewedById(String contactId) {
        return repository.getRecentViewedById(contactId);
    }
}