description: MDM 特有的 API 5 件套落地、MDM BaseEntity 字段集、pageNum/list 分页字段差异、i18n 与 frontend-vue 完全一致。
---

# API 开发规范（MDM 落地）

> API 函数命名铁律、HTTP 实例、try/catch/finally、5 件套类型等通用规则见 `profiles/frontend/vue/common/api-conventions.md` 与 `common/type-system.md`。本文件仅说明 MDM 与通用规范的具体差异点。

## 1 HTTP 实例导入（差异点）

MDM 项目使用 `@mdm/core/http` 提供的 `http` 实例，而非通用规范中的 `@/core/http`：

```typescript
// ✅ 正确
import { http } from '@mdm/core/http';

// ❌ 错误
import { http } from '@/core/http';        // 单应用别名，MDM 不使用
import axios from 'axios';                  // 禁止原生 axios
```

## 2 路径前缀

| 项 | 值 |
|----|-----|
| API 路径前缀 | `/api/v1/{module}/{entity}` |
| 模块名 | kebab-case，如 `model-design` |
| 实体名 | kebab-case，如 `data-model` |

## 3 类型 5 件套（继承通用规范）

业务实体 5 件套 `Entity` / `VO` / `CreateDTO` / `UpdateDTO` / `Query` 的命名、顺序、字段约束继承通用规范。MDM 仅在 `BaseEntity` 上有差异（见 §4）。

## 4 MDM BaseEntity（差异点）

MDM 主数据业务实体 MUST 继承 `@mdm/common/types/base` 中的 `BaseEntity`，强制 `name` 字段：

```typescript
// packages/common/src/types/base.ts
export type ID = string;

export interface BaseEntity {
  id: ID;
  name: string;            // 主数据名称（MDM 强制字段）
  description?: string;    // 描述（可选）
  createdAt: string;       // ISO 8601
  updatedAt: string;
  createdBy?: ID;
  updatedBy?: ID;
}
```

```typescript
// modules/modelDesign/types/model.ts
import type { BaseEntity, ID } from '@mdm/common/types/base';

// 1. 数据库实体
export interface ModelEntity extends BaseEntity {
  code: string;
  type: 'physical' | 'logical' | 'view';
  domainId: ID;
  // ... 其他业务字段
}

// 2. 视图对象
export interface ModelVO extends ModelEntity {
  domainName: string;      // 关联展示
  fieldCount: number;      // 派生展示
}

// 3. 创建参数（不含 id/createdAt/updatedAt）
export interface ModelCreateDTO {
  name: string;
  code: string;
  type: 'physical' | 'logical' | 'view';
  domainId: ID;
  description?: string;
}

// 4. 更新参数
export interface ModelUpdateDTO extends Partial<ModelCreateDTO> {
  id: ID;
}

// 5. 查询参数（分页 + 筛选）
export interface ModelQuery extends PaginationParams {
  name?: string;
  code?: string;
  domainId?: ID;
  type?: 'physical' | 'logical' | 'view';
}
```

## 5 分页字段差异（关键）

| 项 | 通用 frontend-vue | MDM |
|----|------------------|-----|
| 请求参数 | `page` | `pageNum` |
| 每页大小 | `pageSize` | `pageSize` |
| 响应数据字段 | `rows` | `list` |
| 响应元信息 | `total` | `total` + `pageNum` + `pageSize` |

```typescript
// packages/common/src/types/base.ts（MDM 版本）
export interface PaginationParams {
  pageNum: number;
  pageSize: number;
}

export interface PaginatedData<T> {
  list: T[];          // 非 rows
  total: number;
  pageNum: number;
  pageSize: number;
}
```

```typescript
// API 函数示例
export const listModel = (params: ModelQuery): Promise<ApiResponse<PaginatedData<ModelEntity>>> =>
  http.get<ApiResponse<PaginatedData<ModelEntity>>>('/api/v1/model-design/models', { params });
```

> 后端返回 `{ code, message, data: { list, total, pageNum, pageSize } }`，前端直接透传 `data` 即可。

## 6 国际化（继承通用规范）

i18n 4 段式 Key（`{moduleName}.{pageName}.{elementName}.{actionName}`）、zh-CN/en-US 同步、占位符 `{name}` 等全部继承通用规范。MDM 无差异。

## 7 错误处理（继承通用规范）

`try/catch/finally` 标准模式继承通用规范，仅消息组件从通用 `{EncapsulatedMessage}.error` 落地为 MDM 具体实现 `TpMessage.error`：

```typescript
// ✅ 正确（MDM 落地）
const loadData = async () => {
  loading.value = true;
  try {
    const res = await listModel(query.value);
    tableData.value = res.data?.list ?? [];
    total.value = res.data?.total ?? 0;
  } catch (error) {
    TpMessage.error('加载失败');
    console.error('[loadData]', error);
  } finally {
    loading.value = false;
  }
};
```