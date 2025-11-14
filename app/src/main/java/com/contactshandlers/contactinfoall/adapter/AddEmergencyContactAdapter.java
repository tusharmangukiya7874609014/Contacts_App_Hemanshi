package com.contactshandlers.contactinfoall.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.databinding.ItemAddEmergencyBinding;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.model.ContactData;

import java.util.List;

public class AddEmergencyContactAdapter extends RecyclerView.Adapter<AddEmergencyContactAdapter.ViewHolder> {

    private final Context context;
    private List<ContactData> contacts;

    public AddEmergencyContactAdapter(Context context, List<ContactData> contacts) {
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
        ItemAddEmergencyBinding binding = ItemAddEmergencyBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        ContactData contact = contacts.get(position);
        holder.binding.tvName.setText(contact.getNameFL());

        if (contact.getPhoto() != null) {
            Glide.with(context).load(contact.getPhoto()).into(holder.binding.ivProfile);
        } else {
            holder.binding.ivProfile.setImageBitmap(Utils.getInitialsBitmap(0, contact.getNameFL(), Utils.colorsList()[position % Utils.colorsList().length]));
        }

        boolean isEmergency = contact.isEmergency();
        holder.binding.ivEmergency.setImageResource(isEmergency ? R.drawable.ic_emergency_select : R.drawable.ic_emergency_unselect);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean newEmergencyStatus = !isEmergency;
                contact.setEmergency(newEmergencyStatus);
                holder.binding.ivEmergency.setImageResource(newEmergencyStatus ? R.drawable.ic_emergency_select : R.drawable.ic_emergency_unselect);
                notifyItemChanged(position);
            }
        });

        holder.binding.ivEmergency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean newEmergencyStatus = !isEmergency;
                contact.setEmergency(newEmergencyStatus);
                holder.binding.ivEmergency.setImageResource(newEmergencyStatus ? R.drawable.ic_emergency_select : R.drawable.ic_emergency_unselect);
                notifyItemChanged(position);
            }
        });
    }

    @Override
    public int getItemCount() { return contacts.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemAddEmergencyBinding binding;
        public ViewHolder(ItemAddEmergencyBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
