import useSWR from 'swr';
import {
  getProfiles,
  createProfile as apiCreateProfile,
  updateProfile as apiUpdateProfile,
  deleteProfile as apiDeleteProfile,
  selectProfile as apiSelectProfile,
} from '../services/api';

export function useProfiles() {
  const { data, mutate, error } = useSWR('profiles', getProfiles);

  const profiles = data?.items || [];
  const currentProfile = profiles.find((p: any) => p.uid === data?.current);

  const selectProfile = async (uid: string) => {
    await apiSelectProfile(uid);
    await mutate();
  };

  const createProfile = async (profileData: any) => {
    await apiCreateProfile(profileData);
    await mutate();
  };

  const updateProfile = async (uid: string) => {
    await apiUpdateProfile(uid);
    await mutate();
  };

  const deleteProfile = async (uid: string) => {
    await apiDeleteProfile(uid);
    await mutate();
  };

  return {
    profiles,
    currentProfile,
    selectProfile,
    createProfile,
    updateProfile,
    deleteProfile,
    mutate,
    loading: !data && !error,
    error,
  };
}

