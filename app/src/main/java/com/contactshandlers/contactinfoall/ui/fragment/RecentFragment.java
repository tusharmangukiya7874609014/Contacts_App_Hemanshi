package com.contactshandlers.contactinfoall.ui.fragment;

import static android.content.Context.INPUT_METHOD_SERVICE;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.CallLog;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.adapter.CallLogAdapter;
import com.contactshandlers.contactinfoall.databinding.FragmentRecentBinding;
import com.contactshandlers.contactinfoall.helper.CallLogHelper;
import com.contactshandlers.contactinfoall.helper.DefaultDialerUtils;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.listeners.CallListener;
import com.contactshandlers.contactinfoall.listeners.OnItemClickListener;
import com.contactshandlers.contactinfoall.model.CallLogItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class RecentFragment extends Fragment implements View.OnClickListener {

    private FragmentRecentBinding binding;
    private Map<String, ArrayList<CallLogItem>> callLogs = new LinkedHashMap<>();
    private Map<String, ArrayList<CallLogItem>> filteredLogs = new HashMap<>();
    private Map<String, ArrayList<CallLogItem>> missedCallLogs = new LinkedHashMap<>();
    private CallLogAdapter adapter;
    private CallLogAdapter missedCallAdapter;
    private static final int REQUEST_CALL_PERMISSION = 1;
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private static final int INITIAL_LOAD_SIZE = 100;
    private static final int BATCH_SIZE = 1000;
    private static final int UI_UPDATE_INTERVAL = 3000;
    private boolean isLoadingData = false;
    private String currentSearchQuery = "";
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;
    private boolean wasInCall = false;
    private ActivityResultLauncher<Intent> defaultDialerLauncher;
    private final AtomicBoolean isBackgroundLoadingComplete = new AtomicBoolean(false);
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingUIUpdate;
    private volatile boolean shouldUpdateUI = false;

    private boolean showMissedCallsOnly = false;

    private void initDefaultDialerLauncher() {
        defaultDialerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        DefaultDialerUtils.handleDefaultDialerResult(requireActivity(), result.getResultCode(),
                                new DefaultDialerUtils.DefaultDialerCallback() {
                                    @Override
                                    public void onDefaultDialerSet() {
                                    }
                                });
                    }
                }
        );
    }

    private void requestDefaultDialer() {
        DefaultDialerUtils.requestDefaultDialer(requireActivity(), defaultDialerLauncher);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRecentBinding.inflate(inflater, container, false);
        init();
        setupPhoneStateListener();
        return binding.getRoot();
    }

    private void init() {
        binding.btnSearch.setOnClickListener(this);
        binding.ivCloseSearch.setOnClickListener(this);
        binding.btnAll.setOnClickListener(this);
        binding.btnMissedCall.setOnClickListener(this);

        initDefaultDialerLauncher();

        binding.rvRecents.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CallLogAdapter(requireContext(), callLogs, new OnItemClickListener() {
            @Override
            public void onClick(String number, int position) {
                deleteCallLog(number);
            }
        }, new CallListener() {
            @Override
            public void onCall(String phoneNumber) {
                makePhoneCall(phoneNumber);
            }
        });
        binding.rvRecents.setAdapter(adapter);

        binding.rvMissedCalls.setLayoutManager(new LinearLayoutManager(requireContext()));
        missedCallAdapter = new CallLogAdapter(requireContext(), missedCallLogs, new OnItemClickListener() {
            @Override
            public void onClick(String number, int position) {
                deleteCallLog(number);
            }
        }, new CallListener() {
            @Override
            public void onCall(String phoneNumber) {
                makePhoneCall(phoneNumber);
            }
        });
        binding.rvMissedCalls.setAdapter(missedCallAdapter);

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_CALL_LOG) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.READ_CALL_LOG, Manifest.permission.WRITE_CALL_LOG, Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS}, 100);
        } else {
            loadCallLogsOptimized(true);
        }

        updateButtonStyles();
    }

    private void initListener() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                currentSearchQuery = query;
                if (isAdded() && binding != null) {
                    if (query.isEmpty()) {
                        if (showMissedCallsOnly) {
                            loadMissedCallsData();
                        } else {
                            updateAllCallsUI(callLogs);
                        }
                    } else {
                        if (showMissedCallsOnly) {
                            performSearchMissedCalls(query);
                        } else {
                            performSearchAllCalls(query);
                        }
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnSearch) {
            binding.clSearch.setVisibility(View.VISIBLE);
        } else if (id == R.id.ivCloseSearch) {
            binding.etSearch.setText("");
            binding.etSearch.clearFocus();
            hideKeyboard();
            binding.clSearch.setVisibility(View.GONE);
        } else if (id == R.id.btnAll) {
            showMissedCallsOnly = false;
            updateButtonStyles();
            showAllCallsView();
            if (currentSearchQuery.isEmpty()) {
                updateAllCallsUI(callLogs);
            } else {
                performSearchAllCalls(currentSearchQuery);
            }
        } else if (id == R.id.btnMissedCall) {
            showMissedCallsOnly = true;
            updateButtonStyles();
            showMissedCallsView();

            if (currentSearchQuery.isEmpty()) {
                loadMissedCallsData();
            } else {
                loadMissedCallsDataThenSearch(currentSearchQuery);
            }
        }
    }

    private void updateButtonStyles() {
        if (binding == null) return;

        if (!showMissedCallsOnly) {
            binding.btnAll.setBackgroundResource(R.drawable.bg_btn);
            binding.btnAll.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));

            binding.btnMissedCall.setBackgroundResource(R.drawable.bg_main);
            binding.btnMissedCall.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_font));
        } else {
            binding.btnAll.setBackgroundResource(R.drawable.bg_main);
            binding.btnAll.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_font));

            binding.btnMissedCall.setBackgroundResource(R.drawable.bg_btn);
            binding.btnMissedCall.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        }
    }

    private void showAllCallsView() {
        binding.rvRecents.setVisibility(View.VISIBLE);
        binding.rvMissedCalls.setVisibility(View.GONE);
    }

    private void showMissedCallsView() {
        binding.rvRecents.setVisibility(View.GONE);
        binding.rvMissedCalls.setVisibility(View.VISIBLE);
    }

    private void loadMissedCallsData() {
        backgroundExecutor.execute(() -> {
            try {
                if (!isAdded() || getContext() == null) return;

                missedCallLogs.clear();
                for (Map.Entry<String, ArrayList<CallLogItem>> entry : callLogs.entrySet()) {
                    String dateKey = entry.getKey();
                    ArrayList<CallLogItem> originalItems = entry.getValue();
                    ArrayList<CallLogItem> missedItems = new ArrayList<>();

                    for (CallLogItem item : originalItems) {
                        if ("Missed Call".equals(item.getCallType())) {
                            missedItems.add(item);
                        }
                    }

                    if (!missedItems.isEmpty()) {
                        missedCallLogs.put(dateKey, missedItems);
                    }
                }

                updateMissedCallsUI(missedCallLogs);
            } catch (Exception e) {
                e.printStackTrace();
                Activity activity = getActivity();
                if (activity != null && !activity.isFinishing()) {
                    activity.runOnUiThread(() -> {
                        if (isAdded() && binding != null) {
                            binding.progress.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }

    private void loadMissedCallsDataThenSearch(String searchQuery) {
        backgroundExecutor.execute(() -> {
            try {
                if (!isAdded() || getContext() == null) return;

                missedCallLogs.clear();
                for (Map.Entry<String, ArrayList<CallLogItem>> entry : callLogs.entrySet()) {
                    String dateKey = entry.getKey();
                    ArrayList<CallLogItem> originalItems = entry.getValue();
                    ArrayList<CallLogItem> missedItems = new ArrayList<>();

                    for (CallLogItem item : originalItems) {
                        if ("Missed Call".equals(item.getCallType())) {
                            missedItems.add(item);
                        }
                    }

                    if (!missedItems.isEmpty()) {
                        missedCallLogs.put(dateKey, missedItems);
                    }
                }

                Map<String, ArrayList<CallLogItem>> filteredGroupedData = new LinkedHashMap<>();

                for (Map.Entry<String, ArrayList<CallLogItem>> entry : missedCallLogs.entrySet()) {
                    String dateHeader = entry.getKey();
                    ArrayList<CallLogItem> filteredItems = new ArrayList<>();

                    for (CallLogItem log : entry.getValue()) {
                        String name = log.getName();
                        String number = log.getNumber();
                        if ((name != null && name.toLowerCase().contains(searchQuery.toLowerCase())) ||
                                (number != null && number.contains(searchQuery))) {
                            filteredItems.add(log);
                        }
                    }

                    if (!filteredItems.isEmpty()) {
                        filteredGroupedData.put(dateHeader, filteredItems);
                    }
                }

                updateMissedCallsUI(filteredGroupedData);

            } catch (Exception e) {
                e.printStackTrace();
                Activity activity = getActivity();
                if (activity != null && !activity.isFinishing()) {
                    activity.runOnUiThread(() -> {
                        if (isAdded() && binding != null) {
                            binding.progress.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            View currentFocus = requireActivity().getCurrentFocus();
            imm.hideSoftInputFromWindow(Objects.requireNonNullElseGet(currentFocus, () -> binding.etSearch).getWindowToken(), 0);
        }
    }

    private void setupPhoneStateListener() {
        telephonyManager = (TelephonyManager) requireContext().getSystemService(Context.TELEPHONY_SERVICE);

        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String phoneNumber) {
                super.onCallStateChanged(state, phoneNumber);

                switch (state) {
                    case TelephonyManager.CALL_STATE_RINGING:
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        wasInCall = true;
                        break;

                    case TelephonyManager.CALL_STATE_IDLE:
                        if (wasInCall) {
                            wasInCall = false;
                            new Handler().postDelayed(() -> {
                                if (isAdded() && binding != null) {
                                    updateCallLogsOptimized(false);
                                }
                            }, 200);
                        }
                        break;
                }
            }
        };

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    private void loadCallLogsOptimized(boolean isProgressVisible) {
        if (isLoadingData) return;

        isLoadingData = true;
        isBackgroundLoadingComplete.set(false);
        shouldUpdateUI = false;

        if (isProgressVisible) {
            binding.progress.setVisibility(View.VISIBLE);
        }

        Context context = getContext();
        if (context == null) return;

        backgroundExecutor.execute(() -> {
            try {
                if (!isAdded() || getContext() == null) return;

                callLogs = CallLogHelper.getCallLogsWithLimit(context, INITIAL_LOAD_SIZE, 0);

                if (binding.etSearch.getText().toString().isEmpty()) {
                    if (showMissedCallsOnly) {
                        showMissedCallsView();
                        loadMissedCallsData();
                    } else {
                        showAllCallsView();
                        updateAllCallsUI(callLogs);
                    }
                }

                isLoadingData = false;
                initListener();

                startOptimizedBackgroundLoading(context);
            } catch (Exception e) {
                isLoadingData = false;
                updateUIError();
            }
        });
    }

    private void startOptimizedBackgroundLoading(Context context) {
        backgroundExecutor.execute(() -> {
            try {
                if (!isAdded() || getContext() == null) return;

                int offset = INITIAL_LOAD_SIZE;
                boolean hasMoreData = true;

                startPeriodicUIUpdates();

                while (hasMoreData && isAdded() && getContext() != null) {
                    Map<String, ArrayList<CallLogItem>> batchData = CallLogHelper.getCallLogsWithLimit(context, BATCH_SIZE, offset);

                    if (batchData.isEmpty()) {
                        hasMoreData = false;
                    } else {
                        synchronized (callLogs) {
                            mergeBatchDataOptimized(batchData);
                        }

                        shouldUpdateUI = true;
                        offset += BATCH_SIZE;
                    }

                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                isBackgroundLoadingComplete.set(true);
                shouldUpdateUI = true;

                if (binding.etSearch.getText().toString().isEmpty()) {
                    if (showMissedCallsOnly) {
                        loadMissedCallsData();
                    } else {
                        updateAllCallsUI(new LinkedHashMap<>(callLogs));
                    }
                }

                loadFilteredDataForSearch(context);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                stopPeriodicUIUpdates();
            }
        });
    }

    private void startPeriodicUIUpdates() {
        pendingUIUpdate = new Runnable() {
            @Override
            public void run() {
                if (shouldUpdateUI && !isBackgroundLoadingComplete.get() && isAdded() && binding != null) {
                    if (binding.etSearch.getText().toString().isEmpty()) {
                        synchronized (callLogs) {
                            if (showMissedCallsOnly) {
                                loadMissedCallsData();
                            } else {
                                updateAllCallsUI(new LinkedHashMap<>(callLogs));
                            }
                        }
                    }
                    shouldUpdateUI = false;

                    if (!isBackgroundLoadingComplete.get()) {
                        uiHandler.postDelayed(this, UI_UPDATE_INTERVAL);
                    }
                }
            }
        };

        uiHandler.postDelayed(pendingUIUpdate, UI_UPDATE_INTERVAL);
    }

    private void stopPeriodicUIUpdates() {
        if (pendingUIUpdate != null) {
            uiHandler.removeCallbacks(pendingUIUpdate);
            pendingUIUpdate = null;
        }
    }

    private void mergeBatchDataOptimized(Map<String, ArrayList<CallLogItem>> batchData) {
        for (Map.Entry<String, ArrayList<CallLogItem>> entry : batchData.entrySet()) {
            String dateKey = entry.getKey();
            ArrayList<CallLogItem> batchItems = entry.getValue();

            if (callLogs.containsKey(dateKey)) {
                ArrayList<CallLogItem> existingItems = callLogs.get(dateKey);
                if (existingItems != null) {
                    for (CallLogItem batchItem : batchItems) {
                        boolean merged = false;

                        int startIndex = Math.max(0, existingItems.size() - 3);
                        for (int i = existingItems.size() - 1; i >= startIndex; i--) {
                            if (canMergeItems(existingItems.get(i), batchItem)) {
                                existingItems.get(i).setCount(existingItems.get(i).getCount() + batchItem.getCount());
                                merged = true;
                                break;
                            }
                        }

                        if (!merged) {
                            existingItems.add(batchItem);
                        }
                    }
                }
            } else {
                callLogs.put(dateKey, new ArrayList<>(batchItems));
            }
        }
    }

    private void updateCallLogsOptimized(boolean isProgressVisible) {
        if (isLoadingData) return;

        isLoadingData = true;
        if (isProgressVisible) {
            binding.progress.setVisibility(View.VISIBLE);
        }

        Context context = getContext();
        if (context == null) return;

        backgroundExecutor.execute(() -> {
            try {
                if (!isAdded() || getContext() == null) return;
                callLogs = CallLogHelper.getCallLogs(context);
                isLoadingData = false;
                if (binding.etSearch.getText().toString().isEmpty()) {
                    if (showMissedCallsOnly) {
                        loadMissedCallsData();
                    } else {
                        updateAllCallsUI(callLogs);
                    }
                }
                loadFilteredDataForSearch(context);
            } catch (Exception e) {
                isLoadingData = false;
                updateUIError();
            }
        });
    }

    private void loadFilteredDataForSearch(Context context) {
        if (!isAdded() || getContext() == null) return;

        backgroundExecutor.execute(() -> {
            try {
                if (!isAdded() || getContext() == null) return;

                filteredLogs = CallLogHelper.getFilterCallLogs(context);

                currentSearchQuery = binding.etSearch.getText().toString();
                if (!currentSearchQuery.isEmpty()) {
                    if (showMissedCallsOnly) {
                        performSearchMissedCalls(currentSearchQuery);
                    } else {
                        performSearchAllCalls(currentSearchQuery);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private boolean canMergeItems(CallLogItem item1, CallLogItem item2) {
        return item1.getCallType().equals(item2.getCallType()) &&
                normalizeNumber(item1.getNumber()).equals(normalizeNumber(item2.getNumber()));
    }

    private String normalizeNumber(String number) {
        if (number == null) return "";
        return number.replaceAll("[^0-9]", "").replaceFirst("^91", "");
    }

    private void updateAllCallsUI(Map<String, ArrayList<CallLogItem>> data) {
        Activity activity = getActivity();
        if (activity != null && !activity.isFinishing()) {
            activity.runOnUiThread(() -> {
                if (isAdded() && binding != null) {
                    adapter.updateDataOptimized(data);
                    toggleEmptyState(data);
                }
            });
        }
    }

    private void updateMissedCallsUI(Map<String, ArrayList<CallLogItem>> data) {
        Activity activity = getActivity();
        if (activity != null && !activity.isFinishing()) {
            activity.runOnUiThread(() -> {
                if (isAdded() && binding != null) {
                    binding.progress.setVisibility(View.GONE);
                    missedCallAdapter.updateDataOptimized(data);
                    toggleEmptyState(data);
                }
            });
        }
    }

    private void performSearchAllCalls(String query) {
        backgroundExecutor.execute(() -> {
            try {
                if (!isAdded() || getContext() == null) return;

                Map<String, ArrayList<CallLogItem>> searchData = filteredLogs.isEmpty() ? callLogs : filteredLogs;
                Map<String, ArrayList<CallLogItem>> filteredGroupedData = new LinkedHashMap<>();

                for (Map.Entry<String, ArrayList<CallLogItem>> entry : searchData.entrySet()) {
                    String dateHeader = entry.getKey();
                    ArrayList<CallLogItem> filteredItems = new ArrayList<>();

                    for (CallLogItem log : entry.getValue()) {
                        String name = log.getName();
                        String number = log.getNumber();
                        if ((name != null && name.toLowerCase().contains(query.toLowerCase())) ||
                                (number != null && number.contains(query))) {
                            filteredItems.add(log);
                        }
                    }

                    if (!filteredItems.isEmpty()) {
                        filteredGroupedData.put(dateHeader, filteredItems);
                    }
                }

                updateAllCallsUI(filteredGroupedData);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void performSearchMissedCalls(String query) {
        backgroundExecutor.execute(() -> {
            try {
                if (!isAdded() || getContext() == null) return;

                if (missedCallLogs.isEmpty()) {
                    for (Map.Entry<String, ArrayList<CallLogItem>> entry : callLogs.entrySet()) {
                        String dateKey = entry.getKey();
                        ArrayList<CallLogItem> originalItems = entry.getValue();
                        ArrayList<CallLogItem> missedItems = new ArrayList<>();

                        for (CallLogItem item : originalItems) {
                            if ("Missed Call".equals(item.getCallType())) {
                                missedItems.add(item);
                            }
                        }

                        if (!missedItems.isEmpty()) {
                            missedCallLogs.put(dateKey, missedItems);
                        }
                    }
                }

                Map<String, ArrayList<CallLogItem>> filteredGroupedData = new LinkedHashMap<>();

                for (Map.Entry<String, ArrayList<CallLogItem>> entry : missedCallLogs.entrySet()) {
                    String dateHeader = entry.getKey();
                    ArrayList<CallLogItem> filteredItems = new ArrayList<>();

                    for (CallLogItem log : entry.getValue()) {
                        String name = log.getName();
                        String number = log.getNumber();
                        if ((name != null && name.toLowerCase().contains(query.toLowerCase())) ||
                                (number != null && number.contains(query))) {
                            filteredItems.add(log);
                        }
                    }

                    if (!filteredItems.isEmpty()) {
                        filteredGroupedData.put(dateHeader, filteredItems);
                    }
                }

                updateMissedCallsUI(filteredGroupedData);

            } catch (Exception e) {
                e.printStackTrace();
                Activity activity = getActivity();
                if (activity != null && !activity.isFinishing()) {
                    activity.runOnUiThread(() -> {
                        if (isAdded() && binding != null) {
                            binding.progress.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }

    private void updateUIError() {
        Activity activity = getActivity();
        if (activity != null && !activity.isFinishing()) {
            activity.runOnUiThread(() -> {
                if (isAdded() && binding != null) {
                    binding.progress.setVisibility(View.GONE);
                }
            });
        }
    }

    private void deleteCallLog(String phoneNumber) {
        backgroundExecutor.execute(() -> {
            try {
                if (!isAdded() || getContext() == null) return;

                Uri callLogUri = CallLog.Calls.CONTENT_URI;
                ContentResolver resolver = requireContext().getContentResolver();

                Cursor cursor = resolver.query(
                        callLogUri,
                        new String[]{CallLog.Calls._ID, CallLog.Calls.NUMBER},
                        null,
                        null,
                        null
                );

                if (cursor != null) {
                    try {
                        boolean deleted = false;
                        while (cursor.moveToNext()) {
                            String number = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER));

                            if (PhoneNumberUtils.compare(number, phoneNumber)) {
                                String selection = CallLog.Calls.NUMBER + "=?";
                                String[] selectionArgs = new String[]{number};
                                int deletedRows = requireContext().getContentResolver().delete(callLogUri, selection, selectionArgs);
                                if (deletedRows > 0) {
                                    deleted = true;
                                }
                            }
                        }

                        if (deleted) {
                            if (isAdded()) {
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(requireContext(), getString(R.string.call_history_deleted_successfully), Toast.LENGTH_SHORT).show();
                                    callLogs.clear();
                                    filteredLogs.clear();
                                    updateCallLogsOptimized(true);
                                });
                            }
                        }
                    } finally {
                        cursor.close();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void toggleEmptyState(Map<String, ArrayList<CallLogItem>> data) {
        if (!isAdded() || binding == null) return;

        boolean isEmpty = data.isEmpty();
        binding.progress.setVisibility(View.GONE);
        binding.clNoRecents.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

        if (showMissedCallsOnly) {
            binding.rvMissedCalls.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            binding.rvRecents.setVisibility(View.GONE);
        } else {
            binding.rvRecents.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            binding.rvMissedCalls.setVisibility(View.GONE);
        }
    }

    private void makePhoneCall(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return;
        }
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.CALL_PHONE, Manifest.permission.ANSWER_PHONE_CALLS}, REQUEST_CALL_PERMISSION);
            return;
        }
        if (Utils.isDefaultDialer(requireContext())) {
            Utils.callContact(requireContext(), phoneNumber);
        } else {
            requestDefaultDialer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_CALL_LOG) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            new Handler().postDelayed(() -> {
                if (isAdded() && binding != null) {
                    updateCallLogsOptimized(false);
                }
            }, 1000);
        }
        if (!binding.etSearch.getText().toString().isEmpty()) {
            binding.clSearch.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadCallLogsOptimized(false);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopPeriodicUIUpdates();
        if (backgroundExecutor != null && !backgroundExecutor.isShutdown()) {
            backgroundExecutor.shutdown();
        }
    }
}