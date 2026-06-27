<script setup lang="ts">
/**
 * VisibilityConfigTab - 显隐条件配置 Tab (§26)
 * 显隐规则列表、添加/删除规则、条件配置抽屉
 */
import { ref, computed } from 'vue';
import { TpConfirm } from '@mdm/common/components/feedback/TpConfirm';
import type {
  VisibilityCondition,
  ConditionOperator,
} from '@/modules/model-design/types/form-design';

const generateId = () => `cond_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;

defineOptions({ name: 'VisibilityConfigTab' });

interface Props {
  conditions: VisibilityCondition[];
  targetFieldId: string | null;
  /** 可用属性列表(用于条件选择) */
  availableAttributes?: { id: string; name: string; englishName: string }[];
}

const props = withDefaults(defineProps<Props>(), {
  availableAttributes: () => [],
});

const emit = defineEmits<{
  'update:conditions': [val: VisibilityCondition[]];
}>();

const operatorOptions: { value: ConditionOperator; label: string }[] = [
  { value: 'eq', label: '等于' },
  { value: 'neq', label: '不等于' },
  { value: 'gt', label: '大于' },
  { value: 'lt', label: '小于' },
  { value: 'gte', label: '大于等于' },
  { value: 'lte', label: '小于等于' },
  { value: 'contains', label: '包含' },
  { value: 'in', label: '在列表中' },
  { value: 'empty', label: '为空' },
  { value: 'notEmpty', label: '不为空' },
];

// §26.1 规则列表(按创建时间倒序 - 这里简化为按数组倒序)
const targetConditions = computed(() => {
  if (!props.targetFieldId) return [];
  return props.conditions
    .filter(c => c.targetAttributeId === props.targetFieldId)
    .reverse();
});

// 抽屉状态
const drawerVisible = ref(false);
const editingConditionId = ref<string | null>(null);

// 临时编辑数据
const tempCondition = ref({
  logic: 'and' as 'and' | 'or',
  action: 'hide' as 'show' | 'hide',
  conditions: [] as { sourceAttributeId: string; operator: ConditionOperator; value: string }[],
});

// §26.3 添加显隐规则(打开抽屉)
const handleAddCondition = () => {
  if (!props.targetFieldId) return;
  editingConditionId.value = null;
  tempCondition.value = {
    logic: 'and',
    action: 'hide',
    conditions: [{ sourceAttributeId: '', operator: 'eq', value: '' }],
  };
  drawerVisible.value = true;
};

// 编辑规则
const handleEditCondition = (condition: VisibilityCondition) => {
  editingConditionId.value = condition.id;
  tempCondition.value = {
    logic: condition.logic,
    action: condition.action,
    conditions: [...condition.conditions.map(c => ({ ...c }))],
  };
  drawerVisible.value = true;
};

// §26.2 删除显隐规则
const handleRemoveCondition = (conditionId: string) => {
  TpConfirm.confirm('确认删除当前显隐规则？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    emit('update:conditions', props.conditions.filter(c => c.id !== conditionId));
  }).catch(() => {});
};

// 添加条件行
const addConditionRow = () => {
  tempCondition.value.conditions.push({ sourceAttributeId: '', operator: 'eq', value: '' });
};

// 删除条件行
const removeConditionRow = (index: number) => {
  tempCondition.value.conditions.splice(index, 1);
};

// 保存规则
const handleSaveCondition = () => {
  if (!props.targetFieldId) return;
  if (tempCondition.value.conditions.length === 0) return;

  if (editingConditionId.value) {
    // 更新
    emit('update:conditions', props.conditions.map(c =>
      c.id === editingConditionId.value
        ? { ...c, logic: tempCondition.value.logic, action: tempCondition.value.action, conditions: [...tempCondition.value.conditions] }
        : c,
    ));
  } else {
    // 新增
    const newCondition: VisibilityCondition = {
      id: generateId(),
      targetAttributeId: props.targetFieldId,
      logic: tempCondition.value.logic,
      action: tempCondition.value.action,
      conditions: [...tempCondition.value.conditions],
    };
    emit('update:conditions', [...props.conditions, newCondition]);
  }
  drawerVisible.value = false;
};

// §26.1 生成规则描述
const getConditionDescription = (condition: VisibilityCondition) => {
  const condParts = condition.conditions.map(c => {
    const attr = props.availableAttributes.find(a => a.id === c.sourceAttributeId);
    const op = operatorOptions.find(o => o.value === c.operator);
    return `${attr?.name || c.sourceAttributeId} ${op?.label || c.operator} ${c.value}`;
  });
  const logicStr = condition.logic === 'and' ? ' 且 ' : ' 或 ';
  const condStr = condParts.join(logicStr);
  const actionStr = condition.action === 'hide' ? '隐藏' : '显示';
  return `当【${condStr}】时${actionStr}`;
};

// 获取属性名称
const getAttributeName = (attrId: string) => {
  return props.availableAttributes.find(a => a.id === attrId)?.name || attrId;
};
</script>

<template>
  <div class="visibility-config-tab">
    <!-- §26.5 帮助提示 -->
    <div class="flex items-center justify-between mb-3">
      <div class="flex items-center gap-2">
        <span class="text-sm font-medium">显隐规则</span>
        <el-tooltip placement="top" content="IF 表达式说明：当条件字段的值满足设定条件时，控制目标字段的显示或隐藏">
          <el-icon class="cursor-help text-[var(--el-color-info)]"><i class="el-icon-question-filled" /></el-icon>
        </el-tooltip>
      </div>
      <el-button type="primary" size="small" :disabled="!targetFieldId" @click="handleAddCondition">
        添加规则
      </el-button>
    </div>

    <div v-if="!targetFieldId" class="text-sm text-[var(--el-text-color-placeholder)] text-center py-4">
      请先选择字段
    </div>

    <div v-else-if="targetConditions.length === 0" class="text-sm text-[var(--el-text-color-placeholder)] text-center py-4">
      暂无显隐规则
    </div>

    <!-- §26.1 规则列表 -->
    <div v-else class="space-y-2">
      <div
        v-for="condition in targetConditions"
        :key="condition.id"
        class="p-3 bg-[var(--el-fill-color-light)] rounded border border-[var(--el-border-color-lighter)]"
      >
        <div class="flex items-start justify-between gap-2">
          <div class="flex-1 min-w-0">
            <div class="text-sm mb-1">
              <el-tag :type="condition.action === 'hide' ? 'danger' : 'success'" size="small" class="mr-1">
                {{ condition.action === 'hide' ? '隐藏' : '显示' }}
              </el-tag>
            </div>
            <div class="text-xs text-[var(--el-text-color-regular)]">
              {{ getConditionDescription(condition) }}
            </div>
            <div class="mt-1 text-xs text-[var(--el-text-color-placeholder)]">
              条件数：{{ condition.conditions.length }} | 逻辑：{{ condition.logic === 'and' ? '且' : '或' }}
            </div>
          </div>
          <div class="flex gap-1 flex-shrink-0">
            <el-button size="small" text type="primary" @click="handleEditCondition(condition)">编辑</el-button>
            <el-button size="small" text type="danger" @click="handleRemoveCondition(condition.id)">删除</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- §26.3 条件配置抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="editingConditionId ? '编辑显隐规则' : '添加显隐规则'"
      size="500px"
      direction="rtl"
    >
      <el-form label-position="top" size="small">
        <!-- 动作 -->
        <el-form-item label="动作">
          <el-radio-group v-model="tempCondition.action">
            <el-radio value="show">显示</el-radio>
            <el-radio value="hide">隐藏</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 逻辑关系 -->
        <el-form-item label="条件逻辑">
          <el-radio-group v-model="tempCondition.logic">
            <el-radio value="and">且 (AND)</el-radio>
            <el-radio value="or">或 (OR)</el-radio>
          </el-radio-group>
        </el-form-item>

        <!-- 条件列表 -->
        <el-form-item label="条件配置">
          <div class="w-full space-y-3">
            <div
              v-for="(cond, index) in tempCondition.conditions"
              :key="index"
              class="p-3 border border-[var(--el-border-color-lighter)] rounded bg-[var(--el-fill-color-lighter)]"
            >
              <div class="flex items-center justify-between mb-2">
                <span class="text-xs font-medium">条件 {{ index + 1 }}</span>
                <el-button
                  v-if="tempCondition.conditions.length > 1"
                  size="small"
                  text
                  type="danger"
                  @click="removeConditionRow(index)"
                >
                  删除
                </el-button>
              </div>

              <el-form-item label="条件属性" class="mb-2">
                <el-select v-model="cond.sourceAttributeId" class="w-full" filterable placeholder="请选择属性">
                  <el-option
                    v-for="attr in availableAttributes"
                    :key="attr.id"
                    :label="`${attr.name} (${attr.englishName})`"
                    :value="attr.id"
                  />
                </el-select>
              </el-form-item>

              <el-form-item label="运算符" class="mb-2">
                <el-select v-model="cond.operator" class="w-full">
                  <el-option
                    v-for="op in operatorOptions"
                    :key="op.value"
                    :label="op.label"
                    :value="op.value"
                  />
                </el-select>
              </el-form-item>

              <el-form-item v-if="cond.operator !== 'empty' && cond.operator !== 'notEmpty'" label="比较值" class="mb-0">
                <el-input v-model="cond.value" placeholder="请输入比较值" />
              </el-form-item>
            </div>

            <el-button size="small" @click="addConditionRow">+ 添加条件</el-button>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveCondition">保存</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.visibility-config-tab {
  width: 100%;
}
</style>
