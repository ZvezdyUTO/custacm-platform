<template>
  <div
    v-if="open"
    class="admin-confirm-backdrop"
    role="presentation"
    @click.self="cancel"
    @keydown.esc.stop.prevent="cancel"
  >
    <section
      class="admin-confirm-dialog"
      :class="`is-${tone}`"
      role="alertdialog"
      aria-modal="true"
      :aria-labelledby="`${dialogId}-title`"
      :aria-describedby="`${dialogId}-description`"
      :aria-busy="busy"
    >
      <span class="admin-confirm-icon" aria-hidden="true">
        <component :is="dialogIcon" :size="25" />
      </span>
      <div class="admin-confirm-copy">
        <h3 :id="`${dialogId}-title`">{{ title }}</h3>
        <p :id="`${dialogId}-description`">{{ description }}</p>
      </div>
      <div class="admin-confirm-actions">
        <button ref="cancelButton" type="button" :disabled="busy" @click="cancel">取消</button>
        <button class="admin-confirm-primary" type="button" :disabled="busy" @click="emit('confirm')">
          {{ busy ? busyLabel : confirmLabel }}
        </button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
// Author: huangbingrui.awa
import { Archive, Trash2, TriangleAlert, UserPlus } from '@lucide/vue';
import { computed, nextTick, ref, watch } from 'vue';

type DialogIcon = 'backup' | 'delete' | 'users' | 'warning';
type DialogTone = 'danger' | 'info' | 'warning';

const props = withDefaults(defineProps<{
  open: boolean;
  dialogId: string;
  title: string;
  description: string;
  confirmLabel: string;
  busyLabel?: string;
  busy?: boolean;
  icon?: DialogIcon;
  tone?: DialogTone;
}>(), {
  busy: false,
  busyLabel: '正在处理…',
  icon: 'warning',
  tone: 'warning',
});

const emit = defineEmits<{ cancel: []; confirm: [] }>();
const cancelButton = ref<{ focus: () => void } | null>(null);
const iconMap = { backup: Archive, delete: Trash2, users: UserPlus, warning: TriangleAlert };
const dialogIcon = computed(() => iconMap[props.icon]);

function cancel() {
  if (!props.busy) emit('cancel');
}

watch(() => props.open, async (open) => {
  if (!open) return;
  await nextTick();
  cancelButton.value?.focus();
});
</script>
