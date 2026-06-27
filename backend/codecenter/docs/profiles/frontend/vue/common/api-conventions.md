| 字段 | 值 |
|------|-----|
| 版本 | 2.0 |
| 层级 | L1 |
| 包类型 | frontend-module |
| 引入条件 | `fingerprint.profiles contains 'frontend-vue'` |
| 所属前端包 | `frontend-vue/common/` |
| 适用版本 | Vue 3 |
| 依赖规范 | `common/architecture.md`（依赖方向与文件定位） |

# API 函数规范

> 本文件定义前端 API 函数层的铁律——5 件套命名、路径前缀、错误处理标准模式、HTTP 实例使用。
> 本文件规则适用于 Vue 3 前端项目。

---

## 1 命名铁律（5 件套）

**PROF-FE-401** 每个业务实体 MUST 导出 5 个 API 函数，缺一不可。 [MUST]

| ✅ 允许（强制） | ❌ 禁止 |
|---------|---------|
| `listEntity` | `queryEntity`、`fetchEntities`、`getList`、`searchEntity` |
| `getEntity` | `findEntity`、`queryEntityById`、`getDetail` |
| `createEntity` | `addEntity`、`insertEntity`、`saveEntity` |
| `updateEntity` | `modifyEntity`、`editEntity`、`saveEntity` |
| `deleteEntity` | `removeEntity`、`delEntity`、`destroyEntity` |

**PROF-FE-402** 5 个函数顺序固定：`list` → `get` → `create` → `update` → `delete`。 [MUST]

**PROF-FE-403** 函数名前 MUST NOT 加 `api.` 前缀（如 `api.listEntity`）。 [MUST]

---

## 2 路径前缀与 HTTP 实例

**PROF-FE-404** API 路径前缀统一：`/api/v1/{module}/{entity}`。 [MUST]

```typescript
// modules/user/api/user.ts
export const listUser = (params: UserQuery) =>
  http.get<PageResult<User>>('/api/v1/user/users', { params });

export const getUser = (id: ID) =>
  http.get<User>(`/api/v1/user/users/${id}`);

export const createUser = (data: UserCreateDTO) =>
  http.post<ID>('/api/v1/user/users', data);

export const updateUser = (id: ID, data: UserUpdateDTO) =>
  http.put<void>(`/api/v1/user/users/${id}`, data);

export const deleteUser = (id: ID) =>
  http.delete<void>(`/api/v1/user/users/${id}`);
```

**PROF-FE-405** 禁止使用 `axios` 原生，必须用项目封装的 HTTP 实例（`http`）。 [MUST]

**PROF-FE-406** 禁止用 `get/post/put/del` 解构导入（统一使用 `http` 实例）。 [MUST]

---

## 3 类型约束

**PROF-FE-407** API 函数 MUST 定义明确的 TypeScript 类型（参数与返回值），MUST NOT 使用 `any`。 [MUST]

```typescript
// 正确
export const listUser = (params: UserQuery): Promise<PageResult<User>> =>
  http.get<PageResult<User>>('/api/v1/user/users', { params });

// 错误：缺少类型
export const listUser = (params) =>
  http.get('/api/v1/user/users', { params });
```

**PROF-FE-408** 业务实体类型遵循 5 件套（Entity / VO / CreateDTO / UpdateDTO / Query），DTO 不含 id/createdAt 等后端生成字段。 [MUST]

> 5 件套详见 `common/type-system.md`。

---

## 4 错误处理标准模式

**PROF-FE-409** 异步操作 MUST 有 `try/catch/finally`，禁止裸 `await`。 [MUST]

**PROF-FE-410** `catch` 中 MUST 使用项目统一消息组件提示用户，禁止只 `console.log`。 [MUST]

**PROF-FE-411** `finally` 中 MUST 重置 `loading` 状态。 [MUST]

**PROF-FE-412** 错误日志 MUST 带 `console.error('[方法名]', error)` 前缀。 [MUST]

```typescript
// ✅ 正确：错误处理标准模式
const loadData = async () => {
  loading.value = true;
  try {
    const res = await listUser(query.value);
    tableData.value = res.data?.rows ?? [];
    total.value = res.data?.total ?? 0;
  } catch (error) {
    // 使用项目统一消息组件提示用户
    showMessage('error', '加载失败');
    console.error('[loadData]', error);
  } finally {
    loading.value = false;
  }
};
```

**PROF-FE-413** 业务码错误由 HTTP 拦截器统一处理（无需在业务代码中手动判断 `res.success`）。 [MUST]

> 拦截器处理 401 跳转登录、403 无权限提示、500 服务异常等通用错误码。业务专属错误码在业务模块内自定义。

---

## 5 异步操作必备要素

| 要素 | 是否必须 | 说明 |
|------|---------|------|
| `try/catch/finally` | ✅ 必须 | 错误处理标准模式 |
| `loading` 状态 | ✅ 必须 | 触发与结束均要重置 |
| 消息提示 | ✅ 必须 | 成功/失败都给用户反馈 |
| 错误日志 | ✅ 必须 | 带方法名前缀便于排查 |
| 超时处理 | ⚠️ 推荐 | HTTP 实例统一配置 |
| 取消请求 | ⚠️ 推荐 | 重复请求防抖 |

---

## 6 API 文件位置决策树

```
→ modules/{m}/api/{entity}.ts
  ├─ 跨模块通用 API → common/api/{name}Api.ts
  └─ 模块业务 API → modules/{m}/api/{entity}.ts
```

```typescript
// 模块 API 文件示例
// modules/user/api/user.ts
import { http } from '@/core/http';
import type { User, UserCreateDTO, UserUpdateDTO, UserQuery, PageResult } from '@/modules/user/types/user';
import type { ID } from '@/common/types/base';

export const listUser = (params: UserQuery) =>
  http.get<PageResult<User>>('/api/v1/user/users', { params });

export const getUser = (id: ID) =>
  http.get<User>(`/api/v1/user/users/${id}`);

export const createUser = (data: UserCreateDTO) =>
  http.post<ID>('/api/v1/user/users', data);

export const updateUser = (id: ID, data: UserUpdateDTO) =>
  http.put<void>(`/api/v1/user/users/${id}`, data);

export const deleteUser = (id: ID) =>
  http.delete<void>(`/api/v1/user/users/${id}`);
```

---

## 7 禁止行为清单

- ❌ 禁止使用 `axios` 原生方法
- ❌ 禁止用 `get/post/put/del` 解构导入
- ❌ 禁止函数名前缀加 `api.`
- ❌ 禁止返回 `any`（必须明确泛型）
- ❌ 禁止省略 5 个函数中的任何一个
- ❌ 禁止省略 `catch`（异步错误必须处理）
- ❌ 禁止只 `console.log`（必须用统一消息组件提示用户）
- ❌ 禁止省略 `finally` 中的 `loading` 重置

---

## 8 与 HTTP 拦截器的关系

| 关注点 | 在 HTTP 拦截器 | 在 API 函数 |
|--------|--------------|------------|
| 401 未登录 | 跳转登录页 | 无需处理 |
| 403 无权限 | 提示"无权限" | 无需处理 |
| 500 服务异常 | 提示"服务异常" | 无需处理 |
| 业务码非 0 | 由拦截器 toast | 无需处理 |
| 业务专属错误 | — | 业务模块内 try/catch 处理 |

**PROF-FE-414** 业务专属错误（业务码非 0 且拦截器未处理的）由调用方在 `catch` 中处理。 [MUST]

---

*本文件规则适用于 Vue 3 前端项目。*
