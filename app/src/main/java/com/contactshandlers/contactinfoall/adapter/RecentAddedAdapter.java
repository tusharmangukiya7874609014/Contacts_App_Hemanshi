package com.contactshandlers.contactinfoall.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.ads.InterstitialAD;
import com.contactshandlers.contactinfoall.databinding.ItemRecenetViewAddedBinding;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.listeners.AdCallback;
import com.contactshandlers.contactinfoall.model.RecentAddedContact;
import com.contactshandlers.contactinfoall.ui.activity.ViewContactActivity;
import com.microsoft.clarity.a.C;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecentAddedAdapter extends RecyclerView.Adapter<RecentAddedAdapter.ViewHolder> {

    private Context context;
    private List<RecentAddedContact> recentAddedContacts;

    public RecentAddedAdapter(Context context, List<RecentAddedContact> recentAddedContacts) {
        this.context = context;
        this.recentAddedContacts = recentAddedContacts;
    }

    public void setRecentAddedContacts(List<RecentAddedContact> recentAddedContacts) {
        this.recentAddedContacts = recentAddedContacts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecentAddedAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRecenetViewAddedBinding binding = ItemRecenetViewAddedBinding.inflate(LayoutInflater.from(context), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentAddedAdapter.ViewHolder holder, int position) {
        RecentAddedContact recentAddedContact = recentAddedContacts.get(position);

        String contactId = recentAddedContact.getContactId();
        Bitmap photo = Utils.getContactPhoto(context, contactId);
        if (photo != null) {
            holder.binding.ivProfile.setImageBitmap(photo);
        } else {
            holder.binding.ivProfile.setImageBitmap(Utils.getInitialsBitmap(0, Utils.getContactNameById(context, contactId), Utils.colorsList()[position % Utils.colorsList().length]));
        }

        holder.binding.tvName.setText(recentAddedContact.getName());

        long timestamp = recentAddedContact.getAddedTimestamp();
        String formattedDate = getFormattedDate(timestamp, context);
        holder.binding.tvContactDate.setText(formattedDate);

        if (position == recentAddedContacts.size() - 1) {
            holder.binding.view.setVisibility(View.GONE);
        } else {
            holder.binding.view.setVisibility(View.VISIBLE);
        }

        holder.itemView.setOnClickListener(v -> {
            InterstitialAD.getInstance().showInterstitial((Activity) context, new AdCallback() {
                @Override
                public void callbackCall() {
                    Intent intent = new Intent(context, ViewContactActivity.class);
                    intent.putExtra(Constants.CONTACT_ID, contactId);
                    context.startActivity(intent);
                }
            });
        });
    }

    private static String getFormattedDate(long timestamp, Context context) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String callDate = sdf.format(new Date(timestamp));

        String today = sdf.format(new Date());
        String yesterday = sdf.format(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000));

        if (callDate.equals(today)) return context.getString(R.string.today);
        if (callDate.equals(yesterday)) return context.getString(R.string.yesterday);
        return callDate;
    }

    @Override
    public int getItemCount() {
        return recentAddedContacts.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemRecenetViewAddedBinding binding;
        public ViewHolder(ItemRecenetViewAddedBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
