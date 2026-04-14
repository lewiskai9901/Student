# 访问控制体系与用户类型统一化重构

**日期**：2026-04-14
**状态**：设计中，待确认
**背景来源**：教师个人空间 brainstorming 过程中发现的系统性架构缺陷

---

## 1. 背景

教师端个人空间（`/my/dashboard`）的需求原本是一个常规 UI 页面，但在 brainstorming 过程中暴露出访问控制与用户类型体系的 6 个结构性问题：

1. `permissions` 表无 `permission_scope` 字段，无法区分"个人空间权限 / 管理后台权限"
2. `UserType.defaultRoleCodes` 等字段存了但从未被消费
3. 权限管理仅支持代码先行，前端只读，无运行时创建
4. V5 数据权限后端 API 完整，但无前端配置 UI
5. `UserType.features` / `metadataSchema` 定义了但不被 User 生命周期约束
6. `Role.data_scope` 遗留字段与 V5 数据权限并存

以及 5 个架构层面的不一致：

- **A.** Casbin / V5 / access_relations 三层存在，但实施严重不均衡
- **B.** Domain Event 几乎无监听器消费
- **C.** `ConfigurableType` 抽象只贯彻了 UserType/OrgType/PlaceType，未覆盖 Role/Permission
- **D.** 前后端能力落差：UserType/数据权限/可编辑权限 API 齐全但无 UI
- **E.** `UserType.defaultRoleCodes` ↔ `user_roles` 无同步机制

经过分析，三层访问控制（Casbin / V5 / access_relations）**是分层流水线而非并行冗余**：

```
Casbin    → API 网关层（927 处）
V5        → SQL 过滤层（32 处）
access_rel → V5 的资源关系数据源（仅 3 个资源使用）
```

设计本身合理，问题在于**实施不彻底**。重构目标是把"架构设计的通用性"落地为"实际可用的通用平台"。

---

## 2. 目标

| # | 目标 | 验收标准 |
|---|------|----------|
| G1 | 权限具备 scope 语义（SELF/MANAGEMENT/PUBLIC），自动区分个人空间与管理后台 | 教师登录不再需要硬编码判断"能否进管理后台" |
| G2 | UserType 全链路生效：默认角色自动分配、features 校验强制、metadataSchema 动态渲染 | 新建 TEACHER 用户自动获得教师角色，无需手动分配 |
| G3 | 数据权限覆盖率提升：所有暴露学生/教师/课表等敏感数据的 Mapper 都标注 `@DataPermission` | ArchUnit 规则强制新 Mapper 必须有注解 |
| G4 | `access_relations` 写入端激活：业务事件自动建立关系 | 任课教师自动建立 teaches 关系，学生入学自动建立 owns 关系 |
| G5 | 管理 UI 补齐：UserType CRUD / 数据权限配置 / 可编辑权限管理 | 管理员不再需要调 API 或写 SQL |
| G6 | 遗留字段彻底删除 | `roles.data_scope` 列及相关代码全部清零 |

---

## 3. 架构决策

### 3.1 三层访问控制职责分工（锁定）

| 层 | 机制 | 回答的问题 | 典型场景 |
|---|------|-----------|----------|
| L1 | Casbin | "用户的角色能调这个接口吗？" | `@CasbinAccess(resource="student:info", action="view")` |
| L2a | V5 + `orgUnitField` | "用户能看哪些批量数据？"（组织边界） | 三年级老师只看三年级学生 |
| L2b | V5 + `resourceType` + access_relations | "用户能看这一条特定记录吗？"（关系边界） | 张老师被授权查看李同学档案 |

**使用规范**：
- 涉及组织树边界过滤 → `orgUnitField`
- 涉及单个资源的细粒度授权 → `resourceType`
- 两者可以共存（先组织过滤，再关系过滤）

### 3.2 权限分层（新引入）

```java
enum PermissionScope {
    PUBLIC,      // 登录即可用（如查看首页公告）
    SELF,        // 个人空间专用（/my/* 路由）
    MANAGEMENT   // 管理后台专用（/dashboard/* 路由）
}
```

