const TOKEN_KEY = 'custacm.accessToken'
const USER_KEY = 'custacm.user'
const LEGACY_TOKEN_KEY = 'memberToken'
const LEGACY_USER_KEY = 'memberUser'
export const SESSION_CHANGE_EVENT = 'custacm:session-change'

function readSession() {
	const token = window.localStorage.getItem(TOKEN_KEY)
	const rawUser = window.localStorage.getItem(USER_KEY)

	if (!token && !rawUser) {
		return null
	}
	if (!token || !rawUser) {
		clearSession()
		return null
	}

	try {
		const user = JSON.parse(rawUser)
		if (
			!user
			|| typeof user !== 'object'
			|| Array.isArray(user)
			|| typeof user.username !== 'string'
			|| !user.username.trim()
		) {
			clearSession()
			return null
		}
		return {token, user}
	} catch (_) {
		clearSession()
		return null
	}
}

export function readUser() {
	const session = readSession()
	return session ? session.user : null
}

export function readToken() {
	const session = readSession()
	return session ? session.token : null
}

export function writeUser(user) {
	const token = window.localStorage.getItem(TOKEN_KEY)
	if (!token || !user?.username) return false
	window.localStorage.setItem(USER_KEY, JSON.stringify({
		username: user.username,
		nickname: user.nickname || user.username,
		avatar: user.avatar || '',
		avatarOriginalUrl: user.avatarOriginalUrl || '',
		signature: user.signature || '',
		links: Array.isArray(user.links) ? user.links : [],
		role: user.role,
	}))
	window.dispatchEvent(new Event(SESSION_CHANGE_EVENT))
	return true
}

export function clearSession() {
	window.localStorage.removeItem(TOKEN_KEY)
	window.localStorage.removeItem(USER_KEY)
	window.localStorage.removeItem(LEGACY_TOKEN_KEY)
	window.localStorage.removeItem(LEGACY_USER_KEY)
	window.dispatchEvent(new Event(SESSION_CHANGE_EVENT))
}
