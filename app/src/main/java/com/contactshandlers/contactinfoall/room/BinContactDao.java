package com.contactshandlers.contactinfoall.room;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.contactshandlers.contactinfoall.model.BinContactEntity;

import java.util.List;

@Dao
public interface BinContactDao {
    @Insert
    void insert(BinContactEntity contact);

    @Delete
    void delete(BinContactEntity contact);

    @Query("SELECT * FROM bin_contacts")
    LiveData<List<BinContactEntity>> getAll();

    @Query("DELETE FROM bin_contacts WHERE timestampDeleted < :timeLimit")
    void deleteOlderThan(long timeLimit);

    @Query("DELETE FROM bin_contacts")
    void clearAll();
}
