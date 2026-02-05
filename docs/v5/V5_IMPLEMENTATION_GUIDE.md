# V5 重构方案 - 实施指南

> **版本**: 3.0
> **日期**: 2026-01-31
> **关联文档**: [V5_ARCHITECTURE.md](./V5_ARCHITECTURE.md)

---

## 一、权限系统实现

### 1.1 数据权限注解

```java
package com.school.management.infrastructure.permission;

import java.lang.annotation.*;

/**
 * 数据权限注解
 * 标注在 Mapper 方法上，自动注入数据过滤条件
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataPermission {

    /**
     * 数据模块代码
     */
    String module();

    /**
     * 表别名（用于 JOIN 查询）
     */
    String tableAlias() default "";

    /**
     * 是否启用（可通过配置动态关闭）
     */
    boolean enabled() default true;
}
```

### 1.2 MyBatis 拦截器

```java
package com.school.management.infrastructure.permission;

import com.school.management.domain.access.model.*;
import com.school.management.domain.access.repository.DataPermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.*;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@Intercepts({
    @Signature(type = StatementHandler.class, method = "prepare",
               args = {Connection.class, Integer.class})
})
public class DataPermissionInterceptor implements Interceptor {

    private final DataPermissionRepository permissionRepository;
    private final UserContextHolder userContextHolder;
    private final DataModuleRegistry moduleRegistry;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler handler = (StatementHandler) invocation.getTarget();
        MetaObject metaObject = SystemMetaObject.forObject(handler);

        MappedStatement ms = (MappedStatement) metaObject.getValue("delegate.mappedStatement");

        // 获取注解
        DataPermission annotation = getDataPermissionAnnotation(ms);
        if (annotation == null || !annotation.enabled()) {
            return invocation.proceed();
        }

        // 获取当前用户
        UserContext user = userContextHolder.getCurrentUser();
        if (user == null || user.isSuperAdmin()) {
            return invocation.proceed();
        }

        // 获取数据权限配置
        String moduleCode = annotation.module();
        DataModuleConfig moduleConfig = moduleRegistry.getModule(moduleCode);
        if (moduleConfig == null) {
            log.warn("未找到数据模块配置: {}", moduleCode);
            return invocation.proceed();
        }

        // 计算数据范围
        MergedDataScope mergedScope = calculateMergedScope(user, moduleCode);
        if (mergedScope.isAll()) {
            return invocation.proceed();
        }

        // 修改 SQL
        BoundSql boundSql = handler.getBoundSql();
        String originalSql = boundSql.getSql();
        String newSql = injectDataFilter(originalSql, mergedScope, moduleConfig,
                                         annotation.tableAlias());

        metaObject.setValue("delegate.boundSql.sql", newSql);

        return invocation.proceed();
    }

    /**
     * 计算合并后的数据范围（多角色取并集）
     */
    private MergedDataScope calculateMergedScope(UserContext user, String moduleCode) {
        List<Long> roleIds = user.getRoleIds();

        MergedDataScope merged = new MergedDataScope();

        for (Long roleId : roleIds) {
            RoleDataPermission permission = permissionRepository
                .findByRoleIdAndModule(roleId, moduleCode);

            if (permission == null) {
                continue;
            }

            DataScopeType scopeType = permission.getScopeType();

            // ALL 优先级最高
            if (scopeType == DataScopeType.ALL) {
                return MergedDataScope.all();
            }

            switch (scopeType) {
                case DEPARTMENT_AND_BELOW:
                    merged.addOrgUnits(getOrgUnitTree(user.getOrgUnitId()));
                    break;
                case DEPARTMENT:
                    merged.addOrgUnit(user.getOrgUnitId());
                    break;
                case CUSTOM:
                    merged.addCustomItems(permission.getScopeItems());
                    break;
                case SELF:
                    merged.setSelfIncluded(true);
                    merged.setUserId(user.getUserId());
                    break;
            }
        }

        return merged;
    }

    /**
     * 注入数据过滤条件
     */
    private String injectDataFilter(String sql, MergedDataScope scope,
                                    DataModuleConfig config, String tableAlias) {
        try {
            Select select = (Select) CCJSqlParserUtil.parse(sql);
            PlainSelect plainSelect = (PlainSelect) select.getSelectBody();

            Expression where = plainSelect.getWhere();
            Expression filterExpr = buildFilterExpression(scope, config, tableAlias);

            if (where == null) {
                plainSelect.setWhere(filterExpr);
            } else {
                plainSelect.setWhere(new AndExpression(where, filterExpr));
            }

            return select.toString();
        } catch (Exception e) {
            log.error("SQL 解析失败: {}", sql, e);
            return sql;
        }
    }

    /**
     * 构建过滤表达式
     */
    private Expression buildFilterExpression(MergedDataScope scope,
                                             DataModuleConfig config,
                                             String tableAlias) {
        List<String> conditions = new ArrayList<>();
        String prefix = tableAlias.isEmpty() ? "" : tableAlias + ".";

        // 部门过滤
        if (!scope.getOrgUnitIds().isEmpty()) {
            String orgField = config.getFilterFields().get("org_unit");
            if (orgField != null) {
                String ids = scope.getOrgUnitIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
                conditions.add(prefix + orgField + " IN (" + ids + ")");
            }
        }

        // 班级过滤
        if (!scope.getClassIds().isEmpty()) {
            String classField = config.getFilterFields().get("class");
            if (classField != null) {
                String ids = scope.getClassIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
                conditions.add(prefix + classField + " IN (" + ids + ")");
            }
        }

        // 创建者过滤（SELF）
        if (scope.isSelfIncluded()) {
            String creatorField = config.getFilterFields().get("creator");
            if (creatorField != null) {
                conditions.add(prefix + creatorField + " = " + scope.getUserId());
            }
        }

        // 用 OR 连接所有条件
        String combined = conditions.stream()
            .collect(Collectors.joining(" OR ", "(", ")"));

        try {
            return CCJSqlParserUtil.parseCondExpression(combined);
        } catch (Exception e) {
            log.error("条件表达式解析失败: {}", combined, e);
            return null;
        }
    }

    private DataPermission getDataPermissionAnnotation(MappedStatement ms) {
        // 从 MappedStatement ID 获取 Mapper 方法
        String id = ms.getId();
        String className = id.substring(0, id.lastIndexOf('.'));
        String methodName = id.substring(id.lastIndexOf('.') + 1);

        try {
            Class<?> mapperClass = Class.forName(className);

            // 先检查类级别注解
            DataPermission classAnnotation = mapperClass.getAnnotation(DataPermission.class);

            // 再检查方法级别注解
            for (Method method : mapperClass.getMethods()) {
                if (method.getName().equals(methodName)) {
                    DataPermission methodAnnotation = method.getAnnotation(DataPermission.class);
                    return methodAnnotation != null ? methodAnnotation : classAnnotation;
                }
            }

            return classAnnotation;
        } catch (Exception e) {
            log.warn("获取数据权限注解失败: {}", id, e);
            return null;
        }
    }

    private Set<Long> getOrgUnitTree(Long orgUnitId) {
        // 获取部门及其所有子部门 ID
        return permissionRepository.findOrgUnitTree(orgUnitId);
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
}
```

