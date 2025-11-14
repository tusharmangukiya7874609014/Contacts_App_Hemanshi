package com.contactshandlers.contactinfoall.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.contactshandlers.contactinfoall.databinding.ItemMergeContactBinding;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.model.Contact;

import java.util.List;

public class ContactMergeAdapter extends RecyclerView.Adapter<ContactMergeAdapter.ViewHolder> {

    private Context context;
    private List<Contact> contacts;

    public ContactMergeAdapter(Context context, List<Contact> contacts) {
        this.context = context;
        this.contacts = contacts;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMergeContactBinding binding = ItemMergeContactBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Contact contact = contacts.get(position);
        holder.binding.tvContactName.setText(contact.getName());
        holder.binding.tvContactNumber.setText(contact.getPhoneNumber());

        if (contact.getPhoto() != null) {
            Glide.with(context).load(contact.getPhoto()).into(holder.binding.ivProfile);
        } else {
            holder.binding.ivProfile.setImageBitmap(Utils.getInitialsBitmap(0, contact.getName(), Utils.colorsList()[position % Utils.colorsList().length]));
        }
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemMergeContactBinding binding;
        public ViewHolder(ItemMergeContactBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}