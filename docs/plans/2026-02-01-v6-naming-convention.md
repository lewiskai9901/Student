# V6 命名规范统一标准

> **版本**: 1.0
> **日期**: 2026-02-01
> **目的**: 确保全系统命名一致性

---

## 一、核心术语映射

### 1.1 中英文对照表

| 中文术语 | 英文术语 | 缩写 | 说明 |
|---------|---------|-----|------|
| 组织 | Organization | org | 通用组织单元 |
| 组织单元 | Organization Unit | org_unit | 具体组织实体 |
| 场所 | Space | space | 通用场所 |
| 用户 | User | user | 系统用户 |
| 检查 | Inspection | inspection | 检查/巡检 |
| 模板 | Template | template | 检查模板 |
| 项目 | Project | project | 检查项目 |
| 任务 | Task | task | 检查任务 |
| 目标 | Target | target | 检查目标 |
| 明细 | Detail | detail | 检查明细 |
| 汇总 | Summary | summary | 数据汇总 |
| 类型 | Type | type | 类型配置 |
| 关系 | Relation | rel | 关联关系 |

### 1.2 废弃术语

| 废弃术语 | 替代术语 | 说明 |
|---------|---------|------|
| 部门 Department | 组织 Organization | 更通用 |
| dept_* | unit_* | 字段前缀 |
| system:department:* | system:org:* | 权限代码 |

---

## 二、数据库命名规范

### 2.1 表命名

```
格式: <模块>_<实体>[_<后缀>]

规则:
- 使用小写字母
- 单词之间用下划线分隔
- 使用复数形式表示集合

示例:
✅ org_units          - 组织单元表
✅ org_types          - 组织类型表
✅ space_types        - 场所类型表
✅ user_types         - 用户类型表
✅ user_org_relations - 用户组织关系表
✅ space_org_relations - 场所组织关系表
✅ inspection_projects - 检查项目表
✅ inspection_tasks   - 检查任务表

❌ departments        - 废弃
❌ user_departments   - 废弃
```

### 2.2 字段命名

```
格式: <前缀>_<属性名>

规则:
- 使用小写字母和下划线
- 统一前缀（不再使用 dept_ 前缀）
- 布尔字段使用 is_ 或 has_ 前缀
- 时间字段使用 _at 后缀
- 日期字段使用 _date 后缀

示例:
✅ unit_code         - 组织代码
✅ unit_name         - 组织名称
✅ type_code         - 类型代码
✅ is_enabled        - 是否启用
✅ has_children      - 是否有子级
✅ created_at        - 创建时间
✅ start_date        - 开始日期

❌ dept_code         - 废弃
❌ dept_name         - 废弃
```

### 2.3 索引命名

```
格式: idx_<表名缩写>_<字段名>
唯一索引: uk_<表名缩写>_<字段名>
主键: pk_<表名>

示例:
✅ idx_ou_type_code   - org_units.type_code 索引
✅ uk_ou_unit_code    - org_units.unit_code 唯一索引
✅ idx_uor_user_org   - user_org_relations 联合索引
```

---

## 三、后端 Java 命名规范

### 3.1 包结构

```
com.school.management/
├── domain/
│   ├── organization/          # 组织领域
│   │   ├── model/
│   │   │   ├── OrgUnit.java           # 聚合根
│   │   │   ├── OrgType.java           # 组织类型实体
│   │   │   └── UserOrgRelation.java   # 用户组织关系
│   │   └── repository/
│   │       ├── OrgUnitRepository.java
│   │       └── OrgTypeRepository.java
│   ├── space/                 # 场所领域
│   └── inspection/            # 检查领域
├── application/
│   └── organization/
│       ├── OrgUnitApplicationService.java
│       └── OrgTypeApplicationService.java
├── infrastructure/
│   └── persistence/
│       └── organization/
│           ├── OrgUnitPO.java
│           ├── OrgTypePO.java
│           └── OrgUnitMapper.java
└── interfaces/
    └── rest/
        └── organization/
            ├── OrgUnitController.java
            └── OrgTypeController.java
```

### 3.2 类命名

