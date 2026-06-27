# 版本管理策略

> 版本: 1.0 | 状态: 已激活 | 最后更新: 2026-06-17

---

## 一、版本模型

每个规范模块（spec module）采用**独立的语义化版本**（Semantic Versioning），格式为：

```
MAJOR.MINOR[.PATCH]
```

| 版本段 | 含义 | 示例场景 |
|--------|------|----------|
| **MAJOR** | 存在不兼容变更 | 删除某条 MUST 规则；改变规则语义；重组规范结构 |
| **MINOR** | 新增规则，向后兼容 | 新增 SHOULD 规则；补充代码示例 |
| **PATCH** | 修正，不影响规则语义 | 修正错别字；更新代码示例中的语法错误 |

---

## 二、独立版本管理

每个规范模块独立维护版本号，互不影响：

```
docs/specs/L0/naming-conventions/     → v2.1
docs/specs/L0/api-design/             → v1.3
docs/specs/L1/ddd/structure/          → v1.0
docs/spi/data-governance/             → v1.0
```

版本号记录位置：

- **规范模块**：各模块的 `_index.yaml` 文件中 `version` 字段
- **领域扩展插件**：`_profile.yaml` 文件中 `version` 字段
- **路由规则**：`routing-rules.yaml` 文件顶部 `version` 字段
- **Schema 文件**：`spec-manifest.schema.yaml` 中 `$schema` 注释

---

## 三、版本跟随策略

项目 manifest（`spec-manifest.yaml`）默认跟随各模块的**最新稳定版本**。具体策略如下：

| 场景 | 策略 | 说明 |
|------|------|------|
| 模块发布 MINOR 版本 | 自动跟随 | 新增规则不影响已有代码 |
| 模块发布 PATCH 版本 | 自动跟随 | 修正类变更无需人工干预 |
| 模块发布 MAJOR 版本 | 人工确认后跟随 | 需评估不兼容变更的影响范围 |
| 领域扩展插件废弃 | 迁移到新插件后解除跟随 | 废弃插件保留 2 个版本周期 |

manifest 中不记录具体版本号，只引用 `spec_id`，版本号由 `routing-rules.yaml` 统一管理。

---

## 四、MAJOR 版本升级迁移流程

当某规范模块发布 MAJOR 版本升级时，须执行以下迁移流程：

### 4.1 影响评估

1. 对比新旧版本的规则差异（diff），识别所有不兼容变更
2. 统计受影响的项目数量（通过各项目的 manifest 引用关系）
3. 评估迁移工作量，产出迁移评估报告

### 4.2 迁移指南编写

每个 MAJOR 升级必须附带迁移指南文档，格式如下：

```markdown
# {spec_id} v{OLD_MAJOR} → v{NEW_MAJOR} 迁移指南

## 不兼容变更列表

| 规则编号 | 变更类型 | 旧规则 | 新规则 | 影响范围 |
|----------|----------|--------|--------|----------|
| NC-003   | 删除     | xxx    | -      | 所有使用该规则的项目 |
| NC-007   | 语义变更 | xxx    | yyy    | Controller 类 |

## 迁移步骤

1. {步骤 1}
2. {步骤 2}
3. {步骤 3}

## 自动化迁移工具

（如可编写脚本自动完成迁移，提供工具链接）

## 已知风险与回滚方案

- 风险 1：{描述} → 回滚方案：{方案}
```

### 4.3 灰度发布

- MAJOR 升级先在 **1-2 个试点项目** 验证
- 试点通过后，分批推广到所有受影响项目
- 每批推广后收集反馈，必要时暂停推广

### 4.4 废弃旧版本

- MAJOR 升级完成后，旧版本进入 **维护期**（仅修复严重 BUG）
- 维护期持续 **2 个 MINOR 版本周期**（约 3-6 个月）
- 维护期结束后，旧版本标记为 `archived`

---

## 五、变更日志（Changelog）要求

每次版本发布必须在对应模块目录下更新 `CHANGELOG.md`，格式遵循 [Keep a Changelog](https://keepachangelog.com/) 规范：

```markdown
# Changelog

## [1.2.0] - 2026-06-15

### Added
- 新增 DG-011 监控指标告警阈值规则（SHOULD）

### Changed
- DG-003 日志格式从 JSON 改为结构化对象（不影响语义）

### Fixed
- DG-008 敏感等级枚举缺少 INTERNAL 级别说明

## [1.1.0] - 2026-05-20

### Added
- 新增 DG-010 数据画像采集钩子规则（SHOULD）

## [1.0.0] - 2026-04-01

### Added
- 初始版本，包含 DG-001 至 DG-009 共 9 条规则
```

### Changelog 规则

1. **每条变更必须关联规范 ID**：如"新增 DG-011"，便于追溯
2. **分类清晰**：Added（新增）、Changed（变更）、Deprecated（废弃）、Removed（删除）、Fixed（修复）
3. **时间倒序**：最新版本在最上方
4. **不可省略**：即使是 PATCH 版本也必须记录变更内容

---

## 六、版本号与 Git 标签

规范仓库使用 Git 标签标记版本发布：

```bash
# 标签格式
git tag -a "specs/{spec_id}/v{version}" -m "Release {spec_id} v{version}"

# 示例
git tag -a "specs/naming-conventions/v2.1" -m "Release naming-conventions v2.1"
git tag -a "spi/data-governance/v1.0" -m "Release data-governance SPI v1.0"
```

---

## 七、版本兼容性矩阵

Skill 引擎在加载规范时，会检查各模块版本之间的兼容性。兼容性信息由 `meta/dependency-matrix.md` 维护。

当检测到不兼容版本组合时，引擎行为：

1. 发出警告，提示开发者具体冲突的模块和版本
2. 回退到最近一个兼容版本组合
3. 若无法回退，仅加载无冲突的模块，并记录 `.spec/compatibility-warning.log`

---

## 八、版本管理委员会

版本发布遵循以下审批流程：

| 版本类型 | 审批角色 | 审批周期 |
|----------|----------|----------|
| PATCH | 规范维护者（单人） | 即时 |
| MINOR | 规范委员会（2 人及以上） | 周会 |
| MAJOR | 技术委员会（3 人及以上） | 月度评审 |

所有版本发布须在规范仓库的 Release Notes 中公示。
