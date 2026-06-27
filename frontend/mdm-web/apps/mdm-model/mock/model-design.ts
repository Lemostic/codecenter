/**
 * mock/model-design.ts
 *
 * model-design 模块 Mock 数据
 * 包含 25 条模拟主数据模型记录，覆盖 draft / active / disabled / reviewing 四种状态
 */

type ModelStatus = 'draft' | 'active' | 'disabled' | 'reviewing';
type ModelType = 'normal' | 'composite' | 'classification';

interface MockSubModel {
  id: string;
  name: string;
  code: string;
  tableName: string;
  description?: string;
  status: ModelStatus;
  sortOrder: number;
}

interface MockStandardFile {
  id: string;
  name: string;
  url: string;
  fileType: string;
}

interface MockModel {
  id: string;
  name: string;
  code: string;
  tableName: string;
  modelType: ModelType;
  description: string;
  status: ModelStatus;
  version: number;
  topicId: string;
  datasourceId: string;
  secretLevel?: string;
  subModels?: MockSubModel[];
  standardFiles?: MockStandardFile[];
  // VO 计算字段
  statusLabel: string;
  modelTypeLabel: string;
  versionLabel: string;
  topicName?: string;
  creatorName: string;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  updatedBy: string;
}

const STATUS_LABEL: Record<ModelStatus, string> = {
  draft: '编辑中',
  active: '生效',
  disabled: '停用',
  reviewing: '审核中',
};

const TYPE_LABEL: Record<ModelType, string> = {
  normal: '普通模型',
  composite: '复合模型',
  classification: '分类模型',
};

/** 生成 ISO 格式时间字符串 */
const makeDate = (daysAgo: number): string =>
  new Date(Date.now() - daysAgo * 86_400_000).toISOString().replace('T', ' ').substring(0, 19);

