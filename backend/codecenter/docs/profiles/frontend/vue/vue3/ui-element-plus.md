| 字段 | 值 |
|------|-----|
| 版本 | 2.0 |
| 层级 | L1 |
| 包类型 | frontend-module |
| 引入条件 | `fingerprint.profiles contains 'frontend-vue'` + 使用 Element Plus |
| 所属前端包 | `frontend-vue/vue3/` |
| 适用版本 | Vue 3.5+ / Element Plus 2.13+ |
| 依赖规范 | `vue3/script-setup.md` |

# Element Plus 组件规范

> 本文件定义 Element Plus 组件的必填属性、弹窗/表单/表格/按钮/卡片/树/描述/选择器/日期选择器规则。
> 本文件规则仅适用于 Vue 3 + Element Plus 2.13+。

---

## 1 必填属性原则

Element Plus 组件存在大量"不报错但行为不一致"的隐性属性。以下原则确保跨项目的一致性。

### 1.1 尺寸属性

**PROF-FE-901** 所有表单类组件 MUST 显式声明 `size`（推荐 `size="default"`），避免继承上下文导致尺寸不一致。 [MUST]

| 组件 | 必填属性 |
|------|---------|
| `el-input` | `size` |
| `el-input-number` | `size` |
| `el-select` | `size` |
| `el-button` | `size` |
| `el-tag` | `size` |
| `el-text` | `size` |

### 1.2 表单必填

| 组件 | 必填属性 | 规则 |
|------|---------|------|
| `el-form` | `size`、`label-width` | 必须显式声明 label 宽度（如 80px/100px/120px） |
| `el-form-item` | `label`、`prop` | `label` 必填；`prop` 在有校验时必填 |

### 1.3 表格必填

| 组件 | 必填属性 | 规则 |
|------|---------|------|
| `el-table` | `data`、`v-loading` | 必须绑定数据和加载状态 |
| `el-table` | `#empty` 槽位 | 表格无数据时必须展示空状态组件 |

### 1.4 弹窗必填

| 组件 | 必填属性 | 规则 |
|------|---------|------|
| `el-dialog` | `v-model`、`title`、`width` | 必须 `append-to-body`、`:close-on-click-modal="false"` |

### 1.5 其他组件必填

| 组件 | 必填属性 | 规则 |
|------|---------|------|
| `el-text` | `size` | `size="default"` |
| `el-tree` | `node-key`、`data` | `:data="treeData"`，`node-key="id"` |
| `el-dropdown` | `@command` | `@command="handleCommand"` 必须显式 |
| `el-tooltip` | `content` | `:content="提示文案"` |
| `el-page-header` | `@back` | `@back="goBack"` 或路由回退 |

### 1.6 受控模式

**PROF-FE-902** 以下组件 MUST 使用受控模式（`v-model`）。 [MUST]

`el-checkbox`、`el-radio`、`el-switch`、`el-tabs`、`el-date-picker`、`el-rate`

---

## 2 弹窗规则

**PROF-FE-903** `el-dialog` 强制配置： [MUST]

- ✅ 必须 `append-to-body`（防 overflow 裁剪）
- ✅ 必须 `:close-on-click-modal="false"`（防误关）
- ✅ 长内容必须 `<el-scrollbar max-height="60vh">`
- ✅ footer 按钮顺序：取消 → 确定
- ✅ 保存按钮必须 `:loading="saving"`

**弹窗宽度选择**：

| 复杂度 | 字段数 | 宽度 |
|--------|--------|------|
| 简单 | 1-3 个字段 | `width="500px"` |
| 中等 | 4-8 个字段 | `width="800px"` |
| 复杂 | > 8 个字段 | `width="1000px"` + el-scrollbar |

```vue
<template>
  <el-dialog
    v-model="visible"
    title="编辑用户"
    width="800px"
    append-to-body
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-scrollbar max-height="60vh">
      <el-form :model="formData" label-width="100px" size="default">
        <el-form-item label="用户名" prop="username" required>
          <el-input v-model="formData.username" clearable />
        </el-form-item>
        <!-- ... -->
      </el-form>
    </el-scrollbar>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
    </template>
  </el-dialog>
</template>
```

---

## 3 表单规则

**PROF-FE-904** 表单强制配置： [MUST]

- ✅ 必填字段 MUST `required` 红星
- ✅ `prop` MUST 对应 `formData` 的字段
- ✅ 输入框 MUST `clearable`（除密码等敏感字段）
- ✅ 长文本 MUST `type="textarea" :rows="3"`
- ✅ 有限字符数 MUST `maxlength` + `show-word-limit`
- ✅ 搜索框加 `:suffix-icon="Search"` + `@keyup.enter`
- ✅ 选择器长列表 MUST `filterable`、MUST `clearable`
- ✅ 日期选择器 MUST 显式 `format` 和 `value-format`

