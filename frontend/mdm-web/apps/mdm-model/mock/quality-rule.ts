/**
 * mock/quality-rule.ts - 质量规则 Mock 数据 + HTTP handlers
 */
import type { MockHandler } from '@mdm/config-vite/mock';
import { basePath, paginate, parseBody, parseQuery, sendError, sendJson, matchesKeyword } from './_helpers';

interface MockQualityRule {
  id: string; modelId: string; name: string; ruleType: string;
  description: string; conditions: any[]; status: string;
  checkTiming: string[]; dataStorage: boolean; sortOrder: number;
  ruleTypeLabel: string; statusLabel: string; attributeNames: string[];
  createdAt: string; updatedAt: string; createdBy: string; updatedBy: string;
}

const makeDate = (daysAgo: number): string =>
  new Date(Date.now() - daysAgo * 86_400_000).toISOString().replace('T', ' ').substring(0, 19);

const TYPE_LABEL: Record<string, string> = {
  notNull: '非空校验', unique: '唯一性校验', format: '格式校验',
  range: '范围校验', regex: '正则校验', custom: '自定义校验', crossTable: '跨表校验',
};

export const mockQualityRules: MockQualityRule[] = [
  {
    id: 'qr-001', modelId: 'model-012', name: '姓名非空校验', ruleType: 'notNull',
    description: '姓名不能为空', conditions: [{ attributeId: 'attr-001', attributeName: '姓名', ruleType: 'notNull', description: '姓名不能为空' }],
    status: 'enabled', checkTiming: ['onSave', 'onImport'], dataStorage: true, sortOrder: 1,
    ruleTypeLabel: '非空校验', statusLabel: '启用', attributeNames: ['姓名'],
    createdAt: makeDate(12), updatedAt: makeDate(5), createdBy: 'user-001', updatedBy: 'user-001',
  },
  {
    id: 'qr-002', modelId: 'model-012', name: '工号唯一性校验', ruleType: 'unique',
    description: '工号不能重复', conditions: [{ attributeId: 'attr-002', attributeName: '工号', ruleType: 'unique', description: '工号不能重复' }],
    status: 'enabled', checkTiming: ['onSave'], dataStorage: true, sortOrder: 2,
    ruleTypeLabel: '唯一性校验', statusLabel: '启用', attributeNames: ['工号'],
    createdAt: makeDate(10), updatedAt: makeDate(3), createdBy: 'user-001', updatedBy: 'user-001',
  },
  {
    id: 'qr-003', modelId: 'model-012', name: '手机号格式校验', ruleType: 'regex',
    description: '手机号必须为11位数字', conditions: [{ attributeId: 'attr-003', attributeName: '手机号', ruleType: 'regex', description: '手机号格式不正确', params: { pattern: '^1[3-9]\\d{9}$' } }],
    status: 'enabled', checkTiming: ['onSave', 'onSubmit'], dataStorage: false, sortOrder: 3,
    ruleTypeLabel: '正则校验', statusLabel: '启用', attributeNames: ['手机号'],
    createdAt: makeDate(8), updatedAt: makeDate(2), createdBy: 'user-001', updatedBy: 'user-001',
  },
  {
    id: 'qr-004', modelId: 'model-012', name: '邮箱格式校验', ruleType: 'format',
    description: '邮箱必须符合标准格式', conditions: [{ attributeId: 'attr-004', attributeName: '邮箱', ruleType: 'format', description: '邮箱格式不正确' }],
    status: 'disabled', checkTiming: ['onSave'], dataStorage: false, sortOrder: 4,
    ruleTypeLabel: '格式校验', statusLabel: '停用', attributeNames: ['邮箱'],
    createdAt: makeDate(6), updatedAt: makeDate(1), createdBy: 'user-001', updatedBy: 'user-001',
  },
];

let idCounter = 100;
function nextId() { return `qr-${String(++idCounter).padStart(3, '0')}`; }

const PREFIX = '/api/v1/model-design/quality-rule';
const RE_LIST = new RegExp(`^${PREFIX}$`);
const RE_DETAIL = new RegExp(`^${PREFIX}/([^/]+)$`);
const RE_BATCH_DELETE = new RegExp(`^${PREFIX}/batch-delete$`);
const RE_ENABLE = new RegExp(`^${PREFIX}/([^/]+)/(enable|disable)$`);
const RE_DATA_STORAGE = new RegExp(`^${PREFIX}/([^/]+)/data-storage$`);

