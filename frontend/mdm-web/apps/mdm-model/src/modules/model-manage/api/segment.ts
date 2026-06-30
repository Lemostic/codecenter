/**
 * modules/model-design/api/segment.ts
 *
 * 码段管理 API（严格按 5 函数 + 扩展操作）
 */
import { http } from '@mdm/core/http';
import type { PaginatedResponse, ApiResponse } from '@mdm/common/types/api';
import type { ID } from '@mdm/common/types/base';
import type {
  SegmentVO, SegmentCreateDTO, SegmentUpdateDTO, SegmentQuery,
  SegmentSelectOption,
} from '@/modules/model-manage/types/segment';
import type { SegmentType } from '@/modules/model-manage/types/coding-rule';
import { FRONT_TYPE_TO_BACK_TYPE, BACK_TYPE_TO_FRONT_TYPE } from '@/modules/model-manage/types/coding-rule';

const PREFIX = '/api/v1/model-design/segment';

/** 把前端的 type 转换为后端 enum 字符串 */
const toBackType = (t: SegmentType | undefined): string | undefined => {
  if (!t) return undefined;
  return FRONT_TYPE_TO_BACK_TYPE[t] || t.toUpperCase();
};

// ========== 5 件套 ==========

/** 列表查询 */
export const listSegment = (query: SegmentQuery) => {
  const backType = toBackType(query.type as SegmentType | undefined);
  const params: SegmentQuery = { ...query, type: backType as any };
  return http.get<PaginatedResponse<SegmentVO>>(PREFIX, { params });
};

/** 详情查询 */
export const getSegment = (id: ID) =>
  http.get<ApiResponse<SegmentVO>>(`${PREFIX}/${id}`);

/** 新增 */
export const createSegment = (data: SegmentCreateDTO) => {
  // 字段映射：name → segmentName, type → segmentType(大写)
  const backType = toBackType(data.type as SegmentType | undefined);
  const payload: any = { ...data, segmentName: data.name, segmentType: backType };
  // 删除前端风格的 name/type
  delete payload.name;
  delete payload.type;
  return http.post<ApiResponse<SegmentVO>>(PREFIX, payload);
};

/** 更新 */
export const updateSegment = (data: SegmentUpdateDTO) => {
  const backType = toBackType(data.type as SegmentType | undefined);
  const payload: any = { ...data, segmentName: data.name, segmentType: backType };
  delete payload.name;
  delete payload.type;
  return http.put<ApiResponse<SegmentVO>>(`${PREFIX}/${data.id}`, payload);
};

/** 删除 */
export const deleteSegment = (id: ID) =>
  http.delete<ApiResponse<void>>(`${PREFIX}/${id}`);

// ========== 扩展操作 ==========

/** 根据模型获取码段下拉选项（用于编码规则配置） */
export const getSegmentOptions = (modelId: ID, type?: SegmentType) =>
  http.get<ApiResponse<SegmentSelectOption[]>>(`${PREFIX}/options`, {
    params: { modelId, type },
  });

/** 批量删除 */
export const batchDeleteSegment = (ids: ID[]) =>
  http.post<ApiResponse<void>>(`${PREFIX}/batch-delete`, { ids });

/** 更新状态（启用/停用） */
export const updateSegmentStatus = (id: ID, status: 'enabled' | 'disabled') =>
  http.put<ApiResponse<void>>(`${PREFIX}/${id}/status`, { status });

/** 检查码段编码唯一性 */
export const checkSegmentCodeUnique = (code: string, excludeId?: ID) =>
  http.get<ApiResponse<boolean>>(`${PREFIX}/check-code`, {
    params: { code, excludeId },
  });

/** 检查码段名称唯一性 */
export const checkSegmentNameUnique = (name: string, excludeId?: ID) =>
  http.get<ApiResponse<boolean>>(`${PREFIX}/check-name`, {
    params: { name, excludeId },
  });

/** 获取码段被引用详情 */
export const getSegmentReferenceInfo = (id: ID) =>
  http.get<ApiResponse<{ ruleCount: number; ruleNames: string[] }>>(
    `${PREFIX}/${id}/references`,
  );

/** 导出码段 */
export const exportSegment = (modelId: ID): Promise<Blob> =>
  http.get(`${PREFIX}/export`, { params: { modelId }, responseType: 'blob' }) as unknown as Promise<Blob>;