```vue
<template>
  <el-form :model="formData" label-width="100px" size="default">
    <el-form-item label="用户名" prop="username" required
      :rules="[{ required: true, message: '请输入用户名', trigger: 'blur' }]">
      <el-input
        v-model="formData.username"
        placeholder="请输入用户名"
        clearable
        maxlength="50"
        show-word-limit
      />
    </el-form-item>
    <el-form-item label="描述" prop="description">
      <el-input
        v-model="formData.description"
        type="textarea"
        :rows="3"
        placeholder="请输入描述"
        maxlength="200"
        show-word-limit
      />
    </el-form-item>
  </el-form>
</template>
```

---

## 4 表格规则

**PROF-FE-905** `el-table` 强制配置： [MUST]

- ✅ 长字段 MUST `show-overflow-tooltip`
- ✅ 状态列用 `el-tag` + `:type` 映射（success/warning/info/danger）
- ✅ 操作列 MUST `link` 类型 + `fixed="right"`
- ✅ 必写 `#empty` 槽位
- ✅ 列宽用 `min-width`（非 `width`）

```vue
<template>
  <el-table
    :data="tableData"
    v-loading="loading"
    border
    stripe
    height="100%"
  >
    <el-table-column prop="name" label="名称" min-width="120" show-overflow-tooltip />
    <el-table-column prop="code" label="编码" min-width="120" show-overflow-tooltip />
    <el-table-column prop="status" label="状态" min-width="100">
      <template #default="{ row }">
        <el-tag :type="statusTagType(row.status)" size="default">
          {{ row.status }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="操作" min-width="180" fixed="right">
      <template #default="{ row }">
        <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
        <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
      </template>
    </el-table-column>
    <template #empty>
      <{EncapsulatedEmpty} description="暂无数据" />
    </template>
  </el-table>
</template>
```

---

## 5 描述列表规则

**PROF-FE-906** `el-descriptions` 强制配置： [MUST]

- ✅ `:column` 设为 `2` 或 `3`（避免单列过长）
- ✅ 状态字段用 `el-tag`
- ❌ `el-descriptions` 在 EP 2.13 中无 `size` 属性（如看到示例有 `size="default"` 是冗余的，删掉）

```vue
<template>
  <el-descriptions :column="2" border>
    <el-descriptions-item label="用户名">{{ user.username }}</el-descriptions-item>
    <el-descriptions-item label="状态">
      <el-tag :type="statusTagType(user.status)" size="default">
        {{ user.status }}
      </el-tag>
    </el-descriptions-item>
    <el-descriptions-item label="邮箱">{{ user.email }}</el-descriptions-item>
    <el-descriptions-item label="创建时间">{{ formatDate(user.createdAt) }}</el-descriptions-item>
  </el-descriptions>
</template>
```

---

## 6 按钮规则

**PROF-FE-907** 按钮强制规则： [MUST]

- ✅ 表格操作列 MUST `link` 类型
- ✅ 主操作 MUST `type="primary"`
- ✅ 危险操作 MUST `type="danger"`
- ✅ 异步操作 MUST `:loading="xxx"`

**图标规则**：

| 按钮 | 是否带图标 | 图标 |
|------|----------|------|
| 新增 | ✅ | `:icon="Plus"` |
| 批量删除 | ✅ | `:icon="Delete"` |
| 编辑 / 单条删除 / 查询 / 重置 / 确定 / 取消 / 导出 / 导入 | ❌ 不带图标 | — |

```vue
<template>
  <!-- ✅ 正确：只有"新增"和"批量删除"带图标 -->
  <el-button type="primary" :icon="Plus" @click="handleAdd">新增</el-button>
  <el-button type="danger" :icon="Delete" @click="handleBatchDelete">批量删除</el-button>

  <!-- ✅ 正确：其他按钮不带图标 -->
  <el-button type="primary" @click="handleSearch">查询</el-button>
  <el-button @click="handleReset">重置</el-button>

  <!-- ✅ 正确：操作列 link 类型 -->
  <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>

  <!-- ❌ 错误：非"新增/批量删除"按钮带图标 -->
  <el-button :icon="Search" @click="handleSearch">查询</el-button>
  <el-button :icon="Refresh" @click="handleReset">重置</el-button>
</template>
```

**图标引入规则**：

- ✅ MUST 从 `@element-plus/icons-vue` 显式 import（不全局注册）
- ✅ 加载状态用 `<el-icon class="is-loading"><Loading /></el-icon>`
- ❌ 禁止用 string 名引用图标（如 `icon="plus"` 在 EP 2.13 已废弃）
- ❌ 禁止用 `type="text"`（已废弃，用 `text` 属性替代）

