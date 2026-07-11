<template>
  <div class="submission-pagination">
    <span>第 {{ page }}/{{ totalPages }} 页</span>
    <label>每页<select :value="limit" :disabled="disabled" @change="changeLimit"><option v-for="size in sizes" :key="size" :value="size">{{ size }}</option></select></label>
    <div class="submission-page-actions">
      <button class="secondary-button compact" :disabled="disabled || page <= 1" type="button" @click="emit('change', page - 1, limit)">上一页</button>
      <button class="secondary-button compact" :disabled="disabled || page >= totalPages" type="button" @click="emit('change', page + 1, limit)">下一页</button>
    </div>
  </div>
</template>
<script setup lang="ts">
// Author: huangbingrui.awa
defineProps<{ page: number; limit: number; totalPages: number; disabled: boolean }>();
const emit = defineEmits<{ change: [page: number, limit: number] }>();
const sizes = [15, 50, 100, 200];
function changeLimit(event: Event) { emit('change', 1, Number((event.target as HTMLSelectElement).value)); }
</script>
