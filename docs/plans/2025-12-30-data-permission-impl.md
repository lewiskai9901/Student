# 数据权限实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 实现基于 @DataScope 注解 + MyBatis 拦截器的数据权限控制系统

**Architecture:** AOP切面拦截带 @DataScope 注解的方法，根据用户角色计算数据范围，通过 ThreadLocal 传递给 MyBatis 拦截器自动拼接 SQL WHERE 条件

**Tech Stack:** Spring AOP, MyBatis-Plus Interceptor, ThreadLocal

---

## Task 1: 创建 @DataScope 注解

**Files:**
- Create: `backend/src/main/java/com/school/management/annotation/DataScope.java`

**Step 1: 创建注解文件**

```java
package com.school.management.annotation;

import java.lang.annotation.*;

/**
 * 数据权限注解
 * 标记在Service方法上，自动进行数据范围过滤
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {
    /**
     * 部门表别名
     */
    String deptAlias() default "";

    /**
     * 班级表别名
     */
    String classAlias() default "";

    /**
     * 用户表别名
     */
    String userAlias() default "";

    /**
     * 模块编码，用于查询角色的数据权限配置
     * 可选值: student, class, dormitory, check_record, task
     */
    String module() default "student";
}
```

**Step 2: 验证编译**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add backend/src/main/java/com/school/management/annotation/DataScope.java
git commit -m "feat(permission): add @DataScope annotation for data permission"
```

---

## Task 2: 创建 DataPermissionContextHolder

**Files:**
- Create: `backend/src/main/java/com/school/management/util/DataPermissionContextHolder.java`

**Step 1: 创建 ThreadLocal 工具类**

```java
package com.school.management.util;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据权限上下文持有者
 * 使用 ThreadLocal 存储当前请求的数据权限过滤条件
 */
public class DataPermissionContextHolder {

    private static final ThreadLocal<Map<String, Object>> CONTEXT = ThreadLocal.withInitial(HashMap::new);

    /**
     * 设置数据权限SQL片段
     */
    public static void setDataScopeSql(String sql) {
        CONTEXT.get().put("dataScopeSql", sql);
    }

    /**
     * 获取数据权限SQL片段
     */
    public static String getDataScopeSql() {
        return (String) CONTEXT.get().get("dataScopeSql");
    }

    /**
     * 设置是否启用数据权限
     */
    public static void setEnabled(boolean enabled) {
        CONTEXT.get().put("enabled", enabled);
    }

    /**
     * 是否启用数据权限
     */
    public static boolean isEnabled() {
        Boolean enabled = (Boolean) CONTEXT.get().get("enabled");
        return enabled != null && enabled;
    }

    /**
     * 清除上下文
     */
    public static void clear() {
        CONTEXT.remove();
    }
}
```

**Step 2: 验证编译**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add backend/src/main/java/com/school/management/util/DataPermissionContextHolder.java
git commit -m "feat(permission): add DataPermissionContextHolder for ThreadLocal context"
```

---

## Task 3: 创建 DataScopeAspect 切面

**Files:**
- Create: `backend/src/main/java/com/school/management/aspect/DataScopeAspect.java`

**Step 1: 创建 AOP 切面**

