package com.contactshandlers.contactinfoall.helper;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telecom.TelecomManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;

import com.contactshandlers.contactinfoall.R;

public class DefaultDialerUtils {

    public interface DefaultDialerCallback {
        void onDefaultDialerSet();
    }

    public static void requestDefaultDialer(Activity activity, ActivityResultLauncher<Intent> launcher) {
        if (Utils.isDefaultDialer(activity)) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = (RoleManager) activity.getSystemService(Context.ROLE_SERVICE);
            if (roleManager != null) {
                Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER);
                if (intent.resolveActivity(activity.getPackageManager()) != null) {
                    launcher.launch(intent);
                } else {
                    showUnsupportedMessage(activity);
                }
            } else {
                showUnsupportedMessage(activity);
            }
        } else {
            TelecomManager telecomManager = (TelecomManager) activity.getSystemService(Context.TELECOM_SERVICE);
            if (telecomManager != null && !activity.getPackageName().equals(telecomManager.getDefaultDialerPackage())) {
                Intent intent = new Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER);
                intent.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, activity.getPackageName());
                if (intent.resolveActivity(activity.getPackageManager()) != null) {
                    launcher.launch(intent);
                } else {
                    showUnsupportedMessage(activity);
                }
            }
        }
    }

    public static void handleDefaultDialerResult(Activity activity, int resultCode, DefaultDialerCallback callback) {
        if (resultCode == Activity.RESULT_OK) {
            if (Utils.isDefaultDialer(activity)) {
                Toast.makeText(activity, activity.getString(R.string.successfully_set_as_default_dialer), Toast.LENGTH_SHORT).show();
                if (callback != null) {
                    callback.onDefaultDialerSet();
                }
            } else {
                Toast.makeText(activity, activity.getString(R.string.failed_to_set_as_default_dialer), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(activity, activity.getString(R.string.default_dialer_request_cancelled), Toast.LENGTH_SHORT).show();
        }
    }

    private static void showUnsupportedMessage(Activity activity) {
        Toast.makeText(activity, activity.getString(R.string.default_dialer_intent_not_supported_on_this_device), Toast.LENGTH_SHORT).show();
    }
}