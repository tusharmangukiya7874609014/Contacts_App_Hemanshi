package com.contactshandlers.contactinfoall.ui.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.role.RoleManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.contactshandlers.contactinfoall.BuildConfig;
import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.adapter.SectionsPagerAdapter;
import com.contactshandlers.contactinfoall.ads.BannerAD;
import com.contactshandlers.contactinfoall.ads.InterstitialAD;
import com.contactshandlers.contactinfoall.ads.NativeAD;
import com.contactshandlers.contactinfoall.databinding.ActivityMainBinding;
import com.contactshandlers.contactinfoall.databinding.CustomTabBinding;
import com.contactshandlers.contactinfoall.databinding.DialogExitBinding;
import com.contactshandlers.contactinfoall.databinding.LayoutCustomDrawerBinding;
import com.contactshandlers.contactinfoall.helper.App;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.helper.NotificationManagerHelper;
import com.contactshandlers.contactinfoall.helper.SharedPreferencesManager;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.model.AdActivityData;
import com.contactshandlers.contactinfoall.model.AdsData;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.microsoft.clarity.Clarity;
import com.microsoft.clarity.ClarityConfig;

import java.util.HashSet;

public class MainActivity extends BaseActivity implements View.OnClickListener {

    private ActivityMainBinding binding;
    private LayoutCustomDrawerBinding drawerBinding;
    private final MainActivity activity = MainActivity.this;
    private FirebaseDatabase mDatabase;
    private DatabaseReference mDatabaseReference;
    private Dialog dialog;
    private String bannerId, interId, nativeId, appOpenId;
    private String nativeType, bannerChange;
    private boolean isAdStart, isShowNotification, isBannerPreload, isInterPreload, isNativePreload;
    private boolean isShowAppOpenBackground, isShowBanner, isShowInter, isShowInterOnBack, isShowNative;
    private boolean alternateInterAppOpen, isCustomizeId, isShowInterAdDialog;
    private boolean languageSelectionAdStart, mainAdStart, viewContactAdStart, recycleBinAdStart, emergencyContactAdStart, themeAdStart, viewHistoryAdStart, callBackAdStart, blockNumberAdStart, mergeOptionsAdStart, mergeSelectionAdStart;
    private String languageSelectionAdType, mainAdType, viewContactAdType, recycleBinAdType, emergencyContactAdType, themeAdType, viewHistoryAdType, callBackAdType, blockNumberAdType, mergeOptionsAdType, mergeSelectionAdType;
    private boolean isLanguageSelectionAdaptiveBanner, isMainAdaptiveBanner, isViewContactAdaptiveBanner, isRecycleBinAdaptiveBanner, isEmergencyContactAdaptiveBanner, isThemeAdaptiveBanner, isViewHistoryAdaptiveBanner, isCallBackAdaptiveBanner, isBlockNumberAdaptiveBanner, isMergeOptionsAdaptiveBanner, isMergeSelectionAdaptiveBanner;
    private String languageSelectionAdaptiveBannerId, mainAdaptiveBannerId, viewContactAdaptiveBannerId, recycleBinAdaptiveBannerId, emergencyContactAdaptiveBannerId, themeAdaptiveBannerId, viewHistoryAdaptiveBannerId, callBackAdaptiveBannerId, blockNumberAdaptiveBannerId, mergeOptionsAdaptiveBannerId, mergeSelectionAdaptiveBannerId;
    private String languageSelectionNativeId, mainNativeId, viewContactNativeId, recycleBinNativeId, emergencyContactNativeId, themeNativeId, viewHistoryNativeId, callBackNativeId, blockNumberNativeId, mergeOptionsNativeId, mergeSelectionNativeId;
    private int interAdCount, interSkipCount, interAdShowTime, interAdCountOnBack, interDialogShowTime;
    private SectionsPagerAdapter sectionsPagerAdapter;
    private DialogExitBinding exitBinding;
    private int tabSelection = 2;
    private int[] tabIcons = {R.drawable.ic_favourite_tab, R.drawable.ic_recents_tab, R.drawable.ic_user_tab, R.drawable.ic_group_tab};
    private String[] tabTitles;
    private int backPressCount = 0, randomNumber = 0;
    private boolean isAlreadyLoad = false;
    private static final int TAB_FAVOURITES = 0;
    private static final int TAB_RECENT = 1;
    private boolean intentHandled = false;
    private NotificationManagerHelper notificationHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ClarityConfig config = new ClarityConfig(Constants.getKeyValue());
        Clarity.initialize(getApplicationContext(), config);

