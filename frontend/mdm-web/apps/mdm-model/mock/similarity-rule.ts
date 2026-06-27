/**
 * mock/similarity-rule.ts
 *
 * 相似规则 Mock 数据 + HTTP handlers
 */

interface MockSimilarityRule {
  id: string;
  modelId: string;
  name: string;
  combinationMode: 'weighted' | 'composite';
  threshold: number;
  checkOnSave: boolean;
  attributeWeights: { attributeId: string; attributeName: string; algorithm: string; weight: number }[];
  sortOrder: number;
  combinationModeLabel: string;
  attributeCount: number;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  updatedBy: string;
}

const makeDate = (daysAgo: number): string =>
  new Date(Date.now() - daysAgo * 86_400_000).toISOString().replace('T', ' ').substring(0, 19);

const rawRules: Omit<MockSimilarityRule, 'attributeCount'>[] = [
  {
    id: 'sr-001', modelId: 'model-012', name: '人员相似检查', combinationMode: 'weighted',
    threshold: 80, checkOnSave: true, sortOrder: 1, combinationModeLabel: '多属性加权平均',
    attributeWeights: [
      { attributeId: 'attr-001', attributeName: '姓名', algorithm: 'editDistance', weight: 40 },
      { attributeId: 'attr-005', attributeName: '身份证号', algorithm: 'jaccard', weight: 60 },
    ],
    createdAt: makeDate(10), updatedAt: makeDate(5), createdBy: 'user-001', updatedBy: 'user-001',
  },
  {
    id: 'sr-002', modelId: 'model-012', name: '手机号重复检查', combinationMode: 'weighted',
    threshold: 90, checkOnSave: false, sortOrder: 2, combinationModeLabel: '多属性加权平均',
    attributeWeights: [
      { attributeId: 'attr-003', attributeName: '手机号', algorithm: 'cosine', weight: 100 },
    ],
    createdAt: makeDate(8), updatedAt: makeDate(3), createdBy: 'user-001', updatedBy: 'user-001',
  },
  {
    id: 'sr-003', modelId: 'model-022', name: '订单相似检查', combinationMode: 'composite',
    threshold: 75, checkOnSave: true, sortOrder: 1, combinationModeLabel: '多属性组合相似',
    attributeWeights: [
      { attributeId: 'attr-019', attributeName: '客户名称', algorithm: 'editDistance', weight: 50 },
      { attributeId: 'attr-020', attributeName: '订单编号', algorithm: 'jaccard', weight: 50 },
    ],
    createdAt: makeDate(5), updatedAt: makeDate(2), createdBy: 'user-001', updatedBy: 'user-001',
  },
];

export const mockSimilarityRules: MockSimilarityRule[] = rawRules.map(r => ({
  ...r,
  attributeCount: r.attributeWeights.length,
}));

let idCounter = 100;
function nextId() { return `sr-${String(++idCounter).padStart(3, '0')}`; }

// ========== Mock HTTP Handlers ==========
import type { MockHandler } from '@mdm/config-vite/mock';
import { basePath, paginate, parseBody, parseQuery, sendError, sendJson } from './_helpers';

const PREFIX = '/api/v1/model-design/similarity-rule';
const RE_LIST = new RegExp(`^${PREFIX}$`);
const RE_DETAIL = new RegExp(`^${PREFIX}/([^/]+)$`);

export const mockSimilarityRuleHandlers: MockHandler[] = [
  {
    method: 'GET', pattern: RE_LIST,
    handler: async (req, res) => {
      const q = parseQuery(req.url ?? '');
      const page = parseInt(q.page ?? '1', 10);
      const pageSize = parseInt(q.pageSize ?? '20', 10);
      const modelId = q.modelId ?? '';
      let list = [...mockSimilarityRules];
      if (modelId) list = list.filter(r => r.modelId === modelId);
      if (q.keyword) {
        const kw = q.keyword.toLowerCase();
        list = list.filter(r => r.name.toLowerCase().includes(kw));
      }
      sendJson(res, paginate(list, page, pageSize));
    },
  },
  {
    method: 'GET', pattern: RE_DETAIL,
    handler: async (req, res) => {
      const id = basePath(req.url ?? '').match(RE_DETAIL)?.[1];
      const item = mockSimilarityRules.find(r => r.id === id);
      if (!item) return sendError(res, '规则不存在', 404);
      sendJson(res, item);
    },
  },
  {
    method: 'POST', pattern: RE_LIST,
    handler: async (req, res) => {
      const body = await parseBody<any>(req);
      const id = nextId();
      const newItem: MockSimilarityRule = {
        ...body, id,
        combinationModeLabel: body.combinationMode === 'weighted' ? '多属性加权平均' : '多属性组合相似',
        attributeCount: body.attributeWeights?.length ?? 0,
        createdAt: new Date().toISOString().replace('T', ' ').slice(0, 19),
        updatedAt: new Date().toISOString().replace('T', ' ').slice(0, 19),
        createdBy: 'user-001', updatedBy: 'user-001',
      };
      mockSimilarityRules.push(newItem);
      sendJson(res, newItem);
    },
  },
  {
    method: 'PUT', pattern: RE_DETAIL,
    handler: async (req, res) => {
      const id = basePath(req.url ?? '').match(RE_DETAIL)?.[1];
      const idx = mockSimilarityRules.findIndex(r => r.id === id);
      if (idx < 0) return sendError(res, '规则不存在', 404);
      const body = await parseBody<any>(req);
      mockSimilarityRules[idx] = { ...mockSimilarityRules[idx], ...body, id: mockSimilarityRules[idx].id };
      sendJson(res, mockSimilarityRules[idx]);
    },
  },
  {
    method: 'DELETE', pattern: RE_DETAIL,
    handler: async (req, res) => {
      const id = basePath(req.url ?? '').match(RE_DETAIL)?.[1];
      const idx = mockSimilarityRules.findIndex(r => r.id === id);
      if (idx < 0) return sendError(res, '规则不存在', 404);
      mockSimilarityRules.splice(idx, 1);
      sendJson(res, null);
    },
  },
];
