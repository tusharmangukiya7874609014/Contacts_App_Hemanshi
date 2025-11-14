package com.contactshandlers.contactinfoall.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.ads.InterstitialAD;
import com.contactshandlers.contactinfoall.databinding.ActivityGroupDetailBinding;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.listeners.AdCallback;
import com.contactshandlers.contactinfoall.model.ContactGroupItem;
import com.contactshandlers.contactinfoall.helper.ContactsGroupsManager;
import com.contactshandlers.contactinfoall.adapter.GroupedContactsAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GroupDetailActivity extends BaseActivity implements View.OnClickListener {

    private ActivityGroupDetailBinding binding;
    private final GroupDetailActivity activity = GroupDetailActivity.this;
    private ContactsGroupsManager contactsManager;
    private GroupedContactsAdapter groupedContactsAdapter;
    private List<ContactGroupItem> contacts = new ArrayList<>();
    private String groupId;
    private String groupTitle;
    private ExecutorService executorService;
    private Handler mainHandler;
    private ActivityResultLauncher<Intent> contactSelectionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityGroupDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initActivityResultLauncher();
        initListener();
        init();
    }

    private void initActivityResultLauncher() {
        contactSelectionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        ArrayList<ContactGroupItem> selectedContacts = result.getData().getParcelableArrayListExtra(Constants.SELECTED_CONTACTS);
                        ArrayList<ContactGroupItem> removedContacts = result.getData().getParcelableArrayListExtra(Constants.REMOVED_CONTACTS);

                        handleContactChanges(selectedContacts, removedContacts);
                    }
                });
    }

    private void initListener() {
        binding.included.ivBack.setOnClickListener(this);
        binding.included.ivMessage.setOnClickListener(this);
        binding.btnAddContact.setOnClickListener(this);
    }

    private void init() {
        executorService = Executors.newFixedThreadPool(2);
        mainHandler = new Handler(Looper.getMainLooper());
        getIntentData();
        binding.included.ivMessage.setVisibility(View.VISIBLE);
        int rightPadding = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                50,
                binding.getRoot().getResources().getDisplayMetrics()
        );
        binding.included.tvHeading.setPadding(0, 0, rightPadding, 0);
        binding.included.tvHeading.setText(groupTitle);
        setupRecyclerView();
        contactsManager = new ContactsGroupsManager(activity);
        loadGroupContacts();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.ivBack) {
            getOnBackPressedDispatcher().onBackPressed();
        } else if (id == R.id.ivMessage) {
            handleMessageGroup();
        } else if (id == R.id.btnAddContact) {
            handleAddContacts();
        }
    }

    private void getIntentData() {
        groupId = getIntent().getStringExtra(Constants.GROUP_ID);
        groupTitle = getIntent().getStringExtra(Constants.GROUP_TITLE);
    }

    private void setupRecyclerView() {
        groupedContactsAdapter = new GroupedContactsAdapter(activity, contacts);

        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        binding.rvContacts.setLayoutManager(layoutManager);
        binding.rvContacts.setAdapter(groupedContactsAdapter);

        binding.rvContacts.setNestedScrollingEnabled(true);
        binding.rvContacts.setHasFixedSize(false);
    }

    private void loadGroupContacts() {
        if (groupId == null) return;

        mainHandler.post(this::showProgress);

        CompletableFuture.supplyAsync(() -> {
            try {
                return contactsManager.getContactsInGroup(groupId);
            } catch (Exception e) {
                return new ArrayList<ContactGroupItem>();
            }
        }, executorService).thenAcceptAsync(loadedContacts -> {
            hideProgress();

            contacts.clear();
            contacts.addAll(loadedContacts);

            groupedContactsAdapter.updateContacts(contacts);

            if (contacts.isEmpty()) {
                showEmptyState();
            } else {
                hideEmptyState();
            }
        }, mainHandler::post);
    }

    private void handleContactChanges(List<ContactGroupItem> selectedContacts, List<ContactGroupItem> removedContacts) {
        if ((selectedContacts == null || selectedContacts.isEmpty()) &&
                (removedContacts == null || removedContacts.isEmpty())) {
            return;
        }

        mainHandler.post(() -> {
            binding.btnAddContact.setEnabled(false);
        });

        CompletableFuture.supplyAsync(() -> {
            int addedCount = 0;
            int removedCount = 0;

            try {
                if (selectedContacts != null && !selectedContacts.isEmpty()) {
                    addedCount = contactsManager.addContactsToGroup(groupId, selectedContacts);
                }

                if (removedContacts != null && !removedContacts.isEmpty()) {
                    for (ContactGroupItem contact : removedContacts) {
                        if (contactsManager.removeContactFromGroup(groupId, contact.getId())) {
                            removedCount++;
                        }
                    }
                }

                return new int[]{addedCount, removedCount};
            } catch (Exception e) {
                return new int[]{0, 0};
            }
        }, executorService).thenAcceptAsync(results -> {
            binding.btnAddContact.setEnabled(true);

            int addedCount = results[0];
            int removedCount = results[1];

            if (addedCount > 0 || removedCount > 0) {
                loadGroupContacts();
            } else {
                Toast.makeText(activity, getString(R.string.failed_to_update_group_please_try_again), Toast.LENGTH_LONG).show();
            }
        }, mainHandler::post);
    }

    private void handleMessageGroup() {
        if (contacts.isEmpty()) {
            Toast.makeText(activity, getString(R.string.no_contacts_to_message), Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<String> phoneNumbers = new ArrayList<>();
        for (ContactGroupItem contact : contacts) {
            if (contact.getPhoneNumber() != null && !contact.getPhoneNumber().isEmpty()) {
                phoneNumbers.add(contact.getPhoneNumber());
            }
        }

        if (phoneNumbers.isEmpty()) {
            return;
        }

        String phoneNumbersStr = String.join(";", phoneNumbers);
        Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
        smsIntent.setData(Uri.parse("smsto:" + phoneNumbersStr));

        try {
            startActivity(smsIntent);
        } catch (Exception e) {
            Toast.makeText(activity, getString(R.string.no_sms_app_found), Toast.LENGTH_SHORT).show();
        }
    }

    private void handleAddContacts() {
        InterstitialAD.getInstance().showInterstitial(activity, new AdCallback() {
            @Override
            public void callbackCall() {
                Intent intent = new Intent(activity, ContactSelectionActivity.class);
                intent.putParcelableArrayListExtra(Constants.EXISTING_CONTACTS, new ArrayList<>(contacts));
                contactSelectionLauncher.launch(intent);
            }
        });
    }

    private void showProgress() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.rvContacts.setVisibility(View.GONE);
        binding.clEmptyGroup.setVisibility(View.GONE);
        binding.btnAddContact.setVisibility(View.GONE);
    }

    private void hideProgress() {
        binding.progressBar.setVisibility(View.GONE);
        binding.btnAddContact.setVisibility(View.VISIBLE);
    }

    private void showEmptyState() {
        binding.clEmptyGroup.setVisibility(View.VISIBLE);
        binding.rvContacts.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        binding.clEmptyGroup.setVisibility(View.GONE);
        binding.rvContacts.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}