# 数据范围编辑器 → 纯关系化 实现规范 (2026-06-22)

> 用户决策: 取消"单一/多锚点"切换 + 删除"仅本人/本组织"预设, **全部统一为"经[关系]"选定**
> (连 创建者/全部 也当关系)。多条件 OR + (将来) 排除/AND。本规范定义实现 + 边界 + 风险。

## 语义澄清 (已答用户)
- **本组织** = "和我有 **成员(member)** 关系的组织" (主体 member 关系所在 org)。
- **仅本人** = "我 **创建(creator)** 的记录" (列资源) / 成员自身 (主体资源)。语义重载, 该删。
- 二者底层就是关系 → 统一为关系选定是对的 (ReBAC 内核, 后端 grant 模型已支持)。

## 目标编辑器形态 (纯关系 OR)
每个资源 = 一组"条件", **OR** (满足任一):
```
显示范围 = 满足任一条:
  经 [创建者 ▾]                    [×]
  经 [成员 ▾] 关系的组织  ☑含下级   [×]
  经 [管理 ▾] 关系的组织            [×]
  [+ 添加条件]
```
- 取消 单一(ScopeBuilder)/多锚点 切换 → **只剩这一个编辑器**, 每资源恒用之。
- 删 "仅本人/本组织/全部" 预设标签 → 并入关系下拉。

## 关系下拉来源 (统一一个下拉)
| 类别 | 来源 | 示例 |
|---|---|---|
| 特殊 | 固定 | 创建者 / 全部(不限) |
| 用户↔组织关系 | relationTypeApi (toType=ORG_UNIT) | 成员/管理/副管理/责任/查阅/关注/任课 |
| 资源关系 (PROVIDER/RECORD) | resource-relations 端点 | 复核/受检/任课老师(由插件解析) |
| 插件维度 (仅保全已存) | 载入时若 grant 含 PLUGIN_DIM | 维度: 班级(BY_CLASS) |

## UI 选项 ↔ grant 映射 (核心, 勿错)
| UI 关系 | grant |
|---|---|
| 创建者 | `{relation:'creator', subject:'SELF'}` |
| 全部 | `{relation:'owner_org', subject:'ALL'}` |
| 成员/管理/…(org 关系 X) | `{relation:'owner_org', subject:'RELATION', subjectParam:X, subtree}` |
| 复核/受检/…(资源 PROVIDER/RECORD 关系 R) | `{relation:R, subject:'SELF'}` |
| 维度 X (PLUGIN_DIM, 仅保全) | `{relation:'owner_org', subject:'PLUGIN_DIM', subjectParam:X}` |

**反向 (载入 grant → UI), 含 legacy 迁移**:
- `{creator,*}`→创建者 / `{owner_org,ALL}`→全部 / `{owner_org,MY_ORG}`→**成员**(legacy MY_ORG 映射) /
  `{owner_org,RELATION,X}`→X / `{owner_org,PLUGIN_DIM,d}`→维度 d / `{R,SELF}`(R≠creator/owner_org)→资源关系 R。

## ⚠ 风险与边界 (实现必守)
1. **PLUGIN_DIM 保全 = 金标准红线**: 班主任 student=BY_CLASS(PLUGIN_DIM)。载入须识别并以"维度"选项保留,
   保存原样回写 `{owner_org,PLUGIN_DIM,BY_CLASS}`。**实现后必 dpt_ct 金标准回归 (载入班主任→保存→student 仍 BY_CLASS→查仍 2 本班生)**。
2. **OR 可做, AND/排除组织不可做**: grant 模型是 OR (并集)。"同时满足两关系(AND)"/"排除和我有X关系的组织(NOT)"
   = **关系代数 (Zanzibar 重写引擎)**, 引擎级大工程 (S/S+), 不在本前端范围。本期只做 OR。
   (现 axis② subjectRelExclude 是"排除有某关系的**人**", 非"排除**组织**", 语义不同, 不混用。)
3. **后端零改**: 目标 grant 全是既有 `relation_grants` 形态, 后端已支持。纯前端 + 映射。

## 实现步骤
1. MultiGrantEditor 重做: 单"关系"下拉(上述来源)+ 含下级(org 关系才显)+ 增删行; 内部做 UI↔grant 映射。
2. DataScopeStudio: 每资源恒用 MultiGrantEditor (删 ScopeBuilder + convertToMulti/simplifyToSingle 切换);
   specByCode 存 relationGrants; 载入把 单 spec/legacy 转 grants; 保存 grants→command。
3. ScopeBuilder.vue: Studio 不再用 (其他消费方? 确认后决定删/留)。
4. 测试: MultiGrantEditor 映射往返单测 (含 PLUGIN_DIM 保全) + DataScopeStudio 单测改 + type-check。
5. **真库**: dpt_ct 金标准回归 (红线#1) + 浏览器实拍。

## 工作量
中等偏大 (映射多分支 + 第 3 次编辑器重构 + 金标准风险)。**应作独立专注会话**, 不在长会话尾仓促做 —— 否则易腐蚀金标准。
