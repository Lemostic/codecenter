# 规范依赖关系矩阵

> 版本: 1.0 | 状态: 已激活 | 最后更新: 2026-06-17

---

## 一、概述

本文档定义规范体系中各模块之间的依赖、兼容与互斥关系。Skill 引擎在加载规范时，依据本矩阵进行合法性校验，确保加载的规范组合是有效的。

关系类型定义：

| 符号 | 含义 | 说明 |
|------|------|------|
| `→` | 依赖 | A → B 表示 A 依赖 B，B 必须先于 A 加载 |
| `+` | 兼容 | A 和 B 可以同时加载，无冲突 |
| `!` | 互斥 | A 和 B 不可同时加载，加载时引擎报错 |
| `~` | 可选依赖 | A 可以增强 B，但 B 可独立使用 |
| `-` | 无关 | A 和 B 无直接关系，互不影响 |

---

## 二、L0 层通用规范关系矩阵

| | naming-conventions | api-design | error-handling | logging-standards | code-comment-standards | data-access | mybatis-plus | redis | messaging | search-engine |
|---|---|---|---|---|---|---|---|---|---|---|
| **naming-conventions** | - | + | + | + | + | + | + | + | + | + |
| **api-design** | → | - | + | + | + | + | - | ~ | ~ | - |
| **error-handling** | → | + | - | + | + | + | - | - | + | - |
| **logging-standards** | → | + | + | - | + | + | - | + | + | + |
| **code-comment-standards** | → | + | + | + | - | + | + | + | + | + |
| **data-access** | → | + | + | + | + | - | + | + | - | + |
| **mybatis-plus** | → | - | - | - | - | → | - | - | - | - |
| **redis** | → | ~ | - | + | - | + | - | - | - | - |
| **messaging** | → | ~ | + | + | - | - | - | - | - | - |
| **search-engine** | → | - | - | + | - | + | - | - | - | - |

### 解读示例

- `api-design → naming-conventions`：API 设计规范依赖命名规范（必须先加载命名规范）
- `mybatis-plus → data-access`：MyBatis-Plus 规范依赖数据访问通用规范
- `redis ~ api-design`：Redis 规范可以增强 API 设计规范（如缓存响应头），但 API 规范可独立使用

---

## 三、L1 层架构规范关系矩阵

| | spring-boot-mvc-layering | spring-boot-mvc-config | ddd-structure | ddd-patterns | ddd-aggregate-design | cqrs-pattern | frontend-component | frontend-state | frontend-styling | hexagonal-ports |
|---|---|---|---|---|---|---|---|---|---|---|
| **spring-boot-mvc-layering** | - | + | ! | - | - | - | - | - | - | ! |
| **spring-boot-mvc-config** | → | - | - | - | - | - | - | - | - | - |
| **ddd-structure** | ! | - | - | + | + | + | - | - | - | + |
| **ddd-patterns** | - | - | → | - | → | + | - | - | - | + |
| **ddd-aggregate-design** | - | - | → | → | - | ~ | - | - | - | ~ |
| **cqrs-pattern** | - | - | → | + | ~ | - | - | - | - | + |
| **frontend-component** | - | - | - | - | - | - | - | + | + | - |
| **frontend-state** | - | - | - | - | - | - | → | - | - | - |
| **frontend-styling** | - | - | - | - | - | - | → | + | - | - |
| **hexagonal-ports** | ! | - | + | + | ~ | + | - | - | - | - |

### 互斥关系说明

| 互斥对 | 原因 |
|--------|------|
| `spring-boot-mvc-layering` ! `ddd-structure` | 分层模型不同：MVC 三层 vs DDD 四层，结构定义冲突 |
| `spring-boot-mvc-layering` ! `hexagonal-ports` | 分层模型不同：MVC 三层 vs 六边形端口/适配器，包结构冲突 |

---

## 四、L2 层领域扩展插件与 L0/L1 依赖关系

| 领域扩展插件 | 依赖（→） | 兼容（+） | 互斥（!） |
|----------|-----------|-----------|-----------|
| **spi-data-governance-rules** | naming-conventions, api-design, logging-standards | error-handling, data-access, mybatis-plus, ddd-structure, ddd-patterns | - |

---

## 五、依赖链分析

以下为已识别的关键依赖链（从根节点到叶子节点）：

### 依赖链 1：读写分离完整依赖路径

```
cqrs-pattern
   → ddd-patterns
      → ddd-structure
         → naming-conventions (L0, universal)
   → ddd-aggregate-design
      → ddd-structure
         → naming-conventions
```

加载 `cqrs-pattern` 时，引擎须按以下顺序加载前置依赖：

