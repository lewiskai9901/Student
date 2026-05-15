# OpenAPI → 前端 SDK 自动同步

后端接口 → 前端 SDK 的 source-of-truth 流水线。一处声明,多端共享。

## 总览

```
                    ┌──────────────────────┐
                    │  Spring Boot 后端     │
                    │  (controllers + DTO) │
                    └──────────┬───────────┘
                               │ springdoc + LongAsStringModelConverter
                               ▼
                  GET /api/v3/api-docs (运行时)
                               │
                               │ bash backend/scripts/export-openapi.sh
                               ▼
                    backend/openapi.json (入 git)
                               │
              ┌────────────────┼────────────────┐
              ▼                                 ▼
   npm run openapi:generate         npm run openapi:generate
   (frontend)                       (miniprogram)
              │                                 │
              ▼                                 ▼
   frontend/src/api-generated/      miniprogram/src/api-generated/
   ├── client.gen.ts                └── types.gen.ts (types only)
   ├── sdk.gen.ts (1025 method)
   ├── types.gen.ts (577 types)
   └── (复用 request.ts axios 实例)
```

## 4 个核心组件

### 1. 后端 ModelConverter (LongAsStringModelConverter.java)

springdoc 默认把 Java `Long` 字段标为 `{"type":"integer","format":"int64"}`,但 Jackson 全局策略
(`config/JacksonConfig.java:37`) 实际把 Long 序列化为 JSON `string`。**Schema 撒谎**会让下游 SDK 类型全错。

Converter 在 schema 生成阶段递归扫,把 Long 字段改为
`{"type":"string","format":"int64","x-typescript-type":"LongId"}`。897 个 Long 字段自动覆盖。

### 2. spec 入 git (backend/openapi.json)

```bash
# 后端改完接口后:
bash backend/scripts/export-openapi.sh
git add backend/openapi.json && git commit -m "..."

# CI 跑这条防 drift:
bash backend/scripts/check-openapi-drift.sh   # 退出码 1 = drift
```

spec 排序 key 写入,保证 deterministic diff。

### 3. frontend codegen (@hey-api/openapi-ts)

`frontend/openapi-ts.config.ts`:
- input: `../backend/openapi.json`
- output: `src/api-generated/` (4 文件,约 1.6MB)
- plugins: axios client (复用 `request.ts` 实例) + typescript + sdk

post-process `scripts/post-process-openapi-sdk.mjs` 把 `string` 字段还原成 `LongId`
(按 `x-typescript-type: LongId` 标记定位)。

```bash
cd frontend && npm run openapi:generate
```

### 4. miniprogram codegen (types only)

`miniprogram/openapi-ts.config.ts`: 只生成 TS 类型,不生成 SDK client
(小程序用 `uni.request`,不接 axios)。手写 wrapper 引用生成类型。

```bash
cd miniprogram && npm run openapi:generate
```

## 使用 generated SDK (frontend)

```typescript
// 旧 (手写,即将 deprecate):
import { getProject } from '@/api/inspection/project'
const p = await getProject(projectId)

// 新 (generated):
import { getProject1 } from '@/api-generated/sdk.gen'
const { data: p } = await getProject1({ path: { id: projectId } })
```

`getProject1` 的 `1` 后缀是 hey-api 处理同名 controller method 时加的(多个 GetById 同存)。

## 何时跑 codegen

每次后端改接口签名:
1. 改 controller / DTO
2. 重启 backend (or `mvn spring-boot:run`)
3. 跑 `bash backend/scripts/export-openapi.sh` 更新 `backend/openapi.json`
4. 跑 `npm run openapi:generate` (frontend) 重生 SDK
5. 跑 `npm run openapi:generate` (miniprogram) 重生 types
6. 4 文件一起 commit

## CI 守护

`.github/workflows/ci.yml` (规划) 应加 step:

```yaml
- name: Check OpenAPI spec drift
  run: |
    # 起后端
    cd backend && mvn spring-boot:run &
    # 等就绪
    timeout 120 bash -c 'until curl -sf http://localhost:8080/api/actuator/health; do sleep 2; done'
    # 比对
    bash backend/scripts/check-openapi-drift.sh

- name: Check generated SDK drift
  run: |
    cd frontend && npm run openapi:generate
    git diff --exit-code src/api-generated/  # 如果生成结果与 commit 不同, fail
```

## 关键决策

- **不在 DTO 上加 `@Schema` 注解** — 897 字段手写工作量大、易遗漏。统一 ModelConverter 处理。
- **不试图改后端 Long → number** — JS 53-bit 精度问题不可绕过,雪花 ID 必须 string。
- **不让 codegen 输出 number 类型** — 那是和 Jackson 行为不一致的撒谎。坚持 LongId(=string)。
- **不强制立即迁移所有 api/*.ts** — 渐进 deprecate 即可,LongId 类型可直接互换。
- **post-process 优于 codegen plugin** — hey-api 不读 vendor extension,自己写 50 行 Node 比 fork plugin 简单可靠。

## 故障排查

| 现象 | 原因 | 修复 |
|------|------|------|
| `npm run openapi:generate` 失败 — spec 不可达 | backend 未起 / `backend/openapi.json` 路径错 | 起后端,或直接读 git 中的 spec |
| 生成 SDK 不含 LongId | post-process 没跑 | `npm run openapi:generate` 已含 post-process,单跑 openapi-ts 不带 |
| `Cannot find name 'LongId'` in types.gen.ts | post-process 失败或被 clean 重置 | 重跑 `npm run openapi:generate` |
| CI drift fail | backend 改了接口但 `openapi.json` 没更新 | 本地跑 `bash backend/scripts/export-openapi.sh` + commit |

## 参考

- 后端: `backend/src/main/java/com/school/management/config/openapi/LongAsStringModelConverter.java`
- 前端: `frontend/openapi-ts.config.ts` / `frontend/scripts/post-process-openapi-sdk.mjs`
- 小程序: `miniprogram/openapi-ts.config.ts` / `miniprogram/scripts/post-process-openapi-sdk.mjs`
- 守护: `backend/scripts/export-openapi.sh` / `check-openapi-drift.sh`
- Jackson Long-as-string 契约: `memory/project_jackson_long_as_string.md`
- LongId 类型迁移: `memory/project_longid_type_migration.md`
