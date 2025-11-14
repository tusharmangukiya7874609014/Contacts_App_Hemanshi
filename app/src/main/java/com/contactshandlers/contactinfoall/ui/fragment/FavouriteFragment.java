package com.contactshandlers.contactinfoall.ui.fragment;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.adapter.FavouriteContactsAdapter;
import com.contactshandlers.contactinfoall.ads.InterstitialAD;
import com.contactshandlers.contactinfoall.databinding.FragmentFavouriteBinding;
import com.contactshandlers.contactinfoall.listeners.AdCallback;
import com.contactshandlers.contactinfoall.model.ContactData;
import com.contactshandlers.contactinfoall.ui.activity.SelectContactActivity;

import java.util.ArrayList;
import java.util.List;

public class FavouriteFragment extends Fragment implements View.OnClickListener {

    private FragmentFavouriteBinding binding;
    private FavouriteContactsAdapter adapter;
    private List<ContactData> favouriteContacts = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFavouriteBinding.inflate(inflater, container, false);

        init();
        initListener();

        return binding.getRoot();
    }

    private void init(){
        binding.rvFavourites.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        adapter = new FavouriteContactsAdapter(requireContext(), favouriteContacts);
        binding.rvFavourites.setAdapter(adapter);

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.READ_CONTACTS,Manifest.permission.WRITE_CONTACTS}, 100);
        } else {
            getFavouriteContacts();
        }
    }

    private void initListener() {
        binding.btnAddFavourite.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnAddFavourite) {
            InterstitialAD.getInstance().showInterstitial(requireActivity(), new AdCallback() {
                @Override
                public void callbackCall() {
                    Intent intent = new Intent(requireContext(), SelectContactActivity.class);
                    startActivity(intent);
                }
            });
        }
    }

    private void getFavouriteContacts() {
        favouriteContacts.clear();
        ContentResolver contentResolver = requireContext().getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI,
                new String[]{ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts.PHOTO_URI},
                ContactsContract.Contacts.STARRED + "=?", new String[]{"1"}, ContactsContract.Contacts.DISPLAY_NAME + " ASC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                String firstName = "";
                String lastName = "";
                Cursor nameCursor = contentResolver.query(
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
                    nameCursor.close();
                }
                String photo = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI));

                Cursor phoneCursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.STARRED},
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        new String[]{id}, null);

                String phone = null;
                boolean isFavourite = false;

                if (phoneCursor != null && phoneCursor.moveToFirst()) {
                    phone = phoneCursor.getString(phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));

                    int starredIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED);
                    if (starredIndex != -1) {
                        isFavourite = phoneCursor.getInt(starredIndex) == 1;
                    }

                    phoneCursor.close();
                }

                if (!TextUtils.isEmpty(firstName) && !TextUtils.isEmpty(phone)) {
                    favouriteContacts.add(new ContactData(id, firstName, lastName, phone, photo, isFavourite, false));
                }
            }
            cursor.close();
        }
        adapter.setContacts(favouriteContacts);
        if (favouriteContacts.isEmpty()) {
            binding.clNoFavourites.setVisibility(View.VISIBLE);
            binding.rvFavourites.setVisibility(View.GONE);
        } else {
            binding.clNoFavourites.setVisibility(View.GONE);
            binding.rvFavourites.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            getFavouriteContacts();
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getFavouriteContacts();
        }
    }
}