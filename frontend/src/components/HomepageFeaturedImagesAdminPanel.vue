<template>
  <section class="homepage-featured-admin admin-reference-page" aria-label="精选图片管理">
    <header class="reference-page-header homepage-featured-header">
      <span class="reference-page-icon"><GalleryHorizontalEnd :size="22" /></span>
      <div>
        <h2>精选图片</h2>
        <p>最多 12 张，上传后统一裁剪为 1200×800。这里的顺序就是主页循环图片带的顺序。</p>
      </div>
      <span class="homepage-featured-count">{{ images.length }} / {{ MAX_IMAGE_COUNT }}</span>
    </header>

    <p v-if="message" class="operation-toast" role="status">{{ message }}</p>
    <p v-if="errorMessage" class="form-error" role="alert">{{ errorMessage }}</p>

    <div class="homepage-featured-list">
      <article v-for="(image, index) in images" :key="image.id" class="homepage-featured-card">
        <div class="homepage-featured-preview">
          <img :src="image.thumbnailUrl || image.imageUrl" :alt="`精选图片 ${index + 1}`">
          <span>{{ String(index + 1).padStart(2, '0') }}</span>
        </div>
        <div class="homepage-featured-actions">
          <button
            :disabled="busy || index === 0"
            type="button"
            :aria-label="`将第 ${index + 1} 张精选图片向左移动`"
            @click="move(index, -1)"
          ><ArrowLeft :size="16" />向左</button>
          <button
            :disabled="busy || index === images.length - 1"
            type="button"
            :aria-label="`将第 ${index + 1} 张精选图片向右移动`"
            @click="move(index, 1)"
          >向右<ArrowRight :size="16" /></button>
          <button
            class="danger-link"
            :disabled="busy"
            type="button"
            :aria-label="`删除第 ${index + 1} 张精选图片`"
            @click="pendingDeleteId = image.id"
          ><Trash2 :size="16" />删除</button>
        </div>
      </article>

      <label v-if="images.length < MAX_IMAGE_COUNT" class="homepage-featured-add" aria-label="添加精选图片">
        <Plus :size="46" :stroke-width="1.4" aria-hidden="true" />
        <strong>添加精选图片</strong>
        <small>可一次选择多张</small>
        <input accept="image/*" multiple type="file" @change="selectFiles">
      </label>
    </div>

    <p v-if="!images.length && !errorMessage" class="homepage-featured-empty">
      还没有精选图片。添加后，主页大图下方会自动出现循环图片带。
    </p>

    <AdminConfirmDialog
      :open="pendingDeleteId !== null"
      dialog-id="homepage-featured-image-delete"
      title="删除这张精选图片？"
      description="删除后主页循环图片带会立即停止展示这张图片。"
      confirm-label="确认删除"
      :busy="busy"
      icon="delete"
      tone="danger"
      @cancel="pendingDeleteId = null"
      @confirm="confirmRemove"
    />

    <div v-if="cropSource" class="homepage-crop-dialog" role="dialog" aria-modal="true" aria-labelledby="featured-crop-title">
      <section class="homepage-crop-panel">
        <header>
          <div>
            <h2 id="featured-crop-title">裁剪精选图片</h2>
            <p>{{ currentFileName }} · 固定 3:2，可拖动并滚轮缩放</p>
          </div>
          <span v-if="pendingFiles.length" class="crop-queue-count">后续还有 {{ pendingFiles.length }} 张</span>
        </header>
        <div class="homepage-crop-stage homepage-featured-crop-stage">
          <img ref="cropImage" :src="cropSource" alt="待裁剪的精选图片" @load="initializeCropper">
        </div>
        <footer>
          <button class="secondary-button" :disabled="busy" type="button" @click="openNextCrop">跳过这张</button>
          <button class="primary-button" :disabled="busy" type="button" @click="uploadCrop">
            <Crop :size="18" />{{ busy ? '正在上传' : '裁剪并上传' }}
          </button>
        </footer>
      </section>
    </div>
  </section>
</template>

<script setup lang="ts">
// Author: huangbingrui.awa
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import {
  ArrowLeft,
  ArrowRight,
  Crop,
  GalleryHorizontalEnd,
  Plus,
  Trash2,
} from '@lucide/vue';
import Cropper from 'cropperjs';
import 'cropperjs/dist/cropper.css';
import type { usePlatformDashboard } from '../composables/usePlatformDashboard';
import AdminConfirmDialog from './AdminConfirmDialog.vue';

const props = defineProps<{ dashboard: ReturnType<typeof usePlatformDashboard> }>();
const MAX_IMAGE_COUNT = 12;
const cropImage = ref<HTMLImageElement | null>(null);
const cropSource = ref('');
const currentFileName = ref('');
const pendingFiles = ref<File[]>([]);
const busy = ref(false);
const message = ref('');
const errorMessage = ref('');
const pendingDeleteId = ref<number | null>(null);
let cropper: Cropper | null = null;

const images = computed(() => props.dashboard.homepageFeaturedImages.value);

onMounted(async () => {
  try {
    await props.dashboard.loadHomepageFeaturedImages();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '精选图片加载失败。';
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
  const occupiedSlots = images.value.length + pendingFiles.value.length + (cropSource.value ? 1 : 0);
  const availableSlots = Math.max(0, MAX_IMAGE_COUNT - occupiedSlots);
  if (!availableSlots) {
    errorMessage.value = '精选图片最多保留 12 张，请先删除一张再上传。';
    return;
  }
  pendingFiles.value = [...pendingFiles.value, ...selected.slice(0, availableSlots)];
  errorMessage.value = selected.length > availableSlots
    ? '精选图片最多保留 12 张，超出的文件未加入。'
    : '';
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
    aspectRatio: 3 / 2,
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
    width: 1200,
    height: 800,
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
    await props.dashboard.uploadHomepageFeaturedImage(blob);
    message.value = `已上传 ${currentFileName.value}。`;
    openNextCrop();
    await nextTick();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '精选图片上传失败。';
  } finally {
    busy.value = false;
  }
}

async function move(index: number, offset: number) {
  const target = index + offset;
  if (target < 0 || target >= images.value.length) return;
  const reordered = [...images.value];
  [reordered[index], reordered[target]] = [reordered[target], reordered[index]];
  busy.value = true;
  errorMessage.value = '';
  try {
    await props.dashboard.reorderHomepageFeaturedImages(reordered.map((item) => item.id));
    message.value = '精选图片顺序已更新。';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '精选图片排序失败。';
  } finally {
    busy.value = false;
  }
}

async function confirmRemove() {
  const id = pendingDeleteId.value;
  if (id === null) return;
  pendingDeleteId.value = null;
  busy.value = true;
  errorMessage.value = '';
  try {
    await props.dashboard.deleteHomepageFeaturedImage(id);
    message.value = '精选图片已删除。';
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '精选图片删除失败。';
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
