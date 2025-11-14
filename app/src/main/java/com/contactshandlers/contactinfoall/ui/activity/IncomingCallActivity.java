package com.contactshandlers.contactinfoall.ui.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.telecom.Call;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.databinding.ActivityIncomingCallBinding;
import com.contactshandlers.contactinfoall.databinding.DialogMessageBinding;
import com.contactshandlers.contactinfoall.helper.CallListHelper;
import com.contactshandlers.contactinfoall.helper.Constants;
import com.contactshandlers.contactinfoall.helper.LocaleHelper;
import com.contactshandlers.contactinfoall.helper.Utils;
import com.contactshandlers.contactinfoall.receiver.CallActionReceiver;
import com.contactshandlers.contactinfoall.service.CallNotificationService;
import com.contactshandlers.contactinfoall.service.MyInCallService;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class IncomingCallActivity extends AppCompatActivity implements View.OnClickListener {
    private ActivityIncomingCallBinding binding;
    private final IncomingCallActivity activity = IncomingCallActivity.this;
    private String phoneNumber;
    private BottomSheetDialog bottomSheetDialog;
    private DialogMessageBinding messageBinding;
    private static final int SMS_PERMISSION_REQUEST_CODE = 101;
    private static final int PHONE_STATE_PERMISSION_REQUEST_CODE = 102;
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;

    private final BroadcastReceiver closeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("ACTION_CLOSE_CALL_UI".equals(intent.getAction())) {
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityIncomingCallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setShowWhenLocked(true);
        setTurnScreenOn(true);

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        setStatusNavigation();
        init();
        initListener();
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

    private void init() {
        phoneNumber = getIntent().getStringExtra(Constants.PHONE_NUMBER);

        if (phoneNumber != null) {
            binding.tvPhoneNumber.setText(phoneNumber);
            String contactName = Utils.getContactNameByNumber(activity, phoneNumber);
            if (contactName != null && !contactName.isEmpty()) {
                binding.tvContactName.setText(contactName);
                binding.tvContactName.setSelected(true);
                String contactId = Utils.getContactIdByName(activity, contactName);
                if (contactId != null && !contactId.isEmpty()) {
                    Bitmap contactUri = Utils.getContactPhoto(activity, contactId);
                    if (contactUri != null) {
                        Glide.with(activity).load(contactUri).into(binding.ivProfile);
                    }
                }
            }
        }

        Call currentCall = MyInCallService.currentCall;
        if (currentCall != null) {
            CallListHelper.handleIncomingCall(currentCall);
        }

        checkAndRequestPhoneStatePermission();
    }

    private void checkAndRequestPhoneStatePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            listenForCallStateChanges();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    PHONE_STATE_PERMISSION_REQUEST_CODE);
        }
    }

    private void listenForCallStateChanges() {
        try {
            phoneStateListener = new PhoneStateListener() {
                @Override
                public void onCallStateChanged(int state, String phoneNumber) {
                    super.onCallStateChanged(state, phoneNumber);
                    switch (state) {
                        case TelephonyManager.CALL_STATE_OFFHOOK:
                            Call currentCall = MyInCallService.currentCall;
                            if (currentCall != null) {
                                CallListHelper.handleIncomingCall(currentCall);
                            }
                            finish();
                            break;
                        case TelephonyManager.CALL_STATE_IDLE:
                            finish();
                            break;
                    }
                }
            };
            telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager != null) {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            stopRingtone();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private void stopRingtone() {
        Intent intent = new Intent(activity, CallNotificationService.class);
        intent.putExtra(Constants.PHONE_NUMBER, phoneNumber);
        intent.putExtra(Constants.TITLE, "ACTION_SILENT_RINGTONE");
        try {
            startForegroundService(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void answerCall() {
        Intent answerIntent = new Intent(this, CallActionReceiver.class);
        answerIntent.setAction("ACTION_ANSWER_CALL");
        answerIntent.putExtra(Constants.PHONE_NUMBER, phoneNumber);
        sendBroadcast(answerIntent);
        finish();
    }

    private void endCall() {
        Intent declineIntent = new Intent(this, CallActionReceiver.class);
        declineIntent.setAction("ACTION_DECLINE_CALL");
        declineIntent.putExtra(Constants.PHONE_NUMBER, phoneNumber);
        sendBroadcast(declineIntent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(closeReceiver, new IntentFilter("ACTION_CLOSE_CALL_UI"), RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(closeReceiver);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (telephonyManager != null && phoneStateListener != null) {
            try {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    private void initListener() {
        binding.btnAnswer.setOnClickListener(this);
        binding.btnDecline.setOnClickListener(this);
        binding.btnMessage.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnAnswer) {
            answerCall();
        } else if (id == R.id.btnDecline) {
            endCall();
        } else if (id == R.id.btnMessage) {
            showMessageDialog();
        } else if (id == R.id.ll1) {
            sendMessage(getString(R.string.can_t_talk_right_now));
        } else if (id == R.id.ll2) {
            sendMessage(getString(R.string.i_ll_call_you_later));
        } else if (id == R.id.ll3) {
            sendMessage(getString(R.string.i_m_on_my_way));
        } else if (id == R.id.ll4) {
            sendMessage(getString(R.string.can_t_talk_right_now_call_me_later));
        } else if (id == R.id.btnSendCustom) {
            String customMessage = messageBinding.etMessage.getText().toString().trim();
            if (!customMessage.isEmpty()) {
                sendMessage(customMessage);
            }
        }
    }

    private void showMessageDialog() {
        bottomSheetDialog = new BottomSheetDialog(activity);
        messageBinding = DialogMessageBinding.inflate(getLayoutInflater());
        bottomSheetDialog.setContentView(messageBinding.getRoot());
        bottomSheetDialog.show();
        messageBinding.ll1.setOnClickListener(this);
        messageBinding.ll2.setOnClickListener(this);
        messageBinding.ll3.setOnClickListener(this);
        messageBinding.ll4.setOnClickListener(this);
        messageBinding.btnSendCustom.setOnClickListener(this);
    }

    private void sendMessage(String message) {
        if (checkSMSPermission()) {
            sendSMS(phoneNumber, message);
            binding.btnDecline.performClick();
            if (bottomSheetDialog != null) {
                bottomSheetDialog.dismiss();
            }
        } else {
            requestSMSPermission();
        }
    }

    private void sendSMS(String phoneNumber, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean checkSMSPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestSMSPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(activity, getString(R.string.permission_granted_try_sending_sms_again), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(activity, getString(R.string.permission_denied_cannot_send_sms), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == PHONE_STATE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                listenForCallStateChanges();
            }
        }
    }
}