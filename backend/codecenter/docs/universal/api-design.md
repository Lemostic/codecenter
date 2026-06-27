# API 设计规范

| 字段 | 值 |
|------|-----|
| 版本 | 1.1 |
| 层级 | L0 |
| 引入条件 | always |
| 适用架构 | 全部 |
| 依赖规范 | UNI-NC(命名规范) |
| 互斥规范 | 无 |

---

## 规范 UNI-AD-001: API 分类与适用场景

**规则:**

1. 所有接口 MUST 在设计和文档中明确归类为以下两类之一:
   - **内部接口（Internal API）**：前后端分离架构中，前端模块与后端服务之间的接口。由同一团队维护，前后端协同发布。
   - **外部接口（External API）**：对第三方系统、开放平台、其他团队暴露的接口。调用方与提供方独立演进。
2. 内部接口 MUST 使用 `/api` 作为基础路径前缀,不带版本号。
3. 外部接口 MUST 使用 `/open-api/v{{version}}` 作为基础路径前缀,带版本号。
4. 项目中 SHOULD 通过 Controller 分包或注解标记区分内部接口与外部接口。
5. 内部接口与外部接口 MUST NOT 复用同一个 Controller 类。

**分类决策表:**

| 维度 | 内部接口（Internal） | 外部接口（External） |
|------|---------------------|---------------------|
| 调用方 | 本项目前端模块 | 第三方系统、其他团队、开放平台 |
| 路径前缀 | `/api/` | `/open-api/v{{version}}/` |
| 版本管理 | 不需要，前后端协同发布 | 必须，独立版本演进 |
| 兼容性承诺 | 前后端同步变更，无兼容期 | 旧版本 MUST 保留兼容期 |
| 鉴权方式 | Session / 内部Token | OAuth2 / API Key / 签名机制 |
| 文档要求 | 接口文档（Swagger/内部文档） | 开放API文档 + 变更日志 |

**示例:**

```
# 内部接口（前后端分离，无版本号）
GET    /api/users                           # 用户列表（内部）
POST   /api/users                           # 创建用户（内部）
GET    /api/users/{userId}                  # 用户详情（内部）

# 外部接口（对外开放，带版本号）
GET    /open-api/v1/users                   # 用户列表（外部 v1）
POST   /open-api/v1/users                   # 创建用户（外部 v1）
GET    /open-api/v2/users                   # 用户列表（外部 v2，含不兼容变更）
```

```java
// 内部接口 Controller
@RestController
@RequestMapping("/api/users")
public class UserInternalController {
    // 前后端分离项目内部使用
}

// 外部接口 Controller（单独分包）
@RestController
@RequestMapping("/open-api/v1/users")
public class UserExternalController {
    // 对外开放平台使用，需考虑版本兼容
}
```

---

## 规范 UNI-AD-002: URL 路径约定

**规则:**

1. 资源路径 MUST 使用复数名词、kebab-case（如 `/users`、`/order-items`）。
2. 路径层级 SHOULD 不超过两层嵌套，超出部分 SHOULD 通过查询参数表达。
3. URL MUST 全小写，单词间用连字符分隔。
4. URL 中 SHOULD 不出现动词（如 `/getUsers`），语义由 HTTP 方法或路径后缀承载。当框架仅支持 GET/POST 时（见 UNI-AD-003），允许使用动名词后缀（如 `/users/list`、`/users/create`）。

**内部接口路径示例:**

```
GET    /api/users                           # 用户列表
GET    /api/users/{userId}                  # 用户详情
POST   /api/users                           # 创建用户
PUT    /api/users/{userId}                  # 更新用户
DELETE /api/users/{userId}                  # 删除用户
GET    /api/users/{userId}/orders           # 用户的订单（子资源）
```

**外部接口路径示例:**

```
GET    /open-api/v1/users                   # 用户列表
GET    /open-api/v1/users/{userId}          # 用户详情
POST   /open-api/v1/users                   # 创建用户
```

---

