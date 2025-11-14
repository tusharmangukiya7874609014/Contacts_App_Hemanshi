package com.contactshandlers.contactinfoall.helper;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;

import com.contactshandlers.contactinfoall.R;
import com.contactshandlers.contactinfoall.service.CallNotificationService;

public class IncomingCallDialog extends Dialog {

    private final Context context;
    private final String phoneNumber;

    public IncomingCallDialog(Context context, String phoneNumber) {
        super(context);
        this.context = context;
        this.phoneNumber = phoneNumber;

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setCancelable(false);
        setContentView(R.layout.dialog_incoming_call);

        if (getWindow() != null) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
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
        Intent intent = new Intent(context, CallNotificationService.class);
        intent.putExtra(Constants.PHONE_NUMBER, phoneNumber);
        intent.putExtra(Constants.TITLE, "ACTION_SILENT_RINGTONE");
        try {
            context.startForegroundService(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
