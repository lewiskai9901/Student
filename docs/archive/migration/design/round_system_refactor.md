# 检查轮次系统重构方案

## 1. 问题分析

### 当前设计（类别独立轮次）
```
卫生检查: check_rounds = 2  → 第1轮(早), 第2轮(晚)
纪律检查: check_rounds = 3  → 第1轮(早), 第2轮(中), 第3轮(晚)
```

### 问题
1. **语义混乱**：卫生的"第2轮"=晚上，纪律的"第2轮"=中午，轮次号相同但含义不同
2. **不符合实际**：检查人员按时间段（早/中/晚）检查，不是按类别分别计轮
3. **统计困难**：想统计"上午检查情况"需要知道每个类别的轮次映射关系
4. **操作复杂**：录入时需要记住每个类别当前是第几轮

---

## 2. 新设计（全局统一轮次）

### 核心思想
- 在检查层面定义总轮次（如3轮=早/中/晚）
- 每个类别配置"参与哪些轮次"

### 示例
```
本次检查：total_rounds = 3（早、中、晚）

类别配置：
- 卫生检查: participated_rounds = "1,3"  → 参与早、晚
- 纪律检查: participated_rounds = "1,2,3" → 参与早、中、晚

效果：
第1轮(早)：卫生 ✓  纪律 ✓
第2轮(中)：卫生 ✗  纪律 ✓
第3轮(晚)：卫生 ✓  纪律 ✓
```

### 优点
1. **语义清晰**：第1轮=早上，第2轮=中午，第3轮=晚上，所有人都能理解
2. **符合实际**：检查人员选"第2轮"就是中午检查
3. **统计简单**：按轮次直接汇总所有类别
4. **配置灵活**：每个类别可以参与任意轮次组合

---

## 3. 数据库修改

### 3.1 检查模板表 (check_templates)
```sql
-- 新增字段
ALTER TABLE check_templates
ADD COLUMN total_rounds INT DEFAULT 1 COMMENT '总轮次数' AFTER description;
```

### 3.2 模板类别表 (template_categories)
```sql
-- 修改字段：从 check_rounds(int) 改为 participated_rounds(varchar)
ALTER TABLE template_categories
ADD COLUMN participated_rounds VARCHAR(50) DEFAULT '1' COMMENT '参与的轮次,逗号分隔,如1,3' AFTER check_rounds;

-- 数据迁移：将 check_rounds=2 转换为 participated_rounds="1,2"
UPDATE template_categories
SET participated_rounds = CASE
    WHEN check_rounds = 1 THEN '1'
    WHEN check_rounds = 2 THEN '1,2'
    WHEN check_rounds = 3 THEN '1,2,3'
    WHEN check_rounds = 4 THEN '1,2,3,4'
    ELSE '1'
END;

-- 更新模板的 total_rounds（取类别中最大的 check_rounds）
UPDATE check_templates ct SET total_rounds = (
    SELECT COALESCE(MAX(check_rounds), 1)
    FROM template_categories tc
    WHERE tc.template_id = ct.id AND tc.deleted = 0
);
```

### 3.3 日常检查表 (daily_checks)
```sql
-- 新增字段
ALTER TABLE daily_checks
ADD COLUMN total_rounds INT DEFAULT 1 COMMENT '总轮次数' AFTER check_type;
```

### 3.4 检查类别表 (daily_check_categories)
```sql
-- 新增字段
ALTER TABLE daily_check_categories
ADD COLUMN participated_rounds VARCHAR(50) DEFAULT '1' COMMENT '参与的轮次,逗号分隔' AFTER check_rounds;

-- 数据迁移
UPDATE daily_check_categories
SET participated_rounds = CASE
    WHEN check_rounds = 1 THEN '1'
    WHEN check_rounds = 2 THEN '1,2'
    WHEN check_rounds = 3 THEN '1,2,3'
    WHEN check_rounds = 4 THEN '1,2,3,4'
    ELSE '1'
END;

-- 更新检查的 total_rounds
UPDATE daily_checks dc SET total_rounds = (
    SELECT COALESCE(MAX(check_rounds), 1)
    FROM daily_check_categories dcc
    WHERE dcc.check_id = dc.id AND dcc.deleted = 0
);
```

---

## 4. 后端修改

### 4.1 实体类修改

**DailyCheck.java**
```java
// 新增字段
private Integer totalRounds;
```

**DailyCheckCategory.java**
```java
// 新增字段
private String participatedRounds;

// 辅助方法
public List<Integer> getParticipatedRoundsList() {
    if (participatedRounds == null || participatedRounds.isEmpty()) {
        return Collections.singletonList(1);
    }
    return Arrays.stream(participatedRounds.split(","))
        .map(String::trim)
        .map(Integer::parseInt)
        .collect(Collectors.toList());
}

public boolean participatesInRound(int round) {
    return getParticipatedRoundsList().contains(round);
}
```

**CheckTemplate.java** 和 **TemplateCategory.java** 同理修改

### 4.2 DTO修改