## 规范 UNI-AD-003: HTTP 方法使用

**规则:**

HTTP 方法的使用分为两种模式，项目 MUST 在 manifest 中声明所使用的模式，并在整个项目中保持一致。

### 模式 A：完整 RESTful 模式

适用于支持完整 HTTP 方法的框架（Spring Boot、Express、FastAPI 等）。

1. `GET` MUST 用于读取资源，MUST 幂等，禁止产生副作用。
2. `POST` MUST 用于创建资源或非幂等操作。
3. `PUT` MUST 用于全量替换资源。
4. `PATCH` SHOULD 用于部分更新资源。
5. `DELETE` MUST 用于删除资源。
6. 批量操作 SHOULD 使用 `POST` 配合资源路径（如 `POST /api/users/batch-delete`）。

### 模式 B：简化 GET/POST 模式

适用于仅支持 GET/POST 的框架，或团队约定统一使用 GET/POST 的项目。

1. `GET` MUST 用于所有查询类操作（列表、详情、搜索等），MUST 幂等。
2. `POST` MUST 用于所有写入类操作（创建、更新、删除、批量操作等）。
3. 写入操作 SHOULD 在 URL 路径中显式表达操作类型（如 `/api/users/create`、`/api/users/update`、`/api/users/delete`）。
4. 查询操作 SHOULD 使用语义化路径（如 `/api/users/list`、`/api/users/detail`、`/api/users/search`）。

### 模式声明

项目 MUST 在 `.spec/spec-manifest.yaml` 中声明 HTTP 方法模式：

```yaml
fingerprint:
  architecture: "spring-boot-mvc"
  http_mode: "full"        # "full" = 完整 RESTful, "simple" = GET/POST 简化
```

**模式 A 示例:**

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping
    public ApiResponse<PageResult<UserResponse>> list(@Valid UserQuery query) { }

    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> detail(@PathVariable Long userId) { }

    @PostMapping
    public ApiResponse<UserResponse> create(@RequestBody @Valid CreateUserRequest request) { }

    @PutMapping("/{userId}")
    public ApiResponse<UserResponse> update(@PathVariable Long userId,
            @RequestBody @Valid UpdateUserRequest request) { }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> delete(@PathVariable Long userId) { }
}
```

**模式 B 示例:**

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/list")
    public ApiResponse<PageResult<UserResponse>> list(@Valid UserQuery query) { }

    @GetMapping("/detail")
    public ApiResponse<UserResponse> detail(@RequestParam Long userId) { }

    @PostMapping("/create")
    public ApiResponse<UserResponse> create(@RequestBody @Valid CreateUserRequest request) { }

    @PostMapping("/update")
    public ApiResponse<UserResponse> update(@RequestBody @Valid UpdateUserRequest request) { }

    @PostMapping("/delete")
    public ApiResponse<Void> delete(@RequestBody DeleteRequest request) { }

    @PostMapping("/batch-delete")
    public ApiResponse<Void> batchDelete(@RequestBody BatchDeleteRequest request) { }
}
```

```typescript
// 前端 API 封装（配合模式 B）
export const userApi = {
  list:    (params: UserQuery) => get<PageResult<UserVO>>('/api/users/list', { params }),
  detail:  (userId: number)   => get<UserVO>('/api/users/detail', { params: { userId } }),
  create:  (data: CreateUserDTO) => post<UserVO>('/api/users/create', data),
  update:  (data: UpdateUserDTO) => post<UserVO>('/api/users/update', data),
  delete:  (userIds: number[]) => post<void>('/api/users/batch-delete', { userIds }),
};
```

---

## 规范 UNI-AD-004: 统一响应格式

**规则:**

1. 所有 API 响应（内部与外部）MUST 使用统一的 JSON 结构，包含以下字段:
   - `code`(String) — 结果码，"000000" 表示成功，其他值表示失败。
   - `message`(String) — 结果描述信息。
   - `data`(Object|null) — 业务数据，失败时为 `null`。
   - `requestId`(String) — 请求唯一标识，用于链路追踪。
