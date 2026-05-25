# 动态低代码页面数据权限测试规格

## 1. 测试目标
- 验证低代码动态 CRUD 在 `FOLLOW_SYSTEM` 策略下复用当前登录人的系统角色数据范围。
- 验证默认 `TENANT` 策略保持历史兼容，只做租户隔离和逻辑删除过滤。
- 验证读写接口都由后端追加权限条件，前端只负责配置和展示。
- 验证角色数据范围新旧编码兼容，历史角色升级后不会被错误解释。

## 2. 基础测试数据
建议准备一张低代码业务表，至少包含以下列：

| 列名 | 用途 |
|------|------|
| `id` | 主键 |
| `tenant_id` | 租户隔离 |
| `create_by` | 本人数据范围 |
| `org_id` | 业务归属组织 |
| `create_dept` | 创建部门兜底组织字段 |
| `region_code` | 行政区划数据范围 |
| `parent_id` | 树形父级 |
| `del_flag` | 逻辑删除 |

低代码模型策略：
- 兼容策略：`dataScope=TENANT`。
- 系统策略：`dataScope=FOLLOW_SYSTEM`，字段映射为 `userColumn=create_by`、`orgColumn=org_id`、`regionColumn=region_code`、`tenantColumn=tenant_id`、`logicDeleteColumn=del_flag`。

## 3. 角色范围矩阵
| 角色数据范围 | 预期条件 | 验收点 |
|--------------|----------|--------|
| `1` 全部 | 不追加组织、本人、区划条件 | 仍保留动态仓储的租户和逻辑删除基础过滤 |
| `2` 本租户 | `tenant_id = 当前租户` | 可见同租户数据，不跨租户 |
| `3` 本组织 | `org_id IN 当前用户组织` | 只看本组织数据 |
| `4` 本组织及子组织 | `org_id IN 当前组织及所有子组织` | 子组织数据可见，兄弟组织不可见 |
| `5` 个人 | `create_by = 当前用户` | 无自定义组织 ID 时按个人执行 |
| `5` 自定义组织兼容 | `org_id IN sys_role_data_scope` | 存在自定义组织 ID 时按旧枚举 `CUSTOM` 执行 |
| `6` 本租户兼容 | `tenant_id = 当前租户` | 兼容旧后端枚举 |
| `7` 行政区划 | `region_code = 当前区划 OR 直接下级区划` | 省级用户视为全部；市级及以下匹配本级和下级 |

## 4. 接口验收用例
| 场景 | 接口 | 预期 |
|------|------|------|
| 单表分页 | `GET /ai/crud/{configKey}/page` | 返回结果满足当前角色数据范围 |
| Join 分页 | `GET /ai/crud/{configKey}/page`，带 Join 运行时配置 | 按主表别名 `t0` 追加权限条件 |
| 树形查询 | `GET /ai/crud/{configKey}/tree` | 只返回有权限节点，并补齐导航祖先节点 |
| 详情 | `GET /ai/crud/{configKey}/{id}` | 越权 ID 返回无权限或不存在业务异常 |
| 编辑 | `PUT /ai/crud/{configKey}` | 越权 ID 影响行数为 0 时返回业务异常 |
| 删除 | `DELETE /ai/crud/{configKey}/{id}` | 越权 ID 不允许删除 |
| 批量删除 | 前端批量触发单条删除接口 | 任一越权记录不能静默成功 |
| 自定义查询 | `POST /ai/custom-query/{configKey}/execute` | 字段白名单和动态数据权限同时生效 |
| 导出 | `POST /ai/crud/{configKey}/export` | 导出结果与分页权限结果一致 |

## 5. 发布校验用例
| 场景 | 预期 |
|------|------|
| `FOLLOW_SYSTEM` 缺少 `tenantColumn` | 发布失败并提示缺少租户字段映射 |
| `FOLLOW_SYSTEM` 缺少 `userColumn` | 发布失败并提示缺少本人数据权限字段 |
| `FOLLOW_SYSTEM` 缺少 `orgColumn` | 发布失败并提示缺少组织数据权限字段 |
| 配置字段存在于模型但真实表列不存在 | 发布失败并提示字段不存在 |
| `SYSTEM_DATA_SCOPE` 历史值 | 保存、发布、运行时归一化为 `FOLLOW_SYSTEM` |
| 默认空策略 | 归一化为 `TENANT`，不改变历史页面行为 |

## 6. 树形与组织筛选用例
| 场景 | 预期 |
|------|------|
| 左树右表选择父级并勾选查询子级 | 查询值扩展为父级和子级集合 |
| 查询子级结果超出角色范围 | 与系统权限取交集，不扩大可见范围 |
| 补齐祖先节点 | 返回 `_scopeAncestor=true`、`_dataScopeWritable=false` |
| 操作补齐祖先节点 | 前端阻断编辑、删除、添加下级；后端写接口仍按权限条件校验 |

## 7. 验证命令
```bash
cd forge
JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.13/libexec/openjdk.jdk/Contents/Home mvn -pl forge-admin-server -am compile -DskipTests

cd ../forge-admin-ui
source ~/.nvm/nvm.sh && nvm use v20.19.0
pnpm exec eslint src/components/ai-form/AiCrudPage.vue src/components/lowcode-builder/model/model-schema.js src/components/lowcode-builder/model/LowcodeModelDesigner.vue src/views/ai/lowcode-builder.vue src/views/ai/lowcode-models.vue
pnpm build
```

## 8. 当前验证结果
- 后端 Maven 编译通过。
- 前端定向 ESLint 通过。
- 前端 build 通过；存在既有 UnoCSS missing-icon、CSS `//` 注释、动态导入提示和 chunk-size warning，不阻断产物生成。
- 接口级角色矩阵需在具备真实用户、角色、组织、区划和低代码业务数据的环境中回归。
