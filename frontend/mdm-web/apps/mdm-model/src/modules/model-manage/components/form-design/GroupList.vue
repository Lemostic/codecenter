<script setup lang="ts">
/**
 * GroupList - 分组列表 (§24)
 * 新增/删除分组、拖拽排序、默认组逻辑、属性显示
 */
import { ref, computed, watch } from 'vue';
import { Plus, Delete, Edit, Minus, Rank } from '@element-plus/icons-vue';
import type { FormGroup } from '../../types/form-design';

defineOptions({ name: 'GroupList' });

const DEFAULT_GROUP_ID = '__default__';

interface Props {
  modelValue?: FormGroup[];
  selectedFieldId?: string | null;
  /** 所有启用的属性(用于显示名称和默认组) */
  allAttributes?: { id: string; name: string; englishName: string; status: 'enabled' | 'disabled' }[];
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: () => [],
  selectedFieldId: null,
  allAttributes: () => [],
});

const emit = defineEmits<{
  (e: 'update:modelValue', v: FormGroup[]): void;
  (e: 'field-click', v: string): void;
}>();

// §24.2/24.3 确保默认组存在
const groupsWithDefault = computed(() => {
  const groups = [...props.modelValue];
  const hasDefault = groups.some(g => g.id === DEFAULT_GROUP_ID);

  if (!hasDefault) {
    // §24.3 默认组包含不在任何自定义分组中的属性
    const customAttrIds = new Set(groups.flatMap(g => g.attributeIds));
    const defaultAttrIds = props.allAttributes
      .filter(a => a.status === 'enabled' && !customAttrIds.has(a.id))
      .map(a => a.id);

    if (defaultAttrIds.length > 0 || groups.length === 0) {
      groups.push({
        id: DEFAULT_GROUP_ID,
        name: '默认组',
        parentId: null,
        sortOrder: groups.length + 1,
        attributeIds: defaultAttrIds,
        collapsible: true,
        defaultCollapsed: false,
      });
    }
  } else {
    // 更新默认组的属性列表(不在任何自定义分组中的属性)
    const customAttrIds = new Set(
      groups.filter(g => g.id !== DEFAULT_GROUP_ID).flatMap(g => g.attributeIds),
    );
    const defaultAttrIds = props.allAttributes
      .filter(a => a.status === 'enabled' && !customAttrIds.has(a.id))
      .map(a => a.id);

    const idx = groups.findIndex(g => g.id === DEFAULT_GROUP_ID);
    if (idx >= 0) {
      groups[idx] = { ...groups[idx], attributeIds: defaultAttrIds };
    }
  }

  // §24.3 默认组排最后
  return groups.sort((a, b) => {
    if (a.id === DEFAULT_GROUP_ID) return 1;
    if (b.id === DEFAULT_GROUP_ID) return -1;
    return a.sortOrder - b.sortOrder;
  });
});

// ========== 折叠状态 ==========
const collapsedGroups = ref<Set<string>>(new Set());

const toggleCollapse = (groupId: string) => {
  if (collapsedGroups.value.has(groupId)) {
    collapsedGroups.value.delete(groupId);
  } else {
    collapsedGroups.value.add(groupId);
  }
};

const isCollapsed = (groupId: string) => collapsedGroups.value.has(groupId);

// ========== §24.5 拖拽排序 ==========
const draggingIndex = ref<number | null>(null);

const handleDragStart = (index: number) => {
  draggingIndex.value = index;
};

const handleDragOver = (event: DragEvent) => {
  event.preventDefault();
};

const handleDrop = (event: DragEvent, targetIndex: number) => {
  event.preventDefault();
  if (draggingIndex.value === null || draggingIndex.value === targetIndex) return;

  const list = groupsWithDefault.value;
  const newList = [...list];
  const [dragged] = newList.splice(draggingIndex.value, 1);

  // 不允许拖拽默认组
  if (dragged.id === DEFAULT_GROUP_ID) {
    draggingIndex.value = null;
    return;
  }

  newList.splice(targetIndex, 0, dragged);
  const reordered = newList
    .filter(g => g.id !== DEFAULT_GROUP_ID)
    .map((g, i) => ({ ...g, sortOrder: i + 1 }));

  draggingIndex.value = null;
  emit('update:modelValue', reordered);
};

const handleDragEnd = () => {
  draggingIndex.value = null;
};

// ========== 分组操作 ==========
// §24.1 新增分组
const addGroup = () => {
  const customGroups = props.modelValue.filter(g => g.id !== DEFAULT_GROUP_ID);
  const newGroup: FormGroup = {
    id: `g_${Date.now()}`,
    name: '新分组',
    parentId: null,
    sortOrder: customGroups.length + 1,
    attributeIds: [],
    collapsible: true,
    defaultCollapsed: false,
  };
  emit('update:modelValue', [...props.modelValue, newGroup]);
};

// §24.7 删除分组：属性自动回归默认分组
const deleteGroup = (groupId: string) => {
  if (groupId === DEFAULT_GROUP_ID) return; // 默认组不可删除
  const filtered = props.modelValue.filter(g => g.id !== groupId);
  emit('update:modelValue', filtered);
};

// 编辑分组名称
const editingGroupId = ref<string | null>(null);
const editingName = ref('');

const startEdit = (group: FormGroup) => {
  if (group.id === DEFAULT_GROUP_ID) return; // 默认组不可编辑
  editingGroupId.value = group.id;
  editingName.value = group.name;
};

