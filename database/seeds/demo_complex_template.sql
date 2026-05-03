-- ============================================================
-- Demo: 复杂检查模板 — 综合校园检查 (4 大区 / 9 子分区 / 25 检查项)
-- 评分模式覆盖: PASS_FAIL / DEDUCTION / ADDITION / DIRECT / LEVEL
-- ============================================================

-- 清理旧的 demo (如果存在, code 唯一)
DELETE FROM insp_template_items WHERE section_id IN (
  SELECT id FROM (SELECT id FROM insp_template_sections WHERE section_code LIKE 'CMP_%') AS s
);
DELETE FROM insp_template_sections WHERE section_code LIKE 'CMP_%';

-- ───────── 根分区: 综合校园检查 (id 100) ─────────
INSERT INTO insp_template_sections
  (id, tenant_id, parent_section_id, section_code, section_name, target_type,
   description, tags, status, latest_version, sort_order, input_mode,
   inspection_mode, target_type_filter, created_by, created_at)
VALUES
  (100, 0, NULL, 'CMP_ROOT', '综合校园检查',
   'ORG', '覆盖卫生/安全/教学/学生 4 大维度的综合性巡检模板, 含 25 检查项',
   '综合,巡检,周检', 'DRAFT', 0, 100, 'INLINE',
   'SNAPSHOT', '["教学班","年级","系部","学校"]', 1, NOW());

-- ───────── 4 个一级分区 ─────────
INSERT INTO insp_template_sections
  (id, tenant_id, parent_section_id, section_code, section_name,
   description, status, sort_order, input_mode, created_by, created_at) VALUES
  (101, 0, 100, 'CMP_HYG',  '卫生与环境', '校园卫生 / 教室 / 公共区域整洁度',  'DRAFT', 1, 'INLINE', 1, NOW()),
  (102, 0, 100, 'CMP_SAF',  '安全管理',   '消防 / 电气 / 设施安全',           'DRAFT', 2, 'INLINE', 1, NOW()),
  (103, 0, 100, 'CMP_TCH',  '教学秩序',   '课堂 / 课间 / 教学规范',           'DRAFT', 3, 'INLINE', 1, NOW()),
  (104, 0, 100, 'CMP_STU',  '学生行为',   '仪容仪表 / 文明礼貌 / 守纪守规',     'DRAFT', 4, 'INLINE', 1, NOW());

-- ───────── 9 个叶子分区 ─────────
INSERT INTO insp_template_sections
  (id, tenant_id, parent_section_id, section_code, section_name,
   status, sort_order, input_mode, created_by, created_at) VALUES
  -- 卫生与环境 (3)
  (110, 0, 101, 'CMP_HYG_CLS', '教室卫生',   'DRAFT', 1, 'INLINE', 1, NOW()),
  (111, 0, 101, 'CMP_HYG_HW',  '楼道卫生',   'DRAFT', 2, 'INLINE', 1, NOW()),
  (112, 0, 101, 'CMP_HYG_PUB', '公共区域',   'DRAFT', 3, 'INLINE', 1, NOW()),
  -- 安全管理 (2)
  (120, 0, 102, 'CMP_SAF_FIRE', '消防安全',  'DRAFT', 1, 'INLINE', 1, NOW()),
  (121, 0, 102, 'CMP_SAF_ELEC', '电气安全',  'DRAFT', 2, 'INLINE', 1, NOW()),
  -- 教学秩序 (2)
  (130, 0, 103, 'CMP_TCH_CLS',  '课堂纪律',  'DRAFT', 1, 'INLINE', 1, NOW()),
  (131, 0, 103, 'CMP_TCH_BRK',  '课间秩序',  'DRAFT', 2, 'INLINE', 1, NOW()),
  -- 学生行为 (2)
  (140, 0, 104, 'CMP_STU_DRESS','仪容仪表',  'DRAFT', 1, 'INLINE', 1, NOW()),
  (141, 0, 104, 'CMP_STU_CIV',  '文明礼貌',  'DRAFT', 2, 'INLINE', 1, NOW());

