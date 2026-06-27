# 命名规范

| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 层级 | L0 |
| 引入条件 | always |
| 适用架构 | 全部 |
| 依赖规范 | 无 |
| 互斥规范 | 无 |

---

## 规范 UNI-NC-001: 命名通用原则

**规则:**

1. 所有标识符 MUST 使用英文单词或广泛认可的技术缩写,禁止拼音或拼音缩写。
2. 名称 MUST 具备描述性,避免单字母变量(循环计数器 `i`/`j`/`k` 除外)。
3. 缩写词在命名中 MUST 视为普通单词处理(如 `Http` 而非 `HTTP`,`Id` 而非 `ID`),保持大小写一致性。
4. SHOULD 使用业务无关的通用术语作为示例基准,如 `user`、`order`、`config`。

**示例:**

```java
// BAD
String yhm = "admin";          // 拼音缩写
String HTTP_CONN_TIMEOUT = "5"; // 缩写词全大写不一致
String d = "2026-01-01";       // 无意义单字母

// GOOD
String userName = "admin";
String httpConnTimeout = "5";
String startDate = "2026-01-01";
```

```typescript
// BAD
const yhm = 'admin';
const HTTP_CONN_TIMEOUT = '5';

// GOOD
const userName = 'admin';
const httpConnTimeout = '5';
```

---

## 规范 UNI-NC-002: Java 命名规则

**规则:**

1. 类名 MUST 使用 PascalCase,名词或名词短语。
2. 方法名 MUST 使用 camelCase,动词或动词短语。
3. 变量名 MUST 使用 camelCase。
4. 常量(含枚举值)MUST 使用 UPPER_SNAKE_CASE。
5. 包名 MUST 全小写,使用点号分隔,格式为 `{{group}}.{{project}}.{{module}}`。
6. 接口名 MUST 使用 PascalCase,不加 `I` 前缀;实现类 SHOULD 加 `Impl` 后缀或使用描述性名称。
7. 泛型参数 SHOULD 使用单一大写字母(`T`、`E`、`K`、`V`)或描述性 PascalCase(`TResult`)。
8. Boolean 变量/方法 SHOULD 使用 `is`、`has`、`can`、`should` 前缀。

**示例:**

```java
// 类名 - PascalCase
public class UserController { }
public class OrderService { }
public class ConfigValidator { }

// 方法名 - camelCase,动词短语
public UserResponse getUserById(Long id) { }
public List<Order> listActiveOrders() { }
public boolean isValidConfig(String key) { }

// 变量名 - camelCase
String userName = "admin";
int maxRetryCount = 3;

// 常量 - UPPER_SNAKE_CASE
public static final int DEFAULT_PAGE_SIZE = 20;
public static final String AUTH_HEADER_NAME = "Authorization";

// 包名 - 全小写
package com.example.usermanagement.controller;
package com.example.usermanagement.service.impl;

// 接口与实现
public interface UserRepository { }
public class JpaUserRepository implements UserRepository { }

// Boolean 命名
boolean isActive = true;
boolean hasPermission = false;
boolean canRetry = true;
```

---

## 规范 UNI-NC-003: TypeScript/JavaScript 命名规则

**规则:**

1. 变量名和函数名 MUST 使用 camelCase。
2. 类名、接口名、类型别名 MUST 使用 PascalCase。
3. React/Vue 组件名 MUST 使用 PascalCase(包括文件名)。
4. CSS 类名 MUST 使用 kebab-case 或 BEM 约定。
5. 常量 SHOULD 使用 UPPER_SNAKE_CASE(模块级导出常量)或 camelCase(局部常量)。
6. 枚举成员名 MUST 使用 PascalCase,枚举值 MUST 使用 UPPER_SNAKE_CASE 或 PascalCase。
7. 私有属性和方法 SHOULD 使用 `#` 前缀(ES Private)或 `_` 前缀(约定私有)。
8. 异步函数 SHOULD 不强制加 `async` 后缀,通过返回类型 `Promise<T>` 表达。

**示例:**

```typescript
// 变量与函数 - camelCase
const userName = 'admin';
const maxRetryCount = 3;
function getUserById(id: string): Promise<User> { }
function calculateTotalPrice(items: Item[]): number { }

// 类/接口/类型 - PascalCase
class UserService { }
interface UserResponse { }
type OrderStatus = 'PENDING' | 'COMPLETED' | 'CANCELLED';

// React 组件 - PascalCase
function UserProfile({ userId }: Props) { }
const OrderList: React.FC = () => { };

// CSS 类名 - kebab-case
// .user-profile { }
// .order-list__item--active { }

// 常量
export const DEFAULT_PAGE_SIZE = 20;
export const AUTH_HEADER_NAME = 'Authorization';

// 枚举
enum LogLevel {
  Error = 'ERROR',
  Warn = 'WARN',
  Info = 'INFO',
}
```

---

## 规范 UNI-NC-004: 文件命名规则

**规则:**

