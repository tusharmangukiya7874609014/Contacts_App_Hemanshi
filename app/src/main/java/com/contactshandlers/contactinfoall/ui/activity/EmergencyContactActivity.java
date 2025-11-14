package com.contactshandlers.contactinfoall.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.adapter.EmergencyContactsAdapter;
import com.contactshandlers.contactinfoall.ads.BannerAD;
import com.contactshandlers.contactinfoall.ads.InterstitialAD;
import com.contactshandlers.contactinfoall.ads.NativeAD;
import com.contactshandlers.contactinfoall.databinding.ActivityEmergencyContactBinding;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.helper.SharedPreferencesManager;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.listeners.AdCallback;
import com.contactshandlers.contactinfoall.model.EmergencyContact;
import com.contactshandlers.contactinfoall.room.EmergencyContactViewModel;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.ArrayList;
import java.util.List;

public class EmergencyContactActivity extends BaseActivity implements View.OnClickListener {

    private ActivityEmergencyContactBinding binding;
    private final EmergencyContactActivity activity = EmergencyContactActivity.this;
    private List<EmergencyContact> emergencyContacts = new ArrayList<>();
    private EmergencyContactsAdapter adapter;
    private EmergencyContactViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityEmergencyContactBinding.inflate(getLayoutInflater());
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
        binding.included.tvHeading.setText(getString(R.string.emergency_contacts));
        binding.rvEmergencyContact.setLayoutManager(new LinearLayoutManager(activity));
        viewModel = new ViewModelProvider(this).get(EmergencyContactViewModel.class);

        boolean isAdStart = SharedPreferencesManager.getInstance().getBooleanValue(Constants.EMERGENCY_CONTACT_ACTIVITY_AD_START, false);
        String adType = SharedPreferencesManager.getInstance().getStringValue(Constants.EMERGENCY_CONTACT_ACTIVITY_AD_TYPE, "");
        boolean isAdaptiveBanner = SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_EMERGENCY_CONTACT_ADAPTIVE_BANNER, false);
        String adaptiveBannerId = SharedPreferencesManager.getInstance().getStringValue(Constants.EMERGENCY_CONTACT_ADAPTIVE_BANNER_ID, "");
        String nativeId = SharedPreferencesManager.getInstance().getStringValue(Constants.EMERGENCY_CONTACT_NATIVE_ID, "");

        if (isAdStart) {
            if (isAdaptiveBanner) {
                BannerAD.getInstance().showBannerAd(activity, binding.adLayout.llBanner, binding.adLayout.llBannerLayout, binding.adLayout.shimmerBanner, adaptiveBannerId);
            } else {
                ShimmerFrameLayout shimmer = getShimmerFrameLayout(adType);
                NativeAD.getInstance().showNativeAd(activity, binding.adLayout.flNativePlaceHolder, binding.adLayout.llNativeLayout, shimmer, adType, nativeId);
            }
        }

        adapter = new EmergencyContactsAdapter(activity, emergencyContacts);
        binding.rvEmergencyContact.setAdapter(adapter);
        getEmergencyContacts();

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
        binding.btnAdd.setOnClickListener(this);
        binding.btnAddEmergency.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.ivBack) {
            getOnBackPressedDispatcher().onBackPressed();
        } else if (id == R.id.btnAdd || id == R.id.btnAddEmergency) {
            InterstitialAD.getInstance().showInterstitial(activity, new AdCallback() {
                @Override
                public void callbackCall() {
                    Intent intent = new Intent(activity, AddEmergencyContactActivity.class);
                    startActivity(intent);
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

    private void getEmergencyContacts() {
        viewModel.getAllContacts().observe(this, new Observer<List<EmergencyContact>>() {
            @Override
            public void onChanged(List<EmergencyContact> emergencyContact) {
                emergencyContacts = emergencyContact;
                adapter.setEmergencyContacts(emergencyContacts);
                if (emergencyContacts.isEmpty()) {
                    binding.clNoEmergency.setVisibility(View.VISIBLE);
                    binding.btnAddEmergency.setVisibility(View.VISIBLE);
                    binding.rvEmergencyContact.setVisibility(View.GONE);
                    binding.btnAdd.setVisibility(View.GONE);
                } else {
                    binding.clNoEmergency.setVisibility(View.GONE);
                    binding.btnAddEmergency.setVisibility(View.GONE);
                    binding.rvEmergencyContact.setVisibility(View.VISIBLE);
                    binding.btnAdd.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NativeAD.getInstance().destroy();
    }
}