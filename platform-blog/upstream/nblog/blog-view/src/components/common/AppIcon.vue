<template>
	<component
		:is="iconComponent"
		class="app-icon"
		:class="{'is-spinning': spin}"
		:size="size"
		:stroke-width="strokeWidth"
		aria-hidden="true"
	/>
</template>

<script setup>
import {computed} from 'vue'
import {
	ArrowDown,
	ArrowUp,
	ArrowUpCircle,
	BarChart3,
	BookOpen,
	Bookmark,
	CalendarDays,
	ChevronDown,
	CircleHelp,
	CircleUserRound,
	Clock3,
	Download,
	Edit3,
	ExternalLink,
	FileText,
	FolderOpen,
	Globe2,
	Grid2X2,
	Home,
	Lightbulb,
	Link2,
	List,
	LoaderCircle,
	LogOut,
	Menu,
	MessageSquare,
	Moon,
	Plus,
	Search,
	Settings,
	Shuffle,
	Sun,
	Tags,
	Trash2,
	Trophy,
	Type,
	UserRound,
} from '@lucide/vue'

const props = defineProps({
	name: {type: String, required: true},
	size: {type: Number, default: 16},
	strokeWidth: {type: Number, default: 2},
	spin: {type: Boolean, default: false},
})

const icons = Object.freeze({
	'arrow-down': ArrowDown,
	'arrow-up': ArrowUp,
	'arrow-up-circle': ArrowUpCircle,
	'bar-chart': BarChart3,
	book: BookOpen,
	bookmark: Bookmark,
	calendar: CalendarDays,
	'chevron-down': ChevronDown,
	clock: Clock3,
	download: Download,
	edit: Edit3,
	external: ExternalLink,
	file: FileText,
	folder: FolderOpen,
	globe: Globe2,
	grid: Grid2X2,
	home: Home,
	lightbulb: Lightbulb,
	link: Link2,
	list: List,
	loader: LoaderCircle,
	'log-out': LogOut,
	menu: Menu,
	message: MessageSquare,
	moon: Moon,
	plus: Plus,
	search: Search,
	settings: Settings,
	shuffle: Shuffle,
	sun: Sun,
	tags: Tags,
	trash: Trash2,
	trophy: Trophy,
	type: Type,
	user: UserRound,
	'user-circle': CircleUserRound,
})

const iconComponent = computed(() => icons[props.name] || CircleHelp)
</script>

<style scoped>
.app-icon {
	display: inline-block;
	flex: 0 0 auto;
	vertical-align: -0.18em;
}

.app-icon.is-spinning {
	animation: app-icon-spin 850ms linear infinite;
}

@keyframes app-icon-spin {
	to { transform: rotate(360deg); }
}

@media (prefers-reduced-motion: reduce) {
	.app-icon.is-spinning { animation: none; }
}
</style>
