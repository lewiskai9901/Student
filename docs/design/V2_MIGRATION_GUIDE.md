# V2 API Migration Guide

本指南说明如何从 V1 API 平滑迁移到 V2 DDD 架构 API。

## 架构对比

| 特性 | V1 (传统分层) | V2 (DDD六边形) |
|------|--------------|----------------|
| 业务逻辑 | Service层 | Domain层 (聚合根) |
| 数据访问 | MyBatis Mapper | Repository接口 |
| 状态管理 | 数据库字段 | 领域状态机 |
| 事件处理 | 无 | 领域事件 |
| API路径 | `/api/*` | `/api/v2/*` |

## 新增领域

### 1. Task 领域 (`/api/v2/tasks`)

**聚合根:** `Task`
- 支持批量分配 (一个任务多个执行人)
- 状态机: PENDING_ACCEPT → IN_PROGRESS → SUBMITTED → APPROVED/REJECTED → COMPLETED
- 领域事件: TaskCreatedEvent, TaskAssignedEvent, TaskSubmittedEvent, etc.

**API 端点:**
```
GET    /api/v2/tasks                    # 查询任务列表
POST   /api/v2/tasks                    # 创建任务
GET    /api/v2/tasks/{id}               # 获取任务详情
POST   /api/v2/tasks/{id}/assign        # 分配任务
POST   /api/v2/tasks/{id}/accept        # 接受任务
POST   /api/v2/tasks/{id}/submit        # 提交任务
POST   /api/v2/tasks/{id}/approve       # 审批通过
POST   /api/v2/tasks/{id}/reject        # 审批拒绝
DELETE /api/v2/tasks/{id}               # 取消任务
```

### 2. Rating 领域 (`/api/v2/ratings`)

**聚合根:** `RatingConfig`, `RatingResult`
- 评比配置管理
- 评比结果状态机: DRAFT → PENDING_APPROVAL → APPROVED → PUBLISHED
- 支持撤销已发布的结果

**API 端点:**
```
# 配置管理
GET    /api/v2/ratings/configs          # 查询配置列表
POST   /api/v2/ratings/configs          # 创建配置
GET    /api/v2/ratings/configs/{id}     # 获取配置
PUT    /api/v2/ratings/configs/{id}     # 更新配置
DELETE /api/v2/ratings/configs/{id}     # 删除配置

# 结果管理
GET    /api/v2/ratings/results          # 查询结果列表
POST   /api/v2/ratings/results/calculate # 计算评比结果
POST   /api/v2/ratings/results/{id}/submit  # 提交审核
POST   /api/v2/ratings/results/{id}/approve # 审核通过
POST   /api/v2/ratings/results/{id}/reject  # 审核拒绝
POST   /api/v2/ratings/results/{id}/publish # 发布结果
POST   /api/v2/ratings/results/{id}/revoke  # 撤销发布
```

## 特性开关

V2 API 支持渐进式发布，通过特性开关控制：

### 配置方式

```yaml
feature:
  toggles:
    v2-rating-api:
      enabled: true
      rollout-percentage: 50  # 50%用户启用
      whitelist-users: [1, 2, 3]  # 白名单用户
    v2-task-api:
      enabled: true
      rollout-percentage: 100
```

### 管理 API

```bash
# 查看所有特性状态
GET /api/v2/features

# 检查特性是否启用
GET /api/v2/features/{name}?userId=123

# 启用特性
POST /api/v2/features/{name}/enable

# 禁用特性
POST /api/v2/features/{name}/disable

# 设置灰度比例
POST /api/v2/features/{name}/rollout?percentage=30
```

## 回滚策略

### 快速回滚

1. **通过 API 禁用:**
   ```bash
   curl -X POST /api/v2/features/v2-rating-api/disable
   curl -X POST /api/v2/features/v2-task-api/disable
   ```

2. **通过配置文件:**
   ```bash
   # 启动时使用回滚配置
   java -jar app.jar --spring.profiles.active=rollback
   ```

