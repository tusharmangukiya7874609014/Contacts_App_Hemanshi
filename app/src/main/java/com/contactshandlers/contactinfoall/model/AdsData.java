package com.contactshandlers.contactinfoall.model;

import com.google.firebase.database.PropertyName;

public class AdsData {

    private boolean adStart;
    private boolean isShowNotification;
    private boolean bannerPreload;
    private boolean interPreload;
    private boolean nativePreload;
    private boolean appOpenBackground;
    private boolean isShowBanner;
    private boolean isShowInter;
    private boolean isShowInterOnBack;
    private boolean isCustomizeId;
    private boolean isShowNative;
    private boolean alternateInterAppOpen;
    private boolean interAdDialog;
    private String appOpenId;
    private String bannerId;
    private String interId;
    private String nativeId;
    private String splashType;
    private String nativeType;
    private String bannerChange;
    private String privacyPolicy;
    private int interAdCount;
    private int interSkipCount;
    private int interAdShowTime;
    private int interAdCountOnBack;
    private int interDialogShoeTime;

    private AdActivityData languageSelectionActivity;
    private AdActivityData mainActivity;
    private AdActivityData viewContactActivity;
    private AdActivityData recycleBinActivity;
    private AdActivityData emergencyContactActivity;
    private AdActivityData themeActivity;
    private AdActivityData viewHistoryActivity;
    private AdActivityData callBackActivity;
    private AdActivityData blockNumberActivity;
    private AdActivityData mergeOptionsActivity;
    private AdActivityData mergeSelectionActivity;
    private AdActivityData privacyPolicyActivity;
    private AdActivityData permissionActivity;

    @PropertyName("ad_start")
    public boolean isAdStart() {
        return adStart;
    }

    @PropertyName("ad_start")
    public void setAdStart(boolean adStart) {
        this.adStart = adStart;
    }

    @PropertyName("is_show_notification")
    public boolean isShowNotification() {
        return isShowNotification;
    }

    @PropertyName("is_show_notification")
    public void setShowNotification(boolean isShowNotification) {
        this.isShowNotification = isShowNotification;
    }

    @PropertyName("banner_preload")
    public boolean isBannerPreload() {
        return bannerPreload;
    }

    @PropertyName("banner_preload")
    public void setBannerPreload(boolean bannerPreload) {
        this.bannerPreload = bannerPreload;
    }

    @PropertyName("inter_preload")
    public boolean isInterPreload() {
        return interPreload;
    }

    @PropertyName("inter_preload")
    public void setInterPreload(boolean interPreload) {
        this.interPreload = interPreload;
    }

    @PropertyName("native_preload")
    public boolean isNativePreload() {
        return nativePreload;
    }

    @PropertyName("native_preload")
    public void setNativePreload(boolean nativePreload) {
        this.nativePreload = nativePreload;
    }

    @PropertyName("app_open_background")
    public boolean isAppOpenBackground() {
        return appOpenBackground;
    }

    @PropertyName("app_open_background")
    public void setAppOpenBackground(boolean appOpenBackground) {
        this.appOpenBackground = appOpenBackground;
    }

    @PropertyName("is_show_banner")
    public boolean isShowBanner() {
        return isShowBanner;
    }

    @PropertyName("is_show_banner")
    public void setShowBanner(boolean isShowBanner) {
        this.isShowBanner = isShowBanner;
    }

    @PropertyName("is_show_inter")
    public boolean isShowInter() {
        return isShowInter;
    }

    @PropertyName("is_show_inter")
    public void setShowInter(boolean isShowInter) {
        this.isShowInter = isShowInter;
    }

    @PropertyName("is_show_inter_on_back")
    public boolean isShowInterOnBack() {
        return isShowInterOnBack;
    }

    @PropertyName("is_show_inter_on_back")
    public void setShowInterOnBack(boolean isShowInterOnBack) {
        this.isShowInterOnBack = isShowInterOnBack;
    }

    @PropertyName("is_customize_id")
    public boolean isCustomizeId() {
        return isCustomizeId;
    }

