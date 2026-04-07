# 统一扩展框架 — 最终架构设计

> **状态**: 待评审
> **日期**: 2026-04-07
> **模式**: C — 插件注册基础 + 管理员追加自定义（Odoo Studio 路线）
> **影响**: 全平台架构基石

---

## 一、一句话总结

**插件定义核心（代码保障），管理员追加扩展（零代码灵活），两者合并为最终 schema。**

## 二、架构全景

```
┌─────────────────────────────────────────────────────────────┐
│                    统一扩展框架                                │
│                                                             │
│  ┌───────────────┐  ┌───────────────┐  ┌────────────────┐  │
│  │ 类型注册表     │  │ SPI 生命周期   │  │ 动态表单引擎   │  │
│  │ (entity_type_ │  │ (Extension    │  │ (DynamicForm  │  │
│  │  configs)     │  │  Dispatcher)  │  │  .vue)        │  │
│  └───────┬───────┘  └───────┬───────┘  └───────┬────────┘  │
│          │                  │                   │           │
│          │    schema合并     │    钩子调度        │  渲染      │
│          └──────┬───────────┘                   │           │
│                 ▼                                ▼           │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              核心通用模块（不含任何行业代码）            │   │
│  │  org_units (attributes)                              │   │
│  │  places (attributes)                                 │   │
│  │  users (attributes)                                  │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
          ▲                    ▲                    ▲
          │                    │                    │
┌─────────┴────┐  ┌───────────┴────┐  ┌───────────┴────┐
│ 教育行业插件  │  │ 物业行业插件    │  │ 医疗行业插件    │
│              │  │ (未来)         │  │ (未来)         │
│ ClassPlugin  │  │ BuildingPlugin │  │ WardPlugin     │
│ TeacherPlugin│  │ TenantPlugin   │  │ DoctorPlugin   │
│ StudentPlugin│  │ RepairPlugin   │  │ NursePlugin    │
│ DormPlugin   │  │                │  │                │
└──────────────┘  └────────────────┘  └────────────────┘
```

## 三、SPI 接口定义

### 3.1 实体类型插件接口

```java
/**
 * 行业插件通过实现此接口向平台注册实体类型。
 * Spring 启动时自动扫描所有实现，注册到 entity_type_configs 表。
 */
public interface EntityTypePlugin {

    // ========== 类型注册（启动时调用一次） ==========

    /** 实体类型: ORG_UNIT / PLACE / USER */
    String getEntityType();

    /** 类型编码: CLASS / CLASSROOM / TEACHER */
    String getTypeCode();

    /** 类型显示名 */
    String getTypeName();

    /** 类型分类 */
    String getCategory();

    /** 父类型编码（为null则是顶级类型） */
    default String getParentTypeCode() { return null; }

    /** 允许的子类型编码 */
    default List<String> getAllowedChildTypeCodes() { return List.of(); }

    /** 系统字段定义（管理员不能删除） */
    List<FieldDefinition> getSystemFields();

    /** 功能开关 */
    default Map<String, Boolean> getFeatures() { return Map.of(); }

    /** UI 配置 */
    default Map<String, Object> getUiConfig() { return Map.of(); }

    // ========== 生命周期钩子 ==========

    default int getOrder() { return 0; }

    /** 创建前：校验、填充默认值 */
    default void beforeCreate(ExtensionContext ctx) {}

    /** 创建后：初始化关联数据 */
    default void afterCreate(ExtensionContext ctx) {}

    /** 更新前 */
    default void beforeUpdate(ExtensionContext ctx) {}

    /** 更新后 */
    default void afterUpdate(ExtensionContext ctx) {}

    /** 删除前：可抛异常拦截 */
    default void beforeDelete(ExtensionContext ctx) {}

    /** 删除后：清理 */
    default void afterDelete(ExtensionContext ctx) {}

    /** 自定义校验（在系统校验之后执行） */
    default List<String> validate(ExtensionContext ctx) { return List.of(); }
}
```

### 3.2 字段定义

```java
public class FieldDefinition {
    String key;           // 字段key，存入 attributes JSON
    String label;         // 显示名
    String type;          // text|number|date|boolean|select|relation|user|tags
    String group;         // 分组名（表单分段显示）
    boolean required;     // 是否必填
    boolean system;       // true=系统字段(管理员不能删)，false=自定义字段
    Object defaultValue;  // 默认值
    Map<String, Object> config;  // 额外配置
    // config 示例:
    //   type=select: { "options": [{"value":1,"label":"普通班"},{"value":2,"label":"重点班"}] }
    //   type=relation: { "target": "grades", "labelField": "gradeName" }
    //   type=user: { "role": "TEACHER" }
    //   type=number: { "min": 2020, "max": 2030 }
}
```