-- ───────── 25 个检查项 (覆盖各种评分模式) ─────────
INSERT INTO insp_template_items
  (id, tenant_id, section_id, item_code, item_name, item_type, scoring_config,
   description, is_scored, sort_order, item_weight, input_mode, created_by, created_at) VALUES

  -- 教室卫生 (3 项 — PASS_FAIL)
  (1001, 0, 110, 'HYG_CLS_FLOOR',  '地面清洁',  'PASS_FAIL',
   '{"mode":"PASS_FAIL","passScore":0,"failScore":-3}',  '地面无垃圾、无水渍', 1, 1, 1.00, 'INLINE', 1, NOW()),
  (1002, 0, 110, 'HYG_CLS_DESK',   '桌椅整洁',  'PASS_FAIL',
   '{"mode":"PASS_FAIL","passScore":0,"failScore":-2}',  '课桌摆放整齐, 无涂鸦', 1, 2, 1.00, 'INLINE', 1, NOW()),
  (1003, 0, 110, 'HYG_CLS_BOARD',  '黑板擦净',  'PASS_FAIL',
   '{"mode":"PASS_FAIL","passScore":0,"failScore":-1}',  '黑板字迹擦净, 粉笔擦清洗', 1, 3, 0.50, 'INLINE', 1, NOW()),

  -- 楼道卫生 (3 项 — DEDUCTION)
  (1010, 0, 111, 'HYG_HW_CLEAN',   '楼道清洁',  'NUMBER',
   '{"mode":"DEDUCTION","maxDeduction":-5,"step":1}',  '楼道地面无垃圾、墙面无污渍', 1, 1, 1.00, 'INLINE', 1, NOW()),
  (1011, 0, 111, 'HYG_HW_BIN',     '垃圾桶状态','NUMBER',
   '{"mode":"DEDUCTION","maxDeduction":-2,"step":1}',  '垃圾桶不溢满, 周边无散落', 1, 2, 0.80, 'INLINE', 1, NOW()),
  (1012, 0, 111, 'HYG_HW_SIGN',    '宣传栏整洁','PASS_FAIL',
   '{"mode":"PASS_FAIL","passScore":0,"failScore":-1}', '宣传栏完整, 海报无破损', 1, 3, 0.50, 'INLINE', 1, NOW()),

  -- 公共区域 (3 项 — DIRECT 直接打分)
  (1020, 0, 112, 'HYG_PUB_WC',     '厕所卫生',  'NUMBER',
   '{"mode":"DIRECT","minScore":0,"maxScore":10,"step":0.5}',  '0-10 分直接评估卫生状况', 1, 1, 1.50, 'INLINE', 1, NOW()),
  (1021, 0, 112, 'HYG_PUB_DINING', '食堂卫生',  'NUMBER',
   '{"mode":"DIRECT","minScore":0,"maxScore":10,"step":0.5}',  '0-10 分直接评估', 1, 2, 1.50, 'INLINE', 1, NOW()),
  (1022, 0, 112, 'HYG_PUB_FIELD',  '操场环境',  'NUMBER',
   '{"mode":"DIRECT","minScore":0,"maxScore":5,"step":1}',  '0-5 分直接评估', 1, 3, 1.00, 'INLINE', 1, NOW()),

  -- 消防安全 (3 项 — PASS_FAIL 严重扣分)
  (1030, 0, 120, 'SAF_FIRE_EXT',   '灭火器到位','PASS_FAIL',
   '{"mode":"PASS_FAIL","passScore":0,"failScore":-10}', '灭火器不缺失, 压力正常', 1, 1, 2.00, 'INLINE', 1, NOW()),
  (1031, 0, 120, 'SAF_FIRE_EXIT',  '安全通道',  'PASS_FAIL',
   '{"mode":"PASS_FAIL","passScore":0,"failScore":-10}', '安全通道无堵塞, 标识清晰', 1, 2, 2.00, 'INLINE', 1, NOW()),
  (1032, 0, 120, 'SAF_FIRE_EMERG', '应急照明',  'PASS_FAIL',
   '{"mode":"PASS_FAIL","passScore":0,"failScore":-5}', '应急灯通电正常', 1, 3, 1.00, 'INLINE', 1, NOW()),

  -- 电气安全 (3 项 — DEDUCTION + 等级评分)
  (1040, 0, 121, 'SAF_ELEC_PLUG',  '插座状态',  'NUMBER',
   '{"mode":"DEDUCTION","maxDeduction":-5,"step":1}', '插座无破损, 无私接乱接', 1, 1, 1.00, 'INLINE', 1, NOW()),
  (1041, 0, 121, 'SAF_ELEC_WIRE',  '线路敷设',  'SELECT',
   '{"mode":"LEVEL","levels":[{"label":"优","score":5},{"label":"良","score":3},{"label":"差","score":-3}]}',
   '电线规范敷设, 无裸露', 1, 2, 1.50, 'INLINE', 1, NOW()),
  (1042, 0, 121, 'SAF_ELEC_DEV',   '用电设备',  'PASS_FAIL',
   '{"mode":"PASS_FAIL","passScore":0,"failScore":-5}', '设备完好, 无超负荷', 1, 3, 1.00, 'INLINE', 1, NOW()),

  -- 课堂纪律 (3 项 — DIRECT + ADDITION)
  (1050, 0, 130, 'TCH_CLS_DISC',   '学生纪律',  'NUMBER',
   '{"mode":"DIRECT","minScore":0,"maxScore":10}', '学生听讲专注, 无嬉戏', 1, 1, 1.00, 'INLINE', 1, NOW()),
  (1051, 0, 130, 'TCH_CLS_TEACH',  '教师授课',  'SELECT',
   '{"mode":"LEVEL","levels":[{"label":"优秀","score":5},{"label":"良好","score":3},{"label":"合格","score":0},{"label":"待改进","score":-3}]}',
   '授课内容充实, 互动良好', 1, 2, 1.50, 'INLINE', 1, NOW()),
  (1052, 0, 130, 'TCH_CLS_INNOV',  '教学创新',  'NUMBER',
   '{"mode":"ADDITION","maxBonus":3,"step":1}', '加分项 — 使用创新教学方法', 1, 3, 0.50, 'INLINE', 1, NOW()),

  -- 课间秩序 (2 项 — PASS_FAIL)
  (1060, 0, 131, 'TCH_BRK_HALL',   '课间走动',  'PASS_FAIL',
   '{"mode":"PASS_FAIL","passScore":0,"failScore":-2}', '不在楼道追跑, 不大声喧哗', 1, 1, 0.80, 'INLINE', 1, NOW()),
  (1061, 0, 131, 'TCH_BRK_ORDER',  '楼道秩序',  'PASS_FAIL',
   '{"mode":"PASS_FAIL","passScore":0,"failScore":-2}', '上下楼右行靠右, 排队有序', 1, 2, 0.80, 'INLINE', 1, NOW()),

  -- 仪容仪表 (3 项 — PASS_FAIL)
  (1070, 0, 140, 'STU_DRESS_UNI',  '校服整洁',  'PASS_FAIL',
   '{"mode":"PASS_FAIL","passScore":0,"failScore":-1}', '统一着校服, 整洁干净', 1, 1, 0.50, 'INLINE', 1, NOW()),
  (1071, 0, 140, 'STU_DRESS_HAIR', '发型规范',  'PASS_FAIL',
   '{"mode":"PASS_FAIL","passScore":0,"failScore":-1}', '不染发不烫发, 长度合规', 1, 2, 0.50, 'INLINE', 1, NOW()),
  (1072, 0, 140, 'STU_DRESS_ACC',  '佩饰简洁',  'PASS_FAIL',
   '{"mode":"PASS_FAIL","passScore":0,"failScore":-0.5}', '不佩戴贵重首饰', 1, 3, 0.30, 'INLINE', 1, NOW()),

  -- 文明礼貌 (2 项 — ADDITION)
  (1080, 0, 141, 'STU_CIV_GREET',  '问候老师',  'NUMBER',
   '{"mode":"ADDITION","maxBonus":2,"step":1}', '加分项 — 主动向老师问好', 1, 1, 0.30, 'INLINE', 1, NOW()),
  (1081, 0, 141, 'STU_CIV_PEACE',  '校园礼仪',  'PASS_FAIL',
   '{"mode":"PASS_FAIL","passScore":0,"failScore":-1}', '不追跑打闹, 不打架斗殴', 1, 2, 0.50, 'INLINE', 1, NOW());