**关键规则**：
- `UserType.features` 中的 capability（如 `canViewOwnSchedule`）→ 自动映射为 SELF 权限
- 判断用户"有无管理视图"：`userPermissions.any(p -> p.scope == MANAGEMENT)`
- 与自定义角色完全解耦，不硬编码任何 role_code

### 3.3 事件驱动编排

现有 Domain Event 发出后无人监听。本次重构新增以下监听器：

| 事件 | 监听器 | 动作 |
|------|--------|------|
| `UserCreatedEvent` | `UserTypeDefaultProvisioner` | 按 UserType.defaultRoleCodes 自动分配角色 |
| `UserTypeUpdatedEvent` | `UserTypeBackfillHandler` | 提供手动触发接口，回溯同步已存在用户 |
| `TeacherAssignedEvent` | `AccessRelationAutoBuilder` | 建立老师 ↔ 班级的 teaches 关系 |
| `StudentEnrolledEvent` | `AccessRelationAutoBuilder` | 建立班主任 ↔ 学生的 manages 关系 |

---

## 4. Phase 分解

### Phase 0：清理 + 规范（2 天，可并行）

**目标**：消除歧义，为后续重构奠定底线。

| 任务 | 产出 |
|------|------|
| 0.1 删除 `roles.data_scope` 列 + 所有读写代码 | 迁移脚本 + 代码清理 PR |
| 0.2 扫描并记录其他 legacy 字段（如 `user_org_relations` 若仍有引用） | 清理报告 |
| 0.3 编写 `docs/access-control-guide.md` | 三层职责、使用规范、开发 checklist |
| 0.4 编写 `docs/access-control-matrix.md` | Casbin/V5/access_relations 分工速查表 |

**验收**：代码库无 `data_scope` 字段残留；新开发者看文档即知何时用哪层。

---

### Phase 1：权限分层（2-3 天，**阻塞教师页**）

**目标**：为 `permissions` 引入 scope 语义，建立 SELF/MANAGEMENT 分界。

| 任务 | 技术点 |
|------|--------|
| 1.1 DB 迁移：`permissions.permission_scope VARCHAR(20) DEFAULT 'MANAGEMENT'` | SQL 迁移脚本 |
| 1.2 `PermissionScope` 枚举（SELF/MANAGEMENT/PUBLIC） | `domain/access/model/PermissionScope.java` |
| 1.3 `Permission` 实体加 `scope` 字段 + PO + Mapper | 现有聚合增强 |
| 1.4 种子数据迁移：按 `permission_code` 前缀批量标注 scope | `my:*` → SELF；其余 → MANAGEMENT；`public:*` → PUBLIC |
| 1.5 `CasbinAccessInterceptor` 融入 scope 校验 | SELF 权限强制校验资源所有人；MANAGEMENT 权限要求用户至少有一个 MANAGEMENT 权限 |
| 1.6 `UserContext` 暴露 `hasAnyManagementPermission()` | `UserContextHolder` 缓存计算结果 |
| 1.7 前端 `auth.ts` 暴露 `hasManagementAccess` 计算属性 | Pinia store 派生状态 |

**验收**：
- 在数据库将某权限改为 SELF 后，非本人访问立即拒绝
- 前端 `hasManagementAccess` 正确反映用户权限 scope 分布

---

### Phase 2：UserType 激活（2-3 天，**阻塞教师页**）

**目标**：让 UserType 的默认值、features、metadataSchema 全部生效。

| 任务 | 技术点 |
|------|--------|
| 2.1 `UserTypeDefaultProvisioner` 监听 `UserCreatedEvent` | 读取 UserType.defaultRoleCodes，调用 `UserRoleService.assignRoleByCode` |
| 2.2 `User` 聚合在 create/update 时校验 UserType.features | `requiresOrg=true` 且 `primaryOrgUnitId == null` → 抛异常 |
| 2.3 扩展 `UserType.features` 定义 capability 类型标志 | 预置 TEACHER 的 5 个 canXxx |
| 2.4 UserType features → SELF 权限映射服务 | `getCapabilityPermissionCodes(UserType)` |
| 2.5 前端 User 表单动态渲染 metadataSchema | 基于选中的 UserType 动态追加字段 |
| 2.6 UserType 变更回溯同步接口 | `POST /user-types/{code}/sync-to-users`，手动触发 |