2. 响应 MUST 包含 `Content-Type: application/json` 头。
3. HTTP 状态码 MUST 与业务语义保持一致（详见 UNI-AD-006）。

**示例:**

```json
// 成功响应
{
  "code": "000000",
  "message": "success",
  "data": {
    "id": 1001,
    "userName": "admin",
    "email": "admin@example.com"
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}

// 失败响应
{
  "code": "A00101",
  "message": "用户名已存在",
  "data": null,
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}

// 分页响应
{
  "code": "000000",
  "message": "success",
  "data": {
    "list": [ ... ],
    "pageNumber": 1,
    "pageSize": 20,
    "total": 156,
    "totalPages": 8
  },
  "requestId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

```java
// Java 响应封装
public class ApiResponse<T> {
    private String code;
    private String message;
    private T data;
    private String requestId;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("000000", "success", data);
    }

    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}

public class PageResult<T> {
    private List<T> list;
    private int pageNumber;
    private int pageSize;
    private long total;
    private int totalPages;
}
```

```typescript
// TypeScript 响应类型
interface ApiResponse<T> {
  code: string;
  message: string;
  data: T | null;
  requestId: string;
}

interface PageResult<T> {
  list: T[];
  pageNumber: number;
  pageSize: number;
  total: number;
  totalPages: number;
}
```

---

## 规范 UNI-AD-005: 错误码设计

**规则:**

1. 错误码 MUST 为 6 位字符串，格式为 `{{category}}{{module}}{{sequence}}`。
2. 错误码分类:
   - `000000` — 成功
   - `A00xxx` — 客户端错误（参数校验失败、未授权等）
   - `B00xxx` — 系统内部错误（服务异常、超时等）
   - `C00xxx` — 第三方服务错误（外部接口调用失败）
3. 系统级错误码 MUST 全局统一，在各服务文档中注册。
4. 业务级错误码 MUST 按模块划分，模块编码由团队协商分配。
5. 错误码 MUST 有对应的可读 `message`，且 `message` 对前端友好。

**示例:**

| 错误码 | 含义 | 分类 |
|--------|------|------|
| `000000` | 成功 | — |
| `A00001` | 参数校验失败 | 客户端 |
| `A00002` | 未授权（未登录） | 客户端 |
| `A00003` | 权限不足 | 客户端 |
| `A00004` | 资源不存在 | 客户端 |
| `A00005` | 请求频率超限 | 客户端 |
| `B00001` | 系统内部异常 | 系统 |
| `B00002` | 数据库操作异常 | 系统 |
| `B00003` | 缓存服务不可用 | 系统 |
| `C00001` | 第三方服务调用超时 | 第三方 |
| `C00002` | 第三方服务返回异常 | 第三方 |

```java
public enum ErrorCode {
    SUCCESS("000000", "success"),
    // A: 客户端错误
    PARAM_INVALID("A00001", "参数校验失败"),
    UNAUTHORIZED("A00002", "未授权，请先登录"),
    FORBIDDEN("A00003", "权限不足"),
    RESOURCE_NOT_FOUND("A00004", "资源不存在"),
    RATE_LIMIT_EXCEEDED("A00005", "请求频率超限"),
    // B: 系统错误
    INTERNAL_ERROR("B00001", "系统内部异常"),
    DATABASE_ERROR("B00002", "数据库操作异常"),
    CACHE_ERROR("B00003", "缓存服务不可用"),
    // C: 第三方错误
    THIRD_PARTY_TIMEOUT("C00001", "第三方服务调用超时"),
    THIRD_PARTY_ERROR("C00002", "第三方服务返回异常");

