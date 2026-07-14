<template>
	<ArticleCatalog :category-list="categoryList" :tag-list="tagList"/>
</template>

<script>
	import ArticleCatalog from '@/views/category/Category.vue'

	export default {
		name: 'Tag',
		components: {ArticleCatalog},
		props: {
			categoryList: {type: Array, default: () => []},
			tagList: {type: Array, default: () => []},
		},
	}
</script>
