# 授权加固方案 (Authorization Hardening)

> 状态：设计稿（未实现）。动因：成员 `a` 被授予"用户管理·本组织"后可改/删本组织管理者 `admin` 的越权洞，以及由此暴露的"数据范围管不了动作级护栏"的普遍缺口。

## 0. 目标与边界

**要解决**
1. 修复"下级能管上级 / 成员能管本组织管理者 / 谁都能动超管 / 能授出比自己高的角色"这类越权。
2. 通用化：补一个**动作级策略护栏层**，并增强数据范围层（读写分离 + 关系类型维度），让这类问题有统一、可扩展、且**配错也兜得住**的解法。

**明确不做**
- 不引入外部策略引擎（OPA / XACML / 重型 ABAC 进程）。
- 不做规则编排 UI（首版规则代码态）。
- 不推翻现有 RBAC(Casbin) / 数据范围 / access_relations。

## 1. 目标态：3 道授权闸

```
入口) 认证 JWT                    你是谁（大门，非授权闸）
闸1) 功能权限 RBAC (Casbin)       能不能调这个动作
闸2) 数据范围 Data Scope          能碰哪些行
       取数手段：组织树 / 创建人 / 自定义 / 关系驱动维度(access_relations)
       【本方案增强】① 读写分离 scope（读宽写窄）② 关系类型维度(含排除)
闸3) 策略护栏 Policy Engine 【新增】 这次·对这个目标·能不能下手（属性级，deny-overrides，默认放行）
```

- **ReBAC(access_relations) 不是独立一层**，是闸2 内部"算哪些行"的手段。
- 闸3 只能在前两闸放行后**再拦一刀**：默认 PERMIT + deny-overrides ⇒ 加任何规则永远是收紧、不可能意外放宽。

## 2. 防御纵深（关键设计原则）

| 角色 | 机制 | 特点 |
|---|---|---|
| 常规布线 | 闸2 关系驱动 scope（a 只管成员，排除管理者）| 直观、数据原生，但**取决于配对没配** |
| 配错兜底 | 闸3 护栏（超管/管理者/提权 永远拦）| **不管 scope 怎么配，底线都在** |

即使有人把 a 的修改范围误配成"本组织全部（含管理者）"，闸3 仍拦住 admin。**单靠 scope 会因配错重新开洞，所以两者都要。**

## 3. 分阶段方案

### P0 — 热修：用户管理护栏（独立 / 最小 / 立即可上）
不依赖新引擎，用现有 `Policy<T>` 机制（或一个 `UserManagementGuard`）在 user 的
`update / delete / role-assign / reset-password / disable` 路径前插入检查。

规则（deny 任一命中即拒）——**首版只用不依赖 level 的两条 + 防提权**：
- `protect-super-admin`：目标是超管 && 自己不是超管 → 拒
- `protect-org-manager`：目标持有"本组织管理者(admin)关系" && 自己不是该组织管理者或更高 → 拒
- `no-escalation`：授角色时，目标角色不得超出自己持有的角色集 → 拒
- `no-self-critical`：不能对自己执行 disable/delete/移除关键角色 → 拒

> `no-manage-senior`（按 rank 比较）**P0 暂不开**——因为 `role.level` 当前被当"排序"用、默认 100 全平级（见 P3）。先用关系/超管这几条不依赖 level 的兜住。

- 数据来源：`UserContextHolder`(subject) + `access_relations`(admin 关系) + 目标用户(本就 load)。
- 验收：a 改 admin → 403；a 改本组织普通成员 → 通过。
- 风险：低（纯增量拦截、默认放行）。工作量：~半天。

