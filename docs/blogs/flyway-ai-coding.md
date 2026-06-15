# AI 三次改我的 Flyway 脚本，第三次差点把生产数据库写废了

> 一篇被 Cursor / Claude / Copilot 反复教育之后，我才搞明白的 AI Coding 时代数据库版本管理生存指南。

## 写在前面：AI 写 SQL，是真的爽，也是真的危险

去年这个时候，我还在手写每一个 Flyway 脚本——`V1.0.7__add_user_status_column.sql`，列名敲一遍、注释敲一遍、回滚预案再敲一遍。

今年我已经习惯了对着 AI 喊一句：

> 「帮我加个用户状态字段，0 启用 1 停用，写个 Flyway 脚本」

三秒钟，脚本就出来了。语法正确、命名规范、连 `COMMENT` 都贴心带上。爽吗？爽得不行。

直到某天上午 10 点，我盯着 IDEA 启动日志，看见那行红色的:

```text
FlywayValidateException: Migration checksum mismatch for migration version 1.0.32
```

——AI 把已经在生产库执行过的脚本，又给我**改了一遍内容**。

那一刻我才意识到：**Flyway 这套设计了十几年的「不可变迁移」哲学，正在被 AI Coding 的"自动化贴心服务"系统性地破坏。**

这篇文章不教 Flyway 基础用法，而是把我和团队过去半年踩过的 5 个真实坑摊开来讲，每一个坑配上 AI 行为模式分析、根因、和我们最终沉淀下来的工程规范。

读完你会知道：在 AI 写 SQL 已经是日常的今天，Flyway 这套老工具应该怎么用，才能不被 AI 反向坑死。

---

## 案例一：AI 修改了已上线的 V1.0.32，应用集体起不来

### 现场

我让 AI 帮我「优化一下昨天那个建表脚本，字段顺序不太合理」。AI 很听话，一顿调整、commit、push。

第二天同事拉代码启动应用：

```text
Migration checksum mismatch for migration version 1.0.32
-> Applied to database : 1234567890
-> Resolved locally    : 9876543210
```

应用直接起不来，团队 8 个人的本地环境集体瘫痪。

### AI 为什么会这么干

因为你让它「优化脚本」，它根本不知道这个脚本**已经在数据库里跑过了**。

Flyway 把每个版本脚本视作**不可变的历史事件**——脚本一旦写入 `flyway_schema_history`，对应的 checksum 就被钉死。本地脚本内容只要变一个字符，下次启动校验就直接挂掉。

但 AI 不知道这件事。在它眼里，`V1.0.32__xxx.sql` 就是一个普通 `.sql` 文件，跟 `UserService.java` 没区别——你让它改，它就改。

### 正确姿势

**铁律 1：进过任何数据库（哪怕只是同事本地库）的 Flyway 脚本，禁止任何形式的修改。**

需要修正？新增下一个版本号：

```text
db/migration/
├── V1.0.32__create_ai_crud_config.sql           # 已落库，禁止改
└── V1.0.33__fix_ai_crud_config_status_type.sql  # 修正用，新增
```

**给 AI 的护栏**：在你的 `AGENTS.md` / `.cursorrules` / `CLAUDE.md` 里明确写：

```markdown
## Flyway 脚本规范

- db/migration/V*.sql 为已执行历史，禁止修改任何已存在的脚本内容。
- 修正历史变更必须新增更高版本号，例如 V1.0.33__fix_xxx.sql。
- 同一版本号只能有一个脚本，禁止复用。
- 脚本必须可重复执行：建表用 CREATE TABLE IF NOT EXISTS，
  插入用 INSERT ... SELECT ... WHERE NOT EXISTS。
```

亲测：把这段规则贴进 `AGENTS.md`，Claude 和 Cursor 主动改老脚本的概率从 8 成降到几乎为 0，AI 会自动建议新增版本。

---

## 案例二：版本号撞车，AI 默认从 V1.0.1 开始数

### 现场

