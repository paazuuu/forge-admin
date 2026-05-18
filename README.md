

<center><img src="images/Logo.png" width="800"></center>

<p align="center">
  🚀 基于 Vue 3 + Spring Boot 3 的企业级中后台管理框架<br>
  ✨ 插件化架构、AI 代码生成、Flowable 工作流与 AI 数据可视化大屏一体化开箱
</p>

<p align="center">
  <a href="https://gitee.com/ForgeLab/forge-admin/stargazers"><img src="https://gitee.com/ForgeLab/forge-admin/badge/star.svg?theme=gvp" alt="Gitee stars"></a>
  <img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="License">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.x-green.svg" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Vue-3.x-brightgreen.svg" alt="Vue3">
  <img src="https://img.shields.io/badge/Report-AI%20Enhanced-orange.svg" alt="AI Dashboard">
</p>

<p align="center">
  <a href="#-在线演示">在线演示</a> ·
  <a href="#-项目亮点">项目亮点</a> ·
  <a href="#-系统截图">系统截图</a> ·
  <a href="#-ai-数据可视化大屏">AI 大屏</a> ·
  <a href="#-快速开始">快速开始</a> ·
  <a href="CHANGELOG.md">更新日志</a>
</p>

---

## ✨ 项目简介

**Forge Admin** 是一套面向企业后台、SaaS 管理端、数据可视化平台和内部低代码工具的中后台框架。它不只提供常见的用户、角色、菜单、字典、文件、日志等基础能力，还把 **AI 代码生成**、**AI 数据大屏**、**Flowable 工作流**、**多租户隔离** 做成可持续扩展的工程体系。

如果你正在搭建一个长期演进的后台系统，Forge Admin 更关注三件事：

| 目标 | Forge Admin 提供什么 |
|------|----------------------|
| 更快交付业务页面 | AI 表单生成、CRUD 页面配置、代码生成插件、可下载代码包 |
| 更稳承载企业复杂度 | RBAC、多租户、数据权限、操作日志、动态配置、文件存储、Excel 导入导出 |
| 更容易扩展新能力 | 微内核 + 插件化架构，业务插件和技术 Starter 分层清晰 |

---

## 🌟 项目亮点

| 能力 | 说明 |
|------|------|
| 🏗️ **微内核插件化** | 核心框架轻量，系统、生成器、任务、消息、流程、AI 等能力以插件方式组合 |
| 🤖 **AI 数据大屏** | 通过自然语言生成大屏，支持组件拖拽、主题定制、真实 API 数据接入和发布 |
| ⚡ **AI 代码生成** | 面向表单和 CRUD 场景，支持 0 代码配置，也支持下载代码包二次开发 |
| 🔐 **多租户 + RBAC** | 租户级数据隔离、菜单权限、按钮权限、角色资源绑定等企业后台基础能力 |
| 🔄 **工作流引擎** | 集成 Flowable，覆盖模型设计、流程发起、待办审批、时间轴追踪 |
| 🧩 **组件化前端** | Vue 3 + Naive UI + UnoCSS，内置字典、区域、上传、图标选择、AI 表单等组件 |
| 🔌 **多 AI 供应商** | 支持阿里百炼、OpenAI、DeepSeek、Ollama、智谱、Moonshot 等模型服务 |
| 📊 **真实数据报表** | 大屏报表可直接对接后端接口，减少静态 Mock 到真实业务之间的落差 |

---

## 🧭 适合场景

- 企业内部管理系统：组织、用户、角色、菜单、配置、文件、日志、通知等通用后台能力。
- 多租户 SaaS 后台：租户隔离、数据权限、客户独立配置、权限精细化控制。
- 审批流业务系统：请假、采购、合同、报销、工单等需要流程编排的业务。
- 数据大屏与驾驶舱：用 AI 快速生成可视化大屏，再接入真实业务 API。
- 低代码/代码生成平台：通过模板、表单设计器和 AI 生成能力沉淀研发资产。

---

## 🏛️ 系统架构

![系统架构图.jpeg](images/%E7%B3%BB%E7%BB%9F%E6%9E%B6%E6%9E%84%E5%9B%BE.jpeg)

