/* ============================================================
 * data.js — 规范体系结构化数据
 * 数据来源：routing-rules.yaml / _composition-presets.yaml / PROFILES.md / registry.yaml
 * 作为指纹匹配模拟器的唯一事实源
 * ============================================================ */

// L0 通用规范（9 份，所有项目强制加载，不可禁用）
const L0_SPECS = [
  { id: 'naming-conventions',   name: '命名约定',     desc: '跨语言的标识符命名规则（变量、函数、类、文件、包）' },
  { id: 'git-workflow',         name: 'Git 工作流',   desc: '分支策略、Conventional Commits、PR 流程、版本标签' },
  { id: 'api-design',           name: 'API 设计',     desc: 'RESTful 设计原则、响应格式、版本控制、错误码' },
  { id: 'security-baseline',    name: '安全基线',     desc: '输入验证、注入防护、认证授权、敏感数据处理' },
  { id: 'testing-standards',    name: '测试标准',     desc: '测试策略、覆盖率要求、分层测试、测试命名' },
  { id: 'logging-standards',    name: '日志标准',     desc: '日志格式、级别使用、脱敏规则、结构化日志' },
  { id: 'exception-handling',   name: '异常处理',     desc: '全局异常处理、自定义异常体系、异常传播策略' },
  { id: 'request-tracing',      name: '链路追踪',     desc: 'traceId 传递、请求上下文、AOP 切面设计' },
  { id: 'change-scope-control', name: '变更范围控制', desc: '防重复造轮子、项目资产清单、术语字典、测试保护' }
];

// L1 架构配套方案包（原子化设计，可自由组合）
// rules 为该包的规则条数（取自 PROFILES.md / ARCHITECTURE.md）
const L1_PACKAGES = [
  // ---- 框架基座 ----
  {
    packageId: 'spring-boot-base',
    type: 'framework-base',
    typeName: '框架基座',
    name: 'Spring Boot 通用基础',
    desc: '构造器注入、统一响应、全局异常、事务管理、测试结构、通用编码约定',
    rules: 24,
    specs: [{ id: 'base-spring-boot', name: 'Spring Boot 通用基础' }],
    auto: false   // 被架构包隐含依赖，不直接出现在勾选区
  },
  // ---- 架构风格（互斥）----
  {
    packageId: 'arch-mvc',
    type: 'architecture',
    typeName: '架构风格',
    name: 'MVC 三层架构',
    desc: '分层结构、职责边界、Controller / Service 规范、REST 接口',
    rules: 34,
    specs: [{ id: 'mvc-structure', name: 'MVC 三层结构' }],
    conflicts: ['arch-ddd'],
    implies: ['spring-boot-base'],
    selectable: true
  },
  {
    packageId: 'arch-ddd',
    type: 'architecture',
    typeName: '架构风格',
    name: 'DDD 四层架构',
    desc: '领域驱动设计：四层结构 + 领域模型 + 事件模式 + 读写分离（CQRS）',
    rules: 90,
    specs: [
      { id: 'ddd-structure',      name: 'DDD 四层结构' },
      { id: 'ddd-domain-model',   name: '领域模型（聚合根/实体/值对象）' },
      { id: 'ddd-event-patterns', name: '领域事件模式' },
      { id: 'ddd-cqrs',           name: '读写分离 CQRS' }
    ],
    conflicts: ['arch-mvc'],
    implies: ['spring-boot-base'],
    selectable: true
  },
  // ---- 持久化技术（可多选）----
  {
    packageId: 'persistence-mybatis-plus',
    type: 'persistence',
    typeName: '持久化',
    name: 'MyBatis-Plus',
    desc: 'BaseMapper、LambdaQueryWrapper、Entity、分页、SQL、乐观锁',
    rules: 18,
    specs: [{ id: 'mbp-persistence', name: 'MyBatis-Plus 持久化' }],
    conflicts: ['persistence-mybatis', 'persistence-jpa'],
    implies: ['spring-boot-base'],
    selectable: true
  },
  {
    packageId: 'persistence-mybatis',
    type: 'persistence',
    typeName: '持久化',
    name: '原生 MyBatis',
    desc: 'XML 映射、动态 SQL、结果映射',
    rules: 13,
    specs: [{ id: 'mb-persistence', name: 'MyBatis 持久化' }],
    conflicts: ['persistence-mybatis-plus', 'persistence-jpa'],
    implies: ['spring-boot-base'],
    selectable: true
  },
  {
    packageId: 'persistence-jpa',
    type: 'persistence',
    typeName: '持久化',
    name: 'Spring Data JPA',
    desc: 'JPA / QueryDSL：Repository、Entity、关联映射',
    rules: 15,
    specs: [{ id: 'jpa-persistence', name: 'JPA 持久化' }],
    conflicts: ['persistence-mybatis-plus', 'persistence-mybatis'],
    implies: ['spring-boot-base'],
    selectable: true
  },
  {
    packageId: 'persistence-redis',
    type: 'persistence',
    typeName: '持久化',
    name: 'Redis',
    desc: 'Key 设计、数据结构选型、缓存一致性、序列化、分布式锁',
    rules: 14,
    specs: [{ id: 'redis-persistence', name: 'Redis 缓存与存储' }],
    implies: ['spring-boot-base'],
    selectable: true
  },
  {
    packageId: 'persistence-elasticsearch',
    type: 'persistence',
    typeName: '持久化',
    name: 'Elasticsearch',
    desc: '索引设计、查询规范、性能优化',
    rules: 14,
    specs: [{ id: 'es-persistence', name: 'Elasticsearch 搜索与存储' }],
    implies: ['spring-boot-base'],
    selectable: true
  },
  // ---- 前端 ----
  {
    packageId: 'frontend-vue',
    type: 'frontend',
    typeName: '前端',
    name: 'React / Vue 前端',
    desc: '项目结构、组件设计、状态管理（Pinia / React Query 等）',
    rules: 47,
    specs: [
      { id: 'fe-structure',  name: '前端项目结构' },
      { id: 'fe-component',  name: '组件设计规范' },
      { id: 'fe-state',      name: '状态管理规范' }
    ],
    selectable: true
  }
];

