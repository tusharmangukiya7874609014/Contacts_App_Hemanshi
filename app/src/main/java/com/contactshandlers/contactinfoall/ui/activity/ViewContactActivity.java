package com.contactshandlers.contactinfoall.ui.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.adapter.CMVAdapter;
import com.contactshandlers.contactinfoall.adapter.ContactInfoAdapter;
import com.contactshandlers.contactinfoall.ads.BannerAD;
import com.contactshandlers.contactinfoall.ads.InterstitialAD;
import com.contactshandlers.contactinfoall.ads.NativeAD;
import com.contactshandlers.contactinfoall.databinding.ActivityViewContactBinding;
import com.contactshandlers.contactinfoall.databinding.DialogCmwBinding;
import com.contactshandlers.contactinfoall.databinding.DialogDeleteBinding;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.helper.DefaultDialerUtils;
import com.contactshandlers.contactinfoall.helper.SharedPreferencesManager;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.listeners.AdCallback;
import com.contactshandlers.contactinfoall.listeners.CallListener;
import com.contactshandlers.contactinfoall.listeners.OnItemClickListener;
import com.contactshandlers.contactinfoall.model.BinContactEntity;
import com.contactshandlers.contactinfoall.model.PhoneItem;
import com.contactshandlers.contactinfoall.model.RecentViewedContact;
import com.contactshandlers.contactinfoall.room.BinContactViewModel;
import com.contactshandlers.contactinfoall.room.ContactDao;
import com.contactshandlers.contactinfoall.room.ContactDatabase;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ViewContactActivity extends BaseActivity implements View.OnClickListener {
    private ActivityViewContactBinding binding;
    private final ViewContactActivity activity = ViewContactActivity.this;
    private ContactInfoAdapter adapter;
    private List<PhoneItem> phoneList = new ArrayList<>();
    private Dialog dialog;
    private DialogCmwBinding dialogBinding;
    private DialogDeleteBinding deleteBinding;
    private static final int REQUEST_CALL_PERMISSION = 1;
    private static final int REQUEST_CONTACTS_PERMISSION = 2;
    private ActivityResultLauncher<Intent> ringtoneLauncher;
    private boolean isFavourite, isBlocked;
    private String contactId;
    private String contactName;
    private String emailAddress;
    private ContactDao contactDao;
    private ExecutorService executorService;
    private boolean hasContactsPermission = false;
    private ActivityResultLauncher<Intent> defaultDialerLauncher;

    private void initDefaultDialerLauncher() {
        defaultDialerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        DefaultDialerUtils.handleDefaultDialerResult(activity, result.getResultCode(),
                                new DefaultDialerUtils.DefaultDialerCallback() {
                                    @Override
                                    public void onDefaultDialerSet() {
                                        setBlockStatus();
                                    }
                                });
                    }
                }
        );
    }

    private void requestDefaultDialer() {
        DefaultDialerUtils.requestDefaultDialer(activity, defaultDialerLauncher);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityViewContactBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Utils.setStatusBarColor(activity);
        initListener();

        ringtoneLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri ringtoneUri = result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    if (ringtoneUri != null) {
                        if (contactId != null) {
                            setContactRingtone(contactId, ringtoneUri);
                        }
                    }
                }
            }
        });

        initDefaultDialerLauncher();

        contactDao = ContactDatabase.getInstance(getApplication()).contactDao();
        executorService = Executors.newSingleThreadExecutor();

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

        checkContactsPermission();

        boolean isAdStart = SharedPreferencesManager.getInstance().getBooleanValue(Constants.VIEW_CONTACT_ACTIVITY_AD_START, false);
        String adType = SharedPreferencesManager.getInstance().getStringValue(Constants.VIEW_CONTACT_ACTIVITY_AD_TYPE, "");
        boolean isAdaptiveBanner = SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_VIEW_CONTACT_ADAPTIVE_BANNER, false);
        String adaptiveBannerId = SharedPreferencesManager.getInstance().getStringValue(Constants.VIEW_CONTACT_ADAPTIVE_BANNER_ID, "");
        String nativeId = SharedPreferencesManager.getInstance().getStringValue(Constants.VIEW_CONTACT_NATIVE_ID, "");

        if (isAdStart) {
            if (isAdaptiveBanner) {
                BannerAD.getInstance().showBannerAd(activity, binding.adLayout.llBanner, binding.adLayout.llBannerLayout, binding.adLayout.shimmerBanner, adaptiveBannerId);
            } else {
                ShimmerFrameLayout shimmer = getShimmerFrameLayout(adType);
                NativeAD.getInstance().showNativeAd(activity, binding.adLayout.flNativePlaceHolder, binding.adLayout.llNativeLayout, shimmer, adType, nativeId);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasContactsPermission) {
            init();
        }
    }

    @SuppressLint("SetTextI18n")
    private void init() {
        contactId = getIntent().getStringExtra(Constants.CONTACT_ID);

        Bitmap photo = Utils.getContactPhoto(activity, contactId);
        if (photo != null) {
            binding.ivProfile.setImageBitmap(photo);
        } else {
            binding.ivProfile.setImageBitmap(Utils.getInitialsBitmap(getColor(R.color.grey_font), Utils.getContactNameById(activity, contactId), getColor(R.color.bg_color)));
        }

        contactName = Utils.getContactNameById(activity, contactId);
        binding.tvName.setText(contactName);
        binding.tvName.setSelected(true);

        if (contactId != null && contactName != null) {
            insertOrUpdateRecentViewed(new RecentViewedContact(contactId, contactName, System.currentTimeMillis()));
            phoneList = Utils.getPhoneNumbersById(activity, contactId);
            if (!phoneList.isEmpty()) {
                binding.rvContacts.setLayoutManager(new LinearLayoutManager(activity));
                adapter = new ContactInfoAdapter(activity, phoneList, new CallListener() {
                    @Override
                    public void onCall(String phoneNumber) {
                        makePhoneCall(phoneNumber);
                    }
                });
                binding.rvContacts.setAdapter(adapter);
            }
        }

        List<String> emailAddresses = Utils.getContactEmails(activity, contactId);
        if (!emailAddresses.isEmpty()) {
            emailAddress = emailAddresses.get(0);
            binding.btnGmail.setImageResource(R.drawable.ic_mail);
            binding.btnGmail.setEnabled(true);
        } else {
            binding.btnGmail.setImageResource(R.drawable.ic_dis_mail);
            binding.btnGmail.setEnabled(false);
        }

        isFavourite = Utils.isContactStarred(activity, contactId);
        if (isFavourite) {
            binding.tvFavourite.setText(getString(R.string.remove_from_favourite_list));
        } else {
            binding.tvFavourite.setText(getString(R.string.add_to_favourite_list));
        }

        setBlockStatus();
    }

    private void setBlockStatus() {
        if (Utils.isDefaultDialer(activity)) {
            isBlocked = Utils.hasAnyBlocked(activity, phoneList);
            if (isBlocked) {
                binding.tvBlock.setText(getString(R.string.unblock_contact));
            } else {
                binding.tvBlock.setText(getString(R.string.block_contact));
            }
        }
    }

    private void initListener() {
        binding.included.ivBack.setOnClickListener(this);
        binding.btnEdit.setOnClickListener(this);
        binding.btnCall.setOnClickListener(this);
        binding.btnMessage.setOnClickListener(this);
        binding.btnWhatsApp.setOnClickListener(this);
        binding.btnGmail.setOnClickListener(this);
        binding.btnDelete.setOnClickListener(this);
        binding.btnShare.setOnClickListener(this);
        binding.btnFavourite.setOnClickListener(this);
        binding.btnRingtone.setOnClickListener(this);
        binding.btnBlock.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.ivBack) {
            getOnBackPressedDispatcher().onBackPressed();
        } else if (id == R.id.btnEdit) {
            InterstitialAD.getInstance().showInterstitial(activity, new AdCallback() {
                @Override
                public void callbackCall() {
                    if (!phoneList.isEmpty()) {
                        startActivity(new Intent(activity, AddEditContactActivity.class)
                                .putExtra(Constants.CONTACT_NUMBER, phoneList.get(0).getPhoneNumber())
                                .putExtra(Constants.CONTACT_NAME, contactName)
                                .putExtra(Constants.TITLE, getString(R.string.edit_contact)));
                    } else {
                        Toast.makeText(activity, getString(R.string.phone_number_not_found), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else if (id == R.id.btnCall) {
            if (phoneList.size() > 1) {
                showDialog(getString(R.string.call));
            } else {
                if (!phoneList.isEmpty()) {
                    String phoneNumber = phoneList.get(0).getPhoneNumber();
                    makePhoneCall(phoneNumber);
                }
            }
        } else if (id == R.id.btnMessage) {
            if (phoneList.size() > 1) {
                showDialog(getString(R.string.message));
            } else {
                if (!phoneList.isEmpty()) {
                    String phoneNumber = phoneList.get(0).getPhoneNumber();
                    Utils.sendSMS(activity, phoneNumber);
                }
            }
        } else if (id == R.id.btnWhatsApp) {
            if (phoneList.size() > 1) {
                showDialog(getString(R.string.whatsapp));
            } else {
                if (!phoneList.isEmpty()) {
                    String phoneNumber = phoneList.get(0).getPhoneNumber();
                    openWhatsApp(phoneNumber);
                }
            }
        } else if (id == R.id.btnGmail) {
            Utils.sendEmail(activity, emailAddress);
        } else if (id == R.id.btnDelete) {
            if (hasContactsPermission) {
                showDeleteDialog();
            }
        } else if (id == R.id.btnShare) {
            if (hasContactsPermission) {
                shareContact(contactName, phoneList);
            }
        } else if (id == R.id.btnFavourite) {
            if (hasContactsPermission) {
                addToFavourites();
            }
        } else if (id == R.id.btnRingtone) {
            if (hasContactsPermission) {
                requestPermissions();
            }
        } else if (id == R.id.btnBlock) {
            if (hasContactsPermission) {
                blockUnblockContact();
            }
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

    public void insertOrUpdateRecentViewed(RecentViewedContact contact) {
        executorService.execute(() -> {
            RecentViewedContact existingContact = contactDao.getRecentViewedById(contact.getContactId());
            if (existingContact == null) {
                contactDao.insertRecentViewed(contact);
            } else {
                contactDao.updateRecentViewed(contact.getContactId(), contact.getName(), contact.getViewedTimestamp());
            }
        });
    }

    private void showDialog(String title) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        dialog = new Dialog(activity);
        dialogBinding = DialogCmwBinding.inflate(getLayoutInflater());
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setContentView(dialogBinding.getRoot());
        dialog.setCancelable(true);

        dialogBinding.btnClose.setOnClickListener(v -> dialog.dismiss());
        dialogBinding.rvCmv.setLayoutManager(new LinearLayoutManager(activity));
        CMVAdapter adapter = new CMVAdapter(activity, phoneList, title, new OnItemClickListener() {
            @Override
            public void onClick(String number, int position) {
                dialog.dismiss();
                if (title.equals(getString(R.string.call))) {
                    makePhoneCall(number);
                } else if (title.equals(getString(R.string.message))) {
                    Utils.sendSMS(activity, number);
                } else if (title.equals(getString(R.string.whatsapp))) {
                    openWhatsApp(number);
                }
            }
        });
        dialogBinding.rvCmv.setAdapter(adapter);

        dialog.show();
    }

    private void openWhatsApp(String phoneNumber) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://wa.me/" + phoneNumber));
            intent.setPackage("com.whatsapp");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(activity, getString(R.string.whatsapp_not_installed), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(activity, getString(R.string.whatsapp_not_installed), Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        dialog = new Dialog(activity);
        deleteBinding = DialogDeleteBinding.inflate(getLayoutInflater());
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
            deleteContact(contactId);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void deleteContact(String contactId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            long timestampDeleted = System.currentTimeMillis();

            BinContactEntity binContact = new BinContactEntity(
                    Utils.getContactNameById(activity, contactId),
                    Utils.getContactPhotoUri(activity, contactId),
                    phoneList,
                    timestampDeleted
            );
            BinContactViewModel viewModel = new ViewModelProvider(this).get(BinContactViewModel.class);

            try {
                ArrayList<ContentProviderOperation> operations = new ArrayList<>();

                operations.add(ContentProviderOperation
                        .newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(ContactsContract.Data.CONTACT_ID + " = ?", new String[]{contactId})
                        .build());

                operations.add(ContentProviderOperation
                        .newDelete(ContactsContract.RawContacts.CONTENT_URI)
                        .withSelection(ContactsContract.RawContacts.CONTACT_ID + " = ?", new String[]{contactId})
                        .build());

                ContentProviderResult[] results = getContentResolver().applyBatch(ContactsContract.AUTHORITY, operations);

                int updatedRows = results.length;

                if (contactDao != null) {
                    if (contactDao.getRecentAddedById(contactId) != null) {
                        contactDao.deleteRecentAddedById(contactId);
                    }
                    if (contactDao.getRecentViewedById(contactId) != null) {
                        contactDao.deleteRecentViewedById(contactId);
                    }
                }

                new Handler(Looper.getMainLooper()).post(() -> {
                    if (updatedRows > 0) {
                        viewModel.insert(binContact);
                        Toast.makeText(activity, getString(R.string.contact_moved_to_bin), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(activity, getString(R.string.failed_to_move_contact), Toast.LENGTH_SHORT).show();
                    }
                    finish();
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void shareContact(String name, List<PhoneItem> phoneList) {
        File file = Utils.saveVCardToFile(activity, name, phoneList);
        if (file != null) {
            Uri fileUri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/x-vcard");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            activity.startActivity(Intent.createChooser(shareIntent, "Share ContactData"));
        }
    }

    private void addToFavourites() {
        isFavourite = !isFavourite;
        Utils.toggleContactStarred(activity, contactId, isFavourite);
        if (isFavourite) {
            binding.tvFavourite.setText(getString(R.string.remove_from_favourite_list));
        } else {
            binding.tvFavourite.setText(getString(R.string.add_to_favourite_list));
        }
    }

    private void requestPermissions() {
        openRingtonePicker(getContactRingtone());
    }

    private void openRingtonePicker(Uri currentRingtoneUri) {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Ringtone");
        if (currentRingtoneUri != null) {
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentRingtoneUri);
        }
        ringtoneLauncher.launch(intent);
    }

    private Uri getContactRingtone() {
        if (contactId == null) return null;

        Uri contactUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId);
        Cursor cursor = getContentResolver().query(contactUri,
                new String[]{ContactsContract.Contacts.CUSTOM_RINGTONE},
                null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                String ringtoneUriString = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.CUSTOM_RINGTONE));
                cursor.close();
                if (ringtoneUriString != null) {
                    return Uri.parse(ringtoneUriString);
                }
            }
            cursor.close();
        }
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
    }

    private void setContactRingtone(String contactId, Uri ringtoneUri) {
        ContentValues values = new ContentValues();
        values.put(ContactsContract.Contacts.CUSTOM_RINGTONE, ringtoneUri.toString());
        int updated = getContentResolver().update(
                ContactsContract.Contacts.CONTENT_URI,
                values,
                ContactsContract.Contacts._ID + " = ?",
                new String[]{contactId}
        );

        if (updated > 0) {
            Toast.makeText(activity, getString(R.string.ringtone_set), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(activity, getString(R.string.failed_to_set_ringtone), Toast.LENGTH_SHORT).show();
        }
    }

    private void blockUnblockContact() {
        if (Utils.isDefaultDialer(activity)) {
            if (isBlocked) {
                Utils.unblockAllNumbers(activity, phoneList);
                binding.tvBlock.setText(getString(R.string.block_contact));
                Toast.makeText(activity, getString(R.string.unblocked), Toast.LENGTH_SHORT).show();
            } else {
                Utils.blockAllNumbers(activity, phoneList);
                binding.tvBlock.setText(getString(R.string.unblock_contact));
                Toast.makeText(activity, getString(R.string.blocked), Toast.LENGTH_SHORT).show();
            }
            isBlocked = !isBlocked;
        } else {
            requestDefaultDialer();
        }
    }

    private void makePhoneCall(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return;
        }
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(activity, Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CALL_PHONE, Manifest.permission.ANSWER_PHONE_CALLS}, REQUEST_CALL_PERMISSION);
            return;
        }
        if (Utils.isDefaultDialer(activity)) {
            Utils.callContact(activity, phoneNumber);
        } else {
            requestDefaultDialer();
        }
    }

    private void checkContactsPermission() {
        hasContactsPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED;

        if (!hasContactsPermission) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS},
                    REQUEST_CONTACTS_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CONTACTS_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                hasContactsPermission = true;
                init();
            } else {
                hasContactsPermission = false;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NativeAD.getInstance().destroy();
    }
}