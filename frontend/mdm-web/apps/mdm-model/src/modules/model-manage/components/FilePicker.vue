<script setup lang="ts">
/**
 * FilePicker - 标准文件选择器
 *
 * 用于模型关联标准文件的上传和选择
 * - 支持上传新文件（跳转至"治理平台-数据管理制度"菜单）
 * - 支持从已有文件列表选择
 *
 * 用法：
 *   <FilePicker v-model="fileList" @change="handleChange" />
 */
defineOptions({ name: 'FilePicker' });

interface FileItem {
  id: string;
  name: string;
  url: string;
}

interface Props {
  modelValue?: FileItem[];
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: () => [],
});

const emit = defineEmits<{
  (e: 'update:modelValue', v: FileItem[]): void;
  (e: 'change', v: FileItem[]): void;
}>();

// TODO: 实现文件选择器
// - 上传按钮：跳转至"治理平台-数据管理制度"菜单
// - 已有文件弹窗选择：调用文件管理 API
// - 显示已选文件列表
// - 支持删除
</script>

<template>
  <div class="file-picker">
    <div class="file-picker__placeholder">
      <TpEmpty description="FilePicker 组件占位，功能开发中" />
    </div>
  </div>
</template>

<style scoped>
.file-picker {
  width: 100%;
}

.file-picker__placeholder {
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
