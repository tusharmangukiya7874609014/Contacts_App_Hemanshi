package com.contactshandlers.contactinfoall.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.databinding.ItemLangBinding;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.helper.SharedPreferencesManager;
import com.contactshandlers.contactinfoall.listeners.LangSelectionListeners;
import com.contactshandlers.contactinfoall.model.LanguageModel;

import java.util.List;

public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.ViewHolder> {

    private Context context;
    private List<LanguageModel> langList;
    private boolean[] isSelected;
    private int lastPosition;
    private LangSelectionListeners listeners;

    public LanguageAdapter(Context context, List<LanguageModel> langList, LangSelectionListeners listeners) {
        this.context = context;
        this.langList = langList;
        this.listeners = listeners;
        this.lastPosition = SharedPreferencesManager.getInstance().getIntValue(Constants.SELECTED_LANG, 0);
        this.isSelected = new boolean[langList.size()];
        this.isSelected[lastPosition] = true;
        this.listeners.onClick(langList.get(lastPosition).getCode(), lastPosition);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLangBinding binding = ItemLangBinding.inflate(LayoutInflater.from(context),parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        int icon = langList.get(position).getIcon();
        String lang = langList.get(position).getLang();
        String locale = langList.get(position).getCode();

        holder.binding.ivIcon.setImageResource(icon);
        holder.binding.tvLanguage.setText(lang);
        holder.binding.tvLanguage.setSelected(true);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listeners.onClick(locale, position);
                isSelected[position] = true;
                if (lastPosition != -1 && lastPosition != position){
                    isSelected[lastPosition] = false;
                }
                notifyItemChanged(lastPosition);
                notifyItemChanged(position);
                lastPosition = position;
            }
        });

        if (isSelected[position]){
            holder.binding.ivSelection.setVisibility(View.VISIBLE);
            holder.binding.clMain.setBackground(context.getDrawable(R.drawable.bg_main_selected));
        }else {
            holder.binding.ivSelection.setVisibility(View.INVISIBLE);
            holder.binding.clMain.setBackground(context.getDrawable(R.drawable.bg_main));
        }
    }

    @Override
    public int getItemCount() {
        return langList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private ItemLangBinding binding;
        public ViewHolder(ItemLangBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
