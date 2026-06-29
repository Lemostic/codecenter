/**
 * modules/model-index/api/model.ts
 *
 * 主数据模型 API —— 已对齐后端契约 /api/mdm/encode/models/*
 *
 * 后端契约：
 *   - 列表  GET    /api/mdm/encode/models/list?page=&size=&tenantId=
 *   - 详情  GET    /api/mdm/encode/models/{id}
 *   - 创建  POST   /api/mdm/encode/models
 *   - 更新  PUT    /api/mdm/encode/models/{id}
 *   - 删除  DELETE /api/mdm/encode/models/{id}
 *   - 生效  POST   /api/mdm/encode/models/{id}/publish
 *   - 停用  POST   /api/mdm/encode/models/{id}/disable
 *   - 启用  POST   /api/mdm/encode/models/{id}/enable
 *
 * 后端字段映射（camelCase）：
 *   id, modelCode, modelName, tableName, modelType(NORMAL|COMPOSITE|CLASSIFY),
 *   themeId, description, securityLevel, version, status(EDIT|EFFECT|DISABLED|AUDITING)
 */
import { http } from '@mdm/core/http';
import type { PaginatedResponse, ApiResponse } from '@mdm/common/types/api';
import type { ID } from '@mdm/common/types/base';
import type {
  ModelVO, ModelCreateDTO, ModelUpdateDTO, ModelQuery,
} from '@/modules/model-index/types/model';

const PREFIX = '/api/mdm/encode/models';

// ============== 后端真实字段类型 ==============

export interface BackendModel {
  id: string;
  modelCode: string;
  modelName: string;
  tableName: string;
  modelType: 'NORMAL' | 'COMPOSITE' | 'CLASSIFY';
  themeId: string | null;
  description: string | null;
  securityLevel: string;
  version: number;
  status: 'EDIT' | 'EFFECT' | 'AUDITING' | 'HISTORY' | 'DISABLED';
  tenantId: string | null;
  createBy: string | null;
  createTime: string | null;
  updateBy: string | null;
  updateTime: string | null;
}

// ============== 字段映射器 ==============

const STATUS_TO_VO: Record<string, ModelVO['status']> = {
  EDIT: 'draft',
  EFFECT: 'active',
  AUDITING: 'reviewing',
  HISTORY: 'draft',
  DISABLED: 'disabled',
};

const TYPE_TO_VO: Record<string, ModelVO['modelType']> = {
  NORMAL: 'normal',
  COMPOSITE: 'composite',
  CLASSIFY: 'classification',
};

const TYPE_TO_BACKEND: Record<string, BackendModel['modelType']> = {
  normal: 'NORMAL',
  composite: 'COMPOSITE',
  classification: 'CLASSIFY',
};

const STATUS_LABEL: Record<ModelVO['status'], string> = {
  draft: '编辑中',
  active: '生效',
  disabled: '停用',
  reviewing: '审核中',
};

const TYPE_LABEL: Record<ModelVO['modelType'], string> = {
  normal: '普通模型',
  composite: '复合模型',
  classification: '分类模型',
};

/** 后端模型 → 前端 VO */
export function toModelVO(m: BackendModel): ModelVO {
  const status = STATUS_TO_VO[m.status] ?? 'draft';
  const modelType = TYPE_TO_VO[m.modelType] ?? 'normal';
  return {
    id: m.id,
    name: m.modelName,
    code: m.modelCode,
    tableName: m.tableName,
    modelType,
    status,
    version: m.version,
    description: m.description ?? '',
    topicId: (m.themeId ?? '') as ID,
    datasourceId: '' as ID,
    secretLevel: m.securityLevel,
    statusLabel: STATUS_LABEL[status],
    modelTypeLabel: TYPE_LABEL[modelType],
    versionLabel: `V${m.version}`,
    creatorName: m.createBy ?? 'system',
    createdAt: m.createTime ?? '',
    updatedAt: m.updateTime ?? '',
    createdBy: m.createBy ?? 'system',
    updatedBy: m.updateBy ?? 'system',
  };
}

/** 前端 DTO → 后端请求体 */
export function toBackendPayload(dto: ModelCreateDTO | ModelUpdateDTO): Partial<BackendModel> {
  const out: Partial<BackendModel> = {};
  if ('id' in dto && dto.id) out.id = dto.id as string;
  if ('name' in dto) out.modelName = dto.name;
  if ('code' in dto) out.modelCode = dto.code;
  if ('tableName' in dto) out.tableName = dto.tableName;
  if ('description' in dto) out.description = dto.description ?? '';
  if ('topicId' in dto) out.themeId = (dto.topicId as string) || null;
  if ('secretLevel' in dto) out.securityLevel = dto.secretLevel;
  if ('modelType' in dto && dto.modelType) out.modelType = TYPE_TO_BACKEND[dto.modelType];
  return out;
}

