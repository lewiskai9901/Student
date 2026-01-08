# 评级模块重构实施计划

## 📋 项目概览

**重构目标**：
1. ✅ 增强评级次数统计功能（按等级、按班级、按院系多维度统计）
2. ✅ 新增荣誉徽章系统（自动授予、手动颁发、证书生成）
3. ✅ 深度集成通报系统（一键生成荣誉榜、全员通报、警示通报）
4. ✅ UI全面重构（卡片式设计、简约现代、减少选择框）

**预计工期**：5.5周

**当前进度**：第1阶段 - 数据库和后端开发（Week 1）

---

## ✅ 已完成工作（2025-12-22）

### 1. 数据库设计 ✓

**迁移脚本**：`database/migrations/V4.3.0__rating_refactor_enhancement.sql`

**新增表（6张）**：
- ✅ `rating_rule_template` - 评级规则模板表
- ✅ `rating_honor_badge` - 荣誉徽章配置表
- ✅ `class_badge_record` - 班级徽章获得记录表
- ✅ `rating_alert_config` - 评级预警配置表
- ✅ `rating_alert_record` - 评级预警记录表
- ✅ `rating_comparison_record` - 评级对比记录表

**增强表（3张）**：
- ✅ `check_plan_rating_rules` - 新增 `template_id`, `alert_enabled` 字段
- ✅ `check_plan_rating_frequency` - 新增 `consecutive_count`, `best_streak`, `recent_dates`, `level_color`, `level_icon` 字段
- ✅ `check_plan_rating_results` - 新增 `previous_level_id`, `level_change`, `percentile`, `department_id`, `department_name` 字段

**系统预设数据**：
- ✅ 插入4个系统预设评级规则模板（三等级标准、竞赛式评比、百分比稳定、纪律标兵）

**索引优化**：
- ✅ 为频繁查询的字段组合添加复合索引

### 2. 后端DTO类 ✓

**统计相关DTO**（5个）：
- ✅ `RatingFrequencyStatisticsVO` - 评级频次统计核心VO
- ✅ `ClassFrequencyDetailVO` - 班级频次详细VO
- ✅ `LevelFrequencyVO` - 等级频次统计（内部类）
- ✅ `DepartmentComparisonVO` - 院系对比VO（内部类）
- ✅ `ClassFrequencySimpleVO` - 班级频次简化VO（内部类）

**徽章相关DTO**（4个）：
- ✅ `RatingBadgeVO` - 徽章配置视图对象
- ✅ `RatingBadgeCreateDTO` - 创建徽章DTO
- ✅ `ClassBadgeRecordVO` - 班级徽章记录VO
- ✅ `BadgeGrantRequest` - 徽章授予请求DTO

**通报相关DTO**（3个）：
- ✅ `HonorNotificationRequest` - 荣誉通报生成请求
- ✅ `CertificateGenerateRequest` - 证书批量生成请求
- ✅ `NotificationGenerateResultVO` - 通报生成结果VO

---

## 🚧 后续工作计划

### 第一阶段：后端开发（剩余1周）

#### Week 1 - Day 3-5：Service层开发

**RatingStatisticsService（评级统计服务）**：
```java
public interface RatingStatisticsService {
    // 获取频次统计（核心方法）
    RatingFrequencyStatisticsVO getFrequencyStatistics(
        Long checkPlanId, Long ruleId, List<Long> levelIds,
        LocalDate periodStart, LocalDate periodEnd, String periodType
    );

    // 按等级获取班级列表
    List<ClassFrequencyDetailVO> getClassesByLevel(
        Long levelId, LocalDate periodStart, LocalDate periodEnd, String sortBy
    );

    // 获取班级荣誉详情
    ClassHonorVO getClassHonors(Long classId);

    // 获取连续获奖TOP榜
    List<ConsecutiveRecordVO> getConsecutiveTop(
        Long ruleId, Long levelId, Integer topN
    );

    // 计算并更新连续记录
    void calculateConsecutiveRecords(Long ruleId, LocalDate date);
}
```

