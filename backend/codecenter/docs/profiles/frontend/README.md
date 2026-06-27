# frontend 索引

> 本目录是 L1 架构配套方案层的前端规范集合。

| 字段 | 值 |
|------|-----|
| 版本 | 2.0 |
| 状态 | Active |
| 维护者 | 规范治理小组 |

---

## 包清单

| 包 ID | 目录 | 说明 | 状态 |
|-------|------|------|------|
| `frontend-vue` | `vue/` | Vue 3 规范 | Active |
| `frontend-react` | `react/` | React 前端规范 | 规划中（未立项） |

## frontend-vue 子结构

```
vue/
├── 00-overview.md              ← 包元信息 + 子文件清单（先读这个）
├── common/                     ← Vue 通用规范（5 份）
├── vue3/                       ← Vue 3 特有规范（7 份 + 8 份骨架参考）
│   └── skeletons/              ← 骨架代码参考（按需查阅）
└── selfcheck.md                ← AI 编码自检清单
```

详细子文件清单、规则数、加载顺序见 `vue/00-overview.md`。

## 何时使用本包

- 新建/维护 Vue.js 前端项目
- 项目技术栈是 Vue 3（Composition API）→ 进入 `vue/vue3/`
- 项目使用 TypeScript → 所有 `vue3/` 下的规范都启用
- 项目使用 Element Plus → 启用 `vue3/ui-element-plus.md`

## 与其他包的关系

- **依赖**：`universal/*`（命名、API、Git、测试、安全基线）
- **不依赖**：后端 `spring-boot-base` / `arch-*` / `persistence-*`

## 维护指南

新增前端规范时：
1. 先在 `vue/00-overview.md` 的子文件清单中占位
2. 按主题选择归属（common / vue3）
3. 文件命名 `NN-主题.md`（NN 两位序号）
4. 规则编号连续 `PROF-FE-XXX`
5. 同步更新 `vue/00-overview.md` 与 `docs/profiles/PROFILES.md` 的规则数

---

*本目录是规范体系的 L1 前端子集。所有规范文本以 Markdown 编写，面向 AI Agent 消费。*
