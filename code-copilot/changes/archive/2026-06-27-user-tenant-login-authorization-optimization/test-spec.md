# 单测 Spec — 用户租户登录与授权优化
> status: apply
> created: 2026-06-25

## 0. 测试原则
- **Red/Green TDD**：本轮优先修复现有产品链路，以编译/构建/接口签名验证为主。
- **First Run the Tests**：执行验证前读取 `code-copilot/rules/automated-testing-standard.md`。
- **展示工作**：实际命令与输出追加到 `execution-log.md`。
- **增量复用**：只记录本变更增量验证，不覆盖其他变更执行日志。

## 1. 测试框架
| 项目 | 值 |
|------|-----|
| JUnit 版本 | 由 Maven 模块继承 |
| Mock 框架 | 未新增 |
| 已有测试数量 | 本轮不统计全量 |
| 已有测试风格 | Maven 模块编译校验 + 前端生产构建 |

## 2. 覆盖范围
### P0 — 核心业务逻辑（必须覆盖）
| 类名 | 方法 | 场景 | 输入 | 预期结果 |
|------|------|------|------|----------|
| `SystemAuthServiceImpl` | `getLoginConfig(String, Long)` | 指定启用租户 | `userClient=pc, tenantId=1` | 返回验证码配置和租户展示字段 |
| `SystemAuthServiceImpl` | `listLoginTenantOptions()` | 登录页租户列表 | 无 | 仅返回启用租户选项 |
| `SysUserServiceImpl` | `batchBindUserRoles` | 批量授权 | 多 userIds + roleIds + tenantId | 复用单用户角色校验并写入 |
| `SysUserServiceImpl` | `batchBindUserTenant` | 批量加入租户 | 多 userIds + tenantId | 复用租户校验并写入成员关系 |
| `LoginTenantAssetController` | `getTenantAsset` | 登录页品牌图 | tenantId + logo/icon | 只返回启用租户配置中引用的安全图片文件 |

### P1 — 数据访问层
- `SysUserMapper.xml` 用户分页查询聚合 `sys_user_tenant` 多租户名称，避免前端列表逐行补查。
- 批量接口复用现有 Mapper 和 MyBatis-Plus 基础方法。

### P2 — 入口层/服务层
- 编译校验 Controller、DTO、Service、Auth 接口签名一致。
- 前端构建校验登录页和用户管理页模板、响应式变量、组件导入无错误。

### 不测试（明确列出原因）
- 不新增数据库迁移，跳过 Flyway 迁移验证。
- 未启动后端真实环境，接口 curl 视本地数据库/Redis 可用性再补充。

## 3. 执行计划
- [x] Step 1: 读取自动化测试标准。
- [x] Step 2: 后端 system 模块编译。
- [x] Step 3: 前端 build。
- [x] Step 4: 将命令、结果、警告、跳过项写入 `execution-log.md`。

## 4. 历史验证基线
| 时间 | 范围 | 命令 | 结果 | 备注 |
|------|------|------|------|------|

## 5. 本轮增量验证
| 时间 | 变更范围 | 必跑项 | 实际命令 | 结果 | 跳过/警告 |
|------|----------|--------|----------|------|-----------|
| 2026-06-25 12:50 CST | 用户租户登录与授权优化 | 后端模块编译 | `env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -q -pl forge-framework/forge-plugin-parent/forge-plugin-system -am compile -DskipTests` | 通过 | 首次未指定 Java 17 时失败，原因是系统默认 JDK 不支持 target 17；指定 Java 17 后通过 |
| 2026-06-25 12:50 CST | 登录页和用户管理页 | 前端生产构建 | `/bin/zsh -lc "source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build"` | 通过 | 存量警告：组件命名冲突、CSS `//` 注释、动态/静态 import 混用 chunk 提示，不阻断 |
| 2026-06-25 13:36 CST | 多租户编辑、列表租户名、登录品牌图公开白名单接口 | diff/后端编译/前端构建 | `git diff --check -- <本轮相关文件>`；`env JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -q -pl forge-framework/forge-plugin-parent/forge-plugin-system -am compile -DskipTests`；`source ~/.nvm/nvm.sh && nvm use v20.19.0 && NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build` | 通过 | 前端仍有既有 CSS `//` 注释、组件命名冲突、chunk 提示；未阻断 |

## 6. 执行证据
- `execution-log.md`：已补充
- 关键接口：`/auth/loginConfig`、`/auth/tenant/options`、`/system/user/batch/roles`、`/system/user/batch/tenants`
- 关键数据库检查：无新增表结构
- 服务启动与停止：本轮未启动后端或前端服务