// ============== 后端分页 {records,total,page,size} → 前端 {rows,total,page,size} ==============

export function toPaginatedResponse<T>(
  backend: { records?: T[]; total?: number; page?: number; size?: number } | undefined,
): PaginatedResponse<T> {
  if (!backend) return { rows: [], total: 0, page: 1, size: 20 } as PaginatedResponse<T>;
  return {
    rows: backend.records ?? [],
    total: backend.total ?? 0,
    page: backend.page ?? 1,
    size: backend.size ?? 20,
  } as PaginatedResponse<T>;
}

// ============== 业务 API ==============

/** 列表查询 */
export async function listModel(query: ModelQuery) {
  // 注意：后端 H2 内存库默认 tenantId=null，过滤传 default 会查不到；
  // 这里不传 tenantId，让后端 Repository.findByTenantId(null) 返回全部
  const params: Record<string, unknown> = {
    page: query.page ?? 1,
    size: query.pageSize ?? 20,
  };
  if (query.keyword) params.keyword = query.keyword;
  if (query.topicId) params.themeId = query.topicId;
  const resp = await http.get<ApiResponse<{ records: BackendModel[]; total: number; page: number; size: number }>>(
    `${PREFIX}/list`,
    { params },
  );
  const page = toPaginatedResponse<BackendModel>(resp.data.data);
  return {
    ...resp,
    data: {
      ...resp.data,
      data: {
        rows: page.rows.map(toModelVO),
        total: page.total,
        page: page.page,
        size: page.size,
      },
    },
  };
}

/** 详情查询 */
export async function getModel(id: ID) {
  const resp = await http.get<ApiResponse<BackendModel>>(`${PREFIX}/${id}`);
  return {
    ...resp,
    data: {
      ...resp.data,
      data: resp.data.data ? toModelVO(resp.data.data) : null,
    },
  };
}

/** 创建 */
export async function createModel(data: ModelCreateDTO) {
  const payload = toBackendPayload(data);
  const resp = await http.post<ApiResponse<BackendModel>>(PREFIX, payload);
  return {
    ...resp,
    data: {
      ...resp.data,
      data: resp.data.data ? toModelVO(resp.data.data) : null,
    },
  };
}

/** 更新 */
export async function updateModel(data: ModelUpdateDTO) {
  const payload = toBackendPayload(data);
  const resp = await http.put<ApiResponse<BackendModel>>(`${PREFIX}/${data.id}`, payload);
  return {
    ...resp,
    data: {
      ...resp.data,
      data: resp.data.data ? toModelVO(resp.data.data) : null,
    },
  };
}

/** 删除 */
export async function deleteModel(id: ID) {
  return http.delete<ApiResponse<void>>(`${PREFIX}/${id}`);
}

/** 生效（draft→active） */
export async function activateModel(id: ID) {
  return http.post<ApiResponse<BackendModel>>(`${PREFIX}/${id}/publish`);
}

/** 停用 */
export async function disableModel(id: ID) {
  return http.post<ApiResponse<BackendModel>>(`${PREFIX}/${id}/disable`);
}

/** 启用（停用→生效） */
export async function enableModel(id: ID) {
  return http.post<ApiResponse<BackendModel>>(`${PREFIX}/${id}/enable`);
}

/** 移动 — 后端暂未提供 /move */
export async function moveModel(_ids: ID[], _targetTopicId: ID) {
  throw new Error('移动模型接口尚未在后端实现，请联系后端同学补充');
}

/** 复制模型 — 后端暂未提供 */
export async function copyModel(_sourceId: ID, _data: ModelCreateDTO) {
  throw new Error('复制模型接口尚未在后端实现');
}

/** 导出 — 后端暂未提供 */
export async function exportModel(_params?: { ids?: ID[]; keyword?: string }): Promise<Blob> {
  throw new Error('导出接口尚未在后端实现');
}

/** 名称唯一性校验 — 后端通过 create 校验，先返回 true */
export async function checkModelNameUnique(_name: string, _excludeId?: ID) {
  return { data: { success: true, data: { data: true } } } as { data: ApiResponse<boolean> };
}

export async function checkModelCodeUnique(_code: string, _excludeId?: ID) {
  return { data: { success: true, data: { data: true } } } as { data: ApiResponse<boolean> };
}

export async function checkTableNameUnique(_tableName: string, _datasourceId: string, _excludeId?: ID) {
  return { data: { success: true, data: { data: true } } } as { data: ApiResponse<boolean> };
}