```java
package com.school.management.aspect;

import com.school.management.annotation.DataScope;
import com.school.management.enums.DataScope as DataScopeEnum;
import com.school.management.security.CustomUserDetails;
import com.school.management.service.DataPermissionService;
import com.school.management.util.DataPermissionContextHolder;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据权限切面
 * 拦截带有 @DataScope 注解的方法，自动计算并设置数据范围过滤条件
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DataScopeAspect {

    private final DataPermissionService dataPermissionService;

    @Before("@annotation(dataScope)")
    public void doBefore(JoinPoint point, DataScope dataScope) {
        // 清除之前的上下文
        DataPermissionContextHolder.clear();

        CustomUserDetails user = SecurityUtils.getCurrentUserDetails();
        if (user == null) {
            log.debug("No authenticated user, skip data scope");
            return;
        }

        // 获取数据范围
        DataScopeEnum scope = dataPermissionService.getDataScope(dataScope.module());
        log.debug("User {} data scope for module {}: {}", user.getUsername(), dataScope.module(), scope);

        // 全部数据权限，不需要过滤
        if (scope == DataScopeEnum.ALL) {
            return;
        }

        // 构建SQL条件
        StringBuilder sql = new StringBuilder();

        switch (scope) {
            case DEPARTMENT:
                if (StringUtils.hasText(dataScope.deptAlias()) && user.getDepartmentId() != null) {
                    sql.append(String.format(" AND %s.id = %d", dataScope.deptAlias(), user.getDepartmentId()));
                }
                break;

            case GRADE:
                // 本年级：通过班级表过滤
                if (StringUtils.hasText(dataScope.classAlias())) {
                    List<Long> classIds = dataPermissionService.getAccessibleClassIds(dataScope.module());
                    if (classIds != null && !classIds.isEmpty()) {
                        String ids = classIds.stream().map(String::valueOf).collect(Collectors.joining(","));
                        sql.append(String.format(" AND %s.id IN (%s)", dataScope.classAlias(), ids));
                    } else {
                        // 没有可访问的班级，返回空结果
                        sql.append(" AND 1=0");
                    }
                }
                break;

            case CLASS:
                if (StringUtils.hasText(dataScope.classAlias())) {
                    List<Long> classIds = dataPermissionService.getAccessibleClassIds(dataScope.module());
                    if (classIds != null && !classIds.isEmpty()) {
                        String ids = classIds.stream().map(String::valueOf).collect(Collectors.joining(","));
                        sql.append(String.format(" AND %s.id IN (%s)", dataScope.classAlias(), ids));
                    } else {
                        sql.append(" AND 1=0");
                    }
                }
                break;

            case SELF:
                if (StringUtils.hasText(dataScope.userAlias())) {
                    sql.append(String.format(" AND %s.id = %d", dataScope.userAlias(), user.getUserId()));
                } else if (StringUtils.hasText(dataScope.classAlias()) && user.getClassId() != null) {
                    sql.append(String.format(" AND %s.id = %d", dataScope.classAlias(), user.getClassId()));
                }
                break;

            default:
                break;
        }

        if (sql.length() > 0) {
            DataPermissionContextHolder.setDataScopeSql(sql.toString());
            DataPermissionContextHolder.setEnabled(true);
            log.debug("Data scope SQL: {}", sql);
        }
    }

    @After("@annotation(dataScope)")
    public void doAfter(JoinPoint point, DataScope dataScope) {
        // 清除上下文，防止内存泄漏
        DataPermissionContextHolder.clear();
    }
}
```

**Step 2: 验证编译**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add backend/src/main/java/com/school/management/aspect/DataScopeAspect.java
git commit -m "feat(permission): add DataScopeAspect for intercepting @DataScope methods"
```

---

## Task 4: 创建 MyBatis 数据权限拦截器

**Files:**
- Create: `backend/src/main/java/com/school/management/config/DataPermissionInterceptor.java`

**Step 1: 创建拦截器**

```java
package com.school.management.config;

import com.school.management.util.DataPermissionContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.Properties;

/**
 * MyBatis 数据权限拦截器
 * 自动在 SELECT 语句中追加数据权限过滤条件
 */
