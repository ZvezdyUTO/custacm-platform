// Author: huangbingrui.awa
import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';
import LoginPanel from '../components/LoginPanel.vue';

describe('Vue training login', () => {
  it('submits the shared Blog account credentials', async () => {
    const signIn = vi.fn().mockResolvedValue(undefined);
    const wrapper = mount(LoginPanel, { props: { signIn } });

    await wrapper.get('input[autocomplete="username"]').setValue('player1');
    await wrapper.get('input[autocomplete="current-password"]').setValue('password');
    await wrapper.get('form').trigger('submit');

    expect(signIn).toHaveBeenCalledWith('player1', 'password');
  });
});
