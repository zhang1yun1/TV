import { Socket } from 'socket.io';
import { ExtendedError } from 'socket.io/dist/namespace';
import { DeviceManager } from '../core/DeviceManager';

export const authMiddleware = (deviceManager: DeviceManager) => {
    return async (socket: Socket, next: (err?: ExtendedError) => void) => {
        try {
            const token = socket.handshake.auth.token;
            
            // 跳过配对请求的认证
            if (socket.handshake.query.pairing === 'true') {
                return next();
            }

            if (!token) {
                return next(new Error('Authentication error: No token provided'));
            }

            const device = await deviceManager.authenticateDevice(token);
            if (!device) {
                return next(new Error('Authentication error: Invalid token'));
            }

            // 将设备信息附加到socket对象
            (socket as any).device = device;
            next();
        } catch (error) {
            next(new Error('Authentication error: ' + (error instanceof Error ? error.message : 'Unknown error')));
        }
    };
}; 