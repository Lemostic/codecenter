<script setup lang="ts">
/**
 * TpPageHeader - 页面标题组件（带返回功能）
 *
 * 在标题左侧显示返回箭头，点击后导航回列表页。
 * 支持通过 `backTo` 指定返回路由，不传则调用 `router.back()`。
 * 右侧可通过 `#actions` 插槽放置操作按钮。
 */
import { ArrowLeft } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';
import type { RouteLocationRaw } from 'vue-router';

defineOptions({ name: 'TpPageHeader' });

const props = defineProps<{
  /** 页面标题 */
  title: string;
  /** 返回路由；不传则 router.back() */
  backTo?: RouteLocationRaw;
}>();

const router = useRouter();

const handleBack = () => {
  if (props.backTo) {
    router.push(props.backTo);
  } else {
    router.back();
  }
};
</script>

<template>
  <div class="flex justify-between items-center flex-shrink-0 mb-3">
    <div class="flex items-center gap-2">
      <el-button
        size="default"
        text
        :icon="ArrowLeft"
        @click="handleBack"
      />
      <h1 class="text-lg font-semibold text-[#1d1d1f] m-0">{{ title }}</h1>
    </div>
    <div v-if="$slots.actions" class="flex items-center gap-2">
      <slot name="actions" />
    </div>
  </div>
</template>
