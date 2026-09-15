// Author: huangbingrui.awa

export function taxonomyTextColor(background) {
	const hex = /^#([0-9a-f]{6})$/i.exec(background || '')?.[1]
	// Legacy tag-cloud HSL colors use a deliberately dark palette.
	if (!hex) return '#ffffff'
	const channels = [0, 2, 4].map(offset => parseInt(hex.slice(offset, offset + 2), 16) / 255)
	const [red, green, blue] = channels.map(value => value <= .04045 ? value / 12.92 : ((value + .055) / 1.055) ** 2.4)
	const luminance = .2126 * red + .7152 * green + .0722 * blue
	return (luminance + .05) / .05 >= 1.05 / (luminance + .05) ? '#000000' : '#ffffff'
}

export function taxonomyStyle(color) {
	const backgroundColor = color || '#8B1E3F'
	const foreground = taxonomyTextColor(backgroundColor)
	return {backgroundColor, color: foreground, '--taxonomy-text-color': foreground}
}
