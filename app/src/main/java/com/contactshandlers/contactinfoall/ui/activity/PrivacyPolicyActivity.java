package com.contactshandlers.contactinfoall.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.ads.BannerAD;
import com.contactshandlers.contactinfoall.ads.NativeAD;
import com.contactshandlers.contactinfoall.databinding.ActivityPrivacyPolicyBinding;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.helper.SharedPreferencesManager;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.model.AdActivityData;
import com.contactshandlers.contactinfoall.model.AdsData;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class PrivacyPolicyActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityPrivacyPolicyBinding binding;
    private final PrivacyPolicyActivity activity = PrivacyPolicyActivity.this;
    private boolean isCheck = true;
    private FirebaseDatabase mDatabase;
    private DatabaseReference mDatabaseReference;
    private boolean isAlreadyLoad = false;
    private String bannerId, nativeId;
    private boolean isAdStart, isShowBanner, isShowNative, isCustomizeId;
    private boolean privacyPolicyAdStart = false;
    private String privacyPolicyAdType;
    private boolean isPrivacyPolicyAdaptiveBanner;
    private String privacyPolicyAdaptiveBannerId;
    private String privacyPolicyNativeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityPrivacyPolicyBinding.inflate(getLayoutInflater());
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

    private void initListener() {
        binding.ivPolicy.setOnClickListener(this);
        binding.btnAgreeAndContinue.setOnClickListener(this);
    }

    private void init() {
        String text = binding.tvPolicy.getText().toString();
        getAndSet();

        SpannableString spannableString = new SpannableString(text);
        String specialString = getString(R.string.privacy_policy);
        int start = text.indexOf(specialString);
        int end = start + specialString.length();
        if (start >= 0) {
            spannableString.setSpan(
                    new ForegroundColorSpan(getColor(R.color.main)),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            spannableString.setSpan(
                    new UnderlineSpan(),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            spannableString.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    String privacy_policy = SharedPreferencesManager.getInstance().getStringValue(Constants.PRIVACY_POLICY, "");
                    if (privacy_policy != null && !privacy_policy.isEmpty()) {
                        Intent fallbackIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(privacy_policy));
                        startActivity(Intent.createChooser(fallbackIntent, "Open with"));
                    } else if (!Utils.isNetworkConnected(activity)) {
                        Toast.makeText(activity, getString(R.string.please_connect_with_internet), Toast.LENGTH_SHORT).show();
                    } else {
                        setPolicyLink();
                    }
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(true);
                    ds.setColor(getColor(R.color.main));
                }
            }, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        binding.tvPolicy.setText(spannableString);
        binding.tvPolicy.setMovementMethod(LinkMovementMethod.getInstance());

        setPolicy(isCheck);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.ivPolicy) {
            isCheck = !isCheck;
            setPolicy(isCheck);
        } else if (id == R.id.btnAgreeAndContinue) {
            if (isCheck) {
                SharedPreferencesManager.getInstance().setBooleanValue(Constants.IS_FIRST, false);
                startActivity(new Intent(activity, PermissionActivity.class));
                finish();
            } else {
                Toast.makeText(activity, getString(R.string.please_check_the_privacy_policy), Toast.LENGTH_SHORT).show();
            }
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
                        SharedPreferencesManager.getInstance().setStringValue(Constants.PRIVACY_POLICY, adsData.getPrivacyPolicy());

                        bannerId = adsData.getBannerId();
                        nativeId = adsData.getNativeId();

                        isAdStart = adsData.isAdStart();
                        isShowBanner = adsData.isShowBanner();
                        isShowNative = adsData.isShowNative();
                        isCustomizeId = adsData.isCustomizeId();

                        AdActivityData privacyPolicyActivity = adsData.getPrivacyPolicyActivity();

                        if (privacyPolicyActivity != null) {
                            privacyPolicyAdStart = privacyPolicyActivity.isAdStart();
                            privacyPolicyAdType = privacyPolicyActivity.getAdType();
                            isPrivacyPolicyAdaptiveBanner = privacyPolicyActivity.isAdaptiveBanner();
                            privacyPolicyAdaptiveBannerId = privacyPolicyActivity.getAdaptiveBannerId();
                            privacyPolicyNativeId = privacyPolicyActivity.getNativeId();
                        }

                        if (isAdStart) {
                            MobileAds.initialize(activity);

                            BannerAD.getInstance().init(activity, isShowBanner, isCustomizeId, bannerId);
                            NativeAD.getInstance().init(activity, isShowNative, isCustomizeId, nativeId);

                            boolean isAdStart = privacyPolicyAdStart;
                            String adType = privacyPolicyAdType;
                            boolean isAdaptiveBanner = isPrivacyPolicyAdaptiveBanner;
                            String adaptiveBannerId = privacyPolicyAdaptiveBannerId;
                            String nativeId = privacyPolicyNativeId;

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

    private void setPolicyLink() {
        if (Utils.isNetworkConnected(activity)) {
            mDatabase = FirebaseDatabase.getInstance();
            mDatabaseReference = mDatabase.getReference("ads_data");

            mDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    AdsData adsData = snapshot.getValue(AdsData.class);
                    if (adsData != null) {
                        SharedPreferencesManager.getInstance().setStringValue(Constants.PRIVACY_POLICY, adsData.getPrivacyPolicy());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        }
    }

    private void setPolicy(boolean isCheck) {
        if (isCheck) {
            binding.ivPolicy.setImageResource(R.drawable.ic_check);
        } else {
            binding.ivPolicy.setImageResource(R.drawable.ic_uncheck);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NativeAD.getInstance().destroy();
    }
}