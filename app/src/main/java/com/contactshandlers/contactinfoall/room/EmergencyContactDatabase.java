package com.contactshandlers.contactinfoall.room;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.contactshandlers.contactinfoall.model.EmergencyContact;

@Database(entities = {EmergencyContact.class}, version = 1)
public abstract class EmergencyContactDatabase extends RoomDatabase {
    private static EmergencyContactDatabase INSTANCE;

    public abstract EmergencyContactDao emergencyContactDao();

    public static synchronized EmergencyContactDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                    EmergencyContactDatabase.class, "emergency_contact_db")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return INSTANCE;
    }
}
