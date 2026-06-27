/**
 * modules/model-design/api/coding-rule.ts
 *
 * 编码规则 API（严格按 5 函数 + 扩展操作 + 版本管理）
 */
import { http } from '@mdm/core/http';
import type { PaginatedResponse, ApiResponse } from '@mdm/common/types/api';
import type { ID } from '@mdm/common/types/base';
import type {
  CodingRuleVO, CodingRuleCreateDTO, CodingRuleUpdateDTO, CodingRuleQuery,
  CodingRuleGroupVO, CodeSegment, SegmentType,
} from '@/modules/model-manage/types/coding-rule';

const PREFIX = '/api/v1/model-design/coding-rule';

// ========== 5 件套 ==========

/** 列表查询（分组展示） */
export const listCodingRule = (query: CodingRuleQuery) =>
  http.get<PaginatedResponse<CodingRuleVO>>(PREFIX, { params: query });

/** 列表查询（分组后的格式，用于列表展示） */
export const listCodingRuleGrouped = (query: CodingRuleQuery) =>
  http.get<PaginatedResponse<CodingRuleGroupVO>>(`${PREFIX}/grouped`, { params: query });

/** 详情查询 */
export const getCodingRule = (id: ID) =>
  http.get<ApiResponse<CodingRuleVO>>(`${PREFIX}/${id}`);

/** 新增 */
export const createCodingRule = (data: CodingRuleCreateDTO) =>
  http.post<ApiResponse<CodingRuleVO>>(PREFIX, data);

/** 更新 */
export const updateCodingRule = (data: CodingRuleUpdateDTO) =>
  http.put<ApiResponse<CodingRuleVO>>(`${PREFIX}/${data.id}`, data);

/** 删除 */
export const deleteCodingRule = (id: ID) =>
  http.delete<ApiResponse<void>>(`${PREFIX}/${id}`);

// ========== 版本管理 ==========

/** 生效 */
export const activateCodingRule = (id: ID) =>
  http.post<ApiResponse<void>>(`${PREFIX}/${id}/activate`);

/** 停用 */
export const disableCodingRule = (id: ID) =>
  http.post<ApiResponse<void>>(`${PREFIX}/${id}/disable`);

/** 启用 */
export const enableCodingRule = (id: ID) =>
  http.post<ApiResponse<void>>(`${PREFIX}/${id}/enable`);

/** 修订（复制生效版本生成新版本） */
export const reviseCodingRule = (id: ID) =>
  http.post<ApiResponse<CodingRuleVO>>(`${PREFIX}/${id}/revise`);

/** 获取规则的所有版本 */
export const getCodingRuleVersions = (versionGroupId: ID) =>
  http.get<ApiResponse<CodingRuleVO[]>>(`${PREFIX}/versions/${versionGroupId}`);

// ========== 码段配置 ==========

/** 获取规则的码段列表 */
export const getCodingRuleSegments = (ruleId: ID) =>
  http.get<ApiResponse<CodeSegment[]>>(`${PREFIX}/${ruleId}/segments`);

/** 保存规则的码段配置 */
export const saveCodingRuleSegments = (ruleId: ID, segments: Omit<CodeSegment, 'id'>[]) =>
  http.put<ApiResponse<CodeSegment[]>>(`${PREFIX}/${ruleId}/segments`, { segments });

// ========== 编码生成 ==========

/** 生成示例编码 */
export const generateSampleCode = (segments: Omit<CodeSegment, 'id'>[]) =>
  http.post<ApiResponse<string>>(`${PREFIX}/sample`, { segments });

/** 校验编码唯一性 */
export const checkCodeUnique = (ruleId: ID, attributeId: ID, code: string) =>
  http.post<ApiResponse<boolean>>(`${PREFIX}/check-unique`, {
    ruleId, attributeId, code,
  });

// ========== 脚本相关 ==========

/** 校验 Groovy 脚本语法 */
export const validateGroovyScript = (script: string) =>
  http.post<ApiResponse<{ valid: boolean; errors?: string[] }>>(
    `${PREFIX}/validate-script`,
    { script },
  );

// ========== 属性相关 ==========

/** 获取模型的可选编码属性（下拉过滤） */
export const getAvailableCodeAttributes = (modelId: ID) =>
  http.get<ApiResponse<{ id: ID; name: string; type: string }[]>>(
    `${PREFIX}/attributes`,
    { params: { modelId } },
  );

// ========== 导入导出 ==========

/** 导出编码规则 */
export const exportCodingRule = (modelId: ID): Promise<Blob> =>
  http.get(`${PREFIX}/export`, { params: { modelId }, responseType: 'blob' }) as unknown as Promise<Blob>;

/** 导出编码规则（含版本） */
export const exportCodingRuleWithVersions = (modelId: ID, attributeId?: ID): Promise<Blob> =>
  http.get(`${PREFIX}/export-with-versions`, {
    params: { modelId, attributeId },
    responseType: 'blob',
  }) as unknown as Promise<Blob>;

// ========== 批量操作 ==========

/** 批量删除 */
export const batchDeleteCodingRule = (ids: ID[]) =>
  http.post<ApiResponse<void>>(`${PREFIX}/batch-delete`, { ids });
