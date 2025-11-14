package com.contactshandlers.contactinfoall.model;

import com.google.firebase.database.PropertyName;

public class AdActivityData {
    public boolean adStart;
    public String adType;
    public String adaptiveBannerId;
    public String nativeId;
    public boolean isAdaptiveBanner;

    public AdActivityData() {
    }

    @PropertyName("ad_start")
    public boolean isAdStart() {
        return adStart;
    }

    @PropertyName("ad_start")
    public void setAdStart(boolean ad_start) {
        this.adStart = ad_start;
    }

    @PropertyName("ad_type")
    public String getAdType() {
        return adType;
    }

    @PropertyName("ad_type")
    public void setAdType(String ad_type) {
        this.adType = ad_type;
    }

    @PropertyName("adaptive_banner_id")
    public String getAdaptiveBannerId() {
        return adaptiveBannerId;
    }

    @PropertyName("adaptive_banner_id")
    public void setAdaptiveBannerId(String adaptive_banner_id) {
        this.adaptiveBannerId = adaptive_banner_id;
    }

    @PropertyName("native_id")
    public String getNativeId() {
        return nativeId;
    }

    @PropertyName("native_id")
    public void setNativeId(String native_id) {
        this.nativeId = native_id;
    }

    @PropertyName("is_adaptive_banner")
    public boolean isAdaptiveBanner() {
        return isAdaptiveBanner;
    }

    @PropertyName("is_adaptive_banner")
    public void setIsAdaptiveBanner(boolean isAdaptiveBanner) {
        this.isAdaptiveBanner = isAdaptiveBanner;
    }

}