package com.contactshandlers.contactinfoall.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.ads.InterstitialAD;
import com.contactshandlers.contactinfoall.databinding.ActivitySettingsBinding;
import com.contactshandlers.contactinfoall.databinding.DialogCallBackScreenBinding;
import com.contactshandlers.contactinfoall.databinding.DialogExportProgressBinding;
import com.contactshandlers.contactinfoall.databinding.DialogFormatBinding;
import com.contactshandlers.contactinfoall.databinding.DialogSortBinding;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.helper.SharedPreferencesManager;
import com.contactshandlers.contactinfoall.listeners.AdCallback;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

public class SettingsActivity extends BaseActivity implements View.OnClickListener{
    
    private ActivitySettingsBinding binding;
    private final SettingsActivity activity = SettingsActivity.this;
    private Dialog dialog;
    private DialogCallBackScreenBinding callBackScreenBinding;
    private DialogFormatBinding formatBinding;
    private DialogSortBinding sortBinding;
    private DialogExportProgressBinding progressDialog;
    private boolean isCallBackScreen, isShowPhoneNumber;
    private static final int REQUEST_PERMISSIONS = 100;
    private int request;
    private ActivityResultLauncher<Intent> importLauncher;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        initListener();
        init();
    }

    private void init() {
        binding.included.tvHeading.setText(getString(R.string.settings));

        isShowPhoneNumber = SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_SHOW_PHONE_NUMBER, true);
        binding.switchPhoneNumber.setChecked(isShowPhoneNumber);

        binding.switchPhoneNumber.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferencesManager.getInstance().setBooleanValue(Constants.IS_SHOW_PHONE_NUMBER, isChecked);
            }
        });

        binding.tvLanguage.setText(SharedPreferencesManager.getInstance().getStringValue(Constants.LANGUAGE_NAME, "English"));
        importLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    importVCard(uri);
                }
            }
        });
    }

    private void initListener() {
        binding.included.ivBack.setOnClickListener(this);
        binding.btnLanguage.setOnClickListener(this);
        binding.btnTheme.setOnClickListener(this);
        binding.btnCallBackScreen.setOnClickListener(this);
        binding.btnBlockedNumbers.setOnClickListener(this);
        binding.btnImportFile.setOnClickListener(this);
        binding.btnExportFile.setOnClickListener(this);
        binding.btnEmergencyContact.setOnClickListener(this);
        binding.btnNameFormat.setOnClickListener(this);
        binding.btnSortBy.setOnClickListener(this);
        binding.btnMergeNumbers.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.ivBack) {
            getOnBackPressedDispatcher().onBackPressed();
        } else if (id == R.id.btnLanguage) {
            InterstitialAD.getInstance().showInterstitial(activity, new AdCallback() {
                @Override
                public void callbackCall() {
                    startActivity(new Intent(activity, LanguageSelectionActivity.class).putExtra(Constants.IS_SECOND, true));
                }
            });
        } else if (id == R.id.btnTheme) {
            InterstitialAD.getInstance().showInterstitial(activity, new AdCallback() {
                @Override
                public void callbackCall() {
                    startActivity(new Intent(activity, ThemeActivity.class));
                }
            });
        } else if (id == R.id.btnCallBackScreen) {
            showCallBackScreenDialog();
        } else if (id == R.id.btnBlockedNumbers) {
            InterstitialAD.getInstance().showInterstitial(activity, new AdCallback() {
                @Override
                public void callbackCall() {
                    startActivity(new Intent(activity, BlockedNumberActivity.class));
                }
            });
        } else if (id == R.id.btnImportFile) {
            request = 1;
            requestPermissions(1);
        } else if (id == R.id.btnExportFile) {
            request = 2;
            requestPermissions(2);
        } else if (id == R.id.btnEmergencyContact) {
            InterstitialAD.getInstance().showInterstitial(activity, new AdCallback() {
                @Override
                public void callbackCall() {
                    startActivity(new Intent(activity, EmergencyContactActivity.class));
                }
            });
        } else if (id == R.id.btnNameFormat) {
            showFormatDialog();
        } else if (id == R.id.btnSortBy) {
            showSortDialog();
        } else if (id == R.id.btnMergeNumbers) {
            InterstitialAD.getInstance().showInterstitial(activity, new AdCallback() {
                @Override
                public void callbackCall() {
                    startActivity(new Intent(activity, MergeOptionsActivity.class));
                }
            });
        }
    }

    private void showCallBackScreenDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        dialog = new Dialog(activity);
        callBackScreenBinding = DialogCallBackScreenBinding.inflate(getLayoutInflater());
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.setContentView(callBackScreenBinding.getRoot());
        dialog.setCancelable(true);

        isCallBackScreen = SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_CALL_BACK_SCREEN, true);
        callBackScreenBinding.switchCallBack.setChecked(isCallBackScreen);

        callBackScreenBinding.btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        callBackScreenBinding.switchCallBack.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferencesManager.getInstance().setBooleanValue(Constants.IS_CALL_BACK_SCREEN, isChecked);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void requestPermissions(int request) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(activity, new String[]{
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.WRITE_CONTACTS,
                }, REQUEST_PERMISSIONS);
            } else {
                if (request == 1) {
                    importContacts();
                } else if (request == 2) {
                    exportContacts();
                }
            }
        } else {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(activity, new String[]{
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.WRITE_CONTACTS,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, REQUEST_PERMISSIONS);
            } else {
                if (request == 1) {
                    importContacts();
                } else if (request == 2) {
                    exportContacts();
                }
            }
        }
    }

    private void importContacts() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/x-vcard");
        importLauncher.launch(intent);
    }

    private void importVCard(Uri uri) {
        showProgressDialog(getString(R.string.importing_contacts));

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                if (inputStream == null) {
                    throw new IOException("Failed to open input stream");
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                int totalContacts = 0;
                int processedContacts = 0;
                int importedContacts = 0;
                int duplicateContacts = 0;

                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("BEGIN:VCARD")) {
                        totalContacts++;
                    }
                }
                inputStream.close();

                inputStream = getContentResolver().openInputStream(uri);
                reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder currentVCard = new StringBuilder();
                boolean inVCard = false;

                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("BEGIN:VCARD")) {
                        inVCard = true;
                        currentVCard = new StringBuilder();
                    }

                    if (inVCard) {
                        currentVCard.append(line).append("\n");
                    }

                    if (line.startsWith("END:VCARD")) {
                        inVCard = false;
                        String vCardContent = currentVCard.toString();

                        ContactInfo contactInfo = extractContactInfo(vCardContent);

                        boolean isDuplicate = contactExists(contactInfo.getName());

                        if (!isDuplicate) {
                            processVCard(vCardContent);
                            importedContacts++;
                        } else {
                            duplicateContacts++;
                        }

                        processedContacts++;

                        int progress = (int) (((float) processedContacts / totalContacts) * 100);
                        updateProgress(progress);
                    }
                }

                inputStream.close();

                handler.post(() -> {
                    dismissProgressDialog();
                    Toast.makeText(activity,
                            getString(R.string.contacts_imported_successfully),
                            Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> {
                    dismissProgressDialog();
                    Toast.makeText(activity,
                            getString(R.string.import_failed),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private static class ContactInfo {
        private String name;
        private List<String> phoneNumbers;
        private List<String> emails;

        public ContactInfo() {
            this.phoneNumbers = new ArrayList<>();
            this.emails = new ArrayList<>();
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getPhoneNumbers() {
            return phoneNumbers;
        }

        public List<String> getEmails() {
            return emails;
        }

        public void addPhoneNumber(String phoneNumber) {
            this.phoneNumbers.add(phoneNumber);
        }

        public void addEmail(String email) {
            this.emails.add(email);
        }
    }

    private ContactInfo extractContactInfo(String vCardData) {
        ContactInfo contactInfo = new ContactInfo();
        String[] lines = vCardData.split("\n");

        for (String line : lines) {
            if (line.startsWith(" ") || line.startsWith("\t")) {
                continue;
            }

            if (line.startsWith("FN:")) {
                contactInfo.setName(line.substring(3).trim());
            }
            else if (line.startsWith("TEL")) {
                String number = line.substring(line.lastIndexOf(":") + 1).trim();
                number = number.replaceAll("[^0-9+]", "");
                contactInfo.addPhoneNumber(number);
            }
            else if (line.startsWith("EMAIL")) {
                String email = line.substring(line.lastIndexOf(":") + 1).trim();
                contactInfo.addEmail(email);
            }
        }

        return contactInfo;
    }

    private void processVCard(String vCardData) {
        try {
            ArrayList<ContentProviderOperation> ops = new ArrayList<>();
            int rawContactInsertIndex = ops.size();

            ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build());

            String[] lines = vCardData.split("\n");
            String fullName = null;
            String firstName = null;
            String lastName = null;
            List<String> phones = new ArrayList<>();
            List<String> emails = new ArrayList<>();
            Map<String, Integer> phoneTypes = new HashMap<>();
            Map<String, Integer> emailTypes = new HashMap<>();

            for (String line : lines) {
                if (line.startsWith(" ") || line.startsWith("\t")) {
                    continue;
                }

                if (line.startsWith("FN:")) {
                    fullName = line.substring(3).trim();
                } else if (line.startsWith("N:")) {
                    String[] nameParts = line.substring(2).split(";");
                    if (nameParts.length > 0) lastName = nameParts[0].trim();
                    if (nameParts.length > 1) firstName = nameParts[1].trim();
                } else if (line.startsWith("TEL")) {
                    int typeValue = ContactsContract.CommonDataKinds.Phone.TYPE_OTHER;
                    if (line.contains("TYPE=")) {
                        String typeStr = line.substring(line.indexOf("TYPE=") + 5);
                        if (typeStr.contains(";")) {
                            typeStr = typeStr.substring(0, typeStr.indexOf(";"));
                        } else if (typeStr.contains(":")) {
                            typeStr = typeStr.substring(0, typeStr.indexOf(":"));
                        }

                        typeStr = typeStr.toUpperCase().replace("\"", "");

                        if (typeStr.contains("HOME")) {
                            typeValue = ContactsContract.CommonDataKinds.Phone.TYPE_HOME;
                        } else if (typeStr.contains("CELL") || typeStr.contains("MOBILE")) {
                            typeValue = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE;
                        } else if (typeStr.contains("WORK")) {
                            typeValue = ContactsContract.CommonDataKinds.Phone.TYPE_WORK;
                        } else if (typeStr.contains("FAX_WORK")) {
                            typeValue = ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK;
                        } else if (typeStr.contains("FAX_HOME")) {
                            typeValue = ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME;
                        } else if (typeStr.contains("PAGER")) {
                            typeValue = ContactsContract.CommonDataKinds.Phone.TYPE_PAGER;
                        } else if (typeStr.contains("OTHER")) {
                            typeValue = ContactsContract.CommonDataKinds.Phone.TYPE_OTHER;
                        } else if (typeStr.contains("CALLBACK")) {
                            typeValue = ContactsContract.CommonDataKinds.Phone.TYPE_CALLBACK;
                        } else if (typeStr.contains("CAR")) {
                            typeValue = ContactsContract.CommonDataKinds.Phone.TYPE_CAR;
                        } else if (typeStr.contains("COMPANY_MAIN")) {
                            typeValue = ContactsContract.CommonDataKinds.Phone.TYPE_COMPANY_MAIN;
                        } else if (typeStr.contains("ISDN")) {
                            typeValue = ContactsContract.CommonDataKinds.Phone.TYPE_ISDN;
                        } else if (typeStr.contains("MAIN")) {
                            typeValue = ContactsContract.CommonDataKinds.Phone.TYPE_MAIN;
                        } else if (typeStr.contains("OTHER_FAX")) {
                            typeValue = ContactsContract.CommonDataKinds.Phone.TYPE_OTHER_FAX;
                        } else if (typeStr.contains("RADIO")) {
                            typeValue = ContactsContract.CommonDataKinds.Phone.TYPE_RADIO;
                        } else if (typeStr.contains("TELEX")) {
                            typeValue = ContactsContract.CommonDataKinds.Phone.TYPE_TELEX;
                        } else if (typeStr.contains("TTY_TDD")) {
                            typeValue = ContactsContract.CommonDataKinds.Phone.TYPE_TTY_TDD;
                        } else if (typeStr.contains("WORK_MOBILE")) {
                            typeValue = ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE;
                        } else if (typeStr.contains("WORK_PAGER")) {
                            typeValue = ContactsContract.CommonDataKinds.Phone.TYPE_WORK_PAGER;
                        } else if (typeStr.contains("ASSISTANT")) {
                            typeValue = ContactsContract.CommonDataKinds.Phone.TYPE_ASSISTANT;
                        } else if (typeStr.contains("MMS")) {
                            typeValue = ContactsContract.CommonDataKinds.Phone.TYPE_MMS;
                        }
                    }

                    String number = line.substring(line.lastIndexOf(":") + 1).trim();
                    phones.add(number);
                    phoneTypes.put(number, typeValue);
                } else if (line.startsWith("EMAIL")) {
                    int typeValue = ContactsContract.CommonDataKinds.Email.TYPE_OTHER;

                    if (line.contains("TYPE=")) {
                        String typeStr = line.substring(line.indexOf("TYPE=") + 5);
                        if (typeStr.contains(";")) {
                            typeStr = typeStr.substring(0, typeStr.indexOf(";"));
                        } else if (typeStr.contains(":")) {
                            typeStr = typeStr.substring(0, typeStr.indexOf(":"));
                        }

                        typeStr = typeStr.toUpperCase().replace("\"", "");

                        if (typeStr.contains("HOME")) {
                            typeValue = ContactsContract.CommonDataKinds.Email.TYPE_HOME;
                        } else if (typeStr.contains("WORK")) {
                            typeValue = ContactsContract.CommonDataKinds.Email.TYPE_WORK;
                        } else if (typeStr.contains("OTHER")) {
                            typeValue = ContactsContract.CommonDataKinds.Email.TYPE_OTHER;
                        } else if (typeStr.contains("MOBILE")) {
                            typeValue = ContactsContract.CommonDataKinds.Email.TYPE_MOBILE;
                        }
                    }

                    String email = line.substring(line.lastIndexOf(":") + 1).trim();
                    emails.add(email);
                    emailTypes.put(email, typeValue);
                }
            }

            if (fullName != null || firstName != null || lastName != null) {
                ContentProviderOperation.Builder nameOp = ContentProviderOperation
                        .newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);

                if (fullName != null) {
                    nameOp.withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, fullName);
                }

                if (firstName != null) {
                    nameOp.withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, firstName);
                }

                if (lastName != null) {
                    nameOp.withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, lastName);
                }

                ops.add(nameOp.build());
            }

            for (String phone : phones) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                                phoneTypes.getOrDefault(phone, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE))
                        .build());
            }

            for (String email : emails) {
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE,
                                emailTypes.getOrDefault(email, ContactsContract.CommonDataKinds.Email.TYPE_HOME))
                        .build());
            }

            getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);

        } catch (
                Exception e) {
        }
    }

    private boolean contactExists(String name) {
        ContentResolver resolver = getContentResolver();

        if (name != null) {
            Cursor nameCursor = resolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    null,
                    ContactsContract.Data.MIMETYPE + " = ? AND " +
                            ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME + " = ?",
                    new String[]{ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE, name},
                    null);

            if (nameCursor != null && nameCursor.getCount() > 0) {
                nameCursor.close();
                return true;
            }

            if (nameCursor != null) nameCursor.close();
        }
        return false;
    }

    private void exportContacts() {
        showProgressDialog(getString(R.string.exporting_contacts));

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<String> contacts = getContacts();
                int totalContacts = contacts.size();

                if (totalContacts == 0) {
                    handler.post(() -> {
                        dismissProgressDialog();
                    });
                    return;
                }

                int processedContacts = 0;
                int lastProgress = 0;

                String fileName = "Contacts_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                        .format(new Date()) + ".vcf";
                OutputStream outputStream = null;
                ContentResolver contentResolver = getContentResolver();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                    values.put(MediaStore.Downloads.MIME_TYPE, "text/x-vcard");
                    values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                    Uri uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (uri != null) {
                        outputStream = contentResolver.openOutputStream(uri);
                    }
                } else {
                    File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    if (!downloadsDir.exists() && !downloadsDir.mkdirs()) {
                        throw new IOException("Could not create Downloads directory");
                    }
                    File file = new File(downloadsDir, fileName);
                    outputStream = new FileOutputStream(file);
                }

                if (outputStream != null) {
                    updateProgress(0);

                    int batchSize = Math.max(1, totalContacts / 20);
                    int contactsInCurrentBatch = 0;

                    for (String contact : contacts) {
                        outputStream.write(contact.getBytes());
                        processedContacts++;
                        contactsInCurrentBatch++;

                        if (contactsInCurrentBatch >= batchSize) {
                            int progress = (int) (((float) processedContacts / totalContacts) * 100);
                            if (progress > lastProgress) {
                                lastProgress = progress;
                                updateProgress(progress);
                            }
                            contactsInCurrentBatch = 0;
                        }
                    }

                    updateProgress(100);
                    outputStream.close();

                    handler.post(() -> {
                        dismissProgressDialog();
                        Toast.makeText(activity,
                                getString(R.string.contacts_exported_to_downloads_folder),
                                Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> {
                    dismissProgressDialog();
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(activity, getString(R.string.export_failed), Toast.LENGTH_LONG).show();
                    });
                });
            }
        });
    }

    private List<String> getContacts() {
        List<String> contacts = new ArrayList<>();
        Set<String> processedPhoneNumbers = new HashSet<>();
        ContentResolver contentResolver = getContentResolver();

        String[] projection = {
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.HAS_PHONE_NUMBER
        };

        try (Cursor cursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                null,
                null,
                ContactsContract.Contacts.DISPLAY_NAME + " ASC")) {

            if (cursor != null) {
                int idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID);
                int nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                int hasPhoneIndex = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);

                if (idIndex < 0 || nameIndex < 0 || hasPhoneIndex < 0) {
                    return contacts;
                }

                while (cursor.moveToNext()) {
                    String contactId = cursor.getString(idIndex);
                    String name = cursor.getString(nameIndex);
                    int hasPhone = cursor.getInt(hasPhoneIndex);

                    if (name == null || name.trim().isEmpty()) {
                        continue;
                    }

                    StringBuilder vCard = new StringBuilder();
                    vCard.append("BEGIN:VCARD\n");
                    vCard.append("VERSION:3.0\n");

                    vCard.append("FN:").append(name).append("\n");

                    String[] nameParts = name.split(" ", 2);
                    if (nameParts.length > 1) {
                        vCard.append("N:").append(nameParts[1]).append(";")
                                .append(nameParts[0]).append(";;;\n");
                    } else {
                        vCard.append("N:;").append(name).append(";;;\n");
                    }

                    if (hasPhone > 0) {
                        try (Cursor phones = contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                new String[]{contactId},
                                null)) {

                            if (phones != null) {
                                int phoneNumberIndex = phones.getColumnIndex(
                                        ContactsContract.CommonDataKinds.Phone.NUMBER);
                                int phoneTypeIndex = phones.getColumnIndex(
                                        ContactsContract.CommonDataKinds.Phone.TYPE);

                                if (phoneNumberIndex >= 0) {
                                    while (phones.moveToNext()) {
                                        String phoneNumber = phones.getString(phoneNumberIndex);
                                        if (phoneNumber != null) {
                                            String normalizedNumber = phoneNumber.replaceAll("[^0-9+]", "");
                                            if (!processedPhoneNumbers.contains(normalizedNumber)) {
                                                processedPhoneNumbers.add(normalizedNumber);

                                                int phoneType = phoneTypeIndex >= 0 ?
                                                        phones.getInt(phoneTypeIndex) :
                                                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE;

                                                String typeStr = "CELL";
                                                switch (phoneType) {
                                                    case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
                                                        typeStr = "HOME";
                                                        break;
                                                    case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
                                                        typeStr = "MOBILE";
                                                        break;
                                                    case ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK:
                                                        typeStr = "FAX_WORK";
                                                        break;
                                                    case ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME:
                                                        typeStr = "FAX_HOME";
                                                        break;
                                                    case ContactsContract.CommonDataKinds.Phone.TYPE_PAGER:
                                                        typeStr = "PAGER";
                                                        break;
                                                    case ContactsContract.CommonDataKinds.Phone.TYPE_OTHER:
                                                        typeStr = "OTHER";
                                                        break;
                                                    case ContactsContract.CommonDataKinds.Phone.TYPE_CALLBACK:
                                                        typeStr = "CALLBACK";
                                                        break;
                                                    case ContactsContract.CommonDataKinds.Phone.TYPE_CAR:
                                                        typeStr = "CAR";
                                                        break;
                                                    case ContactsContract.CommonDataKinds.Phone.TYPE_COMPANY_MAIN:
                                                        typeStr = "COMPANY_MAIN";
                                                        break;
                                                    case ContactsContract.CommonDataKinds.Phone.TYPE_ISDN:
                                                        typeStr = "ISDN";
                                                        break;
                                                    case ContactsContract.CommonDataKinds.Phone.TYPE_OTHER_FAX:
                                                        typeStr = "OTHER_FAX";
                                                        break;
                                                    case ContactsContract.CommonDataKinds.Phone.TYPE_RADIO:
                                                        typeStr = "RADIO";
                                                        break;
                                                    case ContactsContract.CommonDataKinds.Phone.TYPE_TELEX:
                                                        typeStr = "TELEX";
                                                        break;
                                                    case ContactsContract.CommonDataKinds.Phone.TYPE_TTY_TDD:
                                                        typeStr = "TTY_TDD";
                                                        break;
                                                    case ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE:
                                                        typeStr = "WORK_MOBILE";
                                                        break;
                                                    case ContactsContract.CommonDataKinds.Phone.TYPE_WORK_PAGER:
                                                        typeStr = "WORK_PAGER";
                                                        break;
                                                    case ContactsContract.CommonDataKinds.Phone.TYPE_ASSISTANT:
                                                        typeStr = "ASSISTANT";
                                                        break;
                                                    case ContactsContract.CommonDataKinds.Phone.TYPE_MMS:
                                                        typeStr = "MMS";
                                                        break;
                                                }
                                                vCard.append("TEL;TYPE=").append(typeStr).append(":")
                                                        .append(phoneNumber).append("\n");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    try (Cursor emails = contentResolver.query(
                            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                            new String[]{contactId},
                            null)) {

                        if (emails != null) {
                            int emailAddressIndex = emails.getColumnIndex(
                                    ContactsContract.CommonDataKinds.Email.ADDRESS);
                            int emailTypeIndex = emails.getColumnIndex(
                                    ContactsContract.CommonDataKinds.Email.TYPE);

                            if (emailAddressIndex >= 0) {
                                while (emails.moveToNext()) {
                                    String email = emails.getString(emailAddressIndex);
                                    if (email != null && !email.trim().isEmpty()) {
                                        int emailType = emailTypeIndex >= 0 ?
                                                emails.getInt(emailTypeIndex) :
                                                ContactsContract.CommonDataKinds.Email.TYPE_HOME;

                                        String typeStr = "HOME";
                                        switch (emailType) {
                                            case ContactsContract.CommonDataKinds.Email.TYPE_HOME:
                                                typeStr = "HOME";
                                                break;
                                            case ContactsContract.CommonDataKinds.Email.TYPE_WORK:
                                                typeStr = "WORK";
                                                break;
                                            case ContactsContract.CommonDataKinds.Email.TYPE_OTHER:
                                                typeStr = "OTHER";
                                                break;
                                            case ContactsContract.CommonDataKinds.Email.TYPE_MOBILE:
                                                typeStr = "MOBILE";
                                                break;
                                        }
                                        vCard.append("EMAIL;TYPE=").append(typeStr).append(":")
                                                .append(email).append("\n");
                                    }
                                }
                            }
                        }
                    }

                    vCard.append("END:VCARD\n");
                    contacts.add(vCard.toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return contacts;
    }

    private void showFormatDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        dialog = new Dialog(activity);
        formatBinding = DialogFormatBinding.inflate(getLayoutInflater());
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.setContentView(formatBinding.getRoot());
        dialog.setCancelable(true);

        formatBinding.btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        formatBinding.btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        boolean formatByFirst = SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_FORMAT_BY_FIRST, true);
        formatBinding.radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                SharedPreferencesManager.getInstance().setBooleanValue(Constants.IS_FORMAT_BY_FIRST, checkedId == R.id.radioButton1);
                dialog.dismiss();
            }
        });

        if (formatByFirst) {
            formatBinding.radioButton1.setChecked(true);
            formatBinding.radioButton1.setButtonTintList(ColorStateList.valueOf(activity.getColor(R.color.main2)));
            formatBinding.radioButton2.setButtonTintList(ColorStateList.valueOf(activity.getColor(R.color.grey_font)));
            formatBinding.radioButton1.setTextColor(ColorStateList.valueOf(activity.getColor(R.color.main2)));
            formatBinding.radioButton2.setTextColor(ColorStateList.valueOf(activity.getColor(R.color.primary_font)));
        } else {
            formatBinding.radioButton2.setChecked(true);
            formatBinding.radioButton2.setButtonTintList(ColorStateList.valueOf(activity.getColor(R.color.main2)));
            formatBinding.radioButton1.setButtonTintList(ColorStateList.valueOf(activity.getColor(R.color.grey_font)));
            formatBinding.radioButton2.setTextColor(ColorStateList.valueOf(activity.getColor(R.color.main2)));
            formatBinding.radioButton1.setTextColor(ColorStateList.valueOf(activity.getColor(R.color.primary_font)));
        }

        dialog.show();
    }

    private void showSortDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        dialog = new Dialog(activity);
        sortBinding = DialogSortBinding.inflate(getLayoutInflater());
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.setContentView(sortBinding.getRoot());
        dialog.setCancelable(true);

        sortBinding.btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        sortBinding.btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        boolean sortByFirstName = SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_SORT_BY_FIRST_NAME, true);
        sortBinding.radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                SharedPreferencesManager.getInstance().setBooleanValue(Constants.IS_SORT_BY_FIRST_NAME, checkedId == R.id.radioButton1);
                dialog.dismiss();
            }
        });

        if (sortByFirstName) {
            sortBinding.radioButton1.setChecked(true);
            sortBinding.radioButton1.setButtonTintList(ColorStateList.valueOf(activity.getColor(R.color.main2)));
            sortBinding.radioButton2.setButtonTintList(ColorStateList.valueOf(activity.getColor(R.color.grey_font)));
            sortBinding.radioButton1.setTextColor(ColorStateList.valueOf(activity.getColor(R.color.main2)));
            sortBinding.radioButton2.setTextColor(ColorStateList.valueOf(activity.getColor(R.color.primary_font)));
        } else {
            sortBinding.radioButton2.setChecked(true);
            sortBinding.radioButton2.setButtonTintList(ColorStateList.valueOf(activity.getColor(R.color.main2)));
            sortBinding.radioButton1.setButtonTintList(ColorStateList.valueOf(activity.getColor(R.color.grey_font)));
            sortBinding.radioButton2.setTextColor(ColorStateList.valueOf(activity.getColor(R.color.main2)));
            sortBinding.radioButton1.setTextColor(ColorStateList.valueOf(activity.getColor(R.color.primary_font)));
        }

        dialog.show();
    }

    private void showProgressDialog(String message) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        progressDialog = DialogExportProgressBinding.inflate(getLayoutInflater());
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setContentView(progressDialog.getRoot());
        dialog.setCancelable(false);

        progressDialog.tvTitle.setText(message);
        progressDialog.progressBar.setProgress(0);
        progressDialog.tvPercentage.setText("0%");

        handler.post(() -> {
            if (!dialog.isShowing()) {
                dialog.show();
            }
        });
    }

    private void updateProgress(int progress) {
        handler.post(() -> {
            if (dialog != null && dialog.isShowing() && progressDialog != null) {
                progressDialog.progressBar.setProgress(progress);
                progressDialog.tvPercentage.setText(progress + "%");
            }
        });
    }

    private void dismissProgressDialog() {
        handler.post(() -> {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (request == 1) {
                    importContacts();
                } else if (request == 2) {
                    exportContacts();
                }
            } else {
                Toast.makeText(activity, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show();
            }
        }
    }
}