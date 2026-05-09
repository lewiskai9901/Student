# 关系管理性能基准

Phase 7 W7.5 — 关系管理 (`access_relations`) 关键路径性能基线.

## 微基准 (JMH)

跑命令:
```
cd backend
mvn test -Dtest=AccessRelationCheckBenchmark#runBenchmark
```

JMH benchmark 默认 `@Disabled`, 不进 `mvn test` 默认流程. 显式 `-Dtest` 才跑.

### 覆盖项

- `AccessLevel.parse(validString)` — 大写枚举解析
- `AccessLevel.parse(lowerCase)` — 小写解析(走 toUpperCase 路径)
- `AccessLevel.parse(invalid)` — 异常路径(返默认值)
- `AccessLevel.FULL.isReadWrite()` — 状态查询

### 预期吞吐量(参考, 16-core x86_64, JDK 17)

- `AccessLevel.parse(validString)` — > 100M ops/sec
- `AccessLevel.parse(invalid)` — > 80M ops/sec(异常路径)
- `AccessLevel.FULL.isReadWrite()` — > 200M ops/sec

> 这些是纯函数级 micro-benchmark, **不代表生产 check() / expand() 性能**.
> 生产路径含 Redis / MySQL / BFS 展开 / @DataPermission 拦截器, 须用端到端压测.

## 端到端基准 (JMeter / Gatling, 留生产 SRE)

### 目标 SLA

| API | P95 | P99 | 1000 RPS 稳态 |
|---|---|---|---|
| `check(subject, relation, resource)` | < 10ms | < 30ms | 单实例 OK (Redis 缓存命中率 > 90%) |
| `expand(resource, relation)` | < 50ms | < 150ms | 看资源关系数 |
| `grant(...)` | < 100ms | < 300ms | 含审批旁路写 pending |
| `revoke(...)` | < 100ms | < 300ms | 归档 + 软删两次 UPDATE |
| `lookup(subject, relation, resourceType)` | < 30ms | < 100ms | 走 access_relations 索引 |

### 压测 fixture 数据规模

- 100 万 access_relations 行
- 1000 个 active subject (user)
- 1000 个 resource (org_unit + place)
- 10 种 relation_code
- 平均每 user 50 条直接关系 + 5 层 implied 推导

### 关键观测

跑压测时同步开 Grafana 看板:
- `access_relation_check_total{result, cache}` — 命中率
- `access_relation_check_seconds` — 延迟分布
- `access_relation_grant_total{relation, tier}` — grant 分布
- 数据库: `Innodb_row_lock_time_avg`, `Slow_queries`
- Redis: `cache hit ratio`, `evicted_keys`

### 调优建议

如果 P95 超阈值:
1. 看 Redis 命中率 — 低于 80% 调大 `accessCheck.cache.ttl-seconds`
2. 看 implied BFS 深度 — 看是否声明了无意义环, 必要时 `MAX_IMPLIED_DEPTH` 调小
3. 看 `access_relations` 索引 — 应有 `(subject_type, subject_id, relation, deleted)` 复合
4. 看 N+1 — 批量 `batchCheck` 应替代多次 `check`
