description: TpPagination 完整视觉规格、ModelCard 模型卡片完整视觉规格、MDM 项目自定义色与字体、p-3 间距硬约束。
---

# 设计 Token（TpPagination + ModelCard）

> Element Plus 主题色、字体变量、p-3 间距硬约束、圆角系统、阴影系统继承 `profiles/frontend/vue/vue3/design-tokens.md`。本文件仅给出 MDM 特有 `TpPagination` 与 `ModelCard` 的完整视觉规格。

## 1 TpPagination 视觉规格

| 属性 | 值 |
|------|-----|
| 容器高度 | 48px |
| 容器 padding | `4px 16px` |
| 容器 border-top | `1px solid #E1E9F0` |
| 统计区 font-size | 12px |
| 统计区 color | `#585F66` |
| 统计区 line-height | 24px |
| 翻页按钮尺寸 | 28×28px |
| 翻页按钮 border-radius | `2px`（**非 EP 默认 4px**） |
| 上一页/下一页 | `#F5F7FA` 背景、无边框、箭头 `#585F66` |
| 激活页码 | `1px solid #337BFF` 描边、`#337BFF` 文字、白色背景 |
| 普通页码 | `1px solid #E9E9E9` 描边、`#585F66` 文字 |
| 跳页输入框 | 56×28px，`border-radius: 2px`，`1px solid #E1E9F0` |
| 每页条数选择器 | 100×28px，`border-radius: 2px`，`1px solid #E1E9F0`，12px 文字 |

### 规则

| 规则 | 说明 |
|------|------|
| **MDF-FE-DT-001** | MUST NOT 直接使用 `el-pagination`，必须通过 `TpTable` 或 `TpPagination` 间接使用。 |
| **MDF-FE-DT-002** | MUST NOT 修改圆角为 4px（破坏设计规范）。 |
| **MDF-FE-DT-003** | MUST NOT 修改上一/下一页背景色（破坏设计规范）。 |
| **MDF-FE-DT-004** | MUST NOT 修改激活页码描边色或文字色。 |

## 2 ModelCard 视觉规格

### 2.1 容器

| 属性 | 值 |
|------|-----|
| border-radius | `4px`（非 `rounded-lg`） |
| border | `1px solid #dcdfe6` |
| background | `#ffffff` |
| cursor | `pointer` |
| 默认阴影 | `0px 1px 4px 0px rgba(0, 0, 0, 0.08)` |
| hover 阴影 | `0px 2px 8px 0px rgba(0, 0, 0, 0.12)` |
| 选中态 | `ring-2 ring-[var(--el-color-primary)]` |
| hover border-color | `var(--el-color-primary-light-3)` |
| 过渡 | `transition-all duration-200 ease-in-out` |

### 2.2 头部

| 属性 | 值 |
|------|-----|
| 图标框尺寸 | 36×36px |
| 图标框 border-radius | `4px` |
| 图标框 background | `#337bff` |
| 图标尺寸 | 20px，白色 |
| 复选框位置 | `absolute top-4 right-4 z-10` |
| 标题 font-size | 14px |
| 标题 font-weight | 500 |
| 标题 color | `#3d4247` |
| 编码 font-size | 12px |
| 编码 color | `#666666` |
| 图标与文字间距 | `gap: 15px` |
| 头部 padding | `px-4 pt-4 pb-2` |

### 2.3 信息行

| 属性 | 值 |
|------|-----|
| font-size | 13px（非 12px） |
| color | `#666666` |
| line-height | 30px |
| 布局 | 标签与值拼接为整行文本 |

```vue
<div class="text-[13px] text-[#666666] leading-[30px]">
  <span>数据域：{{ model.domainName }}</span>
  <span>字段数：{{ model.fieldCount }}</span>
</div>
```

### 2.4 底部标签区

| 项 | 规格 |
|----|------|
| 布局 | `justify-between` |
| 组合 | 密级标签（可选）+ 版本/状态组合标签 + 授权按钮 |
| 版本左半 | `border-radius: 3px 0 0 3px`、描边、`color: #333` |
| 状态右半 | `border-radius: 0 3px 3px 0`、`background: rgba(227,77,89,0.1)`、`color: #e34d59` |

```vue
<div class="flex items-center justify-between px-4 pb-4">
  <div class="flex items-center gap-1 text-[12px]">
    <span class="border border-[#dcdfe6] text-[#333] rounded-l-[3px] px-2">v{{ model.version }}</span>
    <span class="bg-[rgba(227,77,89,0.1)] text-[#e34d59] rounded-r-[3px] px-2">
      {{ statusText[model.status] }}
    </span>
  </div>
  <el-button link type="primary" @click="handleAuthorize(model)">授权</el-button>
</div>
```

### 2.5 规则

| 规则 | 说明 |
|------|------|
| **MDF-FE-DT-005** | ModelCard MUST 显式 import：`import ModelCard from '@mdm/common/components/data/ModelCard.vue'`。 |
| **MDF-FE-DT-006** | 容器 MUST 4px 圆角，禁止 `rounded-lg`。 |
| **MDF-FE-DT-007** | 图标框 MUST 36×36px 蓝色背景，禁止使用 emerald-500。 |
| **MDF-FE-DT-008** | 信息行 MUST 13px + `#666666`，禁止 12px。 |
| **MDF-FE-DT-009** | 底部版本/状态组合标签 MUST 用 3px 圆角（左半 border-radius 3px 0 0 3px，右半 0 3px 3px 0）。 |

## 3 MDM 项目自定义色（继承通用规范）

| 颜色名称 | 典型色值 | 用途 |
|---------|---------|------|
| 页面背景 | `#f0f2f5` | 全局页面背景 |
| 卡片表头背景 | `#f5f7fa` | el-card 头部、表格表头 |
| 侧边框线 | `#ccc` | aside、分隔线 |
| 标题文字 | `#303133` | 主要文字、标题 |
| 正文文字 | `#606266` | 正文内容 |
| 次要文字 | `#909399` | 辅助说明文字 |
| 页面标题 | `#1d1d1f` | 大号标题文字 |
| 选中浅蓝 | `rgba(64, 158, 255, 0.1)` | 树节点/菜单项选中态背景 |
| 图标蓝色背景 | `#337bff` | 卡片图标方形背景 / 分页激活页码 |

## 4 间距系统（p-3 硬约束，继承通用规范）

| 档位 | Tailwind | 像素 | 用途 |
|------|----------|------|------|
| 基础（默认） | `p-3` | 12px | 工具栏、内容区 wrapper、卡片内边距、对话框 body |
| 紧凑 | `p-2` | 8px | 徽标、tag、小型 badge |
| 宽松（特例） | `p-4` | 16px | 详情页主容器——需评审 |

禁止的反模式：`px-4 py-3` / `p-[12px]` / `m-[16px]` / `h-[60px] px-4` 等硬编码数值。

## 5 字体系统（继承通用规范）

| 场景 | 字号 | 字重 | 颜色 |
|------|------|------|------|
| 页面大标题 | 16-18px | 600 | `#1d1d1f` |
| 卡片标题 | 16px | 600 | `#1d1d1f` |
| 正文标题 | 14px | 500-600 | `#303133` |
| 正文内容 | 14px | 400 | `#303133` |
| 辅助说明 | 13-14px | 400 | `#606266` |
| 次要文字 | 12-13px | 400 | `#909399` |