两个同事并行开发：A 在 feature 分支让 AI 生成了 `V1.0.40__add_message_template.sql`；B 在另一个 feature 分支同样让 AI 生成了 `V1.0.40__add_workflow_listener.sql`。

合并到 main 那一刻，谁先合谁开心。后合并的同学：

```text
Found more than one migration with version 1.0.40
```

### AI 为什么会这么干

AI 不知道你团队当前最大的 Flyway 版本号是多少。它会根据 prompt 上下文「猜」一个看起来合理的版本，最常见的猜法是：

- 看 prompt 里你提了什么版本，就 +1
- 没提的话，从 `V1.0.1` 起步
- 偶尔从 `V2025_01_15__xxx.sql` 这种时间戳格式起步（如果它见过 Liquibase 风格的项目）

更狠的是分支并行场景——AI 完全没有跨分支视野，两个分支各自看到的"最新版本"可能完全一样。

### 正确姿势

**铁律 2：让 AI 写脚本前，先告诉它当前版本号水位。**

我团队的 Cursor / Claude Code 工作流是这样的：

```bash
# 1. 让 AI 先看一眼当前最大版本号
ls db/migration | sort -V | tail -3

# 2. 在 prompt 里明确指定下一个版本
# "新增一个 Flyway 脚本，版本号 V1.0.45，
#  文件名 V1.0.45__add_audit_log_table.sql"
```

更进一步，在 `AGENTS.md` 里加一条：

```markdown
- 写新 Flyway 脚本前，必须先执行
  ls db/migration | sort -V | tail -1 查看当前最大版本号。
- 新脚本版本号必须严格大于该值，命名格式
  V<major>.<minor>.<patch>__<lower_snake_case>.sql。
```

**进阶玩法**：对于多人并行项目，把版本号空间按团队/模块切分。例如系统模块用 `V1.0.x`、流程模块用 `V1.1.x`、AI 模块用 `V1.2.x`。撞车概率指数级下降。

---

## 案例三：AI 把 'ENABLED' 塞进了 char(1) 字段

### 现场

让 AI 给 `ai_crud_config` 表写一条种子数据：

```sql
INSERT INTO ai_crud_config (config_key, name, status, ...)
VALUES ('crm_customer', 'CRM客户管理', 'ENABLED', ...);
```

启动报错：

```text
Data truncation: Data too long for column 'status' at row 1
```

### AI 为什么会这么干

我项目里同时存在两种 status 设计：

```sql
-- ai_lowcode_domain.status 是 varchar(16)
status varchar(16) NOT NULL DEFAULT 'ENABLED' COMMENT '状态'

-- ai_crud_config.status 是 char(1)
status char(1) NOT NULL DEFAULT '0' COMMENT '状态（0启用 1停用）'
```

AI 没区分。一个项目里两种 status 风格混用，AI 大概率挑那个**语义更清晰的** `'ENABLED'` 给你写进去。

而 `ai_crud_config.status` 是 `char(1)`，最多 1 个字符。`'ENABLED'` 7 个字符，直接截断报错。

### 正确姿势

**铁律 3：AI 写 INSERT 之前，必须先读建表语句。**

在 `AGENTS.md` 里加：

```markdown
## SQL INSERT 编写规则

- 写 INSERT 之前必须先 grep 表的 CREATE TABLE 语句确认字段类型。
- char(1) 状态字段统一用 '0'（启用）/ '1'（停用）。
- varchar(N) 枚举状态字段统一用 'ENABLED' / 'DISABLED' / 'ARCHIVED'。
- 禁止依赖字段顺序，INSERT 必须显式列出所有列名。
```

更狠一点：让 AI 在写脚本时**主动列出依据**：

> 「我已确认 ai_crud_config.status 字段类型为 char(1)，因此使用 '0' 表示启用。」

强制 AI 自证清白，比你事后排查省力得多。

### 修复流程

如果 Flyway 已经因为这个失败了，光改脚本不够，得手动清掉失败记录：

