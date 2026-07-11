import { onMounted, readonly, ref } from 'vue';
import { changeCurrentPassword, getCurrentUser, login } from '../api/auth';
import { ApiError } from '../api/client';
import { clearSession, readSession, writeSession } from '../auth/session';
import type { CurrentUser } from '../types';

export type AuthStatus = 'restoring' | 'anonymous' | 'authenticated';

// Author: huangbingrui.awa
export function useAuthSession() {
  const stored = readSession();
  const status = ref<AuthStatus>(stored ? 'restoring' : 'anonymous');
  const token = ref<string | null>(stored?.token ?? null);
  const user = ref<CurrentUser | null>(stored?.user ?? null);
  let generation = 0;
  let signInSequence = 0;

  function commitAnonymous() {
    generation += 1;
    token.value = null;
    user.value = null;
    status.value = 'anonymous';
    clearSession();
  }

  async function restore() {
    if (!token.value || status.value !== 'restoring') return;
    const currentGeneration = generation;
    const storedToken = token.value;
    try {
      const restoredUser = await getCurrentUser(storedToken);
      if (currentGeneration !== generation) return;
      user.value = restoredUser;
      status.value = 'authenticated';
      writeSession(storedToken, restoredUser);
    } catch (error) {
      if (currentGeneration !== generation) return;
      if (error instanceof ApiError && error.status === 401) {
        commitAnonymous();
        return;
      }
      status.value = 'authenticated';
    }
  }

  async function signIn(username: string, password: string) {
    const sequence = ++signInSequence;
    const authenticated = await login(username, password);
    if (sequence !== signInSequence) return;
    generation += 1;
    token.value = authenticated.token;
    user.value = authenticated.user;
    status.value = 'authenticated';
    writeSession(authenticated.token, authenticated.user);
  }

  function signOut() {
    signInSequence += 1;
    commitAnonymous();
  }

  async function changePassword(oldPassword: string, newPassword: string) {
    const activeToken = token.value;
    if (!activeToken) throw new Error('请先登录。');
    const currentGeneration = generation;
    try {
      await changeCurrentPassword(activeToken, oldPassword, newPassword);
    } catch (error) {
      if (error instanceof ApiError && error.status === 401 && currentGeneration === generation) {
        commitAnonymous();
      }
      throw error;
    }
  }

  onMounted(() => void restore());

  return {
    status: readonly(status),
    token: readonly(token),
    user: readonly(user),
    signIn,
    signOut,
    changePassword,
    restore,
  };
}
