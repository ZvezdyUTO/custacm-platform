<template>
  <section class="homepage-banner-admin admin-reference-page" aria-label="首页图片管理">
    <header class="reference-page-header homepage-banner-header">
      <span class="reference-page-icon"><ImagePlus :size="22" /></span>
      <div>
        <h2>首页图片</h2>
        <p>首页最多保留两张图片，上传后统一裁剪为 1920×1080。列表从左到右的顺序，就是首页鼠标移动时的图片顺序。</p>
      </div>
    </header>

    <p v-if="message" class="operation-toast" role="status">{{ message }}</p>
    <p v-if="errorMessage" class="form-error" role="alert">{{ errorMessage }}</p>

    <div class="homepage-banner-list">
      <article v-for="(image, index) in banners" :key="image.id" class="homepage-banner-card">
        <div class="homepage-banner-preview">
          <img :src="image.imageUrl" :alt="`首页图片 ${index + 1}`">
          <span>{{ index + 1 }}</span>
        </div>
        <div class="homepage-banner-actions">
          <button :disabled="busy || index === 0" type="button" :aria-label="`将第 ${index + 1} 张向左移动`" @click="move(index, -1)"><ArrowLeft :size="17" />向左</button>
          <button :disabled="busy || index === banners.length - 1" type="button" :aria-label="`将第 ${index + 1} 张向右移动`" @click="move(index, 1)">向右<ArrowRight :size="17" /></button>
          <button class="danger-link" :disabled="busy || banners.length <= 1" type="button" :aria-label="`删除第 ${index + 1} 张首页图片`" @click="remove(image.id)"><Trash2 :size="17" />删除</button>
        </div>
      </article>
      <label v-if="banners.length < MAX_BANNER_COUNT" class="homepage-banner-add" aria-label="添加首页图片">
        <Plus :size="56" :stroke-width="1.4" aria-hidden="true" />
        <input accept="image/*" multiple type="file" @change="selectFiles">
      </label>
    </div>

    <AdminConfirmDialog
      :open="pendingDeleteId !== null"
      dialog-id="homepage-banner-delete"
      title="删除这张首页图片？"
      description="删除后首页会立即停止展示这张图片；只保留一张图片时不能继续删除。"
      confirm-label="确认删除"
      :busy="busy"
      icon="delete"
      tone="danger"
      @cancel="pendingDeleteId = null"
      @confirm="confirmRemove"
    />

    <div v-if="cropSource" class="homepage-crop-dialog" role="dialog" aria-modal="true" aria-labelledby="homepage-crop-title">
      <section class="homepage-crop-panel">
        <header>
          <div><h2 id="homepage-crop-title">裁剪首页图片</h2><p>{{ currentFileName }} · 固定 16:9，可拖动并滚轮缩放</p></div>
          <span v-if="pendingFiles.length" class="crop-queue-count">后续还有 {{ pendingFiles.length }} 张</span>
        </header>
        <div class="homepage-crop-stage">
          <img ref="cropImage" :src="cropSource" alt="待裁剪的首页图片" @load="initializeCropper">
        </div>
        <footer>
          <button class="secondary-button" :disabled="busy" type="button" @click="cancelCurrentCrop">跳过这张</button>
          <button class="primary-button" :disabled="busy" type="button" @click="uploadCrop"><Crop :size="18" />{{ busy ? '正在上传' : '裁剪并上传' }}</button>
        </footer>
      </section>
    </div>
  </section>
</template>

<script setup lang="ts">
// Author: huangbingrui.awa
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import { ArrowLeft, ArrowRight, Crop, ImagePlus, Plus, Trash2 } from '@lucide/vue';
import Cropper from 'cropperjs';
import 'cropperjs/dist/cropper.css';
import type { usePlatformDashboard } from '../composables/usePlatformDashboard';
import AdminConfirmDialog from './AdminConfirmDialog.vue';

