package com.fongmi.android.tv.server;

import android.util.Log;

import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.event.ActionEvent;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.server.process.RemoteControl;
import com.fongmi.android.tv.utils.JwtManager;

import org.json.JSONObject;
import org.json.JSONException;

import io.socket.client.IO;
import io.socket.client.Socket;

public class SocketManager {
    private static final String TAG = "SocketManager";
    private static SocketManager instance;
    private Socket socket;
    private final RemoteControl remoteControl;
    private boolean isConnected = false;
    private static String SERVER_URL = "http://192.168.31.47:3000"; // 替换为实际的中继服务器地址

    // 连接回调接口
    public interface ConnectCallback {
        void onSuccess();
        void onError(String error);
    }

    private SocketManager() {
        SERVER_URL= Setting.getRemoteServer();
        remoteControl = new RemoteControl();
        initSocket();
    }

    public static SocketManager getInstance() {
        if (instance == null) {
            instance = new SocketManager();
        }
        return instance;
    }

    private void initSocket() {
        try {
            IO.Options opts = new IO.Options();
            opts.reconnection = true;
            opts.reconnectionDelay = 1000;
            opts.reconnectionDelayMax = 5000;
            opts.reconnectionAttempts = Integer.MAX_VALUE;
            opts.timeout = 20000;

            socket = IO.socket(SERVER_URL, opts);
            Log.d(TAG, "Socket initialized with server URL: " + SERVER_URL);
            setupEventHandlers();
        } catch (Exception e) {
            Log.e(TAG, "Socket initialization failed", e);
        }
    }

    private void setupEventHandlers() {
        Log.d(TAG, "Setting up socket event handlers");
        
        socket.on(Socket.EVENT_CONNECT, args -> {
            Log.d(TAG, "Connected to server");
            isConnected = true;
            authenticate();
        });

        socket.on(Socket.EVENT_DISCONNECT, args -> {
            Log.d(TAG, "Disconnected from server");
            isConnected = false;
        });

        socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
            Log.e(TAG, "Connection error: " + args[0]);
        });

        String remoteCommandEvent = SocketEvent.REMOTE_COMMAND.getValue();
        Log.d(TAG, "Registering remote command handler for event: " + remoteCommandEvent);
        
        socket.on(remoteCommandEvent, args -> {
            Log.d(TAG, "Remote command event handler triggered");
            Log.d(TAG, "Command args: " + (args.length > 0 ? args[0].toString() : "null"));
            if (args.length > 0) {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String action = data.getString("action");
                    String d=data.getString("data");
                    Log.d(TAG, "Processing command action: " + action+",data:"+d);
                    handleCommand(action,d);
                } catch (Exception e) {
                    Log.e(TAG, "Error handling command", e);
                }
            } else {
                Log.e(TAG, "Received empty command args");
            }
        });
        
        socket.on(SocketEvent.COMMAND_RESPONSE.getValue(), args -> {
            Log.d(TAG, "Command response: " + args[0]);
        });

        Log.d(TAG, "Event handlers setup completed");
        Log.d(TAG, "Registered events: connect, disconnect, error, remote_command, command_response");
    }

    private void handleCommand(String action,String data) {
        try {
            Log.d(TAG, "Handling command: " + action);
            // 验证命令权限
            String token = PairingManager.getInstance().getAuthToken();
            Log.d(TAG, "Validating command permission for token: " + token);
            if (!JwtManager.getInstance().hasPermission(token, "command." + action)) {
                Log.e(TAG, "Permission denied for command: " + action);
                return;
            }
            Log.d(TAG, "Command permission validated, executing action");
            boolean ok = remoteControl.doAction(action,data);
            if(!ok) {
                Log.e(TAG, "无效的命令: " + action);
            } else {
                Log.d(TAG, "Command executed successfully: " + action);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error executing command: " + action, e);
        }
    }

    private void authenticate() {
        String token = PairingManager.getInstance().getAuthToken();
        if (token != null) {
            Log.d(TAG, "Authenticating with token: " + token);
            JSONObject authData = new JSONObject();
            try {
                authData.put("token", token);
                authData.put("deviceId", PairingManager.getInstance().getDeviceId());
                Log.d(TAG, "Sending auth data: " + authData.toString());
                socket.emit(SocketEvent.AUTH.getValue(), authData);
            } catch (JSONException e) {
                Log.e(TAG, "Error creating auth data", e);
            }
        } else {
            Log.w(TAG, "No auth token available");
        }
    }

    public Socket getSocket() {
        return socket;
    }

    public void connect(ConnectCallback callback) {
        if (!isConnected) {
            // 添加一次性连接成功监听器
            socket.once(Socket.EVENT_CONNECT, args -> {
                callback.onSuccess();
            });

            // 添加一次性连接错误监听器
            socket.once(Socket.EVENT_CONNECT_ERROR, args -> {
                String error = args.length > 0 ? args[0].toString() : "Unknown error";
                callback.onError(error);
            });

            socket.connect();
        } else {
            // 如果已经连接，直接回调成功
            callback.onSuccess();
        }
    }

    public void connect() {
        if (!isConnected) {
            Log.d(TAG, "connect: "+SERVER_URL);
            socket.connect();
        }
    }

    public void disconnect() {
        if (isConnected) {
            socket.disconnect();
            isConnected = false;
        }
    }

    public boolean isConnected() {
        return isConnected;
    }
} 