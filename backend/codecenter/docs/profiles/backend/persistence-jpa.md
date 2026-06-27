| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 层级 | L1 |
| 包类型 | persistence |
| 引入条件 | fingerprint.profiles contains 'persistence-jpa' |
| 适用场景 | 使用 JPA / Spring Data JPA 作为持久化框架的项目 |
| 依赖规范 | spring-boot-base |

# JPA 持久化规范

本包定义 JPA（Spring Data JPA）的使用约定，包括 QueryDSL 集成。

## Repository 接口约定

**PROF-JPA-001** Repository 接口 MUST 继承 `JpaRepository<T, ID>` 或 `PagingAndSortingRepository<T, ID>`，以 `Repository` 结尾。 [MUST]

```java
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByStatus(Integer status);
    Optional<User> findByUsername(String username);
    Page<User> findByStatus(Integer status, Pageable pageable);
}
```

**PROF-JPA-002** 简单查询 SHOULD 使用 Spring Data 方法名推导（Method Name Query Derivation），复杂查询 MUST 使用 `@Query` 或 QueryDSL。 [SHOULD/MUST]

**PROF-JPA-003** 查询方法 MUST NOT 使用 Native SQL，除非 JPA QL 无法表达（如数据库特定函数）。 [MUST]

```java
// 正确：JPQL
@Query("SELECT u FROM User u WHERE u.status = :status AND u.username LIKE %:keyword%")
List<User> searchByKeyword(@Param("status") Integer status, @Param("keyword") String keyword);

// 允许：数据库特定函数
@Query(value = "SELECT * FROM sys_user WHERE JSON_EXTRACT(extra, '$.tag') = :tag", nativeQuery = true)
List<User> findByExtraTag(@Param("tag") String tag);
```

## Entity 设计

**PROF-JPA-004** Entity 类 MUST 使用 `@Entity` 和 `@Table` 注解，MUST 显式指定表名。 [MUST]

**PROF-JPA-005** Entity MUST 包含审计字段，SHOULD 使用 `@EntityListeners(AuditingEntityListener.class)` 自动填充。 [MUST/SHOULD]

```java
@Entity
@Table(name = "sys_user")
@Data
@EntityListeners(AuditingEntityListener.class)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String username;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createTime;

    @LastModifiedDate
    private LocalDateTime updateTime;

    @Version
    private Integer version;  // 乐观锁
}
```

**PROF-JPA-006** 主键策略 MUST 通过 `@GeneratedValue` 显式声明，推荐 `IDENTITY`（自增）或自定义雪花算法。 [MUST]

## 关联映射

**PROF-JPA-007** 关联关系 MUST 使用 `FetchType.LAZY`，MUST NOT 使用 `EAGER` 加载。 [MUST]

**PROF-JPA-008** 双向关联 SHOULD 仅在必要时使用，MUST 正确设置 `mappedBy` 避免重复外键列。 [SHOULD/MUST]

**PROF-JPA-009** 多对多关联 MUST 使用中间表实体替代 `@ManyToMany`，以获得更好的可控性。 [MUST]

## N+1 问题防护

**PROF-JPA-010** 查询涉及关联关系时 MUST 使用 `JOIN FETCH` 或 `@EntityGraph` 避免 N+1 问题。 [MUST]

```java
@Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.id = :id")
Optional<Order> findByIdWithItems(@Param("id") Long id);
```

**PROF-JPA-011** SHOULD 在开发环境启用 `hibernate.generate_statistics` 监控 N+1 查询。 [SHOULD]

## QueryDSL 集成

**PROF-JPA-012** 复杂动态查询 SHOULD 使用 QueryDSL 替代 Criteria API。 [SHOULD]

```java
@Repository
@RequiredArgsConstructor
public class UserQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<UserDTO> searchUsers(UserSearchCondition condition) {
        QUser user = QUser.user;

        return queryFactory
            .select(Projections.constructor(UserDTO.class,
                user.id, user.username, user.status))
            .from(user)
            .where(
                condition.getUsername() != null
                    ? user.username.contains(condition.getUsername()) : null,
                condition.getStatus() != null
                    ? user.status.eq(condition.getStatus()) : null
            )
            .orderBy(user.createTime.desc())
            .fetch();
    }
}
```

**PROF-JPA-013** QueryDSL Q 类 MUST 通过 Maven/Gradle 插件自动生成，MUST NOT 手写。 [MUST]

## 性能约定

**PROF-JPA-014** 批量操作 MUST 使用 `saveAll()` 配合 `spring.jpa.properties.hibernate.jdbc.batch_size` 配置。 [MUST]

**PROF-JPA-015** 大结果集 MUST 使用 `Stream<T>` 或分页处理，MUST NOT 一次加载全部数据到内存。 [MUST]