### 3.3 扩展上下文

```java
public class ExtensionContext {
    String entityType;                    // ORG_UNIT / PLACE / USER
    String typeCode;                      // CLASS / CLASSROOM / TEACHER
    Long entityId;
    String entityName;
    Long parentId;                        // 父节点（组织树）
    Map<String, Object> attributes;       // 当前属性
    Map<String, Object> oldAttributes;    // 旧属性（更新时）
    Long operatorId;                      // 操作人
    
    /** 获取属性值的便捷方法 */
    public <T> T get(String key) { return (T) attributes.get(key); }
    public <T> T getOld(String key) { return oldAttributes != null ? (T) oldAttributes.get(key) : null; }
}
```

## 四、教育行业插件示例

### 4.1 班级插件

```java
@Component
public class ClassPlugin implements EntityTypePlugin {

    @Autowired private JdbcTemplate jdbc;

    public String getEntityType() { return "ORG_UNIT"; }
    public String getTypeCode() { return "CLASS"; }
    public String getTypeName() { return "班级"; }
    public String getCategory() { return "GROUP"; }
    public String getParentTypeCode() { return "GRADE"; }
    public List<String> getAllowedChildTypeCodes() { return List.of(); }

    public List<FieldDefinition> getSystemFields() {
        return List.of(
            field("gradeId", "所属年级", "relation", "基本信息", true,
                Map.of("target", "org_units", "targetTypeCode", "GRADE", "labelField", "unitName")),
            field("majorId", "专业", "relation", "基本信息", false,
                Map.of("target", "majors", "labelField", "majorName")),
            field("headTeacher", "班主任", "user", "人员配置", false,
                Map.of("role", "TEACHER")),
            field("enrollmentYear", "入学年份", "number", "基本信息", true,
                Map.of("min", 2020, "max", 2035)),
            field("duration", "学制(年)", "number", "基本信息", false,
                Map.of("min", 1, "max", 6, "default", 3)),
            field("classType", "班级类型", "select", "基本信息", false,
                Map.of("options", List.of(
                    Map.of("value", 1, "label", "普通班"),
                    Map.of("value", 2, "label", "重点班"),
                    Map.of("value", 3, "label", "实验班"))))
        );
    }

    public Map<String, Boolean> getFeatures() {
        return Map.of("hasStudents", true, "hasTimetable", true, "hasAttendance", true);
    }

    public void beforeDelete(ExtensionContext ctx) {
        Long studentCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM students WHERE class_id = ? AND status = 1 AND deleted = 0",
            Long.class, ctx.getEntityId());
        if (studentCount != null && studentCount > 0) {
            throw new BusinessException("班级下有 " + studentCount + " 名在读学生，无法删除");
        }
    }

    public List<String> validate(ExtensionContext ctx) {
        List<String> errors = new ArrayList<>();
        if (ctx.get("enrollmentYear") == null) errors.add("入学年份不能为空");
        return errors;
    }
}
```

### 4.2 教室插件

```java
@Component
public class ClassroomPlugin implements EntityTypePlugin {

    public String getEntityType() { return "PLACE"; }
    public String getTypeCode() { return "CLASSROOM"; }
    public String getTypeName() { return "教室"; }
    public String getCategory() { return "ROOM"; }

    public List<FieldDefinition> getSystemFields() {
        return List.of(
            field("roomFunction", "教室功能", "select", "基本信息", false,
                Map.of("options", List.of(
                    Map.of("value", "NORMAL", "label", "普通教室"),
                    Map.of("value", "MULTIMEDIA", "label", "多媒体教室"),
                    Map.of("value", "LAB", "label", "实验室"),
                    Map.of("value", "COMPUTER", "label", "机房")))),
            field("hasProjector", "有投影仪", "boolean", "设施配置", false, Map.of()),
            field("hasAC", "有空调", "boolean", "设施配置", false, Map.of()),
            field("examCapacity", "考试容量", "number", "考试配置", false,
                Map.of("min", 0, "max", 200))
        );
    }
}
```

### 4.3 教师插件

