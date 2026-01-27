# 资产管理模块功能增强计划

> 版本: v1.0
> 日期: 2026-01-25
> 状态: 待评审

---

## 一、现有框架分析

### 1.1 工作流框架 (Flowable)

系统已集成 **Flowable 7.x** 工作流引擎，具备以下能力：

```
已有能力：
├── FlowableProcessService      # 流程服务接口
│   ├── deployProcess()         # 部署流程定义
│   ├── startProcess()          # 启动流程实例
│   ├── getUserTasks()          # 获取用户待办
│   ├── completeTask()          # 完成任务（审批通过）
│   ├── rejectToNode()          # 驳回到指定节点
│   ├── transferTask()          # 转交任务
│   ├── terminateProcess()      # 终止流程
│   ├── getProcessDiagram()     # 获取流程图
│   └── getProcessProgress()    # 获取流程进度
├── WorkflowTemplate            # 流程模板实体
│   ├── bpmnXml                 # BPMN流程定义
│   ├── formConfig              # 表单配置
│   └── nodeConfig              # 节点配置（审批人规则）
├── TaskApprovalRecord          # 审批记录
└── SystemMessage               # 系统消息通知
```

### 1.2 前端设计规范

分析现有组件，提取设计模式：

| 设计元素 | 规范 |
|---------|------|
| 页面背景 | `bg-gray-50 min-h-full` |
| 页面内边距 | `p-6` |
| 卡片容器 | `rounded-lg border border-gray-200 bg-white` |
| 卡片内边距 | `p-4` |
| 标题样式 | `text-xl font-semibold text-gray-900` |
| 副标题 | `text-sm text-gray-500` |
| 主按钮 | `bg-blue-600 text-white hover:bg-blue-700 rounded-md px-4 h-9` |
| 次要按钮 | `border border-gray-300 bg-white text-gray-700 hover:bg-gray-50` |
| 输入框 | `h-9 rounded-md border border-gray-300 px-3 text-sm focus:border-blue-500` |
| 状态标签 | `rounded px-2 py-0.5 text-xs` + 颜色类 |
| 批量操作栏 | 选中后显示在表格上方，蓝色背景 |
| 对话框宽度 | 小: `400px`, 中: `520px`, 大: `800px` |

### 1.3 批量操作参考模式

```vue
<!-- 批量操作栏 (参考 BatchAllocationDialog.vue) -->
<div class="bg-gray-50 rounded-lg p-4 mb-4">
  <div class="flex items-center justify-between">
    <div class="flex items-center gap-3">
      <div class="w-10 h-10 rounded-lg bg-purple-100 flex items-center justify-center">
        <el-icon class="text-purple-600 text-lg">...</el-icon>
      </div>
      <div>
        <div class="font-medium text-gray-900">已选择 {{ count }} 项</div>
        <div class="text-xs text-gray-500 mt-0.5">{{ summary }}</div>
      </div>
    </div>
    <el-button text size="small" @click="toggleList">
      {{ showList ? '收起' : '查看' }}
    </el-button>
  </div>
</div>
```

---

## 二、功能清单与优先级

### 阶段一：核心补全（P0）

| # | 功能 | 业务价值 | 复杂度 | 预估工时 |
|---|-----|---------|--------|---------|
| 1 | 批量入库 | ⭐⭐⭐⭐⭐ | ⭐⭐ | 2天 |
| 2 | 批量调拨 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | 3天 |
| 3 | Excel导入导出 | ⭐⭐⭐⭐ | ⭐⭐⭐ | 3天 |

### 阶段二：流程完善（P1）

| # | 功能 | 业务价值 | 复杂度 | 预估工时 |
|---|-----|---------|--------|---------|
| 4 | 资产领用/借用 | ⭐⭐⭐⭐ | ⭐⭐⭐ | 4天 |
| 5 | 调拨审批流程 | ⭐⭐⭐ | ⭐⭐⭐⭐ | 4天 |
| 6 | 折旧自动计算 | ⭐⭐⭐ | ⭐⭐ | 2天 |
| 7 | 盘点流程完善 | ⭐⭐⭐ | ⭐⭐⭐ | 3天 |

### 阶段三：体验优化（P2）

| # | 功能 | 业务价值 | 复杂度 | 预估工时 |
|---|-----|---------|--------|---------|
| 8 | 条码/二维码 | ⭐⭐⭐ | ⭐⭐ | 2天 |
| 9 | 预警机制 | ⭐⭐⭐ | ⭐⭐⭐ | 2天 |
| 10 | 报表导出 | ⭐⭐⭐ | ⭐⭐ | 2天 |

---

## 三、详细设计

### 3.1 批量入库

#### 3.1.1 业务场景

```
学校采购100台联想电脑：
  分类: 办公设备 > 计算机
  品牌: 联想
  型号: ThinkPad E14
  单价: 5000元
  数量: 100台
  存放: 总务处仓库
  责任人: 张三（仓库管理员）

系统自动生成：
  ZC-BG-JS-000001 ~ ZC-BG-JS-000100
```

#### 3.1.2 数据模型

无需新增表，复用现有 `asset` 表。

#### 3.1.3 后端设计

**新增API：**

