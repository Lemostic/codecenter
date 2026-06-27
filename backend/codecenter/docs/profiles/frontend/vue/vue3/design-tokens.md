| 字段 | 值 |
|------|-----|
| 版本 | 2.0 |
| 层级 | L1 |
| 包类型 | frontend-module |
| 引入条件 | `fingerprint.profiles contains 'frontend-vue'` |
| 所属前端包 | `frontend-vue/vue3/` |
| 适用版本 | Vue 3.5+ |
| 依赖规范 | `vue3/component.md`、`vue3/ui-element-plus.md` |

# 设计 Token 规范

> 本文件定义颜色、字体、间距、圆角、阴影等设计 Token，以及 p-3 间距硬约束。
> 本文件规则仅适用于 Vue 3 + Tailwind CSS + Element Plus。具体色值/数值由项目配置覆盖。

---

## 1 颜色系统

### 1.1 Element Plus 主题色（必须用 CSS 变量引用）

**PROF-FE-701** Element Plus 主题色 MUST 用 CSS 变量引用。 [MUST]

| 变量 | 默认值 | 用途 |
|------|--------|------|
| `--el-color-primary` | `#409eff` | 主色/链接色 |
| `--el-color-success` | `#67c23a` | 成功绿 |
| `--el-color-warning` | `#e6a23c` | 警告橙 |
| `--el-color-danger` | `#f56c6c` | 危险红/错误红 |
| `--el-color-info` | `#909399` | 信息灰 |

### 1.2 项目自定义色

**PROF-FE-702** 项目自定义色由项目配置定义（`project_config/design_tokens.md`），使用 Tailwind 任意值引用。 [MUST]

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

### 1.3 使用规则

| 规则 | 说明 |
|------|------|
| **PROF-FE-703** | Element Plus 主题色 MUST 用 CSS 变量引用（如 `var(--el-color-primary)`）。[MUST] |
| **PROF-FE-704** | 项目自定义色 MUST 用 Tailwind 任意值（如 `bg-[#f0f2f5]`、`text-[#1d1d1f]`）。[MUST] |
| **PROF-FE-705** | 禁止使用 Tailwind 默认调色板（`bg-gray-*` / `text-blue-*` 等）。[MUST] |
| **PROF-FE-706** | `bg-white` 和 `bg-transparent` 是例外，允许使用。 [SHOULD] |

### 1.4 文字颜色速查

```css
/* 标题 */    color: #303133;     /* 或 var(--el-text-color-primary) */
/* 正文 */    color: #606266;     /* 或 var(--el-text-color-regular) */
/* 辅助 */    color: #909399;     /* 或 var(--el-text-color-secondary) */
/* 错误 */    color: #f56c6c;     /* 或 var(--el-color-danger) */
/* 成功 */    color: #67c23a;     /* 或 var(--el-color-success) */
```

---

## 2 字体系统

### 2.1 Element Plus 字体变量

| 变量 | 默认值 | 场景 |
|------|--------|------|
| `--el-font-size-extra-large` | `20px` | — |
| `--el-font-size-large` | `18px` | 大标题 |
| `--el-font-size-medium` | `16px` | 页面标题/卡片标题 |
| `--el-font-size-base` | `14px` | 正文（默认） |
| `--el-font-size-small` | `13px` | 辅助说明 |
| `--el-font-size-extra-small` | `12px` | 小标签/时间戳 |

### 2.2 字号使用规范

| 场景 | 字号 | 字重 | 颜色 |
|------|------|------|------|
| 页面大标题 | 16-18px | 600 (bold) | `#1d1d1f` |
| 卡片标题 | 16px | 600 | `#1d1d1f` |
| 正文标题 | 14px | 500-600 | `#303133` |
| 正文内容 | 14px | 400 | `#303133` |
| 辅助说明 | 13-14px | 400 | `#606266` |
| 次要文字 | 12-13px | 400 | `#909399` |
| 小标签 | 12px | 400 | `#666666` |

---

## 3 间距系统

### 3.1 p-3 间距硬约束（核心规则）

**PROF-FE-707** **核心规则**：列表页 / 详情页 / 编辑器 / 对话框 / 卡片 / 工具栏 / 内容区 wrapper / 树搜索框 —— **默认内边距统一 `p-3`（12px）**。 [MUST]

> 一句话：**默认 `p-3`；更紧用 `p-2`；要更松先想清楚是不是该用 `<{EncapsulatedSectionTitle}>` 或换布局**。

### 3.2 间距档位（padding）

| 档位 | Tailwind | 像素值 | 用途 |
|------|----------|--------|------|
| **基础（默认）** | `p-3` | **12px** | 工具栏、内容区 wrapper、卡片内边距、对话框 body、详情页分组、树搜索框 |
| 紧凑 | `p-2` | 8px | 徽标、tag、小型 badge |
| 宽松（特例） | `p-4` | 16px | 详情页主容器、详情卡片、模型卡 header——**需评审通过** |

### 3.3 gap 档位

| 档位 | Tailwind | 像素值 | 用途 |
|------|----------|--------|------|
| 按钮组 / 表单字段 | `gap-2` | 8px | 一行内并排的按钮、el-form-item 之间 |
| 通用垂直/水平 | `gap-3` | 12px | 卡片信息行、列表项、详情页字段行 |
| 顶层 section | `gap-4` | 16px | 详情页不同分组之间 |

### 3.4 DmTable/DmCardList 外层容器特例

