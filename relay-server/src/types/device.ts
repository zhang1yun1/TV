/**
 * 设备类型枚举
 */
export enum DeviceType {
    TV = 'tv',           // 电视设备
    REMOTE = 'remote',   // 遥控设备
}

/**
 * 设备状态枚举
 */
export enum DeviceStatus {
    OFFLINE = 'offline',     // 离线
    ONLINE = 'online',       // 在线
    PAIRING = 'pairing',     // 配对中
}

/**
 * 设备信息接口
 */
export interface Device {
    deviceId: string;        // 设备唯一标识
    deviceName: string;      // 设备名称
    deviceType: DeviceType;  // 设备类型
    status: DeviceStatus;    // 设备状态
    socketId?: string;       // Socket连接ID
    pairCode?: string;       // 配对码
    pairCodeExpireTime?: number; // 配对码过期时间
    accessToken?: string;    // 访问令牌
    lastSeen?: number;       // 最后在线时间
    createdAt: number;       // 创建时间
    updatedAt: number;       // 更新时间
} 