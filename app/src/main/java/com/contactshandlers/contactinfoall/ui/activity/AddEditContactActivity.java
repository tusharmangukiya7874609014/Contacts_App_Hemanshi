package com.contactshandlers.contactinfoall.ui.activity;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.adapter.PhoneAdapter;
import com.contactshandlers.contactinfoall.adapter.SpinnerAccountAdapter;
import com.contactshandlers.contactinfoall.ads.InterstitialAD;
import com.contactshandlers.contactinfoall.databinding.ActivityAddEditContactBinding;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.helper.SharedPreferencesManager;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.listeners.AdCallback;
import com.contactshandlers.contactinfoall.listeners.RemoveListener;
import com.contactshandlers.contactinfoall.model.PhoneItem;
import com.contactshandlers.contactinfoall.model.RecentAddedContact;
import com.contactshandlers.contactinfoall.room.ContactDao;
import com.contactshandlers.contactinfoall.room.ContactDatabase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddEditContactActivity extends BaseActivity implements View.OnClickListener {

    private ActivityAddEditContactBinding binding;
    private final AddEditContactActivity activity = AddEditContactActivity.this;
    private SpinnerAccountAdapter adapter;
    private int selectedPosition;
    private String accountName;
    private String title;
    private Uri imageUri;
    private List<PhoneItem> phoneList = new ArrayList<>();
    private PhoneAdapter phoneAdapter;
    private String contactId;
    private ContactDao contactDao;
    private ExecutorService executorService;
    private static final int REQUEST_CONTACTS_PERMISSION = 100;
    private boolean hasContactsPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddEditContactBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Utils.setStatusBarColor(activity);
        init();
        initListener();
    }

    private void init() {
        String contactNumber = getIntent().getStringExtra(Constants.CONTACT_NUMBER);
        String contactName = getIntent().getStringExtra(Constants.CONTACT_NAME);
        title = getIntent().getStringExtra(Constants.TITLE);
        binding.included.tvHeading.setText(title);

        if (title == null) {
            title = getString(R.string.add_contact);
        }

        contactDao = ContactDatabase.getInstance(getApplication()).contactDao();
        executorService = Executors.newSingleThreadExecutor();

        checkContactsPermission();

        if (getString(R.string.edit_contact).equals(title)) {
            binding.clSpinner.setVisibility(View.GONE);
            if (hasContactsPermission && contactName != null) {
                try {
                    contactId = Utils.getContactIdByName(activity, contactName);
                    if (contactId != null) {
                        binding.tvSaveFrom.setText(getContactStorageType(contactId));
                    } else {
                        binding.tvSaveFrom.setText("Unknown");
                    }
                } catch (SecurityException e) {
                    binding.tvSaveFrom.setText("Unknown");
                }
            } else {
                binding.tvSaveFrom.setText("Unknown");
            }
        } else {
            binding.clSpinner.setVisibility(View.VISIBLE);
            binding.tvSaveFrom.setVisibility(View.GONE);
        }

        binding.rvPhoneNumbers.setLayoutManager(new LinearLayoutManager(activity));
        phoneAdapter = new PhoneAdapter(activity, phoneList, new RemoveListener() {
            @Override
            public void onClick(int position) {
                if (position >= 0 && position < phoneList.size()) {
                    phoneList.remove(position);
                    phoneAdapter.notifyItemRemoved(position);
                    phoneAdapter.notifyItemRangeChanged(position, phoneList.size());
                }
            }
        });
        binding.rvPhoneNumbers.setAdapter(phoneAdapter);

        selectedPosition = SharedPreferencesManager.getInstance().getIntValue(Constants.SELECTED_LANGUAGE_POSITION, 0);
        setSpinner();

        if (contactName != null && contactId != null && hasContactsPermission) {
            try {
                List<PhoneItem> phones = Utils.getPhoneNumbersById(activity, contactId);
                for (PhoneItem phoneItem : phones) {
                    String[] nameParts = contactName.split(" ", 2);
                    binding.etFirstName.setText(nameParts[0]);
                    binding.etLastName.setText(nameParts.length > 1 ? nameParts[1] : "");
                    addPhoneField(phoneItem.getPhoneNumber(), phoneItem.getPhoneType());
                }
                phoneAdapter.setPhoneList(phoneList);
                binding.scrollView.post(() -> binding.scrollView.smoothScrollTo(0, binding.scrollView.getChildAt(0).getBottom()));

                Bitmap photo = Utils.getContactPhoto(activity, contactId);
                if (photo != null) {
                    binding.ivProfile.setImageBitmap(photo);
                } else {
                    binding.ivProfile.setImageBitmap(Utils.getInitialsBitmap(getColor(R.color.grey_font), Utils.getContactNameById(activity, contactId), getColor(R.color.bg_color)));
                }
            } catch (SecurityException e) {
                String[] nameParts = contactName.split(" ", 2);
                binding.etFirstName.setText(nameParts[0]);
                binding.etLastName.setText(nameParts.length > 1 ? nameParts[1] : "");
            }
        } else {
            addPhoneField(contactNumber, "Phone");
        }

        phoneAdapter.setPhoneList(phoneList);
        binding.scrollView.post(() -> binding.scrollView.smoothScrollTo(0, binding.scrollView.getChildAt(0).getBottom()));

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

    private void checkContactsPermission() {
        hasContactsPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED;

        if (!hasContactsPermission) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS},
                    REQUEST_CONTACTS_PERMISSION);
        }
    }

    private void setSpinner() {
        List<String> storageOptions = getStorageOptions(activity);
        adapter = new SpinnerAccountAdapter(activity, storageOptions, selectedPosition);
        binding.spAccounts.setAdapter(adapter);
        binding.spAccounts.setSelection(selectedPosition);
        binding.spAccounts.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String account = storageOptions.get(position);
                binding.spAccounts.setSelection(position);
                accountName = account;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void initListener() {
        binding.included.ivBack.setOnClickListener(this);
        binding.ivProfile.setOnClickListener(this);
        binding.btnAddPhoto.setOnClickListener(this);
        binding.btnAddPhoneNumber.setOnClickListener(this);
        binding.btnSave.setOnClickListener(this);
        binding.btnCancel.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.ivBack || id == R.id.btnCancel) {
            getOnBackPressedDispatcher().onBackPressed();
        } else if (id == R.id.ivProfile || id == R.id.btnAddPhoto) {
            pickImage();
        } else if (id == R.id.btnAddPhoneNumber) {
            addPhoneField(null, null);
            binding.scrollView.post(() -> binding.scrollView.smoothScrollTo(0, binding.scrollView.getChildAt(0).getBottom()));
        } else if (id == R.id.btnSave) {
            if (contactId != null) {
                updateContact(contactId, binding.etFirstName.getText().toString().trim(), binding.etLastName.getText().toString().trim(), phoneList, imageUri);
            } else {
                saveContact(accountName);
            }
        }
    }

    public void insertOrUpdateRecentAdded(RecentAddedContact contact) {
        executorService.execute(() -> {
            RecentAddedContact existingContact = contactDao.getRecentAddedById(contact.getContactId());
            if (existingContact == null) {
                contactDao.insertRecentAdded(contact);
            } else {
                contactDao.updateRecentAdded(contact.getContactId(), contact.getName(), contact.getAddedTimestamp());
            }
        });
    }

    private List<String> getStorageOptions(Context context) {
        List<String> options = new ArrayList<>();

        options.add("Phone Storage");

        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType("com.google");
        for (Account account : accounts) {
            options.add(account.name);
        }

        SubscriptionManager subscriptionManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        if (subscriptionManager != null) {
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                List<SubscriptionInfo> subscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
                if (subscriptionInfoList != null) {
                    if (subscriptionInfoList.size() == 1) {
                        options.add("SIM Card");
                    } else {
                        for (SubscriptionInfo info : subscriptionInfoList) {
                            String simDisplayName = info.getDisplayName().toString();
                            options.add("SIM Card: " + simDisplayName);
                        }
                    }
                }
            } else {
                requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, 1);
            }
        }
        return options;
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, 2);
        } else {
            Toast.makeText(activity, getString(R.string.no_app_available_to_pick_images), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2 && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            binding.ivProfile.setImageURI(imageUri);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CONTACTS_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                hasContactsPermission = true;
                String contactName = getIntent().getStringExtra(Constants.CONTACT_NAME);
                if (contactName != null && getString(R.string.edit_contact).equals(title)) {
                    init();
                }
            } else {
                hasContactsPermission = false;
                Toast.makeText(activity, getString(R.string.permission_denied), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setSpinner();
        }
    }

    private void addPhoneField(@Nullable String prefilledNumber, @Nullable String prefilledType) {
        if (phoneAdapter == null) {
            return;
        }
        phoneList.add(new PhoneItem(prefilledNumber != null ? prefilledNumber : "", prefilledType != null ? prefilledType : "Phone"));
        phoneAdapter.notifyItemInserted(phoneList.size() - 1);
    }

    private void saveContact(String storageOption) {
        if (!hasContactsPermission) {
            checkContactsPermission();
            return;
        }

        String firstName = binding.etFirstName.getText().toString().trim();
        String lastName = binding.etLastName.getText().toString().trim();
        String fullName = firstName + (lastName.isEmpty() ? "" : " " + lastName);

        if (!firstName.isEmpty() && !phoneList.isEmpty()) {
            try {
                Cursor cursor = getContentResolver().query(
                        ContactsContract.Contacts.CONTENT_URI,
                        new String[]{ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME},
                        null,
                        null,
                        null
                );

                if (cursor != null) {
                    try {
                        while (cursor.moveToNext()) {
                            String existingName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
                            if (existingName != null && existingName.equalsIgnoreCase(fullName)) {
                                Toast.makeText(activity, getString(R.string.contact_with_this_name_already_exists), Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                    } finally {
                        cursor.close();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            boolean contactExists = false;
            for (PhoneItem phoneItem : phoneList) {
                String phone = phoneItem.getPhoneNumber();
                if (phone != null && !phone.trim().isEmpty()) {
                    if (isExactPhoneNumberExists(phone)) {
                        contactExists = true;
                        break;
                    }
                }
            }

            if (contactExists) {
                Toast.makeText(activity, getString(R.string.contact_with_this_number_already_exists), Toast.LENGTH_SHORT).show();
                return;
            }

            ArrayList<ContentProviderOperation> ops = new ArrayList<>();

            String accountType = null;
            String accountName = null;

            if (storageOption != null) {
                if (storageOption.equals("Phone Storage")) {
                    accountType = null;
                    accountName = null;
                } else if (storageOption.contains("@")) {
                    accountType = "com.google";
                    accountName = storageOption;
                } else if (storageOption.contains("SIM Card")) {
                    SubscriptionManager subscriptionManager = (SubscriptionManager) getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                    if (subscriptionManager != null) {
                        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                            List<SubscriptionInfo> subscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
                            if (subscriptionInfoList != null) {
                                for (SubscriptionInfo info : subscriptionInfoList) {
                                    String simName = "SIM Card: " + info.getDisplayName().toString();
                                    if (storageOption.equals(simName)) {
                                        accountType = "vnd.sec.contact.sim";
                                        accountName = info.getIccId();
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, accountType)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, accountName)
                    .build());

            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, firstName)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, lastName)
                    .build());

            int invalid = 0;
            for (PhoneItem phoneItem : phoneList) {
                String phone = phoneItem.getPhoneNumber();
                String typeLabel = phoneItem.getPhoneType();
                if (phone != null && !phone.trim().isEmpty()) {
                    int phoneType = getPhoneType(typeLabel);
                    ContentProviderOperation.Builder phoneBuilder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phoneType);
                    if (phoneType == ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM) {
                        phoneBuilder.withValue(ContactsContract.CommonDataKinds.Phone.LABEL, typeLabel);
                    }
                    ops.add(phoneBuilder.build());
                } else {
                    invalid++;
                }
            }

            if (phoneList.size() != invalid) {
                if (imageUri != null) {
                    try {
                        byte[] photoBytes = getImageBytes(imageUri);
                        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
                                .build());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    ContentProviderResult[] results = getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
                    if (results.length > 0) {
                        assert results[0].uri != null;
                        long rawContactId = ContentUris.parseId(results[0].uri);
                        String contactId = getContactIdFromRawContactId(rawContactId);
                        Toast.makeText(activity, getString(R.string.contact_saved), Toast.LENGTH_SHORT).show();
                        String contactName = firstName + " " + lastName;
                        insertOrUpdateRecentAdded(new RecentAddedContact(contactId, contactName, System.currentTimeMillis()));
                        finish();
                    }
                } catch (SecurityException e) {
                    Toast.makeText(activity, getString(R.string.failed_to_save_contact), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(activity, getString(R.string.please_enter_name_and_phone_number), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(activity, getString(R.string.please_enter_name_and_phone_number), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isExactPhoneNumberExists(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }

        if (!hasContactsPermission) {
            return false;
        }

        String normalizedNewNumber = normalizePhoneNumber(phoneNumber);

        try {
            Cursor cursor = getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                    },
                    null,
                    null,
                    null
            );

            if (cursor != null) {
                try {
                    while (cursor.moveToNext()) {
                        String existingNumber = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        String normalizedExistingNumber = normalizePhoneNumber(existingNumber);

                        if (normalizedNewNumber.equals(normalizedExistingNumber)) {
                            return true;
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return "";
        return phoneNumber.replaceAll("[^\\d]", "");
    }

    private String getContactIdFromRawContactId(long rawContactId) {
        String contactId = null;
        Cursor cursor = getContentResolver().query(
                ContactsContract.RawContacts.CONTENT_URI,
                new String[]{ContactsContract.RawContacts.CONTACT_ID},
                ContactsContract.RawContacts._ID + " = ?",
                new String[]{String.valueOf(rawContactId)},
                null
        );
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.CONTACT_ID));
            }
            cursor.close();
        }
        return contactId;
    }


    private byte[] getImageBytes(Uri imageUri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(imageUri);
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }

        return byteBuffer.toByteArray();
    }

    private void updateContact(String contactId, String firstName, String lastName, List<PhoneItem> phoneList, Uri imageUri) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        if (!firstName.isEmpty() && !phoneList.isEmpty()) {
            ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                    .withSelection(ContactsContract.Data.CONTACT_ID + "=? AND " +
                                    ContactsContract.Data.MIMETYPE + "=?",
                            new String[]{contactId, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE})
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, firstName)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, lastName)
                    .build());

            long rawContactId = getRawContactId(contactId);

            List<String> existingPhones = getExistingPhoneNumbers(contactId);

            int invalid = 0;
            for (PhoneItem phoneItem : phoneList) {
                String phone = phoneItem.getPhoneNumber();
                String typeLabel = phoneItem.getPhoneType();

                if (phone != null && !phone.trim().isEmpty()) {
                    int phoneType = getPhoneType(typeLabel);
                    boolean phoneExists = existingPhones.contains(phone);

                    ContentProviderOperation.Builder builder;
                    if (phoneExists) {
                        builder = ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                                .withSelection(ContactsContract.Data.CONTACT_ID + "=? AND " +
                                                ContactsContract.Data.MIMETYPE + "=? AND " +
                                                ContactsContract.CommonDataKinds.Phone.NUMBER + "=?",
                                        new String[]{contactId, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE, phone});
                    } else {
                        builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
                    }

                    builder.withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phoneType);

                    if (phoneType == ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM) {
                        builder.withValue(ContactsContract.CommonDataKinds.Phone.LABEL, typeLabel);
                    }

                    ops.add(builder.build());
                }else {
                    invalid++;
                }
                existingPhones.remove(phone);
            }

            if (phoneList.size() != invalid) {
                for (String phoneToDelete : existingPhones) {
                    ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                            .withSelection(ContactsContract.Data.CONTACT_ID + "=? AND " +
                                            ContactsContract.Data.MIMETYPE + "=? AND " +
                                            ContactsContract.CommonDataKinds.Phone.NUMBER + "=?",
                                    new String[]{contactId, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE, phoneToDelete})
                            .build());
                }


                if (imageUri != null) {
                    try {
                        byte[] photoBytes = getImageBytes(imageUri);
                        boolean hasPhoto = checkPhotoExists(contactId);

                        if (hasPhoto) {
                            ops.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                                    .withSelection(ContactsContract.Data.CONTACT_ID + "=? AND " +
                                                    ContactsContract.Data.MIMETYPE + "=?",
                                            new String[]{contactId, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE})
                                    .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
                                    .build());
                        } else {
                            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                    .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                                    .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
                                    .build());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
                    Toast.makeText(activity, getString(R.string.contact_updated), Toast.LENGTH_SHORT).show();
                    String contactName = binding.etFirstName.getText().toString().trim() + " " + binding.etLastName.getText().toString().trim();
                    insertOrUpdateRecentAdded(new RecentAddedContact(contactId, contactName, System.currentTimeMillis()));
                    finish();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(activity, getString(R.string.failed_to_update_contact), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(activity, getString(R.string.please_enter_name_and_phone_number), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(activity, getString(R.string.please_enter_name_and_phone_number), Toast.LENGTH_SHORT).show();
        }
    }

    private List<String> getExistingPhoneNumbers(String contactId) {
        List<String> phoneNumbers = new ArrayList<>();

        String selection = ContactsContract.Data.CONTACT_ID + "=? AND " +
                ContactsContract.Data.MIMETYPE + "=?";
        List<String> selectionArgsList = new ArrayList<>();
        selectionArgsList.add(contactId);
        selectionArgsList.add(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);

        String[] selectionArgs = selectionArgsList.toArray(new String[0]);

        Cursor cursor = getContentResolver().query(
                ContactsContract.Data.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                selection,
                selectionArgs,
                null
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                phoneNumbers.add(cursor.getString(0));
            }
            cursor.close();
        }

        return phoneNumbers;
    }

    private long getRawContactId(String contactId) {
        long rawContactId = -1;

        String selection = ContactsContract.RawContacts.CONTACT_ID + "=?";
        List<String> selectionArgsList = new ArrayList<>();
        selectionArgsList.add(contactId);


        String[] selectionArgs = selectionArgsList.toArray(new String[0]);

        Cursor cursor = getContentResolver().query(
                ContactsContract.RawContacts.CONTENT_URI,
                new String[]{ContactsContract.RawContacts._ID},
                selection,
                selectionArgs,
                null
        );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                rawContactId = cursor.getLong(0);
            }
            cursor.close();
        }
        return rawContactId;
    }

    private boolean checkPhotoExists(String contactId) {
        Cursor cursor = getContentResolver().query(
                ContactsContract.Data.CONTENT_URI,
                new String[]{ContactsContract.Data._ID},
                ContactsContract.Data.CONTACT_ID + "=? AND " +
                        ContactsContract.Data.MIMETYPE + "=?",
                new String[]{contactId, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE},
                null
        );
        boolean exists = cursor != null && cursor.moveToFirst();
        if (cursor != null) cursor.close();
        return exists;
    }

    private int getPhoneType(String type) {
        int phoneType;
        switch (type) {
            case "Phone":
                phoneType = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE;
                break;
            case "Home":
                phoneType = ContactsContract.CommonDataKinds.Phone.TYPE_HOME;
                break;
            case "Work":
                phoneType = ContactsContract.CommonDataKinds.Phone.TYPE_WORK;
                break;
            case "Work Fax":
                phoneType = ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK;
                break;
            case "Home Fax":
                phoneType = ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME;
                break;
            case "Pager":
                phoneType = ContactsContract.CommonDataKinds.Phone.TYPE_PAGER;
                break;
            case "Other":
                phoneType = ContactsContract.CommonDataKinds.Phone.TYPE_OTHER;
                break;
            case "Callback":
                phoneType = ContactsContract.CommonDataKinds.Phone.TYPE_CALLBACK;
                break;
            default:
                phoneType = ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM;
                break;
        }
        return phoneType;
    }

    private String getContactStorageType(String contactId) {
        String storageType = "Unknown";

        Cursor cursor = getContentResolver().query(
                ContactsContract.RawContacts.CONTENT_URI,
                new String[]{
                        ContactsContract.RawContacts.ACCOUNT_TYPE,
                        ContactsContract.RawContacts.ACCOUNT_NAME
                },
                ContactsContract.RawContacts.CONTACT_ID + "=?",
                new String[]{contactId},
                null
        );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                String accountType = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_TYPE));
                String accountName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_NAME));

                if (accountType == null || accountType.isEmpty()) {
                    storageType = "Device Storage";
                } else if (accountType.contains("com.google")) {
                    storageType = accountName;
                } else if (accountType.toLowerCase().contains("sim")) {
                    storageType = "SIM Card";
                } else {
                    storageType = "Other (" + accountType + ")";
                }
            }
            cursor.close();
        }

        return storageType;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults, int deviceId) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setSpinner();
        }
    }

}