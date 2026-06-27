| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 层级 | L1 |
| 包类型 | persistence-extension |
| 引入条件 | `fingerprint.profiles contains 'db-migration'` |
| 适用架构 | Spring Boot + Flyway 或 Liquibase |
| 依赖规范 | `universal/naming-conventions.md`、`profiles/backend/persistence-mybatis-plus.md` 或 `persistence-jpa.md` |
| 互斥规范 | 无（同一项目内 Flyway 与 Liquibase 二选一,但本包规范兼容两种工具） |

# 数据库迁移规范

> 本包定义关系型数据库 schema 变更的管理约定,涵盖版本号规则、迁移脚本编写、命名规范、回滚策略、多环境执行顺序。Spring Boot 项目使用 Flyway 或 Liquibase 时必须激活本包。
>
> 与 `persistence-mybatis-plus` / `persistence-jpa` 互补:持久化包定义"代码如何访问数据",本包定义"schema 如何演进"。

---

## 一、迁移工具选型

### 规范 DB-MIG-001: 工具选型 [MUST]

**规则:**

1. 新项目 Spring Boot 集成数据库迁移 MUST 选 Flyway 或 Liquibase 之一,**禁止**用 Hibernate `ddl-auto=update` 维护生产 schema。
2. Flyway vs Liquibase 选择:
   - **Flyway** 优先用于:团队习惯 SQL 优先、迁移脚本以 DDL 为主、版本号语义清晰。
   - **Liquibase** 优先用于:需要 XML/YAML/JSON 描述变更、需要在脚本里写复杂条件分支、跨异构数据库。
3. 同一项目 MUST 选其一,禁止同时使用两个工具管理同一 schema。
4. 选型 SHOULD 在项目立项时确定并写入 `docs/PROFILES.md` 注释,中途切换工具 MUST 经过团队评审。
5. 选型应记录在 `pom.xml` / `build.gradle` 依赖列表中,`db-migration` 包的引用者 MUST 显式声明工具。

---

## 二、版本号规则

### 规范 DB-MIG-002: 版本号命名 [MUST]

**规则:**

1. 迁移脚本版本号 MUST 使用语义化版本:`V<YYYY>.<MM>.<DD>.<序号>__<描述>.sql`(Flyway) 或 `v<YYYY>.<MM>.<DD>.<序号>__<描述>.xml`(Liquibase)。
2. 示例:
   - Flyway: `V20260618.001__create_user_table.sql`、`V20260618.002__add_email_index.sql`
   - Liquibase: `v20260618.001__create_user_table.xml`
3. 序号 SHOULD 从 `001` 开始每日递增,保证同一日内的多个迁移按写入顺序应用。
4. 描述部分 MUST 用英文小写 + 下划线(`snake_case`),动词开头(`create_` / `add_` / `drop_` / `alter_` / `index_` / `seed_`)。
5. 禁止使用语义化版本号(MAJOR.MINOR.PATCH),Flyway 不支持同名多版本,会执行失败。
6. 禁止用日期 + 时间戳(避免同一分钟内多脚本冲突)。
7. 已发布到生产环境的版本号 MUST 永久保留,禁止删除或重命名。

**示例:**

```
# ✅ 正确
V20260618.001__create_users_table.sql
V20260618.002__add_users_email_index.sql
V20260618.003__seed_default_roles.sql

# ❌ 错误
V1.0__create_users_table.sql          # 不支持 MAJOR.MINOR.PATCH
V20260618143000__create_users_table.sql # 同一秒内多脚本冲突
create_users_table.sql                 # 无版本号
```

---

## 三、迁移脚本编写

### 规范 DB-MIG-003: 脚本内容规范 [MUST]

**规则:**

1. 每个迁移脚本 MUST 只包含**一个**原子变更(创建一张表 / 加一个索引 / 改一列类型),禁止多变更打包。
2. 脚本 MUST 显式声明事务边界:Flyway 自动包装单文件为事务(除 PostgreSQL 的 DDL),Liquibase 需 `<rollback>` 配套。
3. 脚本 SHOULD 包含 `COMMENT` 或脚本头部注释说明变更原因、关联 Issue、Ticket 编号。
4. 禁止在迁移脚本中嵌入业务代码(Java / Kotlin 调用、ORM 映射)。
5. 字段类型 MUST 显式声明,禁止依赖数据库隐式默认(尤其字符集、时区)。
6. 字符串字段 MUST 显式声明字符集(`CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci`)与排序规则。
7. 时间字段 MUST 显式声明时区(`TIMESTAMP WITH TIME ZONE` 或 `DATETIME` + 应用层统一 UTC),禁止裸 `TIMESTAMP`。
8. 金额字段 MUST 使用 `DECIMAL(M, D)` 或 `BIGINT`(以分存储),禁止 `FLOAT` / `DOUBLE`。
9. 主键 MUST 显式声明(`PRIMARY KEY (id)`),禁止依赖 ORM 自动建主键。
10. 创建表 MUST 显式声明审计字段:`created_at`, `updated_at`, `created_by`, `updated_by`, `is_deleted`(逻辑删除标记,若需要)。
11. 表名 MUST 用复数小写 + 下划线(`users`、`order_items`),与代码 `*Entity` 类名一一对应。
12. 索引 MUST 显式命名(`idx_users_email`),禁止依赖数据库自动生成索引名。
13. 字段名 MUST 用单数小写 + 下划线(`user_id`、`created_at`),与 Java 驼峰字段名通过 ORM 映射。

