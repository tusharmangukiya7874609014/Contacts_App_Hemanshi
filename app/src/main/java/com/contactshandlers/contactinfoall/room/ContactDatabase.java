package com.contactshandlers.contactinfoall.room;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

import com.contactshandlers.contactinfoall.model.RecentAddedContact;
import com.contactshandlers.contactinfoall.model.RecentViewedContact;

@Database(entities = {RecentAddedContact.class, RecentViewedContact.class}, version = 1)
public abstract class ContactDatabase extends RoomDatabase {
    private static volatile ContactDatabase instance;

    public abstract ContactDao contactDao();

    public static ContactDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (ContactDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            ContactDatabase.class, "contact_db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}