// L2 领域扩展插件（SPI）
const L2_SPI = [
  {
    domain: 'data-governance',
    name: '数据治理领域',
    desc: '数据质量、元数据管理、数据血缘、数据分类等专项规则',
    specId: 'spi-data-governance',
    active: true
  }
];

// 组合推荐预设（取自 _composition-presets.yaml）
const PRESETS = [
  { id: 'spring-mvc-mybatis-plus',     name: 'Spring Boot MVC + MyBatis-Plus', desc: '标准三层 MVC + MyBatis-Plus，适合 CRUD 与管理后台', profiles: ['arch-mvc', 'persistence-mybatis-plus'], tags: ['java','spring-boot','mybatis-plus'] },
  { id: 'spring-mvc-mybatis',          name: 'Spring Boot MVC + MyBatis',      desc: '标准三层 MVC + 原生 MyBatis，需要精细控制 SQL',     profiles: ['arch-mvc', 'persistence-mybatis'],          tags: ['java','spring-boot','mybatis'] },
  { id: 'spring-mvc-jpa',              name: 'Spring Boot MVC + JPA',          desc: '标准三层 MVC + JPA，领域模型简单的应用',             profiles: ['arch-mvc', 'persistence-jpa'],              tags: ['java','spring-boot','jpa'] },
  { id: 'ddd-mybatis-plus',            name: 'DDD + MyBatis-Plus',             desc: 'DDD 四层架构 + MyBatis-Plus，适合复杂业务领域',     profiles: ['arch-ddd', 'persistence-mybatis-plus'],     tags: ['java','spring-boot','ddd'] },
  { id: 'ddd-mybatis-plus-cqrs',       name: 'DDD + MyBatis-Plus + 读写分离',  desc: 'DDD 四层 + CQRS，读写比严重不对称的复杂领域',       profiles: ['arch-ddd', 'persistence-mybatis-plus'],     tags: ['java','spring-boot','ddd','cqrs'] },
  { id: 'ddd-jpa',                     name: 'DDD + JPA / QueryDSL',           desc: 'DDD + JPA，领域模型与持久化解耦最彻底',             profiles: ['arch-ddd', 'persistence-jpa'],              tags: ['java','spring-boot','ddd','jpa'] },
  { id: 'ddd-mybatis-plus-redis',      name: 'DDD + MyBatis-Plus + Redis',     desc: 'DDD + MyBatis-Plus + Redis 缓存，高性能复杂业务',  profiles: ['arch-ddd', 'persistence-mybatis-plus', 'persistence-redis'], tags: ['java','spring-boot','ddd','redis'] },
  { id: 'ddd-full-stack',              name: 'DDD 全技术栈',                   desc: 'DDD + MyBatis-Plus + Redis + ES，搜索/缓存/持久化', profiles: ['arch-ddd', 'persistence-mybatis-plus', 'persistence-redis', 'persistence-elasticsearch'], tags: ['java','spring-boot','ddd','redis','es'] },
  { id: 'vue-frontend',                name: 'Vue 前端',                       desc: 'Vue 3 + TypeScript 前端项目',                       profiles: ['frontend-vue'], tags: ['typescript','vue','frontend'] }
];

// 互斥规则（取自 _composition-presets.yaml conflicts）
const CONFLICTS = [
  { a: 'arch-mvc',                 b: 'arch-ddd',                 reason: 'MVC 三层与 DDD 四层是互斥的架构风格，同一项目只能选一种' },
  { a: 'persistence-mybatis-plus', b: 'persistence-mybatis',      reason: 'MyBatis-Plus 是 MyBatis 的增强版，不应同时使用' },
  { a: 'persistence-mybatis-plus', b: 'persistence-jpa',          reason: '同一项目不应同时使用 MyBatis-Plus 和 JPA' },
  { a: 'persistence-mybatis',      b: 'persistence-jpa',          reason: '同一项目不应同时使用 MyBatis 和 JPA' }
];