### 1.3 合并数据范围类

```java
package com.school.management.infrastructure.permission;

import lombok.Data;
import java.util.*;

@Data
public class MergedDataScope {

    private boolean isAll = false;
    private Set<Long> orgUnitIds = new HashSet<>();
    private Set<Long> classIds = new HashSet<>();
    private Set<Long> buildingIds = new HashSet<>();
    private boolean selfIncluded = false;
    private Long userId;

    public static MergedDataScope all() {
        MergedDataScope scope = new MergedDataScope();
        scope.setAll(true);
        return scope;
    }

    public void addOrgUnit(Long id) {
        if (id != null) {
            orgUnitIds.add(id);
        }
    }

    public void addOrgUnits(Collection<Long> ids) {
        if (ids != null) {
            orgUnitIds.addAll(ids);
        }
    }

    public void addCustomItems(List<DataScopeItem> items) {
        if (items == null) return;

        for (DataScopeItem item : items) {
            switch (item.getItemType()) {
                case ORG_UNIT:
                    if (item.isIncludeChildren()) {
                        // 需要额外查询子部门
                        orgUnitIds.add(item.getScopeId());
                    } else {
                        orgUnitIds.add(item.getScopeId());
                    }
                    break;
                case CLASS:
                    classIds.add(item.getScopeId());
                    break;
                case BUILDING:
                    buildingIds.add(item.getScopeId());
                    break;
            }
        }
    }
}
```

