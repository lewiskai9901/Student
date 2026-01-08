# 数据权限设计方案

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 实现基于注解+MyBatis拦截器的数据权限控制，使班主任只能查看本班学生，主任可查看本系学生。

**Architecture:** 采用若依(RuoYi)框架的成熟方案，通过 `@DataScope` 注解标记需要数据过滤的方法，AOP切面解析注解计算数据范围，MyBatis拦截器自动拼接SQL WHERE条件。

**Tech Stack:** Spring AOP, MyBatis-Plus Interceptor, ThreadLocal

---

## 1. 整体架构

```
Controller (@PreAuthorize - 功能权限)
    ↓
Service (@DataScope - 数据权限注解)
    ↓
AOP切面 (解析注解，计算数据范围，存入ThreadLocal)
    ↓
MyBatis拦截器 (读取ThreadLocal，自动拼接WHERE条件)
    ↓
数据库 (执行带过滤条件的SQL)
```

## 2. 核心组件

### 2.1 @DataScope 注解

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {
    // 部门表别名
    String deptAlias() default "";
    // 班级表别名
    String classAlias() default "";
    // 用户表别名
    String userAlias() default "";
    // 模块编码
    String module() default "student";
}
```

### 2.2 DataScopeAspect 切面

- 拦截带有 @DataScope 注解的方法
- 根据当前用户角色获取数据权限配置
- 计算可访问的部门/班级ID列表
- 将过滤条件存入 DataPermissionContextHolder

### 2.3 DataPermissionInterceptor 拦截器

- 实现 MyBatis Interceptor 接口
- 拦截 SELECT 语句
- 从 ThreadLocal 读取过滤条件
- 自动拼接 WHERE 或 AND 条件

### 2.4 DataPermissionContextHolder

- 基于 ThreadLocal 存储当前请求的数据权限条件
- 请求结束后自动清理

## 3. 数据范围类型

| DataScope | 说明 | SQL条件示例 |
|-----------|------|-------------|
| ALL(1) | 全部数据 | 不追加条件 |
| DEPARTMENT(2) | 本部门 | `d.id = 100` |
| GRADE(3) | 本年级 | `c.grade_id IN (...)` |
| CLASS(4) | 本班级 | `c.id IN (1,2,3)` |
| SELF(5) | 仅本人 | `u.id = 当前用户ID` |

## 4. 使用示例

```java
@Service
public class StudentServiceImpl {

    @DataScope(classAlias = "c", deptAlias = "d", module = "student")
    public List<Student> getStudentList(StudentQueryRequest request) {
        return studentMapper.selectStudentList(request);
    }
}
```

```xml
<!-- StudentMapper.xml -->
<select id="selectStudentList" resultType="Student">
    SELECT s.*, c.name as className, d.name as deptName
    FROM students s
    LEFT JOIN classes c ON s.class_id = c.id
    LEFT JOIN departments d ON c.department_id = d.id
    WHERE s.deleted = 0
    ${dataScope}  <!-- 数据权限条件占位符 -->
</select>
```

## 5. 现有资源复用

项目已有以下可复用组件：
- `DataScope` 枚举 - 5种数据范围定义
- `RoleDataPermission` 实体 - 角色数据权限配置表
- `DataPermissionService` - 数据权限判断接口及实现
- `role_data_permissions` 数据库表

## 6. 需要新增的组件

1. `@DataScope` 注解
2. `DataScopeAspect` AOP切面
3. `DataPermissionContextHolder` ThreadLocal工具类
4. `DataPermissionInterceptor` MyBatis拦截器
5. 修改现有Mapper XML添加 `${dataScope}` 占位符

## 7. 应用模块

需要应用数据权限的模块：
- student - 学生管理
- class - 班级管理
- dormitory - 宿舍管理
- check_record - 检查记录
- task - 任务管理
