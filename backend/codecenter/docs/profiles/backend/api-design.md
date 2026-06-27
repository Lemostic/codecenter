# API 设计规范

| 字段 | 值 |
|------|-----|
| 版本 | 2.0 |
| 层级 | L1 |
| 包类型 | backend |
| 引入条件 | `fingerprint.profiles contains 'api-design'` |
| 适用架构 | 全部后端服务（RESTful API 设计基线） |
| 依赖规范 | `universal/naming-conventions.md`、`universal/exception-handling.md` |
| 互斥规范 | 无 |

> 本包是 L1 后端服务通用基线，定义 RESTful API 的 URL 命名、HTTP 方法语义、统一响应格式、错误码体系与版本管理。
> 配套 `universal/api-design.md`（基础规则）使用，本规范提供完整可落地的细节与示例。

---

## 一、URL 设计原则

### 1.1 RESTful 资源映射

| 操作 | HTTP 方法 | URL 模式 | 说明 |
|------|-----------|----------|------|
| 获取列表 | GET | `/api/v1/{resource}` | 支持分页参数 |
| 获取单个 | GET | `/api/v1/{resource}/{id}` | |
| 创建 | POST | `/api/v1/{resource}` | |
| 全量更新 | PUT | `/api/v1/{resource}/{id}` | 替换整个资源 |
| 部分更新 | PATCH | `/api/v1/{resource}/{id}` | 仅更新指定字段 |
| 删除 | DELETE | `/api/v1/{resource}/{id}` | |
| 嵌套资源 | GET | `/api/v1/{parent}/{id}/{child}` | 父实体下的子资源 |

### 1.2 URL 命名规范

**API-001** 资源名 MUST 使用复数名词（`/users` 而非 `/user`）。 [MUST]

**API-002** URL MUST 使用小写字母 + 中划线（kebab-case），禁止驼峰、下划线、大写。 [MUST]

```
✅ 正确：
/api/v1/users
/api/v1/user-profiles
/api/v1/order-items

❌ 错误：
/api/v1/getUsers      # 动词，方法语义已由 HTTP 表达
/api/v1/user_list     # 下划线
/api/v1/Get_User_Info # 驼峰 + 大写
```

**API-003** URL MUST 以 `/api/v{version}/` 前缀开头，所有 v1+ 接口必须保留向后兼容 ≥ 6 个月。 [MUST]

### 1.3 查询参数规范

| 参数类型 | 命名 | 示例 |
|----------|------|------|
| 分页 | `page`、`pageSize` | `?page=1&pageSize=20` |
| 排序 | `sortBy`、`sortOrder` | `?sortBy=createdAt&sortOrder=desc` |
| 过滤 | 资源字段名 | `?status=active&type=user` |
| 搜索 | `keyword` 或 `q` | `?keyword=zhang` |
| 时间范围 | `startTime`、`endTime`（ISO 8601） | `?startTime=2026-01-01&endTime=2026-12-31` |

**API-004** 分页参数 MUST 使用 `page` + `pageSize`，默认 `page=1`、`pageSize=20`，上限 `pageSize ≤ 200`。 [MUST]

---

## 二、统一响应格式

### 2.1 成功响应

```typescript
interface ApiResponse<T> {
  code: number;        // 业务状态码：0 表示成功
  message: string;     // 提示信息
  data: T;             // 响应数据
  timestamp: string;   // ISO 8601 时间戳
  requestId: string;   // 链路追踪 ID（详见 request-tracing 规范）
}
```

### 2.2 列表响应（分页）

```typescript
interface ApiListResponse<T> {
  code: number;
  message: string;
  data: {
    list: T[];          // 数据列表
    total: number;      // 总记录数
    page: number;       // 当前页（从 1 开始）
    pageSize: number;   // 每页大小
  };
  timestamp: string;
  requestId: string;
}
```

**API-010** 分页响应 MUST 包含 `list` + `total` + `page` + `pageSize` 四字段，禁止使用 `rows` / `records` 等替代命名。 [MUST]

### 2.3 错误响应

