# 统一扩展框架设计 — 通用平台架构基石

> **状态**: 待评审
> **日期**: 2026-04-07
> **影响范围**: 组织管理、场所管理、用户管理 + 所有行业垂直模块

---

## 一、设计目标

**一句话**: 新增一个行业垂直(如从教育切换到物业)，不改一行核心代码，只写配置和插件。

```
核心层 (通用，永不因行业而改)
  ├── 组织管理: 树结构 + 类型 + 动态属性
  ├── 场所管理: 空间结构 + 类型 + 动态属性
  ├── 用户管理: 账号角色 + 类型 + 动态属性
  └── 统一扩展框架: 类型系统 + 动态表单 + SPI + 事件

行业层 (插件化，可插拔)
  ├── 教育插件: 班级/课程/排课/考试/成绩
  ├── 物业插件: 楼栋/房间/报修/收费
  ├── 医疗插件: 科室/病房/排班/病历
  └── ...
```

## 二、三层架构

### Layer 1: 统一类型系统 (已有，需增强)

**现状**: org_unit_types 有 metadata_schema，但 places/users 没有统一的类型扩展。

**目标**: 三个模块共享同一套类型元数据机制。

```
entity_type_configs (统一类型配置表)
  ├── entity_type: 'ORG_UNIT' | 'PLACE' | 'USER'
  ├── type_code: 'CLASS' | 'CLASSROOM' | 'TEACHER'
  ├── type_name: '班级' | '教室' | '教师'
  ├── parent_type_code: 类型层级
  ├── allowed_child_type_codes: 允许的子类型
  ├── metadata_schema: JSON — 定义扩展字段
  │   {
  │     "fields": [
  │       {
  │         "key": "gradeId",
  │         "label": "年级",
  │         "type": "relation",       // text|number|date|select|relation|user|boolean
  │         "target": "grades",       // relation 类型的目标表
  │         "required": true,
  │         "group": "基本信息"
  │       },
  │       {
  │         "key": "headTeacher",
  │         "label": "班主任",
  │         "type": "user",
  │         "role": "TEACHER",
  │         "required": false,
  │         "group": "人员配置"
  │       }
  │     ],
  │     "groups": ["基本信息", "人员配置", "教学设置"]
  │   }
  ├── features: JSON — 功能开关
  │   { "hasStudents": true, "hasTimetable": true, "hasAttendance": true }
  ├── lifecycle_hooks: JSON — SPI 钩子配置
  │   { "afterCreate": ["class-init", "auto-assign-room"] }
  └── ui_config: JSON — 前端展示配置
      { "icon": "school", "color": "#2563eb", "listColumns": ["gradeId","headTeacher"] }
```

**对应实体表的改动**: 无。org_units/places/users 的 attributes JSON 字段已存在，存储所有扩展数据。

### Layer 2: 动态表单引擎

**前端组件**: `DynamicForm.vue`

```vue
<!-- 任何地方需要扩展字段时，只需传入 schema -->
<DynamicForm
  :schema="typeConfig.metadataSchema"
  :values="entity.attributes"
  @change="onAttributeChange"
/>
```

**支持的字段类型:**

| type | 渲染组件 | 说明 |
|------|---------|------|
| text | input | 单行文本 |
| textarea | textarea | 多行文本 |
| number | input[number] | 数字 |
| date | input[date] | 日期 |
| boolean | checkbox | 布尔 |
| select | select | 下拉选择(options 配置) |
| relation | select(远程) | 关联其他实体(target 表) |
| user | select(用户) | 选择用户(可按 role 过滤) |
| tags | multi-select | 标签/多选 |
| json | code-editor | JSON 编辑器 |

**关键设计**: 
- 字段分组(group)控制表单布局
- required 控制前端校验
- 后端在保存 attributes 时也根据 schema 校验

### Layer 3: SPI 扩展点

**接口定义:**

```java
public interface EntityLifecycleExtension {
    
    /** 支持的实体类型: ORG_UNIT / PLACE / USER */
    String getEntityType();
    
    /** 支持的类型编码: CLASS / CLASSROOM / TEACHER / * */
    String getTypeCode();
    
    /** 优先级(多个扩展时的执行顺序) */
    default int getOrder() { return 0; }
    
    /** 创建前: 可修改属性、可拦截 */
    default void beforeCreate(ExtensionContext ctx) {}
    
    /** 创建后: 初始化关联数据 */
    default void afterCreate(ExtensionContext ctx) {}
    
    /** 更新前 */
    default void beforeUpdate(ExtensionContext ctx) {}
    
    /** 更新后 */
    default void afterUpdate(ExtensionContext ctx) {}
    
    /** 删除前: 可拦截(如有学生不能删班级) */
    default void beforeDelete(ExtensionContext ctx) {}
    
    /** 删除后: 清理关联数据 */
    default void afterDelete(ExtensionContext ctx) {}
    
    /** 自定义校验 */
    default List<String> validate(ExtensionContext ctx) { return List.of(); }
}

public class ExtensionContext {
    String entityType;        // ORG_UNIT / PLACE / USER
    String typeCode;          // CLASS / CLASSROOM / TEACHER
    Long entityId;
    Map<String, Object> attributes;     // 当前属性
    Map<String, Object> oldAttributes;  // 旧属性(更新时)
    Long operatorId;
}
```

**扩展注册**: Spring 自动扫描所有 `@Component` 实现类。

**调度器:**

