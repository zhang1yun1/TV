/**
 * 命令类型枚举
 */
export enum CommandType {
    CONTROL = 'control',       // 控制命令
    HEARTBEAT = 'heartbeat',   // 心跳
    RESPONSE = 'response',     // 响应
    SYSTEM = 'system',         // 系统命令
}

/**
 * 控制动作枚举
 */
export enum ControlAction {
    // 方向控制
    DPAD_UP = 'dpad/up',
    DPAD_DOWN = 'dpad/down',
    DPAD_LEFT = 'dpad/left',
    DPAD_RIGHT = 'dpad/right',
    DPAD_CENTER = 'dpad/center',
    
    // 功能键
    HOME = 'home',
    MENU = 'menu',
    BACK = 'back',
    
    // 音量控制
    VOLUME_UP = 'volume/up',
    VOLUME_DOWN = 'volume/down',
    
    // 媒体控制
    PREV = 'prev',
    NEXT = 'next',
    NEXT_SOURCE = 'next_source',
    
    // 字幕和音轨
    SUBTITLE = 'subtitle',
    AUDIO_TRACK = 'audio_track',
    VIDEO_TRACK = 'video_track',
    
    // 片段控制
    SET_OPENING = 'set_opening',
    SET_ENDING = 'set_ending',
}

/**
 * 远程命令接口
 */
export interface RemoteCommand {
    commandId: string;         // 命令唯一标识
    type: CommandType;         // 命令类型
    action: string;           // 控制动作
    deviceId: string;         // 目标设备ID
    timestamp: number;         // 时间戳
    params?: Record<string, any>; // 附加参数
}

/**
 * 命令响应接口
 */
export interface CommandResponse {
    commandId: string;         // 对应的命令ID
    success: boolean;          // 是否成功
    error?: string;           // 错误信息
    data?: any;               // 响应数据
    timestamp: number;        // 时间戳
} 