```typescript
interface ApiErrorResponse {
  code: number;          // 业务错误码（详见 §四）
  message: string;       // 错误信息（用户可读）
  details?: string;      // 详细信息（仅开发环境返回）
  timestamp: string;
  requestId: string;
}
```

**API-011** 错误响应 `details` 字段 MUST 仅在 `application.env != production` 时返回，生产环境禁止泄露堆栈或内部错误。 [MUST]

---

## 三、HTTP 状态码规范

| 状态码 | 含义 | 适用场景 |
|--------|------|----------|
| 200 | OK | 请求成功 |
| 201 | Created | 资源创建成功（POST 响应） |
| 204 | No Content | 删除成功 / 无返回数据 |
| 400 | Bad Request | 参数错误 / 请求体格式错误 |
| 401 | Unauthorized | 未认证 / token 无效 |
| 403 | Forbidden | 已认证但无权限 |
| 404 | Not Found | 资源不存在 |
| 409 | Conflict | 资源冲突（如重复创建） |
| 422 | Unprocessable Entity | 语义错误（如业务规则校验失败） |
| 429 | Too Many Requests | 限流触发 |
| 500 | Internal Server Error | 服务器内部错误 |
| 503 | Service Unavailable | 服务暂时不可用 |

**API-020** HTTP 状态码 MUST 与业务状态码 `code` 协同使用：HTTP 表示传输层结果，业务 `code` 表示业务层结果。 [MUST]

---

## 四、业务错误码体系

### 4.1 错误码分段约定

| 段位 | 含义 | 示例 |
|------|------|------|
| 0 | 成功 | `0` |
| 1000-1999 | 通用错误 | `1001` 参数错误 |
| 2000-2999 | 用户/认证 | `2001` 用户不存在 |
| 3000-3999 | 业务模块 1 | `3001` 订单不存在 |
| 4000-4999 | 业务模块 2 | `4001` 商品下架 |
| ... | ... | 按业务模块递增 |
| 9000-9999 | 系统错误 | `9001` 数据库异常 |

### 4.2 ErrorCode 枚举参考

```typescript
enum BusinessCode {
  SUCCESS = 0,

  // 通用错误 (1000-1999)
  PARAM_ERROR = 1001,
  NOT_FOUND = 1002,
  UNAUTHORIZED = 1003,
  FORBIDDEN = 1004,
  RATE_LIMIT = 1005,

  // 用户/认证 (2000-2999)
  USER_NOT_FOUND = 2001,
  USER_ALREADY_EXISTS = 2002,
  PASSWORD_ERROR = 2003,
  TOKEN_EXPIRED = 2004,
  TOKEN_INVALID = 2005,

  // 订单 (3000-3999)
  ORDER_NOT_FOUND = 3001,
  ORDER_CANNOT_CANCEL = 3002,
  ORDER_PAID = 3003,
  ORDER_REFUNDING = 3004,

  // 系统错误 (9000-9999)
  SYSTEM_ERROR = 9001,
  DATABASE_ERROR = 9002,
  EXTERNAL_SERVICE_ERROR = 9003,
}
```

**API-030** 业务错误码 MUST 按上述段位划分，跨段位复用视为违规。 [MUST]

**API-031** 错误码 MUST 配套 i18n 资源文件，禁止硬编码字符串。 [MUST]

---

## 五、版本管理

### 5.1 URL 版本 vs Header 版本

**API-040** 后端 API MUST 使用 URL 路径版本（`/api/v1/`），不推荐 Header 版本控制。 [MUST]

理由：
- 直观：URL 即可见版本
- 缓存友好：不同版本独立缓存
- 易测试：浏览器直接访问

### 5.2 兼容性策略

| 情况 | 处理 |
|------|------|
| 新增可选字段 | 向后兼容，无需升级版本 |
| 新增接口 | 直接在当前版本添加 |
| 新增必填字段 | 升级到 v2，旧版本字段保留 |
| 删除字段 | 升级到 v2，旧版本字段标记 deprecated |
| 修改字段语义 | 升级到 v2 |

