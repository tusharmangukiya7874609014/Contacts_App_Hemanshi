package com.contactshandlers.contactinfoall.ads;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.contactshandlers.contactinfoall.BuildConfig;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;

public class BannerAD {

    private static BannerAD instance = null;
    private AdView mAdView;
    private boolean ad_start, is_customize_id;
    private String g_banner_id = "";

    public static boolean isBannerStarted(){
        return instance != null;
    }

    public static BannerAD getInstance() {
        if (instance == null) {
            instance = new BannerAD();
        }
        return instance;
    }

    public void init(Context context, boolean ad_start, boolean is_customize_id, String g_banner_id) {
        this.ad_start = ad_start;
        this.is_customize_id = is_customize_id;
        this.g_banner_id = g_banner_id;
        if (BuildConfig.DEBUG) {
            this.g_banner_id = "/6499/example/banner";
        }
    }

    public void showBannerAd(Context context, ViewGroup view, ViewGroup bannerView, ShimmerFrameLayout shimmer, String banner_id) {
        if (mAdView != null && mAdView.getParent() != null) {
            ((ViewGroup) mAdView.getParent()).removeView(mAdView);
        }
        if (ad_start && Utils.isNetworkConnected(context)) {
            shimmer.setVisibility(View.VISIBLE);
            bannerView.setVisibility(View.GONE);
            loadAndShowBannerAd(context, view, bannerView, shimmer, banner_id);
        }
    }

    private int getScreenWidthDp(Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        int widthPixels = context.getResources().getDisplayMetrics().widthPixels;
        return (int)(widthPixels / density);
    }

    private void loadAndShowBannerAd(Context context, ViewGroup view, ViewGroup bannerView, ShimmerFrameLayout shimmer, String banner_id) {
        if (is_customize_id) {
            g_banner_id = banner_id;
            if (BuildConfig.DEBUG) {
                this.g_banner_id = "ca-app-pub-3940256099942544/6300978111";
            }
        }
        if (mAdView != null && mAdView.getParent() != null) {
            ((ViewGroup) mAdView.getParent()).removeView(mAdView);
        }
        view.removeAllViews();
        if (mAdView == null && g_banner_id != null && !g_banner_id.isEmpty()) {
            mAdView = new AdView(context);
            AdSize adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, getScreenWidthDp(context));
            mAdView.setAdSize(adSize);
            mAdView.setAdUnitId(g_banner_id);
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);

            mAdView.setAdListener(new AdListener() {
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    super.onAdFailedToLoad(loadAdError);
                    mAdView = null;
                    shimmer.stopShimmer();
                    shimmer.setVisibility(View.GONE);
                    bannerView.setVisibility(View.GONE);
                }

                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();
                    if (mAdView != null && mAdView.getParent() != null) {
                        ((ViewGroup) mAdView.getParent()).removeView(mAdView);
                    }
                    shimmer.stopShimmer();
                    shimmer.setVisibility(View.GONE);
                    bannerView.setVisibility(View.VISIBLE);
                    if (mAdView != null) {
                        view.removeAllViews();
                        view.addView(mAdView);
                        mAdView = null;
                    }
                }
            });
        } else {
            if (mAdView != null && mAdView.getParent() != null) {
                ((ViewGroup) mAdView.getParent()).removeView(mAdView);
            }
            if (mAdView != null) {
                view.removeAllViews();
                view.addView(mAdView);
                mAdView = null;
            }
            shimmer.stopShimmer();
            shimmer.setVisibility(View.GONE);
            bannerView.setVisibility(View.GONE);
        }
    }
}