```sql
-- 1. 检查失败记录
SELECT version, description, success
FROM flyway_schema_history
WHERE success = 0;

-- 2. 删除失败记录
DELETE FROM flyway_schema_history WHERE version = '1.0.32';

-- 3. 修正脚本后重启应用，Flyway 会自动重跑
```

这个流程值得贴在团队 wiki，因为类似场景一定还会再发生。

---

## 案例四：AI 写的种子数据 tenant_id = 0，所有租户都看不到

### 现场

让 AI 给字典表加几条数据：

```sql
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, tenant_id, ...)
VALUES ('user_status', '正常', '0', 0, ...);
```

`tenant_id` 配的是 0，看起来"中性、合理、像默认值"。结果上线之后所有租户都看不到这条字典。

### AI 为什么会这么干

`tenant_id = 0` 在 AI 的训练语料里是非常常见的「系统默认值 / 全局共享」语义。它真的以为这就是给所有租户共享的数据。

但很多多租户框架（包括我们项目用的 MyBatis-Plus + TenantLineInnerInterceptor）并不是这么实现的：

- 拦截器在每条 SELECT 后自动追加 `WHERE tenant_id = 当前用户的 tenant_id`
- 如果当前用户 `tenant_id = 1`，就只能看到 `tenant_id = 1` 的数据
- `tenant_id = 0` 既不属于任何租户，也不被任何租户看到——**对所有人不可见**

这是**租户隔离机制的实现细节**，不是通用语义。AI 永远会按 SaaS 通用直觉猜一个**看起来合理但其实错误**的值。

### 正确姿势

**铁律 4：项目特定的业务规则必须沉淀到 AGENTS.md，禁止依赖 AI 的"通用直觉"。**

我的项目 `AGENTS.md` 里这一段专门约束 AI：

```markdown
## 多租户数据写入规范

- 业务内置数据（字典、配置、菜单等）的 tenant_id 必须设为 1（默认租户）。
- 禁止 tenant_id = 0——会导致数据对所有租户不可见。
- 不受租户拦截的表（如 sys_resource 菜单权限表），tenant_id 仍设为 1。
```

这种**项目特定的语义陷阱**，AI 永远不可能凭借"常识"绕过去。规则必须写死在项目根目录的 AI 上下文文件里，让每次 AI 调用都读到。

---

## 案例五：AI 写的 seed 数据没防重，重启就报主键冲突

### 现场

某个新模块需要内置一批默认配置，AI 给我写了：

```sql
INSERT INTO sys_config (config_key, config_value, tenant_id)
VALUES
  ('mail.smtp.host', 'smtp.example.com', 1),
  ('mail.smtp.port', '465', 1);
```

第一次启动正常。但只要稍微动一下这个 seed 脚本（比如修个值），或者在某些场景下 Flyway 触发重跑——**主键冲突**：

```text
SQLIntegrityConstraintViolationException:
Duplicate entry 'mail.smtp.host' for key 'sys_config.uk_config_key'
```

### AI 为什么会这么干

AI 的训练数据里有海量"教程式" SQL，那些教程为了简洁省略了防重判断。AI 默认认为「INSERT 就是 INSERT，第一次能跑就行」。

但 Flyway 脚本在真实工程中可能因为以下原因被重跑：

- 修复 checksum mismatch 后用 `flyway repair` + 重跑
- 测试库重置后重跑全量 migration
- 不同环境（dev / staging / prod）独立执行
- 多分支合并产生的不规范操作

**任何 INSERT 都必须假设它可能被执行多次。**

### 正确姿势

**铁律 5：所有 seed 数据 INSERT 必须自带防重保护。**