```java
@Component
public class TeacherPlugin implements EntityTypePlugin {

    public String getEntityType() { return "USER"; }
    public String getTypeCode() { return "TEACHER"; }
    public String getTypeName() { return "教师"; }
    public String getCategory() { return "STAFF"; }

    public List<FieldDefinition> getSystemFields() {
        return List.of(
            field("teacherNo", "工号", "text", "基本信息", true, Map.of()),
            field("subject", "学科", "select", "教学信息", false,
                Map.of("options", List.of(
                    Map.of("value", "MATH", "label", "数学"),
                    Map.of("value", "CHINESE", "label", "语文"),
                    Map.of("value", "ENGLISH", "label", "英语"),
                    Map.of("value", "PHYSICS", "label", "物理")))),
            field("title", "职称", "select", "教学信息", false,
                Map.of("options", List.of(
                    Map.of("value", "PRIMARY", "label", "初级"),
                    Map.of("value", "INTERMEDIATE", "label", "中级"),
                    Map.of("value", "SENIOR", "label", "高级"),
                    Map.of("value", "PROFESSOR", "label", "正高级")))),
            field("maxWeeklyHours", "周最大课时", "number", "排课配置", false,
                Map.of("min", 0, "max", 30, "default", 16))
        );
    }
}
```

## 五、Schema 合并机制

```
最终 schema = 插件系统字段(system=true) + 管理员自定义字段(system=false)

entity_type_configs 表存储合并后的结果:
{
  "fields": [
    // 插件注册的系统字段（管理员不能删，标记 system=true）
    { "key": "gradeId", "label": "所属年级", "type": "relation", "system": true, ... },
    { "key": "headTeacher", "label": "班主任", "type": "user", "system": true, ... },
    
    // 管理员追加的自定义字段（可自由增删，标记 system=false）
    { "key": "motto", "label": "班训", "type": "text", "system": false },
    { "key": "flagColor", "label": "班旗颜色", "type": "select", "system": false,
      "config": { "options": [{"value":"red","label":"红"},{"value":"blue","label":"蓝"}] } }
  ]
}
```

**启动时合并流程:**
```
1. Spring 扫描所有 EntityTypePlugin 实现
2. 对每个插件:
   a. 查 entity_type_configs 是否已有此 entityType+typeCode
   b. 如果没有: 插入新记录，systemFields → metadata_schema
   c. 如果已有: 合并 — 保留管理员的 customFields，更新 systemFields
   d. 标记 is_plugin_registered = true
3. 对 entity_type_configs 中 is_plugin_registered=false 的:
   → 这些是纯管理员创建的类型（无插件支撑），标记为 custom_only
```

## 六、entity_type_configs 表设计

```sql
CREATE TABLE entity_type_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    entity_type VARCHAR(20) NOT NULL COMMENT 'ORG_UNIT/PLACE/USER',
    type_code VARCHAR(50) NOT NULL,
    type_name VARCHAR(50) NOT NULL,
    category VARCHAR(30),
    parent_type_code VARCHAR(50),
    allowed_child_type_codes JSON,
    
    -- Schema: 插件字段 + 管理员字段合并
    metadata_schema JSON NOT NULL DEFAULT '{"fields":[]}',
    
    -- 功能与配置
    features JSON DEFAULT '{}',
    ui_config JSON DEFAULT '{}' COMMENT '{"icon":"school","color":"#2563eb"}',
    
    -- 插件信息
    is_plugin_registered TINYINT DEFAULT 0 COMMENT '是否由插件注册',
    plugin_class VARCHAR(200) COMMENT '插件实现类名',
    
    -- 管理
    is_system TINYINT DEFAULT 0,
    is_enabled TINYINT DEFAULT 1,
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    
    UNIQUE KEY uk_entity_type (entity_type, type_code)
) COMMENT '统一实体类型注册表';
```

## 七、ExtensionDispatcher（核心调度器）

```java
@Service
public class ExtensionDispatcher {

    private final Map<String, List<EntityTypePlugin>> pluginRegistry = new HashMap<>();

    @Autowired
    public ExtensionDispatcher(List<EntityTypePlugin> plugins) {
        // 按 entityType_typeCode 分组注册
        for (EntityTypePlugin plugin : plugins) {
            String key = plugin.getEntityType() + "_" + plugin.getTypeCode();
            pluginRegistry.computeIfAbsent(key, k -> new ArrayList<>()).add(plugin);
        }
        // 按 order 排序
        pluginRegistry.values().forEach(list -> list.sort(
            Comparator.comparingInt(EntityTypePlugin::getOrder)));
    }

    public void fireBeforeCreate(String entityType, String typeCode, ExtensionContext ctx) {
        getPlugins(entityType, typeCode).forEach(p -> p.beforeCreate(ctx));
    }

    public void fireAfterCreate(String entityType, String typeCode, ExtensionContext ctx) {
        getPlugins(entityType, typeCode).forEach(p -> p.afterCreate(ctx));
    }

    public List<String> fireValidate(String entityType, String typeCode, ExtensionContext ctx) {
        return getPlugins(entityType, typeCode).stream()
            .flatMap(p -> p.validate(ctx).stream())
            .collect(Collectors.toList());
    }

    // ... beforeUpdate, afterUpdate, beforeDelete, afterDelete

    private List<EntityTypePlugin> getPlugins(String entityType, String typeCode) {
        List<EntityTypePlugin> exact = pluginRegistry.getOrDefault(
            entityType + "_" + typeCode, List.of());
        List<EntityTypePlugin> wildcard = pluginRegistry.getOrDefault(
            entityType + "_*", List.of());
        List<EntityTypePlugin> all = new ArrayList<>(exact);
        all.addAll(wildcard);
        return all;
    }
}
```

