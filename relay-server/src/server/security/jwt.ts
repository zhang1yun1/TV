import jwt from 'jsonwebtoken';
import { config } from '../config';

export interface JWTPayload {
    deviceId: string;
    deviceType: string;
    iat?: number;
    exp?: number;
}

export class JWT {
    /**
     * 生成 JWT token
     */
    public static async sign(payload: Omit<JWTPayload, 'iat' | 'exp'>): Promise<string> {
        return new Promise((resolve, reject) => {
            jwt.sign(
                payload,
                config.jwtSecret,
                {
                    expiresIn: '30d',
                    algorithm: 'HS256'
                },
                (err, token) => {
                    if (err) reject(err);
                    else resolve(token as string);
                }
            );
        });
    }

    /**
     * 验证 JWT token
     */
    public static async verify(token: string): Promise<JWTPayload> {
        return new Promise((resolve, reject) => {
            jwt.verify(
                token,
                config.jwtSecret,
                {
                    algorithms: ['HS256']
                },
                (err, decoded) => {
                    if (err) reject(err);
                    else resolve(decoded as JWTPayload);
                }
            );
        });
    }

    /**
     * 解码 JWT token（不验证签名）
     */
    public static decode(token: string): JWTPayload | null {
        try {
            return jwt.decode(token) as JWTPayload;
        } catch {
            return null;
        }
    }
}