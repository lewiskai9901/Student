# Casbin 迁移进度检查清单

> 创建日期: 2026-01-02
> 最后更新: 2026-01-02
> 当前状态: **阶段一至五已完成，阶段六进行中**

## 使用说明

此文档用于跟踪 Casbin 数据权限系统重构的进度。每完成一项任务，请将 `[ ]` 改为 `[x]`，并记录完成日期。

如果上下文丢失，可以从此文档恢复进度状态。

---

## 阶段一：环境准备 (预计 1-2 天)

### 1.1 依赖添加
- [x] 在 `pom.xml` 中添加 Casbin 依赖 ✓ (2026-01-02)
  ```xml
  <dependency>
      <groupId>org.casbin</groupId>
      <artifactId>jcasbin</artifactId>
      <version>1.55.0</version>
  </dependency>
  <dependency>
      <groupId>org.casbin</groupId>
      <artifactId>jdbc-adapter</artifactId>
      <version>2.7.0</version>
  </dependency>
  ```
- [x] 运行 `mvn clean install` 验证依赖正常 ✓ (2026-01-02)

### 1.2 配置文件创建
- [x] 创建 `backend/src/main/resources/casbin/model.conf` ✓ (2026-01-02)
- [x] 验证 model.conf 语法正确 ✓ (2026-01-02)

### 1.3 数据库表创建
- [x] 创建 `casbin_rule` 表 ✓ SQL已准备 (2026-01-02)
- [x] 创建 `scope_metadata` 表 ✓ SQL已准备 (2026-01-02)
- [x] 创建 `user_scope_assignments` 表 ✓ SQL已准备 (2026-01-02)
- [x] 创建 `permission_audit_log` 表 ✓ SQL已准备 (2026-01-02)
- [x] 创建 `role_permission_templates` 表 ✓ SQL已准备 (2026-01-02)
- [x] 插入初始范围元数据 ✓ SQL已准备 (2026-01-02)
- [x] 插入角色权限模板 ✓ SQL已准备 (2026-01-02)

**SQL脚本位置**: `backend/sql/casbin_init.sql`

**执行方式**:
```bash
mysql -u root -p student_management < backend/sql/casbin_init.sql
```

**阶段一完成日期**: 2026-01-02 (代码已完成，需执行SQL)

---

## 阶段二：核心服务开发 (预计 3-4 天)

### 2.1 包结构创建
- [x] 创建 `com.school.management.casbin.config` 包 ✓ (2026-01-02)
- [x] 创建 `com.school.management.casbin.service` 包 ✓ (2026-01-02)
- [x] 创建 `com.school.management.casbin.function` 包 ✓ (2026-01-02)
- [x] 创建 `com.school.management.casbin.dto` 包 ✓ (2026-01-02)
- [x] 创建 `com.school.management.casbin.controller` 包 ✓ (2026-01-02)

### 2.2 配置类开发
- [x] 实现 `CasbinConfig.java` ✓ (2026-01-02)
  - [x] 配置 JDBCAdapter
  - [x] 配置 Enforcer Bean
  - [x] 使用内置 g2 函数替代自定义函数

### 2.3 自定义函数开发
- [x] 实现 `ScopeMatchFunction.java` ✓ (2026-01-02)
  - [x] 全局范围匹配 (`scope:*`)
  - [x] 精确匹配
  - [x] 层级匹配
  - 注：暂时使用 Casbin 内置 g2() 实现层级匹配

### 2.4 缓存服务开发
- [x] 实现 `ScopeHierarchyCache.java` ✓ (2026-01-02)
  - [x] 初始化加载层级关系
  - [x] 刷新方法
  - [x] isChildOf 判断方法
  - [x] getParents 方法

### 2.5 权限执行服务开发
- [x] 实现 `CasbinEnforcerService.java` ✓ (2026-01-02)
  - [x] checkPermission 方法
  - [x] getUserScopes 方法
  - [x] getAccessibleClassIds 方法
  - [x] assignUserScope 方法
  - [x] removeUserScope 方法
  - [x] assignUserRole 方法
  - [x] addPolicy 方法

