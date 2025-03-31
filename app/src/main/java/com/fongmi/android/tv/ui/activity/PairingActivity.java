package com.fongmi.android.tv.ui.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.server.PairingManager;
import com.fongmi.android.tv.server.SocketManager;

public class PairingActivity extends AppCompatActivity {
    private TextView pairingCodeText;
    private TextView pairingStatusText;
    private TextView pairingTimerText;
    private View pairingLayout;
    private View pairedLayout;
    private SocketManager socketManager;
    private PairingManager pairingManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pairing);
        
        initView();
        initManager();
        startPairing();
    }

    private void initView() {
        pairingCodeText = findViewById(R.id.pairing_code);
        pairingStatusText = findViewById(R.id.pairing_status);
        pairingTimerText = findViewById(R.id.pairing_timer);
        pairingLayout = findViewById(R.id.pairing_layout);
        pairedLayout = findViewById(R.id.paired_layout);

        // 初始化视图状态
        pairingLayout.setVisibility(View.VISIBLE);
        pairedLayout.setVisibility(View.GONE);
        pairingStatusText.setText("正在初始化...");
        pairingTimerText.setText("");
    }

    private void initManager() {
        socketManager = SocketManager.getInstance();
        pairingManager = PairingManager.getInstance();
        
        // 设置配对状态监听器
        pairingManager.setPairingListener(new PairingManager.PairingListener() {
            @Override
            public void onPairingCodeGenerated(String code) {
                pairingCodeText.setText(code);
                pairingStatusText.setText("等待配对...");
            }

            @Override
            public void onPairingSuccess() {
                pairingLayout.setVisibility(View.GONE);
                pairedLayout.setVisibility(View.VISIBLE);
                Toast.makeText(PairingActivity.this, "配对成功", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onPairingFailed(String message) {
                pairingStatusText.setText("配对失败: " + message);
                Toast.makeText(PairingActivity.this, "配对失败", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPairingTimeout() {
                pairingStatusText.setText("配对超时");
                Toast.makeText(PairingActivity.this, "配对超时", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onTimeRemaining(int seconds) {
                pairingTimerText.setText(String.format("剩余时间: %d秒", seconds));
            }
        });
    }

    private void startPairing() {
        // 确保Socket已连接
        if (!socketManager.isConnected()) {
            pairingStatusText.setText("正在连接服务器...");
            socketManager.connect(new SocketManager.ConnectCallback() {
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> {
                        if (!isFinishing()) {
                            pairingManager.startPairing();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        if (!isFinishing()) {
                            pairingStatusText.setText("连接服务器失败: " + error);
                            Toast.makeText(PairingActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        } else {
            // Socket已连接，直接开始配对
            pairingManager.startPairing();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pairingManager.stopPairing();
    }

    // 返回按钮点击处理
    @Override
    public void onBackPressed() {
        pairingManager.stopPairing();
        super.onBackPressed();
    }
} 