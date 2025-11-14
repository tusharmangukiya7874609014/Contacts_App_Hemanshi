package com.contactshandlers.contactinfoall.helper;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.databinding.DialogCreateGroupBinding;
import com.contactshandlers.contactinfoall.databinding.DialogSelectAccountBinding;
import com.contactshandlers.contactinfoall.model.Account;

import java.util.List;

public class GroupCreationHelper {

    public interface OnGroupCreatedListener {
        void onGroupCreated(String groupName, Account selectedAccount);

        void onGroupCreationCancelled();
    }

    private Context context;
    private ContactsGroupsManager contactsManager;
    private OnGroupCreatedListener listener;
    private DialogCreateGroupBinding groupBinding;
    private DialogSelectAccountBinding accountBinding;

    public GroupCreationHelper(Context context, ContactsGroupsManager contactsManager) {
        this.context = context;
        this.contactsManager = contactsManager;
    }

    public void setOnGroupCreatedListener(OnGroupCreatedListener listener) {
        this.listener = listener;
    }

    public void showCreateGroupDialog() {
        showGroupNameDialog();
    }

    private void showGroupNameDialog() {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        groupBinding = DialogCreateGroupBinding.inflate(LayoutInflater.from(context));
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setContentView(groupBinding.getRoot());
        dialog.setCancelable(true);

        groupBinding.btnClose.setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) {
                listener.onGroupCreationCancelled();
            }
        });

        groupBinding.btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) {
                listener.onGroupCreationCancelled();
            }
        });

        groupBinding.btnOk.setOnClickListener(v -> {
            String groupName = groupBinding.etGroupName.getText().toString().trim();

            if (groupName.isEmpty()) {
                Toast.makeText(context, context.getString(R.string.please_enter_a_group_name), Toast.LENGTH_SHORT).show();
                return;
            }

            dialog.dismiss();
            showAccountSelectionDialog(groupName);
        });

        dialog.show();
    }

    private void showAccountSelectionDialog(String groupName) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        accountBinding = DialogSelectAccountBinding.inflate(LayoutInflater.from(context));
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setContentView(accountBinding.getRoot());
        dialog.setCancelable(true);

        List<Account> accounts = contactsManager.getAvailableAccounts();

        if (accounts.isEmpty()) {
            Toast.makeText(context, context.getString(R.string.no_accounts_available_for_group_creation), Toast.LENGTH_LONG).show();
            dialog.dismiss();
            return;
        }

        accountBinding.rgAccounts.removeAllViews();

        for (int i = 0; i < accounts.size(); i++) {
            Account account = accounts.get(i);
            RadioButton radioButton = new RadioButton(context);
            radioButton.setText(account.getDisplayName());
            radioButton.setTag(account);
            radioButton.setPadding(16, 12, 16, 12);
            radioButton.setTextSize(15);

            radioButton.setChecked(false);
            setRadioButtonStyle(radioButton, false);

            accountBinding.rgAccounts.addView(radioButton);
        }

        accountBinding.rgAccounts.clearCheck();

        accountBinding.rgAccounts.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                for (int i = 0; i < group.getChildCount(); i++) {
                    RadioButton rb = (RadioButton) group.getChildAt(i);
                    setRadioButtonStyle(rb, rb.getId() == checkedId);
                }
            }
        });

        accountBinding.btnClose.setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) {
                listener.onGroupCreationCancelled();
            }
        });

        accountBinding.btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            if (listener != null) {
                listener.onGroupCreationCancelled();
            }
        });

        accountBinding.btnOk.setOnClickListener(v -> {
            int selectedId = accountBinding.rgAccounts.getCheckedRadioButtonId();

            if (selectedId == -1) {
                Toast.makeText(context, context.getString(R.string.please_select_an_account), Toast.LENGTH_SHORT).show();
                return;
            }

            RadioButton selectedRadioButton = accountBinding.getRoot().findViewById(selectedId);
            Account selectedAccount = (Account) selectedRadioButton.getTag();

            if (selectedAccount == null) {
                Toast.makeText(context, context.getString(R.string.please_select_an_account), Toast.LENGTH_SHORT).show();
                return;
            }

            dialog.dismiss();

            createGroup(groupName, selectedAccount);
        });

        dialog.show();
    }

    private void setRadioButtonStyle(RadioButton radioButton, boolean isChecked) {
        if (isChecked) {
            radioButton.setButtonTintList(ColorStateList.valueOf(context.getColor(R.color.main2)));
            radioButton.setTextColor(ColorStateList.valueOf(context.getColor(R.color.main2)));
        } else {
            radioButton.setButtonTintList(ColorStateList.valueOf(context.getColor(R.color.grey_font)));
            radioButton.setTextColor(ColorStateList.valueOf(context.getColor(R.color.primary_font)));
        }
    }

    private void createGroup(String groupName, Account selectedAccount) {
        try {
            boolean success = contactsManager.createContactGroup(groupName, selectedAccount);
            if (success) {
                if (listener != null) {
                    listener.onGroupCreated(groupName, selectedAccount);
                }
            } else {
                Toast.makeText(context, context.getString(R.string.failed_to_create_group_please_try_again), Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, context.getString(R.string.failed_to_create_group_please_try_again), Toast.LENGTH_LONG).show();
        }
    }
}