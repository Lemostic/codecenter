# apps/mdm-model/CLAUDE.md

`@mdm/model-app` —— 主数据模型管理平台（开发服务器 http://localhost:3000）

> 根规则见 `../../../CLAUDE.md`。本文件只描述 model app 自身特有的约定。

## 启动

```bash
# 在仓库根目录
pnpm dev                 # 启动本 app
# 或在 apps/mdm-model 下
pnpm dev                 # vite
```

`.env.development` 关键变量：`VITE_USE_MOCK`、`VITE_API_BASE_URL`、`VITE_APP_TITLE`。

## 模块清单

| 模块名 | 路由前缀 | 中文名 | 权限前缀 |
|--------|---------|--------|---------|
| `model-index` | `/model-index` | 模型列表页面 | `model-index:read` |
| `model-design` | `/model-design` | 填报设计 | `model-design:read` / `:write` |
| `model-field-config` | `/model-field-config` | 属性配置 | `model-field-config:read` / `:write` |
| `model-manage` | `/model-manage` | 模型管理 | `model-manage:read` / `:write` |
| `model-quality-rule` | `/model-quality-rule` | 质量规则 | `model-quality-rule:read` / `:write` |
| `model-similar-rules` | `/model-similar-rules` | 相似规则 | `model-similar-rules:read` / `:write` |

> 未来在本 app 新增模块：创建 `src/modules/<name>/`，包含 `routes.ts` 即被自动接入。

## 与其他 app / 包的关系

- **可依赖**：`@mdm/core`、`@mdm/common`、`@mdm/config-vite`、`element-plus`、`vue-i18n` 等
- **禁止依赖**：其他 app 的私有模块
- **被依赖**：无（leaf app）

跨包复用统一放 `packages/mdm-common/`；如需新增共享 composable/组件，在那里加而**不是**在 `src/components/`。
