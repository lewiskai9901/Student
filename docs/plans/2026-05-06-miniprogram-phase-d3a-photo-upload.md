# 小程序 Phase D-3a — 拍照水印 + 文件上传 + 整改附件 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 把 inspection 整改提交从"纯文本"升级为"文本 + 附件",打通拍照→水印→上传→addEvidence→submitCorrection 完整链路,并补一个后端通用文件上传端点。

**Architecture:**
- 后端补一个 `FileUploadController` 暴露 `FileStorageService.upload`,30-50 LOC,1 端点
- 前端 PlatformCapability 扩展 `watermarkImage(file, text)` 端口(canvas 操作 mp/h5 差异在 capability 层吸收)
- 整改详情页加"附件"区:列表 + 添加按钮 + 单张缩略图;提交时附 `evidenceIds`
- 不引入 OCR/AI/EXIF 解析(仅时间戳水印,GPS 可选不上)
- D-3a 不涉及评分组件 / 申诉 / 微信订阅 — 那些是 D-3b/D-3c

**Tech Stack:**
- 后端:Spring `MultipartFile`、existing `FileStorageService`、`@CasbinAccess(resource="insp:submission", action="execute")`
- 前端:uni-app `<canvas>` + `uni.canvasToTempFilePath` + `capability.takePhoto`/`capability.uploadFile`(已存在)

**Out of scope:**
- 视频证据(只做 PHOTO)
- 缩略图生成(后端如未自动生成,前端不补)
- 删除已上传 evidence(用户提交前不需要;post-submit 不可改)
- GPS 水印(需要 location 权限链,此期不做,只放 timestamp)

---

## 后端契约(D-3a 新增)

| Method+Path | 入参 | 返回 |
|---|---|---|
| POST `/files/upload` (multipart/form-data) | `file: MultipartFile`, `directory: String` (可选,默认 `inspection`) | `Result<{ fileName, fileUrl, size }>` |

权限:任何已登录用户(`@PreAuthorize("isAuthenticated()")`)。directory 仅允许白名单(`inspection`/`attachments`)防止任意目录写入。

---

## Task 1: 后端 FileUploadController

**Files:**
- Create: `backend/src/main/java/com/school/management/interfaces/rest/file/FileUploadController.java`
- Test: `backend/src/test/java/com/school/management/interfaces/rest/file/FileUploadControllerTest.java`

### Step 1: Write the failing test

```java
// FileUploadControllerTest.java
package com.school.management.interfaces.rest.file;

import com.school.management.infrastructure.external.FileStorageService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileUploadControllerTest {

    @Test
    void upload_returnsFilenameAndUrlForWhitelistedDirectory() {
        FileStorageService storage = Mockito.mock(FileStorageService.class);
        Mockito.when(storage.upload(Mockito.any(), Mockito.eq("inspection")))
               .thenReturn("https://cdn.test/inspection/abc.jpg");

        FileUploadController controller = new FileUploadController(storage);
        MockMultipartFile file = new MockMultipartFile("file", "abc.jpg", "image/jpeg", new byte[]{1,2,3});

        var result = controller.upload(file, "inspection");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getFileName()).isEqualTo("abc.jpg");
        assertThat(result.getData().getFileUrl()).isEqualTo("https://cdn.test/inspection/abc.jpg");
        assertThat(result.getData().getSize()).isEqualTo(3L);
    }

    @Test
    void upload_rejectsDirectoryNotInWhitelist() {
        FileStorageService storage = Mockito.mock(FileStorageService.class);
        FileUploadController controller = new FileUploadController(storage);
        MockMultipartFile file = new MockMultipartFile("file", "x.jpg", "image/jpeg", new byte[]{1});

        assertThatThrownBy(() -> controller.upload(file, "../../etc"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("directory");
    }

    @Test
    void upload_defaultsToInspectionWhenDirectoryNull() {
        FileStorageService storage = Mockito.mock(FileStorageService.class);
        Mockito.when(storage.upload(Mockito.any(), Mockito.eq("inspection")))
               .thenReturn("https://cdn.test/inspection/x.jpg");

        FileUploadController controller = new FileUploadController(storage);
        MockMultipartFile file = new MockMultipartFile("file", "x.jpg", "image/jpeg", new byte[]{0});

        var result = controller.upload(file, null);
        assertThat(result.getData().getFileUrl()).isEqualTo("https://cdn.test/inspection/x.jpg");
    }
}
```

