| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 层级 | L1 |
| 包类型 | persistence |
| 引入条件 | fingerprint.profiles contains 'persistence-elasticsearch' |
| 适用场景 | 使用 Elasticsearch 作为搜索引擎或日志存储的项目 |
| 依赖规范 | spring-boot-base |

# Elasticsearch 持久化规范

本包定义 Elasticsearch 在项目中的使用约定，包括索引设计、查询规范与性能优化。

## 索引设计

**PROF-ES-001** 索引名 MUST 使用小写 + 中划线分隔：`{项目}-{模块}-{环境}`。 [MUST]

```
myapp-order-dev
myapp-log-prod
myapp-product-search-dev
```

**PROF-ES-002** 索引 MUST 显式定义 Mapping，MUST NOT 依赖动态映射（Dynamic Mapping）。 [MUST]

```json
{
  "mappings": {
    "properties": {
      "orderId": { "type": "keyword" },
      "orderNo": { "type": "keyword" },
      "userId": { "type": "keyword" },
      "status": { "type": "keyword" },
      "totalAmount": { "type": "scaled_float", "scaling_factor": 100 },
      "items": {
        "type": "nested",
        "properties": {
          "productId": { "type": "keyword" },
          "quantity": { "type": "integer" },
          "price": { "type": "scaled_float", "scaling_factor": 100 }
        }
      },
      "createTime": { "type": "date", "format": "yyyy-MM-dd HH:mm:ss||epoch_millis" }
    }
  }
}
```

**PROF-ES-003** 字段类型 MUST 根据查询方式选择：精确匹配用 `keyword`，全文搜索用 `text`，MUST NOT 混淆。 [MUST]

**PROF-ES-004** 嵌套对象（内部数组）MUST 使用 `nested` 类型，MUST NOT 使用默认的 `object` 类型（会导致查询错误）。 [MUST]

## 查询规范

**PROF-ES-005** 查询 MUST 使用 `bool` 查询组合条件，MUST NOT 使用已废弃的 `filtered` 查询。 [MUST]

**PROF-ES-006** 精确匹配 MUST 使用 `term` / `terms`，全文搜索 MUST 使用 `match` / `multi_match`。 [MUST]

**PROF-ES-007** 分页查询 MUST 使用 `from` + `size`（浅分页）或 `search_after`（深分页），MUST NOT 使用 `from` 超过 10000。 [MUST]

```java
// 浅分页
SearchRequest request = new SearchRequest("myapp-order-dev");
request.source()
    .query(QueryBuilders.boolQuery()
        .must(QueryBuilders.termQuery("status", "CONFIRMED"))
        .filter(QueryBuilders.rangeQuery("createTime").gte("2024-01-01")))
    .from(0)
    .size(20)
    .sort("createTime", SortOrder.DESC);

// 深分页（search_after）
request.source()
    .searchAfter(new Object[]{lastCreateTime, lastOrderId})
    .size(100);
```

## 性能优化

**PROF-ES-008** 写入 SHOULD 使用 Bulk API 批量提交，MUST NOT 逐条索引。 [SHOULD/MUST]

**PROF-ES-009** 查询结果 MUST 仅返回所需字段（`_source` 过滤），MUST NOT 返回整个 `_source`。 [MUST]

**PROF-ES-010** 聚合查询 SHOULD 使用 `filter` 上下文而非 `query` 上下文，提升缓存命中率。 [SHOULD]

## 与数据库同步

**PROF-ES-011** 数据库到 ES 的同步 SHOULD 使用领域事件或 Canal 监听 binlog，MUST NOT 在业务代码中直接双写。 [SHOULD/MUST]

**PROF-ES-012** 同步操作 MUST 保证幂等性，同一文档多次写入结果一致。 [MUST]

## 客户端规范

**PROF-ES-013** MUST 使用 Elasticsearch 官方 Java Client（`co.elastic.clients:elasticsearch-java`），MUST NOT 使用已废弃的 Transport Client 或 RestHighLevelClient。 [MUST]

**PROF-ES-014** 客户端配置 MUST 包含连接超时、Socket 超时和重试策略。 [MUST]
