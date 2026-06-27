<script setup lang="ts">
/**
 * TopicList - 主题域管理
 *
 * 左树右表结构：
 *  - 左侧：主题域懒加载树（顶部「新增根主题」按钮 + 级联复选框）
 *  - 右侧：当前父级下的子主题列表（支持行内新增/编辑/删除）
 *
 * 点击树节点 → 右侧列表过滤为该节点的子主题；
 * 未选中任何节点时默认展示根主题（parentId = null）。
 */
import { ref, reactive, onMounted, computed, nextTick } from 'vue';
import { Plus, Search } from '@element-plus/icons-vue';
import TpLeftTreeLayout from '@mdm/common/components/layout/TpLeftTreeLayout.vue';
import TpTreeLazy, { type TpTreeLoadFn, type TpTreeNode } from '@mdm/common/components/data/TpTreeLazy.vue';
import TpTable, { type TpTableColumn } from '@mdm/common/components/data/TpTable.vue';
import TopicFormDialog from '@/modules/model-design/components/TopicFormDialog.vue';
import { TpConfirm } from '@mdm/common/components/feedback/TpConfirm';
import { TpMessage } from '@mdm/common/components/feedback/TpMessage';
import {
  listTopic, deleteTopic, getTopicRootTree, getTopicChildren,
} from '@/modules/model-design/api/topic';
import type { TopicVO, TopicQuery, TopicTreeNode } from '@/modules/model-design/types/topic';
import type { ID } from '@mdm/common/types/base';
import { useTopicInlineEdit } from '@/modules/model-design/composables/useTopicInlineEdit';

defineOptions({ name: 'TopicList' });

const { t } = useI18n();

// ========== 行内编辑（loadData 定义后初始化）==========

// ========== 列配置 ==========
const columns: TpTableColumn[] = [
  { prop: 'name', label: t('modelDesign.topic.list.col.name'), minWidth: 180, showOverflowTooltip: true },
  { prop: 'sortOrder', label: t('modelDesign.topic.list.col.sortOrder'), minWidth: 100 },
  { prop: 'level', label: t('modelDesign.topic.list.col.level'), minWidth: 80 },
  { prop: 'isLeaf', label: t('modelDesign.topic.list.col.isLeaf'), minWidth: 100 },
  { prop: 'hasModel', label: t('modelDesign.topic.list.col.hasModel'), minWidth: 100 },
  { prop: 'description', label: t('modelDesign.topic.list.col.description'), minWidth: 200 },
  { prop: 'createdBy', label: t('modelDesign.topic.list.col.creator'), minWidth: 120 },
  { prop: 'createdAt', label: t('modelDesign.topic.list.col.createdAt'), minWidth: 160 },
  { prop: 'actions', label: t('modelDesign.topic.list.col.actions'), minWidth: 220, fixed: 'right' },
];

// ========== 状态 ==========
const loading = ref(false);
const tableData = ref<TopicVO[]>([]);
const total = ref(0);
const query = reactive<{ page: number; pageSize: number; keyword: string }>({
  page: 1,
  pageSize: 20,
  keyword: '',
});
const searchKeyword = ref('');

const isLeftCollapsed = ref(false);

/** 当前选中的父级节点（null 表示根主题） */
const selectedNode = ref<TopicTreeNode | null>(null);

/** 级联数据复选框 */
const cascade = ref(false);

/** 编辑行原始数据（用于取消时恢复） */
const originalRowData = ref<TopicVO | null>(null);

/** 弹窗状态（仅用于根主题新增） */
const editorOpen = ref(false);
const editorMode = ref<'create' | 'edit'>('create');
const editorTopicId = ref<ID>('');
const editorParentId = ref<ID | null>(null);

/** 表格选中行（用于批量删除） */
const selectedIds = ref<Set<string>>(new Set());

/** 用于强制刷新左侧树 */
const treeKey = ref(0);

// ========== 计算属性 ==========
const selectedNodeHasModel = computed(() => selectedNode.value?.hasModel ?? false);

const isInlineEditing = (row: TopicVO) =>
  row.id === '' || editingRow.value?.id === row.id;

