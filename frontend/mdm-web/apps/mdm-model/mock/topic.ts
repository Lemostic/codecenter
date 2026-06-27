/**
 * mock/topic.ts
 *
 * 主题域 Mock 数据
 * 包含 3 级主题域分类树结构
 */

interface MockTopic {
  id: string;
  name: string;
  parentId: string | null;
  sortOrder: number;
  description: string;
  isLeaf: boolean;
  level: number;
  hasModel: boolean;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  updatedBy: string;
}

const makeDate = (daysAgo: number): string =>
  new Date(Date.now() - daysAgo * 86_400_000).toISOString().replace('T', ' ').substring(0, 19);

/** 主题域数据（3 级树） */
export const mockTopics: MockTopic[] = [
  // ===== 一级分类 =====
  { id: 'topic-001', name: '常用主数据',     parentId: null,       sortOrder: 1, description: '日常业务中最常使用的主数据分类', isLeaf: false, level: 0, hasModel: false, createdAt: makeDate(90), updatedAt: makeDate(30), createdBy: 'user-001', updatedBy: 'user-001' },
  { id: 'topic-002', name: '标准件',         parentId: null,       sortOrder: 2, description: '标准件相关主数据', isLeaf: false, level: 0, hasModel: false, createdAt: makeDate(85), updatedAt: makeDate(25), createdBy: 'user-001', updatedBy: 'user-001' },
  { id: 'topic-100', name: '组织与人员',     parentId: null,       sortOrder: 3, description: '组织架构及人员管理相关主数据', isLeaf: false, level: 0, hasModel: false, createdAt: makeDate(80), updatedAt: makeDate(20), createdBy: 'user-001', updatedBy: 'user-001' },
  { id: 'topic-200', name: '财务主数据',     parentId: null,       sortOrder: 4, description: '财务管理相关主数据', isLeaf: false, level: 0, hasModel: false, createdAt: makeDate(75), updatedAt: makeDate(15), createdBy: 'user-001', updatedBy: 'user-001' },
  { id: 'topic-300', name: '供应链',         parentId: null,       sortOrder: 5, description: '供应链相关主数据', isLeaf: false, level: 0, hasModel: false, createdAt: makeDate(70), updatedAt: makeDate(10), createdBy: 'user-001', updatedBy: 'user-001' },

  // ===== 二级分类（常用主数据） =====
  { id: 'topic-003', name: '物料主数据',     parentId: 'topic-001', sortOrder: 1, description: '物料相关', isLeaf: true, level: 1, hasModel: true, createdAt: makeDate(60), updatedAt: makeDate(20), createdBy: 'user-001', updatedBy: 'user-001' },
  { id: 'topic-010', name: '供应商主数据',   parentId: 'topic-001', sortOrder: 2, description: '供应商相关', isLeaf: true, level: 1, hasModel: true, createdAt: makeDate(58), updatedAt: makeDate(18), createdBy: 'user-001', updatedBy: 'user-001' },
  { id: 'topic-011', name: '客户主数据',     parentId: 'topic-001', sortOrder: 3, description: '客户相关', isLeaf: true, level: 1, hasModel: true, createdAt: makeDate(55), updatedAt: makeDate(15), createdBy: 'user-001', updatedBy: 'user-001' },

  // ===== 二级分类（标准件） =====
  { id: 'topic-023', name: '标准件信息',     parentId: 'topic-002', sortOrder: 1, description: '标准件基础信息', isLeaf: true, level: 1, hasModel: true, createdAt: makeDate(50), updatedAt: makeDate(10), createdBy: 'user-001', updatedBy: 'user-001' },
  { id: 'topic-024', name: '复合模型',       parentId: 'topic-002', sortOrder: 2, description: '复合结构模型', isLeaf: true, level: 1, hasModel: true, createdAt: makeDate(48), updatedAt: makeDate(8), createdBy: 'user-001', updatedBy: 'user-001' },

  // ===== 三级分类（常用主数据 → 物料主数据 的子分类，但物料主数据 isLeaf=true 所以不会有子分类，这里做演示用） =====
  { id: 'topic-004', name: '金属材料',       parentId: 'topic-003', sortOrder: 1, description: '金属材料分类', isLeaf: true, level: 2, hasModel: true, createdAt: makeDate(45), updatedAt: makeDate(5), createdBy: 'user-001', updatedBy: 'user-001' },
  { id: 'topic-005', name: '非金属材料',     parentId: 'topic-003', sortOrder: 2, description: '非金属材料分类', isLeaf: true, level: 2, hasModel: true, createdAt: makeDate(43), updatedAt: makeDate(4), createdBy: 'user-001', updatedBy: 'user-001' },
  { id: 'topic-006', name: '橡胶',           parentId: 'topic-003', sortOrder: 3, description: '橡胶材料分类', isLeaf: true, level: 2, hasModel: true, createdAt: makeDate(40), updatedAt: makeDate(3), createdBy: 'user-001', updatedBy: 'user-001' },
  { id: 'topic-007', name: '涂料',           parentId: 'topic-003', sortOrder: 4, description: '涂料分类', isLeaf: true, level: 2, hasModel: true, createdAt: makeDate(38), updatedAt: makeDate(2), createdBy: 'user-001', updatedBy: 'user-001' },
  { id: 'topic-008', name: '毛坯',           parentId: 'topic-003', sortOrder: 5, description: '毛坯件分类', isLeaf: true, level: 2, hasModel: true, createdAt: makeDate(35), updatedAt: makeDate(1), createdBy: 'user-001', updatedBy: 'user-001' },
  { id: 'topic-009', name: '自制件',         parentId: 'topic-003', sortOrder: 6, description: '自制件分类', isLeaf: true, level: 2, hasModel: true, createdAt: makeDate(33), updatedAt: makeDate(1), createdBy: 'user-001', updatedBy: 'user-001' },

  // ===== 二级分类（组织与人员） =====
  { id: 'topic-012', name: '组织主数据',     parentId: 'topic-100', sortOrder: 1, description: '组织架构', isLeaf: true, level: 1, hasModel: true, createdAt: makeDate(30), updatedAt: makeDate(5), createdBy: 'user-001', updatedBy: 'user-001' },
  { id: 'topic-013', name: '人员主数据',     parentId: 'topic-100', sortOrder: 2, description: '人员信息', isLeaf: true, level: 1, hasModel: true, createdAt: makeDate(28), updatedAt: makeDate(4), createdBy: 'user-001', updatedBy: 'user-001' },

  // ===== 二级分类（财务主数据） =====
  { id: 'topic-014', name: '会计科目',       parentId: 'topic-200', sortOrder: 1, description: '会计科目体系', isLeaf: true, level: 1, hasModel: true, createdAt: makeDate(25), updatedAt: makeDate(3), createdBy: 'user-001', updatedBy: 'user-001' },
  { id: 'topic-022', name: '币种主数据',     parentId: 'topic-200', sortOrder: 2, description: '币种及汇率', isLeaf: true, level: 1, hasModel: true, createdAt: makeDate(23), updatedAt: makeDate(2), createdBy: 'user-001', updatedBy: 'user-001' },
  { id: 'topic-025', name: '税率配置',       parentId: 'topic-200', sortOrder: 3, description: '税率及免税政策', isLeaf: true, level: 1, hasModel: true, createdAt: makeDate(20), updatedAt: makeDate(1), createdBy: 'user-001', updatedBy: 'user-001' },

  // ===== 二级分类（供应链） =====
  { id: 'topic-015', name: '仓库主数据',     parentId: 'topic-300', sortOrder: 1, description: '仓库信息', isLeaf: true, level: 1, hasModel: true, createdAt: makeDate(18), updatedAt: makeDate(1), createdBy: 'user-001', updatedBy: 'user-001' },
  { id: 'topic-016', name: '品牌信息',       parentId: 'topic-300', sortOrder: 2, description: '品牌及授权', isLeaf: true, level: 1, hasModel: true, createdAt: makeDate(16), updatedAt: makeDate(1), createdBy: 'user-001', updatedBy: 'user-001' },
  { id: 'topic-017', name: '品类定义',       parentId: 'topic-300', sortOrder: 3, description: '品类结构', isLeaf: true, level: 1, hasModel: true, createdAt: makeDate(14), updatedAt: makeDate(1), createdBy: 'user-001', updatedBy: 'user-001' },
  { id: 'topic-018', name: '计量单位',       parentId: 'topic-300', sortOrder: 4, description: '计量单位', isLeaf: true, level: 1, hasModel: true, createdAt: makeDate(12), updatedAt: makeDate(1), createdBy: 'user-001', updatedBy: 'user-001' },
  { id: 'topic-019', name: '价格策略',       parentId: 'topic-300', sortOrder: 5, description: '定价策略', isLeaf: true, level: 1, hasModel: true, createdAt: makeDate(10), updatedAt: makeDate(1), createdBy: 'user-001', updatedBy: 'user-001' },
  { id: 'topic-020', name: '渠道信息',       parentId: 'topic-300', sortOrder: 6, description: '销售渠道', isLeaf: true, level: 1, hasModel: true, createdAt: makeDate(8), updatedAt: makeDate(1), createdBy: 'user-001', updatedBy: 'user-001' },
  { id: 'topic-021', name: '区域定义',       parentId: 'topic-300', sortOrder: 7, description: '行政区域', isLeaf: true, level: 1, hasModel: true, createdAt: makeDate(6), updatedAt: makeDate(1), createdBy: 'user-001', updatedBy: 'user-001' },
];