### 1.4 用户上下文

```java
package com.school.management.infrastructure.permission;

import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.List;

@Data
public class UserContext {
    private Long userId;
    private String username;
    private Long orgUnitId;
    private List<Long> roleIds;
    private boolean superAdmin;
}

@Component
@RequestScope
public class UserContextHolder {

    private UserContext currentUser;

    public void setCurrentUser(UserContext user) {
        this.currentUser = user;
    }

    public UserContext getCurrentUser() {
        return currentUser;
    }

    public void clear() {
        this.currentUser = null;
    }
}
```

### 1.5 权限服务

```java
package com.school.management.application.access;

import com.school.management.domain.access.model.*;
import com.school.management.domain.access.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DataPermissionApplicationService {

    private final DataPermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final DataModuleRegistry moduleRegistry;

    /**
     * 获取角色的数据权限配置
     */
    @Cacheable(value = "roleDataPermission", key = "#roleId")
    public RoleDataPermissionDTO getRoleDataPermissions(Long roleId) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new BusinessException("角色不存在"));

        List<RoleDataPermission> permissions = permissionRepository
            .findByRoleId(roleId);

        return RoleDataPermissionDTO.builder()
            .roleId(roleId)
            .roleName(role.getName())
            .modulePermissions(convertToDTO(permissions))
            .build();
    }

    /**
     * 更新角色的数据权限配置
     */
    @Transactional
    @CacheEvict(value = "roleDataPermission", key = "#roleId")
    public void updateRoleDataPermissions(Long roleId,
                                          List<ModulePermissionDTO> permissions) {
        // 验证角色存在
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new BusinessException("角色不存在"));

        // 删除旧配置
        permissionRepository.deleteByRoleId(roleId);

        // 保存新配置
        for (ModulePermissionDTO dto : permissions) {
            // 验证模块存在
            if (!moduleRegistry.hasModule(dto.getModuleCode())) {
                throw new BusinessException("无效的数据模块: " + dto.getModuleCode());
            }

            RoleDataPermission permission = RoleDataPermission.builder()
                .roleId(roleId)
                .moduleCode(dto.getModuleCode())
                .scopeType(DataScopeType.valueOf(dto.getScopeCode()))
                .build();

            permissionRepository.save(permission);

            // 保存自定义范围项
            if (DataScopeType.CUSTOM.name().equals(dto.getScopeCode())) {
                saveScopeItems(permission.getId(), dto.getScopeItems());
            }
        }

        // 发布缓存失效事件
        publishCacheInvalidation(roleId);
    }

    /**
     * 获取所有数据模块（按领域分组）
     */
    public Map<String, List<DataModuleDTO>> getDataModules() {
        Map<String, List<DataModuleDTO>> grouped = new LinkedHashMap<>();

        for (DataModuleConfig config : moduleRegistry.getAllModules()) {
            String domain = config.getDomain();
            grouped.computeIfAbsent(domain, k -> new ArrayList<>())
                .add(DataModuleDTO.from(config));
        }

        return grouped;
    }

    /**
     * 获取所有数据范围类型
     */
    public List<DataScopeDTO> getDataScopes() {
        return Arrays.stream(DataScopeType.values())
            .map(type -> DataScopeDTO.builder()
                .code(type.name())
                .name(type.getDisplayName())
                .priority(type.getPriority())
                .build())
            .collect(Collectors.toList());
    }

    /**
     * 获取自定义范围可选项
     */
    public List<ScopeItemOption> getScopeItemOptions(String itemTypeCode,
                                                      String keyword) {
        DataScopeItemType itemType = DataScopeItemType.valueOf(itemTypeCode);

        switch (itemType) {
            case ORG_UNIT:
                return permissionRepository.searchOrgUnits(keyword);
            case CLASS:
                return permissionRepository.searchClasses(keyword);
            case BUILDING:
                return permissionRepository.searchBuildings(keyword);
            default:
                return Collections.emptyList();
        }
    }

    private void saveScopeItems(Long permissionId, List<ScopeItemDTO> items) {
        if (items == null || items.isEmpty()) return;

        for (ScopeItemDTO item : items) {
            DataScopeItem scopeItem = DataScopeItem.builder()
                .permissionId(permissionId)
                .itemType(DataScopeItemType.valueOf(item.getItemTypeCode()))
                .scopeId(item.getScopeId())
                .scopeName(item.getScopeName())
                .includeChildren(item.isIncludeChildren())
                .build();

            permissionRepository.saveScopeItem(scopeItem);
        }
    }

    private void publishCacheInvalidation(Long roleId) {
        // 通过 Redis 发布缓存失效消息
        // 所有节点收到消息后清除相关用户的权限缓存
    }
}
```

