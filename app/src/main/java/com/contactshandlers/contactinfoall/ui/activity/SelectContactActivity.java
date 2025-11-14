package com.contactshandlers.contactinfoall.ui.activity;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.adapter.SelectContactsAdapter;
import com.contactshandlers.contactinfoall.ads.InterstitialAD;
import com.contactshandlers.contactinfoall.databinding.ActivitySelectContactBinding;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.listeners.AdCallback;
import com.contactshandlers.contactinfoall.model.ContactData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SelectContactActivity extends BaseActivity implements View.OnClickListener {

    private ActivitySelectContactBinding binding;
    private final SelectContactActivity activity = SelectContactActivity.this;
    private SelectContactsAdapter adapter;
    private List<ContactData> contactsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivitySelectContactBinding.inflate(getLayoutInflater());
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
        binding.included.tvHeading.setText(getString(R.string.select_contact));

        binding.rvData.setLayoutManager(new LinearLayoutManager(activity));
        adapter = new SelectContactsAdapter(activity, contactsList);
        binding.rvData.setAdapter(adapter);

        if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_CONTACTS,Manifest.permission.WRITE_CONTACTS}, 100);
        } else {
            getAllContacts();
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

    private void initListener() {
        binding.included.ivBack.setOnClickListener(this);
        binding.btnSave.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.ivBack) {
            getOnBackPressedDispatcher().onBackPressed();
        } else if (id == R.id.btnSave) {
            InterstitialAD.getInstance().showInterstitial(activity, new AdCallback() {
                @Override
                public void callbackCall() {
                    saveContacts();
                }
            });
        }
    }

    private void getAllContacts() {
        contactsList = new ArrayList<>();
        Set<String> addedContactIds = new HashSet<>();

        ContentResolver contentResolver = getContentResolver();

        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
                ContactsContract.CommonDataKinds.Phone.STARRED
        };

        Cursor cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        );

        if (cursor != null) {
            int idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
            int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            int photoIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI);
            int starIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED);

            while (cursor.moveToNext()) {
                String id = cursor.getString(idIndex);
                if (addedContactIds.contains(id)) continue;

                String fullName = cursor.getString(nameIndex);
                String phone = cursor.getString(numberIndex);
                String photo = cursor.getString(photoIndex);
                boolean isFavourite = cursor.getInt(starIndex) == 1;

                if (!TextUtils.isEmpty(fullName) && !TextUtils.isEmpty(phone)) {
                    String[] nameParts = fullName.split(" ", 2);
                    String firstName = nameParts.length > 0 ? nameParts[0] : "";
                    String lastName = nameParts.length > 1 ? nameParts[1] : "";

                    contactsList.add(new ContactData(id, firstName, lastName, phone, photo, isFavourite, false));
                    addedContactIds.add(id);
                }
            }
            cursor.close();
        }

        adapter.setContacts(contactsList);
        binding.tvNotFound.setVisibility(contactsList.isEmpty() ? View.VISIBLE : View.GONE);

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        filter(binding.etSearch.getText().toString());
    }


    private void filter(String query) {
        List<ContactData> contacts = new ArrayList<>();
        if (query.isEmpty()) {
            contacts.addAll(contactsList);
        } else {
            for (ContactData contact : contactsList) {
                if (contact.getNameFL().toLowerCase().contains(query.toLowerCase())) {
                    contacts.add(contact);
                }
            }
        }
        adapter.setContacts(contacts);
        binding.tvNotFound.setVisibility(contacts.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void saveContacts() {
        if (contactsList.isEmpty()) {
            finish();
        } else {
            for (ContactData contact : contactsList) {
                Utils.toggleContactStarred(activity, contact.getId(), contact.isFavourite());
            }
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            getAllContacts();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults, int deviceId) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getAllContacts();
        }
    }

}