    @PropertyName("is_customize_id")
    public void setCustomizeId(boolean isCustomizeId) {
        this.isCustomizeId = isCustomizeId;
    }

    @PropertyName("is_show_native")
    public boolean isShowNative() {
        return isShowNative;
    }

    @PropertyName("is_show_native")
    public void setShowNative(boolean isShowNative) {
        this.isShowNative = isShowNative;
    }

    @PropertyName("alternate_inter_app_open")
    public boolean isAlternateInterAppOpen() {
        return alternateInterAppOpen;
    }

    @PropertyName("alternate_inter_app_open")
    public void setAlternateInterAppOpen(boolean alternateInterAppOpen) {
        this.alternateInterAppOpen = alternateInterAppOpen;
    }

    @PropertyName("inter_ad_dialog")
    public boolean isInterAdDialog() {
        return interAdDialog;
    }

    @PropertyName("inter_ad_dialog")
    public void setInterAdDialog(boolean interAdDialog) {
        this.interAdDialog = interAdDialog;
    }

    @PropertyName("app_open_id")
    public String getAppOpenId() {
        return appOpenId;
    }

    @PropertyName("app_open_id")
    public void setAppOpenId(String appOpenId) {
        this.appOpenId = appOpenId;
    }

    @PropertyName("banner_id")
    public String getBannerId() {
        return bannerId;
    }

    @PropertyName("banner_id")
    public void setBannerId(String bannerId) {
        this.bannerId = bannerId;
    }

    @PropertyName("inter_id")
    public String getInterId() {
        return interId;
    }

    @PropertyName("inter_id")
    public void setInterId(String interId) {
        this.interId = interId;
    }

    @PropertyName("native_id")
    public String getNativeId() {
        return nativeId;
    }

    @PropertyName("native_id")
    public void setNativeId(String nativeId) {
        this.nativeId = nativeId;
    }

    @PropertyName("splash_type")
    public String getSplashType() {
        return splashType;
    }

    @PropertyName("splash_type")
    public void setSplashType(String splashType) {
        this.splashType = splashType;
    }

    @PropertyName("native_type")
    public String getNativeType() {
        return nativeType;
    }

    @PropertyName("native_type")
    public void setNativeType(String nativeType) {
        this.nativeType = nativeType;
    }

    @PropertyName("banner_change")
    public String getBannerChange() {
        return bannerChange;
    }

    @PropertyName("banner_change")
    public void setBannerChange(String bannerChange) {
        this.bannerChange = bannerChange;
    }

    @PropertyName("privacy_policy")
    public String getPrivacyPolicy() {
        return privacyPolicy;
    }

    @PropertyName("privacy_policy")
    public void setPrivacyPolicy(String privacyPolicy) {
        this.privacyPolicy = privacyPolicy;
    }

    @PropertyName("inter_ad_count")
    public int getInterAdCount() {
        return interAdCount;
    }

    @PropertyName("inter_ad_count")
    public void setInterAdCount(int interAdCount) {
        this.interAdCount = interAdCount;
    }

    @PropertyName("inter_skip_count")
    public int getInterSkipCount() {
        return interSkipCount;
    }

    @PropertyName("inter_skip_count")
    public void setInterSkipCount(int interSkipCount) {
        this.interSkipCount = interSkipCount;
    }

    @PropertyName("inter_ad_show_time")
    public int getInterAdShowTime() {
        return interAdShowTime;
    }

    @PropertyName("inter_ad_show_time")
    public void setInterAdShowTime(int interAdShowTime) {
        this.interAdShowTime = interAdShowTime;
    }

    @PropertyName("inter_ad_count_on_back")
    public int getInterAdCountOnBack() {
        return interAdCountOnBack;
    }

    @PropertyName("inter_ad_count_on_back")
    public void setInterAdCountOnBack(int interAdCountOnBack) {
        this.interAdCountOnBack = interAdCountOnBack;
    }

    @PropertyName("inter_dialog_show_time")
    public int getInterDialogShoeTime() {
        return interDialogShoeTime;
    }

    @PropertyName("inter_dialog_show_time")
    public void setInterDialogShoeTime(int interDialogShoeTime) {
        this.interDialogShoeTime = interDialogShoeTime;
    }