**RatingBadgeService（荣誉徽章服务）**：
```java
public interface RatingBadgeService {
    // 创建徽章
    Long createBadge(RatingBadgeCreateDTO dto);

    // 获取徽章列表
    List<RatingBadgeVO> getBadgesByPlan(Long checkPlanId);

    // 检查并自动授予徽章
    List<BadgeGrantResultVO> checkAndGrantBadges(
        Long checkPlanId, LocalDate periodStart, LocalDate periodEnd
    );

    // 手动授予徽章
    void grantBadge(BadgeGrantRequest request);

    // 获取班级徽章记录
    List<ClassBadgeRecordVO> getClassBadgeRecords(Long classId);

    // 撤销徽章
    void revokeBadge(Long recordId, String reason);
}
```

**RatingNotificationService（评级通报服务）**：
```java
public interface RatingNotificationService {
    // 生成荣誉通报
    NotificationGenerateResultVO generateHonorNotification(
        HonorNotificationRequest request
    );

    // 生成全员通报
    NotificationGenerateResultVO generateFullNotification(
        FullNotificationRequest request
    );

    // 批量生成证书
    List<String> generateCertificates(
        CertificateGenerateRequest request
    );

    // 生成海报式公告
    String generatePoster(PosterGenerateRequest request);
}
```

### 第二阶段：Controller层开发（Week 2 - Day 1-3）

**RatingStatisticsController**：
- `GET /rating-statistics/frequency` - 获取评级频次统计
- `GET /rating-statistics/level/{levelId}/classes` - 按等级获取班级列表
- `GET /rating-statistics/class/{classId}/honors` - 获取班级荣誉
- `GET /rating-statistics/consecutive-top` - 获取连续获奖TOP榜

**RatingBadgeController**：
- `POST /rating-badge/create` - 创建徽章
- `GET /rating-badge/list` - 获取徽章列表
- `POST /rating-badge/check-and-grant` - 检查并授予徽章
- `POST /rating-badge/grant` - 手动授予徽章
- `GET /rating-badge/class/{classId}/records` - 获取班级徽章记录

**RatingNotificationController**：
- `POST /rating-notification/honor/generate` - 生成荣誉通报
- `POST /rating-notification/full/generate` - 生成全员通报
- `POST /rating-notification/certificate/batch` - 批量生成证书
- `POST /rating-notification/poster/generate` - 生成海报

### 第三阶段：前端开发（Week 2 Day 4-5 + Week 3-4）

#### 核心页面（Week 3）

**1. 评级次数统计中心页面**
- 路径：`frontend/src/views/quantification/RatingStatisticsCenter.vue`
- 组件：
  - `StatisticsFilter.vue` - 筛选器卡片
  - `OverviewCards.vue` - 概览卡片（4个）
  - `LevelStatisticsCard.vue` - 等级统计大卡片
  - `DepartmentComparison.vue` - 院系对比卡片

**2. 等级详情页面**
- 路径：`frontend/src/views/quantification/LevelDetailView.vue`
- 功能：展示某个等级下所有班级的详细频次信息
- 组件：
  - `ClassFrequencyTable.vue` - 班级频次表格
  - `ConsecutiveRecordBadge.vue` - 连续记录徽章

**3. 班级荣誉展示页面**
- 路径：`frontend/src/views/quantification/ClassHonorView.vue`
- 功能：展示班级的所有徽章和荣誉
- 组件：
  - `BadgeShowcase.vue` - 徽章展示区
  - `HonorTimeline.vue` - 荣誉时间轴

#### 通报功能（Week 4）

**4. 通报生成对话框**
- 路径：`frontend/src/views/quantification/components/NotificationGenerateDialog.vue`
- 功能：卡片式选择通报类型和配置
- 组件：
  - `NotificationTypeCard.vue` - 通报类型卡片
  - `LevelSelector.vue` - 等级选择器（卡片式）
  - `DisplayOptionsCard.vue` - 展示选项卡片

