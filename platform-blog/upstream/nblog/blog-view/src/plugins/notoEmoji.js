// Author: huangbingrui.awa
export const NOTO_EMOJI_SPRITE = '/emoji/noto/smileys-emotion.svg'

export const notoEmojiCategories = Object.freeze([
	{
		id: 'frequent',
		label: '常用',
		emojis: [
			['😀', '1F600', '开心'], ['😂', '1F602', '笑哭'], ['😊', '1F60A', '微笑'],
			['😍', '1F60D', '喜欢'], ['🥰', '1F970', '幸福'], ['😘', '1F618', '飞吻'],
			['😎', '1F60E', '酷'], ['🤔', '1F914', '思考'], ['🥺', '1F97A', '拜托'],
			['😭', '1F62D', '大哭'], ['😡', '1F621', '生气'], ['🤯', '1F92F', '震惊'],
			['❤️', '2764', '红心'], ['💯', '1F4AF', '满分'], ['💩', '1F4A9', '便便'],
		],
	},
	{
		id: 'smileys',
		label: '笑脸',
		emojis: [
			['😁', '1F601', '露齿笑'], ['😃', '1F603', '大笑'], ['😄', '1F604', '眯眼笑'],
			['😅', '1F605', '冒汗笑'], ['😆', '1F606', '斜眼笑'], ['😇', '1F607', '天使'],
			['😉', '1F609', '眨眼'], ['😋', '1F60B', '好吃'], ['😌', '1F60C', '轻松'],
			['😜', '1F61C', '调皮'], ['🤪', '1F92A', '鬼脸'], ['🤭', '1F92D', '捂嘴笑'],
			['🤣', '1F923', '笑翻'], ['🥳', '1F973', '庆祝'], ['🙃', '1F643', '倒脸'],
			['🤓', '1F913', '书呆子'], ['🧐', '1F9D0', '单片眼镜'], ['🤗', '1F917', '拥抱'],
		],
	},
	{
		id: 'moods',
		label: '情绪',
		emojis: [
			['😏', '1F60F', '得意'], ['😒', '1F612', '不悦'], ['😔', '1F614', '沉思'],
			['😕', '1F615', '困惑'], ['😖', '1F616', '难受'], ['😞', '1F61E', '失望'],
			['😟', '1F61F', '担心'], ['😢', '1F622', '流泪'], ['😣', '1F623', '忍耐'],
			['😤', '1F624', '傲慢'], ['😥', '1F625', '难过'], ['😨', '1F628', '害怕'],
			['😩', '1F629', '疲惫'], ['😪', '1F62A', '困'], ['😫', '1F62B', '累'],
			['😬', '1F62C', '尴尬'], ['😱', '1F631', '惊恐'], ['😳', '1F633', '脸红'],
			['😴', '1F634', '睡觉'], ['😵', '1F635', '晕'], ['😶', '1F636', '无语'],
			['😷', '1F637', '口罩'], ['🤒', '1F912', '发烧'], ['🤕', '1F915', '受伤'],
			['🤢', '1F922', '恶心'], ['🤮', '1F92E', '呕吐'], ['🤧', '1F927', '打喷嚏'],
			['🥵', '1F975', '热'], ['🥶', '1F976', '冷'], ['🥴', '1F974', '迷糊'],
			['🥱', '1F971', '打哈欠'],
		],
	},
	{
		id: 'hearts',
		label: '爱心',
		emojis: [
			['💋', '1F48B', '唇印'], ['💌', '1F48C', '情书'], ['💓', '1F493', '心跳'],
			['💔', '1F494', '心碎'], ['💕', '1F495', '两颗心'], ['💖', '1F496', '闪亮的心'],
			['💗', '1F497', '成长的心'], ['💘', '1F498', '丘比特'], ['💙', '1F499', '蓝心'],
			['💚', '1F49A', '绿心'], ['💛', '1F49B', '黄心'], ['💜', '1F49C', '紫心'],
			['🖤', '1F5A4', '黑心'], ['🤍', '1F90D', '白心'], ['🤎', '1F90E', '棕心'],
			['❣️', '2763', '心叹号'], ['💝', '1F49D', '礼物心'], ['💞', '1F49E', '旋转的心'],
			['💟', '1F49F', '心形装饰'],
		],
	},
].map(category => Object.freeze({
	...category,
	emojis: Object.freeze(category.emojis.map(([unicode, codepoint, label]) => Object.freeze({
		unicode,
		codepoint,
		label,
	}))),
})))

export const notoEmojis = Object.freeze(notoEmojiCategories.flatMap(category => category.emojis))

export function notoEmojiUrl(emoji) {
	return `${NOTO_EMOJI_SPRITE}#${emoji.codepoint}`
}

export function renderNotoEmoji(value) {
	let rendered = String(value ?? '')
	for (const emoji of [...notoEmojis].sort((left, right) => right.unicode.length - left.unicode.length)) {
		const image = `<img class="noto-emoji" src="${notoEmojiUrl(emoji)}" alt="${emoji.unicode}" title="${emoji.label}" draggable="false">`
		rendered = rendered.replaceAll(emoji.unicode, image)
	}
	return rendered
}
