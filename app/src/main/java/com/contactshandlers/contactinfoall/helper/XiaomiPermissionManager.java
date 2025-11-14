package com.contactshandlers.contactinfoall.helper;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import java.util.List;

public class XiaomiPermissionManager {

    private final Activity activity;
    private final Context context;

    public XiaomiPermissionManager(Activity activity) {
        this.activity = activity;
        this.context = activity.getApplicationContext();
    }

    public void requestAutoStartPermission() {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"));
            activity.startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.miui.securitycenter",
                        "com.miui.autostart.AutoStartManagementActivity"));
                activity.startActivity(intent);
            } catch (Exception e2) {
                openAppSettings();
            }
        }
    }

    public void requestShowLockScreenPermission() {
        try {
            Intent intent = new Intent("miui.intent.action.APP_PERM_EDITOR");
            intent.setClassName("com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity");
            intent.putExtra("extra_pkgname", context.getPackageName());
            activity.startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.miui.securitycenter",
                        "com.miui.permcenter.permissions.AppPermissionsEditorActivity"));
                intent.putExtra("extra_pkgname", context.getPackageName());
                activity.startActivity(intent);
            } catch (Exception e2) {
                openAppSettings();
            }
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        activity.startActivity(intent);
    }

    public static boolean isXiaomiDevice() {
        return Build.MANUFACTURER.equalsIgnoreCase("xiaomi")
                || Build.MANUFACTURER.equalsIgnoreCase("redmi")
                || Build.MANUFACTURER.equalsIgnoreCase("poco");
    }
}