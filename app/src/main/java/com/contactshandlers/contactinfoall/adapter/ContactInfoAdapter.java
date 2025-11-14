package com.contactshandlers.contactinfoall.adapter;

import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.databinding.ItemContactInfoBinding;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.listeners.CallListener;
import com.contactshandlers.contactinfoall.model.PhoneItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ContactInfoAdapter extends RecyclerView.Adapter<ContactInfoAdapter.PhoneViewHolder> {
    private final List<PhoneItem> phoneList;
    private final Context context;
    private CallListener callListener;

    public ContactInfoAdapter(Context context, List<PhoneItem> phoneList, CallListener callListener) {
        this.context = context;
        this.phoneList = phoneList;
        this.callListener = callListener;
    }

    @NonNull
    @Override
    public PhoneViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContactInfoBinding binding = ItemContactInfoBinding.inflate(LayoutInflater.from(context), parent, false);
        return new PhoneViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PhoneViewHolder holder, int position) {
        PhoneItem phoneItem = phoneList.get(position);

        holder.binding.tvNumber.setText(phoneItem.getPhoneNumber());
        holder.binding.tvType.setText(phoneItem.getPhoneType());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callListener.onCall(phoneItem.getPhoneNumber());
            }
        });
    }

    @Override
    public int getItemCount() {
        return phoneList.size();
    }

    public static class PhoneViewHolder extends RecyclerView.ViewHolder {
        private final ItemContactInfoBinding binding;
        public PhoneViewHolder(ItemContactInfoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
