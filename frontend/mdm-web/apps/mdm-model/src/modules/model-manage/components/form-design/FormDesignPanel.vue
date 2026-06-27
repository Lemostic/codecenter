<script setup lang="ts">
/**
 * FormDesignPanel - 填报设计 Tab 主容器
 * 三列布局：FieldTreePanel + FormPreviewPanel + AttributeConfigPanel
 */
import { ref, onMounted } from 'vue';
import { getFormDesignByModel, updateFormDesign } from '@/modules/model-design/api/form-design';
import type {
  FormGroup,
  FieldStyle,
  VisibilityCondition,
  LayoutColumns,
  FieldLayout,
  SortFieldConfig,
  TreeStyleConfig,
} from '@/modules/model-design/types/form-design';
import type { ID } from '@mdm/common/types/base';
import LayoutSwitcher from './LayoutSwitcher.vue';
import FieldTreePanel from './FieldTreePanel.vue';
import FormPreviewPanel from './FormPreviewPanel.vue';
import AttributeConfigPanel from './AttributeConfigPanel.vue';
import { TpMessage } from '@mdm/common/components/feedback/TpMessage';

defineOptions({ name: 'FormDesignPanel' });

const props = defineProps<{
  modelId: string;
  modelStatus: 'draft' | 'published';
  modelVersion: number;
}>();

// 状态
const loading = ref(false);
const saving = ref(false);
const layoutColumns = ref<LayoutColumns>(2);
const fieldLayout = ref<FieldLayout>('horizontal');
const groups = ref<FormGroup[]>([]);
const fieldStyles = ref<Record<string, FieldStyle>>({});
const visibilityConditions = ref<VisibilityCondition[]>([]);
const sortFields = ref<SortFieldConfig[]>([]);
const treeStyle = ref<TreeStyleConfig>({ displayMode: 'list' });
const selectedFieldId = ref<string | null>(null);
const formDesignId = ref<ID | null>(null);

// Mock 属性列表(用于预览和配置)
const availableAttributes = ref<{ id: string; name: string; englishName: string; dataType: string; status: 'enabled' | 'disabled' }[]>([
  { id: 'attr_1', name: '供应商名称', englishName: 'M_NAME', dataType: 'VARCHAR', status: 'enabled' },
  { id: 'attr_2', name: '供应商编码', englishName: 'M_CODE', dataType: 'VARCHAR', status: 'enabled' },
  { id: 'attr_3', name: '联系电话', englishName: 'M_PHONE', dataType: 'VARCHAR', status: 'enabled' },
  { id: 'attr_4', name: '联系地址', englishName: 'M_ADDRESS', dataType: 'VARCHAR', status: 'enabled' },
  { id: 'attr_5', name: '成立日期', englishName: 'M_FOUND_DATE', dataType: 'DATE', status: 'enabled' },
  { id: 'attr_6', name: '注册资本', englishName: 'M_CAPITAL', dataType: 'DECIMAL', status: 'enabled' },
  { id: 'attr_7', name: '备注', englishName: 'M_REMARK', dataType: 'TEXT', status: 'enabled' },
  { id: 'attr_8', name: '状态', englishName: 'M_STATUS', dataType: 'VARCHAR', status: 'enabled' },
]);

// 加载数据
const loadData = async () => {
  loading.value = true;
  try {
    const res = await getFormDesignByModel(props.modelId, props.modelVersion);
    const data = res.data.data;
    if (data) {
      formDesignId.value = data.id;
      groups.value = data.groups || [];
      fieldStyles.value = (data.fieldStyles || []).reduce((acc: Record<string, FieldStyle>, style: FieldStyle) => {
        acc[style.attributeId] = style;
        return acc;
      }, {} as Record<string, FieldStyle>);
      visibilityConditions.value = data.visibilityConditions || [];
      layoutColumns.value = data.layoutColumns || 2;
      fieldLayout.value = data.fieldLayout || 'horizontal';
      sortFields.value = data.sortFields || [];
      treeStyle.value = data.treeStyle || { displayMode: 'list' };
    }
  } catch (err) {
    console.error('[FormDesignPanel] load error', err);
  } finally {
    loading.value = false;
  }
};

// 保存配置
const handleSave = async () => {
  if (!formDesignId.value) {
    TpMessage.error('表单设计 ID 不存在，请重新加载');
    return;
  }
  saving.value = true;
  try {
    const fieldStylesArray = Object.values(fieldStyles.value);
    await updateFormDesign({
      id: formDesignId.value,
      layoutColumns: layoutColumns.value,
      fieldLayout: fieldLayout.value,
      groups: groups.value,
      fieldStyles: fieldStylesArray,
      visibilityConditions: visibilityConditions.value,
      sortFields: sortFields.value,
      treeStyle: treeStyle.value,
    });
    TpMessage.success('保存成功');
  } catch (err) {
    TpMessage.error('保存失败');
  } finally {
    saving.value = false;
  }
};

// 字段选择
const handleFieldSelect = (fieldId: string) => {
  selectedFieldId.value = fieldId;
};

// 初始化
onMounted(() => {
  loadData();
});

defineExpose({ loadData });
</script>

<template>
  <div class="form-design-panel flex h-full">
    <!-- 第一列：字段树 -->
    <div class="w-48 border-r border-[var(--el-border-color-lighter)]">
      <FieldTreePanel :model-id="modelId" @select="handleFieldSelect" />
    </div>

    <!-- 第二列：表单预览 -->
    <div class="flex-1 p-4 overflow-auto">
      <FormPreviewPanel
        v-model:groups="groups"
        v-model:field-styles="fieldStyles"
        :layout-columns="layoutColumns"
        :field-layout="fieldLayout"
        :selected-field-id="selectedFieldId"
        :attributes="availableAttributes"
        :visibility-conditions="visibilityConditions"
        @field-select="handleFieldSelect"
      />
    </div>

    <!-- 第三列：属性配置 -->
    <div class="w-80 border-l border-[var(--el-border-color-lighter)]">
      <AttributeConfigPanel
        v-model:field-styles="fieldStyles"
        v-model:visibility-conditions="visibilityConditions"
        v-model:layout-columns="layoutColumns"
        v-model:field-layout="fieldLayout"
        v-model:sort-fields="sortFields"
        v-model:tree-style="treeStyle"
        :selected-field-id="selectedFieldId"
        :available-attributes="availableAttributes"
      />
    </div>

    <!-- 底部工具栏 -->
    <div class="form-design-panel__footer px-4 py-3">
      <LayoutSwitcher v-model="layoutColumns" />
      <el-button type="primary" :loading="saving" @click="handleSave">保存配置</el-button>
    </div>
  </div>
</template>

<style scoped>
.form-design-panel {
  position: relative;
  background: var(--el-fill-color-light);
}

.form-design-panel__footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--el-fill-color-lighter);
  border-top: 1px solid var(--el-border-color-lighter);
}
</style>
