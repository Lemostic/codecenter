/**
 * useCrudList 单元测试
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { ref } from 'vue';
import { useCrudList } from './useCrudList';

// ========== Mock 依赖 ==========
vi.mock('@mdm/common/components/feedback/TpConfirm', () => ({
  TpConfirm: {
    delete: vi.fn(),
    confirm: vi.fn(),
  },
}));

vi.mock('@mdm/common/components/feedback/TpMessage', () => ({
  TpMessage: {
    success: vi.fn(),
    warning: vi.fn(),
    error: vi.fn(),
  },
}));

// ========== 类型定义 ==========
interface TestQuery extends Partial<{ page: number; pageSize: number; name: string }> {
  page: number;
  pageSize: number;
}

interface TestRow {
  id: string;
  name: string;
}

// ========== 测试工具函数 ==========
function createTestFetchApi(pages: TestRow[][]) {
  return vi.fn().mockImplementation((query: TestQuery) => {
    const pageIndex = query.page - 1;
    const rows = pages[pageIndex] ?? [];
    return Promise.resolve({
      rows,
      total: pages.flat().length,
    });
  });
}

function createTestDeleteApi() {
  return vi.fn().mockResolvedValue(undefined);
}

describe('useCrudList', () => {
  // 重置所有 mock
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('基本状态初始化', () => {
    it('应正确初始化分页状态', () => {
      const fetchApi = createTestFetchApi([[]]);
      const { loading, currentPage, pageSize, total } = useCrudList({
        fetchApi,
        defaultQuery: () => ({ page: 1, pageSize: 20 }),
      });

      expect(loading.value).toBe(false);
      expect(currentPage.value).toBe(1);
      expect(pageSize.value).toBe(20);
      expect(total.value).toBe(0);
    });

    it('应使用自定义默认分页大小', () => {
      const fetchApi = createTestFetchApi([[]]);
      const { pageSize } = useCrudList({
        fetchApi,
        defaultQuery: () => ({ page: 1, pageSize: 10 }),
        defaultPageSize: 10,
      });

      expect(pageSize.value).toBe(10);
    });
  });

  describe('loadData', () => {
    it('应正确加载数据', async () => {
      const mockData = [
        { id: '1', name: 'item1' },
        { id: '2', name: 'item2' },
      ];
      const fetchApi = createTestFetchApi([mockData]);
      const { loadData, tableData, total, loading } = useCrudList({
        fetchApi,
        defaultQuery: () => ({ page: 1, pageSize: 20 }),
      });

      await loadData();

      expect(fetchApi).toHaveBeenCalledWith({ page: 1, pageSize: 20 });
      expect(tableData.value).toEqual(mockData);
      expect(total.value).toBe(2);
      expect(loading.value).toBe(false);
    });

    it('加载失败时应清空数据', async () => {
      const fetchApi = vi.fn().mockRejectedValue(new Error('API Error'));
      const { loadData, tableData, total, loading } = useCrudList({
        fetchApi,
        defaultQuery: () => ({ page: 1, pageSize: 20 }),
      });

      await loadData();

      expect(tableData.value).toEqual([]);
      expect(total.value).toBe(0);
      expect(loading.value).toBe(false);
    });

    it('加载中时应设置 loading 状态', async () => {
      let resolveLoadData: (value: unknown) => void;
      const fetchApi = vi.fn().mockImplementation(
        () =>
          new Promise((resolve) => {
            resolveLoadData = resolve;
          }),
      );
      const { loadData, loading } = useCrudList({
        fetchApi,
        defaultQuery: () => ({ page: 1, pageSize: 20 }),
      });

      const loadPromise = loadData();
      expect(loading.value).toBe(true);

      resolveLoadData!({ rows: [], total: 0 });
      await loadPromise;

      expect(loading.value).toBe(false);
    });
  });

  describe('handleSearch', () => {
    it('应重置到第一页并重新加载', async () => {
      const mockData = [{ id: '1', name: 'item1' }];
      const fetchApi = createTestFetchApi([mockData, []]);
      const { handleSearch, currentPage, loadData } = useCrudList({
        fetchApi,
        defaultQuery: () => ({ page: 1, pageSize: 20 }),
      });

      // 手动设置到第二页
      currentPage.value = 2;
      await loadData(); // 加载第二页数据

      handleSearch();

      expect(currentPage.value).toBe(1);
      expect(fetchApi).toHaveBeenLastCalledWith({ page: 1, pageSize: 20 });
    });
  });

  describe('handleReset', () => {
    it('应重置查询条件到默认值', async () => {
      const fetchApi = createTestFetchApi([[]]);
      const { handleReset, query, currentPage } = useCrudList({
        fetchApi,
        defaultQuery: () => ({ page: 1, pageSize: 20, name: '' }),
      });

      // 修改查询条件
      query.name = 'test';
      currentPage.value = 3;

      handleReset();

      expect(query.name).toBe('');
      expect(currentPage.value).toBe(1);
    });
  });

  describe('handlePageChange', () => {
    it('应正确切换页码', async () => {
      const mockData = [{ id: '1', name: 'item1' }];
      const fetchApi = createTestFetchApi([[], mockData]);
      const { handlePageChange, currentPage, loadData } = useCrudList({
        fetchApi,
        defaultQuery: () => ({ page: 1, pageSize: 20 }),
      });

      await loadData(); // 加载第一页
      handlePageChange(2);

      expect(currentPage.value).toBe(2);
      expect(fetchApi).toHaveBeenLastCalledWith({ page: 2, pageSize: 20 });
    });
  });

  describe('handleSizeChange', () => {
    it('应切换每页条数并回到第一页', async () => {
      const fetchApi = createTestFetchApi([[]]);
      const { handleSizeChange, pageSize, currentPage } = useCrudList({
        fetchApi,
        defaultQuery: () => ({ page: 1, pageSize: 20 }),
      });

      currentPage.value = 3;
      handleSizeChange(50);

      expect(pageSize.value).toBe(50);
      expect(currentPage.value).toBe(1);
      expect(fetchApi).toHaveBeenCalledWith({ page: 1, pageSize: 50 });
    });
  });

  describe('handleDelete', () => {
    it('应调用删除 API 并刷新列表', async () => {
      const { TpConfirm } = await import('@mdm/common/components/feedback/TpConfirm');
      const { TpMessage } = await import('@mdm/common/components/feedback/TpMessage');
      const fetchApi = createTestFetchApi([[{ id: '1', name: 'item1' }]]);
      const deleteApi = createTestDeleteApi();
      const { handleDelete, loadData } = useCrudList({
        fetchApi,
        deleteApi,
        defaultQuery: () => ({ page: 1, pageSize: 20 }),
      });

      // Mock 用户确认删除
      vi.mocked(TpConfirm.delete).mockResolvedValue('confirm' as any);

      await loadData();
      await handleDelete('1', 'item1');

      expect(TpConfirm.delete).toHaveBeenCalled();
      expect(deleteApi).toHaveBeenCalledWith('1');
      expect(TpMessage.success).toHaveBeenCalledWith('删除成功');
    });

    it('无 deleteApi 时不应执行删除', async () => {
      const fetchApi = createTestFetchApi([[{ id: '1', name: 'item1' }]]);
      const { handleDelete, loadData } = useCrudList({
        fetchApi,
        defaultQuery: () => ({ page: 1, pageSize: 20 }),
      });

      await loadData();
      await handleDelete('1', 'item1');

      // TpConfirm.delete 不应被调用
      const { TpConfirm } = await import('@mdm/common/components/feedback/TpConfirm');
      expect(TpConfirm.delete).not.toHaveBeenCalled();
    });

    it('最后一页只剩一条时删除后应回退页码', async () => {
      const fetchApi = createTestFetchApi([[], [{ id: '1', name: 'item1' }]]);
      const deleteApi = createTestDeleteApi();
      const { handleDelete, currentPage, loadData } = useCrudList({
        fetchApi,
        deleteApi,
        defaultQuery: () => ({ page: 1, pageSize: 20 }),
      });

      const { TpConfirm } = await import('@mdm/common/components/feedback/TpConfirm');
      vi.mocked(TpConfirm.delete).mockResolvedValue('confirm' as any);

      // 加载第二页（只有一条数据）
      currentPage.value = 2;
      await loadData();

      await handleDelete('1', 'item1');

      expect(currentPage.value).toBe(1);
    });
  });

  describe('reload', () => {
    it('应保持当前页码重新加载', async () => {
      const fetchApi = createTestFetchApi([[], [{ id: '2', name: 'item2' }]]);
      const { reload, currentPage } = useCrudList({
        fetchApi,
        defaultQuery: () => ({ page: 1, pageSize: 20 }),
      });

      currentPage.value = 2;
      await reload();

      expect(fetchApi).toHaveBeenLastCalledWith({ page: 2, pageSize: 20 });
    });
  });
});
