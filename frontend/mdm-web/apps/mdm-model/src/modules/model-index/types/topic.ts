/**
 * modules/model-index/types/topic.ts
 *
 * 主题域 - 业务实体类型
 */
import type { BaseEntity, ID, PaginationParams } from '@mdm/common/types/base';

// ========== 视图对象 ==========

export interface TopicVO extends BaseEntity {
  name: string;
  code?: string;
  parentId: ID | null;
  sortOrder: number;
  level: number;
  isLeaf: boolean;
  hasModel: boolean;
  description?: string;
  creatorName?: string;
}

// ========== 创建参数 ==========

export interface TopicCreateDTO {
  name: string;
  parentId: ID | null;
  sortOrder?: number;
  description?: string;
}

// ========== 更新参数 ==========

export interface TopicUpdateDTO extends Partial<TopicCreateDTO> {
  id: ID;
}

// ========== 查询参数 ==========

export interface TopicQuery extends PaginationParams {
  keyword?: string;
  parentId?: ID | null;
}

// ========== 树节点 ==========

export interface TopicTreeNode {
  id: ID;
  name: string;
  label: string;
  parentId: ID | null;
  isLeaf: boolean;
  hasModel: boolean;
  level: number;
  children?: TopicTreeNode[];
}