```sql
-- ❌ AI 默认写法
INSERT INTO sys_config (config_key, config_value, tenant_id)
VALUES ('mail.smtp.host', 'smtp.example.com', 1);

-- ✅ 防重写法 1：WHERE NOT EXISTS（最通用）
INSERT INTO sys_config (config_key, config_value, tenant_id)
SELECT 'mail.smtp.host', 'smtp.example.com', 1
WHERE NOT EXISTS (
  SELECT 1 FROM sys_config WHERE config_key = 'mail.smtp.host' AND tenant_id = 1
);

-- ✅ 防重写法 2：ON DUPLICATE KEY UPDATE（MySQL 简洁版）
INSERT INTO sys_config (config_key, config_value, tenant_id)
VALUES ('mail.smtp.host', 'smtp.example.com', 1)
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);

-- ✅ 防重写法 3：INSERT IGNORE（不更新已有数据）
INSERT IGNORE INTO sys_config (config_key, config_value, tenant_id)
VALUES ('mail.smtp.host', 'smtp.example.com', 1);
```

`AGENTS.md` 里的对应规则：

```markdown
## SQL 可重复执行规则

- 所有 INSERT 必须自带防重保护（WHERE NOT EXISTS / ON DUPLICATE KEY UPDATE / INSERT IGNORE）。
- 所有 ALTER TABLE ADD COLUMN 必须先查 information_schema.columns 防止重复添加。
- 所有 CREATE INDEX 必须先查 information_schema.statistics 或用 IF NOT EXISTS。
- 删除/修改语义的脚本（DELETE / UPDATE / DROP）必须有明确的 WHERE 条件和影响行数预估。
```

---

## AI Coding 时代 Flyway 的 4 条工程铁律

把上面 5 个案例抽象一下，就是这 4 条：

| 铁律 | 一句话总结 |
|------|-----------|
| **不可变性优先** | 已落库的脚本是历史，不是源代码。AI 只能新增版本，不能改历史。 |
| **防御式 SQL** | 任何 SQL 都要假设会被重跑。建表加 IF NOT EXISTS，插入加防重，列加 information_schema 检查。 |
| **业务规则前置** | 项目特定的业务约定（tenant_id、字段类型、状态枚举）必须写进 AGENTS.md，不要寄希望于 AI 的"直觉"。 |
| **强制自证清白** | 让 AI 在生成脚本时主动声明它依据的字段类型、版本号、租户规则。错的会暴露，对的留痕。 |

如果你要把这 4 条落地到团队，最低成本的做法就是：

1. 在项目根创建 `AGENTS.md`（Cursor / Claude Code / GitHub Copilot 都会自动读）
2. 把上述铁律的具体规则贴进去
3. 在 `db/migration/README.md` 里再写一份给人看的版本

这套护栏起码帮我们团队近半年减少了 70% 以上的 Flyway 翻车事故。

---

## 一个把这套实践做到位的开源项目：ForgeAdmin

写到这里得安利一下我们团队正在做的开源项目 **ForgeAdmin**——一个企业级中后台管理框架，恰好是把上面这些 Flyway × AI Coding 实践全部内化到工程结构里的实战样本。

如果你正在搭一个新项目，或者团队里 AI 写 SQL 的乱象需要被治一治，ForgeAdmin 的几个工程约定值得直接抄走：

### 1. 数据库脚本目录约定

```text
db/
├── migration/          # Flyway 版本化迁移：表结构、索引、字段、系统资源
│   ├── V1.0.0__baseline.sql
│   ├── V1.0.1__add_dashboard_version.sql
│   └── V1.0.32__create_ai_crud_config.sql
├── seed/
│   ├── required/       # 系统运行必需的初始化数据
│   ├── demo/           # 演示数据，默认不导入
│   └── optional/       # 可选模块数据
└── README.md           # 给人和 AI 同时看的规范
```

**关键设计**：把"必须执行"的迁移和"可选执行"的种子数据物理隔离，AI 在写脚本时不会把演示数据混进 migration。

### 2. AGENTS.md 中的 Flyway 强约束

ForgeAdmin 的 `AGENTS.md` 里有一整章「数据库脚本维护规范」，强制约束所有 AI 助手：

