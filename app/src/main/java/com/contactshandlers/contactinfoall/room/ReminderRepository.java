package com.contactshandlers.contactinfoall.room;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.contactshandlers.contactinfoall.model.Reminder;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReminderRepository {

    private ReminderDao reminderDao;
    private final ExecutorService executorService;

    public ReminderRepository(Application application) {
        ReminderDatabase database = ReminderDatabase.getInstance(application);
        reminderDao = database.reminderDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public void insert(Reminder reminder) {
        executorService.execute(() -> reminderDao.insert(reminder));
    }

    public LiveData<List<Reminder>> getAllReminders(String phoneNumber) {
        return reminderDao.getAllReminders(phoneNumber);
    }

    public void delete(Reminder reminder) {
        executorService.execute(() -> reminderDao.delete(reminder));
    }

    public void deleteReminderById(long reminderId) {
        executorService.execute(() -> reminderDao.deleteReminderById(reminderId));
    }
}
