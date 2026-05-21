# ADR-009: 多租户暂缓 — 脚手架保持休眠,不拆除

**Date**: 2026-05-21
**Status**: Deferred
**决策**: 按单租户运行;保留休眠的多租户脚手架,不彻底拆除

## Context

项目早期为"多租户/多校区部署"埋了一套脚手架:

- `tenant_id` 列加到几乎每张表 —— DB 实测 **314 个 `tenant_id` 列**
- ~210 个 PO/源文件带 `tenantId` 字段
- `infrastructure/tenant/` 的 `TenantContext` / `TenantContextHolder` / `TenantInterceptor`
- `TenantController` / `TenantApplicationService` / `tenants` 表
- `JwtAuthenticationFilter` 每请求解析 tenantId 并写入 `TenantContextHolder`
- `DataPermissionInterceptor`、`OrgScopeHelper`、`InspectionScopeHelper`、
  `MsgNotificationMapper` 等在 SQL 子查询里用 `tenant_id`
- `TenantPluginService` —— 插件启用按租户键 (`V20260422_1` 迁移)

但**全局强制层从未实装**:`TenantInterceptor` 的 `@Component` 被注释掉、
`intercept()` 是空 `TODO`。实际上全系统按 `tenant=1` 运行
(`TenantContextHolder` 无 context 时默认返回 `1L`)。

2026-05-20 的安全审计 (S 系列) 把"跨租户隔离"列为 P2;2026-05-21 业主
明确**项目不考虑多租户**。

## Decision

**保持多租户脚手架休眠,不拆除。** 按单租户运行。

### 为什么不彻底拆除

评估过"彻底移除多租户代码与 DB 列",结论是 **ROI 显著为负**:

| 维度 | 评估 |
|---|---|
| 成本 | 删 ~210 文件的字段 + DROP **314 个 DB 列**的迁移 + 重写 37 处 SQL + **重写 `DataPermissionInterceptor`**(其组织子树子查询依赖 `tenant_id`)+ 重写 `OrgScopeHelper`/`InspectionScopeHelper`/`JwtAuthenticationFilter` + 重做 `TenantPluginService` + 改 11 个测试。数天工作量。 |
| 风险 | 极高 —— 动的是每条 SQL 的数据访问路径,一处错即全平台数据访问故障。 |
| 功能收益 | **零** —— 多租户维度当前休眠、全栈一致、无害,不报错、不卡功能、不挡开发。 |
| 可逆性 | 拆掉后若再要多租户 = 推倒重建。 |

"彻底删除"原则针对的是**不一致的半成品**(删了字段留着 DB 列);而
`tenant_id` 是一个**全栈一致的维度** —— 完整保留或完整拆除,无中间态;
完整拆除的代价与收益严重不成比例。

### 实际做的(方案 A)

- `TenantInterceptor` 加明确决策注释(指向本 ADR),`@Component` 保持注释掉。
- 不动 PO 字段、不动 DB 列、不动任何 SQL —— 维持休眠现状。

## Consequences

### Positive
- 零风险、零工作量,精力可投入更高回报处(测试覆盖、e2e 等)。
- 将来若要多租户,脚手架(列/字段/上下文管道/请求解析)都还在,
  只需实装全局拦截器即可,不必从零埋点。

### Negative
- 代码库保留一套不启用的维度,对新读代码的人是一点噪音
  —— 由本 ADR + `TenantInterceptor` 注释消化。
- `tenants` 表 1 行、`TenantController` 等在单租户下无实际意义,但保留
  (拆它们会造成"租户 CRUD 没了、tenant_id 列还在"的不一致)。

### 将来真要启用多租户的触发条件

出现以下任一商业信号才重新评估:
1. 需要给不同客户/校区做**数据隔离的同实例部署**(SaaS 多租)。
2. 单实例需承载多个互不可见的组织。

届时:实装 `TenantInterceptor` 的 SQL 注入(或换 MyBatis-Plus
`TenantLineInnerInterceptor`)、补 JdbcTemplate 直查路径的 `tenant_id`
过滤、补 `@Async`/定时任务的租户上下文传递、核对存量数据 `tenant_id`
回填。详见安全审计 memory `project_s_security_audit.md` 的 P2 段。

## Related

- `infrastructure/tenant/TenantInterceptor.java` —— 决策注释
- Memory: `project_s_security_audit.md` (P2 段)
- 安全审计 S 系列 (P0/P1/P3 已修,P2 多租户隔离 = 本 ADR 决定跳过)
