/**
 * modules/model-index/api/model.ts
 *
 * 主数据模型 API（严格按 5 函数 + 扩展操作）
 */
import { http } from '@mdm/core/http';
import type { PaginatedResponse, ApiResponse } from '@mdm/common/types/api';
import type {
  ModelVO, ModelCreateDTO, ModelUpdateDTO, ModelQuery,
} from '@/modules/model-index/types/model';
import type { ID } from '@mdm/common/types/base';

const PREFIX = '/api/v1/model-design/model';

/** 列表查询 */
export const listModel = (query: ModelQuery) =>
  http.get<PaginatedResponse<ModelVO>>(PREFIX, { params: query });

/** 详情查询 */
export const getModel = (id: ID) =>
  http.get<ApiResponse<ModelVO>>(`${PREFIX}/${id}`);

/** 新增 */
export const createModel = (data: ModelCreateDTO) =>
  http.post<ApiResponse<ModelVO>>(PREFIX, data);

/** 更新 */
export const updateModel = (data: ModelUpdateDTO) =>
  http.put<ApiResponse<ModelVO>>(`${PREFIX}/${data.id}`, data);

/** 删除 */
export const deleteModel = (id: ID) =>
  http.delete<ApiResponse<void>>(`${PREFIX}/${id}`);

// ========== 扩展操作 ==========

/** 生效 */
export const activateModel = (id: ID) =>
  http.put<ApiResponse<void>>(`${PREFIX}/${id}/activate`);

/** 停用 */
export const disableModel = (id: ID) =>
  http.put<ApiResponse<void>>(`${PREFIX}/${id}/disable`);

/** 启用（从停用恢复） */
export const enableModel = (id: ID) =>
  http.put<ApiResponse<void>>(`${PREFIX}/${id}/enable`);

/** 移动至目标分类 */
export const moveModel = (ids: ID[], targetTopicId: ID) =>
  http.put<ApiResponse<void>>(`${PREFIX}/move`, { ids, targetTopicId });

/** 复制模型 */
export const copyModel = (sourceId: ID, data: ModelCreateDTO) =>
  http.post<ApiResponse<ModelVO>>(`${PREFIX}/copy`, { sourceId, ...data });

/** 导出 */
export const exportModel = (params?: { ids?: ID[]; keyword?: string }): Promise<Blob> =>
  http.get(`${PREFIX}/export`, { params, responseType: 'blob' }) as unknown as Promise<Blob>;

/** 校验名称唯一性 */
export const checkModelNameUnique = (name: string, excludeId?: ID) =>
  http.get<ApiResponse<boolean>>(`${PREFIX}/check-name`, { params: { name, excludeId } });

/** 校验编码唯一性 */
export const checkModelCodeUnique = (code: string, excludeId?: ID) =>
  http.get<ApiResponse<boolean>>(`${PREFIX}/check-code`, { params: { code, excludeId } });

/** 校验表名称唯一性 */
export const checkTableNameUnique = (tableName: string, datasourceId: string, excludeId?: ID) =>
  http.get<ApiResponse<boolean>>(`${PREFIX}/check-table`, { params: { tableName, datasourceId, excludeId } });
