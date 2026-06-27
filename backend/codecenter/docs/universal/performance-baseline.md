# 性能基线规范

| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 层级 | L0 |
| 引入条件 | always |
| 适用架构 | 全部 |
| 依赖规范 | UNI-NC(命名规范)、UNI-LS(日志规范) |
| 互斥规范 | 无 |

---

## 一、数据访问防劣化

### 规范 UNI-PB-001: N+1 查询检测与修复 [MUST]

**规则:**

1. ORM 关联查询 MUST 使用 JOIN FETCH / `@BatchSize` / 应用层批量 IN 查询,禁止循环内逐条查询关联数据。
2. ORM 懒加载关联 SHOULD 配置 `@BatchSize` 或等效批量抓取策略。
3. 开发环境 MUST 开启 SQL 日志统计,单次请求 SQL 超过 **20 次** SHOULD 记录 WARN。

**示例:**

```java
// BAD: N+1 — 每条 order 触发一次 SQL
for (Order o : orders) {
    o.setUserName(userRepository.findById(o.getUserId()).getUserName());
}
// GOOD: 应用层批量 IN + Map 查找
Map<Long, User> userMap = userRepository.findByIdIn(userIds)
    .stream().collect(Collectors.toMap(User::getId, Function.identity()));
orders.forEach(o -> o.setUserName(userMap.get(o.getUserId()).getUserName()));
// GOOD: JOIN FETCH
@Query("SELECT o FROM Order o JOIN FETCH o.user WHERE o.status = :status")
List<Order> findOrdersWithUser(@Param("status") String status);
```

```typescript
// BAD: 循环内逐条 await
for (const o of orders) {
  o.userName = (await db.query('SELECT user_name FROM users WHERE id=$1', [o.user_id])).rows[0].user_name;
}
// GOOD: 批量 IN + Map
const userMap = new Map((await db.query(
  'SELECT id, user_name FROM users WHERE id = ANY($1)', [userIds]
)).rows.map(u => [u.id, u]));
orders.forEach(o => { o.userName = userMap.get(o.user_id).user_name; });
```

---

### 规范 UNI-PB-002: 禁止循环内单条 DB/RPC 调用 [MUST]

**规则:**

1. MUST 禁止在 `for`/`while`/`forEach` 循环体内执行单条数据库查询或远程 RPC 调用。
2. 循环前 MUST 将数据批量查出,再在循环体内做内存操作。
3. 若业务确需逐条处理,MUST 改用批量接口或限制并发量。

**示例:**

```java
// BAD: 循环内逐条 RPC
for (Long uid : userIds) {
    order.setUserNick(userRpcService.getUser(uid).getNickName());
}
// GOOD: 循环前批量查询
Map<Long, UserDTO> userMap = userRpcService.batchGetUsers(userIds);
for (Order o : orders) { o.setUserNick(userMap.get(o.getUserId()).getNickName()); }
```

```typescript
// BAD: 循环内逐条 await
for (const o of orders) {
  o.productName = (await productApi.getById(o.productId)).name;
}
// GOOD: 批量查询 + Map
const productMap = new Map((await productApi.batchGet(productIds)).map(p => [p.id, p]));
orders.forEach(o => { o.productName = productMap.get(o.productId)?.name; });
```

---

### 规范 UNI-PB-003: 禁止 SELECT * [MUST]

**规则:**

1. `SELECT` 语句 MUST 明确列出所需字段,禁止 `SELECT *`。
2. ORM 框架 SHOULD 使用 DTO 投影(Projection)而非实体全量映射。

**示例:**

```java
// BAD
@Query("SELECT u FROM User u WHERE u.status = :status")
List<User> findAll(@Param("status") String status);
// GOOD: DTO 投影 — 只查需要的字段
@Query("SELECT new com.example.dto.UserSummary(u.id, u.userName, u.email) "
     + "FROM User u WHERE u.status = :status")
List<UserSummary> findSummaries(@Param("status") String status);
```