---

## 7 卡片规则

**PROF-FE-908** `el-card` 强制规则： [MUST]

- ✅ MUST `shadow="never"`（避免视觉噪音）
- ✅ header MUST 用 `<template #header>`（非 prop）
- ✅ 卡片间距用 Tailwind 原子类（非硬编码 px 值）
- ❌ 列表页的卡片视图不使用 `el-card`，使用项目自定义卡片组件

```vue
<template>
  <el-card shadow="never" class="mb-4">
    <template #header>
      <span class="font-semibold">卡片标题</span>
    </template>
    卡片内容
  </el-card>
</template>
```

---

## 8 树形规则

**PROF-FE-909** `el-tree` 强制规则： [MUST]

- ✅ MUST `node-key`（用于高亮/选中）
- ✅ MUST `:props` 映射字段名
- ✅ 如项目封装了树形组件，优先使用项目封装版本（`vue3/encapsulated.md`）
- ❌ 禁止 > 100 节点用非懒加载树

---

## 9 选择器 / 输入框 / 日期选择器

### el-select

**PROF-FE-910** `el-select` 强制规则： [MUST]

- ✅ 长列表 MUST `filterable`
- ✅ MUST `clearable`（除非只读场景）
- ✅ 宽度 MUST `style="width: 100%"`（在 form-item 内）
- ❌ 禁止 option 数量 > 100 时不虚拟滚动

### el-input

**PROF-FE-911** `el-input` 强制规则： [MUST]

- ✅ MUST `clearable`（除密码字段）
- ✅ 有限字符数 MUST `maxlength` + `show-word-limit`
- ✅ 描述/备注用 `type="textarea" :rows="3"`
- ✅ 搜索框加 `:suffix-icon="Search"` + `@keyup.enter`

### el-date-picker

**PROF-FE-912** `el-date-picker` 强制规则： [MUST]

- ✅ MUST 显式 `format` 和 `value-format`（避免歧义）
- ✅ 日期范围使用 `type="daterange"` + 2 个 placeholder
- ❌ 禁止用 `type="datetime"` 又不显式时间格式

```vue
<template>
  <el-date-picker
    v-model="dateRange"
    type="daterange"
    range-separator="至"
    start-placeholder="开始日期"
    end-placeholder="结束日期"
    format="YYYY-MM-DD"
    value-format="YYYY-MM-DD"
  />
</template>
```

---

## 10 标签页 / 折叠面板

### el-tabs

**PROF-FE-913** `el-tabs` 强制规则： [MUST]

- ✅ MUST `v-model` 受控
- ✅ `name` 与 `v-model` 绑定的变量值一致
- ✅ 详情页用 `{EncapsulatedTabs}`（项目封装）

### el-collapse

```vue
<template>
  <el-collapse v-model="activeNames">
    <el-collapse-item title="基本信息" name="basic">
      <!-- 内容 -->
    </el-collapse-item>
  </el-collapse>
</template>
```

---

## 11 强制行为清单

- ✅ 所有 props 按"必填 → 常用 → 事件"顺序书写
- ✅ 所有事件 `@xxx` MUST 显式（即使 handler 是 `() => {}`）
- ✅ 所有列表渲染 MUST `:key`
- ✅ 所有异步操作 MUST `try/catch/finally`
- ✅ 所有外链跳转 MUST `target="_blank" rel="noopener noreferrer"`
- ✅ 图标 MUST 从 `@element-plus/icons-vue` 显式 import
- ✅ 加载状态用 `<el-icon class="is-loading"><Loading /></el-icon>`

---

## 12 禁止行为清单

- ❌ 禁止省略 `size="default"`
- ❌ 禁止在视图中硬编码颜色（必须用 CSS 变量或 Tailwind）
- ❌ 禁止在视图里硬编码间距数值
- ❌ 禁止操作列按钮用普通类型（必须 `link`）
- ❌ 禁止弹窗不写 `append-to-body`
- ❌ 禁止表单不写 `label-width`
- ❌ 禁止表格不写 `#empty` 槽位
- ❌ 禁止表格列硬编码 `width`（用 `min-width`）
- ❌ 禁止分页用非受控模式
- ❌ 禁止组件 props 超过 10 个
- ❌ 禁止 `el-tag` 超过 4 种状态用同一组件
- ❌ 禁止用 string 名引用图标
- ❌ 禁止用 `type="text"`（已废弃）

---

*本文件规则仅适用于 Vue 3 + Element Plus 2.13+。*
