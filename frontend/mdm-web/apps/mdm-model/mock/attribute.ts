/**
 * mock/attribute.ts
 *
 * model-design/attribute 模块 Mock 数据
 * 为属性编辑页面提供测试数据
 */

type AttributeStatus = 'enabled' | 'disabled';
type PhysicalDataType =
  | 'VARCHAR' | 'CHAR' | 'TEXT' | 'CLOB'
  | 'INT' | 'BIGINT' | 'DECIMAL' | 'FLOAT' | 'DOUBLE'
  | 'DATE' | 'DATETIME' | 'TIMESTAMP'
  | 'BLOB';

interface MockAttribute {
  id: string;
  modelId: string;
  name: string;
  englishName: string;
  dataType: PhysicalDataType;
  dataCategory: 'string' | 'number' | 'date' | 'file' | 'clob';
  length?: number;
  lengthUnit?: 'byte' | 'char';
  precision?: number;
  positiveOnly: boolean;
  required: boolean;
  unique: boolean;
  comment: string;
  sortOrder: number;
  status: AttributeStatus;
  version: number;
  matchField: boolean;
  processField: boolean;
  fileField: boolean;
  defaultValue?: string;
  hasBeenActive: boolean;
  // VO 计算字段
  statusLabel: string;
  dataCategoryLabel: string;
  relationDisplay?: string;
  expressionSummary?: string;
  isReferenced?: boolean;
  referenceDetails?: string[];
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  updatedBy: string;
}

const STATUS_LABEL: Record<AttributeStatus, string> = {
  enabled: '启用',
  disabled: '停用',
};

const CATEGORY_LABEL: Record<string, string> = {
  string: '字符',
  number: '数值',
  date: '日期',
  file: '文件',
  clob: '大文本',
};

const makeDate = (daysAgo: number): string =>
  new Date(Date.now() - daysAgo * 86_400_000).toISOString().replace('T', ' ').substring(0, 19);

