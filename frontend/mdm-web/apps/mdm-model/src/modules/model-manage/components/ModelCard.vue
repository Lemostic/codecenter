<script setup lang="ts">
/**
 * ModelCard - 模型卡片组件
 *
 * 展示单个主数据模型的卡片视图，包含模型图标、名称、
 * 表名称、模型类型、状态标签和授权按钮。
 * 样式严格对照 Figma 设计稿（资产/结构化资产节点）。
 */
import { Document } from '@element-plus/icons-vue';
import type { CheckboxValueType } from 'element-plus';
import type { ModelVO } from '@/modules/model-design/types/model';

defineOptions({ name: 'ModelCard' });

const props = defineProps<{
  /** 模型数据 */
  model: ModelVO;
  /** 是否选中 */
  selected?: boolean;
}>();

const emit = defineEmits<{
  (e: 'click', model: ModelVO): void;
  (e: 'select', model: ModelVO, selected: boolean): void;
  (e: 'auth', model: ModelVO): void;
}>();

const handleClick = () => emit('click', props.model);

const handleCheckboxChange = (val: CheckboxValueType) => emit('select', props.model, Boolean(val));

const handleAuth = (e: Event) => {
  e.stopPropagation();
  emit('auth', props.model);
};

/** 密级 → 中文 */
const securityLevelLabel = computed<string>(() => {
  const raw = (props.model.securityLevel || props.model.secretLevel || '').toString().trim();
  if (!raw) return '';
  const map: Record<string, string> = {
    INTERNAL: '内部', PUBLIC: '公开', OPEN: '公开', SECRET: '秘密', CONFIDENTIAL: '机密', TOP_SECRET: '绝密',
  };
  return map[raw.toUpperCase()] ?? raw;
});

/** 状态徽章配色: 生效=绿, 其余=红 */
const statusBadgeStyle = computed(() => {
  const isActive = props.model.status === 'EFFECT' || props.model.statusLabel === '生效';
  if (isActive) return { bg: 'rgba(82, 196, 26, 0.12)', border: 'rgba(82, 196, 26, 0.5)', color: '#389e0d' };
  return { bg: 'rgba(227, 77, 89, 0.1)', border: 'rgba(227, 77, 89, 0.5)', color: '#e34d59' };
});
</script>

<template>
  <div
    class="relative bg-white cursor-pointer transition-all"
    style="border-radius: 4px; border: 1px solid #dcdfe6; box-shadow: 0px 1px 4px 0px rgba(0, 0, 0, 0.08);"
    :class="{ 'ring-2 ring-[var(--el-color-primary)]': selected }"
    @click="handleClick"
  >
    <!-- 复选框（右上角） -->
    <div class="absolute top-4 right-4 z-10" @click.stop>
      <el-checkbox
        :model-value="selected"
        @update:model-value="handleCheckboxChange"
      />
    </div>

    <!-- 卡片头部：图标 + 名称 + 编码 -->
    <div class="flex items-start gap-[15px] px-4 pt-4 pb-2">
      <!-- 图标框 36x36 蓝色 -->
      <div class="flex-shrink-0 flex items-center justify-center" style="width: 36px; height: 36px; border-radius: 4px; background: #337bff;">
        <el-icon :size="20" color="#fff"><Document /></el-icon>
      </div>
      <!-- 名称 + 编码（纵向排列） -->
      <div class="flex-1 min-w-0">
        <div class="truncate" style="font-size: 14px; font-weight: 500; color: #3d4247; line-height: 24px;" :title="model.modelName || model.name">
          {{ model.modelName || model.name }}
        </div>
        <div class="truncate" style="font-size: 12px; font-weight: 400; color: #666666; line-height: 20px;" :title="model.modelCode || model.code">
          {{ model.modelCode || model.code }}
        </div>
      </div>
    </div>

    <!-- 信息行 -->
    <div class="px-4 space-y-0">
      <div class="flex items-center" style="font-size: 13px; color: #666666; line-height: 30px;">
        表名称：{{ model.tableName }}
      </div>
      <div class="flex items-center" style="font-size: 13px; color: #666666; line-height: 30px;">
        模型类型：{{ model.modelTypeLabel }}
      </div>
      <div class="flex items-center" style="font-size: 13px; color: #666666; line-height: 30px;">
        创建时间：{{ model.createTime || model.createdAt }}
      </div>
    </div>

    <!-- 底部：标签 + 授权按钮 -->
    <div class="flex items-center justify-between px-4 pb-4 pt-2">
      <div class="flex items-center">
        <!-- 机密标签（可选，有值时显示） -->
        <div
          v-if="securityLevelLabel"
          class="flex items-center justify-center"
          style="padding: 6px 8px; border-radius: 3px; background: rgba(227, 77, 89, 0.1); border: 1px solid rgba(227, 77, 89, 0.5);"
        >
          <span style="font-size: 12px; line-height: 16px; color: #e34d59;">{{ securityLevelLabel }}</span>
        </div>
        <!-- 版本 + 状态组合标签 -->
        <div class="flex items-center">
          <div
            class="flex items-center justify-center"
            style="padding: 6px 8px; border-radius: 3px 0 0 3px; border: 1px solid rgba(227, 77, 89, 0.1);"
          >
            <span style="font-size: 12px; line-height: 16px; color: #333333;">{{ model.versionLabel }}</span>
          </div>
          <div
            class="flex items-center justify-center"
            style="padding: 6px 8px; border-radius: 0 3px 3px 0;"
            :style="{ background: statusBadgeStyle.bg, border: '1px solid ' + statusBadgeStyle.border }"
          >
            <span :style="{ fontSize: '12px', lineHeight: '16px', color: statusBadgeStyle.color }">{{ model.statusLabel }}</span>
          </div>
        </div>
      </div>
      <!-- 授权按钮 -->
      <el-button size="default" @click="handleAuth" style="border-radius: 2px;">授权</el-button>
    </div>
  </div>
</template>
