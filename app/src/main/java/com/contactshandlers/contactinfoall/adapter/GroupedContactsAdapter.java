package com.contactshandlers.contactinfoall.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.contactshandlers.contactinfoall.databinding.ItemContactGroupBinding;
import com.contactshandlers.contactinfoall.model.ContactGroupItem;
import com.contactshandlers.contactinfoall.model.ContactGroupData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class GroupedContactsAdapter extends RecyclerView.Adapter<GroupedContactsAdapter.ViewHolder> {

    private Context context;
    private List<ContactGroupData> groupedContacts;
    private List<ContactGroupItem> originalContacts;

    public GroupedContactsAdapter(Context context, List<ContactGroupItem> contacts) {
        this.context = context;
        this.originalContacts = contacts;
        this.groupedContacts = new ArrayList<>();
        processContactsIntoGroups();
    }

    private void processContactsIntoGroups() {
        groupedContacts.clear();

        if (originalContacts == null || originalContacts.isEmpty()) {
            return;
        }

        Map<String, List<ContactGroupItem>> contactGroups = new TreeMap<>();

        for (ContactGroupItem contact : originalContacts) {
            String name = contact.getDisplayName();
            String firstLetter = "";

            if (name != null && !name.trim().isEmpty()) {
                firstLetter = name.trim().substring(0, 1).toUpperCase();
            } else {
                continue;
            }

            contactGroups.computeIfAbsent(firstLetter, k -> new ArrayList<>()).add(contact);
        }

        for (Map.Entry<String, List<ContactGroupItem>> entry : contactGroups.entrySet()) {
            List<ContactGroupItem> contactsInGroup = entry.getValue();

            contactsInGroup.sort((c1, c2) -> {
                String name1 = c1.getDisplayName() != null ? c1.getDisplayName() : "";
                String name2 = c2.getDisplayName() != null ? c2.getDisplayName() : "";
                return name1.compareToIgnoreCase(name2);
            });

            groupedContacts.add(new ContactGroupData(entry.getKey(), contactsInGroup));
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContactGroupBinding binding = ItemContactGroupBinding.inflate(LayoutInflater.from(context), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ContactGroupData groupData = groupedContacts.get(position);
        holder.binding.tvLetter.setText(groupData.getGroupLetter());
        SimpleContactsAdapter contactsAdapter = new SimpleContactsAdapter(context, groupData.getContacts());
        holder.binding.rvContacts.setLayoutManager(new LinearLayoutManager(context));
        holder.binding.rvContacts.setAdapter(contactsAdapter);
    }

    @Override
    public int getItemCount() {
        return groupedContacts != null ? groupedContacts.size() : 0;
    }

    public void updateContacts(List<ContactGroupItem> newContacts) {
        this.originalContacts = newContacts;
        processContactsIntoGroups();
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemContactGroupBinding binding;

        public ViewHolder(ItemContactGroupBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}