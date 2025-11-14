package com.contactshandlers.contactinfoall.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.ads.InterstitialAD;
import com.contactshandlers.contactinfoall.databinding.DialogDeleteCallHistoryBinding;
import com.contactshandlers.contactinfoall.databinding.ItemCallLogBinding;
import com.contactshandlers.contactinfoall.databinding.ItemDateBinding;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.listeners.AdCallback;
import com.contactshandlers.contactinfoall.listeners.CallListener;
import com.contactshandlers.contactinfoall.listeners.OnItemClickListener;
import com.contactshandlers.contactinfoall.model.CallLogItem;
import com.contactshandlers.contactinfoall.ui.activity.AddEditContactActivity;
import com.contactshandlers.contactinfoall.ui.activity.ViewHistoryActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CallLogAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private final List<Object> originalItems;
    private Dialog dialog;
    private DialogDeleteCallHistoryBinding deleteBinding;
    private OnItemClickListener listener;
    private CallListener callListener;
    private ExecutorService diffExecutor = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final int LARGE_DATASET_THRESHOLD = 1000;
    private boolean[] isSelected;
    private int lastPosition = -1;
    private Map<String, Boolean> selectionMap = new HashMap<>();

    private final android.util.LruCache<String, Bitmap> bitmapCache = new android.util.LruCache<String, Bitmap>(50) {
        @Override
        protected int sizeOf(String key, Bitmap bitmap) {
            return bitmap.getByteCount() / 1024;
        }
    };

    public CallLogAdapter(Context context, Map<String, ArrayList<CallLogItem>> groupedData, OnItemClickListener listener, CallListener callListener) {
        this.context = context;
        originalItems = new ArrayList<>();

        for (Map.Entry<String, ArrayList<CallLogItem>> entry : groupedData.entrySet()) {
            originalItems.add(entry.getKey());
            originalItems.addAll(entry.getValue());
        }

        this.isSelected = new boolean[originalItems.size()];
        this.selectionMap = new HashMap<>();

        this.listener = listener;
        this.callListener = callListener;
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= 0 && position < originalItems.size()) {
            return originalItems.get(position) instanceof String ? 0 : 1;
        }
        return 1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 0) {
            ItemDateBinding binding = ItemDateBinding.inflate(LayoutInflater.from(context), parent, false);
            return new DateViewHolder(binding);
        } else {
            ItemCallLogBinding binding = ItemCallLogBinding.inflate(LayoutInflater.from(context), parent, false);
            return new CallViewHolder(binding);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position >= originalItems.size()) {
            return;
        }

        if (originalItems.get(position) instanceof String) {
            DateViewHolder dateViewHolder = (DateViewHolder) holder;
            dateViewHolder.clear();
            dateViewHolder.binding.tvDate.setText((String) originalItems.get(position));
        } else {
            CallViewHolder callViewHolder = (CallViewHolder) holder;
            CallLogItem log = (CallLogItem) originalItems.get(position);

            callViewHolder.clear();

            callViewHolder.bind(log, position);
        }
    }

    @Override
    public int getItemCount() {
        return originalItems.size();
    }

    public void updateDataOptimized(Map<String, ArrayList<CallLogItem>> newData) {
        List<Object> newItems = createFlatList(newData);

        if (newItems.size() > LARGE_DATASET_THRESHOLD) {
            updateDataInBackground(newItems);
        } else {
            updateDataImmediate(newItems);
        }
    }

    private void updateDataInBackground(List<Object> newItems) {
        if (diffExecutor.isShutdown()) {
            updateDataImmediate(newItems);
            return;
        }

        diffExecutor.execute(() -> {
            try {
                final Map<String, Boolean> savedSelectionState = new HashMap<>();
                saveCurrentSelectionStateToMap(savedSelectionState);

                DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                        new CallLogDiffCallback(new ArrayList<>(originalItems), newItems),
                        true
                );

                mainHandler.post(() -> {
                    try {
                        originalItems.clear();
                        originalItems.addAll(newItems);
                        this.isSelected = new boolean[originalItems.size()];
                        restoreSelectionStateFromMap(savedSelectionState);
                        diffResult.dispatchUpdatesTo(this);
                    } catch (Exception e) {
                        updateDataImmediate(newItems);
                    }
                });

            } catch (Exception e) {
                mainHandler.post(() -> updateDataImmediate(newItems));
            }
        });
    }

    private void updateDataImmediate(List<Object> newItems) {
        saveCurrentSelectionState();
        originalItems.clear();
        originalItems.addAll(newItems);
        this.isSelected = new boolean[originalItems.size()];
        restoreSelectionState();
        notifyDataSetChanged();
    }

    private List<Object> createFlatList(Map<String, ArrayList<CallLogItem>> groupedData) {
        List<Object> items = new ArrayList<>();
        for (Map.Entry<String, ArrayList<CallLogItem>> entry : groupedData.entrySet()) {
            items.add(entry.getKey());
            items.addAll(entry.getValue());
        }
        return items;
    }

    public void setData(Map<String, ArrayList<CallLogItem>> groupedData) {
        updateDataOptimized(groupedData);
    }

    private void saveCurrentSelectionState() {
        selectionMap.clear();
        saveCurrentSelectionStateToMap(selectionMap);
    }

    private void saveCurrentSelectionStateToMap(Map<String, Boolean> targetMap) {
        targetMap.clear();
        if (isSelected != null && originalItems != null) {
            for (int i = 0; i < Math.min(isSelected.length, originalItems.size()); i++) {
                if (isSelected[i] && originalItems.get(i) instanceof CallLogItem) {
                    CallLogItem item = (CallLogItem) originalItems.get(i);
                    String key = createSelectionKey(item);
                    targetMap.put(key, true);
                }
            }
        }
    }

    private void restoreSelectionState() {
        restoreSelectionStateFromMap(selectionMap);
    }

    private void restoreSelectionStateFromMap(Map<String, Boolean> savedState) {
        lastPosition = -1;

        if (savedState.isEmpty() || isSelected == null || originalItems == null) {
            return;
        }

        for (int i = 0; i < Math.min(isSelected.length, originalItems.size()); i++) {
            if (originalItems.get(i) instanceof CallLogItem) {
                CallLogItem item = (CallLogItem) originalItems.get(i);
                String key = createSelectionKey(item);

                if (savedState.containsKey(key) && savedState.get(key)) {
                    isSelected[i] = true;
                    lastPosition = i;
                }
            }
        }
    }

    private String createSelectionKey(CallLogItem item) {
        return item.getNumber() + "_" + item.getTime() + "_" + item.getCallType();
    }

    private static class CallLogDiffCallback extends DiffUtil.Callback {
        private final List<Object> oldList;
        private final List<Object> newList;

        public CallLogDiffCallback(List<Object> oldList, List<Object> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            if (oldItemPosition >= oldList.size() || newItemPosition >= newList.size()) {
                return false;
            }

            Object oldItem = oldList.get(oldItemPosition);
            Object newItem = newList.get(newItemPosition);

            if (oldItem instanceof String && newItem instanceof String) {
                return oldItem.equals(newItem);
            }

            if (oldItem instanceof CallLogItem && newItem instanceof CallLogItem) {
                CallLogItem oldCallLog = (CallLogItem) oldItem;
                CallLogItem newCallLog = (CallLogItem) newItem;

                return Objects.equals(oldCallLog.getNumber(), newCallLog.getNumber()) &&
                        Objects.equals(oldCallLog.getTime(), newCallLog.getTime()) &&
                        Objects.equals(oldCallLog.getCallType(), newCallLog.getCallType());
            }

            return false;
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            if (oldItemPosition >= oldList.size() || newItemPosition >= newList.size()) {
                return false;
            }

            Object oldItem = oldList.get(oldItemPosition);
            Object newItem = newList.get(newItemPosition);

            if (oldItem instanceof String && newItem instanceof String) {
                return oldItem.equals(newItem);
            }

            if (oldItem instanceof CallLogItem && newItem instanceof CallLogItem) {
                CallLogItem oldCallLog = (CallLogItem) oldItem;
                CallLogItem newCallLog = (CallLogItem) newItem;

                return Objects.equals(oldCallLog.getName(), newCallLog.getName()) &&
                        Objects.equals(oldCallLog.getNumber(), newCallLog.getNumber()) &&
                        Objects.equals(oldCallLog.getCallType(), newCallLog.getCallType()) &&
                        Objects.equals(oldCallLog.getDuration(), newCallLog.getDuration()) &&
                        Objects.equals(oldCallLog.getTime(), newCallLog.getTime()) &&
                        oldCallLog.getCount() == newCallLog.getCount();
            }

            return false;
        }
    }

    public void showCallOptionsPopup(View anchorView, String phoneNumber) {
        try {
            LayoutInflater inflater = LayoutInflater.from(context);
            View popupView = inflater.inflate(R.layout.popup_menu_layout, null);

            TextView numberText = popupView.findViewById(R.id.tvPhoneNumber);
            if (numberText != null) {
                numberText.setText(phoneNumber);
            }

            final PopupWindow popupWindow = new PopupWindow(popupView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, true);

            popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.bg_popup));
            popupWindow.setElevation(10);
            popupWindow.setAnimationStyle(R.style.PopupAnimation);
            popupWindow.setOutsideTouchable(true);
            popupWindow.setFocusable(true);

            int[] location = new int[2];
            anchorView.getLocationOnScreen(location);
            int anchorX = location[0];
            int anchorY = location[1];

            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            int screenHeight = displayMetrics.heightPixels;

            popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int popupHeight = popupView.getMeasuredHeight();

            if (anchorY + anchorView.getHeight() + popupHeight > screenHeight) {
                popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, anchorX, anchorY - popupHeight);
            } else {
                popupWindow.showAsDropDown(anchorView, 0, 0);
            }

            View copyOption = popupView.findViewById(R.id.option_copy);
            if (copyOption != null) {
                copyOption.setOnClickListener(v -> {
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Phone Number", phoneNumber);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                    }
                    popupWindow.dismiss();
                });
            }

            View callOption = popupView.findViewById(R.id.option_call);
            if (callOption != null) {
                callOption.setOnClickListener(v -> {
                    callListener.onCall(phoneNumber);
                    popupWindow.dismiss();
                });
            }

            View messageOption = popupView.findViewById(R.id.option_message);
            if (messageOption != null) {
                messageOption.setOnClickListener(v -> {
                    Utils.sendSMS(context, phoneNumber);
                    popupWindow.dismiss();
                });
            }

            View deleteOption = popupView.findViewById(R.id.option_delete);
            if (deleteOption != null) {
                deleteOption.setOnClickListener(v -> {
                    showDeleteDialog(phoneNumber);
                    popupWindow.dismiss();
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showDeleteDialog(String phoneNumber) {
        try {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }

            dialog = new Dialog(context);
            deleteBinding = DialogDeleteCallHistoryBinding.inflate(((Activity) context).getLayoutInflater());
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
            dialog.setContentView(deleteBinding.getRoot());
            dialog.setCancelable(true);

            String message = context.getString(R.string.clear_history_prompt, phoneNumber);
            deleteBinding.tv2.setText(message);

            deleteBinding.btnCancel.setOnClickListener(v -> dialog.dismiss());

            deleteBinding.btnDelete.setOnClickListener(v -> {
                listener.onClick(phoneNumber, 0);
                dialog.dismiss();
            });

            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class DateViewHolder extends RecyclerView.ViewHolder {
        private final ItemDateBinding binding;

        DateViewHolder(ItemDateBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void clear() {
            binding.tvDate.setText("");
        }
    }

    public class CallViewHolder extends RecyclerView.ViewHolder {
        private final ItemCallLogBinding binding;

        CallViewHolder(ItemCallLogBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void clear() {
            binding.tvTime.setText("");
            binding.tvCallType.setText("");
            binding.tvName.setText("");
            binding.tvCount.setVisibility(View.GONE);
            binding.ivProfile.setImageDrawable(null);
            binding.ivCallType.setImageDrawable(null);

            binding.btnCall.setOnClickListener(null);
            binding.llMain.setOnLongClickListener(null);
            binding.llMain.setOnClickListener(null);
        }

        @SuppressLint("SetTextI18n")
        public void bind(CallLogItem log, int position) {
            try {
                binding.tvTime.setText(log.getTime());
                binding.tvCallType.setText(log.getCallType());

                String name = log.getName();
                String number = log.getNumber();
                String contactId;

                if (name != null && !name.isEmpty()) {
                    binding.tvName.setText(name);
                    contactId = Utils.getContactIdByName(context, name);
                } else {
                    contactId = null;
                    binding.tvName.setText(number);
                }

                String contactName = Utils.getContactNameByNumber(context, number);
                if (contactName == null) {
                    setupUnknownContact();
                } else {
                    setupKnownContact();
                }

                binding.tvName.setSelected(true);

                loadProfileImage(log, contactId, position);

                setCallTypeUI(log);

                if (log.getCount() > 1) {
                    binding.tvCount.setVisibility(View.VISIBLE);
                    binding.tvCount.setText("(" + log.getCount() + ")");
                } else {
                    binding.tvCount.setVisibility(View.GONE);
                }

                setupClickListeners(log, name, number, position);

                boolean isItemSelected = isValidPosition(position) && isSelected[position];
                updateSelectionUI(isItemSelected);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void setupClickListeners(CallLogItem log, String name, String number, int position) {
            binding.btnAddContact.setOnClickListener(v -> {
                InterstitialAD.getInstance().showInterstitial((Activity) context, new AdCallback() {
                    @Override
                    public void callbackCall() {
                        context.startActivity(new Intent(context, AddEditContactActivity.class)
                                .putExtra(Constants.CONTACT_NUMBER, number)
                                .putExtra(Constants.CONTACT_NAME, name)
                                .putExtra(Constants.TITLE, context.getString(R.string.add_contact)));
                    }
                });
            });

            binding.btnVideoCall.setOnClickListener(v -> {
                startWhatsAppVideoCall(context, number);
            });

            binding.btnMessage.setOnClickListener(v -> {
                Utils.sendSMS(context, number);
            });

            binding.btnHistory.setOnClickListener(v -> {
                InterstitialAD.getInstance().showInterstitial((Activity) context, new AdCallback() {
                    @Override
                    public void callbackCall() {
                        context.startActivity(new Intent(context, ViewHistoryActivity.class)
                                .putExtra(Constants.CONTACT_NUMBER, number));
                    }
                });
            });

            binding.llMain.setOnLongClickListener(v -> {
                showCallOptionsPopup(binding.btnCall, number);
                return true;
            });

            binding.btnCall.setOnClickListener(v -> callListener.onCall(number));

            binding.llMain.setOnClickListener(v -> handleItemSelection(position));
        }

        private void handleItemSelection(int position) {
            if (!isValidPosition(position)) {
                return;
            }

            isSelected[position] = !isSelected[position];

            if (isSelected[position]) {
                if (lastPosition != -1 && lastPosition != position && isValidPosition(lastPosition)) {
                    isSelected[lastPosition] = false;
                    notifyItemChanged(lastPosition);
                }
                lastPosition = position;
            } else {
                if (lastPosition == position) {
                    lastPosition = -1;
                }
            }

            if (position < originalItems.size() && originalItems.get(position) instanceof CallLogItem) {
                CallLogItem item = (CallLogItem) originalItems.get(position);
                String key = createSelectionKey(item);

                if (isSelected[position]) {
                    selectionMap.put(key, true);
                } else {
                    selectionMap.remove(key);
                }
            }

            notifyItemChanged(position);
        }

        private boolean isValidPosition(int position) {
            return position >= 0 && position < originalItems.size() &&
                    isSelected != null && position < isSelected.length;
        }

        private void updateSelectionUI(boolean isItemSelected) {
            if (isItemSelected) {
                binding.llInfo.setVisibility(View.VISIBLE);
            } else {
                binding.llInfo.setVisibility(View.GONE);
            }
        }

        private void startWhatsAppVideoCall(Context context, String phoneNumber) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://wa.me/" + phoneNumber));
            intent.setPackage("com.whatsapp");
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            } else {
                Toast.makeText(context, context.getString(R.string.whatsapp_not_installed), Toast.LENGTH_SHORT).show();
            }
        }

        private void setupKnownContact() {
            binding.btnAddContact.setVisibility(View.GONE);
            binding.btnVideoCall.setVisibility(View.VISIBLE);
        }

        private void setupUnknownContact() {
            binding.btnAddContact.setVisibility(View.VISIBLE);
            binding.btnVideoCall.setVisibility(View.GONE);
        }

        private void loadProfileImage(CallLogItem log, String contactId, int position) {
            try {
                String cacheKey = contactId != null ? contactId : log.getNumber();
                Bitmap cachedBitmap = bitmapCache.get(cacheKey);

                if (cachedBitmap != null) {
                    binding.ivProfile.setImageBitmap(cachedBitmap);
                    return;
                }

                Bitmap bitmap;
                if (contactId != null) {
                    Bitmap photo = Utils.getContactPhoto(context, contactId);
                    if (photo != null) {
                        bitmap = photo;
                    } else {
                        bitmap = Utils.getInitialsBitmap(0, log.getName(),
                                Utils.colorsList()[position % Utils.colorsList().length]);
                    }
                } else {
                    bitmap = Utils.getInitialsBitmap(context, R.drawable.ic_user,
                            Utils.colorsList()[position % Utils.colorsList().length]);
                }

                bitmapCache.put(cacheKey, bitmap);
                binding.ivProfile.setImageBitmap(bitmap);
            } catch (Exception e) {
                binding.ivProfile.setImageResource(R.drawable.ic_user);
            }
        }

        private void setCallTypeUI(CallLogItem log) {
            try {
                switch (log.getCallType()) {
                    case "Incoming":
                    case "Rejected":
                        binding.ivCallType.setImageResource(R.drawable.ic_incoming);
                        binding.tvCallType.setTextColor(context.getColor(R.color.grey_font));
                        binding.ivCallType.setImageTintList(ColorStateList.valueOf(context.getColor(R.color.grey_font)));
                        break;
                    case "Outgoing":
                        binding.ivCallType.setImageResource(R.drawable.ic_outgoing);
                        binding.tvCallType.setTextColor(context.getColor(R.color.grey_font));
                        binding.ivCallType.setImageTintList(ColorStateList.valueOf(context.getColor(R.color.grey_font)));
                        break;
                    case "Missed Call":
                        binding.ivCallType.setImageResource(R.drawable.ic_missed);
                        binding.tvCallType.setTextColor(context.getColor(R.color.red));
                        binding.ivCallType.setImageTintList(ColorStateList.valueOf(context.getColor(R.color.red)));
                        break;
                    default:
                        binding.ivCallType.setImageResource(R.drawable.ic_incoming);
                        binding.tvCallType.setTextColor(context.getColor(R.color.grey_font));
                        binding.ivCallType.setImageTintList(ColorStateList.valueOf(context.getColor(R.color.grey_font)));
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (diffExecutor != null && !diffExecutor.isShutdown()) {
            diffExecutor.shutdown();
        }
    }
}