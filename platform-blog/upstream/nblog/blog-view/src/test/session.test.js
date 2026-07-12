// Author: huangbingrui.awa
import {beforeAll, beforeEach, describe, expect, it, vi} from 'vitest'
import {clearSession, readToken, readUser, SESSION_CHANGE_EVENT, writeUser} from '@/auth/session'

describe('shared Blog session', () => {
	beforeAll(() => {
		const values = new Map()
		Object.defineProperty(window, 'localStorage', {
			configurable: true,
			value: {
				clear: () => values.clear(),
				getItem: key => values.has(key) ? values.get(key) : null,
				removeItem: key => values.delete(key),
				setItem: (key, value) => values.set(key, String(value)),
			},
		})
	})

	beforeEach(() => {
		window.localStorage.clear()
	})

	it('reads a paired token and user summary', () => {
		window.localStorage.setItem('custacm.accessToken', 'token-value')
		window.localStorage.setItem('custacm.user', JSON.stringify({username: 'alice', nickname: 'Alice'}))

		expect(readToken()).toBe('token-value')
		expect(readUser()).toEqual({username: 'alice', nickname: 'Alice'})
	})

	it('preserves the current role for the account menu', () => {
		window.localStorage.setItem('custacm.accessToken', 'token-value')
		window.localStorage.setItem('custacm.user', JSON.stringify({username: 'admin', nickname: 'Administrator', role: 'ROLE_admin'}))

		expect(readUser()).toMatchObject({username: 'admin', role: 'ROLE_admin'})
	})

	it('clears orphaned session data without touching unrelated storage', () => {
		window.localStorage.setItem('custacm.accessToken', 'orphan-token')
		window.localStorage.setItem('memberToken', 'legacy-token')
		window.localStorage.setItem('identification', 'comment-identity')

		expect(readToken()).toBeNull()
		expect(window.localStorage.getItem('custacm.accessToken')).toBeNull()
		expect(window.localStorage.getItem('memberToken')).toBeNull()
		expect(window.localStorage.getItem('identification')).toBe('comment-identity')
	})

	it('emits the stable session change event when logging out', () => {
		const listener = vi.fn()
		window.addEventListener(SESSION_CHANGE_EVENT, listener, {once: true})

		clearSession()

		expect(listener).toHaveBeenCalledOnce()
	})

	it('updates the stored profile after an avatar change', () => {
		window.localStorage.setItem('custacm.accessToken', 'token-value')
		window.localStorage.setItem('custacm.user', JSON.stringify({username: 'alice', nickname: 'Alice'}))
		const listener = vi.fn()
		window.addEventListener(SESSION_CHANGE_EVENT, listener, {once: true})

		expect(writeUser({
			username: 'alice',
			nickname: '新昵称',
			avatar: '/api/image/avatar.png',
			signature: '保持好奇',
			links: [{id: 1, label: 'GitHub', url: 'https://github.com/alice', sortOrder: 0}],
			role: 'ROLE_player',
		})).toBe(true)
		expect(readUser()).toMatchObject({
			nickname: '新昵称',
			avatar: '/api/image/avatar.png',
			signature: '保持好奇',
			links: [{label: 'GitHub'}],
		})
		expect(listener).toHaveBeenCalledOnce()
	})
})
