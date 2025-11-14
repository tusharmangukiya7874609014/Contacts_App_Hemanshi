package com.contactshandlers.contactinfoall.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.databinding.ItemCmwBinding;
import com.contactshandlers.contactinfoall.listeners.OnItemClickListener;
import com.contactshandlers.contactinfoall.model.PhoneItem;

import java.util.List;

public class CMVAdapter extends RecyclerView.Adapter<CMVAdapter.ViewHolder> {

    private Context context;
    private List<PhoneItem> numbers;
    private String title;
    private OnItemClickListener onItemClickListener;

    public CMVAdapter(Context context, List<PhoneItem> numbers, String title, OnItemClickListener onItemClickListener) {
        this.context = context;
        this.numbers = numbers;
        this.title = title;
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public CMVAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCmwBinding binding = ItemCmwBinding.inflate(LayoutInflater.from(context), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CMVAdapter.ViewHolder holder, int position) {
        String number = numbers.get(position).getPhoneNumber();
        holder.binding.tvNumber.setText(number);

        if (title.equals(context.getString(R.string.call))) {
            holder.binding.ivIcon.setImageResource(R.drawable.ic_call_mul);
        } else if (title.equals(context.getString(R.string.message))) {
            holder.binding.ivIcon.setImageResource(R.drawable.ic_message_mul);
        } else if (title.equals(context.getString(R.string.whatsapp))) {
            holder.binding.ivIcon.setImageResource(R.drawable.ic_whatsapp_mul);
        }

        holder.itemView.setOnClickListener(v -> onItemClickListener.onClick(number, position));
    }

    @Override
    public int getItemCount() {
        return numbers.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemCmwBinding binding;
        public ViewHolder(ItemCmwBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
