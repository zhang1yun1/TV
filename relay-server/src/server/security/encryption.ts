import { createCipheriv, createDecipheriv, randomBytes } from 'crypto';

export class Encryption {
    private static readonly ALGORITHM = 'aes-256-gcm';
    private static readonly KEY_LENGTH = 32;
    private static readonly IV_LENGTH = 12;
    private static readonly AUTH_TAG_LENGTH = 16;

    private readonly key: Buffer;

    constructor(key?: string) {
        if (key) {
            this.key = Buffer.from(key, 'hex');
        } else {
            this.key = randomBytes(Encryption.KEY_LENGTH);
        }
    }

    /**
     * 加密数据
     */
    public encrypt(data: string): { encrypted: string; iv: string; authTag: string } {
        const iv = randomBytes(Encryption.IV_LENGTH);
        const cipher = createCipheriv(Encryption.ALGORITHM, this.key, iv);

        let encrypted = cipher.update(data, 'utf8', 'hex');
        encrypted += cipher.final('hex');

        return {
            encrypted,
            iv: iv.toString('hex'),
            authTag: (cipher.getAuthTag()).toString('hex')
        };
    }

    /**
     * 解密数据
     */
    public decrypt(encrypted: string, iv: string, authTag: string): string {
        const decipher = createDecipheriv(
            Encryption.ALGORITHM,
            this.key,
            Buffer.from(iv, 'hex')
        );

        decipher.setAuthTag(Buffer.from(authTag, 'hex'));

        let decrypted = decipher.update(encrypted, 'hex', 'utf8');
        decrypted += decipher.final('utf8');

        return decrypted;
    }

    /**
     * 获取加密密钥
     */
    public getKey(): string {
        return this.key.toString('hex');
    }

    /**
     * 生成新的加密密钥
     */
    public static generateKey(): string {
        return randomBytes(Encryption.KEY_LENGTH).toString('hex');
    }
}