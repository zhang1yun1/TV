 package com.fongmi.android.tv.server;

public enum SocketEvent {
    CONNECT("connect"),
    DISCONNECT("disconnect"),
    ERROR("error"),
    AUTH("auth"),
    AUTH_SUCCESS("auth_success"),
    AUTH_ERROR("auth_error"),
    REMOTE_COMMAND("remote_command"),
    COMMAND_RESPONSE("command_response");

    private final String value;

    SocketEvent(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}