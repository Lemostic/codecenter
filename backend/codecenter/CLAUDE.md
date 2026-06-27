
<!-- ENTERPRISE-CODING-STANDARDS -->
## 企业编码规范引擎 — 指令协议

> 本块由 \`bootstrap.sh\` 自动注入，记录规范引擎的加载协议。
> 如需更新，重新运行 \`bash docs/governance/scripts/bootstrap.sh --force\`。

### AI 执行协议（MUST 按顺序）

在收到任何编码相关任务时，你必须先执行以下三步，再开始编码：

**第 1 步：加载规范索引**

阅读 \`docs/skill/SKILL.md\`，了解规范引擎的加载原理和可用的 profiles 列表。

**第 2 步：读取项目指纹**

阅读 \`.spec/spec-manifest.yaml\` 中的 \`fingerprint.profiles\` 字段。它列出了当前项目激活的架构包 ID。本项目已激活：

\`\`\`
# 如需查看当前激活列表：
# cat .spec/spec-manifest.yaml | grep -A 20 '^fingerprint:'
\`\`\`

**第 3 步：按 profiles 加载对应规范**

根据第 2 步获得的 profiles 列表，按包 ID 一一加载：

| 包 ID | 规范文件（相对于 docs/） | 何时加载 |
|-------|------------------------|---------|
| \`spring-boot-base\` | \`profiles/backend/spring-boot-base.md\` | 任何后端项目 |
| \`arch-mvc\` | \`profiles/backend/arch-mvc.md\` | MVC 三层架构 |
| \`arch-ddd\` | \`profiles/backend/ddd/*.md\` | DDD 四层架构 |
| \`persistence-mybatis-plus\` | \`profiles/backend/persistence-mybatis-plus.md\` | 使用 MyBatis-Plus |
| \`persistence-*\` | \`profiles/backend/persistence-*.md\` | 对应持久化技术 |
| \`messaging-kafka\` | \`profiles/backend/messaging-kafka.md\` | Kafka 消息队列 |
| \`messaging-rocketmq\` | \`profiles/backend/messaging-rocketmq.md\` | RocketMQ 消息队列（与 Kafka 互斥） |
| \`testing-jvm\` | \`profiles/backend/testing-jvm.md\` | Java/JVM 项目使用 JUnit 5 + Mockito + TestContainers |
| \`db-migration\` | \`profiles/backend/db-migration.md\` | 使用 Flyway/Liquibase 管理 schema 演进 |
| \`frontend-vue\` | \`profiles/frontend/vue/00-overview.md\`（先读概览），然后加载 \`common/\` + \`vue3/\` | Vue 3 前端项目 |

### L0 通用规范（始终生效，无需在 profiles 中声明）

无论 profiles 列表是什么，以下 L0 规范始终适用：

| 规范 | 文件 |
|------|------|
| 命名规范 | \`universal/naming-conventions.md\` |
| API 设计 | \`universal/api-design.md\` |
| 安全基线 | \`universal/security-baseline.md\` |
| 测试规范 | \`universal/testing-standards.md\` |
| 日志规范 | \`universal/logging-standards.md\` |
| 异常处理 | \`universal/exception-handling.md\` |
| 链路追踪 | \`universal/request-tracing.md\` |
| Git 工作流 | \`universal/git-workflow.md\` |
| 变更范围控制 | \`universal/change-scope-control.md\` |

### 异常处理

- 如果 \`.spec/spec-manifest.yaml\` 不存在 → 提示用户运行 \`bash docs/governance/scripts/bootstrap.sh\`
- 如果某个 profile 文件不存在 → 跳过，不影响其他规范加载
- 如果 profiles 列表为空 → 只加载 L0 通用规范

### 规范体系全貌

\`\`\`
cat docs/README.md        # 体系概述 + 三级引导链图解
cat docs/governance/GOVERNANCE.md  # 治理流程
cat docs/governance/QUICKSTART.md  # 快速指南
\`\`\`
