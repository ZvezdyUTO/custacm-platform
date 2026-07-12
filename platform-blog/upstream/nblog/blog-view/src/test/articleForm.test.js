// Author: huangbingrui.awa
import {describe, expect, it} from 'vitest'
import {articleRequest, isArticleAuthor, markdownTextFromFile} from '@/util/articleForm'

describe('article form', () => {
	it('builds the player article request and calculates Unicode words', () => {
		expect(articleRequest({
			title: '  A+B  ', firstPicture: ' /cover.png ', description: ' 题解 ', content: ' 你好A ',
			categoryId: '3', tagList: [4, ' 动态规划 '], published: true, commentEnabled: false,
		})).toEqual({
			title: 'A+B', firstPicture: '/cover.png', firstPictureAssetId: null, description: '题解', content: '你好A', words: 3,
			cate: 3, tagList: [4, '动态规划'], published: true, internal: false, commentEnabled: false,
		})
	})

	it('allows an article without a cover image and removes empty tag names', () => {
		expect(articleRequest({
			title: '题解', firstPicture: null, description: '描述', content: '正文', categoryId: 3,
			tagList: [8, '  ', '图论'], published: false, commentEnabled: true,
		})).toMatchObject({firstPicture: '', firstPictureAssetId: null, tagList: [8, '图论']})
	})

	it('reads markdown text and removes a byte-order mark', async () => {
		const file = {name: 'solution.md', size: 20, text: async () => '\uFEFF# Solution'}
		await expect(markdownTextFromFile(file)).resolves.toBe('# Solution')
	})

	it('rejects non-markdown and oversized files', async () => {
		await expect(markdownTextFromFile({name: 'solution.txt', size: 1, text: async () => ''})).rejects.toThrow('.md')
		await expect(markdownTextFromFile({name: 'solution.md', size: 3 * 1024 * 1024, text: async () => ''})).rejects.toThrow('2MB')
	})

	it('shows owner actions only when the public author username matches the session', () => {
		expect(isArticleAuthor({authorUsername: 'alice'}, {username: 'alice'})).toBe(true)
		expect(isArticleAuthor({authorUsername: 'alice'}, {username: 'bob'})).toBe(false)
		expect(isArticleAuthor({authorUsername: null}, {username: 'alice'})).toBe(false)
	})
})
