import { useState, useEffect } from 'react';
import { getSettings, updateSettings as apiUpdateSettings } from '../services/api';
import { saveSettings as saveLocalSettings } from '../services/initialize';

const DEFAULT_SETTINGS = {
  autoStart: false,
  autoConnect: false,
  darkMode: false,
  mixedPort: 7897,
  allowLan: false,
  ipv6: false,
  batteryOptimization: false,
  dataCompression: false,
  logLevel: 'info',
};

export function useSettings() {
  const [settings, setSettings] = useState(DEFAULT_SETTINGS);

  useEffect(() => {
    loadSettings();
  }, []);

  const loadSettings = async () => {
    try {
      const data = await getSettings();
      if (data) {
        setSettings({ ...DEFAULT_SETTINGS, ...data });
      }
    } catch (error) {
      console.error('加载设置失败:', error);
    }
  };

  const updateSetting = async (key: string, value: any) => {
    const newSettings = { ...settings, [key]: value };
    setSettings(newSettings);
    
    try {
      await apiUpdateSettings(newSettings);
      await saveLocalSettings(newSettings);
    } catch (error) {
      console.error('更新设置失败:', error);
      // 回滚
      setSettings(settings);
    }
  };

  const exportConfig = async () => {
    // TODO: 实现配置导出
    console.log('导出配置');
  };

  const importConfig = async () => {
    // TODO: 实现配置导入
    console.log('导入配置');
  };

  const resetSettings = async () => {
    setSettings(DEFAULT_SETTINGS);
    try {
      await apiUpdateSettings(DEFAULT_SETTINGS);
      await saveLocalSettings(DEFAULT_SETTINGS);
    } catch (error) {
      console.error('重置设置失败:', error);
    }
  };

  return {
    settings,
    updateSetting,
    exportConfig,
    importConfig,
    resetSettings,
  };
}

