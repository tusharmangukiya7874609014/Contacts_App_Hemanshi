package com.contactshandlers.contactinfoall.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.databinding.ItemReminderBinding;
import com.contactshandlers.contactinfoall.model.Reminder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ViewHolder> {

    private Context context;
    private List<Reminder> reminders;
    private OnDeleteClickListener onDeleteClickListener;

    public interface OnDeleteClickListener {
        void onDelete(Reminder reminder);
    }

    public ReminderAdapter(Context context, List<Reminder> reminders, OnDeleteClickListener listener) {
        this.context = context;
        this.reminders = reminders;
        this.onDeleteClickListener = listener;
    }

    public void setReminders(List<Reminder> reminders) {
        this.reminders = reminders;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReminderBinding binding = ItemReminderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Reminder reminder = reminders.get(position);
        holder.binding.ivColor.setCardBackgroundColor(Color.parseColor(reminder.getColor()));
        holder.binding.tvReminder.setText(reminder.getMessage());

        long reminderTime = reminder.getReminderTime();
        holder.binding.tvTime.setText(formatLongToDayTime(reminderTime, context));
        
        holder.binding.btnDelete.setOnClickListener(v -> onDeleteClickListener.onDelete(reminder));
    }

    public static String formatLongToDayTime(long timestamp, Context context) {
        Date date = new Date(timestamp);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        String today = dayFormat.format(new Date(calendar.getTimeInMillis()));
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        String tomorrow = dayFormat.format(new Date(calendar.getTimeInMillis()));

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String formattedTime = timeFormat.format(date);

        String result = today.equals(dayFormat.format(date)) ? context.getString(R.string.today) + ", " + formattedTime :
                tomorrow.equals(dayFormat.format(date)) ? context.getString(R.string.tomorrow) + ", " + formattedTime : formattedTime;

        return result;
    }

    @Override
    public int getItemCount() {
        return reminders.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemReminderBinding binding;

        public ViewHolder(ItemReminderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
