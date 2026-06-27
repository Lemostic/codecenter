# model-design — 设计文档

> 最后更新：2026-06-17
> 关联模块：model-design

> **本节为占位**。完整设计随模块迭代补充。

## 1. 实体模型

- **Topic**（主题域）：树形结构，支持层级分类
- **Model**（主数据模型）：核心建模单元，含版本与状态机
- **Attribute**（属性）：模型下的字段定义
- **CodingRule**（编码规则）：模型编码生成规则
- **QualityRule**（质量规则）：数据质量校验规则
- **SimilarityRule**（相似度规则）：查重 / 合并规则
- **FormDesign**（填报设计）：动态表单定义

## 2. 状态机

- Model: `draft → published → archived`
- Attribute: `enabled / disabled`
- FormDesign: `draft / published`

## 3. 页面 / 组件

- `ModelList.vue` / `ModelEditor.vue` / `ModelDetail.vue`
- `TopicList.vue`
- `ModelAttributeEditor.vue`
- 子模块：`coding-rule/` / `quality-rule/` / `similarity-rule/` / `form-design/`

## 4. Composables / Store

- `useModelDesignList`
- `useAttributeEditor`
- 模块 store：`useModelDesignStore`
