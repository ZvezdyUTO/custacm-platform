<template>
  <div
    v-if="open"
    class="admin-confirm-backdrop"
    role="presentation"
    @click.self="cancel"
    @keydown.esc.stop.prevent="cancel"
  >
    <section
      ref="dialog"
      class="admin-confirm-dialog"
      :class="`is-${tone}`"
      role="alertdialog"
      tabindex="-1"
      aria-modal="true"
      :aria-labelledby="`${dialogId}-title`"
      :aria-describedby="`${dialogId}-description`"
      :aria-busy="busy"
      @keydown.tab="trapFocus"
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
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue';

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
const dialog = ref<globalThis.HTMLElement | null>(null);
const cancelButton = ref<globalThis.HTMLButtonElement | null>(null);
let previouslyFocused: globalThis.HTMLElement | null = null;
const iconMap = { backup: Archive, delete: Trash2, users: UserPlus, warning: TriangleAlert };
const dialogIcon = computed(() => iconMap[props.icon]);

function cancel() {
  if (!props.busy) emit('cancel');
}

function focusableElements() {
  return Array.from(dialog.value?.querySelectorAll<globalThis.HTMLElement>(
    'button:not(:disabled), [href], input:not(:disabled), select:not(:disabled), textarea:not(:disabled), [tabindex]:not([tabindex="-1"])',
  ) ?? []).filter((element) => !element.hasAttribute('hidden'));
}

function trapFocus(event: globalThis.KeyboardEvent) {
  const focusable = focusableElements();
  if (focusable.length === 0) {
    event.preventDefault();
    dialog.value?.focus();
    return;
  }
  const first = focusable[0];
  const last = focusable[focusable.length - 1];
  const active = document.activeElement;
  if (event.shiftKey && (active === first || !dialog.value?.contains(active))) {
    event.preventDefault();
    last.focus();
  } else if (!event.shiftKey && active === last) {
    event.preventDefault();
    first.focus();
  }
}

function restoreFocus() {
  const target = previouslyFocused;
  previouslyFocused = null;
  if (target?.isConnected) target.focus();
}

watch(() => props.open, async (open) => {
  if (!open) {
    await nextTick();
    restoreFocus();
    return;
  }
  previouslyFocused = document.activeElement instanceof globalThis.HTMLElement ? document.activeElement : null;
  await nextTick();
  (cancelButton.value ?? dialog.value)?.focus();
}, { immediate: true });

onBeforeUnmount(restoreFocus);
</script>
