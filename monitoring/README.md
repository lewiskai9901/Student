# Monitoring (Phase 8.3)

## 启动本地 Prometheus + Grafana

```bash
# 假设 backend 跑在 host.docker.internal:8080 (WSL/Windows) 或 localhost:8080 (Linux)
docker compose -f monitoring/docker-compose.yml up -d
```

## Prometheus scrape config

`monitoring/prometheus.yml`:

```yaml
global:
  scrape_interval: 15s
scrape_configs:
  - job_name: student-management
    metrics_path: /api/actuator/prometheus
    static_configs:
      - targets: ['host.docker.internal:8080']
```

## Grafana dashboards

Import from `monitoring/grafana/dashboards/*.json`:

| File | 用途 |
|---|---|
| `jvm-overview.json` | JVM 内存/线程/GC — 通用健康 |
| `http-per-plugin.json` | API QPS 按 plugin industry 分组 (需埋点) |
| `plugin-platform.json` | ContributionDispatcher 扫描统计 / 插件 enable 状态 |

dashboard 用到的关键 metric (由 Spring Boot Actuator / Micrometer 原生提供):

- `jvm_memory_used_bytes{area=heap,...}`
- `http_server_requests_seconds_count{uri, method, status}`
- `jvm_threads_live_threads`
- `process_cpu_usage`
- `hikaricp_connections_active` (如用 HikariCP, Druid 有类似)

业务维度 (Phase 8.3 未全埋点, 留 TODO):
- `plugin_loaded_total{code}` — 按行业统计装载的 Contribution 数
- `plugin_enabled{code}` — gauge, 0/1 表示插件是否启用

## 接入生产

prod 扔给 Prometheus operator / VictoriaMetrics / 云厂商托管即可. scrape 路径稳定.
