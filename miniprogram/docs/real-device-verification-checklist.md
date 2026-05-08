# 小程序真机验证 Checklist

> 累积 8 个 phase 未真跑过后端,本次按序回归。
> 记录每项 ✅/❌,❌ 项贴出 console + network 截图,我在线帮看。

## Step 1: 启动后端

```bash
cd D:/学生管理系统/backend
JAVA_HOME="/c/Program Files/Java/jdk-17" PATH="/c/Program Files/Java/jdk-17/bin:$PATH" mvn spring-boot:run -DskipTests
```

等到看到:
```
Tomcat started on port(s): 8080
Started ManagementApplication in X seconds
```

测一下后端真活着:
```bash
curl http://localhost:8080/api/auth/login -X POST -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```
应该返回 JSON:`{"code":200,"data":{"accessToken":"...","userInfo":{...},"enabledPlugins":[...]},...}`

- [ ] 后端启动成功
- [ ] curl 登录返回 200 + accessToken
- [ ] response 里有 `enabledPlugins` 字段(D-1 验证)
- [ ] response 里 `userInfo` 嵌套结构正确

## Step 2: 启动小程序 dev

```bash
cd D:/学生管理系统/miniprogram
npm run dev:mp-weixin
```

等到看到 "DONE Build complete." (持续 watch 模式)

打开微信开发者工具 → 导入项目 → 选目录 `D:\学生管理系统\miniprogram\dist\dev\mp-weixin` → AppID 用测试号或绑定的实际 AppID。

- [ ] dev server 持续 watch 不报错
- [ ] 微信开发者工具加载项目成功
- [ ] 编译无错误,模拟器渲染出登录页

## Step 3: 登录(D-1 验证)

输入 `admin` / `admin123` → 提交。

- [ ] 登录成功跳转到首页
- [ ] localStorage 里有 accessToken / refreshToken / user / enabledPlugins / tenantId / orgUnitId
- [ ] 控制台无红错

**常见问题:**
- 如果 401 → 后端 CORS 没放小程序 origin,看后端 SecurityConfig
- 如果 BizError "用户名或密码错" → 数据库 admin 账号可能没初始化,跑 `database/init/init_data.sql`

## Step 4: 首页(Phase A + Phase E)

- [ ] 首页显示 "Hi, {name}"
- [ ] 看到 "核心" section 3 个 grid:**组织架构 / 通讯录 / 场所**
- [ ] 看到 "功能" section,根据 enabledPlugins 显示插件菜单(inspection / education / 等)
- [ ] 如果 enabledPlugins 包含 'inspection' → 看到检查相关菜单(我的任务/扫码/可领/我的整改/我的申诉)
- [ ] 如果 enabledPlugins 包含 'EDU' → 看到教育菜单(学生/班级/我的班级)

**如果某个插件菜单不显示:**
- 检查后端 `tenant_plugin_enablement` 表是否启用了对应 industry_code
- enabledPlugins 大小写敏感:inspection 小写 / EDU 大写 / HEALTH 大写

## Step 5: 我的资料(E-1)

底部 tab 切到"我的"。

- [ ] 显示 avatar / 姓名 / username / type-badge / 手机 / 邮箱 / 组织 ID / 租户 / 角色 chips
- [ ] 切回首页再回"我的" → 数据 onShow 重新拉(network 看到 GET /auth/me)
- [ ] 退出登录 → 跳回登录页 + storage 清

## Step 6: 组织架构(E-2)

首页核心 → 组织架构。

- [ ] 树渲染,顶层 root 自动展开
- [ ] 点节点 caret → 折叠/展开
- [ ] 节点显示:unitName + statusLabel badge + headcount + unitType
- [ ] 子层缩进正确(每层 24rpx)

## Step 7: 通讯录(E-3)

首页核心 → 通讯录。

- [ ] 列表显示同 orgUnit 的用户(avatar/姓名/username/type-badge)
- [ ] 搜索框输入 → 客户端实时过滤(姓名/手机/邮箱)
- [ ] 点行 → 展开 inline 详情(邮箱/所属/状态/角色)
- [ ] 再点行 → 收起

## Step 8: 场所浏览(E-4)

首页核心 → 场所。

- [ ] 树渲染 + 折叠
- [ ] hasCapacity 节点显示进度条 + "已用/总数"

## Step 9: inspection 任务(C + D-2 + D-3a + D-3b)

需要后端 inspection 数据:任务 / 整改单 / 提交 / 评分项。

如果数据库没数据,**先用 web 端创建一个项目 + 任务 + 给当前 admin 分配**,或跳过这部分。

### 9.1 任务列表 (C)
- [ ] 进入"我的任务" → 列表显示
- [ ] 进入"可领任务" → 显示未认领

### 9.2 任务状态机 (D-2)
- [ ] 找一个 PENDING 任务 → "认领任务"按钮 → 点 → toast "已认领" → 状态变 CLAIMED
- [ ] CLAIMED 状态 → "开始执行"按钮 → 点 → toast → 状态变 IN_PROGRESS
- [ ] IN_PROGRESS 状态 → 现在显示 **2 个按钮**:填写检查表 / 提交任务

### 9.3 检查表填写 (D-3b-1)
- [ ] 点"填写检查表" → 进入 submission-detail 页
- [ ] 列出所有 item,按 sectionName 分组
- [ ] 找一个 PASS_FAIL item → 点"通过"或"不通过" → 切换高亮
- [ ] 找一个 DIRECT item → 输数字 → blur 时保存 → toast
- [ ] 其他 mode item → 显示"请用 web 端填写"灰态
- [ ] 全填完 → "提交检查表"按钮可点 → 点 → toast + 跳回 task-detail

