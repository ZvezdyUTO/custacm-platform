<template>
	<!--标签云-->
	<div class="ui segments m-box">
		<div class="ui secondary segment"><i class="tags icon"></i>标签云</div>
		<div class="ui segment sidebar-accent m-padding-small">
			<router-link :to="`/tag/${tag.name}`" class="ui label m-text-500" :style="{backgroundColor: tagColor(tag), color: '#fff'}" v-for="tag in tagList" :key="tag.name">
				{{ tag.name }}
			</router-link>
		</div>
	</div>
</template>

<script>
	export default {
		name: "Tags",
		props: {
			tagList: {
				type: Array,
				required: true
			},
		},
		methods: {
			tagColor(tag) {
				if (/^#[0-9a-f]{6}$/i.test(tag.color || '')) return tag.color
				let hash = 2166136261
				for (const char of tag.name || '') {
					hash ^= char.codePointAt(0) || 0
					hash = Math.imul(hash, 16777619)
				}
				const value = Math.abs(hash)
				return `hsl(${value % 360} ${65 + (Math.abs(hash >>> 8) % 26)}% ${22 + (Math.abs(hash >>> 16) % 6)}%)`
			},
		},
	}
</script>

<style scoped>
	.secondary.segment {
		padding: 10px;
	}

	.m-padding-small {
		padding: 7px;
	}

	.label {
		margin: 3px !important;
	}
</style>