```java
// AssetController.java
@Operation(summary = "批量入库")
@PostMapping("/batch")
@PreAuthorize("hasAuthority('asset:manage')")
public ApiResponse<BatchCreateResult> batchCreateAssets(
    @Valid @RequestBody BatchCreateAssetRequest request
);

// BatchCreateAssetRequest.java
@Data
public class BatchCreateAssetRequest {
    @NotBlank private String assetName;      // 资产名称
    @NotNull private Long categoryId;        // 分类ID
    private String brand;                    // 品牌
    private String model;                    // 型号
    private String unit;                     // 计量单位
    @Min(1) @Max(1000)
    private Integer quantity = 1;            // 数量（批量入库核心字段）
    private BigDecimal originalValue;        // 单价
    private LocalDate purchaseDate;          // 购置日期
    private LocalDate warrantyDate;          // 保修期至
    private String supplier;                 // 供应商
    private String locationType;             // 位置类型
    private Long locationId;                 // 位置ID
    private String locationName;             // 位置名称
    private Long responsibleUserId;          // 责任人ID
    private String responsibleUserName;      // 责任人姓名
    private String remark;                   // 备注
}

// BatchCreateResult.java
@Data
public class BatchCreateResult {
    private Integer totalCount;              // 总数
    private Integer successCount;            // 成功数
    private String firstAssetCode;           // 首个编号
    private String lastAssetCode;            // 末个编号
    private List<Long> assetIds;             // 生成的资产ID列表
}
```

**服务实现：**

```java
// FixedAssetApplicationService.java
@Transactional
public BatchCreateResult batchCreateAssets(BatchCreateAssetCommand command) {
    // 1. 获取分类信息
    AssetCategory category = categoryRepository.findById(command.getCategoryId());

    // 2. 批量生成资产
    List<Asset> assets = new ArrayList<>();
    for (int i = 0; i < command.getQuantity(); i++) {
        String assetCode = assetRepository.generateAssetCode(category.getCategoryCode());
        Asset asset = Asset.create(
            assetCode,
            command.getAssetName(),
            category,
            command.getOriginalValue(),
            AssetLocation.of(command.getLocationType(), command.getLocationId(), command.getLocationName()),
            command.getResponsibleUserId(),
            command.getResponsibleUserName(),
            command.getOperatorId(),
            command.getOperatorName()
        );
        // 设置其他属性...
        assets.add(asset);
    }

    // 3. 批量保存
    assetRepository.batchSave(assets);

    // 4. 返回结果
    return BatchCreateResult.builder()
        .totalCount(command.getQuantity())
        .successCount(assets.size())
        .firstAssetCode(assets.get(0).getAssetCode())
        .lastAssetCode(assets.get(assets.size() - 1).getAssetCode())
        .assetIds(assets.stream().map(Asset::getId).collect(Collectors.toList()))
        .build();
}
```

#### 3.1.4 前端设计

**组件：`AssetBatchCreateDialog.vue`**

```
┌─────────────────────────────────────────────────────────────┐
│  批量入库                                            [×]    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  📦  批量入库模式                                    │   │
│  │      同一批次的相同资产，系统自动生成唯一编号          │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ━━ 基本信息 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   │
│                                                             │
│  资产名称 *        [联想ThinkPad E14笔记本电脑      ]       │
│                                                             │
│  资产分类 *        [办公设备 > 计算机              ▼]       │
│                                                             │
│  品牌              [联想                            ]       │
│  型号              [ThinkPad E14                    ]       │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  入库数量 *                                          │   │
│  │  ┌─────────────────────────────────────────────┐    │   │
│  │  │              [  100  ]  台                   │    │   │
│  │  └─────────────────────────────────────────────┘    │   │
│  │  系统将生成 100 件资产，编号：ZC-BG-JS-XXXX 系列      │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ━━ 财务信息 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   │
│                                                             │
│  单价(元)          [5000.00                        ]       │
│  购置日期          [2026-01-25                    📅]       │
│  保修期至          [2029-01-25                    📅]       │
│  供应商            [联想官方商城                    ]       │
│                                                             │
│  ━━ 存放位置 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   │
│                                                             │
│  [仓库 ▼] [主校区 > 行政楼 > 1F > 总务处仓库      ▼]       │
│                                                             │
│  责任人            [张三 (总务处)                  👤]       │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│  预计总价值：¥500,000.00                                    │
│                                     [取消]  [确认入库]      │
└─────────────────────────────────────────────────────────────┘
```

**关键交互：**

1. 数量输入框使用 `el-input-number`，范围 1-1000
2. 分类选择使用级联选择器，与现有保持一致
3. 位置选择复用 `LocationSelector` 组件
4. 责任人选择复用 `UserSelector` 组件
5. 实时显示预计总价值 = 单价 × 数量
6. 成功后显示结果摘要（首末编号）

---

### 3.2 批量调拨

#### 3.2.1 业务场景

```
从 总务处仓库 调拨到 图书馆三楼阅览室：
  选择资产：
    - ZC-BG-JS-000001 联想电脑
    - ZC-BG-JS-000002 联想电脑
    - ZC-BG-JS-000003 联想电脑
  新位置：图书馆三楼阅览室
  新责任人：李馆长
  调拨备注：图书馆设备升级

生成调拨单：DB-2026012500001
```

#### 3.2.2 数据模型

**新增表：资产调拨单**

