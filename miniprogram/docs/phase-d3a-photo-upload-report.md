# Phase D-3a Report — 拍照水印 + 文件上传 + 整改附件

> 日期: 2026-05-07
> 分支: `feature/miniprogram-phase-d3a`
> 状态: ✅ 完成

## 一句话总结

inspection 整改提交从纯文本升级为"文本 + 附件"。打通了 **拍照 → 加水印 → 上传 → addEvidence → submitCorrection** 完整链路;后端补一个通用文件上传端点;前端 capability 层新增 `watermarkImage` 端口 + `uploadWrapped` 助手。

## 范围

✅ **完成:**
- 后端:`POST /files/upload` 通用上传端点(directory 白名单 `inspection`/`attachments`,默认 `inspection`)
- 前端 capability:`watermarkImage(file, opts)` 端口(uni canvas timestamp 水印)
- 前端 core/api:`uploadWrapped<T>` 助手(自动注入 Authorization + 解 Result 信封)
- 前端 capability.uploadFile 重构返回原始 body(替代之前硬编码 `{url, key}`)
- 前端 inspectionApi:`addEvidence` 新增 + `submitCorrection` 接受 `evidenceIds`
- 前端页面 correction-detail.vue:附件区(scroll-view 缩略图 + "+" 按钮)+ 完整 5 步上传流水线 + 隐藏 canvas + 提交携带 evidenceIds
- 集成测试:3 项契约测试(链路串联 / takePhoto 取消 / upload 失败)

❌ **不在本期(留 D-3b/D-3c):**
- 评分组件(submission-detail 页 + ScoringPolicy 渲染)— D-3b
- 申诉提交入口 + 页面 — D-3b
- 微信订阅消息 — D-3c
- 视频证据 / 文档附件 — 仅做 PHOTO

## 6 commit 序列

| SHA | 说明 |
|---|---|
| `16c4ca7b` | T1: 后端 FileUploadController + 3 单测 |
| `84dcb265` | T2: capability.watermarkImage(uni canvas)+ 3 单测 |
| `e1f44c4c` | T3: inspectionApi.addEvidence + submitCorrection evidenceIds + 2 单测 |
| `66b77909` | T4a: capability.uploadFile 重构 + uploadWrapped 助手 + 6 单测 |
| `79f28739` | T4b: correction-detail 拍照流水线 + 隐藏 canvas + 提交带 evidenceIds |
| `f7e5a3f5` | T5: 集成测试 — 拍照上传 evidence 链路 |

## 后端契约(D-3a 新增)

| Method+Path | 入参 | 返回 | 权限 |
|---|---|---|---|
| POST `/files/upload` (multipart) | `file: MultipartFile`, `directory: String?` (default `inspection`) | `Result<{ fileName, fileUrl, size }>` | `@PreAuthorize("isAuthenticated()")` |

directory 仅允许 `{inspection, attachments}` — 路径穿越攻击早期 fail。

## 前端 API 表面(D-3a 新增)

```ts
// core/platform/capability.ts
interface WatermarkOpts { canvasId, text, position?, fontSize?, color?, shadowColor? }
capability.watermarkImage(file, opts): Promise<LocalFile>

// core/api/upload.ts (新建)
uploadWrapped<T>(file, { url, formData?, fieldName? }): Promise<T>
// 自动加 Bearer token, 解 Result 信封, 失败抛 BizError

// plugins/inspection/api/inspection.ts
inspectionApi.addEvidence(submissionId, { detailId?, evidenceType, fileName, fileUrl }): Promise<InspEvidence>
inspectionApi.submitCorrection(id, note, evidenceIds?): Promise<CorrectiveCase>
```

## 测试基线

- 起点:23 文件 / 104 测试
- 终点:**25 文件 / 118 测试,0 失败**
- 增量:
  - T1 后端 +3 单测(JUnit)
  - T2 +3(canvas validation + happy path)
  - T3 +2(addEvidence / submitCorrection evidenceIds)
  - T4a +6(1 in capability + 5 in upload.test.ts)
  - T4b +0(页面集成,无 render 测试)
  - T5 +3(集成测试)

后端 mvn test FileUploadControllerTest 3/3 通过,mvn compile 干净。
前端 type-check / lint 0 errors;`npm run ci` 全绿。

## 文件清单

### 后端 (2 文件,新建)
- `backend/.../interfaces/rest/file/FileUploadController.java`
- `backend/.../test/.../FileUploadControllerTest.java`

### 前端核心层 (3 修改 + 1 新建)
- `miniprogram/src/core/platform/capability.ts` — 加 WatermarkOpts + watermarkImage;删 RemoteFile;改 uploadFile 返回类型
- `miniprogram/src/core/platform/weixin.ts` — 实现 watermarkImage;uploadFile 返回原始 body
- `miniprogram/src/core/platform/capability.test.ts` — +4 测试(3 watermark + 1 uploadFile body)
- `miniprogram/src/core/api/upload.ts` — 新建 uploadWrapped + 5 测试

