# 填报设计功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现填报设计功能，包括三列布局（字段树/表单预览/属性配置）、分组管理、样式配置、显隐条件、布局样式

**Architecture:** FormDesignPanel 作为主容器，管理状态和 API 调用；三个子面板各自独立，通过 props/emit 与父组件通信

**Tech Stack:** Vue 3 Composition API + Element Plus + Formily + TypeScript

---

## 文件结构

```
apps/mdm-flow/src/modules/model-design/components/form-design/
├── FormDesignPanel.vue         # 主容器（已有占位，需重写）
├── FieldTreePanel.vue          # 字段树（新建）
├── FormPreviewPanel.vue        # 表单预览（新建）
├── AttributeConfigPanel.vue    # 属性配置面板（新建）
├── StyleConfigTab.vue         # 属性样式Tab（新建）
├── VisibilityConfigTab.vue      # 显隐配置Tab（已有占位，需重写）
├── LayoutConfigTab.vue         # 布局样式Tab（新建）
└── GroupList.vue              # 分组管理（已有占位，需实现）
```

---

## Task 1: 重写 FormDesignPanel 主容器

**Files:**
- Modify: `apps/mdm-flow/src/modules/model-design/components/form-design/FormDesignPanel.vue`

**Steps:**

- [ ] **Step 1: 阅读现有占位代码**

```bash
# 读取现有文件了解结构
```

- [ ] **Step 2: 实现状态管理**

```typescript
// src/modules/model-design/components/form-design/FormDesignPanel.vue
<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { getFormDesignByModel, createFormDesign, updateFormDesign } from '@/modules/model-design/api/form-design';
import type { FormGroup, FieldStyle, VisibilityCondition, FormDesignVO } from '@/modules/model-design/types/form-design';
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
const layoutColumns = ref<1 | 2 | 3 | 4>(2);
const groups = ref<FormGroup[]>([]);
const fieldStyles = ref<Record<string, FieldStyle>>({});
const visibilityConditions = ref<VisibilityCondition[]>([]);
const selectedFieldId = ref<string | null>(null);

// 加载数据
const loadData = async () => {
  loading.value = true;
  try {
    const res = await getFormDesignByModel(props.modelId, props.modelVersion);
    const data = res.data.data;
    if (data) {
      groups.value = data.groups || [];
      fieldStyles.value = data.fieldStyles || {};
      visibilityConditions.value = data.visibilityConditions || [];
      layoutColumns.value = data.layoutColumns || 2;
    }
  } catch (err) {
    console.error('[FormDesignPanel] load error', err);
  } finally {
    loading.value = false;
  }
};

// 保存配置
const handleSave = async () => {
  saving.value = true;
  try {
    const data = {
      modelId: props.modelId,
      layoutColumns: layoutColumns.value,
      groups: groups.value,
      fieldStyles: fieldStyles.value,
      visibilityConditions: visibilityConditions.value,
    };
    // 判断创建或更新
    await updateFormDesign(data as any);
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
        :selected-field-id="selectedFieldId"
      />
    </div>

    <!-- 第三列：属性配置 -->
    <div class="w-80 border-l border-[var(--el-border-color-lighter)]">
      <AttributeConfigPanel
        v-model:field-styles="fieldStyles"
        v-model:visibility-conditions="visibilityConditions"
        v-model:layout-columns="layoutColumns"
        :selected-field-id="selectedFieldId"
      />
    </div>

    <!-- 底部工具栏 -->
    <div class="form-design-panel__footer">
      <LayoutSwitcher v-model="layoutColumns" />
      <el-button type="primary" :loading="saving" @click="handleSave">保存配置</el-button>
    </div>
  </div>
</template>
```

- [ ] **Step 3: 添加样式**

```vue
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
  padding: 12px 16px;
  background: var(--el-fill-color-lighter);
  border-top: 1px solid var(--el-border-color-lighter);
}
</style>
```

- [ ] **Step 4: 提交**

```bash
git add apps/mdm-flow/src/modules/model-design/components/form-design/FormDesignPanel.vue
git commit -m "feat(form-design): rewrite FormDesignPanel as main container"
```

