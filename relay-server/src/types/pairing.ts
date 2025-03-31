/**
 * 配对请求接口
 */
export interface PairRequest {
    pairCode: string;       // 配对码
    deviceName: string;     // 设备名称
    deviceType: string;     // 设备类型
}

/**
 * 配对响应接口
 */
export interface PairResponse {
    success: boolean;       // 是否成功
    accessToken?: string;   // 访问令牌
    deviceId?: string;      // 设备ID
    error?: string;        // 错误信息
}

/**
 * 配对状态枚举
 */
export enum PairStatus {
    PENDING = 'pending',    // 等待配对
    SUCCESS = 'success',    // 配对成功
    FAILED = 'failed',      // 配对失败
    EXPIRED = 'expired',    // 已过期
} 