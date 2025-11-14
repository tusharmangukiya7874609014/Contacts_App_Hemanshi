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
import com.contactshandlers.contactinfoall.databinding.ItemEmergencyContactBinding;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.listeners.AdCallback;
import com.contactshandlers.contactinfoall.model.ContactData;
import com.contactshandlers.contactinfoall.model.EmergencyContact;
import com.contactshandlers.contactinfoall.ui.activity.EmergencyContactActivity;
import com.contactshandlers.contactinfoall.ui.activity.ViewContactActivity;

import java.util.List;

public class EmergencyContactsAdapter extends RecyclerView.Adapter<EmergencyContactsAdapter.ViewHolder> {

    private Context context;
    private List<EmergencyContact> emergencyContacts;

    public EmergencyContactsAdapter(Context context, List<EmergencyContact> emergencyContacts) {
        this.context = context;
        this.emergencyContacts = emergencyContacts;
    }

    public void setEmergencyContacts(List<EmergencyContact> emergencyContacts) {
        this.emergencyContacts = emergencyContacts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EmergencyContactsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemEmergencyContactBinding binding = ItemEmergencyContactBinding.inflate(LayoutInflater.from(context), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull EmergencyContactsAdapter.ViewHolder holder, int position) {
        EmergencyContact contact = emergencyContacts.get(position);
        holder.binding.tvName.setText(contact.getName());
        holder.binding.tvNumber.setText(contact.getPhone());

        if (contact.getPhotoUri() != null) {
            Glide.with(context).load(contact.getPhotoUri()).into(holder.binding.ivProfile);
        } else {
            holder.binding.ivProfile.setImageBitmap(Utils.getInitialsBitmap(0, contact.getName(), Utils.colorsList()[position % Utils.colorsList().length]));
        }

        holder.itemView.setOnClickListener(v -> {
            InterstitialAD.getInstance().showInterstitial((Activity) context, new AdCallback() {
                @Override
                public void callbackCall() {
                    Intent intent = new Intent(context, ViewContactActivity.class);
                    intent.putExtra(Constants.CONTACT_ID, contact.getContactId());
                    context.startActivity(intent);
                }
            });
        });
    }

    @Override
    public int getItemCount() {
        return emergencyContacts.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemEmergencyContactBinding binding;
        public ViewHolder(ItemEmergencyContactBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