---

## Task 2: 实现 LayoutSwitcher 组件

**Files:**
- Create: `apps/mdm-flow/src/modules/model-design/components/form-design/LayoutSwitcher.vue`

**Steps:**

- [ ] **Step 1: 创建组件**

```vue
<script setup lang="ts">
/**
 * LayoutSwitcher - 布局列数切换
 */
defineOptions({ name: 'LayoutSwitcher' });

interface Props {
  modelValue: 1 | 2 | 3 | 4;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  'update:modelValue': [val: 1 | 2 | 3 | 4];
}>();

const options = [
  { value: 1, label: '一列' },
  { value: 2, label: '两列' },
  { value: 3, label: '三列' },
  { value: 4, label: '四列' },
] as const;

const handleSelect = (val: 1 | 2 | 3 | 4) => {
  emit('update:modelValue', val);
};
</script>

<template>
  <div class="layout-switcher flex items-center gap-2">
    <span class="text-sm text-[var(--el-text-color-secondary)]">表单布局：</span>
    <el-radio-group :model-value="modelValue" @update:model-value="handleSelect">
      <el-radio-button v-for="opt in options" :key="opt.value" :value="opt.value">
        {{ opt.label }}
      </el-radio-button>
    </el-radio-group>
  </div>
</template>
```

- [ ] **Step 2: 提交**

```bash
git add apps/mdm-flow/src/modules/model-design/components/form-design/LayoutSwitcher.vue
git commit -m "feat(form-design): add LayoutSwitcher component"
```

---

## Task 3: 实现 FieldTreePanel 字段树

**Files:**
- Create: `apps/mdm-flow/src/modules/model-design/components/form-design/FieldTreePanel.vue`

**Steps:**

- [ ] **Step 1: 创建组件**

```vue
<script setup lang="ts">
/**
 * FieldTreePanel - 字段树（展示复合模型的层级结构）
 */
import { ref, onMounted } from 'vue';
import { Search } from '@element-plus/icons-vue';

defineOptions({ name: 'FieldTreePanel' });

interface TreeNode {
  id: string;
  name: string;
  isLeaf: boolean;
  children?: TreeNode[];
}

interface Props {
  modelId: string;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  'select': [fieldId: string];
}>();

const loading = ref(false);
const treeData = ref<TreeNode[]>([]);
const searchKeyword = ref('');

// TODO: 调用 API 获取复合模型结构
const loadData = async () => {
  loading.value = true;
  try {
    // mock 数据
    treeData.value = [
      {
        id: 'main',
        name: '供应商_20583_文雪颖',
        isLeaf: false,
        children: [
          { id: 'sub1', name: '机构信息', isLeaf: true },
          { id: 'sub2', name: '附件信息', isLeaf: true },
        ],
      },
    ];
  } finally {
    loading.value = false;
  }
};

const handleNodeClick = (node: TreeNode) => {
  if (node.isLeaf) {
    emit('select', node.id);
  }
};

onMounted(() => {
  loadData();
});
</script>

<template>
  <div class="field-tree-panel flex flex-col h-full">
    <!-- 标题 -->
    <div class="p-3 border-b border-[var(--el-border-color-lighter)]">
      <span class="text-sm font-medium">字段列表</span>
    </div>

    <!-- 搜索框 -->
    <div class="p-2">
      <el-input v-model="searchKeyword" placeholder="搜索" size="small" :prefix-icon="Search" />
    </div>

    <!-- 树 -->
    <div class="flex-1 overflow-auto p-2">
      <el-tree
        :data="treeData"
        :props="{ label: 'name', children: 'children' }"
        :expand-on-click-node="false"
        default-expand-all
        @node-click="handleNodeClick"
      />
    </div>
  </div>
</template>

<style scoped>
.field-tree-panel {
  background: var(--el-fill-color-lighter);
}
</style>
```

- [ ] **Step 2: 提交**

