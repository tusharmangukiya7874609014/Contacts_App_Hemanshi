package com.contactshandlers.contactinfoall.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.databinding.ItemSpinnerDropdownBinding;
import com.contactshandlers.contactinfoall.databinding.ItemSpinnerSelectedBinding;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.helper.SharedPreferencesManager;

import java.lang.reflect.Method;
import java.util.List;

public class SpinnerAccountAdapter extends ArrayAdapter<String> {

    private Context context;
    private int selectedPosition;

    public SpinnerAccountAdapter(Context context, List<String> accounts, int selectedPosition) {
        super(context, 0, accounts);
        this.context = context;
        this.selectedPosition = selectedPosition;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ItemSpinnerSelectedBinding binding = ItemSpinnerSelectedBinding.inflate(LayoutInflater.from(context), parent, false);
        String account = getItem(position);
        if (account != null) {
            binding.tvSpinnerItem.setText(account);
        }
        return binding.getRoot();
    }

    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        ItemSpinnerDropdownBinding binding = ItemSpinnerDropdownBinding.inflate(LayoutInflater.from(context), parent, false);
        String account = getItem(position);

        if (account != null) {
            binding.tvDropdownItem.setText(account);
        }

        if (position == selectedPosition){
            binding.tvDropdownItem.setBackgroundColor(context.getColor(R.color.bg_account));
        }else {
            binding.tvDropdownItem.setBackgroundColor(context.getColor(R.color.bg_color));
        }

        binding.tvDropdownItem.setOnClickListener(v -> {
            selectedPosition = position;
            SharedPreferencesManager.getInstance().setIntValue(Constants.SELECTED_LANGUAGE_POSITION, selectedPosition);
            notifyDataSetChanged();

            if (context instanceof Activity) {
                Spinner spinner = ((Activity) context).findViewById(R.id.spAccounts);
                if (spinner != null) {
                    spinner.setSelection(position);
                    try {
                        Method method = Spinner.class.getDeclaredMethod("onDetachedFromWindow");
                        method.setAccessible(true);
                        method.invoke(spinner);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        return binding.getRoot();
    }
}