        Utils.setStatusBarColor(activity);

        App.AppOpenAdManager.isShowBackOpenAd = false;

        getAndSet();

        init();
        initListener();

        handleNotificationIntent(getIntent());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(Constants.TAB_SELECTION, tabSelection);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        tabSelection = savedInstanceState.getInt(Constants.TAB_SELECTION, 2);
        setTabSelection(tabSelection);
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        try {
            setIntent(intent);
            intentHandled = false;
            if (isAlreadyLoad) {
                handleNotificationIntent(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleNotificationIntent(Intent intent) {
        if (intent == null || intentHandled) {
            return;
        }

        try {
            int fragmentToOpen = intent.getIntExtra(Constants.OPEN_FRAGMENT, 0);
            if (fragmentToOpen == 1) {
                intentHandled = true;

                new Handler(Looper.getMainLooper()).postDelayed(this::openRecentFragmentPerfect, 200);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openRecentFragmentPerfect() {
        try {
            int currentTab = getCurrentTabPosition();
            if (currentTab == TAB_RECENT) {
                return;
            }
            navigateToRecentTab();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void navigateToRecentTab() {
        try {
            binding.viewPager.setCurrentItem(TAB_RECENT, false);

            TabLayout.Tab recentTab = binding.tabLayout.getTabAt(TAB_RECENT);
            if (recentTab != null) {
                recentTab.select();
            }

            setTabSelection(TAB_RECENT);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getCurrentTabPosition() {
        try {
            return binding.viewPager.getCurrentItem();
        } catch (Exception e) {
            return TAB_FAVOURITES;
        }
    }

    private void init() {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(activity, Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CALL_PHONE, Manifest.permission.ANSWER_PHONE_CALLS}, 102);
        }

        backPressCount = SharedPreferencesManager.getInstance().getIntValue(Constants.BACK_PRESS_COUNT, 0);
        randomNumber = SharedPreferencesManager.getInstance().getIntValue(Constants.RANDOM_NUMBER, 0);
        if (randomNumber == 0) {
            randomNumber = (int) (Math.random() * 3) + 5;
            SharedPreferencesManager.getInstance().setIntValue(Constants.RANDOM_NUMBER, randomNumber);
        }

        tabTitles = new String[]{getString(R.string.favourites), getString(R.string.recents), getString(R.string.contacts), getString(R.string.groups)};

        sectionsPagerAdapter = new SectionsPagerAdapter(activity);
        binding.viewPager.setAdapter(sectionsPagerAdapter);
        binding.viewPager.setUserInputEnabled(false);

        setupTabs();
        binding.viewPager.setCurrentItem(tabSelection, false);
        setTabSelection(tabSelection);

        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
            tab.setCustomView(getTabView(position, position == tabSelection));
            tab.view.setOnClickListener(v -> {
                binding.viewPager.setCurrentItem(position, false);
            });
        }).attach();

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                setTabSelection(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        onBack();
    }

    private void initListener() {
        binding.btnDialer.setOnClickListener(this);
        binding.btnDrawer.setOnClickListener(this);

        drawerBinding = LayoutCustomDrawerBinding.inflate(getLayoutInflater());
        binding.navigationView.addView(drawerBinding.getRoot());

        Bitmap bitmap = Utils.getInitialsBitmap(activity, R.drawable.ic_user,
                Utils.colorsList()[0 % Utils.colorsList().length]);
        drawerBinding.ivProfile.setImageBitmap(bitmap);

        drawerBinding.btnContacts.setOnClickListener(this);
        drawerBinding.btnCallerId.setOnClickListener(this);
        drawerBinding.btnRecycleBin.setOnClickListener(this);
        drawerBinding.btnSettings.setOnClickListener(this);
        drawerBinding.btnPrivacyPolicy.setOnClickListener(this);
        drawerBinding.btnRateUs.setOnClickListener(this);
        drawerBinding.btnShareApp.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnDialer) {
            startActivity(new Intent(activity, CallDialerActivity.class));
            overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_top);
        } else if (id == R.id.btnDrawer) {
            if (Utils.isCallerIdApp(activity)){
                drawerBinding.btnCallerId.setVisibility(View.GONE);
            } else {
                drawerBinding.btnCallerId.setVisibility(View.VISIBLE);
            }

            Cursor cursor = getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                    },
                    null,
                    null,
                    null
            );
            int totalCount = 0;

            if (cursor != null) {
                int idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
                int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                HashSet<String> uniqueContacts = new HashSet<>();

                while (cursor.moveToNext()) {
                    String contactId = cursor.getString(idIndex);
                    if (uniqueContacts.contains(contactId)) continue;

                    String displayName = cursor.getString(nameIndex);
                    String phone = cursor.getString(numberIndex);

                    if (!TextUtils.isEmpty(displayName) && !TextUtils.isEmpty(phone)) {
                        uniqueContacts.add(contactId);
                        totalCount++;
                    }
                }

                cursor.close();
            }

            drawerBinding.tvContactsCounter.setText(String.valueOf(totalCount));

            binding.main.openDrawer(androidx.core.view.GravityCompat.START);
        } else if (id == R.id.btnContacts) {
            binding.main.closeDrawers();
        } else if (id == R.id.btnCallerId) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                RoleManager roleManager = (RoleManager) getSystemService(Context.ROLE_SERVICE);
                if (roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                    if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                        Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING);
                        startActivityForResult(intent, 1001);
                    }
                }
            }
            binding.main.closeDrawers();
        } else if (id == R.id.btnRecycleBin) {
            startActivity(new Intent(activity, RecycleBinActivity.class));
            binding.main.closeDrawers();
        } else if (id == R.id.btnSettings) {
            startActivity(new Intent(activity, SettingsActivity.class));
            binding.main.closeDrawers();
        } else if (id == R.id.btnPrivacyPolicy) {
            String privacy_policy = SharedPreferencesManager.getInstance().getStringValue(Constants.PRIVACY_POLICY, "");
            if (privacy_policy != null && !privacy_policy.trim().isEmpty()) {
                Uri uri = Uri.parse(privacy_policy);
                try {
                    CustomTabColorSchemeParams toolbarColors = new CustomTabColorSchemeParams.Builder()
                            .setToolbarColor(getColor(R.color.bg_screen))
                            .build();
                    CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                    builder.setDefaultColorSchemeParams(toolbarColors);
                    CustomTabsIntent customTabsIntent = builder.build();
                    customTabsIntent.launchUrl(activity, uri);
                } catch (ActivityNotFoundException e) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(Intent.createChooser(intent, "Open with"));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
            binding.main.closeDrawers();
        } else if (id == R.id.btnRateUs) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + BuildConfig.APPLICATION_ID));
            intent.setPackage(getPackageName());
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                intent.setPackage(null);
                startActivity(intent);
                intent.setData(Uri.parse("http://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID));
            }
            binding.main.closeDrawers();
        } else if (id == R.id.btnShareApp) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT,
                    "Hey check out my app at: https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID);
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
            binding.main.closeDrawers();
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
                        interId = adsData.getInterId();
                        nativeId = adsData.getNativeId();
                        appOpenId = adsData.getAppOpenId();

                        nativeType = adsData.getNativeType();
                        bannerChange = adsData.getBannerChange();
                        SharedPreferencesManager.getInstance().setStringValue(Constants.NATIVE_TYPE, nativeType);
                        SharedPreferencesManager.getInstance().setStringValue(Constants.BANNER_CHANGE, bannerChange);

                        isAdStart = adsData.isAdStart();
                        isShowNotification = adsData.isShowNotification();
                        SharedPreferencesManager.getInstance().setBooleanValue(Constants.IS_SHOW_NOTIFICATION, isShowNotification);
                        isBannerPreload = adsData.isBannerPreload();
                        isInterPreload = adsData.isInterPreload();
                        isNativePreload = adsData.isNativePreload();
                        isShowBanner = adsData.isShowBanner();
                        isShowInter = adsData.isShowInter();
                        isShowInterOnBack = adsData.isShowInterOnBack();
                        isShowNative = adsData.isShowNative();
                        isCustomizeId = adsData.isCustomizeId();
                        isShowAppOpenBackground = adsData.isAppOpenBackground();

                        SharedPreferencesManager.getInstance().setStringValue(Constants.PRIVACY_POLICY, adsData.getPrivacyPolicy());

                        alternateInterAppOpen = adsData.isAlternateInterAppOpen();
                        isShowInterAdDialog = adsData.isInterAdDialog();
                        interAdCount = adsData.getInterAdCount();
                        interSkipCount = adsData.getInterSkipCount();
                        interAdShowTime = adsData.getInterAdShowTime();
                        interAdCountOnBack = adsData.getInterAdCountOnBack();
                        interDialogShowTime = adsData.getInterDialogShoeTime();

                        AdActivityData languageSelectionActivity = adsData.getLanguageSelectionActivity();
                        AdActivityData mainActivity = adsData.getMainActivity();
                        AdActivityData viewContactActivity = adsData.getViewContactActivity();
                        AdActivityData recycleBinActivity = adsData.getRecycleBinActivity();
                        AdActivityData emergencyContactActivity = adsData.getEmergencyContactActivity();
                        AdActivityData themeActivity = adsData.getThemeActivity();
                        AdActivityData viewHistoryActivity = adsData.getViewHistoryActivity();
                        AdActivityData callBackActivity = adsData.getCallBackActivity();
                        AdActivityData blockNumberActivity = adsData.getBlockNumberActivity();
                        AdActivityData mergeOptionsActivity = adsData.getMergeOptionsActivity();
                        AdActivityData mergeSelectionActivity = adsData.getMergeSelectionActivity();

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

                        if (mainActivity != null) {
                            mainAdStart = mainActivity.isAdStart();
                            mainAdType = mainActivity.getAdType();
                            isMainAdaptiveBanner = mainActivity.isAdaptiveBanner();
                            mainAdaptiveBannerId = mainActivity.getAdaptiveBannerId();
                            mainNativeId = mainActivity.getNativeId();
                            SharedPreferencesManager.getInstance().setBooleanValue(Constants.MAIN_ACTIVITY_AD_START, mainAdStart);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.MAIN_ACTIVITY_AD_TYPE, mainAdType);
                            SharedPreferencesManager.getInstance().setBooleanValue(Constants.IS_MAIN_ADAPTIVE_BANNER, isMainAdaptiveBanner);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.MAIN_NATIVE_ID, mainNativeId);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.MAIN_ADAPTIVE_BANNER_ID, mainAdaptiveBannerId);
                        }

                        if (viewContactActivity != null) {
                            viewContactAdStart = viewContactActivity.isAdStart();
                            viewContactAdType = viewContactActivity.getAdType();
                            isViewContactAdaptiveBanner = viewContactActivity.isAdaptiveBanner();
                            viewContactAdaptiveBannerId = viewContactActivity.getAdaptiveBannerId();
                            viewContactNativeId = viewContactActivity.getNativeId();
                            SharedPreferencesManager.getInstance().setBooleanValue(Constants.VIEW_CONTACT_ACTIVITY_AD_START, viewContactAdStart);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.VIEW_CONTACT_ACTIVITY_AD_TYPE, viewContactAdType);
                            SharedPreferencesManager.getInstance().setBooleanValue(Constants.IS_VIEW_CONTACT_ADAPTIVE_BANNER, isViewContactAdaptiveBanner);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.VIEW_CONTACT_NATIVE_ID, viewContactNativeId);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.VIEW_CONTACT_ADAPTIVE_BANNER_ID, viewContactAdaptiveBannerId);
                        }

                        if (recycleBinActivity != null) {
                            recycleBinAdStart = recycleBinActivity.isAdStart();
                            recycleBinAdType = recycleBinActivity.getAdType();
                            isRecycleBinAdaptiveBanner = recycleBinActivity.isAdaptiveBanner();
                            recycleBinAdaptiveBannerId = recycleBinActivity.getAdaptiveBannerId();
                            recycleBinNativeId = recycleBinActivity.getNativeId();
                            SharedPreferencesManager.getInstance().setBooleanValue(Constants.RECYCLE_BIN_ACTIVITY_AD_START, recycleBinAdStart);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.RECYCLE_BIN_ACTIVITY_AD_TYPE, recycleBinAdType);
                            SharedPreferencesManager.getInstance().setBooleanValue(Constants.IS_RECYCLE_BIN_ADAPTIVE_BANNER, isRecycleBinAdaptiveBanner);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.RECYCLE_BIN_NATIVE_ID, recycleBinNativeId);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.RECYCLE_BIN_ADAPTIVE_BANNER_ID, recycleBinAdaptiveBannerId);
                        }

                        if (emergencyContactActivity != null) {
                            emergencyContactAdStart = emergencyContactActivity.isAdStart();
                            emergencyContactAdType = emergencyContactActivity.getAdType();
                            isEmergencyContactAdaptiveBanner = emergencyContactActivity.isAdaptiveBanner();
                            emergencyContactAdaptiveBannerId = emergencyContactActivity.getAdaptiveBannerId();
                            emergencyContactNativeId = emergencyContactActivity.getNativeId();
                            SharedPreferencesManager.getInstance().setBooleanValue(Constants.EMERGENCY_CONTACT_ACTIVITY_AD_START, emergencyContactAdStart);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.EMERGENCY_CONTACT_ACTIVITY_AD_TYPE, emergencyContactAdType);
                            SharedPreferencesManager.getInstance().setBooleanValue(Constants.IS_EMERGENCY_CONTACT_ADAPTIVE_BANNER, isEmergencyContactAdaptiveBanner);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.EMERGENCY_CONTACT_NATIVE_ID, emergencyContactNativeId);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.EMERGENCY_CONTACT_ADAPTIVE_BANNER_ID, emergencyContactAdaptiveBannerId);
                        }

                        if (themeActivity != null) {
                            themeAdStart = themeActivity.isAdStart();
                            themeAdType = themeActivity.getAdType();
                            isThemeAdaptiveBanner = themeActivity.isAdaptiveBanner();
                            themeAdaptiveBannerId = themeActivity.getAdaptiveBannerId();
                            themeNativeId = themeActivity.getNativeId();
                            SharedPreferencesManager.getInstance().setBooleanValue(Constants.THEME_ACTIVITY_AD_START, themeAdStart);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.THEME_ACTIVITY_AD_TYPE, themeAdType);
                            SharedPreferencesManager.getInstance().setBooleanValue(Constants.IS_THEME_ADAPTIVE_BANNER, isThemeAdaptiveBanner);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.THEME_NATIVE_ID, themeNativeId);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.THEME_ADAPTIVE_BANNER_ID, themeAdaptiveBannerId);
                        }

                        if (viewHistoryActivity != null) {
                            viewHistoryAdStart = viewHistoryActivity.isAdStart();
                            viewHistoryAdType = viewHistoryActivity.getAdType();
                            isViewHistoryAdaptiveBanner = viewHistoryActivity.isAdaptiveBanner();
                            viewHistoryAdaptiveBannerId = viewHistoryActivity.getAdaptiveBannerId();
                            viewHistoryNativeId = viewHistoryActivity.getNativeId();
                            SharedPreferencesManager.getInstance().setBooleanValue(Constants.VIEW_HISTORY_ACTIVITY_AD_START, viewHistoryAdStart);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.VIEW_HISTORY_ACTIVITY_AD_TYPE, viewHistoryAdType);
                            SharedPreferencesManager.getInstance().setBooleanValue(Constants.IS_VIEW_HISTORY_ADAPTIVE_BANNER, isViewHistoryAdaptiveBanner);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.VIEW_HISTORY_NATIVE_ID, viewHistoryNativeId);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.VIEW_HISTORY_ADAPTIVE_BANNER_ID, viewHistoryAdaptiveBannerId);
                        }

                        if (callBackActivity != null) {
                            callBackAdStart = callBackActivity.isAdStart();
                            callBackAdType = callBackActivity.getAdType();
                            isCallBackAdaptiveBanner = callBackActivity.isAdaptiveBanner();
                            callBackAdaptiveBannerId = callBackActivity.getAdaptiveBannerId();
                            callBackNativeId = callBackActivity.getNativeId();
                            SharedPreferencesManager.getInstance().setBooleanValue(Constants.CALL_BACK_ACTIVITY_AD_START, callBackAdStart);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.CALL_BACK_ACTIVITY_AD_TYPE, callBackAdType);
                            SharedPreferencesManager.getInstance().setBooleanValue(Constants.IS_CALL_BACK_ADAPTIVE_BANNER, isCallBackAdaptiveBanner);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.CALL_BACK_NATIVE_ID, callBackNativeId);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.CALL_BACK_ADAPTIVE_BANNER_ID, callBackAdaptiveBannerId);
                        }

                        if (blockNumberActivity != null) {
                            blockNumberAdStart = blockNumberActivity.isAdStart();
                            blockNumberAdType = blockNumberActivity.getAdType();
                            isBlockNumberAdaptiveBanner = blockNumberActivity.isAdaptiveBanner();
                            blockNumberAdaptiveBannerId = blockNumberActivity.getAdaptiveBannerId();
                            blockNumberNativeId = blockNumberActivity.getNativeId();
                            SharedPreferencesManager.getInstance().setBooleanValue(Constants.BLOCK_NUMBER_ACTIVITY_AD_START, blockNumberAdStart);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.BLOCK_NUMBER_ACTIVITY_AD_TYPE, blockNumberAdType);
                            SharedPreferencesManager.getInstance().setBooleanValue(Constants.IS_BLOCK_NUMBER_ADAPTIVE_BANNER, isBlockNumberAdaptiveBanner);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.BLOCK_NUMBER_NATIVE_ID, blockNumberNativeId);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.BLOCK_NUMBER_ADAPTIVE_BANNER_ID, blockNumberAdaptiveBannerId);
                        }

                        if (mergeOptionsActivity != null) {
                            mergeOptionsAdStart = mergeOptionsActivity.isAdStart();
                            mergeOptionsAdType = mergeOptionsActivity.getAdType();
                            isMergeOptionsAdaptiveBanner = mergeOptionsActivity.isAdaptiveBanner();
                            mergeOptionsAdaptiveBannerId = mergeOptionsActivity.getAdaptiveBannerId();
                            mergeOptionsNativeId = mergeOptionsActivity.getNativeId();
                            SharedPreferencesManager.getInstance().setBooleanValue(Constants.MERGE_OPTIONS_ACTIVITY_AD_START, mergeOptionsAdStart);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.MERGE_OPTIONS_ACTIVITY_AD_TYPE, mergeOptionsAdType);
                            SharedPreferencesManager.getInstance().setBooleanValue(Constants.IS_MERGE_OPTIONS_ADAPTIVE_BANNER, isMergeOptionsAdaptiveBanner);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.MERGE_OPTIONS_NATIVE_ID, mergeOptionsNativeId);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.MERGE_OPTIONS_ADAPTIVE_BANNER_ID, mergeOptionsAdaptiveBannerId);
                        }

                        if (mergeSelectionActivity != null) {
                            mergeSelectionAdStart = mergeSelectionActivity.isAdStart();
                            mergeSelectionAdType = mergeSelectionActivity.getAdType();
                            isMergeSelectionAdaptiveBanner = mergeSelectionActivity.isAdaptiveBanner();
                            mergeSelectionAdaptiveBannerId = mergeSelectionActivity.getAdaptiveBannerId();
                            mergeSelectionNativeId = mergeSelectionActivity.getNativeId();
                            SharedPreferencesManager.getInstance().setBooleanValue(Constants.MERGE_SELECTION_ACTIVITY_AD_START, mergeSelectionAdStart);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.MERGE_SELECTION_ACTIVITY_AD_TYPE, mergeSelectionAdType);
                            SharedPreferencesManager.getInstance().setBooleanValue(Constants.IS_MERGE_SELECTION_ADAPTIVE_BANNER, isMergeSelectionAdaptiveBanner);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.MERGE_SELECTION_NATIVE_ID, mergeSelectionNativeId);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.MERGE_SELECTION_ADAPTIVE_BANNER_ID, mergeSelectionAdaptiveBannerId);
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
                            } else {
                                startNotification();
                            }
                        } else {
                            startNotification();
                        }

                        if (isAdStart) {
                            MobileAds.initialize(activity);

                            BannerAD.getInstance().init(activity, isShowBanner, isCustomizeId, bannerId);
                            InterstitialAD.getInstance().init(activity, isShowInter, interAdCount, interSkipCount, interAdShowTime, interId);
                            NativeAD.getInstance().init(activity, isShowNative, isCustomizeId, nativeId);

                            if (isShowAppOpenBackground) {
                                App.AppOpenAdManager.getInstance().setId(appOpenId);
                            }
                            App.AppOpenAdManager.isShowBackOpenAd = true;

                            boolean isAdStart = SharedPreferencesManager.getInstance().getBooleanValue(Constants.MAIN_ACTIVITY_AD_START, false);
                            String adType = SharedPreferencesManager.getInstance().getStringValue(Constants.MAIN_ACTIVITY_AD_TYPE, "");
                            boolean isAdaptiveBanner = SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_MAIN_ADAPTIVE_BANNER, false);
                            String adaptiveBannerId = SharedPreferencesManager.getInstance().getStringValue(Constants.MAIN_ADAPTIVE_BANNER_ID, "");
                            String nativeId = SharedPreferencesManager.getInstance().getStringValue(Constants.MAIN_NATIVE_ID, "");

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

    private void setupTabs() {
        for (int i = 0; i < tabIcons.length; i++) {
            TabLayout.Tab tab = binding.tabLayout.newTab();
            tab.setCustomView(getTabView(i, false));
            binding.tabLayout.addTab(tab);
        }
    }

    private View getTabView(int position, boolean isSelected) {
        CustomTabBinding tabBinding = CustomTabBinding.inflate(LayoutInflater.from(activity));

        tabBinding.ivTab.setImageResource(tabIcons[position]);
        tabBinding.tvTabText.setText(tabTitles[position]);

        if (isSelected && !tabTitles[position].isEmpty()) {
            tabBinding.tvTabText.setTextColor(activity.getResources().getColor(R.color.main));
            tabBinding.ivTab.setColorFilter(activity.getResources().getColor(R.color.main));
        } else {
            tabBinding.tvTabText.setTextColor(activity.getResources().getColor(R.color.grey_font));
            tabBinding.ivTab.setColorFilter(activity.getResources().getColor(R.color.grey_font));
        }

        return tabBinding.getRoot();
    }

    private void setTabSelection(int selectedPosition) {
        for (int i = 0; i < binding.tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = binding.tabLayout.getTabAt(i);
            if (tab != null && tab.getCustomView() != null) {
                tabSelection = selectedPosition;
                tab.setCustomView(getTabView(i, i == selectedPosition));
            }
        }
    }

    private void startNotification() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
            @SuppressLint("BatteryLife") Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
        notificationHelper = new NotificationManagerHelper(activity);
        notificationHelper.initializeOnAppStart();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startNotification();
            }
        }
    }

    private void onBack() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                backPressCount++;
                SharedPreferencesManager.getInstance().setIntValue(Constants.BACK_PRESS_COUNT, backPressCount);
                if (backPressCount >= randomNumber) {
                    showDialogExit();
                    backPressCount = 0;
                    randomNumber = (int) (Math.random() * 3) + 5;
                    SharedPreferencesManager.getInstance().setIntValue(Constants.BACK_PRESS_COUNT, backPressCount);
                    SharedPreferencesManager.getInstance().setIntValue(Constants.RANDOM_NUMBER, randomNumber);
                } else {
                    App.AppOpenAdManager.isShowBackOpenAd = false;
                    finishAffinity();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void showDialogExit() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        dialog = new Dialog(activity);
        exitBinding = DialogExitBinding.inflate(getLayoutInflater());
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setContentView(exitBinding.getRoot());
        dialog.setCancelable(true);

        exitBinding.btnRate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                dialog.dismiss();
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
            }
        });

        exitBinding.btnCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                App.AppOpenAdManager.isShowBackOpenAd = false;
                dialog.dismiss();
                finishAffinity();
            }
        });

        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        App.AppOpenAdManager.isShowBackOpenAd = false;
        NativeAD.getInstance().destroy();
    }
}