```
1. naming-conventions (L0, universal)
2. ddd-structure (L1)
3. ddd-patterns (L1)
4. ddd-aggregate-design (L1)
5. cqrs-pattern (L1)
```

### 依赖链 2：Spring Boot MVC 完整依赖路径

```
spring-boot-mvc-layering
   → naming-conventions (L0, universal)

spring-boot-mvc-config
   → spring-boot-mvc-layering
      → naming-conventions
```

### 依赖链 3：数据管理领域扩展完整依赖路径

```
spi-data-governance-rules (L2)
   → naming-conventions (L0, universal)
   → api-design (L0)
      → naming-conventions
   → logging-standards (L0, universal)
```

### 依赖链 4：前端框架完整依赖路径

```
frontend-state-management
   → frontend-component-design
      → naming-conventions (L0, universal)

frontend-styling
   → frontend-component-design
      → naming-conventions
```

---

## 六、冲突检测规则

Skill 引擎在加载规范时执行以下冲突检测：

### 6.1 加载前校验

```
FOR each spec in load_list:
    FOR each dep in spec.depends_on:
        IF dep NOT in loaded_specs:
            RAISE ERROR "规范 {spec.spec_id} 依赖的 {dep} 尚未加载"

    FOR each loaded in loaded_specs:
        IF (spec, loaded) in conflict_matrix:
            RAISE ERROR "规范 {spec.spec_id} 与 {loaded.spec_id} 互斥，不可同时加载"
```

### 6.2 优先级冲突裁决

当两条规范的规则内容产生语义冲突时：

```
优先级排序：
1. 层级优先：L2 > L1 > L0
2. 强制优先：MUST > SHOULD > MAY
3. 特化优先：更具体的规则 > 更通用的规则
4. 人工裁决：上述三条均无法判定时，提示开发者选择
```

### 6.3 循环依赖检测

当前规范体系中**不存在循环依赖**。所有依赖关系形成有向无环图（DAG）。

若未来新增规范引入了循环依赖，引擎将在启动阶段检测并报错：

```
ERROR: 检测到循环依赖：
  spec-a → spec-b → spec-c → spec-a
请解除循环依赖后重试。
```

---

## 七、典型项目加载组合

### 组合 A：DDD + 数据管理项目

```yaml
architecture: "ddd"
tech_stack: ["spring-boot", "mybatis-plus", "mysql", "redis", "kafka"]
domains: ["data-governance"]
```

加载顺序：

| 序号 | 规范 ID | 层级 | 加载原因 |
|------|---------|------|----------|
| 1 | naming-conventions | L0 | universal |
| 2 | error-handling | L0 | universal |
| 3 | logging-standards | L0 | universal |
| 4 | code-comment-standards | L0 | universal |
| 5 | api-design | L0 | architecture profile |
| 6 | data-access | L0 | architecture profile |
| 7 | mybatis-plus-conventions | L0 | tech_stack match |
| 8 | redis-usage-standards | L0 | tech_stack match |
| 9 | messaging-standards | L0 | tech_stack match |
| 10 | ddd-structure | L1 | architecture match |
| 11 | ddd-patterns | L1 | architecture match |
| 12 | ddd-aggregate-design | L1 | architecture match |
| 13 | cqrs-pattern | L1 | architecture match |
| 14 | spi-data-governance-rules | L2 | domain match |

### 组合 B：Spring Boot MVC 前端项目

```yaml
architecture: "spring-boot-mvc"
tech_stack: ["spring-boot", "mysql"]
domains: []
```

加载顺序：

| 序号 | 规范 ID | 层级 | 加载原因 |
|------|---------|------|----------|
| 1 | naming-conventions | L0 | universal |
| 2 | error-handling | L0 | universal |
| 3 | logging-standards | L0 | universal |
| 4 | code-comment-standards | L0 | universal |
| 5 | api-design | L0 | architecture profile |
| 6 | data-access | L0 | architecture profile |
| 7 | spring-boot-mvc-layering | L1 | architecture match |
| 8 | spring-boot-mvc-config | L1 | architecture match |

---

## 八、矩阵维护规则

1. **新增规范模块**时，必须同步更新本矩阵中的关系定义
2. **修改依赖关系**时，须在变更记录中说明原因和影响范围
3. **互斥关系**的新增须经规范委员会审批，因为会影响已有项目
4. 本矩阵每次更新后，须运行自动化兼容性检测脚本验证一致性

---

## 九、变更记录

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0 | 2026-06-17 | 初始版本，定义 L0/L1/L2 三层规范关系矩阵 |