### P1 — 策略引擎骨架（通用化，把 P0 规则迁入）
- 组件：`AccessRequest{subject,action,resource,env}` / `PolicyRule{appliesTo,evaluate→PERMIT|DENY|N/A}` / `Decision` / `PolicyEngine.decide()`（deny-overrides，默认 PERMIT）。
- 接入：注解 `@PolicyCheck(action,resourceType)` + AOP；复杂场景显式 `policyEngine.requirePermit(req)`。
- 规则写法：**代码规则为主**（类型安全、可单测）；SpEL 配置规则留给"管理员要运行期调"的少数场景。
- 扩展：`Contribution.PolicyContribution` 让插件加规则；核心内置 P0 那几条。
- 审计：DENY 记 `activity_events`（who / action / target / ruleId）。
- `explain(req)` 干跑接口：配置界面可"模拟某人能否对某目标做某事"。
- 工作量：中。

### P2 — 数据范围增强（读写分离 + 关系类型维度）
1. **读写分离 scope**：`role_data_scopes` 增加 action 维度（READ / WRITE），或把敏感资源拆成 `xxx_view` / `xxx_manage` 两个资源码；`@DataPermission` 按 SQL 语句类型(select/update/delete)取对应 scope。→ 支持"查看本组织全部、只能改成员"。
2. **关系类型维度 `BY_RELATION`**：拦截器的 `access_relations` 子查询**加 `relation` 类型过滤 + 排除**（`NOT EXISTS admin 关系`）。新增插件 scope 维度，UI 在 user 资源上可选"本组织成员（排除管理者）"。
   - 现状缺口：当前子查询只按 `resource_type`+subject 过滤，**不看 relation 类型**，无排除。
3. **用户级 CUSTOM**：自定义范围支持按"具体用户"挑（现按组织）。
- 工作量：中大（动 `role_data_scopes` 模型 + 拦截器 SQL 构造）。

### P3 — `role.level` 扶正（让数字职级可用，可选）
- 现状矛盾：领域层 `Role.outranks()` 用 level 当职级（**数小为高**，默认 100）；但前端角色表单把 level 当"**排序**"在填。同字段两义。
- 方案：要么**新增独立 `rank` 字段**（与排序 level 分离）、要么**明确 level=职级**并修前端"排序"标签 + 配出真实梯度 + 固定方向。
- 扶正后开启 `no-manage-senior`（按 rank 比较）规则。
- 工作量：小，但需梳理现有角色数据。

## 4. a-admin 端到端验收剧本（贯穿各阶段）

设定：组织 A；admin 是 A 的管理者(`access_relations` admin 关系)、超管；a 是 A 成员；a 获 `user:update` + 数据范围"本组织"。

| 步骤 | 期望 |
|---|---|
| a 打开用户列表 | 能看到 admin（闸2 放行，组织花名册该看全）|
| a 点"编辑/删除/重置密码/改角色" admin | 闸3 `protect-org-manager`/`protect-super-admin` → **DENY 403**，提示"无权操作该用户" |
| a 编辑本组织普通成员(成员关系、非管理者) | 通过 |
| a 尝试把某人角色调成超管 | `no-escalation` → DENY |
| (P2 后)给 a 配"修改范围=本组织成员(排除管理者)" | admin 直接不在 a 的可改集合（行级过滤）|
| (误配)把 a 修改范围配成"本组织全部" | 闸3 仍拦住 admin（兜底）|

## 5. 推荐落地顺序

```
P0 (堵洞, 半天, 强烈建议先做)
  → P1 (引擎骨架, 中)
    → P2 (读写分离 + 关系维度, 按需)
      → P3 (level 扶正, 按需)
```

- **P0 可独立交付**，先把越权洞堵上，不必等后续。
- P1 把护栏通用化、可扩展、可审计。
- P2/P3 是"做得更细/更顺手"，按业务实际需求决定要不要上。

## 6. 风险与权衡
- **性能**：闸3 每次 mutating 多一次内存规则评估（目标已 load）——可忽略。
- **误拦**：默认放行 + 规则强制单测 + 上线前 `explain` 干跑核对。
- **level 依赖**：P0 故意不依赖 level；依赖 level 的规则留到 P3 扶正后再开，避免"默认全平级"导致的失准或误拦。
- **配置复杂度**：读写分离 + 关系维度会让数据权限配置更细，需在 UI 上把语义标注清楚（与"检查模块 scope 标注"同理）。
