/**
 * BPMN.js 中文翻译
 */
export const translations: Record<string, string> = {
  // Search
  'Search in diagram': '在流程图中搜索',

  // Palette - 左侧工具栏元素 (小写形式 - bpmn-js 实际使用)
  'Activate hand tool': '激活拖拽工具',
  'Activate lasso tool': '激活套索工具',
  'Activate create/remove space tool': '激活创建/删除空间工具',
  'Activate global connect tool': '激活全局连接工具',
  'Activate the hand tool': '激活拖拽工具',
  'Activate the lasso tool': '激活套索工具',
  'Activate the create/remove space tool': '激活创建/删除空间工具',
  'Activate the global connect tool': '激活全局连接工具',
  'Create start event': '创建开始事件',
  'Create end event': '创建结束事件',
  'Create task': '创建任务',
  'Create user task': '创建用户任务',
  'Create gateway': '创建网关',
  'Create intermediate/boundary event': '创建中间/边界事件',
  'Create pool/participant': '创建池/参与者',
  'Create group': '创建分组',
  'Create data object reference': '创建数据对象引用',
  'Create data store reference': '创建数据存储引用',
  'Create expanded sub-process': '创建展开的子流程',
  'Create call activity': '创建调用活动',

  // Palette - 大写形式 (兼容)
  'Create StartEvent': '创建开始事件',
  'Create EndEvent': '创建结束事件',
  'Create Task': '创建任务',
  'Create UserTask': '创建用户任务',
  'Create Gateway': '创建网关',
  'Create Intermediate/Boundary Event': '创建中间/边界事件',
  'Create Pool/Participant': '创建池/参与者',
  'Create Group': '创建分组',
  'Create DataObjectReference': '创建数据对象引用',
  'Create DataStoreReference': '创建数据存储引用',
  'Create expanded SubProcess': '创建展开的子流程',
  'Create CallActivity': '创建调用活动',

  // 带占位符的翻译键（模板）
  'Create {type}': '创建{type}',
  'Append {type}': '追加{type}',
  'Add {type}': '添加{type}',
  'Connect using {type}': '使用{type}连接',
  'Change element': '更改元素',
  'Change type': '更改类型',
  'Change {element} to {newType}': '将{element}更改为{newType}',

  // Context Pad - 上下文菜单
  'Append Task': '追加任务',
  'Append UserTask': '追加用户任务',
  'Append Gateway': '追加网关',
  'Append EndEvent': '追加结束事件',
  'Append Intermediate/Boundary Event': '追加中间/边界事件',
  'Append MessageIntermediateCatchEvent': '追加消息中间捕获事件',
  'Append TimerIntermediateCatchEvent': '追加定时器中间捕获事件',
  'Append ConditionIntermediateCatchEvent': '追加条件中间捕获事件',
  'Append SignalIntermediateCatchEvent': '追加信号中间捕获事件',
  'Connect using Association': '使用关联连接',
  'Connect using Sequence/MessageFlow or Association': '使用顺序流/消息流或关联连接',
  'Connect using DataInputAssociation': '使用数据输入关联连接',
  'Remove': '删除',
  'Delete': '删除',

  // Tasks - 任务类型
  'Task': '任务',
  'User Task': '用户任务',
  'Service Task': '服务任务',
  'Script Task': '脚本任务',
  'Business Rule Task': '业务规则任务',
  'Send Task': '发送任务',
  'Receive Task': '接收任务',
  'Manual Task': '手工任务',
  'Call Activity': '调用活动',
  'Sub Process (collapsed)': '子流程（折叠）',
  'Sub Process (expanded)': '子流程（展开）',
  'Sub Process': '子流程',

  // Events - 事件类型
  'Start Event': '开始事件',
  'End Event': '结束事件',
  'Intermediate Throw Event': '中间抛出事件',
  'Intermediate Catch Event': '中间捕获事件',
  'Boundary Event': '边界事件',

  // Start Events
  'Message Start Event': '消息开始事件',
  'Timer Start Event': '定时器开始事件',
  'Conditional Start Event': '条件开始事件',
  'Signal Start Event': '信号开始事件',

  // End Events
  'Message End Event': '消息结束事件',
  'Escalation End Event': '升级结束事件',
  'Error End Event': '错误结束事件',
  'Compensation End Event': '补偿结束事件',
  'Signal End Event': '信号结束事件',
  'Terminate End Event': '终止结束事件',

  // Intermediate Events
  'Message Intermediate Catch Event': '消息中间捕获事件',
  'Message Intermediate Throw Event': '消息中间抛出事件',
  'Timer Intermediate Catch Event': '定时器中间捕获事件',
  'Escalation Intermediate Throw Event': '升级中间抛出事件',
  'Conditional Intermediate Catch Event': '条件中间捕获事件',
  'Link Intermediate Catch Event': '链接中间捕获事件',
  'Link Intermediate Throw Event': '链接中间抛出事件',
  'Compensation Intermediate Throw Event': '补偿中间抛出事件',
  'Signal Intermediate Catch Event': '信号中间捕获事件',
  'Signal Intermediate Throw Event': '信号中间抛出事件',

  // Boundary Events
  'Message Boundary Event': '消息边界事件',
  'Timer Boundary Event': '定时器边界事件',
  'Escalation Boundary Event': '升级边界事件',
  'Conditional Boundary Event': '条件边界事件',
  'Error Boundary Event': '错误边界事件',
  'Compensation Boundary Event': '补偿边界事件',
  'Signal Boundary Event': '信号边界事件',
  'Non-interrupting Message Boundary Event': '非中断消息边界事件',
  'Non-interrupting Timer Boundary Event': '非中断定时器边界事件',
  'Non-interrupting Escalation Boundary Event': '非中断升级边界事件',
  'Non-interrupting Conditional Boundary Event': '非中断条件边界事件',
  'Non-interrupting Signal Boundary Event': '非中断信号边界事件',

  // Gateways - 网关类型
  'Gateway': '网关',
  'Exclusive Gateway': '排他网关',
  'Parallel Gateway': '并行网关',
  'Inclusive Gateway': '包容网关',
  'Complex Gateway': '复杂网关',
  'Event based Gateway': '事件网关',

  // Flows - 连线类型
  'Sequence Flow': '顺序流',
  'Message Flow': '消息流',
  'Association': '关联',
  'Data Association': '数据关联',
  'Default Flow': '默认流',
  'Conditional Flow': '条件流',

  // Artifacts
  'Data Object Reference': '数据对象引用',
  'Data Store Reference': '数据存储引用',
  'Group': '分组',
  'Text Annotation': '文本注释',

  // Participant
  'Participant': '参与者',
  'Pool': '池',
  'Lane': '泳道',
  'Expanded Pool': '展开的池',
  'Collapsed Pool': '折叠的池',

  // Properties Panel
  'General': '常规',
  'Details': '详情',
  'Id': '标识',
  'Name': '名称',
  'Documentation': '文档',
  'Element Documentation': '元素文档',
  'Process Documentation': '流程文档',
  'Execution listeners': '执行监听器',
  'Task listeners': '任务监听器',
  'Extensions': '扩展',
  'Input/Output': '输入/输出',
  'Connector': '连接器',
  'Input Parameters': '输入参数',
  'Output Parameters': '输出参数',
  'Asynchronous Continuations': '异步延续',
  'Job Configuration': '作业配置',
  'Candidate Starter Configuration': '候选启动器配置',
  'History Configuration': '历史配置',
  'Tasklist Configuration': '任务列表配置',
  'External Task Configuration': '外部任务配置',
  'Retry Time Cycle': '重试时间周期',
  'Due Date': '截止日期',
  'Follow Up Date': '跟进日期',
  'Priority': '优先级',
  'Assignee': '办理人',
  'Candidate Users': '候选用户',
  'Candidate Groups': '候选组',
  'Forms': '表单',
  'Form Key': '表单键',
  'Form Type': '表单类型',
  'Form Fields': '表单字段',
  'Business Key': '业务键',

  // Multi-instance
  'Multi Instance': '多实例',
  'Loop': '循环',
  'Loop Characteristics': '循环特性',
  'Sequential': '串行',
  'Parallel': '并行',
  'Collection': '集合',
  'Element Variable': '元素变量',
  'Completion Condition': '完成条件',

  // Compensation
  'Is for Compensation': '用于补偿',
  'Activity Ref': '活动引用',
  'Wait for Completion': '等待完成',

  // Script
  'Script': '脚本',
  'Script Format': '脚本格式',
  'Script Type': '脚本类型',
  'Result Variable': '结果变量',

  // Service Task
  'Implementation': '实现',
  'Java Class': 'Java类',
  'Expression': '表达式',
  'Delegate Expression': '委托表达式',
  'DMN': 'DMN',
  'External': '外部',
  'Topic': '主题',

  // Message
  'Message': '消息',
  'Message Name': '消息名称',
  'Message Ref': '消息引用',

  // Signal
  'Signal': '信号',
  'Signal Name': '信号名称',
  'Signal Ref': '信号引用',

  // Error
  'Error': '错误',
  'Error Name': '错误名称',
  'Error Code': '错误代码',
  'Error Ref': '错误引用',

  // Timer
  'Timer': '定时器',
  'Timer Definition Type': '定时器定义类型',
  'Timer Definition': '定时器定义',
  'Date': '日期',
  'Duration': '持续时间',
  'Cycle': '循环',

  // Condition
  'Condition': '条件',
  'Condition Type': '条件类型',
  'Condition Expression': '条件表达式',
  'Variable Name': '变量名',
  'Variable Event': '变量事件',

  // Actions
  'Edit': '编辑',
  'Copy': '复制',
  'Paste': '粘贴',
  'Undo': '撤销',
  'Redo': '重做',
  'Find': '查找',
  'Move to Origin': '移动到原点',
  'Move Canvas': '移动画布',
  'Hand Tool': '拖拽工具',
  'Lasso Tool': '套索工具',
  'Space Tool': '空间工具',
  'Global Connect Tool': '全局连接工具',

  // Validation
  'element required': '必填元素',
  'must reference': '必须引用',
  'must be set': '必须设置',
  'must be defined': '必须定义',
  'is invalid': '无效',
  'is missing': '缺失',

  // Misc
  'no parent for {element} in {parent}': '{parent}中没有{element}的父元素',
  'no shape type specified': '未指定形状类型',
  'flow elements must be children of pools/participants': '流程元素必须是池/参与者的子元素',
  'out of bounds release': '超出边界释放',
  'more': '更多',
  'fewer': '更少',
  'element {element} referenced by {referenced}#{property} not yet drawn': '由{referenced}#{property}引用的元素{element}尚未绘制',
  'element {element} referenced by {referenced}#{property}': '由{referenced}#{property}引用的元素{element}',

  // 泳道
  'Add Lane above': '在上方添加泳道',
  'Add Lane below': '在下方添加泳道',
  'Divide into two Lanes': '分成两个泳道',
  'Divide into three Lanes': '分成三个泳道',

  // 颜色
  'Set Color': '设置颜色',
  'Default': '默认',

  // 注释和连接
  'Connect to annotation': '连接到注释',
  'Connect to other element': '连接到其他元素',
  'Add text annotation': '添加文本注释',
  'Assign': '分配',

  // Context Pad - 更多上下文菜单
  'Append task': '追加任务',
  'Append gateway': '追加网关',
  'Append end event': '追加结束事件',
  'Append intermediate/boundary event': '追加中间/边界事件',
  'Change to parallel multi-instance': '更改为并行多实例',
  'Change to sequential multi-instance': '更改为串行多实例',
  'Ad-hoc': '即席',
  'Toggle ad-hoc sub-process marker': '切换即席子流程标记',
  'Toggle multi-instance marker': '切换多实例标记',
  'Collapse (remove content)': '折叠（删除内容）',
  'Expand': '展开',

  // 快捷方式
  'Keyboard Shortcuts': '键盘快捷键',

  // 导入导出
  'Save diagram': '保存流程图',
  'Open diagram': '打开流程图',
  'Export as image': '导出为图片',
  'Export as XML': '导出为XML',
  'Print diagram': '打印流程图',

  // 其他常用
  'type': '类型',
  'value': '值',
  'property': '属性',
  'properties': '属性',
  'variable': '变量',
  'yes': '是',
  'no': '否',
  'true': '是',
  'false': '否',
  'none': '无',
  'empty': '空',
  'undefined': '未定义',

  // Replace Menu - 替换菜单（小写）
  'Start event': '开始事件',
  'End event': '结束事件',
  'User task': '用户任务',
  'Service task': '服务任务',
  'Script task': '脚本任务',
  'Business rule task': '业务规则任务',
  'Send task': '发送任务',
  'Receive task': '接收任务',
  'Manual task': '手工任务',
  'Exclusive gateway': '排他网关',
  'Parallel gateway': '并行网关',
  'Inclusive gateway': '包容网关',
  'Complex gateway': '复杂网关',
  'Event-based gateway': '事件网关',
  'Intermediate throw event': '中间抛出事件',
  'Intermediate catch event': '中间捕获事件',
  'Boundary event': '边界事件',
  'Transaction': '事务',
  'Event sub-process': '事件子流程',
  'Sub-process': '子流程',
  'Collapsed sub-process': '折叠的子流程',
  'Expanded sub-process': '展开的子流程',

  // 缩写类型（palette 使用）
  'StartEvent': '开始事件',
  'EndEvent': '结束事件',
  'UserTask': '用户任务',
  'ServiceTask': '服务任务',
  'ScriptTask': '脚本任务',
  'BusinessRuleTask': '业务规则任务',
  'SendTask': '发送任务',
  'ReceiveTask': '接收任务',
  'ManualTask': '手工任务',
  'ExclusiveGateway': '排他网关',
  'ParallelGateway': '并行网关',
  'InclusiveGateway': '包容网关',
  'ComplexGateway': '复杂网关',
  'EventBasedGateway': '事件网关',
  'IntermediateThrowEvent': '中间抛出事件',
  'IntermediateCatchEvent': '中间捕获事件',
  'BoundaryEvent': '边界事件',
  'DataObjectReference': '数据对象引用',
  'DataStoreReference': '数据存储引用',
  'TextAnnotation': '文本注释',
  'CallActivity': '调用活动',
  'SubProcess': '子流程'
}

export default translations
