description: MDM 特有检查项清单（Tp-* 引用、@mdm 路径别名、useCrudList 使用、ModelCard 规格等），含 frontend-vue 通用自检项的引用。
---

# MDM 编码自检清单

> 通用自检项（命名合规、类型合规、文件位置合规、API 合规、UI 组件合规、设计 Token 合规等）见 `profiles/frontend/vue/selfcheck.md`（60+ 检查项）。本清单仅补充 **MDM 特有**的检查项。

---

## 1. 路径别名合规

| ✓ | 检查项 | 详见 |
|---|--------|------|
| ☐ | 业务代码在 `apps/{app}/modules/{m}` 下 | `domain-spec §MDF-FE-003` |
| ☐ | `http` 从 `@mdm/core/http` 导入（非 `@/core/http`） | `references/api-development §1` |
| ☐ | 跨应用组件从 `@mdm/common/...` 导入 | `domain-spec §MDF-FE-005` |
| ☐ | 跨应用类型从 `@mdm/types/...` 导入 | `domain-spec §MDF-FE-006` |
| ☐ | 无 `../../../` 多层相对路径 | `domain-spec §MDF-FE-007` |

## 2. Tp-* 封装组件合规

| ✓ | 检查项 | 详见 |
|---|--------|------|
| ☐ | 列表/表格用 `TpTable`（非 `el-table`） | `references/components §2` |
| ☐ | `TpTable` 节点带 `class="flex-1 min-h-0"` | `MDF-FE-CMP-001` |
| ☐ | 分页器用 `TpPagination`（非 `el-pagination`） | `MDF-FE-CMP-009` |
| ☐ | 重要操作用 `TpConfirm`（非 `el-popconfirm`） | `MDF-FE-CMP-011` |
| ☐ | 消息提示用 `TpMessage.success/.error`（非 `ElMessage`） | `MDF-FE-CMP-014` |
| ☐ | 空状态用 `TpEmpty state="..."`（非 `el-empty`） | `MDF-FE-CMP-017` |
| ☐ | 详情分组用 `TpSectionTitle`（非 `el-divider`） | `MDF-FE-CMP-020` |
| ☐ | 顶层路由页面用 `TpPageFrame`（非手写外壳） | `MDF-FE-CMP-022` |
| ☐ | 编辑/详情页用 `TpPageHeader`（非手写标题 div） | `MDF-FE-CMP-029` |
| ☐ | 树控件用 `TpTree` / `TpTreeLazy`（非直接 `el-tree`） | `MDF-FE-CMP-035` |
| ☐ | 左树右表用 `TpLeftTreeLayout`（非手写 flex 分栏） | `MDF-FE-CMP-038` |
| ☐ | 卡片视图用 `TpCardList`（非手写 `<div class="grid">`） | `MDF-FE-CMP-043` |
| ☐ | 所有 Tp-* 显式 import（无全局注册） | 各组件规则 |

## 3. 业务 Composable 合规

| ✓ | 检查项 | 详见 |
|---|--------|------|
| ☐ | 列表页使用 `useCrudList`（非手写 loading + pageNum + pageSize + fetchData） | `MDF-FE-009` |
| ☐ | 导入/导出页使用 `useImportExport`（非裸调 API） | `MDF-FE-010` |
| ☐ | 表单校验使用 `useFormValidation().rules`（非内联 rules 数组） | `MDF-FE-011` |

## 4. BaseEntity 与分页合规

| ✓ | 检查项 | 详见 |
|---|--------|------|
| ☐ | 业务实体继承 `@mdm/common/types/base` 的 `BaseEntity` | `MDF-FE-016` |
| ☐ | `name` 字段必备，`description` 可选 | `MDF-FE-016` |
| ☐ | 分页参数 `PaginationParams` 使用 `pageNum`（非 `page`） | `MDF-FE-017` |
| ☐ | 分页响应使用 `list` 字段（非 `rows`） | `MDF-FE-017` |
| ☐ | DTO 不含 `id` / `createdAt` / `updatedAt` | `MDF-FE-018` |

## 5. statusTagType 合规

| ✓ | 检查项 | 详见 |
|---|--------|------|
| ☐ | 状态字段从 `@mdm/common/constants/statusTagType` 导入 | `MDF-FE-013` |
| ☐ | 无本地复刻 `statusTagType` 函数 | `MDF-FE-013` |

## 6. TpPagination 视觉合规

| ✓ | 检查项 | 详见 |
|---|--------|------|
| ☐ | 翻页按钮圆角为 2px（非 4px） | `MDF-FE-DT-002` |
| ☐ | 上一/下一页背景为 `#F5F7FA` | `MDF-FE-DT-003` |
| ☐ | 激活页码描边 `1px solid #337BFF` + 文字 `#337BFF` | `MDF-FE-DT-004` |
| ☐ | 容器 48px 高 + 顶部 `1px solid #E1E9F0` | `references/design-tokens §1` |

## 7. ModelCard 视觉合规

| ✓ | 检查项 | 详见 |
|---|--------|------|
| ☐ | 容器 4px 圆角（非 `rounded-lg`） | `MDF-FE-DT-006` |
| ☐ | 图标框 36×36px 蓝色背景 `#337bff`（非 emerald-500） | `MDF-FE-DT-007` |
| ☐ | 信息行 13px + `#666666`（非 12px） | `MDF-FE-DT-008` |
| ☐ | 底部版本/状态组合标签用 3px 圆角 | `MDF-FE-DT-009` |

## 8. TpLeftTreeLayout 存储 key 合规

| ✓ | 检查项 | 详见 |
|---|--------|------|
| ☐ | `storageKey` 用 `{module}-{tree-name}-width` 命名 | `MDF-FE-102` |
| ☐ | `defaultWidth` 推荐 280（默认 300） | `references/components §11` |

---

## 快速检查命令

编码完成后，快速过一遍：

1. `http` 是否从 `@mdm/core/http` 导入？
2. 列表页是否使用 `useCrudList`？
3. TpTable 是否有 `class="flex-1 min-h-0"`？
4. 是否所有 11 类强制场景都用了 `Tp-*` 组件？
5. 按钮图标是否只有「新增」和「批量删除」带图标？
6. p-3 间距是否正确使用？
7. 错误处理是否有 `try/catch/finally` + `TpMessage.error`？
8. 状态字段是否使用 `statusTagType` 常量？
9. BaseEntity 是否继承 `@mdm/common/types/base`？
10. ModelCard 圆角是否为 4px（非 rounded-lg）？

---

## 自检完成确认

```
✅ 通用自检项（frontend-vue 60+ 项）已全部通过
✅ MDM 特有自检项（30+ 项）已全部通过
✅ 任何不通过项已修复并重新自检
✅ 完成时间：____
✅ 提交人：____
```

> **自检未通过 → 禁止提交**。