### Step 2: Run test (RED)

```
cd backend
JAVA_HOME="/c/Program Files/Java/jdk-17" PATH="/c/Program Files/Java/jdk-17/bin:/c/Program Files/apache-maven-3.9.11/bin:$PATH" mvn test -Dtest=FileUploadControllerTest
```
Expected: FAIL — class not found.

### Step 3: Implement

```java
// FileUploadController.java
package com.school.management.interfaces.rest.file;

import com.school.management.common.api.Result;
import com.school.management.infrastructure.external.FileStorageService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * 通用文件上传端点 — 包装 FileStorageService 暴露给前端 (D-3a 引入).
 * 仅支持白名单目录, 防止任意目录写入.
 */
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileUploadController {

    private static final Set<String> ALLOWED_DIRS = Set.of("inspection", "attachments");
    private static final String DEFAULT_DIR = "inspection";

    private final FileStorageService storageService;

    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public Result<UploadResponse> upload(@RequestParam("file") MultipartFile file,
                                          @RequestParam(value = "directory", required = false) String directory) {
        String dir = (directory == null || directory.isBlank()) ? DEFAULT_DIR : directory;
        if (!ALLOWED_DIRS.contains(dir)) {
            throw new IllegalArgumentException("directory '" + dir + "' is not allowed");
        }
        String url = storageService.upload(file, dir);
        return Result.success(UploadResponse.builder()
            .fileName(file.getOriginalFilename())
            .fileUrl(url)
            .size(file.getSize())
            .build());
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UploadResponse {
        private String fileName;
        private String fileUrl;
        private Long size;
    }
}
```

### Step 4: Run test (GREEN)

```
mvn test -Dtest=FileUploadControllerTest
```
Expected: 3/3 PASS.

### Step 5: Run full backend compile to ensure no broken existing code

```
mvn compile -DskipTests
```
Expected: BUILD SUCCESS.

### Step 6: Commit

```bash
git add backend/src/main/java/com/school/management/interfaces/rest/file/FileUploadController.java \
        backend/src/test/java/com/school/management/interfaces/rest/file/FileUploadControllerTest.java
git commit -m "feat(backend/file): 通用文件上传端点 POST /files/upload (白名单 directory)"
```

---

## Task 2: 前端 PlatformCapability 扩展 watermarkImage 端口

**Files:**
- Modify: `miniprogram/src/core/platform/capability.ts` (interface)
- Modify: `miniprogram/src/core/platform/weixin.ts` (impl)
- Test: `miniprogram/src/core/platform/weixin.test.ts` (extend if exists, else create)

把 canvas 水印操作隔离在 capability 层 — 插件代码不感知 mp/h5 差异。

### Spec

```ts
interface WatermarkOpts {
  text: string                    // e.g. "2026-05-06 14:30:00 张三"
  position?: 'bottom-right' | 'bottom-left' | 'top-right' | 'top-left' // default 'bottom-right'
  fontSize?: number               // default 24 (rpx via uni)
  color?: string                  // default '#fff'
  shadowColor?: string            // default 'rgba(0,0,0,0.6)' for legibility
}

interface PlatformCapability {
  // ... existing
  watermarkImage(file: LocalFile, opts: WatermarkOpts): Promise<LocalFile>
}
```

### Step 1: Extend interface (capability.ts)

Add `WatermarkOpts` interface and `watermarkImage` method to `PlatformCapability`.

### Step 2: Implement in WeixinCapability (weixin.ts)

Use uni `<canvas>` API. **关键约束:** uni 小程序的 canvas 必须在页面 DOM 里有对应 `<canvas canvas-id="...">`,不能纯运行时建。所以这个实现需要一个**幕后页面级 canvas**。

