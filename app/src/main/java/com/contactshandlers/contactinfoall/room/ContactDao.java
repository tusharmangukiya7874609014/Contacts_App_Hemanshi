package com.contactshandlers.contactinfoall.room;

import androidx.lifecycle.LiveData;
import androidx.room.*;

import com.contactshandlers.contactinfoall.model.RecentAddedContact;
import com.contactshandlers.contactinfoall.model.RecentViewedContact;

import java.util.List;

@Dao
public interface ContactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRecentAdded(RecentAddedContact contact);

    @Query("UPDATE recent_added SET name = :name, addedTimestamp = :timestamp WHERE contactId = :id")
    void updateRecentAdded(String id, String name, long timestamp);

    @Query("SELECT * FROM recent_added ORDER BY addedTimestamp DESC")
    LiveData<List<RecentAddedContact>> getRecentAdded();

    @Query("DELETE FROM recent_added")
    void deleteAllRecentAdded();

    @Query("DELETE FROM recent_added WHERE contactId = :contactId")
    void deleteRecentAddedById(String contactId);

    @Query("SELECT * FROM recent_added WHERE contactId = :contactId")
    RecentAddedContact getRecentAddedById(String contactId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRecentViewed(RecentViewedContact contact);

    @Query("UPDATE recent_viewed SET name = :name, viewedTimestamp = :timestamp WHERE contactId = :id")
    void updateRecentViewed(String id, String name, long timestamp);

    @Query("SELECT * FROM recent_viewed ORDER BY viewedTimestamp DESC")
    LiveData<List<RecentViewedContact>> getRecentViewed();

    @Query("DELETE FROM recent_viewed")
    void deleteAllRecentViewed();

    @Query("DELETE FROM recent_viewed WHERE contactId = :contactId")
    void deleteRecentViewedById(String contactId);

    @Query("SELECT * FROM recent_viewed WHERE contactId = :contactId")
    RecentViewedContact getRecentViewedById(String contactId);
}