```sql
-- 调拨单主表
CREATE TABLE asset_transfer_order (
    id BIGINT PRIMARY KEY,
    order_no VARCHAR(32) NOT NULL UNIQUE COMMENT '调拨单号',

    -- 调出信息
    from_location_type VARCHAR(32) COMMENT '原位置类型',
    from_location_id BIGINT COMMENT '原位置ID',
    from_location_name VARCHAR(200) COMMENT '原位置名称',
    from_responsible_user_id BIGINT COMMENT '原责任人ID',
    from_responsible_user_name VARCHAR(50) COMMENT '原责任人姓名',

    -- 调入信息
    to_location_type VARCHAR(32) NOT NULL COMMENT '目标位置类型',
    to_location_id BIGINT COMMENT '目标位置ID',
    to_location_name VARCHAR(200) NOT NULL COMMENT '目标位置名称',
    to_responsible_user_id BIGINT COMMENT '新责任人ID',
    to_responsible_user_name VARCHAR(50) COMMENT '新责任人姓名',

    -- 数量统计
    total_count INT NOT NULL DEFAULT 0 COMMENT '调拨资产总数',
    total_value DECIMAL(15,2) COMMENT '调拨资产总值',

    -- 审批信息（可选）
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0草稿 1待审批 2已通过 3已驳回 4已执行',
    process_instance_id VARCHAR(64) COMMENT 'Flowable流程实例ID',

    -- 备注
    remark VARCHAR(500) COMMENT '调拨原因/备注',

    -- 操作信息
    applicant_id BIGINT NOT NULL COMMENT '申请人ID',
    applicant_name VARCHAR(50) NOT NULL COMMENT '申请人姓名',
    apply_time DATETIME NOT NULL COMMENT '申请时间',
    execute_time DATETIME COMMENT '执行时间',

    -- 通用字段
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,

    INDEX idx_order_no (order_no),
    INDEX idx_status (status),
    INDEX idx_apply_time (apply_time)
) COMMENT '资产调拨单';

-- 调拨单明细表
CREATE TABLE asset_transfer_order_item (
    id BIGINT PRIMARY KEY,
    order_id BIGINT NOT NULL COMMENT '调拨单ID',
    asset_id BIGINT NOT NULL COMMENT '资产ID',

    -- 冗余资产信息（快照）
    asset_code VARCHAR(50) NOT NULL COMMENT '资产编号',
    asset_name VARCHAR(200) NOT NULL COMMENT '资产名称',
    category_name VARCHAR(100) COMMENT '分类名称',
    original_value DECIMAL(15,2) COMMENT '原值',

    -- 调出时位置快照
    from_location_name VARCHAR(200) COMMENT '原位置',
    from_responsible_user_name VARCHAR(50) COMMENT '原责任人',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_order_id (order_id),
    INDEX idx_asset_id (asset_id)
) COMMENT '调拨单明细';
```

#### 3.2.3 后端设计

**领域模型：**

```java
// domain/asset/model/aggregate/TransferOrder.java
@Getter
public class TransferOrder extends AggregateRoot<Long> {
    private String orderNo;
    private AssetLocation fromLocation;
    private AssetLocation toLocation;
    private Long fromResponsibleUserId;
    private String fromResponsibleUserName;
    private Long toResponsibleUserId;
    private String toResponsibleUserName;
    private List<TransferOrderItem> items;
    private TransferOrderStatus status;
    private String processInstanceId;
    private String remark;
    private Long applicantId;
    private String applicantName;
    private LocalDateTime applyTime;
    private LocalDateTime executeTime;

    // 创建调拨单
    public static TransferOrder create(
        String orderNo,
        AssetLocation toLocation,
        Long toResponsibleUserId,
        String toResponsibleUserName,
        String remark,
        Long applicantId,
        String applicantName
    ) {
        // ...
    }

    // 添加调拨项
    public void addItem(Asset asset) {
        // 校验资产状态
        if (!asset.canTransfer()) {
            throw new BusinessException("资产 " + asset.getAssetCode() + " 不可调拨");
        }
        // 记录原位置快照
        TransferOrderItem item = new TransferOrderItem(
            asset.getId(),
            asset.getAssetCode(),
            asset.getAssetName(),
            asset.getCategoryName(),
            asset.getOriginalValue(),
            asset.getLocationName(),
            asset.getResponsibleUserName()
        );
        items.add(item);
    }

    // 提交审批
    public void submitForApproval(String processInstanceId) {
        this.status = TransferOrderStatus.PENDING_APPROVAL;
        this.processInstanceId = processInstanceId;
    }

    // 审批通过
    public void approve() {
        this.status = TransferOrderStatus.APPROVED;
    }

    // 驳回
    public void reject() {
        this.status = TransferOrderStatus.REJECTED;
    }

    // 执行调拨
    public void execute(List<Asset> assets) {
        if (status != TransferOrderStatus.APPROVED && status != TransferOrderStatus.DRAFT) {
            throw new BusinessException("当前状态不允许执行");
        }

        for (Asset asset : assets) {
            asset.transfer(
                toLocation,
                toResponsibleUserId,
                toResponsibleUserName,
                applicantId,
                applicantName
            );
        }

        this.status = TransferOrderStatus.EXECUTED;
        this.executeTime = LocalDateTime.now();
    }
}
```

**新增API：**

```java
// AssetTransferController.java
@Tag(name = "资产调拨管理")
@RestController
@RequestMapping("/v2/asset-transfers")
@RequiredArgsConstructor
public class AssetTransferController {

    @Operation(summary = "创建调拨单")
    @PostMapping
    @PreAuthorize("hasAuthority('asset:manage')")
    public ApiResponse<Long> createTransferOrder(
        @Valid @RequestBody CreateTransferOrderRequest request
    );

    @Operation(summary = "获取调拨单详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('asset:list')")
    public ApiResponse<TransferOrderDTO> getTransferOrder(@PathVariable Long id);

    @Operation(summary = "调拨单列表")
    @GetMapping
    @PreAuthorize("hasAuthority('asset:list')")
    public ApiResponse<PageResult<TransferOrderDTO>> listTransferOrders(
        @RequestParam(required = false) Integer status,
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "20") Integer pageSize
    );

    @Operation(summary = "提交审批")
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('asset:manage')")
    public ApiResponse<Void> submitForApproval(@PathVariable Long id);

    @Operation(summary = "直接执行（无需审批时）")
    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAuthority('asset:manage')")
    public ApiResponse<Void> executeTransfer(@PathVariable Long id);

    @Operation(summary = "撤销调拨单")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('asset:manage')")
    public ApiResponse<Void> cancelTransferOrder(@PathVariable Long id);
}

// CreateTransferOrderRequest.java
@Data
public class CreateTransferOrderRequest {
    @NotEmpty(message = "请选择要调拨的资产")
    @Size(max = 500, message = "单次最多调拨500件资产")
    private List<Long> assetIds;           // 资产ID列表

    @NotBlank private String locationType;  // 目标位置类型
    private Long locationId;                // 目标位置ID
    @NotBlank private String locationName;  // 目标位置名称

    private Long responsibleUserId;         // 新责任人ID
    private String responsibleUserName;     // 新责任人姓名

    private String remark;                  // 调拨备注
    private Boolean needApproval = false;   // 是否需要审批
}
```