**5. 证书生成器**
- 路径：`frontend/src/views/quantification/components/CertificateGenerator.vue`
- 功能：批量生成荣誉证书
- 组件：
  - `CertificatePreview.vue` - 证书预览
  - `CertificateTemplate.vue` - 证书模板选择

**6. 徽章管理页面**
- 路径：`frontend/src/views/quantification/BadgeManagement.vue`
- 功能：配置和管理荣誉徽章
- 组件：
  - `BadgeConfigCard.vue` - 徽章配置卡片
  - `GrantConditionEditor.vue` - 授予条件编辑器

### 第四阶段：测试和优化（Week 5-6）

#### Week 5：功能测试
- 单元测试
- 集成测试
- 前后端联调

#### Week 6前半周：性能优化
- SQL查询优化
- 前端渲染优化
- 接口响应时间优化

#### Week 6后半周：上线准备
- 数据迁移测试
- 用户培训材料
- 部署和上线

---

## 📊 技术实现要点

### 1. 评级次数统计算法

**连续记录计算**：
```java
// 伪代码
public void calculateConsecutiveRecords(Long ruleId, LocalDate date) {
    // 1. 获取所有班级当天的评级结果
    List<RatingResult> results = getRatingResults(ruleId, date);

    // 2. 对每个班级，获取其历史评级记录
    for (RatingResult result : results) {
        List<RatingResult> history = getHistoryResults(
            result.getClassId(), ruleId, result.getLevelId()
        );

        // 3. 倒序遍历，计算连续次数
        int consecutive = 0;
        LocalDate currentDate = date;
        for (RatingResult h : history) {
            if (h.getCheckDate().equals(currentDate.minusDays(1))) {
                consecutive++;
                currentDate = h.getCheckDate();
            } else {
                break;
            }
        }

        // 4. 更新频次表的consecutive_count和best_streak
        updateFrequencyRecord(result.getClassId(), result.getLevelId(),
                             consecutive, Math.max(consecutive, currentBestStreak));
    }
}
```

### 2. 徽章自动授予机制

**触发时机**：
- 评级结果发布后（监听CheckRecordPublishedEvent）
- 定时任务（每日凌晨检查）
- 手动触发

**授予逻辑**：
```java
public List<BadgeGrantResultVO> checkAndGrantBadges(
    Long checkPlanId, LocalDate periodStart, LocalDate periodEnd
) {
    // 1. 获取所有启用的徽章配置
    List<RatingBadge> badges = getEnabledBadges(checkPlanId);

    // 2. 对每个徽章，检查符合条件的班级
    List<BadgeGrantResultVO> results = new ArrayList<>();
    for (RatingBadge badge : badges) {
        List<Long> qualifiedClassIds = findQualifiedClasses(
            badge.getGrantCondition(), periodStart, periodEnd
        );

        // 3. 过滤已经获得该徽章的班级
        qualifiedClassIds = filterAlreadyGranted(
            badge.getId(), qualifiedClassIds, periodStart, periodEnd
        );

        // 4. 授予徽章
        for (Long classId : qualifiedClassIds) {
            grantBadgeToClass(badge.getId(), classId, periodStart, periodEnd);
            results.add(new BadgeGrantResultVO(badge, classId));
        }
    }

    return results;
}
```

### 3. 通报文档生成

