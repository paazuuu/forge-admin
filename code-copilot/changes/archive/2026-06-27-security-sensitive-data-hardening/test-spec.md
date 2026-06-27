# Test Spec

## 变更范围

- 后端：`forge-plugin-system`、`forge-starter-auth`、`forge-starter-core`、`forge-starter-file`
- 前端：`forge-admin-ui` 登录成功处理、路由守卫、用户 Store/Helper
- 数据库：`forge-server/db/migration/V1.0.67__harden_sensitive_data_and_upload_controls.sql`

## 静态验证

1. 检查敏感字段不再序列化：
   - `SysUser.password`、`SysUser.salt` 带 `@JsonIgnore`。
   - `LoginUser.forcePasswordChange` 能正常序列化给前端。
2. 检查缓存敏感键模式：
   - `crypto:session:*`
   - `Authorization:login:*`
   - `satoken:*`
   - `auth:sso:*`
3. 检查上传拒绝列表包含：
   - `jsp,jspx,php,asp,aspx,html,htm,js,mjs,ts,vue,sh,bat,cmd,exe,dll,jar,war,ear,sql`

## 后端编译验证

执行：

```bash
cd forge-server && mvn -q -pl forge-admin-server -am -DskipTests compile
```

预期：

- 编译成功。
- 无新增 Java 语法错误、Mapper XML 解析错误或模块依赖缺失。

## 前端构建验证

执行：

```bash
source ~/.nvm/nvm.sh && nvm use v20.19.0
NODE_OPTIONS=--max-old-space-size=8192 pnpm --dir forge-admin-ui build
```

预期：

- 构建成功。
- 允许保留既有 chunk size 或 UnoCSS warning。

## 接口验证建议

需要本地后端与数据库可用时执行：

1. 登录获取 token。
2. 请求 `/system/user/page?pageNum=1&pageSize=10`，确认响应不含 `password`、`salt`，手机号/身份证号脱敏。
3. 请求 `/system/config/page?pageNum=1&pageSize=10&configKey=sys.user.initPassword`，确认 `configValue` 不为明文。
4. 请求 `/system/cache/page?pattern=crypto:session:*`，确认敏感键不可枚举。
5. 请求 `/system/cache/getInfo?key=crypto:session:test`，确认被拒绝。
6. 请求 `/system/storage/config/default`，确认响应不含 `accessKey`、`secretKey`。
7. 上传 `.jsp`、`.html` 文件到 `/api/file/upload`，确认失败。
8. 标记当前用户 `force_password_change=1` 后登录，确认只能访问 `/profile` 并可修改密码；改密后 `force_password_change=0`。

## 跳过项

- 生产 OSS AK/SK 吊销、IP 白名单、访问日志开启由云平台/运维执行，不在自动化测试中验证。
- 批量密码重置需要通知流程，不通过 Flyway 直接生成并分发新密码。
