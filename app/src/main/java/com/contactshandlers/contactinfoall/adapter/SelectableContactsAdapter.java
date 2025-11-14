package com.contactshandlers.contactinfoall.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.databinding.ItemSelectableContactBinding;
import com.contactshandlers.contactinfoall.model.ContactGroupItem;
import com.contactshandlers.contactinfoall.helper.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SelectableContactsAdapter extends RecyclerView.Adapter<SelectableContactsAdapter.ViewHolder> {

    private List<ContactGroupItem> contacts;
    private Context context;
    private Set<String> selectedContactIds;
    private Set<String> existingContactIds;
    private Set<String> removedContactIds;

    public SelectableContactsAdapter(Context context, List<ContactGroupItem> contacts, Set<String> existingContactIds) {
        this.context = context;
        this.contacts = contacts;
        this.existingContactIds = existingContactIds != null ? existingContactIds : new HashSet<>();
        this.selectedContactIds = new HashSet<>();
        this.removedContactIds = new HashSet<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSelectableContactBinding binding = ItemSelectableContactBinding.inflate(LayoutInflater.from(context), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ContactGroupItem contact = contacts.get(position);

        holder.binding.tvName.setText(contact.getDisplayName());

        if (contact.getPhoto() != null) {
            Glide.with(context).load(contact.getPhoto()).into(holder.binding.ivProfile);
        } else {
            holder.binding.ivProfile.setImageBitmap(Utils.getInitialsBitmap(0, contact.getDisplayName(), Utils.colorsList()[position % Utils.colorsList().length]));
        }

        boolean alreadyInGroup = existingContactIds.contains(contact.getId());
        boolean isCurrentlySelected = selectedContactIds.contains(contact.getId());
        boolean isMarkedForRemoval = removedContactIds.contains(contact.getId());

        if (alreadyInGroup && isMarkedForRemoval) {
            holder.binding.ivCheck.setImageResource(R.drawable.ic_uncheck_gre);
        } else if (alreadyInGroup || isCurrentlySelected) {
            holder.binding.ivCheck.setImageResource(R.drawable.ic_check);
        } else {
            holder.binding.ivCheck.setImageResource(R.drawable.ic_uncheck_gre);
        }

        setupClickListeners(holder, contact, alreadyInGroup);
    }

    private void setupClickListeners(ViewHolder holder, ContactGroupItem contact, boolean alreadyInGroup) {
        View.OnClickListener toggleSelection = v -> {
            boolean wasSelected = selectedContactIds.contains(contact.getId());
            boolean isMarkedForRemoval = removedContactIds.contains(contact.getId());

            if (alreadyInGroup) {
                if (isMarkedForRemoval) {
                    removedContactIds.remove(contact.getId());
                    holder.binding.ivCheck.setImageResource(R.drawable.ic_check);
                } else {
                    removedContactIds.add(contact.getId());
                    holder.binding.ivCheck.setImageResource(R.drawable.ic_uncheck_gre);
                }
            } else {
                if (wasSelected) {
                    selectedContactIds.remove(contact.getId());
                    holder.binding.ivCheck.setImageResource(R.drawable.ic_uncheck_gre);
                } else {
                    selectedContactIds.add(contact.getId());
                    holder.binding.ivCheck.setImageResource(R.drawable.ic_check);
                }
            }
        };

        holder.itemView.setOnClickListener(toggleSelection);
        holder.binding.ivCheck.setOnClickListener(toggleSelection);
    }

    @Override
    public int getItemCount() {
        return contacts != null ? contacts.size() : 0;
    }

    public List<ContactGroupItem> getSelectedContacts() {
        List<ContactGroupItem> selectedContacts = new ArrayList<>();
        for (ContactGroupItem contact : contacts) {
            if (selectedContactIds.contains(contact.getId())) {
                selectedContacts.add(contact);
            }
        }
        return selectedContacts;
    }

    public List<ContactGroupItem> getRemovedContacts() {
        List<ContactGroupItem> removedContacts = new ArrayList<>();
        for (ContactGroupItem contact : contacts) {
            if (removedContactIds.contains(contact.getId())) {
                removedContacts.add(contact);
            }
        }
        return removedContacts;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemSelectableContactBinding binding;

        public ViewHolder(ItemSelectableContactBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}