| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 层级 | L1 |
| 包类型 | architecture |
| 引入条件 | fingerprint.profiles contains 'arch-mvc' |
| 适用场景 | 标准 CRUD 应用、管理后台、三层 MVC 架构 |
| 依赖规范 | spring-boot-base |
| 互斥规范 | arch-ddd |

# MVC 三层架构

本包定义 Spring Boot MVC 三层架构的分层结构与职责边界。持久化相关规范请配合 `persistence-*` 包使用。

## 标准目录布局

```
project/
├── src/main/java/{{package_base}}/
│   ├── controller/     # 接入层：HTTP 请求处理
│   ├── service/        # 业务层：业务逻辑编排
│   │   └── impl/
│   ├── mapper/         # 持久层：数据访问 (MyBatis)
│   │   # 或 repository/ (JPA)
│   ├── model/
│   │   ├── entity/     # 数据库实体
│   │   ├── dto/        # 数据传输对象（入参/出参）
│   │   ├── vo/         # 视图对象（返回前端）
│   │   └── query/      # 查询参数封装
│   ├── config/         # 配置类
│   ├── common/         # 公共组件（枚举、常量、工具）
│   │   ├── enums/
│   │   ├── constants/
│   │   └── utils/
│   └── interceptor/    # 拦截器 / 过滤器
├── src/main/resources/
│   ├── mapper/         # MyBatis XML 映射文件（如使用 MyBatis）
│   ├── application.yml
│   └── application-{{env}}.yml
└── src/test/java/{{package_base}}/
    ├── controller/
    ├── service/
    └── mapper/
```

## 分层职责

### 接入层（Controller）

**职责**：接收 HTTP 请求，参数校验，调用 Service，封装响应。

**PROF-MVC-001** Controller MUST NOT 包含任何业务逻辑，仅负责参数接收、校验与转发。 [MUST]

**PROF-MVC-002** Controller MUST NOT 直接调用 Mapper/Repository，必须通过 Service 层访问数据。 [MUST]

**PROF-MVC-003** Controller 方法体 MUST NOT 超过 15 行（不含注解），超出 SHOULD 提取到 Service。 [MUST/SHOULD]

**PROF-MVC-004** Controller MUST NOT 直接操作 `HttpServletRequest` / `HttpServletResponse`，MAY 在拦截器中使用。 [MUST/MAY]

**PROF-MVC-005** Controller MUST NOT 包含对象转换逻辑（Entity → VO），转换 MUST 在 Service 层或 Converter 工具类中完成。 [MUST]

```java
// 正确：Controller 仅做参数校验与委托
@GetMapping("/{id}")
public Result<UserVO> detail(@PathVariable Long id) {
    return Result.ok(userService.getById(id));
}

// 错误：Controller 直接操作数据库
@GetMapping("/{id}")
public Result<UserVO> detail(@PathVariable Long id) {
    User user = userMapper.selectById(id);  // 禁止跨层调用
    return Result.ok(convert(user));
}
```

### 业务层（Service）

**职责**：编排业务逻辑，管理事务，调用持久层。

**PROF-MVC-006** Service 层 MUST 封装所有业务规则，Controller 不得参与业务判断。 [MUST]

**PROF-MVC-007** Service 层 SHOULD 面向接口编程，接口定义在 `service/`，实现放在 `service/impl/`。 [SHOULD]

```java
// 接口定义
public interface UserService {
    UserVO getById(Long id);
    PageResult<UserVO> page(UserQuery query);
    Long create(UserCreateDTO dto);
}

// 实现
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;

    @Override
    public UserVO getById(Long id) {
        User entity = userMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return UserConverter.toVO(entity);
    }
}
```

### 持久层（Mapper / Repository）

**职责**：封装数据库访问，提供 CRUD 操作。

**PROF-MVC-008** 持久层 MUST NOT 包含业务逻辑，仅负责 SQL 执行与结果映射。 [MUST]

**PROF-MVC-009** 复杂 SQL MUST 使用 XML 映射文件（MyBatis）或 QueryDSL/Specification（JPA），简单 CRUD MAY 使用注解。 [MUST/MAY]

## 依赖方向

**PROF-MVC-010** 层间依赖 MUST 严格单向：`Controller → Service → Mapper/Repository`，禁止反向依赖。 [MUST]

**PROF-MVC-011** 禁止循环依赖：若 A 层依赖 B 层，则 B 层 MUST NOT 依赖 A 层。 [MUST]

```
Controller → Service → Mapper/Repository
     ✗            ✗          ✗
Mapper ↛ Service ↛ Controller（禁止反向）
```

**PROF-MVC-012** Service 之间 MAY 相互调用，但 MUST 避免循环引用；若出现，SHOULD 抽取公共逻辑到独立 Service。 [MAY/MUST/SHOULD]

## 包结构规则

**PROF-MVC-013** 每个功能模块 SHOULD 按上述分包结构组织，MAY 在业务规模较大时按业务域拆分顶级包。 [SHOULD/MAY]

