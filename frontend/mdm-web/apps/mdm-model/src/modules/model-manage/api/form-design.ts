/**
 * modules/model-design/api/form-design.ts
 *
 * 填报设计 API（严格按 5 函数 + 扩展操作）
 */
import { http } from '@mdm/core/http';
import type { ApiResponse } from '@mdm/common/types/api';
import type {
  FormDesignVO, FormDesignCreateDTO, FormDesignUpdateDTO, FormDesignQuery,
} from '@/modules/model-design/types/form-design';
import type { ID } from '@mdm/common/types/base';

const PREFIX = '/api/v1/model-design/form-design';

/** 查询填报设计配置 */
export const listFormDesign = (query: FormDesignQuery) =>
  http.get<ApiResponse<FormDesignVO>>(PREFIX, { params: query });

/** 详情查询（同 listFormDesign，保持一致的 5 函数接口） */
export const getFormDesign = (id: ID) =>
  http.get<ApiResponse<FormDesignVO>>(`${PREFIX}/${id}`);

/** 创建填报设计 */
export const createFormDesign = (data: FormDesignCreateDTO) =>
  http.post<ApiResponse<FormDesignVO>>(PREFIX, data);

/** 更新填报设计 */
export const updateFormDesign = (data: FormDesignUpdateDTO) =>
  http.put<ApiResponse<FormDesignVO>>(`${PREFIX}/${data.id}`, data);

/** 删除填报设计 */
export const deleteFormDesign = (id: ID) =>
  http.delete<ApiResponse<void>>(`${PREFIX}/${id}`);

// ========== 扩展操作 ==========

/** 获取模型的填报设计（按 modelId + version 查询） */
export const getFormDesignByModel = (modelId: ID, version?: number) =>
  http.get<ApiResponse<FormDesignVO>>(`${PREFIX}/model/${modelId}`, { params: { version } });

/** 导出填报设计 */
export const exportFormDesign = (modelId: ID, version?: number): Promise<Blob> =>
  http.get(`${PREFIX}/export`, { params: { modelId, version }, responseType: 'blob' }) as unknown as Promise<Blob>;