---

## 二、检查系统实现

### 2.1 Mapper 使用示例

```java
package com.school.management.infrastructure.persistence.inspection;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.infrastructure.permission.DataPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@DataPermission(module = "inspection_record")  // 类级别注解
public interface TargetInspectionRecordMapper extends BaseMapper<TargetInspectionRecordPO> {

    /**
     * 查询任务下的检查记录（自动应用数据权限过滤）
     */
    List<TargetInspectionRecordPO> selectByTaskId(@Param("taskId") Long taskId);

    /**
     * 自定义复杂查询（指定表别名）
     */
    @DataPermission(module = "inspection_record", tableAlias = "r")
    List<RecordWithDeductionVO> selectWithDeductions(
        @Param("taskId") Long taskId,
        @Param("targetType") String targetType
    );

    /**
     * 跳过数据权限检查的查询（用于系统内部统计）
     */
    @DataPermission(module = "inspection_record", enabled = false)
    Long countAllByProjectId(@Param("projectId") Long projectId);
}
```

### 2.2 检查记录服务

```java
package com.school.management.application.inspection;

import com.school.management.domain.inspection.model.*;
import com.school.management.domain.inspection.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InspectionRecordApplicationService {

    private final InspectionTaskRepository taskRepository;
    private final TargetInspectionRecordRepository recordRepository;
    private final DeductionRecordRepository deductionRepository;

    /**
     * 创建检查记录
     */
    @Transactional
    public Long createRecord(CreateRecordCommand command) {
        // 1. 验证任务存在且可录入
        InspectionTask task = taskRepository.findById(command.getTaskId())
            .orElseThrow(() -> new BusinessException("任务不存在"));

        if (!task.canRecord()) {
            throw new BusinessException("任务状态不允许录入");
        }

        // 2. 创建检查记录
        TargetInspectionRecord record = TargetInspectionRecord.builder()
            .taskId(command.getTaskId())
            .targetType(command.getTargetType())
            .targetId(command.getTargetId())
            .baseScore(task.getProject().getTemplate().getBaseScore())
            .inspectorId(command.getInspectorId())
            .inspectedAt(command.getInspectedAt())
            .build();

        // 3. 填充快照信息
        record.captureSnapshot(command.getTargetSnapshot());

        // 4. 保存记录
        recordRepository.save(record);

        // 5. 保存扣分明细
        if (command.getDeductions() != null) {
            for (DeductionDTO deduction : command.getDeductions()) {
                DeductionRecord dr = DeductionRecord.builder()
                    .targetRecordId(record.getId())
                    .categoryId(deduction.getCategoryId())
                    .scoreItemId(deduction.getScoreItemId())
                    .score(deduction.getScore())
                    .scoreType(deduction.getScoreType())
                    .quantity(deduction.getQuantity())
                    .studentId(deduction.getStudentId())
                    .remark(deduction.getRemark())
                    .build();

                deductionRepository.save(dr);
            }
        }

        // 6. 计算分数
        record.calculateScore();
        recordRepository.update(record);

        return record.getId();
    }

    /**
     * 批量查询检查记录（自动应用数据权限）
     */
    public PageResult<RecordDTO> queryRecords(RecordQueryCommand query) {
        // Mapper 已标注 @DataPermission，会自动过滤
        Page<TargetInspectionRecord> page = recordRepository.findByCondition(
            query.getTaskId(),
            query.getTargetType(),
            query.getPageNum(),
            query.getPageSize()
        );

        return PageResult.from(page, this::convertToDTO);
    }
}
```

