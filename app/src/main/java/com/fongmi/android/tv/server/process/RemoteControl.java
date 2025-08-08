package com.fongmi.android.tv.server.process;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.text.TextUtils;
import android.view.KeyEvent;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.event.ActionEvent;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.event.ServerEvent;
import com.fongmi.android.tv.server.Nano;
import com.fongmi.android.tv.server.impl.Process;
import com.fongmi.android.tv.ui.activity.Home2Activity;
import com.fongmi.android.tv.ui.activity.LiveActivity;
import com.fongmi.android.tv.ui.activity.SettingPlayerActivity;

import fi.iki.elonen.NanoHTTPD;

import java.util.Map;

public class RemoteControl implements Process {

    private final Instrumentation instrumentation;
    private final AudioManager audioManager;
    private final Context context;

    public RemoteControl() {
        instrumentation = new Instrumentation();
        audioManager = (AudioManager) App.get().getSystemService(Context.AUDIO_SERVICE);
        context = App.get();
    }

    @Override
    public boolean isRequest(NanoHTTPD.IHTTPSession session, String path) {
        return path.startsWith("/remote");
    }

    @Override
    public NanoHTTPD.Response doResponse(NanoHTTPD.IHTTPSession session, String path, Map<String, String> files) {
        try {
            String action = path.substring("/remote/".length());
            boolean ok=doAction(action,"");
            if(!ok){
                return Nano.error("未知的遥控器命令: " + action);
            }
            
            return Nano.ok();
        } catch (Exception e) {
            return Nano.error(e.getMessage());
        }
    }
    public boolean doAction(String action,String data){
        switch (action) {
            case "dpad/up":
                simulateKeyEvent(KeyEvent.KEYCODE_DPAD_UP);
                break;
            case "dpad/down":
                simulateKeyEvent(KeyEvent.KEYCODE_DPAD_DOWN);
                break;
            case "dpad/left":
                simulateKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT);
                break;
            case "dpad/right":
                simulateKeyEvent(KeyEvent.KEYCODE_DPAD_RIGHT);
                break;
            case "dpad/center":
                simulateKeyEvent(KeyEvent.KEYCODE_DPAD_CENTER);
                break;
            case "home":
                goToHome();
                break;
            case "menu":
                simulateKeyEvent(KeyEvent.KEYCODE_MENU);
                break;
            case "back":
                simulateKeyEvent(KeyEvent.KEYCODE_BACK);
                break;
            case "volume/up":
                adjustVolume(AudioManager.ADJUST_RAISE);
                break;
            case "volume/down":
                adjustVolume(AudioManager.ADJUST_LOWER);
                break;
            case "play":
                ActionEvent.send(ActionEvent.PLAY);
                break;
            case "pause":
                ActionEvent.send(ActionEvent.PAUSE);
                break;
            case "prev":
                ActionEvent.send(ActionEvent.PREV);
                break;
            case "next":
                ActionEvent.send(ActionEvent.NEXT);
                break;
            case "next_source":
                simulateKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT);
                break;
            case "subtitle":
                simulateKeyEvent(KeyEvent.KEYCODE_CAPTIONS);
                break;
            case "audio_track":
                simulateKeyEvent(KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK);
                break;
            case "video_track":
                simulateKeyEvent(KeyEvent.KEYCODE_TV_INPUT);
                break;
            case "set_opening":
                simulateKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY);
                break;
            case "set_ending":
                simulateKeyEvent(KeyEvent.KEYCODE_MEDIA_STOP);
                break;
            case "live":
                goToLive();
                break;
            case "settings":
                goToSettings();
                break;
            case "search":
            {
                if (!TextUtils.isEmpty(data))
                    ServerEvent.search(data);
                break;
            }
            default:
                return false;
        }
        return true;
    }

    private void goToHome() {
        try {
            Intent intent = new Intent(context, Home2Activity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void goToLive() {
        try {
            Intent intent = new Intent(context, LiveActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(intent);
        } catch (Exception e) { 
            e.printStackTrace();
        }
    }
    private void goToSettings() {
        try {
            Intent intent = new Intent(context, SettingPlayerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    

    private void simulateKeyEvent(final int keyCode) {
        try {
            instrumentation.sendKeyDownUpSync(keyCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void adjustVolume(int direction) {
        try {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                direction,
                AudioManager.FLAG_SHOW_UI
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 