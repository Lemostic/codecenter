| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 层级 | L1 |
| 包类型 | persistence |
| 引入条件 | fingerprint.profiles contains 'persistence-mybatis-plus' |
| 适用场景 | 使用 MyBatis-Plus 作为持久化框架的 Spring Boot 项目 |
| 依赖规范 | spring-boot-base |

# MyBatis-Plus 持久化规范包

> 本规范包从 MVC 持久化规范中提取 MyBatis-Plus 专属规则，适用于所有采用 MyBatis-Plus 作为 ORM 框架的 Spring Boot 项目。
> 规范中 `{{package_base}}` 代表项目基础包路径，例如 `com.example.project`。

---

## 1. MyBatis-Plus 基础约定

**PROF-MBP-001** 所有 Mapper 接口 MUST 继承 `BaseMapper<T>`，获得标准 CRUD 能力。 [MUST]

Mapper 接口通过继承 `BaseMapper<T>` 自动获得 `insert`、`deleteById`、`updateById`、`selectById`、`selectList` 等 17 个内置方法，避免重复编写单表操作 SQL。仅在需要 BaseMapper 无法覆盖的自定义查询时，才在接口中声明额外方法。

```java
package {{package_base}}.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 仅在此处定义 BaseMapper 无法覆盖的自定义查询
    List<UserVO> selectUserWithRole(@Param("query") UserQuery query);
}
```

---

**PROF-MBP-002** 单表 CRUD MUST 使用 BaseMapper 内置方法，MUST NOT 手写重复 SQL。 [MUST]

MyBatis-Plus 的 `BaseMapper` 和 `LambdaQueryWrapper` / `LambdaUpdateWrapper` 已覆盖绝大多数单表操作场景。手写与内置方法功能相同的 SQL 既浪费开发时间，又增加维护成本，还可能因字段变更导致不一致。

```java
// 正确：使用内置方法
User user = userMapper.selectById(id);

List<User> activeUsers = userMapper.selectList(
    new LambdaQueryWrapper<User>()
        .eq(User::getStatus, 1)
        .orderByDesc(User::getCreateTime)
);

// 使用 LambdaUpdateWrapper 进行条件更新
userMapper.update(null,
    new LambdaUpdateWrapper<User>()
        .eq(User::getStatus, 0)
        .set(User::getStatus, 1)
        .set(User::getUpdateTime, LocalDateTime.now())
);

// 错误：手写已有能力的 SQL
@Select("SELECT * FROM sys_user WHERE id = #{id}")
User selectById(@Param("id") Long id);

// 错误：手写可由 LambdaQueryWrapper 完成的查询
@Select("SELECT * FROM sys_user WHERE status = #{status} ORDER BY create_time DESC")
List<User> selectByStatus(@Param("status") Integer status);
```

---

**PROF-MBP-003** 多表关联查询 MUST 使用 XML 映射文件，MUST NOT 使用注解 SQL。 [MUST]

多表 JOIN 查询的 SQL 通常较长且包含动态条件，使用注解 SQL（`@Select`、`@Update` 等）会导致 Java 代码中混入大量 SQL 字符串，可读性差、难以维护。XML 映射文件支持 `<if>`、`<choose>`、`<foreach>` 等动态标签，更适合复杂查询场景。

```xml
<!-- UserMapper.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="{{package_base}}.mapper.UserMapper">

    <select id="selectUserWithRole" resultType="{{package_base}}.model.vo.UserVO">
        SELECT u.id, u.username, u.status, r.role_name
        FROM sys_user u
        LEFT JOIN sys_user_role ur ON u.id = ur.user_id
        LEFT JOIN sys_role r ON ur.role_id = r.id
        <where>
            <if test="query.username != null and query.username != ''">
                AND u.username LIKE CONCAT('%', #{query.username}, '%')
            </if>
            <if test="query.status != null">
                AND u.status = #{query.status}
            </if>
            <if test="query.roleId != null">
                AND r.id = #{query.roleId}
            </if>
        </where>
        ORDER BY u.create_time DESC
    </select>

</mapper>
```

---

## 2. Entity 设计

**PROF-MBP-004** Entity 类 MUST 使用 `@TableName` 注解显式指定表名，MUST NOT 依赖自动推断。 [MUST]

