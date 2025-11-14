package com.contactshandlers.contactinfoall.room;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.contactshandlers.contactinfoall.helper.Converters;
import com.contactshandlers.contactinfoall.model.BinContactEntity;

@Database(entities = {BinContactEntity.class}, version = 1)
@TypeConverters(Converters.class)
public abstract class BinContactDatabase extends RoomDatabase {
    private static BinContactDatabase instance;

    public abstract BinContactDao binContactDao();

    public static synchronized BinContactDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    BinContactDatabase.class, "bin_contact_db")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
