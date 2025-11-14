package com.contactshandlers.contactinfoall.helper;

import androidx.room.TypeConverter;

import com.contactshandlers.contactinfoall.model.PhoneItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;

public class Converters {
    @TypeConverter
    public String fromPhoneItemList(List<PhoneItem> list) {
        return new Gson().toJson(list);
    }

    @TypeConverter
    public List<PhoneItem> toPhoneItemList(String value) {
        Type listType = new TypeToken<List<PhoneItem>>(){}.getType();
        return new Gson().fromJson(value, listType);
    }
}