```bash
git add apps/mdm-flow/src/modules/model-design/components/form-design/FieldTreePanel.vue
git commit -m "feat(form-design): add FieldTreePanel component"
```

---

## Task 4: 实现 FormPreviewPanel 表单预览

**Files:**
- Create: `apps/mdm-flow/src/modules/model-design/components/form-design/FormPreviewPanel.vue`

**Steps:**

- [ ] **Step 1: 创建组件**

```vue
<script setup lang="ts">
/**
 * FormPreviewPanel - 表单预览
 */
import { computed } from 'vue';
import type { FormGroup, FieldStyle } from '@/modules/model-design/types/form-design';
import GroupList from './GroupList.vue';

defineOptions({ name: 'FormPreviewPanel' });

interface Props {
  groups: FormGroup[];
  fieldStyles: Record<string, FieldStyle>;
  layoutColumns: 1 | 2 | 3 | 4;
  selectedFieldId: string | null;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  'update:groups': [val: FormGroup[]];
  'update:fieldStyles': [val: Record<string, FieldStyle>];
}>();

// 表单样式
const formClass = computed(() => {
  return {
    'grid grid-cols-1 gap-4': props.layoutColumns === 1,
    'grid grid-cols-2 gap-4': props.layoutColumns === 2,
    'grid grid-cols-3 gap-4': props.layoutColumns === 3,
    'grid grid-cols-4 gap-4': props.layoutColumns === 4,
  };
});
</script>

<template>
  <div class="form-preview-panel">
    <!-- 消息提示 -->
    <el-alert type="info" :closable="false" show-icon class="mb-4">
      <template #title>提示</template>
      <div class="text-xs">
        在主数据查看/维护页面，将按属性填报设计渲染页面<br>
        输入方式根据模型属性配置的内容不同，存在不同的可选项和默认值
      </div>
    </el-alert>

    <!-- 操作按钮 -->
    <div class="flex gap-2 mb-4">
      <el-button size="small">新建字段分组</el-button>
      <el-button size="small">导入分组及属性样式</el-button>
    </div>

    <!-- 分组列表 -->
    <GroupList
      v-model="groups"
      :selected-field-id="selectedFieldId"
      @field-click="(id) => emit('update:fieldStyles', { ...fieldStyles, [id]: fieldStyles[id] || {} })"
    />
  </div>
</template>
```

- [ ] **Step 2: 提交**

```bash
git add apps/mdm-flow/src/modules/model-design/components/form-design/FormPreviewPanel.vue
git commit -m "feat(form-design): add FormPreviewPanel component"
```

---

## Task 5: 实现 GroupList 分组管理

**Files:**
- Modify: `apps/mdm-flow/src/modules/model-design/components/form-design/GroupList.vue`

**Steps:**

- [ ] **Step 1: 阅读现有占位代码**

```bash
# 读取 apps/mdm-flow/src/modules/model-design/components/form-design/GroupList.vue
```

- [ ] **Step 2: 实现分组列表**

