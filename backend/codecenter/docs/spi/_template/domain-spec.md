| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 层级 | L2 |
| 引入条件 | domains contains '{{domain_tag}}' |
| 适用架构 | {{domain_description}} |
| 依赖规范 | {{dependency_list}} |
| 互斥规范 | 无 |

# {{domain_display_name}} 领域规范

> 版本: 1.0 | 状态: 草案 | 最后更新: {{date}}

---

## 一、概述

本规范定义 `{{domain_name}}` 领域的通用编码规则，适用于所有涉及该领域业务逻辑的项目。规范条目按优先级分为：

- **MUST（强制）**：违反将导致代码审查不通过
- **SHOULD（推荐）**：强烈建议遵守，违反需说明理由
- **MAY（可选）**：根据项目实际情况选择

---

## 二、核心规则

### 规则 {{DOMAIN_ABBR}}-001：{{rule_title_1}}

**优先级：MUST**

{{rule_description_1}}

```java
// 正例：{{good_example_description_1}}
{{good_code_example_1}}
```

```java
// 反例：{{bad_example_description_1}}
{{bad_code_example_1}}
```

---

### 规则 {{DOMAIN_ABBR}}-002：{{rule_title_2}}

**优先级：MUST**

{{rule_description_2}}

```java
// 正例
{{good_code_example_2}}
```

```java
// 反例
{{bad_code_example_2}}
```

---

### 规则 {{DOMAIN_ABBR}}-003：{{rule_title_3}}

**优先级：SHOULD**

{{rule_description_3}}

---

## 三、扩展规则

### 规则 {{DOMAIN_ABBR}}-101：{{ext_rule_title_1}}

**优先级：SHOULD**

{{ext_rule_description_1}}

---

### 规则 {{DOMAIN_ABBR}}-102：{{ext_rule_title_2}}

**优先级：MAY**

{{ext_rule_description_2}}

---

## 四、验收标准

| 验收项 | 标准 |
|--------|------|
| 规则覆盖率 | 核心规则（MUST）≥ 5 条 |
| 代码示例 | 每条 MUST 规则至少提供 1 组正反例 |
| 依赖声明 | `depends_on` 中列出所有依赖的基础规范 |
| 命名一致性 | 规则编号前缀与 `_profile.yaml` 中的领域缩写一致 |

---

## 五、变更记录

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0 | {{date}} | 初始版本 |
