/**
 * EDU 插件专属的类型码常量.
 *
 * 这些字符串对应后端 entity_type_configs 表中 source='EducationPlugin' 的记录,
 * 由 StudentPlugin / ClassPlugin / GradePlugin 等声明. Core 代码不应引用, 只有
 * EDU 插件的前端视图 (如 views/teaching/*) 才用得到.
 */
export const EduOrgTypes = {
  GRADE: 'GRADE',
  CLASS: 'CLASS',
  MAJOR: 'MAJOR',
  DEPARTMENT: 'DEPARTMENT',
} as const;

export type EduOrgType = typeof EduOrgTypes[keyof typeof EduOrgTypes];
