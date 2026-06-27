# 架构配套方案包索引

> 本文件是 L1 架构配套方案的总索引。所有包均为原子化设计，可自由组合。

| 字段 | 值 |
|------|-----|
| 版本 | 2.4 |
| 状态 | Active |
| 更新日期 | 2026-06-26 |

---

## 设计理念

架构配套方案采用**原子化组合**设计，取代过去的"一个架构一个 Profile"模式。每个包聚焦一个技术维度，开发者根据项目技术栈自由组合。

维度按平台分为两大目录：

- **`backend/`** — 后端方案（框架基座 + 架构风格 + 持久化技术 + 消息队列 + 测试框架 + 数据库迁移 + DevOps + API/错误处理/安全）
- **`frontend/`** — 前端方案（Vue 3）

## 包清单

### 后端

#### 框架基座

| 包 ID | 文件 | 说明 | 规则数 |
|-------|------|------|--------|
| spring-boot-base | `backend/spring-boot-base.md` | Spring Boot 通用基础：构造器注入、统一响应、全局异常、事务管理、测试结构、通用编码 | 24 |

#### 架构风格（互斥，后端二选一）

| 包 ID | 文件/目录 | 说明 | 规则数 |
|-------|----------|------|--------|
| arch-mvc | `backend/arch-mvc.md` | MVC 三层架构：分层结构、职责边界、Controller/Service 规范、REST 接口 | 34 |
| arch-ddd | `backend/ddd/` | DDD 四层架构，含 4 个子模块 | ~90 |

arch-ddd 子模块：

| 子模块 | 文件 | 说明 |
|--------|------|------|
| 架构结构 | `backend/ddd/arch-ddd.md` | 四层分层、依赖方向、多模块工程、Client 模式 |
| 领域模型 | `backend/ddd/domain-model.md` | 聚合根、实体、值对象、领域事件、命名规范、单元测试 |
| 事件模式 | `backend/ddd/event-patterns.md` | 领域事件设计、发布策略、处理器、事件溯源 |
| 读写分离 | `backend/ddd/cqrs.md` | 命令端/查询端分离、数据同步、反模式清单 |

#### 持久化技术（可多选）

| 包 ID | 文件 | 说明 | 规则数 |
|-------|------|------|--------|
| persistence-mybatis-plus | `backend/persistence-mybatis-plus.md` | MyBatis-Plus：BaseMapper、Entity、分页、SQL、乐观锁 | 18 |
| persistence-mybatis | `backend/persistence-mybatis.md` | 原生 MyBatis：XML 映射、动态 SQL、结果映射 | 13 |
| persistence-jpa | `backend/persistence-jpa.md` | Spring Data JPA / QueryDSL：Repository、Entity、关联映射 | 15 |
| persistence-redis | `backend/persistence-redis.md` | Redis：Key 设计、数据结构选型、缓存一致性、序列化 | 14 |
| persistence-elasticsearch | `backend/persistence-elasticsearch.md` | Elasticsearch：索引设计、查询规范、性能优化 | 14 |

#### 消息队列（互斥，多选一）

| 包 ID | 文件 | 说明 | 规则数 |
|-------|------|------|--------|
| messaging-kafka | `backend/messaging-kafka.md` | Kafka：Topic 设计、消息信封、生产者/消费者、事务消息、监控 | 20 |
| messaging-rocketmq | `backend/messaging-rocketmq.md` | RocketMQ：Topic/Tag、事务消息、顺序消息、延迟消息、与 Kafka 互斥 | 18 |

#### 测试框架

| 包 ID | 文件 | 说明 | 规则数 |
|-------|------|------|--------|
| testing-jvm | `backend/testing-jvm.md` | JVM 测试框架：JUnit 5、AssertJ、Mockito、Spring Boot 测试切片、TestContainers、参数化测试 | 12 |

#### 数据库迁移

| 包 ID | 文件 | 说明 | 规则数 |
|-------|------|------|--------|
| db-migration | `backend/db-migration.md` | Flyway/Liquibase 选型、版本号规则、脚本编写、回滚、多环境、跨服务协调 | 11 |

#### DevOps 与 CI/CD（流程落地层）

| 包 ID | 文件 | 说明 | 规则数 |
|-------|------|------|--------|
| devops-cicd | `backend/devops-cicd.md` | CI 流水线 6 阶段、Docker 多阶段构建、K8s 健康检查、4 个黄金信号告警、镜像/部署检查清单 | 14 |

#### API 设计（接口契约层）

| 包 ID | 文件 | 说明 | 规则数 |
|-------|------|------|--------|
| api-design | `backend/api-design.md` | RESTful URL 设计、统一响应格式、HTTP 状态码、业务错误码体系、版本管理、OpenAPI 契约 | 50 |

#### 错误处理与日志（异常处理层）

| 包 ID | 文件 | 说明 | 规则数 |
|-------|------|------|--------|
| error-handling | `backend/error-handling.md` | 8 种错误分类、ErrorCode 枚举、BizException、全局异常处理器、traceId 注入、结构化日志、敏感字段脱敏、Feign 异常还原 | 70 |

#### 安全实践（安全防护层）

| 包 ID | 文件 | 说明 | 规则数 |
|-------|------|------|--------|
| security-practices | `backend/security-practices.md` | SQL 注入/XSS/CSRF 防护、JWT 认证、BCrypt 密码哈希、RBAC 权限模型、安全响应头、安全审计日志、依赖/镜像扫描 | 90 |

