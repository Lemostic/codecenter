/**
 * modules/model-design/api/similarity-rule.ts
 *
 * 相似规则 API（严格按 5 函数 + 扩展操作）
 */
import { http } from '@mdm/core/http';
import type { PaginatedResponse, ApiResponse } from '@mdm/common/types/api';
import type {
  SimilarityRuleVO, SimilarityRuleCreateDTO, SimilarityRuleUpdateDTO, SimilarityRuleQuery,
} from '@/modules/model-design/types/similarity-rule';
import type { ID } from '@mdm/common/types/base';

const PREFIX = '/api/v1/model-design/similarity-rule';

/** 列表查询 */
export const listSimilarityRule = (query: SimilarityRuleQuery) =>
  http.get<PaginatedResponse<SimilarityRuleVO>>(PREFIX, { params: query });

/** 详情查询 */
export const getSimilarityRule = (id: ID) =>
  http.get<ApiResponse<SimilarityRuleVO>>(`${PREFIX}/${id}`);

/** 新增 */
export const createSimilarityRule = (data: SimilarityRuleCreateDTO) =>
  http.post<ApiResponse<SimilarityRuleVO>>(PREFIX, data);

/** 更新 */
export const updateSimilarityRule = (data: SimilarityRuleUpdateDTO) =>
  http.put<ApiResponse<SimilarityRuleVO>>(`${PREFIX}/${data.id}`, data);

/** 删除 */
export const deleteSimilarityRule = (id: ID) =>
  http.delete<ApiResponse<void>>(`${PREFIX}/${id}`);

// ========== 扩展操作 ==========

/** 批量删除 */
export const batchDeleteSimilarityRule = (ids: ID[]) =>
  http.post<ApiResponse<void>>(`${PREFIX}/batch-delete`, { ids });

/** 导出相似规则 */
export const exportSimilarityRule = (modelId: ID): Promise<Blob> =>
  http.get(`${PREFIX}/export`, { params: { modelId }, responseType: 'blob' }) as unknown as Promise<Blob>;