### 前端业务层 (3 修改)
- `miniprogram/src/plugins/inspection/api/inspection.ts` — addEvidence + submitCorrection 签名扩展
- `miniprogram/src/plugins/inspection/api/types.ts` — 新增 EvidenceType + InspEvidence;补 CorrectiveCase.detailId? 字段
- `miniprogram/src/plugins/inspection/api/inspection.write.test.ts` — +2 测试

### 前端页面 (1 修改)
- `miniprogram/src/plugins/inspection/pages/correction-detail.vue` — +159 行(附件 UI + 5 步流水线 + 隐藏 canvas)

### 前端集成测试 (1 新建)
- `miniprogram/src/__integration__/inspection-attachment.test.ts` — 3 测试

## 设计决策记录

### 为什么 capability.uploadFile 重构返回 unknown

Phase A 的 `uploadFile` 返回 `{url, key}` 是凭空假设的响应形状。我们实际的后端返回 Result 信封 `{code:200, data:{fileName, fileUrl, size}}`。三种处理:
- (A) 修 capability 把 Result 解开 → 把 capability 绑死到我们的后端格式
- (B) 让 plugin 直接调 `uni.uploadFile` → 违反 mp-boundary 规则
- (C) capability 返回原始 body,新加 `core/api/uploadWrapped` 做信封解包 + auth ← **采用**

(C) 让 capability 保持纯协议层(只关心 HTTP),业务层处理信封,清楚分层。

### 为什么 watermarkImage 需要 canvasId 参数

uni 小程序的 canvas 必须在页面 DOM 里(不能纯运行时建)。所以 watermarkImage 接受 `canvasId`,调用方提供页面级 `<canvas canvas-id="xxx">`。我们在 correction-detail.vue 里放了一个 `position:fixed; left:-9999rpx` 的隐藏 canvas。

### 为什么 addEvidence 失败时不回滚已上传的 OSS 文件

OSS 上多孤立文件 vs 用户体验复杂度的取舍:孤立文件占空间但不影响业务;给用户一个"上传成功但登记失败"的复杂 UI 收益小。后端可以加定期清理孤立 evidence 的 cron,这不是小程序端的事。

### evidence_type 默认 'PHOTO'

D-3a 只支持拍照流;后端 `EvidenceType` 枚举有 PHOTO/VIDEO/DOCUMENT,小程序端目前硬编码 'PHOTO'。视频/文档证据 D-3b/c 再扩。

## 已知限制 / 真机验证清单

代码层 type-check + lint + 单测全绿,但以下需真机:

- [ ] 后端启动 + 用户登录小程序
- [ ] 整改单 IN_PROGRESS 状态下进入 correction-detail
- [ ] 看见"附件 (可选,拍照自动加水印)"区 + 一个 "+" 按钮
- [ ] 点 "+" → 摄像头打开 → 拍 1 张 → 看到缩略图入列表
- [ ] 验证水印位置(右下角)、内容(YYYY-MM-DD HH:mm 用户名)
- [ ] 多拍 3 张 → 列表横向 scroll
- [ ] 点缩略图右上角 ✕ → 删除入列表中的项
- [ ] 整改说明 ≥5 字 + 至少 1 张图 → 点"提交整改"
- [ ] 后端 `inspection_evidences` 表新增 N 行,`inspection_corrective_cases.correction_evidence_ids` 字段含三个 id
- [ ] 模拟无网 → toast "上传失败,请检查网络"
- [ ] 模拟超大文件 → toast 后端的 bizMessage(配额超限)
- [ ] 摄像头权限拒绝 → 静默返回(不 toast)

## 真机水印调优清单

文字宽度估算 `text.length * fontSize * 0.55` 是个粗估,中英文混排会偏。真机上可能要做:
- 用 `ctx.measureText(text)` 测准(uni 支持)
- 加深背景半透明 backdrop 提高对比
- 太小的图(<400px)可能让水印看不清,需要按图尺寸自适应 fontSize

## Next Steps (D-3b)

- 创建 `submission-detail` 页面 — 进入任务后看 detail 列表 + 按 ScoringPolicy 渲染评分控件 + 调 `PUT /inspection/submissions/details/{detailId}/response`
- 创建 `submit-appeal` 页面 — 从 submission-detail 入,带 `submissionDetailId` + textarea(申诉理由)+ 调 `POST /inspection/appeals`
- inspection manifest 加 2 路由 / 1 menu(申诉入口)+ event `inspection.appeal.created` 真触发(Phase D-2 manifest 已声明)

预估 1.5 天。

---

**P.S.** 本期由 6 个 commit 构成 + 全 subagent-driven-development(每 task 独立 implementer + 自检测试)。中途发现 Phase A 的 `capability.uploadFile` 假设性 API,顺带做了重构 — 这种"实际接通时发现旧设计有偏差"的良性迭代,后续也会遇到。