## 八、PluginRegistrar（启动时注册）

```java
@Component
public class PluginRegistrar implements ApplicationRunner {

    @Autowired private List<EntityTypePlugin> plugins;
    @Autowired private JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        for (EntityTypePlugin plugin : plugins) {
            registerOrMerge(plugin);
        }
        log.info("已注册 {} 个实体类型插件", plugins.size());
    }

    private void registerOrMerge(EntityTypePlugin plugin) {
        String entityType = plugin.getEntityType();
        String typeCode = plugin.getTypeCode();

        Long exists = jdbc.queryForObject(
            "SELECT COUNT(1) FROM entity_type_configs WHERE entity_type=? AND type_code=? AND deleted=0",
            Long.class, entityType, typeCode);

        ObjectMapper om = new ObjectMapper();
        String systemFieldsJson = toJson(plugin.getSystemFields());
        String featuresJson = toJson(plugin.getFeatures());
        String uiConfigJson = toJson(plugin.getUiConfig());
        String childCodes = toJson(plugin.getAllowedChildTypeCodes());

        if (exists == null || exists == 0) {
            // 新注册
            jdbc.update(
                "INSERT INTO entity_type_configs (entity_type, type_code, type_name, category, " +
                "parent_type_code, allowed_child_type_codes, metadata_schema, features, ui_config, " +
                "is_plugin_registered, plugin_class, is_enabled, deleted) " +
                "VALUES (?,?,?,?,?,?,?,?,?,1,?,1,0)",
                entityType, typeCode, plugin.getTypeName(), plugin.getCategory(),
                plugin.getParentTypeCode(), childCodes,
                buildSchema(plugin.getSystemFields(), List.of()),
                featuresJson, uiConfigJson, plugin.getClass().getName());
        } else {
            // 已存在: 合并 — 保留管理员自定义字段，更新系统字段
            String currentSchema = jdbc.queryForObject(
                "SELECT metadata_schema FROM entity_type_configs WHERE entity_type=? AND type_code=? AND deleted=0",
                String.class, entityType, typeCode);
            List<FieldDefinition> customFields = extractCustomFields(currentSchema);
            String mergedSchema = buildSchema(plugin.getSystemFields(), customFields);

            jdbc.update(
                "UPDATE entity_type_configs SET type_name=?, category=?, parent_type_code=?, " +
                "allowed_child_type_codes=?, metadata_schema=?, features=?, ui_config=?, " +
                "is_plugin_registered=1, plugin_class=? " +
                "WHERE entity_type=? AND type_code=? AND deleted=0",
                plugin.getTypeName(), plugin.getCategory(), plugin.getParentTypeCode(),
                childCodes, mergedSchema, featuresJson, uiConfigJson,
                plugin.getClass().getName(), entityType, typeCode);
        }
    }
}
```

## 九、DynamicForm.vue（前端核心组件）

