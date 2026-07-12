// Author: huangbingrui.awa
import {shallowMount} from '@vue/test-utils'
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

	it('closes the crop dialog after the avatar request has finished saving', async () => {
		readToken.mockReturnValue('token-value')
		readUser.mockReturnValue({username: 'alice'})
		updateCurrentAvatar.mockResolvedValue({username: 'alice'})
		const view = {saving: false}
		const close = vi.fn(() => expect(view.saving).toBe(false))
		Object.assign(view, {
			errorMessage: '',
			authUser: null,
			$refs: {cropDialog: {close}},
			$nextTick: () => Promise.resolve(),
			msgSuccess: vi.fn(),
		})

		await Introduction.methods.saveAvatar.call(view, new Blob(['avatar']))

		expect(writeUser).toHaveBeenCalledWith({username: 'alice'})
		expect(close).toHaveBeenCalledOnce()
		expect(view.msgSuccess).toHaveBeenCalledWith('头像已更新')
	})
})