### 2.6 范围管理服务开发
- [x] 实现 `CasbinScopeService.java` ✓ (2026-01-02)
  - [x] assignScope 方法
  - [x] batchAssignScopes 方法
  - [x] removeScope 方法
  - [x] getUserScopes 方法
  - [x] applyTemplate 方法 (TODO: 需要完善)

### 2.7 DTO 类开发
- [x] 创建 `ScopeAssignmentDTO.java` ✓ (2026-01-02)
- [x] 创建 `PermissionCheckDTO.java` ✓ (2026-01-02)
- [x] 创建 `ScopeMetadataDTO.java` ✓ (2026-01-02)

### 2.8 Mapper 开发
- [x] 创建 `UserScopeAssignmentMapper.java` ✓ (2026-01-02)
- [x] 创建 `ScopeMetadataMapper.java` ✓ (2026-01-02)
- [x] 创建 `PermissionAuditLogMapper.java` ✓ (2026-01-02)
- [x] 创建对应的 XML 映射文件 ✓ (2026-01-02)

### 2.9 单元测试
- [ ] CasbinEnforcerService 单元测试
- [ ] CasbinScopeService 单元测试
- [ ] ScopeMatchFunction 单元测试
- [ ] 集成测试

**阶段二完成日期**: 2026-01-02 (核心代码已完成，测试待补充)

---

## 阶段三：数据迁移 (预计 1-2 天)

### 3.1 迁移脚本编写
- [x] 编写 `user_data_scopes` 迁移脚本 ✓ (2026-01-02)
  - [x] 读取现有数据
  - [x] 转换为 Casbin 规则格式
  - [x] 插入 casbin_rule 表
  - [x] 插入 user_scope_assignments 表
- [x] 编写范围元数据同步脚本 ✓ (2026-01-02)
- [x] 编写层级关系(g2)初始化脚本 ✓ (2026-01-02)

**迁移脚本位置**: `backend/sql/casbin_init.sql` (步骤2-4，取消注释执行)

### 3.2 迁移执行
- [x] 执行数据库初始化SQL ✓ (2026-01-02)
  - casbin_rule 表创建并初始化 (15条策略)
  - scope_metadata 表创建并初始化 (4条记录)
  - user_scope_assignments 表创建
  - role_permission_templates 表创建并初始化 (6条记录)
- [ ] 备份现有数据库
- [ ] 在测试环境执行迁移
- [ ] 验证迁移数据正确性
- [ ] 在生产环境执行迁移 (如适用)

### 3.3 数据验证
- [ ] 对比迁移前后用户权限
- [ ] 抽样验证权限判断结果
- [ ] 修复发现的问题

**阶段三完成日期**: ___________ (脚本已准备，待执行)

---

## 阶段四：API 开发 (预计 2-3 天)

### 4.1 Controller 开发
- [x] 实现 `ScopeManageController.java` ✓ (2026-01-02)
  - [x] POST /api/v2/scopes/assign
  - [x] DELETE /api/v2/scopes/revoke
  - [x] GET /api/v2/scopes/user/{userId}
  - [x] POST /api/v2/scopes/batch-assign
  - [ ] POST /api/v2/scopes/apply-template (TODO)
  - [ ] GET /api/v2/scopes/templates (TODO)
  - [x] GET /api/v2/scopes/metadata

### 4.2 现有服务集成
- [x] 通过 `DataPermissionService` 接口实现无缝切换 ✓ (2026-01-02)
  - `CasbinDataPermissionService` 实现 `DataPermissionService` 接口
  - 使用 `@Primary` 注解在 Casbin 启用时优先加载
  - 现有服务无需修改，自动使用新实现
- [x] 更新 `StudentService` 使用新权限检查 ✓ (自动)
- [x] 更新 `ClassService` 使用新权限检查 ✓ (自动)
- [x] 更新 `DormitoryService` 使用新权限检查 ✓ (自动)
- [x] 更新其他需要数据权限控制的服务 ✓ (自动)

### 4.3 权限拦截器
- [x] 保持现有 `DataScopeAspect` 不变 ✓ (2026-01-02)
- [x] 通过 `DataPermissionService` 接口自动集成 Casbin ✓ (2026-01-02)

