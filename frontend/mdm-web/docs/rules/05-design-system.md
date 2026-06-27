---
description: 项目设计系统 — 颜色系统（EP 主题色 + 项目自定义色 + --mdm-bg-color）、字体系统（--mdm-font-size + EP 字体变量）、间距系统（p-3 硬约束）、圆角/阴影/Tailwind 类名速查。
paths:
  - "**/*.vue"
  - "**/*.css"
---

## 1 页面布局基础

### 背景色

> 背景色由 `@mdm/core/config` 的配置体系控制（CSS 变量 `--mdm-bg-color`），`applyConfig()` 会在应用启动时写入 `document.body.style.backgroundColor`。
> 当前使用 `StaticProvider`（框架固定值），项目实际背景色为 `#f0f2f5`。

```css
body { background-color: var(--mdm-bg-color, #f0f2f5); }
```

### 主内容区域

```css
.el-main {
  padding: 0px;
  overflow: hidden;  /* 配合子页面 flex 布局实现高度自适应 */
}
.el-container { height: 100%; }
```

### 侧边栏

```css
.el-aside {
  width: 248px;
  border-right: 1px solid #ccc;
  background-color: #fff;
}
```

---

## 2 颜色系统

### Element Plus 主题色（必须用 CSS 变量引用）

| 变量 | 色值 | 用途 |
|------|------|------|
| `--el-color-primary` | `#409eff` | 主色/链接色 |
| `--el-color-success` | `#67c23a` | 成功绿 |
| `--el-color-warning` | `#e6a23c` | 警告橙 |
| `--el-color-danger` | `#f56c6c` | 危险红/错误红 |
| `--el-color-info` | `#909399` | 信息灰 |

### 项目自定义色（用 Tailwind 任意值引用）

| 颜色名称 | 色值 | 用途 |
|---------|------|------|
| 页面背景 | `var(--mdm-bg-color, #f0f2f5)` | 全局页面背景（由配置体系控制） |
| 卡片表头背景 | `#f5f7fa` | el-card 头部、表格表头 |
| 侧边框线 | `#ccc` | aside、分隔线 |
| 标题文字 | `#303133` | 主要文字、标题 |
| 正文文字 | `#606266` | 正文内容 |
| 次要文字 | `#909399` | 辅助说明文字 |
| 页面标题 | `#1d1d1f` | 大号标题文字 |
| 深色背景 | `#253858` | Token 显示框背景 |
| 黑色字体#333 | `#333333` | 卡片/表格中主要标识文字 |
| 黑色字体#666 | `#666666` | 卡片/表格中次要元信息 |
| 选中浅蓝 | `rgba(64, 158, 255, 0.1)` | 树节点/菜单项选中态背景 |
| 图标蓝色背景 | `#337bff` | 卡片图标方形背景 / 分页激活页码 |
| 主题色浅背景 | `#F5F7FA` | 分页上一页/下一页按钮背景 |
| 控件描边色 | `#E1E9F0` | 分页容器顶边框 / 输入框描边 |
| 分页次要文本 | `#585F66` | 分页统计文字 / 普通页码文字 |
| 分页普通描边 | `#E9E9E9` | 非激活页码按钮描边 |

### 使用规则

- EP 变量列有值 → 必须用 CSS 变量（如 `var(--el-color-primary)`）
- EP 变量列为 `—` → 用 Tailwind 任意值（如 `bg-[#f0f2f5]`、`text-[#1d1d1f]`）
- ❌ 禁止使用 Tailwind 默认调色板（`bg-gray-*` / `text-blue-*` 等）
- ✅ `bg-white` 和 `bg-transparent` 是例外

### 文字颜色速查

```css
/* 标题 */    color: #303133;     /* 或 var(--el-text-color-primary) */
/* 正文 */    color: #606266;     /* 或 var(--el-text-color-regular) */
/* 辅助 */    color: #909399;     /* 或 var(--el-text-color-secondary) */
/* 错误 */    color: #f56c6c;     /* 或 var(--el-color-danger) */
/* 成功 */    color: #67c23a;     /* 或 var(--el-color-success) */
```

---

## 3 字体系统