后端采用 `forge-framework` + `forge-plugin-parent` + `forge-starter-parent` 的分层方式：Starter 负责认证、缓存、ORM、多租户、数据权限、加解密、日志、文件、Excel 等底层能力；Plugin 负责系统管理、代码生成、任务调度、消息中心、流程和 AI 等业务能力；应用服务按场景聚合插件并对外提供接口。

---

## 📺 在线演示

| 入口     | 地址 |
|--------|------|
| 后台管理   | http://www.dlforgelab.com:8084/forge/login |
| 项目文档   | http://www.dlforgelab.com:8084/forge-docs/ |
| 大屏设计器  | http://www.dlforgelab.com:8084/forge-report/|
| Gitee  | https://gitee.com/ForgeLab/forge-admin |
| GitHub | https://github.com/yaomindong1996/forge-admin |

默认体验账号：`admin` / `123456`

---

## 🖼️ 系统截图

### 后台管理系统

#### 登录页面

![登录页.png](images/%E7%99%BB%E5%BD%95%E9%A1%B5.png)

支持账号密码登录与验证码校验，作为后台系统的统一认证入口。

#### 首页仪表盘

![首页.png](images/%E9%A6%96%E9%A1%B5.png)

集中展示系统运行状态、业务指标和常用入口，适合作为管理端工作台。

#### 菜单管理

![菜单管理.png](images/%E8%8F%9C%E5%8D%95%E7%AE%A1%E7%90%86.png)

支持动态路由、菜单目录、按钮权限和资源绑定，便于快速搭建权限导航。

#### 配置管理

![配置管理.png](images/%E9%85%8D%E7%BD%AE%E7%AE%A1%E7%90%86.png)

系统参数、字典数据等基础配置可以在后台动态维护，减少重复发布。

#### 消息管理

![消息管理](images/%E6%B6%88%E6%81%AF%E7%AE%A1%E7%90%86.png)

统一管理站内信、系统通知和消息模板，适合接入业务提醒、审批通知等场景。

#### 流程管理

![流程模型](images/%E6%B5%81%E7%A8%8B%E6%A8%A1%E5%9E%8B.png)
![流程设计](images/%E6%B5%81%E7%A8%8B%E8%AE%BE%E8%AE%A1.png)
![流程时间轴](images/%E6%B5%81%E7%A8%8B%E6%97%B6%E9%97%B4%E8%BD%B4.png)

基于 Flowable 提供流程模型、在线设计、节点配置、审批记录与流程时间轴。

#### 我的待办

![我的待办](images/%E6%88%91%E7%9A%84%E5%BE%85%E5%8A%9E.png)

审批人可以集中处理待办任务，快速完成通过、驳回和流程跟踪。

#### 文件管理

![文件管理](images/%E6%96%87%E4%BB%B6%E7%AE%A1%E7%90%86.png)

统一文件管理能力，支持本地存储、对象存储和鉴权访问。

#### 数据权限配置

![数据权限配置](images/%E6%95%B0%E6%8D%AE%E6%9D%83%E9%99%90%E9%85%8D%E7%BD%AE.png)

支持按组织、角色和业务规则配置数据范围，降低多组织数据越权风险。

#### Excel 导出配置

![excel导出配置](images/excel%E5%AF%BC%E5%87%BA%E9%85%8D%E7%BD%AE.png)

导入导出模板可配置，减少大量重复注解和临时导出代码。

#### 服务监控

![服务监控](images/%E6%9C%8D%E5%8A%A1%E7%9B%91%E6%8E%A7.png)

查看 CPU、内存、磁盘等运行指标，辅助排查本地和测试环境问题。

---

## 🤖 AI 数据可视化大屏

**Forge AI** 是项目内置的 AI 数据可视化低代码平台。你可以先用自然语言描述业务目标，让 AI 生成大屏草稿，再通过可视化编辑器调整组件、数据源、主题和交互，最后发布为可访问页面。

### 核心特性

