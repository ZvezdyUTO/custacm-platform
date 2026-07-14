// Author: huangbingrui.awa
import { flushPromises, mount } from '@vue/test-utils';
import { afterEach, describe, expect, it, vi } from 'vitest';
import LoginPanel from '../components/LoginPanel.vue';
import { ApiError } from '../api/client';

describe('Vue training login', () => {
	afterEach(() => {
		vi.useRealTimers();
		Object.defineProperty(window, 'scrollY', { configurable: true, value: 0 });
		document.documentElement.classList.remove('login-scrollbar-hidden');
	});

  it('submits the shared Blog account credentials', async () => {
    const signIn = vi.fn().mockResolvedValue(undefined);
    const wrapper = mount(LoginPanel, { props: { signIn } });

    await wrapper.get('input[autocomplete="username"]').setValue('player1');
    await wrapper.get('input[autocomplete="current-password"]').setValue('password');
    await wrapper.get('form').trigger('submit');

    expect(signIn).toHaveBeenCalledWith('player1', 'password');
    expect(wrapper.find('.login-brand-mark').exists()).toBe(true);
    expect(wrapper.get('input[autocomplete="username"]').attributes('placeholder')).toBe('用户名');
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
		expect(wrapper.get('.login-submit-status').text()).toContain('5 秒后重试');
		await vi.advanceTimersByTimeAsync(5000);
		expect(wrapper.get('.login-submit').attributes('disabled')).toBeUndefined();
		expect(wrapper.get('.login-submit-status').text()).toContain('登录');
	});

	it('scales the central brand mark down while scrolling and restores it at the top', async () => {
		const scrollHost = document.createElement('div');
		scrollHost.className = 'training-site is-login-page';
		document.body.append(scrollHost);
		const wrapper = mount(LoginPanel, { props: { signIn: vi.fn() }, attachTo: scrollHost });
		expect(wrapper.get('.login-brand-mark').attributes('style')).toContain('scale(1)');

		scrollHost.scrollTop = 190;
		scrollHost.dispatchEvent(new Event('scroll'));
		await wrapper.vm.$nextTick();
		expect(wrapper.get('.login-brand-mark').attributes('style')).toContain('scale(0.75)');

		scrollHost.scrollTop = 0;
		scrollHost.dispatchEvent(new Event('scroll'));
		await wrapper.vm.$nextTick();
		expect(wrapper.get('.login-brand-mark').attributes('style')).toContain('scale(1)');
		wrapper.unmount();
		scrollHost.remove();
	});

	it('hides the viewport scrollbar only while the login panel is mounted', () => {
		const wrapper = mount(LoginPanel, { props: { signIn: vi.fn() } });
		expect(document.documentElement.classList.contains('login-scrollbar-hidden')).toBe(true);
		wrapper.unmount();
		expect(document.documentElement.classList.contains('login-scrollbar-hidden')).toBe(false);
	});
});
