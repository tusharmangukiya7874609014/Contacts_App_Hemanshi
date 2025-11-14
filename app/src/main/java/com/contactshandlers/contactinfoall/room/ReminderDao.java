package com.contactshandlers.contactinfoall.room;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.contactshandlers.contactinfoall.model.Reminder;

import java.util.List;

@Dao
public interface ReminderDao {
    @Insert
    void insert(Reminder reminder);

    @Query("SELECT * FROM reminders WHERE phoneNumber = :phoneNumber ORDER BY reminderTime ASC")
    LiveData<List<Reminder>> getAllReminders(String phoneNumber);

    @Delete
    void delete(Reminder reminder);

    @Query("DELETE FROM reminders WHERE timestamp = :reminderId")
    void deleteReminderById(long reminderId);
}