```typescript
// BAD
await db.query('SELECT * FROM users WHERE status = $1', ['ACTIVE']);
// GOOD
await db.query('SELECT id, user_name, email FROM users WHERE status = $1', ['ACTIVE']);
```

---

### 规范 UNI-PB-004: 关联查询必须有索引支撑 [MUST]

**规则:**

1. `WHERE`、`JOIN`、`ORDER BY` 涉及的高频字段 MUST 建立合适的索引。
2. 组合索引 MUST 遵循最左前缀原则,高区分度字段 SHOULD 放在前列。
3. 新增或修改的 SQL SHOULD 上线前执行 `EXPLAIN` 分析,确认索引命中。
4. 深度分页(offset > **10000**)MUST 使用游标分页(keyset pagination)。

**示例:**

```sql
-- BAD: 深度分页(扫描 100020 行,丢弃 100000)
SELECT id, user_name FROM users ORDER BY id LIMIT 100000, 20;
-- GOOD: 游标分页
SELECT id, user_name FROM users WHERE id > :lastId ORDER BY id LIMIT 20;
-- GOOD: 组合索引
CREATE INDEX idx_user_status_created ON users (status, created_at DESC);
```

```java
// 游标分页实现
public CursorPageResult<UserResponse> listByCursor(Long lastId, int pageSize) {
    List<User> users = userRepository.findByIdGreaterThanOrderByIdAsc(
        lastId, PageRequest.of(0, pageSize + 1));
    boolean hasMore = users.size() > pageSize;
    if (hasMore) { users = users.subList(0, pageSize); }
    List<UserResponse> list = users.stream().map(UserMapper::toResponse).toList();
    return new CursorPageResult<>(list,
        hasMore ? list.get(list.size() - 1).getId() : null, hasMore);
}
```

---

## 二、分页与批量约束

### 规范 UNI-PB-005: 列表查询强制分页 [MUST]

**规则:**

1. 所有列表查询接口 MUST 强制分页,禁止无 `LIMIT` 的全量查询。
2. 未传入分页参数时 MUST 使用默认值:`pageNumber=1`,`pageSize=20`。
3. `pageSize` MUST 设置服务端上限,SHOULD 不超过 **200**,超出 MUST 拒绝请求。
4. 数据导出 MUST 使用流式输出或异步导出,禁止全量加载到内存。

**示例:**

```java
@GetMapping
public ApiResponse<PageResult<UserResponse>> listUsers(
        @RequestParam(defaultValue = "1") int pageNumber,
        @RequestParam(defaultValue = "20") int pageSize) {
    if (pageSize > 200) {
        throw new ValidationException("A00001", "pageSize 不能超过 200");
    }
    return ApiResponse.success(userService.listUsers(pageNumber, pageSize));
}
```

```typescript
async function fetchUsers(page: number, size: number): Promise<PageResult<UserVO>> {
  const safePageSize = Math.min(Math.max(size, 1), 200);
  const { data } = await get<PageResult<UserVO>>('/api/users/list', {
    params: { pageNumber: page, pageSize: safePageSize },
  });
  return data;
}
```

---

### 规范 UNI-PB-006: 批量操作单次上限 [MUST]

**规则:**

1. 批量操作(创建、更新、删除)MUST 限制单次请求条数,上限 MUST 不超过 **500 条**。
2. 超出上限 MUST 拒绝请求,不允许服务端静默截断。
3. SQL `IN` 子句参数数量 SHOULD 不超过 **1000 个**。

**示例:**

```java
@PostMapping("/batch-import")
public ApiResponse<BatchResult> batchImport(@RequestBody List<ImportItem> items) {
    if (items.size() > 500) {
        throw new ValidationException("A00001", "单次批量导入不能超过 500 条");
    }
    return ApiResponse.success(importService.processBatch(items));
}
```

```typescript
const BATCH_LIMIT = 500;
async function batchUpdate(items: UpdateItem[]): Promise<BatchResult> {
  if (items.length > BATCH_LIMIT) {
    throw new Error(`批量操作上限 ${BATCH_LIMIT} 条`);
  }
  return await api.post('/api/items/batch-update', { items });
}
```

