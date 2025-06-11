import { createServer } from 'http';
import { Server } from 'socket.io';
import { config } from 'dotenv';
import path from 'path';
import express from 'express';
import jwt from 'jsonwebtoken';
import { 
  SocketEvent, 
  Device, 
  DeviceStatus, 
  RemoteCommand, 
  CommandResponse,
  PairRequest,
  PairResponse,
  PairStatus,
  DeviceType
} from '../types';

// 加载环境变量
config();

const app = express();
const httpServer = createServer(app);
const io = new Server(httpServer, {
  cors: {
    origin: process.env.CORS_ORIGIN || '*',
    methods: ['GET', 'POST'],
    credentials: true,
    allowedHeaders: ['*']
  },
  transports: ['websocket', 'polling'],
  pingTimeout: parseInt(process.env.WS_PING_TIMEOUT || '5000'),
  pingInterval: parseInt(process.env.WS_PING_INTERVAL || '30000')
});

// 存储已连接的设备
const connectedDevices = new Map<string, Device>();
// 存储待配对的设备
const pendingPairings = new Map<string, Device>();

// 静态文件服务
app.use(express.static(path.join(__dirname, '../public')));

class ServerPairingManager {
  private pairings = new Map<string, {
    deviceId: string,
    code: string,
    expireTime: number,
    socketId: string
  }>();

  generatePairingCode(deviceId: string, socketId: string): string {
    // 生成6位数字配对码
    const code = Math.floor(100000 + Math.random() * 900000).toString();
    const expireTime = Date.now() + 5 * 60 * 1000; // 5分钟过期
    
    this.pairings.set(code, {
      deviceId,
      code,
      expireTime,
      socketId
    });

    // 定时清理过期配对码
    setTimeout(() => {
      this.pairings.delete(code);
    }, 5 * 60 * 1000);

    return code;
  }

  validatePairing(code: string): { deviceId: string, socketId: string } | null {
    const pairing = this.pairings.get(code);
    if (!pairing || Date.now() > pairing.expireTime) {
      return null;
    }
    return {
      deviceId: pairing.deviceId,
      socketId: pairing.socketId
    };
  }

  removePairing(code: string) {
    this.pairings.delete(code);
  }
}

const pairingManager = new ServerPairingManager();

