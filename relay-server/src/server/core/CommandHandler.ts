import { Server as SocketIOServer, Socket } from 'socket.io';
import { RemoteCommand, CommandResponse, SocketEvent, CommandType } from '../../types';
import { DeviceManager } from './DeviceManager';

export class CommandHandler {
    constructor(
        private io: SocketIOServer,
        private deviceManager: DeviceManager
    ) {}

    public async handleCommand(socket: Socket, command: RemoteCommand): Promise<void> {
        try {
            // 验证命令
            this.validateCommand(command);

            // 获取目标设备的socket ID
            const targetSocketId = this.deviceManager.getDeviceSocketId(command.deviceId);
            if (!targetSocketId) {
                throw new Error('Target device is offline');
            }

            // 获取目标设备的socket
            const targetSocket = this.io.sockets.sockets.get(targetSocketId);
            if (!targetSocket) {
                throw new Error('Target device not connected');
            }

            // 转发命令到目标设备
            const response = await this.forwardCommand(targetSocket, command);

            // 发送响应给源设备
            socket.emit(SocketEvent.COMMAND_RESPONSE, response);

        } catch (error) {
            const errorResponse: CommandResponse = {
                commandId: command.commandId || 'unknown',
                success: false,
                error: error instanceof Error ? error.message : 'Unknown error',
                timestamp: Date.now()
            };
            socket.emit(SocketEvent.COMMAND_RESPONSE, errorResponse);
        }
    }

    private validateCommand(command: RemoteCommand): void {
        if (!command.commandId || !command.deviceId || !command.action) {
            throw new Error('Invalid command format');
        }

        // 验证目标设备是否存在
        const targetDevice = this.deviceManager.getDevice(command.deviceId);
        if (!targetDevice) {
            throw new Error('Target device not found');
        }
    }

    private async forwardCommand(targetSocket: Socket, command: RemoteCommand): Promise<CommandResponse> {
        return new Promise((resolve, reject) => {
            targetSocket.timeout(5000).emit(SocketEvent.REMOTE_COMMAND, command, (err: Error | null, response: CommandResponse) => {
                if (err) {
                    reject(new Error('Command timeout'));
                } else {
                    resolve(response);
                }
            });
        });
    }
} 