import { RemoteCommand, CommandResponse } from './command';
import { PairRequest, PairResponse } from './pairing';
import { Device } from './device';

/**
 * Socket事件名称枚举
 */
export enum SocketEvent {
    // 连接相关
    CONNECT = 'connect',
    DISCONNECT = 'disconnect',
    ERROR = 'error',
    
    // 认证相关
    AUTH = 'auth',
    AUTH_SUCCESS = 'auth_success',
    AUTH_ERROR = 'auth_error',
    
    // 配对相关
    PAIR_REQUEST = 'pair_request',
    PAIR_RESPONSE = 'pair_response',
    
    // 命令相关
    REMOTE_COMMAND = 'remote_command',
    COMMAND_RESPONSE = 'command_response',
    
    // 心跳相关
    HEARTBEAT = 'heartbeat',
    HEARTBEAT_ACK = 'heartbeat_ack',
    
    // 状态相关
    STATUS_UPDATE = 'status_update',
    DEVICE_ONLINE = 'device_online',
    DEVICE_OFFLINE = 'device_offline',
}

/**
 * Socket事件数据类型映射
 */
export interface SocketEventMap {
    [SocketEvent.CONNECT]: undefined;
    [SocketEvent.DISCONNECT]: string;
    [SocketEvent.ERROR]: Error;
    
    [SocketEvent.AUTH]: { token: string };
    [SocketEvent.AUTH_SUCCESS]: { device: Device };
    [SocketEvent.AUTH_ERROR]: { error: string };
    
    [SocketEvent.PAIR_REQUEST]: PairRequest;
    [SocketEvent.PAIR_RESPONSE]: PairResponse;
    
    [SocketEvent.REMOTE_COMMAND]: RemoteCommand;
    [SocketEvent.COMMAND_RESPONSE]: CommandResponse;
    
    [SocketEvent.HEARTBEAT]: undefined;
    [SocketEvent.HEARTBEAT_ACK]: { timestamp: number };
    
    [SocketEvent.STATUS_UPDATE]: { deviceId: string; status: string };
    [SocketEvent.DEVICE_ONLINE]: { deviceId: string };
    [SocketEvent.DEVICE_OFFLINE]: { deviceId: string };
}

/**
 * Socket客户端配置接口
 */
export interface SocketClientConfig {
    serverUrl: string;
    accessToken: string;
    autoReconnect?: boolean;
    reconnectAttempts?: number;
    reconnectDelay?: number;
    pingInterval?: number;
    pingTimeout?: number;
}

/**
 * Socket服务器配置接口
 */
export interface SocketServerConfig {
    port: number;
    cors?: {
        origin: string | string[];
        methods?: string[];
    };
    pingInterval?: number;
    pingTimeout?: number;
} 