@Slf4j
@Component
@Intercepts({
    @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class DataPermissionInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 检查是否启用数据权限
        if (!DataPermissionContextHolder.isEnabled()) {
            return invocation.proceed();
        }

        String dataScopeSql = DataPermissionContextHolder.getDataScopeSql();
        if (!StringUtils.hasText(dataScopeSql)) {
            return invocation.proceed();
        }

        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);

        // 获取 MappedStatement
        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");

        // 只处理 SELECT 语句
        if (mappedStatement.getSqlCommandType() != SqlCommandType.SELECT) {
            return invocation.proceed();
        }

        // 获取原始 SQL
        BoundSql boundSql = statementHandler.getBoundSql();
        String originalSql = boundSql.getSql();

        // 追加数据权限条件
        String newSql = addDataScopeCondition(originalSql, dataScopeSql);

        // 替换 SQL
        setFieldValue(boundSql, "sql", newSql);

        log.debug("Original SQL: {}", originalSql);
        log.debug("Modified SQL: {}", newSql);

        return invocation.proceed();
    }

    /**
     * 在 SQL 中追加数据权限条件
     */
    private String addDataScopeCondition(String originalSql, String dataScopeSql) {
        String sql = originalSql.trim();
        String lowerSql = sql.toLowerCase();

        // 查找 WHERE 子句位置
        int whereIndex = lowerSql.lastIndexOf(" where ");
        int orderIndex = lowerSql.lastIndexOf(" order by ");
        int groupIndex = lowerSql.lastIndexOf(" group by ");
        int limitIndex = lowerSql.lastIndexOf(" limit ");

        // 确定插入位置
        int insertIndex = sql.length();
        if (limitIndex > 0) insertIndex = Math.min(insertIndex, limitIndex);
        if (orderIndex > 0) insertIndex = Math.min(insertIndex, orderIndex);
        if (groupIndex > 0) insertIndex = Math.min(insertIndex, groupIndex);

        if (whereIndex > 0) {
            // 已有 WHERE，在适当位置插入 AND 条件
            String beforeInsert = sql.substring(0, insertIndex);
            String afterInsert = sql.substring(insertIndex);
            return beforeInsert + " " + dataScopeSql + " " + afterInsert;
        } else {
            // 没有 WHERE，添加 WHERE 1=1 然后追加条件
            String beforeInsert = sql.substring(0, insertIndex);
            String afterInsert = sql.substring(insertIndex);
            return beforeInsert + " WHERE 1=1 " + dataScopeSql + " " + afterInsert;
        }
    }

    /**
     * 通过反射设置字段值
     */
    private void setFieldValue(Object obj, String fieldName, Object value) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            log.error("Failed to set field value: {}", fieldName, e);
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // 可配置属性
    }
}
```

**Step 2: 验证编译**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add backend/src/main/java/com/school/management/config/DataPermissionInterceptor.java
git commit -m "feat(permission): add MyBatis DataPermissionInterceptor for auto SQL modification"
```

---

## Task 5: 注册拦截器到 MyBatis 配置

**Files:**
- Modify: `backend/src/main/java/com/school/management/config/MyBatisConfig.java`

**Step 1: 检查现有配置**

查看是否已有 MyBatisConfig，如果没有则创建。

**Step 2: 添加拦截器注册**

如果文件不存在，创建：

```java
package com.school.management.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * MyBatis 配置
 */
@Configuration
public class MyBatisConfig {

    @Autowired
    private List<SqlSessionFactory> sqlSessionFactoryList;

    @Autowired
    private DataPermissionInterceptor dataPermissionInterceptor;

    @PostConstruct
    public void addInterceptor() {
        for (SqlSessionFactory sqlSessionFactory : sqlSessionFactoryList) {
            sqlSessionFactory.getConfiguration().addInterceptor(dataPermissionInterceptor);
        }
    }
}
```

**Step 3: 验证编译**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

**Step 4: Commit**

```bash
git add backend/src/main/java/com/school/management/config/MyBatisConfig.java
git commit -m "feat(permission): register DataPermissionInterceptor in MyBatis config"
```

---

## Task 6: 在 StudentService 中应用数据权限

**Files:**
- Modify: `backend/src/main/java/com/school/management/service/impl/StudentServiceImpl.java`

**Step 1: 添加 @DataScope 注解到查询方法**

