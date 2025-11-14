package com.contactshandlers.contactinfoall.adapter;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.databinding.DialogRenameFieldBinding;
import com.contactshandlers.contactinfoall.databinding.ItemPhoneBinding;
import com.contactshandlers.contactinfoall.listeners.RemoveListener;
import com.contactshandlers.contactinfoall.model.PhoneItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PhoneAdapter extends RecyclerView.Adapter<PhoneAdapter.ViewHolder> {
    private List<PhoneItem> phoneList;
    private final Context context;
    private Dialog dialog;
    private DialogRenameFieldBinding renameFieldBinding;
    private RemoveListener listener;

    public PhoneAdapter(Context context, List<PhoneItem> phoneList, RemoveListener listener) {
        this.context = context;
        this.phoneList = phoneList;
        this.listener = listener;
    }

    public void setPhoneList(List<PhoneItem> phoneList) {
        this.phoneList = phoneList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPhoneBinding binding = ItemPhoneBinding.inflate(LayoutInflater.from(context), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PhoneItem phoneItem = phoneList.get(position);

        List<String> phoneTypes = new ArrayList<>(Arrays.asList(context.getResources().getStringArray(R.array.phone_types)));
        SpinnerPhoneAdapter adapter = new SpinnerPhoneAdapter(context, phoneTypes);
        holder.binding.spPhoneType.setAdapter(adapter);

        int spinnerPosition = adapter.getPosition(phoneItem.getPhoneType());
        if (spinnerPosition != -1) {
            adapter.setSelectedPosition(spinnerPosition);
            holder.binding.spPhoneType.setSelection(spinnerPosition);
        } else {
            phoneTypes.remove(phoneTypes.size() - 1);
            phoneTypes.add(phoneItem.getPhoneType());
            adapter.notifyDataSetChanged();
            int newPosition = phoneTypes.size() - 1;
            adapter.setSelectedPosition(newPosition);
            holder.binding.spPhoneType.setSelection(newPosition);
        }

        holder.binding.spPhoneType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                adapter.setSelectedPosition(pos);
                if (holder.isUserInteraction[0] && pos == phoneTypes.size() - 1) {
                    showCustomTypeDialog(holder.binding.spPhoneType, phoneItem, adapter, phoneTypes);
                } else {
                    phoneItem.setPhoneType(phoneTypes.get(pos));
                }
                holder.isUserInteraction[0] = false;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        holder.binding.spPhoneType.setOnTouchListener((v, event) -> {
            holder.isUserInteraction[0] = true;
            return false;
        });

        holder.binding.etPhone.removeTextChangedListener((TextWatcher) holder.binding.etPhone.getTag());
        holder.binding.etPhone.setText(phoneItem.getPhoneNumber());
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                phoneItem.setPhoneNumber(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        holder.binding.etPhone.addTextChangedListener(textWatcher);
        holder.binding.etPhone.setTag(textWatcher);

        holder.binding.ivRemove.setOnClickListener(v -> {
            listener.onClick(position);
        });
    }

    @Override
    public int getItemCount() {
        return phoneList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemPhoneBinding binding;
        final boolean[] isUserInteraction = {false};

        public ViewHolder(ItemPhoneBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private void showCustomTypeDialog(Spinner spinner, PhoneItem phoneItem, ArrayAdapter<String> adapter, List<String> phoneTypes) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        dialog = new Dialog(context);
        renameFieldBinding = DialogRenameFieldBinding.inflate(((Activity) context).getLayoutInflater());
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setContentView(renameFieldBinding.getRoot());
        dialog.setCancelable(true);

        renameFieldBinding.btnCancel.setOnClickListener(v -> dialog.dismiss());
        renameFieldBinding.btnRename.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String customType = renameFieldBinding.etField.getText().toString().trim();
                if (!customType.isEmpty()) {
                    int customIndex = phoneTypes.size() - 1;
                    phoneTypes.set(customIndex, customType);
                    adapter.notifyDataSetChanged();
                    spinner.setSelection(customIndex);
                    phoneItem.setPhoneType(customType);
                }
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}
