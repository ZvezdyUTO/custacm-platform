// Author: huangbingrui.awa
import {beforeEach, describe, expect, it, vi} from 'vitest'

const mocks = vi.hoisted(() => ({
	clearSession: vi.fn(),
	getCurrentProfile: vi.fn(),
	readToken: vi.fn(),
	readUser: vi.fn(),
	setAchievementProfileOrder: vi.fn(),
	setAchievementProfileVisibility: vi.fn(),
	updateCurrentAvatar: vi.fn(),
	writeUser: vi.fn(),
}))

vi.mock('@/api/profile', () => ({
	changeCurrentPassword: vi.fn(),
	getCurrentProfile: mocks.getCurrentProfile,
	replaceCurrentProfileLinks: vi.fn(),
	updateCurrentAvatar: mocks.updateCurrentAvatar,
	updateCurrentProfile: vi.fn(),
}))
vi.mock('@/api/player-competition', () => ({
	setAchievementProfileOrder: mocks.setAchievementProfileOrder,
	setAchievementProfileVisibility: mocks.setAchievementProfileVisibility,
}))
vi.mock('@/auth/session', () => ({
	clearSession: mocks.clearSession,
	readToken: mocks.readToken,
	readUser: mocks.readUser,
	writeUser: mocks.writeUser,
	SESSION_CHANGE_EVENT: 'custacm:session-change',
}))

import Profile from '@/views/profile/Profile.vue'
import profileSource from '@/views/profile/Profile.vue?raw'

function deferred() {
	let resolve
	let reject
	const promise = new Promise((resolvePromise, rejectPromise) => {
		resolve = resolvePromise
		reject = rejectPromise
	})
	return {promise, reject, resolve}
}

