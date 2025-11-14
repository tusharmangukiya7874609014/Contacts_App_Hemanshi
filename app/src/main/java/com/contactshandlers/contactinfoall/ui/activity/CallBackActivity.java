package com.contactshandlers.contactinfoall.ui.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.adapter.CallHistoryAdapter;
import com.contactshandlers.contactinfoall.adapter.ColorAdapter;
import com.contactshandlers.contactinfoall.adapter.ReminderAdapter;
import com.contactshandlers.contactinfoall.ads.BannerAD;
import com.contactshandlers.contactinfoall.ads.InterstitialAD;
import com.contactshandlers.contactinfoall.ads.NativeAD;
import com.contactshandlers.contactinfoall.databinding.ActivityCallBackBinding;
import com.contactshandlers.contactinfoall.helper.CallHistoryHelper;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.helper.DefaultDialerUtils;
import com.contactshandlers.contactinfoall.helper.LocaleHelper;
import com.contactshandlers.contactinfoall.helper.SharedPreferencesManager;
import com.contactshandlers.contactinfoall.listeners.AdCallback;
import com.contactshandlers.contactinfoall.model.AdActivityData;
import com.contactshandlers.contactinfoall.model.AdsData;
import com.contactshandlers.contactinfoall.receiver.ReminderReceiver;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.listeners.OnColorClickListener;
import com.contactshandlers.contactinfoall.model.CallHistory;
import com.contactshandlers.contactinfoall.model.ColorModel;
import com.contactshandlers.contactinfoall.model.PhoneItem;
import com.contactshandlers.contactinfoall.model.Reminder;
import com.contactshandlers.contactinfoall.room.ReminderViewModel;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CallBackActivity extends BaseActivity implements View.OnClickListener {

    private ActivityCallBackBinding binding;
    private final CallBackActivity activity = CallBackActivity.this;
    private Calendar calendar;
    private String phoneNumber;
    private String contactName;
    private String contactId;
    private boolean isBlocked;
    private CallHistoryAdapter adapter;
    private ReminderAdapter reminderAdapter;
    private ReminderViewModel reminderViewModel;
    private List<Reminder> reminderList = new ArrayList<>();
    private List<PhoneItem> phoneList = new ArrayList<>();
    private FirebaseDatabase mDatabase;
    private DatabaseReference mDatabaseReference;
    private String reminderColor;
    private boolean isAlreadyLoad = false;
    private String bannerId, nativeId;
    private boolean isAdStart, isShowBanner, isShowNative, isCustomizeId;
    private boolean callBackAdStart;
    private String callBackAdType;
    private boolean isCallBackAdaptiveBanner;
    private String callBackAdaptiveBannerId;
    private String callBackNativeId;
    private ActivityResultLauncher<Intent> defaultDialerLauncher;

    private void initDefaultDialerLauncher() {
        defaultDialerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        DefaultDialerUtils.handleDefaultDialerResult(activity, result.getResultCode(),
                                new DefaultDialerUtils.DefaultDialerCallback() {
                                    @Override
                                    public void onDefaultDialerSet() {
                                        setBlockStatus();
                                    }
                                });
                    }
                }
        );
    }

    private void requestDefaultDialer() {
        DefaultDialerUtils.requestDefaultDialer(activity, defaultDialerLauncher);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityCallBackBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Utils.setStatusBarColor(activity);

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_CALL_LOG) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_CALL_LOG, Manifest.permission.WRITE_CALL_LOG, Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS}, 101);
        } else {
            initListeners();
            init();
        }
    }

    private void init() {
        initDefaultDialerLauncher();

        phoneNumber = getIntent().getStringExtra(Constants.PHONE_NUMBER);

        String type = Utils.getPhoneTypeByNumber(activity, phoneNumber);
        phoneList.add(new PhoneItem(phoneNumber, type));

        if (BannerAD.isBannerStarted()) {
            boolean isAdStart = SharedPreferencesManager.getInstance().getBooleanValue(Constants.CALL_BACK_ACTIVITY_AD_START, false);
            String adType = SharedPreferencesManager.getInstance().getStringValue(Constants.CALL_BACK_ACTIVITY_AD_TYPE, "");
            boolean isAdaptiveBanner = SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_CALL_BACK_ADAPTIVE_BANNER, false);
            String adaptiveBannerId = SharedPreferencesManager.getInstance().getStringValue(Constants.CALL_BACK_ADAPTIVE_BANNER_ID, "");
            String nativeId = SharedPreferencesManager.getInstance().getStringValue(Constants.CALL_BACK_NATIVE_ID, "");

            if (isAdStart) {
                if (isAdaptiveBanner) {
                    BannerAD.getInstance().showBannerAd(activity, binding.adLayout.llBanner, binding.adLayout.llBannerLayout, binding.adLayout.shimmerBanner, adaptiveBannerId);
                } else {
                    ShimmerFrameLayout shimmer = getShimmerFrameLayout(adType);
                    NativeAD.getInstance().showNativeAd(activity, binding.adLayout.flNativePlaceHolder, binding.adLayout.llNativeLayout, shimmer, adType, nativeId);
                }
            }
        } else {
            getAndSet();
        }

        setBlockStatus();

        if (phoneNumber != null) {
            binding.tvPhoneNumber.setText(phoneNumber);
            contactName = Utils.getContactNameByNumber(activity, phoneNumber);
            binding.tvContactName.setText(contactName);
            if (contactName != null && !contactName.isEmpty()) {
                contactId = Utils.getContactIdByName(activity, contactName);
                if (contactId != null && !contactId.isEmpty()) {
                    Bitmap contactUri = Utils.getContactPhoto(activity, contactId);
                    if (contactUri != null) {
                        Glide.with(activity).load(contactUri).into(binding.ivProfile);
                    }
                }
            } else {
                binding.tvContactName.setText(getString(R.string.unknown_caller));
            }
        }

        List<CallHistory> callHistory = CallHistoryHelper.getCallLogsForContact(activity, phoneNumber, 5);

        long time = callHistory.isEmpty() ? 0 : callHistory.get(0).getDurationInSeconds();
        if (!callHistory.isEmpty()) {
            if (!callHistory.get(0).getCallType().equals("Missed Call")) {
                binding.tvDuration.setText(formatDuration(time));
            }
        }

        binding.rvHistory.setLayoutManager(new LinearLayoutManager(activity));
        adapter = new CallHistoryAdapter(activity, callHistory);
        binding.rvHistory.setAdapter(adapter);

        binding.rvColor.setLayoutManager(new GridLayoutManager(activity, 6));
        List<ColorModel> colorList = new ArrayList<>();
        colorList.add(new ColorModel("#3F569B"));
        colorList.add(new ColorModel("#D84E3C"));
        colorList.add(new ColorModel("#1AA0D7"));
        colorList.add(new ColorModel("#7F9346"));
        colorList.add(new ColorModel("#773CD8"));
        colorList.add(new ColorModel("#D83CC8"));
        ColorAdapter colorAdapter = new ColorAdapter(activity, colorList, new OnColorClickListener() {
            @Override
            public void OnClick(String color) {
                reminderColor = color;
            }
        });
        binding.rvColor.setAdapter(colorAdapter);

        binding.rvReminders.setLayoutManager(new LinearLayoutManager(activity));
        reminderAdapter = new ReminderAdapter(activity, reminderList, new ReminderAdapter.OnDeleteClickListener() {
            @Override
            public void onDelete(Reminder reminder) {
                reminderViewModel.delete(reminder);
                cancelReminder(activity, reminder.getTimestamp());
            }
        });
        binding.rvReminders.setAdapter(reminderAdapter);

        reminderViewModel = new ViewModelProvider(this).get(ReminderViewModel.class);
        reminderViewModel.getAllReminders(phoneNumber).observe(this, new Observer<List<Reminder>>() {
            @Override
            public void onChanged(List<Reminder> reminders) {
                reminderAdapter.setReminders(reminders);
            }
        });

        binding.btnList.performClick();

        disableNumberPickerEditText(binding.npHour);
        disableNumberPickerEditText(binding.npMinute);
    }

    private void setBlockStatus() {
        if (Utils.isDefaultDialer(activity)) {
            isBlocked = Utils.hasAnyBlocked(activity, phoneList);
            if (isBlocked) {
                binding.tvBlock.setText(getString(R.string.unblock));
            } else {
                binding.tvBlock.setText(getString(R.string.block));
            }
        }
    }

    private void initListeners() {
        binding.btnCall.setOnClickListener(this);

        binding.btnList.setOnClickListener(this);
        binding.btnMessage.setOnClickListener(this);
        binding.btnReminder.setOnClickListener(this);
        binding.btnMore.setOnClickListener(this);

        binding.ll1.setOnClickListener(this);
        binding.ll2.setOnClickListener(this);
        binding.ll3.setOnClickListener(this);
        binding.ll4.setOnClickListener(this);

        binding.ivSend1.setOnClickListener(this);
        binding.ivSend2.setOnClickListener(this);
        binding.ivSend3.setOnClickListener(this);
        binding.ivSend4.setOnClickListener(this);

        binding.btnSendCustom.setOnClickListener(this);

        binding.btnCreateReminder.setOnClickListener(this);
        binding.btnTomorrow.setOnClickListener(this);
        binding.btnToday.setOnClickListener(this);
        binding.btnSave.setOnClickListener(this);
        binding.btnCancel.setOnClickListener(this);

        binding.btnViewContact.setOnClickListener(this);
        binding.btnSendMessage.setOnClickListener(this);
        binding.btnBlock.setOnClickListener(this);
        binding.btnWhatsapp.setOnClickListener(this);

        binding.viewDisable.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnCall) {
            if (Utils.isDefaultDialer(activity)) {
                Utils.callContact(activity, phoneNumber);
                finish();
            } else {
                requestDefaultDialer();
            }
        } else if (id == R.id.btnList) {
            binding.btnList.setBackground(getDrawable(R.drawable.bg_btn));
            binding.btnMessage.setBackground(getDrawable(R.drawable.bg_main));
            binding.btnReminder.setBackground(getDrawable(R.drawable.bg_main));
            binding.btnMore.setBackground(getDrawable(R.drawable.bg_main));
            binding.clList.setVisibility(View.VISIBLE);
            binding.clMessage.setVisibility(View.GONE);
            binding.clReminder.setVisibility(View.GONE);
            binding.clMore.setVisibility(View.GONE);
            binding.btnList.setImageTintList(ColorStateList.valueOf(getColor(R.color.white)));
            binding.btnMessage.setImageTintList(ColorStateList.valueOf(getColor(R.color.grey_font)));
            binding.btnReminder.setImageTintList(ColorStateList.valueOf(getColor(R.color.grey_font)));
            binding.btnMore.setImageTintList(ColorStateList.valueOf(getColor(R.color.grey_font)));
        } else if (id == R.id.btnMessage) {
            binding.btnList.setBackground(getDrawable(R.drawable.bg_main));
            binding.btnMessage.setBackground(getDrawable(R.drawable.bg_btn));
            binding.btnReminder.setBackground(getDrawable(R.drawable.bg_main));
            binding.btnMore.setBackground(getDrawable(R.drawable.bg_main));
            binding.clList.setVisibility(View.GONE);
            binding.clMessage.setVisibility(View.VISIBLE);
            binding.clReminder.setVisibility(View.GONE);
            binding.clMore.setVisibility(View.GONE);
            binding.btnList.setImageTintList(ColorStateList.valueOf(getColor(R.color.grey_font)));
            binding.btnMessage.setImageTintList(ColorStateList.valueOf(getColor(R.color.white)));
            binding.btnReminder.setImageTintList(ColorStateList.valueOf(getColor(R.color.grey_font)));
            binding.btnMore.setImageTintList(ColorStateList.valueOf(getColor(R.color.grey_font)));
        } else if (id == R.id.btnReminder) {
            binding.btnList.setBackground(getDrawable(R.drawable.bg_main));
            binding.btnMessage.setBackground(getDrawable(R.drawable.bg_main));
            binding.btnReminder.setBackground(getDrawable(R.drawable.bg_btn));
            binding.btnMore.setBackground(getDrawable(R.drawable.bg_main));
            binding.clList.setVisibility(View.GONE);
            binding.clMessage.setVisibility(View.GONE);
            binding.clReminder.setVisibility(View.VISIBLE);
            binding.clMore.setVisibility(View.GONE);
            binding.btnList.setImageTintList(ColorStateList.valueOf(getColor(R.color.grey_font)));
            binding.btnMessage.setImageTintList(ColorStateList.valueOf(getColor(R.color.grey_font)));
            binding.btnReminder.setImageTintList(ColorStateList.valueOf(getColor(R.color.white)));
            binding.btnMore.setImageTintList(ColorStateList.valueOf(getColor(R.color.grey_font)));
        } else if (id == R.id.btnMore) {
            binding.btnList.setBackground(getDrawable(R.drawable.bg_main));
            binding.btnMessage.setBackground(getDrawable(R.drawable.bg_main));
            binding.btnReminder.setBackground(getDrawable(R.drawable.bg_main));
            binding.btnMore.setBackground(getDrawable(R.drawable.bg_btn));
            binding.clList.setVisibility(View.GONE);
            binding.clMessage.setVisibility(View.GONE);
            binding.clReminder.setVisibility(View.GONE);
            binding.clMore.setVisibility(View.VISIBLE);
            binding.btnList.setImageTintList(ColorStateList.valueOf(getColor(R.color.grey_font)));
            binding.btnMessage.setImageTintList(ColorStateList.valueOf(getColor(R.color.grey_font)));
            binding.btnReminder.setImageTintList(ColorStateList.valueOf(getColor(R.color.grey_font)));
            binding.btnMore.setImageTintList(ColorStateList.valueOf(getColor(R.color.white)));
        } else if (id == R.id.ll1) {
            binding.iv1.setImageResource(R.drawable.ic_select);
            binding.iv2.setImageResource(R.drawable.ic_unselect);
            binding.iv3.setImageResource(R.drawable.ic_unselect);
            binding.iv4.setImageResource(R.drawable.ic_unselect);
            binding.ivSend1.setVisibility(View.VISIBLE);
            binding.ivSend2.setVisibility(View.GONE);
            binding.ivSend3.setVisibility(View.GONE);
            binding.ivSend4.setVisibility(View.GONE);
        } else if (id == R.id.ll2) {
            binding.iv1.setImageResource(R.drawable.ic_unselect);
            binding.iv2.setImageResource(R.drawable.ic_select);
            binding.iv3.setImageResource(R.drawable.ic_unselect);
            binding.iv4.setImageResource(R.drawable.ic_unselect);
            binding.ivSend1.setVisibility(View.GONE);
            binding.ivSend2.setVisibility(View.VISIBLE);
            binding.ivSend3.setVisibility(View.GONE);
            binding.ivSend4.setVisibility(View.GONE);
        } else if (id == R.id.ll3) {
            binding.iv1.setImageResource(R.drawable.ic_unselect);
            binding.iv2.setImageResource(R.drawable.ic_unselect);
            binding.iv3.setImageResource(R.drawable.ic_select);
            binding.iv4.setImageResource(R.drawable.ic_unselect);
            binding.ivSend1.setVisibility(View.GONE);
            binding.ivSend2.setVisibility(View.GONE);
            binding.ivSend3.setVisibility(View.VISIBLE);
            binding.ivSend4.setVisibility(View.GONE);
        } else if (id == R.id.ll4) {
            binding.iv1.setImageResource(R.drawable.ic_unselect);
            binding.iv2.setImageResource(R.drawable.ic_unselect);
            binding.iv3.setImageResource(R.drawable.ic_unselect);
            binding.iv4.setImageResource(R.drawable.ic_select);
            binding.ivSend1.setVisibility(View.GONE);
            binding.ivSend2.setVisibility(View.GONE);
            binding.ivSend3.setVisibility(View.GONE);
            binding.ivSend4.setVisibility(View.VISIBLE);
        } else if (id == R.id.ivSend1) {
            sendMessage(getString(R.string.can_t_talk_right_now));
        } else if (id == R.id.ivSend2) {
            sendMessage(getString(R.string.i_ll_call_you_later));
        } else if (id == R.id.ivSend3) {
            sendMessage(getString(R.string.i_m_on_my_way));
        } else if (id == R.id.ivSend4) {
            sendMessage(getString(R.string.can_t_talk_right_now_call_me_later));
        } else if (id == R.id.btnSendCustom) {
            String message = binding.etMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
            } else {
                binding.etMessage.setError(getString(R.string.write_personal_message));
                binding.etMessage.requestFocus();
            }
        } else if (id == R.id.btnCreateReminder) {
            binding.llCreate.setVisibility(View.GONE);
            binding.clCreateReminder.setVisibility(View.VISIBLE);

            calendar = Calendar.getInstance();
            binding.npHour.setMinValue(0);
            binding.npHour.setMaxValue(23);
            binding.npHour.setValue(calendar.get(Calendar.HOUR_OF_DAY));

            binding.npMinute.setMinValue(0);
            binding.npMinute.setMaxValue(59);
            binding.npMinute.setValue(calendar.get(Calendar.MINUTE));

            binding.npHour.setOnValueChangedListener((picker, oldVal, newVal) -> {
                calendar.set(Calendar.HOUR_OF_DAY, newVal);
            });

            binding.npMinute.setOnValueChangedListener((picker, oldVal, newVal) -> {
                calendar.set(Calendar.MINUTE, newVal);
            });

            binding.btnToday.performClick();
        } else if (id == R.id.btnTomorrow) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            binding.btnTomorrow.setBackground(getDrawable(R.drawable.bg_main));
            binding.btnToday.setBackground(null);
        } else if (id == R.id.btnToday) {
            calendar.set(Calendar.DAY_OF_MONTH, Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
            binding.btnTomorrow.setBackground(null);
            binding.btnToday.setBackground(getDrawable(R.drawable.bg_main));
        } else if (id == R.id.btnSave) {
            int selectedHour = binding.npHour.getValue();
            int selectedMinute = binding.npMinute.getValue();

            calendar.set(Calendar.HOUR_OF_DAY, selectedHour);
            calendar.set(Calendar.MINUTE, selectedMinute);

            long selectedTimeMillis = calendar.getTimeInMillis();

            if (selectedTimeMillis <= System.currentTimeMillis()) {
                Toast.makeText(activity, getString(R.string.please_select_a_future_time), Toast.LENGTH_SHORT).show();
                return;
            }

            String reminderMessage = binding.etReminder.getText().toString().trim();
            if (reminderMessage.isEmpty()) {
                binding.etReminder.setError(getString(R.string.remind_me_about));
                binding.etReminder.requestFocus();
                return;
            }

            String selectedColor = reminderColor;
            String title;
            if (contactName != null && !contactName.isEmpty()) {
                title = contactName;
            } else {
                title = phoneNumber;
            }
            saveReminder(reminderMessage, selectedTimeMillis, selectedColor, title);
        } else if (id == R.id.btnCancel) {
            binding.llCreate.setVisibility(View.VISIBLE);
            binding.clCreateReminder.setVisibility(View.GONE);
        } else if (id == R.id.btnViewContact) {
            if (contactId != null && !contactId.isEmpty()) {
                InterstitialAD.getInstance().showInterstitial(activity, new AdCallback() {
                    @Override
                    public void callbackCall() {
                        Intent intent = new Intent(activity, ViewContactActivity.class);
                        intent.putExtra(Constants.CONTACT_ID, contactId);
                        startActivity(intent);
                        finish();
                    }
                });
            }
        } else if (id == R.id.btnSendMessage) {
            sendMessage("");
        } else if (id == R.id.btnBlock) {
            blockUnblockContact();
        } else if (id == R.id.btnWhatsapp) {
            openWhatsApp(phoneNumber);
        }
    }

    private ShimmerFrameLayout getShimmerFrameLayout(String type) {
        ShimmerFrameLayout shimmer = null;
        if (type.equalsIgnoreCase("large")) {
            shimmer = binding.adLayout.shimmerNativeLarge;
        } else if (type.equalsIgnoreCase("medium")) {
            shimmer = binding.adLayout.shimmerNativeMedium;
        } else if (type.equalsIgnoreCase("small")) {
            shimmer = binding.adLayout.shimmerNativeSmall;
        }
        return shimmer;
    }

    @SuppressLint("DefaultLocale")
    private static String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format("%02d:%02d", minutes, secs);
        }
    }

    private void disableNumberPickerEditText(NumberPicker numberPicker) {
        try {
            for (int i = 0; i < numberPicker.getChildCount(); i++) {
                View child = numberPicker.getChildAt(i);
                if (child instanceof EditText) {
                    EditText editText = (EditText) child;

                    editText.setFocusable(false);
                    editText.setFocusableInTouchMode(false);
                    editText.setClickable(false);
                    editText.setLongClickable(false);
                    editText.setCursorVisible(false);
                    editText.setKeyListener(null);

                    editText.setOnClickListener(v -> {
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String message) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("smsto:" + phoneNumber));
        intent.putExtra("sms_body", message);

        try {
            startActivity(intent);
            finish();
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, getString(R.string.no_messaging_app_found), Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    public static void setReminder(Context context, long timeInMillis, String message, long reminderId, String title) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(Constants.REMINDER_MESSAGE, message);
        intent.putExtra(Constants.REMINDER_ID, reminderId);
        intent.putExtra(Constants.TITLE, title);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (int) reminderId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
            } else {
                if (context instanceof Activity) {
                    Intent alarmPermissionIntent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    context.startActivity(alarmPermissionIntent);
                }
            }
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
        }
    }

    private void getAndSet() {
        if (isAlreadyLoad) {
            return;
        }
        binding.adLayout.shimmerNativeLarge.setVisibility(View.VISIBLE);
        if (Utils.isNetworkConnected(activity)) {
            mDatabase = FirebaseDatabase.getInstance();
            mDatabaseReference = mDatabase.getReference("ads_data");

            mDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    AdsData adsData = snapshot.getValue(AdsData.class);
                    if (adsData != null) {
                        bannerId = adsData.getBannerId();
                        nativeId = adsData.getNativeId();

                        isAdStart = adsData.isAdStart();
                        isShowBanner = adsData.isShowBanner();
                        isShowNative = adsData.isShowNative();
                        isCustomizeId = adsData.isCustomizeId();

                        AdActivityData callBackActivity = adsData.getCallBackActivity();

                        if (callBackActivity != null) {
                            callBackAdStart = callBackActivity.isAdStart();
                            callBackAdType = callBackActivity.getAdType();
                            isCallBackAdaptiveBanner = callBackActivity.isAdaptiveBanner();
                            callBackAdaptiveBannerId = callBackActivity.getAdaptiveBannerId();
                            callBackNativeId = callBackActivity.getNativeId();
                            SharedPreferencesManager.getInstance().setBooleanValue(Constants.CALL_BACK_ACTIVITY_AD_START, callBackAdStart);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.CALL_BACK_ACTIVITY_AD_TYPE, callBackAdType);
                            SharedPreferencesManager.getInstance().setBooleanValue(Constants.IS_CALL_BACK_ADAPTIVE_BANNER, isCallBackAdaptiveBanner);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.CALL_BACK_NATIVE_ID, callBackNativeId);
                            SharedPreferencesManager.getInstance().setStringValue(Constants.CALL_BACK_ADAPTIVE_BANNER_ID, callBackAdaptiveBannerId);
                        }

                        if (isAdStart) {
                            MobileAds.initialize(activity);

                            BannerAD.getInstance().init(activity, isShowBanner, isCustomizeId, bannerId);
                            NativeAD.getInstance().init(activity, isShowNative, isCustomizeId, nativeId);

                            boolean isAdStart = SharedPreferencesManager.getInstance().getBooleanValue(Constants.CALL_BACK_ACTIVITY_AD_START, false);
                            String adType = SharedPreferencesManager.getInstance().getStringValue(Constants.CALL_BACK_ACTIVITY_AD_TYPE, "");
                            boolean isAdaptiveBanner = SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_CALL_BACK_ADAPTIVE_BANNER, false);
                            String adaptiveBannerId = SharedPreferencesManager.getInstance().getStringValue(Constants.CALL_BACK_ADAPTIVE_BANNER_ID, "");
                            String nativeId = SharedPreferencesManager.getInstance().getStringValue(Constants.CALL_BACK_NATIVE_ID, "");

                            if (isAdStart) {
                                if (isAdaptiveBanner) {
                                    BannerAD.getInstance().showBannerAd(activity, binding.adLayout.llBanner, binding.adLayout.llBannerLayout, binding.adLayout.shimmerBanner, adaptiveBannerId);
                                } else {
                                    ShimmerFrameLayout shimmer = getShimmerFrameLayout(adType);
                                    NativeAD.getInstance().showNativeAd(activity, binding.adLayout.flNativePlaceHolder, binding.adLayout.llNativeLayout, shimmer, adType, nativeId);
                                }
                            }
                        } else {
                            binding.adLayout.shimmerNativeLarge.setVisibility(View.GONE);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    binding.adLayout.shimmerNativeLarge.setVisibility(View.GONE);
                }
            });
        } else {
            binding.adLayout.shimmerNativeLarge.setVisibility(View.GONE);
        }
        isAlreadyLoad = true;
    }

    public static void cancelReminder(Context context, long reminderId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (int) reminderId, intent, PendingIntent.FLAG_IMMUTABLE);

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    private void saveReminder(String message, long timeInMillis, String color, String title) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            startActivity(intent);
            return;
        }

        Reminder reminder = new Reminder(System.currentTimeMillis(), contactName, phoneNumber, message, timeInMillis, color);
        reminderViewModel.insert(reminder);
        setReminder(getApplicationContext(), timeInMillis, message, reminder.getTimestamp(), title);

        Toast.makeText(activity, getString(R.string.reminder_saved), Toast.LENGTH_SHORT).show();
        finish();
    }

    private void blockUnblockContact() {
        if (Utils.isDefaultDialer(activity)) {
            if (isBlocked) {
                Utils.unblockAllNumbers(activity, phoneList);
                binding.tvBlock.setText(getString(R.string.block));
                Toast.makeText(activity, getString(R.string.unblocked), Toast.LENGTH_SHORT).show();
            } else {
                Utils.blockAllNumbers(activity, phoneList);
                binding.tvBlock.setText(getString(R.string.unblock));
                Toast.makeText(activity, getString(R.string.blocked), Toast.LENGTH_SHORT).show();
            }
            isBlocked = !isBlocked;
        } else {
            requestDefaultDialer();
        }
    }

    private void openWhatsApp(String phoneNumber) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://wa.me/" + phoneNumber));
            intent.setPackage("com.whatsapp");
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(activity, getString(R.string.whatsapp_not_installed), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(activity, getString(R.string.whatsapp_not_installed), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initListeners();
                init();
            } else {
                Toast.makeText(activity, getString(R.string.permission_denied), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NativeAD.getInstance().destroy();
    }
}