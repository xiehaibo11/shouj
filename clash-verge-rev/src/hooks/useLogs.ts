import { useState, useEffect } from 'react';
import { getLogs, clearLogs as apiClearLogs } from '../services/api';

export function useLogs() {
  const [logs, setLogs] = useState<any[]>([]);

  useEffect(() => {
    const fetchLogs = async () => {
      try {
        const data = await getLogs();
        setLogs(data || []);
      } catch (error) {
        console.error('获取日志失败:', error);
      }
    };

    fetchLogs();
    const interval = setInterval(fetchLogs, 2000);

    return () => clearInterval(interval);
  }, []);

  const clearLogs = async () => {
    try {
      await apiClearLogs();
      setLogs([]);
    } catch (error) {
      console.error('清除日志失败:', error);
    }
  };

  return {
    logs,
    clearLogs,
  };
}