/** 属性原始数据定义 */
const rawAttributes: Array<{
  modelId: string;
  name: string;
  englishName: string;
  dataType: PhysicalDataType;
  dataCategory: MockAttribute['dataCategory'];
  length?: number;
  precision?: number;
  required: boolean;
  unique: boolean;
  comment: string;
  sortOrder: number;
  status: AttributeStatus;
  matchField: boolean;
  processField: boolean;
  relationDisplay?: string;
  expressionSummary?: string;
}> = [
  // model-025 (人员主数据) 的属性
  { modelId: 'model-012', name: '姓名', englishName: 'name', dataType: 'VARCHAR', dataCategory: 'string', length: 100, required: true, unique: false, comment: '员工姓名', sortOrder: 1, status: 'enabled', matchField: true, processField: true },
  { modelId: 'model-012', name: '工号', englishName: 'employee_no', dataType: 'VARCHAR', dataCategory: 'string', length: 50, required: true, unique: true, comment: '员工工号，全局唯一', sortOrder: 2, status: 'enabled', matchField: true, processField: true },
  { modelId: 'model-012', name: '性别', englishName: 'gender', dataType: 'VARCHAR', dataCategory: 'string', length: 10, required: true, unique: false, comment: '性别', sortOrder: 3, status: 'enabled', matchField: false, processField: false, relationDisplay: '性别字典' },
  { modelId: 'model-012', name: '出生日期', englishName: 'birth_date', dataType: 'DATE', dataCategory: 'date', required: false, unique: false, comment: '出生日期', sortOrder: 4, status: 'enabled', matchField: false, processField: false },
  { modelId: 'model-012', name: '手机号', englishName: 'phone', dataType: 'VARCHAR', dataCategory: 'string', length: 20, required: true, unique: true, comment: '手机号码', sortOrder: 5, status: 'enabled', matchField: true, processField: false },
  { modelId: 'model-012', name: '邮箱', englishName: 'email', dataType: 'VARCHAR', dataCategory: 'string', length: 200, required: false, unique: true, comment: '电子邮箱', sortOrder: 6, status: 'enabled', matchField: false, processField: false },
  { modelId: 'model-012', name: '所属部门', englishName: 'dept_id', dataType: 'BIGINT', dataCategory: 'number', required: true, unique: false, comment: '所属部门ID', sortOrder: 7, status: 'enabled', matchField: false, processField: true, relationDisplay: '组织主数据' },
  { modelId: 'model-012', name: '岗位', englishName: 'position', dataType: 'VARCHAR', dataCategory: 'string', length: 100, required: false, unique: false, comment: '岗位名称', sortOrder: 8, status: 'enabled', matchField: false, processField: false },
  { modelId: 'model-012', name: '入职日期', englishName: 'hire_date', dataType: 'DATE', dataCategory: 'date', required: true, unique: false, comment: '入职日期', sortOrder: 9, status: 'enabled', matchField: false, processField: true },
  { modelId: 'model-012', name: '职级', englishName: 'job_level', dataType: 'VARCHAR', dataCategory: 'string', length: 50, required: false, unique: false, comment: '职级', sortOrder: 10, status: 'enabled', matchField: false, processField: false, relationDisplay: '职级字典' },
  { modelId: 'model-012', name: '合同类型', englishName: 'contract_type', dataType: 'VARCHAR', dataCategory: 'string', length: 50, required: false, unique: false, comment: '合同类型', sortOrder: 11, status: 'disabled', matchField: false, processField: false },
  { modelId: 'model-012', name: '学历', englishName: 'education', dataType: 'VARCHAR', dataCategory: 'string', length: 50, required: false, unique: false, comment: '最高学历', sortOrder: 12, status: 'enabled', matchField: false, processField: false, relationDisplay: '学历字典' },
  { modelId: 'model-012', name: '薪资等级', englishName: 'salary_level', dataType: 'DECIMAL', dataCategory: 'number', length: 10, precision: 2, required: false, unique: false, comment: '薪资等级', sortOrder: 13, status: 'disabled', matchField: false, processField: true },
  { modelId: 'model-012', name: '备注', englishName: 'remark', dataType: 'TEXT', dataCategory: 'clob', required: false, unique: false, comment: '备注信息', sortOrder: 14, status: 'enabled', matchField: false, processField: false },
  { modelId: 'model-012', name: '身份证号', englishName: 'id_card_no', dataType: 'VARCHAR', dataCategory: 'string', length: 18, required: true, unique: true, comment: '身份证号码', sortOrder: 15, status: 'enabled', matchField: true, processField: true },
  { modelId: 'model-012', name: '状态', englishName: 'status', dataType: 'VARCHAR', dataCategory: 'string', length: 20, required: true, unique: false, comment: '人员状态', sortOrder: 16, status: 'enabled', matchField: false, processField: true, relationDisplay: '人员状态字典' },
  { modelId: 'model-012', name: '头像', englishName: 'avatar', dataType: 'VARCHAR', dataCategory: 'file', length: 500, required: false, unique: false, comment: '头像文件路径', sortOrder: 17, status: 'enabled', matchField: false, processField: false },
  { modelId: 'model-012', name: '更新时间', englishName: 'updated_at', dataType: 'DATETIME', dataCategory: 'date', required: false, unique: false, comment: '最后更新时间', sortOrder: 18, status: 'enabled', matchField: false, processField: false, expressionSummary: '自动取当前时间' },

  // model-022 (复合订单模型) 子模型属性
  // 订单主表 sub-001
  { modelId: 'model-022', name: '订单编号', englishName: 'order_no', dataType: 'VARCHAR', dataCategory: 'string', length: 50, required: true, unique: true, comment: '订单编号', sortOrder: 1, status: 'enabled', matchField: true, processField: true },
  { modelId: 'model-022', name: '客户名称', englishName: 'customer_name', dataType: 'VARCHAR', dataCategory: 'string', length: 200, required: true, unique: false, comment: '客户名称', sortOrder: 2, status: 'enabled', matchField: true, processField: false, relationDisplay: '客户主数据' },
  { modelId: 'model-022', name: '订单金额', englishName: 'total_amount', dataType: 'DECIMAL', dataCategory: 'number', length: 15, precision: 2, required: true, unique: false, comment: '订单总金额', sortOrder: 3, status: 'enabled', matchField: false, processField: true, expressionSummary: 'SUM(明细.金额)' },
  { modelId: 'model-022', name: '下单时间', englishName: 'order_time', dataType: 'DATETIME', dataCategory: 'date', required: true, unique: false, comment: '下单时间', sortOrder: 4, status: 'enabled', matchField: false, processField: true },
  { modelId: 'model-022', name: '订单状态', englishName: 'order_status', dataType: 'VARCHAR', dataCategory: 'string', length: 30, required: true, unique: false, comment: '订单状态', sortOrder: 5, status: 'enabled', matchField: false, processField: true, relationDisplay: '订单状态字典' },
  // 订单明细 sub-002
  { modelId: 'model-022', name: '物料编码', englishName: 'material_code', dataType: 'VARCHAR', dataCategory: 'string', length: 50, required: true, unique: false, comment: '物料编码', sortOrder: 6, status: 'enabled', matchField: true, processField: false, relationDisplay: '物料主数据' },
  { modelId: 'model-022', name: '数量', englishName: 'quantity', dataType: 'DECIMAL', dataCategory: 'number', length: 12, precision: 2, required: true, unique: false, comment: '数量', sortOrder: 7, status: 'enabled', matchField: false, processField: false },
  { modelId: 'model-022', name: '单价', englishName: 'unit_price', dataType: 'DECIMAL', dataCategory: 'number', length: 12, precision: 2, required: true, unique: false, comment: '单价', sortOrder: 8, status: 'enabled', matchField: false, processField: false },
  { modelId: 'model-022', name: '金额', englishName: 'amount', dataType: 'DECIMAL', dataCategory: 'number', length: 15, precision: 2, required: true, unique: false, comment: '金额', sortOrder: 9, status: 'enabled', matchField: false, processField: true, expressionSummary: '数量*单价' },
  // 订单物流 sub-003
  { modelId: 'model-022', name: '物流单号', englishName: 'tracking_no', dataType: 'VARCHAR', dataCategory: 'string', length: 100, required: false, unique: true, comment: '物流单号', sortOrder: 10, status: 'enabled', matchField: true, processField: false },
  { modelId: 'model-022', name: '发货时间', englishName: 'ship_time', dataType: 'DATETIME', dataCategory: 'date', required: false, unique: false, comment: '发货时间', sortOrder: 11, status: 'enabled', matchField: false, processField: false },
  { modelId: 'model-022', name: '收货地址', englishName: 'shipping_address', dataType: 'VARCHAR', dataCategory: 'string', length: 500, required: false, unique: false, comment: '收货地址', sortOrder: 12, status: 'disabled', matchField: false, processField: false },
];

