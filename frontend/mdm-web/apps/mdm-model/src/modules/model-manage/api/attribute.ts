/**
 * modules/model-design/api/attribute.ts
 *
 * 属性配置 API（严格按 5 函数 + 扩展操作）
 */
import { http } from '@mdm/core/http';
import type { PaginatedResponse, ApiResponse } from '@mdm/common/types/api';
import type {
  AttributeVO, AttributeCreateDTO, AttributeUpdateDTO, AttributeQuery,
} from '@/modules/model-design/types/attribute';
import type { RelationConfig, ExpressionConfig } from '@/modules/model-design/types/attribute';
import type { ID } from '@mdm/common/types/base';

const PREFIX = '/api/v1/model-design/attribute';

/** 列表查询 */
export const listAttribute = (query: AttributeQuery) =>
  http.get<PaginatedResponse<AttributeVO>>(PREFIX, { params: query });

/** 详情查询 */
export const getAttribute = (id: ID) =>
  http.get<ApiResponse<AttributeVO>>(`${PREFIX}/${id}`);

/** 新增 */
export const createAttribute = (data: AttributeCreateDTO) =>
  http.post<ApiResponse<AttributeVO>>(PREFIX, data);

/** 更新 */
export const updateAttribute = (data: AttributeUpdateDTO) =>
  http.put<ApiResponse<AttributeVO>>(`${PREFIX}/${data.id}`, data);

/** 删除 */
export const deleteAttribute = (id: ID) =>
  http.delete<ApiResponse<void>>(`${PREFIX}/${id}`);

// ========== 扩展操作 ==========

/** 批量删除 */
export const batchDeleteAttribute = (ids: ID[]) =>
  http.post<ApiResponse<void>>(`${PREFIX}/batch-delete`, { ids });

/** 启用属性 */
export const enableAttribute = (id: ID) =>
  http.put<ApiResponse<void>>(`${PREFIX}/${id}/enable`);

/** 停用属性 */
export const disableAttribute = (id: ID) =>
  http.put<ApiResponse<void>>(`${PREFIX}/${id}/disable`);

/** 批量保存属性（模型管理页面批量编辑后一次提交） */
export const batchSaveAttributes = (modelId: ID, attributes: (AttributeCreateDTO | AttributeUpdateDTO)[]) =>
  http.post<ApiResponse<AttributeVO[]>>(`${PREFIX}/batch-save`, { modelId, attributes });

/** 校验属性名称唯一性 */
export const checkAttributeNameUnique = (modelId: ID, name: string, excludeId?: ID) =>
  http.get<ApiResponse<boolean>>(`${PREFIX}/check-name`, { params: { modelId, name, excludeId } });

/** 校验英文名称唯一性 */
export const checkAttributeEnglishNameUnique = (modelId: ID, englishName: string, excludeId?: ID) =>
  http.get<ApiResponse<boolean>>(`${PREFIX}/check-english-name`, { params: { modelId, englishName, excludeId } });

/** 检查属性引用依赖（删除前校验） */
export const checkAttributeReferences = (id: ID) =>
  http.get<ApiResponse<{ isReferenced: boolean; details: string[] }>>(`${PREFIX}/${id}/references`);

/** 保存关联对象配置 */
export const saveRelationConfig = (attributeId: ID, config: RelationConfig) =>
  http.put<ApiResponse<void>>(`${PREFIX}/${attributeId}/relation`, config);

/** 保存计算表达式配置 */
export const saveExpressionConfig = (attributeId: ID, config: ExpressionConfig) =>
  http.put<ApiResponse<void>>(`${PREFIX}/${attributeId}/expression`, config);

/** 保存匹配字段配置 */
export const saveMatchFieldConfig = (attributeId: ID, isMatch: boolean) =>
  http.put<ApiResponse<void>>(`${PREFIX}/${attributeId}/match-field`, { matchField: isMatch });

/** 保存流程字段配置 */
export const saveProcessFieldConfig = (attributeId: ID, isProcess: boolean) =>
  http.put<ApiResponse<void>>(`${PREFIX}/${attributeId}/process-field`, { processField: isProcess });

/** 导出属性配置 */
export const exportAttributeConfig = (modelId: ID, version?: number): Promise<Blob> =>
  http.get(`${PREFIX}/export`, { params: { modelId, version }, responseType: 'blob' }) as unknown as Promise<Blob>;
