// Author: huangbingrui.awa
import { mount } from '@vue/test-utils';
import { nextTick } from 'vue';
import { afterEach, describe, expect, it } from 'vitest';
import AdminConfirmDialog from '../components/AdminConfirmDialog.vue';

afterEach(() => {
  document.body.innerHTML = '';
});

describe('AdminConfirmDialog accessibility', () => {
  it('moves focus into the dialog, traps Tab, handles Escape and restores focus', async () => {
    const trigger = document.createElement('button');
    document.body.append(trigger);
    trigger.focus();

    const wrapper = mount(AdminConfirmDialog, {
      attachTo: document.body,
      props: {
        open: false,
        dialogId: 'test-confirm',
        title: '确认操作',
        description: '这项操作需要确认。',
        confirmLabel: '继续',
      },
    });

    await wrapper.setProps({ open: true });
    await nextTick();
    const cancel = wrapper.get('.admin-confirm-actions button:first-child').element as HTMLButtonElement;
    const confirm = wrapper.get('.admin-confirm-primary').element as HTMLButtonElement;
    expect(document.activeElement).toBe(cancel);

    confirm.focus();
    await wrapper.get('.admin-confirm-dialog').trigger('keydown', { key: 'Tab' });
    expect(document.activeElement).toBe(cancel);

    cancel.focus();
    await wrapper.get('.admin-confirm-dialog').trigger('keydown', { key: 'Tab', shiftKey: true });
    expect(document.activeElement).toBe(confirm);

    await wrapper.get('.admin-confirm-dialog').trigger('keydown', { key: 'Escape' });
    expect(wrapper.emitted('cancel')).toHaveLength(1);

    await wrapper.setProps({ open: false });
    await nextTick();
    expect(document.activeElement).toBe(trigger);
    wrapper.unmount();
  });

  it('does not cancel while the confirmed operation is busy', async () => {
    const wrapper = mount(AdminConfirmDialog, {
      props: {
        open: true,
        busy: true,
        dialogId: 'busy-confirm',
        title: '正在处理',
        description: '请稍候。',
        confirmLabel: '继续',
      },
    });

    await wrapper.get('.admin-confirm-backdrop').trigger('keydown', { key: 'Escape' });
    expect(wrapper.emitted('cancel')).toBeUndefined();
    wrapper.unmount();
  });
});
