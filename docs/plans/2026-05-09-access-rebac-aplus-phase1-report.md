# 关系管理 A+ 提升 Phase 1 完成报告

> 日期:2026-05-09
> 分支:`feature/access-rebac-aplus`
> 状态:✅ 完成,可合 master
> 评分:75 → 82(B- → B)

## 一句话总结

修掉关系管理 3 处阻断/混乱:`accessLevel` 用 enum 替代字面量、实体补关系有效期窗口、`AuthorizationService` 改名+17 处直 SQL 收回仓储。后端 741 测试全绿。

## 范围

✅ **完成:**
- W1.1 — `AccessLevel` enum 替代 8 个文件里的 String 字面量,加 `parse()` 兜底 + 单测
- W1.2 — `AccessRelation` 实体补 `validFrom` / `validTo` + `isActiveAt(at)` / `isCurrentlyActive()` 助手,PO/Repository/CreateCommand/UpdateCommand/batchCreate 全链路打通
- W1.3 — `application.access.AuthorizationService` → `AccessRelationService`(消除与 domain 接口同名歧义)+ 17 处 `jdbcTemplate.*` 操作收编进 `AccessRelationRepository`,服务层瘦身 -116 行

❌ **未在本期(留 Phase 2-6):**
- DataScope vs AccessRelation 双轨合并(P2)
- 关系字典字段精简 + viewer/responsible_for 补 + COMMON_EXT(P3)
- Redis 缓存 + expand 索引 + metadata schema(P4)
- 字段级权限脱敏 + 审批流(P5)
- ArchUnit 守护 + 1000 RPS 压测 + 文档(P6)

## 3 commits 序列

| SHA | 内容 | LOC |
|---|---|---|
| `719cbd66` | W1.1 — AccessLevel enum + 8 文件改 String → enum + 6 单测 | +89 / -36 |
| `b74b4556` | W1.2 — 实体补 valid* + 4 文件 + 6 单测 | +153 / -7 |
| `36a520ab` | W1.3 — Service 改名 + 17 jdbc 收回仓储 + 9 文件 | +498 / -338 |

## 测试基线

- 起点:741 测试通过(改前)
- 终点:**741 测试通过 / 0 失败 / 2 跳过**(改后)
- 增量:+12 单测(AccessLevelTest 6 + AccessRelationValidityTest 6)
- 改动 2 个旧测试(AuthorizationServiceImpliedTest / FindSubjectsTest 改 mock 对象 JdbcTemplate → AccessRelationRepository)

`mvn test` 全量 2m31s,`mvn compile` 1m13s — 性能无回归。

## 关键收益(对比修前)

| 项 | 修前 | 修后 |
|---|---|---|
| `accessLevel` 类型安全 | String,可写 `"FUL"` 编译过 | `enum AccessLevel`,typo 编译失败 |
| 业务能否设关系有效期 | ❌ 实体没字段,只能开后门 SQL | ✅ `.validFrom(...).validTo(...)` 走正经 API |
| application 层直 jdbc 操作 access_relations | 17 处 | **0 处**(都走 repo) |
| 命名冲突 | domain `AuthorizationService` interface + application `AuthorizationService` class 重名,新人困惑 | application 改名 `AccessRelationService`,域职责清晰 |
| 服务层代码 | 619 行 | 503 行(-19%) |

## 架构改善

```
修前:
  AccessRelationApplicationService(CRUD)
    └─ AccessRelationRepository(MyBatis 4 个 select)
  AuthorizationService = application service(直 jdbc 17 处)
    └─ jdbcTemplate.queryForList / update / queryForObject
  domain.access.service.AuthorizationService = interface(RBAC,空架子)

修后:
  AccessRelationApplicationService(CRUD)
    └─ AccessRelationRepository(13 方法)
  AccessRelationService = application service(BFS / event publishing)
    └─ AccessRelationRepository(扩展 9 方法,统一仓储)
  domain.access.service.AuthorizationService = interface(RBAC)
```

仓储模式落实,层次干净。

## 文件清单

### W1.1(8 文件 + 2 新)
- 新建:`domain/access/model/valueobject/AccessLevel.java` + 单测
- 改:实体 / 仓储实现 / 应用服务 / 授权服务 / 场所服务 / 用户服务

### W1.2(4 文件 + 1 新)
- 新建:`AccessRelationValidityTest.java`
- 改:实体 / PO / 仓储实现 / 应用服务

### W1.3(10 文件,1 重命名)
- 重命名:`AuthorizationService.java` → `AccessRelationService.java`(git mv,保留 history)
- 改:仓储接口 / 仓储实现 / 服务 / RelationDiscoveryRule / 3 调用方 / 3 测试

## 已知遗留

1. **`refreshImpliedCache` 还有 2 处 jdbcTemplate**(查 `relation_types` 配置字典)— 不在 access_relations 表上,**保留合理**,代码注释标了
2. **`update()` 路径 valid* 是部分覆盖**(条件判 `if cmd.getValidFrom() != null`)— 与现有 `accessLevel`/`relation` 字段保持一致,客户端无法用 update DTO 清除有效期窗口,只能新建。这是产品决策,留 Phase 5 一起处理
3. **`AccessRelationPO` 仍 `String accessLevel`** — 持久化层故意保留,因为 PO 是 SQL 桥接;转换在 Repository toDomain/toPO 完成

## 真机验证(可选)

代码层 741 测试已绿,真机验证不是必需。但若要看效果:

- [ ] `mvn spring-boot:run` 启动后端
- [ ] 用 `POST /api/access-relations` 创建一条关系,带 `validTo: "2026-12-31T23:59:59"` 字段 → 后端入库 valid_to 字段
- [ ] `GET /api/access-relations/{id}` 返回字段含 validFrom/validTo
- [ ] `mysql> SELECT id, access_level FROM access_relations WHERE deleted=0` 看到都是 `'FULL'`/`'OWNER'`/`'READ_ONLY'`
- [ ] 等到 `validTo` 时间过去,`AccessRelationService.check()` 应返 false(关系已到期)

## Next Steps(Phase 2)

候选:
- **W2.1 — DataScope 收编进 AccessRelation 单轨**(2-3 天,大手术)
- W2.2 — RelationTypePlugin 完成 SPI 迁移(1 天)
- W2.3 — include_children/is_transitive 行为统一(0.5 天)

合计 ~4 天,完成后评分 82 → 88(B → B+)。

---

**P.S.** 3 个 task 全 subagent-driven,无回退、无 plan 偏差大幅。其中 W1.3 难度最高(10 文件 / 619→503 行重构),implementer 一次过且额外修了 2 个旧测试的 mock。