---

### 规范 UNI-PB-007: 大批量必须分批处理 [SHOULD]

**规则:**

1. 批量接口内部 SHOULD 拆分为小批次执行(每批 **50~100 条**),避免单次事务过大。
2. 大批量数据处理 MUST 使用流式查询或分页游标,禁止一次性全量加载。
3. 分批处理 SHOULD 在每批之间释放事务,避免长事务锁表。

**示例:**

```java
public BatchResult processBatch(List<ImportItem> items) {
    int batchSize = 100, success = 0, failed = 0;
    for (int i = 0; i < items.size(); i += batchSize) {
        List<ImportItem> batch = items.subList(i, Math.min(i + batchSize, items.size()));
        BatchResult r = doProcessBatch(batch);
        success += r.getSuccess(); failed += r.getFailed();
    }
    return new BatchResult(success, failed);
}
```

```typescript
async function processLargeDataset(itemIds: number[]): Promise<void> {
  for (let i = 0; i < itemIds.length; i += 100) {
    await processBatch(itemIds.slice(i, i + 100));
  }
}
```

---

## 三、前端渲染性能

### 规范 UNI-PB-008: 路由级懒加载 [MUST]

**规则:**

1. 路由组件 MUST 使用 `() => import()` 动态导入,禁止入口文件静态导入所有页面。
2. 非首屏重型组件(图表、富文本)SHOULD 使用 `defineAsyncComponent` / `React.lazy`。
3. 第三方 UI 库 MUST 按需引入,禁止全量导入(如 `import ElementPlus from 'element-plus'`)。

**示例:**

```typescript
// Vue Router 路由级懒加载
const routes = [
  { path: '/', component: () => import('@/views/Home.vue') },
  { path: '/dashboard', component: () => import('@/views/Dashboard.vue') },
];
// BAD: import ElementPlus from 'element-plus';
// GOOD: import { ElButton, ElInput } from 'element-plus';
```

```vue
<!-- 非首屏重型组件 -->
<script setup lang="ts">
import { defineAsyncComponent } from 'vue';
const LazyChart = defineAsyncComponent(() => import('@/components/ChartPanel.vue'));
</script>
<template>
  <Suspense>
    <template #default><LazyChart /></template>
    <template #fallback><LoadingSkeleton /></template>
  </Suspense>
</template>
```

---

### 规范 UNI-PB-009: 大列表虚拟滚动 [SHOULD]

**规则:**

1. 列表数据量超过 **100 条**时,SHOULD 使用虚拟滚动,仅渲染可视区域 DOM 节点。
2. 虚拟滚动容器 MUST 保持固定高度,避免布局抖动。
3. 列表项高度不固定时,SHOULD 使用动态高度虚拟滚动方案。

**示例:**

```vue
<template>
  <RecycleScroller class="scroller" :items="list" :item-size="56"
    key-field="id" v-slot="{ item }">
    <div class="list-item">{{ item.name }} — {{ item.status }}</div>
  </RecycleScroller>
</template>
<script setup lang="ts">
import { RecycleScroller } from 'vue-virtual-scroller';
defineProps<{ list: ListItem[] }>();
</script>
```

---

### 规范 UNI-PB-010: 图片 WebP 优先 + 懒加载 [MUST]

**规则:**

1. 图片 MUST 优先使用 WebP 格式,需兼容旧浏览器时 SHOULD 用 `<picture>` 降级。
2. 图片 MUST 设置明确的 `width` 和 `height`,避免 CLS。
3. 非首屏图片 MUST 使用 `loading="lazy"`。

**示例:**

```html
<picture>
  <source srcset="/images/hero.webp" type="image/webp">
  <source srcset="/images/hero.jpg" type="image/jpeg">
  <img src="/images/hero.jpg" alt="Hero" width="1200" height="600"
       loading="lazy" decoding="async">
</picture>
<img src="/images/avatar.webp" alt="Avatar" width="48" height="48" loading="lazy">
```

---