MyBatis-Plus 默认将类名按驼峰转下划线规则推断表名，当类名与表名不符合约定时会导致运行时错误。显式声明表名可以避免因重构类名而引发的隐蔽 Bug，同时也提高了代码的可读性。

```java
package {{package_base}}.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class User {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String username;
    private String password;
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
```

---

**PROF-MBP-005** 所有 Entity MUST 包含审计字段：`createTime`、`updateTime`；SHOULD 包含逻辑删除字段 `deleted`。 [MUST/SHOULD]

审计字段用于追踪数据的创建和修改时间，是排查问题和数据回溯的基础。逻辑删除字段可以避免物理删除导致的数据丢失，同时保留数据关联完整性。

```java
@Data
@TableName("sys_order")
public class Order {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;
    private Integer status;

    // MUST：审计字段
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // SHOULD：逻辑删除字段
    @TableLogic
    private Integer deleted;
}
```

> **说明**：如果业务场景确实不需要逻辑删除（如日志表、临时数据表），可以省略 `deleted` 字段，但 MUST 在代码评审中说明原因。

---

**PROF-MBP-006** 审计字段 MUST 使用 `@TableField(fill = FieldFill.INSERT/UPDATE)` 配合 `MetaObjectHandler` 自动填充。 [MUST]

手动在每个 Service 方法中设置审计字段容易遗漏且代码重复。通过 MyBatis-Plus 的 `MetaObjectHandler` 机制可以实现统一、自动化的字段填充，确保所有写入路径（包括直接调用 Mapper 的场景）都能正确填充。

```java
package {{package_base}}.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
```

> **注意**：`strictInsertFill` / `strictUpdateFill` 仅在字段值为 `null` 时填充，不会覆盖手动设置的值。如果需要在每次更新时强制刷新 `updateTime`，可以使用 `setFieldValByName` 替代。

---

**PROF-MBP-007** 主键策略 MUST 在 `@TableId` 中显式声明，推荐 `ASSIGN_ID`（雪花算法）或 `AUTO`（自增）。 [MUST]

不声明主键策略时，MyBatis-Plus 会使用全局默认策略（`IdType.NONE`），可能导致主键生成行为不符合预期。显式声明主键策略可以确保分布式场景下主键唯一性，同时提升代码可读性。

```java
// 推荐：雪花算法生成分布式唯一 ID
@TableId(type = IdType.ASSIGN_ID)
private Long id;

// 可选：数据库自增主键（适用于单库场景）
@TableId(type = IdType.AUTO)
private Long id;

// 可选：业务自定义主键（如订单号）
@TableId(type = IdType.INPUT)
private String orderNo;
```

> **推荐**：分布式系统优先使用 `ASSIGN_ID`，单机应用可使用 `AUTO`。MUST NOT 使用 `IdType.NONE` 或省略 `type` 属性。

---

## 3. 分页规范

**PROF-MBP-008** 分页查询 MUST 使用 MyBatis-Plus 的 `Page<T>` 对象，MUST NOT 手动拼接 LIMIT。 [MUST]

手动拼接 `LIMIT` 容易引入 SQL 注入风险，且无法自动获取总记录数。MyBatis-Plus 的分页插件会自动处理 `COUNT` 查询和 `LIMIT` 拼接，保证分页逻辑的统一性和安全性。

```java
package {{package_base}}.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public PageResult<UserVO> page(UserQuery query) {
        Page<User> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .like(StringUtils.isNotBlank(query.getUsername()),
                      User::getUsername, query.getUsername())
                .eq(query.getStatus() != null, User::getStatus, query.getStatus())
                .orderByDesc(User::getCreateTime);

        Page<User> result = userMapper.selectPage(page, wrapper);
        return PageResult.of(result, UserConverter::toVO);
    }
}
```

> **前提**：项目中 MUST 注册 `MybatisPlusInterceptor` 并添加 `PaginationInnerInterceptor`，否则分页不生效。