**验收**：
- 新建 TEACHER 用户（不选角色），保存后该用户自动拥有 `教师基础` / `课表查看` 等默认角色
- 创建老师时未选部门 → 校验失败
- 选择 STUDENT 类型时表单自动出现"学号"字段（若 STUDENT.metadataSchema 声明了）

---

### Phase 3：管理 UI 补齐（5-7 天，可并行）

**目标**：把 Phase 1/2 建立的能力暴露到管理界面。

| 模块 | 估时 | 核心页面 |
|------|------|---------|
| 3.1 UserType CRUD UI | 2 天 | `views/access/UserTypeListView.vue` + 编辑器（features 勾选、defaultRoleCodes 选择、metadataSchema JSON 编辑器） |
| 3.2 数据权限配置 UI（V5） | 3 天 | `views/access/RoleDataPermissionView.vue`，按角色配置每模块 scope + custom items |
| 3.3 权限管理升级为可编辑 | 2 天 | `views/access/PermissionListView.vue` 开放创建/编辑/删除，scope 下拉选择 |

**验收**：
- 管理员可在 UI 创建新 UserType 并勾选 features，全程不写 SQL
- 管理员可为某角色配置"学生" 模块的 CUSTOM scope，选三年级所有班级
- 管理员可创建新权限，指定 scope，立即可分配给角色

---

### Phase 4：数据权限覆盖 + access_relations 激活（4-5 天）

**目标**：把 V5 从 32 处扩展到全覆盖，激活 access_relations 写入端。

| 任务 | 技术点 |
|------|--------|
| 4.1 审计所有 Mapper，列出缺 `@DataPermission` 的 | 扫描脚本 + 审计报告 |
| 4.2 批量补全注解（学生/教师/课表/成绩/宿舍等敏感数据） | 合理选择 orgUnitField / resourceType |
| 4.3 ArchUnit 规则：新 Mapper 必须有 `@DataPermission` 或显式 `@PublicData` 豁免 | `backend/src/test/java/.../ArchUnitMapperDataPermissionTest.java` |
| 4.4 `AccessRelationAutoBuilder` 监听关键业务事件 | Teacher↔Class, Class↔Student, Substitute↔Instance |
| 4.5 补齐 access_relations 管理 UI（现 `AccessRelationManager.vue` 增强） | 支持审计、批量导入、过期自动清理 |

**验收**：
- `@DataPermission` 覆盖 80+ 处
- 新任命一位任课老师后，`access_relations` 表自动生成对应行
- 删除一个 Mapper 不加注解时，CI 测试失败

---

### Phase 5：收口 + 文档（2 天）

| 任务 | 产出 |
|------|------|
| 5.1 遗留命名扫描（grep `legacy`、`old_`、`deprecated` 注释） | 清理报告 + PR |
| 5.2 更新架构文档 | `docs/design/access-control.md` / `docs/design/user-type-system.md` |
| 5.3 录一段内部 demo 视频（可选） | 展示新建用户全流程 |

---

## 5. 依赖关系图

```
Phase 0 (清理)
   │
   ├─→ Phase 1 (权限分层) ─┐
   │                        ├─→ Phase 3 (管理 UI)
   └─→ Phase 2 (UserType) ─┘          │
               │                      ▼
               ▼                 可上线教师页 MVP
        Phase 4 (数据权限覆盖)
               │
               ▼
          Phase 5 (收口)
```

**最小路径跑通教师页**：Phase 0 → Phase 1 → Phase 2，约 **6-8 天**。

---

## 6. 风险与对策

