import { invoke } from '@tauri-apps/api/core';
import axios from 'axios';

// Tauri命令调用
export async function getProxies() {
  return invoke('get_proxies');
}

export async function selectProxy(group: string, proxy: string) {
  return invoke('select_proxy', { group, proxy });
}

export async function testDelay(proxyName: string, timeout = 5000) {
  return invoke('test_delay', { proxyName, timeout });
}

export async function getProfiles() {
  return invoke('get_profiles');
}

export async function createProfile(data: any) {
  return invoke('create_profile', { data });
}

export async function updateProfile(uid: string) {
  return invoke('update_profile', { uid });
}

export async function deleteProfile(uid: string) {
  return invoke('delete_profile', { uid });
}

export async function selectProfile(uid: string) {
  return invoke('select_profile', { uid });
}

export async function getConnections() {
  return invoke('get_connections');
}

export async function closeConnection(id: string) {
  return invoke('close_connection', { id });
}

export async function closeAllConnections() {
  return invoke('close_all_connections');
}

export async function getRules() {
  return invoke('get_rules');
}

export async function getLogs() {
  return invoke('get_logs');
}

export async function clearLogs() {
  return invoke('clear_logs');
}

export async function getSettings() {
  return invoke('get_settings');
}

export async function updateSettings(settings: any) {
  return invoke('update_settings', { settings });
}

// VPN 相关
export async function startVPN() {
  return invoke('start_vpn');
}

export async function stopVPN() {
  return invoke('stop_vpn');
}

export async function getVPNStatus() {
  return invoke('get_vpn_status');
}

// 流量统计
export async function getTrafficStats() {
  return invoke('get_traffic_stats');
}

export async function resetTrafficStats() {
  return invoke('reset_traffic_stats');
}

