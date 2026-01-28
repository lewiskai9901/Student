# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **Student Management System (学生管理系统)** built with Spring Boot 3.2 + Vue.js 3 + TypeScript. It's a modern education information platform featuring student information management, V4 inspection system with auto scheduling and analytics, dormitory management, and WeChat miniprogram support.

**Key Features:**
- RBAC-based user permission management with JWT authentication (access + refresh tokens)
- Student information management with Excel import/export
- V4 inspection system with session-based architecture, corrective actions, auto scheduling, analytics, and data export
- Department/Class/Dormitory/Classroom organization management
- WeChat miniprogram integration (in development)

## Build and Run Commands

### Backend (Spring Boot)

**Start backend server:**
```bash
cd backend
JAVA_HOME="/c/Program Files/Java/jdk-17" PATH="/c/Program Files/Java/jdk-17/bin:$PATH" mvn spring-boot:run -DskipTests
```

**Build:**
```bash
cd backend
mvn clean package -DskipTests
```

**Run tests:**
```bash
cd backend
mvn test
```

**Run single test:**
```bash
cd backend
mvn test -Dtest=PasswordTest
```

**Backend runs on:** `http://localhost:8080/api`
**Swagger UI:** `http://localhost:8080/api/swagger-ui.html`
**Druid Monitor:** `http://localhost:8080/api/druid/` (admin/admin123)

### Frontend (Vue 3 + Vite)

**Start dev server:**
```bash
cd frontend
npm install  # first time only
npm run dev
```

**Build for production:**
```bash
cd frontend
npm run build
```

**Type checking:**
```bash
cd frontend
npm run type-check
```

**Linting:**
```bash
cd frontend
npm run lint
```

**Frontend runs on:** `http://localhost:3000`

### Database

**Initialize database:**
```bash
# Create database
mysql -u root -p -e "CREATE DATABASE student_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# Import schema and initial data
mysql -u root -p student_management < database/schema/complete_schema_v2.sql
mysql -u root -p student_management < database/init/init_data.sql
```

**Backup database (Windows):**
```bash
scripts\backup-database.bat
```

**Default credentials:**
- Username: `admin`
- Password: `admin123`

## Architecture

### Backend Architecture (DDD)

本项目采用 **DDD (Domain-Driven Design)** 六边形架构。

**包结构:**
```
com.school.management/
├── domain/                    # 领域层 - 核心业务逻辑
│   ├── organization/          # 组织管理领域
│   │   ├── model/            # 聚合根、实体、值对象
│   │   ├── repository/       # 仓储接口
│   │   └── event/            # 领域事件
│   ├── inspection/           # 量化检查领域
│   │   ├── model/            # 检查领域模型
│   │   ├── repository/       # 仓储接口
│   │   ├── saga/             # Saga编排 (跨聚合工作流)
│   │   └── export/           # 数据导出
│   ├── access/               # 权限管理领域
│   └── shared/               # 共享内核 (Entity, AggregateRoot, ValueObject)
├── application/              # 应用层 - 用例编排
│   ├── organization/         # 组织管理应用服务
│   ├── inspection/           # 量化检查应用服务
│   └── access/               # 权限管理应用服务
├── infrastructure/           # 基础设施层 - 技术实现
│   ├── persistence/          # 持久化实现 (Repository实现, Mapper, PO)
│   ├── cache/                # 缓存实现
│   └── event/                # 事件发布实现
└── interfaces/               # 接口层 - API端点
    └── rest/                 # REST控制器
```

**API端点:**
- `/api/org-units` - 组织单元管理
- `/api/organization/classes` - 班级管理
- `/api/permissions` - 权限管理
- `/api/roles` - 角色管理
- `/api/inspection-templates` - 检查模板管理

**Security Architecture:**
- Dual-token JWT system (access token: 2h, refresh token: 30d)
- Token blacklist using Redis for logout
- Spring Security with stateless session
- CORS configured for multiple origins
- Password encoding with BCrypt