const confirmEdit = () => {
  if (editingGroupId.value && editingName.value.trim()) {
    emit(
      'update:modelValue',
      props.modelValue.map(g =>
        g.id === editingGroupId.value ? { ...g, name: editingName.value.trim() } : g,
      ),
    );
  }
  editingGroupId.value = null;
  editingName.value = '';
};

const cancelEdit = () => {
  editingGroupId.value = null;
  editingName.value = '';
};

// 点击字段
const handleFieldClick = (attributeId: string) => {
  emit('field-click', attributeId);
};

// 获取属性名称
const getAttrName = (attrId: string) => {
  const attr = props.allAttributes.find(a => a.id === attrId);
  return attr?.name || attrId;
};

// §24.4 获取属性状态
const getAttrStatus = (attrId: string) => {
  return props.allAttributes.find(a => a.id === attrId)?.status;
};

// §24.2 总数
const totalAttrCount = computed(() => props.allAttributes.filter(a => a.status === 'enabled').length);
</script>

<template>
  <div class="group-list">
    <div
      v-for="(group, index) in groupsWithDefault"
      :key="group.id"
      class="group-item mb-3"
      :class="{
        'is-dragging': draggingIndex === index,
        'is-default': group.id === DEFAULT_GROUP_ID,
      }"
      :draggable="group.id !== DEFAULT_GROUP_ID"
      @dragstart="handleDragStart(index)"
      @dragover="handleDragOver($event)"
      @drop="handleDrop($event, index)"
      @dragend="handleDragEnd"
    >
      <!-- §24.2 分组头 -->
      <div
        class="group-header flex items-center justify-between p-3 rounded-t cursor-pointer select-none"
        :class="isCollapsed(group.id) ? 'rounded-b' : ''"
        @click="toggleCollapse(group.id)"
      >
        <div class="flex items-center gap-2 min-w-0">
          <!-- 拖拽手柄(默认组不可拖拽) -->
          <el-icon
            v-if="group.id !== DEFAULT_GROUP_ID"
            class="drag-handle text-[var(--el-text-color-placeholder)] flex-shrink-0 cursor-grab"
          >
            <Rank />
          </el-icon>

          <!-- 折叠图标 -->
          <el-icon class="flex-shrink-0">
            <component :is="isCollapsed(group.id) ? Plus : Minus" />
          </el-icon>

          <!-- 分组名称 -->
          <template v-if="editingGroupId === group.id">
            <el-input
              v-model="editingName"
              size="small"
              class="flex-1 min-w-0"
              @click.stop
              @keyup.enter="confirmEdit"
              @keyup.escape="cancelEdit"
              @blur="confirmEdit"
            />
          </template>
          <template v-else>
            <span class="font-medium truncate">{{ group.name }}</span>
            <!-- §24.2 当前分组属性数/模型属性总数 -->
            <span class="text-xs text-[var(--el-text-color-placeholder)] flex-shrink-0">
              ({{ group.attributeIds.length }}/{{ totalAttrCount }})
            </span>
          </template>
        </div>

        <!-- 操作按钮(默认组无操作) -->
        <div v-if="group.id !== DEFAULT_GROUP_ID" class="flex gap-1 flex-shrink-0" @click.stop>
          <template v-if="editingGroupId === group.id">
            <el-button size="small" text @click="confirmEdit">确认</el-button>
            <el-button size="small" text @click="cancelEdit">取消</el-button>
          </template>
          <template v-else>
            <el-button size="small" text :icon="Edit" @click="startEdit(group)" />
            <el-button size="small" text :icon="Delete" type="danger" @click="deleteGroup(group.id)" />
          </template>
        </div>
      </div>

      <!-- 分组内容 -->
      <div
        v-if="!isCollapsed(group.id)"
        class="group-content p-3 border border-t-0 rounded-b"
      >
        <div v-if="group.attributeIds.length === 0" class="text-xs text-[var(--el-text-color-placeholder)]">
          {{ group.id === DEFAULT_GROUP_ID ? '所有属性已分配到自定义分组' : '暂未包含字段，请从左侧添加' }}
        </div>
        <div v-else class="space-y-1">
          <div
            v-for="attrId in group.attributeIds"
            :key="attrId"
            class="field-item px-2 py-1 text-sm rounded cursor-pointer hover:bg-[var(--el-fill-color-light)]"
            :class="{ 'bg-[var(--el-color-primary-light-8)]': selectedFieldId === attrId }"
            @click="handleFieldClick(attrId)"
          >
            <span>{{ getAttrName(attrId) }}</span>
            <!-- §24.4 已停用属性标识 -->
            <span v-if="getAttrStatus(attrId) === 'disabled'" class="text-red-500 text-xs ml-1">(已停用)</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 添加分组按钮 -->
    <el-button class="w-full mt-2" :icon="Plus" @click="addGroup">
      新建分组
    </el-button>
  </div>
</template>

<style scoped>
.group-list {
  width: 100%;
}

.group-item {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  overflow: hidden;
  transition: opacity 0.15s, background-color 0.15s;
}

.group-item.is-dragging {
  opacity: 0.5;
  background-color: var(--el-fill-color);
}

.group-item.is-default .group-header {
  background-color: var(--el-fill-color);
}

.group-header {
  background-color: var(--el-fill-color-light);
}

.drag-handle:hover {
  color: var(--el-text-color-regular);
}

.group-content {
  border-color: var(--el-border-color-lighter);
}

.field-item {
  transition: background-color 0.15s;
}
</style>