**示例:**

```sql
-- V20260618.001__create_users_table.sql
-- 创建用户表(关联 Issue #1234 用户中心重构)

CREATE TABLE users (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    email           VARCHAR(255)    NOT NULL,
    username        VARCHAR(64)     NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    status          VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by      BIGINT          NULL,
    updated_by      BIGINT          NULL,
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_status (status)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='用户主表';
```

---

### 规范 DB-MIG-004: 索引与约束设计 [MUST]

**规则:**

1. 频繁查询字段(status / type / created_at / 外键) MUST 建索引,索引名格式:`idx_<表名>_<字段名>`。
2. 唯一约束 MUST 显式命名:`uk_<表名>_<字段名>`。
3. 外键约束 SHOULD 在数据库层声明(`FOREIGN KEY`),应用层不重复校验(避免双写不一致)。
4. 复合索引 MUST 按"高基数 + 范围查询"字段顺序排列,单字段索引 SHOULD 标注为复合索引的前缀以避免冗余。
5. 索引数量 MUST 控制:单表索引数 SHOULD ≤ 8,过多时考虑冗余字段或归档表。
6. 禁止在低基数字段(`status` 只有 3 个枚举值)上建独立索引,除非配合复合索引使用。
7. 软删除字段(`is_deleted`)建议在常用查询条件中加 `WHERE is_deleted = 0`,索引应包含 `is_deleted` 列。

---

## 四、回滚策略

### 规范 DB-MIG-005: 回滚脚本 [MUST]

**规则:**

1. 破坏性变更(`DROP` / `ALTER` 删除列 / 改类型) MUST 提供回滚脚本。
2. Flyway: 回滚脚本命名 `U<YYYY>.<MM>.<DD>.<序号>__<描述>.sql`,与 `V` 脚本序号一致。
3. Liquibase: 变更文件 MUST 包含 `<rollback>` 块。
4. 禁止在生产环境手工执行 SQL 回滚(必须通过迁移工具),便于审计与回放。
5. 数据回滚(数据迁移 + 修复) MUST 提供"撤销 SQL + 重新应用 V 脚本"两条路径,不可只有数据删除。
6. 已经执行到生产环境的 V 脚本 MUST NOT 删除或重命名,只能追加 U 回滚脚本。
7. 测试环境回滚 SHOULD 用 `flyway:clean` + 重新 `flyway:migrate`(仅限 dev/test,生产环境 MUST NOT clean)。

---

## 五、多环境执行

### 规范 DB-MIG-006: 多环境执行顺序 [MUST]

**规则:**

1. 迁移脚本 MUST 在以下顺序执行:
   - **dev**（开发环境）— 单个开发者本地数据库
   - **test**（测试环境）— 共享开发测试库,CI 跑集成测试用
   - **staging**（预发环境）— 与生产配置一致,用于回归测试
   - **prod**（生产环境）— 真实用户数据
2. 每个 V 脚本 MUST 在前一环境跑过且无错误后,才能进入下一环境。
3. dev 阶段允许用 `flyway:clean` + 重新 `migrate` 反复重置;test / staging / prod MUST 严禁 clean。
4. staging 与 prod 迁移 MUST 由 DBA 或授权人员手工触发(避免 CI 自动推送破坏性变更),或使用单独的"生产发布"CI 流水线。
5. CI 流水线 SHOULD 包含迁移脚本的 dry-run 验证:`flyway:validate`(检查版本号连续性、SQL 语法)。
6. 跨环境脚本内容 MUST 一致(同一 commit 生成的 V 脚本,在所有环境跑相同内容),禁止在不同环境分别写脚本。

---

## 六、不可逆迁移的特别约定

### 规范 DB-MIG-007: 破坏性变更流程 [MUST]

**规则:**

1. 以下变更 MUST 走"扩展 + 收缩"两步迁移,禁止一次性破坏性变更:
   - 删除列 / 删除表
   - 改列类型(尤其是 NOT NULL 字段)
   - 改主键 / 外键约束
   - 改唯一索引(可能引入重复值)