**Word文档生成（Apache POI）**：
```java
public String generateHonorNotification(HonorNotificationRequest request) {
    // 1. 查询数据
    RatingFrequencyStatisticsVO stats = getFrequencyStatistics(request);

    // 2. 创建Word文档
    XWPFDocument document = new XWPFDocument();

    // 3. 添加标题
    XWPFParagraph title = document.createParagraph();
    title.setAlignment(ParagraphAlignment.CENTER);
    XWPFRun titleRun = title.createRun();
    titleRun.setText(request.getNotificationTitle());
    titleRun.setFontSize(22);
    titleRun.setBold(true);

    // 4. 添加正文
    // 4.1 月度标兵
    XWPFParagraph p1 = document.createParagraph();
    p1.createRun().setText("一、月度卫生标兵（获得15次以上优秀）");

    // 4.2 循环添加班级信息
    for (ClassFrequencyDetailVO cls : stats.getTopClasses()) {
        XWPFParagraph p = document.createParagraph();
        p.createRun().setText(String.format(
            "    %d. %s（%s）          获得%d次优秀，占比%.1f%%",
            cls.getRanking(), cls.getClassName(),
            cls.getDepartmentName(), cls.getFrequency(), cls.getRate()
        ));
    }

    // 5. 保存文件
    String fileName = generateFileName(request);
    FileOutputStream out = new FileOutputStream(fileName);
    document.write(out);
    out.close();

    return fileName;
}
```

**PDF证书生成（iText）**：
```java
public String generateCertificate(CertificateGenerateRequest request) {
    Document document = new Document(PageSize.A4.rotate());
    String fileName = generateCertificateFileName(request);
    PdfWriter.getInstance(document, new FileOutputStream(fileName));

    document.open();

    // 添加边框
    PdfContentByte canvas = writer.getDirectContent();
    canvas.setColorStroke(BaseColor.GOLD);
    canvas.setLineWidth(3);
    canvas.rectangle(30, 30, PageSize.A4.getHeight() - 60,
                     PageSize.A4.getWidth() - 60);
    canvas.stroke();

    // 添加标题"荣誉证书"
    Paragraph title = new Paragraph("荣 誉 证 书",
                                    new Font(baseFont, 36, Font.BOLD));
    title.setAlignment(Element.ALIGN_CENTER);
    document.add(title);

    // 添加班级名称
    Paragraph className = new Paragraph(
        request.getClassName() + "：",
        new Font(baseFont, 24)
    );
    document.add(className);

    // 添加表彰内容
    Paragraph content = new Paragraph(
        request.getHonorContent(),
        new Font(baseFont, 18)
    );
    document.add(content);

    // 添加落款
    Paragraph issuer = new Paragraph(
        request.getIssuerName() + "\n" +
        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")),
        new Font(baseFont, 16)
    );
    issuer.setAlignment(Element.ALIGN_RIGHT);
    document.add(issuer);

    document.close();
    return fileName;
}
```

---

## 🎨 UI设计规范

### 配色方案

```css
:root {
  /* 主色 */
  --primary: #1a56db;
  --primary-light: #3f83f8;

  /* 等级色 */
  --level-excellent: #10b981;
  --level-good: #3b82f6;
  --level-average: #f59e0b;
  --level-poor: #ef4444;

  /* 徽章色 */
  --badge-gold: #fbbf24;
  --badge-silver: #c0c0c0;
  --badge-bronze: #cd7f32;

  /* 中性色 */
  --gray-50: #f9fafb;
  --gray-100: #f3f4f6;
  --gray-600: #4b5563;
  --gray-900: #111827;
}
```

### 卡片样式

```css
.card {
  background: var(--gray-50);
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.card:hover {
  transform: translateY(-4px);
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
}

.card-gap {
  gap: 24px;
}
```

### 动画效果

```css
/* 数字滚动动画 */
@keyframes countUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 荣誉榜渐入动画 */
.honor-item {
  animation: slideInUp 0.5s ease forwards;
}

.honor-item:nth-child(1) { animation-delay: 0.1s; }
.honor-item:nth-child(2) { animation-delay: 0.2s; }
.honor-item:nth-child(3) { animation-delay: 0.3s; }
```

---

## 📝 API文档示例

### 获取评级频次统计

**接口**：`GET /api/rating-statistics/frequency`

