package com.contactshandlers.contactinfoall.ui.activity;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.adapter.LanguageAdapter;
import com.contactshandlers.contactinfoall.ads.BannerAD;
import com.contactshandlers.contactinfoall.ads.InterstitialAD;
import com.contactshandlers.contactinfoall.ads.NativeAD;
import com.contactshandlers.contactinfoall.databinding.ActivityLanguageSelectionBinding;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.helper.LocaleHelper;
import com.contactshandlers.contactinfoall.helper.SharedPreferencesManager;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.listeners.AdCallback;
import com.contactshandlers.contactinfoall.listeners.LangSelectionListeners;
import com.contactshandlers.contactinfoall.model.AdActivityData;
import com.contactshandlers.contactinfoall.model.AdsData;
import com.contactshandlers.contactinfoall.model.LanguageModel;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class LanguageSelectionActivity extends BaseActivity implements View.OnClickListener {

    private ActivityLanguageSelectionBinding binding;
    private final LanguageSelectionActivity activity = LanguageSelectionActivity.this;
    private LanguageAdapter adapter;
    private List<LanguageModel> langList = new ArrayList<>();
    private FirebaseDatabase mDatabase;
    private DatabaseReference mDatabaseReference;
    private String lang;
    private int pos;
    private boolean isAlreadyLoad = false;
    private String bannerId, nativeId;
    private boolean isAdStart, isShowBanner, isShowNative, isCustomizeId;
    private boolean languageSelectionAdStart;
    private String languageSelectionAdType;
    private boolean isLanguageSelectionAdaptiveBanner;
    private String languageSelectionAdaptiveBannerId;
    private String languageSelectionNativeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityLanguageSelectionBinding.inflate(getLayoutInflater());
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
        startRippleAnimation();
        setLangData();

        if (!getIntent().getBooleanExtra(Constants.IS_SECOND, false)) {
            onBack();
            getAndSet();
        } else {
            boolean isAdStart = SharedPreferencesManager.getInstance().getBooleanValue(Constants.LANGUAGE_SELECTION_ACTIVITY_AD_START, false);
            String adType = SharedPreferencesManager.getInstance().getStringValue(Constants.LANGUAGE_SELECTION_ACTIVITY_AD_TYPE, "");
            boolean isAdaptiveBanner = SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_LANGUAGE_SELECTION_ADAPTIVE_BANNER, false);
            String adaptiveBannerId = SharedPreferencesManager.getInstance().getStringValue(Constants.LANGUAGE_SELECTION_ADAPTIVE_BANNER_ID, "");
            String nativeId = SharedPreferencesManager.getInstance().getStringValue(Constants.LANGUAGE_SELECTION_NATIVE_ID, "");

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
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        Context context = LocaleHelper.getLocale(newBase);
        super.attachBaseContext(context);
    }

    private void initListener() {
        binding.btnContinue.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnContinue) {
            InterstitialAD.getInstance().showInterstitial(activity, new AdCallback() {
                @Override
                public void callbackCall() {
                    if (lang != null && !lang.isEmpty()) {
                        LocaleHelper.putLocale(lang);
                        SharedPreferencesManager.getInstance().setBooleanValue(Constants.IS_LANG_SELECT, true);
                        SharedPreferencesManager.getInstance().setIntValue(Constants.SELECTED_LANG, pos);
                        SharedPreferencesManager.getInstance().setStringValue(Constants.LANGUAGE_NAME, langList.get(pos).getLang());
                        LocaleHelper.getLocale(activity);
                        startActivity(new Intent(activity, MainActivity.class));
                        finishAffinity();
                    }
                }
            });
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

    private void getAndSet() {
        if (isAlreadyLoad) {
            return;
        }
        binding.adLayout.shimmerNativeLarge.setVisibility(View.VISIBLE);
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

                        AdActivityData languageSelectionActivity = adsData.getLanguageSelectionActivity();

                        if (languageSelectionActivity != null) {
                            languageSelectionAdStart = languageSelectionActivity.isAdStart();
                            languageSelectionAdType = languageSelectionActivity.getAdType();
                            isLanguageSelectionAdaptiveBanner = languageSelectionActivity.isAdaptiveBanner();
                            languageSelectionAdaptiveBannerId = languageSelectionActivity.getAdaptiveBannerId();
                            languageSelectionNativeId = languageSelectionActivity.getNativeId();
                            SharedPreferencesManager.getInstance().setBooleanValue(Constants.LANGUAGE_SELECTION_ACTIVITY_AD_START, languageSelectionAdStart);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.LANGUAGE_SELECTION_ACTIVITY_AD_TYPE, languageSelectionAdType);
                            SharedPreferencesManager.getInstance().setBooleanValue(Constants.IS_LANGUAGE_SELECTION_ADAPTIVE_BANNER, isLanguageSelectionAdaptiveBanner);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.LANGUAGE_SELECTION_NATIVE_ID, languageSelectionNativeId);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.LANGUAGE_SELECTION_ADAPTIVE_BANNER_ID, languageSelectionAdaptiveBannerId);
                        }

                        if (isAdStart) {
                            MobileAds.initialize(activity);

                            BannerAD.getInstance().init(activity, isShowBanner, isCustomizeId, bannerId);
                            NativeAD.getInstance().init(activity, isShowNative, isCustomizeId, nativeId);

                            boolean isAdStart = SharedPreferencesManager.getInstance().getBooleanValue(Constants.LANGUAGE_SELECTION_ACTIVITY_AD_START, false);
                            String adType = SharedPreferencesManager.getInstance().getStringValue(Constants.LANGUAGE_SELECTION_ACTIVITY_AD_TYPE, "");
                            boolean isAdaptiveBanner = SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_LANGUAGE_SELECTION_ADAPTIVE_BANNER, false);
                            String adaptiveBannerId = SharedPreferencesManager.getInstance().getStringValue(Constants.LANGUAGE_SELECTION_ADAPTIVE_BANNER_ID, "");
                            String nativeId = SharedPreferencesManager.getInstance().getStringValue(Constants.LANGUAGE_SELECTION_NATIVE_ID, "");

                            binding.adLayout.shimmerNativeLarge.setVisibility(View.GONE);
                            if (isAdStart) {
                                if (isAdaptiveBanner) {
                                    BannerAD.getInstance().showBannerAd(activity, binding.adLayout.llBanner, binding.adLayout.llBannerLayout, binding.adLayout.shimmerBanner, adaptiveBannerId);
                                } else {
                                    ShimmerFrameLayout shimmer = getShimmerFrameLayout(adType);
                                    NativeAD.getInstance().showNativeAd(activity, binding.adLayout.flNativePlaceHolder, binding.adLayout.llNativeLayout, shimmer, adType, nativeId);
                                }
                            }
                        } else {
                            binding.adLayout.shimmerNativeLarge.setVisibility(View.GONE);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    binding.adLayout.shimmerNativeLarge.setVisibility(View.GONE);
                }
            });
        } else {
            binding.adLayout.shimmerNativeLarge.setVisibility(View.GONE);
        }
        isAlreadyLoad = true;
    }

    private void startRippleAnimation() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(binding.btnContinue, "scaleX", 1f, 1.3f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(binding.btnContinue, "scaleY", 1f, 1.3f, 1f);

        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleX.setRepeatMode(ValueAnimator.RESTART);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatMode(ValueAnimator.RESTART);

        scaleX.setDuration(2000);
        scaleY.setDuration(2000);
        scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleY.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.start();
    }

    private void setLangData() {
        langList.add(new LanguageModel(R.drawable.img_english, "English (English)", "en"));
        langList.add(new LanguageModel(R.drawable.img_india, "Hindi (हिंदी)", "hi"));
        langList.add(new LanguageModel(R.drawable.img_german, "German (Deutsch)", "de"));
        langList.add(new LanguageModel(R.drawable.img_portuguese, "Portuguese (Português)", "pt"));
        langList.add(new LanguageModel(R.drawable.img_arabic, "Arabic (عربي)", "ar"));
        langList.add(new LanguageModel(R.drawable.img_turkish, "Turkish (Türkçe)", "tr"));
        langList.add(new LanguageModel(R.drawable.img_russian, "Russian (Русский)", "ru"));
        langList.add(new LanguageModel(R.drawable.img_japanese, "Japanese (日本語)", "ja"));
        langList.add(new LanguageModel(R.drawable.img_korean, "Korean (한국인)", "ko"));
        langList.add(new LanguageModel(R.drawable.img_india, "Tamil (தமிழ்)", "ta"));
        langList.add(new LanguageModel(R.drawable.img_india, "Telugu (తెలుగు)", "te"));
        langList.add(new LanguageModel(R.drawable.img_french, "French (Français)", "fr"));
        langList.add(new LanguageModel(R.drawable.img_dutch, "Dutch (Nederlands)", "nl"));
        langList.add(new LanguageModel(R.drawable.img_danish, "Danish (dansk)", "da"));
        langList.add(new LanguageModel(R.drawable.img_finnish, "Finnish (suomalainen)", "fi"));
        langList.add(new LanguageModel(R.drawable.img_swedish, "Swedish (svenska)", "sv"));
        langList.add(new LanguageModel(R.drawable.img_italian, "Italian (Italiana)", "it"));
        langList.add(new LanguageModel(R.drawable.img_spanish, "Spanish (Española)", "es"));
        langList.add(new LanguageModel(R.drawable.img_norwegian, "Norwegian (norsk)", "no"));
        langList.add(new LanguageModel(R.drawable.img_bengali, "Bengali (বাংলা)", "bn"));
        langList.add(new LanguageModel(R.drawable.img_chinese, "Chinese (中国人)", "zh"));
        langList.add(new LanguageModel(R.drawable.img_indonesian, "Indonesian (Indonesia)", "id"));

        binding.rvLanguage.setLayoutManager(new LinearLayoutManager(activity));
        adapter = new LanguageAdapter(activity, langList, new LangSelectionListeners() {
            @Override
            public void onClick(String langLocale, int position) {
                lang = langLocale;
                pos = position;
            }
        });
        binding.rvLanguage.setAdapter(adapter);
    }

    private void onBack() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
                System.exit(0);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NativeAD.getInstance().destroy();
    }
}