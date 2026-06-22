# 业务数据源配置教程

业务数据源用于让 `forge-business` 手写业务模块按当前租户切换到对应业务库。它和低代码运行数据源不是一回事：

| 类型 | 控制范围 | 典型使用方 |
|------|----------|------------|
| 平台主数据源 | 用户、租户、菜单、权限、字典、系统配置、低代码元数据 | Forge 平台自身 |
| 低代码运行数据源 | 某个低代码业务对象的动态 CRUD 读写库 | 低代码应用中心 |
| 租户业务数据源 | 某个租户下 `forge-business` 模块的 MyBatis-Plus Mapper/XML 读写库 | 手写业务模块 |

租户业务数据源只影响显式声明了 `@TenantBusinessDataSource` 或使用 `TenantBusinessDataSourceExecutor` 的业务逻辑。平台表 `sys_*`、低代码元数据表和数据权限控制面元数据仍然走主库。

## 配置前提

完成页面配置之前，后端必须已经在 baomidou dynamic-datasource 中声明可用的业务库。租户表里保存的 `default_business_datasource_code` 必须和 dynamic-datasource 的 dsKey 完全一致。

```yaml
spring:
  datasource:
    dynamic:
      primary: master
      strict: true
      datasource:
        master:
          url: jdbc:mysql://127.0.0.1:3306/forge_admin
          username: root
          password: your-password
          driver-class-name: com.mysql.cj.jdbc.Driver
        tenant_biz_demo:
          url: jdbc:mysql://127.0.0.1:3306/forge_tenant_biz_demo
          username: root
          password: your-password
          driver-class-name: com.mysql.cj.jdbc.Driver

forge:
  business:
    datasource:
      enabled: true
      tenant-routing-enabled-default: false
```

还需要把系统配置 `business.datasource.tenant-routing-enabled` 调整为 `true`。Flyway 会初始化该配置，默认值是 `false`，目的是避免未准备业务库时误切库。

## 操作流程

### 1. 新增租户业务数据源

进入“开发者工具 / 数据源管理”，点击“新增数据源”。

重点字段：

| 字段 | 填写说明 |
|------|----------|
| 数据源名称 | 给运维和业务人员看的名称，例如“租户 A 业务库” |
| 数据源编码 | 必须等于 `spring.datasource.dynamic.datasource` 下的 dsKey，例如 `tenant_biz_demo` |
| 用途范围 | 选择“租户业务” |
| 风险等级 | 旧系统生产库建议选“高” |
| 只读模式 | 只读库开启后会禁止运行写入和 DDL |
| 允许运行写入 | 业务调试和正式写入需要设置为“允许” |
| 允许运行 DDL | 租户业务库一般保持“禁止”，除非明确允许平台创建演示表或业务表 |
| 启用状态 | 必须为“启用”才会出现在租户配置下拉框 |

![数据源管理配置租户业务用途](/images/business-datasource/datasource-config.svg)

> 注意：这里的数据源记录用于后台页面选择、权限提示和连接测试。真正的 ORM 切库仍由 dynamic-datasource 控制，所以只在页面新增数据源、不在 `application.yml` 配置同名 dsKey，会在运行时提示“租户业务数据源未在 dynamic-datasource 中配置”。

### 2. 绑定租户默认业务库

进入“系统管理 / 租户管理”，编辑目标租户，在“业务数据源”分组中选择“默认业务库”。

保存后，租户表会记录：

- `default_business_datasource_id`：页面选择的数据源 ID。
- `default_business_datasource_code`：实际用于 dynamic-datasource 切换的 dsKey。

如果不选择默认业务库，`forge-business` 会回退到 `master`。

![租户管理绑定默认业务库](/images/business-datasource/tenant-binding.svg)

### 3. 打开业务数据源调试台验证

进入“应用中心 / 业务数据源调试”。

推荐验证顺序：

1. 选择租户。
2. 点击“检测路由”，确认“租户配置”和“线程 dsKey”是否为目标 dsKey。
3. 点击“写入测试记录”，调试接口会在当前路由的数据源中自动创建 `business_datasource_demo` 表并写入一条记录。
4. 点击“读取记录”，确认记录只出现在当前租户对应业务库。