// ========== 数据加载 ==========
const loadData = async () => {
  loading.value = true;
  try {
    const params: TopicQuery = {
      page: query.page,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      parentId: selectedNode.value ? selectedNode.value.id : null,
    };
    const res = await listTopic(params);
    const page = res.data;
    tableData.value = page?.data?.rows ?? [];
    total.value = page?.data?.total ?? 0;
  } catch (error) {
    tableData.value = [];
    total.value = 0;
    TpMessage.error(t('modelDesign.topic.message.loadFailed'));
    console.error('[loadTopics]', error);
  } finally {
    loading.value = false;
  }
};

// ========== 行内编辑 ==========
const { editingRow, newRowData, startNew, startEdit, cancel, saveNew, saveEdit } =
  useTopicInlineEdit({
    getMaxSortOrder: () => tableData.value.length > 0 ? Math.max(...tableData.value.map(r => r.sortOrder)) : 0,
    onRefresh: loadData,
  });

// ========== 搜索 / 重置 ==========
const handleSearch = () => {
  query.keyword = searchKeyword.value.trim();
  query.page = 1;
  loadData();
};

const handleReset = () => {
  searchKeyword.value = '';
  query.keyword = '';
  query.page = 1;
  loadData();
};

// ========== 树懒加载 ==========
const fetchTopicChildren = async (parentId?: string): Promise<TpTreeNode[]> => {
  const res = parentId === undefined
    ? await getTopicRootTree()
    : await getTopicChildren(parentId);
  return (res.data?.data ?? []) as unknown as TpTreeNode[];
};

const loadTreeNode: TpTreeLoadFn = (node, resolve) => {
  const parentId = node.level === 0 ? undefined : String(node.data.id);
  fetchTopicChildren(parentId)
    .then(resolve)
    .catch((error) => {
      console.error('[loadTreeNode]', error);
      resolve([]);
    });
};

/** 点击树节点 → 过滤右侧列表 */
const handleNodeClick = (node: TpTreeNode) => {
  if (node.id == null) return;
  selectedNode.value = {
    id: String(node.id) as ID,
    name: String(node.label ?? node.name ?? ''),
    isLeaf: Boolean(node.isLeaf),
    parentId: null,
    hasModel: false, // 树节点默认无 hasModel，按需补充
  };
  query.page = 1;
  loadData();
};

const currentLabel = computed(() => {
  if (!selectedNode.value) {
    return t('modelDesign.topic.list.currentLabel.root');
  }
  return t('modelDesign.topic.list.currentLabel.child', { name: selectedNode.value.name });
});

const hasSelection = computed(() => selectedIds.value.size > 0);

const handleSelectChange = (rows: TopicVO[]) => {
  selectedIds.value = new Set(rows.map((r) => String(r.id)));
};

// ========== 弹窗操作（仅用于根主题新增） ==========
const openEditor = (mode: 'create' | 'edit', topicId: ID = '', parentId: ID | null = null) => {
  editorMode.value = mode;
  editorTopicId.value = topicId;
  editorParentId.value = parentId;
  editorOpen.value = true;
};

const handleNewRoot = () => openEditor('create', '', null);

const handleEditorSuccess = () => {
  loadData();
  treeKey.value++;
};

// ========== 行内新增 ==========
const handleAdd = () => {
  if (selectedNode.value?.hasModel) {
    TpMessage.warning(t('modelDesign.topic.message.cannotAddChildWithModel'));
    return;
  }
  const defaultData = startNew(selectedNode.value?.id ?? null);
  tableData.value = [defaultData as TopicVO, ...tableData.value];
  nextTick(() => {
    const firstInput = document.querySelector('.inline-edit-name-input') as HTMLInputElement;
    firstInput?.focus();
  });
};

const handleAddChild = (row: TopicVO) => {
  if (row.hasModel) {
    TpMessage.warning(t('modelDesign.topic.message.cannotAddChildWithModel'));
    return;
  }
  const defaultData = startNew(row.id);
  tableData.value = [defaultData as TopicVO, ...tableData.value];
  nextTick(() => {
    const firstInput = document.querySelector('.inline-edit-name-input') as HTMLInputElement;
    firstInput?.focus();
  });
};

// ========== 行内编辑 ==========
const handleRowEdit = (row: TopicVO) => {
  originalRowData.value = { ...row };
  startEdit(row);
};

const handleSave = async (row: TopicVO) => {
  let ok = false;
  if (row.isNew) {
    ok = await saveNew(row);
  } else {
    ok = await saveEdit(row);
  }
  if (ok) {
    tableData.value = tableData.value.filter(r => !(r as TopicVO & { isEditing?: boolean }).isEditing);
    originalRowData.value = null;
  }
};

