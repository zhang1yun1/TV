import { Server as SocketIOServer } from 'socket.io';
import { createServer, Server as HTTPServer } from 'http';
import { SocketServerConfig, SocketEvent } from '../../types';
import { DeviceManager } from './DeviceManager';
import { CommandHandler } from './CommandHandler';
import { authMiddleware } from '../middleware/authMiddleware';

export class SocketServer {
    private io: SocketIOServer;
    private httpServer: HTTPServer;
    private deviceManager: DeviceManager;
    private commandHandler: CommandHandler;

    constructor(private config: SocketServerConfig) {
        this.httpServer = createServer();
        this.io = new SocketIOServer(this.httpServer, {
            cors: this.config.cors,
            pingInterval: this.config.pingInterval || 30000,
            pingTimeout: this.config.pingTimeout || 5000,
        });

        this.deviceManager = new DeviceManager();
        this.commandHandler = new CommandHandler(this.io, this.deviceManager);

        this.setupMiddleware();
        this.setupEventHandlers();
    }

    private setupMiddleware(): void {
        this.io.use(authMiddleware(this.deviceManager));
    }

    private setupEventHandlers(): void {
        this.io.on(SocketEvent.CONNECT, (socket) => {
            console.log(`Device connected: ${socket.id}`);

            // 处理设备认证
            socket.on(SocketEvent.AUTH, async (data) => {
                try {
                    const device = await this.deviceManager.authenticateDevice(data.token);
                    if (device) {
                        socket.emit(SocketEvent.AUTH_SUCCESS, { device });
                        await this.deviceManager.updateDeviceSocket(device.deviceId, socket.id);
                    } else {
                        socket.emit(SocketEvent.AUTH_ERROR, { error: 'Invalid token' });
                        socket.disconnect();
                    }
                } catch (error: any) {
                    socket.emit(SocketEvent.AUTH_ERROR, { error: error.message });
                    socket.disconnect();
                }
            });

            // 处理配对请求
            socket.on(SocketEvent.PAIR_REQUEST, async (request) => {
                try {
                    const response = await this.deviceManager.handlePairRequest(request);
                    socket.emit(SocketEvent.PAIR_RESPONSE, response);
                } catch (error: any) {
                    socket.emit(SocketEvent.PAIR_RESPONSE, {
                        success: false,
                        error: error.message
                    });
                }
            });

            // 处理远程命令
            socket.on(SocketEvent.REMOTE_COMMAND, async (command) => {
                try {
                    await this.commandHandler.handleCommand(socket, command);
                } catch (error: any) {
                    socket.emit(SocketEvent.COMMAND_RESPONSE, {
                        commandId: command.commandId,
                        success: false,
                        error: error.message,
                        timestamp: Date.now()
                    });
                }
            });

            // 处理心跳
            socket.on(SocketEvent.HEARTBEAT, () => {
                socket.emit(SocketEvent.HEARTBEAT_ACK, {
                    timestamp: Date.now()
                });
                this.deviceManager.updateDeviceLastSeen(socket.id);
            });

            // 处理断开连接
            socket.on(SocketEvent.DISCONNECT, () => {
                console.log(`Device disconnected: ${socket.id}`);
                this.deviceManager.handleDeviceDisconnect(socket.id);
            });
        });
    }

    public start(): void {
        this.httpServer.listen(this.config.port, () => {
            console.log(`Socket.IO server is running on port ${this.config.port}`);
        });
    }

    public stop(): void {
        this.io.close(() => {
            console.log('Socket.IO server stopped');
        });
    }
} 