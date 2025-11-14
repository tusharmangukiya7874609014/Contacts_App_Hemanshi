package com.contactshandlers.contactinfoall.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.contactshandlers.contactinfoall.databinding.ItemColorBinding;
import com.contactshandlers.contactinfoall.listeners.OnColorClickListener;
import com.contactshandlers.contactinfoall.model.ColorModel;

import java.util.List;

public class ColorAdapter extends RecyclerView.Adapter<ColorAdapter.ViewHolder> {

    private Context context;
    private List<ColorModel> colorList;
    private OnColorClickListener onColorClickListener;

    public ColorAdapter(Context context, List<ColorModel> colorList, OnColorClickListener onColorClickListener) {
        this.context = context;
        this.colorList = colorList;
        this.onColorClickListener = onColorClickListener;
        this.colorList.get(0).setSelected(true);
        this.onColorClickListener.OnClick(colorList.get(0).getColor());
    }

    @NonNull
    @Override
    public ColorAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemColorBinding binding = ItemColorBinding.inflate(LayoutInflater.from(context), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ColorAdapter.ViewHolder holder, int position) {
        ColorModel colorModel = colorList.get(position);
        holder.binding.ivColor.setCardBackgroundColor(Color.parseColor(colorModel.getColor()));
        holder.binding.ivSelection.setVisibility(colorModel.isSelected() ? View.VISIBLE : View.GONE);
        holder.itemView.setOnClickListener(v -> {
            onColorClickListener.OnClick(colorModel.getColor());
            for (ColorModel model : colorList) {
                model.setSelected(false);
            }
            colorModel.setSelected(true);
            notifyDataSetChanged();
        });
    }

    @Override
    public int getItemCount() {
        return colorList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemColorBinding binding;
        public ViewHolder(ItemColorBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
