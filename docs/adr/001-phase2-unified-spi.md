# ADR 001: Phase 2 — 收敛 8 SPI 为 PluginPackage + Contribution sealed

- **Status**: Accepted
- **Date**: 2026-04-19
- **Commit**: `7859a347`

## Context

项目到 Phase 1 时有 8 个并列的插件 SPI 接口, 每个都有自己的 @Component 扫描 + Registrar 落表:

- `PluginManifest` — 行业包元数据
- `EntityTypePlugin` — 实体类型 (user/org/place 子类型)
- `RelationTypePlugin` — 关系类型
- `MessagingDomainPlugin` — 触发点 + 事件类型 + 默认触发器三合一
- `PermissionProvider` — 权限码
- `RolePresetPlugin` — 预置角色
- `DataScopePlugin` — 数据范围维度
- `MenuContributionPlugin` — 前端菜单

问题:
- **新增插件要写 6~7 个 @Component 小类**, 即使只贡献 2-3 种东西
- Registrar 之间有一致性漏洞 (`ArchUnitPluginArchitectureTest` 要求每种 SPI impl 都 `@Component`, 容易忘记)
- 治理 API (enable/disable/uninstall) 必须枚举 7 个表, 每加一种就改 7 处
- 跨插件冲突检测分散在 7 个 Registrar, 语义不一致

## Decision

引入**顶层协议** `PluginPackage`, 内含一个 `Stream<Contribution> contribute()` 方法. `Contribution` 是 sealed interface, permits 8 个 record 子类型, 一对一包装原 7 个 SPI 的 def 类型 + 1 个扩展位.

```java
public sealed interface Contribution permits
    EntityTypeContribution, RelationTypeContribution, EventDomainContribution,
    PermissionContribution, RoleContribution, MenuContribution,
    DataScopeContribution, DomainContribution {
    String uniqueKey();
}
```

配套:
- 新 `ContributionDispatcher` @Order(60), 登记所有 contribution (当前只做冲突检测 + 日志, 不接管 UPSERT)
- 旧 7 SPI 全部打 `@Deprecated(forRemoval=false)`, 现有 @Component 实现**保持不变**
- 新插件可以通过 PluginPackage.contribute() 直接声明, 免去 7 个 @Component

## Considered Alternatives

### A. 不动 (维持 8 SPI)
否决 — 新插件摩擦太大, 治理 API 和测试规则每加一种 SPI 都要改

### B. 彻底改用 PF4J / OSGi 类加载器隔离
否决 — Java 17 生态 Security Manager deprecated, 真隔离必须走 microservices; 投入 2-3 月, 本项目无独立部署场景, ROI 负. 详见 [ADR 003 / MF POC findings](../../plans/poc-module-federation-findings.md)

### C. 改 PluginPackage 但**立刻下线**旧 7 SPI
否决 — 32 个 Education Plugin 实例一口气迁移风险极高. 选 @Deprecated + 双路并行, 未来会话慢慢迁

## Consequences

### 正向
- 新插件 (HEALTH) 可以只写一个 `HealthcareManifest` + contribute() 搞定几种声明
- `Contribution.uniqueKey()` 给跨插件冲突检测统一语义
- 治理 API 围绕一个 `PluginPackage` bean 展开, 加新行业无需改治理代码
- ArchUnit 规则"每种 SPI impl 必须 @Component" 在新 PluginPackage 下自然消失 (只扫 PluginPackage @Component)
- `UnifiedPluginPackageTest` 11 条守护 (sealed permits 数量/deprecated/metadata 桥接等) 防回退

### 负向
- **双路径并行**: 同一个插件既可通过旧 SPI @Component 又可通过 PluginPackage.contribute() 声明. 临时阶段复杂度 ↑
- `ContributionDispatcher` 当前是 "登记不下发" 的空壳, 需要 Phase 7+ 真正接管 UPSERT 才彻底去掉旧 Registrar
- 旧 Registrar 仍然存在, 启动日志行数没减少

### 跟进项 (在后续 phase 解决)
- Phase 7.1: 加第 9 种 `RouteContribution` (前端路由声明式)
- Phase 7.5: `PluginConfigSchema` 作为 PluginPackage 另一个默认方法
- 未来 Phase: 把 EDU / CORE 逐个插件从旧 @Component 迁到 PluginPackage.contribute()
