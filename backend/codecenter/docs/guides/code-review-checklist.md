# Code Review 清单

> 本清单是 PR 评审阶段的检查项,与 `design-review-checklist.md`（设计阶段自检）配合使用。
> 评审者按本清单逐项打勾,确保 AI 生成的代码变更符合企业编码规范。
>
> **使用方式**:在 PR 模板中引用本清单链接,评审者按"硬性"项目强制拦截,"建议"项目作为加分项。

| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 状态 | Active |
| 适用 | 所有 PR（含 AI 辅助生成的代码） |

---

## 一、命名与结构（硬性）

- [ ] **文件名符合规范** — PascalCase（类）/ camelCase（方法、变量）/ SCREAMING_SNAKE（常量）
- [ ] **包路径与目录层级一致** — 物理文件位置 = `package` 声明,无错位
- [ ] **类名是名词,方法名是动词** — `UserService.createUser()` 而非 `UserService.process()`
- [ ] **无拼音/缩写/无意义名** — 不出现 `data` / `info` / `tmp` / `handleClick` 等模糊命名
- [ ] **Vue 文件名 PascalCase + 4 后缀** — `UserList.vue` / `UserDetail.vue` / `UserEditor.vue` / `UserIndex.vue`

## 二、类型与接口（硬性）

- [ ] **无 `any` 类型** — TS 严格模式开启,必要时用 `unknown` + 类型守卫
- [ ] **DTO 不含 id / createdAt 等后端生成字段** — 创建参数仅含业务字段
- [ ] **VO 不直接传给 API** — VO 仅用于展示,必须显式转换为 DTO
- [ ] **业务实体 5 件套齐全** — Entity / VO / CreateDTO / UpdateDTO / Query（缺一即不合格）
- [ ] **泛型约束明确** — `Promise<PageResult<User>>` 而非 `Promise<any>`
- [ ] **返回类型显式声明** — `function getUser(): User` 而非依赖类型推断（公共 API 强制）

## 三、API 与错误处理（硬性）

- [ ] **5 件套函数齐全且顺序固定** — list / get / create / update / delete
- [ ] **异步操作有 try / catch / finally** — 禁止裸 `await`
- [ ] **loading 状态正确重置** — finally 中复位,无论成功失败
- [ ] **错误用统一消息组件提示用户** — 禁止仅 `console.log`
- [ ] **错误日志带方法名前缀** — `console.error('[handleSubmit]', error)`
- [ ] **路径前缀符合规范** — `/api/v1/{module}/{entity}`
- [ ] **业务码错误由拦截器统一处理** — 不在业务代码中重复判断 `res.success`
- [ ] **超时配置存在** — 同步发送 / 远程调用有超时上限(默认 3s,长任务 30s)

## 四、组件与状态（Vue 3 硬性）

- [ ] **使用 `<script setup lang="ts">`** — 禁止 Options API
- [ ] **块顺序正确** — `<script setup>` → `<template>` → `<style scoped>`
- [ ] **块内 10 步骤顺序** — import / defineOptions / defineProps / defineEmits / state / computed / methods / watch / lifecycle / defineExpose
- [ ] **Props 使用 TypeScript 泛型** — 禁止 `defineProps({ id: String })` 运行时声明
- [ ] **Emits 使用 TypeScript 泛型** — 事件类型与 payload 显式声明
- [ ] **本地状态用 `ref` / `reactive`** — 不提升到全局 Store
- [ ] **派生状态用 `computed`** — 禁止用 `ref` 存储 + `watch` 同步
- [ ] **列表渲染使用稳定唯一的 `key`** — 禁止用 index
- [ ] **Props ≤ 10 个** — 超出拆组件
- [ ] **单文件 ≤ 600 行** — 超出拆组件或提取 composable
- [ ] **跨组件通信按优先级** — Props > Slot > Composable > Store > Provide/Inject

## 五、UI 组件使用（硬性）

- [ ] **所有表单类组件有 `size="default"`** — 避免尺寸继承不一致
- [ ] **`el-form` 有 `label-width`** — 对齐不混乱
- [ ] **`el-table` 有 `#empty` 槽位** — 用 `{EncapsulatedEmpty}`（或 `el-empty`）
- [ ] **所有分页用 `{EncapsulatedPagination}`** — 受控模式 + 当前项目封装
- [ ] **`el-dialog` 有 `append-to-body` + `close-on-click-modal="false"`** — 防 overflow 裁剪、防误关
- [ ] **`el-tag` 有 `size="default"`** — 状态徽标统一
- [ ] **必填字段标 `required`** — 红星提示
- [ ] **列表用 `{EncapsulatedTable}`**（非 el-table） / **空状态用 `{EncapsulatedEmpty}`** / **确认用 `{EncapsulatedConfirm}`** / **消息用 `{EncapsulatedMessage}`**
- [ ] **操作列按钮是 `link` 类型** + **列宽用 `min-width`** + **长字段 `show-overflow-tooltip`**
- [ ] **弹窗包 `el-scrollbar`**（长内容场景）
- [ ] **异步操作按钮有 `:loading`** — 保存/提交按钮
- [ ] **图标从 `@element-plus/icons-vue` 显式 import** — 禁止 string 名
- [ ] **「新增」按钮带 `:icon="Plus"`** / **「批量删除」带 `:icon="Delete"`** / 其他按钮不带图标

