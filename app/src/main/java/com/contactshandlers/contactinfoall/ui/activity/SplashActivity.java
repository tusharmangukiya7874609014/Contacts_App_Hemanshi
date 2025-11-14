package com.contactshandlers.contactinfoall.ui.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.databinding.ActivitySplashBinding;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.helper.SharedPreferencesManager;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.helper.XiaomiPermissionManager;

public class SplashActivity extends AppCompatActivity {

    private ActivitySplashBinding binding;
    private final SplashActivity activity = SplashActivity.this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Utils.setStatusBarColor(activity);

        if (SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_FIRST, true)) {
            startActivity(new Intent(activity, PrivacyPolicyActivity.class));
        } else if (Utils.isRequirePermissions(activity)) {
            startActivity(new Intent(activity, PermissionActivity.class));
        } else if (XiaomiPermissionManager.isXiaomiDevice() && !SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_AUTO_START_PERMISSION, false)) {
            startActivity(new Intent(activity, AutoStartPermissionActivity.class));
        } else if (XiaomiPermissionManager.isXiaomiDevice() && !SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_LOCK_AND_POPUP_PERMISSION, false)) {
            startActivity(new Intent(activity, LockAndPopupPermissionActivity.class));
        } else if (!SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_LANG_SELECT, false)) {
            startActivity(new Intent(activity, LanguageSelectionActivity.class));
        } else {
            startActivity(new Intent(activity, MainActivity.class));
        }
        finish();
    }
}