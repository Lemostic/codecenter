/**
 * coding-rule API 单元测试
 *
 * 测试编码规则 API 函数的请求参数和响应格式
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

describe('coding-rule API', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('listCodingRule', () => {
    it('应使用 GET 请求', async () => {
      const { listCodingRule } = await import('./coding-rule');

      (http.get as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: { success: true, data: { rows: [], total: 0 } },
      });

      await listCodingRule({ modelId: 'test-id', page: 1, pageSize: 20 });

      expect(http.get).toHaveBeenCalledWith(
        '/api/v1/model-design/coding-rule',
        expect.objectContaining({ params: expect.any(Object) }),
      );
    });

    it('应传递查询参数', async () => {
      const { listCodingRule } = await import('./coding-rule');

      (http.get as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: { success: true, data: { rows: [], total: 0 } },
      });

      await listCodingRule({
        modelId: 'test-id',
        keyword: 'test',
        page: 2,
        pageSize: 10,
      });

      expect(http.get).toHaveBeenCalledWith(
        '/api/v1/model-design/coding-rule',
        expect.objectContaining({
          params: expect.objectContaining({
            modelId: 'test-id',
            keyword: 'test',
            page: 2,
            pageSize: 10,
          }),
        }),
      );
    });
  });

  describe('getCodingRule', () => {
    it('应使用正确的 ID 调用 GET 请求', async () => {
      const { getCodingRule } = await import('./coding-rule');

      (http.get as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: { success: true, data: {} },
      });

      await getCodingRule('rule-id');

      expect(http.get).toHaveBeenCalledWith(
        '/api/v1/model-design/coding-rule/rule-id',
      );
    });
  });

  describe('createCodingRule', () => {
    it('应使用 POST 请求', async () => {
      const { createCodingRule } = await import('./coding-rule');

      (http.post as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: { success: true, data: {} },
      });

      await createCodingRule({
        modelId: 'test-id',
        attributeId: 'attr-id',
        name: '测试规则',
        ruleDefinitionType: 'segment',
        generationTiming: 'button',
      });

      expect(http.post).toHaveBeenCalledWith(
        '/api/v1/model-design/coding-rule',
        expect.objectContaining({
          modelId: 'test-id',
          attributeId: 'attr-id',
          name: '测试规则',
          ruleDefinitionType: 'segment',
          generationTiming: 'button',
        }),
      );
    });
  });

  describe('updateCodingRule', () => {
    it('应使用 PUT 请求并包含 ID', async () => {
      const { updateCodingRule } = await import('./coding-rule');

      (http.put as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: { success: true, data: {} },
      });

      await updateCodingRule({
        id: 'rule-id',
        name: '更新后的规则',
      });

      expect(http.put).toHaveBeenCalledWith(
        '/api/v1/model-design/coding-rule/rule-id',
        expect.objectContaining({
          id: 'rule-id',
          name: '更新后的规则',
        }),
      );
    });
  });

  describe('deleteCodingRule', () => {
    it('应使用 DELETE 请求', async () => {
      const { deleteCodingRule } = await import('./coding-rule');

      (http.delete as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: { success: true },
      });

      await deleteCodingRule('rule-id');

      expect(http.delete).toHaveBeenCalledWith(
        '/api/v1/model-design/coding-rule/rule-id',
      );
    });
  });

  describe('activateCodingRule', () => {
    it('应使用 POST 请求激活规则', async () => {
      const { activateCodingRule } = await import('./coding-rule');

      (http.post as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: { success: true },
      });

      await activateCodingRule('rule-id');

      expect(http.post).toHaveBeenCalledWith(
        '/api/v1/model-design/coding-rule/rule-id/activate',
      );
    });
  });

  describe('disableCodingRule', () => {
    it('应使用 POST 请求停用规则', async () => {
      const { disableCodingRule } = await import('./coding-rule');

      (http.post as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: { success: true },
      });

      await disableCodingRule('rule-id');

      expect(http.post).toHaveBeenCalledWith(
        '/api/v1/model-design/coding-rule/rule-id/disable',
      );
    });
  });

  describe('enableCodingRule', () => {
    it('应使用 POST 请求启用规则', async () => {
      const { enableCodingRule } = await import('./coding-rule');

      (http.post as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: { success: true },
      });

      await enableCodingRule('rule-id');

      expect(http.post).toHaveBeenCalledWith(
        '/api/v1/model-design/coding-rule/rule-id/enable',
      );
    });
  });

  describe('reviseCodingRule', () => {
    it('应使用 POST 请求修订规则', async () => {
      const { reviseCodingRule } = await import('./coding-rule');

      (http.post as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: { success: true, data: {} },
      });

      await reviseCodingRule('rule-id');

      expect(http.post).toHaveBeenCalledWith(
        '/api/v1/model-design/coding-rule/rule-id/revise',
      );
    });
  });

  describe('validateGroovyScript', () => {
    it('应使用 POST 请求校验脚本', async () => {
      const { validateGroovyScript } = await import('./coding-rule');

      (http.post as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: { success: true, data: { valid: true, errors: [] } },
      });

      await validateGroovyScript('def test = "hello"');

      expect(http.post).toHaveBeenCalledWith(
        '/api/v1/model-design/coding-rule/validate-script',
        { script: 'def test = "hello"' },
      );
    });
  });

  describe('generateSampleCode', () => {
    it('应使用 POST 请求生成示例编码', async () => {
      const { generateSampleCode } = await import('./coding-rule');

      (http.post as ReturnType<typeof vi.fn>).mockResolvedValue({
        data: { success: true, data: 'PREFIX001' },
      });

      await generateSampleCode([{ type: 'fixed', sortOrder: 1, value: 'PREFIX' }]);

      expect(http.post).toHaveBeenCalledWith(
        '/api/v1/model-design/coding-rule/sample',
        expect.objectContaining({
          segments: expect.any(Array),
        }),
      );
    });
  });
});
