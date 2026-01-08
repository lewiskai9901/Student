# 学生管理系统前端

基于 Vue 3 + TypeScript + Element Plus 的前端项目

## 技术栈

- **框架**: Vue 3.4+
- **构建工具**: Vite 4.4+
- **语言**: TypeScript 5.0+
- **UI库**: Element Plus 2.4+
- **状态管理**: Pinia 2.1+
- **路由**: Vue Router 4.2+
- **HTTP客户端**: Axios 1.5+
- **CSS预处理器**: Sass
- **代码规范**: ESLint + Prettier

## 项目结构

```
src/
├── api/              # API接口定义
├── assets/           # 静态资源
│   ├── icons/        # 图标
│   ├── images/       # 图片
│   └── styles/       # 样式文件
├── components/       # 公共组件
├── hooks/            # 组合式函数
├── layouts/          # 布局组件
├── router/           # 路由配置
├── stores/           # Pinia状态管理
├── types/            # TypeScript类型定义
├── utils/            # 工具函数
└── views/            # 页面组件
```

## 开发指南

### 环境要求

- Node.js >= 18.0.0
- pnpm >= 7.0.0 (推荐)

### 安装依赖

```bash
cd frontend
npm install
# 或者
pnpm install
```

### 开发模式

```bash
npm run dev
# 或者
pnpm dev
```

### 构建生产版本

```bash
npm run build
# 或者
pnpm build
```

### 代码检查和格式化

```bash
# ESLint检查
npm run lint

# Prettier格式化
npm run format
```

## 功能模块

- [x] 用户认证（登录/登出）
- [x] 路由权限控制
- [x] 主页面布局
- [x] 学生管理基础页面
- [ ] 学生CRUD功能
- [ ] 班级管理
- [ ] 宿舍管理
- [ ] 量化检查
- [ ] 量化记录
- [ ] 量化统计
- [ ] 系统管理

## API接口

前端通过 `/api` 代理访问后端API，后端服务运行在 `http://localhost:8080`

## 注意事项

1. 所有API调用都已配置统一的错误处理
2. 路由访问需要对应的权限
3. 组件已配置自动导入（Element Plus）
4. 开发时热更新已配置