package com.fongmi.android.tv.server;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.utils.JwtManager;

import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import io.socket.client.Socket;

public class PairingManager {
    private static final String TAG = "PairingManager";
    private static final String PREFS_NAME = "pairing";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final long PAIRING_TIMEOUT = TimeUnit.MINUTES.toMillis(5);
    private static final int TIMER_INTERVAL = 1000; // 1秒

    private static PairingManager instance;
    private final SharedPreferences prefs;
    private final Socket socket;
    private Timer pairingTimer;
    private PairingListener pairingListener;
    private final Handler mainHandler;
    private boolean isPairing;
    private String currentPairingCode;

    // 配对监听器接口
    public interface PairingListener {
        void onPairingCodeGenerated(String code);
        void onPairingSuccess();
        void onPairingFailed(String message);
        void onPairingTimeout();
        void onTimeRemaining(int seconds);
    }

    private PairingManager() {
        prefs = App.get().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        socket = SocketManager.getInstance().getSocket();
        mainHandler = new Handler(Looper.getMainLooper());
        setupEventHandlers();
    }

    public static PairingManager getInstance() {
        if (instance == null) {
            instance = new PairingManager();
        }
        return instance;
    }

    private void setupEventHandlers() {
        Log.d(TAG, "Setting up socket event handlers");
        
        // 处理配对码生成响应
        socket.on("pairing_code_generated", args -> {
            Log.d(TAG, "Received pairing code from server: " + (args.length > 0 ? args[0] : "null"));
            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String code = data.getString("code");
                    currentPairingCode = code;
                    
                    if (pairingListener != null) {
                        mainHandler.post(() -> pairingListener.onPairingCodeGenerated(code));
                    }
                    
                    // 启动配对超时计时器
                    startPairingTimer();
                } catch (Exception e) {
                    Log.e(TAG, "Error handling pairing code", e);
                }
            }
        });

        // 处理配对成功事件
        socket.on("pairing_success", args -> {
            Log.d(TAG, "Received pairing success event: " + (args.length > 0 ? args[0] : "null"));
            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String token = data.getString("token");
                    String deviceId = data.getString("deviceId");
                    
                    // 保存认证信息
                    saveAuthInfo(deviceId, token);
                    
                    if (pairingListener != null) {
                        mainHandler.post(() -> pairingListener.onPairingSuccess());
                    }
                    
                    stopPairing();
                } catch (Exception e) {
                    Log.e(TAG, "Error handling pairing success", e);
                    if (pairingListener != null) {
                        mainHandler.post(() -> pairingListener.onPairingFailed("处理配对响应失败"));
                    }
                }
            }
        });
    }

    public void setPairingListener(PairingListener listener) {
        this.pairingListener = listener;
    }

    public void startPairing() {
        Log.d(TAG, "Starting pairing process");
        if (isPairing) {
            Log.w(TAG, "Pairing already in progress");
            return;
        }
        isPairing = true;

        try {
            // 请求服务器生成配对码
            JSONObject data = new JSONObject();
            data.put("deviceId", getDeviceId());
            Log.d(TAG, "Requesting pairing code from server");
            socket.emit("request_pairing_code", data);
        } catch (Exception e) {
            Log.e(TAG, "Error requesting pairing code", e);
            if (pairingListener != null) {
                mainHandler.post(() -> pairingListener.onPairingFailed("请求配对码失败"));
            }
            stopPairing();
        }
    }

    public void stopPairing() {
        Log.d(TAG, "Stopping pairing process");
        isPairing = false;
        currentPairingCode = null;
        stopPairingTimer();
    }

    private void startPairingTimer() {
        stopPairingTimer();

        pairingTimer = new Timer();
        final long startTime = System.currentTimeMillis();
        
        pairingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                long elapsedTime = currentTime - startTime;
                long remainingTime = PAIRING_TIMEOUT - elapsedTime;

                if (remainingTime <= 0) {
                    mainHandler.post(() -> {
                        if (pairingListener != null) {
                            pairingListener.onPairingTimeout();
                        }
                    });
                    stopPairing();
                } else {
                    int remainingSeconds = (int) (remainingTime / 1000);
                    mainHandler.post(() -> {
                        if (pairingListener != null) {
                            pairingListener.onTimeRemaining(remainingSeconds);
                        }
                    });
                }
            }
        }, 0, TIMER_INTERVAL);
    }

    private void stopPairingTimer() {
        if (pairingTimer != null) {
            pairingTimer.cancel();
            pairingTimer = null;
        }
    }

    private void saveAuthInfo(String deviceId, String token) {
        Log.d(TAG, "Saving auth information - deviceId: " + deviceId+",token:"+token);
        prefs.edit()
            .putString(KEY_DEVICE_ID, deviceId)
            .putString(KEY_AUTH_TOKEN, token)
            .apply();
    }

    @SuppressLint("HardwareIds")
    public String getDeviceId() {
        String deviceId = prefs.getString(KEY_DEVICE_ID, null);
        if (deviceId == null) {
            deviceId = "tv-" + android.os.Build.SERIAL;
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply();
        }
        return deviceId;
    }

    public String getAuthToken() {
        return prefs.getString(KEY_AUTH_TOKEN, null);
    }

    public void clearAuth() {
        prefs.edit()
            .remove(KEY_DEVICE_ID)
            .remove(KEY_AUTH_TOKEN)
            .apply();
    }
} 