package com.contactshandlers.contactinfoall.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.contactshandlers.contactinfoall.databinding.ItemBlockedNumbersBinding;
import com.contactshandlers.contactinfoall.listeners.OnItemClickListener;

import java.util.List;

public class BlockedNumbersAdapter extends RecyclerView.Adapter<BlockedNumbersAdapter.ViewHolder> {

    private final Context context;
    private List<String> blockedNumbers;
    private OnItemClickListener onItemClickListener;

    public BlockedNumbersAdapter(Context context, List<String> blockedNumbers, OnItemClickListener onItemClickListener) {
        this.context = context;
        this.blockedNumbers = blockedNumbers;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public BlockedNumbersAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBlockedNumbersBinding binding = ItemBlockedNumbersBinding.inflate(LayoutInflater.from(context), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull BlockedNumbersAdapter.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        String number = blockedNumbers.get(position);
        holder.binding.tvNumber.setText(number);
        holder.binding.ivUnblock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemClickListener.onClick(number, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return blockedNumbers.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemBlockedNumbersBinding binding;
        public ViewHolder(ItemBlockedNumbersBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
