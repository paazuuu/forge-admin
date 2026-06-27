# Security Sensitive Data Hardening
> status: done

## 背景

当前安全评估指出 5 类敏感信息与上传面风险：

- `/system/config/page` 返回系统参数时暴露 `sys.user.initPassword` 明文默认密码。
- `/system/user/page` 返回用户分页时包含 `password`、`salt` 等认证字段，并直接暴露手机号、身份证号等 PII。
- `/system/cache/page` 与 `/system/cache/getInfo` 可枚举 Redis 敏感键和值，包括加密会话密钥、Sa-Token token/session 等。
- `/system/storage/config/default` 可能返回 OSS AccessKey/SecretKey 等云存储凭据。
- `/api/file/upload` 允许认证用户上传 HTML、JSP、PHP 等高风险文件，文件类型校验依赖存储配置且默认配置可能为空。

## 目标

1. 系统配置接口不再返回密码、密钥、token、secret 等敏感配置明文。
2. 用户分页、详情、个人资料接口不再返回密码哈希和盐；用户列表手机号、身份证号等 PII 默认脱敏。
3. 缓存管理接口只允许超级管理员访问，并对敏感缓存键做列表过滤、详情拒绝、删除保护。
4. 文件存储配置普通上传场景只返回安全元数据，管理接口返回的 AK/SK 默认脱敏。
5. 上传接口强制执行安全文件类型策略：限制扩展名、拒绝脚本/网页可执行类文件、校验 MIME 与扩展名基本一致。
6. 增加用户级强制改密标记，支持“初始密码/管理员重置后首次登录必须修改密码”。
7. 迁移脚本清空默认密码配置、为存量用户标记强制改密、补齐默认上传白名单。

## 非目标

- 不在代码中提交新的固定默认密码。
- 不在代码中硬编码本次泄露的真实密码、AK/SK。
- 不直接替生产环境吊销云厂商密钥；该动作必须由运维在云平台完成。
- 不静默生成用户新密码并通知用户；生产批量密码重置需要配套短信、邮件或人工通知流程。

## 设计

### 1. 系统配置敏感值处理

- 新增统一敏感键判断工具，识别包含 `password`、`secret`、`token`、`key`、`credential`、`ak`、`sk` 等关键字的配置键。
- `SysConfigController` 对 page/list/detail 返回对象做脱敏拷贝，敏感 `configValue` 返回 `******`。
- `/system/config/configKey/{configKey}` 对敏感配置键拒绝返回明文。
- `SysConfigServiceImpl.updateConfig` 对敏感配置支持“脱敏占位值不更新原值”，避免前端保存 `******` 覆盖真实值。
- `sys.user.initPassword` 作为高危默认密码配置，通过迁移脚本清空并停用。

### 2. 用户数据返回面收口

- `SysUser.password`、`SysUser.salt` 使用 `@JsonIgnore`，从所有 JSON 响应中排除。
- 新增 `forcePasswordChange` 字段，用于登录态强制改密。
- `/system/user/page` 返回前对 `phone`、`idCard`、`email` 做脱敏；详情接口只清空认证字段，不脱敏需要编辑的字段。
- `/auth/userInfo`、`/system/user/profile` 返回前清空认证字段，并保留 `forcePasswordChange` 供前端判断。

### 3. 强制改密

- `sys_user` 新增 `force_password_change`，默认 `0`。
- 新增用户、管理员重置密码时设置为 `1`。
- 用户通过 `/auth/changePassword` 修改成功后设置为 `0`。
- 登录构建 `LoginUser` 时带出 `forcePasswordChange`。
- `ApiPermissionInterceptor` 在 API 权限判断前检查登录用户强制改密状态；若为 `true`，只允许访问 `/auth/userInfo`、`/auth/changePassword`、`/auth/logout` 和必要密钥交换接口。
- 前端登录后如发现 `forcePasswordChange=true`，跳转 `/profile` 并提示必须改密；路由守卫阻止访问其他页面。