## 六、布局与设计 Token（硬性）

- [ ] **默认内边距 `p-3`**（12px） — 工具栏、wrapper、对话框、详情页分组
- [ ] **仅一行工具栏不加 `border-b`** / 多行工具栏最下面一行不加
- [ ] **颜色引用设计 Token**（CSS 变量 / Tailwind 任意值） — 禁止硬编码
- [ ] **间距引用 Tailwind 原子类** — 禁止硬编码 px
- [ ] **无 Tailwind 默认调色板**（`bg-gray-*` / `text-blue-*`）
- [ ] **有底部操作栏的页面用 flex 三段式布局** — `{EncapsulatedPageFrame}` + 内容 flex-1 + 底部 flex-shrink-0
- [ ] **编辑页/详情页用 `<PageHeader>`** — 禁止手写标题 div

## 七、后端架构（硬性,适用相关包时）

- [ ] **MVC/DDD 分层明确** — Controller 不直接调 Mapper(DDD 项目)/Repository(JPA 项目)
- [ ] **领域层不依赖基础设施层** — `arch-ddd` 包规范
- [ ] **Repository 方法命名规范** — `findByXxx` / `existsByXxx` / `deleteByXxx`
- [ ] **Service 事务边界明确** — `@Transactional` 标注在 Service 方法,不滥用
- [ ] **异常统一抛 `BusinessException`** — 不裸抛 `RuntimeException`
- [ ] **API 响应格式符合规范** — `{ success, data, message, code }` 统一结构
- [ ] **traceId 传递** — HTTP 入口 + 异步/跨进程透传

## 八、消息队列（硬性,适用相关包时）

- [ ] **Topic / Tag 命名规范** — `<业务域>_<实体>_<动作>` / 全小写下划线
- [ ] **消息含 `messageId` 幂等字段** — 消费者能去重
- [ ] **消息含 `traceId`** — 跨服务链路追踪
- [ ] **生产环境同步发送 3 秒超时** + 失败重试 3 次 + 降级到本地消息表
- [ ] **消费者实现幂等** — messageId 去重表(SETNX 或业务表唯一索引)
- [ ] **死信队列有监控告警** + 落库便于人工排查
- [ ] **RocketMQ 顺序消息单线程消费** + Kafka 顺序消息 partition 路由
- [ ] **Kafka 消费者 offset 提交策略明确** — 手动 commit 在业务处理成功后

## 九、安全（硬性）

- [ ] **无硬编码密钥** — API key / password / token 走环境变量
- [ ] **SQL 参数化** — 禁止字符串拼接(`+ value +`)
- [ ] **XSS 防护** — v-html 禁用 / 用户输入 sanitize
- [ ] **CSRF 防护** — POST/PUT/DELETE 请求带 token
- [ ] **敏感字段脱敏** — 日志中身份证 / 手机号 / 邮箱打印前 mask
- [ ] **API 权限校验** — 路由 `meta.permission` 与后端 `@PreAuthorize` 双重校验
- [ ] **跨域配置** — CORS 白名单而非 `*`

## 十、性能（硬性）

- [ ] **无 N+1 查询** — 列表查询禁止循环触发 SQL/Repository
- [ ] **分页参数受控** — 列表接口有 `page` / `pageSize`,上限 `pageSize ≤ 200`
- [ ] **批量操作使用 `batchSave` / `batchUpdate`** — 禁止循环单条调用
- [ ] **大数据量列表 lazyLoad / virtual scroll** — 1000+ 行考虑虚拟滚动
- [ ] **避免在模板中调用方法** — 复杂计算用 `computed` 缓存
- [ ] **图片懒加载** — `loading="lazy"` 或 IntersectionObserver
- [ ] **长列表 key 用稳定字段** — index 会导致重渲染

## 十一、测试（硬性,适用相关包时）

- [ ] **单测覆盖率 ≥ 80%**（核心 Service / 领域逻辑）
- [ ] **新功能 MUST 有单测** — AI 不得省略测试
- [ ] **测试有 Given-When-Then 三段式** + DisplayName 中文描述
- [ ] **AssertJ 断言**(JVM)/ **Vitest + Testing Library**(前端) — 禁止断言方式混乱
- [ ] **集成测试用 TestContainers** — 禁止连共享 dev/prod DB
- [ ] **慢测试标 `@Tag("slow")`** — 默认不跑,CI 通过 `runSlowTests=true` 触发
- [ ] **已有测试 MUST NOT 删除/修改** — AI 不得通过删除测试"修复"构建失败

## 十二、AI 协作规范（硬性,适用 AI 生成 PR）