```java
// 小规模项目：按层分包
com.example.project.controller.UserController
com.example.project.service.UserService
com.example.project.mapper.UserMapper

// 较大项目：按业务域分包
com.example.project.user.controller.UserController
com.example.project.user.service.UserService
com.example.project.user.mapper.UserMapper
com.example.project.order.controller.OrderController
com.example.project.order.service.OrderService
```

**PROF-MVC-014** `config` 包 SHOULD 集中管理所有 `@Configuration` 类，按功能命名：`WebConfig`、`MybatisConfig`、`SecurityConfig`。 [SHOULD]

## Controller 命名规范

**PROF-MVC-015** Controller 类名 MUST 以 `Controller` 结尾，前缀为资源名称。 [MUST]

**PROF-MVC-016** Controller 方法名 MUST 使用动词开头，遵循统一语义：[MUST]

| 操作 | 方法名 | HTTP 方法 |
|------|--------|---------|
| 分页列表 | `page` / `list` | GET |
| 详情 | `detail` / `get` | GET |
| 新增 | `create` / `add` | POST |
| 修改 | `update` / `modify` | PUT |
| 删除 | `delete` / `remove` | DELETE |
| 导出 | `export` | GET |
| 导入 | `import` | POST |

## 参数校验

**PROF-MVC-017** 所有 `@RequestBody` 参数 MUST 配合 `@Valid` 或 `@Validated` 注解触发 Bean Validation。 [MUST]

**PROF-MVC-018** 校验规则 MUST 定义在 DTO 字段注解上，MUST NOT 在 Controller 中手写 if-else 校验。 [MUST]

**PROF-MVC-019** `@PathVariable` 和 `@RequestParam` 参数 SHOULD 使用内联校验，配合 `@Validated` 在类级别启用。 [SHOULD]

## Swagger / OpenAPI 注解

**PROF-MVC-020** 每个 Controller 类 MUST 添加 `@Tag` 注解标注分组名称。 [MUST]

**PROF-MVC-021** 每个接口方法 SHOULD 添加 `@Operation(summary, description)` 注解。 [SHOULD]

**PROF-MVC-022** DTO 字段 SHOULD 使用 `@Schema(description, example)` 提供字段说明与示例值。 [SHOULD]

## 请求参数规范

**PROF-MVC-023** 参数类型选择 MUST 遵循以下规则：[MUST]

| 场景 | 注解 | 说明 |
|------|------|------|
| 路径参数（资源标识） | `@PathVariable` | RESTful 标准用法 |
| 简单查询参数 | `@RequestParam` | 1-3 个简单参数 |
| 查询参数对象 | 无注解 / `@ModelAttribute` | 多条件查询封装为 Query 对象 |
| 请求体（JSON） | `@RequestBody` | 创建/更新操作 |

**PROF-MVC-024** `@RequestBody` MUST NOT 与 `@RequestParam` 在同一方法中混合使用（`@PathVariable` 除外）。 [MUST]

**PROF-MVC-025** 文件上传接口 MUST 使用 `@RequestPart` 或 `@RequestParam` 配合 `MultipartFile`。 [MUST]

## REST 接口命名

**PROF-MVC-026** URL 路径 MUST 使用 kebab-case，资源名用复数形式。 [MUST]

```
GET    /api/users          # 用户列表
GET    /api/users/{id}     # 用户详情
POST   /api/users          # 创建用户
PUT    /api/users/{id}     # 更新用户
DELETE /api/users/{id}     # 删除用户
GET    /api/user-roles     # 复合资源名用 kebab-case
```

**PROF-MVC-027** URL MUST NOT 包含动词，操作语义由 HTTP 方法表达。 [MUST]

**PROF-MVC-028** 接口路径 SHOULD 添加 `/api` 前缀，版本化时使用 `/api/v1`、`/api/v2`。 [SHOULD]

## Controller 瘦身原则

**PROF-MVC-029** 单个 Controller 类 MUST NOT 超过 10 个接口方法，超出 SHOULD 按职责拆分到多个 Controller。 [MUST/SHOULD]

## 模型对象命名

**PROF-MVC-030** 数据模型类 MUST 遵循以下命名规则：[MUST]

| 类型 | 命名规则 | 示例 |
|------|---------|------|
| Entity | 与数据库表对应，名词 | `User`, `Order` |
| DTO | 以 `DTO` 结尾，描述操作 | `UserCreateDTO`, `UserUpdateDTO` |
| VO | 以 `VO` 结尾，描述视图 | `UserVO`, `UserDetailVO` |
| Query | 以 `Query` 结尾，封装查询条件 | `UserQuery`, `OrderQuery` |

**PROF-MVC-031** DTO MUST NOT 直接复用 Entity，即使字段相同也 SHOULD 独立定义，避免内部结构泄露。 [MUST/SHOULD]

## Service 命名

**PROF-MVC-032** Service 接口 MUST 以 `Service` 结尾，实现类 MUST 追加 `Impl` 后缀。 [MUST]

**PROF-MVC-033** Service 方法名 SHOULD 与 Controller 保持一致，MAY 增加业务语义前缀。 [SHOULD/MAY]

## 演进路径

**PROF-MVC-034** 项目从 MVC 演进到 DDD 时，SHOULD 逐步将业务逻辑从 Service 层下沉到领域层，而非一步到位重构。 [SHOULD]