    @PropertyName("language_selection_activity")
    public AdActivityData getLanguageSelectionActivity() {
        return languageSelectionActivity;
    }

    @PropertyName("language_selection_activity")
    public void setLanguageSelectionActivity(AdActivityData languageSelectionActivity) {
        this.languageSelectionActivity = languageSelectionActivity;
    }

    @PropertyName("main_activity")
    public AdActivityData getMainActivity() {
        return mainActivity;
    }

    @PropertyName("main_activity")
    public void setMainActivity(AdActivityData mainActivity) {
        this.mainActivity = mainActivity;
    }

    @PropertyName("view_contact_activity")
    public AdActivityData getViewContactActivity() {
        return viewContactActivity;
    }

    @PropertyName("view_contact_activity")
    public void setViewContactActivity(AdActivityData viewContactActivity) {
        this.viewContactActivity = viewContactActivity;
    }

    @PropertyName("recycle_bin_activity")
    public AdActivityData getRecycleBinActivity() {
        return recycleBinActivity;
    }

    @PropertyName("recycle_bin_activity")
    public void setRecycleBinActivity(AdActivityData recycleBinActivity) {
        this.recycleBinActivity = recycleBinActivity;
    }

    @PropertyName("emergency_contact_activity")
    public AdActivityData getEmergencyContactActivity() {
        return emergencyContactActivity;
    }

    @PropertyName("emergency_contact_activity")
    public void setEmergencyContactActivity(AdActivityData emergencyContactActivity) {
        this.emergencyContactActivity = emergencyContactActivity;
    }

    @PropertyName("theme_activity")
    public AdActivityData getThemeActivity() {
        return themeActivity;
    }

    @PropertyName("theme_activity")
    public void setThemeActivity(AdActivityData themeActivity) {
        this.themeActivity = themeActivity;
    }

    @PropertyName("view_history_activity")
    public AdActivityData getViewHistoryActivity() {
        return viewHistoryActivity;
    }

    @PropertyName("view_history_activity")
    public void setViewHistoryActivity(AdActivityData viewHistoryActivity) {
        this.viewHistoryActivity = viewHistoryActivity;
    }

    @PropertyName("call_back_activity")
    public AdActivityData getCallBackActivity() {
        return callBackActivity;
    }

    @PropertyName("call_back_activity")
    public void setCallBackActivity(AdActivityData callBackActivity) {
        this.callBackActivity = callBackActivity;
    }

    @PropertyName("block_number_activity")
    public AdActivityData getBlockNumberActivity() {
        return blockNumberActivity;
    }

    @PropertyName("block_number_activity")
    public void setBlockNumberActivity(AdActivityData blockNumberActivity) {
        this.blockNumberActivity = blockNumberActivity;
    }

    @PropertyName("merge_options_activity")
    public AdActivityData getMergeOptionsActivity() {
        return mergeOptionsActivity;
    }

    @PropertyName("merge_options_activity")
    public void setMergeOptionsActivity(AdActivityData mergeOptionsActivity) {
        this.mergeOptionsActivity = mergeOptionsActivity;
    }

    @PropertyName("merge_selection_activity")
    public AdActivityData getMergeSelectionActivity() {
        return mergeSelectionActivity;
    }

    @PropertyName("merge_selection_activity")
    public void setMergeSelectionActivity(AdActivityData mergeSelectionActivity) {
        this.mergeSelectionActivity = mergeSelectionActivity;
    }

    @PropertyName("privacy_policy_activity")
    public AdActivityData getPrivacyPolicyActivity() {
        return privacyPolicyActivity;
    }

    @PropertyName("privacy_policy_activity")
    public void setPrivacyPolicyActivity(AdActivityData privacyPolicyActivity) {
        this.privacyPolicyActivity = privacyPolicyActivity;
    }

    @PropertyName("permission_activity")
    public AdActivityData getPermissionActivity() {
        return permissionActivity;
    }

    @PropertyName("permission_activity")
    public void setPermissionActivity(AdActivityData permissionActivity) {
        this.permissionActivity = permissionActivity;
    }

}