2. 第一步:MUST 先做"双写期"——新字段/新表就位,代码同时读写新旧两处。
3. 第二步:数据迁移与验证(后台 Job 或迁移工具批处理),确保数据一致。
4. 第三步:切换代码只读新字段/新表。
5. 第四步(可选):观察稳定期后,再发脚本删除旧字段/旧表。
6. 任何破坏性变更 MUST 在 PR 描述、文档、commit message 中明确标注 `BREAKING`。
7. 涉及千万级以上数据量的 schema 变更 SHOULD 在低峰期执行,且 MUST 提前通知运维与业务方。

---

## 七、回滚机制

### 规范 DB-MIG-008: 迁移失败回滚 [MUST]

**规则:**

1. 单条迁移脚本执行失败 MUST 自动回滚该脚本(若该数据库支持 DDL 事务,如 PostgreSQL),Flyway 默认开启。
2. MySQL 不支持 DDL 事务,失败时 MUST 手工干预:记录失败点 + 提供补丁脚本 `V<原序号>.1__fix_xxx.sql`。
3. 跨脚本批量迁移(SQL 段)失败时,已执行的脚本 MUST 保留在 `flyway_schema_history` 表中,禁止手工删除记录。
4. 修复后 MUST 重新跑 `flyway:validate` 确认一致性。
5. 失败的迁移 MUST NOT 通过修改已执行的脚本"重置状态"——必须新增修复脚本。

---

## 八、应用启动集成

### 规范 DB-MIG-009: Spring Boot 集成 [MUST]

**规则:**

1. `pom.xml` / `build.gradle` MUST 显式声明 Flyway 或 Liquibase 依赖,版本与 Spring Boot 兼容矩阵匹配。
2. `application.yml` / `application-test.yml` MUST 配置 `spring.flyway.enabled=true`(或 `spring.liquibase.enabled=true`)。
3. 生产环境 MUST 设置 `spring.flyway.baseline-on-migrate=true`(已有 schema 升级场景)。
4. `spring.jpa.hibernate.ddl-auto` MUST 设为 `validate` 或 `none`,禁止 `update` / `create` / `create-drop`(避免与迁移工具冲突)。
5. 迁移脚本目录 MUST 放在 `src/main/resources/db/migration/`(Maven 标准),Spring Boot 自动发现。
6. 多数据源场景下,每个数据源 MUST 显式配置独立的 Flyway/Liquibase Bean,绑定到对应的 DataSource。
7. CI 流水线 MUST 在打包前跑 `flyway:validate`,失败时阻断构建(避免无效 SQL 部署)。

---

## 九、种子数据

### 规范 DB-MIG-010: 种子数据 [MUST]

**规则:**

1. 必要的基础数据(字典、配置、默认角色) MUST 通过迁移脚本 `__seed_*.sql` 注入,禁止应用启动时硬编码插入。
2. 种子数据脚本 MUST 与 schema 脚本使用相同版本号机制(`V<YYYY>.<MM>.<DD>.<序号>__seed_*.sql`)。
3. 种子数据 MUST 是 `idempotent` 的:重复执行结果一致(用 `INSERT ... ON DUPLICATE KEY UPDATE` 或 `MERGE`)。
4. 生产环境的种子数据 MUST 经过 DBA 评审,禁止开发环境直接迁移。
5. 测试数据(SQL fixture) MUST 放在 `src/test/resources/db/test-data/`,与生产迁移目录分开。

---

## 十、跨服务迁移协调

### 规范 DB-MIG-011: 跨服务 schema 依赖 [MUST]

**规则:**

1. 微服务架构下,每个服务 MUST 独立管理自己的 schema(数据库隔离),禁止跨服务共享表。
2. 服务 A 引用服务 B 的数据时,MUST 通过 API 调用或事件订阅,禁止直接读 B 的表。
3. 跨服务的 schema 变更顺序 MUST 在 PR 描述中标注,合并时按依赖图部署:
   - 先发布"扩展期"变更(添加字段、表)
   - 再发布"使用期"变更(代码读写新字段)
   - 最后发布"收缩期"变更(删除旧字段)
4. 跨服务的迁移脚本 SHOULD 在低峰期(凌晨)部署,部署前 MUST 通知上下游服务方。
5. 跨服务发布顺序 MUST 写入 CI 流水线或运维手册,新人接手时 MUST 严格遵循。

---

## 变更记录

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0 | 2026-06-18 | 初版:Flyway/Liquibase 选型 + 版本号规则 + 脚本编写 + 回滚 + 多环境 + 破坏性变更 + 跨服务协调 11 条规则 |

---

*本包是持久化包(`persistence-mybatis-plus` / `persistence-jpa`)的 L1 扩展,定义 schema 演进规则。本包 MUST NOT 包含 SQL 方言特定语法外的 DML 操作。*
