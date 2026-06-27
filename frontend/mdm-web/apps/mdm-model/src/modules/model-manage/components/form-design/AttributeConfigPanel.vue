<script setup lang="ts">
/**
 * AttributeConfigPanel - 属性配置面板（5个Tab）
 * 属性样式 / 显隐配置 / 布局样式 / 树列样式 / 排序字段
 */
import { ref, computed } from 'vue';
import type {
  FieldStyle,
  VisibilityCondition,
  LayoutColumns,
  FieldLayout,
  SortFieldConfig,
  TreeStyleConfig,
} from '@/modules/model-design/types/form-design';
import StyleConfigTab from './StyleConfigTab.vue';
import VisibilityConfigTab from './VisibilityConfigTab.vue';
import LayoutConfigTab from './LayoutConfigTab.vue';
import TreeStylePanel from './TreeStylePanel.vue';
import SortConfigPanel from './SortConfigPanel.vue';

defineOptions({ name: 'FormDesignAttributeConfigPanel' });

interface Props {
  fieldStyles: Record<string, FieldStyle>;
  visibilityConditions: VisibilityCondition[];
  layoutColumns: LayoutColumns;
  fieldLayout?: FieldLayout;
  selectedFieldId: string | null;
  sortFields?: SortFieldConfig[];
  treeStyle?: TreeStyleConfig;
  /** 可用属性列表 */
  availableAttributes?: { id: string; name: string; englishName: string; dataType: string; status: 'enabled' | 'disabled' }[];
}

const props = withDefaults(defineProps<Props>(), {
  fieldLayout: 'horizontal',
  sortFields: () => [],
  availableAttributes: () => [],
});

const emit = defineEmits<{
  'update:fieldStyles': [val: Record<string, FieldStyle>];
  'update:visibilityConditions': [val: VisibilityCondition[]];
  'update:layoutColumns': [val: LayoutColumns];
  'update:fieldLayout': [val: FieldLayout];
  'update:sortFields': [val: SortFieldConfig[]];
  'update:treeStyle': [val: TreeStyleConfig];
}>();

const activeTab = ref('style');

// 当前选中字段的样式
const currentFieldStyle = computed(() => {
  if (!props.selectedFieldId) return null;
  return props.fieldStyles[props.selectedFieldId] || null;
});

// 当前选中字段的属性信息
const currentAttribute = computed(() => {
  if (!props.selectedFieldId) return null;
  return props.availableAttributes.find(a => a.id === props.selectedFieldId) || null;
});

// 更新字段样式
const handleStyleChange = (style: Partial<FieldStyle>) => {
  if (!props.selectedFieldId) return;
  emit('update:fieldStyles', {
    ...props.fieldStyles,
    [props.selectedFieldId]: {
      attributeId: props.selectedFieldId,
      controlType: 'text',
      colSpan: 1,
      fullRow: false,
      readonly: false,
      ...currentFieldStyle.value,
      ...style,
    },
  });
};
</script>

<template>
  <div class="attribute-config-panel flex flex-col h-full">
    <!-- Tab 头 -->
    <el-tabs v-model="activeTab" class="px-2">
      <el-tab-pane label="属性样式" name="style" />
      <el-tab-pane label="显隐配置" name="visibility" />
      <el-tab-pane label="布局样式" name="layout" />
      <el-tab-pane label="树列样式" name="tree" />
      <el-tab-pane label="排序字段" name="sort" />
    </el-tabs>

    <!-- Tab 内容 -->
    <div class="flex-1 overflow-auto p-3">
      <!-- 属性样式 Tab -->
      <template v-if="activeTab === 'style'">
        <div v-if="!selectedFieldId" class="flex items-center justify-center h-full text-sm text-[var(--el-text-color-placeholder)]">
          请先选择字段
        </div>
        <StyleConfigTab
          v-else
          :field-style="currentFieldStyle"
          :attribute-name="currentAttribute?.name"
          :data-type="currentAttribute?.dataType"
          @change="handleStyleChange"
        />
      </template>

      <!-- 显隐配置 Tab -->
      <VisibilityConfigTab
        v-else-if="activeTab === 'visibility'"
        :conditions="visibilityConditions"
        :target-field-id="selectedFieldId"
        :available-attributes="availableAttributes"
        @update:conditions="emit('update:visibilityConditions', $event)"
      />

      <!-- 布局样式 Tab -->
      <LayoutConfigTab
        v-else-if="activeTab === 'layout'"
        :layout-columns="layoutColumns"
        :field-layout="fieldLayout"
        @update:layout-columns="emit('update:layoutColumns', $event)"
        @update:field-layout="emit('update:fieldLayout', $event)"
      />

      <!-- 树列样式 Tab -->
      <TreeStylePanel
        v-else-if="activeTab === 'tree'"
        :model-value="treeStyle || { displayMode: 'list' }"
        :available-attributes="availableAttributes"
        @update:model-value="emit('update:treeStyle', $event)"
      />

      <!-- 排序字段 Tab -->
      <SortConfigPanel
        v-else-if="activeTab === 'sort'"
        :sort-fields="sortFields"
        :available-attributes="availableAttributes"
        @update:sort-fields="emit('update:sortFields', $event)"
      />
    </div>
  </div>
</template>

<style scoped>
.attribute-config-panel {
  background: var(--el-fill-color-lighter);
}
</style>
