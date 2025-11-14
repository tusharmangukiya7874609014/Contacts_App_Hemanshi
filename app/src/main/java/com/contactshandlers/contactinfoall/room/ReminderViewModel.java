package com.contactshandlers.contactinfoall.room;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.contactshandlers.contactinfoall.model.Reminder;

import java.util.List;

public class ReminderViewModel extends AndroidViewModel {

    private ReminderRepository repository;

    public ReminderViewModel(Application application) {
        super(application);
        repository = new ReminderRepository(application);
    }

    public void insert(Reminder reminder) {
        repository.insert(reminder);
    }

    public void delete(Reminder reminder) {
        repository.delete(reminder);
    }

    public LiveData<List<Reminder>> getAllReminders(String phoneNumber) {
        return repository.getAllReminders(phoneNumber);
    }

    public void deleteReminderById(long reminderId) {
        repository.deleteReminderById(reminderId);
    }
}
