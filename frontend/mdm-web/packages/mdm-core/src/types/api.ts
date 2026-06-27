/**
 * API 响应统一格式
 */
import type { PaginatedData } from './base';

/** 统一 API 响应包装 */
export interface ApiResponse<T = unknown> {
  success: boolean;
  message?: string;
  data: T;
  code?: string;
}

/** 分页 API 响应（data 为 PaginatedData） */
export type PaginatedResponse<T> = ApiResponse<PaginatedData<T>>;

/** 统一 ID 参数 */
export interface IdParam {
  id: string;
}