当 wrapper 内嵌 `{EncapsulatedTable}` / `{EncapsulatedCardList}` 且紧邻上方工具栏时，wrapper **只保留 `px-3`**（顶部间距由工具栏的 `p-3` 提供，底部由分页栏提供）。

### 3.5 禁止的反模式

| ❌ 反模式 | ✅ 正确写法 |
|----------|------------|
| `px-4 py-3` / `pr-3 pl-3` 分写 | `p-3` |
| `h-[60px] px-4` | `p-3`（去掉固定高度） |
| `p-4` 直接写工具栏 | `p-3` |
| 包 DmTable 的 wrapper 写 `p-3` | `px-3`（紧邻工具栏时） |
| 工具栏用 `mb-4` 分隔 | `p-3` wrapper + `border-b` |
| `p-[12px]` `m-[16px]` 硬编码数值 | `p-3` `m-4` |

### 3.6 工具栏 border 规则

**PROF-FE-708** 工具栏 border 规则： [MUST]

- **仅一行工具栏** → 不加 `border-b`
- **多行工具栏叠放** → 行间加 `border-b`，但**最下面一行不加**

详见 `vue3/page-patterns.md §7`。

---

## 4 圆角系统

**PROF-FE-709** 圆角 MUST 引用设计 Token，禁止硬编码。 [MUST]

| 圆角 | Tailwind / CSS | 用途 |
|------|----------------|------|
| 小圆角 | `rounded` / `rounded-sm` / 2-4px | 按钮、输入框、标签 |
| 中圆角 | `rounded-lg` / 8px | 卡片、对话框 |
| 大圆角 | `rounded-xl` / 12px | 大型容器 |
| 胶囊 | `rounded-[20px]` / 20px | 胶囊标签 |
| 全圆 | `rounded-full` / 50% | 头像、圆形图标 |

---

## 5 阴影系统

| 场景 | 典型值 |
|------|--------|
| 标准阴影 | `var(--el-box-shadow)` |
| 卡片默认阴影 | `0px 1px 4px 0px rgba(0, 0, 0, 0.08)` |
| 卡片 hover 阴影 | `0px 2px 8px 0px rgba(0, 0, 0, 0.12)` |
| `el-card` | `shadow="never"` |
| 折叠按钮阴影 | `0 2px 6px rgba(0, 0, 0, 0.08)` |
| 折叠按钮 hover | `0 2px 8px rgba(64, 158, 255, 0.2)` |

---

## 6 过渡与动画

```css
/* 标准过渡 */   transition: all 0.3s ease;
/* 快速过渡 */   transition: all 0.2s ease;
/* 弹性过渡 */   transition: all 0.3s cubic-bezier(.4, 0, 0.2, 1);
```

---

## 7 样式覆盖规则

**PROF-FE-710** 覆盖 Element Plus 样式 MUST 用 `:deep()` + `var()` 引用主题变量。 [MUST]

```css
/* ✅ 正确：使用 :deep() + var() */
:deep(.el-button--primary) {
  background-color: var(--el-color-primary);
  border-color: var(--el-color-primary);
}

/* ❌ 错误：在 :deep() 中硬编码颜色 */
:deep(.el-button--primary) {
  background-color: #409eff;  /* ❌ */
}
```

**PROF-FE-711** 禁止在 `<style scoped>` 中硬编码颜色，应搬到 template 用 Tailwind。 [MUST]

---

## 8 Tailwind 常用类名速查

**布局**：`flex` `grid` `flex-col` `flex-wrap` `justify-between` `justify-center` `items-center` `items-start` `items-end`

**间距**：`p-3` `p-4` `px-3` `py-3` `m-3` `m-4` `mx-4` `my-4` `gap-2` `gap-3` `gap-4` `space-x-4` `space-y-4`

**尺寸**：`w-full` `w-1/2` `h-full` `h-screen` `min-h-0` `flex-1`

**文字**：`text-xs` `text-sm` `text-base` `text-lg` `text-xl` `font-medium` `font-semibold` `font-bold` `truncate` `line-clamp-2`

**背景**：`bg-white` `bg-transparent` `bg-[#f5f7fa]` `bg-[#f0f2f5]`

**边框**：`border` `border-0` `border-2` `border-[#e4e7ed]` `border-[#ccc]` `rounded` `rounded-lg` `rounded-full`

**定位**：`relative` `absolute` `fixed` `top-0` `right-0` `bottom-0` `left-0` `inset-0` `z-10` `z-50`

**其他**：`overflow-hidden` `overflow-auto` `cursor-pointer` `opacity-50` `whitespace-nowrap`

---

## 9 禁止行为清单

- ❌ 禁止在视图中硬编码颜色（必须用 CSS 变量或 Tailwind）
- ❌ 禁止在视图里硬编码间距数值（必须用 Tailwind 原子类）
- ❌ 禁止使用 Tailwind 默认调色板（`bg-gray-*` / `text-blue-*` / `bg-red-500`）
- ❌ 禁止在 `:deep()` 中硬编码颜色
- ❌ 禁止在 `<style scoped>` 中硬编码颜色
- ❌ 禁止用 `p-[12px]` `m-[16px]` 硬编码数值
- ❌ 禁止在 EP 主题色场景用 Tailwind 调色板（如 `text-blue-500` 替代 `var(--el-color-primary)`）

---

*本文件规则仅适用于 Vue 3 + Tailwind CSS。具体色值/数值由项目 `project_config/design_tokens.md` 覆盖。*
