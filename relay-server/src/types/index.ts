export * from './device';
export * from './command';
export * from './pairing';
export * from './socket';

export interface RemoteCommand {
    commandId?: string;
    action: string;
    data:string;
    deviceId: string;
    timestamp: number;
} 