package com.contactshandlers.contactinfoall.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.contactshandlers.contactinfoall.helper.Converters;

import java.util.List;

@Entity(tableName = "bin_contacts")
public class BinContactEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String imageUri;
    public boolean isSelected = false;

    @TypeConverters(Converters.class)
    public List<PhoneItem> phoneList;

    public long timestampDeleted;

    public BinContactEntity(String name, String imageUri, List<PhoneItem> phoneList, long timestampDeleted) {
        this.name = name;
        this.imageUri = imageUri;
        this.phoneList = phoneList;
        this.timestampDeleted = timestampDeleted;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    public List<PhoneItem> getPhoneList() {
        return phoneList;
    }

    public void setPhoneList(List<PhoneItem> phoneList) {
        this.phoneList = phoneList;
    }

    public long getTimestampDeleted() {
        return timestampDeleted;
    }

    public void setTimestampDeleted(long timestampDeleted) {
        this.timestampDeleted = timestampDeleted;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