### 2.3 申诉服务

```java
package com.school.management.application.inspection;

import com.school.management.domain.inspection.model.*;
import com.school.management.domain.inspection.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppealApplicationService {

    private final AppealRepository appealRepository;
    private final TargetInspectionRecordRepository recordRepository;
    private final DeductionRecordRepository deductionRepository;

    /**
     * 提交申诉
     */
    @Transactional
    public Long submitAppeal(SubmitAppealCommand command) {
        // 1. 验证扣分记录存在
        DeductionRecord deduction = deductionRepository.findById(command.getDeductionId())
            .orElseThrow(() -> new BusinessException("扣分记录不存在"));

        // 2. 验证是否在申诉窗口期
        TargetInspectionRecord record = recordRepository.findById(deduction.getTargetRecordId())
            .orElseThrow(() -> new BusinessException("检查记录不存在"));

        if (!record.isInAppealWindow()) {
            throw new BusinessException("已超过申诉期限");
        }

        // 3. 验证是否重复申诉
        if (appealRepository.existsByDeductionId(command.getDeductionId())) {
            throw new BusinessException("该扣分已存在申诉");
        }

        // 4. 创建申诉
        Appeal appeal = Appeal.builder()
            .deductionId(command.getDeductionId())
            .targetRecordId(deduction.getTargetRecordId())
            .appealType(command.getAppealType())
            .appealReason(command.getAppealReason())
            .evidenceUrls(command.getEvidenceUrls())
            .status(AppealStatus.PENDING)
            .build();

        // 5. 复制归属信息用于数据权限
        appeal.copyOwnership(record);

        appealRepository.save(appeal);

        return appeal.getId();
    }

    /**
     * 一级审核
     */
    @Transactional
    public void reviewLevel1(Long appealId, ReviewCommand command) {
        Appeal appeal = appealRepository.findById(appealId)
            .orElseThrow(() -> new BusinessException("申诉不存在"));

        if (appeal.getStatus() != AppealStatus.PENDING) {
            throw new BusinessException("申诉状态不正确");
        }

        if (command.isApproved()) {
            appeal.passLevel1Review(command.getReviewerId(), command.getComment());
        } else {
            appeal.rejectLevel1Review(command.getReviewerId(), command.getComment());
        }

        appealRepository.update(appeal);
    }

    /**
     * 二级审核（最终审核）
     */
    @Transactional
    public void reviewLevel2(Long appealId, ReviewCommand command) {
        Appeal appeal = appealRepository.findById(appealId)
            .orElseThrow(() -> new BusinessException("申诉不存在"));

        if (appeal.getStatus() != AppealStatus.PENDING_LEVEL2_REVIEW) {
            throw new BusinessException("申诉状态不正确");
        }

        if (command.isApproved()) {
            appeal.passLevel2Review(command.getReviewerId(), command.getComment());

            // 申诉通过，更新检查记录分数
            applyAppealResult(appeal);
        } else {
            appeal.rejectLevel2Review(command.getReviewerId(), command.getComment());
        }

        appealRepository.update(appeal);
    }

    /**
     * 应用申诉结果（更新分数）
     */
    private void applyAppealResult(Appeal appeal) {
        // 1. 标记原扣分记录为已撤销
        DeductionRecord deduction = deductionRepository.findById(appeal.getDeductionId())
            .orElseThrow(() -> new BusinessException("扣分记录不存在"));

        deduction.revoke(appeal.getId());
        deductionRepository.update(deduction);

        // 2. 重新计算检查记录分数
        TargetInspectionRecord record = recordRepository.findById(appeal.getTargetRecordId())
            .orElseThrow(() -> new BusinessException("检查记录不存在"));

        record.recalculateScore();
        recordRepository.update(record);
    }
}
```

---

## 三、Controller 示例

### 3.1 数据权限配置 Controller