### 4.4 API 测试
- [x] 手动API测试通过 ✓ (2026-01-02)
  - GET /api/v2/scopes/metadata ✓ 获取范围元数据
  - GET /api/v2/scopes/user/{userId} ✓ 获取用户范围
  - POST /api/v2/scopes/assign ✓ 分配范围
  - DELETE /api/v2/scopes/revoke ✓ 撤销范围(逻辑删除)
  - POST /api/v2/scopes/check ✓ 权限检查
- [ ] 编写自动化API测试用例
- [ ] 测试边界情况

**阶段四完成日期**: 2026-01-02 ✓ (核心API和服务集成已完成)

---

## 阶段五：前端开发 (预计 2-3 天)

### 5.1 API 接口定义
- [x] 在 `src/api/` 创建 scope 相关 API 文件 ✓ (2026-01-02)
  - 文件: `frontend/src/api/scope.ts`
  - 调用新 API: `/api/v2/scopes/*`
- [x] 定义 TypeScript 类型 ✓ (2026-01-02)
  - `ScopeAssignmentDTO`
  - `ScopeMetadataDTO`
  - `PermissionCheckDTO`
  - `ScopeTypeCode`

### 5.2 组件开发
- [x] 更新 `UserDataScopeManager.vue` 组件 ✓ (2026-01-02)
  - [x] 范围类型选择 (ALL/DEPT/GRADE/DEPT_GRADE/CLASS/SELF)
  - [x] 部门选择器
  - [x] 年级选择器
  - [x] 班级选择器
  - [x] 已分配范围列表
  - [x] 移除范围功能
  - [x] 范围表达式预览

### 5.3 页面集成
- [x] 用户管理页面已有"数据范围"按钮 ✓ (已存在)
- [x] 已集成更新后的 UserDataScopeManager 组件 ✓ (2026-01-02)
- [ ] 调整角色管理页面 (可选，新系统不依赖角色数据权限)

### 5.4 前端测试
- [x] 前端构建通过 ✓ (2026-01-02)
- [x] 与后端 API 联调 ✓ (2026-01-02)
  - 所有 Casbin API 测试通过
  - 范围分配/撤销/查询功能正常
- [ ] 端到端测试

**阶段五完成日期**: 2026-01-02 ✓ (前端代码和API联调已完成)

---

## 阶段六：切换与清理 (预计 1-2 天)

### 6.1 切换准备
- [x] 添加功能开关配置 ✓ (2026-01-02)
  - `casbin.enabled` 配置项已添加到 application.yml
  - 所有 Casbin 组件已添加 `@ConditionalOnProperty` 注解
  - 旧版 `DataPermissionServiceImpl` 保持为 `casbin.enabled=false` 时的默认实现
- [ ] 准备回滚脚本

### 6.2 灰度发布
- [ ] 在测试环境完整验证
- [ ] 选择部分用户灰度
- [ ] 监控错误日志
- [ ] 收集用户反馈

### 6.3 全量切换
- [ ] 切换到新权限系统
- [ ] 监控系统运行状态
- [ ] 确认无异常

### 6.4 清理工作
- [ ] 废弃旧的 `UserDataScopeService` (保留作为回退)
- [ ] 废弃旧的 `RoleDataPermissionService` (保留作为回退)
- [x] 移除旧代码或标记为 @Deprecated ✓ (2026-01-02)
  - `DataPermissionServiceImpl` 已标记为 @Deprecated
  - 添加了迁移说明和移除计划 (V5.0)
- [ ] 更新相关文档

### 6.5 文档更新
- [ ] 更新 CLAUDE.md
- [ ] 更新 API 文档
- [ ] 编写用户使用指南

**阶段六完成日期**: ___________

---

## 问题记录

在实施过程中遇到的问题和解决方案记录在此处。

### 问题 #1
- **描述**:
- **解决方案**:
- **解决日期**:

### 问题 #2
- **描述**:
- **解决方案**:
- **解决日期**:

---

## 回滚计划

如果新系统出现严重问题，按以下步骤回滚：

1. [ ] 关闭 Casbin 功能开关
2. [ ] 切换回旧权限服务
3. [ ] 验证系统功能正常
4. [ ] 分析问题原因
5. [ ] 修复后重新迁移

---

## 完成确认

- [ ] 所有阶段任务完成
- [ ] 测试通过
- [ ] 文档更新
- [ ] 生产环境验证

**项目完成日期**: ___________

**负责人签字**: ___________
