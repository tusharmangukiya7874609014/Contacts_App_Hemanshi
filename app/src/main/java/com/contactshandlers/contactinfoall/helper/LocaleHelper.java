package com.contactshandlers.contactinfoall.helper;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;

public class LocaleHelper {
    public static Context getLocale(Context context) {
        String language = SharedPreferencesManager.getInstance().getStringValue(Constants.LOCALE, "en");
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration conf = res.getConfiguration();
        conf.setLocale(locale);

        Context localizedContext = context.createConfigurationContext(conf);
        SharedPreferencesManager.getInstance().setStringValue(Constants.LOCALE, language);

        return localizedContext;
    }

    public static void putLocale(String language) {
        SharedPreferencesManager.getInstance().setStringValue(Constants.LOCALE, language);
    }
}
