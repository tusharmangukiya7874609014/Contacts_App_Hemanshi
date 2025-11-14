package com.contactshandlers.contactinfoall.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.databinding.ItemGroupBinding;
import com.contactshandlers.contactinfoall.model.ContactsGroups;
import com.contactshandlers.contactinfoall.helper.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ContactGroupsAdapter extends RecyclerView.Adapter<ContactGroupsAdapter.ViewHolder> {

    private List<ContactsGroups> contactGroups;
    private Context context;
    private OnGroupClickListener onGroupClickListener;
    private boolean selectionMode = false;
    private Set<Integer> selectedItems = new HashSet<>();
    private OnSelectionModeListener selectionModeListener;

    public interface OnGroupClickListener {
        void onGroupClick(ContactsGroups group, int position);
    }

    public interface OnSelectionModeListener {
        void onSelectionModeStarted();

        void onSelectionModeEnded();

        void onSelectionChanged(int selectedCount);
    }

    public ContactGroupsAdapter(Context context, List<ContactsGroups> contactGroups) {
        this.context = context;
        this.contactGroups = contactGroups;
    }

    public void setOnGroupClickListener(OnGroupClickListener listener) {
        this.onGroupClickListener = listener;
    }

    public void setOnSelectionModeListener(OnSelectionModeListener listener) {
        this.selectionModeListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemGroupBinding binding = ItemGroupBinding.inflate(LayoutInflater.from(context), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ContactsGroups group = contactGroups.get(position);

        if (group.getTitle() != null && !group.getTitle().isEmpty()) {
            holder.binding.tvGroupName.setText(group.getTitle() + "(" + group.getContactCount() + ")");
        } else {
            holder.binding.tvGroupName.setText("Unnamed Group(" + group.getContactCount() + ")");
        }

        holder.binding.ivGroup.setImageBitmap(Utils.getInitialsBitmap(context, R.drawable.ic_group_tab,
                Utils.colorsList()[position % Utils.colorsList().length]));

        boolean isSelected = selectedItems.contains(position);
        updateItemSelection(holder, isSelected);

        holder.itemView.setOnClickListener(v -> {
            if (selectionMode) {
                toggleSelection(position);
            } else {
                if (onGroupClickListener != null) {
                    onGroupClickListener.onGroupClick(group, position);
                }
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!selectionMode) {
                startSelectionMode();
                toggleSelection(position);
            }
            return false;
        });
    }

    private void updateItemSelection(ViewHolder holder, boolean isSelected) {
        if (isSelected) {
            holder.itemView.setBackgroundColor(context.getColor(R.color.bg_recent_option));
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    public void startSelectionMode() {
        if (!selectionMode) {
            selectionMode = true;
            selectedItems.clear();
            if (selectionModeListener != null) {
                selectionModeListener.onSelectionModeStarted();
            }
            notifyDataSetChanged();
        }
    }

    public void endSelectionMode() {
        if (selectionMode) {
            selectionMode = false;
            selectedItems.clear();
            if (selectionModeListener != null) {
                selectionModeListener.onSelectionModeEnded();
            }
            notifyDataSetChanged();
        }
    }

    public void toggleSelection(int position) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position);
        } else {
            selectedItems.add(position);
        }

        notifyItemChanged(position);

        if (selectionModeListener != null) {
            selectionModeListener.onSelectionChanged(selectedItems.size());
        }

        if (selectedItems.isEmpty()) {
            endSelectionMode();
        }
    }

    public void selectAll() {
        if (selectionMode) {
            selectedItems.clear();
            for (int i = 0; i < contactGroups.size(); i++) {
                selectedItems.add(i);
            }
            notifyDataSetChanged();
            if (selectionModeListener != null) {
                selectionModeListener.onSelectionChanged(selectedItems.size());
            }
        }
    }

    public void clearSelection() {
        selectedItems.clear();
        if (selectionMode) {
            endSelectionMode();
        }
    }

    public List<ContactsGroups> getSelectedGroups() {
        List<ContactsGroups> selected = new ArrayList<>();
        for (Integer position : selectedItems) {
            if (position < contactGroups.size()) {
                selected.add(contactGroups.get(position));
            }
        }
        return selected;
    }

    public int getSelectedCount() {
        return selectedItems.size();
    }

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public boolean isSelected(int position) {
        return selectedItems.contains(position);
    }

    @Override
    public int getItemCount() {
        return contactGroups != null ? contactGroups.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemGroupBinding binding;

        public ViewHolder(ItemGroupBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}