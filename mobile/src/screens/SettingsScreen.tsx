import React from 'react';
import { View, ScrollView, StyleSheet, Alert } from 'react-native';
import {
  List,
  Switch,
  Divider,
  useTheme,
} from 'react-native-paper';

import { useSettings } from '../hooks/useSettings';

export default function SettingsScreen() {
  const theme = useTheme();
  const {
    settings,
    updateSetting,
    exportConfig,
    importConfig,
    resetSettings,
  } = useSettings();

  const handleReset = () => {
    Alert.alert(
      '确认重置',
      '确定要重置所有设置吗？',
      [
        { text: '取消', style: 'cancel' },
        {
          text: '重置',
          style: 'destructive',
          onPress: resetSettings,
        },
      ]
    );
  };

  return (
    <ScrollView style={styles.container}>
      {/* 常规设置 */}
      <List.Section>
        <List.Subheader>常规</List.Subheader>
        <List.Item
          title="开机自启动"
          description="应用随系统自动启动"
          right={() => (
            <Switch
              value={settings.autoStart}
              onValueChange={(value) => updateSetting('autoStart', value)}
            />
          )}
        />
        <Divider />
        <List.Item
          title="启动时自动连接"
          description="应用启动时自动启用代理"
          right={() => (
            <Switch
              value={settings.autoConnect}
              onValueChange={(value) => updateSetting('autoConnect', value)}
            />
          )}
        />
        <Divider />
        <List.Item
          title="深色模式"
          description="使用深色主题"
          right={() => (
            <Switch
              value={settings.darkMode}
              onValueChange={(value) => updateSetting('darkMode', value)}
            />
          )}
        />
      </List.Section>

      {/* 代理设置 */}
      <List.Section>
        <List.Subheader>代理</List.Subheader>
        <List.Item
          title="混合端口"
          description={`HTTP/SOCKS5 混合端口: ${settings.mixedPort}`}
          left={props => <List.Icon {...props} icon="ethernet" />}
          onPress={() => {/* TODO: 打开端口设置对话框 */}}
        />
        <Divider />
        <List.Item
          title="允许局域网连接"
          description="允许其他设备通过此设备连接代理"
          right={() => (
            <Switch
              value={settings.allowLan}
              onValueChange={(value) => updateSetting('allowLan', value)}
            />
          )}
        />
        <Divider />
        <List.Item
          title="启用 IPv6"
          description="支持 IPv6 连接"
          right={() => (
            <Switch
              value={settings.ipv6}
              onValueChange={(value) => updateSetting('ipv6', value)}
            />
          )}
        />
      </List.Section>

      {/* 性能设置 */}
      <List.Section>
        <List.Subheader>性能</List.Subheader>
        <List.Item
          title="省电模式"
          description="降低后台活动以节省电量"
          right={() => (
            <Switch
              value={settings.batteryOptimization}
              onValueChange={(value) => updateSetting('batteryOptimization', value)}
            />
          )}
        />
        <Divider />
        <List.Item
          title="数据压缩"
          description="压缩传输数据以节省流量"
          right={() => (
            <Switch
              value={settings.dataCompression}
              onValueChange={(value) => updateSetting('dataCompression', value)}
            />
          )}
        />
      </List.Section>

      {/* 高级设置 */}
      <List.Section>
        <List.Subheader>高级</List.Subheader>
        <List.Item
          title="DNS 设置"
          description="配置 DNS 服务器"
          left={props => <List.Icon {...props} icon="dns" />}
          right={props => <List.Icon {...props} icon="chevron-right" />}
          onPress={() => {/* TODO: 打开DNS设置页面 */}}
        />
        <Divider />
        <List.Item
          title="日志级别"
          description={`当前: ${settings.logLevel}`}
          left={props => <List.Icon {...props} icon="text-box" />}
          right={props => <List.Icon {...props} icon="chevron-right" />}
          onPress={() => {/* TODO: 打开日志级别选择 */}}
        />
      </List.Section>

      {/* 备份与恢复 */}
      <List.Section>
        <List.Subheader>备份与恢复</List.Subheader>
        <List.Item
          title="导出配置"
          description="导出所有设置和配置"
          left={props => <List.Icon {...props} icon="export" />}
          onPress={exportConfig}
        />
        <Divider />
        <List.Item
          title="导入配置"
          description="从文件导入配置"
          left={props => <List.Icon {...props} icon="import" />}
          onPress={importConfig}
        />
        <Divider />
        <List.Item
          title="重置设置"
          description="恢复默认设置"
          left={props => <List.Icon {...props} icon="restore" />}
          onPress={handleReset}
        />
      </List.Section>

      {/* 关于 */}
      <List.Section>
        <List.Subheader>关于</List.Subheader>
        <List.Item
          title="版本"
          description="1.0.0"
          left={props => <List.Icon {...props} icon="information" />}
        />
        <Divider />
        <List.Item
          title="GitHub"
          description="查看源代码"
          left={props => <List.Icon {...props} icon="github" />}
          right={props => <List.Icon {...props} icon="open-in-new" />}
          onPress={() => {/* TODO: 打开GitHub链接 */}}
        />
        <Divider />
        <List.Item
          title="许可证"
          description="GPL-3.0"
          left={props => <List.Icon {...props} icon="license" />}
        />
      </List.Section>

      <View style={styles.bottomSpace} />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  bottomSpace: {
    height: 32,
  },
});

