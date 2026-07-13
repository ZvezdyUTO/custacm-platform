// Author: huangbingrui.awa
import { flushPromises, mount } from '@vue/test-utils';
import { afterEach, describe, expect, it, vi } from 'vitest';
import LoginPanel from '../components/LoginPanel.vue';
import { ApiError } from '../api/client';

describe('Vue training login', () => {
	afterEach(() => vi.useRealTimers());

  it('submits the shared Blog account credentials', async () => {
    const signIn = vi.fn().mockResolvedValue(undefined);
    const wrapper = mount(LoginPanel, { props: { signIn } });

    await wrapper.get('input[autocomplete="username"]').setValue('player1');
    await wrapper.get('input[autocomplete="current-password"]').setValue('password');
    await wrapper.get('form').trigger('submit');

    expect(signIn).toHaveBeenCalledWith('player1', 'password');
  });

	it('disables retries for the server-provided five-second cooldown', async () => {
		vi.useFakeTimers();
		const signIn = vi.fn().mockRejectedValue(new ApiError(
			401,
			'AUTH_BAD_CREDENTIALS',
			'用户名或密码错误！',
			null,
			5,
		));
		const wrapper = mount(LoginPanel, { props: { signIn } });

		await wrapper.get('input[autocomplete="username"]').setValue('player1');
		await wrapper.get('input[autocomplete="current-password"]').setValue('wrong');
		await wrapper.get('form').trigger('submit');
		await flushPromises();

		expect(wrapper.get('.login-submit').attributes('disabled')).toBeDefined();
		expect(wrapper.get('.login-submit').text()).toContain('5 秒后重试');
		await vi.advanceTimersByTimeAsync(5000);
		expect(wrapper.get('.login-submit').attributes('disabled')).toBeUndefined();
		expect(wrapper.get('.login-submit').text()).toContain('登录');
	});
});
