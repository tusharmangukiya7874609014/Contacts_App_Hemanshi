package com.contactshandlers.contactinfoall.ui.activity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.provider.BlockedNumberContract;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.adapter.BlockedNumbersAdapter;
import com.contactshandlers.contactinfoall.ads.BannerAD;
import com.contactshandlers.contactinfoall.ads.InterstitialAD;
import com.contactshandlers.contactinfoall.ads.NativeAD;
import com.contactshandlers.contactinfoall.databinding.ActivityBlockedNumberBinding;
import com.contactshandlers.contactinfoall.databinding.DialogUnblockBinding;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.helper.SharedPreferencesManager;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.listeners.AdCallback;
import com.contactshandlers.contactinfoall.listeners.OnItemClickListener;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.ArrayList;
import java.util.List;

public class BlockedNumberActivity extends BaseActivity implements View.OnClickListener {

    private ActivityBlockedNumberBinding binding;
    private final BlockedNumberActivity activity = BlockedNumberActivity.this;
    private boolean isUnknown;
    private BlockedNumbersAdapter adapter;
    private List<String> blockedNumbers = new ArrayList<>();
    private Dialog dialog;
    private DialogUnblockBinding unblockBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityBlockedNumberBinding.inflate(getLayoutInflater());
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
        binding.included.tvHeading.setText(getString(R.string.blocked_numbers));
        isUnknown = SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_UNKNOWN, false);
        binding.switchUnknown.setChecked(isUnknown);

        boolean isAdStart = SharedPreferencesManager.getInstance().getBooleanValue(Constants.BLOCK_NUMBER_ACTIVITY_AD_START, false);
        String adType = SharedPreferencesManager.getInstance().getStringValue(Constants.BLOCK_NUMBER_ACTIVITY_AD_TYPE, "");
        boolean isAdaptiveBanner = SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_BLOCK_NUMBER_ADAPTIVE_BANNER, false);
        String adaptiveBannerId = SharedPreferencesManager.getInstance().getStringValue(Constants.BLOCK_NUMBER_ADAPTIVE_BANNER_ID, "");
        String nativeId = SharedPreferencesManager.getInstance().getStringValue(Constants.BLOCK_NUMBER_NATIVE_ID, "");

        if (isAdStart) {
            if (isAdaptiveBanner) {
                BannerAD.getInstance().showBannerAd(activity, binding.adLayout.llBanner, binding.adLayout.llBannerLayout, binding.adLayout.shimmerBanner, adaptiveBannerId);
            } else {
                ShimmerFrameLayout shimmer = getShimmerFrameLayout(adType);
                NativeAD.getInstance().showNativeAd(activity, binding.adLayout.flNativePlaceHolder, binding.adLayout.llNativeLayout, shimmer, adType, nativeId);
            }
        }

        binding.switchUnknown.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferencesManager.getInstance().setBooleanValue(Constants.IS_UNKNOWN, isChecked);
                handleUnknownNumberBlocking(isChecked);
            }
        });

        blockedNumbers = getBlockedNumbers();
        binding.rvBlockedNumbers.setLayoutManager(new LinearLayoutManager(activity));
        adapter = new BlockedNumbersAdapter(activity, blockedNumbers, new OnItemClickListener() {
            @Override
            public void onClick(String number, int position) {
                showUnblockDialog(number);
            }
        });
        binding.rvBlockedNumbers.setAdapter(adapter);

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

    private List<String> getBlockedNumbers() {
        List<String> blockedNumbers = new ArrayList<>();

        try {
            Cursor cursor = getContentResolver().query(
                    BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                    new String[]{BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER},
                    null,
                    null,
                    null
            );

            if (cursor != null) {
                int numberIndex = cursor.getColumnIndex(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER);
                while (cursor.moveToNext()) {
                    String number = cursor.getString(numberIndex);
                    blockedNumbers.add(number);
                }
                cursor.close();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        return blockedNumbers;
    }

    @SuppressLint("SetTextI18n")
    private void showUnblockDialog(String number) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        dialog = new Dialog(activity);
        unblockBinding = DialogUnblockBinding.inflate(getLayoutInflater());
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setContentView(unblockBinding.getRoot());
        dialog.setCancelable(true);

        unblockBinding.tvTitle.setText(getString(R.string.unblock) + " " + number + "?");

        unblockBinding.btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        unblockBinding.btnUnblock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.unblockNumber(activity, number);

                int currentPosition = -1;
                for (int i = 0; i < blockedNumbers.size(); i++) {
                    if (blockedNumbers.get(i).equals(number)) {
                        currentPosition = i;
                        break;
                    }
                }

                if (currentPosition != -1) {
                    blockedNumbers.remove(currentPosition);
                    adapter.notifyItemRemoved(currentPosition);
                    adapter.notifyItemRangeChanged(currentPosition, blockedNumbers.size());
                } else {
                    refreshBlockedNumbers();
                }

                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void refreshBlockedNumbers() {
        blockedNumbers.clear();
        blockedNumbers.addAll(getBlockedNumbers());
        adapter.notifyDataSetChanged();
    }

    private void handleUnknownNumberBlocking(boolean shouldBlock) {
        try {
            if (shouldBlock) {
                ContentValues values = new ContentValues();
                values.put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, "unknown");
                getContentResolver().insert(BlockedNumberContract.BlockedNumbers.CONTENT_URI, values);
            } else {
                String selection = BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER + " = ?";
                String[] selectionArgs = new String[] {"unknown"};
                getContentResolver().delete(BlockedNumberContract.BlockedNumbers.CONTENT_URI, selection, selectionArgs);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NativeAD.getInstance().destroy();
    }
}