```java
package com.school.management.interfaces.rest.access;

import com.school.management.application.access.DataPermissionApplicationService;
import com.school.management.interfaces.rest.access.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Tag(name = "角色数据权限配置")
public class RoleDataPermissionController {

    private final DataPermissionApplicationService service;

    @GetMapping("/data-permissions/modules")
    @PreAuthorize("hasAuthority('role:data_permission:view')")
    @Operation(summary = "获取所有数据模块（按领域分组）")
    public Map<String, List<DataModuleDTO>> getDataModules() {
        return service.getDataModules();
    }

    @GetMapping("/data-permissions/scopes")
    @PreAuthorize("hasAuthority('role:data_permission:view')")
    @Operation(summary = "获取所有数据范围类型")
    public List<DataScopeDTO> getDataScopes() {
        return service.getDataScopes();
    }

    @GetMapping("/data-permissions/scope-item-types")
    @PreAuthorize("hasAuthority('role:data_permission:view')")
    @Operation(summary = "获取自定义范围项类型")
    public List<ScopeItemTypeDTO> getScopeItemTypes() {
        return service.getScopeItemTypes();
    }

    @GetMapping("/data-permissions/scope-items")
    @PreAuthorize("hasAuthority('role:data_permission:view')")
    @Operation(summary = "搜索自定义范围可选项")
    public List<ScopeItemOption> searchScopeItems(
            @RequestParam String itemTypeCode,
            @RequestParam(required = false) String keyword) {
        return service.getScopeItemOptions(itemTypeCode, keyword);
    }

    @GetMapping("/{roleId}/data-permissions")
    @PreAuthorize("hasAuthority('role:data_permission:view')")
    @Operation(summary = "获取角色数据权限配置")
    public RoleDataPermissionDTO getRoleDataPermissions(@PathVariable Long roleId) {
        return service.getRoleDataPermissions(roleId);
    }

    @PutMapping("/{roleId}/data-permissions")
    @PreAuthorize("hasAuthority('role:data_permission:edit')")
    @Operation(summary = "更新角色数据权限配置")
    public void updateRoleDataPermissions(
            @PathVariable Long roleId,
            @RequestBody @Valid UpdateDataPermissionRequest request) {
        service.updateRoleDataPermissions(roleId, request.getModulePermissions());
    }
}
```

### 3.2 检查记录 Controller

```java
package com.school.management.interfaces.rest.inspection;

import com.school.management.application.inspection.InspectionRecordApplicationService;
import com.school.management.interfaces.rest.inspection.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inspection/records")
@RequiredArgsConstructor
@Tag(name = "检查记录管理")
public class InspectionRecordController {

    private final InspectionRecordApplicationService service;

    @GetMapping
    @PreAuthorize("hasAuthority('inspection:record:view')")
    @Operation(summary = "查询检查记录列表")
    public PageResult<RecordDTO> queryRecords(RecordQueryRequest request) {
        // 数据权限在 Mapper 层自动过滤
        return service.queryRecords(request.toCommand());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('inspection:record:create')")
    @Operation(summary = "创建检查记录")
    public Long createRecord(@RequestBody @Valid CreateRecordRequest request) {
        return service.createRecord(request.toCommand());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('inspection:record:update')")
    @Operation(summary = "更新检查记录")
    public void updateRecord(
            @PathVariable Long id,
            @RequestBody @Valid UpdateRecordRequest request) {
        service.updateRecord(id, request.toCommand());
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('inspection:record:submit')")
    @Operation(summary = "提交检查记录")
    public void submitRecord(@PathVariable Long id) {
        service.submitRecord(id);
    }
}
```

---

## 四、前端实现

### 4.1 API 模块

