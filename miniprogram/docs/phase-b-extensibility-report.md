# Phase B Extensibility Report — Healthcare 桩插件验证

> 日期: 2026-05-05
> 分支: `feature/miniprogram-phase-b`
> 状态: 4 个验证点全部通过

## 背景

Phase A 交付了完整的插件平台 SPI(对偶后端 sealed Contribution + ContributionDispatcher + EventBus + PlatformCapability + 4 条 ESLint 边界守护)。但 Phase A 只有 demo 一个插件,无法证明扩展点在第二个独立插件上不漏。

Phase B 通过 **healthcare** 桩插件验证 4 个扩展性维度。**不写真实业务**,只跑契约。

---

## V1: 新插件零侵入

**指标**: 加 healthcare 不改 `core/` 任何文件。

```bash
$ git diff master..HEAD --stat -- 'miniprogram/src/core/**'
(无输出)
```

**结论**: ✅ 通过。core 0 改动。

---

## V2: 分包生效

**指标**: healthcare 进 `pages.json` `subPackages`,主包 `pages` 不含其代码。

```json
{
  "pages": [4 项 — 都是 core/pages/* 主包页面],
  "subPackages": [
    { "root": "plugins/demo", "pages": ["pages/hello"] },
    { "root": "plugins/healthcare", "pages": ["pages/index"] }
  ]
}
```

主包页面数: **4**(`core/pages/index/index`, `login/index`, `message/index`, `mine/index`)
分包数: **2**(demo + healthcare)
healthcare 源代码完全在 `src/plugins/healthcare/`。

**结论**: ✅ 通过。健康域插件代码不进主包。

---

## V3: 运行期 enabled() gate

**指标**: 当 `tenantPlugins` 不含 `healthcare` 时,该插件不激活、菜单/路由不渲染。

测试: `src/__integration__/enabled-gate.test.ts` 3 个 case 全绿:

| Scenario | tenantPlugins | 期望 active | 实际 |
|---|---|---|---|
| 空租户 | `[]` | 不激活 | 0 个 ✅ |
| 含 healthcare | `['healthcare']` | 激活 | `['healthcare']` ✅ |
| 含其他但不含 healthcare | `['demo']` | 不激活 | 0 个 ✅ |

healthcare manifest 的 `enabled: (ctx) => ctx.tenantPlugins.includes('healthcare')` 是真实生效的运行期门。多租户/多场景部署可以靠后端 `enabledPlugins` 字段动态控制各插件可见性。

**结论**: ✅ 通过。

---

## V4: 跨插件事件(EventBus)

**指标**: 一个插件订阅另一个插件发布的事件,不通过 import 而通过 EventBus,符合 ESLint `no-cross-plugin-import` 守护。

测试: `src/__integration__/cross-plugin-event.test.ts` 2 个 case 全绿。

**关键交互**:
1. demo 插件在 `bootstrap(ctx)` 钩子里订阅 `healthcare.patient.scanned`
2. healthcare 插件 emit 该事件(测试用 bus.emit 模拟扫码触发)
3. demo 收到 payload 并写入测试可观察的 `_crossPluginAuditLog`

**关键证据**:
- demo `manifest.ts` 没有 `import healthcare/...` 任何代码 — 由 `mp-boundary/no-cross-plugin-import` ESLint 规则强制
- 通信只走 `ctx.bus`(共享 EventBus 单例)
- 事件名 `healthcare.patient.scanned` 由 healthcare 在 `EventContribution` 声明,demo 用字符串订阅
- 事件总线是 namespace-routing(按事件名分发),不是 lifecycle-coupled —— 第二个 case 验证了即便 healthcare 未激活,bus.emit 仍按字符串路由

**结论**: ✅ 通过。

---

## 体积数据

`git diff master..HEAD --stat -- 'miniprogram/src/plugins/**'`(plugins 总变更):

```
miniprogram/src/plugins/demo/manifest.ts           | 27 ++++++-------
miniprogram/src/plugins/healthcare/manifest.test.ts | 29 +++++++++++++
miniprogram/src/plugins/healthcare/manifest.ts     | 47 ++++++++++++++++++++++
miniprogram/src/plugins/healthcare/pages/index.vue | 41 +++++++++++++++++++
miniprogram/src/plugins/index.ts                   |  3 +-
miniprogram/src/plugins/manifests.json             | 13 ++++++
```

healthcare 总代码量:**约 117 行**(manifest 47 + page 41 + test 29)。
demo manifest 变更:加 `bootstrap` 钩子 + 测试探针,净增 12 行。

**核心代码 0 改动**;扩展插件代价仅一个 manifest + 页面。

---

## 测试统计

- Phase A 完成时: **51 测试**
- Phase B 完成时: **59 测试**(+8)
  - `healthcare/manifest.test.ts`:3 个(注册不冲突 / scan prefix 唯一 / 5 个 Contribution)
  - `__integration__/enabled-gate.test.ts`:3 个(V3)
  - `__integration__/cross-plugin-event.test.ts`:2 个(V4)
- `npm run ci` 全绿(lint + type-check + verify-manifest + test)

---

## 综合结论

Phase A 的 SPI 在第二个独立插件 healthcare 上**无任何漏洞**。4 个扩展性维度全部验证:零侵入 / 分包 / enabled gate / 跨插件事件。

可以开 Phase C(inspection 真实业务接入)。

---

## 经验回填(写给 Phase C 与未来插件作者)

1. **集成测试位置**: 跨插件交互测试放 `src/__integration__/`,不被 `mp-boundary/no-cross-plugin-import` 误伤(规则只激活在 `src/plugins/<key>/` 路径下)。集成目录 import `@plugins/A` + `@plugins/B` 完全合法。

2. **测试探针约定**: 测试-only 的可观察 state 用 `_` 前缀(如 `_crossPluginAuditLog`),与插件公开 API 区分。生产代码不应该消费下划线前缀符号。

3. **enabled(ctx) 必须是纯 predicate**: 不能有 side effect。`activatePlugins` 在登录或重新认证时可能多次调用,有副作用会重复触发。如果插件需要"激活时跑一次",写在 `bootstrap` 而不是 `enabled`。

4. **JSON 镜像的边界**: `manifests.json` 给 Node 构建脚本用,不能携带函数(scan-resolver 的 `resolve`、bootstrap 钩子、enabled 谓词)。这是必要的 — TS manifest 才是真源,JSON 只是 verify-manifest 的扁平视图。Phase C 加新插件务必双写。

5. **EventBus 是 namespace-routing**: bus.emit 按事件名字符串分发,不关心插件激活状态。订阅者(bootstrap)注册一次即终身有效。如果需要"healthcare 未激活时也阻止 demo 收事件",用 enabled gate 控制订阅本身,而不是依赖 bus 的生命周期。

6. **scan-resolver 的 resolve 函数无法在 verify-manifest 校验**: JSON 镜像里只能放 `prefix + priority`,`resolve` 函数走单测。如果未来新增需要校验 resolver 行为的契约,得在 manifest.test.ts 里覆盖。

7. **跨插件事件需要事先约定事件名**: 没有强制的"事件类型导入"机制(避免破坏插件边界)。最佳实践是发布插件在 `EventContribution` 里声明 `eventName` + `payloadSchema`,文档里记录,订阅插件按字符串订阅。可考虑 Phase C 加 `dispatcher.queryEventNames()` 帮助调试。
