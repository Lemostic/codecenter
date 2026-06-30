<script setup lang="ts">
/**
 * ModelList - 主数据建模主页面
 *
 * 左侧主题域分类树 + 右侧模型卡片/列表双视图。
 * 工具栏分两行：操作按钮行 + 筛选搜索行。
 */
import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import type { AxiosResponse } from 'axios';
import { Delete, Search, Grid, List, ArrowDown } from '@element-plus/icons-vue';
import TpLeftTreeLayout from '@mdm/common/components/layout/TpLeftTreeLayout.vue';
import TpPageFrame from '@mdm/common/components/layout/TpPageFrame.vue';
import TpTreeLazy from '@mdm/common/components/data/TpTreeLazy.vue';
import type { TpTreeLoadFn, TpTreeNode } from '@mdm/common/components/data/TpTreeLazy.vue';
import TpTable, { type TpTableColumn } from '@mdm/common/components/data/TpTable.vue';
import TpCardList from '@mdm/common/components/data/TpCardList.vue';
import ModelCard from '@/modules/model-index/components/ModelCard.vue';
import {
  MODEL_STATUS_OPTIONS, MODEL_TYPE_OPTIONS,
} from '@/modules/model-index/types/model';
import type { ModelVO, ModelStatus, ModelType } from '@/modules/model-index/types/model';
import { listModel, deleteModel } from '@/modules/model-index/api/model';
import { getTopicRootTree, getTopicChildren } from '@/modules/model-index/api/topic';
import { TpConfirm } from '@mdm/common/components/feedback/TpConfirm';
import { TpMessage } from '@mdm/common/components/feedback/TpMessage';
import type { ApiResponse, PaginatedResponse } from '@mdm/common/types/api';
import type { ID } from '@mdm/common/types/base';
import type { TopicTreeNode } from '@/modules/model-index/types/topic';

defineOptions({ name: 'ModelList' });

const { t } = useI18n();
const router = useRouter();

// ========== 响应式状态 ==========

/** 视图模式：卡片 / 列表 */
const viewMode = ref<'card' | 'list'>('card');

/** 列表视图列配置 */
const listColumns: TpTableColumn[] = [
  { prop: 'name', label: t('modelDesign.list.col.name'), minWidth: 160, showOverflowTooltip: true },
  { prop: 'code', label: t('modelDesign.list.col.code'), minWidth: 140, showOverflowTooltip: true },
  { prop: 'tableName', label: t('modelDesign.list.col.tableName'), minWidth: 160, showOverflowTooltip: true },
  { prop: 'modelTypeLabel', label: t('modelDesign.list.col.modelType'), minWidth: 100 },
  { prop: 'versionLabel', label: t('modelDesign.list.col.version'), minWidth: 80 },
  { prop: 'status', label: t('modelDesign.list.col.status'), minWidth: 100 },
];

/** 筛选条件 */
const filters = reactive({
  keyword: '',
  status: '' as ModelStatus | '',
  modelType: '' as ModelType | '',
});

/** 当前选中的主题域节点 */
const selectedTopicId = ref<ID | null>(null);
const selectedTopicIsLeaf = ref(true);
const cascade = ref(true);

