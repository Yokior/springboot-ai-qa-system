# 🧠 智能问答知识库平台

> 基于 Spring Boot + 权限管理框架 + AI 技术构建的智能问答系统，支持文档上传、段落匹配与 AI 回答，适用于企业内部知识管理。

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-2.5.15-brightgreen" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Vue-2.6.12-blue" alt="Vue">
  <img src="https://img.shields.io/badge/MySQL-5.7+-orange" alt="MySQL">
  <img src="https://img.shields.io/badge/Redis-6.0+-red" alt="Redis">
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License">
</p>

## 📌 项目简介

本项目是一个**支持多团队隔离的企业级智能问答系统**，用户可上传 PDF/Word/TXT 文档，使用自然语言提问，AI 自动从知识库中提取答案。系统实现了完善的权限控制体系，适合企业内部知识管理与智能问答场景。

## 🧱 技术栈

### 后端技术

- **核心框架**：Spring Boot 2.5.15
- **权限控制**：基于 RBAC 的权限框架 (角色权限 + 数据权限)
- **持久层**：MyBatis
- **数据库**：MySQL + Redis
- **文件处理**：Apache Tika / POI
- **分词工具**：HanLP
- **段落匹配**：TF-IDF / BM25 (可配置切换)
- **AI 接口**：DeepSeek / 通义千问 API / 自定义实现

### 前端技术

- **框架**：Vue 2.6.12
- **UI 组件**：Element UI 2.15.14
- **状态管理**：Vuex 3.6.0
- **路由**：Vue Router 3.4.9
- **HTTP 客户端**：Axios 0.28.1
- **可视化**：ECharts 5.4.0

## 📦 系统功能

### 用户与团队管理

- 多团队隔离架构，支持团队创建、管理与成员控制
- 完善的 RBAC 权限体系，支持系统管理员、团队创建者、团队管理员、团队成员等角色
- 团队申请与审批流程，确保系统安全性

### 知识库管理

- 支持上传 PDF、Word、TXT、MD 等多种格式文档
- 文档自动解析、分段与索引
- 文档管理、分类与检索

### 智能问答

- 基于自然语言处理的问题分析
- 智能段落匹配与相关度排序（支持 TF-IDF 和 BM25 两种算法）
- 接入大模型 API，生成准确、流畅的回答
- 问答历史记录与导出功能

### 系统管理

- 用户、角色、权限管理
- 系统监控与日志查看
- 参数配置与字典管理

## 🏗️ 系统架构

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   前端应用层      │    │  后端服务层        │    │     存储层        │
│  Vue + Element  │────>│  Spring Boot    │────>│ MySQL + Redis   │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                               │
                               ▼
                        ┌─────────────────┐
                        │    AI 服务层     │
                        │ 大模型 API 接口   │
                        └─────────────────┘
```

### 核心处理流程

1. **文档处理流程**：上传 → 解析 → 分词 → 分段 → 特征提取 → 存储
2. **问答流程**：提问 → 分词 → 段落匹配(TF-IDF/BM25) → 相关段落检索 → AI 生成回答 → 返回结果

## 🚀 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.6+
- MySQL 5.7+
- Redis 6.0+
- Node.js 12+

### 后端部署

```bash
# 克隆项目
git clone https://github.com/Yokior/springboot-ai-qa-system.git

# 进入项目目录
cd ai-qa-system

# 编译打包
mvn clean package -DskipTests

# 运行应用
java -jar yokior-admin/target/yokior-admin.jar
```

### 前端部署

```bash
# 进入前端项目目录
cd qa-front

# 安装依赖
npm install

# 开发环境运行
npm run dev

# 生产环境打包
npm run build:prod
```

### 配置说明

1. 修改 `application.yml` 中的数据库连接信息
2. 配置 Redis 连接信息
3. 配置 AI API 密钥与接口地址
4. 配置文件上传路径与大小限制
5. 选择段落匹配算法（TF-IDF 或 BM25）

## 🔐 系统角色与权限

| 角色 | 权限说明 |
|------|----------|
| **系统管理员** | 全局权限，可查看所有团队信息、用户列表、审批团队创建申请等 |
| **团队创建者** | 创建团队，指定管理员，删除团队，转让所有权 |
| **团队管理员** | 管理团队成员、上传/删除资料、设置权限、处理加入申请 |
| **团队成员** | 只能使用该团队的知识库进行问答，不能访问后台管理功能 |

## 📋 数据库设计

系统采用多表设计，主要包括：
- 用户表 (qa_user)
- 团队表 (qa_team)
- 用户-团队关系表 (qa_user_team)
- 团队邀请表 (qa_team_invite)
- 文档元数据表 (qa_document)
- 文档段落表 (qa_document_paragraph)
- 问答消息表 (chat_message)
- 问答会话表 (chat_session)

## 🧩 项目特色

1. **多租户架构**：支持多个团队独立空间，数据严格隔离
2. **完善权限体系**：基于 RBAC 模型，支持多级权限控制
3. **智能段落匹配**：同时支持 TF-IDF 和 BM25 算法，BM25 算法考虑了文档长度和词频饱和度，提供更高的匹配精度
4. **AI 能力集成**：对接主流大模型 API，提供智能回答
5. **模块化设计**：系统划分为多个功能模块，便于扩展和维护

## 📝 开发计划

- [ ] 支持更多文档格式
- [x] 引入 BM25 算法，提升匹配精度
- [ ] 引入向量数据库，进一步提升匹配精度
- [ ] 优化 AI 提示词工程
- [ ] 添加用户反馈机制
- [ ] 支持知识库数据导出
- [ ] 移动端适配

## 📄 许可证

本项目采用 [MIT 许可证](LICENSE)

## 👨‍💻 贡献指南

欢迎提交 Issue 或 Pull Request 来帮助改进本项目！

## 📞 联系方式

- 项目地址：[GitHub](https://github.com/Yokior/springboot-ai-qa-system)
- 演示地址：[https://ai.yokior.top](https://ai.yokior.top)
- 联系邮箱：yokior@yokior.top