**DailyScoringInitResponse.java**
```java
public class DailyScoringInitResponse {
    // 新增
    private Integer totalRounds;

    @Data
    public static class CategoryInfo {
        // 修改：从 checkRounds 改为 participatedRounds
        private String participatedRounds;

        // 辅助字段（方便前端使用）
        private List<Integer> participatedRoundsList;
    }
}
```

### 4.3 Service修改

**DailyCheckScoringServiceImpl.java** - 初始化方法
```java
// 返回 totalRounds 和每个类别的 participatedRounds
response.setTotalRounds(dailyCheck.getTotalRounds());
categoryInfo.setParticipatedRounds(category.getParticipatedRounds());
```

**录入验证**
```java
// 验证当前轮次是否在该类别的参与轮次内
if (!category.participatesInRound(checkRound)) {
    throw new BusinessException("该类别不参与第" + checkRound + "轮检查");
}
```

---

## 5. 前端修改

### 5.1 轮次选择逻辑变更

**从**：类别层面的轮次选择
```vue
<!-- 每个类别独立显示轮次 -->
<div v-if="currentCategory.checkRounds > 1">
  <button v-for="round in currentCategory.checkRounds">第{{round}}轮</button>
</div>
```

**改为**：检查层面的全局轮次选择
```vue
<!-- 全局轮次选择（类别列表上方） -->
<div v-if="initData.totalRounds > 1" class="round-selector">
  <button
    v-for="round in initData.totalRounds"
    :key="round"
    @click="handleGlobalRoundChange(round)"
    :class="{ active: activeRound === round }"
  >
    第{{round}}轮
  </button>
</div>

<!-- 类别列表（根据当前轮次过滤，只显示参与该轮次的类别） -->
<div v-for="category in filteredCategories">
  {{ category.categoryName }}
</div>
```

### 5.2 类别过滤逻辑
```typescript
// 根据当前选中的轮次，过滤出参与该轮次的类别
const filteredCategories = computed(() => {
  return initData.categories.filter(cat => {
    const rounds = cat.participatedRounds?.split(',').map(Number) || [1]
    return rounds.includes(activeRound.value)
  })
})
```

### 5.3 快捷录入修改
```vue
<!-- 先选轮次 -->
<div class="round-selector">
  <button v-for="round in totalRounds" @click="selectRound(round)">
    第{{round}}轮
  </button>
</div>

<!-- 再选扣分项（根据轮次过滤） -->
<div v-for="item in filteredDeductionItems">
  <!-- 只显示参与当前轮次的类别下的扣分项 -->
</div>
```

---

## 6. UI交互设计

### 6.1 检查录入页面

```
┌─────────────────────────────────────────────────────────────┐
│  ← 返回   2025-12-17 日常检查                    扣分: -15  │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────────────┐   │
│  │  [第1轮(早)] [第2轮(中)] [第3轮(晚)]  ← 全局轮次选择  │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌────────────┐  ┌────────────────────────────────────────┐ │
│  │ 检查类别    │  │ 班级列表 / 扣分项                      │ │
│  │            │  │                                        │ │
│  │ [卫生检查] │  │  ...                                   │ │
│  │ [纪律检查] │  │                                        │ │
│  │            │  │                                        │ │
│  │ (仅显示参  │  │                                        │ │
│  │  与当前轮  │  │                                        │ │
│  │  次的类别) │  │                                        │ │
│  └────────────┘  └────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 6.2 模板配置页面

```
┌─────────────────────────────────────────────────────────────┐
│  模板名称: 日常检查模板                                      │
│  总轮次数: [3] ← 可配置                                      │
├─────────────────────────────────────────────────────────────┤
│  检查类别                        参与轮次                    │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ 卫生检查                      [✓]1 [ ]2 [✓]3         │   │
│  │ 纪律检查                      [✓]1 [✓]2 [✓]3         │   │
│  │ 出勤检查                      [✓]1 [ ]2 [ ]3         │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

---

## 7. 向后兼容

### 7.1 数据迁移策略
- 保留原 `check_rounds` 字段（不删除，标记为废弃）
- 新增 `participated_rounds` 字段
- 运行迁移脚本转换数据
- 后续版本再清理废弃字段

### 7.2 API兼容
- 新增字段不影响现有API消费者
- 逐步废弃 `checkRounds` 字段，改用 `participatedRounds`

---

## 8. 实施步骤

1. **数据库修改**（10分钟）
   - 执行 ALTER TABLE 添加新字段
   - 执行数据迁移脚本

2. **后端修改**（30分钟）
   - 修改实体类
   - 修改DTO
   - 修改Service层逻辑
   - 修改模板配置接口

3. **前端修改**（1小时）
   - 修改录入页面轮次选择逻辑
   - 修改模板配置页面
   - 修改快捷录入组件
   - 修改统计报表

4. **测试验证**（30分钟）
   - 测试模板配置
   - 测试检查录入
   - 测试统计报表
   - 测试数据迁移结果

---

## 9. 确认事项

请确认以下内容后开始实施：

1. 是否同意上述设计方案？
2. 轮次是否需要命名？（如"早检"、"中检"、"晚检"）还是只用数字？
3. 是否需要保留原有的 `check_rounds` 字段做兼容？