/** 原始数据 */
const rawModels: Array<{
  name: string;
  code: string;
  tableName: string;
  modelType: ModelType;
  description: string;
  status: ModelStatus;
  version: number;
  topicId: string;
  subModels?: MockSubModel[];
}> = [
  { name: '物料主数据',     code: 'MaterialMaster',    tableName: 'MD_MATERIAL',      modelType: 'normal',     description: '原材料与半成品物料的统一编码管理', status: 'active',    version: 2, topicId: 'topic-003' },
  { name: '金属材料',       code: 'MetalMaterial',     tableName: 'MD_METAL',         modelType: 'normal',     description: '金属材料分类及属性管理', status: 'active',    version: 1, topicId: 'topic-004' },
  { name: '非金属材料',     code: 'NonMetalMaterial',  tableName: 'MD_NON_METAL',     modelType: 'normal',     description: '非金属材料分类及属性管理', status: 'active',    version: 1, topicId: 'topic-005' },
  { name: '橡胶材料',       code: 'RubberMaterial',    tableName: 'MD_RUBBER',        modelType: 'normal',     description: '橡胶材料分类及属性管理', status: 'draft',     version: 1, topicId: 'topic-006' },
  { name: '涂料信息',       code: 'CoatingInfo',       tableName: 'MD_COATING',       modelType: 'normal',     description: '涂料分类及技术参数管理', status: 'draft',     version: 1, topicId: 'topic-007' },
  { name: '毛坯件',         code: 'RoughPart',         tableName: 'MD_ROUGH_PART',    modelType: 'normal',     description: '毛坯件基础信息及管理', status: 'active',    version: 1, topicId: 'topic-008' },
  { name: '自制件',         code: 'SelfMadePart',      tableName: 'MD_SELF_MADE',     modelType: 'normal',     description: '自制件生产工艺及管理', status: 'reviewing', version: 1, topicId: 'topic-009' },
  { name: '供应商主数据',   code: 'SupplierMaster',    tableName: 'MD_SUPPLIER',      modelType: 'normal',     description: '供应商资质、合同及评价信息管理', status: 'active',    version: 3, topicId: 'topic-010' },
  { name: '客户主数据',     code: 'CustomerMaster',    tableName: 'MD_CUSTOMER',      modelType: 'normal',     description: '客户基本信息、联系方式及信用等级管理', status: 'active',    version: 2, topicId: 'topic-011' },
  { name: '组织主数据',     code: 'OrgMaster',         tableName: 'MD_ORG',           modelType: 'normal',     description: '组织架构及部门信息管理', status: 'active',    version: 1, topicId: 'topic-012' },
  { name: '人员主数据',     code: 'PersonnelMaster',   tableName: 'MD_PERSONNEL',     modelType: 'normal',     description: '员工基本信息及岗位管理', status: 'active',    version: 1, topicId: 'topic-013' },
  { name: '会计科目主数据', code: 'AccountSubject',    tableName: 'MD_ACCOUNT',       modelType: 'normal',     description: '会计科目体系及核算规则管理', status: 'draft',     version: 1, topicId: 'topic-014' },
  { name: '仓库主数据',     code: 'WarehouseMaster',   tableName: 'MD_WAREHOUSE',     modelType: 'normal',     description: '仓库位置、容量及管辖范围定义', status: 'active',    version: 1, topicId: 'topic-015' },
  { name: '品牌信息',       code: 'BrandInfo',         tableName: 'MD_BRAND',         modelType: 'normal',     description: '品牌注册信息及品牌授权管理', status: 'disabled',  version: 1, topicId: 'topic-016' },
  { name: '品类定义',       code: 'CategoryDef',       tableName: 'MD_CATEGORY',      modelType: 'normal',     description: '商品品类的层级结构与属性定义', status: 'active',    version: 2, topicId: 'topic-017' },
  { name: '计量单位',       code: 'UnitOfMeasure',     tableName: 'MD_UNIT',          modelType: 'normal',     description: '统一计量单位及换算关系维护', status: 'active',    version: 1, topicId: 'topic-018' },
  { name: '价格策略',       code: 'PriceStrategy',     tableName: 'MD_PRICE',         modelType: 'normal',     description: '定价规则、折扣策略与价格审批流程', status: 'draft',     version: 1, topicId: 'topic-019' },
  { name: '渠道信息',       code: 'ChannelInfo',       tableName: 'MD_CHANNEL',       modelType: 'normal',     description: '销售渠道类型与渠道商信息管理', status: 'active',    version: 1, topicId: 'topic-020' },
  { name: '区域定义',       code: 'RegionDef',         tableName: 'MD_REGION',        modelType: 'normal',     description: '国家、省/州、城市等行政区域层级定义', status: 'active',    version: 2, topicId: 'topic-021' },
  { name: '币种主数据',     code: 'CurrencyMaster',    tableName: 'MD_CURRENCY',      modelType: 'normal',     description: '币种编码、符号及汇率基准管理', status: 'active',    version: 1, topicId: 'topic-022' },
  { name: '标准件信息',     code: 'StandardPartInfo',  tableName: 'ODS_ERP_01_SC04',  modelType: 'normal',     description: '标准件基础信息及规格参数管理', status: 'reviewing', version: 1, topicId: 'topic-023' },
  { name: '复合订单模型',   code: 'CompositeOrder',    tableName: 'MD_COMPOSITE_ORDER', modelType: 'composite', description: '订单主表关联多子表复合模型', status: 'draft',     version: 1, topicId: 'topic-024',
    subModels: [
      { id: 'sub-001', name: '订单主表', code: 'OrderMain', tableName: 'MD_ORDER_MAIN', description: '订单主表', status: 'active', sortOrder: 1 },
      { id: 'sub-002', name: '订单明细', code: 'OrderDetail', tableName: 'MD_ORDER_DETAIL', description: '订单明细子表', status: 'active', sortOrder: 2 },
      { id: 'sub-003', name: '订单物流', code: 'OrderLogistics', tableName: 'MD_ORDER_LOGISTICS', description: '订单物流子表', status: 'draft', sortOrder: 3 },
    ],
  },
  { name: '税率配置',     code: 'TaxRateConfig',     tableName: 'MD_TAX_RATE',     modelType: 'normal',     description: '各区域适用税率及免税政策配置', status: 'draft',     version: 1, topicId: 'topic-025' },
  { name: '规格参数',     code: 'SpecParam',         tableName: 'MD_SPEC_PARAM',   modelType: 'normal',     description: '商品规格参数模板及自定义属性管理', status: 'active',    version: 1, topicId: 'topic-003' },
  { name: '批次信息',     code: 'BatchInfo',         tableName: 'MD_BATCH',        modelType: 'normal',     description: '生产批次追溯与有效期管理', status: 'disabled',   version: 1, topicId: 'topic-003' },
];

