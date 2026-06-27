# Tasks

- [x] 定位 `/system/config/page`、`/system/user/page`、`/system/cache/page`、`/system/storage/config/default`、`/api/file/upload` 实现位置。
- [x] 明确已有 `V1.0.65` 已对部分平台入口做用户类型边界，但 Controller 仍需服务端兜底。
- [x] 新增敏感字段脱敏工具，复用到系统配置、文件存储配置、缓存值预览。
- [x] 系统配置 page/list/detail/key 查询隐藏或拒绝敏感配置明文。
- [x] 用户实体隐藏 `password/salt`，列表返回手机号/身份证号/邮箱脱敏。
- [x] 增加 `forcePasswordChange` 字段并接入登录态、管理员重置、新增用户、用户改密。
- [x] 在 API 拦截器中限制强制改密用户访问业务接口。
- [x] 缓存管理接口过滤敏感键，禁止读取/删除/清空敏感缓存。
- [x] 文件存储配置接口返回 AK/SK 脱敏，保存时避免占位值覆盖原密钥。
- [x] 文件上传增加默认安全白名单、高风险扩展名拒绝和 MIME 校验。
- [x] 前端登录成功和路由守卫接入强制改密状态，跳转个人中心改密。
- [x] 新增 Flyway 迁移脚本补齐 `force_password_change`、清空默认密码配置、修正默认上传白名单。
- [x] 按自动化测试标准执行增量验证并记录到 `execution-log.md`。