| 类型 | 命名规则 | 示例 |
|-----|---------|------|
| 聚合根 | PascalCase | `OrgUnit` |
| 实体 | PascalCase | `OrgType` |
| 值对象 | PascalCase | `OrgUnitId` |
| 持久化对象 | PascalCase + PO | `OrgUnitPO` |
| Repository接口 | Name + Repository | `OrgUnitRepository` |
| Repository实现 | Name + RepositoryImpl | `OrgUnitRepositoryImpl` |
| 应用服务 | Name + ApplicationService | `OrgUnitApplicationService` |
| 领域服务 | Name + Service | `OrgRankingService` |
| 控制器 | Name + Controller | `OrgUnitController` |
| Mapper | Name + Mapper | `OrgUnitMapper` |

### 3.3 方法命名

```java
// Repository 方法
OrgUnit findById(Long id);
OrgUnit findByUnitCode(String unitCode);
List<OrgUnit> findByParentId(Long parentId);
List<OrgUnit> findByTypeCode(String typeCode);
void save(OrgUnit orgUnit);
void delete(OrgUnit orgUnit);

// Service 方法
OrgUnit createOrgUnit(CreateOrgUnitCommand command);
OrgUnit updateOrgUnit(UpdateOrgUnitCommand command);
void deleteOrgUnit(Long id);
List<OrgUnit> queryOrgUnits(OrgUnitQuery query);
OrgUnitTree buildOrgTree(Long rootId);

// Controller 方法
@GetMapping
Page<OrgUnitResponse> list(OrgUnitQueryRequest request);

@PostMapping
OrgUnitResponse create(@RequestBody CreateOrgUnitRequest request);

@PutMapping("/{id}")
OrgUnitResponse update(@PathVariable Long id, @RequestBody UpdateOrgUnitRequest request);

@DeleteMapping("/{id}")
void delete(@PathVariable Long id);
```

### 3.4 变量命名

```java
// 实体变量
OrgUnit orgUnit;              // 单个实体
List<OrgUnit> orgUnits;       // 实体列表
Long orgUnitId;               // ID
String unitCode;              // 代码

// 关系变量
UserOrgRelation relation;
List<UserOrgRelation> userOrgRelations;

// 查询条件
String typeCode;
Long parentId;
Boolean isEnabled;

// 集合
Set<Long> orgUnitIds;
Map<Long, OrgUnit> orgUnitMap;
```

### 3.5 常量命名

```java
public class OrgConstants {
    // 关系类型
    public static final String RELATION_TYPE_PRIMARY = "PRIMARY";
    public static final String RELATION_TYPE_SECONDARY = "SECONDARY";
    public static final String RELATION_TYPE_TEMPORARY = "TEMPORARY";

    // 默认值
    public static final int DEFAULT_TREE_LEVEL = 1;
    public static final String DEFAULT_SCENE_CODE = "SCHOOL";
}
```

---

## 四、前端 TypeScript/Vue 命名规范

### 4.1 文件结构

```
src/
├── api/
│   ├── organization.ts        # 组织管理 API
│   ├── orgType.ts             # 组织类型 API
│   ├── space.ts               # 场所管理 API
│   └── inspection.ts          # 检查管理 API
├── types/
│   ├── organization.ts        # 组织相关类型
│   ├── space.ts               # 场所相关类型
│   └── inspection.ts          # 检查相关类型
├── stores/
│   ├── organization.ts        # 组织 store
│   └── inspection.ts          # 检查 store
├── views/
│   ├── organization/
│   │   ├── OrgUnitList.vue        # 组织列表
│   │   ├── OrgTypeConfig.vue      # 组织类型配置
│   │   └── UserOrgManager.vue     # 用户组织管理
│   └── inspection/
│       ├── ProjectList.vue        # 项目列表
│       └── ProjectWizard.vue      # 项目创建向导
└── components/
    ├── organization/
    │   ├── OrgTree.vue            # 组织树
    │   ├── OrgSelector.vue        # 组织选择器
    │   └── OrgTypeIcon.vue        # 组织类型图标
    └── common/
        └── ...
```

### 4.2 类型定义

