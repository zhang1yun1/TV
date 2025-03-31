import dotenv from 'dotenv';

// 加载环境变量
dotenv.config();

interface Config {
    port: number;
    jwtSecret: string;
    nodeEnv: string;
    wsConfig: {
        pingInterval: number;
        pingTimeout: number;
    };
}

export const config: Config = {
    port: parseInt(process.env.PORT || '3000', 10),
    jwtSecret: process.env.JWT_SECRET || 'your-default-secret-key',
    nodeEnv: process.env.NODE_ENV || 'development',
    wsConfig: {
        pingInterval: parseInt(process.env.WS_PING_INTERVAL || '30000', 10),
        pingTimeout: parseInt(process.env.WS_PING_TIMEOUT || '5000', 10),
    }
}; 