// Socket.IO 连接处理
io.on('connection', (socket) => {
  console.log(new Date().toLocaleString()+' '+'Client connected:', socket.id);

  // 处理TV端请求配对码
  socket.on('request_pairing_code', (data: { deviceId: string }) => {
    console.log(new Date().toLocaleString()+' '+'Received pairing code request:', data);
    
    const code = pairingManager.generatePairingCode(data.deviceId, socket.id);
    
    // 返回配对码给TV端
    socket.emit('pairing_code_generated', { code });
    console.log(new Date().toLocaleString()+' '+'Generated pairing code:', code);
  });

  // 处理遥控端配对请求
  socket.on('pair', async (request: { code: string,deviceName:string }) => {
    console.log(new Date().toLocaleString()+' '+'Pairing request received:', request);
    
    try {
      const pairing = pairingManager.validatePairing(request.code);
      if (!pairing) {
        throw new Error('无效的配对码或配对码已过期');
      }

      // 生成访问令牌
      const accessToken = jwt.sign(
        {
          deviceId: pairing.deviceId,
          deviceType: DeviceType.REMOTE,
          deviceName: request.deviceName??"TV",
          type:"tv"
        },
        process.env.JWT_SECRET || 'default_secret',
        { expiresIn: '720d' }
      );

      // 创建设备记录
      const device: Device = {
        deviceId: pairing.deviceId,
        deviceType: DeviceType.REMOTE,
        deviceName: request.deviceName??"TV",
        status: DeviceStatus.ONLINE,
        createdAt: Date.now(),
        updatedAt: Date.now(),
        socketId: socket.id
      };

      // 更新连接设备列表
      connectedDevices.set(device.deviceId, device);
      
      // 发送配对成功响应给遥控器客户端
      socket.emit('pairingResult', {
        success: true,
        accessToken,
        device: {
          id: device.deviceId,
          name: device.deviceName
        }
      });

      // 通知TV端配对成功
      io.to(pairing.socketId).emit('pairing_success', {
        deviceId: device.deviceId,
        token: accessToken
      });

      // 移除已使用的配对码
      pairingManager.removePairing(request.code);

      console.log(new Date().toLocaleString()+' '+'Pairing successful:', device.deviceId);
    } catch (error: any) {
      console.error('Pairing failed:', error);
      socket.emit('pairingResult', {
        success: false,
        message: error.message || '配对失败'
      });
    }
  });

  // 认证处理
  socket.on(SocketEvent.AUTH, async (data: { token: string, deviceId: string }) => {
    try {
      // 验证 JWT token
      const decoded = jwt.verify(data.token, process.env.JWT_SECRET || 'default_secret') as Device;
      
      // 更新设备状态
      const device: Device = {
        ...decoded,
        socketId: socket.id,
        status: DeviceStatus.ONLINE,
        lastSeen: Date.now(),
        updatedAt: Date.now()
      };
      
      // 存储设备信息
      connectedDevices.set(device.deviceId, device);
      
      // 加入设备房间
      socket.join(device.deviceId);
      console.log(new Date().toLocaleString()+' '+'Device joined room:', device.deviceId);
      
      // 发送认证成功响应
      socket.emit(SocketEvent.AUTH_SUCCESS, { device });
      
      // 广播设备上线消息
      io.emit(SocketEvent.DEVICE_ONLINE, { deviceId: device.deviceId });
      
      console.log(new Date().toLocaleString()+' '+'Device authenticated and online:', device.deviceId);
    } catch (error: any) {
      console.error('Authentication failed:', error);
      socket.emit(SocketEvent.AUTH_ERROR, { error: 'Invalid token' });
    }
  });

  // 远程控制命令处理
  socket.on(SocketEvent.REMOTE_COMMAND, async (command: RemoteCommand) => {
    console.log(new Date().toLocaleString()+' '+'Remote command received:', command);
    
    try {
      // 检查目标设备是否在线
      const targetDevice = connectedDevices.get(command.deviceId);
      if (!targetDevice) {
        throw new Error('Device not found');
      }
      
      if (targetDevice.status !== DeviceStatus.ONLINE) {
        console.log(new Date().toLocaleString()+' '+'Device status:', targetDevice.status);
        throw new Error('Target device is offline');
      }
      
      // 转发命令到目标设备
      console.log(new Date().toLocaleString()+' '+'Target device socket ID:', targetDevice.socketId);
      console.log(new Date().toLocaleString()+' '+'Target device status:', targetDevice.status);
      console.log(new Date().toLocaleString()+' '+'Command event name:', SocketEvent.REMOTE_COMMAND);
      
      // 同时使用 socket ID 和设备房间发送命令
      io.to(targetDevice.socketId!).emit(SocketEvent.REMOTE_COMMAND, command);
      io.to(targetDevice.deviceId).emit(SocketEvent.REMOTE_COMMAND, command);
      
      console.log('Command forwarded successfully to socket:', targetDevice.socketId);
      console.log(new Date().toLocaleString()+' '+'Command forwarded successfully to device room:', targetDevice.deviceId);
      // 发送响应给源设备
      const response: CommandResponse = {
        commandId: command.commandId || `cmd_${Date.now()}`,
        success: true,
        timestamp: Date.now()
      };
      socket.emit(SocketEvent.COMMAND_RESPONSE, response);
      
    } catch (error: any) {
      console.error('Command processing failed:', error);
      const errorResponse: CommandResponse = {
        commandId: command.commandId || `cmd_${Date.now()}`,
        success: false,
        error: error.message,
        timestamp: Date.now()
      };
      socket.emit(SocketEvent.COMMAND_RESPONSE, errorResponse);
    }
  });

  socket.on('disconnect', () => {
    // 查找并更新断开连接的设备状态
    for (const [deviceId, device] of connectedDevices.entries()) {
      if (device.socketId === socket.id) {
        device.status = DeviceStatus.OFFLINE;
        device.lastSeen = Date.now();
        device.updatedAt = Date.now();
        connectedDevices.set(deviceId, device);
        
        // 广播设备离线消息
        io.emit(SocketEvent.DEVICE_OFFLINE, { 
          deviceId,
          status: DeviceStatus.OFFLINE
        });
        
        console.log(new Date().toLocaleString()+' '+'Device disconnected and marked offline:', deviceId);
        break;
      }
    }
    console.log(new Date().toLocaleString()+' '+'Client disconnected:', socket.id);
  });
});

// 启动服务器
const PORT = process.env.PORT || 3000;
httpServer.listen(PORT, () => {
  console.log(new Date().toLocaleString()+' '+'Server is running on port '+PORT);
}); 