package com.contactshandlers.contactinfoall.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.databinding.ItemSpinnerBinding;

import java.util.List;

public class SpinnerPhoneAdapter extends ArrayAdapter<String> {

    private final Context context;
    private final List<String> items;
    private int selectedPosition = -1;

    public SpinnerPhoneAdapter(Context context, List<String> items) {
        super(context, R.layout.item_spinner, items);
        this.context = context;
        this.items = items;
    }

    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ItemSpinnerBinding binding = ItemSpinnerBinding.inflate(LayoutInflater.from(context), parent, false);
        binding.tvText.setText(items.get(position));
        binding.ivIcon.setVisibility(View.GONE);
        binding.tvText.setTextColor(ContextCompat.getColor(context, R.color.main));
        return binding.getRoot();
    }

    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        ItemSpinnerBinding binding = ItemSpinnerBinding.inflate(LayoutInflater.from(context), parent, false);

        binding.tvText.setText(items.get(position));

        if (position == items.size() - 1) {
            binding.ivIcon.setVisibility(View.VISIBLE);
        } else {
            binding.ivIcon.setVisibility(View.GONE);
        }

        if (position == selectedPosition) {
            binding.tvText.setTextColor(ContextCompat.getColor(context, R.color.main));
        } else {
            binding.tvText.setTextColor(ContextCompat.getColor(context, R.color.grey_font));
        }

        return binding.getRoot();
    }
}
