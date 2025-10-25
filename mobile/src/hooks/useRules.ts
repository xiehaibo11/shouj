import useSWR from 'swr';
import { getRules } from '../services/api';

export function useRules() {
  const { data, mutate, error } = useSWR('rules', getRules);

  return {
    rules: data || [],
    mutate,
    loading: !data && !error,
    error,
  };
}

