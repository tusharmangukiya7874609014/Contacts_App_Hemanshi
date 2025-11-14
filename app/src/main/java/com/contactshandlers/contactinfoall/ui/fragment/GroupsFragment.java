package com.contactshandlers.contactinfoall.ui.fragment;

import static android.content.Context.INPUT_METHOD_SERVICE;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.adapter.ContactGroupsAdapter;
import com.contactshandlers.contactinfoall.ads.InterstitialAD;
import com.contactshandlers.contactinfoall.databinding.DialogDeleteGroupBinding;
import com.contactshandlers.contactinfoall.databinding.DialogEditGroupBinding;
import com.contactshandlers.contactinfoall.databinding.FragmentGroupsBinding;
import com.contactshandlers.contactinfoall.listeners.AdCallback;
import com.contactshandlers.contactinfoall.model.Account;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.model.ContactsGroups;
import com.contactshandlers.contactinfoall.helper.ContactsGroupsManager;
import com.contactshandlers.contactinfoall.helper.GroupCreationHelper;
import com.contactshandlers.contactinfoall.ui.activity.GroupDetailActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GroupsFragment extends Fragment implements
        View.OnClickListener,
        ContactGroupsAdapter.OnGroupClickListener,
        ContactGroupsAdapter.OnSelectionModeListener,
        GroupCreationHelper.OnGroupCreatedListener {

    private FragmentGroupsBinding binding;
    private static final int PERMISSION_REQUEST_READ_CONTACTS = 1;
    private static final int PERMISSION_REQUEST_WRITE_CONTACTS = 2;
    private ContactsGroupsManager contactsManager;
    private ContactGroupsAdapter groupsAdapter;
    private List<ContactsGroups> allGroups = new ArrayList<>();
    private List<ContactsGroups> filteredGroups = new ArrayList<>();
    private GroupCreationHelper groupCreationHelper;
    private Dialog dialog;
    private DialogEditGroupBinding editGroupBinding;
    private DialogDeleteGroupBinding deleteGroupBinding;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentGroupsBinding.inflate(inflater, container, false);

        init();
        initListener();

        return binding.getRoot();
    }

    private void init() {
        executorService = Executors.newFixedThreadPool(3);
        mainHandler = new Handler(Looper.getMainLooper());

        setupRecyclerView();
        setupSearchFunctionality();

        contactsManager = new ContactsGroupsManager(requireContext());

        checkPermissionsAndLoadData();

        groupCreationHelper = new GroupCreationHelper(requireContext(), contactsManager);
        groupCreationHelper.setOnGroupCreatedListener(this);
    }

    private void initListener() {
        binding.btnCreateGroup.setOnClickListener(this);
        binding.btnSearch.setOnClickListener(this);
        binding.ivCloseSearch.setOnClickListener(this);
        binding.btnEdit.setOnClickListener(this);
        binding.btnSelectAll.setOnClickListener(this);
        binding.btnDelete.setOnClickListener(this);
        binding.ivClose.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnCreateGroup) {
            showCreateGroupDialog();
        } else if (id == R.id.btnSearch) {
            binding.clSearch.setVisibility(View.VISIBLE);
        } else if (id == R.id.ivCloseSearch) {
            binding.etSearch.setText("");
            binding.etSearch.clearFocus();
            hideKeyboard();
            binding.clSearch.setVisibility(View.GONE);
        } else if (id == R.id.btnEdit) {
            handleEditSelectedGroups();
        } else if (id == R.id.btnSelectAll) {
            handleSelectAll();
        } else if (id == R.id.btnDelete) {
            handleDeleteSelectedGroups();
        } else if (id == R.id.ivClose) {
            groupsAdapter.clearSelection();
            if (!binding.etSearch.getText().toString().isEmpty()){
                binding.clSearch.setVisibility(View.VISIBLE);
            }
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            View currentFocus = requireActivity().getCurrentFocus();
            imm.hideSoftInputFromWindow(Objects.requireNonNullElseGet(currentFocus, () -> binding.etSearch).getWindowToken(), 0);
        }
    }

    private void setupRecyclerView() {
        groupsAdapter = new ContactGroupsAdapter(requireContext(), filteredGroups);
        groupsAdapter.setOnGroupClickListener(this);
        groupsAdapter.setOnSelectionModeListener(this);

        binding.rvGroups.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvGroups.setAdapter(groupsAdapter);
    }

    private void setupSearchFunctionality() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterGroups(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void checkPermissionsAndLoadData() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.READ_CONTACTS},
                    PERMISSION_REQUEST_READ_CONTACTS);
        } else {
            loadContactGroups();
        }
    }

    private void showCreateGroupDialog() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.WRITE_CONTACTS},
                    PERMISSION_REQUEST_WRITE_CONTACTS);
            return;
        }

        groupCreationHelper.showCreateGroupDialog();
    }

    @Override
    public void onSelectionModeStarted() {
        showSelectionMode();
        updateSelectionModeUI();
    }

    @Override
    public void onSelectionModeEnded() {
        hideSelectionMode();
    }

    @Override
    public void onSelectionChanged(int selectedCount) {
        updateSelectionModeUI();
    }

    private void showSelectionMode() {
        binding.clSelectionMode.setVisibility(View.VISIBLE);
        binding.clHeader.setVisibility(View.GONE);
        binding.btnCreateGroup.setVisibility(View.GONE);
        binding.btnSearch.setVisibility(View.GONE);
    }

    private void hideSelectionMode() {
        binding.clSelectionMode.setVisibility(View.GONE);
        binding.clHeader.setVisibility(View.VISIBLE);
        binding.btnCreateGroup.setVisibility(View.VISIBLE);
        binding.btnSearch.setVisibility(View.VISIBLE);
    }

    private void updateSelectionModeUI() {
        if (groupsAdapter != null) {
            int count = groupsAdapter.getSelectedCount();
            binding.tvCounting.setText(String.valueOf(count));
            if (count == 1) {
                binding.btnEdit.setVisibility(View.VISIBLE);
            } else {
                binding.btnEdit.setVisibility(View.GONE);
            }
        }
    }

    private void handleEditSelectedGroups() {
        List<ContactsGroups> selectedGroups = groupsAdapter.getSelectedGroups();

        if (selectedGroups.size() == 1) {
            ContactsGroups group = selectedGroups.get(0);
            showEditGroupDialog(group);
        } else if (selectedGroups.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.no_groups_selected), Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSelectAll() {
        if (groupsAdapter != null) {
            groupsAdapter.selectAll();
        }
    }

    private void handleDeleteSelectedGroups() {
        List<ContactsGroups> selectedGroups = groupsAdapter.getSelectedGroups();

        if (selectedGroups.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.no_groups_selected), Toast.LENGTH_SHORT).show();
            return;
        }

        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        dialog = new Dialog(requireContext());
        deleteGroupBinding = DialogDeleteGroupBinding.inflate(getLayoutInflater());
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.setContentView(deleteGroupBinding.getRoot());
        dialog.setCancelable(true);

        String message = selectedGroups.size() == 1
                ? getString(R.string.are_you_sure_want_to_delete) + " \"" + selectedGroups.get(0).getTitle() + "\"?"
                : getString(R.string.are_you_sure_want_to_delete) + " " + selectedGroups.size() + " " + getString(R.string.groups) + "?";
        deleteGroupBinding.tv2.setText(message);

        deleteGroupBinding.btnClose.setOnClickListener(v -> dialog.dismiss());
        deleteGroupBinding.btnCancel.setOnClickListener(v -> dialog.dismiss());
        deleteGroupBinding.btnDelete.setOnClickListener(v -> {
            deleteSelectedGroups(selectedGroups);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showEditGroupDialog(ContactsGroups group) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        dialog = new Dialog(requireContext());
        editGroupBinding = DialogEditGroupBinding.inflate(getLayoutInflater());
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.setContentView(editGroupBinding.getRoot());
        dialog.setCancelable(true);

        editGroupBinding.etGroupName.setText(group.getTitle());
        editGroupBinding.etGroupName.setSelectAllOnFocus(true);

        editGroupBinding.btnClose.setOnClickListener(v -> dialog.dismiss());
        editGroupBinding.btnCancel.setOnClickListener(v -> {
            groupsAdapter.endSelectionMode();
            dialog.dismiss();
        });
        editGroupBinding.btnOk.setOnClickListener(v -> {
            String newName = editGroupBinding.etGroupName.getText().toString().trim();
            if (!newName.isEmpty() && !newName.equals(group.getTitle())) {
                updateGroupName(group, newName);
            }
            groupsAdapter.endSelectionMode();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateGroupName(ContactsGroups group, String newName) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.WRITE_CONTACTS},
                    PERMISSION_REQUEST_WRITE_CONTACTS);
            return;
        }

        updateGroupNameAsync(group, newName);
    }

    private void deleteSelectedGroups(List<ContactsGroups> selectedGroups) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.WRITE_CONTACTS},
                    PERMISSION_REQUEST_WRITE_CONTACTS);
            return;
        }

        deleteGroupsAsync(selectedGroups);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_WRITE_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                groupCreationHelper.showCreateGroupDialog();
            }
        } else if (requestCode == PERMISSION_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadContactGroups();
            }
        }
    }

    @Override
    public void onGroupCreated(String groupName, Account selectedAccount) {
        loadContactGroups();
    }

    @Override
    public void onGroupCreationCancelled() {
    }

    private void loadContactGroups() {
        mainHandler.post(this::showProgress);

        CompletableFuture.supplyAsync(() -> {
            try {
                return contactsManager.getAllContactGroups();
            } catch (Exception e) {
                return new ArrayList<ContactsGroups>();
            }
        }, executorService).thenAcceptAsync(groups -> {
            hideProgress();

            allGroups.clear();
            allGroups.addAll(groups);

            filteredGroups.clear();
            filteredGroups.addAll(groups);

            groupsAdapter.notifyDataSetChanged();
            updateUIState();
        }, mainHandler::post);
    }

    private void updateGroupNameAsync(ContactsGroups group, String newName) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return contactsManager.updateGroupName(group.getId(), newName);
            } catch (Exception e) {
                return false;
            }
        }, executorService).thenAcceptAsync(success -> {
            if (success) {
                loadContactGroups();
            } else {
                Toast.makeText(requireContext(), getString(R.string.failed_to_rename_group), Toast.LENGTH_LONG).show();
            }
        }, mainHandler::post);
    }

    private void deleteGroupsAsync(List<ContactsGroups> selectedGroups) {
        CompletableFuture.supplyAsync(() -> {
            int deletedCount = 0;
            try {
                for (ContactsGroups group : selectedGroups) {
                    if (contactsManager.deleteGroup(group.getId())) {
                        deletedCount++;
                    }
                }
                return deletedCount;
            } catch (Exception e) {
                return 0;
            }
        }, executorService).thenAcceptAsync(deletedCount -> {
            if (groupsAdapter != null) {
                groupsAdapter.endSelectionMode();
            }

            if (deletedCount > 0) {
                Toast.makeText(requireContext(), getString(R.string.group_deleted_successfully), Toast.LENGTH_SHORT).show();
                loadContactGroups();
            } else {
                Toast.makeText(requireContext(), getString(R.string.failed_to_delete_groups), Toast.LENGTH_LONG).show();
            }
        }, mainHandler::post);
    }

    private void filterGroups(String query) {
        filteredGroups.clear();

        if (query.isEmpty()) {
            filteredGroups.addAll(allGroups);
        } else {
            for (ContactsGroups group : allGroups) {
                if (group.getTitle() != null &&
                        group.getTitle().toLowerCase().contains(query.toLowerCase())) {
                    filteredGroups.add(group);
                }
            }
        }

        groupsAdapter.notifyDataSetChanged();
        updateUIState();
    }

    private void updateUIState() {
        if (filteredGroups.isEmpty()) {
            showEmptyState();
        } else {
            hideEmptyState();
        }
    }

    private void showProgress() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.rvGroups.setVisibility(View.GONE);
        binding.clEmptyGroup.setVisibility(View.GONE);
    }

    private void hideProgress() {
        binding.progressBar.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        binding.clEmptyGroup.setVisibility(View.VISIBLE);
        binding.rvGroups.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        binding.clEmptyGroup.setVisibility(View.GONE);
        binding.rvGroups.setVisibility(View.VISIBLE);
    }

    @Override
    public void onGroupClick(ContactsGroups group, int position) {
        InterstitialAD.getInstance().showInterstitial(requireActivity(), new AdCallback() {
            @Override
            public void callbackCall() {
                Intent intent = new Intent(requireContext(), GroupDetailActivity.class);
                intent.putExtra(Constants.GROUP_ID, group.getId());
                intent.putExtra(Constants.GROUP_TITLE, group.getTitle());
                startActivity(intent);
            }
        });
    }

    public boolean onBackPressed() {
        if (groupsAdapter != null && groupsAdapter.isSelectionMode()) {
            groupsAdapter.endSelectionMode();
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {
            loadContactGroups();
        }
        binding.etSearch.setText("");
        binding.clSearch.setVisibility(View.GONE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}