describe('profile achievement visibility', () => {
	const achievement = {
		competitionId: 31,
		awardId: 71,
		competitionFullName: 'ICPC Shanghai',
		profileVisible: false,
		profileOrder: null,
	}

	beforeEach(() => {
		vi.clearAllMocks()
		mocks.readToken.mockReturnValue('token-alice')
		mocks.readUser.mockReturnValue({username: 'alice', nickname: 'Alice'})
		mocks.setAchievementProfileVisibility.mockResolvedValue(undefined)
		mocks.setAchievementProfileOrder.mockResolvedValue(undefined)
		mocks.updateCurrentAvatar.mockResolvedValue({username: 'alice', avatar: '/uploads/alice.png'})
	})

	function target(overrides = {}) {
		return {
			sessionGeneration: 0,
			sessionToken: 'token-alice',
			sessionUsername: 'alice',
			profileRequestId: 0,
			profileSaveRequestId: 0,
			avatarMutationId: 0,
			avatarSaving: false,
			avatarError: '',
			profileLoading: false,
			profileLoaded: true,
			profileLoadError: '',
			profile: {username: 'alice', nickname: 'Alice', achievements: [achievement]},
			currentProfile: {username: 'alice', nickname: 'Alice', achievements: [achievement]},
			achievements: [achievement],
			achievementError: '',
			achievementUpdating: {},
			achievementMutationSequence: 0,
			achievementOrderMutationId: 0,
			achievementOrderSaving: false,
			isSessionCurrent: Profile.methods.isSessionCurrent,
			isProfileRequestCurrent: Profile.methods.isProfileRequestCurrent,
			errorStatus: Profile.methods.errorStatus,
			expireCurrentSession: Profile.methods.expireCurrentSession,
			errorMessage: Profile.methods.errorMessage,
			achievementKey: Profile.methods.achievementKey,
			changeAchievementOrder: Profile.methods.changeAchievementOrder,
			msgSuccess: vi.fn(),
			$router: {push: vi.fn()},
			...overrides,
		}
	}

	it('writes the successful visibility choice back without refetching the profile', async () => {
		const view = target({profile: null})

		await Profile.methods.changeAchievementVisibility.call(view, achievement, true)

		expect(mocks.setAchievementProfileVisibility).toHaveBeenCalledWith('token-alice', 31, 71, true)
		expect(view.profile.achievements[0].profileVisible).toBe(true)
		expect(view.profile.achievements[0].profileOrder).toBe(1)
		expect(mocks.writeUser).toHaveBeenCalledWith(view.profile)
		expect(view.achievementUpdating).toEqual({})
	})

	it('does not start a duplicate request for the same award', async () => {
		const view = {
			achievementError: '',
			achievementUpdating: {'31:71': true},
			achievementKey: Profile.methods.achievementKey,
		}

		await Profile.methods.changeAchievementVisibility.call(view, achievement, true)

		expect(mocks.setAchievementProfileVisibility).not.toHaveBeenCalled()
	})

	it('clears only an expired session and returns to the profile after login', async () => {
		mocks.setAchievementProfileVisibility.mockRejectedValue({code: 401})
		const view = target()

		await Profile.methods.changeAchievementVisibility.call(view, achievement, true)

		expect(mocks.clearSession).toHaveBeenCalledOnce()
		expect(view.$router.push).toHaveBeenCalledWith({path: '/training/login', query: {returnTo: '/profile'}})
	})

	it('shows loading until the current profile request has completed', async () => {
		const pending = deferred()
		mocks.getCurrentProfile.mockReturnValue(pending.promise)
		const view = target({profile: null, profileLoaded: false})

		const loading = Profile.methods.loadProfile.call(view)
		expect(view.profileLoading).toBe(true)
		expect(view.profileLoaded).toBe(false)
		pending.resolve({username: 'alice', achievements: []})
		await loading

		expect(view.profileLoading).toBe(false)
		expect(view.profileLoaded).toBe(true)
		expect(mocks.writeUser).toHaveBeenCalledWith({username: 'alice', achievements: []})
		expect(profileSource).toContain(':loading="profileLoading"')
		expect(profileSource).toContain(':show-empty="profileLoaded"')
		expect(profileSource).toContain('@retry="loadProfile"')
	})

	it('keeps a profile load failure in the achievements panel with retry available', async () => {
		mocks.getCurrentProfile.mockRejectedValue(new Error('网络离线'))
		const view = target({profile: null, profileLoaded: false})

		await Profile.methods.loadProfile.call(view)

		expect(view.profileLoading).toBe(false)
		expect(view.profileLoaded).toBe(false)
		expect(view.profileLoadError).toBe('网络离线')
		expect(mocks.writeUser).not.toHaveBeenCalled()
	})

	it('clears the current session when loading /player/me returns 401', async () => {
		mocks.getCurrentProfile.mockRejectedValue({response: {status: 401}})
		const view = target({profile: null, profileLoaded: false})

		await Profile.methods.loadProfile.call(view)

		expect(mocks.clearSession).toHaveBeenCalledOnce()
		expect(view.$router.push).toHaveBeenCalledWith({path: '/training/login', query: {returnTo: '/profile'}})
	})

	it('discards an old profile response after the account changes', async () => {
		const pending = deferred()
		mocks.getCurrentProfile.mockReturnValue(pending.promise)
		const view = target({profile: null, profileLoaded: false})
		const loading = Profile.methods.loadProfile.call(view)

		mocks.readToken.mockReturnValue('token-bob')
		mocks.readUser.mockReturnValue({username: 'bob'})
		view.sessionGeneration += 1
		pending.resolve({username: 'alice', achievements: [achievement]})
		await loading

		expect(mocks.writeUser).not.toHaveBeenCalled()
		expect(view.profile).toBeNull()
	})

	it('does not write an old visibility mutation into a new account', async () => {
		const pending = deferred()
		mocks.setAchievementProfileVisibility.mockReturnValue(pending.promise)
		const view = target()
		const mutation = Profile.methods.changeAchievementVisibility.call(view, achievement, true)

		mocks.readToken.mockReturnValue('token-bob')
		mocks.readUser.mockReturnValue({username: 'bob'})
		view.sessionGeneration += 1
		pending.resolve()
		await mutation

		expect(mocks.writeUser).not.toHaveBeenCalled()
		expect(view.msgSuccess).not.toHaveBeenCalled()
		expect(view.profile.achievements[0].profileVisible).toBe(false)
	})

	it('optimistically reorders all public awards and persists the exact award ids', async () => {
		const pending = deferred()
		mocks.setAchievementProfileOrder.mockReturnValue(pending.promise)
		const first = {...achievement, awardId: 71, profileVisible: true, profileOrder: 1}
		const second = {...achievement, awardId: 72, profileVisible: true, profileOrder: 2}
		const hidden = {...achievement, awardId: 73, profileVisible: false, profileOrder: null}
		const achievements = [first, hidden, second]
		const view = target({
			profile: {username: 'alice', achievements},
			currentProfile: {username: 'alice', achievements},
			achievements,
		})

		const mutation = Profile.methods.changeAchievementOrder.call(view, [72, 71])

		expect(view.achievementOrderSaving).toBe(true)
		expect(view.profile.achievements.map(item => item.profileOrder)).toEqual([2, null, 1])
		expect(mocks.writeUser).not.toHaveBeenCalled()
		pending.resolve()
		await mutation

		expect(mocks.setAchievementProfileOrder).toHaveBeenCalledWith('token-alice', [72, 71])
		expect(mocks.writeUser).toHaveBeenCalledWith(view.profile)
		expect(view.achievementOrderSaving).toBe(false)
		expect(view.msgSuccess).toHaveBeenCalledWith('公开荣誉排序已保存')
	})

	it('rolls an optimistic public award order back when persistence fails', async () => {
		mocks.setAchievementProfileOrder.mockRejectedValue(new Error('排序保存失败'))
		const first = {...achievement, awardId: 71, profileVisible: true, profileOrder: 1}
		const second = {...achievement, awardId: 72, profileVisible: true, profileOrder: 2}
		const achievements = [first, second]
		const view = target({
			profile: {username: 'alice', achievements},
			currentProfile: {username: 'alice', achievements},
			achievements,
		})

		await Profile.methods.changeAchievementOrder.call(view, [72, 71])

		expect(view.profile.achievements.map(item => item.profileOrder)).toEqual([1, 2])
		expect(view.achievementError).toBe('排序保存失败')
		expect(mocks.writeUser).not.toHaveBeenCalled()
	})

	it('reapplies a persisted order over a concurrent profile replacement', async () => {
		const pending = deferred()
		mocks.setAchievementProfileOrder.mockReturnValue(pending.promise)
		const first = {...achievement, awardId: 71, profileVisible: true, profileOrder: 1}
		const second = {...achievement, awardId: 72, profileVisible: true, profileOrder: 2}
		const view = target({profile: {username: 'alice', nickname: 'Alice', achievements: [first, second]}})
		delete view.currentProfile
		delete view.achievements
		Object.defineProperties(view, {
			currentProfile: {get() { return this.profile }},
			achievements: {get() { return this.profile.achievements }},
		})
		const mutation = Profile.methods.changeAchievementOrder.call(view, [72, 71])

		view.profile = {
			username: 'alice',
			nickname: '并发更新后的昵称',
			achievements: [{...first}, {...second}],
		}
		pending.resolve()
		await mutation

		expect(view.profile.nickname).toBe('并发更新后的昵称')
		expect(view.profile.achievements.map(item => item.profileOrder)).toEqual([2, 1])
		expect(mocks.writeUser).toHaveBeenCalledWith(view.profile)
	})

	it('rejects an incomplete order before making a request', async () => {
		const first = {...achievement, awardId: 71, profileVisible: true, profileOrder: 1}
		const second = {...achievement, awardId: 72, profileVisible: true, profileOrder: 2}
		const achievements = [first, second]
		const view = target({
			profile: {username: 'alice', achievements},
			currentProfile: {username: 'alice', achievements},
			achievements,
		})

		await Profile.methods.changeAchievementOrder.call(view, [72])

		expect(mocks.setAchievementProfileOrder).not.toHaveBeenCalled()
		expect(view.achievementError).toContain('排序已变化')
	})

	it('does not write an old order mutation into a new account', async () => {
		const pending = deferred()
		mocks.setAchievementProfileOrder.mockReturnValue(pending.promise)
		const first = {...achievement, awardId: 71, profileVisible: true, profileOrder: 1}
		const second = {...achievement, awardId: 72, profileVisible: true, profileOrder: 2}
		const achievements = [first, second]
		const view = target({
			profile: {username: 'alice', achievements},
			currentProfile: {username: 'alice', achievements},
			achievements,
		})
		const mutation = Profile.methods.changeAchievementOrder.call(view, [72, 71])

		mocks.readToken.mockReturnValue('token-bob')
		mocks.readUser.mockReturnValue({username: 'bob'})
		view.sessionGeneration += 1
		pending.resolve()
		await mutation

		expect(mocks.writeUser).not.toHaveBeenCalled()
		expect(view.msgSuccess).not.toHaveBeenCalled()
	})

	it('reloads only when the session identity changes', () => {
		const view = target({
			authUser: {username: 'alice'},
			editing: false,
			loadProfile: vi.fn(),
			saving: false,
			saveError: '',
			passwordSaving: false,
			passwordError: '',
		})

		Profile.methods.refreshUser.call(view)
		expect(view.loadProfile).not.toHaveBeenCalled()

		mocks.readToken.mockReturnValue('token-bob')
		mocks.readUser.mockReturnValue({username: 'bob'})
		Profile.methods.refreshUser.call(view)

		expect(view.sessionGeneration).toBe(1)
		expect(view.loadProfile).toHaveBeenCalledOnce()
	})

	it('shows one clickable avatar identity header and removes duplicate profile cards', () => {
		expect(profileSource).toContain('class="profile-avatar-button"')
		expect(profileSource).toContain('aria-label="更换头像"')
		expect(profileSource).toContain('<AvatarCropDialog')
		expect(profileSource).not.toContain('class="oj-grid"')
		expect(profileSource).not.toContain('class="identity-grid"')
		expect(profileSource).not.toContain('class="signature-preview"')
		expect(profileSource).toContain(':maxlength="signatureMaxLength"')
		expect(profileSource).toContain('const MAX_SIGNATURE_LENGTH = 40')
	})

	it('updates the avatar in the current profile while preserving loaded achievements', async () => {
		const close = vi.fn()
		const view = target({
			authUser: {username: 'alice', nickname: 'Alice'},
			$nextTick: vi.fn().mockResolvedValue(undefined),
			$refs: {avatarCropDialog: {close}},
		})
		const blob = new Blob(['avatar'], {type: 'image/png'})

		await Profile.methods.saveAvatar.call(view, blob)

		expect(mocks.updateCurrentAvatar).toHaveBeenCalledWith('token-alice', blob)
		expect(view.profile.avatar).toBe('/uploads/alice.png')
		expect(view.profile.achievements).toEqual([achievement])
		expect(mocks.writeUser).toHaveBeenCalledWith(view.profile)
		expect(close).toHaveBeenCalledOnce()
	})
})
