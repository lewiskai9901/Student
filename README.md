# 学生管理系统

基于 Spring Boot 3.2 + Vue.js 3.4 + TypeScript 的现代化学生管理系统，支持学生信息管理、量化检查2.0、班级宿舍管理等功能，并包含微信小程序端。

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
- **架构**: DDD (Domain-Driven Design) 六边形架构 (V2 API)
- **数据库**: MySQL 8.0+
- **ORM**: MyBatis Plus 3.5+
- **安全认证**: Spring Security + JWT + Casbin
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
├── backend/                    # 后端项目 (Spring Boot)
├── frontend/                   # 前端项目 (Vue 3 + TypeScript)
├── miniprogram/                # 微信小程序
├── docs/                       # 📚 项目文档（标准化组织）
│   ├── design/                 # 设计文档
│   │   ├── architecture.md             # 技术架构设计
│   │   ├── database-design.md          # 数据库设计
│   │   ├── api-specification.md        # API接口规范
│   │   ├── frontend-architecture.md    # 前端架构设计
│   │   ├── miniprogram-design.md       # 小程序设计
│   │   └── security-design.md          # 安全设计方案
│   ├── development/            # 开发文档
│   │   ├── coding-standards.md         # 开发规范
│   │   └── testing-guide.md            # 测试指南
│   ├── deployment/             # 部署文档
│   │   ├── deployment-basic.md         # 基础部署指南
│   │   ├── deployment-checklist.md     # 部署检查清单
│   │   └── database-backup.md          # 数据库备份方案
│   ├── api/                    # API文档
│   │   ├── swagger-guide.md            # Swagger使用指南
│   │   └── api-examples.md             # API调用示例
│   ├── optimization/           # 优化文档
│   │   ├── session-2-summary.md        # 优化会话2总结
│   │   ├── future-plans.md             # 未来优化计划
│   │   └── overall-summary.md          # 优化总览
│   └── features/               # 功能文档
│       ├── quantification-v2/          # 量化系统2.0
│       │   ├── README.md
│       │   ├── quickstart.md
│       │   ├── development-report.md
│       │   └── delivery-summary.md
│       └── miniprogram/                # 小程序功能
├── database/                   # 数据库脚本
│   ├── schema/                 # 表结构
│   ├── migrations/             # 迁移脚本
│   └── init/                   # 初始化数据
├── scripts/                    # 工具脚本
│   ├── backup-database.bat     # Windows数据库备份
│   ├── backup-database.sh      # Linux/Mac数据库备份
│   ├── start-backend.bat       # 启动后端（Windows）
│   └── start-frontend.bat      # 启动前端（Windows）
├── archive/                    # 历史文档归档
│   ├── 2024-Q4/                # 2024年第四季度归档
│   │   ├── 修复记录/
│   │   ├── 重构记录/
│   │   ├── 测试报告/
│   │   └── 开发记录/
├── .env.example                # 环境变量示例
├── .env.production             # 生产环境配置
├── PROJECT_NAVIGATION_STANDARD.md  # 项目导航规范
└── README.md                   # 本文档
```

## 🏗️ V2 API 架构 (DDD)

项目采用 **V1 (传统分层) + V2 (DDD六边形架构)** 并行架构，新功能优先使用V2 API。

### V2 后端架构
```
com.school.management/
├── domain/                    # 领域层 - 核心业务逻辑
│   ├── organization/          # 组织管理领域 (OrgUnit, SchoolClass)
│   ├── inspection/            # 量化检查领域 (Template, Record, Appeal)
│   ├── access/                # 权限管理领域 (Permission, Role)
│   └── shared/                # 共享内核 (Entity, AggregateRoot, ValueObject)
├── application/              # 应用层 - 用例编排 (ApplicationService)
├── infrastructure/           # 基础设施层 - 技术实现 (Repository实现, Mapper)
└── interfaces/               # 接口层 - REST控制器 (/api/v2/*)
```

### V2 API端点
| 模块 | 端点 | 说明 |
|------|------|------|
| 组织管理 | `/api/v2/org-units` | 组织单元管理（部门、系部等） |
| 组织管理 | `/api/v2/organization/classes` | 班级管理 |
| 权限管理 | `/api/v2/permissions` | 权限管理 |
| 权限管理 | `/api/v2/roles` | 角色管理 |
| 量化检查 | `/api/v2/inspection-templates` | 检查模板管理 |

### V2 前端结构
```
frontend/src/
├── api/v2/              # V2 API模块 (organization.ts, inspection.ts, access.ts)
├── types/v2/            # V2 TypeScript类型定义
├── stores/v2/           # V2 Pinia状态管理
├── views/v2/            # V2 视图组件
└── router/v2.ts         # V2 路由配置
```

### V2 前端路由
- `/v2/organization/units` - 组织架构管理
- `/v2/organization/classes` - 班级管理
- `/v2/inspection/templates` - 检查模板
- `/v2/inspection/records` - 检查记录
- `/v2/access/roles` - 角色管理
- `/v2/access/permissions` - 权限管理

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

### 4. 量化检查系统 2.0 🔥
- ✅ **检查模板配置**
  - 检查类型管理（宿舍、教室、纪律、卫生等）
  - 扣分项配置（固定扣分、人次扣分、区间扣分）
  - 关联资源配置（宿舍、教室）

- ✅ **检查记录管理 V3**
  - 打分界面优化（卡片式UI、实时保存）
  - 照片上传功能
  - 备注功能
  - 学生关联功能
  - 检查记录列表（分页、筛选、导出）
  - 检查详情查看

- ✅ **统计分析**
  - 班级排名统计
  - 检查类型统计
  - 时间维度统计
  - 得分趋势分析

- ✅ **申诉管理**
  - 申诉提交
  - 申诉审核
  - 申诉记录查询

### 5. 系统功能
- ✅ 数据字典管理
- ✅ 操作日志记录
- ✅ 异常处理机制
- ✅ 参数验证框架
- ✅ 统一响应格式
- ✅ 文件上传管理

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
mysql -u root -p student_management < database/schema/complete_schema.sql
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

> 📌 **文档已重新组织**: 所有文档已按照 [项目导航规范](./PROJECT_NAVIGATION_STANDARD.md) 进行标准化整理

### 🎨 设计文档 (`docs/design/`)
- [技术架构设计](./docs/design/architecture.md) - 系统技术架构和技术选型说明
- [数据库设计](./docs/design/database-design.md) - 数据库表结构、关系和索引设计
- [API接口规范](./docs/design/api-specification.md) - RESTful API接口标准和规范
- [前端架构设计](./docs/design/frontend-architecture.md) - 前端架构、组件和状态管理设计
- [小程序设计](./docs/design/miniprogram-design.md) - 微信小程序架构和功能设计
- [安全设计方案](./docs/design/security-design.md) - 安全机制、权限控制和数据保护

### 💻 开发文档 (`docs/development/`)
- [开发规范](./docs/development/coding-standards.md) - 代码规范、Git规范和开发流程

### 🚀 部署文档 (`docs/deployment/`)
- [基础部署指南](./docs/deployment/deployment-basic.md) - 开发和生产环境部署说明
- [部署检查清单](./docs/deployment/deployment-checklist.md) - 部署前后检查项
- [数据库备份方案](./docs/deployment/database-backup.md) - 自动化备份配置和恢复流程

### 📡 API文档 (`docs/api/`)
- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html` (在线交互式文档)
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs` (API规范定义)
- API使用示例和最佳实践 (待完善)

### ⚡ 优化文档 (`docs/optimization/`)
- [优化会话2总结](./docs/optimization/session-2-summary.md) - JWT安全、数据库备份、Sass更新、Swagger集成等
- [未来优化计划](./docs/optimization/future-plans.md) - 待实施的性能和架构优化建议
- [优化总览](./docs/optimization/overall-summary.md) - 所有优化工作的综合总结

### ✨ 功能文档 (`docs/features/`)
- **量化检查系统2.0** (`docs/features/quantification-v2/`)
  - 模块说明、快速开始、开发报告、交付总结
- **小程序功能** (待完善)

## 📊 项目状态

### 已完成功能 ✅

#### 基础功能
- [x] 用户权限管理系统（RBAC）
- [x] JWT双令牌认证机制
- [x] 学生信息管理
- [x] 部门管理（树形结构）
- [x] 班级管理
- [x] 宿舍管理
- [x] 教室管理

#### 量化检查系统 2.0
- [x] 检查模板配置
- [x] 扣分项配置（三种模式）
- [x] 检查记录V3（自动保存）
- [x] 照片上传功能
- [x] 备注功能
- [x] 学生关联功能
- [x] 统计分析功能
- [x] 申诉管理系统

#### 前端优化
- [x] 用户列表性能优化
- [x] 检查记录列表优化
- [x] 卡片式打分界面
- [x] 响应式布局
- [x] UI/UX优化

### 最近更新 🔄
**2026年1月2日 - DDD架构重构**
- ✅ 完成V2 API DDD六边形架构实现
- ✅ 实现组织管理领域 (OrgUnit, SchoolClass)
- ✅ 实现权限管理领域 (Permission, Role)
- ✅ 实现量化检查领域 (InspectionTemplate)
- ✅ 前后端V2 API联调完成
- ✅ Casbin权限系统集成
- ✅ 完整的集成测试和性能测试

**2025年11月18日 - 项目导航与规范优化**
- ✅ 建立项目导航规范标准 (PROJECT_NAVIGATION_STANDARD.md)
- ✅ 重组文档目录结构 (docs/design, docs/deployment, docs/optimization等)
- ✅ 更新主README文档导航链接
- ✅ 集成Swagger/OpenAPI 3.0文档系统
- ✅ 增强全局异常处理 (新增10+异常处理器)
- ✅ 实施JWT安全加固 (64字节安全密钥)
- ✅ 部署数据库自动备份系统
- ✅ 修复Sass依赖警告 (更新至1.94.1)

**2025年11月3日 - 量化系统优化**
- ✅ 修复人次扣分模式输入框不显示人数的问题
- ✅ 修复固定扣分模式点击没有选中UI变化的问题
- ✅ 优化项目文档结构
- ✅ 整理和归档历史文档

### 进行中功能 🚧
- [ ] 小程序功能开发
- [ ] 数据导出优化
- [ ] 移动端适配

### 计划功能 📋
- [ ] 消息通知系统
- [ ] 成绩管理模块
- [ ] 课程表管理
- [ ] 考勤管理
- [ ] 数据大屏展示

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

**版本**: v2.2.0
**更新时间**: 2026年1月2日
**开发状态**: 持续开发中 🚀
**架构**: V1 + V2 (DDD) 并行 ✅
**文档组织**: 已标准化 ✅
