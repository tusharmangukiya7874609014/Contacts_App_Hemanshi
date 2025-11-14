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
import com.contactshandlers.contactinfoall.databinding.ItemFavouriteContactBinding;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.listeners.AdCallback;
import com.contactshandlers.contactinfoall.model.ContactData;
import com.contactshandlers.contactinfoall.ui.activity.ViewContactActivity;

import java.util.List;

public class FavouriteContactsAdapter extends RecyclerView.Adapter<FavouriteContactsAdapter.ViewHolder> {

    private List<ContactData> contacts;
    private final Context context;

    public FavouriteContactsAdapter(Context context, List<ContactData> contacts) {
        this.context = context;
        this.contacts = contacts;
    }

    public void setContacts(List<ContactData> contacts) {
        this.contacts = contacts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFavouriteContactBinding binding = ItemFavouriteContactBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ContactData contact = contacts.get(position);
        holder.binding.tvName.setText(contact.getNameFL());

        if (contact.getPhoto() != null) {
            Glide.with(context).load(contact.getPhoto()).into(holder.binding.ivProfile);
        } else {
            holder.binding.ivProfile.setImageBitmap(Utils.getInitialsBitmap(0, contact.getNameFL(), Utils.colorsList()[position % Utils.colorsList().length]));
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

    @Override
    public int getItemCount() { return contacts.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemFavouriteContactBinding binding;
        public ViewHolder(ItemFavouriteContactBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
