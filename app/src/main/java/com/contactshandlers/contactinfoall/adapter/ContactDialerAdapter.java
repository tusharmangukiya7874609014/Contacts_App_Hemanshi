package com.contactshandlers.contactinfoall.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.databinding.ItemContactDialerBinding;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.listeners.CallListener;
import com.contactshandlers.contactinfoall.model.ContactData;

import java.util.List;

public class ContactDialerAdapter extends RecyclerView.Adapter<ContactDialerAdapter.ViewHolder> {
    private List<ContactData> contacts;
    private Context context;
    private CallListener callListener;

    public ContactDialerAdapter(Context context, List<ContactData> contacts, CallListener callListener) {
        this.context = context;
        this.contacts = contacts;
        this.callListener = callListener;
    }

    public void setContacts(List<ContactData> contacts) {
        this.contacts = contacts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContactDialerBinding binding = ItemContactDialerBinding.inflate(LayoutInflater.from(context), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ContactData contact = contacts.get(position);
        String contactId = contact.getId();

        holder.binding.tvName.setText(contact.getNameFL());
        holder.binding.tvNumber.setText(contact.getPhoneNumber());

        if (contactId != null) {
            Bitmap photo = Utils.getContactPhoto(context, contactId);
            if (photo != null) {
                holder.binding.ivProfile.setImageBitmap(photo);
            } else {
                holder.binding.ivProfile.setImageBitmap(Utils.getInitialsBitmap(context.getColor(R.color.grey_font), contact.getNameFL(), context.getColor(R.color.bg_color)));
            }
        }else {
            holder.binding.ivProfile.setImageResource(R.drawable.ic_profile);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callListener.onCall(contact.getPhoneNumber());
            }
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemContactDialerBinding binding;
        public ViewHolder(ItemContactDialerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
