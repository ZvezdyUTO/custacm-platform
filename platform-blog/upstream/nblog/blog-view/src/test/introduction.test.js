// Author: huangbingrui.awa
import {flushPromises, shallowMount} from '@vue/test-utils'
import {beforeEach, describe, expect, it, vi} from 'vitest'

const {getCurrentProfile, getPublicProfile, updateCurrentAvatar, readToken, readUser, writeUser} = vi.hoisted(() => ({
	getCurrentProfile: vi.fn(),
	getPublicProfile: vi.fn(),
	updateCurrentAvatar: vi.fn(),
	readToken: vi.fn(),
	readUser: vi.fn(),
	writeUser: vi.fn(),
}))

vi.mock('@/api/profile', () => ({getCurrentProfile, getPublicProfile, updateCurrentAvatar}))
vi.mock('@/auth/session', () => ({
	readToken,
	readUser,
	writeUser,
	SESSION_CHANGE_EVENT: 'custacm:session-change',
}))
import Introduction from '@/components/sidebar/Introduction.vue'
import AchievementsPanel from '@/components/profile/AchievementsPanel.vue'
import introductionSource from '@/components/sidebar/Introduction.vue?raw'

function deferred() {
	let resolve
	const promise = new Promise(resolvePromise => { resolve = resolvePromise })
	return {promise, resolve}
}