```vue
<template>
  <div v-for="group in fieldGroups" :key="group.name">
    <h4 class="tm-section-title">{{ group.name }}</h4>
    <div class="tm-fields" :class="group.fields.length > 1 ? 'tm-cols-2' : ''">
      <div v-for="field in group.fields" :key="field.key" class="tm-field">
        <label class="tm-label">
          {{ field.label }}
          <span v-if="field.required" class="req">*</span>
          <span v-if="field.system" class="sys-badge">系统</span>
        </label>

        <!-- 根据 field.type 渲染不同组件 -->
        <input v-if="field.type === 'text'" v-model="values[field.key]" class="tm-input" />
        <input v-else-if="field.type === 'number'" v-model.number="values[field.key]"
               type="number" :min="field.config?.min" :max="field.config?.max" class="tm-input" />
        <input v-else-if="field.type === 'date'" v-model="values[field.key]" type="date" class="tm-input" />
        <textarea v-else-if="field.type === 'textarea'" v-model="values[field.key]" class="tm-textarea" />
        <label v-else-if="field.type === 'boolean'" style="display:flex;align-items:center;gap:6px;">
          <input type="checkbox" v-model="values[field.key]" /> {{ values[field.key] ? '是' : '否' }}
        </label>
        <select v-else-if="field.type === 'select'" v-model="values[field.key]" class="tm-field-select">
          <option :value="undefined">请选择</option>
          <option v-for="opt in field.config?.options" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
        </select>
        <RelationField v-else-if="field.type === 'relation'" v-model="values[field.key]" :config="field.config" />
        <UserField v-else-if="field.type === 'user'" v-model="values[field.key]" :role="field.config?.role" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{ schema: { fields: any[] }; values: Record<string, any> }>()

const fieldGroups = computed(() => {
  const groups = new Map<string, any[]>()
  for (const f of props.schema.fields) {
    const g = f.group || '其他'
    if (!groups.has(g)) groups.set(g, [])
    groups.get(g)!.push(f)
  }
  return Array.from(groups.entries()).map(([name, fields]) => ({ name, fields }))
})
</script>
```

## 十、现有模块改动清单

### 必须改

| 文件/表 | 改动 | 原因 |
|---------|------|------|
| 新建 `entity_type_configs` 表 | 新增 | 统一类型注册表 |
| 新建 `EntityTypePlugin` 接口 | 新增 | SPI 扩展点 |
| 新建 `ExtensionDispatcher` | 新增 | 插件调度器 |
| 新建 `PluginRegistrar` | 新增 | 启动时注册插件 |
| 新建 `DynamicForm.vue` | 新增 | 动态表单引擎 |
| `OrgUnitApplicationService.createOrgUnit()` | 改 | 加 dispatcher.fireBeforeCreate/afterCreate |
| `OrgUnitApplicationService.autoCreateClassBinding()` | 删 | 由 ClassPlugin.afterCreate 替代 |
| `OrgUnitForm.vue` | 改 | 通用字段 + DynamicForm 渲染扩展字段 |
| `Place.java` L49 classId | 删 | 改为 attributes 存储 |
| `users` 表 | 加 attributes JSON 列 | 用户也需要扩展属性 |

### 新建（教育行业插件）

| 文件 | 作用 |
|------|------|
| `ClassPlugin.java` | 班级类型注册 + 生命周期 |
| `TeacherPlugin.java` | 教师类型注册 |
| `StudentPlugin.java` | 学生类型注册 |
| `ClassroomPlugin.java` | 教室类型注册 |
| `DormitoryPlugin.java` | 宿舍类型注册 |
| `GradePlugin.java` | 年级类型注册 |

### 逐步废弃

| 内容 | 替代方案 | 时机 |
|------|---------|------|
| `org_unit_types` 表 | 数据迁入 entity_type_configs | Phase 2 |
| `classes` 表 | 数据迁入 org_units.attributes | Phase 3 |
| `ClassForm.vue` | DynamicForm 自动渲染 | Phase 2 |
| `ClassDetailPanel.vue` | DynamicForm 详情模式 | Phase 2 |
| `TeacherProfileController` | TeacherPlugin + 通用 attributes API | Phase 3 |

### 不动

| 模块 | 原因 |
|------|------|
| 权限/角色/DataScope | 100% 通用 |
| 排课/考试/成绩 | 行业层，在插件之上 |
| 校历管理 | 通用基础设施 |

## 十一、实施路线

### Phase 1: 核心框架（1周）
1. entity_type_configs 表 + 迁移脚本
2. EntityTypePlugin 接口 + FieldDefinition
3. ExtensionDispatcher + PluginRegistrar
4. DynamicForm.vue + RelationField + UserField
5. OrgUnitForm 接入 DynamicForm
6. ClassPlugin（第一个插件）

### Phase 2: 全面接入（1周）
7. PlaceForm 接入 DynamicForm
8. UserForm 接入 DynamicForm
9. ClassroomPlugin + DormitoryPlugin
10. TeacherPlugin + StudentPlugin
11. 管理员自定义字段 UI（TypeConfigEditor）
12. 迁移 org_unit_types → entity_type_configs

### Phase 3: 数据迁移（1周）
13. classes 表数据 → org_units.attributes
14. 废弃 ClassForm/ClassDetailPanel
15. Place.classId → attributes
16. users 表加 attributes + 迁移教师数据
17. 全面测试 + 文档
