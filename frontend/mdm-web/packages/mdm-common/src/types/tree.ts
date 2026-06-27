/**
 * common/types/tree.ts - 树结构通用类型
 *
 * 分类树、主题域树、组织树等场景复用。
 */
import type { ID } from './base';

/** 通用树节点（最小形态） */
export interface TreeNode {
  id: ID;
  name: string;
  parentId: ID | null;
  isLeaf: boolean;
  children?: TreeNode[];
}

/** 带排序的树节点 */
export interface SortableTreeNode extends TreeNode {
  sortOrder: number;
}

/** el-tree-select 选项 */
export interface TreeSelectOption {
  value: ID;
  label: string;
  children?: TreeSelectOption[];
  disabled?: boolean;
  isLeaf?: boolean;
}

/** 树查询参数 */
export interface TreeQuery {
  /** 父节点 ID（懒加载场景） */
  parentId?: ID | null;
  /** 关键字搜索 */
  keyword?: string;
}

/** 树节点懒加载返回 */
export interface LazyTreeNode {
  id: ID;
  name: string;
  isLeaf: boolean;
}