let attrIdCounter = 0;

/** 生成完整 Mock 属性数据 */
export const mockAttributes: MockAttribute[] = rawAttributes.map((item) => {
  attrIdCounter++;
  return {
    id: `attr-${String(attrIdCounter).padStart(3, '0')}`,
    modelId: item.modelId,
    name: item.name,
    englishName: item.englishName,
    dataType: item.dataType,
    dataCategory: item.dataCategory,
    length: item.length,
    lengthUnit: item.dataCategory === 'string' ? 'char' : undefined,
    precision: item.precision,
    positiveOnly: false,
    required: item.required,
    unique: item.unique,
    comment: item.comment,
    sortOrder: item.sortOrder,
    status: item.status,
    version: 1,
    matchField: item.matchField,
    processField: item.processField,
    fileField: item.dataCategory === 'file',
    defaultValue: undefined,
    hasBeenActive: item.status === 'enabled',
    statusLabel: STATUS_LABEL[item.status],
    dataCategoryLabel: CATEGORY_LABEL[item.dataCategory],
    relationDisplay: item.relationDisplay,
    expressionSummary: item.expressionSummary,
    isReferenced: false,
    referenceDetails: [],
    createdAt: makeDate(30 - attrIdCounter),
    updatedAt: makeDate(Math.max(0, 15 - attrIdCounter)),
    createdBy: 'user-001',
    updatedBy: 'user-001',
  };
});