**Data Permission Architecture:**
数据权限系统采用DDD架构，支持细粒度的模块级数据访问控制。

- **DataScope枚举** (`domain/access/model/DataScope.java`):
  - `ALL` - 全部数据
  - `DEPARTMENT_AND_BELOW` - 本部门及以下
  - `DEPARTMENT` - 仅本部门
  - `GRADE` - 本年级
  - `CLASS` - 仅本班级
  - `CUSTOM` - 自定义范围
  - `SELF` - 仅本人

- **DataModule枚举** (`domain/access/model/DataModule.java`):
  - Organization领域: `org_unit`, `student`, `dormitory`, `classroom`
  - Inspection领域: `inspection_template`, `inspection_record`, `appeal`
  - Evaluation领域: `rating`
  - Task领域: `task`

- **API端点**:
  - `GET /api/roles/{roleId}/data-permissions` - 获取角色数据权限配置
  - `PUT /api/roles/{roleId}/data-permissions` - 更新角色数据权限配置
  - `GET /api/roles/data-permissions/modules` - 获取所有数据模块（按领域分组）
  - `GET /api/roles/data-permissions/scopes` - 获取所有数据范围选项

- **自定义范围**: 通过`role_custom_scope`表存储角色对特定模块的自定义组织单元访问权限

**Key Packages:**
- `com.school.management.domain.access`: Data permissions, roles, authentication domain models
- `com.school.management.infrastructure.cache`: Caching infrastructure
- `com.school.management.infrastructure.event`: Domain event publishing infrastructure

**Database Access:**
- MyBatis Plus 3.5.7 with automatic CRUD
- Druid connection pool with monitoring
- Logical delete support (`deleted` field)
- Snowflake ID generation (`@TableId(type = IdType.ASSIGN_ID)`)

### Frontend Architecture

**Frontend Structure:**
```
src/
├── api/                 # API模块
│   ├── organization.ts  # 组织管理API (orgUnitApi, schoolClassApi)
│   ├── inspection.ts    # 量化检查API (templateApi, recordApi, appealApi)
│   └── access.ts        # 权限管理API (permissionApi, roleApi)
├── types/               # TypeScript类型定义
├── stores/              # Pinia状态管理
├── views/               # 视图组件
│   ├── organization/    # OrgUnitsView, SchoolClassesView
│   ├── inspection/      # TemplateListView, RecordListView, AppealListView
│   └── access/          # RoleListView, PermissionListView
├── components/          # Reusable UI components
├── layouts/             # Layout components (MainLayout)
├── router/              # Vue Router (index.ts) with permission guards
└── utils/               # Utility functions
```

**DDD对齐导航结构:**
```
/ (MainLayout)
├── /dashboard (首页, order: 1)
├── /organization (组织管理, order: 2) ← Organization领域
│   ├── /organization/units - 组织架构
│   ├── /organization/classes - 班级管理
│   ├── /organization/students - 学生管理
│   ├── /organization/academic/* - 年级专业
│   ├── /organization/dormitory/* - 宿舍管理
│   └── /organization/teaching/* - 教学设施
├── /inspection (量化检查, order: 3) ← Inspection领域
│   ├── /inspection/config - 量化配置
│   ├── /inspection/plans - 检查计划
│   ├── /inspection/appeals - 申诉管理
│   └── /inspection/* - 其他检查功能
├── /evaluation (综合测评, order: 4)
├── /task (任务管理, order: 5)
├── /access (权限管理, order: 6) ← Access领域
│   ├── /access/users - 用户管理
│   ├── /access/roles - 角色管理
│   └── /access/permissions - 权限管理
└── /settings (系统设置, order: 7)
    ├── /settings/configs - 系统配置
    ├── /settings/weight - 加权配置
    └── /settings/* - 其他设置
```

**State Management:**
- Pinia stores for authentication (`stores/auth.ts`)
- Token stored in localStorage
- Automatic token refresh on 401 responses
- Permission-based route filtering

