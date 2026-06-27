<script setup lang="ts">
/**
 * TpTree - 通用树组件（全量数据版）
 *
 * 一次性传入完整树数据，适用于数据量不大（< 1000 节点）的场景。
 * 数据量大的场景请用 TpTreeLazy（懒加载版）。
 *
 * 视觉规范与 Figma node-id=437-33778 对齐。
 *
 * 特性：
 *   - 顶部搜索框（label 模糊匹配，实时过滤）
 *   - 转发所有 el-tree 事件
 *   - 内置默认节点（Folder 图标 + label），父组件可通过 #node 插槽覆盖
 */
import { ref } from 'vue';
import { Search, Folder } from '@element-plus/icons-vue';

export interface TpTreeNode {
  id?: string | number;
  [key: string]: unknown;
}

const props = withDefaults(
  defineProps<{
    /** 完整树数据 */
    data: TpTreeNode[];
    /** 节点唯一标识字段名 */
    nodeKey?: string;
    /** 节点字段映射（label/children/isLeaf） */
    fieldMap?: { label?: string; children?: string; isLeaf?: string };
    /** 是否显示搜索框 */
    searchable?: boolean;
    /** 搜索框 placeholder */
    searchPlaceholder?: string;
    /** 自定义搜索匹配方法（默认按 label 模糊匹配） */
    filterNodeMethod?: (query: string, node: TpTreeNode) => boolean;
    /** 是否高亮当前节点 */
    highlightCurrent?: boolean;
    /** 点击节点时是否展开 */
    expandOnClickNode?: boolean;
    /** 默认展开全部 */
    defaultExpandAll?: boolean;
    /** 默认展开的节点 keys */
    defaultExpandedKeys?: (string | number)[];
    /** 节点缩进（px），对应 el-tree indent prop */
    indent?: number;
  }>(),
  {
    nodeKey: 'id',
    fieldMap: () => ({ label: 'name', children: 'children', isLeaf: 'isLeaf' }),
    searchable: true,
    searchPlaceholder: '请输入关键字',
    filterNodeMethod: undefined,
    highlightCurrent: true,
    expandOnClickNode: false,
    defaultExpandAll: false,
    defaultExpandedKeys: () => [],
    indent: 24,
  },
);

const emit = defineEmits<{
  (e: 'node-click', node: TpTreeNode): void;
  (e: 'node-expand', node: TpTreeNode): void;
  (e: 'node-collapse', node: TpTreeNode): void;
  (e: 'current-change', node: TpTreeNode | null): void;
}>();

// ========== 搜索 ==========
const searchQuery = ref('');
const elTreeRef = ref<{ filter: (q: string) => void } | null>(null);

const defaultFilter = (query: string, node: TpTreeNode): boolean => {
  if (!query) return true;
  const labelKey = props.fieldMap.label ?? 'name';
  const label = String(node[labelKey] ?? '');
  return label.toLowerCase().includes(query.toLowerCase());
};

const filterMethod = props.filterNodeMethod ?? defaultFilter;

const handleSearchInput = () => {
  elTreeRef.value?.filter(searchQuery.value);
};

// ========== 默认节点渲染 ==========
const getLabel = (data: TpTreeNode): string => {
  const labelKey = props.fieldMap.label ?? 'name';
  return String(data[labelKey] ?? '');
};

// ========== 事件转发 ==========
const handleNodeClick = (node: TpTreeNode) => emit('node-click', node);
const handleNodeExpand = (node: TpTreeNode) => emit('node-expand', node);
const handleNodeCollapse = (node: TpTreeNode) => emit('node-collapse', node);
const handleCurrentChange = (node: TpTreeNode | null) => emit('current-change', node);

defineExpose({
  /** 设置搜索词（外部触发） */
  setSearch: (q: string) => {
    searchQuery.value = q;
    handleSearchInput();
  },
  /** 清空搜索 */
  clearSearch: () => {
    searchQuery.value = '';
    handleSearchInput();
  },
});
</script>

<template>
  <div class="dm-tree flex flex-col h-full bg-white">
    <!-- 搜索框 -->
    <div
      v-if="searchable"
      class="dm-tree-search px-3 py-2 flex-shrink-0"
    >
      <el-input
        v-model="searchQuery"
        :placeholder="searchPlaceholder"
        clearable
        size="default"
        @clear="handleSearchInput"
        @input="handleSearchInput"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <!-- 树 -->
    <div class="dm-tree-body flex-1 min-h-0 overflow-auto">
      <el-tree
        ref="elTreeRef"
        :data="data"
        :node-key="nodeKey"
        :props="fieldMap"
        :indent="indent"
        :filter-node-method="filterMethod"
        :highlight-current="highlightCurrent"
        :expand-on-click-node="expandOnClickNode"
        :default-expand-all="defaultExpandAll"
        :default-expanded-keys="defaultExpandedKeys"
        @node-click="handleNodeClick"
        @node-expand="handleNodeExpand"
        @node-collapse="handleNodeCollapse"
        @current-change="handleCurrentChange"
      >
        <template #default="{ data }">
          <slot name="node" :data="data">
            <span class="dm-tree-node-label">{{ getLabel(data) }}</span>
          </slot>
        </template>
      </el-tree>
    </div>
  </div>
</template>

<style scoped>

</style>