/** 获取下一个属性 ID */
export function nextAttrId(): string {
  attrIdCounter++;
  return `attr-${String(attrIdCounter).padStart(3, '0')}`;
}

// ========== Mock HTTP Handlers ==========
import type { MockHandler } from '@mdm/config-vite/mock';
import {
  basePath,
  paginate,
  parseBody,
  parseQuery,
  sendError,
  sendJson,
} from './_helpers';

const PREFIX = '/api/v1/model-design/attribute';
const RE_LIST = new RegExp(`^${PREFIX}$`);
const RE_DETAIL = new RegExp(`^${PREFIX}/([^/]+)$`);
const RE_BATCH_DELETE = new RegExp(`^${PREFIX}/batch-delete$`);
const RE_BATCH_SAVE = new RegExp(`^${PREFIX}/batch-save$`);
const RE_CHECK_NAME = new RegExp(`^${PREFIX}/check-name$`);
const RE_CHECK_ENGLISH = new RegExp(`^${PREFIX}/check-english-name$`);
const RE_REFERENCES = new RegExp(`^${PREFIX}/([^/]+)/references$`);
const RE_ENABLE = new RegExp(`^${PREFIX}/([^/]+)/(enable|disable)$`);
const RE_RELATION = new RegExp(`^${PREFIX}/([^/]+)/relation$`);
const RE_EXPRESSION = new RegExp(`^${PREFIX}/([^/]+)/expression$`);
const RE_MATCH_FIELD = new RegExp(`^${PREFIX}/([^/]+)/match-field$`);
const RE_PROCESS_FIELD = new RegExp(`^${PREFIX}/([^/]+)/process-field$`);

