---
description: Dm-* 组件设计规格 — ModelCard Figma规格、TpCardList规则、TpPagination设计规格、PageHeader规则、statusTagType映射。
paths:
  - "**/*.vue"
  - "**/*.ts"
---

## 1 ModelCard 设计规格

### 卡片容器

| 属性 | 值 |
|------|-----|
| border-radius | `4px`（非 rounded-lg） |
| border | `1px solid #dcdfe6` |
| background | `#ffffff` |
| cursor | `pointer` |
| 默认阴影 | `0px 1px 4px 0px rgba(0, 0, 0, 0.08)` |
| hover 阴影 | `0px 2px 8px 0px rgba(0, 0, 0, 0.12)` |
| 选中态 | `ring-2 ring-[var(--el-color-primary)]` |
| hover border-color | `var(--el-color-primary-light-3)` |
| 过渡 | `transition-all duration-200 ease-in-out` |

### 卡片头部

| 属性 | 值 |
|------|-----|
| 图标框尺寸 | 36x36px |
| 图标框 border-radius | `4px` |
| 图标框 background | `#337bff`（蓝色） |
| 图标尺寸 | 20px，白色 |
| 复选框位置 | `absolute top-4 right-4 z-10`（右上角） |
| 标题 font-size | 14px |
| 标题 font-weight | 500（medium） |
| 标题 color | `#3d4247` |
| 编码 font-size | 12px |
| 编码 color | `#666666` |
| 图标与文字间距 | `gap: 15px` |
| 头部 padding | `px-4 pt-4 pb-2` |

### 信息行

| 属性 | 值 |
|------|-----|
| font-size | 13px（非 12px） |
| color | `#666666` |
| line-height | 30px |
| 布局 | 标签与值拼接为整行文本 |

### 底部标签区

- `justify-between`
- 密级标签（可选）+ 版本/状态组合标签 + 授权按钮
- 版本左半：`border-radius: 3px 0 0 3px`，描边，`color: #333`
- 状态右半：`border-radius: 0 3px 3px 0`，`background: rgba(227,77,89,0.1)`，`color: #e34d59`

---

## 2 TpCardList 规则

- ✅ 必须显式 import：`import TpCardList from '@mdm/common/components/data/TpCardList.vue';`
- ✅ 节点带 `class="flex-1 min-h-0"`
- ✅ 数据驱动：`:data` 传入数组
- ✅ 作用域插槽 `#item="{ item, index }"`，不用 default slot
- ✅ 分页：v-model:pageNum / v-model:pageSize + :total + @page-change
- ✅ 空态自动推算，可选 `:empty-text`
- ✅ 卡片最小宽度默认 `280px`，可调 `:grid-min-width`
- ✅ grid 间距档位 2/3/4/6/8，默认 4
- ❌ 禁止内部用 `el-pagination` 替换 `TpPagination`

---

## 3 TpPagination 设计规格

| 属性 | 值 |
|------|-----|
| 容器高度 | 48px |
| 容器 padding | `4px 16px` |
| 容器 border-top | `1px solid #E1E9F0` |
| 统计区 font-size | 12px |
| 统计区 color | `#585F66` |
| 统计区 line-height | 24px |
| 翻页按钮尺寸 | 28x28px |
| 翻页按钮 border-radius | `2px`（非 EP 默认 4px） |
| 上一页/下一页 | `#F5F7FA` 背景、无边框、箭头 `#585F66` |
| 激活页码 | `1px solid #337BFF` 描边、`#337BFF` 文字、白色背景 |
| 普通页码 | `1px solid #E9E9E9` 描边、`#585F66` 文字 |
| 跳页输入框 | 56x28px，`border-radius: 2px`，`1px solid #E1E9F0` |
| 每页条数选择器 | 100x28px，`border-radius: 2px`，`1px solid #E1E9F0`，12px |

- ✅ 所有分页控件统一 `border-radius: 2px`（非 Element Plus 默认 4px）
- ❌ 禁止直接使用 `el-pagination`（必须通过 TpPagination 或 TpTable 间接使用）
- ❌ 禁止修改分页组件的圆角为 4px 或其他非 2px 值
- ❌ 禁止修改翻页按钮（上一页/下一页）的背景色为白色或主题色填充

---

## 4 TpPageHeader 组件规则

- ✅ 编辑页和详情页的页头**必须**使用 `<TpPageHeader>`（`@mdm/common/components/structure/TpPageHeader.vue`）
- ✅ `title` prop 必填
- ✅ `backTo` prop 指定返回路由（通常为列表页路由 name），不传则 `router.back()`
- ✅ 右侧操作按钮用 `#actions` 插槽
- ✅ 结构：左侧 ArrowLeft + 标题 + 右侧 `#actions`
- ❌ 禁止手写页头标题 div
- ❌ 列表页不使用 TpPageHeader

---

## 5 statusTagType 标准映射

```typescript
const statusTagType = (status: string) => {
  const map: Record<string, string> = {
    draft: 'info',
    published: 'success',
    archived: 'warning',
    enabled: 'success',
    disabled: 'info',
    error: 'danger',
  };
  return map[status] ?? 'info';
};
```