### 9.4 提交任务 (D-2)
- [ ] task-detail → 点"提交任务" → 后端检查 submission 完整 → toast "已提交" + reLaunch my-tasks

### 9.5 整改单文本 (D-2)
- [ ] 进入"我的整改" → 列表
- [ ] 找一个 IN_PROGRESS / REJECTED 整改单 → 点详情
- [ ] 看到 textarea + 附件区 + "提交整改"按钮
- [ ] 输 ≥5 字 → 点提交 → toast + 跳回 my-corrections

### 9.6 整改单附件 (D-3a)
- [ ] correction-detail 的附件区 → 点 "+"
- [ ] 摄像头打开 → 拍 1 张
- [ ] 缩略图入列表(注意水印!右下角应有 timestamp + 用户名)
- [ ] 多拍 3 张 → 横向 scroll
- [ ] 点缩略图右上角 ✕ → 删除
- [ ] 提交整改 → 后端 inspection_evidences 表新增 N 行,case.correction_evidence_ids 含这些 id

**附件流程关键点(易错):**
- 摄像头权限第一次会弹授权
- canvas 水印渲染依赖隐藏 canvas — 看模拟器 console 有无 canvas 错
- 上传 multipart 时 Authorization header 必须带 — 检查 network

### 9.7 漂移自愈 (D-2 fix)
两个设备开同一 PENDING 任务:A 先认领,B 再点 → B 应看到 BizError toast + 页面自动 reload(状态机自愈)。

- [ ] 模拟并发 → BizError 后 reload 而不是停在错误状态

### 9.8 申诉(D-3b-2)
- [ ] submission-detail 任意已 score item → 看到"申诉此项"链接
- [ ] 点 → submit-appeal 页 → 输 ≥5 字 → 提交 → toast + 跳回
- [ ] 进入"我的申诉" → 列表里看到刚提交的

### 9.9 扫码(C)
- [ ] 首页 → 扫码检查 → 模拟扫一个 `INSPECTION:TASK:123` 二维码 → 跳到 task-detail?id=123

## Step 10: education 浏览(G)

需要租户启用 EDU,且数据库有学生/班级数据。

- [ ] 首页"功能"看到 学生 / 班级 菜单
- [ ] 学生页 → 列表 + 搜索 + 加载更多
- [ ] 点学生 → 详情卡(姓名/学号/手机/邮箱/状态)
- [ ] 班级页 → 列表
- [ ] 点班级 → 详情 + 内嵌该班学生

## Step 11: 我的班级(G-2)

需要当前 admin 是某班的 myRole(班主任/任课老师等)。

- [ ] 首页看到"我的班级"菜单
- [ ] 进入:1 班自动选,多班 picker
- [ ] Overview 卡:总人数 / 男 / 女 / 平均分 / 排名 / 趋势(▲▼)
- [ ] 学生 tab → 名单 + 搜索
- [ ] 宿舍 tab → 楼栋分组 + 房间 + 床位

**已知后端 bug 已修(G-2):**
`MyClassController` 3 个端点 `{classId}` 重命名为 `{orgUnitId}` 修了 path variable 名字 mismatch。如果你拉的是修过的 master 应该没事;如果遇到 "missing path variable" → 后端没 rebuild。

## Step 12: 插件 enable/disable(架构验证)

把当前租户的 EDU 在 `tenant_plugin_enablement` 表里 disable,**重新登录小程序**:

- [ ] enabledPlugins 不再含 'EDU'
- [ ] 学生 / 班级 / 我的班级 菜单**消失**
- [ ] 其他插件(inspection)菜单还在
- [ ] 重新启用 EDU + 再登录 → 菜单回来

这是 Phase G 验证 plugin 架构真工作的关键场景。

## Step 13: 401 处理(全期通用)

让 access_token 过期(改后端 `jwt.access-token-expiration` 为 60s,等过期):

- [ ] 任何请求 401 → 当前简化处理:清登录态 + reLaunch login 页
- [ ] 不会无限循环
- [ ] 用户重新登录后能继续

(Phase D 计划做 refresh token 自动续期但还没做,目前是粗暴处理)

## 常见问题速查

| 现象 | 原因 | 快查 |
|---|---|---|
| 所有请求 401 | token 过期或 storage 丢 | 看 storage.accessToken |
| 所有请求 BizError "no permission" | 当前用户 Casbin 没相应 view 权限 | 后端日志 + 给 admin 加 super 权限测 |
| 插件菜单不显示 | enabledPlugins 没含对应 code | DevTools storage.enabledPlugins,后端 tenant_plugin_enablement 表 |
| 拍照水印没出来 | canvas-id 没找到节点 / image getInfo 失败 | DevTools console + ctx.draw 错 |
| 上传 失败 HTTP 401 | uploadWrapped 没注入 Authorization | 看 network 请求 header |
| 上传 后端 500 | FileStorageService 路径写权限 / 文件大小限制 | 后端日志 |
| pages.json 路由不对 | dev 没把新页 build 进来 | 重启 npm run dev |

## 验证完后

- 把成功的项打 ✅,失败的贴 console + network 给我
- 对就贴在这条 chat 里,我帮看