const props = defineProps<{ dashboard: ReturnType<typeof usePlatformDashboard> }>();
const MAX_BANNER_COUNT = 2;
const cropImage = ref<HTMLImageElement | null>(null);
const cropSource = ref('');
const currentFileName = ref('');
const pendingFiles = ref<File[]>([]);
const busy = ref(false);
const message = ref('');
const errorMessage = ref('');
const pendingDeleteId = ref<number | null>(null);
let cropper: Cropper | null = null;

const banners = computed(() => props.dashboard.homepageBanners.value);

onMounted(async () => {
  try {
    await props.dashboard.loadHomepageBanners();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '首页图片加载失败。';
  }
});

onBeforeUnmount(() => {
  destroyCropper();
  revokeCropSource();
});

function selectFiles(event: Event) {
  const input = event.target as HTMLInputElement;
  const selected = Array.from(input.files || []).filter((file) => file.type.startsWith('image/'));
  input.value = '';
  if (!selected.length) {
    errorMessage.value = '请选择图片文件。';
    return;
  }
  const occupiedSlots = banners.value.length + pendingFiles.value.length + (cropSource.value ? 1 : 0);
  const availableSlots = Math.max(0, MAX_BANNER_COUNT - occupiedSlots);
  if (!availableSlots) {
    errorMessage.value = '首页最多保留两张图片，请先删除一张再上传。';
    return;
  }
  pendingFiles.value = [...pendingFiles.value, ...selected.slice(0, availableSlots)];
  errorMessage.value = selected.length > availableSlots ? '首页最多保留两张图片，超出的文件未加入。' : '';
  if (!cropSource.value) openNextCrop();
}

function openNextCrop() {
  destroyCropper();
  revokeCropSource();
  const next = pendingFiles.value.shift();
  if (!next) {
    currentFileName.value = '';
    return;
  }
  currentFileName.value = next.name;
  cropSource.value = URL.createObjectURL(next);
}

function initializeCropper() {
  destroyCropper();
  if (!cropImage.value) return;
  cropper = new Cropper(cropImage.value, {
    aspectRatio: 16 / 9,
    autoCropArea: 1,
    background: false,
    checkOrientation: true,
    dragMode: 'move',
    guides: true,
    responsive: true,
    viewMode: 1,
  });
}

async function uploadCrop() {
  if (!cropper) return;
  const canvas = cropper.getCroppedCanvas({
    width: 1920,
    height: 1080,
    imageSmoothingEnabled: true,
    imageSmoothingQuality: 'high',
  });
  const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/jpeg', 0.9));
  if (!blob) {
    errorMessage.value = '图片裁剪失败，请换一张图片重试。';
    return;
  }
  busy.value = true;
  errorMessage.value = '';
  try {
    await props.dashboard.uploadHomepageBanner(blob);
    message.value = `已上传 ${currentFileName.value}。`;
    openNextCrop();
    await nextTick();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '首页图片上传失败。';
  } finally {
    busy.value = false;
  }
}

function cancelCurrentCrop() {
  openNextCrop();
}

async function move(index: number, offset: number) {
  const target = index + offset;
  if (target < 0 || target >= banners.value.length) return;
  const reordered = [...banners.value];
  [reordered[index], reordered[target]] = [reordered[target], reordered[index]];
  busy.value = true;
  errorMessage.value = '';
  try {
    await props.dashboard.reorderHomepageBanners(reordered.map((item) => item.id));
    message.value = '首页图片顺序已更新。';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '首页图片排序失败。';
  } finally {
    busy.value = false;
  }
}

function remove(id: number) {
  pendingDeleteId.value = id;
}

async function confirmRemove() {
  const id = pendingDeleteId.value;
  if (id === null) return;
  pendingDeleteId.value = null;
  busy.value = true;
  errorMessage.value = '';
  try {
    await props.dashboard.deleteHomepageBanner(id);
    message.value = '首页图片已删除。';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '首页图片删除失败。';
  } finally {
    busy.value = false;
  }
}

function destroyCropper() {
  cropper?.destroy();
  cropper = null;
}

function revokeCropSource() {
  if (!cropSource.value) return;
  URL.revokeObjectURL(cropSource.value);
  cropSource.value = '';
}
</script>
