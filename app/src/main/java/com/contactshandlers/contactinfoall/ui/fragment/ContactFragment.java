package com.contactshandlers.contactinfoall.ui.fragment;

import static android.content.Context.INPUT_METHOD_SERVICE;

import android.Manifest;
import android.app.Dialog;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.TypefaceSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.adapter.ContactGroupAdapter;
import com.contactshandlers.contactinfoall.adapter.RecentAddedAdapter;
import com.contactshandlers.contactinfoall.adapter.RecentViewAdapter;
import com.contactshandlers.contactinfoall.databinding.DialogClearRoomBinding;
import com.contactshandlers.contactinfoall.databinding.DialogDeleteBinding;
import com.contactshandlers.contactinfoall.databinding.FragmentContactBinding;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.helper.SharedPreferencesManager;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.model.BinContactEntity;
import com.contactshandlers.contactinfoall.model.ContactData;
import com.contactshandlers.contactinfoall.model.ContactGroup;
import com.contactshandlers.contactinfoall.model.PhoneItem;
import com.contactshandlers.contactinfoall.model.RecentAddedContact;
import com.contactshandlers.contactinfoall.model.RecentViewedContact;
import com.contactshandlers.contactinfoall.room.BinContactViewModel;
import com.contactshandlers.contactinfoall.room.ContactDao;
import com.contactshandlers.contactinfoall.room.ContactDatabase;
import com.contactshandlers.contactinfoall.room.ContactViewModel;
import com.contactshandlers.contactinfoall.ui.activity.AddEditContactActivity;
import com.contactshandlers.contactinfoall.ui.activity.CallDialerActivity;
import com.contactshandlers.contactinfoall.ui.activity.SettingsActivity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Executors;

public class ContactFragment extends Fragment implements View.OnClickListener {