#### 3.2.4 前端设计

**步骤1：资产列表增加多选**

修改 `AssetManagementCenter.vue`：

```vue
<!-- 表格视图增加多选列 -->
<el-table
  :data="assetList"
  @selection-change="handleSelectionChange"
>
  <el-table-column type="selection" width="50" />
  <!-- 其他列保持不变 -->
</el-table>

<!-- 批量操作栏（选中时显示） -->
<div
  v-if="selectedAssets.length > 0"
  class="mb-4 bg-blue-50 border border-blue-200 rounded-lg p-3"
>
  <div class="flex items-center justify-between">
    <div class="flex items-center gap-3">
      <span class="text-sm text-blue-700">
        已选择 <strong>{{ selectedAssets.length }}</strong> 件资产
      </span>
      <el-button text type="primary" size="small" @click="clearSelection">
        取消选择
      </el-button>
    </div>
    <div class="flex items-center gap-2">
      <el-button size="small" @click="handleBatchTransfer">
        <el-icon><Position /></el-icon>
        批量调拨
      </el-button>
      <el-button size="small" @click="handleBatchScrap">
        <el-icon><Delete /></el-icon>
        批量报废
      </el-button>
    </div>
  </div>
</div>
```

**步骤2：批量调拨对话框**

**组件：`AssetBatchTransferDialog.vue`**

```
┌─────────────────────────────────────────────────────────────┐
│  批量调拨                                            [×]    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ 📦  已选择 3 件资产                          [查看]  │   │
│  │     联想电脑 2件、惠普打印机 1件                      │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌───────────────────┐     ┌───────────────────┐           │
│  │ 原位置            │  →  │ 新位置            │           │
│  │ 总务处仓库        │     │ 待选择...         │           │
│  └───────────────────┘     └───────────────────┘           │
│                                                             │
│  ━━ 调拨目标 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   │
│                                                             │
│  存放位置 *                                                 │
│  [教室 ▼] [主校区 > 图书馆 > 3F > 阅览室          ▼]       │
│                                                             │
│  新责任人（可选）                                            │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ 👤  点击选择新责任人                                 │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  调拨备注                                                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ 图书馆设备升级配置                                   │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ━━ 审批设置 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   │
│                                                             │
│  ○ 直接执行（无需审批）                                     │
│  ● 提交审批                                                 │
│    审批流程：部门负责人 → 资产管理员                        │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│  调拨资产总值：¥15,000.00                                   │
│                                     [取消]  [确认调拨]      │
└─────────────────────────────────────────────────────────────┘
```

**展开资产列表：**

```
┌─────────────────────────────────────────────────────────────┐
│ 📦  已选择 3 件资产                               [收起]   │
│     联想电脑 2件、惠普打印机 1件                            │
├─────────────────────────────────────────────────────────────┤
│ ┌─────────┐ ┌─────────┐ ┌─────────┐                        │
│ │ZC-BG-001│ │ZC-BG-002│ │ZC-BG-003│                        │
│ │联想电脑  │ │联想电脑  │ │惠普打印机│                        │
│ │总务处仓库│ │总务处仓库│ │总务处仓库│                        │
│ │   [×]   │ │   [×]   │ │   [×]   │                        │
│ └─────────┘ └─────────┘ └─────────┘                        │
└─────────────────────────────────────────────────────────────┘
```

---

### 3.3 Excel导入导出

#### 3.3.1 导入模板

```
| 资产名称* | 分类编码* | 品牌 | 型号 | 数量 | 单价 | 购置日期 | 保修期至 | 供应商 | 存放位置 | 责任人 | 备注 |
|----------|----------|------|------|------|------|----------|----------|--------|----------|--------|------|
| 联想电脑  | BG-JS    | 联想 | E14  | 10   | 5000 | 2026-01-25 | 2029-01-25 | 联想商城 | 总务处仓库 | 张三 | |
```

#### 3.3.2 后端设计

```java
// AssetExportController.java
@Tag(name = "资产导入导出")
@RestController
@RequestMapping("/v2/assets/export")
@RequiredArgsConstructor
public class AssetExportController {

    @Operation(summary = "下载导入模板")
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response);

    @Operation(summary = "导入资产")
    @PostMapping("/import")
    @PreAuthorize("hasAuthority('asset:manage')")
    public ApiResponse<ImportResult> importAssets(@RequestParam("file") MultipartFile file);

    @Operation(summary = "导出资产列表")
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('asset:list')")
    public void exportAssets(
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) Integer status,
        @RequestParam(required = false) String keyword,
        HttpServletResponse response
    );
}

// ImportResult.java
@Data
public class ImportResult {
    private Integer totalCount;       // 总行数
    private Integer successCount;     // 成功数
    private Integer failCount;        // 失败数
    private List<ImportError> errors; // 错误明细
}

@Data
public class ImportError {
    private Integer rowNum;           // 行号
    private String assetName;         // 资产名称
    private String errorMessage;      // 错误原因
}
```

