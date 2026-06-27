# frontend-vue 包概览

> 本文件是 `frontend-vue` 包的总入口，定义包的元信息、适用场景、与其它包的关系以及子文件清单。
> AI 在加载前端规范时 MUST 先读本文件，再按需深入到具体子文件。

| 字段 | 值 |
|------|-----|
| 版本 | 2.0 |
| 层级 | L1 |
| 包类型 | frontend |
| 包 ID | `frontend-vue` |
| 引入条件 | `fingerprint.profiles contains 'frontend-vue'` |
| 适用场景 | Vue.js 3.x 前端项目 |
| 依赖规范 | `universal/*`（命名、API、Git、测试、安全等） |

---

## 设计目标

本包的目标不是把源文档"全文搬运"过来，而是建立**面向 AI Agent 消费的工程化编码规范基础设施**：

- **AI 可解析**：所有规则有明确 ID、级别（MUST/SHOULD/MAY）、示例或反例
- **AI 可按需加载**：按子文件粒度拆分，AI 只加载与当前任务相关的章节
- **业务无关**：规范本身不包含业务术语，不绑定具体产品
- **可演进**：每个子文件独立版本化，演进不互相牵连

## 与三层模型的关系

本包属于 L1 架构配套方案层，提供**前端维度的原子化包**：

```
L0 通用规范层（universal/*） — 跨语言/跨架构/始终加载
  │
  └─ L1 架构配套方案层（profiles/*）
       ├─ 后端：spring-boot-base / arch-mvc / arch-ddd / persistence-*
       └─ 前端：frontend-vue（本包）
            ├─ common/  ── Vue 通用规范
            └─ vue3/    ── Vue 3 + Element Plus + Tailwind CSS
```

## 版本说明

本规范仅支持 Vue 3。Vue 2 已停止官方维护，不再纳入规范范围。

## 子文件清单

### common/ — Vue 通用（5 份）

| 文件 | 主题 | 主要内容 | 规则数 |
|------|------|---------|--------|
| `common/architecture.md` | 三层架构 | core/common/modules 分层、依赖方向、5 个文件定位决策树、路由规范 | 22 |
| `common/structure.md` | 目录与导入 | 标准目录布局、就近原则、Barrel exports、路径别名、导入顺序 | 19 |
| `common/api-conventions.md` | API 铁律 | 5 件套函数（list/get/create/update/delete）、错误处理 try/catch/finally 模板 | 14 |
| `common/type-system.md` | 类型系统 | BaseEntity 基座类型、业务实体 5 件套、泛型约束 | 10 |
| `common/i18n.md` | 国际化 | 4 段式 Key 命名空间、语言包结构、铁律 | 11 |

### vue3/ — Vue 3 特有（7 份）

| 文件 | 主题 | 主要内容 | 规则数 |
|------|------|---------|--------|
| `vue3/script-setup.md` | script setup 10 步 | 块内顺序、defineProps/Emits/Expose 规则、命名规范 | 18 |
| `vue3/component.md` | 组件设计 | Props 接口、组件体量、命名、组合模式、a11y、样式隔离 | 13 |
| `vue3/state.md` | 状态管理 | 4 类状态（本地/共享/服务端/URL）、跨组件通信优先级 | 17 |
| `vue3/ui-element-plus.md` | Element Plus | 必填属性清单、弹窗/表单/表格/按钮/卡片/树/描述规则 | 13 |
| `vue3/encapsulated.md` | 强制封装场景 | 10 类强制封装场景、组件选用决策树（占位符命名） | 3 + 10 场景 |
| `vue3/page-patterns.md` | 页面模式 | 页面命名铁律、{EncapsulatedPageFrame}/PageHeader/LeftTreeLayout | 7 |
| `vue3/design-tokens.md` | 设计 Token | 颜色/字体/间距/圆角/阴影 + p-3 间距硬约束 | 11 |

### vue3/skeletons/ — 骨架代码参考（8 份，参考性，非规则）

| 文件 | 内容 |
|------|------|
| `skeletons/list-page.md` | 列表页完整 + 最小骨架 + 禁忌写法 |
| `skeletons/editor-page.md` | 编辑页 flex 三段式 + PageHeader |
| `skeletons/left-tree-page.md` | 左树右表骨架 + 工具栏对齐规则 |
| `skeletons/dialog-form.md` | 弹窗内嵌表单 + el-scrollbar |
| `skeletons/card-view.md` | DmCardList + ModelCard 完整示例 |
| `skeletons/table-toolbar.md` | 单行/两行工具栏 + border 规则 |
| `skeletons/view-switcher.md` | 卡片/列表视图文字按钮切换 |
| `skeletons/status-tag-map.md` | statusTagType 标准映射表 |

### selfcheck.md — AI 编码自检清单（1 份）

| 文件 | 内容 |
|------|------|
| `../selfcheck.md` | 50+ 项 AI 编码后自检清单（common 通用 + vue3 特有） |

## 加载顺序（推荐）

1. 读本概览文件（`00-overview.md`）
2. 根据项目版本，**必读**对应子目录的 5~7 份规范文件
3. 编码完成后读 `selfcheck.md` 逐项 ✓ 检查
4. 遇到具体场景时按需查 `vue3/skeletons/` 中的骨架代码

## 变更管理

- 本包**接受外部演进**：可与 `universal/*` 共存，不强制覆盖 L0
- 子文件独立版本化（见各文件顶部 `| 版本 |` 字段）
- 变更流程遵循 `docs/ARCHITECTURE.md §4.2`：提案 → 评审 → 实施 → 发布 → 通知

---

*本包随项目演进持续更新。所有规范文本以 Markdown 编写，统一规则 ID 格式 `PROF-FE-XXX`。*
