import { Device, DeviceType, DeviceStatus } from '../../types';
import { createHash } from 'crypto';

export class DeviceVerification {
    private readonly deviceSignatures: Map<string, string> = new Map();

    /**
     * 生成设备签名
     */
    public generateDeviceSignature(device: Device): string {
        const signatureData = {
            deviceId: device.deviceId,
            deviceName: device.deviceName,
            deviceType: device.deviceType,
            createdAt: device.createdAt
        };

        const signature = createHash('sha256')
            .update(JSON.stringify(signatureData))
            .digest('hex');

        this.deviceSignatures.set(device.deviceId, signature);
        return signature;
    }

    /**
     * 验证设备签名
     */
    public verifyDeviceSignature(device: Device, signature: string): boolean {
        const storedSignature = this.deviceSignatures.get(device.deviceId);
        return storedSignature === signature;
    }

    /**
     * 验证设备状态
     */
    public validateDeviceStatus(device: Device): boolean {
        return device.status === DeviceStatus.ONLINE;
    }

    /**
     * 验证设备类型
     */
    public validateDeviceType(device: Device, allowedTypes: DeviceType[]): boolean {
        return allowedTypes.includes(device.deviceType);
    }

    /**
     * 验证设备时间戳
     */
    public validateDeviceTimestamp(device: Device, maxAge: number = 5 * 60 * 1000): boolean {
        if (!device.lastSeen) return false;
        const now = Date.now();
        return (now - device.lastSeen) <= maxAge;
    }

    /**
     * 完整的设备验证
     */
    public validateDevice(
        device: Device,
        signature: string,
        allowedTypes: DeviceType[],
        maxAge?: number
    ): boolean {
        return (
            this.verifyDeviceSignature(device, signature) &&
            this.validateDeviceStatus(device) &&
            this.validateDeviceType(device, allowedTypes) &&
            this.validateDeviceTimestamp(device, maxAge)
        );
    }

    /**
     * 移除设备签名
     */
    public removeDeviceSignature(deviceId: string): void {
        this.deviceSignatures.delete(deviceId);
    }

    /**
     * 更新设备签名
     */
    public updateDeviceSignature(device: Device): string {
        return this.generateDeviceSignature(device);
    }
}