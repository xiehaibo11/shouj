import useSWR from 'swr';
import { getConnections } from '../services/api';

export function useConnections() {
  const { data, mutate, error } = useSWR('connections', getConnections, {
    refreshInterval: 1000,
  });

  return {
    connections: data || [],
    mutate,
    loading: !data && !error,
    error,
  };
}