```vue
<script setup lang="ts">
/**
 * GroupList - 分组列表（支持新增/删除/拖拽）
 */
import { ref, computed } from 'vue';
import { Plus, Delete, Edit } from '@element-plus/icons-vue';
import type { FormGroup } from '@/modules/model-design/types/form-design';
import DraggableList from '@mdm/common/components/data/DraggableList.vue';

defineOptions({ name: 'GroupList' });

interface Props {
  modelValue: FormGroup[];
  selectedFieldId: string | null;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  'update:modelValue': [val: FormGroup[]];
  'field-click': [fieldId: string];
}>();

// 分组折叠状态
const collapsedGroups = ref<Set<string>>(new Set());

const toggleCollapse = (groupId: string) => {
  if (collapsedGroups.value.has(groupId)) {
    collapsedGroups.value.delete(groupId);
  } else {
    collapsedGroups.value.add(groupId);
  }
};

const handleReorder = (newGroups: FormGroup[]) => {
  emit('update:modelValue', newGroups);
};

const addGroup = () => {
  const newGroup: FormGroup = {
    id: `g_${Date.now()}`,
    name: '新分组',
    sortOrder: props.modelValue.length + 1,
    attributeIds: [],
    collapsible: true,
    defaultCollapsed: false,
  };
  emit('update:modelValue', [...props.modelValue, newGroup]);
};

const deleteGroup = (groupId: string) => {
  emit('update:modelValue', props.modelValue.filter(g => g.id !== groupId));
};

const updateGroupName = (groupId: string, name: string) => {
  emit('update:modelValue', props.modelValue.map(g => 
    g.id === groupId ? { ...g, name } : g
  ));
};
</script>

<template>
  <div class="group-list">
    <!-- 分组列表 -->
    <div v-for="group in modelValue" :key="group.id" class="group-item mb-4">
      <!-- 分组头 -->
      <div 
        class="group-header flex items-center justify-between p-3 bg-[var(--el-fill-color-light)] rounded-t cursor-pointer"
        @click="toggleCollapse(group.id)"
      >
        <div class="flex items-center gap-2">
          <el-icon>
            <component :is="collapsedGroups.has(group.id) ? 'Plus' : 'Minus'" />
          </el-icon>
          <span class="font-medium">{{ group.name }}</span>
        </div>
        <div class="flex gap-1" @click.stop>
          <el-button size="small" text :icon="Edit" @click="updateGroupName(group.id, group.name)" />
          <el-button size="small" text :icon="Delete" type="danger" @click="deleteGroup(group.id)" />
        </div>
      </div>

      <!-- 分组内容 -->
      <div v-if="!collapsedGroups.has(group.id)" class="group-content p-3 border border-t-0 border-[var(--el-border-color-lighter)] rounded-b">
        <div class="text-xs text-[var(--el-text-color-placeholder)]">
          点击字段进行配置
        </div>
      </div>
    </div>

    <!-- 添加分组按钮 -->
    <el-button class="w-full" :icon="Plus" @click="addGroup">
      新建分组
    </el-button>
  </div>
</template>

<style scoped>
.group-item {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  overflow: hidden;
}
</style>
```

- [ ] **Step 3: 提交**

```bash
git add apps/mdm-flow/src/modules/model-design/components/form-design/GroupList.vue
git commit -m "feat(form-design): implement GroupList component"
```

---

## Task 6: 实现 AttributeConfigPanel 属性配置面板

**Files:**
- Create: `apps/mdm-flow/src/modules/model-design/components/form-design/AttributeConfigPanel.vue`

**Steps:**

- [ ] **Step 1: 创建组件**

```vue
<script setup lang="ts">
/**
 * AttributeConfigPanel - 属性配置面板（3个Tab）
 */
import { ref, computed } from 'vue';
import type { FieldStyle, VisibilityCondition } from '@/modules/model-design/types/form-design';
import StyleConfigTab from './StyleConfigTab.vue';
import VisibilityConfigTab from './VisibilityConfigTab.vue';
import LayoutConfigTab from './LayoutConfigTab.vue';

defineOptions({ name: 'AttributeConfigPanel' });

interface Props {
  fieldStyles: Record<string, FieldStyle>;
  visibilityConditions: VisibilityCondition[];
  layoutColumns: 1 | 2 | 3 | 4;
  selectedFieldId: string | null;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  'update:fieldStyles': [val: Record<string, FieldStyle>];
  'update:visibilityConditions': [val: VisibilityCondition[]];
  'update:layoutColumns': [val: 1 | 2 | 3 | 4];
}>();

const activeTab = ref('style');

// 当前选中字段的样式
const currentFieldStyle = computed(() => {
  if (!props.selectedFieldId) return null;
  return props.fieldStyles[props.selectedFieldId] || null;
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
    </el-tabs>

    <!-- Tab 内容 -->
    <div class="flex-1 overflow-auto p-3">
      <!-- 未选中字段提示 -->
      <div v-if="!selectedFieldId" class="flex items-center justify-center h-full text-sm text-[var(--el-text-color-placeholder)]">
        请先选择字段
      </div>

      <!-- 属性样式 Tab -->
      <StyleConfigTab
        v-else-if="activeTab === 'style'"
        :field-style="currentFieldStyle"
        @change="handleStyleChange"
      />

      <!-- 显隐配置 Tab -->
      <VisibilityConfigTab
        v-else-if="activeTab === 'visibility'"
        :conditions="visibilityConditions"
        :target-field-id="selectedFieldId"
        @update:conditions="emit('update:visibilityConditions', $event)"
      />

      <!-- 布局样式 Tab -->
      <LayoutConfigTab
        v-else-if="activeTab === 'layout'"
        :layout-columns="layoutColumns"
        @update:layout-columns="emit('update:layoutColumns', $event)"
      />
    </div>

    <!-- 保存按钮 -->
    <div class="p-3 border-t border-[var(--el-border-color-lighter)]">
      <el-button type="primary" class="w-full">保存</el-button>
    </div>
  </div>
</template>

<style scoped>
.attribute-config-panel {
  background: var(--el-fill-color-lighter);
}
</style>
```

