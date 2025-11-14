package com.contactshandlers.contactinfoall.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.databinding.ItemDuplicateContactGroupBinding;
import com.contactshandlers.contactinfoall.model.DuplicateGroup;

import java.util.ArrayList;
import java.util.List;

public class DuplicateContactsAdapter extends RecyclerView.Adapter<DuplicateContactsAdapter.ViewHolder> {

    private Context context;
    private List<DuplicateGroup> duplicateGroups = new ArrayList<>();
    private ContactMergeAdapter adapter;
    private boolean[] isSelected;
    private int lastPosition = 0;
    private final OnDuplicateGroupClickListener listener;

    public interface OnDuplicateGroupClickListener {
        void onDuplicateGroupClick(DuplicateGroup duplicateGroup);
    }

    public DuplicateContactsAdapter(Context context, OnDuplicateGroupClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setDuplicateGroups(List<DuplicateGroup> duplicateGroups) {
        this.duplicateGroups = duplicateGroups;
        this.isSelected = new boolean[duplicateGroups.size()];
        this.isSelected[lastPosition] = true;
        this.listener.onDuplicateGroupClick(duplicateGroups.get(lastPosition));
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DuplicateContactsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDuplicateContactGroupBinding binding = ItemDuplicateContactGroupBinding.inflate(LayoutInflater.from(context), parent, false);
        return new DuplicateContactsAdapter.ViewHolder(binding);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull DuplicateContactsAdapter.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        DuplicateGroup group = duplicateGroups.get(position);
        if (context.getString(R.string.mobile).equals(group.getGroupType())) {
            holder.binding.tvGroupKey.setText(context.getString(R.string.mobile) + ": " + group.getGroupKey());
        } else {
            holder.binding.tvGroupKey.setText(context.getString(R.string.name) + ": " + group.getGroupKey());
        }

        setupRecyclerView(holder, group);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onDuplicateGroupClick(group);
                isSelected[position] = true;
                if (lastPosition != -1 && lastPosition != position){
                    isSelected[lastPosition] = false;
                }
                notifyItemChanged(lastPosition);
                notifyItemChanged(position);
                lastPosition = position;
            }
        });

        if (isSelected[position]){
            holder.binding.ivCheck.setImageResource(R.drawable.ic_check_duplicate);
        }else {
            holder.binding.ivCheck.setImageResource(R.drawable.ic_uncheck);
        }
    }

    private void setupRecyclerView(DuplicateContactsAdapter.ViewHolder holder, DuplicateGroup group) {
        holder.binding.rvDuplicateContacts.setLayoutManager(new LinearLayoutManager(context));
        adapter = new ContactMergeAdapter(context, group.getContacts());
        holder.binding.rvDuplicateContacts.setAdapter(adapter);
    }

    @Override
    public int getItemCount() {
        return duplicateGroups.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemDuplicateContactGroupBinding binding;
        public ViewHolder(ItemDuplicateContactGroupBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