    private final String code;
    private final String message;
}
```

---

## 规范 UNI-AD-006: HTTP 状态码映射

**规则:**

1. API 响应 MUST 使用以下 HTTP 状态码:
   - `200 OK` — 成功（通用）
   - `201 Created` — 创建成功（仅限模式 A 的 POST）
   - `204 No Content` — 成功但无返回体
   - `400 Bad Request` — 请求参数错误
   - `401 Unauthorized` — 未认证
   - `403 Forbidden` — 已认证但权限不足
   - `404 Not Found` — 资源不存在
   - `409 Conflict` — 资源冲突（如重复创建）
   - `429 Too Many Requests` — 请求频率超限
   - `500 Internal Server Error` — 服务端内部错误
   - `502 Bad Gateway` — 网关错误
   - `503 Service Unavailable` — 服务不可用
2. 模式 B（GET/POST 简化模式）下，所有成功响应 SHOULD 统一使用 `200 OK`，不强制使用 `201`。
3. 业务错误码 MUST 放在响应体的 `code` 字段中，不通过 HTTP 状态码传递业务语义。

---

## 规范 UNI-AD-007: 分页约定

**规则:**

1. 列表接口 MUST 支持分页，默认分页参数为 `pageNumber`（从 1 开始）和 `pageSize`。
2. `pageSize` MUST 设置上限，SHOULD 不超过 100。
3. 分页响应 MUST 包含 `list`、`pageNumber`、`pageSize`、`total`、`totalPages`。
4. 排序参数 SHOULD 使用 `sortField` 和 `sortOrder`（asc/desc）。

**模式 A 示例:**

```
GET /api/users?pageNumber=2&pageSize=20&sortField=createdAt&sortOrder=desc
```

**模式 B 示例:**

```
GET /api/users/list?pageNumber=2&pageSize=20&sortField=createdAt&sortOrder=desc
```

```java
@GetMapping
public ApiResponse<PageResult<UserResponse>> list(
        @RequestParam(defaultValue = "1") int pageNumber,
        @RequestParam(defaultValue = "20") int pageSize,
        @RequestParam(defaultValue = "createdAt") String sortField,
        @RequestParam(defaultValue = "desc") String sortOrder) {
    if (pageSize > 100) {
        throw new ValidationException("A00001", "pageSize 不能超过 100");
    }
    // ...
}
```

---

## 规范 UNI-AD-008: API 版本管理策略（仅外部接口）

**规则:**

1. 版本管理规则 **仅适用于外部接口（External API）**。内部接口由前后端协同发布，不需要版本管理。
2. 外部接口版本 MUST 通过 URL 路径前缀（`/open-api/v1/`、`/open-api/v2/`）发布，禁止使用 Header 或查询参数传递版本号。
3. 大版本升级（BREAKING CHANGE）MUST 发布新版本号，旧版本 MUST 保留至少 **6 个月**兼容期。
4. 小版本兼容变更（新增字段、新增接口）MAY 在当前版本上直接扩展，无需递增版本号。
5. 废弃接口 MUST 在响应头中添加 `Deprecation: true` 和 `Sunset` 日期。
6. 废弃通知 SHOULD 通过文档、邮件、日志等多渠道提前告知调用方。
7. 内部接口如需变更，通过前后端协同发版解决，MAY 在代码仓库中标注变更日志，无需维护旧版本。

**外部接口版本示例:**

```
# 当前版本
GET /open-api/v1/users

# 新版本（含不兼容变更）
GET /open-api/v2/users

# 废弃接口响应头
HTTP/1.1 200 OK
Deprecation: true
Sunset: Sat, 01 Jan 2027 00:00:00 GMT
Link: <https://docs.example.com/migration/v2>; rel="successor-version"
```

**内部接口变更流程（无版本管理）:**

```
1. 前端与后端约定接口变更内容
2. 后端先发布兼容变更（新增字段/接口）
3. 前端适配并发布
4. 后端清理废弃字段/接口（确认前端已不依赖后）
```

---

## 变更记录

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0 | 2026-06-17 | 初始版本 |
| 1.1 | 2026-06-17 | 重构：新增 API 分类（内部/外部）、HTTP 方法双模式（RESTful/GET+POST）、版本管理仅限外部接口 |
