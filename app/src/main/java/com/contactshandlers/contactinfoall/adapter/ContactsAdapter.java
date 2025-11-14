package com.contactshandlers.contactinfoall.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.ads.InterstitialAD;
import com.contactshandlers.contactinfoall.databinding.ItemContactBinding;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.helper.SharedPreferencesManager;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.listeners.AdCallback;
import com.contactshandlers.contactinfoall.model.ContactData;
import com.contactshandlers.contactinfoall.ui.activity.ViewContactActivity;
import com.contactshandlers.contactinfoall.ui.fragment.ContactFragment;

import java.util.List;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ViewHolder> {

    private final Context context;
    private List<ContactData> contactList;

    public ContactsAdapter(List<ContactData> contactList, Context context) {
        this.contactList = contactList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContactBinding binding = ItemContactBinding.inflate(LayoutInflater.from(context), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        ContactData contact = contactList.get(position);

        boolean isFormatFirst = SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_FORMAT_BY_FIRST, true);
        holder.binding.tvName.setText(contact.getFormattedName(isFormatFirst));

        boolean isShowPhoneNumber = SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_SHOW_PHONE_NUMBER, true);
        if (isShowPhoneNumber){
            holder.binding.tvNumber.setVisibility(View.VISIBLE);
            holder.binding.tvNumber.setText(contact.getPhoneNumber());
        } else {
            holder.binding.tvNumber.setVisibility(View.GONE);
            holder.binding.tvNumber.setText("");
        }

        if (contact.getPhoto() != null) {
            Glide.with(context).load(contact.getPhoto()).into(holder.binding.ivProfile);
        } else {
            holder.binding.ivProfile.setImageBitmap(Utils.getInitialsBitmap(0, contact.getNameFL(), Utils.colorsList()[position % Utils.colorsList().length]));
        }

        if (contact.isSelected()) {
            holder.itemView.setBackgroundColor(context.getColor(R.color.bg_recent_option));
        } else {
            holder.itemView.setBackground(context.getDrawable(R.drawable.ripple_effect_contact));
        }

        if (position == contactList.size() - 1) {
            holder.binding.view.setVisibility(View.GONE);
        } else {
            holder.binding.view.setVisibility(View.VISIBLE);
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
            } else {
                InterstitialAD.getInstance().showInterstitial((Activity) context, new AdCallback() {
                    @Override
                    public void callbackCall() {
                        Intent intent = new Intent(context, ViewContactActivity.class);
                        intent.putExtra(Constants.CONTACT_ID, contact.getId());
                        context.startActivity(intent);
                    }
                });
            }
        });
    }

    void toggleSelection(int position) {
        ContactData contact = contactList.get(position);
        if (ContactFragment.selectedContacts.contains(contact)) {
            ContactFragment.selectedContacts.remove(contact);
            contact.setSelected(false);
        } else {
            ContactFragment.selectedContacts.add(contact);
            contact.setSelected(true);
        }
        ContactFragment.changeText();
        notifyItemChanged(position);
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemContactBinding binding;
        public ViewHolder(ItemContactBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
