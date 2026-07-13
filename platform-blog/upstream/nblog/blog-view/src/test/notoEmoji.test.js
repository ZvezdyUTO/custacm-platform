// Author: huangbingrui.awa
import {readFileSync} from 'node:fs'
import {resolve} from 'node:path'
import {describe, expect, it} from 'vitest'
import {notoEmojiCategories, notoEmojis, notoEmojiUrl, renderNotoEmoji} from '@/plugins/notoEmoji'
import {formatCommentContent} from '@/util/commentContent'

describe('Noto emoji comments', () => {
	it('ships every picker glyph in the local Noto sprite', () => {
		const sprite = readFileSync(resolve(process.cwd(), 'public/emoji/noto/smileys-emotion.svg'), 'utf8')
		const codepoints = notoEmojis.map(emoji => emoji.codepoint)

		expect(notoEmojiCategories.map(category => category.label)).toEqual(['常用', '笑脸', '情绪', '爱心'])
		expect(new Set(codepoints).size).toBe(codepoints.length)
		for (const codepoint of codepoints) expect(sprite).toContain(`id="${codepoint}"`)
		expect(notoEmojiUrl(notoEmojis[0])).toMatch(/^\/emoji\/noto\/smileys-emotion\.svg#/)
	})

	it('stores Unicode while rendering supported emoji with local Noto images', () => {
		const rendered = renderNotoEmoji('写得真好 😀❤️')

		expect(rendered).toContain('写得真好')
		expect(rendered).toContain('class="noto-emoji"')
		expect(rendered).toContain('alt="😀"')
		expect(rendered).toContain('/emoji/noto/smileys-emotion.svg#1F600')
		expect(rendered).toContain('/emoji/noto/smileys-emotion.svg#2764')
	})

	it('escapes comment HTML before adding trusted Noto image markup', () => {
		const rendered = formatCommentContent('<script>alert(1)</script> 😀')

		expect(rendered).toContain('&lt;script&gt;alert(1)&lt;/script&gt;')
		expect(rendered).toContain('class="noto-emoji"')
		expect(rendered).not.toContain('<script>')
	})
})