| 特性 | 说明 |
|------|------|
| 🤖 AI 智能生成 | 接入大模型后，可通过对话生成大屏结构、组件布局和基础配置 |
| 🧩 组件素材库 | 内置图表、文字、图片、视频、滚动表格、装饰边框、数字翻牌等组件 |
| 🎨 主题定制 | 支持深浅主题、背景、全局滤镜、画布尺寸和自适应方式 |
| 📊 数据接入 | 支持静态数据、动态 HTTP 请求和数据池，方便接入后端业务 API |
| ⚡ 事件交互 | 支持点击、双击、鼠标进入/移出、生命周期事件和自定义 JavaScript |
| 🚀 一键发布 | 编辑完成即可发布预览链接，便于分享、嵌入和交付 |

### 界面预览

| 页面 | 截图 |
|------|------|
| **登录页** | ![登录页](images/report/login-page.png) |
| **项目列表** | ![项目列表](images/report/project-home.png) |
| **画布编辑器** | ![画布编辑器](images/report/canvas-editor.png) |
| **AI 供应商配置** | ![AI供应商配置](images/report/AI%E4%BE%9B%E5%BA%94%E5%95%86%E9%85%8D%E7%BD%AE.png) |

### 内置组件

| 分类 | 组件 |
|------|------|
| 图表 | 柱状图、横向柱状图、折线图、面积图、饼图、环形图、雷达图、散点图、热力图、漏斗图、水球图、中国地图 |
| 信息 | 文字、渐变文字、词云、图片、视频、嵌套网页 |
| 表格 | 滚动排名列表、滚动表格 |
| 装饰 | 边框 01~13、装饰 01~05、数字翻牌、时钟、倒计时、数字计数 |

### AI 供应商

支持阿里百炼（通义千问）、OpenAI（GPT）、智谱 AI（GLM）、Moonshot（Kimi）、DeepSeek、Ollama 等主流 AI 服务，也支持兼容 OpenAI API 格式的自定义服务。

---

## 🧠 AI 驱动的代码生成

Forge Admin 的代码生成能力面向真实后台研发流程：可以通过 AI 辅助生成表单和 CRUD 页面，也可以通过模板市场进行个性化配置。简单业务可以 0 代码上线，复杂业务可以下载代码包继续二次开发。

### AI 表单生成

![AI表单生成.png](images/AI%E4%BB%A3%E7%A0%81%E7%94%9F%E6%88%90/AI%E8%A1%A8%E5%8D%95%E7%94%9F%E6%88%90.png)

### AI 表单生成列表

![AI表单生成列表.png](images/AI%E4%BB%A3%E7%A0%81%E7%94%9F%E6%88%90/AI%E8%A1%A8%E5%8D%95%E7%94%9F%E6%88%90%E5%88%97%E8%A1%A8.png)

### 模板配置

![模版配置.png](images/AI%E4%BB%A3%E7%A0%81%E7%94%9F%E6%88%90/%E6%A8%A1%E7%89%88%E9%85%8D%E7%BD%AE.png)

### 表单编辑

![表单编辑.png](images/AI%E4%BB%A3%E7%A0%81%E7%94%9F%E6%88%90/%E8%A1%A8%E5%8D%95%E7%BC%96%E8%BE%91.png)

---

## 💻 技术栈

### 后端

| 技术 | 用途 |
|------|------|
| Java 17 | 后端运行环境 |
| Spring Boot 3.2 | 应用开发框架 |
| MyBatis-Plus 3.5 | ORM 与分页能力 |
| Sa-Token 1.38 | 登录认证、权限校验、Token 管理 |
| Flowable 7.0 | 工作流建模与执行 |
| Redis / Redisson | 缓存、分布式锁、会话能力 |
| Quartz / SnailJob | 任务调度 |
| Maven | 后端多模块构建 |

### 前端

| 技术 | 用途 |
|------|------|
| Vue 3.5 | 前端框架 |
| Naive UI 2.42 | 管理端组件库 |
| Vite 7 | 开发服务器与构建工具 |
| Pinia 3 | 状态管理 |
| Vue Router 4.5 | 动态路由与权限路由 |
| UnoCSS 66 | 原子化样式 |
| ECharts / VChart | 图表与大屏可视化 |
| BPMN.js / CodeMirror | 流程设计与代码编辑 |

---

## 📁 项目结构