策略选项 A(简单):`watermarkImage` 接受一个 `canvasId` 上下文,要求调用方页面有 canvas。
策略选项 B(我们采用):**懒加载**到一个共享隐藏 canvas。但 uni 没有"全局 canvas"。所以实际上做法 C:**让 capability.watermarkImage 接受 canvasId 参数**,调用方负责提供页面级 canvas 节点。

修订 spec(更现实):

```ts
interface WatermarkOpts {
  canvasId: string                // page-level <canvas canvas-id="xxx"> id
  text: string
  position?: ...
  fontSize?: ...
  color?: ...
  shadowColor?: ...
}
```

实现伪代码:

```ts
async watermarkImage(file: LocalFile, opts: WatermarkOpts): Promise<LocalFile> {
  const { canvasId, text } = opts
  const fontSize = opts.fontSize ?? 24
  const color = opts.color ?? '#fff'
  const shadow = opts.shadowColor ?? 'rgba(0,0,0,0.6)'

  // 1. 拿图片元数据
  const info = await new Promise<UniApp.GetImageInfoSuccessData>((resolve, reject) =>
    uni.getImageInfo({ src: file.path, success: resolve, fail: reject })
  )

  // 2. 在 canvas 上绘制
  const ctx = uni.createCanvasContext(canvasId)
  // 设置 canvas 尺寸需要在 setup 时通过 page data;运行时设宽高用 ctx.draw 调整
  ctx.drawImage(info.path, 0, 0, info.width, info.height)
  ctx.setFontSize(fontSize)
  ctx.setFillStyle(color)
  ctx.setShadow(2, 2, 4, shadow)
  // 简单 bottom-right 位置
  const padding = 20
  const textWidth = text.length * fontSize * 0.55  // approx
  ctx.fillText(text, info.width - textWidth - padding, info.height - padding)

  // 3. 等待 draw 完成后导出
  await new Promise<void>((resolve) => ctx.draw(false, () => resolve()))

  const tmpPath = await new Promise<string>((resolve, reject) =>
    uni.canvasToTempFilePath({
      canvasId,
      success: (res) => resolve(res.tempFilePath),
      fail: reject
    })
  )

  // 4. 返回新 LocalFile (size 用原始 size 近似 — 更准确得 getFileInfo)
  return { path: tmpPath, size: file.size }
}
```

### Step 3: Test (limited — full canvas test需要 mp 环境)

加一个轻量单测:`watermarkImage` opts validation(text 必填、canvasId 必填、position 默认值),不测真实 canvas 渲染(那是真机活)。

```ts
// weixin.test.ts (extend or create)
describe('WeixinCapability.watermarkImage', () => {
  it('throws if canvasId missing', async () => {
    const cap = new WeixinCapability()
    await expect(
      cap.watermarkImage({ path: 'x.jpg', size: 1 }, { canvasId: '', text: 't' })
    ).rejects.toThrow(/canvasId/)
  })

  it('throws if text missing', async () => {
    const cap = new WeixinCapability()
    await expect(
      cap.watermarkImage({ path: 'x.jpg', size: 1 }, { canvasId: 'c', text: '' })
    ).rejects.toThrow(/text/)
  })
})
```

(底层 uni.* mocked via node test env — already done in existing weixin.test.ts pattern)

### Step 4: Verify CI

```
cd miniprogram
npm run ci
```
Expected: 23 → 25 files / 104 → 106 tests (2 new tests for watermark validation).

### Step 5: Commit

```bash
git add miniprogram/src/core/platform/capability.ts \
        miniprogram/src/core/platform/weixin.ts \
        miniprogram/src/core/platform/weixin.test.ts
git commit -m "feat(miniprogram/core): PlatformCapability.watermarkImage 端口 (timestamp 水印)"
```

---

## Task 3: 前端 inspectionApi.addEvidence + 扩展 submitCorrection 签名