```java
package {{package_base}}.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

---

**PROF-MBP-009** 分页参数 SHOULD 封装在 `PageParam` 基类中，提供默认值与上限校验。 [SHOULD]

将分页参数统一封装在基类中，避免每个查询 DTO 重复定义 `pageNum` / `pageSize` 字段，同时通过 `@Min` / `@Max` 注解防止非法参数（如负数页码、超大 pageSize）。

```java
package {{package_base}}.model.param;

import lombok.Data;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Data
public class PageParam {

    @Min(value = 1, message = "页码最小值为 1")
    private int pageNum = 1;

    @Min(value = 1, message = "每页条数最小值为 1")
    @Max(value = 200, message = "每页条数最大值为 200")
    private int pageSize = 20;
}
```

使用方式——所有查询 DTO 继承 `PageParam`：

```java
package {{package_base}}.model.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserQuery extends PageParam {
    private String username;
    private Integer status;
}
```

---

**PROF-MBP-010** 单次查询 MUST 限制最大返回条数，默认上限 200 条，超出 MUST 返回错误或截断。 [MUST]

未限制返回条数的查询在数据量增长后可能导致内存溢出或响应超时。即使业务场景需要"查询全部"，也 MUST 设置安全上限。

```java
// 方式一：通过 PageParam 的 @Max 注解拦截（推荐）
@Max(value = 200, message = "单次查询最多返回 200 条")
private int pageSize = 20;

// 方式二：在 Service 层做兜底校验
public Page<User> listUsers(UserQuery query) {
    int safePageSize = Math.min(query.getPageSize(), 200);
    Page<User> page = new Page<>(query.getPageNum(), safePageSize);
    return userMapper.selectPage(page, buildWrapper(query));
}

// 方式三：非分页列表查询也需要限制
public List<UserVO> listAll(UserQuery query) {
    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
            .eq(User::getStatus, query.getStatus())
            .last("LIMIT 200");  // 安全上限
    return userMapper.selectList(wrapper)
            .stream()
            .map(UserConverter::toVO)
            .collect(Collectors.toList());
}
```

---

## 4. SQL 规范（MyBatis-Plus 相关）

**PROF-MBP-011** XML 映射文件 MUST NOT 使用 `SELECT *`，MUST 显式列出所需字段。 [MUST]

`SELECT *` 会导致以下问题：
1. 返回不必要的字段，增加网络传输和内存开销
2. 表结构变更后可能导致字段映射错误
3. 无法利用覆盖索引优化查询性能

```xml
<!-- 正确：显式列出字段 -->
<select id="selectById" resultType="{{package_base}}.model.vo.UserVO">
    SELECT id, username, status, create_time
    FROM sys_user
    WHERE id = #{id}
</select>

<!-- 错误：使用 SELECT * -->
<select id="selectById" resultType="{{package_base}}.model.vo.UserVO">
    SELECT * FROM sys_user WHERE id = #{id}
</select>
```

> **补充**：对于 BaseMapper 内置方法（如 `selectById`、`selectList`），MyBatis-Plus 会自动生成字段列表，不受此限制。本规则仅约束 XML 中手写的 SQL。

---

**PROF-MBP-012** 批量操作 MUST 使用 `saveBatch` / `insertBatch`，MUST NOT 在循环中逐条执行。 [MUST]

循环逐条插入会产生 N 次数据库交互，严重影响性能。`saveBatch` 会将数据分批提交，显著减少数据库往返次数。

```java
package {{package_base}}.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    // 正确：使用 saveBatch 批量插入，第二个参数为每批大小
    public void batchCreateUsers(List<User> userList) {
        this.saveBatch(userList, 500);
    }

    // 正确：使用 updateBatchById 批量更新
    public void batchUpdateStatus(List<User> userList) {
        this.updateBatchById(userList, 500);
    }

    // 错误：循环逐条插入
    public void batchCreateUsersWrong(List<User> userList) {
        for (User user : userList) {
            userMapper.insert(user);  // N 次数据库交互，严禁
        }
    }
}
```

> **性能参考**：插入 10000 条数据，逐条插入约需 30-60 秒，`saveBatch(500)` 约需 1-3 秒，性能差距 10-30 倍。

---

**PROF-MBP-013** 查询条件 MUST 使用参数化查询，MUST NOT 拼接 SQL 字符串，防止注入。 [MUST]

SQL 注入是最常见的安全漏洞之一。MyBatis-Plus 的 `LambdaQueryWrapper` 和 XML 中的 `#{}` 占位符均自动进行参数化处理，可以安全地防止 SQL 注入。