- [ ] **Step 2: 提交**

```bash
git add apps/mdm-flow/src/modules/model-design/components/form-design/AttributeConfigPanel.vue
git commit -m "feat(form-design): add AttributeConfigPanel component"
```

---

## Task 7: 实现 StyleConfigTab 属性样式Tab

**Files:**
- Create: `apps/mdm-flow/src/modules/model-design/components/form-design/StyleConfigTab.vue`

**Steps:**

- [ ] **Step 1: 创建组件**

```vue
<script setup lang="ts">
/**
 * StyleConfigTab - 属性样式配置
 */
import type { FieldStyle, ControlType } from '@/modules/model-design/types/form-design';

defineOptions({ name: 'StyleConfigTab' });

interface Props {
  fieldStyle: FieldStyle | null;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  'change': [style: Partial<FieldStyle>];
}>();

const controlTypeOptions: { value: ControlType; label: string }[] = [
  { value: 'text', label: '文本框' },
  { value: 'number', label: '数字框' },
  { value: 'date', label: '日期选择' },
  { value: 'datetime', label: '日期时间' },
  { value: 'select', label: '下拉列表' },
  { value: 'multiselect', label: '多选下拉' },
  { value: 'radio', label: '单选按钮' },
  { value: 'checkbox', label: '复选框' },
  { value: 'textarea', label: '文本域' },
  { value: 'file', label: '文件上传' },
];

const handleChange = (key: keyof FieldStyle, value: any) => {
  emit('change', { [key]: value });
};
</script>

<template>
  <div class="style-config-tab">
    <el-form label-position="top" size="small">
      <el-form-item label="控件类型">
        <el-select
          :model-value="fieldStyle?.controlType || 'text'"
          class="w-full"
          @update:model-value="handleChange('controlType', $event)"
        >
          <el-option
            v-for="opt in controlTypeOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="占位提示">
        <el-input
          :model-value="fieldStyle?.placeholder || ''"
          placeholder="请输入"
          @update:model-value="handleChange('placeholder', $event)"
        />
      </el-form-item>

      <el-form-item label="是否只读">
        <el-switch
          :model-value="fieldStyle?.readonly || false"
          @update:model-value="handleChange('readonly', $event)"
        />
      </el-form-item>

      <el-form-item label="是否必填">
        <el-switch
          :model-value="fieldStyle?.required || false"
          @update:model-value="handleChange('required', $event)"
        />
      </el-form-item>

      <el-form-item label="占位列数">
        <el-select
          :model-value="fieldStyle?.colSpan || 1"
          class="w-full"
          @update:model-value="handleChange('colSpan', $event)"
        >
          <el-option :label="1" :value="1" />
          <el-option :label="2" :value="2" />
          <el-option :label="3" :value="3" />
          <el-option :label="4" :value="4" />
        </el-select>
      </el-form-item>

      <el-form-item label="是否整行">
        <el-switch
          :model-value="fieldStyle?.fullRow || false"
          @update:model-value="handleChange('fullRow', $event)"
        />
      </el-form-item>
    </el-form>
  </div>
</template>
```

