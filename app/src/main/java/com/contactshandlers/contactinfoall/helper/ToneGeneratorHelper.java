package com.contactshandlers.contactinfoall.helper;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;

public class ToneGeneratorHelper {
    private static final int TONE_RELATIVE_VOLUME = 80; // The DTMF tone volume relative to other sounds in the stream
    private static final int DIAL_TONE_STREAM_TYPE = AudioManager.STREAM_DTMF;

    private final AudioManager audioManager;
    private final ToneGenerator toneGenerator;
    private final long minToneLengthMs;
    private long toneStartTimeMs = 0L;

    private static final HashMap<Character, Integer> charToTone = new HashMap<>();
    static {
        charToTone.put('0', ToneGenerator.TONE_DTMF_0);
        charToTone.put('1', ToneGenerator.TONE_DTMF_1);
        charToTone.put('2', ToneGenerator.TONE_DTMF_2);
        charToTone.put('3', ToneGenerator.TONE_DTMF_3);
        charToTone.put('4', ToneGenerator.TONE_DTMF_4);
        charToTone.put('5', ToneGenerator.TONE_DTMF_5);
        charToTone.put('6', ToneGenerator.TONE_DTMF_6);
        charToTone.put('7', ToneGenerator.TONE_DTMF_7);
        charToTone.put('8', ToneGenerator.TONE_DTMF_8);
        charToTone.put('9', ToneGenerator.TONE_DTMF_9);
        charToTone.put('#', ToneGenerator.TONE_DTMF_P);
        charToTone.put('*', ToneGenerator.TONE_DTMF_S);
    }

    public ToneGeneratorHelper(Context context, long minToneLengthMs) {
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.minToneLengthMs = minToneLengthMs;

        ToneGenerator tg;
        try {
            tg = new ToneGenerator(DIAL_TONE_STREAM_TYPE, TONE_RELATIVE_VOLUME);
        } catch (Exception e) {
            tg = null;
        }
        this.toneGenerator = tg;
    }

    private boolean isSilent() {
        int mode = audioManager.getRingerMode();
        return mode == AudioManager.RINGER_MODE_SILENT || mode == AudioManager.RINGER_MODE_VIBRATE;
    }

    public void startTone(char c) {
        toneStartTimeMs = System.currentTimeMillis();
        Integer tone = charToTone.get(c);
        startTone(tone != null ? tone : -1);
    }

    private void startTone(int tone) {
        if (tone != -1 && !isSilent() && toneGenerator != null) {
            toneGenerator.startTone(tone);
        }
    }

    public void stopTone() {
        long elapsed = System.currentTimeMillis() - toneStartTimeMs;
        if (elapsed < minToneLengthMs && toneGenerator != null) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> toneGenerator.stopTone(), minToneLengthMs - elapsed);
        } else if (toneGenerator != null) {
            toneGenerator.stopTone();
        }
    }
}
