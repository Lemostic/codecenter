/**
 * 跨模块基座类型
 *
 * @mdm/core 与 @mdm/common 各自独立定义（同源结构，互不 re-export），
 * app 内部按路径就近选择（核心层用 @mdm/core/types，组件 / 业务用 @mdm/common/types）。
 *
 * 字段定义与 @mdm/common/types/base 保持一致（详见那里）。
 */
export type ID = string;

/** 所有实体的基类 */
export interface BaseEntity {
  id: ID;
  name: string;
  description?: string;
  createdAt: string;   // ISO 8601
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
  page: number;
  pageSize: number;
}

/** 分页响应数据 */
export interface PaginatedData<T> {
  rows: T[];
  total: number;
}
