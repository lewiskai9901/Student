/**
 * Phase 2 — 实体标签 (Entity Label) composable.
 *
 * 把通用 view 中"学生 / 班级 / 学校 / 宿舍"等行业字面量替换为运行时占位.
 * 切换到医疗 (患者/科室/病房) / 工厂 (员工/车间/产线) 不需要改源码.
 *
 * 数据源:
 *  - 优先: hasFeature() 推断当前激活的行业插件 (EDU/HEALTH 等)
 *  - 次选: entity_type_configs 后端配置 (typeName 中文)
 *  - 兜底: 内置默认 (主体=人员, 父级=组织)
 *
 * 用法:
 * <pre>
 *   const { subject, group, parent, place } = useEntityLabel()
 *   // EDU 启用时: subject="学生", group="班级", parent="年级", place="宿舍"
 *   // HEALTH 启用时: subject="患者", group="科室", parent="医院", place="病房"
 *
 *   // 模板中:
 *   <h2>{{ subject }}花名册</h2>
 *   <p>共 {{ total }} 名{{ subject }}</p>
 * </pre>
 */
import { computed } from 'vue'
import { hasFeature } from './useFeature'

interface LabelSet {
  subject: string         // "学生" / "患者" / "员工"
  subjectPlural?: string  // "学生们" / "患者们"
  group: string           // "班级" / "科室" / "班组"
  parent: string          // "年级" / "医院" / "厂区"
  place: string           // "宿舍" / "病房" / "车间"
  campus: string          // "学校" / "医院" / "工厂"
  organizer: string       // "班主任" / "主治医生" / "组长"
  evaluator: string       // "教师" / "护士" / "工长"
}

const PRESETS: Record<string, LabelSet> = {
  EDU: {
    subject:    '学生',
    group:      '班级',
    parent:     '年级',
    place:      '宿舍',
    campus:     '学校',
    organizer:  '班主任',
    evaluator:  '教师',
  },
  HEALTH: {
    subject:    '患者',
    group:      '科室',
    parent:     '医院',
    place:      '病房',
    campus:     '医院',
    organizer:  '主治医生',
    evaluator:  '护士',
  },
  // 通用兜底
  CORE: {
    subject:    '主体',
    group:      '组织',
    parent:     '父级组织',
    place:      '场所',
    campus:     '机构',
    organizer:  '负责人',
    evaluator:  '检查员',
  },
}

/**
 * 推断当前活跃的行业 — 多个启用时, EDU 优先 (学校场景占多数).
 */
function activeIndustry(): string {
  if (hasFeature('EDU')) return 'EDU'
  if (hasFeature('HEALTH')) return 'HEALTH'
  return 'CORE'
}

/**
 * 主入口 — 响应式获取当前行业的标签集.
 */
export function useEntityLabel() {
  const labels = computed<LabelSet>(() => {
    const industry = activeIndustry()
    return PRESETS[industry] || PRESETS.CORE
  })
  return {
    labels,
    subject:    computed(() => labels.value.subject),
    group:      computed(() => labels.value.group),
    parent:     computed(() => labels.value.parent),
    place:      computed(() => labels.value.place),
    campus:     computed(() => labels.value.campus),
    organizer:  computed(() => labels.value.organizer),
    evaluator:  computed(() => labels.value.evaluator),
  }
}

/**
 * 静态版本 — 在 .ts 工具函数 / 非 setup 上下文用.
 */
export function getEntityLabel(): LabelSet {
  const industry = activeIndustry()
  return PRESETS[industry] || PRESETS.CORE
}

/**
 * 单字段静态读取 — 节省调用代码.
 */
export function entityLabel(key: keyof LabelSet): string {
  const labels = getEntityLabel()
  return (labels[key] ?? PRESETS.CORE[key] ?? '') as string
}
