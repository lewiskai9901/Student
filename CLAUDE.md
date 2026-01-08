# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **Student Management System (学生管理系统)** built with Spring Boot 3.2 + Vue.js 3 + TypeScript. It's a modern education information platform featuring student information management, quantification check system 2.0, dormitory management, and WeChat miniprogram support.

**Key Features:**
- RBAC-based user permission management with JWT authentication (access + refresh tokens)
- Student information management with Excel import/export
- Quantification check system 2.0 with scoring templates, deduction items, appeals
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
mysql -u root -p student_management < database/schema/complete_schema.sql
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

### Backend Architecture (V2 - DDD)

本项目采用 **DDD (Domain-Driven Design)** 架构，V1和V2 API并行运行。

**DDD 六边形架构包结构:**
```
com.school.management/
├── domain/                    # 领域层 - 核心业务逻辑
│   ├── organization/          # 组织管理领域
│   │   ├── model/            # 聚合根、实体、值对象
│   │   ├── repository/       # 仓储接口
│   │   └── event/            # 领域事件
│   ├── inspection/           # 量化检查领域
│   ├── access/               # 权限管理领域
│   └── shared/               # 共享内核 (Entity, AggregateRoot, ValueObject)
├── application/              # 应用层 - 用例编排
│   ├── organization/         # 组织管理应用服务
│   ├── inspection/           # 量化检查应用服务
│   └── access/               # 权限管理应用服务
├── infrastructure/           # 基础设施层 - 技术实现
│   └── persistence/          # 持久化实现 (Repository实现, Mapper, PO)
└── interfaces/               # 接口层 - API端点
    └── rest/                 # REST控制器 (/v2/*)
```

**V2 API端点:**
- `/api/v2/org-units` - 组织单元管理
- `/api/v2/organization/classes` - 班级管理
- `/api/v2/permissions` - 权限管理
- `/api/v2/roles` - 角色管理
- `/api/v2/inspection-templates` - 检查模板管理

### Backend Architecture (V1 - 传统分层)

**Layered Structure:**
- **Controller Layer** (`controller/`): HTTP request handling, parameter validation, RESTful APIs
- **Service Layer** (`service/`): Business logic, transaction management
- **Mapper Layer** (`mapper/`): MyBatis data access (with XML in `resources/mapper/`)
- **Entity Layer** (`entity/`): Database entities (JPA annotations + MyBatis Plus)
- **DTO Layer** (`dto/`): Data Transfer Objects for API requests/responses
- **Security** (`security/`): JWT authentication, Spring Security configuration
- **Config** (`config/`): Application configuration beans

**Key Packages:**
- `com.school.management.security`: JWT token service, authentication filter, custom user details
- `com.school.management.exception`: Business exceptions, global exception handler
- `com.school.management.common`: Common response wrappers (`Result`, `ApiResponse`, `PageResult`)

**Security Architecture:**
- Dual-token JWT system (access token: 2h, refresh token: 30d)
- Token blacklist using Redis for logout
- Spring Security with stateless session
- CORS configured for multiple origins
- Password encoding with BCrypt

**Database Access:**
- MyBatis Plus 3.5.7 with automatic CRUD
- Druid connection pool with monitoring
- Logical delete support (`deleted` field)
- Snowflake ID generation (`@TableId(type = IdType.ASSIGN_ID)`)

### Frontend Architecture

**V2 DDD适配结构:**
```
src/
├── api/v2/              # V2 API模块
│   ├── organization.ts  # 组织管理API (orgUnitApi, schoolClassApi)
│   ├── inspection.ts    # 量化检查API (templateApi, recordApi, appealApi)
│   └── access.ts        # 权限管理API (permissionApi, roleApi)
├── types/v2/            # V2 TypeScript类型定义
├── stores/v2/           # V2 Pinia状态管理
├── views/v2/            # V2 视图组件
│   ├── organization/    # OrgUnitsView, SchoolClassesView
│   ├── inspection/      # TemplateListView, RecordListView, AppealListView
│   └── access/          # RoleListView, PermissionListView
└── router/v2.ts         # V2 路由配置
```

**V2 路由:**
- `/v2/organization/units` - 组织架构管理
- `/v2/organization/classes` - 班级管理
- `/v2/inspection/templates` - 检查模板
- `/v2/inspection/records` - 检查记录
- `/v2/access/roles` - 角色管理
- `/v2/access/permissions` - 权限管理

**V1 Structure:**
- `src/api/`: Axios HTTP client modules (organized by domain)
- `src/stores/`: Pinia state management (auth, user, app state)
- `src/router/`: Vue Router with permission guards
- `src/views/`: Page components (organized by feature)
- `src/components/`: Reusable UI components
- `src/layouts/`: Layout components (MainLayout)
- `src/types/`: TypeScript type definitions
- `src/utils/`: Utility functions

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

### Quantification System 2.0

The quantification check system has evolved through multiple versions. Current V3 implementation:

**Key Entities:**
- `CheckTemplate`: Check templates with deduction items
- `DailyCheck`: Daily check records with scoring
- `DeductionItem`: Configurable deduction rules (fixed/per-person/range scoring)
- `CheckItemAppeal`: Appeal management for disputed scores

**Deduction Modes:**
1. **FIXED_DEDUCT**: Fixed score deduction
2. **PER_PERSON_DEDUCT**: Deduction multiplied by number of people
3. **SCORE_RANGE**: Configurable score ranges

**Deprecated:**
- `_deprecated_v1/` packages contain old quantification logic (DO NOT USE)

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
- DTOs for API input/output, Entities for database

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
- **Features:** `docs/features/quantification-v2/` - Quantification system 2.0 docs

Refer to `README.md` and `PROJECT_NAVIGATION_STANDARD.md` for complete project navigation.
