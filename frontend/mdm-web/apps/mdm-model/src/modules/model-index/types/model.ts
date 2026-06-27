/**
 * modules/model-index/types/model.ts
 *
 * 主数据模型 - 业务实体类型（5 件套）
 */
import type { BaseEntity, ID, PaginationParams } from '@mdm/common/types/base';

// ========== 枚举/常量 ==========

/** 模型类型：普通模型 / 复合模型 / 分类模型 */
export type ModelType = 'normal' | 'composite' | 'classification';

export const MODEL_TYPE_OPTIONS: { value: ModelType; label: string }[] = [
  { value: 'normal', label: '普通模型' },
  { value: 'composite', label: '复合模型' },
  { value: 'classification', label: '分类模型' },
];

export const MODEL_TYPE_LABEL: Record<ModelType, string> = {
  normal: '普通模型',
  composite: '复合模型',
  classification: '分类模型',
};

/** 模型状态：编辑中 / 生效 / 停用 / 审核中 */
export type ModelStatus = 'draft' | 'active' | 'disabled' | 'reviewing';

export const MODEL_STATUS_OPTIONS: { value: ModelStatus; label: string }[] = [
  { value: 'draft', label: '编辑中' },
  { value: 'active', label: '生效' },
  { value: 'disabled', label: '停用' },
  { value: 'reviewing', label: '审核中' },
];

export const MODEL_STATUS_LABEL: Record<ModelStatus, string> = {
  draft: '编辑中',
  active: '生效',
  disabled: '停用',
  reviewing: '审核中',
};

export const MODEL_STATUS_TAG: Record<ModelStatus, 'info' | 'success' | 'warning' | 'danger'> = {
  draft: 'info',
  active: 'success',
  disabled: 'warning',
  reviewing: 'danger',
};

/** 模型状态对应圆点颜色 */
export const MODEL_STATUS_DOT_COLOR: Record<ModelStatus, string> = {
  draft: '#909399',
  active: '#67C23A',
  disabled: '#E6A23C',
  reviewing: '#409EFF',
};

// ========== 子模型（复合模型用） ==========

export interface SubModel {
  id: ID;
  name: string;
  code: string;
  tableName: string;
  description?: string;
  status: ModelStatus;
  sortOrder: number;
}

// ========== 关联标准文件 ==========

export interface StandardFile {
  id: ID;
  name: string;
  url: string;
  fileType: string;
}

// ========== 1. 数据库实体 ==========

export interface ModelEntity extends BaseEntity {
  /** 模型名称（全局唯一） */
  name: string;
  /** 模型编码（全局唯一） */
  code: string;
  /** 表名称（同一数据源下唯一） */
  tableName: string;
  /** 模型类型 */
  modelType: ModelType;
  /** 模型状态 */
  status: ModelStatus;
  /** 版本号（V1, V2...） */
  version: number;
  /** 备注 */
  description: string;
  /** 所属主题域 ID */
  topicId: ID;
  /** 目标数据源 ID */
  datasourceId: ID;
  /** 模型密级（可选，开启密级管理时才有） */
  secretLevel?: string;
  /** 子模型列表（复合模型时非空） */
  subModels?: SubModel[];
  /** 关联标准文件 */
  standardFiles?: StandardFile[];
}

// ========== 2. 视图对象 ==========

export interface ModelVO extends ModelEntity {
  /** 状态标签文本 */
  statusLabel: string;
  /** 类型标签文本 */
  modelTypeLabel: string;
  /** 版本显示文本（如 V1） */
  versionLabel: string;
  /** 所属分类名称 */
  topicName?: string;
  /** 创建人名称 */
  creatorName: string;
}

// ========== 3. 创建参数 ==========

export interface ModelCreateDTO {
  name: string;
  code: string;
  tableName: string;
  modelType: ModelType;
  description?: string;
  topicId: ID;
  datasourceId: ID;
  secretLevel?: string;
  subModels?: Omit<SubModel, 'id' | 'status'>[];
  standardFileIds?: ID[];
}

// ========== 4. 更新参数 ==========

export interface ModelUpdateDTO extends Partial<Omit<ModelCreateDTO, 'modelType'>> {
  id: ID;
}

// ========== 5. 查询参数 ==========

export interface ModelQuery extends PaginationParams {
  /** 名称或编码模糊搜索 */
  keyword?: string;
  /** 状态过滤 */
  status?: ModelStatus;
  /** 模型类型过滤 */
  modelType?: ModelType;
  /** 主题域 ID 过滤 */
  topicId?: ID;
  /** 是否显示级联数据（包含子分类的模型） */
  cascade?: boolean;
  /** 排序字段 */
  sortBy?: string;
  /** 排序方向 */
  sortOrder?: 'asc' | 'desc';
}
