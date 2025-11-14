package com.contactshandlers.contactinfoall.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.ads.BannerAD;
import com.contactshandlers.contactinfoall.ads.InterstitialAD;
import com.contactshandlers.contactinfoall.ads.NativeAD;
import com.contactshandlers.contactinfoall.databinding.ActivityMergeSelectionBinding;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.helper.SharedPreferencesManager;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.helper.ContactManager;
import com.contactshandlers.contactinfoall.listeners.AdCallback;
import com.contactshandlers.contactinfoall.model.DuplicateGroup;
import com.contactshandlers.contactinfoall.adapter.DuplicateGroupAdapter;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MergeSelectionActivity extends BaseActivity implements View.OnClickListener{

    private ActivityMergeSelectionBinding binding;
    private final MergeSelectionActivity activity = MergeSelectionActivity.this;
    private DuplicateGroupAdapter adapter;
    private ContactManager contactManager;
    private String mergeType, title;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMergeSelectionBinding.inflate(getLayoutInflater());
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
        binding.included.ivBack.setOnClickListener(this);
    }

    private void init(){
        contactManager = new ContactManager(activity);
        binding.included.tvHeading.setText(getString(R.string.merge_duplicate_contact));

        title = getIntent().getStringExtra(Constants.TITLE);
        mergeType = getIntent().getStringExtra(Constants.MERGE_TYPE);
        if (title != null && !title.isEmpty()) {
            binding.included.tvHeading.setText(title);
        }

        setupRecyclerView();
        loadAccountsWithDuplicates();

        boolean isAdStart = SharedPreferencesManager.getInstance().getBooleanValue(Constants.MERGE_SELECTION_ACTIVITY_AD_START, false);
        String adType = SharedPreferencesManager.getInstance().getStringValue(Constants.MERGE_SELECTION_ACTIVITY_AD_TYPE, "");
        boolean isAdaptiveBanner = SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_MERGE_SELECTION_ADAPTIVE_BANNER, false);
        String adaptiveBannerId = SharedPreferencesManager.getInstance().getStringValue(Constants.MERGE_SELECTION_ADAPTIVE_BANNER_ID, "");
        String nativeId = SharedPreferencesManager.getInstance().getStringValue(Constants.MERGE_SELECTION_NATIVE_ID, "");

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

    private void setupRecyclerView() {
        binding.rvSelectGroup.setLayoutManager(new LinearLayoutManager(activity));
        adapter = new DuplicateGroupAdapter(activity, duplicateGroup -> {
            InterstitialAD.getInstance().showInterstitial(activity, new AdCallback() {
                @Override
                public void callbackCall() {
                    Intent intent = new Intent(activity, DuplicateListActivity.class);
                    intent.putExtra(Constants.TITLE, title);
                    intent.putExtra(Constants.ACCOUNT_GROUP, duplicateGroup);
                    intent.putExtra(Constants.MERGE_TYPE, mergeType);
                    startActivity(intent);
                }
            });
        });
        binding.rvSelectGroup.setAdapter(adapter);
    }

    private void loadAccountsWithDuplicates() {
        showLoading(true);

        executor.execute(() -> {
            List<DuplicateGroup> accountsWithDuplicates = contactManager.getAccountsWithDuplicates(mergeType);

            handler.post(() -> {
                showLoading(false);
                if (accountsWithDuplicates.isEmpty()) {
                    showEmptyState();
                } else {
                    showNormalState();
                    adapter.setDuplicateGroups(accountsWithDuplicates);
                }
            });
        });
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.rvSelectGroup.setVisibility(show ? View.GONE : View.VISIBLE);
        binding.tv1.setVisibility(show ? View.GONE : binding.tv1.getVisibility());
    }

    private void showEmptyState() {
        binding.clNoDuplicateContacts.setVisibility(View.VISIBLE);
        binding.tv1.setVisibility(View.GONE);
        binding.rvSelectGroup.setVisibility(View.GONE);
    }

    private void showNormalState() {
        binding.clNoDuplicateContacts.setVisibility(View.GONE);
        binding.tv1.setVisibility(View.VISIBLE);
        binding.rvSelectGroup.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (contactManager != null) {
            contactManager.clearCache();
        }
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (contactManager != null) {
            contactManager.clearCache();
            loadAccountsWithDuplicates();
        }
        NativeAD.getInstance().destroy();
    }
}