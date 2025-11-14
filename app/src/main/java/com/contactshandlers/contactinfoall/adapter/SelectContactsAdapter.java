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
import com.contactshandlers.contactinfoall.databinding.ItemSelectContactBinding;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.model.ContactData;

import java.util.List;

public class SelectContactsAdapter extends RecyclerView.Adapter<SelectContactsAdapter.ViewHolder> {

    private final Context context;
    private List<ContactData> contacts;

    public SelectContactsAdapter(Context context, List<ContactData> contacts) {
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
        ItemSelectContactBinding binding = ItemSelectContactBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        ContactData contact = contacts.get(position);
        holder.binding.tvName.setText(contact.getNameFL());
        holder.binding.tvNumber.setText(contact.getPhoneNumber());

        if (contact.getPhoto() != null) {
            Glide.with(context).load(contact.getPhoto()).into(holder.binding.ivProfile);
        } else {
            holder.binding.ivProfile.setImageBitmap(Utils.getInitialsBitmap(0, contact.getNameFL(), Utils.colorsList()[position % Utils.colorsList().length]));
        }

        boolean isFavourite = contact.isFavourite();
        holder.binding.ivFavourite.setImageResource(isFavourite ? R.drawable.ic_favourite : R.drawable.ic_favourite_tab);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean newStarStatus = !isFavourite;
                contact.setFavourite(newStarStatus);
                holder.binding.ivFavourite.setImageResource(newStarStatus ? R.drawable.ic_favourite : R.drawable.ic_favourite_tab);
                notifyItemChanged(position);
            }
        });

        holder.binding.ivFavourite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean newStarStatus = !isFavourite;
                contact.setFavourite(newStarStatus);
                holder.binding.ivFavourite.setImageResource(newStarStatus ? R.drawable.ic_favourite : R.drawable.ic_favourite_tab);
                notifyItemChanged(position);
            }
        });
    }

    @Override
    public int getItemCount() { return contacts.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemSelectContactBinding binding;
        public ViewHolder(ItemSelectContactBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
