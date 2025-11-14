package com.contactshandlers.contactinfoall.ui.activity;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.ads.BannerAD;
import com.contactshandlers.contactinfoall.ads.NativeAD;
import com.contactshandlers.contactinfoall.databinding.ActivityPermissionBinding;
import com.contactshandlers.contactinfoall.databinding.DialogPermissionSettingsBinding;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.helper.SharedPreferencesManager;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.helper.XiaomiPermissionManager;
import com.contactshandlers.contactinfoall.model.AdActivityData;
import com.contactshandlers.contactinfoall.model.AdsData;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class PermissionActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityPermissionBinding binding;
    private final PermissionActivity activity = PermissionActivity.this;
    private Dialog dialog;
    private DialogPermissionSettingsBinding settingsBinding;
    private String[] requiredPermissions;
    private int currentPermissionIndex = 0;
    private ActivityResultLauncher<String> permissionLauncher;

    private FirebaseDatabase mDatabase;
    private DatabaseReference mDatabaseReference;
    private boolean isAlreadyLoad = false;
    private String bannerId, nativeId;
    private boolean isAdStart, isShowBanner, isShowNative, isCustomizeId;
    private boolean permissionAdStart = false;
    private String permissionAdType;
    private boolean isPermissionAdaptiveBanner;
    private String permissionAdaptiveBannerId;
    private String permissionNativeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityPermissionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Utils.setStatusBarColor(activity);
        initPermissions();
        initPermissionLaunchers();
        initListener();
        init();
    }

    private void initPermissions() {
        List<String> permissions = new ArrayList<>();

        permissions.add(Manifest.permission.READ_CONTACTS);
        permissions.add(Manifest.permission.WRITE_CONTACTS);

        permissions.add(Manifest.permission.READ_CALL_LOG);
        permissions.add(Manifest.permission.WRITE_CALL_LOG);

        permissions.add(Manifest.permission.READ_PHONE_STATE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        requiredPermissions = permissions.toArray(new String[0]);
    }

    private void initPermissionLaunchers() {
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        currentPermissionIndex++;
                        requestNextPermission();
                    } else {
                        handlePermissionDenied();
                    }
                }
        );
    }

    private void initListener() {
        binding.btnAllowPermission.setOnClickListener(this);
    }

    private void init() {
        getAndSet();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnAllowPermission) {
            requestPermissionsSequentially();
        }
    }

    private void getAndSet() {
        if (isAlreadyLoad) {
            return;
        }
        binding.adLayout.shimmerBanner.setVisibility(View.VISIBLE);
        if (Utils.isNetworkConnected(activity)) {
            mDatabase = FirebaseDatabase.getInstance();
            mDatabaseReference = mDatabase.getReference("ads_data");

            mDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    AdsData adsData = snapshot.getValue(AdsData.class);
                    if (adsData != null) {
                        bannerId = adsData.getBannerId();
                        nativeId = adsData.getNativeId();

                        isAdStart = adsData.isAdStart();
                        isShowBanner = adsData.isShowBanner();
                        isShowNative = adsData.isShowNative();
                        isCustomizeId = adsData.isCustomizeId();

                        AdActivityData permissionActivity = adsData.getPermissionActivity();

                        if (permissionActivity != null) {
                            permissionAdStart = permissionActivity.isAdStart();
                            permissionAdType = permissionActivity.getAdType();
                            isPermissionAdaptiveBanner = permissionActivity.isAdaptiveBanner();
                            permissionAdaptiveBannerId = permissionActivity.getAdaptiveBannerId();
                            permissionNativeId = permissionActivity.getNativeId();
                        }

                        if (isAdStart) {
                            MobileAds.initialize(activity);

                            BannerAD.getInstance().init(activity, isShowBanner, isCustomizeId, bannerId);
                            NativeAD.getInstance().init(activity, isShowNative, isCustomizeId, nativeId);

                            boolean isAdStart = permissionAdStart;
                            String adType = permissionAdType;
                            boolean isAdaptiveBanner = isPermissionAdaptiveBanner;
                            String adaptiveBannerId = permissionAdaptiveBannerId;
                            String nativeId = permissionNativeId;

                            binding.adLayout.shimmerBanner.setVisibility(View.GONE);
                            if (isAdStart) {
                                if (isAdaptiveBanner) {
                                    BannerAD.getInstance().showBannerAd(activity, binding.adLayout.llBanner, binding.adLayout.llBannerLayout, binding.adLayout.shimmerBanner, adaptiveBannerId);
                                } else {
                                    ShimmerFrameLayout shimmer = getShimmerFrameLayout(adType);
                                    NativeAD.getInstance().showNativeAd(activity, binding.adLayout.flNativePlaceHolder, binding.adLayout.llNativeLayout, shimmer, adType, nativeId);
                                }
                            }
                        } else {
                            binding.adLayout.shimmerBanner.setVisibility(View.GONE);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    binding.adLayout.shimmerBanner.setVisibility(View.GONE);
                }
            });
        } else {
            binding.adLayout.shimmerBanner.setVisibility(View.GONE);
        }
        isAlreadyLoad = true;
    }

    private ShimmerFrameLayout getShimmerFrameLayout(String type) {
        ShimmerFrameLayout shimmer = null;
        if (type.equalsIgnoreCase("large")) {
            shimmer = binding.adLayout.shimmerNativeLarge;
        } else if (type.equalsIgnoreCase("medium")) {
            shimmer = binding.adLayout.shimmerNativeMedium;
        } else if (type.equalsIgnoreCase("small")) {
            shimmer = binding.adLayout.shimmerNativeSmall;
        }
        return shimmer;
    }

    private void requestPermissionsSequentially() {
        currentPermissionIndex = 0;
        requestNextPermission();
    }

    private void requestNextPermission() {
        if (currentPermissionIndex >= requiredPermissions.length) {
            if (areAllPermissionsGranted()) {
                onAllPermissionsGranted();
            } else {
                permissionDenied();
            }
            return;
        }

        String permission = requiredPermissions[currentPermissionIndex];

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            currentPermissionIndex++;
            requestNextPermission();
        } else {
            if (shouldShowRequestPermissionRationale(permission)) {
                showPermissionRationale(permission);
            } else {
                permissionLauncher.launch(permission);
            }
        }
    }

    private boolean areAllPermissionsGranted() {
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void handlePermissionDenied() {
        String permission = requiredPermissions[currentPermissionIndex];

        if (shouldShowRequestPermissionRationale(permission)) {
            requestNextPermission();
        } else {
            permissionDenied();
            showSettingsDialog();
        }
    }

    private void showPermissionRationale(String permission) {
        String message = getPermissionRationaleMessage(permission);

        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        dialog = new Dialog(activity);
        settingsBinding = DialogPermissionSettingsBinding.inflate(getLayoutInflater());
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setContentView(settingsBinding.getRoot());
        dialog.setCancelable(true);

        settingsBinding.tv2.setText(message);
        settingsBinding.btnSettings.setText(getString(R.string.allow));

        settingsBinding.btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            permissionDenied();
        });

        settingsBinding.btnSettings.setOnClickListener(v -> {
            dialog.dismiss();
            permissionLauncher.launch(permission);
        });

        dialog.show();
    }

    private void showSettingsDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        dialog = new Dialog(activity);
        settingsBinding = DialogPermissionSettingsBinding.inflate(getLayoutInflater());
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setContentView(settingsBinding.getRoot());
        dialog.setCancelable(true);

        settingsBinding.btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            permissionDenied();
        });

        settingsBinding.btnSettings.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        });

        dialog.show();
    }

    private void permissionDenied(){
        Toast.makeText(activity, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show();
    }

    private String getPermissionRationaleMessage(String permission) {
        switch (permission) {
            case Manifest.permission.READ_CONTACTS:
            case Manifest.permission.WRITE_CONTACTS:
                return getString(R.string.this_app_needs_access_to_your_contacts_to_manage_and_display_contact_information);
            case Manifest.permission.READ_CALL_LOG:
            case Manifest.permission.WRITE_CALL_LOG:
                return getString(R.string.this_app_needs_access_to_your_call_logs_to_provide_call_related_features);
            case Manifest.permission.READ_PHONE_STATE:
                return getString(R.string.this_app_needs_access_to_phone_state_to_identify_incoming_and_outgoing_calls);
            case Manifest.permission.POST_NOTIFICATIONS:
                return getString(R.string.this_app_needs_notification_permission_to_alert_you_about_important_contacts);
            default:
                return getString(R.string.this_permission_is_required_for_the_app_to_function_properly);
        }
    }

    private void onAllPermissionsGranted() {
        if (XiaomiPermissionManager.isXiaomiDevice() && !SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_AUTO_START_PERMISSION, false)) {
            startActivity(new Intent(activity, AutoStartPermissionActivity.class));
        } else if (!SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_LANG_SELECT, false)) {
            startActivity(new Intent(activity, LanguageSelectionActivity.class));
        } else {
            startActivity(new Intent(activity, MainActivity.class));
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NativeAD.getInstance().destroy();
    }
}