```java
// 正确：LambdaQueryWrapper 自动参数化
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
        .like(User::getUsername, keyword)
        .eq(User::getStatus, status);
List<User> users = userMapper.selectList(wrapper);

// 正确：XML 中使用 #{} 参数化
// <select id="selectByName">
//     SELECT id, username FROM sys_user WHERE username = #{name}
// </select>

// 错误：字符串拼接（SQL 注入风险）
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
wrapper.apply("username = '" + keyword + "'");  // 严禁：直接拼接用户输入

// 正确：apply 中使用占位符
wrapper.apply("username = {0}", keyword);  // 安全：参数化处理
```

```xml
<!-- 错误：使用 ${} 直接替换（SQL 注入风险） -->
<select id="selectByName" resultType="User">
    SELECT id, username FROM sys_user WHERE username = '${name}'
</select>

<!-- 正确：使用 #{} 参数化 -->
<select id="selectByName" resultType="User">
    SELECT id, username FROM sys_user WHERE username = #{name}
</select>
```

> **唯一例外**：动态表名、动态列名等 DDL 场景可以使用 `${}`，但 MUST 在代码层做白名单校验。

---

**PROF-MBP-014** WHERE 条件字段 SHOULD 建立索引，联合索引 SHOULD 遵循最左前缀原则。 [SHOULD]

没有索引的 WHERE 条件会导致全表扫描，随着数据量增长查询性能急剧下降。联合索引的最左前缀原则决定了索引的生效范围，不遵循该原则会导致索引失效。

```sql
-- 单字段索引：高频查询条件字段 SHOULD 建索引
CREATE INDEX idx_user_status ON sys_user (status);
CREATE INDEX idx_user_username ON sys_user (username);

-- 联合索引：遵循最左前缀原则
-- 以下索引可以支持：
--   WHERE tenant_id = ? AND status = ?
--   WHERE tenant_id = ?
-- 但不支持：
--   WHERE status = ?（缺少最左字段 tenant_id）
CREATE INDEX idx_user_tenant_status ON sys_user (tenant_id, status);

-- 排序字段也可以纳入联合索引
CREATE INDEX idx_user_status_create_time ON sys_user (status, create_time DESC);
```

> **实践建议**：
> 1. 对 `EXPLAIN` 结果中 `type = ALL` 的查询重点关注
> 2. 区分度低的字段（如 `gender`）不适合单独建索引
> 3. 联合索引字段数建议不超过 5 个

---

**PROF-MBP-015** 大表分页 SHOULD 使用游标分页替代 `LIMIT offset, size`，避免深分页性能问题。 [SHOULD]

传统 `LIMIT offset, size` 分页在 offset 较大时（如第 10000 页），数据库需要扫描并丢弃前 `offset` 条记录，性能急剧下降。游标分页利用主键或有序索引直接定位起始位置，性能恒定。

```xml
<!-- 游标分页示例 -->
<select id="selectByCursor" resultType="{{package_base}}.model.entity.User">
    SELECT id, username, status, create_time
    FROM sys_user
    WHERE id &gt; #{lastId}
    ORDER BY id ASC
    LIMIT #{size}
</select>
```

```java
// Service 层：游标分页封装
public CursorResult<UserVO> pageByCursor(Long lastId, int size) {
    int safeSize = Math.min(size, 200);
    List<User> users = userMapper.selectByCursor(lastId, safeSize);

    List<UserVO> voList = users.stream()
            .map(UserConverter::toVO)
            .collect(Collectors.toList());

    Long nextCursor = users.isEmpty() ? null : users.get(users.size() - 1).getId();
    boolean hasMore = users.size() == safeSize;

    return new CursorResult<>(voList, nextCursor, hasMore);
}
```

```java
// 游标分页结果封装
package {{package_base}}.model.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class CursorResult<T> {
    private List<T> data;
    private Long nextCursor;
    private boolean hasMore;
}
```

> **适用场景**：无限滚动列表、数据同步、导出任务等不需要跳页的场景。需要跳页的管理后台仍可使用传统分页。

