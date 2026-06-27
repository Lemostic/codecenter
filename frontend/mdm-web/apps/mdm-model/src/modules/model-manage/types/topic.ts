/**
 * modules/model-design/types/topic.ts
 *
 * 主题域 - 业务实体类型（5 件套）
 */
import type { BaseEntity, ID, PaginationParams } from '@mdm/common/types/base';

// ========== 状态枚举 ==========

/** 主题状态（逻辑删除场景可选，当前需求未明确状态管理，预留） */
export type TopicStatus = 'enabled';

// ========== 1. 数据库实体 ==========

export interface TopicEntity extends BaseEntity {
  /** 主题名称（同父级下唯一） */
  name: string;
  /** 父主题 ID，根节点为 null */
  parentId: ID | null;
  /** 排序号 */
  sortOrder: number;
  /** 备注 */
  description: string;
  /** 是否末级（无子主题） */
  isLeaf: boolean;
  /** 层级深度（根节点为 0） */
  level: number;
  /** 该主题下是否有模型 */
  hasModel?: boolean;
}

// ========== 2. 视图对象 ==========

export interface TopicVO extends TopicEntity {
  children?: TopicVO[];
  /** 内联编辑 UI 状态：是否为新增行（未持久化） */
  isNew?: boolean;
  /** 内联编辑 UI 状态：当前是否处于编辑模式 */
  isEditing?: boolean;
}

// ========== 3. 创建参数 ==========

export interface TopicCreateDTO {
  name: string;
  parentId: ID | null;
  sortOrder: number;
  description?: string;
}

// ========== 4. 更新参数 ==========

export interface TopicUpdateDTO extends Partial<TopicCreateDTO> {
  id: ID;
}

// ========== 5. 查询参数 ==========

export interface TopicQuery extends PaginationParams {
  /** 名称模糊搜索 */
  keyword?: string;
  /** 父节点过滤 */
  parentId?: ID | null;
}

// ========== 树相关 ==========

/** 主题域树节点（懒加载返回） */
export interface TopicTreeNode {
  id: ID;
  name: string;
  isLeaf: boolean;
  parentId: ID | null;
  hasModel?: boolean;
}

/** 导入行 */
export interface TopicImportRow {
  parentPath: string;
  name: string;
  description?: string;
  sortOrder?: number;
}
