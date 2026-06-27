# model-design — API 契约

> 最后更新：2026-06-23
> 关联模块：model-design

> **本节为占位**。API 详细约定与列表随模块迭代补充，全局约定见 [`../../api/overview.md`](../../api/overview.md)。

## 路径前缀

```
/api/v1/model-design/{entity}
```

## 接口列表

| 接口 | 方法 | 路径 |
|---|---|---|
| 模型 CRUD | list / get / create / update / delete | `/model` |
| 主题域 | list / 树形 | `/topic` |
| 属性 | list / get / create / update / delete | `/attribute` |
| 编码规则 | list / get / create / update / delete | `/coding-rule` |
| 质量规则 | list / get / create / update / delete | `/quality-rule` |
| 相似度规则 | list / get / create / update / delete | `/similarity-rule` |
| 填报设计 | list / get / create / update / delete | `/form-design` |

详见源码：`apps/mdm-model/src/modules/model-design/api/`。
