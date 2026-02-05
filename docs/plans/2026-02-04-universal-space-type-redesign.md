# 通用场所类型系统重构设计

## 背景

当前场所管理系统存在以下问题：
1. 表单过长，需要滚动才能填完
2. 空间列表占用空间太大
3. 创建空间时无法选择空间类型
4. 类型配置不够灵活，无法满足不同行业需求

## 设计目标

- 支持树形类型层级配置
- 每种类型绑定独立的属性模板
- 提供行业预置模板（学校、公司、医院、工厂）
- 抽屉面板表单布局，提升用户体验
- 系统通用性：适用于学校、公司、医院、工厂等

---

## 数据库设计

### 1. 场所类型表 (space_types) - 已存在，需扩展

```sql
ALTER TABLE space_types ADD COLUMN parent_type_id BIGINT COMMENT '父类型ID，支持层级';
ALTER TABLE space_types ADD COLUMN allowed_child_types JSON COMMENT '允许的子类型ID列表';
ALTER TABLE space_types ADD COLUMN is_leaf BOOLEAN DEFAULT FALSE COMMENT '是否叶子节点（不能再有子空间）';
ALTER TABLE space_types ADD COLUMN preset_code VARCHAR(50) COMMENT '预置类型代码（用于行业模板）';
```

### 2. 类型属性模板表 (space_type_attributes)

```sql
CREATE TABLE space_type_attributes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type_id BIGINT NOT NULL COMMENT '关联的空间类型ID',
    attribute_key VARCHAR(50) NOT NULL COMMENT '属性键名',
    attribute_label VARCHAR(100) NOT NULL COMMENT '属性显示名称',
    attribute_type ENUM('TEXT', 'NUMBER', 'BOOLEAN', 'SELECT', 'DATE') NOT NULL COMMENT '属性类型',
    options JSON COMMENT '选项列表（SELECT类型使用）',
    is_required BOOLEAN DEFAULT FALSE COMMENT '是否必填',
    default_value VARCHAR(255) COMMENT '默认值',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    FOREIGN KEY (type_id) REFERENCES space_types(id)
);
```

### 3. 行业预置模板表 (space_preset_templates)

