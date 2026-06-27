<script setup lang="ts">
/**
 * SegmentList - 码段管理
 *
 * 页面布局：左侧分类导航 + 右侧码段列表
 * - 左侧：8类码段类型导航（固定码/流水码/日期码/特征码/区间流水码/引用码/动态流水码/引用流水码/日期流水码）
 * - 右侧：当前选中类型的码段列表（分页、搜索、引用状态显示）
 *
 * 路由：/model-design/segment
 */
import { ref, reactive, onMounted, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import type { AxiosResponse } from 'axios';
import { Search, Plus, View } from '@element-plus/icons-vue';
import TpTable, { type TpTableColumn } from '@mdm/common/components/data/TpTable.vue';
import TpEmpty from '@mdm/common/components/data/TpEmpty.vue';
import { TpMessage } from '@mdm/common/components/feedback/TpMessage';
import { TpConfirm } from '@mdm/common/components/feedback/TpConfirm';
import SegmentFormDialog from '@/modules/model-design/components/coding-rule/SegmentFormDialog.vue';
import {
  listSegment, deleteSegment, batchDeleteSegment,
} from '@/modules/model-manage/api/segment';
import type { SegmentVO, SegmentQuery } from '@/modules/model-manage/types/segment';
import { SEGMENT_TYPE_OPTIONS, SEGMENT_TYPE_LABEL } from '@/modules/model-manage/types/segment';
import type { SegmentType } from '@/modules/model-manage/types/coding-rule';

defineOptions({ name: 'SegmentList' });

const { t } = useI18n();
const route = useRoute();
const router = useRouter();

// ========== 常量 ==========

/** 码段类型导航（与左侧分类对应） */
const SEGMENT_TYPE_NAV: { label: string; value: SegmentType }[] = [
  { label: '固定码', value: 'fixed' },
  { label: '流水码', value: 'serial' },
  { label: '日期码', value: 'date' },
  { label: '特征码', value: 'feature' },
  { label: '区间流水码', value: 'rangeSerial' },
  { label: '引用码', value: 'ref' },
  { label: '动态流水码', value: 'dynamicSerial' },
  { label: '日期流水码', value: 'dateSerial' },
  { label: '引用流水码', value: 'refSerial' },
];

// ========== 状态 ==========

/** 当前选中的码段类型 */
const selectedType = ref<SegmentType>('fixed');

/** 加载状态 */
const loading = ref(false);

/** 列表数据 */
const tableData = ref<SegmentVO[]>([]);

/** 总数 */
const total = ref(0);

/** 分页 */
const currentPage = ref(1);
const pageSize = ref(20);

/** 搜索关键词 */
const searchKeyword = ref('');

/** 新增/编辑弹窗 */
const dialogVisible = ref(false);
const dialogTitle = ref('新增码段');
const dialogMode = ref<'create' | 'edit'>('create');
const editingSegmentId = ref<string | null>(null);

/** 表格选中行 */
const selectedRows = ref<SegmentVO[]>([]);

// ========== 列配置 ==========

const columns = computed<TpTableColumn[]>(() => [
  { type: 'selection', width: 60 },
  { prop: 'code', label: '码段编码', minWidth: 120, showOverflowTooltip: true },
  { prop: 'name', label: '码段名称', minWidth: 150, showOverflowTooltip: true },
  {
    prop: 'typeName',
    label: '码段类型',
    minWidth: 100,
    formatter: (row: SegmentVO) => SEGMENT_TYPE_LABEL[row.type as SegmentType] || row.type,
  },
  {
    prop: 'referenceStatus',
    label: '引用状态',
    minWidth: 100,
    formatter: (row: SegmentVO) => row.referenceStatus === 'used' ? '已使用' : '未使用',
    cellClass: (row: SegmentVO) => row.referenceStatus === 'used' ? 'text-[var(--el-color-warning)]' : 'text-[var(--el-color-success)]',
  },
  { prop: 'prefix', label: '前缀', minWidth: 80 },
  { prop: 'suffix', label: '后缀', minWidth: 80 },
  { prop: 'description', label: '描述', minWidth: 150, showOverflowTooltip: true },
  { prop: 'actions', label: '操作', minWidth: 180, fixed: 'right' },
]);

// ========== 计算属性 ==========

/** 当前类型名称 */
const currentTypeName = computed(() => SEGMENT_TYPE_LABEL[selectedType.value] || selectedType.value);

// ========== 方法 ==========

/** 加载数据 */
const loadData = async () => {
  loading.value = true;
  try {
    const query: SegmentQuery = {
      modelId: route.query.modelId as string | undefined,
      type: selectedType.value,
      keyword: searchKeyword.value || undefined,
      page: currentPage.value,
      pageSize: pageSize.value,
    };
    const res = (await listSegment(query)) as unknown as AxiosResponse<any>;
    const pageData = res.data?.data;
    if (pageData) {
      tableData.value = (pageData.rows ?? []).map((row: SegmentVO) => ({
        ...row,
        typeName: SEGMENT_TYPE_LABEL[row.type as SegmentType] || row.type,
      }));
      total.value = pageData.total ?? 0;
    }
  } catch (error) {
    console.error('[SegmentList] load error', error);
    tableData.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
};

/** 切换类型 */
const handleTypeChange = (type: SegmentType) => {
  selectedType.value = type;
  currentPage.value = 1;
  loadData();
};

/** 搜索 */
const handleSearch = () => {
  currentPage.value = 1;
  loadData();
};

/** 重置 */
const handleReset = () => {
  searchKeyword.value = '';
  currentPage.value = 1;
  loadData();
};

/** 分页变化 */
const handlePageChange = (page: number) => {
  currentPage.value = page;
  loadData();
};

/** 每页条数变化 */
const handleSizeChange = (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
  loadData();
};

/** 新增 */
const handleAdd = () => {
  dialogTitle.value = `新增${currentTypeName.value}`;
  dialogMode.value = 'create';
  editingSegmentId.value = null;
  dialogVisible.value = true;
};

/** 查看 */
const handleView = (row: SegmentVO) => {
  dialogTitle.value = `查看${currentTypeName.value}`;
  dialogMode.value = 'view';
  editingSegmentId.value = row.id;
  dialogVisible.value = true;
};

/** 编辑 */
const handleEdit = (row: SegmentVO) => {
  if (row.referenceStatus === 'used') {
    TpMessage.warning('该码段已被编码规则引用，禁止编辑');
    return;
  }
  dialogTitle.value = `编辑${currentTypeName.value}`;
  dialogMode.value = 'edit';
  editingSegmentId.value = row.id;
  dialogVisible.value = true;
};

/** 删除 */
const handleDelete = async (row: SegmentVO) => {
  if (row.referenceStatus === 'used') {
    TpMessage.warning('该码段已被编码规则引用，禁止删除');
    return;
  }
  try {
    await TpConfirm.delete(`确定要删除码段"${row.name}"吗？`);
    await deleteSegment(row.id);
    TpMessage.success('删除成功');
    await loadData();
  } catch (error) {
    if ((error as any)?.message !== 'cancel') {
      console.error('[SegmentList] delete error', error);
    }
  }
};

/** 批量删除 */
const handleBatchDelete = async () => {
  if (selectedRows.value.length === 0) {
    TpMessage.warning('请选择要删除的码段');
    return;
  }
  const usedRows = selectedRows.value.filter(row => row.referenceStatus === 'used');
  if (usedRows.length > 0) {
    TpMessage.warning(`所选码段中有 ${usedRows.length} 个已被引用，禁止删除`);
    return;
  }
  try {
    await TpConfirm.delete(`确定要删除所选的 ${selectedRows.value.length} 个码段吗？`);
    const ids = selectedRows.value.map(row => row.id);
    await batchDeleteSegment(ids);
    TpMessage.success('删除成功');
    selectedRows.value = [];
    await loadData();
  } catch (error) {
    if ((error as any)?.message !== 'cancel') {
      console.error('[SegmentList] batch delete error', error);
    }
  }
};

/** 弹窗成功回调 */
const handleDialogSuccess = () => {
  dialogVisible.value = false;
  loadData();
};

/** 选择变化 */
const handleSelectionChange = (rows: SegmentVO[]) => {
  selectedRows.value = rows;
};

// ========== 生命周期 ==========

onMounted(() => {
  loadData();
});

// 路由参数变化时重新加载
watch(() => route.query.modelId, () => {
  loadData();
});
</script>

<template>
  <div class="segment-list h-full flex flex-col">
    <!-- 顶部工具栏 -->
    <div class="flex items-center justify-between px-4 py-3 border-b border-[var(--el-border-color-lighter)] bg-white">
      <div class="flex items-center gap-4">
        <h3 class="text-base font-medium">{{ currentTypeName }}管理</h3>
        <span class="text-sm text-[var(--el-text-color-secondary)]">
          共 {{ total }} 条
        </span>
      </div>
      <div class="flex items-center gap-2">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索码段名称/编码"
          :prefix-icon="Search"
          clearable
          style="width: 200px"
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        />
        <el-button @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-button
          v-if="selectedRows.length > 0"
          type="danger"
          @click="handleBatchDelete"
        >
          批量删除({{ selectedRows.length }})
        </el-button>
        <el-button type="primary" :icon="Plus" @click="handleAdd">
          新增{{ currentTypeName }}
        </el-button>
      </div>
    </div>

    <!-- 主体内容 -->
    <div class="flex-1 min-h-0 flex">
      <!-- 左侧类型导航 -->
      <div class="w-48 border-r border-[var(--el-border-color-lighter)] bg-[var(--el-fill-color-lighter)] overflow-y-auto">
        <div class="py-2">
          <div
            v-for="nav in SEGMENT_TYPE_NAV"
            :key="nav.value"
            :class="[
              'px-4 py-2.5 cursor-pointer transition-colors',
              selectedType === nav.value
                ? 'bg-white text-[var(--el-color-primary)] font-medium border-l-2 border-[var(--el-color-primary)]'
                : 'text-[var(--el-text-color-regular)] hover:bg-[var(--el-fill-color)]',
            ]"
            @click="handleTypeChange(nav.value)"
          >
            {{ nav.label }}
          </div>
        </div>
      </div>

      <!-- 右侧码段列表 -->
      <div class="flex-1 min-h-0 px-4 py-4 overflow-auto">
        <TpTable
          :columns="columns"
          :data="tableData"
          :loading="loading"
          :total="total"
          :current-page="currentPage"
          :page-size="pageSize"
          border
          stripe
          @selection-change="handleSelectionChange"
          @update:current-page="handlePageChange"
          @update:page-size="handleSizeChange"
        >
          <template #col-actions="{ row }">
            <el-button size="small" link type="primary" :icon="View" @click="handleView(row as SegmentVO)">
              查看
            </el-button>
            <el-button
              size="small"
              link
              type="primary"
              :disabled="(row as SegmentVO).referenceStatus === 'used'"
              @click="handleEdit(row as SegmentVO)"
            >
              编辑
            </el-button>
            <el-button
              size="small"
              link
              type="danger"
              :disabled="(row as SegmentVO).referenceStatus === 'used'"
              @click="handleDelete(row as SegmentVO)"
            >
              删除
            </el-button>
          </template>

          <template #empty>
            <TpEmpty state="no-data" :description="`暂无${currentTypeName}，请点击新增`" />
          </template>
        </TpTable>
      </div>
    </div>

    <!-- 新增/编辑/查看弹窗 -->
    <SegmentFormDialog
      v-model="dialogVisible"
      :title="dialogTitle"
      :mode="dialogMode"
      :segment-id="editingSegmentId"
      :segment-type="selectedType"
      :model-id="route.query.modelId as string | undefined"
      @success="handleDialogSuccess"
    />
  </div>
</template>

<style scoped>
.segment-list {
  background: var(--el-bg-color);
}
</style>