/** 模型列表数据 */
const loading = ref(false);
const models = ref<ModelVO[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(20);

/** 选中的模型 ID 集合 */
const selectedIds = ref<Set<ID>>(new Set());

/** 左侧树折叠状态 */
const leftCollapsed = ref(false);

// ========== 计算属性 ==========

/** 是否可以新建/复制模型（只有末级节点可以） */
const canCreate = computed(() => selectedTopicIsLeaf.value);

/** 是否有选中的模型 */
const hasSelection = computed(() => selectedIds.value.size > 0);

// ========== 方法 ==========

/** 加载模型列表 */
const loadModels = async () => {
  loading.value = true;
  try {
    // http.get 实际返回 AxiosResponse，TS 类型在 API 层声明不全，这里补一次断言
    const res = (await listModel({
      page: currentPage.value,
      pageSize: pageSize.value,
      keyword: filters.keyword || undefined,
      status: (filters.status as ModelStatus) || undefined,
      modelType: (filters.modelType as ModelType) || undefined,
      topicId: selectedTopicId.value || undefined,
      cascade: cascade.value,
      sortBy: sortBy.value || undefined,
      sortOrder: sortOrder.value,
    })) as unknown as AxiosResponse<PaginatedResponse<ModelVO>>;
    const page = res.data;
    if (page?.success && page.data) {
      models.value = [...page.data.rows];
      total.value = page.data.total;
    }
  } catch (error) {
    models.value = [];
    total.value = 0;
    TpMessage.error(t('modelDesign.message.loadFailed'));
    console.error('[loadModels]', error);
  } finally {
    loading.value = false;
  }
};

// ========== 排序状态 ==========

/** 排序字段 */
const sortBy = ref<'name' | 'createdAt' | null>(null);
/** 排序方向 */
const sortOrder = ref<'asc' | 'desc'>('desc');

/** 切换排序 */
const handleSort = (field: 'name' | 'createdAt') => {
  if (sortBy.value === field) {
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc';
  } else {
    sortBy.value = field;
    sortOrder.value = 'desc';
  }
  currentPage.value = 1;
  loadModels();
};

/** 搜索 */
const handleSearch = () => {
  currentPage.value = 1;
  loadModels();
};

/** 分页大小变化 */
const handleSizeChange = (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
  loadModels();
};

/** 卡片点击 → 进入模型管理 */
const handleCardClick = (model: ModelVO) => {
  router.push({ name: 'model-design-detail', params: { id: model.id } });
};

/** 卡片选中 */
const handleCardSelect = (model: ModelVO, selected: boolean) => {
  if (selected) {
    selectedIds.value.add(model.id);
  } else {
    selectedIds.value.delete(model.id);
  }
};

/** 删除选中模型（批量） */
const handleDelete = async () => {
  if (!hasSelection.value) {
    TpMessage.warning(t('modelDesign.message.selectAtLeastOne'));
    return;
  }
  const ids = Array.from(selectedIds.value);
  try {
    await TpConfirm.confirm({
      message: t('modelDesign.message.batchDeleteConfirm', { count: ids.length }),
      type: 'warning',
    });
  } catch {
    // 用户取消确认
    return;
  }
  // 并行删除，单条失败不影响其他
  const results = await Promise.allSettled(ids.map(id => deleteModel(id)));
  const failed = results.filter(r => r.status === 'rejected');
  if (failed.length === 0) {
    TpMessage.success(t('modelDesign.message.deleteSuccess'));
    selectedIds.value.clear();
    loadModels();
  } else {
    TpMessage.error(t('modelDesign.message.loadFailed'));
    console.error('[handleDelete] 部分删除失败', failed);
  }
};

/** 通知"功能开发中" */
const notifyFeatureDeveloping = (featureKey: string) => {
  TpMessage.info(t('modelDesign.message.featureDeveloping', { feature: featureKey }));
};

/** 复制模型（占位） */
const handleCopy = () => {
  notifyFeatureDeveloping(t('modelDesign.list.toolbar.copy'));
};

/** 导入模型（占位） */
const handleImport = () => {
  notifyFeatureDeveloping(t('modelDesign.list.toolbar.import'));
};

/** 复用资产（占位） */
const handleReuse = () => {
  notifyFeatureDeveloping(t('modelDesign.list.toolbar.reuse'));
};

/** 移动（占位） */
const handleMove = () => {
  notifyFeatureDeveloping(t('modelDesign.list.toolbar.move'));
};

/** 导出（占位） */
const handleExport = () => {
  notifyFeatureDeveloping(t('modelDesign.list.toolbar.export'));
};

// ========== 主题域树 ==========

/** 取主题域懒加载节点（封装一层断言，避开 API 层缺失的 AxiosResponse 包装） */
const fetchTopicChildren = async (parentId?: string): Promise<TpTreeNode[]> => {
  const apiCall = parentId === undefined
    ? getTopicRootTree()
    : getTopicChildren(parentId);
  const res = (await apiCall) as unknown as AxiosResponse<ApiResponse<TopicTreeNode[]>>;
  return (res.data.data ?? []) as unknown as TpTreeNode[];
};

const loadTreeNode: TpTreeLoadFn = (node, resolve) => {
  const parentId = node.level === 0 ? undefined : String(node.data.id);
  fetchTopicChildren(parentId)
    .then(resolve)
    .catch((error) => {
      resolve([]);
      console.error('[loadTreeNode]', error);
    });
};

/** 树节点点击 */
const handleTreeNodeClick = (node: TpTreeNode) => {
  if (node.id == null) return;
  selectedTopicId.value = String(node.id) as ID;
  selectedTopicIsLeaf.value = node.isLeaf ?? true;
  currentPage.value = 1;
  selectedIds.value.clear();
  loadModels();
};

// ========== 生命周期 ==========

onMounted(() => {
  loadModels();
});
</script>

<template>
  <TpPageFrame>
    <TpLeftTreeLayout
      v-model:collapsed="leftCollapsed"
      class="flex-1 min-h-0"
      :default-width="280"
      :min-width="220"
      storage-key="model-index-tree-width"
    >
      <!-- 左侧：主题域分类树 -->
      <template #left>
        <TpTreeLazy
          :load="loadTreeNode"
          :field-map="{ label: 'domainName' }"
          search-placeholder="搜索主题域"
          :expand-on-click-node="false"
          @node-click="handleTreeNodeClick"
        />
      </template>

      <!-- 右侧：模型展示区 -->
      <template #right>
        <div class="flex flex-col h-full">

          <!-- 工具栏第1行：筛选 + 搜索 + 视图切换（Figma 设计） -->
          <div class="flex items-center justify-between p-3 flex-shrink-0 border-b border-[#e9e9e9]">
            <!-- LEFT: 视图切换 + 排序标签 -->
            <div class="flex items-center gap-2">
              <!-- 视图切换（图标按钮组） -->
              <div class="flex border border-[#e1e9f0] rounded-sm overflow-hidden">
                <button
                  class="flex items-center justify-center w-8 h-8 transition-colors"
                  :class="viewMode === 'card' ? 'bg-[#fff] text-[#2E8AE6]' : 'bg-[#eee] text-[#666] hover:bg-[#e5e5e5]'"
                  @click="viewMode = 'card'"
                >
                  <el-icon><Grid /></el-icon>
                </button>
                <button
                  class="flex items-center justify-center w-8 h-8 border-l border-[#e1e9f0] transition-colors"
                  :class="viewMode === 'list' ? 'bg-[#fff] text-[#2E8AE6]' : 'bg-[#eee] text-[#666] hover:bg-[#e5e5e5]'"
                  @click="viewMode = 'list'"
                >
                  <el-icon><List /></el-icon>
                </button>
              </div>

              <!-- 排序标签：模型名称 -->
              <button
                class="flex items-center gap-1 h-8 px-2 bg-white border border-[#dcdfe6] rounded-sm text-xs font-medium text-[#333] whitespace-nowrap hover:border-[#2E8AE6]"
                @click="handleSort('name')"
              >
                {{ t('modelDesign.list.sort.byName') }}
                <el-icon :size="14" :class="sortBy === 'name' ? 'text-[#2E8AE6]' : 'text-[#999]'">
                  <ArrowDown :class="sortBy === 'name' && sortOrder === 'asc' ? 'rotate-180' : ''" />
                </el-icon>
              </button>
              <!-- 排序标签：创建时间 -->
              <button
                class="flex items-center gap-1 h-8 px-2 bg-white border border-[#dcdfe6] rounded-sm text-xs font-medium text-[#333] whitespace-nowrap hover:border-[#2E8AE6]"
                @click="handleSort('createdAt')"
              >
                {{ t('modelDesign.list.sort.byCreatedAt') }}
                <el-icon :size="14" :class="sortBy === 'createdAt' ? 'text-[#2E8AE6]' : 'text-[#999]'">
                  <ArrowDown :class="sortBy === 'createdAt' && sortOrder === 'asc' ? 'rotate-180' : ''" />
                </el-icon>
              </button>
            </div>

            <!-- RIGHT: 模型类型 Select + 搜索框 + 状态 Select -->
            <div class="flex items-center gap-2">
              <el-select
                v-model="filters.modelType"
                :placeholder="t('modelDesign.list.search.typePlaceholder')"
                clearable
                size="default"
                style="width: 160px"
                @change="handleSearch"
              >
                <el-option
                  v-for="opt in MODEL_TYPE_OPTIONS"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
              <el-input
                v-model="filters.keyword"
                :placeholder="t('modelDesign.list.search.keywordPlaceholder')"
                clearable
                size="default"
                style="width: 280px"
                @keyup.enter="handleSearch"
              >
                <template #suffix>
                  <el-icon class="cursor-pointer text-[#999] hover:text-[#333]" @click="handleSearch">
                    <Search />
                  </el-icon>
                </template>
              </el-input>
              <el-select
                v-model="filters.status"
                :placeholder="t('modelDesign.list.search.statusPlaceholder')"
                clearable
                size="default"
                style="width: 160px"
                @change="handleSearch"
              >
                <el-option
                  v-for="opt in MODEL_STATUS_OPTIONS"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </div>
          </div>

          <!-- 工具栏第2行：操作按钮 -->
          <div class="flex items-center justify-between p-3 flex-shrink-0">
            <div class="flex items-center gap-2">
              <el-button size="default" :disabled="!hasSelection" @click="handleMove">
                {{ t('modelDesign.list.toolbar.move') }}
              </el-button>
              <el-button size="default" :icon="Delete" :disabled="!hasSelection" @click="handleDelete">
                {{ t('modelDesign.list.toolbar.delete') }}
              </el-button>
              <el-button size="default" @click="handleExport">
                {{ t('modelDesign.list.toolbar.export') }}
              </el-button>
            </div>
            <div class="flex items-center gap-2">
              <el-checkbox v-model="cascade" @change="loadModels">
                {{ t('modelDesign.list.view.cascade') }}
              </el-checkbox>
            </div>
          </div>

          <!-- 内容区 -->
          <div class="flex-1 min-h-0 flex flex-col px-3">
            <!-- 卡片视图 -->
            <TpCardList
              v-if="viewMode === 'card'"
              class="flex-1 min-h-0"
              :data="models"
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :total="total"
              :loading="loading"
              :empty-text="t('modelDesign.list.empty')"
              @page-change="loadModels"
              @size-change="handleSizeChange"
            >
              <template #item="{ item }">
                <ModelCard
                  :model="item"
                  :selected="selectedIds.has(item.id)"
                  @click="handleCardClick"
                  @select="handleCardSelect"
                />
              </template>
            </TpCardList>

            <!-- 列表视图 -->
            <TpTable
              v-else
              class="flex-1 min-h-0"
              :columns="listColumns"
              :data="models"
              :loading="loading"
              :total="total"
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              @page-change="loadModels"
              @size-change="handleSizeChange"
            >
            </TpTable>
          </div>
        </div>
      </template>
    </TpLeftTreeLayout>
  </TpPageFrame>

</template>