#### 3.3.3 前端设计

**导入对话框：**

```
┌─────────────────────────────────────────────────────────────┐
│  导入资产                                            [×]    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 下载模板                                                │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ 📥  下载Excel导入模板                                │   │
│  │     包含必填字段说明和示例数据                        │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  2. 上传文件                                                │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                                                      │   │
│  │     📄 点击或拖拽文件到此处上传                       │   │
│  │        支持 .xlsx, .xls 格式，最大 5MB               │   │
│  │                                                      │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ⚠️ 导入说明：                                              │
│  • 资产名称、分类编码为必填项                               │
│  • 分类编码需与系统中的分类编码一致                         │
│  • 单次最多导入 1000 条记录                                │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│                                     [取消]  [开始导入]      │
└─────────────────────────────────────────────────────────────┘
```

**导入结果：**

```
┌─────────────────────────────────────────────────────────────┐
│  导入结果                                            [×]    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ ✅ 导入完成                                          │   │
│  │    成功 98 条 / 失败 2 条 / 共 100 条               │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  失败记录：                                                 │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ 第3行 | 联想电脑A | 分类编码 "ABC" 不存在            │   │
│  │ 第7行 | 惠普打印机 | 数量必须大于0                   │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  📥 下载失败记录                                            │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│                                              [完成]         │
└─────────────────────────────────────────────────────────────┘
```

---

### 3.4 资产领用/借用

#### 3.4.1 业务场景

```
场景1 - 借用（需归还）：
  借用人：王老师
  借用资产：投影仪 ZC-DZ-001
  借用原因：部门会议使用
  预计归还：3天后
  状态流转：空闲 → 借出中 → 归还后恢复空闲

场景2 - 领用（消耗性）：
  领用人：李秘书
  领用资产：打印纸 10包
  领用原因：办公室日常使用
  无需归还
```

#### 3.4.2 数据模型

```sql
CREATE TABLE asset_borrow (
    id BIGINT PRIMARY KEY,
    borrow_no VARCHAR(32) NOT NULL UNIQUE COMMENT '借用单号',
    borrow_type TINYINT NOT NULL COMMENT '类型: 1领用 2借用',

    -- 资产信息
    asset_id BIGINT NOT NULL COMMENT '资产ID',
    asset_code VARCHAR(50) NOT NULL COMMENT '资产编号',
    asset_name VARCHAR(200) NOT NULL COMMENT '资产名称',
    quantity INT NOT NULL DEFAULT 1 COMMENT '借用数量',

    -- 借用人信息
    borrower_id BIGINT NOT NULL COMMENT '借用人ID',
    borrower_name VARCHAR(50) NOT NULL COMMENT '借用人姓名',
    borrower_dept VARCHAR(100) COMMENT '借用人部门',

    -- 时间信息
    borrow_date DATETIME NOT NULL COMMENT '借出日期',
    expected_return_date DATE COMMENT '预计归还日期（借用必填）',
    actual_return_date DATETIME COMMENT '实际归还日期',

    -- 归还信息
    return_condition VARCHAR(200) COMMENT '归还状况',
    return_remark VARCHAR(500) COMMENT '归还备注',

    -- 申请信息
    purpose VARCHAR(500) COMMENT '借用原因',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1借出中 2已归还 3已逾期 4已取消',

    -- 审批信息（可选）
    need_approval TINYINT DEFAULT 0 COMMENT '是否需要审批',
    approval_status TINYINT COMMENT '审批状态',
    process_instance_id VARCHAR(64) COMMENT '流程实例ID',

    -- 操作信息
    operator_id BIGINT NOT NULL COMMENT '经办人ID',
    operator_name VARCHAR(50) NOT NULL COMMENT '经办人姓名',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,

    INDEX idx_borrow_no (borrow_no),
    INDEX idx_asset_id (asset_id),
    INDEX idx_borrower_id (borrower_id),
    INDEX idx_status (status),
    INDEX idx_expected_return (expected_return_date)
) COMMENT '资产借用记录';
```

#### 3.4.3 前端设计

**借用申请对话框：**

```
┌─────────────────────────────────────────────────────────────┐
│  资产借用                                            [×]    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  借用类型                                                   │
│  ┌─────────────────────┐ ┌─────────────────────┐           │
│  │ ○ 领用（无需归还）   │ │ ● 借用（需要归还）   │           │
│  │    适用于消耗品      │ │    适用于设备借用    │           │
│  └─────────────────────┘ └─────────────────────┘           │
│                                                             │
│  ━━ 资产信息 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ 📦 ZC-DZ-001 | 爱普生投影仪 | 当前位置：会议室A      │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ━━ 借用信息 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   │
│                                                             │
│  借用人 *            [王老师 (教务处)              👤]      │
│                                                             │
│  预计归还日期 *      [2026-01-28                    📅]     │
│                                                             │
│  借用原因 *                                                 │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ 部门月度会议使用                                     │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│                                     [取消]  [确认借出]      │
└─────────────────────────────────────────────────────────────┘
```

**借用记录列表：**

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  资产借用记录                                                                │
├─────────────────────────────────────────────────────────────────────────────┤
│  [借出中] [已归还] [已逾期] [全部]          🔍 搜索借用人/资产...   [新建借用] │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ JY-2026012500001                                            借出中   │   │
│  │ 📦 爱普生投影仪 (ZC-DZ-001)                                          │   │
│  │ 👤 王老师 (教务处)     📅 预计归还：2026-01-28 (还剩3天)              │   │
│  │                                                    [归还]  [详情]    │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ JY-2026012400001                                            ⚠️ 已逾期 │   │
│  │ 📦 佳能相机 (ZC-SY-002)                                              │   │
│  │ 👤 李记者 (宣传部)     📅 应归还：2026-01-20 (已逾期5天)              │   │
│  │                                                    [催还]  [归还]    │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

