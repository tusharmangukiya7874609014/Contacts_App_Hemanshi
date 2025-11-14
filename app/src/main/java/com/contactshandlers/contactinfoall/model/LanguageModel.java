package com.contactshandlers.contactinfoall.model;

public class LanguageModel {
    private int icon;
    private String lang;
    private String code;

    public LanguageModel(int icon, String lang, String code) {
        this.icon = icon;
        this.lang = lang;
        this.code = code;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