/** 获取下一个主题 ID */
export function nextTopicId(): string {
  const maxNum = mockTopics.reduce((max, t) => {
    const n = parseInt(t.id.replace('topic-', ''), 10);
    return n > max ? n : max;
  }, 0);
  return `topic-${String(maxNum + 1).padStart(3, '0')}`;
}

/** 转为树节点 */
export function toTopicTreeNode(t: MockTopic) {
  return { id: t.id, name: t.name, isLeaf: t.isLeaf, parentId: t.parentId };
}

/** 转为视图对象 */
export function toTopicVO(t: MockTopic) {
  return { ...t };
}

// ========== Mock HTTP Handlers ==========
import type { MockHandler } from '@mdm/config-vite/mock';
import {
  basePath,
  matchesKeyword,
  paginate,
  parseBody,
  parseQuery,
  sendError,
  sendJson,
} from './_helpers';

const PREFIX = '/api/v1/model-design/topic';
const RE_LIST = new RegExp(`^${PREFIX}$`);
const RE_DETAIL = new RegExp(`^${PREFIX}/([^/]+)$`);
const RE_TREE_ROOT = new RegExp(`^${PREFIX}/tree/root$`);
const RE_TREE_FULL = new RegExp(`^${PREFIX}/tree/full$`);
const RE_TREE_CHILDREN = new RegExp(`^${PREFIX}/tree/([^/]+)/children$`);
const RE_CHECK_NAME = new RegExp(`^${PREFIX}/check-name$`);

