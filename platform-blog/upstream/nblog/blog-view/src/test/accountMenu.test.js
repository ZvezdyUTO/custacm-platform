// Author: huangbingrui.awa
import {describe, expect, it} from 'vitest'
import {accountMenuItems} from '@/auth/account-menu'

describe('Blog account menu', () => {
	it('keeps profile and logout for a player', () => {
		expect(accountMenuItems({role: 'ROLE_player'}).map(item => item.label)).toEqual(['我的主页', '退出登录'])
	})

	it('adds the admin entry only for an administrator', () => {
		expect(accountMenuItems({role: 'ROLE_admin'}).map(item => item.label)).toEqual(['我的主页', '管理员界面', '退出登录'])
	})
})
