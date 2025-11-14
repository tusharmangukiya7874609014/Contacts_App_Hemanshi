package com.contactshandlers.contactinfoall.room;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.contactshandlers.contactinfoall.model.Reminder;

@Database(entities = {Reminder.class}, version = 1)
public abstract class ReminderDatabase extends RoomDatabase {
    private static ReminderDatabase instance;
    public abstract ReminderDao reminderDao();

    public static synchronized ReminderDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    ReminderDatabase.class, "reminder_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
