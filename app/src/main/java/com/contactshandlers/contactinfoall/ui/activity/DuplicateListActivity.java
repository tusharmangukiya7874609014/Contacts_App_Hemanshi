package com.contactshandlers.contactinfoall.ui.activity;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.ads.InterstitialAD;
import com.contactshandlers.contactinfoall.databinding.ActivityDuplicateListBinding;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.listeners.AdCallback;
import com.contactshandlers.contactinfoall.model.Contact;
import com.contactshandlers.contactinfoall.helper.ContactManager;
import com.contactshandlers.contactinfoall.adapter.DuplicateContactsAdapter;
import com.contactshandlers.contactinfoall.model.DuplicateGroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DuplicateListActivity extends BaseActivity implements View.OnClickListener {

    private ActivityDuplicateListBinding binding;
    private final DuplicateListActivity activity = DuplicateListActivity.this;
    private DuplicateContactsAdapter adapter;
    private ContactManager contactManager;
    private DuplicateGroup accountGroup;
    private DuplicateGroup duplicateGroup;
    private String mergeType, title;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityDuplicateListBinding.inflate(getLayoutInflater());
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
        binding.btnMergeAllButton.setOnClickListener(this);
    }

    private void init() {
        contactManager = new ContactManager(this);

        accountGroup = (DuplicateGroup) getIntent().getSerializableExtra(Constants.ACCOUNT_GROUP);
        mergeType = getIntent().getStringExtra(Constants.MERGE_TYPE);
        title = getIntent().getStringExtra(Constants.TITLE);
        if (title != null && !title.isEmpty()) {
            binding.included.tvHeading.setText(title);
        }

        setupRecyclerView();
        loadDuplicates();

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
            handleBackPress();
        } else if (id == R.id.btnMergeAllButton) {
            mergeAllContacts(duplicateGroup);
        }
    }

    private void handleBackPress() {
        getOnBackPressedDispatcher().onBackPressed();
    }

    private void setupRecyclerView() {
        binding.rvDuplicateContactsList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DuplicateContactsAdapter(activity, new DuplicateContactsAdapter.OnDuplicateGroupClickListener() {
            @Override
            public void onDuplicateGroupClick(DuplicateGroup duplicateGroup) {
                activity.duplicateGroup = duplicateGroup;
            }
        });
        binding.rvDuplicateContactsList.setAdapter(adapter);
    }

    private void loadDuplicates() {
        showLoading(true);

        executor.execute(() -> {
            List<Contact> refreshedContacts = getRefreshedAccountContacts();

            List<DuplicateGroup> result = contactManager.findDuplicatesInAccount(
                    refreshedContacts,
                    mergeType
            );

            handler.post(() -> {
                showLoading(false);
                if (result.isEmpty()) {
                    showEmptyState();
                } else {
                    showNormalState();
                    adapter.setDuplicateGroups(result);
                    if (!result.isEmpty()) {
                        duplicateGroup = result.get(0);
                    }
                }
            });
        });
    }

    private List<Contact> getRefreshedAccountContacts() {
        contactManager.clearCache();

        List<DuplicateGroup> allAccountGroups = contactManager.getAccountsWithDuplicates(mergeType);

        for (DuplicateGroup group : allAccountGroups) {
            if (group.getGroupKey().equals(accountGroup.getGroupKey())) {
                accountGroup = group;
                return group.getContacts();
            }
        }

        return new ArrayList<>();
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.rvDuplicateContactsList.setVisibility(show ? View.GONE : View.VISIBLE);
        binding.btnMergeAllButton.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showEmptyState() {
        binding.rvDuplicateContactsList.setVisibility(View.GONE);
        binding.btnMergeAllButton.setVisibility(View.GONE);
        handleBackPress();
    }

    private void showNormalState() {
        binding.rvDuplicateContactsList.setVisibility(View.VISIBLE);
        binding.btnMergeAllButton.setVisibility(View.VISIBLE);
    }

    private void mergeAllContacts(DuplicateGroup duplicateGroup) {
        if (duplicateGroup == null) {
            Toast.makeText(activity, getString(R.string.select_a_group_to_merge_duplicate), Toast.LENGTH_SHORT).show();
            return;
        }

        List<Contact> contacts = duplicateGroup.getContacts();
        if (contacts.size() < 2) {
            return;
        }

        binding.btnMergeAllButton.setEnabled(false);
        showLoading(true);

        executor.execute(() -> {
            try {
                Contact masterContact = contacts.get(0);
                List<Contact> contactsToMerge = contacts.subList(1, contacts.size());

                ArrayList<ContentProviderOperation> operations = new ArrayList<>();

                Set<String> allPhoneNumbers = new HashSet<>();
                Set<String> allEmails = new HashSet<>();
                String masterName = masterContact.getName();

                for (Contact contact : contacts) {
                    if (contact.getPhoneNumbers() != null) {
                        allPhoneNumbers.addAll(contact.getPhoneNumbers());
                    }
                    if (contact.getEmails() != null) {
                        allEmails.addAll(contact.getEmails());
                    }

                    if (contact.getName() != null &&
                            (masterName == null || contact.getName().length() > masterName.length())) {
                        masterName = contact.getName();
                    }
                }

                clearContactData(operations, masterContact.getId());
                addDataToMasterContact(operations, masterContact.getId(), masterName, allPhoneNumbers, allEmails);
                deleteDuplicateContacts(operations, contactsToMerge);

                ContentResolver resolver = getContentResolver();
                resolver.applyBatch(ContactsContract.AUTHORITY, operations);

                handler.post(() -> {
                    binding.btnMergeAllButton.setEnabled(true);
                    showLoading(false);
                    Toast.makeText(activity, getString(R.string.contacts_merged_successfully), Toast.LENGTH_SHORT).show();
                    loadDuplicates();
                });

            } catch (RemoteException | OperationApplicationException e) {
                handler.post(() -> {
                    binding.btnMergeAllButton.setEnabled(true);
                    showLoading(false);
                    Toast.makeText(activity, getString(R.string.merge_failed), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void clearContactData(ArrayList<ContentProviderOperation> operations, String contactId) {
        operations.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                .withSelection(ContactsContract.Data.CONTACT_ID + "=? AND " +
                                ContactsContract.Data.MIMETYPE + "=?",
                        new String[]{contactId, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE})
                .build());

        operations.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                .withSelection(ContactsContract.Data.CONTACT_ID + "=? AND " +
                                ContactsContract.Data.MIMETYPE + "=?",
                        new String[]{contactId, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE})
                .build());
    }

    private void addDataToMasterContact(ArrayList<ContentProviderOperation> operations, String contactId,
                                        String masterName, Set<String> phoneNumbers, Set<String> emails) {

        String rawContactId = getRawContactId(contactId);
        if (rawContactId == null) {
            return;
        }

        if (masterName != null && !masterName.trim().isEmpty()) {
            operations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, masterName)
                    .build());
        }

        for (String phoneNumber : phoneNumbers) {
            if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                operations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber.trim())
                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                        .build());
            }
        }

        for (String email : emails) {
            if (email != null && !email.trim().isEmpty()) {
                operations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Email.DATA, email.trim())
                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_HOME)
                        .build());
            }
        }
    }

    private void deleteDuplicateContacts(ArrayList<ContentProviderOperation> operations, List<Contact> contactsToDelete) {
        for (Contact contact : contactsToDelete) {
            List<String> rawContactIds = getAllRawContactIds(contact.getId());

            for (String rawContactId : rawContactIds) {
                operations.add(ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI)
                        .withSelection(ContactsContract.RawContacts._ID + "=?",
                                new String[]{rawContactId})
                        .build());
            }
        }
    }

    private String getRawContactId(String contactId) {
        String rawContactId = null;

        Cursor cursor = getContentResolver().query(
                ContactsContract.RawContacts.CONTENT_URI,
                new String[]{ContactsContract.RawContacts._ID},
                ContactsContract.RawContacts.CONTACT_ID + "=? AND " + ContactsContract.RawContacts.DELETED + "=0",
                new String[]{contactId},
                null
        );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                rawContactId = cursor.getString(0);
            }
            cursor.close();
        }

        return rawContactId;
    }

    private List<String> getAllRawContactIds(String contactId) {
        List<String> rawContactIds = new ArrayList<>();

        Cursor cursor = getContentResolver().query(
                ContactsContract.RawContacts.CONTENT_URI,
                new String[]{ContactsContract.RawContacts._ID},
                ContactsContract.RawContacts.CONTACT_ID + "=? AND " + ContactsContract.RawContacts.DELETED + "=0",
                new String[]{contactId},
                null
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                rawContactIds.add(cursor.getString(0));
            }
            cursor.close();
        }

        return rawContactIds;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}