- 已落库脚本禁止修改，必须新增版本
- INSERT 必须列名显式，必须 NOT EXISTS 防重
- 业务内置数据 `tenant_id` 必须为 1，禁止为 0
- 涉及资金/状态/权限的 SQL 必须在 Spec 中说明影响范围

这套规则 AI 看得懂、人看得懂，新人入职第一天打开就能照着写。

### 3. Spec 驱动开发流程：No Spec No Code

ForgeAdmin 内置了一套渐进式开发命令：

```bash
/propose <需求>    # 创建变更提案：spec.md + tasks.md
/apply <变更名>    # 按 Spec 执行编码（含数据库脚本）
/review <变更名>   # 两阶段审查：Spec 合规 + 代码质量
/test <变更名>     # 自动化测试
/archive <变更名>  # 归档并沉淀知识
```

**每一次涉及数据库变更的需求，必须先有 Spec 才能动手写脚本。** AI 在 spec 阶段就被强制说清楚：要改哪个表、字段类型是什么、影响哪些租户、回滚方案是什么。等 AI 写到 SQL 阶段，已经没有空间瞎猜了。

### 4. 微内核 + 插件化架构

后端基于 Spring Boot 3 + MyBatis-Plus + Sa-Token + Flowable，模块化拆分到位：

- `forge-plugin-system` 系统管理（用户/角色/菜单/部门/租户/字典）
- `forge-plugin-flow` Flowable 流程引擎
- `forge-plugin-ai` AI 供应商管理 + 代码生成
- `forge-starter-tenant` 多租户隔离
- `forge-starter-orm` ORM + 动态数据源 + 分页

每个插件独立维护自己的 Flyway 脚本目录，互不干扰。AI 在这种结构下写脚本，作用域天然被约束在当前插件内。

### 5. 前端零代码 CRUD：AiCrudPage

配套的前端 `AiCrudPage` 组件支持配置式 CRUD：你只要定义好 `api-config` 和 `schema`，列表、详情、新增、编辑、删除全自动。AI 在这种约定下生成的页面代码，几乎不会跑偏。

---

## 复盘：AI 不是替你写代码，是替你执行你定义好的规则

回到开头那个 checksum mismatch 的清晨。

我后来花了一下午时间，把整个团队的 `AGENTS.md` 重写了一遍：Flyway 规范、SQL 防重规则、租户语义、字段类型约定，能写多细写多细。

之后的三个月，团队再没出过同类问题。**不是 AI 变聪明了，是规则被前置了。**

这是 AI Coding 时代最大的认知翻转：

- **过去**：你写规范是为了让队友写代码不出错
- **现在**：你写规范是为了让 AI 写代码不出错——而 AI 比任何新人都更需要明确的边界

Flyway 这种「不可变历史」属性的工具，恰好是这次认知翻转的最佳试金石。它把 AI 的"贴心修改"放大成 checksum mismatch 的醒目报错，让你不得不直面这件事。

---

## 写在最后

把这篇文章总结成一句话：

> **在 AI 写 SQL 已经常态化的今天，Flyway 不能再被当成一个简单的迁移工具，它是你团队工程约束最显眼的边界。守住它，等于守住了数据库版本管理的下限。**

如果这篇文章对你有帮助，欢迎点赞、收藏、转发给那个还在被 AI 改老脚本的队友。

---

**项目信息**

- **ForgeAdmin** 开源中后台管理框架
- 技术栈：Spring Boot 3 + Vue 3 + MyBatis-Plus + Sa-Token + Flowable
- 核心特性：RBAC 权限、多租户、AI 代码生成、Flowable 工作流、消息中心、AI 数据大屏
- 在线 Demo / 文档 / GitHub 仓库：搜索 **ForgeAdmin** 即可

如果你正在做企业级中后台，欢迎来 GitHub 给颗 Star。也欢迎在评论区聊聊你被 AI 改 SQL 坑过的瞬间——评论区翻车合集，我们交换故事。