- [ ] **Step 2: 提交**

```bash
git add apps/mdm-flow/src/modules/model-design/components/form-design/StyleConfigTab.vue
git commit -m "feat(form-design): add StyleConfigTab component"
```

---

## Task 8: 实现 VisibilityConfigTab 显隐配置Tab

**Files:**
- Modify: `apps/mdm-flow/src/modules/model-design/components/form-design/VisibilityConfigPanel.vue`

**Steps:**

- [ ] **Step 1: 阅读现有占位代码**

```bash
# 读取 apps/mdm-flow/src/modules/model-design/components/form-design/VisibilityConfigPanel.vue
```

- [ ] **Step 2: 重写为 VisibilityConfigTab**

```vue
<script setup lang="ts">
/**
 * VisibilityConfigTab - 显隐配置
 */
import { ref } from 'vue';
import type { VisibilityCondition } from '@/modules/model-design/types/form-design';
import ExpressionBuilder from '@mdm/common/components/form/ExpressionBuilder.vue';
import { Delete, Edit } from '@element-plus/icons-vue';

defineOptions({ name: 'VisibilityConfigTab' });

interface Props {
  conditions: VisibilityCondition[];
  targetFieldId: string | null;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  'update:conditions': [val: VisibilityCondition[]];
}>();

const showDialog = ref(false);
const editingIndex = ref<number | null>(null);
const currentExpression = ref('');

const handleAdd = () => {
  editingIndex.value = null;
  currentExpression.value = '';
  showDialog.value = true;
};

const handleEdit = (index: number) => {
  editingIndex.value = index;
  currentExpression.value = props.conditions[index].expression || '';
  showDialog.value = true;
};

const handleDelete = (index: number) => {
  emit('update:conditions', props.conditions.filter((_, i) => i !== index));
};

const handleConfirm = (expression: string) => {
  const newCondition: VisibilityCondition = {
    id: `vc_${Date.now()}`,
    targetAttributeId: props.targetFieldId || '',
    logic: 'and',
    conditions: [],
    action: 'show',
  };
  
  if (editingIndex.value !== null) {
    // 更新
    const updated = [...props.conditions];
    updated[editingIndex.value] = { ...updated[editingIndex.value], expression };
    emit('update:conditions', updated);
  } else {
    // 新增
    emit('update:conditions', [...props.conditions, newCondition]);
  }
  showDialog.value = false;
};
</script>

<template>
  <div class="visibility-config-tab">
    <!-- 添加按钮 -->
    <el-button type="primary" size="small" class="mb-3" @click="handleAdd">
      添加显隐规则
    </el-button>

    <!-- 条件列表 -->
    <el-table :data="conditions" size="small" class="mb-3">
      <el-table-column type="index" width="50" label="序号" />
      <el-table-column prop="expression" label="条件" />
      <el-table-column prop="targetAttributeId" label="目标字段" />
      <el-table-column label="操作" width="80">
        <template #default="{ row, $index }">
          <el-button size="small" text :icon="Edit" @click="handleEdit($index)" />
          <el-button size="small" text :icon="Delete" type="danger" @click="handleDelete($index)" />
        </template>
      </el-table-column>
    </el-table>

    <!-- 空状态 -->
    <div v-if="!conditions.length" class="text-center text-sm text-[var(--el-text-color-placeholder)] py-4">
      暂无显隐规则
    </div>

    <!-- 表达式编辑器弹窗 -->
    <el-dialog v-model="showDialog" title="配置显隐条件" width="700px">
      <ExpressionBuilder v-model="currentExpression" />
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleConfirm(currentExpression)">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
```

- [ ] **Step 3: 提交**

```bash
git add apps/mdm-flow/src/modules/model-design/components/form-design/VisibilityConfigPanel.vue
git commit -m "feat(form-design): implement VisibilityConfigTab component"
```

---

## Task 9: 实现 LayoutConfigTab 布局样式Tab

**Files:**
- Create: `apps/mdm-flow/src/modules/model-design/components/form-design/LayoutConfigTab.vue`