---

## 5. Mapper 命名

**PROF-MBP-016** MyBatis Mapper 接口 MUST 以 "Mapper" 结尾。 [MUST]

统一以 `Mapper` 结尾可以在包扫描、代码检索和团队协作中快速定位持久层组件。这也是 MyBatis-Plus `@MapperScan` 扫描的惯例。

```java
// 正确
@Mapper
public interface UserMapper extends BaseMapper<User> { }

@Mapper
public interface OrderMapper extends BaseMapper<Order> { }

@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> { }

// 错误：命名不规范
@Mapper
public interface UserDao extends BaseMapper<User> { }       // 不要使用 Dao 后缀

@Mapper
public interface UserRepository extends BaseMapper<User> { } // 不要使用 Repository 后缀
```

```java
// MapperScan 配置示例
package {{package_base}}.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("{{package_base}}.mapper")
public class MyBatisScanConfig {
    // MapperScan 会自动扫描 mapper 包下所有接口
}
```

---

**PROF-MBP-017** XML 映射文件 MUST 与 Mapper 接口同名，放置在 `resources/mapper/` 目录下。 [MUST]

XML 文件名与 Mapper 接口一一对应，方便定位和维护。统一的目录结构有利于团队协作和项目交接。

```
项目目录结构：
src/main/java/
  └── {{package_base}}/
      └── mapper/
          ├── UserMapper.java          # Mapper 接口
          ├── OrderMapper.java
          └── SysConfigMapper.java

src/main/resources/
  └── mapper/
      ├── UserMapper.xml               # 对应 UserMapper.java
      ├── OrderMapper.xml              # 对应 OrderMapper.java
      └── SysConfigMapper.xml          # 对应 SysConfigMapper.java
```

```yaml
# application.yml 中的 MyBatis 配置
mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: {{package_base}}.model.entity
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # 开发环境开启，生产 MUST 关闭
```

> **大型项目建议**：当 Mapper 数量超过 30 个时，可以按业务模块在 `mapper/` 下建立子目录，如 `resources/mapper/user/`、`resources/mapper/order/`，同时更新 `mapper-locations` 为 `classpath:mapper/**/*.xml`。

---

## 6. 乐观锁

**PROF-MBP-018** 乐观锁 SHOULD 使用 MyBatis-Plus `@Version` 插件实现。 [SHOULD]

在并发更新场景下，乐观锁可以防止"丢失更新"问题。MyBatis-Plus 提供了内置的 `@Version` 注解和 `OptimisticLockerInnerInterceptor` 拦截器，无需手动编写 `WHERE version = ?` 和 `SET version = version + 1` 逻辑。

**第一步**：注册乐观锁插件

```java
package {{package_base}}.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 乐观锁插件 MUST 在分页插件之前添加
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

**第二步**：Entity 中添加 `@Version` 字段

```java
package {{package_base}}.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_product")
public class Product {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;
    private Integer stock;
    private Integer status;

    // 乐观锁版本号字段
    @Version
    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
```

**第三步**：使用乐观锁更新

```java
// 正确：先查询获取 version，再更新
// MyBatis-Plus 会自动生成如下 SQL：
// UPDATE sys_product SET stock = ?, version = version + 1
// WHERE id = ? AND version = ?
public void deductStock(Long productId) {
    Product product = productMapper.selectById(productId);
    if (product == null) {
        throw new BusinessException("商品不存在");
    }
    if (product.getStock() <= 0) {
        throw new BusinessException("库存不足");
    }
    product.setStock(product.getStock() - 1);
    int rows = productMapper.updateById(product);
    if (rows == 0) {
        // version 不匹配，说明被其他线程修改了
        throw new BusinessException("操作冲突，请重试");
    }
}

// 错误：不使用乐观锁，并发场景下会丢失更新
productMapper.update(null,
    new LambdaUpdateWrapper<Product>()
        .eq(Product::getId, productId)
        .setSql("stock = stock - 1"));  // 没有版本控制，可能超卖
```

> **适用场景**：库存扣减、余额变更、配置更新等并发修改场景。对于超高频并发（如秒杀），建议结合 Redis 分布式锁或数据库悲观锁使用。
