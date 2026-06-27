# API 全局约定

> 最后更新：2026-06-17

> **本节为占位**。模块级接口细节已迁入 `docs/modules/{module}/api.md`。

## 1. 响应格式

所有 API 返回统一结构：

```typescript
interface ApiResponse<T = unknown> {
  success: boolean;
  data?: T;
  message?: string;
  code?: string;
}
```

分页响应：

```typescript
interface PaginatedData<T> {
  rows: T[];
  total: number;
}
```

## 2. API 函数命名（5 件套）

每个业务实体必须导出 5 个函数（顺序固定）：

```typescript
listEntity(query: EntityQuery): Promise<PaginatedData<EntityVO>>;
getEntity(id: ID): Promise<EntityVO>;
createEntity(dto: EntityCreateDTO): Promise<EntityVO>;
updateEntity(dto: EntityUpdateDTO): Promise<EntityVO>;
deleteEntity(id: ID): Promise<void>;
```

## 3. 路径前缀

```
/api/v1/{module}/{entity}
```

## 4. 错误码

- HTTP 401 → 调 `logout()`，清 token
- HTTP 403 → 提示"无访问权限"
- HTTP 500 → 提示"服务器错误"
- 业务错误（`success === false`）→ 用 `TpMessage.error` 提示 message

## 5. 认证方式

`Authorization: Bearer <token>` 请求头（从 `@mdm/core/auth` 的 `getToken()` 读取）。

## 6. 模块级 API 文档索引

- [model-design](../modules/model-design/api.md)
- [metamanage-data](../modules/metamanage-data/api.md)
- [metamanage-datatype](../modules/metamanage-datatype/api.md)
