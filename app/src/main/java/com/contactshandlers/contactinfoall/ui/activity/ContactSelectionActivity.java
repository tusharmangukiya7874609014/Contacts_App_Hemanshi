package com.contactshandlers.contactinfoall.ui.activity;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.databinding.ActivityContactSelectionBinding;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.model.ContactGroupItem;
import com.contactshandlers.contactinfoall.adapter.SelectableContactsAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ContactSelectionActivity extends BaseActivity implements View.OnClickListener {

    private static final int CONTACTS_PERMISSION_REQUEST = 100;
    private ActivityContactSelectionBinding binding;
    private ContactSelectionActivity activity = ContactSelectionActivity.this;
    private SelectableContactsAdapter contactsAdapter;
    private List<ContactGroupItem> allContacts;
    private List<ContactGroupItem> filteredContacts;
    private List<ContactGroupItem> existingContacts = new ArrayList<>();
    private Set<String> existingContactIds;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityContactSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initListener();
        init();
    }

    private void initListener() {
        binding.included.ivBack.setOnClickListener(this);
        binding.btnCancel.setOnClickListener(this);
        binding.btnSave.setOnClickListener(this);
    }

    private void init() {
        executorService = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());

        getIntentData();
        initializeViews();
        setupRecyclerView();
        setupSearchFunctionality();

        checkPermissionsAndLoadContacts();
    }

    private void getIntentData() {
        existingContacts = getIntent().getParcelableArrayListExtra(Constants.EXISTING_CONTACTS);

        if (existingContacts == null) {
            existingContacts = new ArrayList<>();
        }

        existingContactIds = new HashSet<>();
        for (ContactGroupItem contact : existingContacts) {
            existingContactIds.add(contact.getId());
        }
    }

    private void initializeViews() {
        binding.included.tvHeading.setText(getString(R.string.select_contact));
        allContacts = new ArrayList<>();
        filteredContacts = new ArrayList<>();
    }

    private void setupRecyclerView() {
        contactsAdapter = new SelectableContactsAdapter(activity, filteredContacts, existingContactIds);

        binding.rvContacts.setLayoutManager(new LinearLayoutManager(activity));
        binding.rvContacts.setAdapter(contactsAdapter);
    }

    private void setupSearchFunctionality() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterContacts(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void checkPermissionsAndLoadContacts() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    CONTACTS_PERMISSION_REQUEST);
        } else {
            loadAllContactsDirect();
        }
    }

    private void loadAllContactsDirect() {
        mainHandler.post(this::showProgress);

        CompletableFuture.supplyAsync(() -> {
            List<ContactGroupItem> contactsList = new ArrayList<>();
            Set<String> addedContactIds = new HashSet<>();

            try {
                ContentResolver contentResolver = getContentResolver();

                String[] projection = {
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.PHOTO_URI
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

                    while (cursor.moveToNext()) {
                        String id = cursor.getString(idIndex);
                        if (addedContactIds.contains(id)) continue;

                        String displayName = cursor.getString(nameIndex);
                        String phoneNumber = cursor.getString(numberIndex);
                        String photoUri = cursor.getString(photoIndex);

                        if (!TextUtils.isEmpty(displayName) && !TextUtils.isEmpty(phoneNumber)) {
                            ContactGroupItem contact = new ContactGroupItem();
                            contact.setId(id);
                            contact.setDisplayName(displayName);
                            contact.setPhoneNumber(phoneNumber);
                            contact.setPhoto(photoUri);

                            contactsList.add(contact);
                            addedContactIds.add(id);
                        }
                    }
                    cursor.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return contactsList;

        }, executorService).thenAcceptAsync(loadedContacts -> {
            hideProgress();

            allContacts.clear();
            allContacts.addAll(loadedContacts);

            filteredContacts.clear();
            filteredContacts.addAll(loadedContacts);

            contactsAdapter.notifyDataSetChanged();
            updateUIState();

            String searchQuery = binding.etSearch.getText().toString().trim();
            if (!searchQuery.isEmpty()) {
                filterContacts(searchQuery);
            }

        }, mainHandler::post);
    }

    private void filterContacts(String query) {
        filteredContacts.clear();

        if (query.isEmpty()) {
            filteredContacts.addAll(allContacts);
        } else {
            String queryLower = query.toLowerCase();
            for (ContactGroupItem contact : allContacts) {
                if (contact.getDisplayName() != null &&
                        contact.getDisplayName().toLowerCase().contains(queryLower)) {
                    filteredContacts.add(contact);
                }
            }
        }

        contactsAdapter.notifyDataSetChanged();
        updateUIState();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.ivBack || id == R.id.btnCancel) {
            setResult(RESULT_CANCELED);
            finish();
        } else if (id == R.id.btnSave) {
            handleSaveChanges();
        }
    }

    private void handleSaveChanges() {
        List<ContactGroupItem> selectedContacts = contactsAdapter.getSelectedContacts();
        List<ContactGroupItem> removedContacts = contactsAdapter.getRemovedContacts();

        if (selectedContacts.isEmpty() && removedContacts.isEmpty()) {
            return;
        }

        Intent resultIntent = new Intent();
        resultIntent.putParcelableArrayListExtra(Constants.SELECTED_CONTACTS, new ArrayList<>(selectedContacts));
        resultIntent.putParcelableArrayListExtra(Constants.REMOVED_CONTACTS, new ArrayList<>(removedContacts));
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void showProgress() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.rvContacts.setVisibility(View.GONE);
        binding.tvNotFound.setVisibility(View.GONE);
    }

    private void hideProgress() {
        binding.progressBar.setVisibility(View.GONE);
    }

    private void updateUIState() {
        if (filteredContacts.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
        }
    }

    private void showEmptyState() {
        binding.tvNotFound.setVisibility(View.VISIBLE);
        binding.rvContacts.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        binding.tvNotFound.setVisibility(View.GONE);
        binding.rvContacts.setVisibility(View.VISIBLE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CONTACTS_PERMISSION_REQUEST && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadAllContactsDirect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            // Refresh contacts if needed
            if (allContacts.isEmpty()) {
                loadAllContactsDirect();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}