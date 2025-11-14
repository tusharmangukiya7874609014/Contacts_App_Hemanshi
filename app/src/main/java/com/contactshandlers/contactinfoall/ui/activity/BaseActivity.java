package com.contactshandlers.contactinfoall.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.contactshandlers.contactinfoall.helper.LocaleHelper;
import com.contactshandlers.contactinfoall.helper.Utils;

public class BaseActivity extends AppCompatActivity {

    private final BaseActivity activity = BaseActivity.this;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Utils.isRequirePermissions(activity)){
            startActivity(new Intent(activity, PermissionActivity.class));
            finish();
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        Context context = LocaleHelper.getLocale(newBase);
        Utils.setTheme();
        super.attachBaseContext(context);
    }
}