// Author: huangbingrui.awa

export const coverImageState = {
	data() {
		return {failedCoverSources: []}
	},
	methods: {
		hasCover(source) {
			return Boolean(source) && !this.failedCoverSources.includes(source)
		},
		coverFailed(event) {
			const source = event.currentTarget?.getAttribute('src')
			if (source && !this.failedCoverSources.includes(source)) this.failedCoverSources.push(source)
		},
	},
}
