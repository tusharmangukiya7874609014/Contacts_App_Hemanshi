package com.contactshandlers.contactinfoall.helper;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.appopen.AppOpenAd;

import java.util.Date;

public class App extends Application implements Application.ActivityLifecycleCallbacks, LifecycleObserver {

    private AppOpenAdManager appOpenAdManager;
    private Activity currentActivity;

    @Override
    public void onCreate() {
        super.onCreate();
        this.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        appOpenAdManager = new AppOpenAdManager();
        MobileAds.initialize(getApplicationContext());
        SharedPreferencesManager.init(getApplicationContext());
        Utils.setTheme();
    }

    static {
        System.loadLibrary("contactinfoall");
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    protected void onAppForegrounded() {
        if (AppOpenAdManager.isShowBackOpenAd) {
            if (currentActivity != null) {
                appOpenAdManager.showAdIfAvailable(currentActivity, new OnShowAdCompleteListener() {
                    @Override
                    public void onShowAdComplete() {

                    }
                });
            }
        }
    }

    public interface OnShowAdCompleteListener {
        void onShowAdComplete();
    }

    public static class AppOpenAdManager {

        public static boolean isShowBackOpenAd;
        private long loadTime = 0;
        private static String gAppOpen = "";
        private AppOpenAd appOpenAd = null;
        private boolean isLoadingAd = false;
        private boolean isShowingAd = false;
        public static AppOpenAdManager instance;

        public static AppOpenAdManager getInstance() {
            if (instance == null) {
                instance = new AppOpenAdManager();
            }
            return instance;
        }

        public void setId(String gAppOpen){
            AppOpenAdManager.gAppOpen = gAppOpen;
        }

        public AppOpenAdManager() {}

        private void loadAd(Context context) {
            if (context == null){
                return;
            }
            if (isLoadingAd || isAdAvailable()) {
                return;
            }

            if (gAppOpen != null && !gAppOpen.isEmpty()) {
                isLoadingAd = true;
                AdRequest request = new AdRequest.Builder().build();
                AppOpenAd.load(
                        context, gAppOpen, request,
                        new AppOpenAd.AppOpenAdLoadCallback() {
                            @Override
                            public void onAdLoaded(@NonNull AppOpenAd ad) {
                                appOpenAd = ad;
                                isLoadingAd = false;
                                loadTime = (new Date()).getTime();
                            }

                            @Override
                            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                                isLoadingAd = false;
                            }
                        });
            }
        }

        private boolean wasLoadTimeLessThanNHoursAgo() {
            long dateDifference = (new Date()).getTime() - this.loadTime;
            long numMilliSecondsPerHour = 3600000;
            return (dateDifference < (numMilliSecondsPerHour * (long) 4));
        }

        private boolean isAdAvailable() {
            return appOpenAd != null && wasLoadTimeLessThanNHoursAgo();
        }

        public void showAdIfAvailable(
                @NonNull final Activity activity,
                @NonNull OnShowAdCompleteListener onShowAdCompleteListener){
            if (isShowingAd) {
                return;
            }

            if (!isAdAvailable()) {
                onShowAdCompleteListener.onShowAdComplete();
                loadAd(activity);
                return;
            }

            appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    appOpenAd = null;
                    isShowingAd = false;

                    onShowAdCompleteListener.onShowAdComplete();
                    loadAd(activity);
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    appOpenAd = null;
                    isShowingAd = false;

                    onShowAdCompleteListener.onShowAdComplete();
                    loadAd(activity);
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    isShowingAd = true;
                }
            });
            appOpenAd.show(activity);
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (!appOpenAdManager.isShowingAd) {
            currentActivity = activity;
        }
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {}

    @Override
    public void onActivityStopped(@NonNull Activity activity) {}

    @Override
    public void onActivityPaused(@NonNull Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {}

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {}
}