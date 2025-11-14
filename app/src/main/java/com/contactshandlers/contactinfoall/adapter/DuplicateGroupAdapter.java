package com.contactshandlers.contactinfoall.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.databinding.ItemDuplicateGroupBinding;
import com.contactshandlers.contactinfoall.model.DuplicateGroup;

import java.util.ArrayList;
import java.util.List;

public class DuplicateGroupAdapter extends RecyclerView.Adapter<DuplicateGroupAdapter.ViewHolder> {

    private Context context;
    private List<DuplicateGroup> duplicateGroups = new ArrayList<>();
    private OnGroupClickListener onGroupClickListener;

    public interface OnGroupClickListener {
        void onGroupClick(DuplicateGroup duplicateGroup);
    }

    public DuplicateGroupAdapter(Context context, OnGroupClickListener onGroupClickListener) {
        this.context = context;
        this.onGroupClickListener = onGroupClickListener;
    }

    public void setDuplicateGroups(List<DuplicateGroup> duplicateGroups) {
        this.duplicateGroups = duplicateGroups;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDuplicateGroupBinding binding = ItemDuplicateGroupBinding.inflate(LayoutInflater.from(context), parent, false);
        return new ViewHolder(binding);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DuplicateGroup group = duplicateGroups.get(position);
        holder.binding.tvGroupName.setText(group.getGroupKey());
        holder.binding.tvCountDuplicate.setText(group.getDuplicateCount() + " " + context.getString(R.string.duplicate_contacts));
        holder.itemView.setOnClickListener(v -> {
            if (onGroupClickListener != null) {
                onGroupClickListener.onGroupClick(group);
            }
        });
    }

    @Override
    public int getItemCount() {
        return duplicateGroups.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemDuplicateGroupBinding binding;
        public ViewHolder(ItemDuplicateGroupBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}