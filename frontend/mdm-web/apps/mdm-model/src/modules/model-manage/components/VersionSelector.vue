<script setup lang="ts">
/**
 * VersionSelector - 版本选择器
 *
 * 用于模型管理，历史版本下拉切换
 *
 * 用法：
 *   <VersionSelector
 *     v-model="currentVersion"
 *     :versions="versionList"
 *     @change="handleChange"
 *   />
 *
 * 版本格式：V1, V2, V3...
 * 状态：生效（绿色）、编辑中（蓝色）、历史（灰色）
 */
defineOptions({ name: 'VersionSelector' });

interface VersionItem {
  version: string;
  status: 'active' | 'draft' | 'history';
}

interface Props {
  modelValue?: string;
  /** 版本列表 */
  versions?: VersionItem[];
}

withDefaults(defineProps<Props>(), {
  modelValue: undefined,
  versions: () => [],
});

const emit = defineEmits<{
  (e: 'update:modelValue', v: string): void;
  (e: 'change', v: string): void;
}>();

const currentVersion = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
});

const options = computed(() =>
  props.versions.map((v) => ({
    value: v.version,
    label: v.version,
    disabled: v.status === 'history',
  })),
);
</script>

<template>
  <el-select
    v-model="currentVersion"
    placeholder="选择版本"
    class="version-selector"
    @change="emit('change', $event)"
  >
    <el-option
      v-for="opt in options"
      :key="opt.value"
      :label="opt.label"
      :value="opt.value"
      :disabled="opt.disabled"
    />
  </el-select>
</template>

<style scoped>
.version-selector {
  width: 120px;
}
</style>