export const mockTopicHandlers: MockHandler[] = [
  // 列表（分页）
  {
    method: 'GET',
    pattern: RE_LIST,
    handler: async (req, res) => {
      const q = parseQuery(req.url ?? '');
      const page = parseInt(q.page ?? '1', 10);
      const pageSize = parseInt(q.pageSize ?? '20', 10);
      const keyword = q.keyword ?? '';
      const parentId = q.parentId ?? '';
      let list = [...mockTopics];
      if (keyword) list = list.filter((t) => matchesKeyword(t as never, ['name'], keyword));
      if (parentId !== '') list = list.filter((t) => t.parentId === parentId);
      sendJson(res, paginate(list, page, pageSize));
    },
  },
  // 详情
  {
    method: 'GET',
    pattern: RE_DETAIL,
    handler: async (req, res) => {
      const id = basePath(req.url ?? '').match(RE_DETAIL)?.[1];
      const item = mockTopics.find((t) => t.id === id);
      if (!item) return sendError(res, '主题不存在', 404);
      sendJson(res, item);
    },
  },
  // 新增
  {
    method: 'POST',
    pattern: RE_LIST,
    handler: async (req, res) => {
      const body = await parseBody<MockTopic>(req);
      const id = nextTopicId();
      const newItem: MockTopic = {
        ...body,
        id,
        createdAt: new Date().toISOString().replace('T', ' ').slice(0, 19),
        updatedAt: new Date().toISOString().replace('T', ' ').slice(0, 19),
        createdBy: 'user-001',
        updatedBy: 'user-001',
      };
      mockTopics.push(newItem);
      sendJson(res, newItem);
    },
  },
  // 更新
  {
    method: 'PUT',
    pattern: RE_DETAIL,
    handler: async (req, res) => {
      const id = basePath(req.url ?? '').match(RE_DETAIL)?.[1];
      const idx = mockTopics.findIndex((t) => t.id === id);
      if (idx < 0) return sendError(res, '主题不存在', 404);
      const body = await parseBody<Partial<MockTopic>>(req);
      mockTopics[idx] = {
        ...mockTopics[idx],
        ...body,
        id: mockTopics[idx].id,
        updatedAt: new Date().toISOString().replace('T', ' ').slice(0, 19),
        updatedBy: 'user-001',
      };
      sendJson(res, mockTopics[idx]);
    },
  },
  // 删除（支持批量）
  {
    method: 'DELETE',
    pattern: RE_LIST,
    handler: async (req, res) => {
      const body = await parseBody<{ ids: string[] }>(req);
      const ids = body.ids ?? [];
      for (const id of ids) {
        const idx = mockTopics.findIndex((t) => t.id === id);
        if (idx >= 0) mockTopics.splice(idx, 1);
      }
      sendJson(res, null);
    },
  },
  // 树根节点（懒加载第一层）
  {
    method: 'GET',
    pattern: RE_TREE_ROOT,
    handler: async (_req, res) => {
      const list = mockTopics
        .filter((t) => t.parentId === null)
        .map((t) => ({ id: t.id, name: t.name, isLeaf: t.isLeaf }));
      sendJson(res, list);
    },
  },
  // 树子节点
  {
    method: 'GET',
    pattern: RE_TREE_CHILDREN,
    handler: async (req, res) => {
      const parentId = basePath(req.url ?? '').match(RE_TREE_CHILDREN)?.[1];
      const list = mockTopics
        .filter((t) => t.parentId === parentId)
        .map((t) => ({ id: t.id, name: t.name, isLeaf: t.isLeaf }));
      sendJson(res, list);
    },
  },
  // 完整树
  {
    method: 'GET',
    pattern: RE_TREE_FULL,
    handler: async (_req, res) => {
      sendJson(res, mockTopics);
    },
  },
  // 名称唯一性
  {
    method: 'GET',
    pattern: RE_CHECK_NAME,
    handler: async (req, res) => {
      const q = parseQuery(req.url ?? '');
      const exists = mockTopics.some(
        (t) => t.name === q.name && t.parentId === (q.parentId || null) && t.id !== q.excludeId,
      );
      sendJson(res, exists);
    },
  },
];