```text
forge/
├── forge-admin-server/          # 主应用入口，聚合后台管理能力
├── forge-report-server/         # AI 大屏报表服务
├── forge-app-server/            # App 接口服务
├── forge-flow/                  # 独立流程服务与流程客户端
├── forge-business/              # 业务模块
└── forge-framework/
    ├── forge-dependencies/      # 统一依赖版本管理
    ├── forge-plugin-parent/     # system / generator / job / message / flow / ai 等业务插件
    └── forge-starter-parent/    # auth / cache / orm / tenant / crypto / excel / file 等技术 Starter

forge-admin-ui/                  # 后台管理系统前端
forge-report-ui/                 # AI 数据可视化大屏前端
forge-docs/                      # VitePress 文档站
code-copilot/                    # AI 编程规则与变更管理
```

---

## 🚀 快速开始

### 环境要求

| 环境 | 推荐版本 |
|------|----------|
| JDK | 17+ |
| Node.js | 20.19+ |
| pnpm | 8+ |
| MySQL | 8.0+ |
| Redis | 6.0+ |

### 1. 克隆项目

```bash
git clone https://gitee.com/ForgeLab/forge-admin.git
cd forge-admin
```

### 2. 初始化数据库

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE forge DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"

# 后台管理基础表
mysql -u root -p forge < forge/forge-admin-server/sql/初始化脚本.sql

# 如果启用 AI 大屏，再导入报表服务表
mysql -u root -p forge < forge/forge-report-server/sql/report-init.sql
```

### 3. 准备本地配置

```bash
# 后台管理服务
cp forge/forge-admin-server/src/main/resources/application-dev.example.yml \
   forge/forge-admin-server/src/main/resources/application-dev.yml

# 独立流程服务
cp forge/forge-flow/forge-flow-server/src/main/resources/application-dev.example.yml \
   forge/forge-flow/forge-flow-server/src/main/resources/application-dev.yml
```

然后按本地环境修改 MySQL、Redis、文件存储、AI 供应商等配置。`application-dev.yml` 属于本地配置，敏感信息不要提交到仓库；新增通用配置时，请同步更新 `application-dev.example.yml` 并使用占位符。

### 4. 启动后端

```bash
# 主后台服务，默认 http://localhost:8580
cd forge/forge-admin-server
mvn spring-boot:run
```

可选服务：

```bash
# AI 大屏报表服务，默认 http://localhost:8581
cd forge/forge-report-server
mvn spring-boot:run

# 独立流程服务，默认 http://localhost:8081
cd forge/forge-flow/forge-flow-server
mvn spring-boot:run
```

### 5. 启动前端

```bash
# 后台管理前端，默认 http://localhost:3000
cd forge-admin-ui
pnpm install
pnpm dev
```

```bash
# AI 大屏前端，默认 http://localhost:3021/forge-report
cd forge-report-ui
pnpm install
pnpm dev
```

默认登录账号：`admin` / `123456`

### 6. 构建生产包

```bash
# 后台管理前端
cd forge-admin-ui
pnpm build

# AI 大屏前端
cd forge-report-ui
pnpm build

