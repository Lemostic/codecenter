---
name: mdm-frontend-rules
description: MDM 项目前端编码规范技能。当用户提到"MDM 编码规范"、"主数据前端"、"TpTable"、"TpPageFrame"、"TpLeftTreeLayout"、"ModelCard"、"useCrudList"、"@mdm"路径别名，或需要应用主数据产品的前端特有规范时触发。
---

# MDM 前端编码规范技能

本 skill 提供主数据产品（MDM）前端特有的编码规范，是对 `frontend-vue` 通用规范包的领域扩展。激活条件：项目 manifest 同时声明 `frontend-vue` profile 与 `mdm-frontend` domain。

## 核心定位

| 通用规范（L1） | MDM 特有（L2） |
|---------------|---------------|
| `frontend-vue` 占位符 `{EncapsulatedXxx}` | `Tp-` 前缀具体实现 |
| 命名铁律、API 5 件套、类型 5 件套、i18n 4 段式 | MDM BaseEntity（强制 `name`）、`pageNum`/`list` 分页字段 |
| p-3 间距、Element Plus 必填、按钮图标 | `TpPagination` 完整视觉规格、`ModelCard` 视觉规格 |
| `useCrudList` 等 Composable | （无）→ MDM 强制使用 `useCrudList` / `useImportExport` / `useFormValidation` |
| `@/` 单应用路径别名 | `@mdm/core`、`@mdm/common`、`@mdm/types` monorepo 别名 |

## 快速索引

| 任务场景 | 加载文件 |
|----------|----------|
| "技术栈/设计原则/script setup" | `references/getting-started.md` |
| "API/类型/i18n/错误处理" | `references/api-development.md` |
| "Tp-* 组件怎么用" | `references/components.md` |
| "TpPageFrame/TpLeftTreeLayout/TpPageHeader 骨架" | `references/pages.md` |
| "TpPagination/ModelCard 视觉规格" | `references/design-tokens.md` |
| "MDM 编码完成后自检" | `references/selfcheck.md` |

## 关键规范速查

### Tp-* 封装组件（11 类强制场景）

`TpTable` / `TpPagination` / `TpConfirm` / `TpMessage` / `TpEmpty` / `TpSectionTitle` / `TpPageFrame` / `TpPageHeader` / `TpTree` / `TpTreeLazy` / `TpLeftTreeLayout` / `TpCardList`

### MDM 路径别名

```
@mdm/core       → packages/core/src
@mdm/common     → packages/common/src
@mdm/types      → packages/types/src
~{app}          → apps/{app}/src
```

### MDM BaseEntity

```typescript
export interface BaseEntity {
  id: ID;
  name: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
  createdBy?: ID;
  updatedBy?: ID;
}
```

### MDM 业务 Composable

- `useCrudList<Entity>({ listApi, deleteApi?, defaultPageSize? })` — 列表页必用
- `useImportExport({ importApi, exportApi, taskQueryApi? })` — 导入导出必用
- `useFormValidation()` + `rules.required/maxLength/pattern` — 表单校验必用

### statusTagType 6 状态映射

```typescript
export const statusTagType = (status: string) => {
  const map = {
    draft: 'info', published: 'success', archived: 'warning',
    enabled: 'success', disabled: 'info', error: 'danger',
  };
  return map[status] ?? 'info';
};
```

### TpTable 关键规则

必须带 `class="flex-1 min-h-0"`（禁止 `h-full`），内置 `TpPagination`。

### TpPagination 关键规格

圆角 `2px`、容器 `48px` 高、上一/下一页 `#F5F7FA` 背景、激活页 `1px solid #337BFF` 描边。

### ModelCard 关键规格

容器 `4px` 圆角、图标框 `36×36px` 蓝色 `#337bff`、信息行 `13px/#666666`、底部版本/状态组合标签。

### 错误处理模式

```typescript
try {
  // 业务逻辑
} catch (error) {
  TpMessage.error('提示文案');
  console.error('[方法名]', error);
} finally {
  loading.value = false;
}
```

## 规范文档（6 个 references，无数字前缀）

| 文件 | 内容 |
|------|------|
| `references/getting-started.md` | 技术栈基线、pnpm monorepo、7 大设计原则 |
| `references/api-development.md` | `@mdm/core/http`、MDM BaseEntity、`pageNum/list` 分页 |
| `references/components.md` | Tp-* 11 类组件 API 详表 + Element Plus 必填 |
| `references/pages.md` | 页面骨架（列表/左树右表/flex 三段/卡片视图） |
| `references/design-tokens.md` | TpPagination + ModelCard 完整视觉规格 |
| `references/selfcheck.md` | MDM 特有检查项清单 |

## 使用方式

### 按需加载

```
用户：帮我实现一个左树右表页面
→ 加载 references/pages.md + references/components.md
```

### 特定规范

```
用户：TpPagination 的圆角为什么是 2px 不是 4px？
→ 加载 references/design-tokens.md
```

### 代码审查

```
用户：检查这段 MDM 代码是否符合规范
→ 加载 references/selfcheck.md + domain-spec.md 验收标准
```

### 完整规范

```
用户：/mdm-coding-rules
→ 加载全部 references + domain-spec.md
```

## 与 frontend-vue 的边界

- **复用**：命名、API 5 件套、类型 5 件套、i18n、Element Plus 必填、按钮图标、p-3 间距、flex 三段、状态标签三态——这些全部继承 `frontend-vue` 包，本 skill 不重复
- **扩展**：`Tp-` 具体实现、`@mdm/*` 别名、`useCrudList` 签名、`ModelCard` 规格、`pageNum/list` 分页字段
- **冲突时**：以 `frontend-vue` 为准（L1 优先级高于 L2 扩展）