```typescript
// types/organization.ts

// 组织单元
export interface OrgUnit {
  id: number
  unitCode: string
  unitName: string
  typeCode: string
  parentId: number | null
  treePath: string
  treeLevel: number
  attributes: Record<string, unknown>
  memberCount: number
  childCount: number
  isEnabled: boolean
  createdAt: string
  updatedAt: string
}

// 组织类型
export interface OrgType {
  id: number
  typeCode: string
  typeName: string
  level: number
  parentTypeCodes: string[]
  canHaveChildren: boolean
  canHaveSpaces: boolean
  canHaveUsers: boolean
  icon: string
  color: string
  weightAttribute: string
  sceneCode: string
  isSystem: boolean
  isEnabled: boolean
}

// 用户组织关系
export interface UserOrgRelation {
  id: number
  userId: number
  orgId: number
  relationType: 'PRIMARY' | 'SECONDARY' | 'TEMPORARY'
  roleInOrg: string
  startDate: string | null
  endDate: string | null
  weight: number
  isEnabled: boolean
}

// 查询参数
export interface OrgUnitQuery {
  keyword?: string
  typeCode?: string
  parentId?: number
  isEnabled?: boolean
  page: number
  size: number
}

// 创建请求
export interface CreateOrgUnitRequest {
  unitCode: string
  unitName: string
  typeCode: string
  parentId?: number
  attributes?: Record<string, unknown>
}
```

### 4.3 API 模块

```typescript
// api/organization.ts
import request from '@/utils/request'
import type { OrgUnit, OrgUnitQuery, CreateOrgUnitRequest } from '@/types/organization'

const BASE_URL = '/org-units'

export const orgUnitApi = {
  // 查询列表
  list(params: OrgUnitQuery) {
    return request.get<Page<OrgUnit>>(BASE_URL, { params })
  },

  // 获取详情
  getById(id: number) {
    return request.get<OrgUnit>(`${BASE_URL}/${id}`)
  },

  // 创建
  create(data: CreateOrgUnitRequest) {
    return request.post<OrgUnit>(BASE_URL, data)
  },

  // 更新
  update(id: number, data: Partial<CreateOrgUnitRequest>) {
    return request.put<OrgUnit>(`${BASE_URL}/${id}`, data)
  },

  // 删除
  delete(id: number) {
    return request.delete(`${BASE_URL}/${id}`)
  },

  // 获取组织树
  getTree(rootId?: number) {
    return request.get<OrgUnit[]>(`${BASE_URL}/tree`, { params: { rootId } })
  }
}
```

### 4.4 组件命名

```vue
<!-- 文件名: OrgUnitList.vue -->
<script setup lang="ts">
// 使用 setup 语法

// Props
const props = defineProps<{
  parentId?: number
  typeCode?: string
}>()

// Emits
const emit = defineEmits<{
  (e: 'select', orgUnit: OrgUnit): void
  (e: 'edit', orgUnit: OrgUnit): void
}>()

// 响应式数据
const orgUnits = ref<OrgUnit[]>([])
const loading = ref(false)
const selectedOrgUnit = ref<OrgUnit | null>(null)

// 方法
const loadOrgUnits = async () => {
  loading.value = true
  try {
    const result = await orgUnitApi.list({ parentId: props.parentId })
    orgUnits.value = result.content
  } finally {
    loading.value = false
  }
}

const handleSelect = (orgUnit: OrgUnit) => {
  selectedOrgUnit.value = orgUnit
  emit('select', orgUnit)
}
</script>
```

### 4.5 Store 命名

```typescript
// stores/organization.ts
import { defineStore } from 'pinia'
import type { OrgUnit, OrgType } from '@/types/organization'

export const useOrganizationStore = defineStore('organization', () => {
  // State
  const orgUnits = ref<OrgUnit[]>([])
  const orgTypes = ref<OrgType[]>([])
  const currentOrgUnit = ref<OrgUnit | null>(null)
  const loading = ref(false)

  // Getters
  const orgUnitMap = computed(() => {
    return new Map(orgUnits.value.map(u => [u.id, u]))
  })

  const orgTypeMap = computed(() => {
    return new Map(orgTypes.value.map(t => [t.typeCode, t]))
  })

  // Actions
  const loadOrgUnits = async () => {
    loading.value = true
    try {
      orgUnits.value = await orgUnitApi.getTree()
    } finally {
      loading.value = false
    }
  }

  const loadOrgTypes = async () => {
    orgTypes.value = await orgTypeApi.list()
  }

  return {
    orgUnits,
    orgTypes,
    currentOrgUnit,
    loading,
    orgUnitMap,
    orgTypeMap,
    loadOrgUnits,
    loadOrgTypes
  }
})
```