![业务数据源调试台验证路由](/images/business-datasource/debug-routing.svg)

调试台返回的关键字段：

| 字段 | 判断方式 |
|------|----------|
| 当前数据库 | 应显示为目标业务库名称 |
| 线程 dsKey | 应等于租户绑定的 `default_business_datasource_code` |
| 路由状态 | 业务库表示已切换；主库表示配置关闭或租户未绑定 |
| routeKey | 写入演示记录时记录当前 dynamic-datasource key |

## 业务代码如何接入

普通 Web 请求中，业务 Service 标注 `@TenantBusinessDataSource` 即可。Mapper 仍然按 MyBatis-Plus 和 XML 正常写，不需要手动拿 `JdbcTemplate`。

```java
@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractMapper contractMapper;

    @TenantBusinessDataSource
    @Transactional(rollbackFor = Exception.class)
    public Long createContract(Contract contract) {
        contractMapper.insert(contract);
        return contract.getId();
    }
}
```

定时任务、消息消费、补偿任务这类没有登录态的场景，应显式传入租户 ID：

```java
@Component
@RequiredArgsConstructor
public class ContractJob {

    private final TenantBusinessDataSourceExecutor dataSourceExecutor;
    private final ContractMapper contractMapper;

    public void refreshTenantContracts(Long tenantId) {
        dataSourceExecutor.execute(tenantId, () -> {
            contractMapper.refreshStatistics();
            return null;
        });
    }
}
```

`@Async` 和项目线程池会通过 `TenantBusinessDataSourceTaskDecorator` 传播租户和 dynamic-datasource 上下文；任务结束后会恢复线程原上下文，避免串库。

## 数据权限和平台表

切到业务库以后，业务 SQL 不能再依赖业务库里存在平台表。当前实现已经做了控制面隔离：

- `sys_data_scope_config`
- `sys_role`
- `sys_role_data_scope`
- `sys_org`
- `sys_region_code`

这些表会从 `forge.datascope.metadata-datasource` 加载，默认是 `master`。数据权限拦截器运行时只读取内存快照，并把行政区划权限解析成业务库可执行的字面量条件，不会在业务库里查询 `sys_region_code`。

如果你的业务 Mapper 需要数据权限，请确保业务主表自身存在配置中引用的业务字段，例如 `tenant_id`、`create_by`、`create_dept`、`region_code`。自定义数据权限 SQL 不要写跨平台表子查询。

## 常见问题

### 选择了租户业务库，但调试台仍显示 master

按顺序检查：

1. `forge.business.datasource.enabled` 是否为 `true`。
2. 系统配置 `business.datasource.tenant-routing-enabled` 是否为 `true`。
3. 租户是否保存了“默认业务库”。
4. 数据源编码是否和 dynamic-datasource dsKey 完全一致。
5. 业务方法是否标注 `@TenantBusinessDataSource`，或是否通过 `TenantBusinessDataSourceExecutor` 执行。

### 报“租户业务数据源未在 dynamic-datasource 中配置”

说明租户表保存了某个 `default_business_datasource_code`，但 `spring.datasource.dynamic.datasource` 中没有同名 dsKey。把两边编码改一致后重启后端。

### 报 `Table '<业务库>.sys_data_scope_config' doesn't exist`

说明还有旧代码或自定义 SQL 在业务库查询平台控制面表。新实现下数据权限控制面元数据应从 `master` 快照读取；业务库只保存业务表。检查自定义数据权限 SQL，避免引用 `sys_*` 平台表。

### 写入测试记录失败

调试接口会自动创建 `business_datasource_demo` 表。若失败，通常是业务库账号没有建表权限，或数据源被配置为只读、禁止写入。生产库不建议用调试写入，先在测试业务库验证路由。

## 推荐验收清单

- 数据源管理中存在用途为“租户业务”的数据源，且编码等于 dynamic-datasource dsKey。
- 租户管理中目标租户已绑定默认业务库。
- 全局开关和系统配置开关都已启用。
- 调试台“检测路由”显示目标 dsKey。
- 调试台写入记录后，目标业务库出现 `business_datasource_demo` 表和对应租户数据。
- 实际业务 Service 使用 `@TenantBusinessDataSource`，Mapper/XML 查询不引用平台表。