> body 的 `font-size` 由 `@mdm/core/config` 的配置体系控制（CSS 变量 `--mdm-font-size`），默认 `12px`。
> 以下 Element Plus 变量和项目字号规范是在此基准上的相对值。

### 框架字体变量

| 变量 | 默认值 | 来源 |
|------|--------|------|
| `--mdm-font-size` | 12px | `applyConfig()` 写入，可被配置体系覆盖 |

### Element Plus 字体变量

| 变量 | 值 | 场景 |
|------|-----|------|
| `--el-font-size-extra-large` | 20px | — |
| `--el-font-size-large` | 18px | 大标题 |
| `--el-font-size-medium` | 16px | 页面标题/卡片标题 |
| `--el-font-size-base` | 14px | 正文（默认） |
| `--el-font-size-small` | 13px | 辅助说明 |
| `--el-font-size-extra-small` | 12px | 小标签/时间戳 |

### 项目字体使用规范

| 场景 | 字号 | 字重 | 颜色 |
|------|------|------|------|
| 页面大标题 | 16-18px | 600 (bold) | `#1d1d1f` |
| 卡片标题 | 16px | 600 | `#1d1d1f` |
| 正文标题 | 14px | 500-600 | `#303133` |
| 正文内容 | 14px | 400 | `#303133` |
| 辅助说明 | 13-14px | 400 | `#606266` |
| 次要文字 | 12-13px | 400 | `#909399` |
| 小标签 | 12px | 400 | `#666666` |
| 卡片标题文字 | 14px | 500 (medium) | `#3d4247` |
| 卡片编码 | 12px | 400 | `#666666` |
| 卡片元信息 | 13px | 400 | `#666666` |
| 版本标签 | 12px | 400 | `#333333` |

---

## 4 间距系统

### 常用间距

| 档位 | Tailwind | 像素值 | 用途 |
|------|----------|--------|------|
| xs | `gap-1` / `m-1` | 4px | 紧凑间距 |
| sm | `gap-2` / `m-2` | 8px | 小间距 |
| md | `gap-4` / `m-4` | 16px | 标准间距 |
| lg | `gap-6` / `m-6` | 24px | 大间距 |
| xl | `gap-8` / `m-8` | 32px | 更大间距 |

### 项目常用间距场景

| 场景 | 值 |
|------|-----|
| 页面级内边距 | `p-4`（16px） |
| 表单项间距 | `mb-4`（16px） |
| 按钮间距 | `gap-2`（8px） |
| 图标与文字（≤14px 字号） | `mr-2.5`（10px） |
| 图标与文字（≥16px 字号） | `mr-3`（12px） |
| 卡片网格间距 | `gap-4`（16px） |

### 栅格间距

常用 `gutter` 取值：**16 / 20 / 24 / 30**

---

## 5 圆角系统

| 圆角 | Tailwind / CSS | 用途 |
|------|----------------|------|
| 小圆角 | `rounded` / `rounded-sm` / 2-4px | 按钮、输入框、标签 |
| 中圆角 | `rounded-lg` / 8px | 卡片、对话框 |
| 大圆角 | `rounded-xl` / 12px | 大型容器 |
| 胶囊 | `rounded-[20px]` / 20px | 胶囊标签 |
| 全圆 | `rounded-full` / 50% | 头像、圆形图标 |

---

## 6 阴影系统

| 场景 | 值 |
|------|-----|
| 标准阴影 | `var(--el-box-shadow)` |
| 卡片默认阴影 | `0px 1px 4px 0px rgba(0, 0, 0, 0.08)` |
| 卡片 hover 阴影 | `0px 2px 8px 0px rgba(0, 0, 0, 0.12)` |
| `el-card` | `shadow="never"` |
| 折叠按钮阴影 | `0 2px 6px rgba(0, 0, 0, 0.08)` |
| 折叠按钮 hover | `0 2px 8px rgba(64, 158, 255, 0.2)` |

---

## 7 过渡与动画

```css
/* 标准过渡 */   transition: all 0.3s ease;
/* 快速过渡 */   transition: all 0.2s ease;
/* 弹性过渡 */   transition: all 0.3s cubic-bezier(.4, 0, 0.2, 1);
```