```sql
CREATE TABLE space_preset_templates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    preset_code VARCHAR(50) NOT NULL UNIQUE COMMENT '预置代码：SCHOOL, COMPANY, HOSPITAL, FACTORY',
    preset_name VARCHAR(100) NOT NULL COMMENT '预置名称',
    description VARCHAR(500) COMMENT '描述',
    template_data JSON NOT NULL COMMENT '完整的类型树+属性配置JSON',
    is_enabled BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

---

## 预置模板数据

### 学校预置 (SCHOOL)

```json
{
  "types": [
    {
      "code": "CAMPUS",
      "name": "校区",
      "icon": "School",
      "children": ["BUILDING", "SPORTS_FIELD", "PARKING"]
    },
    {
      "code": "BUILDING",
      "name": "楼栋",
      "icon": "OfficeBuilding",
      "children": ["FLOOR"],
      "attributes": [
        {"key": "floors", "label": "楼层数", "type": "NUMBER", "required": true},
        {"key": "buildYear", "label": "建成年份", "type": "NUMBER"},
        {"key": "area", "label": "建筑面积(㎡)", "type": "NUMBER"}
      ]
    },
    {
      "code": "FLOOR",
      "name": "楼层",
      "icon": "Files",
      "children": ["CLASSROOM", "DORMITORY", "OFFICE", "LAB", "MEETING"]
    },
    {
      "code": "CLASSROOM",
      "name": "教室",
      "icon": "Reading",
      "isLeaf": true,
      "attributes": [
        {"key": "capacity", "label": "座位数", "type": "NUMBER", "required": true},
        {"key": "hasMultimedia", "label": "多媒体设备", "type": "BOOLEAN"},
        {"key": "hasAC", "label": "空调", "type": "BOOLEAN"}
      ]
    },
    {
      "code": "DORMITORY",
      "name": "宿舍",
      "icon": "House",
      "isLeaf": true,
      "attributes": [
        {"key": "beds", "label": "床位数", "type": "NUMBER", "required": true},
        {"key": "hasBathroom", "label": "独立卫浴", "type": "BOOLEAN"},
        {"key": "hasAC", "label": "空调", "type": "BOOLEAN"}
      ]
    }
  ]
}
```

### 公司预置 (COMPANY)

```json
{
  "types": [
    {
      "code": "PARK",
      "name": "园区",
      "icon": "Location",
      "children": ["OFFICE_BUILDING", "WORKSHOP", "WAREHOUSE"]
    },
    {
      "code": "OFFICE_BUILDING",
      "name": "办公楼",
      "children": ["FLOOR"]
    },
    {
      "code": "FLOOR",
      "name": "楼层",
      "children": ["OFFICE", "MEETING", "RECEPTION"]
    },
    {
      "code": "OFFICE",
      "name": "办公室",
      "isLeaf": true,
      "attributes": [
        {"key": "workstations", "label": "工位数", "type": "NUMBER"},
        {"key": "phoneExt", "label": "电话分机", "type": "TEXT"}
      ]
    }
  ]
}
```

---

## 后端实现

### Domain层

1. **SpaceTypeAttribute** - 类型属性值对象
2. **SpacePresetTemplate** - 预置模板实体

### Application层

1. **SpaceTypeApplicationService** - 扩展
   - `getTypeTree()` - 获取类型层级树
   - `getAllowedChildTypes(parentTypeId)` - 获取允许的子类型
   - `getTypeAttributes(typeId)` - 获取类型属性模板

2. **SpacePresetApplicationService** - 新增
   - `listPresets()` - 列出所有预置模板
   - `applyPreset(presetCode)` - 应用预置模板
   - `exportCurrentConfig()` - 导出当前配置

### Interface层

1. **SpaceTypeController** - 扩展
   - `GET /api/space-types/tree` - 获取类型树
   - `GET /api/space-types/{id}/allowed-children` - 获取允许子类型
   - `GET /api/space-types/{id}/attributes` - 获取属性模板

2. **SpacePresetController** - 新增
   - `GET /api/space-presets` - 列出预置
   - `POST /api/space-presets/{code}/apply` - 应用预置

---

## 前端实现

### UI改进

1. **抽屉面板表单** - 替代当前的长表单
   - 右侧滑出抽屉
   - 分步表单：基础信息 → 类型属性 → 确认
   - 宽度 500px

2. **类型层级树配置** - 新增配置页面
   - 左侧：类型树预览
   - 右侧：类型属性配置
   - 支持拖拽排序

3. **预置模板选择** - 首次使用引导
   - 卡片式展示各行业模板
   - 一键应用

### 组件结构

```
views/space/
├── UniversalSpaceManagement.vue     # 主页面（修改）
├── components/
│   ├── SpaceDrawerForm.vue          # 抽屉表单（新增）
│   ├── SpaceTypeTree.vue            # 类型树选择器（新增）
│   └── DynamicAttributeForm.vue     # 动态属性表单（新增）
└── config/
    ├── SpaceTypeConfigV2.vue        # 类型配置页（新增）
    └── PresetSelector.vue           # 预置选择器（新增）
```

---

## 实现步骤

### Phase 1: 数据库与后端 (4个任务)

1. 执行数据库迁移脚本
2. 创建SpaceTypeAttribute领域模型和仓储
3. 扩展SpaceTypeApplicationService
4. 创建预置模板服务和控制器

### Phase 2: 前端改造 (5个任务)

5. 创建SpaceDrawerForm抽屉表单组件
6. 创建DynamicAttributeForm动态属性表单
7. 创建SpaceTypeTree类型选择组件
8. 修改UniversalSpaceManagement使用新组件
9. 创建SpaceTypeConfigV2配置页面

### Phase 3: 预置模板 (2个任务)

10. 创建PresetSelector组件
11. 插入学校/公司/医院/工厂预置数据

---

## 验证方式

1. **类型配置**: 能够创建树形类型层级
2. **属性模板**: 每种类型能定义不同属性
3. **创建空间**: 选择类型后动态显示对应属性
4. **预置导入**: 一键导入学校/公司预置配置
5. **表单体验**: 抽屉面板流畅，无需滚动
