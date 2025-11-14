package com.contactshandlers.contactinfoall.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.databinding.ItemHistoryBinding;
import com.contactshandlers.contactinfoall.model.CallHistory;

import java.util.List;

public class CallHistoryAdapter extends RecyclerView.Adapter<CallHistoryAdapter.ViewHolder> {
    private List<CallHistory> callLogList;
    private Context context;

    public CallHistoryAdapter(Context context, List<CallHistory> callLogList) {
        this.context = context;
        this.callLogList = callLogList;
    }

    public void setCallLogList(List<CallHistory> callLogList) {
        this.callLogList.clear();
        this.callLogList.addAll(callLogList);
        notifyDataSetChanged();
    }

    public void addCallLogData(List<CallHistory> newCallLogs) {
        if (newCallLogs != null && !newCallLogs.isEmpty()) {
            this.callLogList.addAll(newCallLogs);
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHistoryBinding binding = ItemHistoryBinding.inflate(LayoutInflater.from(context), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CallHistory item = callLogList.get(position);
        holder.binding.tvCallType.setText(item.getCallType());
        holder.binding.tvDateTime.setText(item.getDateTime());
        holder.binding.tvDuration.setText(item.getDuration());

        switch (item.getCallType()) {
            case "Incoming":
            case "Rejected":
                holder.binding.ivCallType.setImageResource(R.drawable.ic_incoming);
                holder.binding.ivCallType.setImageTintList(ColorStateList.valueOf(context.getColor(R.color.grey_font)));
                holder.binding.tvCallType.setTextColor(context.getColor(R.color.primary_font));
                holder.binding.tvDuration.setVisibility(View.VISIBLE);
                break;
            case "Outgoing":
                holder.binding.ivCallType.setImageResource(R.drawable.ic_outgoing);
                holder.binding.ivCallType.setImageTintList(ColorStateList.valueOf(context.getColor(R.color.grey_font)));
                holder.binding.tvCallType.setTextColor(context.getColor(R.color.primary_font));
                holder.binding.tvDuration.setVisibility(View.VISIBLE);
                break;
            case "Missed Call":
                holder.binding.ivCallType.setImageResource(R.drawable.ic_missed);
                holder.binding.ivCallType.setImageTintList(ColorStateList.valueOf(context.getColor(R.color.red)));
                holder.binding.tvCallType.setTextColor(context.getColor(R.color.red));
                holder.binding.tvDuration.setVisibility(View.GONE);
                break;
            default:
                holder.binding.ivCallType.setImageResource(R.drawable.ic_incoming);
                holder.binding.ivCallType.setImageTintList(ColorStateList.valueOf(context.getColor(R.color.grey_font)));
                holder.binding.tvCallType.setTextColor(context.getColor(R.color.primary_font));
                holder.binding.tvDuration.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return callLogList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemHistoryBinding binding;
        public ViewHolder(ItemHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}