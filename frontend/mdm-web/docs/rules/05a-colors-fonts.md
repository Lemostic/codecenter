---
description: 设计系统（上）— 页面布局基础、颜色系统（EP主题色 + 项目自定义色 + --mdm-bg-color）、字体系统（--mdm-font-size + EP字体变量 + 项目字号规范）。
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