| 风险 | 概率 | 对策 |
|------|------|------|
| 删除 `data_scope` 列时有遗漏代码引用 | 中 | Phase 0.1 先全仓 grep，写单元测试覆盖所有路径 |
| 批量给现有权限标 scope 时分类错误（比如把个人权限误标为 MANAGEMENT） | 高 | 先小批量试点（只处理 `my:*` 前缀），观察线上行为再扩大 |
| `UserTypeDefaultProvisioner` 自动分配角色导致循环事件 | 低 | 使用 `@TransactionalEventListener(AFTER_COMMIT)`，避免事务嵌套 |
| ArchUnit 规则上线后大量 Mapper 失败 | 高 | 先加 `@AllowedLegacy` 注解标记豁免，逐批清理 |
| 前端 UserType 动态表单复杂度超预期 | 中 | MVP 阶段只支持简单字段（string/number/boolean），复杂嵌套延后 |

---

## 7. 验收矩阵

重构完成后，以下 7 个场景必须通过手工验证：

| # | 场景 | 预期 |
|---|------|------|
| V1 | 新建 TEACHER 用户，只填基本信息 | 自动分配默认角色，自动具备 SELF 权限 |
| V2 | 教师登录后访问 `/my/schedule` | 正确显示自己的课表 |
| V3 | 教师登录后尝试访问 `/admin/users` | 被 Casbin 拒绝（无 MANAGEMENT 权限） |
| V4 | 管理员在 UI 新建权限 `my:grade:view`，标记 scope=SELF | 立即可分配给角色，路由守卫生效 |
| V5 | 管理员为"班主任"角色配置"学生"模块 CUSTOM scope | SQL 自动追加 WHERE 只返回对应班级学生 |
| V6 | 任命王老师为三年级 2 班任课 | `access_relations` 自动生成 `王老师 teaches 三年级2班` 行 |
| V7 | 删除某 Mapper 上的 `@DataPermission` 注解后跑 CI | 单元测试失败，禁止合入 |

---

## 8. 不做的事（Out of Scope）

- ❌ 迁移 Role 到 `ConfigurableType` 抽象（工作量大，收益不明显，留待 Phase 6 独立评估）
- ❌ 迁移 Permission 到 `ConfigurableType` 抽象（同上）
- ❌ 引入 Field-level 权限（字段级脱敏，属于未来 Phase 7）
- ❌ 多租户数据隔离深度改造（当前 `tenant_id` 已足够，本次不扩展）

---

## 9. 开始条件

用户确认方案后，从 Phase 0 开始串行推进。每个 Phase 完成后进行以下动作：

1. 跑完整测试套件（含 ArchUnit）
2. 手工验收对应场景
3. 提交 PR 并自我 review
4. 进入下一 Phase

---

## 10. 附录：关键文件清单

### 后端新增/修改
- `domain/access/model/PermissionScope.java`（新）
- `domain/access/model/Permission.java`（加 scope 字段）
- `domain/access/interceptor/CasbinAccessInterceptor.java`（融入 scope 校验）
- `application/user/UserTypeDefaultProvisioner.java`（新，事件监听器）
- `application/access/AccessRelationAutoBuilder.java`（新，事件监听器）
- `infrastructure/access/UserContextHolder.java`（增强 hasAnyManagementPermission）
- `database/migrations/V4X.0.0__permission_scope.sql`（新）
- `database/migrations/V4X.0.1__remove_role_data_scope.sql`（新）

### 前端新增/修改
- `views/access/UserTypeListView.vue`（新）
- `views/access/RoleDataPermissionView.vue`（新）
- `views/access/PermissionListView.vue`（改造为可编辑）
- `stores/auth.ts`（增加 hasManagementAccess）
- `components/access/UserTypeFeatureEditor.vue`（新）
- `components/access/DynamicMetadataForm.vue`（新）

### 文档新增
- `docs/design/access-control.md`（新）
- `docs/design/user-type-system.md`（新）
- `docs/access-control-matrix.md`（新，速查表）

---

**下一步**：用户确认方案或调整后，Phase 0 立即启动。