    private FragmentContactBinding binding;
    private List<ContactGroup> contactGroups = new ArrayList<>();
    private static ImageView ivClose;
    private static TextView tvCounting;
    private static ConstraintLayout clSelectionMode, clHeader, clSearch;
    private static HorizontalScrollView hsv;
    private ContactGroupAdapter adapter;
    private List<RecentAddedContact> recentAddedContacts = new ArrayList<>();
    private List<RecentViewedContact> recentViewedContacts = new ArrayList<>();
    private RecentAddedAdapter recentAddedAdapter;
    private RecentViewAdapter recentViewAdapter;
    private ContactViewModel contactViewModel;
    private Dialog dialog;
    private DialogDeleteBinding deleteBinding;
    private DialogClearRoomBinding clearRoomBinding;
    private boolean isAscending = true;
    public static boolean isSelectionMode = false;
    public static final Set<ContactData> selectedContacts = new HashSet<>();
    private ContactDao contactDao;
    private boolean sortByFirstName, isFormatFirst;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentContactBinding.inflate(inflater, container, false);
        initListener();
        init();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ivClose = view.findViewById(R.id.ivClose);
        tvCounting = view.findViewById(R.id.tvCounting);
        clSelectionMode = view.findViewById(R.id.clSelectionMode);
        clHeader = view.findViewById(R.id.clHeader);
        clSearch = view.findViewById(R.id.clSearch);
        hsv = view.findViewById(R.id.hsv);
    }

    private void init() {
        isAscending = SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_ASCENDING, true);
        sortByFirstName = SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_SORT_BY_FIRST_NAME, true);
        isFormatFirst = SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_FORMAT_BY_FIRST, true);

        contactDao = ContactDatabase.getInstance(requireContext()).contactDao();

        binding.rvContacts.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ContactGroupAdapter(contactGroups, requireContext());
        binding.rvContacts.setAdapter(adapter);

        binding.rvRecentAdded.setLayoutManager(new LinearLayoutManager(requireContext()));
        recentAddedAdapter = new RecentAddedAdapter(requireContext(), recentAddedContacts);
        binding.rvRecentAdded.setAdapter(recentAddedAdapter);

        binding.rvRecentView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recentViewAdapter = new RecentViewAdapter(requireContext(), recentViewedContacts);
        binding.rvRecentView.setAdapter(recentViewAdapter);

        contactViewModel = new ViewModelProvider(this).get(ContactViewModel.class);

        binding.btnAll.performClick();

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.READ_CONTACTS,Manifest.permission.WRITE_CONTACTS}, 100);
        } else {
            getAllContacts();
            getRecents();
        }
    }

    private void initListener() {
        binding.btnCreateContact.setOnClickListener(this);
        binding.btnSearch.setOnClickListener(this);
        binding.ivCloseSearch.setOnClickListener(this);
        binding.ivMoreAll.setOnClickListener(this);
        binding.ivMoreAdded.setOnClickListener(this);
        binding.ivMoreView.setOnClickListener(this);
        binding.btnAll.setOnClickListener(this);
        binding.btnRecentAdded.setOnClickListener(this);
        binding.btnRecentView.setOnClickListener(this);
        binding.ivClose.setOnClickListener(this);
        binding.ivShare.setOnClickListener(this);
        binding.ivDelete.setOnClickListener(this);
        binding.ivMore.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnCreateContact) {
            startActivity(new Intent(requireContext(), AddEditContactActivity.class).putExtra(Constants.TITLE, getString(R.string.add_contact)));
        } else if (id == R.id.btnSearch) {
            binding.clSearch.setVisibility(View.VISIBLE);
        } else if (id == R.id.ivCloseSearch) {
            binding.etSearch.setText("");
            binding.etSearch.clearFocus();
            hideKeyboard();
            binding.clSearch.setVisibility(View.GONE);
        } else if (id == R.id.ivMoreAll) {
            showPopupMenu(v, 1);
        } else if (id == R.id.ivMoreAdded) {
            showPopupMenu(v, 2);
        } else if (id == R.id.ivMoreView) {
            showPopupMenu(v, 3);
        } else if (id == R.id.btnAll) {
            binding.clAll.setVisibility(View.VISIBLE);
            binding.clAdded.setVisibility(View.GONE);
            binding.clView.setVisibility(View.GONE);
            binding.llMoreAll.setVisibility(View.VISIBLE);
            binding.llMoreAdded.setVisibility(View.GONE);
            binding.llMoreView.setVisibility(View.GONE);
            binding.btnAll.setBackgroundResource(R.drawable.bg_btn);
            binding.btnRecentAdded.setBackgroundResource(R.drawable.bg_main);
            binding.btnRecentView.setBackgroundResource(R.drawable.bg_main);
            binding.btnAll.setTextColor(requireContext().getColor(R.color.white));
            binding.btnRecentAdded.setTextColor(requireContext().getColor(R.color.grey_font));
            binding.btnRecentView.setTextColor(requireContext().getColor(R.color.grey_font));
        } else if (id == R.id.btnRecentAdded) {
            binding.clAll.setVisibility(View.GONE);
            binding.clAdded.setVisibility(View.VISIBLE);
            binding.clView.setVisibility(View.GONE);
                binding.llMoreAll.setVisibility(View.GONE);
            binding.llMoreAdded.setVisibility(View.VISIBLE);
            binding.llMoreView.setVisibility(View.GONE);
            binding.btnAll.setBackgroundResource(R.drawable.bg_main);
            binding.btnRecentAdded.setBackgroundResource(R.drawable.bg_btn);
            binding.btnRecentView.setBackgroundResource(R.drawable.bg_main);
            binding.btnAll.setTextColor(requireContext().getColor(R.color.grey_font));
            binding.btnRecentAdded.setTextColor(requireContext().getColor(R.color.white));
            binding.btnRecentView.setTextColor(requireContext().getColor(R.color.grey_font));
        } else if (id == R.id.btnRecentView) {
            binding.clAll.setVisibility(View.GONE);
            binding.clAdded.setVisibility(View.GONE);
            binding.clView.setVisibility(View.VISIBLE);
            binding.llMoreAll.setVisibility(View.GONE);
            binding.llMoreAdded.setVisibility(View.GONE);
            binding.llMoreView.setVisibility(View.VISIBLE);
            binding.btnAll.setBackgroundResource(R.drawable.bg_main);
            binding.btnRecentAdded.setBackgroundResource(R.drawable.bg_main);
            binding.btnRecentView.setBackgroundResource(R.drawable.bg_btn);
            binding.btnAll.setTextColor(requireContext().getColor(R.color.grey_font));
            binding.btnRecentAdded.setTextColor(requireContext().getColor(R.color.grey_font));
            binding.btnRecentView.setTextColor(requireContext().getColor(R.color.white));
        } else if (id == R.id.ivClose) {
            isSelectionMode = false;
            selectedContacts.clear();
            binding.clSelectionMode.setVisibility(View.GONE);
            binding.clHeader.setVisibility(View.VISIBLE);
            binding.hsv.setVisibility(View.VISIBLE);
            if (!binding.etSearch.getText().toString().isEmpty()){
                binding.clSearch.setVisibility(View.VISIBLE);
            }
            deselectAllContacts();
            adapter.notifyDataSetChanged();
        } else if (id == R.id.ivShare) {
            if (!selectedContacts.isEmpty()) {
                shareContacts();
            } else {
                Toast.makeText(requireContext(), getString(R.string.please_select_at_least_one_contact), Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.ivDelete) {
            if (!selectedContacts.isEmpty()) {
                showDeleteDialog();
            } else {
                Toast.makeText(requireContext(), getString(R.string.please_select_at_least_one_contact), Toast.LENGTH_SHORT).show();
            }

        } else if (id == R.id.ivMore) {
            showPopupMenu(v, 0);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            View currentFocus = requireActivity().getCurrentFocus();
            imm.hideSoftInputFromWindow(Objects.requireNonNullElseGet(currentFocus, () -> binding.etSearch).getWindowToken(), 0);
        }
    }

    private void getAllContacts() {
        contactGroups.clear();
        Map<String, ContactData> contactMap = new HashMap<>();

        ContentResolver contentResolver = requireContext().getContentResolver();
        Cursor cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
                        ContactsContract.CommonDataKinds.Phone.STARRED
                },
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        );

        if (cursor != null) {
            int idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
            int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            int photoIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI);
            int starredIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED);

            while (cursor.moveToNext()) {
                String id = cursor.getString(idIndex);
                if (contactMap.containsKey(id)) continue;

                String displayName = cursor.getString(nameIndex);
                String phone = cursor.getString(numberIndex);
                String photo = cursor.getString(photoIndex);
                boolean isFavourite = cursor.getInt(starredIndex) == 1;

                String[] split = displayName != null ? displayName.split(" ", 2) : new String[]{"", ""};
                String firstName = split[0];
                String lastName = (split.length > 1) ? split[1] : "";

                if (!TextUtils.isEmpty(firstName) && !TextUtils.isEmpty(phone)) {
                    contactMap.put(id, new ContactData(id, firstName, lastName, phone, photo, isFavourite, false));
                }
            }
            cursor.close();
        }

        Map<String, List<ContactData>> groupedMap = new TreeMap<>();
        for (ContactData contact : contactMap.values()) {
            String firstLetter = contact.getFormattedName(isFormatFirst).substring(0, 1).toUpperCase();
            groupedMap.computeIfAbsent(firstLetter, k -> new ArrayList<>()).add(contact);
        }

        for (Map.Entry<String, List<ContactData>> entry : groupedMap.entrySet()) {
            contactGroups.add(new ContactGroup(entry.getKey(), entry.getValue()));
        }

        adapter.setContactGroups(contactGroups);
        toggleEmptyState(contactGroups);

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.btnAll.performClick();
                filterContacts(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        sortContacts();
        filterContacts(binding.etSearch.getText().toString());
    }


    private void filterContacts(String query) {
        List<ContactGroup> contacts = new ArrayList<>();
        if (TextUtils.isEmpty(query)) {
            contacts.addAll(contactGroups);
        } else {
            for (ContactGroup group : contactGroups) {
                List<ContactData> filteredContacts = new ArrayList<>();

                for (ContactData contact : group.getContacts()) {
                    if (contact.getNameFL().toLowerCase().contains(query.toLowerCase())) {
                        filteredContacts.add(contact);
                    }
                }

                if (!filteredContacts.isEmpty()) {
                    contacts.add(new ContactGroup(group.getLetter(), filteredContacts));
                }
            }
        }
        adapter.setContactGroups(contacts);
        toggleEmptyState(contacts);
    }

    private void toggleEmptyState(List<ContactGroup> contactGroups) {
        if (contactGroups.isEmpty()) {
            binding.clNoContacts.setVisibility(View.VISIBLE);
            binding.ivMoreAll.setVisibility(View.GONE);
        } else {
            binding.clNoContacts.setVisibility(View.GONE);
            binding.ivMoreAll.setVisibility(View.VISIBLE);
        }
    }

    private void getRecents() {
        contactViewModel.getRecentAdded().observe(getViewLifecycleOwner(), new Observer<List<RecentAddedContact>>() {
            @Override
            public void onChanged(List<RecentAddedContact> recentAdded) {
                recentAddedContacts = recentAdded;
                recentAddedAdapter.setRecentAddedContacts(recentAddedContacts);

                if (recentAddedContacts.isEmpty()) {
                    binding.clNoDataAdded.setVisibility(View.VISIBLE);
                    binding.rvRecentAdded.setVisibility(View.GONE);
                    binding.ivMoreAdded.setVisibility(View.GONE);
                } else {
                    binding.clNoDataAdded.setVisibility(View.GONE);
                    binding.rvRecentAdded.setVisibility(View.VISIBLE);
                    binding.ivMoreAdded.setVisibility(View.VISIBLE);
                }
            }
        });

        contactViewModel.getRecentViewed().observe(getViewLifecycleOwner(), new Observer<List<RecentViewedContact>>() {
            @Override
            public void onChanged(List<RecentViewedContact> recentViewed) {
                recentViewedContacts = recentViewed;
                recentViewAdapter.setRecentViewedContacts(recentViewedContacts);

                if (recentViewedContacts.isEmpty()) {
                    binding.clNoDataView.setVisibility(View.VISIBLE);
                    binding.rvRecentView.setVisibility(View.GONE);
                    binding.ivMoreView.setVisibility(View.GONE);
                } else {
                    binding.clNoDataView.setVisibility(View.GONE);
                    binding.rvRecentView.setVisibility(View.VISIBLE);
                    binding.ivMoreView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void showPopupMenu(View view, int selectType) {
        PopupMenu popup = new PopupMenu(requireContext(), view);
        MenuInflater inflater = popup.getMenuInflater();
        if (selectType == 1) {
            inflater.inflate(R.menu.contacts_options_menu, popup.getMenu());
        } else if (selectType == 2) {
            inflater.inflate(R.menu.recent_added_options_menu, popup.getMenu());
        } else if (selectType == 3) {
            inflater.inflate(R.menu.recent_viewed_options_menu, popup.getMenu());
        } else if (selectType == 0) {
            inflater.inflate(R.menu.selection_mode_options_menu, popup.getMenu());
        }

        MenuItem sortItem = popup.getMenu().findItem(R.id.action_sort);
        if (sortItem != null) {
            if (isAscending) {
                sortItem.setTitle(getString(R.string.descending));
            } else {
                sortItem.setTitle(getString(R.string.ascending));
            }
        }

        boolean isAllSelected;
        List<ContactData> contacts = new ArrayList<>();
        for (ContactGroup group : contactGroups) {
            for (ContactData contact : group.getContacts()) {
                contacts.add(contact);
            }
        }
        MenuItem selectUnselectItem = popup.getMenu().findItem(R.id.action_select_unselect_all);
        if (selectUnselectItem != null) {
            if (contacts.size() == selectedContacts.size()) {
                isAllSelected = true;
                selectUnselectItem.setTitle(getString(R.string.deselect_all));
            } else {
                isAllSelected = false;
                selectUnselectItem.setTitle(getString(R.string.select_all));
            }
        } else {
            isAllSelected = false;
        }

        try {
            Field mPopup = PopupMenu.class.getDeclaredField("mPopup");
            mPopup.setAccessible(true);
            Object menuPopupHelper = mPopup.get(popup);
            Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
            Method setForceShowIcon = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
            setForceShowIcon.invoke(menuPopupHelper, true);

            Method getListView = classPopupHelper.getMethod("getListView");
            ListView listView = (ListView) getListView.invoke(menuPopupHelper);

            if (listView != null) {
                Drawable background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_popup);
                listView.setBackground(background);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Typeface typeface = ResourcesCompat.getFont(requireContext(), R.font.poppins_medium);
        Menu menu = popup.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            MenuItem menuItem = menu.getItem(i);
            SpannableString styledTitle = new SpannableString(menuItem.getTitle());

            styledTitle.setSpan(new ForegroundColorSpan(requireContext().getColor(R.color.primary_font)), 0, styledTitle.length(), 0);

            assert typeface != null;
            styledTitle.setSpan(new TypefaceSpan(typeface), 0, styledTitle.length(), 0);

            menuItem.setTitle(styledTitle);
        }

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_select) {
                isSelectionMode = true;
                binding.tvCounting.setText(String.valueOf(selectedContacts.size()));
                binding.clSelectionMode.setVisibility(View.VISIBLE);
                binding.clHeader.setVisibility(View.GONE);
                binding.clSearch.setVisibility(View.GONE);
                binding.hsv.setVisibility(View.GONE);
                return true;
            } else if (item.getItemId() == R.id.action_select_all) {
                isSelectionMode = true;
                binding.tvCounting.setText(String.valueOf(selectedContacts.size()));
                binding.clSelectionMode.setVisibility(View.VISIBLE);
                binding.clHeader.setVisibility(View.GONE);
                binding.clSearch.setVisibility(View.GONE);
                binding.hsv.setVisibility(View.GONE);
                selectAllContacts();
                if (selectedContacts.isEmpty()){
                    binding.ivClose.performClick();
                }
                return true;
            } else if (item.getItemId() == R.id.action_sort) {
                toggleSortOrder();
                return true;
            } else if (item.getItemId() == R.id.action_settings) {
                startActivity(new Intent(getContext(), SettingsActivity.class));
                return true;
            } else if (item.getItemId() == R.id.action_clear_added) {
                showClearDialog(getString(R.string.are_you_sure_want_to_clear_all_recently_added), 1);
                return true;
            } else if (item.getItemId() == R.id.action_clear_view) {
                showClearDialog(getString(R.string.are_you_sure_want_to_clear_all_recently_viewed), 2);
                return true;
            } else if (item.getItemId() == R.id.action_select_unselect_all) {
                if (isAllSelected) {
                    deselectAllContacts();
                } else {
                    selectAllContacts();
                }
                return true;
            } else {
                return false;
            }
        });
        popup.show();
    }

    private void toggleSortOrder() {
        isAscending = !isAscending;
        SharedPreferencesManager.getInstance().setBooleanValue(Constants.IS_ASCENDING, isAscending);
        sortContacts();
    }

    private void sortContacts() {
        if (contactGroups == null || contactGroups.isEmpty()) return;

        Collections.sort(contactGroups, (group1, group2) -> {
            if (isAscending) {
                return group1.getLetter().compareTo(group2.getLetter());
            } else {
                return group2.getLetter().compareTo(group1.getLetter());
            }
        });

        for (ContactGroup group : contactGroups) {
            Collections.sort(group.getContacts(), (contact1, contact2) -> {
                String name1 = sortByFirstName ? contact1.getFirstName() : contact1.getLastName();
                String name2 = sortByFirstName ? contact2.getFirstName() : contact2.getLastName();

                if (name1 == null) name1 = "";
                if (name2 == null) name2 = "";

                if (isAscending) {
                    return name1.compareToIgnoreCase(name2);
                } else {
                    return name2.compareToIgnoreCase(name1);
                }
            });
        }

        adapter.setContactGroups(contactGroups);
    }

    private void showClearDialog(String title, int clearType) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        dialog = new Dialog(requireContext());
        clearRoomBinding = DialogClearRoomBinding.inflate(getLayoutInflater());
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.setContentView(clearRoomBinding.getRoot());
        dialog.setCancelable(true);

        clearRoomBinding.tvTitle.setText(title);

        clearRoomBinding.btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        clearRoomBinding.btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (clearType == 1) {
                    contactViewModel.deleteRecentAdded();
                } else if (clearType == 2) {
                    contactViewModel.deleteRecentViewed();
                }
            }
        });

        dialog.show();
    }

    private void showDeleteDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        dialog = new Dialog(requireContext());
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
            deleteContacts();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void deleteContacts() {
        Context context = getContext();
        if (context == null) {
            return;
        }

        ContentResolver contentResolver = context.getContentResolver();
        BinContactViewModel viewModel = new ViewModelProvider(this).get(BinContactViewModel.class);

        Executors.newSingleThreadExecutor().execute(() -> {
            int delete = 0;
            List<ContactData> contactsToDelete;

            synchronized (selectedContacts) {
                contactsToDelete = new ArrayList<>(selectedContacts);
            }

            for (ContactData contact : contactsToDelete) {
                try {
                    String contactId = contact.getId();
                    if (contactId == null || contactId.trim().isEmpty()) {
                        continue;
                    }

                    String name = Utils.getContactNameById(context, contactId);
                    if (name == null || name.trim().isEmpty()) {
                        name = contact.getNameFL();
                        if (name == null || name.trim().isEmpty()) {
                            name = "Unknown ContactGroupItem";
                        }
                    }

                    List<PhoneItem> phoneList = Utils.getPhoneNumbersById(context, contactId);

                    long timestampDeleted = System.currentTimeMillis();
                    BinContactEntity binContact = new BinContactEntity(
                            name,
                            Utils.getContactPhotoUri(context, contactId),
                            phoneList,
                            timestampDeleted
                    );

                    ArrayList<ContentProviderOperation> operations = new ArrayList<>();

                    operations.add(ContentProviderOperation
                            .newDelete(ContactsContract.Data.CONTENT_URI)
                            .withSelection(ContactsContract.Data.CONTACT_ID + " = ?", new String[]{contactId})
                            .build());

                    operations.add(ContentProviderOperation
                            .newDelete(ContactsContract.RawContacts.CONTENT_URI)
                            .withSelection(ContactsContract.RawContacts.CONTACT_ID + " = ?", new String[]{contactId})
                            .build());

                    ContentProviderResult[] results = requireContext().getContentResolver().applyBatch(ContactsContract.AUTHORITY, operations);

                    int updatedRows = results.length;

                    if (updatedRows > 0) {
                        viewModel.insert(binContact);
                        delete++;
                    }

                    if (contactDao != null) {
                        try {
                            if (contactDao.getRecentAddedById(contactId) != null) {
                                contactDao.deleteRecentAddedById(contactId);
                            }
                            if (contactDao.getRecentViewedById(contactId) != null) {
                                contactDao.deleteRecentViewedById(contactId);
                            }
                        } catch (Exception daoException) {
                            daoException.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            int finalDelete = delete;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (isAdded()) {
                    if (finalDelete == contactsToDelete.size()) {
                        Toast.makeText(context, getString(R.string.contact_moved_to_bin), Toast.LENGTH_SHORT).show();
                        if (binding != null) {
                            binding.ivClose.performClick();
                        }
                    } else {
                        Toast.makeText(context, getString(R.string.failed_to_move_contact), Toast.LENGTH_SHORT).show();
                    }
                    getAllContacts();
                }
            });
        });
    }

    private void shareContacts() {
        try {
            File vcfFile = new File(requireContext().getCacheDir(), "contacts.vcf");
            FileWriter writer = new FileWriter(vcfFile);

            for (ContactData contact : selectedContacts) {
                writer.append("BEGIN:VCARD\n");
                writer.append("VERSION:3.0\n");
                writer.append("FN:").append(contact.getNameFL()).append("\n");
                List<PhoneItem> phoneItems = Utils.getPhoneNumbersById(requireContext(), contact.getId());
                for (PhoneItem phoneItem : phoneItems) {
                    writer.append("TEL;TYPE=").append(phoneItem.getPhoneType()).append(":")
                            .append(phoneItem.getPhoneNumber()).append("\n");
                }
                writer.append("END:VCARD\n");
            }

            writer.close();

            Uri uri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", vcfFile);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/x-vcard");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share Contacts"));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void selectAllContacts() {
        selectedContacts.clear();
        for (ContactGroup group : contactGroups) {
            for (ContactData contact : group.getContacts()) {
                contact.setSelected(true);
                selectedContacts.add(contact);
            }
        }
        adapter.notifyDataSetChanged();
        tvCounting.setText(String.valueOf(selectedContacts.size()));
    }

    private void deselectAllContacts() {
        for (ContactGroup group : contactGroups) {
            for (ContactData contact : group.getContacts()) {
                contact.setSelected(false);
            }
        }
        selectedContacts.clear();
        isSelectionMode = false;
        binding.clSelectionMode.setVisibility(View.GONE);
        binding.clHeader.setVisibility(View.VISIBLE);
        binding.hsv.setVisibility(View.VISIBLE);
        if (!binding.etSearch.getText().toString().isEmpty()){
            binding.clSearch.setVisibility(View.VISIBLE);
        }
        adapter.notifyDataSetChanged();
        tvCounting.setText(String.valueOf(selectedContacts.size()));
    }

    public static void changeText() {
        isSelectionMode = true;
        tvCounting.setText(String.valueOf(selectedContacts.size()));
        clSelectionMode.setVisibility(View.VISIBLE);
        clHeader.setVisibility(View.GONE);
        clSearch.setVisibility(View.GONE);
        hsv.setVisibility(View.GONE);
        if (selectedContacts.isEmpty()){
            ivClose.performClick();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            getAllContacts();
            getRecents();
            binding.ivClose.performClick();
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getAllContacts();
            getRecents();
        }
    }
}