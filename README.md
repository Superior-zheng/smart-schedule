# Smart Schedule

Smart Schedule 是一个面向学习与日常规划场景的智能日程管理系统。项目采用前后端分离架构，支持用户登录、任务管理、AI 日程解析、计时事项、数据统计、签到打卡、愿景板和学习资料库等功能。

该项目为本科毕业设计作品，重点展示了 Spring Boot + Vue 技术栈下的完整 Web 应用开发流程，以及大语言模型在自然语言日程解析中的应用。

## 功能特性

- 用户注册、登录、修改密码、修改昵称和注销账号
- JWT 鉴权与多用户数据隔离
- 普通提醒事项管理，支持优先级、截止时间、完成和超时状态
- 计时事项管理，支持计划用时和前端倒计时器
- AI 智能解析自然语言输入，自动生成日程任务
- 每日晨报，根据待办数量生成简短提示语
- 数据统计看板，展示任务完成率、超时率和任务类型占比
- 签到打卡，支持自定义打卡项目和每日记录
- 愿景板/倒数日，支持目标日期、正计时、倒计时和重复模式
- 学习资料库，支持保存学习链接和备注
- 管理员角色可查看全部用户任务

## 技术栈

### 后端

- Java 17
- Spring Boot 3
- MyBatis-Plus
- MySQL
- JWT
- BCrypt
- DeepSeek API

### 前端

- Vue 3
- Vite
- Vue Router
- Element Plus
- Axios
- ECharts

## 项目结构

```text
smart-schedule
├── backend                 # Spring Boot 后端服务
│   ├── src/main/java       # 后端业务代码
│   ├── src/main/resources  # 配置文件与数据库脚本
│   └── pom.xml             # Maven 配置
├── frontend                # Vue 前端项目
│   ├── src                 # 前端页面、路由、工具方法
│   ├── public              # 静态资源
│   └── package.json        # 前端依赖与脚本
└── README.md
```

## 数据库

项目使用 MySQL，默认数据库名为 `smart_schedule`。后端启动时会根据 `backend/src/main/resources/schema.sql` 初始化表结构。

主要数据表包括：

- `user`：用户信息
- `task`：任务与日程事项
- `habit`：打卡项目
- `checkin_record`：打卡记录
- `vision_item`：愿景板/倒数日
- `library_link`：学习资料链接

## 环境变量

后端配置位于 `backend/src/main/resources/application.yml`。建议通过环境变量配置敏感信息：

```text
DEEPSEEK_API_KEY=你的 DeepSeek API Key
DEEPSEEK_API_URL=https://api.deepseek.com/chat/completions
MYSQL_USERNAME=root
MYSQL_PASSWORD=你的数据库密码
JWT_SECRET=至少 64 字符的随机字符串
CORS_ORIGINS=http://localhost:5173
```

如果未配置 `DEEPSEEK_API_KEY`，系统会使用本地兜底解析逻辑，仍可创建任务。

## 本地运行

### 1. 启动后端

先创建 MySQL 数据库：

```sql
CREATE DATABASE smart_schedule DEFAULT CHARACTER SET utf8mb4;
```

进入后端目录并启动：

```bash
cd backend
./mvnw spring-boot:run
```

Windows 下也可以使用：

```bash
cd backend
mvnw.cmd spring-boot:run
```

后端默认运行在：

```text
http://localhost:8080
```

### 2. 启动前端

进入前端目录：

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在：

```text
http://localhost:5173
```

如需修改后端地址，可在 `frontend/.env` 中配置：

```text
VITE_API_BASE_URL=http://localhost:8080
```

## 核心流程

用户可以在首页输入自然语言日程，例如：

```text
明天晚上 8 点看网课 40 分钟，周五前提交论文初稿，今晚记得吃药
```

系统会调用 AI 接口解析输入内容，识别任务标题、类型、时间、优先级和计划用时，并保存到数据库。解析后的任务会分别进入提醒事项或计时事项页面，用户可以继续编辑、完成、删除或查看统计结果。

## 说明

本仓库当前作为毕业设计项目归档使用。项目中的 API Key、数据库密码等敏感信息不应提交到仓库，请使用环境变量或本地配置进行管理。
