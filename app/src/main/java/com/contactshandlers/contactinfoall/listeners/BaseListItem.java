package com.contactshandlers.contactinfoall.listeners;

public interface BaseListItem {
    int TYPE_HEADER = 0;
    int TYPE_CONTACT = 1;
    
    int getItemType();
}