**Files:**
- Modify: `miniprogram/src/plugins/inspection/api/inspection.ts`
- Modify: `miniprogram/src/plugins/inspection/api/types.ts` (加 InspEvidence type)
- Modify: `miniprogram/src/plugins/inspection/api/inspection.write.test.ts` (加测试)

### Spec

```ts
// types.ts 新增
export type EvidenceType = 'PHOTO' | 'VIDEO' | 'DOCUMENT'

export interface InspEvidence {
  id: number
  submissionId: number
  detailId?: number
  evidenceType: EvidenceType
  fileName: string
  fileUrl: string
  fileSize?: number
  capturedAt?: string
}

// inspection.ts 新增 + 修改
addEvidence: (
  submissionId: number,
  body: { detailId?: number; evidenceType: EvidenceType; fileName: string; fileUrl: string }
) => requestWrapped<InspEvidence>({
  url: `/inspection/submissions/${submissionId}/evidences`,
  method: 'POST',
  data: body
})

// 扩展 submitCorrection 签名 (向后兼容: 第三参可选)
submitCorrection: (id: number, correctionNote: string, evidenceIds: number[] = []) =>
  requestWrapped<CorrectiveCase>({
    url: `/inspection/corrective-cases/${id}/submit-correction`,
    method: 'POST',
    data: { correctionNote, evidenceIds }
  })
```

### Step 1: Write tests (RED)

加 2 个测试到 `inspection.write.test.ts`:

```ts
it('addEvidence POSTs body to submissions evidences endpoint', async () => {
  requestWrappedMock.mockResolvedValueOnce({ id: 99, submissionId: 7, evidenceType: 'PHOTO', fileName: 'a.jpg', fileUrl: 'https://cdn/a.jpg' })
  const r = await inspectionApi.addEvidence(7, {
    detailId: 21, evidenceType: 'PHOTO', fileName: 'a.jpg', fileUrl: 'https://cdn/a.jpg'
  })
  expect(requestWrappedMock).toHaveBeenCalledWith({
    url: '/inspection/submissions/7/evidences',
    method: 'POST',
    data: { detailId: 21, evidenceType: 'PHOTO', fileName: 'a.jpg', fileUrl: 'https://cdn/a.jpg' }
  })
  expect(r.id).toBe(99)
})

it('submitCorrection passes evidenceIds when provided', async () => {
  requestWrappedMock.mockResolvedValueOnce({ id: 9, status: 'SUBMITTED' })
  await inspectionApi.submitCorrection(9, '已完成', [99, 100])
  expect(requestWrappedMock).toHaveBeenCalledWith({
    url: '/inspection/corrective-cases/9/submit-correction',
    method: 'POST',
    data: { correctionNote: '已完成', evidenceIds: [99, 100] }
  })
})
```

第 5 个原有 submitCorrection 测试(没传 evidenceIds)仍应通过 — 默认 `[]`。

### Step 2-5: Run RED → GREEN → CI → commit

```bash
npx vitest run src/plugins/inspection/api/inspection.write.test.ts
# implement → green
npm run ci  # 25/108 expected (+2 tests)
git add src/plugins/inspection/api/inspection.ts src/plugins/inspection/api/types.ts src/plugins/inspection/api/inspection.write.test.ts
git commit -m "feat(miniprogram/inspection): addEvidence API + submitCorrection 接收 evidenceIds"
```

---

## Task 4: 前端 correction-detail.vue 接通拍照流

**Files:**
- Modify: `miniprogram/src/plugins/inspection/pages/correction-detail.vue`

### UX 设计

submit 分支(IN_PROGRESS / REJECTED 状态)增加"附件"区,顺序:
1. textarea(整改说明,已存)
2. **新增** "附件 (可选)" 区:
   - 横向 scroll-view 缩略图列表(已上传的) + 末尾"+"按钮
   - 点 "+" → `capability.takePhoto({sourceType:['camera']})` → 选第一张 → watermark → upload → addEvidence → 入列表
   - 上传中显示进度(单个 photo 上传 ~1-3s,简单 loading 即可)
   - 缩略图右上角小 "✕" 删按钮(本地 evidenceIds 数组里删,后端不调用 DELETE — 用户提交前可改主意)
