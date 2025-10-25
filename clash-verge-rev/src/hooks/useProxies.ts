import { useState, useEffect } from 'react';
import useSWR from 'swr';
import { getProxies, selectProxy } from '../services/api';

export function useProxies() {
  const { data, mutate, error } = useSWR('proxies', getProxies, {
    refreshInterval: 5000,
  });

  const [currentGroup, setCurrentGroup] = useState('GLOBAL');

  const groups = data?.groups || [];
  const proxies = data?.proxies || [];
  const currentProxy = data?.current || null;

  const selectGroup = (groupName: string) => {
    setCurrentGroup(groupName);
  };

  const select = async (proxyName: string) => {
    await selectProxy(currentGroup, proxyName);
    await mutate();
  };

  return {
    groups,
    proxies,
    currentProxy,
    currentGroup,
    selectGroup,
    select,
    mutate,
    loading: !data && !error,
    error,
  };
}

