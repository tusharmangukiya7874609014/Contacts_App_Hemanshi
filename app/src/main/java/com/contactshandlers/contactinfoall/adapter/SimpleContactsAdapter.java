package com.contactshandlers.contactinfoall.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.contactshandlers.contactinfoall.ads.InterstitialAD;
import com.contactshandlers.contactinfoall.databinding.ItemContactBinding;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.model.ContactGroupItem;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.listeners.AdCallback;
import com.contactshandlers.contactinfoall.ui.activity.ViewContactActivity;

import java.util.List;

public class SimpleContactsAdapter extends RecyclerView.Adapter<SimpleContactsAdapter.ViewHolder> {

    private List<ContactGroupItem> contacts;
    private Context context;

    public SimpleContactsAdapter(Context context, List<ContactGroupItem> contacts) {
        this.context = context;
        this.contacts = contacts;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContactBinding binding = ItemContactBinding.inflate(LayoutInflater.from(context), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ContactGroupItem contact = contacts.get(position);

        if (contact.getPhoto() != null) {
            Glide.with(context).load(contact.getPhoto()).into(holder.binding.ivProfile);
        } else {
            holder.binding.ivProfile.setImageBitmap(Utils.getInitialsBitmap(0, contact.getDisplayName(), Utils.colorsList()[position % Utils.colorsList().length]));
        }

        if (position == contacts.size() - 1) {
            holder.binding.view.setVisibility(View.GONE);
        } else {
            holder.binding.view.setVisibility(View.VISIBLE);
        }

        holder.binding.tvName.setText(contact.getDisplayName());

        setupClickListeners(holder, contact);
    }

    private void setupClickListeners(ViewHolder holder, ContactGroupItem contact) {
        holder.itemView.setOnClickListener(v -> {
            InterstitialAD.getInstance().showInterstitial((Activity) context, new AdCallback() {
                @Override
                public void callbackCall() {
                    Intent intent = new Intent(context, ViewContactActivity.class);
                    intent.putExtra(Constants.CONTACT_ID, contact.getId());
                    context.startActivity(intent);
                }
            });
        });
    }

    @Override
    public int getItemCount() {
        return contacts != null ? contacts.size() : 0;
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemContactBinding binding;
        public ViewHolder(ItemContactBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}