在 `getStudentList` 或类似的列表查询方法上添加注解：

```java
import com.school.management.annotation.DataScope;

// 在查询学生列表的方法上添加
@DataScope(classAlias = "c", deptAlias = "d", module = "student")
public IPage<StudentResponse> getStudentPage(StudentQueryRequest request) {
    // 现有实现保持不变
}
```

**Step 2: 验证编译**

Run: `cd backend && mvn compile -q`
Expected: BUILD SUCCESS

**Step 3: Commit**

```bash
git add backend/src/main/java/com/school/management/service/impl/StudentServiceImpl.java
git commit -m "feat(permission): apply @DataScope to StudentService query methods"
```

---

## Task 7: 配置角色数据权限测试数据

**Files:**
- Create: `backend/sql/data_permission_test_data.sql`

**Step 1: 创建测试数据 SQL**

```sql
-- 为班主任角色配置学生模块的数据权限（本班级）
INSERT INTO role_data_permissions (id, role_id, module_code, data_scope, created_at, updated_at, deleted)
SELECT
    FLOOR(RAND() * 9000000000000000000) + 1000000000000000000,
    r.id,
    'student',
    4, -- CLASS: 本班级
    NOW(),
    NOW(),
    0
FROM roles r
WHERE r.code = 'CLASS_TEACHER'
ON DUPLICATE KEY UPDATE data_scope = 4;

-- 为教务主任角色配置学生模块的数据权限（本部门）
INSERT INTO role_data_permissions (id, role_id, module_code, data_scope, created_at, updated_at, deleted)
SELECT
    FLOOR(RAND() * 9000000000000000000) + 1000000000000000000,
    r.id,
    'student',
    2, -- DEPARTMENT: 本部门
    NOW(),
    NOW(),
    0
FROM roles r
WHERE r.code = 'ACADEMIC_DIRECTOR'
ON DUPLICATE KEY UPDATE data_scope = 2;

-- 为学校管理员配置学生模块的数据权限（全部）
INSERT INTO role_data_permissions (id, role_id, module_code, data_scope, created_at, updated_at, deleted)
SELECT
    FLOOR(RAND() * 9000000000000000000) + 1000000000000000000,
    r.id,
    'student',
    1, -- ALL: 全部
    NOW(),
    NOW(),
    0
FROM roles r
WHERE r.code IN ('SUPER_ADMIN', 'SCHOOL_ADMIN')
ON DUPLICATE KEY UPDATE data_scope = 1;
```

**Step 2: 执行 SQL**

Run: `mysql -uroot -p123456 student_management < backend/sql/data_permission_test_data.sql`

**Step 3: Commit**

```bash
git add backend/sql/data_permission_test_data.sql
git commit -m "feat(permission): add test data for role data permissions"
```

---

## Task 8: 功能测试

**Step 1: 启动后端服务**

Run: `cd backend && mvn spring-boot:run -DskipTests`

**Step 2: 使用班主任账号测试**

```bash
# 登录班主任
curl -s -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"teacher_wang","password":"admin123"}'

# 使用返回的 token 查询学生列表
curl -s -X GET "http://localhost:8080/api/students?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer <token>"
```

Expected: 只返回班主任所管理班级的学生

**Step 3: 使用管理员账号测试**

```bash
# 登录管理员
curl -s -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 查询学生列表
curl -s -X GET "http://localhost:8080/api/students?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer <token>"
```

Expected: 返回全部学生数据

**Step 4: Commit**

```bash
git add .
git commit -m "feat(permission): complete data permission implementation"
```

---

## 验收标准

1. [x] @DataScope 注解可正常解析
2. [x] AOP 切面能拦截带注解的方法
3. [x] MyBatis 拦截器能自动修改 SQL
4. [x] 班主任只能查看本班学生
5. [x] 主任能查看本部门学生
6. [x] 管理员能查看全部学生