/** 生成 25 条完整 Mock 数据 */
export const mockModels: MockModel[] = rawModels.map((item, index) => ({
  ...item,
  id: `model-${String(index + 1).padStart(3, '0')}`,
  datasourceId: 'ds-001',
  statusLabel: STATUS_LABEL[item.status],
  modelTypeLabel: TYPE_LABEL[item.modelType],
  versionLabel: `V${item.version}`,
  creatorName: '数据管理员',
  createdAt: makeDate(30 - index),
  updatedAt: makeDate(Math.max(0, 15 - index)),
  createdBy: 'user-001',
  updatedBy: 'user-001',
}));

/** 获取下一个模型 ID */
export function nextModelId(): string {
  const maxNum = mockModels.reduce((max, m) => {
    const n = parseInt(m.id.replace('model-', ''), 10);
    return n > max ? n : max;
  }, 0);
  return `model-${String(maxNum + 1).padStart(3, '0')}`;
}

/** 转为视图对象 */
export function toModelVO(m: MockModel) {
  return { ...m };
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

const PREFIX = '/api/v1/model-design/model';
const RE_LIST = new RegExp(`^${PREFIX}$`);
const RE_DETAIL = new RegExp(`^${PREFIX}/([^/]+)$`);
const RE_ACTIVATE = new RegExp(`^${PREFIX}/([^/]+)/(activate|disable|enable)$`);
const RE_CHECK_NAME = new RegExp(`^${PREFIX}/check-name$`);
const RE_CHECK_CODE = new RegExp(`^${PREFIX}/check-code$`);
const RE_CHECK_TABLE = new RegExp(`^${PREFIX}/check-table$`);
const RE_MOVE = new RegExp(`^${PREFIX}/move$`);
const RE_COPY = new RegExp(`^${PREFIX}/copy$`);

export const mockModelHandlers: MockHandler[] = [
  // 列表
  {
    method: 'GET',
    pattern: RE_LIST,
    handler: async (req, res) => {
      const q = parseQuery(req.url ?? '');
      const page = parseInt(q.page ?? '1', 10);
      const pageSize = parseInt(q.pageSize ?? '20', 10);
      const keyword = q.keyword ?? '';
      const status = q.status ?? '';
      const topicId = q.topicId ?? '';
      let list = [...mockModels];
      if (keyword) list = list.filter((m) => matchesKeyword(m as never, ['name', 'code', 'tableName'], keyword));
      if (status) list = list.filter((m) => m.status === status);
      if (topicId) list = list.filter((m) => m.topicId === topicId);
      sendJson(res, paginate(list, page, pageSize));
    },
  },
  // 详情
  {
    method: 'GET',
    pattern: RE_DETAIL,
    handler: async (req, res) => {
      const id = basePath(req.url ?? '').match(RE_DETAIL)?.[1];
      const item = mockModels.find((m) => m.id === id);
      if (!item) return sendError(res, '模型不存在', 404);
      sendJson(res, item);
    },
  },
  // 新增
  {
    method: 'POST',
    pattern: RE_LIST,
    handler: async (req, res) => {
      const body = await parseBody<MockModel>(req);
      const id = nextModelId();
      const newItem: MockModel = {
        ...body,
        id,
        statusLabel: '编辑中',
        modelTypeLabel: '普通模型',
        versionLabel: `V${body.version ?? 1}`,
        createdAt: new Date().toISOString().replace('T', ' ').slice(0, 19),
        updatedAt: new Date().toISOString().replace('T', ' ').slice(0, 19),
        createdBy: 'user-001',
        updatedBy: 'user-001',
      };
      mockModels.unshift(newItem);
      sendJson(res, newItem);
    },
  },
  // 更新
  {
    method: 'PUT',
    pattern: RE_DETAIL,
    handler: async (req, res) => {
      const id = basePath(req.url ?? '').match(RE_DETAIL)?.[1];
      const idx = mockModels.findIndex((m) => m.id === id);
      if (idx < 0) return sendError(res, '模型不存在', 404);
      const body = await parseBody<Partial<MockModel>>(req);
      mockModels[idx] = {
        ...mockModels[idx],
        ...body,
        id: mockModels[idx].id,
        updatedAt: new Date().toISOString().replace('T', ' ').slice(0, 19),
        updatedBy: 'user-001',
      };
      sendJson(res, mockModels[idx]);
    },
  },
  // 删除
  {
    method: 'DELETE',
    pattern: RE_DETAIL,
    handler: async (req, res) => {
      const id = basePath(req.url ?? '').match(RE_DETAIL)?.[1];
      const idx = mockModels.findIndex((m) => m.id === id);
      if (idx < 0) return sendError(res, '模型不存在', 404);
      mockModels.splice(idx, 1);
      sendJson(res, null);
    },
  },
  // 激活 / 停用 / 启用
  {
    method: 'PUT',
    pattern: RE_ACTIVATE,
    handler: async (req, res) => {
      const m = basePath(req.url ?? '').match(RE_ACTIVATE);
      const id = m?.[1];
      const action = m?.[2] as 'activate' | 'disable' | 'enable' | undefined;
      const item = mockModels.find((x) => x.id === id);
      if (!item) return sendError(res, '模型不存在', 404);
      if (action === 'activate') item.status = 'active';
      else if (action === 'disable') item.status = 'disabled';
      else if (action === 'enable') item.status = 'active';
      item.statusLabel = item.status === 'active' ? '生效' : item.status === 'disabled' ? '停用' : '编辑中';
      item.updatedAt = new Date().toISOString().replace('T', ' ').slice(0, 19);
      sendJson(res, null);
    },
  },
  // 唯一性校验
  {
    method: 'GET',
    pattern: RE_CHECK_NAME,
    handler: async (req, res) => {
      const q = parseQuery(req.url ?? '');
      const exists = mockModels.some((m) => m.name === q.name && m.id !== q.excludeId);
      sendJson(res, exists);
    },
  },
  {
    method: 'GET',
    pattern: RE_CHECK_CODE,
    handler: async (req, res) => {
      const q = parseQuery(req.url ?? '');
      const exists = mockModels.some((m) => m.code === q.code && m.id !== q.excludeId);
      sendJson(res, exists);
    },
  },
  {
    method: 'GET',
    pattern: RE_CHECK_TABLE,
    handler: async (req, res) => {
      const q = parseQuery(req.url ?? '');
      const exists = mockModels.some(
        (m) => m.tableName === q.tableName && m.id !== q.excludeId,
      );
      sendJson(res, exists);
    },
  },
  // 移动
  {
    method: 'PUT',
    pattern: RE_MOVE,
    handler: async (req, res) => {
      const body = await parseBody<{ ids: string[]; targetTopicId: string }>(req);
      body.ids.forEach((id) => {
        const item = mockModels.find((m) => m.id === id);
        if (item) item.topicId = body.targetTopicId;
      });
      sendJson(res, null);
    },
  },
  // 复制
  {
    method: 'POST',
    pattern: RE_COPY,
    handler: async (req, res) => {
      const body = await parseBody<MockModel & { sourceId: string }>(req);
      const id = nextModelId();
      const copy: MockModel = {
        ...body,
        id,
        createdAt: new Date().toISOString().replace('T', ' ').slice(0, 19),
        updatedAt: new Date().toISOString().replace('T', ' ').slice(0, 19),
      };
      mockModels.unshift(copy);
      sendJson(res, copy);
    },
  },
];
