/**
 * segment API 单元测试
 *
 * 测试码段管理 API 函数的请求参数和响应格式
 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { http } from '@mdm/core/http';

// Mock http
vi.mock('@mdm/core/http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('segment API', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('listSegment', () => {
    it('应使用 GET 请求', async () => {
      const { listSegment } = await import('./segment');

      (http.get as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: { success: true, data: { rows: [], total: 0 } },
      });

      await listSegment({ page: 1, pageSize: 20 });

      expect(http.get).toHaveBeenCalledWith(
        '/api/v1/model-design/segment',
        expect.objectContaining({ params: expect.any(Object) }),
      );
    });

    it('应传递码段类型过滤参数', async () => {
      const { listSegment } = await import('./segment');

      (http.get as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: { success: true, data: { rows: [], total: 0 } },
      });

      await listSegment({
        type: 'fixed',
        keyword: 'test',
        page: 1,
        pageSize: 20,
      });

      expect(http.get).toHaveBeenCalledWith(
        '/api/v1/model-design/segment',
        expect.objectContaining({
          params: expect.objectContaining({
            type: 'fixed',
            keyword: 'test',
          }),
        }),
      );
    });
  });

  describe('getSegment', () => {
    it('应使用正确的 ID 调用 GET 请求', async () => {
      const { getSegment } = await import('./segment');

      (http.get as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: { success: true, data: {} },
      });

      await getSegment('segment-id');

      expect(http.get).toHaveBeenCalledWith(
        '/api/v1/model-design/segment/segment-id',
      );
    });
  });

  describe('createSegment', () => {
    it('应使用 POST 请求', async () => {
      const { createSegment } = await import('./segment');

      (http.post as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: { success: true, data: {} },
      });

      await createSegment({
        type: 'fixed',
        name: '固定码1',
        value: 'PREFIX',
      });

      expect(http.post).toHaveBeenCalledWith(
        '/api/v1/model-design/segment',
        expect.objectContaining({
          type: 'fixed',
          name: '固定码1',
          value: 'PREFIX',
        }),
      );
    });

    it('应支持流水码配置', async () => {
      const { createSegment } = await import('./segment');

      (http.post as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: { success: true, data: {} },
      });

      await createSegment({
        type: 'serial',
        name: '流水码1',
        length: 4,
        startValue: 1,
        step: 1,
      });

      expect(http.post).toHaveBeenCalledWith(
        '/api/v1/model-design/segment',
        expect.objectContaining({
          type: 'serial',
          name: '流水码1',
          length: 4,
          startValue: 1,
          step: 1,
        }),
      );
    });
  });

  describe('updateSegment', () => {
    it('应使用 PUT 请求并包含 ID', async () => {
      const { updateSegment } = await import('./segment');

      (http.put as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: { success: true, data: {} },
      });

      await updateSegment({
        id: 'segment-id',
        name: '更新后的码段',
      });

      expect(http.put).toHaveBeenCalledWith(
        '/api/v1/model-design/segment/segment-id',
        expect.objectContaining({
          id: 'segment-id',
          name: '更新后的码段',
        }),
      );
    });
  });

  describe('deleteSegment', () => {
    it('应使用 DELETE 请求', async () => {
      const { deleteSegment } = await import('./segment');

      (http.delete as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: { success: true },
      });

      await deleteSegment('segment-id');

      expect(http.delete).toHaveBeenCalledWith(
        '/api/v1/model-design/segment/segment-id',
      );
    });
  });

  describe('batchDeleteSegment', () => {
    it('应使用 POST 请求批量删除', async () => {
      const { batchDeleteSegment } = await import('./segment');

      (http.post as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: { success: true },
      });

      await batchDeleteSegment(['id1', 'id2', 'id3']);

      expect(http.post).toHaveBeenCalledWith(
        '/api/v1/model-design/segment/batch-delete',
        { ids: ['id1', 'id2', 'id3'] },
      );
    });
  });

  describe('updateSegmentStatus', () => {
    it('应使用 PUT 请求更新状态', async () => {
      const { updateSegmentStatus } = await import('./segment');

      (http.put as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: { success: true },
      });

      await updateSegmentStatus('segment-id', 'disabled');

      expect(http.put).toHaveBeenCalledWith(
        '/api/v1/model-design/segment/segment-id/status',
        { status: 'disabled' },
      );
    });
  });

  describe('checkSegmentCodeUnique', () => {
    it('应使用 GET 请求检查编码唯一性', async () => {
      const { checkSegmentCodeUnique } = await import('./segment');

      (http.get as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: { success: true, data: true },
      });

      await checkSegmentCodeUnique('GD001');

      expect(http.get).toHaveBeenCalledWith(
        '/api/v1/model-design/segment/check-code',
        expect.objectContaining({ params: { code: 'GD001' } }),
      );
    });

    it('应支持排除 ID 参数', async () => {
      const { checkSegmentCodeUnique } = await import('./segment');

      (http.get as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: { success: true, data: true },
      });

      await checkSegmentCodeUnique('GD001', 'exclude-id');

      expect(http.get).toHaveBeenCalledWith(
        '/api/v1/model-design/segment/check-code',
        expect.objectContaining({ params: { code: 'GD001', excludeId: 'exclude-id' } }),
      );
    });
  });

  describe('checkSegmentNameUnique', () => {
    it('应使用 GET 请求检查名称唯一性', async () => {
      const { checkSegmentNameUnique } = await import('./segment');

      (http.get as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: { success: true, data: true },
      });

      await checkSegmentNameUnique('固定码001');

      expect(http.get).toHaveBeenCalledWith(
        '/api/v1/model-design/segment/check-name',
        expect.objectContaining({ params: { name: '固定码001' } }),
      );
    });
  });

  describe('getSegmentReferenceInfo', () => {
    it('应使用 GET 请求获取引用信息', async () => {
      const { getSegmentReferenceInfo } = await import('./segment');

      (http.get as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: { success: true, data: { ruleCount: 2, ruleNames: ['规则1', '规则2'] } },
      });

      await getSegmentReferenceInfo('segment-id');

      expect(http.get).toHaveBeenCalledWith(
        '/api/v1/model-design/segment/segment-id/references',
      );
    });
  });
});
