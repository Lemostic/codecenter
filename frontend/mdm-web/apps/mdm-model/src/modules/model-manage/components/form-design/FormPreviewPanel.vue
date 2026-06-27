<script setup lang="ts">
/**
 * FormPreviewPanel - 表单预览 (§30)
 * 反映属性样式、显隐条件、布局样式、分组展示
 */
import { computed, ref } from 'vue';
import type { FormGroup, FieldStyle, FieldLayout, VisibilityCondition } from '../../types/form-design';
import { CONTROL_TYPE_LABEL, TOOLTIP_EFFECT_LABEL } from '../../types/form-design';
import type { ControlType, TooltipEffect } from '../../types/form-design';
import GroupList from './GroupList.vue';

defineOptions({ name: 'FormPreviewPanel' });

interface Props {
  groups: FormGroup[];
  fieldStyles: Record<string, FieldStyle>;
  layoutColumns: 1 | 2 | 3 | 4;
  fieldLayout?: FieldLayout;
  selectedFieldId: string | null;
  /** 可用属性信息(用于预览渲染) */
  attributes?: { id: string; name: string; englishName: string; dataType: string; status: 'enabled' | 'disabled' }[];
  /** 显隐条件 */
  visibilityConditions?: VisibilityCondition[];
}

const props = withDefaults(defineProps<Props>(), {
  fieldLayout: 'horizontal',
  attributes: () => [],
  visibilityConditions: () => [],
});

const emit = defineEmits<{
  'update:groups': [val: FormGroup[]];
  'update:fieldStyles': [val: Record<string, FieldStyle>];
  'field-select': [fieldId: string];
}>();

const groupsModel = computed({
  get: () => props.groups,
  set: (val) => emit('update:groups', val),
});

// §30.1 预览模式切换(主表/子表)
const activeTable = ref('main');

// §30.5 获取属性信息
const getAttrInfo = (attrId: string) => {
  return props.attributes.find(a => a.id === attrId);
};

// 获取字段样式
const getFieldStyle = (attrId: string): FieldStyle => {
  return props.fieldStyles[attrId] || {
    attributeId: attrId,
    controlType: 'text',
    colSpan: 1,
    fullRow: false,
    readonly: false,
  };
};

// §30.3 检查字段是否应该隐藏(根据显隐条件)
const isFieldHidden = (attrId: string) => {
  return props.visibilityConditions.some(cond => {
    if (cond.targetAttributeId !== attrId) return false;
    // 简化：检查条件是否满足(这里用 mock 逻辑)
    return cond.action === 'hide';
  });
};

// 处理字段点击
const handleFieldClick = (attrId: string) => {
  emit('field-select', attrId);
};

// §30.2 获取控件渲染提示
const getControlLabel = (style: FieldStyle) => {
  return CONTROL_TYPE_LABEL[style.controlType as ControlType] || '文本框';
};

// §24.2 分组标题：当前分组属性数/模型属性总数
const totalAttrCount = computed(() => props.attributes.length);

// 提示消息
const tipMessage = '在主数据查看/维护页面，将按属性填报设计渲染页面\n输入方式根据模型属性配置的内容不同，存在不同的可选项和默认值';
</script>