### 3.5 调拨审批流程

#### 3.5.1 流程设计

```
┌────────┐    ┌──────────────┐    ┌──────────────┐    ┌────────┐
│  申请   │ → │ 部门负责人审批 │ → │ 资产管理员确认 │ → │  执行   │
└────────┘    └──────────────┘    └──────────────┘    └────────┘
                    │                    │
                    ↓                    ↓
               ┌────────┐           ┌────────┐
               │  驳回   │           │  驳回   │
               └────────┘           └────────┘
```

#### 3.5.2 使用现有工作流

```java
// 创建调拨审批流程模板
WorkflowTemplate template = new WorkflowTemplate();
template.setTemplateName("资产调拨审批");
template.setTemplateCode("ASSET_TRANSFER_APPROVAL");
template.setTemplateType("ASSET");
template.setBpmnXml(generateTransferApprovalBpmn());
template.setNodeConfig(Map.of(
    "deptApproval", Map.of(
        "type", "DEPT_LEADER",  // 部门负责人
        "name", "部门审批"
    ),
    "assetApproval", Map.of(
        "type", "ROLE",
        "roleCode", "ASSET_MANAGER",  // 资产管理员角色
        "name", "资产管理员确认"
    )
));
```

#### 3.5.3 审批页面

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  调拨审批详情                                                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  调拨单号：DB-2026012500001                                状态：待审批     │
│  申请人：张三 (总务处)                                     申请时间：今天    │
│                                                                             │
│  ━━ 调拨信息 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   │
│                                                                             │
│  ┌───────────────────────┐  →  ┌───────────────────────┐                   │
│  │ 总务处仓库             │     │ 图书馆三楼阅览室       │                   │
│  │ 责任人：张三           │     │ 责任人：李馆长         │                   │
│  └───────────────────────┘     └───────────────────────┘                   │
│                                                                             │
│  调拨原因：图书馆设备升级配置                                                │
│                                                                             │
│  ━━ 调拨资产 (3件，共 ¥15,000) ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ 编号          │ 名称           │ 分类      │ 原值      │ 原位置     │   │
│  ├─────────────────────────────────────────────────────────────────────┤   │
│  │ ZC-BG-JS-001 │ 联想电脑       │ 计算机    │ ¥5,000   │ 总务处仓库  │   │
│  │ ZC-BG-JS-002 │ 联想电脑       │ 计算机    │ ¥5,000   │ 总务处仓库  │   │
│  │ ZC-BG-DY-001 │ 惠普打印机     │ 打印设备  │ ¥5,000   │ 总务处仓库  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ━━ 审批流程 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   │
│                                                                             │
│  ● 申请提交 ────────●────────── ○ 部门审批 ────────── ○ 资产管理员 ─── ○ 完成 │
│    张三               当前节点      待审批               待审批             │
│    01-25 10:30                                                              │
│                                                                             │
│  ━━ 审批意见 ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ 请输入审批意见...                                                    │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                          [驳回]  [转交]  [通过]             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

### 3.6 折旧自动计算

#### 3.6.1 折旧方法

采用 **直线法（平均年限法）**：

```
月折旧额 = (原值 - 残值) / (折旧年限 × 12)
残值 = 原值 × 残值率（默认5%）
累计折旧 = 月折旧额 × 已使用月数
净值 = 原值 - 累计折旧
```

#### 3.6.2 实现方案

```java
// 定时任务：每月1日凌晨计算折旧
@Scheduled(cron = "0 0 1 1 * ?")
public void calculateMonthlyDepreciation() {
    // 查询所有在用且未折旧完毕的资产
    List<Asset> assets = assetRepository.findDepreciableAssets();

    for (Asset asset : assets) {
        AssetCategory category = asset.getCategory();
        if (category.getDepreciationYears() == null || category.getDepreciationYears() <= 0) {
            continue;
        }

        // 计算月折旧额
        BigDecimal residualRate = new BigDecimal("0.05");  // 残值率5%
        BigDecimal residualValue = asset.getOriginalValue().multiply(residualRate);
        BigDecimal depreciableValue = asset.getOriginalValue().subtract(residualValue);
        BigDecimal monthlyDepreciation = depreciableValue
            .divide(new BigDecimal(category.getDepreciationYears() * 12), 2, RoundingMode.HALF_UP);

        // 更新净值
        BigDecimal newNetValue = asset.getNetValue().subtract(monthlyDepreciation);
        if (newNetValue.compareTo(residualValue) < 0) {
            newNetValue = residualValue;  // 净值不能低于残值
        }

        asset.updateNetValue(newNetValue);
        assetRepository.save(asset);

        // 记录折旧历史
        depreciationHistoryRepository.save(new DepreciationHistory(
            asset.getId(),
            monthlyDepreciation,
            newNetValue,
            LocalDate.now()
        ));
    }
}
```

---

### 3.7 盘点流程完善

#### 3.7.1 盘点流程

```
创建盘点计划 → 生成盘点清单 → 执行盘点 → 处理盘差 → 完成盘点
     ↓              ↓            ↓           ↓
   选择范围      自动生成      逐件核对    盘盈入账
  （分类/位置）               标记差异    盘亏报损
```

#### 3.7.2 前端设计

**盘点计划创建：**

