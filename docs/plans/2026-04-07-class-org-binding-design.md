# 班级管理与组织管理联动重构设计

> **日期**: 2026-04-07
> **核心原则**: 组织管理纯通用，班级是组织节点的行业扩展

## 一、架构

```
org_units (通用组织树)
  ├── 枣庄技师学院 (SCHOOL)
  │   ├── 经济与信息技术系 (DEPARTMENT) 
  │   │   ├── 2025级 (GRADE)            ← 由组织管理创建
  │   │   │   ├── 高一1班 (CLASS)       ← 由组织管理创建
  │   │   │   └── 高一2班 (CLASS)       ← 由组织管理创建
  │   │   └── 计算机教研室 (TEACHING_GROUP)
  │   └── 行政处室 (ADMIN_OFFICE)

classes (班级扩展表) — 关联 org_units
  ├── org_unit_id → 高一1班的org_unit.id
  │   + 年级、专业、班主任、入学年份、学制...
  └── org_unit_id → 高一2班的org_unit.id
      + 年级、专业、班主任、入学年份、学制...
```

## 二、联动机制

### 自动绑定
当在组织管理中创建类型为 CLASS 的节点时:
- org_units 创建成功
- 自动在 classes 表插入一条记录，org_unit_id 指向新节点
- class_name 同步 org_unit.unit_name
- org_unit_id 通过树路径自动推导 grade_id (父级年级节点)

### 班级管理页面
不再独立创建班级，而是:
1. 列出所有 type_code=CLASS 的组织节点
2. 每个节点关联的 classes 扩展信息可编辑
3. 编辑字段: 专业、班主任、入学年份、学制、班级类型等

### 同步策略
- org_units.unit_name 修改 → classes.class_name 同步
- org_units 删除 → classes 软删除
- classes 表不允许独立创建(必须从组织树发起)

## 三、后端改动

### SchoolClassController 改造
- GET /students/classes → 查 org_units WHERE type_code='CLASS' + LEFT JOIN classes
- POST /students/classes → 不再直接创建，由组织管理联动创建
- PUT /students/classes/{id} → 只更新 classes 表的扩展字段
- 新增: POST /students/classes/bind/{orgUnitId} → 手动绑定已有节点

### OrgUnit 创建时联动
在 org_unit 创建后端逻辑中，当 type_code 对应 category=GROUP 时，
自动插入 classes 记录。

## 四、前端改动

### 班级管理页面重构 (学生管理 → 班级管理)
- 列表: 从 org_units 树过滤 CLASS 类型节点
- 每行显示: 组织名称 + 扩展信息(专业/班主任/人数)
- 编辑: 只编辑行业扩展字段，不编辑组织基本信息
- 组织基本信息(名称/编码)在组织管理中修改

### 删除 ClassForm 在组织管理中的引用 ✅ (已完成)