### 规范 UNI-PB-011: 首屏关键资源 preconnect/prefetch [SHOULD]

**规则:**

1. 首屏依赖的第三方域名 SHOULD 使用 `<link rel="preconnect">` 预建立连接。
2. 关键字体、首屏大图 SHOULD 使用 `<link rel="preload">`。
3. 非关键 CSS SHOULD 异步加载,JS 脚本 SHOULD 使用 `defer`。

**示例:**

```html
<head>
  <link rel="preconnect" href="https://cdn.example.com" crossorigin>
  <link rel="preload" href="/fonts/main.woff2" as="font" type="font/woff2" crossorigin>
  <link rel="stylesheet" href="/styles/main.css" media="print" onload="this.media='all'">
  <script src="/js/app.js" defer></script>
</head>
```

---

## 四、算法与数据结构选择

### 规范 UNI-PB-012: 集合查找用 Map/Set 而非 List 遍历 [MUST]

**规则:**

1. 按 key 查找 MUST 使用 `Map`/`HashMap`/`Set`,禁止 `List` 线性遍历查找。
2. 两个集合做关联匹配时,MUST 先将一个集合转为 `Map`,再做 O(1) 查找。
3. 去重操作 MUST 使用 `Set`,禁止 `List.contains()` 判断。

**示例:**

```java
// BAD: O(n) per lookup
User matched = users.stream()
    .filter(u -> u.getId().equals(order.getUserId())).findFirst().orElse(null);
// GOOD: O(1)
Map<Long, User> userMap = users.stream()
    .collect(Collectors.toMap(User::getId, Function.identity()));
User matched = userMap.get(order.getUserId());
```

```typescript
// BAD: O(n)
const user = users.find(u => u.id === order.userId);
// GOOD: O(1)
const userMap = new Map(users.map(u => [u.id, u]));
const user = userMap.get(order.userId);
// BAD: O(n^2) 去重
if (!arr.includes(name)) arr.push(name);
// GOOD: O(n) 去重
const unique = [...new Set(names)];
```

---

### 规范 UNI-PB-013: 字符串拼接用 StringBuilder/Array.join [MUST]

**规则:**

1. 循环内字符串拼接 MUST 使用 `StringBuilder`(Java)或数组收集后 `join()`(TypeScript),禁止 `+` / `+=`。
2. 非循环场景的简单拼接(2~3 个字符串)MAY 使用 `+` 操作符。

**示例:**

```java
// BAD: O(n^2) — 每次循环创建新 String 对象
String result = "";
for (String item : items) { result += item + ","; }
// GOOD: StringBuilder
StringBuilder sb = new StringBuilder(items.size() * 20);
for (String item : items) { sb.append(item).append(','); }
String result = sb.toString();
```

```typescript
// BAD
let result = '';
for (const item of items) { result += item + ','; }
// GOOD
const result = items.join(',');
```

---

### 规范 UNI-PB-014: 嵌套循环不得超过两层 [SHOULD]

**规则:**

1. 两层以上嵌套循环 MUST 视为 O(n^2)+ 警告,SHOULD 重构为 Map 查找、双指针或数据库 JOIN。
2. 若业务确需多层遍历,MUST 添加注释说明必要性及数据量上限。
3. 避免在热路径(高频调用方法)上创建大量临时对象,减少 GC 压力。

**示例:**

```java
// BAD: 三重嵌套
for (Department d : depts)
  for (Team t : d.getTeams())
    for (User u : t.getUsers()) { /* ... */ }
// GOOD: 扁平化 Stream
depts.stream()
    .flatMap(d -> d.getTeams().stream())
    .flatMap(t -> t.getUsers().stream())
    .filter(u -> "ACTIVE".equals(u.getStatus())).toList();
```

```typescript
// BAD: O(n*m) 双重循环匹配
for (const o of orders)
  for (const p of products)
    if (o.productId === p.id) matched.push({ order: o, product: p });
// GOOD: O(n+m) Map 查找
const pm = new Map(products.map(p => [p.id, p]));
const matched = orders.filter(o => pm.has(o.productId))
    .map(o => ({ order: o, product: pm.get(o.productId)! }));
```

