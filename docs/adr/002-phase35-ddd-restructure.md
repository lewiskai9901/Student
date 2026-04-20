# ADR 002: Phase 3.5 — 教育领域从 domain/ 迁至 plugins/education/

- **Status**: Accepted
- **Date**: 2026-04-19
- **Commits**: `bd6d0552` `4772b8a5` `c31afb68` `b0e6d4a2` (补丁)

## Context

Phase 2 之前, 项目声称"通用核心 + 行业垂直扩展", 但**物理结构骗人**:

```
com.school.management.domain/
├── access/         ← 真通用
├── organization/   ← 真通用
├── place/          ← 真通用
├── shared/         ← 真通用
├── student/        ← ❌ 教育特定, 不应在 domain/
├── academic/       ← ❌ 教育特定
├── teaching/       ← ❌ 教育特定
└── calendar/       ← ❌ 教育特定
```

`application/` 和 `interfaces/rest/` 同样混. 结果:
- 新人看 `domain/student` 以为是平台核心
- 要做**纯医院版 / 纯养老版**部署时, 挑不出"要带的教育代码"
- ArchUnit 规则"业务代码不得依赖行业插件" 形同虚设 (行业代码本来就在"业务"目录下)

## Decision

把 `domain/{student,academic,teaching,calendar}` 及配套 `application/` `infrastructure/persistence/` `interfaces/rest/` 的同名子包, 物理迁到:

```
infrastructure/extension/plugins/education/
├── domain/ {student,academic,teaching,calendar}
├── application/ {...}
├── infrastructure/persistence/ {...}
└── interfaces/rest/ {...}
```

### 关键执行细节
- **4 commits 分领域迁** (calendar → teaching → academic → student), 每次独立回滚
- 用 **`git mv` + Python literal-replace 脚本** (`scripts/phase35-rewrite.py`), 不用 sed regex (避免误伤注释/字符串)
- 同时迁 `application/myclass/` + `application/events/StudentEventHandler.java` (语义归属插件)
- 补 `StudentManagementApplication @MapperScan` 一条通配 path 覆盖新位置

### ArchUnit 守护
- 新规则 `migrated_education_domains_must_not_return_to_top_level_domain`: 禁止未来任何人往 `com.school.management.domain.{student,academic,teaching,calendar}` 下再建类
- DddLayerTest 的通配 `..domain..` / `..application..` / `..interfaces..` 收紧为精确前缀 `com.school.management.{domain,application,interfaces}..`, 防止插件内部分层被误判为业务层

## Considered Alternatives

### A. 只改 ArchUnit 规则, 不动代码
否决 — 物理位置不对, 外观就不对. ArchUnit 规则需要一堆 exception 白名单, 不可持续

### B. 一次性全迁, 单 commit
否决 — 260 文件一口气动, review 和回滚都吃力. 分 3 commit 按领域走

### C. 把 inspection 也迁到 plugin
暂缓 — inspection 172 文件是 V7 检查平台, 是否"教育特定"存在争议 (医疗机构自查/养老院自查同样用)视为**通用能力**, 留在 domain/inspection/. 如未来真要拆 OEM 版, 再启动

### D. 用 IDE Refactor Move Package
否决 — 这是 Claude Code (CLI) 环境, 没 IDE. 用 `git mv + Edit + Python literal-replace` 三件套, 效果等价, 可 git log --follow 追溯

## Consequences

### 正向
- `domain/` 下已无 student/academic/teaching/calendar, "通用"名副其实
- Phase 4 / 未来 "纯医院版" 部署时可以精确挑 `plugins/education/` 目录走或不走
- 新增行业 (HEALTH) 有清晰模板: 建 `plugins/healthcare/` 同构 4 层
- ArchUnit 规则锁死, 新人不可能再把教育代码写到 domain/

### 负向
- 包路径深 (`com.school.management.infrastructure.extension.plugins.education.domain.student.model.aggregate.Student`), import 行变长
- `@MapperScan` 配置多了一条通配 path
- 历史 git log 通过 rename 追溯, 不是 inline edit — IDE 定位稍慢

### 事故档案: commit c31afb68 漏提 128 个文件
`git mv` 把 rename 先暂存, 后续 Python literal-replace 改了内容, 但我第一轮 `git add` 只 add 了 `scripts/` + test 文件. 导致 128 个文件新路径存在但**内容是旧 package declaration**. 运行中没暴露 (Maven 用磁盘), 但 fresh checkout mvn compile 会 fail.
修复: `b0e6d4a2` 补提全部 literal rewrite. **教训**: git mv + 内容改写要连续两次 `git add .`