3. "提交整改" 按钮(传 evidenceIds 给 submitCorrection)

### 隐藏 canvas

页面模板加 `<canvas canvas-id="watermark-canvas" style="position:fixed;left:-9999px;width:1080px;height:1080px;" />`。1080x1080 是默认绘图尺寸,实际绘制时根据 image 尺寸动态调。

### 错误处理

- takePhoto 用户取消 → 静默(不 toast)
- watermark 失败 → toast,保留原图作 fallback?**决策:不 fallback,toast "水印生成失败,请重试" 后中止**(避免用户上传无水印图)
- upload 失败(网络/后端 5xx)→ toast "上传失败,请检查网络",不入列表
- addEvidence 失败(已上传 OSS 但后端注册失败)→ toast,记 console.warn,不入列表(OSS 上多了孤立文件不影响业务)

### 数据状态

```ts
interface AttachedEvidence {
  evidenceId: number      // backend InspEvidence.id
  fileUrl: string         // for thumbnail
  fileName: string        // display only
}

const evidences = ref<AttachedEvidence[]>([])
const adding = ref(false)
```

submitCorrection 传 `evidences.value.map(e => e.evidenceId)`。

### Step 1: 改 `<script setup>` — 加 takePhoto/watermark/upload/addEvidence pipeline

(详见 implementer prompt;约 +60 行)

### Step 2: 改 template — 加附件区 + 隐藏 canvas

### Step 3: 改 style — 缩略图列表样式

### Step 4: 跑 type-check / lint / ci(25/108 不变)

### Step 5: Commit

```bash
git add miniprogram/src/plugins/inspection/pages/correction-detail.vue
git commit -m "feat(miniprogram/inspection): correction-detail 接通拍照水印上传 + evidenceIds 提交"
```

---

## Task 5: 集成测试 + 报告 + 合并

### Integration test

`src/__integration__/inspection-attachment.test.ts` — 3 测试:
1. addEvidence + submitCorrection 链路:模拟 take→upload→addEvidence→submit,断言 evidenceIds 传到 submit
2. take 取消(takePhoto reject 'cancel')→ 不调用 upload,不调用 addEvidence
3. upload 失败 → 不调用 addEvidence

工作量:60 行测试。

预期 CI:25 → 26 files / 108 → 111 tests。

### 报告

`miniprogram/docs/phase-d3a-photo-upload-report.md`:
- 范围 / out of scope
- 后端契约新增 (`/files/upload`)
- 前端新增 (capability.watermarkImage / addEvidence / 整改附件 UI)
- 文件清单
- 测试基线
- 真机验证清单
- D-3b 入口提示

### Merge

```bash
cd /d/学生管理系统
git merge --no-ff feature/miniprogram-phase-d3a -m "Merge branch 'feature/miniprogram-phase-d3a' — Phase D-3a 拍照水印上传 + 整改附件闭环"
git worktree remove .worktrees/miniprogram-phase-d3a
git branch -d feature/miniprogram-phase-d3a
```

---

## Verification Checklist

- [ ] 后端 mvn compile 通过
- [ ] 后端 FileUploadControllerTest 3/3 通过
- [ ] 前端 npm run ci 26 files / 111 tests
- [ ] 前端 type-check / lint 0 errors
- [ ] 报告写完
- [ ] master 合并 + 清理

## 真机待验

代码层只能验到契约,以下需真机:
- [ ] 后端启动,前端发 multipart 上传一张图 → 看 OSS/本地存储有文件 + URL 可访问
- [ ] correction-detail "+" 按钮 → 摄像头打开 → 拍 → 看到 watermark 文字在右下角 → 缩略图入列表
- [ ] 多拍 3 张 → 提交整改 → 后端 evidenceIds 三个都入库
- [ ] watermark 文字内容正确(timestamp + 用户名)
- [ ] 删除其中一张缩略图 → 列表更新
- [ ] 上传中点取消 (UI 不暴露,跳过)