```
┌─────────────────────────────────────────────────────────────┐
│  创建盘点计划                                        [×]    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  盘点名称 *        [2026年度固定资产盘点              ]      │
│                                                             │
│  盘点范围 *                                                 │
│  ○ 全部资产                                                 │
│  ● 按分类  [办公设备 ▼] [计算机、打印设备 ▼]               │
│  ○ 按位置  [选择楼栋/楼层 ▼]                               │
│  ○ 按部门  [选择部门 ▼]                                    │
│                                                             │
│  盘点人员 *        [张三, 李四, 王五             👤]        │
│                                                             │
│  计划开始日期      [2026-01-25                    📅]       │
│  计划结束日期      [2026-01-31                    📅]       │
│                                                             │
│  备注                                                       │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ 年度固定资产例行盘点                                  │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│  预计盘点资产：128 件                                       │
│                                     [取消]  [创建计划]      │
└─────────────────────────────────────────────────────────────┘
```

**盘点执行：**

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  盘点执行 - 2026年度固定资产盘点                              进度: 45/128   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  [未盘点] [已盘点] [有差异] [全部]      🔍 搜索编号/名称...      [扫码盘点]  │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ ☐ ZC-BG-JS-001 | 联想电脑 | 图书馆三楼阅览室 | 李馆长              │   │
│  │                                                                      │   │
│  │   应有: 1件    实盘: [    ] 件    状态: [正常 ▼]    备注: [      ]  │   │
│  │                                                       [确认盘点]    │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ ☑ ZC-BG-JS-002 | 联想电脑 | 图书馆三楼阅览室 | 李馆长    ✓ 已盘点  │   │
│  │   应有: 1件    实盘: 1件    状态: 正常                              │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ ☑ ZC-BG-DY-001 | 惠普打印机 | 总务处仓库 | 张三         ⚠️ 盘亏    │   │
│  │   应有: 1件    实盘: 0件    状态: 盘亏    备注: 设备丢失待查        │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│  已盘点: 45件   正常: 43件   盘盈: 0件   盘亏: 2件                          │
│                                                        [暂存]  [完成盘点]  │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

### 3.8 条码/二维码

#### 3.8.1 标签设计

```
┌─────────────────────────────────────┐
│ ████████████████████████████████████│
│ ██                              ██ │
│ ██   [二维码]                    ██ │
│ ██                              ██ │
│ ████████████████████████████████████│
│                                     │
│ ZC-BG-JS-000001                     │
│ 联想ThinkPad E14笔记本电脑           │
│ 图书馆三楼阅览室                     │
│                                     │
│ |||||||||||||||||||||||||||||||||||  │
│       (条形码)                       │
└─────────────────────────────────────┘
```

#### 3.8.2 实现方案

