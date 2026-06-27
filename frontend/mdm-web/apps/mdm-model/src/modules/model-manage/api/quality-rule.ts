/**
 * modules/model-design/api/quality-rule.ts
 *
 * 质量规则 API（严格按 5 函数 + 扩展操作）
 */
import { http } from '@mdm/core/http';
import type { PaginatedResponse, ApiResponse } from '@mdm/common/types/api';
import type {
  QualityRuleVO, QualityRuleCreateDTO, QualityRuleUpdateDTO, QualityRuleQuery,
} from '@/modules/model-design/types/quality-rule';
import type { ID } from '@mdm/common/types/base';

const PREFIX = '/api/v1/model-design/quality-rule';

/** 列表查询 */
export const listQualityRule = (query: QualityRuleQuery) =>
  http.get<PaginatedResponse<QualityRuleVO>>(PREFIX, { params: query });

/** 详情查询 */
export const getQualityRule = (id: ID) =>
  http.get<ApiResponse<QualityRuleVO>>(`${PREFIX}/${id}`);

/** 新增 */
export const createQualityRule = (data: QualityRuleCreateDTO) =>
  http.post<ApiResponse<QualityRuleVO>>(PREFIX, data);

/** 更新 */
export const updateQualityRule = (data: QualityRuleUpdateDTO) =>
  http.put<ApiResponse<QualityRuleVO>>(`${PREFIX}/${data.id}`, data);

/** 删除 */
export const deleteQualityRule = (id: ID) =>
  http.delete<ApiResponse<void>>(`${PREFIX}/${id}`);

// ========== 扩展操作 ==========

/** 批量删除 */
export const batchDeleteQualityRule = (ids: ID[]) =>
  http.post<ApiResponse<void>>(`${PREFIX}/batch-delete`, { ids });

/** 启用质量规则 */
export const enableQualityRule = (id: ID) =>
  http.put<ApiResponse<void>>(`${PREFIX}/${id}/enable`);

/** 停用质量规则 */
export const disableQualityRule = (id: ID) =>
  http.put<ApiResponse<void>>(`${PREFIX}/${id}/disable`);

/** 配置数据入库 */
export const setDataStorage = (id: ID, dataStorage: boolean) =>
  http.put<ApiResponse<void>>(`${PREFIX}/${id}/data-storage`, { dataStorage });

/** 导出质量规则 */
export const exportQualityRule = (modelId: ID): Promise<Blob> =>
  http.get(`${PREFIX}/export`, { params: { modelId }, responseType: 'blob' }) as unknown as Promise<Blob>;
