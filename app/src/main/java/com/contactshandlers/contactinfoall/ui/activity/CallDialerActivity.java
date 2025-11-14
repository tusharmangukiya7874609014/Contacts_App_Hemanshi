package com.contactshandlers.contactinfoall.ui.activity;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.AnimationUtils;
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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.adapter.ContactDialerAdapter;
import com.contactshandlers.contactinfoall.databinding.ActivityCallDialerBinding;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.helper.DefaultDialerUtils;
import com.contactshandlers.contactinfoall.helper.ToneGeneratorHelper;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.listeners.CallListener;
import com.contactshandlers.contactinfoall.model.ContactData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CallDialerActivity extends BaseActivity implements View.OnClickListener, View.OnLongClickListener {

    private ActivityCallDialerBinding binding;
    private CallDialerActivity activity = CallDialerActivity.this;
    private final StringBuilder phoneNumber = new StringBuilder();
    private ContactDialerAdapter adapter;
    private List<ContactData> contactsList = new ArrayList<>();
    private static final int REQUEST_CALL_PERMISSION = 1;
    private static final int REQUEST_READ_WRITE_PERMISSION = 2;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final long SEARCH_DELAY_MS = 0;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private static final int MAX_PHONE_NUMBER_LENGTH = 40;
    private ToneGeneratorHelper toneGeneratorHelper;
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
        binding = ActivityCallDialerBinding.inflate(getLayoutInflater());
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

    private void init() {
        onBack();
        initDefaultDialerLauncher();
        binding.btnDialer.performClick();
        binding.tvPhoneNumber.setShowSoftInputOnFocus(false);

        toneGeneratorHelper = new ToneGeneratorHelper(activity, 150L);

        Intent intent = getIntent();
        Uri data = intent.getData();

        if (data != null && data.getScheme().equals("tel")) {
            String number = data.getSchemeSpecificPart();

            binding.tvPhoneNumber.setText(number);
            phoneNumber.append(number);
        }

        binding.rvContacts.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    hideView(binding.clDialer);
                }
            }
        });

        binding.tvPhoneNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                phoneNumber.setLength(0);
                phoneNumber.append(s);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.rvContacts.setLayoutManager(new LinearLayoutManager(activity));
        adapter = new ContactDialerAdapter(activity, contactsList, new CallListener() {
            @Override
            public void onCall(String phoneNumber) {
                makePhoneCall(phoneNumber);
            }
        });
        binding.rvContacts.setAdapter(adapter);
    }

    private void initListener() {
        binding.btnDialer.setOnClickListener(this);
        binding.btnCall.setOnClickListener(this);
        binding.btnDelete.setOnClickListener(this);
        binding.tvAddNumber.setOnClickListener(this);
        binding.btnOne.setOnClickListener(this);
        binding.btnTwo.setOnClickListener(this);
        binding.btnThree.setOnClickListener(this);
        binding.btnFour.setOnClickListener(this);
        binding.btnFive.setOnClickListener(this);
        binding.btnSix.setOnClickListener(this);
        binding.btnSeven.setOnClickListener(this);
        binding.btnEight.setOnClickListener(this);
        binding.btnNine.setOnClickListener(this);
        binding.btnZero.setOnClickListener(this);
        binding.btnStar.setOnClickListener(this);
        binding.btnHash.setOnClickListener(this);

        binding.btnZero.setOnLongClickListener(this);
        binding.btnDelete.setOnLongClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnDialer) {
            showView(binding.clDialer);
        } else if (id == R.id.btnCall) {
            makePhoneCall(phoneNumber.toString());
        } else if (id == R.id.btnDelete) {
            handleDelete();
        } else if (id == R.id.tvAddNumber) {
            if (TextUtils.isEmpty(phoneNumber)) {
                Toast.makeText(activity, getString(R.string.enter_a_valid_number), Toast.LENGTH_SHORT).show();
                return;
            }
            addNewContact();
        } else if (id == R.id.btnOne) {
            onNumberClick("1");
        } else if (id == R.id.btnTwo) {
            onNumberClick("2");
        } else if (id == R.id.btnThree) {
            onNumberClick("3");
        } else if (id == R.id.btnFour) {
            onNumberClick("4");
        } else if (id == R.id.btnFive) {
            onNumberClick("5");
        } else if (id == R.id.btnSix) {
            onNumberClick("6");
        } else if (id == R.id.btnSeven) {
            onNumberClick("7");
        } else if (id == R.id.btnEight) {
            onNumberClick("8");
        } else if (id == R.id.btnNine) {
            onNumberClick("9");
        } else if (id == R.id.btnZero) {
            onNumberClick("0");
        } else if (id == R.id.btnStar) {
            onNumberClick("*");
        } else if (id == R.id.btnHash) {
            onNumberClick("#");
        }
    }

    @Override
    public boolean onLongClick(View v) {
        int id = v.getId();
        if (id == R.id.btnZero) {
            if (phoneNumber.length() < MAX_PHONE_NUMBER_LENGTH) {
                onNumberClick("+");
            }
        } else if (id == R.id.btnDelete) {
            if (phoneNumber.length() > 0) {
                phoneNumber.setLength(0);
                binding.tvPhoneNumber.setText("");
                updatePhoneNumberView();
            }
        }
        return true;
    }

    private void getAllContacts(String userInput) {
        if (!hasContactPermissions()) {
            contactsList.clear();
            adapter.setContacts(contactsList);
            return;
        }

        contactsList.clear();

        if (!userInput.isEmpty()) {
            try {
                ContentResolver contentResolver = getContentResolver();
                String selection = ContactsContract.CommonDataKinds.Phone.NUMBER + " LIKE ?";
                String[] selectionArgs = new String[]{"%" + userInput + "%"};

                Cursor cursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        selection,
                        selectionArgs,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
                );

                Set<String> addedContactIds = new HashSet<>();

                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));

                        if (addedContactIds.contains(id)) {
                            continue;
                        }

                        String firstName = "";
                        String lastName = "";
                        Cursor nameCursor = null;
                        try {
                            nameCursor = contentResolver.query(
                                    ContactsContract.Data.CONTENT_URI,
                                    new String[]{
                                            ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                                            ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME
                                    },
                                    ContactsContract.Data.CONTACT_ID + " = ? AND " +
                                            ContactsContract.Data.MIMETYPE + " = ?",
                                    new String[]{id, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE},
                                    null
                            );

                            if (nameCursor != null && nameCursor.moveToFirst()) {
                                firstName = nameCursor.getString(nameCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
                                lastName = nameCursor.getString(nameCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));
                            }
                        } finally {
                            if (nameCursor != null) {
                                nameCursor.close();
                            }
                        }

                        String phone = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        String photo = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI));
                        boolean isFavourite = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.STARRED)) == 1;

                        if (!TextUtils.isEmpty(firstName) && !TextUtils.isEmpty(phone)) {
                            contactsList.add(new ContactData(id, firstName, lastName, phone, photo, isFavourite, false));
                            addedContactIds.add(id);
                        }
                    }
                    cursor.close();
                }
            } catch (Exception e) {
                contactsList.clear();
            }
        }

        adapter.setContacts(contactsList);
    }

    private void showView(View view) {
        view.setTranslationY(view.getHeight());
        view.setVisibility(View.VISIBLE);
        view.animate()
                .translationY(0)
                .setDuration(200)
                .setInterpolator(AnimationUtils.loadInterpolator(this, android.R.interpolator.fast_out_slow_in))
                .start();
        binding.btnDialer.setVisibility(View.GONE);
    }

    private void hideView(View view) {
        binding.btnDialer.setVisibility(View.VISIBLE);
        view.animate()
                .translationY(view.getHeight())
                .setDuration(200)
                .setInterpolator(AnimationUtils.loadInterpolator(this, android.R.interpolator.fast_out_linear_in))
                .withEndAction(() -> view.setVisibility(View.GONE))
                .start();
    }

    public void onNumberClick(String digit) {
        if (phoneNumber.length() >= MAX_PHONE_NUMBER_LENGTH) {
            return;
        }

        int cursorPosition = binding.tvPhoneNumber.getSelectionStart();
        if (cursorPosition < 0) {
            cursorPosition = 0;
        } else if (cursorPosition > phoneNumber.length()) {
            cursorPosition = phoneNumber.length();
        }

        try {
            phoneNumber.insert(cursorPosition, digit);
            startDialpadTone(digit.charAt(0));
            binding.tvPhoneNumber.setText(phoneNumber.toString());

            int newPosition = cursorPosition + 1;
            if (newPosition <= phoneNumber.length()) {
                binding.tvPhoneNumber.setSelection(newPosition);
            }

            updatePhoneNumberView();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startDialpadTone(char c) {
        if (toneGeneratorHelper != null) {
            toneGeneratorHelper.startTone(c);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopDialpadTone();
                }
            }, 100);
        }
    }

    private void stopDialpadTone() {
        if (toneGeneratorHelper != null) {
            toneGeneratorHelper.stopTone();
        }
    }

    private void handleDelete() {
        if (phoneNumber.length() == 0) {
            return;
        }

        stopDialpadTone();

        int cursorPosition = binding.tvPhoneNumber.getSelectionStart();

        try {
            if (cursorPosition > 0 && cursorPosition <= phoneNumber.length()) {
                phoneNumber.delete(cursorPosition - 1, cursorPosition);
                binding.tvPhoneNumber.setText(phoneNumber.toString());

                int newPosition = cursorPosition - 1;
                if (newPosition <= phoneNumber.length()) {
                    binding.tvPhoneNumber.setSelection(newPosition);
                }

                updatePhoneNumberView();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updatePhoneNumberView() {
        if (phoneNumber.length() > 0) {
            binding.tvAddNumber.setVisibility(View.VISIBLE);
        } else {
            binding.tvAddNumber.setVisibility(View.INVISIBLE);
        }

        if (hasContactPermissions()) {
            debouncedSearch(phoneNumber.toString());
        }
    }

    private void makePhoneCall(String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            Toast.makeText(activity, getString(R.string.enter_a_valid_number), Toast.LENGTH_SHORT).show();
            return;
        }
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CALL_PHONE, Manifest.permission.ANSWER_PHONE_CALLS}, REQUEST_CALL_PERMISSION);
            return;
        }
        if (Utils.isDefaultDialer(activity)) {
            Utils.callContact(activity, phoneNumber);
            finish();
        } else {
            requestDefaultDialer();
        }
    }

    private void addNewContact() {
        startActivity(new Intent(activity, AddEditContactActivity.class)
                .putExtra(Constants.CONTACT_NUMBER, phoneNumber.toString())
                .putExtra(Constants.TITLE, getString(R.string.add_contact)));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (hasContactPermissions()) {
            getAllContacts(phoneNumber.toString());
        } else {
            requestContactPermissions();
            contactsList.clear();
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_WRITE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getAllContacts(phoneNumber.toString());
            } else {
                contactsList.clear();
                adapter.notifyDataSetChanged();
                Toast.makeText(activity, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void onBack() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                overridePendingTransition(R.anim.slide_in_top, R.anim.slide_out_bottom);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void debouncedSearch(final String query) {
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }

        searchRunnable = () -> {
            if (hasContactPermissions()) {
                performContactSearch(query);
            }
        };

        searchHandler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
    }

    private boolean hasContactPermissions() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestContactPermissions() {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS}, REQUEST_READ_WRITE_PERMISSION);
    }

    private void performContactSearch(String userInput) {
        executorService.execute(() -> {
            List<ContactData> searchResults = new ArrayList<>();

            if (!hasContactPermissions()) {
                runOnUiThread(() -> {
                    contactsList.clear();
                    adapter.setContacts(contactsList);
                });
                return;
            }

            if (!TextUtils.isEmpty(userInput)) {
                try {
                    ContentResolver contentResolver = getContentResolver();

                    String[] projection = {
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                            ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
                            ContactsContract.CommonDataKinds.Phone.STARRED
                    };

                    String selection = "(" +
                            ContactsContract.CommonDataKinds.Phone.NUMBER + " LIKE ? OR " +
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?)";
                    String[] selectionArgs = new String[]{"%" + userInput + "%", "%" + userInput + "%"};

                    String sortOrder = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC LIMIT 50";

                    Cursor cursor = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            projection,
                            selection,
                            selectionArgs,
                            sortOrder
                    );

                    Set<String> addedContactIds = new HashSet<>();

                    if (cursor != null) {
                        try {
                            while (cursor.moveToNext() && searchResults.size() < 50) {
                                String id = cursor.getString(0);

                                if (addedContactIds.contains(id)) {
                                    continue;
                                }

                                String displayName = cursor.getString(1);
                                String phone = cursor.getString(2);
                                String photo = cursor.getString(3);
                                boolean isFavourite = cursor.getInt(4) == 1;

                                if (!TextUtils.isEmpty(displayName) && !TextUtils.isEmpty(phone)) {
                                    String firstName = displayName;
                                    String lastName = "";

                                    int spaceIndex = displayName.indexOf(' ');
                                    if (spaceIndex > 0) {
                                        firstName = displayName.substring(0, spaceIndex);
                                        lastName = displayName.substring(spaceIndex + 1);
                                    }

                                    searchResults.add(new ContactData(id, firstName, lastName, phone, photo, isFavourite, false));
                                    addedContactIds.add(id);
                                }
                            }
                        } finally {
                            cursor.close();
                        }
                    }
                } catch (Exception e) {
                    searchResults.clear();
                }
            }

            runOnUiThread(() -> {
                contactsList.clear();
                contactsList.addAll(searchResults);
                adapter.setContacts(contactsList);
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
        searchHandler.removeCallbacksAndMessages(null);
    }
}