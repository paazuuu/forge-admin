# AI Generate P3 Optimization Tasks

- [x] 创建 P3 变更说明文档。
- [x] 扩展 P3 文档，纳入数据资产血缘、影响分析和 prompt 脱敏要求。
- [x] 新增 report-ui 数据集运行时预检工具。
- [x] 在 AI 助手中展示数据预检状态和刷新入口。
- [x] 将预检结果写入 `compactBusinessContext` 的提示词上下文。
- [x] 生成前自动补齐预检缓存，失败时保留降级提示。
- [x] 新增后端 AI 大屏组件血缘表、实体、Mapper XML、Service 和影响分析接口。
- [x] 生成记录保存时写入组件血缘项。
- [x] 清洗 prompt 字段上下文：隐藏字段过滤，脱敏字段只给安全说明。
- [x] 后端 AI 上下文按当前用户数据集权限过滤，并服务端清洗 HIDDEN/MASK 字段。
- [x] 数据资产管理数据集下架前展示 AI 大屏血缘影响提示。
- [x] 运行 `git diff --check`、后端 Maven 编译和 `forge-report-ui pnpm build`。
- [x] 运行 `forge-admin-ui pnpm build`。
