package com.contactshandlers.contactinfoall.adapter;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.databinding.ItemContactGroupBinding;
import com.contactshandlers.contactinfoall.model.ContactGroup;

import java.util.ArrayList;
import java.util.List;

public class ContactGroupAdapter extends RecyclerView.Adapter<ContactGroupAdapter.ViewHolder> {

    private List<ContactGroup> contactGroups;
    private Context context;

    public ContactGroupAdapter(List<ContactGroup> contactGroups, Context context) {
        this.contactGroups = contactGroups;
        this.context = context;
    }

    public void setContactGroups(List<ContactGroup> contactGroups) {
        this.contactGroups = contactGroups;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContactGroupBinding binding = ItemContactGroupBinding.inflate(LayoutInflater.from(context), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ContactGroup group = contactGroups.get(position);
        holder.binding.tvLetter.setText(group.getLetter());
        holder.binding.rvContacts.setLayoutManager(new LinearLayoutManager(context));
        ContactsAdapter contactAdapter = new ContactsAdapter(group.getContacts(), context);
        holder.binding.rvContacts.setAdapter(contactAdapter);
    }

    @Override
    public int getItemCount() {
        return contactGroups.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemContactGroupBinding binding;

        public ViewHolder(ItemContactGroupBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}