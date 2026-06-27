/**
 * common/types/api.ts - 统一 API 响应格式
 */
import type { PaginatedData } from './base';

/** 统一 API 响应 */
export interface ApiResponse<T = unknown> {
  success: boolean;
  data?: T;
  message?: string;
  code?: string;
}

/** 分页 API 响应 */
export interface PaginatedResponse<T> {
  success: boolean;
  data?: PaginatedData<T>;
  message?: string;
  code?: string;
}

/** 统一 ID 参数 */
export interface IdParam {
  id: import('./base').ID;
}
