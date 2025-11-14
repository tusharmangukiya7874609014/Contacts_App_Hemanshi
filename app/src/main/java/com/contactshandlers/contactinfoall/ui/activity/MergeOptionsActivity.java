package com.contactshandlers.contactinfoall.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.ads.BannerAD;
import com.contactshandlers.contactinfoall.ads.InterstitialAD;
import com.contactshandlers.contactinfoall.ads.NativeAD;
import com.contactshandlers.contactinfoall.databinding.ActivityMergeOptionsBinding;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.helper.SharedPreferencesManager;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.listeners.AdCallback;
import com.facebook.shimmer.ShimmerFrameLayout;

public class MergeOptionsActivity extends BaseActivity implements View.OnClickListener {

    private ActivityMergeOptionsBinding binding;
    private final MergeOptionsActivity activity = MergeOptionsActivity.this;
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMergeOptionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Utils.setStatusBarColor(activity);
        initListener();
        init();
    }

    private void initListener(){
        binding.included.ivBack.setOnClickListener(this);
        binding.btnContactNumber.setOnClickListener(this);
        binding.btnContactName.setOnClickListener(this);
    }

    private void init(){
        binding.included.tvHeading.setText(getString(R.string.merge_duplicate_contact));

        boolean isAdStart = SharedPreferencesManager.getInstance().getBooleanValue(Constants.MERGE_OPTIONS_ACTIVITY_AD_START, false);
        String adType = SharedPreferencesManager.getInstance().getStringValue(Constants.MERGE_OPTIONS_ACTIVITY_AD_TYPE, "");
        boolean isAdaptiveBanner = SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_MERGE_OPTIONS_ADAPTIVE_BANNER, false);
        String adaptiveBannerId = SharedPreferencesManager.getInstance().getStringValue(Constants.MERGE_OPTIONS_ADAPTIVE_BANNER_ID, "");
        String nativeId = SharedPreferencesManager.getInstance().getStringValue(Constants.MERGE_OPTIONS_NATIVE_ID, "");

        if (isAdStart) {
            if (isAdaptiveBanner) {
                BannerAD.getInstance().showBannerAd(activity, binding.adLayout.llBanner, binding.adLayout.llBannerLayout, binding.adLayout.shimmerBanner, adaptiveBannerId);
            } else {
                ShimmerFrameLayout shimmer = getShimmerFrameLayout(adType);
                NativeAD.getInstance().showNativeAd(activity, binding.adLayout.flNativePlaceHolder, binding.adLayout.llNativeLayout, shimmer, adType, nativeId);
            }
        }

        getOnBackPressedDispatcher().addCallback(activity, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                InterstitialAD.getInstance().showInterstitial(activity, new AdCallback() {
                    @Override
                    public void callbackCall() {
                        finish();
                    }
                });
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.ivBack) {
            getOnBackPressedDispatcher().onBackPressed();
        } else if (id == R.id.btnContactNumber) {
            if (checkPermission()) {
                startDuplicateActivity(getString(R.string.duplicate_contact_number), getString(R.string.mobile));
            }
        } else if (id == R.id.btnContactName) {
            if (checkPermission()) {
                startDuplicateActivity(getString(R.string.duplicate_contact_name), getString(R.string.name));
            }
        }
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

    private boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS},
                    PERMISSIONS_REQUEST_READ_CONTACTS);
            return false;
        }
        return true;
    }

    private void startDuplicateActivity(String title, String mergeType) {
        InterstitialAD.getInstance().showInterstitial(activity, new AdCallback() {
            @Override
            public void callbackCall() {
                Intent intent = new Intent(activity, MergeSelectionActivity.class);
                intent.putExtra(Constants.TITLE, title);
                intent.putExtra(Constants.MERGE_TYPE, mergeType);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(activity, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NativeAD.getInstance().destroy();
    }
}