```typescript
// src/api/dataPermission.ts
import request from '@/utils/request'

export interface DataModuleDTO {
  code: string
  name: string
  domain: string
  filterFields: Record<string, string>
}

export interface DataScopeDTO {
  code: string
  name: string
  priority: number
}

export interface ScopeItemDTO {
  itemTypeCode: string
  scopeId: number
  scopeName: string
  includeChildren: boolean
}

export interface ModulePermissionDTO {
  moduleCode: string
  scopeCode: string
  scopeItems: ScopeItemDTO[]
}

export interface RoleDataPermissionDTO {
  roleId: number
  roleName: string
  modulePermissions: ModulePermissionDTO[]
}

// 获取所有数据模块
export function getDataModules() {
  return request.get<Record<string, DataModuleDTO[]>>('/roles/data-permissions/modules')
}

// 获取所有数据范围类型
export function getDataScopes() {
  return request.get<DataScopeDTO[]>('/roles/data-permissions/scopes')
}

// 获取自定义范围项类型
export function getScopeItemTypes() {
  return request.get<{ code: string; name: string; supportsChildren: boolean }[]>(
    '/roles/data-permissions/scope-item-types'
  )
}

// 搜索自定义范围可选项
export function searchScopeItems(itemTypeCode: string, keyword?: string) {
  return request.get<{ id: number; name: string; parentName?: string }[]>(
    '/roles/data-permissions/scope-items',
    { params: { itemTypeCode, keyword } }
  )
}

// 获取角色数据权限配置
export function getRoleDataPermissions(roleId: number) {
  return request.get<RoleDataPermissionDTO>(`/roles/${roleId}/data-permissions`)
}

// 更新角色数据权限配置
export function updateRoleDataPermissions(roleId: number, permissions: ModulePermissionDTO[]) {
  return request.put(`/roles/${roleId}/data-permissions`, { modulePermissions: permissions })
}
```

### 4.2 权限配置组件

```vue
<!-- src/components/permission/RoleDataPermissionDialog.vue -->
<template>
  <el-dialog
    v-model="visible"
    title="数据权限配置"
    width="900px"
    :close-on-click-modal="false"
  >
    <div v-loading="loading">
      <div class="role-info">
        <span>角色：</span>
        <el-tag>{{ role?.name }}</el-tag>
      </div>

      <el-tabs v-model="activeDomain">
        <el-tab-pane
          v-for="(modules, domain) in groupedModules"
          :key="domain"
          :label="domainNames[domain]"
          :name="domain"
        >
          <el-table :data="modules" border>
            <el-table-column prop="name" label="数据模块" width="180" />
            <el-table-column label="数据范围" width="200">
              <template #default="{ row }">
                <el-select
                  v-model="permissionMap[row.code].scopeCode"
                  placeholder="请选择"
                  @change="handleScopeChange(row.code)"
                >
                  <el-option
                    v-for="scope in dataScopes"
                    :key="scope.code"
                    :label="scope.name"
                    :value="scope.code"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="自定义范围">
              <template #default="{ row }">
                <template v-if="permissionMap[row.code].scopeCode === 'CUSTOM'">
                  <el-tag
                    v-for="item in permissionMap[row.code].scopeItems"
                    :key="`${item.itemTypeCode}-${item.scopeId}`"
                    closable
                    @close="removeItem(row.code, item)"
                    class="scope-tag"
                  >
                    {{ item.scopeName }}
                    <span v-if="item.includeChildren">（含下级）</span>
                  </el-tag>
                  <el-button
                    type="primary"
                    link
                    @click="openItemDialog(row.code)"
                  >
                    + 添加
                  </el-button>
                </template>
                <span v-else class="text-muted">-</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">
        保存
      </el-button>
    </template>

    <!-- 自定义范围项对话框 -->
    <CustomScopeItemDialog
      v-model="itemDialogVisible"
      :module-code="currentModuleCode"
      @confirm="handleAddItems"
    />
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getDataModules,
  getDataScopes,
  getRoleDataPermissions,
  updateRoleDataPermissions,
  type DataModuleDTO,
  type DataScopeDTO,
  type ModulePermissionDTO,
  type ScopeItemDTO
} from '@/api/dataPermission'
import CustomScopeItemDialog from './CustomScopeItemDialog.vue'

interface Props {
  modelValue: boolean
  role: { id: number; name: string } | null
}

const props = defineProps<Props>()
const emit = defineEmits(['update:modelValue', 'saved'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const loading = ref(false)
const saving = ref(false)
const activeDomain = ref('organization')

const groupedModules = ref<Record<string, DataModuleDTO[]>>({})
const dataScopes = ref<DataScopeDTO[]>([])
const permissionMap = ref<Record<string, ModulePermissionDTO>>({})

const domainNames: Record<string, string> = {
  organization: '组织管理',
  inspection: '量化检查',
  space: '场所管理',
  access: '权限管理'
}

const itemDialogVisible = ref(false)
const currentModuleCode = ref('')

watch(
  () => props.modelValue,
  async (val) => {
    if (val && props.role) {
      await loadData()
    }
  }
)

async function loadData() {
  loading.value = true
  try {
    const [modulesRes, scopesRes, permissionsRes] = await Promise.all([
      getDataModules(),
      getDataScopes(),
      getRoleDataPermissions(props.role!.id)
    ])

    groupedModules.value = modulesRes
    dataScopes.value = scopesRes

    // 初始化权限映射
    const map: Record<string, ModulePermissionDTO> = {}
    for (const modules of Object.values(modulesRes)) {
      for (const module of modules) {
        const existing = permissionsRes.modulePermissions.find(
          (p) => p.moduleCode === module.code
        )
        map[module.code] = existing || {
          moduleCode: module.code,
          scopeCode: 'DEPARTMENT_AND_BELOW',
          scopeItems: []
        }
      }
    }
    permissionMap.value = map
  } finally {
    loading.value = false
  }
}

function handleScopeChange(moduleCode: string) {
  if (permissionMap.value[moduleCode].scopeCode !== 'CUSTOM') {
    permissionMap.value[moduleCode].scopeItems = []
  }
}

function openItemDialog(moduleCode: string) {
  currentModuleCode.value = moduleCode
  itemDialogVisible.value = true
}

function handleAddItems(items: ScopeItemDTO[]) {
  const existing = permissionMap.value[currentModuleCode.value].scopeItems
  for (const item of items) {
    const exists = existing.some(
      (e) => e.itemTypeCode === item.itemTypeCode && e.scopeId === item.scopeId
    )
    if (!exists) {
      existing.push(item)
    }
  }
}

function removeItem(moduleCode: string, item: ScopeItemDTO) {
  const items = permissionMap.value[moduleCode].scopeItems
  const index = items.findIndex(
    (i) => i.itemTypeCode === item.itemTypeCode && i.scopeId === item.scopeId
  )
  if (index !== -1) {
    items.splice(index, 1)
  }
}

async function handleSave() {
  saving.value = true
  try {
    const permissions = Object.values(permissionMap.value)
    await updateRoleDataPermissions(props.role!.id, permissions)
    ElMessage.success('保存成功')
    emit('saved')
    visible.value = false
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.role-info {
  margin-bottom: 16px;
}
.scope-tag {
  margin-right: 8px;
  margin-bottom: 4px;
}
.text-muted {
  color: #909399;
}
</style>
```