**Key Patterns:**
- Composition API with `<script setup lang="ts">`
- Auto-import for Vue APIs, Element Plus components
- Lazy-loaded routes for code splitting
- Axios interceptors for auth headers and error handling

## Important Implementation Notes

### Authentication Flow

1. User logs in via `/api/auth/login` → receives access + refresh tokens
2. Access token stored in localStorage, sent via `Authorization: Bearer {token}` header
3. Backend `JwtAuthenticationFilter` validates token on each request
4. On token expiry, frontend auto-refreshes using `/api/auth/refresh`
5. Logout calls `/api/auth/logout` → token added to Redis blacklist

### V4 Inspection System

The inspection system uses a session-based architecture (V4) with the following capabilities:
- **Inspection Sessions**: Session-based check management with configurable templates
- **Corrective Actions**: Track and manage corrective actions linked to inspection findings
- **Student Behavior**: Record and analyze student behavior patterns
- **Auto Scheduling**: Automated scheduling of inspection sessions
- **Analytics**: Inspection data analytics and department ranking
- **Data Export**: Export inspection data for reporting

Key domain packages: `domain/inspection/saga/` for cross-aggregate workflow orchestration, `domain/inspection/export/` for data export logic.

### MyBatis Plus Configuration

**Pagination:**
```java
// In service method
Page<Student> page = new Page<>(pageNum, pageSize);
Page<Student> result = studentMapper.selectPage(page, queryWrapper);
```

**Logical Delete:**
- Entities with `deleted` field automatically handle soft deletes
- No need to manually check `deleted = 0` in queries

**Custom SQL:**
- XML mappers in `resources/mapper/**/*.xml`
- Use `@Mapper` annotation on interfaces

### Common Issues and Solutions

**Port 8080 already in use:**
```bash
# Find and kill process (Windows)
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

**Database connection issues:**
- Check `application.yml` datasource configuration
- Verify MySQL is running on port 3306
- Default credentials: root/123456

**JWT token expired errors:**
- Tokens expire after 2 hours (configurable via `jwt.access-token-expiration`)
- Frontend should auto-refresh; check browser console for errors

**CORS issues:**
- Backend allows origins: localhost:3000, localhost:5173, etc.
- Check `SecurityConfig.corsConfigurationSource()` for allowed origins

### Testing

**Backend Testing:**
- Unit tests in `src/test/java/`
- Use `@SpringBootTest` for integration tests
- Password generation utility: `TestBCrypt.java`

**Frontend Testing:**
- Playwright tests configured (`@playwright/test`)
- Run with: `npm run test` (if test scripts defined)

### Code Style

**Backend:**
- Use Lombok annotations (`@Data`, `@Builder`, `@Slf4j`)
- Service methods should be transactional: `@Transactional`
- Controller methods use `@PreAuthorize` for permission checks
- Domain models in `domain/*/model/`, persistence objects in `infrastructure/persistence/`

**Frontend:**
- Use Composition API with `<script setup>`
- TypeScript strict mode enabled
- ESLint + Prettier for formatting
- Use auto-imported Element Plus components

### Environment Variables

**Backend** (via `application.yml`):
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`: Database connection
- `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`: Redis connection
- `JWT_SECRET`: JWT signing key (min 64 bytes for HS512)
- `FILE_UPLOAD_PATH`: File upload directory

**Frontend** (via `.env` files):
- `VITE_API_BASE_URL`: Backend API base URL

## Documentation

Comprehensive documentation is organized in `docs/`:
- **Design:** `docs/design/` - Architecture, database, API specs, security
- **Development:** `docs/development/` - Coding standards, testing guide
- **Deployment:** `docs/deployment/` - Deployment guides, backup strategies
- **Features:** `docs/features/` - Feature-specific documentation

Refer to `README.md` and `PROJECT_NAVIGATION_STANDARD.md` for complete project navigation.
