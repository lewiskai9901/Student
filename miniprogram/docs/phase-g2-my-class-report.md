# Phase G-2 Report — 我的班级(班主任视角)

> 日期: 2026-05-07
> 分支: `feature/miniprogram-phase-g2`
> 状态: ✅ 完成

## 一句话总结

education 插件深化第一步:接通"我的班级"工作台,班主任能看本班概览(人数/排名/平均分趋势)+ 学生名册 + 宿舍分布。**顺手修了后端 MyClassController 3 处 path variable 名字 bug**(Spring 运行时会失败,从未被真用过)。

## 范围

✅ **完成:**
- **后端 bug 修复** — `MyClassController` 3 个 `@GetMapping("/classes/{classId}/...")` 路径变量重命名为 `{orgUnitId}`,与 `@PathVariable Long orgUnitId` 参数名对齐(service signature 用 orgUnitId)
- 4 read API:`myClassApi.list/overview/students/dormitoryDistribution`
- 新页 `my-class.vue`:
  - 单/多班级 picker(一个班自动选,多班 picker 切换)
  - Overview 卡:总人数 / 男女 / 平均分 / 排名 / 趋势(▲▼)
  - 学生 tab:列表 + 客户端搜索(姓名/学号)
  - 宿舍 tab:按楼栋分组 + 房间 + 床位
- manifest 加 1 perm + 1 menu + 1 route

❌ **不在本期(留后续):**
- 课表(TeachingScheduleController)— 周视图 UI 复杂留下次
- 出勤/成绩/学籍预警(写场景多)
- buildingType 国际化(MALE/FEMALE/MIXED 直显)

## Commit

| SHA | 说明 |
|---|---|
| `c143c144` | Phase G-2: 我的班级页 + 班级 overview/学生/宿舍 + 修复 MyClassController @PathVariable bug |

## 后端 Bug 详情

`MyClassController.java` 原代码:
```java
@GetMapping("/classes/{classId}/overview")
public Result<...> getClassOverview(@PathVariable Long orgUnitId, ...) {
    ...service.getClassOverview(orgUnitId, ...);
}
```

Spring 运行时会抛 `MissingPathVariableException: orgUnitId` — 因为 path 声明 `{classId}`,但参数名是 `orgUnitId`,Spring 找不到对应。

修法:把 path 里 `{classId}` 改成 `{orgUnitId}`(选 path 重命名而不是参数重命名,因为 service signature 是 `orgUnitId`,统一对齐)。

3 个端点都同样修复:
- `GET /my-class/classes/{orgUnitId}/overview`
- `GET /my-class/classes/{orgUnitId}/user_student`
- `GET /my-class/classes/{orgUnitId}/dormitory-distribution`

`mvn compile -DskipTests` 通过。无新单测(原本就没有 controller 单测)。

## 后端契约对照表

| 端点 | 用途 | 关键字段 |
|---|---|---|
| `GET /my-class/classes` | 我管理的班级列表 | id, classCode, className, currentSize, weeklyRank, weeklyScore, scoreTrend[] |
| `GET /my-class/classes/{orgUnitId}/overview` | 班级概览 | studentCount, male/femaleCount, classRank, averageScore, scoreTrend, scoreTrendList[] |
| `GET /my-class/classes/{orgUnitId}/user_student?keyword=&status=` | 班级学生 | id, studentNo, name, gender, dormitoryName, bedNo |
| `GET /my-class/classes/{orgUnitId}/dormitory-distribution` | 班级宿舍分布 | buildingId, buildingName, buildingType, rooms[] {dormitoryId, roomNo, **user_student**[]} |

注意:`DormitoryRoomDTO.user_student`(后端 Java 字段名也是 snake_case,Jackson 默认按字段名序列化)— 前端 TS 接口对应 `user_student` 字段名。

## 测试基线

- 起点:31 文件 / 142 测试
- 终点:**32 文件 / 146 测试** 全绿(+1 文件 +4 测试)
- type-check + lint 0 errors

## Manifest 变化

education 现在:**5 perms + 3 menus + 5 routes**

新菜单 `education.my-class` order=39,排在"学生"(40)前面 — 班主任最常用的入口最靠前。

## 已知限制 / 真机验证

- [ ] 后端启动 + 班主任账号登录
- [ ] 首页 home-grid 看到"我的班级"菜单(EDU 启用 + 角色有 my-class 权限时)
- [ ] 进入页:有 1 个班级 → 自动选;多班 → picker 切换
- [ ] Overview 数字准确(对比 web 端)
- [ ] 学生 tab → 搜索"张" → 客户端过滤
- [ ] 宿舍 tab → 按楼栋分组 + 床位
- [ ] 非班主任登录 → 显示"您当前不是任何班级的负责人"

## 下一步候选(education 深化)

- **课表查看**(teaching schedule)— 老师 / 学生看自己周课表,周 grid UI ~1 天
- **校历**(academic calendar + semesters)— 全员通用 ~0.5 天
- **出勤签到**(attendance)— 老师写场景,需要逐学生选状态 ~1 天
- **成绩查询**(grade)— 学生看自己 / 老师看本班 ~0.5 天
- **教师档案**(teacher profile)— stub 通讯录扩展 ~0.5 天

或者其他方向(真机验证 / D-3c 订阅消息 / Phase F 监控)。

---

**P.S.** 这次 implementer 多写了 1 个 BizError 测试(没在 spec 里),保持与 student.test.ts 一致 — 是好的对齐。后端 bug 修复直接做了,避免之后真机时被 trap。
