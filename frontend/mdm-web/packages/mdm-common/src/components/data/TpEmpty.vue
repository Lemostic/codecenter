<script setup lang="ts">
/**
 * TpEmpty - 统一空状态组件
 *
 * 封装 el-empty，提供一致的空状态展示样式。
 * 可通过插槽自定义描述文案和操作按钮。
 */

defineOptions({ name: 'TpEmpty' });

withDefaults(defineProps<{
  /** 描述文案 */
  description?: string;
  /** 图片尺寸（px） */
  imageSize?: number;
}>(), {
  description: '暂无数据',
  imageSize: 120,
});
</script>

<template>
  <div class="flex flex-col items-center justify-center h-full min-h-[200px]">
    <el-empty :description="description" :image-size="imageSize">
      <template #description>
        <slot name="description">
          <p>{{ description }}</p>
        </slot>
      </template>
      <template #default>
        <slot />
      </template>
    </el-empty>
  </div>
</template>
