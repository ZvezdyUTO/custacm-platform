// Author: huangbingrui.awa
import {afterEach, describe, expect, it, vi} from 'vitest'

import mutations from '@/store/mutations'
import state from '@/store/state'
import {RESET_COMMENT_FORM} from '@/store/mutations-types'

describe('comment form state', () => {
	afterEach(() => vi.restoreAllMocks())

	it('keeps only content and does not persist legacy visitor fields', () => {
		const setItem = vi.spyOn(Storage.prototype, 'setItem')
		const localState = {commentForm: {content: '待发送评论'}}

		mutations[RESET_COMMENT_FORM](localState)

		expect(Object.keys(state.commentForm)).toEqual(['content'])
		expect(localState.commentForm).toEqual({content: ''})
		expect(setItem).not.toHaveBeenCalled()
	})
})