```javascript
// 前端生成二维码
import QRCode from 'qrcode'

async function generateAssetQRCode(asset) {
  // 二维码内容：资产详情页URL
  const url = `${window.location.origin}/asset/${asset.id}`
  return await QRCode.toDataURL(url, {
    width: 150,
    margin: 1
  })
}

// 打印标签
function printAssetLabel(assets) {
  const printWindow = window.open('', '_blank')
  printWindow.document.write(`
    <html>
      <head>
        <title>资产标签打印</title>
        <style>
          .label { width: 5cm; height: 3cm; border: 1px solid #000; margin: 2mm; }
          .qrcode { width: 2cm; height: 2cm; }
          /* ... */
        </style>
      </head>
      <body>
        ${assets.map(asset => generateLabelHTML(asset)).join('')}
      </body>
    </html>
  `)
  printWindow.print()
}
```

---

### 3.9 预警机制

#### 3.9.1 预警类型

| 预警类型 | 触发条件 | 通知方式 |
|---------|---------|---------|
| 保修到期 | 保修期 ≤ 30天 | 系统消息 + 待办 |
| 借用逾期 | 已超预计归还日期 | 系统消息 + 邮件 |
| 盘点差异 | 存在盘亏记录 | 系统消息 |
| 维修超时 | 维修中 ≥ 30天 | 系统消息 |

#### 3.9.2 定时任务

```java
@Component
@RequiredArgsConstructor
public class AssetAlertTask {

    @Scheduled(cron = "0 0 8 * * ?")  // 每天8点执行
    public void checkWarrantyExpiring() {
        // 查询30天内保修到期的资产
        LocalDate threshold = LocalDate.now().plusDays(30);
        List<Asset> expiringAssets = assetRepository.findByWarrantyDateBefore(threshold);

        for (Asset asset : expiringAssets) {
            systemMessageService.sendAlert(
                asset.getResponsibleUserId(),
                "保修到期提醒",
                String.format("资产 %s（%s）将于 %s 保修到期，请及时处理。",
                    asset.getAssetName(), asset.getAssetCode(), asset.getWarrantyDate())
            );
        }
    }

    @Scheduled(cron = "0 0 9 * * ?")  // 每天9点执行
    public void checkBorrowOverdue() {
        // 查询已逾期的借用记录
        List<AssetBorrow> overdueRecords = borrowRepository.findOverdue();

        for (AssetBorrow record : overdueRecords) {
            // 更新状态为逾期
            record.markAsOverdue();
            borrowRepository.save(record);

            // 发送催还通知
            systemMessageService.sendAlert(
                record.getBorrowerId(),
                "资产催还通知",
                String.format("您借用的 %s（%s）已逾期 %d 天，请尽快归还。",
                    record.getAssetName(), record.getAssetCode(),
                    ChronoUnit.DAYS.between(record.getExpectedReturnDate(), LocalDate.now()))
            );
        }
    }
}
```

---

### 3.10 报表导出

#### 3.10.1 报表类型

| 报表名称 | 内容 | 格式 |
|---------|------|------|
| 资产台账 | 完整资产清单 | Excel |
| 分类汇总 | 按分类统计数量和价值 | Excel/PDF |
| 部门资产 | 按部门/位置统计 | Excel |
| 调拨记录 | 调拨单明细 | Excel |
| 折旧明细 | 月度折旧记录 | Excel |
| 盘点报告 | 盘点结果汇总 | PDF |

#### 3.10.2 导出API

```java
@Tag(name = "资产报表")
@RestController
@RequestMapping("/v2/asset-reports")
public class AssetReportController {

    @GetMapping("/ledger")
    public void exportLedger(AssetQueryCriteria criteria, HttpServletResponse response);

    @GetMapping("/category-summary")
    public void exportCategorySummary(HttpServletResponse response);

    @GetMapping("/department-assets")
    public void exportDepartmentAssets(@RequestParam Long orgUnitId, HttpServletResponse response);

    @GetMapping("/transfer-records")
    public void exportTransferRecords(
        @RequestParam LocalDate startDate,
        @RequestParam LocalDate endDate,
        HttpServletResponse response
    );

    @GetMapping("/inventory-report/{inventoryId}")
    public void exportInventoryReport(@PathVariable Long inventoryId, HttpServletResponse response);
}
```

---

## 四、实施时间表

```
2026年1-2月
├── Week 1: 批量入库
│   ├── Day 1-2: 后端API开发
│   └── Day 3-4: 前端对话框开发
│
├── Week 2: 批量调拨
│   ├── Day 1: 数据模型设计
│   ├── Day 2-3: 后端服务开发
│   └── Day 4-5: 前端多选+调拨对话框
│
├── Week 3: Excel导入导出
│   ├── Day 1-2: 后端导入导出服务
│   └── Day 3: 前端导入对话框
│
├── Week 4: 资产借用
│   ├── Day 1: 数据模型
│   ├── Day 2-3: 后端服务
│   └── Day 4-5: 前端页面
│
├── Week 5: 调拨审批流程
│   ├── Day 1-2: 流程模板配置
│   ├── Day 2-3: 审批服务集成
│   └── Day 4: 审批页面
│
├── Week 6: 折旧+盘点
│   ├── Day 1-2: 折旧计算
│   └── Day 3-5: 盘点流程完善
│
└── Week 7-8: 优化功能
    ├── 条码/二维码
    ├── 预警机制
    └── 报表导出
```

---

## 五、风险与注意事项

### 5.1 技术风险

| 风险 | 影响 | 缓解措施 |
|-----|------|---------|
| 批量操作性能 | 大量数据处理慢 | 分批处理，异步执行 |
| 工作流复杂度 | 审批逻辑复杂 | 复用现有框架，简化流程 |
| 数据一致性 | 调拨/借用并发问题 | 使用乐观锁，事务控制 |

### 5.2 兼容性

- 现有资产数据保持不变
- 新增表不影响现有功能
- API采用V2版本，与现有V1隔离

### 5.3 权限控制

新增权限点：

```
asset:batch          # 批量操作权限
asset:transfer:apply # 调拨申请权限
asset:transfer:approve # 调拨审批权限
asset:borrow         # 借用管理权限
asset:inventory      # 盘点权限
asset:report         # 报表导出权限
```

---

## 六、附录

### A. 文件清单

**后端新增文件：**
```
interfaces/rest/asset/
├── AssetTransferController.java
├── AssetBorrowController.java
├── AssetExportController.java
├── AssetReportController.java
└── dto/
    ├── BatchCreateAssetRequest.java
    ├── CreateTransferOrderRequest.java
    ├── CreateBorrowRequest.java
    └── ...

domain/asset/model/aggregate/
├── TransferOrder.java
└── AssetBorrow.java

application/asset/
├── AssetTransferApplicationService.java
├── AssetBorrowApplicationService.java
└── command/
    ├── BatchCreateAssetCommand.java
    └── ...
```

**前端新增文件：**
```
views/asset/components/
├── AssetBatchCreateDialog.vue
├── AssetBatchTransferDialog.vue
├── AssetImportDialog.vue
├── AssetBorrowDialog.vue
├── AssetBorrowReturnDialog.vue
└── AssetLabelPrint.vue

views/asset/
├── AssetBorrowList.vue
├── AssetTransferList.vue
├── AssetInventoryView.vue
└── AssetReportView.vue

api/v2/
├── assetTransfer.ts
├── assetBorrow.ts
└── assetReport.ts
```

### B. 接口清单

| 模块 | 方法 | 路径 | 说明 |
|-----|------|------|------|
| 批量入库 | POST | /v2/assets/batch | 批量创建资产 |
| 调拨管理 | POST | /v2/asset-transfers | 创建调拨单 |
| | GET | /v2/asset-transfers | 调拨单列表 |
| | GET | /v2/asset-transfers/{id} | 调拨单详情 |
| | POST | /v2/asset-transfers/{id}/submit | 提交审批 |
| | POST | /v2/asset-transfers/{id}/execute | 执行调拨 |
| 借用管理 | POST | /v2/asset-borrows | 创建借用 |
| | GET | /v2/asset-borrows | 借用列表 |
| | POST | /v2/asset-borrows/{id}/return | 归还资产 |
| 导入导出 | GET | /v2/assets/export/template | 下载模板 |
| | POST | /v2/assets/export/import | 导入资产 |
| | GET | /v2/assets/export/export | 导出资产 |
| 报表 | GET | /v2/asset-reports/ledger | 资产台账 |
| | GET | /v2/asset-reports/category-summary | 分类汇总 |