export const mockQualityRuleHandlers: MockHandler[] = [
  {
    method: 'GET', pattern: RE_LIST,
    handler: async (req: any, res: any) => {
      const q = parseQuery(req.url ?? '');
      const page = parseInt(q.page ?? '1', 10);
      const pageSize = parseInt(q.pageSize ?? '20', 10);
      const modelId = q.modelId ?? '';
      let list = [...mockQualityRules];
      if (modelId) list = list.filter(r => r.modelId === modelId);
      if (q.keyword) list = list.filter(r => matchesKeyword(r as any, ['name'], q.keyword));
      if (q.ruleType) list = list.filter(r => r.ruleType === q.ruleType);
      sendJson(res, paginate(list, page, pageSize));
    },
  },
  {
    method: 'GET', pattern: RE_DETAIL,
    handler: async (req: any, res: any) => {
      const id = basePath(req.url ?? '').match(RE_DETAIL)?.[1];
      const item = mockQualityRules.find(r => r.id === id);
      if (!item) return sendError(res, '规则不存在', 404);
      sendJson(res, item);
    },
  },
  {
    method: 'POST', pattern: RE_LIST,
    handler: async (req: any, res: any) => {
      const body = await parseBody<any>(req);
      const id = nextId();
      const newItem: MockQualityRule = {
        ...body, id,
        ruleTypeLabel: TYPE_LABEL[body.ruleType] || body.ruleType,
        statusLabel: '启用', status: 'enabled',
        attributeNames: body.conditions?.map((c: any) => c.attributeName).filter(Boolean) ?? [],
        createdAt: new Date().toISOString().replace('T', ' ').slice(0, 19),
        updatedAt: new Date().toISOString().replace('T', ' ').slice(0, 19),
        createdBy: 'user-001', updatedBy: 'user-001',
      };
      mockQualityRules.push(newItem);
      sendJson(res, newItem);
    },
  },
  {
    method: 'PUT', pattern: RE_DETAIL,
    handler: async (req: any, res: any) => {
      const id = basePath(req.url ?? '').match(RE_DETAIL)?.[1];
      const idx = mockQualityRules.findIndex(r => r.id === id);
      if (idx < 0) return sendError(res, '规则不存在', 404);
      const body = await parseBody<any>(req);
      mockQualityRules[idx] = { ...mockQualityRules[idx], ...body, id: mockQualityRules[idx].id };
      sendJson(res, mockQualityRules[idx]);
    },
  },
  {
    method: 'DELETE', pattern: RE_DETAIL,
    handler: async (req: any, res: any) => {
      const id = basePath(req.url ?? '').match(RE_DETAIL)?.[1];
      const idx = mockQualityRules.findIndex(r => r.id === id);
      if (idx < 0) return sendError(res, '规则不存在', 404);
      mockQualityRules.splice(idx, 1);
      sendJson(res, null);
    },
  },
  {
    method: 'POST', pattern: RE_BATCH_DELETE,
    handler: async (req: any, res: any) => {
      const body = await parseBody<{ ids: string[] }>(req);
      for (const id of body.ids ?? []) {
        const idx = mockQualityRules.findIndex(r => r.id === id);
        if (idx >= 0) mockQualityRules.splice(idx, 1);
      }
      sendJson(res, null);
    },
  },
  {
    method: 'PUT', pattern: RE_ENABLE,
    handler: async (req: any, res: any) => {
      const m = basePath(req.url ?? '').match(RE_ENABLE);
      const id = m?.[1]; const action = m?.[2];
      const item = mockQualityRules.find(r => r.id === id);
      if (!item) return sendError(res, '规则不存在', 404);
      item.status = action === 'enable' ? 'enabled' : 'disabled';
      item.statusLabel = item.status === 'enabled' ? '启用' : '停用';
      sendJson(res, null);
    },
  },
  {
    method: 'PUT', pattern: RE_DATA_STORAGE,
    handler: async (req: any, res: any) => {
      const id = basePath(req.url ?? '').match(RE_DATA_STORAGE)?.[1];
      const item = mockQualityRules.find(r => r.id === id);
      if (!item) return sendError(res, '规则不存在', 404);
      const body = await parseBody<{ dataStorage: boolean }>(req);
      item.dataStorage = body.dataStorage;
      sendJson(res, null);
    },
  },
];