---

## 8 p-3 间距规则（硬约束）

> **核心规则**：列表页 / 详情页 / 编辑器 / 对话框 / 卡片 / 工具栏 / 内容区 wrapper / 树搜索框 —— **默认内边距统一 `p-3`（12px）**。
> 一句话：**默认 `p-3`；更紧用 `p-2`；要更松先想清楚是不是该用 `<TpSectionTitle>` 或换布局**。

### 间距档位

| 档位 | Tailwind | 像素值 | 用途 |
|------|----------|--------|------|
| **基础（默认）** | `p-3` | **12px** | 工具栏、内容区 wrapper、卡片内边距、对话框 body、详情页分组、树搜索框 |
| 紧凑 | `p-2` | 8px | 徽标、tag、小型 badge |
| 宽松（特例） | `p-4` | 16px | 详情页主容器、详情卡片、模型卡 header——**需评审** |

### gap 档位

| 档位 | Tailwind | 像素值 | 用途 |
|------|----------|--------|------|
| 按钮组 / 表单字段 | `gap-2` | 8px | 一行内并排的按钮、el-form-item 之间 |
| 通用垂直/水平 | `gap-3` | 12px | 卡片信息行、列表项、详情页字段行 |
| 顶层 section | `gap-4` | 16px | 详情页不同分组之间 |

### TpTable/TpCardList 外层容器特例

当 wrapper 内嵌 TpTable / TpCardList 且紧邻上方工具栏时，wrapper **只保留 `px-3`**（顶部间距由工具栏的 `p-3` 提供，底部由分页栏提供）。

### 禁止的反模式

| ❌ 反模式 | ✅ 正确写法 |
|----------|------------|
| `px-4 py-3` / `pr-3 pl-3` 分写 | `p-3` |
| `h-[60px] px-4` | `p-3`（去掉固定高度） |
| `p-4` 直接写工具栏 | `p-3` |
| 包 TpTable 的 wrapper 写 `p-3` | `px-3`（紧邻工具栏时） |
| 工具栏用 `mb-4` 分隔 | `p-3` wrapper + `border-b` |
| `p-[12px]` `m-[16px]` | `p-3` `m-4` |

### 工具栏 border 规则

- **仅一行工具栏** → 不加 `border-b`
- **多行工具栏叠放** → 行间加 `border-b`，但**最下面一行不加**

---

## 9 样式覆盖规则

- ✅ 覆盖 Element Plus 样式用 `:deep()` + `var()` 引用主题变量
- ❌ 禁止在 `:deep()` 中硬编码颜色

### 通用样式类名

```css
/* 主要标签文字 */
.main-label { font-weight: 600; color: var(--el-text-color-primary); }

/* 表格单行底边框（全局覆盖） */
el-table td { border-bottom: 1px solid #dcdfe6; }

/* 表格表头背景（全局覆盖） */
el-table th { background: #f5f7fa; }
```

---

## 10 Tailwind 常用类名速查

**布局**：`flex` `grid` `flex-col` `flex-wrap` `justify-between` `justify-center` `items-center` `items-start` `items-end`

**间距**：`p-4` `px-4` `py-4` `m-4` `mx-4` `my-4` `gap-4` `space-x-4` `space-y-4`

**尺寸**：`w-full` `w-1/2` `h-full` `h-screen` `min-h-0`

**文字**：`text-sm` `text-base` `text-lg` `text-xl` `font-medium` `font-semibold` `font-bold` `truncate` `line-clamp-2`

**背景**：`bg-white` `bg-transparent` `bg-[var(--mdm-bg-color)]` `bg-[#f5f7fa]` `bg-[#f0f2f5]`

**边框**：`border` `border-0` `border-2` `border-[#e4e7ed]` `border-[#ccc]` `rounded` `rounded-lg` `rounded-full`

**定位**：`relative` `absolute` `fixed` `top-0` `right-0` `bottom-0` `left-0` `inset-0` `z-10` `z-50`

**其他**：`overflow-hidden` `overflow-auto` `cursor-pointer` `opacity-50` `whitespace-nowrap`
