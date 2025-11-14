package com.contactshandlers.contactinfoall.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.databinding.ActivityLockAndPopupPermissionBinding;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.helper.SharedPreferencesManager;
import com.contactshandlers.contactinfoall.helper.XiaomiPermissionManager;

public class LockAndPopupPermissionActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityLockAndPopupPermissionBinding binding;
    private LockAndPopupPermissionActivity activity = LockAndPopupPermissionActivity.this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityLockAndPopupPermissionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        initListener();
        init();
    }

    private void initListener() {
        binding.included.ivBack.setOnClickListener(this);
        binding.btnAllowPermission.setOnClickListener(this);
        binding.btnMaybeLater.setOnClickListener(this);
    }

    private void init() {
        binding.included.tvHeading.setText(getString(R.string.setup_for_xiaomi_phones));
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.ivBack) {
            finishAffinity();
        } else if (id == R.id.btnAllowPermission) {
            XiaomiPermissionManager xiaomiPermissionManager = new XiaomiPermissionManager(activity);
            xiaomiPermissionManager.requestShowLockScreenPermission();
        } else if (id == R.id.btnMaybeLater) {
            SharedPreferencesManager.getInstance().setBooleanValue(Constants.IS_LOCK_AND_POPUP_PERMISSION, true);
            if (!SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_LANG_SELECT, false)) {
                startActivity(new Intent(activity, LanguageSelectionActivity.class));
            } else {
                startActivity(new Intent(activity, MainActivity.class));
            }
            finish();
        }
    }
}