export const mockAttributeHandlers: MockHandler[] = [
  // 列表
  {
    method: 'GET',
    pattern: RE_LIST,
    handler: async (req, res) => {
      const q = parseQuery(req.url ?? '');
      const page = parseInt(q.page ?? '1', 10);
      const pageSize = parseInt(q.pageSize ?? '20', 10);
      const modelId = q.modelId ?? '';
      let list = [...mockAttributes];
      if (modelId) list = list.filter((a) => a.modelId === modelId);
      sendJson(res, paginate(list, page, pageSize));
    },
  },
  // 详情
  {
    method: 'GET',
    pattern: RE_DETAIL,
    handler: async (req, res) => {
      const id = basePath(req.url ?? '').match(RE_DETAIL)?.[1];
      const item = mockAttributes.find((a) => a.id === id);
      if (!item) return sendError(res, '属性不存在', 404);
      sendJson(res, item);
    },
  },
  // 新增
  {
    method: 'POST',
    pattern: RE_LIST,
    handler: async (req, res) => {
      const body = await parseBody<MockAttribute>(req);
      const id = nextAttrId();
      const newItem: MockAttribute = {
        ...body,
        id,
        createdAt: new Date().toISOString().replace('T', ' ').slice(0, 19),
        updatedAt: new Date().toISOString().replace('T', ' ').slice(0, 19),
        createdBy: 'user-001',
        updatedBy: 'user-001',
      };
      mockAttributes.push(newItem);
      sendJson(res, newItem);
    },
  },
  // 更新
  {
    method: 'PUT',
    pattern: RE_DETAIL,
    handler: async (req, res) => {
      const id = basePath(req.url ?? '').match(RE_DETAIL)?.[1];
      const idx = mockAttributes.findIndex((a) => a.id === id);
      if (idx < 0) return sendError(res, '属性不存在', 404);
      const body = await parseBody<Partial<MockAttribute>>(req);
      mockAttributes[idx] = {
        ...mockAttributes[idx],
        ...body,
        id: mockAttributes[idx].id,
        updatedAt: new Date().toISOString().replace('T', ' ').slice(0, 19),
        updatedBy: 'user-001',
      };
      sendJson(res, mockAttributes[idx]);
    },
  },
  // 删除
  {
    method: 'DELETE',
    pattern: RE_DETAIL,
    handler: async (req, res) => {
      const id = basePath(req.url ?? '').match(RE_DETAIL)?.[1];
      const idx = mockAttributes.findIndex((a) => a.id === id);
      if (idx < 0) return sendError(res, '属性不存在', 404);
      mockAttributes.splice(idx, 1);
      sendJson(res, null);
    },
  },
  // 批量删除
  {
    method: 'POST',
    pattern: RE_BATCH_DELETE,
    handler: async (req, res) => {
      const body = await parseBody<{ ids: string[] }>(req);
      for (const id of body.ids ?? []) {
        const idx = mockAttributes.findIndex((a) => a.id === id);
        if (idx >= 0) mockAttributes.splice(idx, 1);
      }
      sendJson(res, null);
    },
  },
  // 批量保存
  {
    method: 'POST',
    pattern: RE_BATCH_SAVE,
    handler: async (req, res) => {
      const body = await parseBody<{
        modelId: string;
        attributes: (Partial<MockAttribute> & { id?: string })[];
      }>(req);
      const result: MockAttribute[] = [];
      for (const item of body.attributes) {
        if (item.id) {
          const idx = mockAttributes.findIndex((a) => a.id === item.id);
          if (idx >= 0) {
            mockAttributes[idx] = { ...mockAttributes[idx], ...item };
            result.push(mockAttributes[idx]);
          }
        } else {
          const id = nextAttrId();
          const newItem: MockAttribute = {
            ...(item as MockAttribute),
            id,
            modelId: body.modelId,
            createdAt: new Date().toISOString().replace('T', ' ').slice(0, 19),
            updatedAt: new Date().toISOString().replace('T', ' ').slice(0, 19),
            createdBy: 'user-001',
            updatedBy: 'user-001',
          };
          mockAttributes.push(newItem);
          result.push(newItem);
        }
      }
      sendJson(res, result);
    },
  },
  // 启用 / 停用
  {
    method: 'PUT',
    pattern: RE_ENABLE,
    handler: async (req, res) => {
      const m = basePath(req.url ?? '').match(RE_ENABLE);
      const id = m?.[1];
      const action = m?.[2];
      const item = mockAttributes.find((a) => a.id === id);
      if (!item) return sendError(res, '属性不存在', 404);
      item.status = action === 'enable' ? 'enabled' : 'disabled';
      item.statusLabel = item.status === 'enabled' ? '启用' : '停用';
      sendJson(res, null);
    },
  },
  // 唯一性校验
  {
    method: 'GET',
    pattern: RE_CHECK_NAME,
    handler: async (req, res) => {
      const q = parseQuery(req.url ?? '');
      const exists = mockAttributes.some(
        (a) => a.modelId === q.modelId && a.name === q.name && a.id !== q.excludeId,
      );
      sendJson(res, exists);
    },
  },
  {
    method: 'GET',
    pattern: RE_CHECK_ENGLISH,
    handler: async (req, res) => {
      const q = parseQuery(req.url ?? '');
      const exists = mockAttributes.some(
        (a) => a.modelId === q.modelId && a.englishName === q.englishName && a.id !== q.excludeId,
      );
      sendJson(res, exists);
    },
  },
  // 引用检查
  {
    method: 'GET',
    pattern: RE_REFERENCES,
    handler: async (_req, res) => {
      sendJson(res, { isReferenced: false, details: [] });
    },
  },
  // 关联对象 / 表达式 / 匹配字段 / 流程字段（mock 全部成功）
  { method: 'PUT', pattern: RE_RELATION, handler: async (_req, res) => sendJson(res, null) },
  { method: 'PUT', pattern: RE_EXPRESSION, handler: async (_req, res) => sendJson(res, null) },
  { method: 'PUT', pattern: RE_MATCH_FIELD, handler: async (_req, res) => sendJson(res, null) },
  { method: 'PUT', pattern: RE_PROCESS_FIELD, handler: async (_req, res) => sendJson(res, null) },
];
