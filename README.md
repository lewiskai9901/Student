# 学生管理系统

基于 Spring Boot 3.2 + Vue.js 3.4 + TypeScript 的现代化学生管理系统，采用 DDD 六边形架构，支持学生信息管理、V4检查系统（自动排程/分析/导出）、资产管理、宿舍管理等功能。

## 📋 目录

- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [核心功能](#核心功能)
- [快速开始](#快速开始)
- [开发文档](#开发文档)
- [项目状态](#项目状态)

## 🛠 技术栈

### 后端技术
- **框架**: Spring Boot 3.2+
- **架构**: DDD (Domain-Driven Design) 六边形架构
- **数据库**: MySQL 8.0+
- **ORM**: MyBatis Plus 3.5+
- **安全认证**: Spring Security + JWT
- **缓存**: Redis
- **文档**: Swagger 3.0 (OpenAPI)
- **构建工具**: Maven
- **Java版本**: JDK 17+

### 前端技术
- **框架**: Vue.js 3.4+
- **语言**: TypeScript
- **UI组件**: Element Plus
- **状态管理**: Pinia
- **路由**: Vue Router 4
- **HTTP客户端**: Axios
- **构建工具**: Vite

### 小程序技术
- **框架**: 微信小程序原生开发
- **UI组件**: Vant Weapp

## 📁 项目结构

```
学生管理系统/
├── backend/                    # 后端项目 (Spring Boot DDD架构)
├── frontend/                   # 前端项目 (Vue 3 + TypeScript)
├── database/                   # 数据库脚本
│   ├── schema/                 # 表结构 (complete_schema_v2.sql)
│   └── init/                   # 初始化数据
├── docs/                       # 项目文档
│   ├── design/                 # 设计文档
│   ├── features/               # 功能文档
│   ├── plans/                  # 历史规划文档
│   ├── architecture/           # 架构文档
│   └── standards/              # 编码标准
├── scripts/                    # 工具脚本
└── README.md                   # 本文档
```

## 🏗️ 系统架构 (DDD)

项目采用 **DDD 六边形架构**，所有代码遵循领域驱动设计原则。

### 后端架构
```
com.school.management/
├── domain/                    # 领域层 - 核心业务逻辑
│   ├── organization/          # 组织管理领域 (OrgUnit, SchoolClass, Student)
│   ├── inspection/            # 量化检查领域 (Template, Session, Record, Appeal)
│   ├── access/                # 权限管理领域 (Permission, Role, DataPermission)
│   ├── asset/                 # 资产管理领域
│   └── shared/                # 共享内核 (Entity, AggregateRoot, ValueObject)
├── application/              # 应用层 - 用例编排
│   ├── organization/         # 组织管理应用服务
│   ├── inspection/           # 检查应用服务 (saga/, export/)
│   ├── access/               # 权限应用服务
│   └── asset/                # 资产应用服务
├── infrastructure/           # 基础设施层 - 技术实现
│   ├── persistence/          # 持久化 (Repository实现, Mapper, PO)
│   ├── cache/                # Redis缓存
│   ├── event/                # 领域事件发布
│   └── audit/                # 审计日志
└── interfaces/               # 接口层 - REST控制器
    └── rest/                 # REST API端点
```

### 核心API端点
| 领域 | 端点 | 说明 |
|------|------|------|
| 组织管理 | `/api/org-units` | 组织单元管理（部门、系部等） |
| 组织管理 | `/api/organization/classes` | 班级管理 |
| 组织管理 | `/api/students` | 学生管理 |
| 权限管理 | `/api/permissions` | 权限管理 |
| 权限管理 | `/api/roles` | 角色管理 |
| 量化检查 | `/api/inspection-templates` | 检查模板管理 |
| 量化检查 | `/api/inspection-sessions` | 检查会话管理 |
| 量化检查 | `/api/export-center` | 数据导出中心 |
| 资产管理 | `/api/assets` | 资产管理 |

### 前端结构
```
frontend/src/
├── api/                 # API模块 (organization.ts, inspection.ts, access.ts)
├── types/               # TypeScript类型定义
├── stores/              # Pinia状态管理
├── views/               # 视图组件
│   ├── organization/    # 组织管理视图
│   ├── inspection/      # 检查管理视图
│   ├── access/          # 权限管理视图
│   └── asset/           # 资产管理视图
├── components/          # 可复用UI组件
├── layouts/             # 布局组件
├── router/              # Vue Router (权限守卫)
└── composables/         # 组合式函数
```

### 前端路由
- `/organization/units` - 组织架构管理
- `/organization/classes` - 班级管理
- `/organization/students` - 学生管理
- `/inspection/config` - 量化配置
- `/inspection/plans` - 检查计划
- `/inspection/appeals` - 申诉管理
- `/access/users` - 用户管理
- `/access/roles` - 角色管理
- `/access/permissions` - 权限管理

## ✨ 核心功能

### 1. 用户权限管理
- ✅ 用户管理（增删改查、状态管理、批量操作）
- ✅ 角色管理（角色分配、权限配置）
- ✅ 权限管理（细粒度权限控制、动态路由）
- ✅ JWT认证（访问令牌+刷新令牌）
- ✅ 令牌黑名单（安全退出登录）
- ✅ 部门管理（树形结构、层级管理）

### 2. 学生信息管理
- ✅ 学生档案管理（基本信息、联系方式）
- ✅ 学生分页查询和高级筛选
- ✅ 学生状态管理（在校、休学、毕业等）
- ✅ 批量导入导出
- ✅ 学生搜索（支持姓名、学号）

### 3. 组织架构管理
- ✅ 部门管理（树形结构、层级管理、部门负责人）
- ✅ 班级管理（班主任分配、学生统计）
- ✅ 宿舍管理（床位分配、入住管理）
- ✅ 教室管理（教室分配、容量管理）

### 4. V4 量化检查系统
- ✅ **检查模板配置**
  - 检查分类管理（宿舍、教室、纪律、卫生等）
  - 扣分项配置（固定扣分、人次扣分、区间扣分）
  - 加分项配置（固定加分、递进加分）

- ✅ **会话式检查管理**
  - 检查会话创建与发布
  - 班级检查记录（扣分明细、加分记录）
  - 检查清单响应

- ✅ **Saga编排**
  - 检查完成自动触发评级计算
  - 评级完成自动触发通知

- ✅ **自动排程**
  - 排程策略配置（轮换算法、检查员池）
  - 自动执行与会话创建

- ✅ **数据分析**
  - 班级排名与部门排名
  - 分析快照（按日/周/月）
  - 趋势分析

- ✅ **数据导出中心**
  - 扣分明细导出
  - 评级报表导出
  - 统计报表导出

- ✅ **申诉管理**
  - 两级审核流程
  - 申诉记录追踪

- ✅ **纠正行动**
  - 自动规则触发
  - 行动跟踪与验证

- ✅ **学生行为**
  - 行为记录追踪
  - 行为预警系统

### 5. 资产管理
- ✅ 资产台账（编码、分类、位置）
- ✅ 资产借用与归还
- ✅ 资产盘点（盘盈/盘亏）
- ✅ 资产折旧管理
- ✅ 资产维修记录
- ✅ 资产审批流程
- ✅ 资产预警

### 6. 系统功能
- ✅ 操作日志与审计日志
- ✅ 异常处理机制（领域异常分类）
- ✅ 参数验证框架
- ✅ 统一响应格式
- ✅ Redis缓存（模板/排名/分析）
- ✅ 系统消息通知

## 🚀 快速开始

### 环境要求
- JDK 17+
- MySQL 8.0+
- Node.js 18+
- Maven 3.6+

### 1. 数据库初始化

```bash
# 创建数据库
mysql -u root -p
CREATE DATABASE student_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 执行初始化脚本
mysql -u root -p student_management < database/schema/complete_schema_v2.sql
mysql -u root -p student_management < database/init/init_data.sql
```

### 2. 后端启动

```bash
cd backend

# 修改 application.yml 配置数据库连接信息

# 启动后端服务
mvn spring-boot:run
# 或使用启动脚本
./start-backend.sh   # Linux/Mac
start-backend.bat    # Windows
```

后端服务将在 `http://localhost:8080` 启动

### 3. 前端启动

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
# 或使用启动脚本
./start-frontend.sh  # Linux/Mac
start-frontend.bat   # Windows
```

前端服务将在 `http://localhost:3000` 启动

### 4. 默认账号

```
超级管理员账号:
用户名: admin
密码: admin123

普通管理员账号:
用户名: teacher01
密码: 123456
```

## 📚 开发文档

### 设计文档 (`docs/design/`)
- [V4检查系统综合设计](./docs/design/quantification-v4-comprehensive-redesign.md) - V4检查架构
- [数据库设计](./docs/design/database-v2-ddd.md) - DDD数据库设计
- [统计分析设计](./docs/design/statistics-analysis-redesign.md) - 统计分析架构

### 架构文档 (`docs/architecture/`)
- [数据权限V4](./docs/architecture/data-permission-v4.md) - 数据权限架构
- [DDD前端架构](./docs/architecture/DDD_FRONTEND_ARCHITECTURE.md) - 前端DDD对齐

### 标准文档 (`docs/standards/`)
- [API命名规范](./docs/standards/api-naming.md) - API接口命名标准
- [编码风格](./docs/standards/coding-style.md) - 代码规范
- [DDD分层标准](./docs/standards/ddd-layers.md) - 领域驱动分层规范

### 功能文档 (`docs/features/`)
- [导出中心设计](./docs/features/export-center-design.md) - 数据导出中心
- [检查计划集成设计](./docs/features/check-plan-integrated-system-design.md) - 检查计划系统

### API文档
- **Swagger UI**: `http://localhost:8080/api/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/api/v3/api-docs`

## 📊 项目状态

### 已完成功能 ✅

#### 基础功能
- [x] 用户权限管理系统（RBAC + 数据权限）
- [x] JWT双令牌认证机制
- [x] 学生信息管理
- [x] 组织架构管理（树形结构、层级关系）
- [x] 班级管理
- [x] 空间管理（宿舍、教室统一）
- [x] 年级/专业/专业方向管理

#### V4量化检查系统
- [x] 检查模板配置（分类、扣分项、加分项）
- [x] 会话式检查管理
- [x] Saga编排（自动评级+通知）
- [x] 自动排程（策略轮换）
- [x] 数据分析与快照
- [x] 导出中心（3种场景）
- [x] 申诉管理（两级审核）
- [x] 纠正行动与自动规则
- [x] 学生行为记录与预警

#### 资产管理
- [x] 资产台账与分类
- [x] 借用/归还/盘点/折旧/维修/审批/预警

### 最近更新 🔄
**2026年1月28日 - 全面DDD重构完成**
- ✅ 删除全部V1遗留代码（~730文件）
- ✅ 完成纯DDD六边形架构迁移
- ✅ 实现V4检查系统全部功能（Saga、排程、分析、导出、纠正行动、行为追踪）
- ✅ 前端结构扁平化（去除v2子目录）
- ✅ 完整数据库Schema重建（36张V4新增表）
- ✅ 421个单元测试全部通过
- ✅ 权重双模式（标准加权平均 + 分类权重）
- ✅ Redis缓存集成（模板/排名/分析）

### 进行中功能 🚧
- [ ] 小程序功能开发
- [ ] 移动端适配

## 🔧 技术特点

### 后端特点
- **模块化设计**: 清晰的分层架构，易于维护和扩展
- **安全可靠**: Spring Security + JWT保证系统安全
- **高性能**: 使用缓存机制提升系统性能
- **标准化**: RESTful API设计，统一响应格式
- **可观测**: 完善的日志记录和异常处理

### 前端特点
- **现代化**: Vue 3 Composition API + TypeScript
- **组件化**: 可复用的业务组件
- **类型安全**: 完整的TypeScript类型定义
- **用户友好**: Element Plus提供优质的UI体验
- **性能优化**: 路由懒加载、组件按需引入

## 📝 许可证

[MIT License](LICENSE)

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📮 联系方式

如有问题或建议，请通过以下方式联系：
- 提交 GitHub Issue
- 发送邮件至项目维护者

---

**版本**: v2.0.0-ddd-only
**更新时间**: 2026年1月28日
**开发状态**: 持续开发中 🚀
**架构**: 纯DDD六边形架构 ✅