---

## 五、性能感知编码习惯

### 规范 UNI-PB-015: 数据库批量操作用 batch [MUST]

**规则:**

1. 多条记录插入/更新 MUST 使用批量 SQL,禁止逐条 `INSERT`/`UPDATE`。
2. JDBC MUST 使用 `addBatch()` + `executeBatch()`,MyBatis MUST 使用 `<foreach>` 批量语法。
3. 批量写入 SHOULD 配合事务控制,一次性提交。

**示例:**

```java
// BAD: 逐条 INSERT — 每条一次网络往返
for (User u : users) { userRepository.save(u); }
// GOOD: JPA batch save
userRepository.saveAll(users);
// GOOD: MyBatis foreach 批量
@Insert("<script>INSERT INTO users (user_name, email) VALUES "
    + "<foreach collection='users' item='u' separator=','>"
    + "(#{u.userName}, #{u.email})</foreach></script>")
void batchInsert(@Param("users") List<User> users);
```

```typescript
// BAD: 逐条 INSERT
for (const u of users) {
  await db.query('INSERT INTO users (user_name, email) VALUES ($1,$2)',
    [u.userName, u.email]);
}
// GOOD: 拼接批量 INSERT
const cols = users.map((_, i) => `($${i*2+1},$${i*2+2})`).join(',');
const vals = users.flatMap(u => [u.userName, u.email]);
await db.query(`INSERT INTO users (user_name, email) VALUES ${cols}`, vals);
```

---

### 规范 UNI-PB-016: 远程调用结果缓存到局部变量 [SHOULD]

**规则:**

1. 同一方法内多次需要同一远程调用结果时,SHOULD 缓存到局部变量,禁止重复调用。
2. 循环或高频方法中,MUST 将不变的远程查询结果提升到循环外部。
3. 方法参数中的远程查询结果 SHOULD 由调用方传入。

**示例:**

```java
// BAD: 循环内重复查询不变量
for (Order o : orders) {
    applyConfig(o, configService.getConfig("ORDER_RULE")); // 每条都查一次
}
// GOOD: 不变量提升到循环外
Config config = configService.getConfig("ORDER_RULE"); // 只查一次
for (Order o : orders) { applyConfig(o, config); }
```

```typescript
// BAD: 同一函数内两次 RPC
const order = await orderApi.get(id);
await validate(order);
await enrich(order); // enrich 内部又调了 orderApi.get(id)
// GOOD: 一次查询,结果传递
const order = await orderApi.get(id);
await validate(order);
await enrich(order); // 直接使用传入的 order
```

---

### 规范 UNI-PB-017: 日志 lazy evaluation [SHOULD]

**规则:**

1. `DEBUG`/`TRACE` 日志 MUST 使用占位符格式 `log.debug("x: {}", val)`,禁止字符串拼接。
2. 日志参数涉及昂贵计算(JSON 序列化)时,SHOULD 先判断日志级别再计算。
3. 生产环境 SHOULD 将 `DEBUG` 默认关闭,通过动态配置按需开启。

**示例:**

```java
// BAD: DEBUG 关闭时仍执行 JSON 序列化
log.debug("request: " + JSON.toJSONString(request));
// GOOD: 占位符
log.debug("request: {}", request);
// GOOD: 昂贵计算先判断级别
if (log.isDebugEnabled()) {
    log.debug("detail: {}", expensiveSerialize(order));
}
```

```typescript
// BAD: 始终执行 stringify
logger.debug(`request: ${JSON.stringify(request)}`);
// GOOD: 条件判断
if (logger.isDebugEnabled()) {
  logger.debug(`request: ${JSON.stringify(request)}`);
}
```

---

## 变更记录

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0 | 2026-06-18 | 初始版本:聚焦日常编码性能模式,覆盖数据访问防劣化、分页与批量约束、前端渲染性能、算法与数据结构选择、性能感知编码习惯 |
