package com.fongmi.android.tv;


import android.content.Intent;
import android.provider.Settings;

import com.fongmi.android.tv.player.Players;
import com.github.catvod.utils.Prefers;

public class Setting {

    public static String getDoh() {
        return Prefers.getString("doh");
    }

    public static void putDoh(String doh) {
        Prefers.put("doh", doh);
    }

    public static String getKeyword() {
        return Prefers.getString("keyword");
    }

    public static void putKeyword(String keyword) {
        Prefers.put("keyword", keyword);
    }

    public static String getHot() {
        return Prefers.getString("hot");
    }

    public static void putHot(String hot) {
        Prefers.put("hot", hot);
    }

    public static String getUa() {
        return Prefers.getString("ua");
    }

    public static void putUa(String ua) {
        Prefers.put("ua", ua);
    }

    public static int getWall() {
        return Prefers.getInt("wall", 1);
    }

    public static void putWall(int wall) {
        Prefers.put("wall", wall);
    }

    public static int getReset() {
        return Prefers.getInt("reset", 0);
    }

    public static void putReset(int reset) {
        Prefers.put("reset", reset);
    }
    public static int getPlayer() {
        return Prefers.getInt("player", Players.EXO);
    }

    public static void putPlayer(int player) {
        Prefers.put("player", player);
    }

    public static int getLivePlayer() {
        return Prefers.getInt("player_live", getPlayer());
    }

    public static void putLivePlayer(int player) {
        Prefers.put("player_live", player);
    }

    public static int getDecode() {
        return Prefers.getInt("decode", Players.HARD);
    }

    public static void putDecode(int decode) {
        Prefers.put("decode", decode);
    }

    public static int getRender() {
        return Prefers.getInt("render", 0);
    }

    public static void putRender(int render) {
        Prefers.put("render", render);
    }

    public static int getQuality() {
        return Prefers.getInt("quality", 2);
    }

    public static void putQuality(int quality) {
        Prefers.put("quality", quality);
    }

    public static int getSize() {
        return Prefers.getInt("size", 2);
    }

    public static void putSize(int size) {
        Prefers.put("size", size);
    }

    public static int getViewType(int viewType) {
        return Prefers.getInt("viewType", viewType);
    }

    public static void putViewType(int viewType) {
        Prefers.put("viewType", viewType);
    }

    public static int getScale() {
        return Prefers.getInt("scale");
    }

    public static void putScale(int scale) {
        Prefers.put("scale", scale);
    }

    public static int getLiveScale() {
        return Prefers.getInt("scale_live", getScale());
    }

    public static void putLiveScale(int scale) {
        Prefers.put("scale_live", scale);
    }

    public static int getBuffer() {
        return Math.min(Math.max(Prefers.getInt("buffer"), 1), 10);
    }

    public static void putBuffer(int buffer) {
        Prefers.put("buffer", buffer);
    }

    public static int getBackground() {
        return Prefers.getInt("background", 2);
    }

    public static void putBackground(int background) {
        Prefers.put("background", background);
    }

    public static int getSiteMode() {
        return Prefers.getInt("site_mode");
    }

    public static void putSiteMode(int mode) {
        Prefers.put("site_mode", mode);
    }

    public static int getSyncMode() {
        return Prefers.getInt("sync_mode");
    }

    public static void putSyncMode(int mode) {
        Prefers.put("sync_mode", mode);
    }

    public static boolean isIncognito() {
        return Prefers.getBoolean("incognito");
    }

    public static void putIncognito(boolean incognito) {
        Prefers.put("incognito", incognito);
    }

    public static boolean isBootLive() {
        return Prefers.getBoolean("boot_live");
    }

    public static void putBootLive(boolean boot) {
        Prefers.put("boot_live", boot);
    }

    public static boolean isInvert() {
        return Prefers.getBoolean("invert");
    }

    public static void putInvert(boolean invert) {
        Prefers.put("invert", invert);
    }

    public static boolean isAcross() {
        return Prefers.getBoolean("across", true);
    }

    public static void putAcross(boolean across) {
        Prefers.put("across", across);
    }

    public static boolean isChange() {
        return Prefers.getBoolean("change", true);
    }

    public static void putChange(boolean change) {
        Prefers.put("change", change);
    }

    public static boolean getUpdate() {
        return Prefers.getBoolean("update", true);
    }

    public static void putUpdate(boolean update) {
        Prefers.put("update", update);
    }

    public static boolean isCaption() {
        return Prefers.getBoolean("caption");
    }

    public static void putCaption(boolean caption) {
        Prefers.put("caption", caption);
    }

    public static boolean isTunnel() {
        return Prefers.getBoolean("tunnel");
    }

    public static void putTunnel(boolean tunnel) {
        Prefers.put("tunnel", tunnel);
    }
    public static boolean isGoLive() {
        return Prefers.getBoolean("goLive",false);
    }

    public static void putGoLive(boolean goLive) {
        Prefers.put("goLive", goLive);
    }
    public static String getRemoteServer() {
        return Prefers.getString("remote_server","");
    }

    public static void putRemoteServer(String rs) {
        Prefers.put("remote_server", rs);
    }
    public static boolean isAudioPrefer() {
        return Prefers.getBoolean("audio_prefer");
    }

