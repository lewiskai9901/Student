# Tier 5 + Tier 6 业务深测结果

**执行日期**: 2026-05-13
**总耗时**: ~2.5 小时(规划 + 写 spec + 跑 + 修)
**起点**: master `ec62057c` (部署收官)

## TL;DR

补齐 **6 个高价值 spec + 1 个 smoke 脚本**,覆盖之前 5+11+5 commit 修过但缺 e2e 守护的核心业务路径。
共 **22 个 test cases 全绿**(5 frontend e2e + 1 frontend e2e + 5 frontend e2e + 3 frontend e2e + 3 frontend e2e + 4 miniprogram vitest + 1 smoke 脚本)。

发现 **1 个真实契约 mismatch bug**:`JacksonConfig` 全局 Long→string 策略导致前端 types 标 `number` 实际收到 `string`。已记录,本轮通过容错断言通过测试。

## 新增测试文件

| # | 文件 | 测试数 | 守护范围 |
|---|---|---|---|
| A-1 | `frontend/e2e/inspection-template-drift.spec.ts` | 5 | 模板漂移检测 + 升级幂等 + 多模板项目 |
| A-2 | `frontend/e2e/inspection-appeal-concurrency.spec.ts` | 3 | 并发申诉 UNIQUE INDEX 防竞态 |
| A-3 | `frontend/e2e/inspection-project-policy.spec.ts` | 3 | 项目级策略(max_reject/appeal_window) 改即生效 |
| A-4 | `frontend/e2e/inspection-audit-logs.spec.ts` | 4 | 审计日志写 + 查 + 过滤 + 上限裁剪 |
| A-5 | `frontend/e2e/inspection-batch-tasks.spec.ts` | 3 | 离职检查员重派 + fallback 校验 + partial-success |
| B-6 | `miniprogram/src/__integration__/backend-contract.test.ts` | 4 | 真 backend 联调契约 (login/me/my-tasks/my-appeals) |
| B-7 | `miniprogram/scripts/smoke-h5.sh` | 1 (脚本) | health + login + contract test 一键 |

**总计**: 22 test cases / 6 new spec files / 1 shell smoke + npm script `smoke:h5`

## 通过率

```
Block A (Tier 5):  18 test  ✓ 18 passed   (1 修后通过 + 1 flaky retry 后稳定)
Block B (Tier 6):   4 test  ✓  4 passed   (修 1 个 Long→string 类型断言)
Smoke (B-7):        1 脚本  ✓ 全 3 步通过
```

## 关键发现

### 1. Jackson 全局 Long→string 序列化策略导致 typed 契约 mismatch

**位置**: `backend/src/main/java/com/school/management/config/JacksonConfig.java:37-38`

```java
longModule.addSerializer(Long.class, ToStringSerializer.instance);
longModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
```

**影响**:
- 所有 `Long id / userId / projectId / templateId / submissionDetailId / expiresIn` 在 HTTP 响应中实际是 **string**
- frontend `types/inspection.ts` 和 miniprogram `types.ts` 标的 `number` 是**错的**
- 没炸是因为 JS `==` 自动 coerce 不严格相等;但 `typeof v === 'number'` 全部失败
- typed SDK 自动化(OpenAPI generation)会直接撞上这个

**当前处理**:
- B-6 spec 用 `expectNumericId()` helper 兼容 string|number 都接受
- **未改 backend**(设计决策合理 — 防 JS 53-bit 雪花 ID 精度丢失)
- **未改 frontend types**(大工程,~50+ interfaces)

**后续行动建议**:
- 短期: 在 `frontend/src/types/` 写 helper type `LongId = number | string`,新代码用它
- 中期: axios response interceptor 把已知 Long 字段 string → number(仅当 < MAX_SAFE_INTEGER)
- 长期: 跟 OpenAPI codegen 一起设计 Long 包装类型

### 2. 冷启动 race 导致前端 e2e 偶发 flake

Block A-1 首次跑 4 个 test 都因 `waitForFunction(!/login)` 超 30s 而失败,retry 后全过。
**根因**: Playwright 4 worker 并发登录 → backend 第一波 30+ 请求让 token 颁发慢。
**缓解**: 5 个 spec 都 `retries: 1`,且后续 4 个 spec 跑 `--workers=2` 后无 flake。
**长期**: 用 Playwright `storageState` 缓存登录态,跳过 beforeEach 重复登录。本轮未实施。

### 3. UNIQUE INDEX pending_lock_key 真实工作

A-2 并发起 2 个相同申诉,**确实只有 1 个 200,1 个 400/500**。证实:
- `inspection_appeals.pending_lock_key` 生成列 + UNIQUE INDEX 起作用
- DataIntegrityViolationException 被业务层捕获并返回结构化错误信息(文案含"重复"/"已存在"等)

### 4. 离职重派 fallback 用户存在性校验真实工作

A-5 用 `fallbackInspectorId=99999999`(必不存在)POST,backend 立即抛
`IllegalArgumentException: fallbackInspectorId 对应用户不存在或已删除`。

## 跑法

```bash
# 启 backend
cd backend && JAVA_HOME=/c/Program\ Files/Java/jdk-17 \
  PATH=$JAVA_HOME/bin:/c/Program\ Files/apache-maven-3.9.11/bin:$PATH \
  mvn spring-boot:run -DskipTests -Dspring-boot.run.profiles=dev

# Tier 5 Block A — frontend e2e (Playwright 自动起 frontend dev server)
cd frontend && npx playwright test e2e/inspection-template-drift.spec.ts \
  e2e/inspection-appeal-concurrency.spec.ts \
  e2e/inspection-project-policy.spec.ts \
  e2e/inspection-audit-logs.spec.ts \
  e2e/inspection-batch-tasks.spec.ts --workers=2

# Tier 6 Block B — miniprogram smoke (真打 backend)
cd miniprogram && npm run smoke:h5
```

## 未做(可作下轮范围)

- ~~Tier 5 完整 happy path~~ (模板创建→任务派发→打分→申诉→整改) — 价值偏低,各步骤已分散覆盖
- ~~UI 视觉守护~~ — `inspection-visual-snapshot.spec.ts` 已有
- Playwright `storageState` 重构(节省 5×3=15s/run cold-start)— 性能优化
- frontend types Long→string 全局重构 — 大工程,单独成项
- miniprogram dev:h5 真启动 + Playwright 模拟操作 — 与 Tier 5 chromium e2e 重叠度高

## 评估

业务 e2e 守护层从 "5 个 spec 覆 UI 骨架 + AD_HOC 任务" 推到 "10 个 spec 覆盖 11-commit 修过的全部高风险路径 + 真后端契约"。

**Tier 5 业务 A-**(已修 bug 全有 e2e 守护;UI 完整流程未做 happy path)
**Tier 6 多端 B+**(契约层 vitest 跑通;真 h5 UI 自动化未做)

下一步可选:
1. Playwright `storageState` 重构(< 0.5 天)
2. 全局 Long→string 类型契约重设(2-3 天大工程)
3. 真上 docker + 跑全套 e2e in CI(deployment-readiness #1 follow-up)
