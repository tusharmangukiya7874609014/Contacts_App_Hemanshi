package com.contactshandlers.contactinfoall.room;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.contactshandlers.contactinfoall.model.EmergencyContact;

import java.util.List;

@Dao
public interface EmergencyContactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(EmergencyContact contact);

    @Query("DELETE FROM emergency_contacts WHERE contact_id = :contactId")
    void deleteByContactId(String contactId);

    @Query("SELECT * FROM emergency_contacts")
    LiveData<List<EmergencyContact>> getAllContacts();
}
