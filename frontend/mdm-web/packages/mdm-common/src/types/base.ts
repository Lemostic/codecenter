/**
 * common/types/base.ts - 跨模块基座类型
 *
 * 所有业务实体的基础类型定义。
 *
 * 与 `@mdm/core/types/base` 保持字段一致（`name, description?, createdAt, updatedAt, createdBy, updatedBy`），
 * 实际各包独立定义、互不 re-export——调用方按需 import。
 */

/** 统一 ID 类型 */
export type ID = string;

/** 所有实体的基类 */
export interface BaseEntity {
  id: ID;
  /** 业务名称（默认必填） */
  name: string;
  /** 业务描述（可选） */
  description?: string;
  createdAt: string;
  updatedAt: string;
  createdBy?: string;
  updatedBy?: string;
}

/** 审计信息（混入使用） */
export interface AuditInfo {
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  updatedBy: string;
}

/** 分页请求参数 */
export interface PaginationParams {
  /** 当前页（从 1 开始） */
  page: number;
  /** 每页条数（默认 20） */
  pageSize: number;
}

/** 分页响应数据 */
export interface PaginatedData<T> {
  rows: T[];
  total: number;
}
