/**
 * modules/model-design/types/form-design.ts
 *
 * 填报设计 - 业务实体类型（5 件套）
 */
import type { BaseEntity, ID, PaginationParams } from '@mdm/common/types/base';

// ========== 枚举/常量 ==========

/** 布局列数 */
export type LayoutColumns = 1 | 2 | 3 | 4;

/** 控件类型 */
export type ControlType =
  | 'text' | 'number' | 'date' | 'datetime'
  | 'select' | 'multiselect' | 'radio' | 'checkbox'
  | 'textarea' | 'file' | 'tree-select' | 'cascader';

export const CONTROL_TYPE_LABEL: Record<ControlType, string> = {
  text: '文本框',
  number: '数字框',
  date: '日期选择',
  datetime: '日期时间',
  select: '下拉列表',
  multiselect: '多选下拉',
  radio: '单选按钮',
  checkbox: '复选框',
  textarea: '文本域',
  file: '文件上传',
  'tree-select': '树形选择',
  cascader: '级联选择',
};

/** 输入方式 */
export type InputMethod = 'single-line' | 'multi-line' | 'dropdown' | 'button-group' | 'dialog' | 'auto' | 'file-selector' | 'readonly';

export const INPUT_METHOD_LABEL: Record<InputMethod, string> = {
  'single-line': '单行输入框',
  'multi-line': '多行输入框',
  'dropdown': '下拉框',
  'button-group': '按钮组',
  'dialog': '弹窗',
  'auto': '自动',
  'file-selector': '文件选择器',
  'readonly': '不可编辑',
};

/** 提示效果 */
export type TooltipEffect = 'text' | 'icon' | 'bubble';

export const TOOLTIP_EFFECT_LABEL: Record<TooltipEffect, string> = {
  text: '文本提示',
  icon: '图标提示',
  bubble: '隐藏气泡提示',
};

/** 展现方式 */
export type DisplayMode = 'list' | 'tree' | 'tree-list';

export const DISPLAY_MODE_LABEL: Record<DisplayMode, string> = {
  list: '列表',
  tree: '树',
  'tree-list': '树列表',
};

/** 字段布局方式 */
export type FieldLayout = 'horizontal' | 'vertical';

export const FIELD_LAYOUT_LABEL: Record<FieldLayout, string> = {
  horizontal: '左右',
  vertical: '上下',
};

/** 显隐条件运算符 */
export type ConditionOperator = 'eq' | 'neq' | 'gt' | 'lt' | 'gte' | 'lte' | 'contains' | 'in' | 'empty' | 'notEmpty';

/** 条件逻辑关系 */
export type ConditionLogic = 'and' | 'or';

// ========== 子类型 ==========

/** 属性分组 */
export interface FormGroup {
  id: ID;
  /** 分组名称 */
  name: string;
  /** 父分组 ID（null 为根分组） */
  parentId: ID | null;
  /** 排序号 */
  sortOrder: number;
  /** 包含的属性 ID 列表 */
  attributeIds: ID[];
  /** 是否可折叠 */
  collapsible: boolean;
  /** 默认折叠 */
  defaultCollapsed: boolean;
}

/** 属性样式配置 */
export interface FieldStyle {
  /** 属性 ID */
  attributeId: ID;
  /** 控件类型 */
  controlType: ControlType;
  /** 占位列数 */
  colSpan: number;
  /** 是否独占一行 */
  fullRow: boolean;
  /** 占位提示文字 */
  placeholder?: string;
  /** 是否只读 */
  readonly: boolean;
  /** 输入方式 */
  inputMethod?: InputMethod;
  /** 输入框高度倍数 (1-10) */
  heightMultiple?: number;
  /** 属性提示文字 (200字符) */
  tooltip?: string;
  /** 提示效果 */
  tooltipEffect?: TooltipEffect;
  /** 是否公式字段 */
  isFormula?: boolean;
  /** 额外配置（JSON） */
  extraConfig?: Record<string, unknown>;
}

/** 树列样式配置 */
export interface TreeStyleConfig {
  /** 展现方式 */
  displayMode: DisplayMode;
  /** 子字段 ID (树/树列表) */
  childFieldId?: ID;
  /** 父字段 ID (树/树列表) */
  parentFieldId?: ID;
  /** 显示字段 ID 列表 */
  displayFieldIds?: ID[];
  /** 级联显示父模型 */
  cascadeParentModel?: boolean;
  /** 树对象字段 ID (树列表) */
  treeObjectFieldId?: ID;
}

/** 显隐条件 */
export interface VisibilityCondition {
  id: ID;
  /** 目标属性 ID（被控制的属性） */
  targetAttributeId: ID;
  /** 条件逻辑 */
  logic: ConditionLogic;
  /** 条件组 */
  conditions: {
    /** 条件属性 ID */
    sourceAttributeId: ID;
    /** 运算符 */
    operator: ConditionOperator;
    /** 比较值 */
    value: string;
  }[];
  /** 条件满足时的动作 */
  action: 'show' | 'hide';
}

/** 排序字段配置 */
export interface SortFieldConfig {
  /** 属性 ID */
  attributeId: ID;
  /** 排序方向 */
  direction: 'asc' | 'desc';
  /** 排序优先级 */
  priority: number;
}

// ========== 1. 数据库实体 ==========

export interface FormDesignEntity extends BaseEntity {
  /** 所属模型 ID */
  modelId: ID;
  /** 版本号 */
  version: number;
  /** 布局列数 */
  layoutColumns: LayoutColumns;
  /** 字段布局方式 */
  fieldLayout: FieldLayout;
  /** 属性分组配置 */
  groups: FormGroup[];
  /** 属性样式配置 */
  fieldStyles: FieldStyle[];
  /** 显隐条件 */
  visibilityConditions: VisibilityCondition[];
  /** 排序字段 */
  sortFields: SortFieldConfig[];
  /** 树列样式配置 */
  treeStyle?: TreeStyleConfig;
}

// ========== 2. 视图对象 ==========

export interface FormDesignVO extends FormDesignEntity {
  /** 模型名称（冗余） */
  modelName?: string;
}

// ========== 3. 创建参数 ==========

export interface FormDesignCreateDTO {
  modelId: ID;
  layoutColumns?: LayoutColumns;
  fieldLayout?: FieldLayout;
  groups?: FormGroup[];
  fieldStyles?: FieldStyle[];
  visibilityConditions?: VisibilityCondition[];
  sortFields?: SortFieldConfig[];
  treeStyle?: TreeStyleConfig;
}

// ========== 4. 更新参数 ==========

export interface FormDesignUpdateDTO {
  id: ID;
  layoutColumns?: LayoutColumns;
  fieldLayout?: FieldLayout;
  groups?: FormGroup[];
  fieldStyles?: FieldStyle[];
  visibilityConditions?: VisibilityCondition[];
  sortFields?: SortFieldConfig[];
  treeStyle?: TreeStyleConfig;
}

// ========== 5. 查询参数 ==========

export interface FormDesignQuery {
  /** 所属模型 ID */
  modelId: ID;
  /** 版本号 */
  version?: number;
}