    public static void putAudioPrefer(boolean audioPrefer) {
        Prefers.put("audio_prefer", audioPrefer);
    }

    public static boolean isVideoPrefer() {
        return Prefers.getBoolean("video_prefer");
    }

    public static void putVideoPrefer(boolean videoPrefer) {
        Prefers.put("video_prefer", videoPrefer);
    }

    public static boolean isPreferAAC() {
        return Prefers.getBoolean("prefer_aac");
    }

    public static void putPreferAAC(boolean preferAAC) {
        Prefers.put("prefer_aac", preferAAC);
    }

    public static boolean isDanmakuLoad() {
        return Prefers.getBoolean("danmaku_load");
    }

    public static void putDanmakuLoad(boolean danmakuLoad) {
        Prefers.put("danmaku_load", danmakuLoad);
    }

    public static boolean isDanmakuShow() {
        return Prefers.getBoolean("danmaku_show");
    }

    public static void putDanmakuShow(boolean danmakuShow) {
        Prefers.put("danmaku_show", danmakuShow);
    }

    public static boolean isZhuyin() {
        return Prefers.getBoolean("zhuyin");
    }

    public static void putZhuyin(boolean zhuyin) {
        Prefers.put("zhuyin", zhuyin);
    }

    public static float getSpeed() {
        return Math.min(Math.max(Prefers.getFloat("speed", 3), 2), 5);
    }

    public static void putSpeed(float speed) {
        Prefers.put("speed", speed);
    }

    public static float getSubtitleTextSize() {
        return Prefers.getFloat("subtitle_text_size");
    }

    public static void putSubtitleTextSize(float value) {
        Prefers.put("subtitle_text_size", value);
    }

    public static float getSubtitlePosition() {
        return Prefers.getFloat("subtitle_position");
    }

    public static void putSubtitlePosition(float value) {
        Prefers.put("subtitle_position", value);
    }

    public static float getThumbnail() {
        return 0.3f * getQuality() + 0.4f;
    }

    public static boolean isBackgroundOff() {
        return getBackground() == 0;
    }

    public static boolean isBackgroundOn() {
        return getBackground() == 1 || getBackground() == 2;
    }

    public static boolean isBackgroundPiP() {
        return getBackground() == 2;
    }

    public static boolean hasCaption() {
        return new Intent(Settings.ACTION_CAPTIONING_SETTINGS).resolveActivity(App.get().getPackageManager()) != null;
    }
    public static boolean isAggregatedSearch() {
        return Prefers.getBoolean("aggregated_search", false);
    }

    public static void putAggregatedSearch(boolean search) {
        Prefers.put("aggregated_search", search);
    }
    public static void putHomeUI(int key) {
        Prefers.put("home_ui", key);
    }

    public static int getHomeUI() {
        return Prefers.getInt("home_ui", 1);
    }

    public static void putHomeButtons(String buttons) {
        Prefers.put("home_buttons", buttons);
    }

    public static String getHomeButtons(String defaultValue) {
        return Prefers.getString("home_buttons", defaultValue);
    }

    public static void putHomeButtonsSorted(String buttons) {
        Prefers.put("home_buttons_sorted", buttons);
    }

    public static String getHomeButtonsSorted(String defaultValue) {
        return Prefers.getString("home_buttons_sorted", defaultValue);
    }

    public static boolean isHomeHistory() {
        return Prefers.getBoolean("home_history", true);
    }

    public static void putHomeHistory(boolean show) {
        Prefers.put("home_history", show);
    }

    public static void putConfigCache(int key) {
        Prefers.put("config_cache", key);
    }

    public static int getConfigCache() {
        return Math.min(Prefers.getInt("config_cache", 0), 2);
    }

    public static void putParseWebView(int key) {
        Prefers.put("parse_webview", key);
    }

    public static int getParseWebView() {
        return Prefers.getInt("parse_webview", 0);
    }

    public static boolean isSiteSearch() {
        return Prefers.getBoolean("site_search", false);
    }

    public static void putSiteSearch(boolean search) {
        Prefers.put("site_search", search);
    }

    public static boolean isRemoveAd() {
        return Prefers.getBoolean("remove_ad", false);
    }

    public static void putRemoveAd(boolean remove) {
        Prefers.put("remove_ad", remove);
    }

    public static String getThunderCacheDir() {
        return Prefers.getString("thunder_cache_dir", "");
    }

    public static void putThunderCacheDir(String dir) {
        Prefers.put("thunder_cache_dir", dir);
    }

    public static void putHomeMenuKey(int key) {
        Prefers.put("home_menu_key", key);
    }

    public static int getHomeMenuKey() {
        return Prefers.getInt("home_menu_key", 0);
    }
    public static boolean isHomeSiteLock() {
        return Prefers.getBoolean("home_site_lock", false);
    }

    public static void putHomeSiteLock(boolean lock) {
        Prefers.put("home_site_lock", lock);
    }

    public static boolean isAutoPlayHistory() {
        return Prefers.getBoolean("auto_play_history", false);
    }

    public static void putAutoPlayHistory(boolean autoPlay) {
        Prefers.put("auto_play_history", autoPlay);
    }

}
