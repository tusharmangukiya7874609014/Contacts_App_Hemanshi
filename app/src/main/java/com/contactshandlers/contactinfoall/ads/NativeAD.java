package com.contactshandlers.contactinfoall.ads;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.contactshandlers.contactinfoall.BuildConfig;
import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;

public class NativeAD {

    private static NativeAD instance = null;
    private boolean ad_start, is_customize_id;
    private String g_native_id = "";
    private boolean adNativeLoading;
    private NativeAd mNativeAd;
    private AdLoader.Builder adBuilder;
    private AdLoader adLoader;

    public static NativeAD getInstance() {
        if (instance == null) {
            instance = new NativeAD();
        }
        return instance;
    }

    public void init(Context context, boolean ad_start, boolean is_customize_id, String g_native_id) {
        this.ad_start = ad_start;
        this.is_customize_id = is_customize_id;
        this.g_native_id = g_native_id;
        if (BuildConfig.DEBUG) {
            this.g_native_id = "/6499/example/native";
        }
    }

    public void showNativeAd(Context context, FrameLayout frameLayout, ViewGroup nativeView, ShimmerFrameLayout shimmer, String native_type, String native_id) {
        if (ad_start && Utils.isNetworkConnected(context)) {
            shimmer.setVisibility(View.VISIBLE);
            nativeView.setVisibility(View.GONE);
            loadAndShowNativeAd(context, frameLayout, nativeView, shimmer, native_type, native_id);
        }
    }

    private void loadAndShowNativeAd(Context context, FrameLayout frameLayout, ViewGroup nativeView, ShimmerFrameLayout shimmer, String native_type, String native_id) {
        adNativeLoading = false;
        if (is_customize_id) {
            g_native_id = native_id;
            if (BuildConfig.DEBUG) {
                this.g_native_id = "/6499/example/native";
            }
        }
        if (mNativeAd == null && g_native_id != null && !g_native_id.isEmpty()) {
            adBuilder = new AdLoader.Builder(context, g_native_id);
            adBuilder.forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
                @Override
                public void onNativeAdLoaded(@NonNull NativeAd nativeAd) {
                    if (mNativeAd != null) {
                        mNativeAd.destroy();
                    }
                    mNativeAd = nativeAd;
                }
            });