```java
@Service
public class ExtensionDispatcher {
    
    @Autowired
    private List<EntityLifecycleExtension> extensions;
    
    public void fireBeforeCreate(String entityType, String typeCode, ExtensionContext ctx) {
        extensions.stream()
            .filter(e -> matches(e, entityType, typeCode))
            .sorted(Comparator.comparingInt(EntityLifecycleExtension::getOrder))
            .forEach(e -> e.beforeCreate(ctx));
    }
    // ... fireAfterCreate, fireBeforeUpdate, etc.
}
```

**教育行业的扩展示例:**

```java
@Component
public class ClassLifecycleExtension implements EntityLifecycleExtension {
    
    public String getEntityType() { return "ORG_UNIT"; }
    public String getTypeCode() { return "CLASS"; }
    
    public void afterCreate(ExtensionContext ctx) {
        // 自动为新班级创建默认的课表结构
        // 通知教务系统有新班级
    }
    
    public void beforeDelete(ExtensionContext ctx) {
        // 检查是否有在读学生，有则拦截删除
        int studentCount = studentRepo.countByClassOrgUnitId(ctx.entityId);
        if (studentCount > 0) {
            throw new BusinessException("班级下有 " + studentCount + " 名在读学生，无法删除");
        }
    }
    
    public List<String> validate(ExtensionContext ctx) {
        List<String> errors = new ArrayList<>();
        if (ctx.attributes.get("gradeId") == null) {
            errors.add("班级必须关联年级");
        }
        return errors;
    }
}
```

### Layer 4: 领域事件 (已有，增强)

```java
// 核心模块发布通用事件
eventPublisher.publish(new EntityCreatedEvent("ORG_UNIT", "CLASS", orgUnitId, attributes));
eventPublisher.publish(new EntityUpdatedEvent("PLACE", "CLASSROOM", placeId, oldAttrs, newAttrs));

// 任何模块可监听
@EventListener
public void onClassCreated(EntityCreatedEvent event) {
    if ("ORG_UNIT".equals(event.getEntityType()) && "CLASS".equals(event.getTypeCode())) {
        // 教育模块的异步处理: 统计更新、通知推送等
    }
}
```

## 三、数据模型变化

### 新增: entity_type_configs (统一类型配置)

```sql
CREATE TABLE entity_type_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    entity_type VARCHAR(20) NOT NULL COMMENT 'ORG_UNIT/PLACE/USER',
    type_code VARCHAR(50) NOT NULL,
    type_name VARCHAR(50) NOT NULL,
    category VARCHAR(30) COMMENT '大类',
    parent_type_code VARCHAR(50),
    allowed_child_type_codes JSON,
    metadata_schema JSON COMMENT '扩展字段定义',
    features JSON COMMENT '功能开关',
    lifecycle_hooks JSON COMMENT 'SPI钩子配置',
    ui_config JSON COMMENT '前端展示配置',
    is_system TINYINT DEFAULT 0,
    is_enabled TINYINT DEFAULT 1,
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_entity_type (entity_type, type_code)
) COMMENT '统一实体类型配置';
```

### 保留: 现有表

- org_units: 保留，attributes 存扩展数据
- places: 保留，attributes 存扩展数据  
- users: 可能需要加 attributes JSON 字段
- org_unit_types: 逐步迁移到 entity_type_configs，过渡期并存

### 废弃: 行业扩展表

- classes → 数据迁移到 org_units.attributes
- (未来) dormitory_buildings → places.attributes

## 四、前端架构

```
components/extension/
├── DynamicForm.vue           ← 根据 schema 渲染表单
├── DynamicTable.vue          ← 根据 schema 渲染列表列
├── DynamicDetail.vue         ← 根据 schema 渲染详情
├── fields/                   ← 各字段类型组件
│   ├── TextField.vue
│   ├── NumberField.vue
│   ├── DateField.vue
│   ├── SelectField.vue
│   ├── RelationField.vue     ← 关联选择器(远程搜索)
│   ├── UserField.vue         ← 用户选择器
│   ├── BooleanField.vue
│   └── TagsField.vue
└── TypeConfigEditor.vue      ← 管理员编辑 metadata_schema 的可视化工具
```

**使用方式(以组织管理为例):**

```vue
<!-- OrgUnitForm.vue -->
<template>
  <!-- 通用字段: 名称、编码(永远存在) -->
  <div class="tm-field">
    <label class="tm-label">名称</label>
    <input v-model="form.unitName" class="tm-input" />
  </div>
  
  <!-- 动态扩展字段: 根据类型配置自动渲染 -->
  <DynamicForm
    v-if="typeSchema"
    :schema="typeSchema"
    :values="form.attributes"
    @change="form.attributes = $event"
  />
</template>
```

## 五、实施路径

### Phase 1: 基础框架 (本次)
1. 创建 entity_type_configs 表
2. 迁移 org_unit_types 数据
3. 实现 DynamicForm.vue 组件
4. 改造 OrgUnitForm 使用动态表单
5. 实现 EntityLifecycleExtension 接口 + 调度器
6. 实现第一个扩展: ClassLifecycleExtension

### Phase 2: 全面覆盖
7. 场所管理接入动态表单
8. 用户管理接入动态表单
9. 实现 TypeConfigEditor (可视化配置 schema)
10. 迁移 classes 表数据到 attributes

### Phase 3: 高级功能
11. 计算字段(如: 学生数自动统计)
12. 字段联动(如: 选了年级自动过滤专业)
13. 导入/导出 schema 配置
14. 行业模板(一键导入"教育行业"全套类型配置)

## 六、与现有系统的兼容

**过渡期策略:**
- entity_type_configs 与 org_unit_types 并存
- classes 表保留，但新数据同时写入 attributes
- 前端同时支持旧表单和动态表单
- 逐步迁移，不一步到位
