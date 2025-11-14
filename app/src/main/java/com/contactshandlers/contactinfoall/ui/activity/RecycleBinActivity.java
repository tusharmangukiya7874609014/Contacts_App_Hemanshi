package com.contactshandlers.contactinfoall.ui.activity;

import android.Manifest;
import android.app.Dialog;
import android.content.ContentProviderOperation;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.TypefaceSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.adapter.BinContactAdapter;
import com.contactshandlers.contactinfoall.ads.BannerAD;
import com.contactshandlers.contactinfoall.ads.InterstitialAD;
import com.contactshandlers.contactinfoall.ads.NativeAD;
import com.contactshandlers.contactinfoall.databinding.ActivityRecycleBinBinding;
import com.contactshandlers.contactinfoall.databinding.DialogDeleteForeverBinding;
import com.contactshandlers.contactinfoall.databinding.DialogEmptyBinBinding;
import com.contactshandlers.contactinfoall.databinding.DialogRestoreBinding;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.helper.SharedPreferencesManager;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.listeners.AdCallback;
import com.contactshandlers.contactinfoall.model.BinContactEntity;
import com.contactshandlers.contactinfoall.model.PhoneItem;
import com.contactshandlers.contactinfoall.room.BinContactViewModel;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RecycleBinActivity extends BaseActivity implements View.OnClickListener {

    private ActivityRecycleBinBinding binding;
    private final RecycleBinActivity activity = RecycleBinActivity.this;
    public static boolean isSelectionMode = false;
    public static Set<BinContactEntity> binSelectedContacts = new HashSet<>();
    private List<BinContactEntity> binContacts;
    private BinContactViewModel viewModel;
    private BinContactAdapter adapter;
    private static TextView tvCounting;
    private static LinearLayout llHeader;
    private static ImageView ivMoreOptions, ivClose;;
    private static ConstraintLayout clSelectionMode;
    private Dialog dialog;
    private DialogEmptyBinBinding emptyBinBinding;
    private DialogDeleteForeverBinding deleteBinding;
    private DialogRestoreBinding restoreBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityRecycleBinBinding.inflate(getLayoutInflater());
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
        binding.included.tvHeading.setText(getString(R.string.recycle_bin));

        tvCounting = findViewById(R.id.tvCounting);
        llHeader = findViewById(R.id.llHeader);
        clSelectionMode = findViewById(R.id.clSelectionMode);
        ivMoreOptions = findViewById(R.id.ivMoreOptions);
        ivClose = findViewById(R.id.ivClose);

        viewModel = new ViewModelProvider(this).get(BinContactViewModel.class);

        boolean isAdStart = SharedPreferencesManager.getInstance().getBooleanValue(Constants.RECYCLE_BIN_ACTIVITY_AD_START, false);
        String adType = SharedPreferencesManager.getInstance().getStringValue(Constants.RECYCLE_BIN_ACTIVITY_AD_TYPE, "");
        boolean isAdaptiveBanner = SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_RECYCLE_BIN_ADAPTIVE_BANNER, false);
        String adaptiveBannerId = SharedPreferencesManager.getInstance().getStringValue(Constants.RECYCLE_BIN_ADAPTIVE_BANNER_ID, "");
        String nativeId = SharedPreferencesManager.getInstance().getStringValue(Constants.RECYCLE_BIN_NATIVE_ID, "");

        if (isAdStart) {
            if (isAdaptiveBanner) {
                BannerAD.getInstance().showBannerAd(activity, binding.adLayout.llBanner, binding.adLayout.llBannerLayout, binding.adLayout.shimmerBanner, adaptiveBannerId);
            } else {
                ShimmerFrameLayout shimmer = getShimmerFrameLayout(adType);
                NativeAD.getInstance().showNativeAd(activity, binding.adLayout.flNativePlaceHolder, binding.adLayout.llNativeLayout, shimmer, adType, nativeId);
            }
        }

        adapter = new BinContactAdapter(activity, viewModel);
        binding.rvRecycleData.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRecycleData.setAdapter(adapter);

        viewModel.autoDeleteOldContacts();

        viewModel.getAllContacts().observe(this, new Observer<List<BinContactEntity>>() {
            @Override
            public void onChanged(List<BinContactEntity> binContactEntities) {
                binContacts = binContactEntities;
                adapter.setList(binContactEntities);
                if (binContactEntities.isEmpty()) {
                    binding.clNoContacts.setVisibility(View.VISIBLE);
                    binding.rvRecycleData.setVisibility(View.GONE);
                    binding.btnEmptyBin.setVisibility(View.GONE);
                    binding.ivMoreOptions.setVisibility(View.GONE);
                } else {
                    binding.clNoContacts.setVisibility(View.GONE);
                    binding.rvRecycleData.setVisibility(View.VISIBLE);
                    binding.btnEmptyBin.setVisibility(View.VISIBLE);
                    binding.ivMoreOptions.setVisibility(View.VISIBLE);
                }
            }
        });

        getOnBackPressedDispatcher().addCallback(activity, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                InterstitialAD.getInstance().showInterstitial(activity, new AdCallback() {
                    @Override
                    public void callbackCall() {
                        binding.ivClose.performClick();
                        finish();
                    }
                });
            }
        });
    }

    private void initListener() {
        binding.included.ivBack.setOnClickListener(this);
        binding.ivMoreOptions.setOnClickListener(this);
        binding.ivClose.setOnClickListener(this);
        binding.ivRestore.setOnClickListener(this);
        binding.ivDelete.setOnClickListener(this);
        binding.ivSelection.setOnClickListener(this);
        binding.btnEmptyBin.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.ivBack) {
            binding.ivClose.performClick();
            getOnBackPressedDispatcher().onBackPressed();
        } else if (id == R.id.ivMoreOptions) {
            showPopupMenu(v, 1);
        } else if (id == R.id.ivClose) {
            isSelectionMode = false;
            binSelectedContacts.clear();
            binding.clSelectionMode.setVisibility(View.GONE);
            binding.llHeader.setVisibility(View.VISIBLE);
            deselectAll();
            adapter.notifyDataSetChanged();
        } else if (id == R.id.ivRestore) {
            if (!binSelectedContacts.isEmpty()) {
                showRestoreDialog();
            } else {
                Toast.makeText(activity, getString(R.string.please_select_at_least_one_contact), Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.ivDelete) {
            if (!binSelectedContacts.isEmpty()) {
                showDeleteDialog();
            } else {
                Toast.makeText(activity, getString(R.string.please_select_at_least_one_contact), Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.ivSelection) {
            showPopupMenu(v, 0);
        } else if (id == R.id.btnEmptyBin) {
            showEmptyBinDialog();
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

    public void restoreSelectedContacts(Set<BinContactEntity> selectedContacts) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(activity, getString(R.string.please_allow_permission_from_settings), Toast.LENGTH_SHORT).show();
            return;
        }

        int count = 0;
        for (BinContactEntity binContact : selectedContacts) {
            ArrayList<ContentProviderOperation> ops = new ArrayList<>();

            int rawContactInsertIndex = ops.size();
            ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build());

            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, binContact.getName())
                    .build());

            for (PhoneItem phone : binContact.getPhoneList()) {
                int phoneType = Utils.convertStringPhoneTypeToInt(phone.getPhoneType());

                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone.getPhoneNumber())
                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phoneType)
                        .build());
            }

            if (binContact.getImageUri() != null && !binContact.getImageUri().isEmpty()) {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(Uri.parse(binContact.getImageUri()));
                    if (inputStream != null) {
                        byte[] photoByte = getBytes(inputStream);
                        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoByte)
                                .build());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            try {
                viewModel.delete(binContact);
                getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
                count++;
            } catch (RemoteException | OperationApplicationException e) {
                e.printStackTrace();

            }

        }
        if (count == selectedContacts.size()) {
            Toast.makeText(activity, getString(R.string.contact_restored), Toast.LENGTH_SHORT).show();
            binding.ivClose.performClick();
        } else {
            Toast.makeText(activity, getString(R.string.failed_to_restore_contact), Toast.LENGTH_SHORT).show();
        }
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    private void showEmptyBinDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        dialog = new Dialog(activity);
        emptyBinBinding = DialogEmptyBinBinding.inflate(getLayoutInflater());
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setContentView(emptyBinBinding.getRoot());
        dialog.setCancelable(true);

        emptyBinBinding.btnClose.setOnClickListener(v -> dialog.dismiss());
        emptyBinBinding.btnCancel.setOnClickListener(v -> dialog.dismiss());

        emptyBinBinding.btnRemove.setOnClickListener(v -> {
            viewModel.clearAll();
            binding.ivClose.performClick();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showRestoreDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        dialog = new Dialog(activity);
        restoreBinding = DialogRestoreBinding.inflate(getLayoutInflater());
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setContentView(restoreBinding.getRoot());
        dialog.setCancelable(true);

        restoreBinding.btnClose.setOnClickListener(v -> dialog.dismiss());
        restoreBinding.btnCancel.setOnClickListener(v -> dialog.dismiss());

        restoreBinding.btnRestore.setOnClickListener(v -> {
            restoreSelectedContacts(binSelectedContacts);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showDeleteDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        dialog = new Dialog(activity);
        deleteBinding = DialogDeleteForeverBinding.inflate(getLayoutInflater());
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setContentView(deleteBinding.getRoot());
        dialog.setCancelable(true);

        deleteBinding.btnClose.setOnClickListener(v -> dialog.dismiss());
        deleteBinding.btnCancel.setOnClickListener(v -> dialog.dismiss());

        deleteBinding.btnDelete.setOnClickListener(v -> {
            for (BinContactEntity contact : binSelectedContacts) {
                viewModel.delete(contact);
            }
            binding.ivClose.performClick();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showPopupMenu(View view, int selectType) {
        PopupMenu popup = new PopupMenu(activity, view);
        MenuInflater inflater = popup.getMenuInflater();
        if (selectType == 1) {
            inflater.inflate(R.menu.contacts_options_menu, popup.getMenu());
        } else if (selectType == 0) {
            inflater.inflate(R.menu.selection_mode_options_menu, popup.getMenu());
        }

        MenuItem sortItem = popup.getMenu().findItem(R.id.action_sort);
        if (sortItem != null) {
            sortItem.setVisible(false);
        }

        boolean isAllSelected;
        MenuItem selectUnselectItem = popup.getMenu().findItem(R.id.action_select_unselect_all);
        if (selectUnselectItem != null) {
            if (binContacts.size() == binSelectedContacts.size()) {
                isAllSelected = true;
                selectUnselectItem.setTitle(getString(R.string.deselect_all));
            } else {
                isAllSelected = false;
                selectUnselectItem.setTitle(getString(R.string.select_all));
            }
        } else {
            isAllSelected = false;
        }

        try {
            Field mPopup = PopupMenu.class.getDeclaredField("mPopup");
            mPopup.setAccessible(true);
            Object menuPopupHelper = mPopup.get(popup);
            Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
            Method setForceShowIcon = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
            setForceShowIcon.invoke(menuPopupHelper, true);

            Method getListView = classPopupHelper.getMethod("getListView");
            ListView listView = (ListView) getListView.invoke(menuPopupHelper);

            if (listView != null) {
                Drawable background = ContextCompat.getDrawable(activity, R.drawable.bg_popup);
                listView.setBackground(background);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Typeface typeface = ResourcesCompat.getFont(activity, R.font.poppins_medium);
        Menu menu = popup.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            MenuItem menuItem = menu.getItem(i);
            SpannableString styledTitle = new SpannableString(menuItem.getTitle());

            styledTitle.setSpan(new ForegroundColorSpan(activity.getColor(R.color.primary_font)), 0, styledTitle.length(), 0);

            assert typeface != null;
            styledTitle.setSpan(new TypefaceSpan(typeface), 0, styledTitle.length(), 0);

            menuItem.setTitle(styledTitle);
        }

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_select) {
                isSelectionMode = true;
                binding.tvCounting.setText(String.valueOf(binSelectedContacts.size()));
                binding.clSelectionMode.setVisibility(View.VISIBLE);
                binding.llHeader.setVisibility(View.INVISIBLE);
                binding.ivMoreOptions.setVisibility(View.GONE);
                return true;
            } else if (item.getItemId() == R.id.action_select_all) {
                isSelectionMode = true;
                binding.tvCounting.setText(String.valueOf(binSelectedContacts.size()));
                binding.clSelectionMode.setVisibility(View.VISIBLE);
                binding.llHeader.setVisibility(View.INVISIBLE);
                binding.ivMoreOptions.setVisibility(View.GONE);
                selectAll();
                if (binSelectedContacts.isEmpty()){
                    binding.ivClose.performClick();
                }
                return true;
            } else if (item.getItemId() == R.id.action_settings) {
                startActivity(new Intent(activity, SettingsActivity.class));
                return true;
            } else if (item.getItemId() == R.id.action_select_unselect_all) {
                if (isAllSelected) {
                    deselectAll();
                } else {
                    selectAll();
                }
                return true;
            } else {
                return false;
            }
        });
        popup.show();
    }

    private void deselectAll() {
        if (binContacts != null) {
            for (BinContactEntity contact : binContacts) {
                contact.setSelected(false);
            }
        }
        binSelectedContacts.clear();
        isSelectionMode = false;
        binding.clSelectionMode.setVisibility(View.GONE);
        binding.llHeader.setVisibility(View.VISIBLE);
        adapter.notifyDataSetChanged();
        tvCounting.setText(String.valueOf(binSelectedContacts.size()));
    }

    private void selectAll() {
        binSelectedContacts.clear();
        if (binContacts != null) {
            for (BinContactEntity contact : binContacts) {
                contact.setSelected(true);
                binSelectedContacts.add(contact);
            }
        }
        adapter.notifyDataSetChanged();
        tvCounting.setText(String.valueOf(binSelectedContacts.size()));
    }

    public static void changeText() {
        isSelectionMode = true;
        tvCounting.setText(String.valueOf(binSelectedContacts.size()));
        clSelectionMode.setVisibility(View.VISIBLE);
        llHeader.setVisibility(View.INVISIBLE);
        ivMoreOptions.setVisibility(View.GONE);
        if (binSelectedContacts.isEmpty()){
            ivClose.performClick();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NativeAD.getInstance().destroy();
    }
}