-- 验证
SELECT '=== 模板结构 ===' AS '';
SELECT id, section_code, section_name, parent_section_id, sort_order
FROM insp_template_sections WHERE section_code LIKE 'CMP_%'
ORDER BY parent_section_id IS NOT NULL, parent_section_id, sort_order;

SELECT '=== 检查项分布 ===' AS '';
SELECT
  s.section_name AS '叶子分区',
  COUNT(i.id) AS '项数',
  GROUP_CONCAT(DISTINCT JSON_UNQUOTE(JSON_EXTRACT(i.scoring_config, '$.mode')) ORDER BY 1) AS '评分模式'
FROM insp_template_sections s
LEFT JOIN insp_template_items i ON i.section_id = s.id AND i.deleted = 0
WHERE s.section_code LIKE 'CMP_%' AND s.parent_section_id NOT IN (100) AND s.parent_section_id IS NOT NULL
GROUP BY s.id, s.section_name
ORDER BY s.sort_order;

SELECT '=== 模板汇总 ===' AS '';
SELECT
  '综合校园检查' AS '模板名',
  (SELECT COUNT(*) FROM insp_template_sections WHERE section_code LIKE 'CMP_%' AND parent_section_id = 100) AS '一级分区',
  (SELECT COUNT(*) FROM insp_template_sections WHERE section_code LIKE 'CMP_%' AND parent_section_id NOT IN (100) AND parent_section_id IS NOT NULL) AS '叶子分区',
  (SELECT COUNT(*) FROM insp_template_items WHERE section_id IN (SELECT id FROM insp_template_sections WHERE section_code LIKE 'CMP_%')) AS '检查项总数';
