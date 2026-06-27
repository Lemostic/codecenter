/**
 * mock/coding-rule.ts - 编码规则 Mock 数据 + HTTP handlers
 */
import type { MockHandler } from '@mdm/config-vite/mock';
import { basePath, paginate, parseBody, parseQuery, sendError, sendJson } from './_helpers';

interface MockCodingRule {
  id: string; modelId: string; name: string; prefix: string;
  segments: any[]; sampleCode: string; targetAttributeId: string;
  targetAttributeName: string; segmentCount: number; sortOrder: number;
  createdAt: string; updatedAt: string; createdBy: string; updatedBy: string;
}

const makeDate = (daysAgo: number): string =>
  new Date(Date.now() - daysAgo * 86_400_000).toISOString().replace('T', ' ').substring(0, 19);

export const mockCodingRules: MockCodingRule[] = [
  {
    id: 'cr-001', modelId: 'model-012', name: '工号编码规则', prefix: 'EMP',
    segments: [
      { id: 'seg-1', type: 'fixed', value: 'EMP', sortOrder: 1 },
      { id: 'seg-2', type: 'date', format: 'yyyyMMdd', sortOrder: 2 },
      { id: 'seg-3', type: 'serial', startValue: 1, step: 1, digits: 4, resetCycle: 'daily', sortOrder: 3 },
    ],
    sampleCode: 'EMP20250101001', targetAttributeId: 'attr-002', targetAttributeName: '工号',
    segmentCount: 3, sortOrder: 1,
    createdAt: makeDate(15), updatedAt: makeDate(5), createdBy: 'user-001', updatedBy: 'user-001',
  },
  {
    id: 'cr-002', modelId: 'model-022', name: '订单编号编码规则', prefix: 'ORD',
    segments: [
      { id: 'seg-4', type: 'fixed', value: 'ORD', sortOrder: 1 },
      { id: 'seg-5', type: 'serial', startValue: 1, step: 1, digits: 6, resetCycle: 'yearly', sortOrder: 2 },
    ],
    sampleCode: 'ORD000001', targetAttributeId: 'attr-019', targetAttributeName: '订单编号',
    segmentCount: 2, sortOrder: 1,
    createdAt: makeDate(10), updatedAt: makeDate(3), createdBy: 'user-001', updatedBy: 'user-001',
  },
];

let idCounter = 100;
function nextId() { return `cr-${String(++idCounter).padStart(3, '0')}`; }

const PREFIX = '/api/v1/model-design/coding-rule';
const RE_LIST = new RegExp(`^${PREFIX}$`);
const RE_DETAIL = new RegExp(`^${PREFIX}/([^/]+)$`);

export const mockCodingRuleHandlers: MockHandler[] = [
  {
    method: 'GET', pattern: RE_LIST,
    handler: async (req, res) => {
      const q = parseQuery(req.url ?? '');
      const page = parseInt(q.page ?? '1', 10);
      const pageSize = parseInt(q.pageSize ?? '20', 10);
      const modelId = q.modelId ?? '';
      let list = [...mockCodingRules];
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
      const item = mockCodingRules.find(r => r.id === id);
      if (!item) return sendError(res, '规则不存在', 404);
      sendJson(res, item);
    },
  },
  {
    method: 'POST', pattern: RE_LIST,
    handler: async (req, res) => {
      const body = await parseBody<any>(req);
      const id = nextId();
      const newItem: MockCodingRule = {
        ...body, id, sampleCode: body.prefix || '***',
        segmentCount: body.segments?.length ?? 0,
        createdAt: new Date().toISOString().replace('T', ' ').slice(0, 19),
        updatedAt: new Date().toISOString().replace('T', ' ').slice(0, 19),
        createdBy: 'user-001', updatedBy: 'user-001',
      };
      mockCodingRules.push(newItem);
      sendJson(res, newItem);
    },
  },
  {
    method: 'PUT', pattern: RE_DETAIL,
    handler: async (req, res) => {
      const id = basePath(req.url ?? '').match(RE_DETAIL)?.[1];
      const idx = mockCodingRules.findIndex(r => r.id === id);
      if (idx < 0) return sendError(res, '规则不存在', 404);
      const body = await parseBody<any>(req);
      mockCodingRules[idx] = { ...mockCodingRules[idx], ...body, id: mockCodingRules[idx].id };
      sendJson(res, mockCodingRules[idx]);
    },
  },
  {
    method: 'DELETE', pattern: RE_DETAIL,
    handler: async (req, res) => {
      const id = basePath(req.url ?? '').match(RE_DETAIL)?.[1];
      const idx = mockCodingRules.findIndex(r => r.id === id);
      if (idx < 0) return sendError(res, '规则不存在', 404);
      mockCodingRules.splice(idx, 1);
      sendJson(res, null);
    },
  },
];
