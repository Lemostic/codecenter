---
description: API 函数与类型系统规范 — 5函数命名铁律、错误处理模式、基座类型（BaseEntity/PaginationParams/PaginatedData）、业务实体5件套、国际化Key规范、ESLint配置。
paths:
  - "**/*.vue"
  - "**/*.ts"
---

## 1 API 函数规范

### 1.1 命名铁律

| ✅ 允许 | ❌ 禁止 |
|---------|---------|
| `listEntity` | `queryEntity`、`fetchEntities`、`getList`、`searchEntity` |
| `getEntity` | `findEntity`、`queryEntityById`、`getDetail` |
| `createEntity` | `addEntity`、`insertEntity`、`saveEntity` |
| `updateEntity` | `modifyEntity`、`editEntity`、`saveEntity` |
| `deleteEntity` | `removeEntity`、`delEntity`、`destroyEntity` |

### 1.2 铁律

- ❌ 禁止使用 `axios` 原生，必须用项目封装的 HTTP 实例（`import { http } from '@mdm/core/http'`）
- ❌ 禁止用 `get/post/put/del` 解构导入（统一使用 `http` 实例）
- ❌ 禁止函数名前缀加 `api.`（如 `api.listEntity`）
- ❌ 禁止返回 `any`，必须明确泛型
- ❌ 禁止省略 5 个函数中的任何一个（即使目前不用）
- ✅ 函数顺序固定：list → get → create → update → delete
- ✅ 路径前缀统一：`/api/v1/{module}/{entity}`
- ✅ Token 注入、业务错误码解包、401 自动跳转登录 均由 `@mdm/core/http` 拦截器处理，业务代码无需关心

### 1.3 错误处理标准模式

```typescript
const loadData = async () => {
  loading.value = true;
  try {
    const res = await listEntity(query.value);
    tableData.value = res.data.list;
    total.value = res.data.total;
  } catch (error) {
    TpMessage.error('加载失败');
    console.error('[loadData]', error);
  } finally {
    loading.value = false;
  }
};
```

- ❌ 禁止省略 `catch`（异步错误必须处理）
- ❌ 禁止只 `console.log`（必须用统一消息组件提示用户）
- ✅ `finally` 中必须重置 `loading`
- ✅ 错误日志必须带 `console.error('[方法名]', error)` 前缀
- ✅ 业务码错误由拦截器统一处理（无需在业务代码中手动判断 `res.success`）

---

## 2 类型系统规范

### 2.1 基座类型模式

每个项目应定义统一的基座类型，供所有业务实体继承：

```typescript
// @mdm/core/types/base.ts
export type ID = string;
export interface BaseEntity {
  id: ID;
  name: string;
  description?: string;
  createdAt: string;       // ISO 8601
  updatedAt: string;
  createdBy?: string;
  updatedBy?: string;
}
export interface PaginationParams { pageNum: number; pageSize: number; }
export interface PaginatedData<T> { list: T[]; total: number; pageNum: number; pageSize: number; }
```

```typescript
// @mdm/core/types/api.ts
export interface ApiResponse<T = unknown> {
  success: boolean;
  data: T;
  message?: string;
  code?: string;
}
```

### 2.2 业务实体 5 件套（强约束）

每个业务实体必须导出以下 5 个类型，不得省略任何一个：

```typescript
// modules/{m}/types/{entity}.ts
// 5 个类型必须全导出，缺一不可
import type { BaseEntity, ID, PaginationParams } from '@mdm/common/types/base';

// 1. 数据库实体（与表结构对齐）
export interface {Entity}Entity extends BaseEntity { name: string; /* ... */ }
// 2. 视图对象（前端展示用）
export interface {Entity}VO extends {Entity}Entity { displayName: string; /* ... */ }
// 3. 创建参数（不包含 id/审计字段）
export interface {Entity}CreateDTO { name: string; /* 必填业务字段 */ }
// 4. 更新参数（继承创建参数 + id）
export interface {Entity}UpdateDTO extends Partial<{Entity}CreateDTO> { id: ID; }
// 5. 查询参数（分页 + 筛选）
export interface {Entity}Query extends PaginationParams { name?: string; /* 筛选条件 */ }
```

| 规则 | 说明 |
|------|------|
| ❌ 5 件套必须全导出 | 缺一个视为不合格 |
| ❌ DTO 不得包含 id/createdAt 等 | 这些字段由后端生成 |
| ❌ VO 不得直接传给 API | 必须用 DTO 转换 |
| ✅ 5 件套顺序固定 | Entity → VO → CreateDTO → UpdateDTO → Query |

---

## 3 国际化规范

### 3.1 Key 命名：4 段式 + 命名空间

```
格式：{moduleName}.{pageName}.{elementName}.{actionName}

示例：
✅ moduleA.list.btn.create        （模块A.列表页.按钮.新增）
✅ moduleA.editor.title            （模块A.编辑页.标题）
❌ common.save                     （无命名空间，多模块必冲突）
❌ moduleA.btn.create              （缺 pageName 段）
```

### 3.2 语言包结构

```typescript
// modules/{m}/locales/zh-CN.ts
export default {
  list: {
    title: '{Entity}列表',
    btn: { create: '新增', edit: '编辑', delete: '删除' },
    col: { name: '名称', code: '编码', status: '状态' },
  },
  editor: {
    title: '编辑{Entity}',
    label: { name: '名称', code: '编码' },
  },
  message: {
    createSuccess: '创建成功',
    deleteConfirm: '确定要删除"{name}"吗？',
  },
};
```

### 3.3 铁律

- ❌ 禁止使用通用 key（`common.save`、`app.confirm`）
- ❌ 禁止中英文混在一个 key
- ✅ Key 必须以模块名开头（camelCase）
- ✅ 嵌套层级不超过 4 层
- ✅ zh-CN 与 en-US 语言包同步维护
- ✅ 翻译缺失时 fallback 到 i18n 兜底文案（在 `core/i18n/` 中定义）

---

## 4 组件拆分（软建议）

- 单文件 `.vue` ≤ 600 行（不含 `<style>`）——超过仅作 PR 评审提示，不强制
- 若模板中出现两种独立业务实体或两组互不相关的状态机，建议按"职责"拆为子组件
- 拆出的子组件放模块内 `components/` 目录

---

## 5 ESLint 配置建议

```javascript
// eslint.config.js
{
  rules: {
    'vue/component-tags-order': ['error', { order: ['script', 'template', 'style'] }],
    'vue/component-name-in-template-casing': ['error', 'PascalCase'],
    '@typescript-eslint/no-explicit-any': 'error',
    '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
    'no-debugger': 'error',
    'no-console': ['warn', { allow: ['warn', 'error'] }],
  }
}
```

> 项目可根据模块结构补充 `no-restricted-imports` 规则，限制跨模块深路径引用。