**API-041** v1 接口 MUST 保持向后兼容 ≥ 6 个月，弃用接口需在响应头加 `Deprecation: true` 并写入 `Sunset` 日期。 [MUST]

### 5.3 版本协商

```http
# 客户端请求
GET /api/v1/users HTTP/1.1
Accept: application/vnd.myapp.v1+json

# 服务端响应
HTTP/1.1 200 OK
Content-Type: application/vnd.myapp.v1+json
Deprecation: true
Sunset: Wed, 01 Jan 2027 00:00:00 GMT
```

---

## 六、请求与响应示例

### 6.1 创建用户

**请求**：

```http
POST /api/v1/users
Content-Type: application/json
Authorization: Bearer {token}

{
  "username": "zhangsan",
  "email": "zhangsan@example.com",
  "password": "SecurePass123"
}
```

**响应**：

```json
{
  "code": 0,
  "message": "用户创建成功",
  "data": {
    "id": "usr_abc123",
    "username": "zhangsan",
    "email": "zhangsan@example.com",
    "createdAt": "2026-06-01T10:30:00Z"
  },
  "timestamp": "2026-06-01T10:30:00Z",
  "requestId": "req_xyz789"
}
```

### 6.2 获取用户列表（分页）

**请求**：

```http
GET /api/v1/users?page=1&pageSize=20&keyword=zhang&sortBy=createdAt&sortOrder=desc
Authorization: Bearer {token}
```

**响应**：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "id": "usr_abc123",
        "username": "zhangsan",
        "email": "zhangsan@example.com"
      }
    ],
    "total": 1,
    "page": 1,
    "pageSize": 20
  },
  "timestamp": "2026-06-01T10:30:00Z",
  "requestId": "req_xyz789"
}
```

---

## 七、接口契约（OpenAPI）

### 7.1 OpenAPI 3.0 契约示例

前后端应在开发前通过 OpenAPI 契约对齐接口：

```yaml
# /docs/contracts/user.yaml
openapi: 3.0.0
info:
  title: User API
  version: 1.0.0

paths:
  /api/v1/users:
    get:
      summary: 获取用户列表
      parameters:
        - name: page
          in: query
          schema: { type: integer, default: 1 }
        - name: pageSize
          in: query
          schema: { type: integer, default: 20, maximum: 200 }
        - name: keyword
          in: query
          schema: { type: string }
      responses:
        '200':
          description: 成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/UserListResponse'

components:
  schemas:
    UserListResponse:
      type: object
      properties:
        code: { type: integer, example: 0 }
        message: { type: string }
        data:
          type: object
          properties:
            list:
              type: array
              items: { $ref: '#/components/schemas/User' }
            total: { type: integer }
            page: { type: integer }
            pageSize: { type: integer }

    User:
      type: object
      properties:
        id: { type: string }
        username: { type: string }
        email: { type: string, format: email }
        createdAt: { type: string, format: date-time }
```

**API-050** 接口实现 MUST 与 OpenAPI 契约一致，CI 阶段应使用 swagger-diff 等工具校验实现与契约一致性。 [MUST]

---

## 八、设计检查清单

### 8.1 URL 检查

- [ ] 资源名是否复数
- [ ] 命名是否小写 + 中划线
- [ ] 是否带版本前缀
- [ ] 是否避免动词
- [ ] 查询参数命名是否规范

### 8.2 响应格式检查

- [ ] 是否包含 `code` + `message` + `data` + `timestamp` + `requestId`
- [ ] 分页响应字段是否齐全
- [ ] 错误响应是否包含错误码段位

### 8.3 错误处理检查

- [ ] HTTP 状态码与业务码是否对应
- [ ] 错误码是否按段位划分
- [ ] 错误消息是否走 i18n
- [ ] 生产环境是否屏蔽内部错误细节

### 8.4 版本管理检查

- [ ] v1 接口是否保持向后兼容
- [ ] 弃用接口是否加 `Deprecation` 头
- [ ] 重大变更是否升级到 v2

---

*本规范与 `universal/exception-handling.md`（异常处理）+ `universal/request-tracing.md`（链路追踪）协同使用。错误码体系与异常处理的映射见 error-handling.md。*