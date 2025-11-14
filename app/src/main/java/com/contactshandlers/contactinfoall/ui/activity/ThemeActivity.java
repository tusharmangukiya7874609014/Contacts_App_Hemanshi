package com.contactshandlers.contactinfoall.ui.activity;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.ads.BannerAD;
import com.contactshandlers.contactinfoall.ads.InterstitialAD;
import com.contactshandlers.contactinfoall.ads.NativeAD;
import com.contactshandlers.contactinfoall.databinding.ActivityThemeBinding;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.helper.SharedPreferencesManager;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.listeners.AdCallback;
import com.facebook.shimmer.ShimmerFrameLayout;

public class ThemeActivity extends BaseActivity implements View.OnClickListener {

    private ActivityThemeBinding binding;
    private final ThemeActivity activity = ThemeActivity.this;
    private String theme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityThemeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Utils.setStatusBarColor(activity);
        init();
        initListener();
    }

    private void init() {
        binding.included.tvHeading.setText(getString(R.string.theme));

        boolean isAdStart = SharedPreferencesManager.getInstance().getBooleanValue(Constants.THEME_ACTIVITY_AD_START, false);
        String adType = SharedPreferencesManager.getInstance().getStringValue(Constants.THEME_ACTIVITY_AD_TYPE, "");
        boolean isAdaptiveBanner = SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_THEME_ADAPTIVE_BANNER, false);
        String adaptiveBannerId = SharedPreferencesManager.getInstance().getStringValue(Constants.THEME_ADAPTIVE_BANNER_ID, "");
        String nativeId = SharedPreferencesManager.getInstance().getStringValue(Constants.THEME_NATIVE_ID, "");

        if (isAdStart) {
            if (isAdaptiveBanner) {
                BannerAD.getInstance().showBannerAd(activity, binding.adLayout.llBanner, binding.adLayout.llBannerLayout, binding.adLayout.shimmerBanner, adaptiveBannerId);
            } else {
                ShimmerFrameLayout shimmer = getShimmerFrameLayout(adType);
                NativeAD.getInstance().showNativeAd(activity, binding.adLayout.flNativePlaceHolder, binding.adLayout.llNativeLayout, shimmer, adType, nativeId);
            }
        }

        theme = SharedPreferencesManager.getInstance().getStringValue(Constants.THEME, Constants.THEME_SYSTEM);

        switch (theme) {
            case Constants.THEME_DARK:
                binding.ivDark.setImageResource(R.drawable.ic_select);
                break;
            case Constants.THEME_LIGHT:
                binding.ivLight.setImageResource(R.drawable.ic_select);
                break;
            case Constants.THEME_SYSTEM:
                binding.ivSystem.setImageResource(R.drawable.ic_select);
                break;
            default:
                break;
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

    private void initListener() {
        binding.included.ivBack.setOnClickListener(this);
        binding.btnThemeDark.setOnClickListener(this);
        binding.btnThemeLight.setOnClickListener(this);
        binding.btnThemeSystem.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.ivBack) {
            getOnBackPressedDispatcher().onBackPressed();
        } else if (id == R.id.btnThemeDark) {
            binding.ivDark.setImageResource(R.drawable.ic_select);
            binding.ivLight.setImageResource(R.drawable.ic_unselect);
            binding.ivSystem.setImageResource(R.drawable.ic_unselect);
            theme = Constants.THEME_DARK;
            saveTheme();
        } else if (id == R.id.btnThemeLight) {
            binding.ivDark.setImageResource(R.drawable.ic_unselect);
            binding.ivLight.setImageResource(R.drawable.ic_select);
            binding.ivSystem.setImageResource(R.drawable.ic_unselect);
            theme = Constants.THEME_LIGHT;
            saveTheme();
        } else if (id == R.id.btnThemeSystem) {
            binding.ivDark.setImageResource(R.drawable.ic_unselect);
            binding.ivLight.setImageResource(R.drawable.ic_unselect);
            binding.ivSystem.setImageResource(R.drawable.ic_select);
            theme = Constants.THEME_SYSTEM;
            saveTheme();
        }
    }

    private void saveTheme() {
        SharedPreferencesManager.getInstance().setStringValue(Constants.THEME, theme);
        Utils.setTheme();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NativeAD.getInstance().destroy();
    }
}