**请求参数**：
```json
{
  "checkPlanId": 1,
  "ruleId": 10,
  "levelIds": [1, 2],
  "periodStart": "2025-12-01",
  "periodEnd": "2025-12-31",
  "periodType": "MONTH"
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "checkPlanId": 1,
    "planName": "2025学年第一学期检查计划",
    "ruleId": 10,
    "ruleName": "每日卫生检查评级",
    "periodStart": "2025-12-01",
    "periodEnd": "2025-12-31",
    "periodLabel": "2025年12月",
    "periodType": "MONTH",
    "totalRatings": 156,
    "awardedClasses": 28,
    "totalClasses": 30,
    "statisticsDays": 20,
    "levelStatistics": [
      {
        "levelId": 1,
        "levelName": "卫生优秀班级",
        "levelColor": "#10b981",
        "levelOrder": 1,
        "totalFrequency": 45,
        "percentage": 28.8,
        "classCount": 28,
        "topClasses": [
          {
            "classId": 101,
            "className": "计算机1班",
            "departmentName": "经信系",
            "frequency": 15,
            "rate": 75.0,
            "ranking": 1
          }
        ]
      }
    ],
    "departmentComparison": [
      {
        "departmentId": 1,
        "departmentName": "经信系",
        "classCount": 15,
        "excellentCount": 12,
        "excellentRate": 80.0,
        "ranking": 1
      }
    ],
    "calculatedAt": "2025-12-22 14:30:00"
  }
}
```

---

## ✅ 验收标准

### 功能验收

1. ✅ 评级次数正确统计（按等级、按班级、按院系）
2. ✅ 连续记录准确计算（连续获得次数、最佳记录）
3. ✅ 徽章自动授予正确触发
4. ✅ 通报文档格式正确、内容完整
5. ✅ 证书批量生成成功、样式美观

### 性能验收

1. ✅ 评级统计查询响应时间 < 500ms
2. ✅ 通报生成时间 < 3秒（30个班级）
3. ✅ 证书批量生成时间 < 5秒（30个证书）
4. ✅ 前端页面首屏渲染 < 1秒

### UI验收

1. ✅ 所有配置界面使用卡片式设计
2. ✅ 选择框数量减少50%以上
3. ✅ 动画效果流畅自然
4. ✅ 移动端自适应良好

---

## 🔧 开发环境准备

### 后端依赖（pom.xml）

```xml
<!-- Apache POI - Word文档生成 -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.3</version>
</dependency>

<!-- iText - PDF生成 -->
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>itextpdf</artifactId>
    <version>5.5.13.3</version>
</dependency>

<!-- iText中文字体支持 -->
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>itext-asian</artifactId>
    <version>5.2.0</version>
</dependency>
```

### 前端依赖（package.json）

```json
{
  "dependencies": {
    "echarts": "^5.4.3",
    "vue-echarts": "^6.6.0",
    "@vueuse/core": "^10.6.1"
  }
}
```

---

## 📌 注意事项

### 1. 数据迁移

- 执行迁移脚本前请**备份数据库**
- 迁移脚本已做兼容处理，支持多次执行
- 验证数据完整性后再删除旧数据

### 2. 性能优化

- 评级统计使用Redis缓存，缓存时间5分钟
- 通报生成使用异步任务，避免阻塞
- 大量数据查询使用分页

### 3. 安全考虑

- 通报文件生成后存储在私有目录
- 文件访问需要权限验证
- 定期清理过期文件

### 4. 兼容性

- 保持现有评级功能完全兼容
- 提供新旧UI切换开关（过渡期）
- API版本管理（v1保留，新增v2）

---

## 🎯 后续优化方向

1. **AI智能分析**：基于历史数据预测班级评级趋势
2. **微信推送**：评级结果自动推送到班主任微信
3. **数据大屏**：实时展示全校评级数据
4. **家长端**：家长可查看班级荣誉
5. **评级申诉**：班级可对评级结果提出异议

---

## 📞 联系方式

**项目负责人**：Claude Code
**技术支持**：开发团队
**更新时间**：2025-12-22
