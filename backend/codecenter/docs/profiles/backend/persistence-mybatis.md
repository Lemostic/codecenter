| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 层级 | L1 |
| 包类型 | persistence |
| 引入条件 | fingerprint.profiles contains 'persistence-mybatis' |
| 适用场景 | 使用原生 MyBatis（非 MyBatis-Plus）作为持久化框架的项目 |
| 依赖规范 | spring-boot-base |

# MyBatis 持久化规范

本包定义原生 MyBatis 的使用约定，适用于不使用 MyBatis-Plus 的项目。

## Mapper 接口约定

**PROF-MB-001** Mapper 接口 MUST 以 `Mapper` 结尾。 [MUST]

**PROF-MB-002** 所有 SQL MUST 定义在 XML 映射文件中，MUST NOT 使用注解 SQL（`@Select`、`@Insert` 等）。 [MUST]

```java
@Mapper
public interface UserMapper {
    User selectById(@Param("id") Long id);
    List<User> selectByCondition(@Param("query") UserQuery query);
    int insert(User user);
    int updateById(User user);
    int deleteById(@Param("id") Long id);
}
```

**PROF-MB-003** XML 文件 MUST 与 Mapper 接口同名，放在 `resources/mapper/` 目录下。 [MUST]

```
UserMapper.java  →  resources/mapper/UserMapper.xml
OrderMapper.java →  resources/mapper/OrderMapper.xml
```

## XML 编写约定

**PROF-MB-004** XML MUST NOT 使用 `SELECT *`，MUST 显式列出字段。 [MUST]

**PROF-MB-005** 查询条件 MUST 使用参数化查询（`#{}`），MUST NOT 使用 `${}` 拼接，防止 SQL 注入。 [MUST]

**PROF-MB-006** 动态 SQL SHOULD 使用 `<where>` + `<if>` 标签，MUST NOT 手动拼接 `WHERE 1=1`。 [SHOULD/MUST]

```xml
<select id="selectByCondition" resultType="User">
    SELECT id, username, status, create_time
    FROM sys_user
    <where>
        <if test="query.username != null and query.username != ''">
            AND username LIKE CONCAT('%', #{query.username}, '%')
        </if>
        <if test="query.status != null">
            AND status = #{query.status}
        </if>
    </where>
    ORDER BY create_time DESC
</select>
```

**PROF-MB-007** 批量操作 MUST 使用 `<foreach>` 标签，MUST NOT 在循环中逐条执行。 [MUST]

```xml
<insert id="insertBatch">
    INSERT INTO sys_user (username, status, create_time)
    VALUES
    <foreach collection="list" item="item" separator=",">
        (#{item.username}, #{item.status}, NOW())
    </foreach>
</insert>
```

## 分页约定

**PROF-MB-008** 分页查询 SHOULD 使用 PageHelper 或 RowBounds，MUST NOT 手动拼接 LIMIT。 [SHOULD/MUST]

**PROF-MB-009** 单次查询 MUST 限制最大返回条数，默认上限 200 条。 [MUST]

## Entity / DO 设计

**PROF-MB-010** Entity 类 SHOULD 使用 POJO 风格，MUST 包含审计字段（createTime、updateTime）。 [SHOULD/MUST]

**PROF-MB-011** 主键策略 MUST 在插入 SQL 中明确处理（雪花算法或自增）。 [MUST]

## 结果映射

**PROF-MB-012** 复杂结果映射 MUST 使用 `<resultMap>` 定义，MUST NOT 依赖自动映射（驼峰转换除外）。 [MUST]

**PROF-MB-013** 多表关联查询 SHOULD 使用 `<association>` 和 `<collection>` 标签处理嵌套映射。 [SHOULD]
