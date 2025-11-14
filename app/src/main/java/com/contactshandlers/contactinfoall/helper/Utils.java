package com.contactshandlers.contactinfoall.helper;

import static android.content.Context.TELECOM_SERVICE;

import android.Manifest;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.BlockedNumberContract;
import android.provider.ContactsContract;
import android.telecom.Call;
import android.telecom.TelecomManager;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.model.PhoneItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Utils {

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    public static boolean isRequirePermissions(Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALL_LOG) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    public static boolean isDefaultDialer(Context context) {
        TelecomManager telecomManager = (TelecomManager) context.getSystemService(TELECOM_SERVICE);
        String defaultDialerPackage = telecomManager.getDefaultDialerPackage();
        return context.getPackageName().equals(defaultDialerPackage);
    }

    public static boolean isCallerIdApp(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = (RoleManager) context.getSystemService(Context.ROLE_SERVICE);
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                return roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING);
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    public static int[] colorsList() {
        return new int[]{Color.parseColor("#FE8A7F"), Color.parseColor("#7C9971"), Color.parseColor("#73B2DF"), Color.parseColor("#FFC06E"), Color.parseColor("#9AAEBB"), Color.parseColor("#8F94FB"), Color.parseColor("#D3CBB8"), Color.parseColor("#EE9CA7"), Color.parseColor("#6B6B83"), Color.parseColor("#DCAA84")};
    }

    public static Bitmap getContactPhoto(Context context, String contactId) {
        if (contactId == null || contactId.isEmpty()) {
            return null;
        }

        try {
            Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.parseLong(contactId));
            InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(), uri);

            if (input != null) {
                return BitmapFactory.decodeStream(input);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getContactPhotoUri(Context context, String contactId) {
        Uri uri = ContactsContract.Contacts.CONTENT_URI;
        Cursor cursor = context.getContentResolver().query(
                uri,
                new String[]{ContactsContract.Contacts.PHOTO_URI},
                ContactsContract.Contacts._ID + " = ?",
                new String[]{contactId},
                null
        );

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI));
                }
            } finally {
                cursor.close();
            }
        }

        return null;
    }

    public static Bitmap getInitialsBitmap(int color, String name, int bgColor) {
        int size = 130;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(bgColor);
        canvas.drawCircle(size / 2, size / 2, size / 2, paint);

        if (color != 0) {
            paint.setColor(color);
        } else {
            paint.setColor(Color.WHITE);
        }
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(50);
        paint.setTextAlign(Paint.Align.CENTER);


        String letter = name != null && !name.isEmpty() ? name.substring(0, 1).toUpperCase() : "?";
        canvas.drawText(letter, size / 2, size / 2 + 15, paint);

        return bitmap;
    }

    public static Bitmap getInitialsBitmap(Context context, @DrawableRes int iconResId, int bgColor) {
        int size = 130;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(bgColor);
        canvas.drawCircle(size / 2, size / 2, size / 2, paint);

        Drawable iconDrawable = ContextCompat.getDrawable(context, iconResId);
        if (iconDrawable != null) {
            int iconSize = 70;
            int left = (size - iconSize) / 2;
            int top = (size - iconSize) / 2;
            int right = left + iconSize;
            int bottom = top + iconSize;

            iconDrawable.setBounds(left, top, right, bottom);
            iconDrawable.draw(canvas);
        }

        return bitmap;
    }

    public static void setStatusBarColor(Activity activity) {
        activity.getWindow().setStatusBarColor(activity.getColor(R.color.bg_screen));
        WindowInsetsControllerCompat insetsController = ViewCompat.getWindowInsetsController(activity.getWindow().getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = activity.getWindow().getInsetsController();
            if (controller != null) {
                if (isDarkTheme(activity)) {
                    controller.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                } else {
                    controller.setSystemBarsAppearance(WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                }
            }
        } else {
            Window window = activity.getWindow();
            View decorView = window.getDecorView();

            if (isDarkTheme(activity)) {
                decorView.setSystemUiVisibility(0);
            } else {
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }

    public static boolean isDarkTheme(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    public static boolean isContactStarred(Context context, String contactId) {
        Uri uri = ContactsContract.Contacts.CONTENT_URI;
        String[] projection = {ContactsContract.Contacts.STARRED};
        String selection = ContactsContract.Contacts._ID + " = ?";
        String[] selectionArgs = {contactId};

        Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int isStarred = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.STARRED));
                cursor.close();
                return isStarred == 1;
            }
            cursor.close();
        }
        return false;
    }

    public static int toggleContactStarred(Context context, String contactId, boolean isStarred) {
        Uri uri = ContactsContract.Contacts.CONTENT_URI;
        ContentValues values = new ContentValues();
        values.put(ContactsContract.Contacts.STARRED, isStarred ? 1 : 0);

        String selection = ContactsContract.Contacts._ID + " = ?";
        String[] selectionArgs = {contactId};

        return context.getContentResolver().update(uri, values, selection, selectionArgs);
    }

    public static boolean hasAnyBlocked(Context context, List<PhoneItem> phoneNumbers) {
        Uri uri = BlockedNumberContract.BlockedNumbers.CONTENT_URI;

        for (PhoneItem item : phoneNumbers) {
            if (item.getPhoneNumber() == null) continue;

            try (Cursor cursor = context.getContentResolver().query(
                    uri,
                    new String[]{BlockedNumberContract.BlockedNumbers.COLUMN_ID},
                    BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER + " = ?",
                    new String[]{item.getPhoneNumber()},
                    null
            )) {
                if (cursor != null && cursor.getCount() > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String getContactNameById(Context context, String contactId) {
        Cursor cursor = context.getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                new String[]{ContactsContract.Contacts.DISPLAY_NAME},
                ContactsContract.Contacts._ID + " = ?",
                new String[]{contactId},
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            String name = cursor.getString(0);
            cursor.close();
            return name;
        }

        if (cursor != null) {
            cursor.close();
        }

        return null;
    }

    public static String getContactNameByNumber(Context context, String number) {
        if (number == null || number.trim().isEmpty()) {
            return "Unknown";
        }

        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        String[] projection = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME};

        Cursor cursor = null;
        String contactName = null;

        try {
            cursor = context.getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                contactName = cursor.getString(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }

        return contactName;
    }

    public static void blockNumber(Context context, String phoneNumber) {
        ContentValues values = new ContentValues();
        values.put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, phoneNumber);
        context.getContentResolver().insert(BlockedNumberContract.BlockedNumbers.CONTENT_URI, values);
    }

    public static void unblockNumber(Context context, String phoneNumber) {
        context.getContentResolver().delete(
                BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER + "=?",
                new String[]{phoneNumber});
    }

    private static boolean isNumberBlocked(Context context, String phoneNumber) {
        Uri uri = BlockedNumberContract.BlockedNumbers.CONTENT_URI;
        String[] projection = {BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER};
        String selection = BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER + "=?";
        String[] selectionArgs = {phoneNumber};

        try (Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null)) {
            return cursor != null && cursor.getCount() > 0;
        }
    }


    public static void blockAllNumbers(Context context, List<PhoneItem> phoneNumbers) {
        for (PhoneItem number : phoneNumbers) {
            if (!isNumberBlocked(context, number.getPhoneNumber())) {
                blockNumber(context, number.getPhoneNumber());
            }
        }
    }

    public static void unblockAllNumbers(Context context, List<PhoneItem> phoneNumbers) {
        for (PhoneItem number : phoneNumbers) {
            if (isNumberBlocked(context, number.getPhoneNumber())) {
                unblockNumber(context, number.getPhoneNumber());

            }
        }
    }

    public static List<PhoneItem> getPhoneNumbersById(Context context, String contactId) {
        List<PhoneItem> phoneList = new ArrayList<>();
        Map<String, PhoneItem> normalizedMap = new LinkedHashMap<>();

        if (contactId == null || contactId.trim().isEmpty()) {
            return phoneList;
        }

        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection = new String[]{
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.LABEL
        };

        String selection = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?";
        String[] selectionArgs = new String[]{contactId.trim()};

        try (Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    int type = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE));
                    String phoneType = getPhoneTypeString(type, cursor);

                    String normalized = phoneNumber.replaceAll("[^0-9]", "");

                    if (!normalizedMap.containsKey(normalized)) {
                        normalizedMap.put(normalized, new PhoneItem(phoneNumber, phoneType));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        phoneList.addAll(normalizedMap.values());
        return phoneList;
    }


    public static String getPhoneTypeByNumber(Context context, String phoneNumber) {
        Cursor cursor = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.TYPE
                },
                ContactsContract.CommonDataKinds.Phone.NUMBER + " = ?",
                new String[]{phoneNumber},
                null
        );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int type = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE));
                return getPhoneTypeString(type, cursor);
            } else {
                cursor.close();
                return "Unknown";
            }
        }
        return "Unknown";
    }

    private static String getPhoneTypeString(int type, Cursor cursor) {
        switch (type) {
            case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
                return "Home";
            case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
                return "Work";
            case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
                return "Phone";
            case ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK:
                return "Work Fax";
            case ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME:
                return "Home Fax";
            case ContactsContract.CommonDataKinds.Phone.TYPE_PAGER:
                return "Pager";
            case ContactsContract.CommonDataKinds.Phone.TYPE_OTHER:
                return "Other";
            case ContactsContract.CommonDataKinds.Phone.TYPE_CALLBACK:
                return "Callback";
            case ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM:
                int labelIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL);
                if (labelIndex != -1) {
                    String customLabel = cursor.getString(labelIndex);
                    return customLabel != null ? customLabel : "Custom";
                }
                return "custom";
            default:
                return "Unknown";
        }
    }

    public static int convertStringPhoneTypeToInt(String phoneTypeString) {
        if (phoneTypeString == null || phoneTypeString.trim().isEmpty()) {
            return ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE;
        }

        switch (phoneTypeString) {
            case "Home":
                return ContactsContract.CommonDataKinds.Phone.TYPE_HOME;
            case "Work":
                return ContactsContract.CommonDataKinds.Phone.TYPE_WORK;
            case "Phone":
                return ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE;
            case "Work Fax":
                return ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK;
            case "Home Fax":
                return ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME;
            case "Pager":
                return ContactsContract.CommonDataKinds.Phone.TYPE_PAGER;
            case "Other":
                return ContactsContract.CommonDataKinds.Phone.TYPE_OTHER;
            case "Callback":
                return ContactsContract.CommonDataKinds.Phone.TYPE_CALLBACK;
            default:
                return ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM;
        }
    }

    public static String getContactIdByName(Context context, String name) {
        Cursor cursor = context.getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                new String[]{ContactsContract.Contacts._ID},
                ContactsContract.Contacts.DISPLAY_NAME + " = ?",
                new String[]{name},
                null
        );

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    public static File saveVCardToFile(Context context, String name, List<PhoneItem> phoneItems) {
        StringBuilder vCardContent = new StringBuilder();
        vCardContent.append("BEGIN:VCARD\n");
        vCardContent.append("VERSION:3.0\n");
        vCardContent.append("FN:").append(name).append("\n");

        for (PhoneItem phoneItem : phoneItems) {
            vCardContent.append("TEL;TYPE=").append(phoneItem.getPhoneType()).append(":")
                    .append(phoneItem.getPhoneNumber()).append("\n");
        }

        vCardContent.append("END:VCARD\n");
        String fileName = name.replaceAll(" ", "_") + ".vcf";

        try {
            File file = new File(context.getCacheDir(), fileName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(vCardContent.toString().getBytes());
            }
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<String> getContactEmails(Context context, String contactId) {
        List<String> emails = new ArrayList<>();

        Cursor cursor = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Email.ADDRESS},
                ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                new String[]{contactId},
                null
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                emails.add(cursor.getString(0));
            }
            cursor.close();
        }

        return emails;
    }

    public static void callContact(Context context, String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) return;

        String normalizedInput = PhoneNumberUtils.normalizeNumber(phoneNumber);

        for (Call call : CallListHelper.getUniqueCallList()) {
            String existingNumber = PhoneNumberUtils.normalizeNumber(call.getDetails().getHandle().getSchemeSpecificPart());
            if (PhoneNumberUtils.compare(existingNumber, normalizedInput)) {
                Toast.makeText(context, context.getString(R.string.number_already_exists_in_list_call_not_placed), Toast.LENGTH_SHORT).show();
                return;
            }
            if (PhoneNumberUtils.compare(existingNumber, phoneNumber)) {
                Toast.makeText(context, context.getString(R.string.number_already_exists_in_list_call_not_placed), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
        Uri uri = Uri.fromParts("tel", normalizedInput, null);
        Bundle extras = new Bundle();
        extras.putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, false);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
            if (telecomManager != null && telecomManager.getDefaultDialerPackage().equals(context.getPackageName())) {
                telecomManager.placeCall(uri, extras);
            } else {
                Intent callIntent = new Intent(Intent.ACTION_CALL, uri);
                callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(callIntent);
            }
        } else {
            Toast.makeText(context, context.getString(R.string.permission_required_to_make_calls), Toast.LENGTH_SHORT).show();
        }
    }

    public static void sendSMS(Context context, String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("smsto:" + phoneNumber));
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, context.getString(R.string.no_messaging_app_found), Toast.LENGTH_SHORT).show();
        }
    }

    public static void sendEmail(Context context, String emailAddress) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + emailAddress));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here");
        intent.putExtra(Intent.EXTRA_TEXT, "Body Here");
        context.startActivity(Intent.createChooser(intent, "Send Email"));
    }

    public static void setTheme() {
        String theme = SharedPreferencesManager.getInstance().getStringValue(Constants.THEME, Constants.THEME_SYSTEM);
        switch (theme) {
            case Constants.THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case Constants.THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case Constants.THEME_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}