<template>
  <div class="form-preview-panel">
    <!-- §30.1 消息提示 -->
    <el-alert type="info" :closable="false" show-icon class="mb-3">
      <template #title>提示</template>
      <div class="text-xs whitespace-pre-line">{{ tipMessage }}</div>
    </el-alert>

    <!-- §30.1 主表/子表切换 -->
    <div class="flex items-center justify-between mb-3">
      <el-radio-group v-model="activeTable" size="small">
        <el-radio-button value="main">主表</el-radio-button>
        <el-radio-button value="sub">子表</el-radio-button>
      </el-radio-group>
      <div class="flex gap-2">
        <el-button size="small">新建字段分组</el-button>
        <el-button size="small">导入分组及属性样式</el-button>
      </div>
    </div>

    <!-- §30.4 + §30.5 预览区域 -->
    <div class="preview-area border border-[var(--el-border-color-lighter)] rounded bg-white p-4">
      <!-- 分组展示 -->
      <div
        v-for="group in groups"
        :key="group.id"
        class="mb-4"
      >
        <!-- §24.2 分组标题 -->
        <div class="text-sm font-medium mb-2 pb-1 border-b border-[var(--el-border-color-lighter)]">
          {{ group.name }}
          <span class="text-xs text-[var(--el-text-color-placeholder)] ml-1">
            ({{ group.attributeIds.length }}/{{ totalAttrCount }})
          </span>
        </div>

        <!-- §30.4 按布局列数渲染字段 -->
        <div
          class="grid gap-3"
          :style="{ gridTemplateColumns: `repeat(${layoutColumns}, 1fr)` }"
        >
          <template v-for="attrId in group.attributeIds" :key="attrId">
            <div
              v-if="!isFieldHidden(attrId)"
              class="field-preview-item"
              :class="{
                'col-span-full': getFieldStyle(attrId).fullRow,
                'is-selected': selectedFieldId === attrId,
              }"
              :style="{
                gridColumn: getFieldStyle(attrId).fullRow ? `span ${layoutColumns}` : `span ${getFieldStyle(attrId).colSpan}`,
              }"
              @click="handleFieldClick(attrId)"
            >
              <!-- §30.4 字段布局：左右/上下 -->
              <div
                class="field-preview-inner"
                :class="fieldLayout === 'vertical' ? 'flex-col' : 'flex-row'"
              >
                <!-- 字段标签 -->
                <div class="field-label flex items-center gap-1 flex-shrink-0">
                  <span>{{ getAttrInfo(attrId)?.name || attrId }}</span>
                  <!-- §24.4 已停用属性标识 -->
                  <el-tag v-if="getAttrInfo(attrId)?.status === 'disabled'" type="danger" size="small">
                    已停用
                  </el-tag>
                  <!-- §25.8 提示效果 -->
                  <el-tooltip
                    v-if="getFieldStyle(attrId).tooltip && getFieldStyle(attrId).tooltipEffect === 'icon'"
                    :content="getFieldStyle(attrId).tooltip"
                    placement="top"
                  >
                    <el-icon class="text-[var(--el-color-info)] cursor-help"><i class="el-icon-question-filled" /></el-icon>
                  </el-tooltip>
                </div>

                <!-- 控件预览 -->
                <div class="field-control flex-1 min-w-0">
                  <!-- §30.2 根据控件类型渲染不同预览 -->
                  <template v-if="getFieldStyle(attrId).controlType === 'textarea'">
                    <div
                      class="preview-textarea bg-[var(--el-fill-color-light)] rounded p-2 text-xs text-[var(--el-text-color-placeholder)]"
                      :style="{ minHeight: `${(getFieldStyle(attrId).heightMultiple || 2) * 40}px` }"
                    >
                      {{ getFieldStyle(attrId).placeholder || getControlLabel(getFieldStyle(attrId)) }}
                    </div>
                  </template>
                  <template v-else-if="getFieldStyle(attrId).controlType === 'select' || getFieldStyle(attrId).controlType === 'multiselect'">
                    <div class="preview-select bg-[var(--el-fill-color-light)] rounded px-2 py-1 text-xs text-[var(--el-text-color-placeholder)]">
                      {{ getFieldStyle(attrId).placeholder || `请选择 - ${getControlLabel(getFieldStyle(attrId))}` }}
                    </div>
                  </template>
                  <template v-else-if="getFieldStyle(attrId).controlType === 'date' || getFieldStyle(attrId).controlType === 'datetime'">
                    <div class="preview-date bg-[var(--el-fill-color-light)] rounded px-2 py-1 text-xs text-[var(--el-text-color-placeholder)]">
                      {{ getFieldStyle(attrId).placeholder || '选择日期' }}
                    </div>
                  </template>
                  <template v-else>
                    <div class="preview-input bg-[var(--el-fill-color-light)] rounded px-2 py-1 text-xs text-[var(--el-text-color-placeholder)]">
                      {{ getFieldStyle(attrId).placeholder || '请输入' }}
                    </div>
                  </template>

                  <!-- §25.7/25.8 文本提示/气泡提示 -->
                  <div
                    v-if="getFieldStyle(attrId).tooltip && getFieldStyle(attrId).tooltipEffect !== 'icon'"
                    class="text-xs text-[var(--el-text-color-placeholder)] mt-1"
                  >
                    {{ getFieldStyle(attrId).tooltip }}
                  </div>
                </div>
              </div>
            </div>
          </template>
        </div>
      </div>

      <!-- 无分组时显示默认组 -->
      <div v-if="groups.length === 0" class="text-center text-sm text-[var(--el-text-color-placeholder)] py-8">
        暂无分组配置，请在左侧创建分组
      </div>
    </div>
  </div>
</template>

<style scoped>
.form-preview-panel {
  height: 100%;
  overflow: auto;
}

.field-preview-item {
  cursor: pointer;
  padding: 8px;
  border: 1px solid transparent;
  border-radius: 4px;
  transition: all 0.15s;
}

.field-preview-item:hover {
  background-color: var(--el-fill-color-light);
}

.field-preview-item.is-selected {
  border-color: var(--el-color-primary);
  background-color: var(--el-color-primary-light-9);
}

.field-preview-inner {
  display: flex;
  gap: 8px;
}

.field-preview-inner.flex-col {
  flex-direction: column;
}

.field-preview-inner.flex-row {
  flex-direction: row;
  align-items: center;
}

.field-label {
  font-size: 13px;
  color: var(--el-text-color-regular);
  white-space: nowrap;
  min-width: 80px;
}

.field-preview-inner.flex-col .field-label {
  margin-bottom: 4px;
}

.field-preview-inner.flex-row .field-label {
  min-width: 80px;
  width: 80px;
}

.preview-input,
.preview-select,
.preview-date,
.preview-textarea {
  border: 1px solid var(--el-border-color);
}
</style>
