<script setup lang="ts">
/**
 * TreeStylePanel - 树列样式配置 (§28)
 * 展现方式(列表/树/树列表) + 对应配置项
 */
import { ref, computed, watch } from 'vue';
import type { TreeStyleConfig, DisplayMode } from '@/modules/model-design/types/form-design';
import { DISPLAY_MODE_LABEL } from '@/modules/model-design/types/form-design';

defineOptions({ name: 'TreeStylePanel' });

interface Props {
  modelValue?: TreeStyleConfig;
  /** 可用属性列表 */
  availableAttributes?: { id: string; name: string; englishName: string; dataType: string }[];
}

const props = withDefaults(defineProps<Props>(), {
  availableAttributes: () => [],
});

const emit = defineEmits<{
  'update:modelValue': [val: TreeStyleConfig];
}>();

// 内部状态
const config = computed<TreeStyleConfig>({
  get: () => props.modelValue || { displayMode: 'list' },
  set: (val) => emit('update:modelValue', val),
});

const displayMode = computed({
  get: () => config.value.displayMode || 'list',
  set: (val: DisplayMode) => {
    emit('update:modelValue', { ...config.value, displayMode: val });
  },
});

const updateField = (key: keyof TreeStyleConfig, value: unknown) => {
  emit('update:modelValue', { ...config.value, [key]: value });
};

// 子字段/父字段不能重复选择
const usedFieldIds = computed(() => {
  const ids: string[] = [];
  if (config.value.childFieldId) ids.push(config.value.childFieldId);
  if (config.value.parentFieldId) ids.push(config.value.parentFieldId);
  return ids;
});

const isFieldUsed = (fieldId: string, currentKey: string) => {
  if (currentKey === 'childFieldId' && fieldId === config.value.parentFieldId) return true;
  if (currentKey === 'parentFieldId' && fieldId === config.value.childFieldId) return true;
  return false;
};
</script>

<template>
  <div class="tree-style-panel">
    <!-- §28.6 静态提示 -->
    <el-alert type="info" :closable="false" show-icon class="mb-3">
      <template #title>提示</template>
      <div class="text-xs">
        配置数据展现方式：列表为默认展现形式；树/树列表需要配置父子字段关系
      </div>
    </el-alert>

    <el-form label-position="top" size="small">
      <!-- §28.1 展现方式 -->
      <el-form-item label="展现方式">
        <el-radio-group :model-value="displayMode" @update:model-value="displayMode = $event">
          <el-radio v-for="(label, key) in DISPLAY_MODE_LABEL" :key="key" :value="key">
            {{ label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>

      <!-- §28.2 列表方式：无需额外配置 -->
      <div v-if="displayMode === 'list'" class="p-3 bg-[var(--el-fill-color-light)] rounded text-sm text-[var(--el-text-color-secondary)]">
        列表方式：默认展现，无需额外配置
      </div>

      <!-- §28.3 树展现方式 -->
      <template v-if="displayMode === 'tree'">
        <el-form-item label="子字段">
          <el-select
            :model-value="config.childFieldId"
            class="w-full"
            filterable
            placeholder="请选择子字段"
            @update:model-value="updateField('childFieldId', $event)"
          >
            <el-option
              v-for="attr in availableAttributes"
              :key="attr.id"
              :label="`${attr.name} (${attr.englishName})`"
              :value="attr.id"
              :disabled="isFieldUsed(attr.id, 'childFieldId')"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="父字段">
          <el-select
            :model-value="config.parentFieldId"
            class="w-full"
            filterable
            placeholder="请选择父字段"
            @update:model-value="updateField('parentFieldId', $event)"
          >
            <el-option
              v-for="attr in availableAttributes"
              :key="attr.id"
              :label="`${attr.name} (${attr.englishName})`"
              :value="attr.id"
              :disabled="isFieldUsed(attr.id, 'parentFieldId')"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="显示字段">
          <el-select
            :model-value="config.displayFieldIds || []"
            class="w-full"
            multiple
            filterable
            placeholder="请选择显示字段"
            @update:model-value="updateField('displayFieldIds', $event)"
          >
            <el-option
              v-for="attr in availableAttributes"
              :key="attr.id"
              :label="`${attr.name} (${attr.englishName})`"
              :value="attr.id"
            />
          </el-select>
        </el-form-item>

        <!-- §28.4 级联显示父模型 -->
        <el-form-item label="级联显示父模型">
          <el-switch
            :model-value="config.cascadeParentModel || false"
            @update:model-value="updateField('cascadeParentModel', $event)"
          />
          <span class="ml-2 text-xs text-[var(--el-text-color-placeholder)]">
            实现关联模型与当前模型组合的完整树
          </span>
        </el-form-item>
      </template>

      <!-- §28.5 树列表展现方式 -->
      <template v-if="displayMode === 'tree-list'">
        <el-form-item label="树对象字段">
          <el-select
            :model-value="config.treeObjectFieldId"
            class="w-full"
            filterable
            placeholder="请选择引用字典/模型的属性"
            @update:model-value="updateField('treeObjectFieldId', $event)"
          >
            <el-option
              v-for="attr in availableAttributes"
              :key="attr.id"
              :label="`${attr.name} (${attr.englishName})`"
              :value="attr.id"
            />
          </el-select>
          <div class="text-xs text-[var(--el-text-color-placeholder)] mt-1">
            引用字典/模型的属性，从被引用的字典/模型中选择子字段/父字段/显示字段
          </div>
        </el-form-item>

        <el-form-item label="子字段">
          <el-select
            :model-value="config.childFieldId"
            class="w-full"
            filterable
            placeholder="请选择子字段"
            @update:model-value="updateField('childFieldId', $event)"
          >
            <el-option
              v-for="attr in availableAttributes"
              :key="attr.id"
              :label="`${attr.name} (${attr.englishName})`"
              :value="attr.id"
              :disabled="isFieldUsed(attr.id, 'childFieldId')"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="父字段">
          <el-select
            :model-value="config.parentFieldId"
            class="w-full"
            filterable
            placeholder="请选择父字段"
            @update:model-value="updateField('parentFieldId', $event)"
          >
            <el-option
              v-for="attr in availableAttributes"
              :key="attr.id"
              :label="`${attr.name} (${attr.englishName})`"
              :value="attr.id"
              :disabled="isFieldUsed(attr.id, 'parentFieldId')"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="显示字段">
          <el-select
            :model-value="config.displayFieldIds || []"
            class="w-full"
            multiple
            filterable
            placeholder="请选择显示字段"
            @update:model-value="updateField('displayFieldIds', $event)"
          >
            <el-option
              v-for="attr in availableAttributes"
              :key="attr.id"
              :label="`${attr.name} (${attr.englishName})`"
              :value="attr.id"
            />
          </el-select>
        </el-form-item>
      </template>
    </el-form>
  </div>
</template>

<style scoped>
.tree-style-panel {
  width: 100%;
}
</style>