            adLoader = adBuilder.withAdListener(new AdListener() {
                @Override
                public void onAdClicked() {
                    super.onAdClicked();
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    super.onAdFailedToLoad(loadAdError);
                    mNativeAd = null;
                    shimmer.stopShimmer();
                    shimmer.setVisibility(View.GONE);
                    nativeView.setVisibility(View.GONE);
                }

                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();
                    adNativeLoading = true;
                    showGNativeAd(context, frameLayout, nativeView, shimmer, native_type);
                }
            }).build();

            adLoader.loadAd(new AdRequest.Builder().build());
        } else {
            shimmer.stopShimmer();
            shimmer.setVisibility(View.GONE);
            nativeView.setVisibility(View.GONE);
        }
    }

    private void showGNativeAd(Context context, FrameLayout frameLayout, ViewGroup nativeView, ShimmerFrameLayout shimmer, String native_type) {
        if (native_type != null) {
            if (native_type.equalsIgnoreCase("large")) {
                showGLargeNativeAd(context, frameLayout, nativeView, shimmer);
            } else if (native_type.equalsIgnoreCase("medium")) {
                showGMediumNativeAd(context, frameLayout, nativeView, shimmer);
            } else if (native_type.equalsIgnoreCase("small")) {
                showGSmallNativeAd(context, frameLayout, nativeView, shimmer);
            }
        }
    }

    private void showGLargeNativeAd(Context context, FrameLayout frameLayout, ViewGroup nativeView, ShimmerFrameLayout shimmer) {
        if (adNativeLoading && mNativeAd != null) {
            NativeAdView nativeAdView;
            LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            nativeAdView = (NativeAdView) li.inflate(R.layout.g_native_ad_large_layout, null);
            frameLayout.removeAllViews();
            frameLayout.addView(nativeAdView);

            nativeAdView.setMediaView(nativeAdView.findViewById(R.id.ad_media));

            nativeAdView.setHeadlineView(nativeAdView.findViewById(R.id.ad_headline));
            nativeAdView.setBodyView(nativeAdView.findViewById(R.id.ad_body));
            nativeAdView.setCallToActionView(nativeAdView.findViewById(R.id.ad_call_to_action));
            nativeAdView.setIconView(nativeAdView.findViewById(R.id.ad_app_icon));

            ((TextView) nativeAdView.getHeadlineView()).setText(mNativeAd.getHeadline());
            nativeAdView.getMediaView().setMediaContent(mNativeAd.getMediaContent());

            if (mNativeAd.getBody() == null) {
                nativeAdView.getBodyView().setVisibility(View.INVISIBLE);
            } else {
                nativeAdView.getBodyView().setVisibility(View.VISIBLE);
                ((TextView) nativeAdView.getBodyView()).setText(mNativeAd.getBody());
            }

            if (mNativeAd.getCallToAction() == null) {
                nativeAdView.getCallToActionView().setVisibility(View.INVISIBLE);
            } else {
                nativeAdView.getCallToActionView().setVisibility(View.VISIBLE);
                ((Button) nativeAdView.getCallToActionView()).setText(mNativeAd.getCallToAction());
            }

            if (mNativeAd.getIcon() == null) {
                nativeAdView.getIconView().setVisibility(View.GONE);
            } else {
                ((ImageView) nativeAdView.getIconView()).setImageDrawable(
                        mNativeAd.getIcon().getDrawable());
                nativeAdView.getIconView().setVisibility(View.VISIBLE);
            }

            nativeAdView.setNativeAd(mNativeAd);

            mNativeAd = null;

            shimmer.stopShimmer();
            shimmer.setVisibility(View.GONE);
            nativeView.setVisibility(View.VISIBLE);
        }
    }

    private void showGMediumNativeAd(Context context, FrameLayout frameLayout, ViewGroup nativeView, ShimmerFrameLayout shimmer) {
        if (adNativeLoading && mNativeAd != null) {
            NativeAdView nativeAdView;
            LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            nativeAdView = (NativeAdView) li.inflate(R.layout.g_native_ad_medium_layout, null);
            frameLayout.removeAllViews();
            frameLayout.addView(nativeAdView);

            nativeAdView.setHeadlineView(nativeAdView.findViewById(R.id.primary));
            nativeAdView.setCallToActionView(nativeAdView.findViewById(R.id.cta));
            nativeAdView.setIconView(nativeAdView.findViewById(R.id.icon));
            nativeAdView.setBodyView(nativeAdView.findViewById(R.id.secondary));

            ((TextView) nativeAdView.getHeadlineView()).setText(mNativeAd.getHeadline());

            if (mNativeAd.getCallToAction() == null) {
                nativeAdView.getCallToActionView().setVisibility(View.INVISIBLE);
            } else {
                nativeAdView.getCallToActionView().setVisibility(View.VISIBLE);
                ((Button) nativeAdView.getCallToActionView()).setText(mNativeAd.getCallToAction());
            }

            if (mNativeAd.getIcon() == null) {
                nativeAdView.getIconView().setVisibility(View.GONE);
            } else {
                ((ImageView) nativeAdView.getIconView()).setImageDrawable(
                        mNativeAd.getIcon().getDrawable());
                nativeAdView.getIconView().setVisibility(View.VISIBLE);
            }

            if (mNativeAd.getBody() == null) {
                nativeAdView.getBodyView().setVisibility(View.INVISIBLE);
            } else {
                nativeAdView.getBodyView().setVisibility(View.VISIBLE);
                ((TextView) nativeAdView.getBodyView()).setText(mNativeAd.getBody());
            }

            nativeAdView.setNativeAd(mNativeAd);

            mNativeAd = null;

            shimmer.stopShimmer();
            shimmer.setVisibility(View.GONE);
            nativeView.setVisibility(View.VISIBLE);
        }
    }

    private void showGSmallNativeAd(Context context, FrameLayout frameLayout, ViewGroup nativeView, ShimmerFrameLayout shimmer) {
        if (adNativeLoading && mNativeAd != null) {
            NativeAdView nativeAdView;
            LayoutInflater li = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            nativeAdView = (NativeAdView) li.inflate(R.layout.g_native_ad_small_layout, null);
            frameLayout.removeAllViews();
            frameLayout.addView(nativeAdView);

            nativeAdView.setHeadlineView(nativeAdView.findViewById(R.id.primary));
            nativeAdView.setCallToActionView(nativeAdView.findViewById(R.id.cta));
            nativeAdView.setIconView(nativeAdView.findViewById(R.id.icon));
            nativeAdView.setBodyView(nativeAdView.findViewById(R.id.secondary));

            ((TextView) nativeAdView.getHeadlineView()).setText(mNativeAd.getHeadline());

            if (mNativeAd.getCallToAction() == null) {
                nativeAdView.getCallToActionView().setVisibility(View.INVISIBLE);
            } else {
                nativeAdView.getCallToActionView().setVisibility(View.VISIBLE);
                ((Button) nativeAdView.getCallToActionView()).setText(mNativeAd.getCallToAction());
            }

            if (mNativeAd.getIcon() == null) {
                nativeAdView.getIconView().setVisibility(View.GONE);
            } else {
                ((ImageView) nativeAdView.getIconView()).setImageDrawable(
                        mNativeAd.getIcon().getDrawable());
                nativeAdView.getIconView().setVisibility(View.VISIBLE);
            }

            if (mNativeAd.getBody() == null) {
                nativeAdView.getBodyView().setVisibility(View.INVISIBLE);
            } else {
                nativeAdView.getBodyView().setVisibility(View.VISIBLE);
                ((TextView) nativeAdView.getBodyView()).setText(mNativeAd.getBody());
            }

            nativeAdView.setNativeAd(mNativeAd);

            mNativeAd = null;

            shimmer.stopShimmer();
            shimmer.setVisibility(View.GONE);
            nativeView.setVisibility(View.VISIBLE);
        }
    }

    public void destroy() {
        if (mNativeAd != null) {
            mNativeAd.destroy();
            mNativeAd = null;
        }
    }
}