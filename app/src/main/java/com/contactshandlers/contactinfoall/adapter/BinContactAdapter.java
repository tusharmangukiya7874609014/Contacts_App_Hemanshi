package com.contactshandlers.contactinfoall.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.databinding.ItemBinContactBinding;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.listeners.OnItemClickListener;
import com.contactshandlers.contactinfoall.model.BinContactEntity;
import com.contactshandlers.contactinfoall.model.ContactData;
import com.contactshandlers.contactinfoall.room.BinContactViewModel;
import com.contactshandlers.contactinfoall.ui.activity.RecycleBinActivity;
import com.contactshandlers.contactinfoall.ui.fragment.ContactFragment;

import java.util.ArrayList;
import java.util.List;

public class BinContactAdapter extends RecyclerView.Adapter<BinContactAdapter.ViewHolder> {

    private Context context;
    private List<BinContactEntity> list = new ArrayList<>();
    private BinContactViewModel viewModel;

    public BinContactAdapter(Context context, BinContactViewModel viewModel) {
        this.context = context;
        this.viewModel = viewModel;
    }

    public void setList(List<BinContactEntity> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BinContactAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBinContactBinding binding = ItemBinContactBinding.inflate(LayoutInflater.from(context), parent, false);
        return new ViewHolder(binding);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(BinContactAdapter.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        BinContactEntity contact = list.get(position);
        holder.binding.tvName.setText(contact.name);
        if (!contact.phoneList.isEmpty()) {
            holder.binding.tvNumber.setText(contact.phoneList.get(0).getPhoneNumber());
        }
        holder.binding.tvTimer.setText(viewModel.getDaysLeft(contact) + " days left");
        if (contact.imageUri != null && !contact.imageUri.isEmpty()) {
            holder.binding.ivProfile.setImageURI(Uri.parse(contact.imageUri));
        } else {
            holder.binding.ivProfile.setImageBitmap(Utils.getInitialsBitmap(0, contact.name, Utils.colorsList()[position % Utils.colorsList().length]));
        }

        if (contact.isSelected()) {
            holder.itemView.setBackgroundColor(context.getColor(R.color.bg_color));
        } else {
            holder.itemView.setBackground(context.getDrawable(R.drawable.ripple_effect));
        }

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (!ContactFragment.isSelectionMode){
                    ContactFragment.isSelectionMode = true;
                    toggleSelection(position);
                }
                return false;
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (ContactFragment.isSelectionMode) {
                toggleSelection(position);
            }
        });
    }

    void toggleSelection(int position) {
        BinContactEntity contact = list.get(position);
        if (RecycleBinActivity.binSelectedContacts.contains(contact)) {
            RecycleBinActivity.binSelectedContacts.remove(contact);
            contact.setSelected(false);
        } else {
            RecycleBinActivity.binSelectedContacts.add(contact);
            contact.setSelected(true);
        }
        RecycleBinActivity.changeText();
        notifyItemChanged(position);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemBinContactBinding binding;

        public ViewHolder(ItemBinContactBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
