// Author: huangbingrui.awa
import tvMapper from '@/plugins/tvMapper.json'
import aruMapper from '@/plugins/aruMapper.json'
import paopaoMapper from '@/plugins/paopaoMapper.json'
import {renderNotoEmoji} from '@/plugins/notoEmoji'

const legacyEmoji = [...tvMapper, ...aruMapper, ...paopaoMapper]

export function escapeCommentHtml(value) {
	return String(value ?? '').replace(/[&<>"']/g, character => ({
		'&': '&amp;',
		'<': '&lt;',
		'>': '&gt;',
		'"': '&quot;',
		"'": '&#39;',
	})[character])
}

export function formatCommentContent(value) {
	let content = escapeCommentHtml(value)
	if (content.includes('@[')) {
		for (const emoji of legacyEmoji) {
			content = content.replace(new RegExp(emoji.reg, 'g'), `<img class="legacy-comment-emoji" src="${emoji.src}" alt="">`)
		}
	}
	return renderNotoEmoji(content)
}
