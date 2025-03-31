import { Device, DeviceType } from '../../types';

export class AccessControl {
    private devicePermissions: Map<string, Set<string>> = new Map();

    /**
     * 添加设备访问权限
     */
    public addDevicePermission(sourceDeviceId: string, targetDeviceId: string): void {
        let permissions = this.devicePermissions.get(sourceDeviceId);
        if (!permissions) {
            permissions = new Set();
            this.devicePermissions.set(sourceDeviceId, permissions);
        }
        permissions.add(targetDeviceId);
    }

    /**
     * 移除设备访问权限
     */
    public removeDevicePermission(sourceDeviceId: string, targetDeviceId: string): void {
        const permissions = this.devicePermissions.get(sourceDeviceId);
        if (permissions) {
            permissions.delete(targetDeviceId);
            if (permissions.size === 0) {
                this.devicePermissions.delete(sourceDeviceId);
            }
        }
    }

    /**
     * 检查设备访问权限
     */
    public checkDevicePermission(sourceDeviceId: string, targetDeviceId: string): boolean {
        const permissions = this.devicePermissions.get(sourceDeviceId);
        return permissions ? permissions.has(targetDeviceId) : false;
    }

    /**
     * 验证设备类型权限
     */
    public validateDeviceTypePermission(device: Device, targetDevice: Device): boolean {
        // 遥控器设备可以控制电视设备
        if (device.deviceType === DeviceType.REMOTE && targetDevice.deviceType === DeviceType.TV) {
            return true;
        }
        return false;
    }

    /**
     * 清除设备的所有权限
     */
    public clearDevicePermissions(deviceId: string): void {
        // 清除作为源设备的权限
        this.devicePermissions.delete(deviceId);

        // 清除作为目标设备的权限
        for (const [sourceId, permissions] of this.devicePermissions.entries()) {
            if (permissions.has(deviceId)) {
                permissions.delete(deviceId);
                if (permissions.size === 0) {
                    this.devicePermissions.delete(sourceId);
                }
            }
        }
    }

    /**
     * 获取设备的所有授权目标
     */
    public getDevicePermissions(deviceId: string): string[] {
        const permissions = this.devicePermissions.get(deviceId);
        return permissions ? Array.from(permissions) : [];
    }

    /**
     * 获取可以访问指定设备的所有源设备
     */
    public getDeviceAccessors(targetDeviceId: string): string[] {
        const accessors: string[] = [];
        for (const [sourceId, permissions] of this.devicePermissions.entries()) {
            if (permissions.has(targetDeviceId)) {
                accessors.push(sourceId);
            }
        }
        return accessors;
    }
} 