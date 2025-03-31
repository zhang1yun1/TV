import jwt from 'jsonwebtoken';
import { config } from '../config';

interface TokenPayload {
    deviceId: string;
    iat?: number;
    exp?: number;
}

export const generateToken = async (deviceId: string): Promise<string> => {
    return new Promise((resolve, reject) => {
        jwt.sign(
            { deviceId },
            config.jwtSecret,
            { expiresIn: '30d' },
            (err, token) => {
                if (err) reject(err);
                else resolve(token as string);
            }
        );
    });
};

export const verifyToken = async (token: string): Promise<TokenPayload> => {
    return new Promise((resolve, reject) => {
        jwt.verify(token, config.jwtSecret, (err, decoded) => {
            if (err) reject(err);
            else resolve(decoded as TokenPayload);
        });
    });
}; 