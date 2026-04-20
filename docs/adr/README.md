# Architecture Decision Records

> 短小的历史决策档案. 每篇讲**当时**为什么这么选, 便于后人看到代码形态时不困惑.
> 格式参考 Michael Nygard 的 ADR 模板.

## 已录

| # | Title | Status | Date |
|---|---|---|---|
| [001](001-phase2-unified-spi.md) | Phase 2: 收敛 8 SPI 为 PluginPackage + Contribution sealed | Accepted | 2026-04-19 |
| [002](002-phase35-ddd-restructure.md) | Phase 3.5: 把教育领域从 domain/ 迁到 plugins/education/ | Accepted | 2026-04-19 |
| [003](003-phase4a-dynamic-routes.md) | Phase 4A: 前端动态路由注册替代 Module Federation | Accepted | 2026-04-19 |

## 下一个要写 ADR 的时机

- 任何 breaking 变更 (接口删改/表结构/部署模型)
- 在 2 条以上合理路径里做取舍
- 回头看会让人问"为什么这么写"的代码形态

写 ADR 的门槛应该低, 但**千万不要写浑水摸鱼的**. 一篇模糊的 ADR 比没 ADR 更有害.
