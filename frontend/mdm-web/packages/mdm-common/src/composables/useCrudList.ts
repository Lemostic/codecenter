/**
 * common/composables/useCrudList.ts
 *
 * 列表页通用 CRUD 逻辑（加载、分页、搜索、重置、批量删除）。
 * 适用于带 TpTable 的列表页面。
 *
 * 用法：
 *   const {
 *     loading, tableData, total, currentPage, pageSize,
 *     query, handleSearch, handleReset, handlePageChange,
 *     handleSizeChange, handleDelete, reload,
 *   } = useCrudList({ fetchApi, deleteApi, defaultQuery, queryParams });
 */
import { ref, reactive } from 'vue';
import type { PaginationParams, PaginatedData } from '@mdm/common/types/base';
import type { ID } from '@mdm/common/types/base';
import { TpConfirm } from '@mdm/common/components/feedback/TpConfirm';
import { TpMessage } from '@mdm/common/components/feedback/TpMessage';

interface UseCrudListOptions<TQuery extends PaginationParams> {
  /** 列表查询 API（返回分页数据） */
  fetchApi: (query: TQuery) => Promise<PaginatedData<Record<string, unknown>>>;
  /** 删除 API（单个 ID） */
  deleteApi?: (id: ID) => Promise<void>;
  /** 默认查询参数工厂 */
  defaultQuery: () => TQuery;
  /** 将 query 响应式对象转为 API 参数的序列化函数（可选，默认直传） */
  queryParams?: () => TQuery;
  /** 每页条数默认值 */
  defaultPageSize?: number;
}

export function useCrudList<TQuery extends PaginationParams>(
  options: UseCrudListOptions<TQuery>,
) {
  const {
    fetchApi,
    deleteApi,
    defaultQuery,
    queryParams,
    defaultPageSize = 20,
  } = options;

  // ========== 响应式状态 ==========
  const loading = ref(false);
  const tableData = ref<Record<string, unknown>[]>([]);
  const total = ref(0);
  const currentPage = ref(1);
  const pageSize = ref(defaultPageSize);
  const query = reactive(defaultQuery()) as TQuery;

  // ========== 方法 ==========

  /** 加载列表数据 */
  const loadData = async () => {
    loading.value = true;
    try {
      const params = queryParams ? queryParams() : { ...query, page: currentPage.value, pageSize: pageSize.value };
      const res = await fetchApi(params);
      tableData.value = res.rows;
      total.value = res.total;
    } catch {
      tableData.value = [];
      total.value = 0;
    } finally {
      loading.value = false;
    }
  };

  /** 搜索（重置到第一页） */
  const handleSearch = () => {
    currentPage.value = 1;
    loadData();
  };

  /** 重置搜索条件并刷新 */
  const handleReset = () => {
    const defaults = defaultQuery();
    Object.assign(query, defaults);
    currentPage.value = 1;
    loadData();
  };

  /** 翻页 */
  const handlePageChange = (page: number) => {
    currentPage.value = page;
    loadData();
  };

  /** 切换每页条数 */
  const handleSizeChange = (size: number) => {
    pageSize.value = size;
    currentPage.value = 1;
    loadData();
  };

  /** 删除单条记录（带确认弹窗） */
  const handleDelete = async (
    id: ID,
    name: string,
    confirmMsg = '确定要删除"{name}"吗？',
  ) => {
    if (!deleteApi) return;
    try {
      await TpConfirm.delete(confirmMsg, { name });
      await deleteApi(id);
      TpMessage.success('删除成功');
      // 当前页只剩最后一条时回退一页
      if (tableData.value.length === 1 && currentPage.value > 1) {
        currentPage.value--;
      }
      await loadData();
    } catch {
      // 用户取消或删除失败，静默处理
    }
  };

  /** 批量删除（带确认弹窗） */
  const handleBatchDelete = async (
    ids: ID[],
    confirmMsg = '确定要删除所选的 {count} 条记录吗？',
  ) => {
    if (!deleteApi || ids.length === 0) {
      TpMessage.warning('请至少选择一条记录进行操作');
      return;
    }
    try {
      await TpConfirm.confirm({
        message: confirmMsg,
        params: { count: String(ids.length) },
      });
      for (const id of ids) {
        await deleteApi(id);
      }
      TpMessage.success('删除成功');
      if (tableData.value.length <= ids.length && currentPage.value > 1) {
        currentPage.value--;
      }
      await loadData();
    } catch {
      // 用户取消或删除失败
    }
  };

  /** 重新加载（保持当前页码） */
  const reload = () => loadData();

  return {
    loading,
    tableData,
    total,
    currentPage,
    pageSize,
    query,
    handleSearch,
    handleReset,
    handlePageChange,
    handleSizeChange,
    handleDelete,
    handleBatchDelete,
    reload,
    loadData,
  };
}