### 前端

| 包 ID | 文件/目录 | 说明 | 规则数 |
|-------|----------|------|--------|
| frontend-vue | `frontend/vue/` | Vue 3 前端：架构、状态、API、类型、i18n、组件、Element Plus、设计 Token | ~125 |

`frontend-vue` 子结构：

| 子目录 | 文件 | 说明 | 规则数 |
|--------|------|------|--------|
| 包概览 | `frontend/vue/00-overview.md` | 包元信息 + 子文件清单 | — |
| common/ | `frontend/vue/common/architecture.md` | 三层架构、依赖方向、5 个文件定位决策树、路由规范 | 22 |
| common/ | `frontend/vue/common/structure.md` | 目录布局、就近原则、Barrel exports、导入顺序、TS 严格模式 | 19 |
| common/ | `frontend/vue/common/api-conventions.md` | API 5 件套铁律、错误处理 try/catch/finally 模板 | 14 |
| common/ | `frontend/vue/common/type-system.md` | BaseEntity 基座类型、业务实体 5 件套 | 10 |
| common/ | `frontend/vue/common/i18n.md` | 4 段式 Key 命名空间、语言包结构 | 11 |
| vue3/ | `frontend/vue/vue3/script-setup.md` | script setup 10 步顺序、defineProps/Emits/Expose | 18 |
| vue3/ | `frontend/vue/vue3/component.md` | Props 设计、组件体量、组合模式、样式隔离 | 13 |
| vue3/ | `frontend/vue/vue3/state.md` | 4 类状态（本地/共享/服务端/URL）、跨组件通信优先级 | 17 |
| vue3/ | `frontend/vue/vue3/ui-element-plus.md` | Element Plus 必填属性、弹窗/表单/表格/按钮/卡片/树 | 13 |
| vue3/ | `frontend/vue/vue3/encapsulated.md` | 10 类强制封装场景、组件选用决策树 | 3 + 10 场景 |
| vue3/ | `frontend/vue/vue3/page-patterns.md` | 页面命名铁律、flex 三段式、左树右表 | 7 |
| vue3/ | `frontend/vue/vue3/design-tokens.md` | 颜色/字体/间距/圆角/阴影、p-3 间距硬约束 | 11 |
| vue3/skeletons/（参考） | 8 份骨架代码 | 列表/编辑/左树右表/弹窗/卡片/工具栏/视图切换/状态映射 | 参考 |
| 自检清单 | `frontend/vue/selfcheck.md` | AI 编码自检清单 50+ 项 | — |

> **关键说明**：
> 1. 包 ID 是单一 `frontend-vue`，与后端 `arch-ddd`（单包 4 个子文件）结构对齐
> 2. 强制封装组件场景用 `{EncapsulatedXxx}` 占位符表示（项目实际可能是 `Dm-` / `Pro-` / `App-` / `Custom-` 等前缀，由项目配置定义）

## 互斥规则

| 包 A | 包 B | 原因 |
|------|------|------|
| arch-mvc | arch-ddd | 三层 vs 四层是互斥的架构风格 |
| persistence-mybatis-plus | persistence-mybatis | MyBatis-Plus 是 MyBatis 的增强版 |
| persistence-mybatis-plus | persistence-jpa | 不应同时使用两种 ORM |
| persistence-mybatis | persistence-jpa | 不应同时使用两种 ORM |
| messaging-kafka | messaging-rocketmq | Kafka 与 RocketMQ 是互斥的消息中间件选型 |

## 组合推荐

详见 `_composition-presets.yaml`。常见组合：

```
Spring MVC + MyBatis-Plus:
  → spring-boot-base + arch-mvc + persistence-mybatis-plus

DDD + MyBatis-Plus + Redis:
  → spring-boot-base + arch-ddd + persistence-mybatis-plus + persistence-redis

DDD + JPA:
  → spring-boot-base + arch-ddd + persistence-jpa

DDD + Kafka:
  → spring-boot-base + arch-ddd + persistence-mybatis-plus + messaging-kafka

Vue 3 前端:
  → frontend-vue
  → frontend-vue.includes: common/* + vue3/* + selfcheck.md

全栈（DDD + Vue 3）:
  → spring-boot-base + arch-ddd + persistence-mybatis-plus + persistence-redis + frontend-vue

全栈（DDD + Vue 3 + Kafka）:
  → spring-boot-base + arch-ddd + persistence-mybatis-plus + persistence-redis + messaging-kafka + frontend-vue

全栈（DDD + Vue 3 + DevOps）:
  → spring-boot-base + arch-ddd + persistence-jpa + persistence-redis + devops-cicd + frontend-vue
```

## 使用方式

### 新项目

运行 `bootstrap.sh` 自动探测技术栈并推荐组合：

```bash
bash docs/governance/scripts/bootstrap.sh
```

### 手动配置

在 `.spec/spec-manifest.yaml` 中声明 profiles 列表：

```yaml
fingerprint:
  profiles:
    - spring-boot-base
    - arch-ddd
    - persistence-mybatis-plus
    - persistence-redis
    - frontend-vue
```

---

*本文件随包结构变更自动更新。新增架构包时 MUST 同步更新本索引。*