// Skill 引擎九阶段（取自 skill/SKILL.md）
const ENGINE_STAGES = [
  { no: 1, title: '读取项目清单',     detail: '读取 .spec/spec-manifest.yaml，获取项目名称、架构、技术栈、领域标签、HTTP 模式、保护配置。' },
  { no: 2, title: '读取资产与术语',   detail: '读取 project-inventory.yaml（已有工具类/API/组件）与 glossary.yaml（统一术语），避免重复造轮子。' },
  { no: 3, title: '解析项目指纹',     detail: '从清单中提取 architecture / tech_stack / domains / http_mode / protection 等维度，建立加载依据。' },
  { no: 4, title: '构建规范目录',     detail: '加载 routing-rules.yaml，只读索引（几百字节/份），不加载全文，节省上下文窗口。' },
  { no: 5, title: '声明变更边界',     detail: '划分核心变更区 / 关联影响区 / 禁止触碰区；测试与工具类默认禁止触碰，超出边界须暂停报告。' },
  { no: 6, title: '按需加载规范',     detail: '根据关键词匹配 + 文件路径匹配 + 强制加载，动态拉取相关规范的完整内容。' },
  { no: 7, title: '防重复检查',       detail: '创建新代码前先查 inventory：完全相同→复用；相似→扩展；同领域→新增方法；确无→新建并登记。' },
  { no: 8, title: '应用规范生成',     detail: 'MUST 强制执行、SHOULD 默认遵守、MAY 按需参考；每条应用都告知开发者对应的规则编号。' },
  { no: 9, title: '更新项目资产',     detail: '新建的工具类/API/组件写回 inventory，新术语写回 glossary，受保护文件的修改记入变更日志。' }
];

// 迭代管理：4 层角色
const GOV_ROLES = [
  { code: 'Contributor',    name: '贡献者',       desc: '普通开发人员，可提交补丁级提案' },
  { code: 'Maintainer',     name: '维护者',       desc: '规范日常维护，可审批补丁/次版，可执行合并' },
  { code: 'Lead Maintainer',name: '首席维护者',   desc: '管理委员会主席，唯一可执行主版本合并的角色' },
  { code: 'Reviewer',       name: '评审委员',     desc: '管理委员会成员，审批主版本变更' }
];

// 迭代管理：自动校验项（14 项，取自 GOVERNANCE.md）
const GOV_CHECKS = [
  { name: '提案文件存在性',     cat: '结构',  level: 'ERROR' },
  { name: 'YAML 语法检查',      cat: '格式',  level: 'ERROR' },
  { name: '冲突标记检查',       cat: '格式',  level: 'ERROR' },
  { name: 'registry 一致性',    cat: '一致性',level: 'ERROR' },
  { name: '架构方案模块完整性', cat: '一致性',level: 'ERROR' },
  { name: '路由规则完整性',     cat: '一致性',level: 'ERROR' },
  { name: '规则 ID 唯一性',     cat: '一致性',level: 'ERROR' },
  { name: '双向引用一致性',     cat: '一致性',level: 'ERROR' },
  { name: '版本号合规',         cat: '版本',  level: 'ERROR' },
  { name: '依赖完整性',         cat: '完整性',level: 'ERROR' },
  { name: 'CHANGELOG 更新',     cat: '审计',  level: 'WARN'  },
  { name: '受保护文件检查',     cat: '安全',  level: 'ERROR' },
  { name: '占位符完整性',       cat: '一致性',level: 'WARN'  },
  { name: '权限校验',           cat: '安全',  level: 'ERROR' }
];

// 变更类型与审批流程
const GOV_CHANGE_TYPES = [
  { type: 'patch', trigger: '修正示例/拼写/格式',   approval: '1 名维护者评审 + CI 通过',     executor: '任意维护者' },
  { type: 'minor', trigger: '新增规则/新增模块',     approval: '2 名维护者评审 + CI 通过',     executor: '指定维护者' },
  { type: 'major', trigger: '结构调整/元数据/权限',  approval: '管理委员会全员通过 + CI 通过',  executor: '首席维护者' }
];

// 体系数据看板
const STATS = [
  { num: 9,  unit: '份',   label: 'L0 通用规范' },
  { num: 11, unit: '个',   label: 'L1 原子化包' },
  { num: 1,  unit: '个',   label: 'L2 领域插件' },
  { num: 37, unit: '个',   label: '注册模块总数' },
  { num: 9,  unit: '阶段', label: 'Skill 引擎流程' },
  { num: 14, unit: '项',   label: '自动校验规则' },
  { num: 4,  unit: '层',   label: '角色权限体系' }
];

// 导出为全局（无需构建工具）
window.SPEC_DATA = {
  L0_SPECS, L1_PACKAGES, L2_SPI, PRESETS, CONFLICTS,
  ENGINE_STAGES, GOV_ROLES, GOV_CHECKS, GOV_CHANGE_TYPES, STATS
};
