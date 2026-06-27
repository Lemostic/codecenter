# OpenWolf

@.wolf/OPENWOLF.md

This project uses OpenWolf for context management. Read and follow .wolf/OPENWOLF.md every session. Check .wolf/cerebrum.md before generating code. Check .wolf/anatomy.md before reading files.


# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目形态

这是一个 **pnpm + turbo monorepo**，包含一个独立部署的 Vue 3 应用和三个共享 workspace 包。

```
master-data-app/
├── apps/
│   └── mdm-model/       @mdm/model-app        端口 3000  主数据模型管理平台
├── packages/
│   ├── config-vite/     @mdm/config-vite      共享 Vite 配置工厂（自动导入、Element Plus resolver、alias）
│   ├── mdm-common/      @mdm/common           共享组件库（TpTable/TpEmpty/TpCardList/TpTree/...）、composable、样式
│   └── mdm-core/        @mdm/core             框架基础（HTTP/Auth/Router/I18n/Layout/Config）
├── docs/                 需求/架构/模块/规范文档
├── turbo.json
├── pnpm-workspace.yaml
└── CLAUDE.md（本文档）
```

依赖方向（**硬约束**）：

```
apps/*  ──→  packages/mdm-common  ──→  packages/mdm-core
                │                          ▲
                └──────────────────────────┘
```

- ❌ `mdm-core` 禁止依赖 `mdm-common` 或 `apps/*`
- ❌ `mdm-common` 禁止依赖 `apps/*`
- ❌ `apps/*` 之间禁止互相 import（共享走 `mdm-common`）
- ✅ app 可同时依赖 `mdm-common` 和 `mdm-core`

## 选择从哪里启动 Claude

| 启动位置 | 适用场景 | 加载的 CLAUDE.md |
|---------|---------|----------------|
| 仓库根目录 | 跨包重构、改共享组件、查看 monorepo 配置 | 根 `CLAUDE.md` |
| `apps/mdm-model/` | 只动 model app 的页面/模块 | 根 + `apps/mdm-model/CLAUDE.md` |
| `packages/mdm-common/` 或 `packages/mdm-core/` | 改共享包 | 根 + 该包 CLAUDE.md（暂无，建议按需加） |

从子目录启动时，跨包访问通过 `additionalDirectories` 或 `pnpm` workspace 软链天然可见；不必额外授权。

## 常用命令

```bash
# 在仓库根目录
pnpm dev                 # turbo run dev，启动 @mdm/model-app
pnpm build               # 全量构建
pnpm typecheck           # turbo run typecheck，对所有包跑 vue-tsc --noEmit
pnpm lint                # eslint .
pnpm clean               # turbo run clean
```

**未配置测试运行器**，不要凭空执行 `pnpm test`。如未来添加，遵循新增脚本。

## 应用入口

app 的 `src/main.ts` 是同一个 bootstrap 模板（见 `@mdm/core/config`、`@mdm/core/router`、`@mdm/core/i18n`）：

1. `loadAndApplyConfig(new StaticProvider())` 加载 PlatformConfig
2. 根据 `config.i18n.locale` 动态 import Element Plus 语言包
3. `createApp` → `use(pinia / router / i18n / ElementPlus)`
4. `useAppStore().init(config)` 注入配置
5. `app.mount('#app')` + 设置 `document.title`

## Mock 与真实后端

每个 app 自己的 `vite.config.ts` 读取该 app 的 `.env.development`，根据 `VITE_USE_MOCK` 切换：

- `VITE_USE_MOCK=true` → 注册该 app 的 `mock/vite-plugin-mock.ts` 中间件，拦截 `/api/*` 返回假数据，并**禁用** proxy
- `VITE_USE_MOCK=false` → 不注册中间件；`server.proxy['/api']` 转发到 `VITE_API_BASE_URL`（默认 `http://localhost:8080`）

Mock 插件只处理该 app 自己的模块。**新增 Mock 接口须放在 `apps/<app>/mock/` 目录，并在 `mock/vite-plugin-mock.ts` 的 `handleRequest` 中注册**。

## 自动注册

`packages/config-vite/src/index.ts` 的 `createMdmPlugins` 统一配置：

- `unplugin-auto-import`：`vue`、`vue-router`、`pinia`、`vue-i18n`
- `unplugin-vue-components`：Element Plus resolver + `src/components`、`src/common/components` 目录

生成的类型落在每个 app 自己的 `src/auto-imports.d.ts` 和 `src/components.d.ts`。

**不要**手动 import `ref`、`useRoute`、`ElButton`、`TpTable` 等——自动导入会处理。新增全局 API 时改 `packages/config-vite/src/index.ts`。

## 模块自动发现

每个 app 内部都用 `import.meta.glob` 扫描本 app 的模块：

- **路由**：`apps/<app>/src/router.ts` 用 `./modules/*/routes.ts`（排除 `_` 前缀）
- **国际化**：`apps/<app>/src/i18n.ts` 用同样的模式

模块目录约定（`apps/<app>/src/modules/<module-name>/`）：

```
api/          # axios 调后端
components/   # 模块私有组件
composables/  # 模块私有 composable
locales/      # 模块 i18n（zh-CN / en-US）
stores/       # 模块 pinia store（可选）
types/        # 模块 TS 类型
views/        # 页面（路由直接指向）
index.ts      # 公开面（仅在被其他模块引用时使用）
routes.ts     # 路由声明（自动发现）
```

模块名用 kebab-case，i18n key 用 camelCase（如 `model-design` → `modelDesign`）。

## 布局说明

**重要**：这是一个**嵌入式二级应用**，顶部菜单和左侧菜单由主系统（iframe/微前端）提供。

- ❌ **禁止**使用 `AppLayout`（含顶部栏+侧边栏）
- ✅ **必须**使用 `EmptyLayout` 或自定义简化布局
- 已创建 `apps/mdm-model/src/components/EmptyLayout.vue` 作为空白布局

新增模块的 `routes.ts` 中：
```ts
const EmptyLayout = () => import('@/components/EmptyLayout.vue');
// ❌ 不要 import('@mdm/core/layout/AppLayout.vue')
```

## 代码改动测试

涉及 CSS 或影响页面结构时，**必须**调用 chrome-devtools-mcp 在浏览器中对所有可交互功能点测一遍。

## 制定计划并涉及页面布局

需要使用文字将组件在页面中的摆放位置进行简单定义，示例如下：

整体结构：左右两栏水平并排。
  左侧区域：放置树形菜单组件。
  右侧区域：垂直分为上下两段。
    上段：放置操作工具栏（左侧放操作按钮，右侧放搜索框）。 
    下段：占据剩余空间，放置数据列表（表格或卡片网格）。