---

## 五、API 路径规范

### 5.1 RESTful 规范

```
格式: /api/[v版本]/<资源>[/<id>][/<子资源>]

规则:
- 使用小写字母
- 使用连字符分隔单词
- 资源使用复数形式
- 使用 HTTP 动词表示操作
```

### 5.2 API 路径对照

| 资源 | 路径 | 说明 |
|-----|------|------|
| 组织单元 | /api/org-units | 组织 CRUD |
| 组织类型 | /api/v6/org-types | 类型配置 |
| 场所类型 | /api/v6/space-types | 类型配置 |
| 用户类型 | /api/v6/user-types | 类型配置 |
| 用户组织关系 | /api/v6/users/:userId/orgs | 用户的组织 |
| 场所组织关系 | /api/v6/spaces/:spaceId/orgs | 场所的组织 |
| 检查项目 | /api/v6/inspection/projects | 项目管理 |
| 检查任务 | /api/v6/inspection/tasks | 任务管理 |
| 我的任务 | /api/v6/inspection/my/tasks | 当前用户任务 |

### 5.3 废弃路径

| 废弃路径 | 替代路径 |
|---------|---------|
| /api/departments | /api/org-units |
| /api/inspection/department-ranking | /api/inspection/org-ranking |

---

## 六、权限代码规范

### 6.1 权限代码格式

```
格式: <模块>:<资源>:<操作>

模块:
- system: 系统管理
- inspection: 检查管理
- asset: 资产管理

资源:
- org: 组织
- space: 场所
- user: 用户
- project: 项目
- task: 任务

操作:
- view: 查看
- create: 创建
- update: 更新
- delete: 删除
- manage: 管理（包含所有操作）
```

### 6.2 权限代码对照

| 旧代码 | 新代码 | 说明 |
|-------|-------|------|
| system:department:view | system:org:view | 查看组织 |
| system:department:create | system:org:create | 创建组织 |
| system:department:update | system:org:update | 更新组织 |
| system:department:delete | system:org:delete | 删除组织 |

### 6.3 新增权限代码

```
# 类型配置
system:org-type:view
system:org-type:manage
system:space-type:view
system:space-type:manage
system:user-type:view
system:user-type:manage

# 关系管理
system:user-org-relation:view
system:user-org-relation:manage
system:space-org-relation:view
system:space-org-relation:manage

# V6 检查
inspection:project:view
inspection:project:create
inspection:project:manage
inspection:task:view
inspection:task:execute
inspection:summary:view
inspection:summary:export
```

---

## 七、Git 提交规范

### 7.1 提交消息格式

```
<type>(<scope>): <subject>

类型(type):
- feat: 新功能
- fix: Bug修复
- refactor: 重构
- rename: 重命名
- docs: 文档
- test: 测试
- chore: 构建/工具

范围(scope):
- org: 组织相关
- space: 场所相关
- user: 用户相关
- inspection: 检查相关
- naming: 命名相关
```

### 7.2 提交示例

```bash
# 命名统一相关
rename(org): rename departments to org_units in database
rename(org): rename DepartmentRankingController to OrgRankingController
refactor(org): update permission codes from department to org

# 功能开发
feat(org): add org_types configuration table
feat(org): implement user-org multi-relation support
feat(inspection): add inspection project wizard step 1-3

# Bug修复
fix(org): fix org tree loading with new type_code field
```

---

## 八、检查清单

### 8.1 命名统一检查

- [ ] 数据库表名使用 org_units（不是 departments）
- [ ] 字段名使用 unit_*（不是 dept_*）
- [ ] Java 类名使用 OrgUnit*（不是 Department*）
- [ ] 前端文件名使用 Org*（不是 Department*）
- [ ] API 路径使用 /org-*（不是 /department*）
- [ ] 权限代码使用 system:org:*（不是 system:department:*）
- [ ] 菜单名称显示"组织"（不是"部门"）

### 8.2 新开发检查

- [ ] 新表名符合规范
- [ ] 新字段名符合规范
- [ ] 新类名符合规范
- [ ] 新 API 路径符合规范
- [ ] 新权限代码符合规范
- [ ] 提交消息符合规范

---

**文档版本**: 1.0
**最后更新**: 2026-02-01
