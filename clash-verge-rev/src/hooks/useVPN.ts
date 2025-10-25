import { useState, useEffect, useCallback } from 'react';
import { startVPN, stopVPN, getVPNStatus } from '../services/vpn';

export function useVPN() {
  const [isConnected, setIsConnected] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    checkStatus();
    const interval = setInterval(checkStatus, 3000);
    return () => clearInterval(interval);
  }, []);

  const checkStatus = async () => {
    try {
      const status = await getVPNStatus();
      setIsConnected(status.connected);
    } catch (err) {
      console.error('检查VPN状态失败:', err);
    }
  };

  const connect = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      // TODO: 从配置获取VPN参数
      const success = await startVPN({
        serverAddress: '127.0.0.1',
        serverPort: 7897,
      });
      if (success) {
        setIsConnected(true);
      } else {
        setError('连接失败');
      }
    } catch (err: any) {
      setError(err.message || '连接失败');
      console.error('VPN连接失败:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  const disconnect = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const success = await stopVPN();
      if (success) {
        setIsConnected(false);
      } else {
        setError('断开失败');
      }
    } catch (err: any) {
      setError(err.message || '断开失败');
      console.error('VPN断开失败:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  const toggle = useCallback(async () => {
    if (isConnected) {
      await disconnect();
    } else {
      await connect();
    }
  }, [isConnected, connect, disconnect]);

  return {
    isConnected,
    loading,
    error,
    connect,
    disconnect,
    toggle,
  };
}

