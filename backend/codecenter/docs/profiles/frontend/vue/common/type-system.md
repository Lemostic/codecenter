| 字段 | 值 |
|------|-----|
| 版本 | 2.0 |
| 层级 | L1 |
| 包类型 | frontend-module |
| 引入条件 | `fingerprint.profiles contains 'frontend-vue'` |
| 所属前端包 | `frontend-vue/common/` |
| 适用版本 | Vue 3 |
| 依赖规范 | `common/architecture.md` |

# 类型系统规范

> 本文件定义前端基座类型与业务实体 5 件套（Entity/VO/CreateDTO/UpdateDTO/Query）的强约束。
> 本文件规则适用于 Vue 3 前端项目。

---

## 1 基座类型模式

**PROF-FE-501** 每个项目应定义统一的基座类型，供所有业务实体继承。 [MUST]

```typescript
// common/types/base.ts

// ID 类型：string | number 由项目统一决定
export type ID = string;

// 基座实体：所有业务实体 MUST 继承
export interface BaseEntity {
  id: ID;
  createdAt: string;       // ISO 8601
  updatedAt: string;
  createdBy: ID;
  updatedBy: ID;
}

// 分页参数
export interface PaginationParams {
  page: number;
  pageSize: number;
}

// 分页响应
export interface PaginatedData<T> {
  rows: T[];
  total: number;
}
```

```typescript
// common/types/api.ts

// 统一 API 响应格式
export interface ApiResponse<T = unknown> {
  success: boolean;
  data?: T;
  message?: string;
  code?: string;
}
```

---

## 2 业务实体 5 件套（强约束）

**PROF-FE-502** 每个业务实体 MUST 导出以下 5 个类型，缺一个视为不合格。 [MUST]

| # | 类型 | 用途 | 字段要求 |
|---|------|------|----------|
| 1 | `Entity` | 数据库实体 | 与后端表结构对齐，继承 `BaseEntity` |
| 2 | `VO` | 视图对象（前端展示用） | 继承 `Entity`，增加展示字段 |
| 3 | `CreateDTO` | 创建参数 | 不含 id/createdAt/updatedAt 等后端生成字段 |
| 4 | `UpdateDTO` | 更新参数 | 继承 `Partial<CreateDTO>` + `id` |
| 5 | `Query` | 查询参数 | 继承 `PaginationParams` + 业务筛选字段 |

**PROF-FE-503** 5 件套顺序固定：`Entity` → `VO` → `CreateDTO` → `UpdateDTO` → `Query`。 [MUST]

**PROF-FE-504** 5 件套 MUST 全部导出（`export interface`），不得省略。 [MUST]

```typescript
// modules/{m}/types/{entity}.ts
// 5 个类型必须全导出，缺一不可
import type { BaseEntity, ID, PaginationParams } from '@/common/types/base';

// 1. 数据库实体（与表结构对齐）
export interface UserEntity extends BaseEntity {
  username: string;
  email: string;
  status: 'enabled' | 'disabled';
  // ...其他业务字段
}

// 2. 视图对象（前端展示用）
export interface UserVO extends UserEntity {
  displayName: string;          // 展示名
  departmentName: string;       // 部门名（关联展示）
  roleList: string[];           // 角色列表
}

// 3. 创建参数（不包含 id/审计字段）
export interface UserCreateDTO {
  username: string;
  email: string;
  password: string;
  // 必填业务字段
}

// 4. 更新参数（继承创建参数 + id）
export interface UserUpdateDTO extends Partial<UserCreateDTO> {
  id: ID;
}

// 5. 查询参数（分页 + 筛选）
export interface UserQuery extends PaginationParams {
  username?: string;
  status?: 'enabled' | 'disabled';
  departmentId?: ID;
  // 筛选条件
}
```

---

## 3 字段约束铁律

| 规则 | 说明 |
|------|------|
| **PROF-FE-505** | `CreateDTO` 不得包含 `id` / `createdAt` / `updatedAt` 等后端生成字段。[MUST] |
| **PROF-FE-506** | `VO` 不得直接传给 API（必须用 DTO 转换）。[MUST] |
| **PROF-FE-507** | `Query` MUST 继承 `PaginationParams`。[MUST] |
| **PROF-FE-508** | `UpdateDTO` MUST 继承 `Partial<CreateDTO>` + `id`。[MUST] |

### VO → DTO 转换示例

```typescript
// 正确：显式转换
const submitData: UserCreateDTO = {
  username: form.displayName,           // VO 字段 → DTO 字段
  email: form.email,
  password: form.password,
};
await createUser(submitData);

// 错误：直接传 VO
await createUser(form);  // ❌ VO 包含 displayName、departmentName 等后端不需要的字段
```

---

## 4 跨模块基座类型位置

| 类型 | 位置 |
|------|------|
| `ID`、`BaseEntity`、`PaginationParams`、`PaginatedData` | `common/types/base.ts` |
| `ApiResponse` | `common/types/api.ts` |
| 业务实体 5 件套 | `modules/{m}/types/{entity}.ts` |
| 业务专属枚举 | `modules/{m}/constants/{name}.ts` |
| 单文件内部使用 | 内联，不单独导出 |

**PROF-FE-509** 跨模块基座类型 MUST 放 `common/types/`，禁止放在具体模块内。 [MUST]

**PROF-FE-510** 模块业务实体 5 件套 MUST 放在 `modules/{m}/types/{entity}.ts`，禁止拆成多个文件。 [MUST]

---

## 5 类型命名规范

| 类型 | 命名规则 | 示例 |
|------|---------|------|
| 基座类型 | PascalCase，描述用途 | `BaseEntity`, `PaginationParams` |
| 业务实体 | `{Entity}Entity` | `UserEntity` |
| 视图对象 | `{Entity}VO` | `UserVO` |
| 创建参数 | `{Entity}CreateDTO` | `UserCreateDTO` |
| 更新参数 | `{Entity}UpdateDTO` | `UserUpdateDTO` |
| 查询参数 | `{Entity}Query` | `UserQuery` |
| 枚举 | `{Entity}{Field}Enum` 或 `SCREAMING_SNAKE` | `UserStatusEnum` |
| 泛型参数 | `T` / `K` / `V` | `PaginatedData<T>` |

---

## 6 禁止行为清单

- ❌ 禁止省略 5 件套中的任何一个
- ❌ 禁止 `CreateDTO` 包含 `id` / `createdAt` 等后端生成字段
- ❌ 禁止 `VO` 直接传给 API 函数
- ❌ 禁止把基座类型放在模块内
- ❌ 禁止用 `any`（必要时用 `unknown` + 类型守卫）
- ❌ 禁止用 `as any` 绕过类型检查
- ❌ 禁止把 5 件套拆成多个文件

---

## 7 与 L0 通用规范的关系

| L0 规范 | 与本文件的关系 |
|---------|---------------|
| `universal/naming-conventions.md` | 命名规则（PascalCase）由 L0 提供，本文件细化业务实体的 5 件套命名 |

---

*本文件规则适用于 Vue 3 前端项目。*