3. **通过环境变量:**
   ```bash
   FEATURE_TOGGLES_V2_RATING_API_ENABLED=false java -jar app.jar
   ```

### 回滚检查清单

- [ ] 禁用 V2 特性开关
- [ ] 检查错误日志
- [ ] 验证 V1 API 功能正常
- [ ] 监控性能指标
- [ ] 通知相关团队

## 性能监控

### 监控端点

```bash
# 查看方法性能统计
GET /api/v2/performance/stats

# 查看慢方法
GET /api/v2/performance/stats/slow

# 系统健康状态
GET /api/v2/performance/health

# 重置统计
POST /api/v2/performance/stats/reset
```

### 使用 @Monitored 注解

```java
@Monitored(threshold = 200, description = "Load user data")
public User loadUser(Long id) {
    // ...
}
```

## 缓存策略

### 缓存层级

| 缓存名 | TTL | 用途 |
|--------|-----|------|
| statistics | 5分钟 | 统计数据 |
| user | 30分钟 | 用户信息 |
| permission | 30分钟 | 权限数据 |
| role | 30分钟 | 角色数据 |
| config | 2小时 | 系统配置 |
| template | 2小时 | 检查模板 |
| department | 2小时 | 部门数据 |
| class | 2小时 | 班级数据 |
| ratingConfig | 2小时 | 评比配置 |

### 缓存使用

```java
// 使用 Spring Cache 注解
@Cacheable(value = "user", key = "#id")
public User findById(Long id) { ... }

// 使用 CacheService
cacheService.getOrLoad("user:" + id, Duration.ofMinutes(30), () -> userRepo.find(id));

// 失效缓存
cacheService.evict("user:" + id);
cacheService.evictByPattern("user:*");
```

## 迁移步骤

### 阶段1: 并行运行 (当前)

- V1 和 V2 API 同时可用
- 通过特性开关控制 V2 访问
- 监控 V2 API 性能和错误率

### 阶段2: 渐进切换

1. 设置 V2 灰度比例为 10%
2. 监控1周，确认稳定
3. 逐步提升: 25% → 50% → 75% → 100%

### 阶段3: V1 废弃

1. V2 灰度比例 100%
2. V1 API 标记 @Deprecated
3. 前端完成 V2 API 迁移
4. 设置 V1 API 过期时间

### 阶段4: V1 移除

1. 移除 V1 Controller
2. 移除 V1 Service
3. 清理遗留代码

## 前端适配

### API 客户端更新

```typescript
// api/v2/rating.ts
export const ratingApi = {
  getConfigs: () => axios.get('/api/v2/ratings/configs'),
  createConfig: (data) => axios.post('/api/v2/ratings/configs', data),
  submitResult: (id) => axios.post(`/api/v2/ratings/results/${id}/submit`),
  // ...
};
```

### 特性检测

```typescript
// utils/feature.ts
export async function isV2Enabled(feature: string): Promise<boolean> {
  const { data } = await axios.get(`/api/v2/features/${feature}`);
  return data.data;
}

// 使用
if (await isV2Enabled('v2-rating-api')) {
  // 使用 V2 API
} else {
  // 回退到 V1 API
}
```

## 问题排查

### 常见问题

1. **V2 API 返回 503**
   - 检查特性开关是否启用
   - 检查用户是否在灰度范围内

2. **状态转换失败**
   - 检查当前状态是否允许该转换
   - 查看 `allowedTransitions` 字段

3. **缓存不一致**
   - 调用 `cacheService.evictByPattern()` 清理
   - 检查 TTL 配置

### 日志查看

```bash
# V2 API 日志
grep "interfaces.rest" logs/app.log

# 领域事件日志
grep "domain.*Event" logs/app.log

# 性能监控日志
grep "SLOW" logs/app.log
```

## 联系方式

如有问题，请联系:
- 技术负责人: xxx
- 架构师: xxx
