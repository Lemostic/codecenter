# Git 工作流规范

| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 层级 | L0 |
| 引入条件 | always |
| 适用架构 | 全部 |
| 依赖规范 | 无 |
| 互斥规范 | 无 |

---

## 规范 UNI-GW-001: 分支策略

**规则:**

1. 仓库 MUST 包含 `main` 分支作为生产发布分支,该分支 MUST 始终保持可部署状态。
2. 仓库 MUST 包含 `develop` 分支作为开发集成分支。
3. 功能开发 MUST 从 `develop` 分支创建 `feature/{{description}}` 分支。
4. 缺陷修复 MUST 从 `main` 分支创建 `hotfix/{{description}}` 分支。
5. 版本发布 MUST 从 `develop` 分支创建 `release/{{version}}` 分支。
6. 分支名 MUST 使用 kebab-case,全小写,使用 `/` 分隔类型与描述。
7. 合并完成后,`feature`、`hotfix`、`release` 分支 MUST 在合并后删除。

**分支生命周期:**

```
main        ───●───────────────●──────── (生产发布)
                \             /
release/1.0 ────●─────●─────●            (发布准备/测试)
                    \
develop     ──●──●──●──●──●──●──●──      (持续集成)
                 /       \    \
feature/xxx ───●──●       \    \
                           \    \
hotfix/xxx   ───────────────●──● (从 main 拉取)
```

**示例:**

```bash
# 功能分支
git checkout develop
git pull origin develop
git checkout -b feature/user-authentication

# 热修复分支
git checkout main
git pull origin main
git checkout -b hotfix/fix-login-timeout

# 发布分支
git checkout develop
git pull origin develop
git checkout -b release/1.2.0
```

---

## 规范 UNI-GW-002: Commit 信息格式

**规则:**

1. Commit 信息 MUST 遵循 Conventional Commits 规范。
2. 格式 MUST 为:`{{type}}({{scope}}): {{subject}}`,其中 scope 可选。
3. 允许的 type 值:MUST 限于以下枚举:
   - `feat` — 新功能
   - `fix` — 修复缺陷
   - `docs` — 文档变更
   - `style` — 代码格式(不影响逻辑)
   - `refactor` — 代码重构(非新功能、非修复)
   - `test` — 测试相关
   - `chore` — 构建/工具链/依赖
   - `perf` — 性能优化
   - `ci` — CI 配置变更
4. subject MUST 使用中文或英文,但同一仓库内 MUST 保持一致。
5. subject MUST 不超过 72 个字符。
6. 若提交包含破坏性变更,MUST 在 subject 末尾或 Body 中标注 `BREAKING CHANGE:`。
7. 禁止使用无意义的提交信息(如 "update"、"fix bug"、"tmp")。

**示例:**

```
feat(user): 新增用户注册接口

- 支持邮箱和手机号两种注册方式
- 增加验证码校验逻辑
- 添加注册频率限制(同一IP每分钟最多5次)

Refs: #1234
```

```
fix(order): 修复订单金额计算精度丢失问题

使用 BigDecimal 替代 double 进行金额运算,
避免浮点数精度丢失导致的金额误差。

BREAKING CHANGE: OrderItem 的 price 字段类型从 double 改为 BigDecimal
Refs: #5678
```

```
chore(deps): 升级 Spring Boot 至 3.2.0
docs(api): 更新用户模块接口文档
refactor(config): 抽取公共配置加载逻辑
test(user): 补充 UserService 单元测试
```

---

## 规范 UNI-GW-003: PR/MR 流程与评审要求

**规则:**

1. 所有代码变更 MUST 通过 Pull Request(PR)或 Merge Request(MR)合并,禁止直接推送到 `main` 或 `develop`。
2. PR MUST 包含以下信息:
   - 标题遵循 Commit 信息格式(type + subject)
   - 描述区说明变更目的、影响范围和测试方式
   - 关联 Issue 编号
3. PR MUST 获得至少 **1 名**非作者审查者批准后方可合并。
4. PR MUST 通过 CI 流水线(编译、单元测试、代码扫描)后方可合并。
5. 单个 PR 变更文件数 SHOULD 不超过 20 个,变更行数 SHOULD 不超过 500 行。
6. PR 作者 MUST 在提交后 24 小时内响应审查评论。
7. 审查者 SHOULD 在收到审查请求后 8 个工作小时内完成首次审查。

**示例 PR 模板:**

```markdown
## 变更说明
<!-- 简要描述本次变更的目的和内容 -->

## 变更类型
- [ ] 新功能 (feat)
- [ ] 缺陷修复 (fix)
- [ ] 重构 (refactor)
- [ ] 其他 (chore/docs/test...)

## 影响范围
<!-- 说明本次变更影响的模块或服务 -->

## 测试方式
- [ ] 单元测试
- [ ] 集成测试
- [ ] 手动验证

## 关联 Issue
Refs: #{{issue_number}}

## 自检清单
- [ ] 代码符合命名规范
- [ ] 已补充/更新相关测试
- [ ] 无敏感信息(密码、密钥)泄露
- [ ] 已更新相关文档
```

---

## 规范 UNI-GW-004: 版本标签与语义化版本

**规则:**

1. 版本号 MUST 遵循语义化版本(SemVer)`{{major}}.{{minor}}.{{patch}}` 格式。
2. 版本标签 MUST 以 `v` 为前缀(如 `v1.2.3`)。
3. 版本号递增规则:
   - `major` — 不兼容的 API 变更(BREAKING CHANGE)
   - `minor` — 向下兼容的功能新增
   - `patch` — 向下兼容的缺陷修复
4. 版本标签 MUST 仅打在 `main` 分支或 `release` 分支合并至 `main` 的 commit 上。
5. 预发布版本 SHOULD 使用后缀(如 `v1.2.0-rc.1`、`v1.2.0-beta.1`)。

**示例:**

```bash
# 打版本标签
git tag -a v1.2.0 -m "release: v1.2.0 - 用户模块上线"
git push origin v1.2.0

# 预发布版本
git tag -a v1.3.0-rc.1 -m "release candidate: v1.3.0-rc.1"
```

---

## 规范 UNI-GW-005: 合并策略

**规则:**

1. `feature` 分支合并至 `develop` MUST 使用 **Squash Merge**,将多个提交压缩为一个干净的提交。
2. `release` 分支合并至 `main` MUST 使用 **Merge Commit**,保留完整的发布历史。
3. `hotfix` 分支合并至 `main` MUST 使用 **Merge Commit**。
4. `hotfix` 修复完成后 MUST 同步合并回 `develop` 分支。
5. 禁止使用 `force push` 到任何公共分支(`main`、`develop`、`release`)。
6. 合并前 MUST 确保分支无冲突,且已通过 CI 检查。

**示例:**

```bash
# feature → develop: Squash Merge
git checkout develop
git merge --squash feature/user-authentication
git commit -m "feat(user): 新增用户认证模块"

# release → main: Merge Commit
git checkout main
git merge --no-ff release/1.2.0 -m "Merge release/1.2.0 into main"
git tag -a v1.2.0 -m "release: v1.2.0"

# hotfix → main + 同步 develop
git checkout main
git merge --no-ff hotfix/fix-login-timeout -m "Merge hotfix/fix-login-timeout into main"
git checkout develop
git merge --no-ff hotfix/fix-login-timeout -m "Sync hotfix to develop"
```
