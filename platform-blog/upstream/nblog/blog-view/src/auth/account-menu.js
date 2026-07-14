// Author: huangbingrui.awa
export function accountMenuItems(user) {
	const items = [
		{command: 'profile', label: '我的主页', icon: 'user', divided: false},
	]
	if (user?.role === 'ROLE_admin') {
		items.push({command: 'admin', label: '管理员界面', icon: 'settings', divided: false})
	}
	items.push({command: 'logout', label: '退出登录', icon: 'log-out', divided: true})
	return items
}