- [ ] **需求理解摘要提交** — AI 在 PR 描述中提供需求理解 + 设计思路
- [ ] **影响范围分析** — 列出修改的模块、文件、对外接口、潜在冲突
- [ ] **已有代码保护** — AI 未修改 `*Test.*` / `*.test.*` / `*.spec.*` / 已有公共 API（除非任务明确要求）
- [ ] **无新增依赖** — AI 未引入未经 manifest 声明的 npm/maven 依赖
- [ ] **变量命名有意义** — 无 `tmp` / `data` / `test1` 等 AI 临时命名残留
- [ ] **代码风格一致** — 与项目既有代码风格一致(ESLint/Prettier 通过)
- [ ] **删除 AI 临时调试代码** — 无 `console.log('debug')` / `// TODO: AI temp` 残留
- [ ] **架构约束未越界** — DDD 项目未让 Controller 直接调 Repository
- [ ] **前端强制封装组件未绕过** — 列表/分页/确认/消息/空状态场景未直接用 el-table/el-pagination/ElMessageBox/ElMessage/el-empty

## 十三、文档与 PR 描述（建议）

- [ ] **PR 标题格式** — `<type>: <description>` (feat/fix/refactor/docs/test/chore)
- [ ] **PR 描述包含**:
  - 需求背景(为什么要做)
  - 实现思路(怎么做的)
  - 影响范围(改了什么文件、对外接口是否变化)
  - 测试情况(单测/e2e 是否覆盖)
  - 截图或录屏(UI 变更必须)
  - 关联 Issue / Ticket 编号
- [ ] **破坏性变更标注** — `BREAKING:` 前缀 + 详细迁移指南
- [ ] **Commit 信息符合 Conventional Commits 规范**

## 十四、安全合规（建议）

- [ ] **大文件未提交** — `.gitignore` 包含 `node_modules` / `target` / `.class` / 压缩包
- [ ] **无 console.log 调试输出** — 提交前清理
- [ ] **无 commented-out 代码** — 提交前清理
- [ ] **无 `// FIXME` / `// XXX` 标记** — 已修复或转 Issue
- [ ] **License / Copyright 头** — 视项目要求

## 评审决策

| 等级 | 标准 | 处理 |
|------|------|------|
| **L1 阻塞（必须修改）** | 任何硬性项目未通过 | 退回作者修改,reviewer 标注具体项 |
| **L2 警告** | 建议项目未通过 | 合并可接受,但作者应在下个 PR 修复 |
| **L3 建议** | 风格/优化项 | 仅评论,不阻塞合并 |

## 评审 SLA

- 首个评审响应: 工作日 4 小时内
- 二次评审响应: 工作日 2 小时内
- 阻塞级 (L1) 必须当个工作日处理

---

## 十五、AI 协作专项审查（硬性 + 建议）

> 本章是 AI 协作生成代码的专项审查项，与 `universal/ai-hallucination-defense.md` 配套使用。
> 当 PR 描述中包含 "AI generated" 或 "Co-authored-by: AI" 标记时，AI 专项检查 MUST 全部勾选。

### 15.1 AI 幻觉防御

- [ ] **AI 标注了信息来源** — 引用任何 API/库/方法时 MUST 在同回复标注来源（UNI-AHD-003）
- [ ] **AI 不使用模糊词** — 输出中无"应该/通常/大多数情况下"等不可验证内容（UNI-AHD-011）
- [ ] **AI 引用了 prompt 中的提示词模板** — 至少使用过 4 模板中 1 个（UNI-AHD-005）
- [ ] **AI 给出了 L1-L4 复用决策** — 创建新类时附"复用检查报告"（UNI-CS-006）

### 15.2 AI 代码验证

- [ ] **类型检查通过** — `tsc --noEmit` 等价命令无报错（UNI-AHD-006 / UNI-AHD-009）
- [ ] **单测覆盖边界** — 不止 happy path，包含 null/空/极值（UNI-AHD-007）
- [ ] **集成测试在真实依赖上跑** — 不允许 mock 替代所有外部依赖（UNI-AHD-008）
- [ ] **AI 引入的依赖有引用源** — `pom.xml` / `package.json` 新增依赖 MUST 是官方包

### 15.3 AI 代码审查重点

- [ ] **API 真实存在** — 不在 IDE 自动完成中出现的 API 视为高风险
- [ ] **类型正确** — 优先信任编译器，AI 类型注释要二次核对
- [ ] **边界条件覆盖** — null / 空数组 / 0 / 极大值
- [ ] **业务逻辑可解释** — AI 对"为什么这么做"无法解释的代码 = 高风险
- [ ] **依赖来源可信** — AI 引入的新依赖 MUST 是官方包，无私有/可疑包

---

*本清单与 `design-review-checklist.md`（设计阶段自检）配合使用:设计阶段用灵魂 5 问 + 范式检查,代码 PR 阶段用本清单逐项打勾。两者覆盖范围不重叠。*