const handleCancel = (row: TopicVO) => {
  if (row.isNew) {
    tableData.value = tableData.value.filter(r => !(r as TopicVO & { isNew?: boolean }).isNew);
  } else {
    const idx = tableData.value.findIndex(r => r.id === row.id);
    if (idx !== -1 && originalRowData.value) {
      tableData.value[idx] = { ...originalRowData.value };
    }
    originalRowData.value = null;
  }
  cancel();
};

// ========== 行内删除 ==========
const handleRowDelete = async (row: TopicVO) => {
  if (row.hasModel) {
    TpMessage.error(t('modelDesign.topic.message.hasModelCannotDelete'));
    return;
  }
  try {
    await TpConfirm.delete(
      t('modelDesign.topic.message.deleteConfirm', { name: row.name }),
    );
  } catch {
    return;
  }
  try {
    await deleteTopic([row.id]);
    TpMessage.success(t('modelDesign.topic.message.deleteSuccess'));
    loadData();
    treeKey.value++;
  } catch (error) {
    console.error('[handleRowDelete]', error);
  }
};

// ========== 批量删除 ==========
const handleBatchDelete = async () => {
  const ids = Array.from(selectedIds.value);
  if (ids.length === 0) {
    TpMessage.warning(t('modelDesign.topic.message.selectAtLeastOne'));
    return;
  }
  // 前端预判：所选行中是否有 hasModel=true 的
  const hasModelRows = tableData.value.filter(r => selectedIds.value.has(r.id) && r.hasModel);
  if (hasModelRows.length > 0) {
    TpMessage.error(t('modelDesign.topic.message.hasModelCannotDelete'));
    return;
  }
  try {
    await TpConfirm.confirm({
      message: t('modelDesign.topic.message.batchDeleteConfirm', { count: ids.length }),
      type: 'warning',
    });
  } catch {
    return;
  }
  try {
    await deleteTopic(ids);
    TpMessage.success(t('modelDesign.topic.message.deleteSuccess'));
    selectedIds.value = new Set();
    loadData();
    treeKey.value++;
  } catch (error) {
    console.error('[handleBatchDelete]', error);
  }
};

const handleImport = () => TpMessage.info('导入功能开发中');
const handleExport = () => TpMessage.info('导出功能开发中');

// ========== 生命周期 ==========
onMounted(() => {
  loadData();
});
</script>