1. Java 文件名 MUST 与主类名一致,使用 PascalCase,扩展名为 `.java`。
2. TypeScript/JavaScript 文件名规则:
   - 组件文件 MUST 使用 PascalCase(如 `UserProfile.tsx`)。
   - 工具/服务/配置文件 SHOULD 使用 camelCase(如 `userService.ts`、`configHelper.ts`)。
   - 测试文件 MUST 使用对应源文件名加 `.test` 或 `.spec` 后缀。
3. 配置文件 MUST 使用 kebab-case 或全小写(如 `docker-compose.yml`、`.eslintrc.json`)。
4. 文档文件 SHOULD 使用 kebab-case(如 `api-design.md`、`release-notes.md`)。
5. SQL 文件 MUST 使用 snake_case(如 `create_user_table.sql`)。

**示例:**

```
src/
├── main/java/com/example/
│   ├── controller/UserController.java      # PascalCase,与类名一致
│   ├── service/UserService.java
│   └── repository/OrderRepository.java
├── frontend/src/
│   ├── components/UserProfile.tsx           # PascalCase,组件
│   ├── hooks/useAuth.ts                    # camelCase,hook
│   ├── services/orderService.ts            # camelCase,服务
│   ├── utils/configHelper.ts              # camelCase,工具
│   └── __tests__/
│       ├── UserProfile.test.tsx            # 源文件 + .test
│       └── orderService.spec.ts
├── config/
│   ├── application.yml                     # 全小写
│   └── docker-compose.yml                  # kebab-case
└── db/
    └── migration/
        └── v1__create_user_table.sql       # snake_case
```

---

## 规范 UNI-NC-005: 包/模块命名规则

**规则:**

1. Java 包名 MUST 全小写,MUST 不包含连字符或下划线。
2. Maven/Gradle artifactId MUST 使用 kebab-case。
3. npm 包名 MUST 使用 kebab-case,scope 使用 `@{{org}}/{{package}}` 格式。
4. Docker 镜像名 MUST 使用 kebab-case,格式为 `{{registry}}/{{project}}/{{service}}:{{tag}}`。
5. 微服务名 MUST 使用 kebab-case,且 SHOULD 在全公司范围内唯一。

**示例:**

```xml
<!-- Maven artifactId: kebab-case -->
<groupId>com.example</groupId>
<artifactId>user-management-service</artifactId>
```

```json
// npm: kebab-case with scope
{
  "name": "@example/user-management-ui",
  "version": "1.0.0"
}
```

```yaml
# Docker 镜像: kebab-case
image: registry.example.com/platform/user-service:1.2.0
```

---

## 规范 UNI-NC-006: 数据库命名规则

**规则:**

1. 表名 MUST 使用 snake_case,MUST 使用复数形式(如 `users`、`orders`)。
2. 列名 MUST 使用 snake_case。
3. 主键列 SHOULD 命名为 `id`。
4. 外键列 SHOULD 命名为 `{{referenced_table_singular}}_id`(如 `user_id`)。
5. 布尔列 SHOULD 使用 `is_`、`has_`、`can_` 前缀。
6. 时间列 SHOULD 使用 `_at` 后缀(如 `created_at`、`updated_at`、`deleted_at`)。
7. 索引名 MUST 使用 `idx_{{table}}_{{columns}}` 格式。
8. 约束名 MUST 使用 `{{type}}_{{table}}_{{columns}}` 格式(如 `uk_users_email`、`fk_orders_user_id`)。

**示例:**

```sql
CREATE TABLE users (
    id          BIGINT PRIMARY KEY,
    user_name   VARCHAR(64)  NOT NULL,
    email       VARCHAR(128) NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP    NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE orders (
    id          BIGINT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    order_no    VARCHAR(64)  NOT NULL,
    status      VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uk_orders_order_no UNIQUE (order_no)
);

CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_orders_created_at ON orders (created_at);
```

---

## 规范 UNI-NC-007: API 端点命名规则

**规则:**

1. URL 路径 MUST 使用 kebab-case,MUST 使用复数名词(如 `/users`、`/order-items`)。
2. URL 路径 MUST 小写,禁止出现动词(动词由 HTTP 方法表达)。
3. 查询参数 MUST 使用 camelCase。
4. 内部接口路径前缀为 `/api/`,外部接口路径前缀为 `/open-api/v{{version}}/`(详见 UNI-AD-001)。
5. 层级嵌套 SHOULD 不超过两层(如 `/api/users/{userId}/orders`)。

**示例:**

```
# 内部接口（前后端分离项目内部,无版本号）
GET    /api/users                      # 获取用户列表
POST   /api/users                      # 创建用户

# 单个资源
GET    /api/users/{userId}             # 获取单个用户
PUT    /api/users/{userId}             # 更新用户
DELETE /api/users/{userId}             # 删除用户

# 子资源
GET    /api/users/{userId}/orders      # 获取用户的订单列表

# 查询参数 - camelCase
GET    /api/users?pageNumber=1&pageSize=20&sortField=createdAt

# 外部接口（对外开放,带版本号）
GET    /open-api/v1/users              # 获取用户列表
POST   /open-api/v1/users              # 创建用户

# BAD - URL 中使用动词或大写
GET    /api/getUsers
GET    /api/User_Profile
```
