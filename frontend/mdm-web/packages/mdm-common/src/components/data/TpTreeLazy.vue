<script setup lang="ts">
/**
 * TpTreeLazy - 通用树组件（懒加载版）
 *
 * 通过 `load` 函数按需加载子节点，适用于大数据量场景。
 * 父节点展开时才请求后端接口拿子节点。
 *
 * 视觉规范与 Figma node-id=437-33778 对齐。
 *
 * 特性：
 *   - 顶部搜索框（label 模糊匹配，实时过滤）
 *   - 转发所有 el-tree 事件
 *   - 内置默认节点（Folder 图标 + label），父组件可通过 #node 插槽覆盖
 *
 * API 约定：load 函数接收 node + resolve 回调，返回 { id, name, isLeaf }[]
 * （name 与 label 字段名通过 props.fieldMap.label 配置，isLeaf 用于 el-tree 跳过子节点加载）
 */
import { ref } from 'vue';
import { Search, Folder } from '@element-plus/icons-vue';

export interface TpTreeNode {
  id?: string | number;
  isLeaf?: boolean;
  [key: string]: unknown;
}

export type TpTreeLoadFn = (
  node: { level: number; data: TpTreeNode },
  resolve: (data: TpTreeNode[]) => void,
) => void;

const props = withDefaults(
  defineProps<{
    /** 按需加载函数 */
    load: TpTreeLoadFn;
    /** 节点唯一标识字段名 */
    nodeKey?: string;
    /** 节点字段映射（label/isLeaf） */
    fieldMap?: { label?: string; isLeaf?: string };
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
    /** 默认展开的节点 keys */
    defaultExpandedKeys?: (string | number)[];
    /** 节点缩进（px），对应 el-tree indent prop */
    indent?: number;
  }>(),
  {
    nodeKey: 'id',
    fieldMap: () => ({ label: 'name', isLeaf: 'isLeaf' }),
    searchable: true,
    searchPlaceholder: '请输入关键字',
    filterNodeMethod: undefined,
    highlightCurrent: true,
    expandOnClickNode: false,
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
      class="dm-tree-search p-3 flex-shrink-0"
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

    <!-- 懒加载树 -->
    <div class="dm-tree-body flex-1 min-h-0 pl-3 pr-3 overflow-auto">
      <el-tree
        ref="elTreeRef"
        :load="load"
        :node-key="nodeKey"
        :props="fieldMap"
        lazy
        :indent="indent"
        :filter-node-method="filterMethod"
        :highlight-current="highlightCurrent"
        :expand-on-click-node="expandOnClickNode"
        :default-expanded-keys="defaultExpandedKeys"
        @node-click="handleNodeClick"
        @node-expand="handleNodeExpand"
        @node-collapse="handleNodeCollapse"
        @current-change="handleCurrentChange"
      >
        <template #default="{ data }">
          <slot name="node" :data="data">
            <el-icon class="dm-tree-node-icon ml-1 mr-1"><Folder /></el-icon>
            <span class="dm-tree-node-label">{{ getLabel(data) }}</span>
          </slot>
        </template>
      </el-tree>
    </div>
  </div>
</template>

<style scoped>
.dm-tree-search :deep(.el-input__inner) {
  font-size: 12px;
}

.dm-tree-search :deep(.el-input__prefix .el-icon) {
  color: #9da7b8;
  font-size: 12px;
}


.dm-tree-body :deep(.el-tree-node__expand-icon) {
  color: #bfbfbf;
  font-size: 12px;
  padding: 0;
}

.dm-tree-node-label {
  font-size: 12px;
  line-height: 22px;
  font-family: 'PingFang SC', -apple-system, sans-serif;
  color: #4f4f4f;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
