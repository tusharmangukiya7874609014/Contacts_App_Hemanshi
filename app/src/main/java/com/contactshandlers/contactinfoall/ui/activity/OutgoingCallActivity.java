package com.contactshandlers.contactinfoall.ui.activity;

import static com.contactshandlers.contactinfoall.helper.CallListHelper.mergedCallsList;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.databinding.ActivityOutgoingCallBinding;
import com.contactshandlers.contactinfoall.databinding.DialogAudioRouteBinding;
import com.contactshandlers.contactinfoall.databinding.ItemCallUserBinding;
import com.contactshandlers.contactinfoall.helper.App;
import com.contactshandlers.contactinfoall.helper.CallListHelper;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.helper.ContactsHelper;
import com.contactshandlers.contactinfoall.helper.LocaleHelper;
import com.contactshandlers.contactinfoall.helper.SharedPreferencesManager;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.receiver.CallActionReceiver;
import com.contactshandlers.contactinfoall.service.CallNotificationService;
import com.contactshandlers.contactinfoall.service.MyInCallService;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class OutgoingCallActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener {

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final Object viewUpdateLock = new Object();
    private static Context context;
    private static ActivityOutgoingCallBinding binding;
    private final OutgoingCallActivity activity = OutgoingCallActivity.this;
    private String phoneNumber, contactName;
    private StringBuilder etNumber = new StringBuilder();
    private Call currentCall;
    private boolean isCallOnHold, isSpeakerOn, isMuted;
    private Call call;
    private int type;
    public static boolean isKeyboard;
    private SensorManager mSensorManager;
    private Sensor mProximity;
    private PowerManager.WakeLock proximityWakeLock;
    private PowerManager powerManager;
    private Dialog dialog;
    private DialogAudioRouteBinding audioRouteBinding;
    private static CircleImageView ivProfile;
    private static TextView tvContactName, tvPhoneNumber, tvStatus;
    private static Chronometer chronometer, conferenceChronometer;
    private static LinearLayout llConference, llContact, btnAddCall;
    private static RecyclerView rvConferenceCallList;
    private static ImageView ivAddCall;
    private static boolean isViewUpdateInProgress = false;
    private AudioManager audioManager;
    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;
    private Handler handler = new Handler(Looper.getMainLooper());
    private final Object callStateLock = new Object();

    private interface CallListAdapterInterface {
        void setList(List<Call> newList);
    }

    public static class CallViewHolder extends RecyclerView.ViewHolder {
        private final ItemCallUserBinding binding;

        public CallViewHolder(ItemCallUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private static class CallListAdapter extends RecyclerView.Adapter<CallViewHolder> implements CallListAdapterInterface {
        private List<Call> calls = new ArrayList<>();

        @NonNull
        @Override
        public CallViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemCallUserBinding binding = ItemCallUserBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new CallViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull CallViewHolder holder, int position) {
            Call call = calls.get(position);
            String number = CallListHelper.getCallNumber(call);

            String name = null;

            if (number != null && !number.trim().isEmpty() && context != null) {
                try {
                    name = ContactsHelper.getContactNameByPhoneNumber(number, context);
                } catch (Exception e) {
                    e.printStackTrace();
                    name = null;
                }
            }

            holder.binding.tvContactName.setText(name != null ? name : (number != null ? number : "Unknown"));

            if (name != null) {
                String contactId = Utils.getContactIdByName(context, name);
                if (contactId != null) {
                    Bitmap contactPhoto = Utils.getContactPhoto(context, contactId);
                    if (context instanceof OutgoingCallActivity) {
                        OutgoingCallActivity activity = (OutgoingCallActivity) context;
                        if (!activity.isDestroyed() && !activity.isFinishing() && contactPhoto != null) {
                            try {
                                Glide.with(context)
                                        .load(contactPhoto)
                                        .circleCrop()
                                        .into(holder.binding.ivProfile);
                            } catch (IllegalArgumentException e) {
                                holder.binding.ivProfile.setImageResource(R.drawable.ic_profile_2);
                            }
                        } else {
                            holder.binding.ivProfile.setImageResource(R.drawable.ic_profile_2);
                        }
                    } else {
                        holder.binding.ivProfile.setImageResource(R.drawable.ic_profile_2);
                    }
                }
            } else {
                holder.binding.ivProfile.setImageResource(R.drawable.ic_profile_2);
            }

            switch (call.getState()) {
                case Call.STATE_ACTIVE:
                    holder.binding.tvStatus.setText("Active");
                    holder.binding.tvStatus.setVisibility(View.GONE);
                    holder.binding.chronometer.setVisibility(View.VISIBLE);
                    holder.binding.chronometer.setBase(SystemClock.elapsedRealtime() - (System.currentTimeMillis() - call.getDetails().getConnectTimeMillis()));
                    holder.binding.chronometer.start();
                    updateNotification();
                    break;
                case Call.STATE_HOLDING:
                    holder.binding.chronometer.stop();
                    holder.binding.chronometer.setVisibility(View.GONE);
                    holder.binding.tvStatus.setVisibility(View.VISIBLE);
                    holder.binding.tvStatus.setText("On Hold");
                    break;
                case Call.STATE_DIALING:
                case Call.STATE_CONNECTING:
                    holder.binding.chronometer.stop();
                    holder.binding.chronometer.setVisibility(View.GONE);
                    holder.binding.tvStatus.setVisibility(View.VISIBLE);
                    holder.binding.tvStatus.setText("Dialing");
                    break;
            }

            setupClickListenersStatic(holder, call);
        }

        @Override
        public int getItemCount() {
            return calls.size();
        }

        @Override
        public void setList(List<Call> newList) {
            calls.clear();
            calls.addAll(newList);
            notifyDataSetChanged();
        }
    }

    private static final CallListAdapter callListAdapter = new CallListAdapter();

    private static void setupClickListenersStatic(CallViewHolder holder, Call call) {
        holder.binding.getRoot().setOnClickListener(v -> {
            if (call.getState() == Call.STATE_HOLDING) {
                toggleHoldStatic(call);
            }
        });

        List<String> mergedCallNumbers = new ArrayList<>();
        for (Call mergedCall : mergedCallsList) {
            String number = CallListHelper.normalizePhoneNumber(CallListHelper.getCallNumber(mergedCall));
            if (number != null && !number.isEmpty()) {
                mergedCallNumbers.add(number);
            }
        }
        boolean isCallMerged = mergedCallNumbers.contains(CallListHelper.normalizePhoneNumber(CallListHelper.getCallNumber(call)));


        holder.binding.ivMerge.setOnClickListener(v -> {
            if (isCallMerged) {
                return;
            }

            List<Call> uniqueCalls = CallListHelper.getUniqueCallList();
            if (uniqueCalls.size() > CallListHelper.MAX_CONFERENCE_PARTICIPANTS) {
                Toast.makeText(context, context.getString(R.string.maximum_conference_participants_reached), Toast.LENGTH_SHORT).show();
                return;
            }

            mergeCalls(call);
        });

        holder.binding.ivSwap.setOnClickListener(v -> {
            if (CallListHelper.canSwapCall(call)) {
                Call heldCall = call;

                Call activeCall = null;
                for (Call c : CallListHelper.getUniqueCallList()) {
                    if (c.getState() == Call.STATE_ACTIVE) {
                        activeCall = c;
                        break;
                    }
                }

                if (activeCall != null && heldCall != null) {

                    CallListHelper.updateSwapNotification(context, heldCall, activeCall);

                    CallListHelper.swapCalls();

                    new Handler().post(() -> {
                        updateCallList(CallListHelper.getUniqueCallList());
                        CallListHelper.CallMode currentMode = CallListHelper.getCurrentCallMode();
                        updateUIForCallModeStatic(currentMode, CallListHelper.getUniqueCallList());
                    });
                } else {
                    CallListHelper.swapCalls();
                    new Handler().postDelayed(() -> updateNotification(), 500);
                }
            }
        });

        holder.binding.ivCallEnd.setOnClickListener(v -> {
            if (call != null) {
                if (CallListHelper.isInConferenceMode()) {
                    CallListHelper.disconnectConferenceParticipant(call);
                } else {
                    call.disconnect();
                }
                CallListHelper.removeCall(call, 6);

                List<Call> remainingCalls = CallListHelper.getUniqueCallList();
                if (remainingCalls.isEmpty()) {
                    if (context != null) {
                        Intent intent = new Intent(context, CallNotificationService.class);
                        context.stopService(intent);
                        if (context instanceof AppCompatActivity) {
                            ((AppCompatActivity) context).finish();
                        }
                    }
                } else {
                    updateCallList(remainingCalls);
                    CallListHelper.CallMode currentMode = CallListHelper.getCurrentCallMode();
                    switch (currentMode) {
                        case CONFERENCE:
                            handleConferenceViewStatic(remainingCalls);
                            break;
                        case MULTI_CALL:
                            handleMultiCallViewStatic(remainingCalls);
                            break;
                        case SINGLE_CALL:
                            handleSingleCallViewStatic(remainingCalls);
                            break;
                    }
                    new Handler().post(() -> updateNotification());
                }
            }
        });

        CallListHelper.CallMode currentMode = CallListHelper.getCurrentCallMode();
        List<String> numbers = new ArrayList<>();
        for (Call regularCall : CallListHelper.getRegularCallList()) {
            if (regularCall.getDetails() != null && regularCall.getDetails().getHandle() != null) {
                numbers.add(regularCall.getDetails().getHandle().getSchemeSpecificPart());
            }
        }

        boolean isInRegularList = CallListHelper.getRegularCallList().contains(call);
        boolean isOnHold = call.getState() == Call.STATE_HOLDING;

        if (currentMode == CallListHelper.CallMode.MULTI_CALL) {
            holder.binding.ivMerge.setVisibility(isOnHold ? View.VISIBLE : View.GONE);
        } else if (currentMode == CallListHelper.CallMode.CONFERENCE) {
            holder.binding.ivMerge.setVisibility(isCallMerged ? View.GONE : View.VISIBLE);
        } else {
            holder.binding.ivMerge.setVisibility(View.GONE);
        }

        if (currentMode == CallListHelper.CallMode.MULTI_CALL) {
            holder.binding.ivSwap.setVisibility(isOnHold ? View.VISIBLE : View.GONE);
        } else if (currentMode == CallListHelper.CallMode.CONFERENCE) {
            holder.binding.ivSwap.setVisibility((isInRegularList && isOnHold) ? View.VISIBLE : View.GONE);
        } else {
            holder.binding.ivSwap.setVisibility(View.GONE);
        }

        holder.binding.ivCallEnd.setVisibility(View.VISIBLE);
    }

    private static void updateAddCallButton() {
        if (binding != null) {
            boolean canAddMoreCalls = CallListHelper.canAddMoreCalls();
            btnAddCall.setEnabled(canAddMoreCalls);
            btnAddCall.setClickable(canAddMoreCalls);
            ivAddCall.setImageTintList(ColorStateList.valueOf(
                    canAddMoreCalls ? Color.WHITE : Color.parseColor("#80FFFFFF")));
        }
    }

    public static void updateButtonStates() {
        updateAddCallButton();
    }

    private BroadcastReceiver closeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("ACTION_CLOSE_CALL_UI".equals(intent.getAction())) {
                finish();
            }
        }
    };

    public static void finishActivity() {
        if (context instanceof OutgoingCallActivity) {
            ((OutgoingCallActivity) context).finish();
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityOutgoingCallBinding.inflate(getLayoutInflater());
        context = this;
        setContentView(binding.getRoot());

        IntentFilter closeFilter = new IntentFilter("ACTION_CLOSE_CALL_UI");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(closeReceiver, closeFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(closeReceiver, closeFilter);
        }

        IntentFilter networkFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(networkStateReceiver, networkFilter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(networkStateReceiver, networkFilter);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        App.AppOpenAdManager.isShowBackOpenAd = false;

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);


        setStatusNavigation();
        initView();
        initListener();
        init();

        setupCallStateListener();
        setupCallList();

        optimizeBatteryUsage();

        setupAudioFocus();
    }

    private void setStatusNavigation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController insetsController = activity.getWindow().getInsetsController();
            if (insetsController != null) {
                insetsController.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
            }
        } else {
            View decorView = activity.getWindow().getDecorView();
            decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                insetsController.hide(WindowInsets.Type.navigationBars());
                insetsController.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        Context context = LocaleHelper.getLocale(newBase);
        super.attachBaseContext(context);
    }

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            String currentText = s.toString();
            if (!currentText.equals(etNumber.toString())) {
                etNumber = new StringBuilder(currentText);
            }
        }
    };

    private boolean hasContactsPermission() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void init() {
        setButtonsDisabled();
        binding.etPhoneNumber.setShowSoftInputOnFocus(false);
        phoneNumber = getIntent().getStringExtra(Constants.PHONE_NUMBER);

        currentCall = MyInCallService.currentCall;
        if (currentCall != null) {
            CallListHelper.handleOutgoingCall(currentCall);
        }

        binding.etPhoneNumber.addTextChangedListener(textWatcher);

        if (phoneNumber != null && !phoneNumber.trim().isEmpty() && hasContactsPermission()) {
            binding.tvPhoneNumber.setText(phoneNumber);
            contactName = Utils.getContactNameByNumber(activity, phoneNumber);
            if (contactName != null && !contactName.isEmpty()) {
                binding.tvContactName.setText(contactName);
                binding.tvContactName.setSelected(true);
                String contactId = Utils.getContactIdByName(activity, contactName);
                if (contactId != null && !contactId.isEmpty()) {
                    Bitmap contactUri = Utils.getContactPhoto(activity, contactId);
                    if (contactUri != null) {
                        Glide.with(activity).load(contactUri).into(binding.ivProfile);
                    } else {
                        binding.ivProfile.setImageResource(R.drawable.ic_profile_2);
                    }
                }
            } else {
                binding.tvContactName.setText(getString(R.string.unknown_caller));
                binding.tvContactName.setSelected(true);
                binding.ivProfile.setImageResource(R.drawable.ic_profile_2);
            }
        } else {
            contactName = null;
        }

        if (getIntent().getBooleanExtra(Constants.IS_INCOMING, false)) {
            listenToIncomingCallState();
        } else {
            listenToCallState();
        }
        checkAudioType();

        if (call != null && call.getDetails() != null && call.getState() == Call.STATE_ACTIVE) {
            long startTimeMillis = call.getDetails().getConnectTimeMillis();

            if (call.getState() == Call.STATE_ACTIVE) {
                setActiveOutGoing();
                binding.chronometer.setBase(SystemClock.elapsedRealtime() - (System.currentTimeMillis() - startTimeMillis));
                binding.chronometer.start();
            }
        }

        //sensor
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null && proximityWakeLock == null) {
            proximityWakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "ContactGroupItem::ProximityWakeLock");
        }
    }

    private void initView() {
        ivProfile = binding.ivProfile;
        tvContactName = binding.tvContactName;
        tvPhoneNumber = binding.tvPhoneNumber;
        tvStatus = binding.tvStatus;
        chronometer = binding.chronometer;
        conferenceChronometer = binding.conferenceChronometer;
        llConference = binding.llConference;
        llContact = binding.llContact;
        btnAddCall = binding.btnAddCall;
        rvConferenceCallList = binding.rvConferenceCallList;
        ivAddCall = binding.ivAddCall;
    }

    private void listenToCallState() {
        call = currentCall;
        if (call != null) {
            call.registerCallback(new Call.Callback() {
                @Override
                public void onStateChanged(Call call, int state) {
                    super.onStateChanged(call, state);
                    switch (state) {
                        case Call.STATE_ACTIVE:
                            if (CallListHelper.isNewCallInProgress()) {
                                CallListHelper.setNewCallInProgress(false);
                                CallListHelper.addCall(call);
                                changeView();
                            }

                            long startTimeMillis = call.getDetails().getConnectTimeMillis();
                            binding.chronometer.setBase(SystemClock.elapsedRealtime() - (System.currentTimeMillis() - startTimeMillis));
                            binding.chronometer.start();

                            setActiveOutGoing();

                            Intent intent = new Intent(activity, CallNotificationService.class);
                            intent.putExtra(Constants.TITLE, "ACTION_UPDATE_NOTIFICATION");
                            intent.putExtra(Constants.PHONE_NUMBER, phoneNumber);
                            intent.putExtra("IS_CALL_RECEIVED", true);
                            startService(intent);

                            break;
                        case Call.STATE_HOLDING:
                            binding.tvStatus.setVisibility(View.VISIBLE);
                            binding.tvStatus.setText("On Hold");
                            break;
                        case Call.STATE_DIALING:
                        case Call.STATE_CONNECTING:
                            binding.tvStatus.setVisibility(View.VISIBLE);
                            binding.chronometer.setVisibility(View.GONE);
                            binding.tvStatus.setText("Dialing...");
                            break;
                        case Call.STATE_NEW:
                            binding.tvStatus.setVisibility(View.VISIBLE);
                            binding.tvStatus.setText("New Call");
                            break;
                        case Call.STATE_SELECT_PHONE_ACCOUNT:
                            binding.tvStatus.setVisibility(View.VISIBLE);
                            binding.tvStatus.setText("Select Phone Account");
                            break;
                        case Call.STATE_DISCONNECTING:
                            binding.tvStatus.setVisibility(View.VISIBLE);
                            binding.tvStatus.setText("Disconnecting...");
                            break;
                        case Call.STATE_DISCONNECTED:
                            if (CallListHelper.isCallListEmpty()) {
                                binding.chronometer.stop();
                                binding.chronometer.setVisibility(View.GONE);
                                binding.tvStatus.setVisibility(View.VISIBLE);
                                binding.tvStatus.setText("Call Ended");

                                if (CallListHelper.isCallListEmpty()) {
                                    App.AppOpenAdManager.isShowBackOpenAd = true;
                                    stopService(new Intent(activity, CallNotificationService.class));
                                }

                                if (SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_CALL_BACK_SCREEN, true)) {
                                    Uri handle = call.getDetails().getHandle();
                                    if (handle != null) {
                                        Intent callbackIntent = new Intent(activity, CallBackActivity.class);
                                        callbackIntent.putExtra(Constants.PHONE_NUMBER, handle.getSchemeSpecificPart());
                                        callbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        startActivity(callbackIntent);
                                    }
                                }

                                finish();
                            }
                            break;
                    }
                }
            });
        }
    }

    private void listenToIncomingCallState() {
        call = currentCall;
        if (call != null) {
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            tm.listen(new PhoneStateListener() {
                @Override
                public void onCallStateChanged(int state, String incomingNumber) {
                    switch (state) {
                        case TelephonyManager.CALL_STATE_RINGING:
                            binding.tvStatus.setVisibility(View.VISIBLE);
                            binding.chronometer.setVisibility(View.GONE);
                            binding.tvStatus.setText("Dialing...");
                            break;
                        case TelephonyManager.CALL_STATE_OFFHOOK:
                            binding.chronometer.setVisibility(View.VISIBLE);
                            binding.tvStatus.setVisibility(View.GONE);
                            long startTimeMillis = call.getDetails().getConnectTimeMillis();
                            binding.chronometer.setBase(SystemClock.elapsedRealtime() - (System.currentTimeMillis() - startTimeMillis));
                            binding.chronometer.start();

                            changeView();

                            binding.btnAddCall.setEnabled(true);
                            binding.btnAddCall.setClickable(true);
                            if (!(CallListHelper.getCallListSize() >= 2)) {
                                binding.ivAddCall.setImageTintList(ColorStateList.valueOf(getColor(R.color.white)));
                            }
                            binding.btnHoldCall.setEnabled(true);
                            binding.btnHoldCall.setClickable(true);
                            binding.ivHoldCall.setImageTintList(ColorStateList.valueOf(getColor(R.color.white)));
                            binding.btnMute.setEnabled(true);
                            binding.btnMute.setClickable(true);
                            if (!isMuted) {
                                binding.ivMute.setImageTintList(ColorStateList.valueOf(getColor(R.color.white)));
                            }

                            break;
                        case TelephonyManager.CALL_STATE_IDLE:
                            if (CallListHelper.isCallListEmpty()) {
                                binding.chronometer.stop();
                                binding.chronometer.setVisibility(View.GONE);
                                binding.tvStatus.setVisibility(View.VISIBLE);
                                binding.tvStatus.setText("Call Ended");

                                if (CallListHelper.isCallListEmpty()) {
                                    App.AppOpenAdManager.isShowBackOpenAd = true;
                                    stopService(new Intent(activity, CallNotificationService.class));
                                }

                                if (SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_CALL_BACK_SCREEN, true)) {
                                    Uri handle = call.getDetails().getHandle();
                                    if (handle != null) {
                                        Intent callbackIntent = new Intent(activity, CallBackActivity.class);
                                        callbackIntent.putExtra(Constants.PHONE_NUMBER, handle.getSchemeSpecificPart());
                                        callbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        startActivity(callbackIntent);
                                    }
                                }

                                finish();
                            }
                            break;

                    }
                }
            }, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    private void initListener() {
        binding.btnAddCall.setOnClickListener(this);
        binding.btnHoldCall.setOnClickListener(this);
        binding.btnBluetooth.setOnClickListener(this);
        binding.btnSpeaker.setOnClickListener(this);
        binding.btnMute.setOnClickListener(this);
        binding.btnKeypad.setOnClickListener(this);
        binding.btnDecline.setOnClickListener(this);

        binding.btnOne.setOnClickListener(this);
        binding.btnTwo.setOnClickListener(this);
        binding.btnThree.setOnClickListener(this);
        binding.btnFour.setOnClickListener(this);
        binding.btnFive.setOnClickListener(this);
        binding.btnSix.setOnClickListener(this);
        binding.btnSeven.setOnClickListener(this);
        binding.btnEight.setOnClickListener(this);
        binding.btnNine.setOnClickListener(this);
        binding.btnZero.setOnClickListener(this);
        binding.btnStar.setOnClickListener(this);
        binding.btnHash.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnAddCall) {
            addNewCall();
        } else if (id == R.id.btnHoldCall) {
            toggleHoldCall();
        } else if (id == R.id.btnBluetooth) {
            toggleBluetooth();
        } else if (id == R.id.btnSpeaker) {
            toggleSpeaker();
        } else if (id == R.id.btnMute) {
            toggleMute();
        } else if (id == R.id.btnKeypad) {
            openKeypad();
        } else if (id == R.id.btnDecline) {
            endCall();
        } else if (id == R.id.btnOne) {
            onNumberClick("1");
        } else if (id == R.id.btnTwo) {
            onNumberClick("2");
        } else if (id == R.id.btnThree) {
            onNumberClick("3");
        } else if (id == R.id.btnFour) {
            onNumberClick("4");
        } else if (id == R.id.btnFive) {
            onNumberClick("5");
        } else if (id == R.id.btnSix) {
            onNumberClick("6");
        } else if (id == R.id.btnSeven) {
            onNumberClick("7");
        } else if (id == R.id.btnEight) {
            onNumberClick("8");
        } else if (id == R.id.btnNine) {
            onNumberClick("9");
        } else if (id == R.id.btnZero) {
            onNumberClick("0");
        } else if (id == R.id.btnStar) {
            onNumberClick("*");
        } else if (id == R.id.btnHash) {
            onNumberClick("#");
        }
    }

    public void onNumberClick(String digit) {
        int cursorPosition = binding.etPhoneNumber.getSelectionStart();
        String currentText = binding.etPhoneNumber.getText().toString();

        if (cursorPosition < 0 || cursorPosition > currentText.length()) {
            cursorPosition = currentText.length();
        }

        if (currentText.length() >= 40) {
            if (call != null) {
                call.playDtmfTone(digit.charAt(0));
                new Handler().postDelayed(() -> {
                    call.stopDtmfTone();
                }, 150L);
            }
            return;
        }

        StringBuilder newText = new StringBuilder(currentText);
        newText.insert(cursorPosition, digit);

        etNumber = newText;

        if (call != null) {
            call.playDtmfTone(digit.charAt(0));
            new Handler().postDelayed(() -> {
                call.stopDtmfTone();
            }, 150L);
        }

        binding.etPhoneNumber.removeTextChangedListener(textWatcher);
        binding.etPhoneNumber.setText(etNumber.toString());

        final int targetCursorPosition = cursorPosition + 1;
        binding.etPhoneNumber.post(() -> {
            try {
                int actualTextLength = binding.etPhoneNumber.getText().length();
                int safeCursorPosition = Math.min(targetCursorPosition, actualTextLength);
                binding.etPhoneNumber.setSelection(safeCursorPosition);
            } catch (Exception e) {
                try {
                    binding.etPhoneNumber.setSelection(binding.etPhoneNumber.getText().length());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        binding.etPhoneNumber.addTextChangedListener(textWatcher);
    }

    private void addNewCall() {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            startActivity(intent);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void toggleSpeaker() {
        if (isSpeakerOn) {
            isSpeakerOn = false;
            MyInCallService.speakerCall(false);
            binding.ivSpeaker.setImageTintList(ColorStateList.valueOf(getColor(R.color.white)));
        } else {
            isSpeakerOn = true;
            MyInCallService.speakerCall(true);
            binding.ivSpeaker.setImageTintList(ColorStateList.valueOf(getColor(R.color.main)));
        }
        binding.ivBluetooth.setImageTintList(ColorStateList.valueOf(getColor(R.color.white)));
    }

    private void toggleMute() {
        if (isMuted) {
            isMuted = false;
            MyInCallService.muteCall(false);
            binding.ivMute.setImageTintList(ColorStateList.valueOf(getColor(R.color.white)));
        } else {
            MyInCallService.muteCall(true);
            isMuted = true;
            binding.ivMute.setImageTintList(ColorStateList.valueOf(getColor(R.color.main)));
        }
    }

    private void openKeypad() {
        isKeyboard = !isKeyboard;
        if (isKeyboard) {
            binding.clKeyBoard.setVisibility(View.VISIBLE);
            binding.ll1.setVisibility(View.GONE);
            binding.llContact.setVisibility(View.GONE);
            binding.ivKeypad.setImageTintList(ContextCompat.getColorStateList(activity, R.color.main));
        } else {
            binding.clKeyBoard.setVisibility(View.GONE);
            binding.ll1.setVisibility(View.VISIBLE);
            binding.llContact.setVisibility(View.VISIBLE);
            binding.ivKeypad.setImageTintList(ContextCompat.getColorStateList(activity, R.color.white));
        }
    }

    private void toggleHoldCall() {
        if (currentCall != null) {
            if (isCallOnHold) {
                isCallOnHold = false;
                MyInCallService.unholdCall(currentCall);
                binding.ivHoldCall.setImageTintList(ColorStateList.valueOf(getColor(R.color.white)));
            } else {
                isCallOnHold = true;
                MyInCallService.holdCall(currentCall);
                binding.ivHoldCall.setImageTintList(ColorStateList.valueOf(getColor(R.color.main)));
            }

            new Handler().postDelayed(() -> {
                CallListHelper.updateActiveCallForNotification();
                updateNotification();
            }, 300);
        }
    }

    private void toggleBluetooth() {
        if (MyInCallService.inCallService == null) return;

        try {
            CallAudioState audioState = MyInCallService.inCallService.getCallAudioState();
            if (audioState == null) return;

            boolean isBluetoothSupported = (audioState.getSupportedRouteMask() & CallAudioState.ROUTE_BLUETOOTH) != 0;

            if (isBluetoothSupported) {
                showAudioRouteDialog();
            } else {
                Toast.makeText(activity, getString(R.string.no_bluetooth_device_connected), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkAudioType() {
        type = getCurrentAudio();
        if (type == 0) {
            binding.ivSpeaker.setImageTintList(ColorStateList.valueOf(getColor(R.color.main)));
            binding.ivBluetooth.setImageTintList(ColorStateList.valueOf(getColor(R.color.white)));
            isSpeakerOn = true;
        } else if (type == 1) {
            binding.ivSpeaker.setImageTintList(ColorStateList.valueOf(getColor(R.color.white)));
            binding.ivBluetooth.setImageTintList(ColorStateList.valueOf(getColor(R.color.main)));
            isSpeakerOn = false;
        } else {
            binding.ivSpeaker.setImageTintList(ColorStateList.valueOf(getColor(R.color.white)));
            binding.ivBluetooth.setImageTintList(ColorStateList.valueOf(getColor(R.color.white)));
            isSpeakerOn = false;
        }
    }

    private void showAudioRouteDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        dialog = new Dialog(activity);
        audioRouteBinding = DialogAudioRouteBinding.inflate(getLayoutInflater());
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.setContentView(audioRouteBinding.getRoot());
        dialog.setCancelable(true);

        audioRouteBinding.btnClose.setOnClickListener(v -> dialog.dismiss());

        int selected = getCurrentAudio();
        if (selected == 0) {
            audioRouteBinding.ivSpeaker.setImageResource(R.drawable.ic_select);
            audioRouteBinding.ivBluetooth.setImageResource(R.drawable.ic_unselect);
            audioRouteBinding.ivEarpiece.setImageResource(R.drawable.ic_unselect);
            audioRouteBinding.ivSpeaker.setImageTintList(ColorStateList.valueOf(getColor(R.color.main)));
            audioRouteBinding.ivBluetooth.setImageTintList(ColorStateList.valueOf(getColor(R.color.white)));
            audioRouteBinding.ivEarpiece.setImageTintList(ColorStateList.valueOf(getColor(R.color.white)));
        } else if (selected == 1) {
            audioRouteBinding.ivSpeaker.setImageResource(R.drawable.ic_unselect);
            audioRouteBinding.ivBluetooth.setImageResource(R.drawable.ic_select);
            audioRouteBinding.ivEarpiece.setImageResource(R.drawable.ic_unselect);
            audioRouteBinding.ivSpeaker.setImageTintList(ColorStateList.valueOf(getColor(R.color.white)));
            audioRouteBinding.ivBluetooth.setImageTintList(ColorStateList.valueOf(getColor(R.color.main)));
            audioRouteBinding.ivEarpiece.setImageTintList(ColorStateList.valueOf(getColor(R.color.white)));
        } else if (selected == 2) {
            audioRouteBinding.ivSpeaker.setImageResource(R.drawable.ic_unselect);
            audioRouteBinding.ivBluetooth.setImageResource(R.drawable.ic_unselect);
            audioRouteBinding.ivEarpiece.setImageResource(R.drawable.ic_select);
            audioRouteBinding.ivSpeaker.setImageTintList(ColorStateList.valueOf(getColor(R.color.white)));
            audioRouteBinding.ivBluetooth.setImageTintList(ColorStateList.valueOf(getColor(R.color.white)));
            audioRouteBinding.ivEarpiece.setImageTintList(ColorStateList.valueOf(getColor(R.color.main)));
        }

        audioRouteBinding.btnSpeaker.setOnClickListener(v -> {
            setAudioRouteSpeaker();
            dialog.dismiss();
        });

        audioRouteBinding.btnBluetooth.setOnClickListener(v -> {
            setAudioRouteBluetooth();
            dialog.dismiss();
        });

        audioRouteBinding.btnEarpiece.setOnClickListener(v -> {
            setAudioRouteEarpiece();
            dialog.dismiss();
        });

        dialog.show();
    }

    public int getCurrentAudio() {
        if (MyInCallService.inCallService == null) {
            return 2;
        }
        CallAudioState audioState = MyInCallService.inCallService.getCallAudioState();
        if (audioState == null) {
            return 2;
        }

        switch (audioState.getRoute()) {
            case CallAudioState.ROUTE_SPEAKER:
                return 0;
            case CallAudioState.ROUTE_BLUETOOTH:
                return 1;
            default:
                return 2;
        }
    }

    public void setAudioRouteSpeaker() {
        isSpeakerOn = false;
        binding.btnSpeaker.performClick();
        binding.ivSpeaker.setImageTintList(ColorStateList.valueOf(getColor(R.color.main)));
        binding.ivBluetooth.setImageTintList(ColorStateList.valueOf(getColor(R.color.white)));
    }

    public void setAudioRouteBluetooth() {
        MyInCallService.inCallService.setAudioRoute(CallAudioState.ROUTE_BLUETOOTH);
        isSpeakerOn = false;
        binding.ivSpeaker.setImageTintList(ColorStateList.valueOf(getColor(R.color.white)));
        binding.ivBluetooth.setImageTintList(ColorStateList.valueOf(getColor(R.color.main)));
    }

    public void setAudioRouteEarpiece() {
        isSpeakerOn = true;
        binding.btnSpeaker.performClick();
        binding.ivSpeaker.setImageTintList(ColorStateList.valueOf(getColor(R.color.white)));
        binding.ivBluetooth.setImageTintList(ColorStateList.valueOf(getColor(R.color.white)));
    }

    private void endCall() {
        Intent hangUpIntent = new Intent(this, CallActionReceiver.class);
        hangUpIntent.setAction("ACTION_HANGUP_CALL");
        hangUpIntent.putExtra(Constants.PHONE_NUMBER, phoneNumber);
        sendBroadcast(hangUpIntent);

        if (CallListHelper.getUniqueCallList().size() == mergedCallsList.size() || CallListHelper.getUniqueCallList().size() == 1) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(CallNotificationService.NOTIFICATION_ID);

            stopService(new Intent(this, CallNotificationService.class));

            CallListHelper.cleanup();

            releaseAudioFocus();

            finish();
        }
    }

    @SuppressLint("ResourceType")
    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
        changeView();

        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkStateReceiver, filter);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (currentCall != null) {
                    handler.postDelayed(this, 1000);
                }
            }
        }, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        unregisterReceiver(networkStateReceiver);
        if (proximityWakeLock != null && proximityWakeLock.isHeld()) {
            proximityWakeLock.release();
        }
    }

    private final BroadcastReceiver networkStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                NetworkInfo networkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
                handleNetworkStateChange(networkInfo != null && networkInfo.isConnected());
            }
        }
    };

    public static void changeView() {
        synchronized (viewUpdateLock) {
            if (isViewUpdateInProgress) {
                return;
            }

            isViewUpdateInProgress = true;
            mainHandler.post(() -> {
                try {
                    List<Call> uniqueCalls = CallListHelper.getUniqueCallList();

                    CallListHelper.CallMode currentMode = CallListHelper.getCurrentCallMode();
                    switch (currentMode) {
                        case CONFERENCE:
                            handleConferenceViewStatic(uniqueCalls);
                            break;
                        case MULTI_CALL:
                            handleMultiCallViewStatic(uniqueCalls);
                            break;
                        case SINGLE_CALL:
                            handleSingleCallViewStatic(uniqueCalls);
                            break;
                    }

                    updateAddCallButton();

                    if (CallListHelper.hasActiveCallChanged() || CallListHelper.hasCallListChanged()) {
                        updateNotification();
                    }

                } finally {
                    isViewUpdateInProgress = false;
                }
            });
        }
    }

    private static void handleSingleCallViewStatic(List<Call> uniqueCalls) {
        if (uniqueCalls.isEmpty() || binding == null) return;

        Call call = uniqueCalls.get(0);

        updateCallUI(call);
        llConference.setVisibility(View.GONE);
        if (!isKeyboard) {
            llContact.setVisibility(View.VISIBLE);
        }
        rvConferenceCallList.setVisibility(View.GONE);
    }

    private static void handleMultiCallViewStatic(List<Call> uniqueCalls) {
        if (binding != null) {
            llConference.setVisibility(View.GONE);
            llContact.setVisibility(View.GONE);
            rvConferenceCallList.setVisibility(View.VISIBLE);

            tvStatus.setText("Multi-Call");

            updateCallList(uniqueCalls);
        }
    }

    private static void handleConferenceViewStatic(List<Call> uniqueCalls) {
        if (binding != null) {
            llConference.setVisibility(View.VISIBLE);
            llContact.setVisibility(View.GONE);
            rvConferenceCallList.setVisibility(View.VISIBLE);

            tvStatus.setText(String.format("Conference (%d)", uniqueCalls.size()));

            long earliestConnectTime = Long.MAX_VALUE;
            boolean hasActiveCall = false;
            for (Call call : uniqueCalls) {
                if (call.getState() == Call.STATE_ACTIVE) {
                    hasActiveCall = true;
                    long connectTime = call.getDetails().getConnectTimeMillis();
                    if (connectTime > 0 && connectTime < earliestConnectTime) {
                        earliestConnectTime = connectTime;
                    }
                }
            }

            if (hasActiveCall && earliestConnectTime != Long.MAX_VALUE) {
                conferenceChronometer.setVisibility(View.VISIBLE);
                conferenceChronometer.setBase(SystemClock.elapsedRealtime() - (System.currentTimeMillis() - earliestConnectTime));
                conferenceChronometer.start();
            } else {
                conferenceChronometer.setVisibility(View.GONE);
            }

            updateCallList(uniqueCalls);
        }
    }

    private static void updateCallList(List<Call> calls) {
        if (calls == null || binding == null) return;

        rvConferenceCallList.setLayoutManager(new LinearLayoutManager(context));
        rvConferenceCallList.setAdapter(callListAdapter);
        callListAdapter.setList(calls);
    }

    private static void toggleHoldStatic(Call call) {
        if (call != null) {
            if (call.getState() == Call.STATE_HOLDING) {
                call.unhold();
            } else {
                call.hold();
            }

            // Update notification after hold state change with a small delay
            new Handler().postDelayed(() -> {
                CallListHelper.updateActiveCallForNotification();
                updateNotification();
            }, 300);
        }
    }

    private static void updateNotification() {
        if (context == null) return;

        Call activeCallForNotification = CallListHelper.getActiveCallForNotification();
        boolean isInConference = CallListHelper.isInConferenceMode();
        List<Call> uniqueCalls = CallListHelper.getUniqueCallList();

        Intent intent = new Intent(context, CallNotificationService.class);

        if (isInConference && !uniqueCalls.isEmpty()) {
            intent.putExtra(Constants.TITLE, "ACTION_CONFERENCE_NOTIFICATION");
            intent.putExtra("IS_CALL_RECEIVED", true);
            context.startService(intent);
            return;
        }

        if (uniqueCalls.isEmpty()) {
            return;
        }

        if (activeCallForNotification != null &&
                activeCallForNotification.getDetails() != null &&
                activeCallForNotification.getDetails().getHandle() != null) {

            String phoneNumber = activeCallForNotification.getDetails().getHandle().getSchemeSpecificPart();

            long connectTime = -1;
            if (activeCallForNotification.getState() == Call.STATE_ACTIVE &&
                    activeCallForNotification.getDetails() != null) {
                connectTime = activeCallForNotification.getDetails().getConnectTimeMillis();
            }

            intent.putExtra(Constants.PHONE_NUMBER, phoneNumber);
            intent.putExtra(Constants.TITLE, "ACTION_UPDATE_NOTIFICATION");
            intent.putExtra("IS_CALL_RECEIVED", activeCallForNotification.getState() == Call.STATE_ACTIVE);

            if (connectTime > 0) {
                intent.putExtra("CONNECT_TIME", connectTime);
            }

            context.startService(intent);
        } else if (!uniqueCalls.isEmpty()) {
            Call firstCall = uniqueCalls.get(0);
            if (firstCall.getDetails() != null && firstCall.getDetails().getHandle() != null) {
                String phoneNumber = firstCall.getDetails().getHandle().getSchemeSpecificPart();

                long connectTime = -1;
                if (firstCall.getState() == Call.STATE_ACTIVE && firstCall.getDetails() != null) {
                    connectTime = firstCall.getDetails().getConnectTimeMillis();
                }

                intent.putExtra(Constants.PHONE_NUMBER, phoneNumber);
                intent.putExtra(Constants.TITLE, "ACTION_UPDATE_NOTIFICATION");
                intent.putExtra("IS_CALL_RECEIVED", firstCall.getState() == Call.STATE_ACTIVE);

                if (connectTime > 0) {
                    intent.putExtra("CONNECT_TIME", connectTime);
                }

                context.startService(intent);
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            float distance = event.values[0];
            if (distance < mProximity.getMaximumRange()) {
                if (proximityWakeLock != null && !proximityWakeLock.isHeld()) {
                    proximityWakeLock.acquire();
                }
            } else {
                if (proximityWakeLock != null && proximityWakeLock.isHeld()) {
                    proximityWakeLock.release();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onDestroy() {
        try {
            unregisterReceiver(closeReceiver);
        } catch (IllegalArgumentException e) {
        }

        super.onDestroy();

        if (proximityWakeLock != null && proximityWakeLock.isHeld()) {
            proximityWakeLock.release();
            proximityWakeLock = null;
        }

        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
            mSensorManager = null;
        }

        releaseAudioFocus();
        if (audioManager != null) {
            audioManager = null;
            audioFocusChangeListener = null;
        }

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    private void setActiveOutGoing() {
        binding.chronometer.setVisibility(View.VISIBLE);
        binding.tvStatus.setVisibility(View.GONE);

        binding.btnAddCall.setEnabled(true);
        binding.btnAddCall.setClickable(true);
        if (!(CallListHelper.getCallListSize() >= 2)) {
            binding.ivAddCall.setImageTintList(ColorStateList.valueOf(getColor(R.color.white)));
        }
        binding.btnHoldCall.setEnabled(true);
        binding.btnHoldCall.setClickable(true);
        binding.ivHoldCall.setImageTintList(ColorStateList.valueOf(getColor(R.color.white)));
        binding.btnMute.setEnabled(true);
        binding.btnMute.setClickable(true);
        if (!isMuted) {
            binding.ivMute.setImageTintList(ColorStateList.valueOf(getColor(R.color.white)));
        }
    }

    private void setButtonsDisabled() {
        binding.btnHoldCall.setEnabled(false);
        binding.btnHoldCall.setClickable(false);
        binding.ivHoldCall.setImageTintList(ColorStateList.valueOf(Color.parseColor("#80FFFFFF")));

        binding.btnAddCall.setEnabled(false);
        binding.btnAddCall.setClickable(false);
        binding.ivAddCall.setImageTintList(ColorStateList.valueOf(Color.parseColor("#80FFFFFF")));

        binding.btnMute.setEnabled(false);
        binding.btnMute.setClickable(false);
        binding.ivMute.setImageTintList(ColorStateList.valueOf(Color.parseColor("#80FFFFFF")));
    }

    private void setupAudioFocus() {
        if (audioManager != null && audioFocusChangeListener != null) {
            int result = audioManager.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN
            );

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED && currentCall != null) {
                // Remove setAudioEnabled as it's not available in Call class
                // currentCall.setAudioEnabled(true);
            }
        }
    }

    private void releaseAudioFocus() {
        if (audioManager != null && audioFocusChangeListener != null) {
            audioManager.abandonAudioFocus(audioFocusChangeListener);
        }
    }

    private void handleNetworkStateChange(boolean isConnected) {
        if (!isConnected) {
            showNetworkLossNotification();
        }
    }

    private void showNetworkLossNotification() {
        android.app.NotificationManager notificationManager = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(activity, "call_channel")
                .setSmallIcon(R.drawable.ic_call)
                .setContentTitle("Network Connection Lost")
                .setContentText("Attempting to maintain call...")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(1001, builder.build());
    }

    private void optimizeBatteryUsage() {
        if (powerManager == null) {
            powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        }

        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "ContactGroupItem:CallWakeLock"
        );
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire(10 * 60 * 1000L);

        Intent intent = new Intent();
        String packageName = getPackageName();
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
            startActivity(intent);
        }
    }

    private void handleDialingState(Call call) {
        updateCallUI(call);
    }

    private void handleRingingState(Call call) {
        updateCallUI(call);
    }

    private void handleHoldingState(Call call) {
        updateCallUI(call);
    }

    private static void updateCallUI(Call call) {
        if (call == null || context == null || binding == null) return;

        if (context instanceof OutgoingCallActivity) {
            OutgoingCallActivity activity = (OutgoingCallActivity) context;
            if (activity.isDestroyed() || activity.isFinishing()) {
                return;
            }
        }

        String number = CallListHelper.getCallNumber(call);

        String name = ContactsHelper.getContactNameByPhoneNumber(number, context);
        if (name != null && !name.isEmpty()) {
            tvContactName.setText(name);
            tvContactName.setSelected(true);
            String contactId = Utils.getContactIdByName(context, name);
            if (contactId != null && !contactId.isEmpty()) {
                Bitmap contactUri = Utils.getContactPhoto(context, contactId);
                if (contactUri != null) {
                    if (context != null && !((OutgoingCallActivity) context).isDestroyed()
                            && !((OutgoingCallActivity) context).isFinishing() && ivProfile != null) {
                        try {
                            Glide.with(context).load(contactUri).into(ivProfile);
                        } catch (IllegalArgumentException e) {
                            ivProfile.setImageResource(R.drawable.ic_profile_2);
                        }
                    } else {
                        if (ivProfile != null) {
                            ivProfile.setImageResource(R.drawable.ic_profile_2);
                        }
                    }
                } else {
                    ivProfile.setImageResource(R.drawable.ic_profile_2);
                }
            }
        } else {
            tvContactName.setText(context.getString(R.string.unknown_caller));
            tvContactName.setSelected(true);
            ivProfile.setImageResource(R.drawable.ic_profile_2);
        }
        tvPhoneNumber.setText(number);

        switch (call.getState()) {
            case Call.STATE_ACTIVE:
                tvStatus.setText("Active");
                tvStatus.setVisibility(View.GONE);
                chronometer.setVisibility(View.VISIBLE);
                chronometer.setBase(SystemClock.elapsedRealtime() - (System.currentTimeMillis() - call.getDetails().getConnectTimeMillis()));
                chronometer.start();
                break;
            case Call.STATE_HOLDING:
                tvStatus.setText("On Hold");
                tvStatus.setVisibility(View.VISIBLE);
                chronometer.setVisibility(View.GONE);
                chronometer.stop();
                break;
            case Call.STATE_DIALING:
            case Call.STATE_CONNECTING:
                tvStatus.setText("Dialing...");
                tvStatus.setVisibility(View.VISIBLE);
                chronometer.setVisibility(View.GONE);
                chronometer.stop();
                break;
            case Call.STATE_RINGING:
                tvStatus.setText("Ringing");
                tvStatus.setVisibility(View.VISIBLE);
                chronometer.setVisibility(View.GONE);
                chronometer.stop();
                break;
        }

        updateAddCallButton();
    }

    private void setupCallStateListener() {
        CallListHelper.initialize(new CallListHelper.CallStateListener() {
            @Override
            public void onCallStateChanged(Call call, int state) {
                updateCallList();
                handleCallStateChange(call, state);
            }

            @Override
            public void onSwapComplete() {
                updateCallList();
                updateNotification();
            }

            @Override
            public void onActiveCallChanged(Call call) {
                updateCallList();
                updateAudioRoute();
                currentCall = call;
                updateUIForCurrentMode();
                updateNotification();
            }

            @Override
            public void onConferenceStateChanged(boolean isConference) {
                updateCallList();
                CallListHelper.CallMode currentMode = CallListHelper.getCurrentCallMode();
                List<Call> uniqueCalls = CallListHelper.getUniqueCallList();
                updateUIForCallMode(currentMode, uniqueCalls);
            }
        });
    }

    private void setupCallList() {
        List<Call> calls = CallListHelper.getUniqueCallList();
        if (calls.isEmpty()) {
            finish();
            return;
        }

        updateCallList();
    }

    private void updateCallList() {
        List<Call> uniqueCalls = CallListHelper.getUniqueCallList();
        setCallList();

        CallListHelper.CallMode currentMode = CallListHelper.getCurrentCallMode();
        updateUIForCallMode(currentMode, uniqueCalls);
    }

    private void updateUIForCallMode(CallListHelper.CallMode currentMode, List<Call> uniqueCalls) {
        switch (currentMode) {
            case CONFERENCE:
                handleConferenceViewStatic(uniqueCalls);
                break;
            case MULTI_CALL:
                handleMultiCallViewStatic(uniqueCalls);
                break;
            case SINGLE_CALL:
                handleSingleCallViewStatic(uniqueCalls);
                break;
        }

        updateAddCallButton();

        CallListHelper.updateNotificationOnModeChange(this);
    }

    private void handleCallStateChange(Call call, int state) {
        if (call == null) return;

        synchronized (callStateLock) {
            switch (state) {
                case Call.STATE_DIALING:
                case Call.STATE_CONNECTING:
                    handleDialingState(call);
                    break;
                case Call.STATE_RINGING:
                    handleRingingState(call);
                    break;
                case Call.STATE_ACTIVE:
                    handleActiveState(call);
                    break;
                case Call.STATE_HOLDING:
                    handleHoldingState(call);
                    break;
                case Call.STATE_DISCONNECTED:
                    handleDisconnectedState(call);
                    break;
            }

            updateUIForCurrentMode();

            // Always update CallListHelper's active call tracking after state changes
            CallListHelper.updateActiveCallForNotification();

            // Update notification after any call state change, with a slight delay
            // to ensure all state changes are processed
            new Handler().postDelayed(() -> updateNotification(), 100);
        }
    }

    private void handleActiveState(Call call) {
        if (call != null) {
            updateCallUI(call);

            // Check if this is the call currently displayed in the UI
            boolean isDisplayedCall = false;
            if (binding.llContact.getVisibility() == View.VISIBLE && call.getDetails() != null &&
                    call.getDetails().getHandle() != null) {
                String callNumber = call.getDetails().getHandle().getSchemeSpecificPart();
                String displayedNumber = binding.tvPhoneNumber.getText().toString();
                isDisplayedCall = callNumber.equals(displayedNumber);
            }

            if (isDisplayedCall) {

                Intent intent = new Intent(this, CallNotificationService.class);
                intent.putExtra(Constants.TITLE, "ACTION_UPDATE_NOTIFICATION");
                intent.putExtra(Constants.PHONE_NUMBER, call.getDetails().getHandle().getSchemeSpecificPart());
                intent.putExtra("IS_CALL_RECEIVED", true);
                startService(intent);
            } else {
                new Handler().post(() -> updateNotification());
            }
        }
    }

    private void handleDisconnectedState(Call call) {
        if (call != null) {
            Intent intent = new Intent(activity, CallNotificationService.class);
            stopService(intent);
            List<Call> uniqueCalls = CallListHelper.getUniqueCallList();
            if (uniqueCalls.isEmpty()) {
                if (SharedPreferencesManager.getInstance().getBooleanValue(Constants.IS_CALL_BACK_SCREEN, true)) {
                    Uri handle = call.getDetails().getHandle();
                    if (handle != null) {
                        Intent callbackIntent = new Intent(activity, CallBackActivity.class);
                        callbackIntent.putExtra(Constants.PHONE_NUMBER, handle.getSchemeSpecificPart());
                        callbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(callbackIntent);
                    }
                }

                finish();
            }
        }
    }

    private void updateUIForCurrentMode() {
        CallListHelper.CallMode currentMode = CallListHelper.getCurrentCallMode();
        List<Call> uniqueCalls = CallListHelper.getUniqueCallList();

        switch (currentMode) {
            case SINGLE_CALL:
                handleSingleCallViewStatic(uniqueCalls);
                break;
            case MULTI_CALL:
                handleMultiCallViewStatic(uniqueCalls);
                break;
            case CONFERENCE:
                handleConferenceViewStatic(uniqueCalls);
                break;
        }
    }

    private void setCallList() {
        if (callListAdapter != null) {
            callListAdapter.notifyDataSetChanged();
        }
    }

    private void updateAudioRoute() {
        if (audioManager != null) {
            audioManager.setSpeakerphoneOn(isSpeakerOn);
        }
    }

    public static void onSwapComplete() {
        if (context == null) return;

        CallListHelper.updateActiveCallForNotification();

        Call activeCall = CallListHelper.getActiveCallForNotification();
        if (activeCall != null && activeCall.getDetails() != null &&
                activeCall.getDetails().getHandle() != null) {

            String phoneNumber = activeCall.getDetails().getHandle().getSchemeSpecificPart();

            long connectTime = -1;
            if (activeCall.getState() == Call.STATE_ACTIVE && activeCall.getDetails() != null) {
                connectTime = activeCall.getDetails().getConnectTimeMillis();
            }

            Intent intent = new Intent(context, CallNotificationService.class);
            intent.putExtra(Constants.PHONE_NUMBER, phoneNumber);
            intent.putExtra(Constants.TITLE, "ACTION_UPDATE_NOTIFICATION");
            intent.putExtra("IS_CALL_RECEIVED", activeCall.getState() == Call.STATE_ACTIVE);

            if (connectTime > 0) {
                intent.putExtra("CONNECT_TIME", connectTime);
            }

            context.startService(intent);
        }

        mainHandler.post(() -> {
            List<Call> calls = CallListHelper.getUniqueCallList();
            updateCallList(calls);

            CallListHelper.CallMode currentMode = CallListHelper.getCurrentCallMode();
            updateUIForCallModeStatic(currentMode, calls);
        });
    }

    private static void updateUIForCallModeStatic(CallListHelper.CallMode currentMode, List<Call> uniqueCalls) {
        switch (currentMode) {
            case CONFERENCE:
                handleConferenceViewStatic(uniqueCalls);
                break;
            case MULTI_CALL:
                handleMultiCallViewStatic(uniqueCalls);
                break;
            case SINGLE_CALL:
                handleSingleCallViewStatic(uniqueCalls);
                break;
        }

        updateAddCallButton();

        if (context != null) {
            CallListHelper.updateNotificationOnModeChange(context);
        }
    }

    private static void mergeCalls(Call call) {
        if (call == null) {
            return;
        }

        List<Call> uniqueCalls = CallListHelper.getUniqueCallList();

        if (uniqueCalls.size() > CallListHelper.MAX_CONFERENCE_PARTICIPANTS) {
            Toast.makeText(context, context.getString(R.string.maximum_conference_participants_reached), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Call activeCall = null;
            List<Call> heldCalls = new ArrayList<>();

            for (Call c : uniqueCalls) {
                if (c.getState() == Call.STATE_ACTIVE) {
                    activeCall = c;
                } else if (c.getState() == Call.STATE_HOLDING) {
                    heldCalls.add(c);
                }
            }

            if (activeCall == null || heldCalls.isEmpty()) {
                if (call.getState() == Call.STATE_ACTIVE) {
                    call.hold();
                    for (int i = 0; i < 10; i++) {
                        if (call.getState() == Call.STATE_HOLDING) {
                            break;
                        }
                        Thread.sleep(100);
                    }
                    heldCalls.add(call);
                } else if (call.getState() == Call.STATE_HOLDING) {
                    heldCalls.add(call);
                }

                for (Call c : uniqueCalls) {
                    if (c != call && c.getState() == Call.STATE_ACTIVE) {
                        activeCall = c;
                        break;
                    }
                }
            }

            if (activeCall == null || heldCalls.isEmpty()) {
                Toast.makeText(context, context.getString(R.string.need_one_active_and_one_held_call_to_create_conference), Toast.LENGTH_SHORT).show();
                return;
            }

            Call firstHeldCall = heldCalls.get(0);

            if (CallListHelper.isInConferenceMode()) {
                if (!mergedCallsList.contains(call)) {
                    mergedCallsList.add(call);
                }
            } else {
                if (!mergedCallsList.contains(firstHeldCall)) {
                    mergedCallsList.add(firstHeldCall);
                }

                if (!mergedCallsList.contains(activeCall)) {
                    mergedCallsList.add(activeCall);
                }
            }

            try {
                firstHeldCall.conference(activeCall);
                CallListHelper.setConferenceMode(true);
            } catch (Exception e) {
                return;
            }

            List<Call> updatedCalls = CallListHelper.getUniqueCallList();
            handleConferenceViewStatic(updatedCalls);

            updateNotification();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}