# 后端全量构建
cd forge
mvn clean install -DskipTests
```

生产环境 Nginx 配置参考 [NGINX_CONFIG.md](NGINX_CONFIG.md)。

---

## 📋 功能模块

### 系统管理

| 模块 | 说明 |
|------|------|
| 用户管理 | 用户增删改查、角色绑定、组织关联 |
| 角色管理 | 角色权限配置、资源绑定、数据范围 |
| 菜单管理 | 动态菜单、页面路由、按钮权限 |
| 部门管理 | 组织架构、树形结构、上下级关系 |
| 岗位管理 | 岗位配置、用户岗位关联 |
| 租户管理 | 多租户配置、租户隔离 |

### 运维与监控

| 模块 | 说明 |
|------|------|
| 在线用户 | 查看在线会话、强制下线 |
| 定时任务 | 任务配置、动态调度、执行日志 |
| 系统日志 | 操作日志、登录日志、异常排查 |
| 服务监控 | CPU、内存、磁盘等指标 |
| 缓存管理 | Redis 缓存可视化操作 |
| 文件管理 | 文件上传、下载、存储配置 |

### 开发者工具

| 模块 | 说明 |
|------|------|
| 代码生成 | 表导入、字段配置、模板管理、代码预览与下载 |
| API 配置 | 接口行为动态配置 |
| 数据源管理 | 多数据源配置 |
| Excel 配置 | 导入导出模板动态配置 |
| 字典管理 | 字典类型、字典数据、状态标签 |
| 通知公告 | 公告发布、阅读状态、消息模板 |

### AI 大屏报表

| 模块 | 说明 |
|------|------|
| 大屏编辑器 | 拖拽式画布、图层管理、组件配置 |
| AI 生成 | 自然语言生成大屏页面 |
| AI 供应商 | 多模型供应商配置与切换 |
| 数据源配置 | 静态数据、动态 HTTP、数据池 |
| 项目管理 | 大屏项目保存、发布、预览 |
| 模板市场 | 行业模板复用与二次编辑 |

---

## 🔌 插件体系

| 插件 | 说明 |
|------|------|
| `forge-plugin-system` | 用户、角色、菜单、部门、岗位、租户、字典等系统管理能力 |
| `forge-plugin-generator` | AI 驱动代码生成、模板配置、代码预览与下载 |
| `forge-plugin-job` | 定时任务、任务触发、执行日志 |
| `forge-plugin-message` | 站内信、系统通知、消息模板 |
| `forge-plugin-flow` | Flowable 流程模型、审批任务、流程事件 |
| `forge-plugin-ai` | AI 供应商、模型配置、对话与生成能力 |

---

## ❓ 常见问题

### Q: 为什么首次启动报数据库或 Redis 连接错误？

A: 需要先复制 `application-dev.example.yml` 为 `application-dev.yml`，并改成本地 MySQL、Redis 连接信息。

### Q: 前端请求后端失败怎么排查？

A: 先确认后端端口是否启动，再检查 `forge-admin-ui/.env.development` 中的 `VITE_HTTP_PROXY_TARGET` 和 `VITE_FLOW_PROXY_TARGET` 是否指向本地服务。

### Q: 新增配置项需要注意什么？

A: 本地敏感配置不要提交；通用配置请同步到 `application-dev.example.yml`，密码、密钥、AK/SK 等统一使用占位符。

### Q: 不小心提交了敏感配置怎么办？

A: 立即更换相关密码或密钥，然后从仓库历史和当前索引中清理敏感文件，并补充 `.gitignore` 规则。

---

## 📝 更新日志

查看 [CHANGELOG.md](CHANGELOG.md) 了解项目版本变化。

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request。建议提交前先说明问题背景、复现步骤或功能目标，便于更快讨论和合并。

---

## 📮 联系作者

项目合作、技术交流、功能建议欢迎联系。

<img src="images/wechat.png" width="200">
<img src="images/wechat1.png" width="200">
<img src="images/微信群.png" width="200">

---

## 💖 开源赞助支持

感谢每一位朋友对项目持续迭代、维护和开源分享的支持。所有赞助不分金额，都会记录在 README 中。

### 赞助规则

- 微信转账/赞赏均可。
- 赞助后可提供：**微信昵称、自定义备注、个人诉求、GitHub/Gitee 主页**。
- 赞助名单长期展示在本项目 README 中。

### 🏆 赞助名单

| 序号 |   微信昵称    |  赞助金额   |    赞助时间    | 个人诉求 / 备注 |
|:--:|:---------:|:-------:|:----------:|:--|
| 1  | Jacstybao | ¥30.00  | 2025-05-12 | 希望升级 springBoot4 |
| 2  |    白哥     | ¥200.00 | 2025-05-12 | 开源赞助 |
| 3  |    *超     | ¥30.00  | 2025-05-12 | 开源赞助 |
| 4  |    薛礼     | ¥100.00 | 2025-05-18 | 开源赞助 |

### 赞助方式

扫码微信即可赞助，备注「开源赞助」，我会及时录入名单更新 README。

<img src="images/微信收款码.png" width="200">

---

## 📄 许可证

本项目基于 [MIT](LICENSE) 许可证开源。
