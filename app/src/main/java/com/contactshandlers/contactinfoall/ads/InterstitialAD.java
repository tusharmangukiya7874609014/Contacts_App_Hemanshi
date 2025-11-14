package com.contactshandlers.contactinfoall.ads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.contactshandlers.contactinfoall.BuildConfig;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.listeners.AdCallback;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class InterstitialAD {

    private static InterstitialAD instance = null;
    private boolean ad_start;
    private String g_inter_id = "";
    private int inter_ad_count, inter_skip_count, inter_ad_show_time;
    private int count = 0, count_show;
    private InterstitialAd mInterstitialAd;
    private AdCallback myCallback;
    private boolean isStartLoading = false;
    private boolean isFirstTime = true;
    private boolean isTimerComplete = false;
    private Handler handler;

    public static InterstitialAD getInstance() {
        if (instance == null) {
            instance = new InterstitialAD();
        }
        return instance;
    }

    public void init(Context context, boolean ad_start, int inter_ad_count, int inter_skip_count, int inter_ad_show_time, String g_inter_id) {
        this.ad_start = ad_start;
        this.inter_ad_count = inter_ad_count;
        this.inter_skip_count = inter_skip_count;
        this.inter_ad_show_time = inter_ad_show_time;
        this.g_inter_id = g_inter_id;

        if (handler != null){
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
        handler = new Handler(Looper.getMainLooper());
        this.count = 0;
        this.count_show = inter_ad_count;
        this.isStartLoading = false;
        this.isFirstTime = true;
        this.isTimerComplete = false;
        if (BuildConfig.DEBUG) {
            this.g_inter_id = "/6499/example/interstitial";
        }
        loadAd(context);
    }

    public void showInterstitial(Activity activity, AdCallback adCallback) {
        myCallback = adCallback;
        if (ad_start && Utils.isNetworkConnected(activity)) {
            if (isFirstTime) {
                showLoadedInterstitialAd(activity);
            } else {
                if (count == inter_skip_count) {
                    showLoadedInterstitialAd(activity);
                    count = 0;
                } else {
                    if (isTimerComplete) {
                        count++;
                    }
                    if (myCallback != null) {
                        myCallback.callbackCall();
                        myCallback = null;
                    }
                }
            }
        } else {
            if (myCallback != null) {
                myCallback.callbackCall();
                myCallback = null;
            }
        }
    }

    private void loadAd(Context context) {
        if (ad_start) {
            loadGInterstitialAd(context);
        }
    }

    private void loadGInterstitialAd(Context context) {
        if (mInterstitialAd == null && !isStartLoading && g_inter_id != null && !g_inter_id.isEmpty()) {
            isStartLoading = true;
            AdRequest adRequest = new AdRequest.Builder().build();
            InterstitialAd.load(context, g_inter_id, adRequest, new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    mInterstitialAd = interstitialAd;
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    mInterstitialAd = null;
                    isStartLoading = false;
                }
            });
        }
    }

    private void showLoadedInterstitialAd(Activity activity) {
        if (mInterstitialAd != null) {
            mInterstitialAd.show(activity);
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    isFirstTime = false;
                    mInterstitialAd = null;
                    isStartLoading = false;
                    if (isTimerComplete) {
                        count_show++;
                    }
                    if (myCallback != null) {
                        myCallback.callbackCall();
                        myCallback = null;
                    }
                    loadAd(activity);
                    if (count_show == inter_ad_count) {
                        startTimer();
                    }
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    mInterstitialAd = null;
                    isStartLoading = false;
                    if (myCallback != null) {
                        myCallback.callbackCall();
                        myCallback = null;
                    }
                    loadAd(activity);
                }
            });
        } else {
            if (myCallback != null) {
                myCallback.callbackCall();
                myCallback = null;
            }
            loadAd(activity);
        }
    }

    private void startTimer() {
        isTimerComplete = false;
        count_show = 0;
        handler.postDelayed(() -> {
            isTimerComplete = true;
            count = 0;
        }, inter_ad_show_time * 1000L);
    }
}