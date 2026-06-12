import { reactive } from 'vue'

/**
 * 关系绑定"业务场景"定义。
 *
 * <p>通用核心只内置 {@link CORE_RELATION_SCENES}（不含任何行业概念）。行业场景（如教育的
 * "指定班主任"）由对应行业插件前端入口在加载时通过 {@link registerRelationScenes} 注册，
 * 仅当该插件启用时才会出现在关系绑定页 —— 与路由按 `EDU.enabled` 条件注册同一套门控理念。
 */
export interface RelationScene {
  code: string
  title: string
  desc: string
  /** access_relations.relation 关系码 */
  relation: string
  subjectType: string
  resourceType: string
  subjectLabel: string
  resourceLabel: string
  /** 创建关系时附加的 metadata（如教育班主任写 {role:'CLASS_TEACHER'} 以与通用 admin 区分） */
  metadata?: Record<string, unknown>
}

/**
 * 通用核心场景 —— 任何部署都有, 全部用中性词, 不出现班级/班主任/学生等行业术语。
 */
export const CORE_RELATION_SCENES: RelationScene[] = [
  {
    code: 'ASSIGN_ORG_ADMIN', title: '组织管理员', desc: '指定某组织的管理员',
    relation: 'admin', subjectType: 'user', resourceType: 'org_unit',
    subjectLabel: '选择管理员', resourceLabel: '选择组织',
  },
  {
    code: 'ADD_MEMBER', title: '加入组织', desc: '把用户加入某组织作为成员',
    relation: 'member', subjectType: 'user', resourceType: 'org_unit',
    subjectLabel: '选择用户', resourceLabel: '选择组织',
  },
  {
    // admin = 管理权 (权限语义); 业务问责的"场所责任人"走 responsible_for (场所表单维护)
    code: 'ASSIGN_PLACE_ADMIN', title: '场所管理员', desc: '指定某场所的管理员（管理权限）',
    relation: 'admin', subjectType: 'user', resourceType: 'place',
    subjectLabel: '选择管理员', resourceLabel: '选择场所',
  },
  {
    // belongs_to = 归属真相源 (覆盖点); 投影列 effective_org_unit_id 由后端投影器同步
    code: 'PLACE_BELONGS_ORG', title: '场所归属', desc: '绑定场所到某组织（无绑定=继承父场所）',
    relation: 'belongs_to', subjectType: 'place', resourceType: 'org_unit',
    subjectLabel: '选择场所', resourceLabel: '归属组织',
  },
  {
    code: 'ADD_GUARDIAN', title: '添加监护人', desc: '为某人绑定监护关系',
    relation: 'guardian_of', subjectType: 'user', resourceType: 'user',
    subjectLabel: '选择监护人', resourceLabel: '选择被监护人',
  },
]

/** 行业插件贡献的场景 —— 按插件码登记, 仅在该插件启用时显示。 */
const pluginScenes = reactive<Record<string, RelationScene[]>>({})

/** 行业插件前端入口加载时调用, 登记本插件的关系场景。 */
export function registerRelationScenes(pluginCode: string, scenes: RelationScene[]): void {
  pluginScenes[pluginCode] = scenes
}

/** 取所有"已启用"插件 (enabledCodes) 登记的场景。 */
export function enabledPluginScenes(enabledCodes: readonly string[]): RelationScene[] {
  return enabledCodes.flatMap(code => pluginScenes[code] ?? [])
}