**Steps:**

- [ ] **Step 1: 创建组件**

```vue
<script setup lang="ts">
/**
 * LayoutConfigTab - 布局样式配置
 */
defineOptions({ name: 'LayoutConfigTab' });

interface Props {
  layoutColumns: 1 | 2 | 3 | 4;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  'update:layoutColumns': [val: 1 | 2 | 3 | 4];
}>();
</script>

<template>
  <div class="layout-config-tab">
    <el-alert type="info" :closable="false" show-icon class="mb-4">
      <template #title>说明</template>
      <span class="text-xs">在主数据查看/维护页面，将按以下布局样式进行渲染</span>
    </el-alert>

    <el-form label-position="top" size="small">
      <el-form-item label="表单布局">
        <el-radio-group
          :model-value="layoutColumns"
          @update:model-value="emit('update:layoutColumns', $event)"
        >
          <el-radio :value="1">单列</el-radio>
          <el-radio :value="2">双列</el-radio>
          <el-radio :value="3">三列</el-radio>
          <el-radio :value="4">四列</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="字段布局">
        <el-radio-group :model-value="1">
          <el-radio :value="1">左右布局</el-radio>
          <el-radio :value="2">上下布局</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
  </div>
</template>
```

- [ ] **Step 2: 提交**

```bash
git add apps/mdm-flow/src/modules/model-design/components/form-design/LayoutConfigTab.vue
git commit -m "feat(form-design): add LayoutConfigTab component"
```

---

## Task 10: 添加 Mock 数据

**Files:**
- Create: `apps/mdm-flow/mock/form-design.ts`

**Steps:**

- [ ] **Step 1: 创建 Mock 数据**

```typescript
/**
 * mock/form-design.ts
 * 填报设计 Mock 数据
 */

export const mockFormDesign = {
  layoutColumns: 2,
  groups: [
    {
      id: 'g1',
      name: '基本信息',
      sortOrder: 1,
      attributeIds: ['a1', 'a2'],
      collapsible: true,
      defaultCollapsed: false,
    },
    {
      id: 'g2',
      name: '附件信息',
      sortOrder: 2,
      attributeIds: ['a3'],
      collapsible: true,
      defaultCollapsed: false,
    },
    {
      id: 'g3',
      name: '默认分组',
      sortOrder: 3,
      attributeIds: ['a4', 'a5'],
      collapsible: true,
      defaultCollapsed: true,
    },
  ],
  fieldStyles: {
    a1: { attributeId: 'a1', controlType: 'text', colSpan: 2, fullRow: false, placeholder: '请输入' },
    a2: { attributeId: 'a2', controlType: 'date', colSpan: 1, fullRow: false },
    a3: { attributeId: 'a3', controlType: 'file', colSpan: 2, fullRow: true },
  },
  visibilityConditions: [],
};
```

- [ ] **Step 2: 注册 Mock 接口**

在 `apps/mdm-flow/mock/vite-plugin-mock.ts` 中注册

- [ ] **Step 3: 提交**

```bash
git add apps/mdm-flow/mock/form-design.ts
git commit -m "feat(form-design): add mock data for form design"
```

---

## Task 11: 集成测试

**Steps:**

- [ ] **Step 1: 启动开发服务器**

```bash
pnpm dev:flow
```

- [ ] **Step 2: 访问填报设计页面**

导航到 `http://localhost:3000/model-design`

- [ ] **Step 3: 测试基本交互**

- [ ] 验证三列布局显示正确
- [ ] 测试 Tab 切换
- [ ] 测试分组新增/删除
- [ ] 测试布局列数切换
- [ ] 测试属性样式配置

- [ ] **Step 4: 提交**

```bash
git add -A
git commit -m "test(form-design): integration test passed"
```

---

## 自检清单

- [ ] 所有组件使用 `<script setup lang="ts">`
- [ ] 所有 Props/Emits 使用 TypeScript 接口
- [ ] 遵循项目编码规范（p-3 间距、Tp-* 组件）
- [ ] 提交信息符合规范（feat/fix/test）