### 4. 缓存敏感键保护

- `SysCacheController` 定义敏感缓存键前缀/模式：
  - `crypto:session:*`
  - `Authorization:login:token:*`
  - `Authorization:login:token-session:*`
  - `Authorization:login:last-active:*`
  - `satoken:*`
  - `auth:sso:*`
- 分页列表默认过滤敏感键，不返回 key 和 valuePreview。
- 精确查询、删除、批量删除、按模式清理遇到敏感键时拒绝操作。
- 保留超级管理员访问控制；敏感键仍不能通过缓存接口读取值。

### 5. 文件存储配置凭据保护

- `/system/storage/config/default` 继续返回上传组件所需安全字段，不含 endpoint/accessKey/secretKey/bucket/domain/basePath。
- 管理类 page/detail 返回的 `accessKey`、`secretKey` 脱敏显示。
- 新增/编辑保存时，若前端提交脱敏占位值，不覆盖原凭据。
- `/system/storage/config/options` 保持只返回 id、configName、storageType、isDefault、enabled。

### 6. 上传安全校验

- `FileManager` 对所有 `MultipartFile` 和流式上传统一执行：
  - 文件名非空、扩展名非空。
  - 高风险扩展名拒绝：`jsp,jspx,php,asp,aspx,html,htm,js,mjs,ts,vue,sh,bat,cmd,exe,dll,jar,war,ear,sql` 等。
  - 若存储配置未配置 `allowedTypes`，使用默认安全白名单：`jpg,jpeg,png,gif,webp,pdf,doc,docx,xls,xlsx,txt,csv,zip,rar,mp4,mp3`。
  - MIME 与扩展名做基础匹配，高风险脚本 MIME 拒绝。
  - 最大文件大小无配置时使用安全默认值。
- 分片上传初始化也校验文件名扩展名；分片内容无法完整 MIME 扫描，保留完成后元数据保存前的文件名策略。

## 数据库迁移

新增 `V1.0.67__harden_sensitive_data_and_upload_controls.sql`：

- `sys_user` 增加 `force_password_change tinyint(1) NOT NULL DEFAULT 0`。
- 存量用户 `force_password_change=1`，用于强制所有已存在账号下次登录先改密。
- `sys_config` 中 `sys.user.initPassword` 清空 `config_value` 并设置为停用或非系统内置。
- `sys_file_storage_config.allowed_types` 为空时回填默认安全白名单，并移除 `svg`、`md` 等容易承载主动内容的默认公共上传类型。

## 验收标准

- `/system/config/page` 和 `/system/config/getById` 不返回 `sys.user.initPassword` 明文。
- `/system/user/page` 响应不含 `password`、`salt`，手机号/身份证号已脱敏。
- `/system/cache/page` 不出现 `crypto:session`、`Authorization:login`、`satoken` 等敏感缓存键；`getInfo/remove/clear` 不能操作这些键。
- `/system/storage/config/default` 不含 AK/SK；管理页只显示脱敏值。
- `/api/file/upload` 上传 `.jsp`、`.php`、`.html`、`.js` 等文件失败。
- 被标记 `forcePasswordChange=true` 的用户登录后只能访问改密相关接口，改密成功后恢复正常访问。

## 生产处置要求

- 立即吊销评估报告中已泄露的 OSS AccessKey/SecretKey，并替换为最小权限新密钥。
- 批量重置所有仍使用泄露默认密码的账号密码；如无法立即通知用户，至少临时禁用或锁定风险账号。
- 开启 OSS 访问日志、异常告警和必要的 IP 白名单。
- 对用户枚举、批量登录失败、缓存管理、文件上传增加审计关注。

## 归档记录（HARD-GATE）
- **状态**：done
- **归档时间**：2026-06-27
- **归档人**：yaomd（批量归档）
- **归档路径**：code-copilot/changes/archive/2026-06-27-security-sensitive-data-hardening/
- **判定依据**：任务清单全部完成，execution-log 验证通过（编译/构建/lint 闭环）。