<template>
  <div class="flex flex-col h-full">
    <TpLeftTreeLayout
      v-model:collapsed="isLeftCollapsed"
      class="flex-1 min-h-0"
      :default-width="280"
      :min-width="220"
      storage-key="model-design-topic-tree-width"
    >
      <!-- 左侧：主题域树 -->
      <template #left>
        <div class="flex flex-col h-full">
          <div class="p-2 flex-shrink-0 border-b border-[#e9e9e9]">
            <el-button
              size="default"
              type="primary"
              :icon="Plus"
              class="w-full"
              @click="handleNewRoot"
            >
              {{ t('modelDesign.topic.list.toolbar.newRoot') }}
            </el-button>
            <el-checkbox v-model="cascade" class="ml-2 mb-1">
              {{ t('modelDesign.topic.list.showCascade') }}
            </el-checkbox>
          </div>
          <div class="flex-1 min-h-0">
            <TpTreeLazy
              :key="treeKey"
              :load="loadTreeNode"
              search-placeholder="搜索主题域"
              :expand-on-click-node="false"
              @node-click="handleNodeClick"
            />
          </div>
        </div>
      </template>

      <!-- 右侧：主题列表 -->
      <template #right>
        <div class="h-full flex flex-col">
          <!-- 工具栏 -->
          <div class="flex items-center justify-between p-3 flex-shrink-0 border-b border-[#e9e9e9]">
            <div class="flex items-center gap-2">
              <el-button
                size="default"
                type="primary"
                :icon="Plus"
                :disabled="!selectedNode"
                @click="handleAdd"
              >
                {{ t('modelDesign.topic.list.toolbar.newChild') }}
              </el-button>
              <el-button size="default" :disabled="!hasSelection" @click="handleBatchDelete">
                {{ t('modelDesign.topic.list.toolbar.delete') }}
              </el-button>
              <el-button size="default" @click="handleImport">
                {{ t('modelDesign.topic.list.toolbar.import') }}
              </el-button>
              <el-button size="default" @click="handleExport">
                {{ t('modelDesign.topic.list.toolbar.export') }}
              </el-button>
            </div>
            <div class="flex items-center gap-2">
              <el-input
                v-model="searchKeyword"
                :placeholder="t('modelDesign.topic.list.search.keywordPlaceholder')"
                clearable
                size="default"
                style="width: 240px"
                @keyup.enter="handleSearch"
                @clear="handleReset"
              >
                <template #suffix>
                  <el-icon class="cursor-pointer text-[#999] hover:text-[#333]" @click="handleSearch">
                    <Search />
                  </el-icon>
                </template>
              </el-input>
              <el-button size="default" type="primary" @click="handleSearch">
                {{ t('modelDesign.topic.list.btn.search') }}
              </el-button>
              <el-button size="default" @click="handleReset">
                {{ t('modelDesign.topic.list.btn.reset') }}
              </el-button>
            </div>
          </div>

          <!-- 表格 + 分页 -->
          <div class="flex-1 min-h-0 flex flex-col pt-3 px-3 pb-0">
            <div class="text-xs text-[#999] flex-shrink-0 mb-2">
              {{ currentLabel }}
            </div>
            <TpTable
              class="flex-1 min-h-0"
              :columns="columns"
              :data="tableData"
              :loading="loading"
              :total="total"
              v-model:current-page="query.page"
              v-model:page-size="query.pageSize"
              @page-change="loadData"
              @size-change="loadData"
              @select-change="handleSelectChange"
            >
              <template #col-name="{ row }">
                <template v-if="isInlineEditing(row as TopicVO)">
                  <el-input
                    v-model="(row as TopicVO).name"
                    class="inline-edit-name-input"
                    size="default"
                    @keyup.enter="handleSave(row as TopicVO)"
                  />
                </template>
                <template v-else>
                  {{ (row as TopicVO).name }}
                </template>
              </template>
              <template #col-sortOrder="{ row }">
                <template v-if="isInlineEditing(row as TopicVO)">
                  <el-input-number
                    v-model="(row as TopicVO).sortOrder"
                    size="default"
                    :min="1"
                    :max="9999"
                    controls-position="right"
                    style="width: 100px"
                  />
                </template>
                <template v-else>
                  {{ (row as TopicVO).sortOrder }}
                </template>
              </template>
              <template #col-description="{ row }">
                <template v-if="isInlineEditing(row as TopicVO)">
                  <el-input
                    v-model="(row as TopicVO).description"
                    size="default"
                    maxlength="200"
                    show-word-limit
                  />
                </template>
                <template v-else>
                  {{ (row as TopicVO).description }}
                </template>
              </template>
              <template #col-isLeaf="{ row }">
                <span>{{ (row as TopicVO).isLeaf ? '是' : '否' }}</span>
              </template>
              <template #col-hasModel="{ row }">
                <el-tag
                  size="default"
                  :type="(row as TopicVO).hasModel ? 'success' : 'info'"
                  effect="plain"
                >
                  {{ (row as TopicVO).hasModel ? '是' : '否' }}
                </el-tag>
              </template>
              <template #col-actions="{ row }">
                <template v-if="isInlineEditing(row as TopicVO)">
                  <el-button size="default" type="primary" @click="handleSave(row as TopicVO)">
                    {{ t('common.confirm') }}
                  </el-button>
                  <el-button size="default" @click="handleCancel(row as TopicVO)">
                    {{ t('common.cancel') }}
                  </el-button>
                </template>
                <template v-else>
                  <el-button size="default" link type="primary" @click="handleRowEdit(row as TopicVO)">
                    {{ t('modelDesign.topic.list.col.edit') }}
                  </el-button>
                  <el-button size="default" link type="primary" @click="handleAddChild(row as TopicVO)">
                    {{ t('modelDesign.topic.list.col.addChild') }}
                  </el-button>
                  <el-button size="default" link type="danger" @click="handleRowDelete(row as TopicVO)">
                    {{ t('modelDesign.topic.list.col.delete') }}
                  </el-button>
                </template>
              </template>
            </TpTable>
          </div>
        </div>
      </template>
    </TpLeftTreeLayout>

    <!-- 新增根主题弹窗 -->
    <TopicFormDialog
      v-model="editorOpen"
      :mode="editorMode"
      :topic-id="editorTopicId"
      :default-parent-id="editorParentId"
      @success="handleEditorSuccess"
    />
  </div>
</template>
