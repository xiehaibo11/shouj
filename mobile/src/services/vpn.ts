import { NativeModules, Platform } from 'react-native';

const { VPNModule } = NativeModules;

/**
 * 请求VPN权限
 */
export async function requestVPNPermission(): Promise<boolean> {
  try {
    if (Platform.OS === 'android') {
      return await VPNModule.requestPermission();
    } else if (Platform.OS === 'ios') {
      // iOS 需要在设置中手动授权
      return true;
    }
  } catch (error) {
    console.error('请求VPN权限失败:', error);
  }
  return false;
}

/**
 * 启动VPN
 */
export async function startVPN(config: VPNConfig): Promise<boolean> {
  try {
    if (Platform.OS === 'android') {
      return await VPNModule.start(config);
    } else if (Platform.OS === 'ios') {
      return await VPNModule.start(config);
    }
  } catch (error) {
    console.error('启动VPN失败:', error);
  }
  return false;
}

/**
 * 停止VPN
 */
export async function stopVPN(): Promise<boolean> {
  try {
    if (Platform.OS === 'android') {
      return await VPNModule.stop();
    } else if (Platform.OS === 'ios') {
      return await VPNModule.stop();
    }
  } catch (error) {
    console.error('停止VPN失败:', error);
  }
  return false;
}

/**
 * 获取VPN状态
 */
export async function getVPNStatus(): Promise<VPNStatus> {
  try {
    if (Platform.OS === 'android') {
      return await VPNModule.getStatus();
    } else if (Platform.OS === 'ios') {
      return await VPNModule.getStatus();
    }
  } catch (error) {
    console.error('获取VPN状态失败:', error);
  }
  return { connected: false };
}

export interface VPNConfig {
  serverAddress: string;
  serverPort: number;
  password?: string;
  method?: string;
  routes?: string[];
  dns?: string[];
}

export interface VPNStatus {
  connected: boolean;
  serverAddress?: string;
  connectionTime?: number;
  upload?: number;
  download?: number;
}