---

## 五、实施阶段

### 5.1 阶段一：基础设施（1-2周）

**目标**：完成数据库表创建和基础框架

**任务清单**：
- [ ] 执行数据库迁移脚本（权限表）
- [ ] 创建 DataPermission 注解
- [ ] 实现 MyBatis 拦截器（基础版）
- [ ] 实现 UserContextHolder
- [ ] 创建数据模块注册表

### 5.2 阶段二：权限系统（2-3周）

**目标**：完成权限系统核心功能

**任务清单**：
- [ ] 实现 DataPermissionApplicationService
- [ ] 实现角色数据权限 CRUD
- [ ] 实现自定义范围项管理
- [ ] 实现多角色权限合并
- [ ] 实现 Redis 缓存
- [ ] 前端权限配置组件

### 5.3 阶段三：检查系统（3-4周）

**目标**：完成检查系统核心功能

**任务清单**：
- [ ] 执行数据库迁移脚本（检查表）
- [ ] 实现模板管理
- [ ] 实现项目管理
- [ ] 实现任务生成
- [ ] 实现检查录入
- [ ] 实现申诉流程
- [ ] 实现整改流程
- [ ] 实现汇总排名

### 5.4 阶段四：集成测试（1周）

**目标**：确保系统稳定可用

**任务清单**：
- [ ] 单元测试覆盖
- [ ] 集成测试
- [ ] 权限功能测试
- [ ] 性能测试
- [ ] 安全测试

---

**文档版本**: 3.0
**最后更新**: 2026-01-31
