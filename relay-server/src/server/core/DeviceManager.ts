import { Device, DeviceStatus, DeviceType, PairRequest, PairResponse, PairStatus } from '../../types';
import { generateToken, verifyToken } from '../utils/jwt';
import { generateId } from '../utils/id';

export class DeviceManager {
    private devices: Map<string, Device> = new Map();
    private socketMap: Map<string, string> = new Map(); // socketId -> deviceId
    private pairCodes: Map<string, string> = new Map(); // pairCode -> deviceId

    // 创建新设备
    public async createDevice(deviceName: string, deviceType: DeviceType): Promise<Device> {
        const deviceId = generateId();
        const device: Device = {
            deviceId,
            deviceName,
            deviceType,
            status: DeviceStatus.OFFLINE,
            createdAt: Date.now(),
            updatedAt: Date.now()
        };

        this.devices.set(deviceId, device);
        return device;
    }

    // 生成配对码
    public async generatePairCode(deviceId: string): Promise<string> {
        const device = this.devices.get(deviceId);
        if (!device) {
            throw new Error('Device not found');
        }

        const pairCode = Math.random().toString(36).substr(2, 6).toUpperCase();
        device.pairCode = pairCode;
        device.pairCodeExpireTime = Date.now() + 5 * 60 * 1000; // 5分钟有效期
        device.status = DeviceStatus.PAIRING;
        device.updatedAt = Date.now();

        this.pairCodes.set(pairCode, deviceId);
        this.devices.set(deviceId, device);

        return pairCode;
    }

    // 处理配对请求
    public async handlePairRequest(request: PairRequest): Promise<PairResponse> {
        const deviceId = this.pairCodes.get(request.pairCode);
        if (!deviceId) {
            return {
                success: false,
                error: 'Invalid pair code'
            };
        }

        const device = this.devices.get(deviceId);
        if (!device || !this.isValidPairCode(device)) {
            return {
                success: false,
                error: 'Pair code expired'
            };
        }

        const accessToken = await generateToken(deviceId);
        device.accessToken = accessToken;
        device.status = DeviceStatus.OFFLINE;
        device.updatedAt = Date.now();

        this.devices.set(deviceId, device);
        this.pairCodes.delete(request.pairCode);

        return {
            success: true,
            accessToken,
            deviceId
        };
    }

    // 认证设备
    public async authenticateDevice(token: string): Promise<Device | null> {
        try {
            const payload = await verifyToken(token);
            const device = this.devices.get(payload.deviceId);
            if (!device) {
                return null;
            }
            return device;
        } catch (error) {
            return null;
        }
    }

    // 更新设备Socket ID
    public async updateDeviceSocket(deviceId: string, socketId: string): Promise<void> {
        const device = this.devices.get(deviceId);
        if (!device) {
            throw new Error('Device not found');
        }

        const oldSocketId = device.socketId;
        if (oldSocketId) {
            this.socketMap.delete(oldSocketId);
        }

        device.socketId = socketId;
        device.status = DeviceStatus.ONLINE;
        device.lastSeen = Date.now();
        device.updatedAt = Date.now();

        this.devices.set(deviceId, device);
        this.socketMap.set(socketId, deviceId);
    }

    // 更新设备最后在线时间
    public async updateDeviceLastSeen(socketId: string): Promise<void> {
        const deviceId = this.socketMap.get(socketId);
        if (!deviceId) {
            return;
        }

        const device = this.devices.get(deviceId);
        if (!device) {
            return;
        }

        device.lastSeen = Date.now();
        device.updatedAt = Date.now();
        this.devices.set(deviceId, device);
    }

    // 处理设备断开连接
    public async handleDeviceDisconnect(socketId: string): Promise<void> {
        const deviceId = this.socketMap.get(socketId);
        if (!deviceId) {
            return;
        }

        const device = this.devices.get(deviceId);
        if (!device) {
            return;
        }

        device.status = DeviceStatus.OFFLINE;
        device.socketId = undefined;
        device.updatedAt = Date.now();

        this.devices.set(deviceId, device);
        this.socketMap.delete(socketId);
    }

    // 获取设备信息
    public getDevice(deviceId: string): Device | undefined {
        return this.devices.get(deviceId);
    }

    // 获取设备的Socket ID
    public getDeviceSocketId(deviceId: string): string | undefined {
        const device = this.devices.get(deviceId);
        return device?.socketId;
    }

    // 检查配对码是否有效
    private isValidPairCode(device: Device): boolean {
        return device.pairCode != null &&
               device.pairCodeExpireTime != null &&
               device.pairCodeExpireTime > Date.now();
    }
} 