describe('Introduction avatar source', () => {
	const avatarSrc = Introduction.computed.avatarSrc

	beforeEach(() => {
		getCurrentProfile.mockReset()
		getPublicProfile.mockReset()
		updateCurrentAvatar.mockReset()
		readToken.mockReset()
		readUser.mockReset()
		writeUser.mockReset()
	})

	it('uses the original asset for the signed-in profile card', () => {
		expect(avatarSrc.call({
			displayProfile: {
				avatar: '/api/image/assets/avatar/thumbnail.png',
				avatarOriginalUrl: '/api/image/assets/avatar/original.png',
			},
		})).toBe('/api/image/assets/avatar/original.png')
	})

	it('falls back to the thumbnail and then the default avatar', () => {
		expect(avatarSrc.call({displayProfile: {avatar: '/api/image/assets/avatar/thumbnail.png'}}))
			.toBe('/api/image/assets/avatar/thumbnail.png')
		expect(avatarSrc.call({displayProfile: null})).toBe('/img/default-avatar.jpg')
	})

	it('limits an existing public signature to forty visible characters', () => {
		const signature = `前${'签'.repeat(50)}后`

		expect(Introduction.computed.profileSignature.call({displayProfile: {signature}}))
			.toBe(Array.from(signature).slice(0, 40).join(''))
	})

	it('shows email below the username on signed-in and public author cards', async () => {
		readUser.mockReturnValue({username: 'alice', nickname: 'Alice', email: 'alice@example.com'})
		readToken.mockReturnValue(null)
		const wrapper = shallowMount(Introduction, {
			global: {
				mocks: {$route: {name: 'home'}, $router: {push: vi.fn()}},
				stubs: {AvatarCropDialog: true, RouterLink: true},
			},
		})

		expect(wrapper.get('.profile-email').text()).toBe('alice@example.com')
		await wrapper.setProps({authorUsername: 'bob', authorSummary: {username: 'bob', email: 'bob@example.com'}})
		expect(wrapper.get('.profile-email').text()).toBe('bob@example.com')
	})

	it('keeps profile links collapsed into a compact summary by default', () => {
		readUser.mockReturnValue({
			username: 'alice',
			links: [{id: 1, label: 'Project', url: 'https://example.com'}],
		})
		readToken.mockReturnValue(null)
		const wrapper = shallowMount(Introduction, {
			global: {
				mocks: {$route: {name: 'blog'}, $router: {push: vi.fn()}},
				stubs: {AvatarCropDialog: true, RouterLink: true},
			},
		})

		const disclosure = wrapper.get('.profile-links-disclosure')
		expect(disclosure.element.tagName).toBe('DETAILS')
		expect(disclosure.attributes('open')).toBeUndefined()
		expect(wrapper.get('.profile-links-heading').text()).toContain('友情链接')
		expect(wrapper.get('.profile-links-heading').text()).toContain('1/8')
		expect(wrapper.get('.profile-links-content').exists()).toBe(true)
		expect(introductionSource).toContain('grid-template-rows: 0fr;')
		expect(introductionSource).toContain('grid-template-rows: 1fr;')
		expect(introductionSource).toContain('@media (prefers-reduced-motion: reduce)')
	})

	it('renders only achievements returned by the public author profile', async () => {
		const achievement = {competitionId: 31, awardId: 71, competitionFullName: 'ICPC Shanghai'}
		readUser.mockReturnValue({username: 'viewer', achievements: [{competitionId: 99, awardId: 99}]})
		readToken.mockReturnValue(null)
		getPublicProfile.mockResolvedValue({username: 'alice', achievements: [achievement], links: []})
		const wrapper = shallowMount(Introduction, {
			props: {authorUsername: 'alice', authorSummary: {username: 'alice'}},
			global: {
				mocks: {$route: {name: 'blog'}, $router: {push: vi.fn()}},
				stubs: {AvatarCropDialog: true, RouterLink: true},
			},
		})
		await flushPromises()

		const panel = wrapper.findComponent(AchievementsPanel)
		expect(panel.exists()).toBe(true)
		expect(panel.props('achievements')).toEqual([achievement])
		expect(panel.props('editable')).toBe(false)
		expect(panel.classes()).toContain('article-author-achievements')
		expect(introductionSource).toContain('.article-author-achievements + .profile-links-disclosure')
	})

	it('exposes a compact action slot beside the author identity', () => {
		expect(introductionSource).toContain('<div class="profile-meta-row">')
		expect(introductionSource).toContain('<slot name="article-actions" />')
		expect(introductionSource).toContain('grid-template-columns: minmax(0, 1fr) auto;')
	})

	it('does not write a current-profile response after the signed-in account changes', async () => {
		const pending = deferred()
		readToken.mockReturnValue('token-alice')
		readUser.mockReturnValue({username: 'alice'})
		getCurrentProfile.mockReturnValue(pending.promise)
		const view = {
			sessionGeneration: 0,
			currentProfileRequestId: 0,
			isSessionCurrent: Introduction.methods.isSessionCurrent,
			authUser: {username: 'alice'},
		}
		const loading = Introduction.methods.loadProfile.call(view)

		readToken.mockReturnValue('token-bob')
		readUser.mockReturnValue({username: 'bob'})
		view.sessionGeneration += 1
		pending.resolve({username: 'alice'})
		await loading

		expect(writeUser).not.toHaveBeenCalled()
		expect(view.authUser).toEqual({username: 'alice'})
	})

	it('closes the crop dialog after the avatar request has finished saving', async () => {
		readToken.mockReturnValue('token-value')
		readUser.mockReturnValue({username: 'alice'})
		updateCurrentAvatar.mockResolvedValue({username: 'alice'})
		const view = {saving: false}
		const close = vi.fn(() => expect(view.saving).toBe(false))
		Object.assign(view, {
			errorMessage: '',
			authUser: null,
			sessionGeneration: 0,
			avatarMutationId: 0,
			isSessionCurrent: Introduction.methods.isSessionCurrent,
			$refs: {cropDialog: {close}},
			$nextTick: () => Promise.resolve(),
			msgSuccess: vi.fn(),
		})

		await Introduction.methods.saveAvatar.call(view, new Blob(['avatar']))

		expect(writeUser).toHaveBeenCalledWith({username: 'alice'})
		expect(close).toHaveBeenCalledOnce()
		expect(view.msgSuccess).toHaveBeenCalledWith('头像已更新')
	})

	it('does not write an avatar response after the session changes', async () => {
		const pending = deferred()
		readToken.mockReturnValue('token-alice')
		readUser.mockReturnValue({username: 'alice'})
		updateCurrentAvatar.mockReturnValue(pending.promise)
		const view = {
			errorMessage: '', saving: false, sessionGeneration: 0, avatarMutationId: 0,
			isSessionCurrent: Introduction.methods.isSessionCurrent,
			$refs: {cropDialog: {close: vi.fn()}},
			$nextTick: () => Promise.resolve(),
			msgSuccess: vi.fn(),
		}
		const saving = Introduction.methods.saveAvatar.call(view, new Blob(['avatar']))

		readToken.mockReturnValue('token-bob')
		readUser.mockReturnValue({username: 'bob'})
		view.sessionGeneration += 1
		pending.resolve({username: 'alice'})
		await saving

		expect(writeUser).not.toHaveBeenCalled()
		expect(view.msgSuccess).not.toHaveBeenCalled()
		expect(view.$refs.cropDialog.close).not.toHaveBeenCalled()
	})
})
