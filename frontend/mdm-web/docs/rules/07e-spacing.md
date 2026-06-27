---
description: p-3 间距规则与 TpPagination 设计规格 — 默认内边距/间距档位/典型用法/迁移表/工具栏border/TpPagination尺寸规格。
paths:
  - "**/*.vue"
---

## 1. p-3 间距规则完整版

来源: §11.21

> **核心规则**：列表页 / 详情页 / 编辑器 / 对话框 / 卡片 / 工具栏 / 内容区 wrapper / 树搜索框 —— **默认内边距统一使用 `p-3`（12px）**。本规则优先级最高——除非下表"宽松间距"明确允许，否则一律 `p-3`。

### 间距档位（padding）

| 档位 | Tailwind | 像素值 | 用途 |
|------|----------|--------|------|
| **基础（默认）** | `p-3` | **12px** | 工具栏、内容区 wrapper、卡片内边距、对话框 body、详情页分组、树搜索框 |
| 紧凑 | `p-2` | 8px | 徽标、tag、小型 badge、搜索框 prefix 间距 |
| 宽松（特例） | `p-4` | 16px | 详情页主容器、详情卡片、模型卡 header/padding 区域——**需评审通过** |

### gap 档位

| 档位 | Tailwind | 像素值 | 用途 |
|------|----------|--------|------|
| 按钮组 / 表单字段 | `gap-2` | 8px | 一行内并排的按钮、el-form-item 之间 |
| 通用垂直/水平 | `gap-3` | 12px | 卡片信息行、列表项之间、详情页字段行 |
| 顶层 section | `gap-4` | 16px | 详情页不同分组之间、TpPageFrame 内部顶层 section |

### TpTable/TpCardList 外层容器特例

当 wrapper 内嵌 `TpTable` / `TpCardList`（自带分页栏），且直接紧邻上方工具栏（已有 `p-3`）时，wrapper **只保留左右间距 `px-3`，顶部和底部间距均为 0**。顶部间距已由工具栏的 `p-3` 提供，底部间距由表格/卡片自带的分页栏提供。

### 禁止的反模式

| ❌ 反模式 | ✅ 正确写法 | 原因 |
|----------|------------|------|
| `px-4 py-3` / `pr-3 pl-3` 分写 | `p-3` | 避免双方向间距不一致 |
| `h-[60px] px-4` 与 padding 双重控制 | `p-3`（去掉 `h-[60px]`） | 高度由内容 + padding 决定 |
| `p-4` 直接写工具栏 | `p-3` | 工具栏属于"基础间距"档位 |
| 包 TpTable/TpCardList 的 wrapper 写 `p-3` | `px-3`（紧邻工具栏时） | 顶部间距由工具栏提供 |
| 工具栏用 `mb-4` 与下方表格分隔 | `p-3` wrapper + `border-b` | 边框 + 统一 padding 视觉更整齐 |
| `p-[12px]` `m-[16px]` 硬编码数值 | `p-3` `m-4` | 视图中禁止硬编码间距 |

### 工具栏 border 规则

- **仅一行工具栏** → 不加 `border-b`。工具栏 `p-3` 底部 12px 间距已提供足够的呼吸空间。
- **多行工具栏叠放** → 行与行之间加 `border-b` 分隔，但**最下面一行（紧邻内容区的行）不加**。

---

## 2. p-3 典型用法速查

来源: §11.21

### 2a. 工具栏

仅一行时不加 border-b。

```vue
<!-- 工具栏：左 = 操作按钮，右 = 筛选/搜索（仅一行时不加 border-b） -->
<div class="flex items-center justify-between p-3 flex-shrink-0">
  <div class="flex items-center gap-2">
    <el-button>新增</el-button>
    <el-button>编辑</el-button>
  </div>
  <div class="flex items-center gap-2">
    <el-input placeholder="搜索" />
  </div>
</div>
```

### 2b. 内容区 wrapper

包住 TpTable / TpCardList，顶部间距由上方工具栏提供，底部由分页栏提供，wrapper 仅需左右间距。

```vue
<!-- 内容区 wrapper：包住 TpTable / TpCardList -->
<div class="flex-1 min-h-0 flex flex-col px-3">
  <TpTable ... />
</div>
```

### 2c. 详情页分组

```vue
<!-- 详情页分组：标题 + 内容 -->
<div class="flex flex-col gap-3 p-3">
  <TpSectionTitle title="基本信息" />
  <el-form ... />
</div>
```

### 2d. 树搜索框

```vue
<!-- 树搜索框（dm-tree-search） -->
<div class="dm-tree-search p-3 flex-shrink-0">
  <el-input ... />
</div>
```

---

## 3. p-3 旧代码迁移对照表

来源: §11.21

| 旧 class | 新 class | 出现位置 |
|----------|----------|----------|
| `px-4 py-3` | `p-3` | 工具栏（ModelList / CategoryList / DataItemList） |
| `pr-3 pl-3` | `p-3` | 内容区 wrapper（ModelList / CategoryList / DataItemList） |
| `h-[60px] px-4` | `p-3`（去掉 `h-[60px]`） | 工具栏首行（ModelList 第 1 行） |
| `pt-2` / `pt-4`（仅顶部留白时） | `p-3`（统一为四方向） | 工具栏与 TpTable 之间的局部间距 |

> 迁移原则：迁移后所有列表页工具栏 / 内容区 / 树搜索框的 class 完全一致（`p-3 flex-shrink-0 border-b border-[#e9e9e9]` 等），便于后续维护与样式统一。

---

## 4. 工具栏 border 规则示例

来源: §11.21

仅一行工具栏不加 `border-b`；多行工具栏叠放时行与行之间加 `border-b`，但最下面一行（紧邻内容区）不加。

```vue
<!-- ✅ 仅一行工具栏：不加 border-b -->
<div class="flex items-center justify-between p-3 flex-shrink-0">
  <div class="flex items-center gap-2">
    <el-button>新增</el-button>
    <el-button>编辑</el-button>
  </div>
  <div class="flex items-center gap-2">
    <el-input placeholder="搜索" />
  </div>
</div>

<!-- ✅ 多行工具栏：行间用 border-b 分隔，最后一行不加 -->
<!-- 第1行：筛选/排序 -->
<div class="flex items-center justify-between p-3 flex-shrink-0 border-b border-[#e9e9e9]">
  <!-- 排序/筛选控件 -->
</div>
<!-- 第2行：操作按钮（紧邻内容区，不加 border-b） -->
<div class="flex items-center justify-between p-3 flex-shrink-0">
  <!-- 操作按钮 -->
</div>
```

---

## 5. TpPagination 设计规格

来源: §11.20

| 属性 | 值 |
|------|-----|
| 容器高度 | 48px |
| 容器 padding | `4px 16px` |
| 容器 border-top | `1px solid #E1E9F0` |
| 统计区 font-size | 12px |
| 统计区 color | `#585F66` |
| 统计区 line-height | 24px |
| 翻页按钮尺寸 | 28x28px |
| 翻页按钮 border-radius | `2px`（非 Element Plus 默认 4px） |
| 上一页/下一页背景 | `#F5F7FA`，无边框，箭头 `#585F66` |
| 激活页码 | `1px solid #337BFF` 描边，`#337BFF` 文字，白色背景 |
| 普通页码 | `1px solid #E9E9E9` 描边，`#585F66` 文字 |
| 跳页输入框 | 56x28px，`border-radius: 2px`，`1px solid #E1E9F0` |
| 每页条数选择器 | 100x28px，`border-radius: 2px`，`1px solid #E1E9F0`，12px 文字 |
