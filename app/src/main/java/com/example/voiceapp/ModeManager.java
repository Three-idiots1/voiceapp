package com.example.voiceapp;

import android.content.Context;
import android.widget.Toast;

public class ModeManager {
    public static final String AUTO_MODE = "자동 모드";
    public static final String MANUAL_MODE = "수동 모드";
    public static final String SCAN_MODE = "스캔 모드";

    private final Context context;
    private String currentMode = MANUAL_MODE;
    private ModeCallback callback;

    public interface ModeCallback {
        void onModeChanged(String mode);
    }

    public ModeManager(Context context) {
        this.context = context;
    }

    public void setCallback(ModeCallback callback) {
        this.callback = callback;
    }

    public void setMode(String mode) {
        if (mode.equals(AUTO_MODE) || mode.equals(MANUAL_MODE) || mode.equals(SCAN_MODE)) {
            currentMode = mode;
            Toast.makeText(context, mode + " 활성화됨", Toast.LENGTH_SHORT).show();

            if (callback != null) {
                callback.onModeChanged(mode);
            }
        }
    }

    public String getCurrentMode() {
        return currentMode;
    }
}