# 二次开发实战教程：从零开发一个业务模块

> 本教程以项目中内置的「员工管理」模块为案例，从数据库建表 → 后端代码 → 前端页面 → 权限配置 → 测试验证，手把手带你走完一个完整 CRUD 模块的开发全流程。
>
> 阅读本教程前，请确保你已：
> - 拉取项目代码并能在本地运行
> - 阅读《[权限体系完整配置指南](./permission-guide.md)》了解 RBAC 权限模型
> - 熟悉 Spring Boot + MyBatis-Plus + Vue 3 基本开发

---

## 一、开发前准备

### 1.1 环境要求

| 环境 | 版本 | 验证命令 |
|------|------|---------|
| JDK | 17+ | `java -version` |
| Maven | 3.8+ | `mvn -version` |
| MySQL | 8.0+ | `mysql --version` |
| Redis | 6.0+ | `redis-cli ping` |
| Node.js | 20.19+ | `node -v` |
| pnpm | 8+ | `pnpm -v` |

### 1.2 项目结构速览

```
forge-admin/
├── forge-server/                       # 后端工程
│   ├── forge-admin-server/             # ⭐ 主应用（业务模块写在这里）
│   │   └── src/main/java/com/mdframe/forge/
│   │       ├── employee/               # 示例：员工管理模块
│   │       │   ├── controller/
│   │       │   ├── service/
│   │       │   ├── mapper/
│   │       │   ├── entity/
│   │       │   └── dto/
│   │       └── ...
│   └── forge-framework/                # 框架层
│       ├── forge-starter-parent/        # 技术 Starter（auth/cache/orm/tenant...）
│       └── forge-plugin-parent/         # 业务 Plugin（system/generator/ai...）
├── forge-admin-ui/                     # 前端工程
│   └── src/views/                      # ⭐ 页面组件写在这里
├── forge-docs/                         # 文档站点（VitePress）
└── forge-report-ui/                    # AI 大屏前端
```

### 1.3 核心开发约定

| 约定 | 说明 | 示例 |
|------|------|------|
| 包名 | `com.mdframe.forge.<模块名>` | `com.mdframe.forge.employee` |
| 表名 | 业务表加业务前缀，含租户字段 | `sys_employee` |
| 实体继承 | 继承 `BaseEntity`（自动填充创建人/时间等） | — |
| Controller 路径 | `/模块名/操作` | `/employee/page`、`/employee/add` |
| 统一返回 | `RespInfo<T>` 包装 | `RespInfo.success(data)` |
| 分页查询 | 继承 `PageQuery`，返回 `Page<T>` | — |
| 前端页面 | `src/views/<模块名>/index.vue` | `src/views/employee/index.vue` |

### 1.4 BaseEntity 提供的基础字段

所有继承 `BaseEntity` 的实体自动拥有以下字段（MyBatis-Plus 自动填充，无需手动赋值）：

| 字段 | 类型 | 说明 | 填充策略 |
|------|------|------|---------|
| `createBy` | Long | 创建人 ID | INSERT 时自动填充 |
| `createTime` | LocalDateTime | 创建时间 | INSERT 时自动填充 |
| `createDept` | Long | 创建部门 ID | INSERT 时自动填充 |
| `updateBy` | Long | 更新人 ID | INSERT + UPDATE 时自动填充 |
| `updateTime` | LocalDateTime | 更新时间 | INSERT + UPDATE 时自动填充 |

---

## 二、数据库建表

### 2.1 创建业务表

以「员工信息表」为例：

```sql
CREATE TABLE IF NOT EXISTS `sys_employee` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
  `emp_name`    VARCHAR(50)  NOT NULL                 COMMENT '姓名',
  `emp_no`      VARCHAR(50)  NOT NULL                 COMMENT '工号',
  `dept_id`     BIGINT        DEFAULT NULL            COMMENT '部门ID',
  `position`    VARCHAR(100)  DEFAULT NULL            COMMENT '职位',
  `hire_date`   DATETIME      DEFAULT NULL            COMMENT '入职日期',
  `phone`       VARCHAR(20)   DEFAULT NULL            COMMENT '手机号',
  `email`       VARCHAR(100)  DEFAULT NULL            COMMENT '邮箱',
  `status`      TINYINT       DEFAULT 1               COMMENT '状态(1:正常,0:停用)',
  `tenant_id`   BIGINT        NOT NULL DEFAULT 1      COMMENT '租户ID',
  `create_dept` BIGINT        DEFAULT NULL            COMMENT '创建部门',
  `create_by`   BIGINT        DEFAULT NULL            COMMENT '创建者',
  `create_time` DATETIME      DEFAULT CURRENT_TIMESTAMP        COMMENT '创建时间',
  `update_by`   BIGINT        DEFAULT NULL            COMMENT '更新者',
  `update_time` DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag`    TINYINT       DEFAULT 0               COMMENT '删除标志(0:未删除,1:已删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_emp_no` (`emp_no`, `tenant_id`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工信息表';
```

::: tip 建表必含字段
所有业务表都应包含 `tenant_id`（租户隔离）、`del_flag`（逻辑删除）、`create_by`/`create_time`/`update_by`/`update_time`（审计字段），以配合框架的自动填充、多租户和数据权限机制。
:::

### 2.2 初始化菜单和权限资源

建表后需要同时在 `sys_resource` 表中插入对应的菜单、按钮和 API 资源，这样系统才能识别新模块：

```sql
-- 1. 新增目录：人事管理
INSERT INTO sys_resource (resource_name, parent_id, resource_type, sort, path, icon, visible, menu_status, is_public, tenant_id, create_time)
SELECT '人事管理', 0, 1, 30, '/hr', 'md-people', 1, 1, 0, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_resource WHERE resource_name = '人事管理' AND parent_id = 0);

-- 2. 新增菜单：员工管理
SET @hr_dir_id = (SELECT id FROM sys_resource WHERE resource_name = '人事管理' AND parent_id = 0 LIMIT 1);

INSERT INTO sys_resource (resource_name, parent_id, resource_type, sort, path, component, perms, icon, visible, menu_status, keep_alive, is_public, tenant_id, create_time)
SELECT '员工管理', @hr_dir_id, 2, 1, '/employee/list', 'employee/index', 'employee:list', 'md-list', 1, 1, 1, 0, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_resource WHERE perms = 'employee:list');

-- 3. 新增按钮权限
SET @emp_menu_id = (SELECT id FROM sys_resource WHERE perms = 'employee:list' LIMIT 1);

INSERT INTO sys_resource (resource_name, parent_id, resource_type, sort, perms, visible, menu_status, is_public, tenant_id, create_time)
SELECT '新增', @emp_menu_id, 3, 1, 'employee:add', 1, 1, 0, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_resource WHERE perms = 'employee:add');

INSERT INTO sys_resource (resource_name, parent_id, resource_type, sort, perms, visible, menu_status, is_public, tenant_id, create_time)
SELECT '编辑', @emp_menu_id, 3, 2, 'employee:edit', 1, 1, 0, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_resource WHERE perms = 'employee:edit');

INSERT INTO sys_resource (resource_name, parent_id, resource_type, sort, perms, visible, menu_status, is_public, tenant_id, create_time)
SELECT '删除', @emp_menu_id, 3, 3, 'employee:remove', 1, 1, 0, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_resource WHERE perms = 'employee:remove');

-- 4. 新增 API 权限
INSERT INTO sys_resource (resource_name, parent_id, resource_type, sort, api_method, api_url, visible, menu_status, is_public, tenant_id, create_time)
SELECT '员工分页查询', @emp_menu_id, 4, 1, 'GET', '/employee/page', 1, 1, 0, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_resource WHERE api_url = '/employee/page');

INSERT INTO sys_resource (resource_name, parent_id, resource_type, sort, api_method, api_url, visible, menu_status, is_public, tenant_id, create_time)
SELECT '新增员工', @emp_menu_id, 4, 2, 'POST', '/employee/add', 1, 1, 0, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_resource WHERE api_url = '/employee/add');

INSERT INTO sys_resource (resource_name, parent_id, resource_type, sort, api_method, api_url, visible, menu_status, is_public, tenant_id, create_time)
SELECT '编辑员工', @emp_menu_id, 4, 3, 'POST', '/employee/edit', 1, 1, 0, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_resource WHERE api_url = '/employee/edit');

INSERT INTO sys_resource (resource_name, parent_id, resource_type, sort, api_method, api_url, visible, menu_status, is_public, tenant_id, create_time)
SELECT '删除员工', @emp_menu_id, 4, 4, 'POST', '/employee/remove/{id}', 1, 1, 0, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_resource WHERE api_url = '/employee/remove/{id}');
```

::: tip 幂等 SQL
所有 seed 脚本使用 `INSERT ... SELECT ... WHERE NOT EXISTS` 保证可重复执行。
:::

![菜单管理-新增资源](https://gitee.com/ForgeLab/forge-admin/raw/main/images/%E8%8F%9C%E5%8D%95%E7%AE%A1%E7%90%86.png)

> 💡 也可以通过系统界面的「系统管理 → 菜单管理」可视化配置，效果与 SQL 相同。

---

## 三、后端代码编写

后端代码遵循 **Entity → DTO → Mapper → Service → Controller** 的分层结构。以下所有代码均来自项目实际的 `employee` 模块。

### 3.1 Entity 实体类

**文件路径**：`forge-admin-server/src/main/java/com/mdframe/forge/employee/entity/Employee.java`

```java
package com.mdframe.forge.employee.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.mdframe.forge.starter.core.domain.BaseEntity;
import com.mdframe.forge.starter.trans.annotation.DictTrans;
import com.mdframe.forge.starter.trans.annotation.TransField;
import com.mdframe.forge.starter.crypto.desensitize.annotation.Desensitize;
import com.mdframe.forge.starter.crypto.desensitize.strategy.DesensitizeType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_employee")
@DictTrans  // ① 启用字典自动翻译
public class Employee extends BaseEntity {  // ② 继承 BaseEntity

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 姓名（③ 数据脱敏：张*三） */
    @Desensitize(type = DesensitizeType.NAME)
    private String empName;

    /** 工号 */
    private String empNo;

    /** 部门ID（④ 字典翻译：自动填充 deptIdName） */
    @TransField(dictType = "sys_dept")
    private Long deptId;

    @TableField(exist = false)  // ⑤ 非数据库字段
    private String deptIdName;

    /** 职位 */
    private String position;

    /** 入职日期 */
    private LocalDateTime hireDate;

    /** 手机号（③ 脱敏：138****5678） */
    @Desensitize(type = DesensitizeType.PHONE)
    private String phone;

    /** 邮箱（③ 脱敏：z***@example.com） */
    @Desensitize(type = DesensitizeType.EMAIL)
    private String email;

    /** 状态(1:正常,0:停用) */
    @TransField(dictType = "sys_normal_disable")
    private Integer status;

    @TableField(exist = false)
    private String statusName;

    /** 租户ID */
    private Long tenantId;

    /** 创建部门 */
    private Long createDept;

    /** 删除标志 */
    @TransField(dictType = "del_flag")
    private Integer delFlag;

    @TableField(exist = false)
    private String delFlagName;
}
```

**核心注解解读**：

| 标注 | 注解 | 作用 |
|------|------|------|
| ① | `@DictTrans` | 类级别，开启字典翻译功能 |
| ② | `extends BaseEntity` | 自动获得 createBy/createTime/updateBy/updateTime/createDept |
| ③ | `@Desensitize` | 接口返回时自动脱敏（姓名、手机、邮箱） |
| ④ | `@TransField` | 指定字典类型，返回时自动翻译填充 `xxxName` 字段 |
| ⑤ | `@TableField(exist = false)` | 标记为非数据库字段，仅用于接收翻译结果 |

### 3.2 DTO 数据传输对象

**查询条件 DTO**：`dto/EmployeeQuery.java`

```java
package com.mdframe.forge.employee.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class EmployeeQuery implements Serializable {
    private Long id;
    private String empName;
    private String empNo;
    private Long deptId;
    private String position;
    private LocalDateTime hireDate;
    private String phone;
    private String email;
    private Integer status;
    private Long tenantId;
    private Long createDept;
    private Integer delFlag;
}
```

**新增/编辑 DTO**：`dto/EmployeeDTO.java`

```java
package com.mdframe.forge.employee.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class EmployeeDTO implements Serializable {
    private Long id;
    private String empName;
    private String empNo;
    private Long deptId;
    private String position;
    private LocalDateTime hireDate;
    private String phone;
    private String email;
    private Integer status;
    private Long tenantId;
    private Long createDept;
    private Integer delFlag;
}
```

::: tip 为什么要拆 Query 和 DTO？
`Query` 用于接收列表查询的筛选条件，`DTO` 用于新增/编辑的表单数据。拆分后职责清晰，且可以针对不同场景做字段验证。
:::

### 3.3 Mapper 接口

**文件路径**：`mapper/EmployeeMapper.java`

```java
package com.mdframe.forge.employee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mdframe.forge.employee.entity.Employee;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {
    // BaseMapper 已提供 selectPage / selectList / insert / updateById / deleteById 等方法
    // 复杂多表查询可在此定义方法，配合 src/main/resources/mapper/*.xml 实现
}
```

对于单表 CRUD，`BaseMapper` 提供的方法已经够用。如需多表关联查询，可在 `resources/mapper/EmployeeMapper.xml` 中写自定义 SQL。

### 3.4 Service 接口与实现

**接口**：`service/IEmployeeService.java`

```java
package com.mdframe.forge.employee.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mdframe.forge.employee.dto.EmployeeDTO;
import com.mdframe.forge.employee.dto.EmployeeQuery;
import com.mdframe.forge.employee.entity.Employee;
import com.mdframe.forge.starter.core.domain.PageQuery;
import java.util.List;

public interface IEmployeeService extends IService<Employee> {

    Page<Employee> selectEmployeePage(PageQuery pageQuery, EmployeeQuery query);

    List<Employee> selectEmployeeList(EmployeeQuery query);

    Employee selectEmployeeById(Long id);

    boolean insertEmployee(EmployeeDTO dto);

    boolean updateEmployee(EmployeeDTO dto);

    boolean deleteEmployeeById(Long id);

    boolean deleteEmployeeByIds(Long[] ids);
}
```

**实现类**：`service/impl/EmployeeServiceImpl.java`

```java
package com.mdframe.forge.employee.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mdframe.forge.employee.dto.EmployeeDTO;
import com.mdframe.forge.employee.dto.EmployeeQuery;
import com.mdframe.forge.employee.entity.Employee;
import com.mdframe.forge.employee.mapper.EmployeeMapper;
import com.mdframe.forge.employee.service.IEmployeeService;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.trans.annotation.DictTranslate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee>
        implements IEmployeeService {

    private final EmployeeMapper employeeMapper;

    @Override
    @DictTranslate  // ① 自动翻译字典字段
    public Page<Employee> selectEmployeePage(PageQuery pageQuery, EmployeeQuery query) {
        LambdaQueryWrapper<Employee> wrapper = buildQueryWrapper(query);
        return employeeMapper.selectPage(pageQuery.toPage(), wrapper);  // ② pageQuery.toPage()
    }

    @Override
    @DictTranslate
    public List<Employee> selectEmployeeList(EmployeeQuery query) {
        LambdaQueryWrapper<Employee> wrapper = buildQueryWrapper(query);
        return employeeMapper.selectList(wrapper);
    }

    @Override
    public Employee selectEmployeeById(Long id) {
        return employeeMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)  // ③ 事务注解
    public boolean insertEmployee(EmployeeDTO dto) {
        Employee employee = new Employee();
        BeanUtil.copyProperties(dto, employee);  // ④ 对象拷贝
        return employeeMapper.insert(employee) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateEmployee(EmployeeDTO dto) {
        Employee employee = new Employee();
        BeanUtil.copyProperties(dto, employee);
        return employeeMapper.updateById(employee) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteEmployeeById(Long id) {
        return employeeMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteEmployeeByIds(Long[] ids) {
        return employeeMapper.deleteBatchIds(Arrays.asList(ids)) > 0;
    }

    /** 构建查询条件 */
    private LambdaQueryWrapper<Employee> buildQueryWrapper(EmployeeQuery query) {
        LambdaQueryWrapper<Employee> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getId() != null, Employee::getId, query.getId());
        wrapper.like(StringUtils.isNotBlank(query.getEmpName()),
                     Employee::getEmpName, query.getEmpName());
        wrapper.like(StringUtils.isNotBlank(query.getEmpNo()),
                     Employee::getEmpNo, query.getEmpNo());
        wrapper.eq(query.getDeptId() != null,
                   Employee::getDeptId, query.getDeptId());
        wrapper.like(StringUtils.isNotBlank(query.getPhone()),
                     Employee::getPhone, query.getPhone());
        wrapper.eq(query.getStatus() != null,
                   Employee::getStatus, query.getStatus());
        return wrapper;
    }
}
```

**关键点解读**：

| 标注 | 说明 |
|------|------|
| ① `@DictTranslate` | Service 方法上加此注解，返回结果中的 `@TransField` 字段自动翻译 |
| ② `pageQuery.toPage()` | `PageQuery` 内置方法，将页码/页大小转为 MyBatis-Plus 的 `Page` 对象 |
| ③ `@Transactional` | 写操作加事务，`rollbackFor = Exception.class` 确保所有异常都回滚 |
| ④ `BeanUtil.copyProperties` | Hutool 工具，将 DTO 属性拷贝到 Entity |

### 3.5 Controller 控制器

**文件路径**：`controller/EmployeeController.java`

```java
package com.mdframe.forge.employee.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mdframe.forge.employee.dto.EmployeeDTO;
import com.mdframe.forge.employee.dto.EmployeeQuery;
import com.mdframe.forge.employee.entity.Employee;
import com.mdframe.forge.employee.service.IEmployeeService;
import com.mdframe.forge.starter.core.annotation.crypto.ApiDecrypt;
import com.mdframe.forge.starter.core.annotation.crypto.ApiEncrypt;
import com.mdframe.forge.starter.core.annotation.log.OperationLog;
import com.mdframe.forge.starter.core.domain.OperationType;
import com.mdframe.forge.starter.core.domain.PageQuery;
import com.mdframe.forge.starter.core.domain.RespInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/employee")
@RequiredArgsConstructor
@ApiDecrypt  // ① 请求体自动解密
@ApiEncrypt  // ② 响应体自动加密
public class EmployeeController {

    private final IEmployeeService employeeService;

    /** 分页查询 */
    @GetMapping("/page")
    @OperationLog(module = "员工管理", type = OperationType.QUERY, desc = "分页查询员工列表")
    public RespInfo<Page<Employee>> page(PageQuery pageQuery, EmployeeQuery query) {
        return RespInfo.success(employeeService.selectEmployeePage(pageQuery, query));
    }

    /** 列表查询 */
    @GetMapping("/list")
    @OperationLog(module = "员工管理", type = OperationType.QUERY, desc = "查询员工列表")
    public RespInfo<List<Employee>> list(EmployeeQuery query) {
        return RespInfo.success(employeeService.selectEmployeeList(query));
    }

    /** 详情查询 */
    @PostMapping("/getById")
    public RespInfo<Employee> getById(@RequestParam Long id) {
        return RespInfo.success(employeeService.selectEmployeeById(id));
    }

    /** 新增 */
    @PostMapping("/add")
    @OperationLog(module = "员工管理", type = OperationType.ADD, desc = "新增员工")
    public RespInfo<Void> add(@RequestBody EmployeeDTO dto) {
        return employeeService.insertEmployee(dto)
            ? RespInfo.success()
            : RespInfo.error("新增失败");
    }

    /** 编辑 */
    @PostMapping("/edit")
    @OperationLog(module = "员工管理", type = OperationType.UPDATE, desc = "修改员工")
    public RespInfo<Void> edit(@RequestBody EmployeeDTO dto) {
        return employeeService.updateEmployee(dto)
            ? RespInfo.success()
            : RespInfo.error("修改失败");
    }

    /** 删除 */
    @PostMapping("/remove/{id}")
    @OperationLog(module = "员工管理", type = OperationType.DELETE, desc = "删除员工")
    public RespInfo<Void> remove(@PathVariable Long id) {
        return employeeService.deleteEmployeeById(id)
            ? RespInfo.success()
            : RespInfo.error("删除失败");
    }

    /** 批量删除 */
    @PostMapping("/removeBatch")
    @OperationLog(module = "员工管理", type = OperationType.DELETE, desc = "批量删除员工")
    public RespInfo<Void> removeBatch(@RequestBody Long[] ids) {
        return employeeService.deleteEmployeeByIds(ids)
            ? RespInfo.success()
            : RespInfo.error("批量删除失败");
    }
}
```

**Controller 注解说明**：

| 注解 | 作用 |
|------|------|
| `@ApiDecrypt` / `@ApiEncrypt` | 请求体 RSA+AES 解密 / 响应体加密，前端无感处理 |
| `@OperationLog` | 操作日志，自动记录到 `sys_log` 表 |
| `@RequestBody` | 接收 JSON 请求体 |
| `@PathVariable` | 接收 URL 路径参数 |
| `@RequestParam` | 接收 URL Query 参数 |

### 3.6 后端文件结构总览

完成后，后端目录结构如下：

```
forge-server/forge-admin-server/src/main/java/com/mdframe/forge/employee/
├── controller/
│   └── EmployeeController.java       # 接口层
├── service/
│   ├── IEmployeeService.java         # 服务接口
│   └── impl/
│       └── EmployeeServiceImpl.java  # 服务实现
├── mapper/
│   └── EmployeeMapper.java           # 数据访问层
├── entity/
│   └── Employee.java                 # 实体类
└── dto/
    ├── EmployeeQuery.java            # 查询条件 DTO
    └── EmployeeDTO.java              # 新增/编辑 DTO
```

---

## 四、前端代码编写

Forge Admin 前端使用 `AiCrudPage` 组件实现配置式 CRUD 页面，绝大多数模块只需写一个配置文件即可完成前端页面。

### 4.1 API 请求层

**文件路径**：`forge-admin-ui/src/api/employee.js`

```javascript
/**
 * 员工管理 API
 */
import { request } from '@/utils'

const BASE = '/employee'

/** 分页查询 */
export function getEmployeePage(params) {
  return request.get(`${BASE}/page`, { params })
}

/** 查询详情 */
export function getEmployeeDetail(id) {
  return request.post(`${BASE}/getById`, null, { params: { id } })
}

/** 新增 */
export function createEmployee(data) {
  return request.post(`${BASE}/add`, data)
}

/** 更新 */
export function updateEmployee(data) {
  return request.post(`${BASE}/edit`, data)
}

/** 删除 */
export function removeEmployee(id) {
  return request.post(`${BASE}/remove`, null, { params: { id } })
}
```

### 4.2 页面组件（AiCrudPage 配置式）

**文件路径**：`forge-admin-ui/src/views/employee/index.vue`

```vue
<template>
  <div class="employee-page">
    <AiCrudPage
      ref="crudRef"
      :api-config="apiConfig"
      :search-schema="computedSearchSchema"
      :columns="computedColumns"
      :edit-schema="computedEditSchema"
      row-key="id"
    />
  </div>
</template>

<script setup>
import { computed, h, onMounted, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import DictTag from '@/components/DictTag.vue'
import { getDictData } from '@/composables/useDict'

defineOptions({ name: 'Employee' })

const crudRef = ref(null)

// ── 接口配置 ──────────────────────────────────────────────
// 格式：'HTTP方法@URL'，AiCrudPage 自动映射到增删改查
const apiConfig = {
  list:   'get@/employee/page',
  detail: 'post@/employee/getById',
  add:    'post@/employee/add',
  update: 'post@/employee/edit',
  delete: 'post@/employee/remove/:id',
}

// ── 字典缓存 ──────────────────────────────────────────────
const dictCache = ref({})

// ── 搜索条件配置 ──────────────────────────────────────────
const searchSchema = [
  { field: 'empName', label: '姓名',   type: 'input' },
  { field: 'empNo',   label: '工号',   type: 'input' },
  { field: 'deptId',  label: '部门',   type: 'select', dictType: 'sys_dept' },
  { field: 'status',  label: '状态',   type: 'select', dictType: 'sys_normal_disable' },
]

// ── 表格列配置 ────────────────────────────────────────────
const columnsSchema = [
  { prop: 'empName',  label: '姓名' },
  { prop: 'empNo',    label: '工号' },
  { prop: 'deptId',   label: '部门',  _transName: 'deptIdName',  _dictType: 'sys_dept' },
  { prop: 'position', label: '职位' },
  { prop: 'hireDate', label: '入职日期' },
  { prop: 'phone',    label: '手机号' },
  { prop: 'email',    label: '邮箱' },
  { prop: 'status',   label: '状态',  _transName: 'statusName', _dictType: 'sys_normal_disable' },
]

// ── 新增/编辑表单配置 ─────────────────────────────────────
const editSchema = [
  { field: 'empName',  label: '姓名',     type: 'input',  required: true },
  { field: 'empNo',    label: '工号',     type: 'input',  required: true },
  { field: 'deptId',   label: '部门',     type: 'select', required: true, dictType: 'sys_dept' },
  { field: 'position', label: '职位',     type: 'input',  required: true },
  { field: 'hireDate', label: '入职日期', type: 'date',   required: true },
  { field: 'phone',    label: '手机号',   type: 'input',  required: true },
  { field: 'email',    label: '邮箱',     type: 'input',  required: true },
  { field: 'status',   label: '状态',     type: 'select', required: true, dictType: 'sys_normal_disable' },
]

// ── 字典预加载 ────────────────────────────────────────────
async function preloadDicts() {
  const types = new Set()
  columnsSchema.forEach(col => col._dictType && types.add(col._dictType))
  ;[...searchSchema, ...editSchema].forEach(f => f.dictType && types.add(f.dictType))
  for (const type of types) {
    try {
      dictCache.value[type] = await getDictData(type)
    } catch (e) {
      console.warn(`[Employee] 加载字典 ${type} 失败`, e)
    }
  }
}
onMounted(preloadDicts)

// ── 表格列：注入字典渲染 ──────────────────────────────────
const computedColumns = computed(() =>
  columnsSchema.map((col) => {
    if (col._transName) {
      return { ...col, render: row => row[col._transName] ?? row[col.prop] }
    }
    if (col._dictType) {
      return {
        ...col,
        render: row => h(DictTag, {
          dictType: col._dictType, value: row[col.prop], size: 'small'
        }),
      }
    }
    return col
  }),
)

// ── 表单字段：注入字典选项 ────────────────────────────────
function transformFields(fields) {
  return fields.map((field) => {
    if (field.dictType && ['select', 'radio', 'checkbox'].includes(field.type)) {
      return {
        ...field,
        props: { ...(field.props || {}), options: dictCache.value[field.dictType] || [] },
      }
    }
    return field
  })
}

const computedSearchSchema = computed(() => transformFields(searchSchema))
const computedEditSchema  = computed(() => transformFields(editSchema))
</script>
```

**AiCrudPage 配置说明**：

| 配置项 | 说明 |
|--------|------|
| `apiConfig` | 接口地址映射，格式为 `HTTP方法@URL`，如 `get@/employee/page` |
| `searchSchema` | 搜索栏字段配置（字段名、标签、类型、字典类型） |
| `columns` | 表格列配置（字段名、标题、字典翻译渲染） |
| `editSchema` | 新增/编辑表单字段配置（字段名、标签、类型、必填、字典） |
| `row-key` | 行唯一标识字段名，通常为主键 `id` |

**字段类型支持**：`input` / `select` / `date` / `radio` / `checkbox` / `textarea` / `number`

**字典翻译**：
- 表格列设置 `_transName` 属性，直接显示后端翻译后的 `xxxName` 字段
- 或设置 `_dictType`，前端用 `DictTag` 组件渲染

::: tip 自动路由
项目使用 `unplugin-vue-router`，路由会根据 `src/views/` 目录结构自动生成。`employee/index.vue` 自动映射到路由 `/employee`，无需手动配置路由文件。
:::

### 4.4 前端文件结构

```
forge-admin-ui/src/
├── api/
│   └── employee.js        # API 请求
└── views/
    └── employee/
        └── index.vue       # CRUD 页面（AiCrudPage 配置式）
```

![员工管理页面效果](https://gitee.com/ForgeLab/forge-admin/raw/main/images/user-management.png)

---

## 五、进阶功能集成

### 5.1 加解密（@ApiDecrypt / @ApiEncrypt）

在 Controller 类上加注解，所有接口自动加解密：

```java
@RestController
@RequestMapping("/employee")
@ApiDecrypt   // 请求体解密
@ApiEncrypt   // 响应体加密
public class EmployeeController { ... }
```

前端无需额外处理，`request` 工具已内置 RSA+AES 加解密。

### 5.2 操作日志（@OperationLog）

```java
@PostMapping("/add")
@OperationLog(module = "员工管理", type = OperationType.ADD, desc = "新增员工")
public RespInfo<Void> add(@RequestBody EmployeeDTO dto) { ... }
```

操作会自动记录到 `sys_log` 表，可在「系统管理 → 操作日志」查看。

### 5.3 字典翻译（@DictTrans / @DictTranslate）

```java
// Entity：标记需要翻译的字段
@DictTrans
public class Employee extends BaseEntity {
    @TransField(dictType = "sys_normal_disable")
    private Integer status;

    @TableField(exist = false)
    private String statusName;  // 自动填充"正常"/"停用"
}

// Service：方法上加 @DictTranslate
@DictTranslate
public Page<Employee> selectEmployeePage(...) { ... }
```

### 5.4 数据脱敏（@Desensitize）

```java
@Desensitize(type = DesensitizeType.NAME)   // 张*三
private String empName;

@Desensitize(type = DesensitizeType.PHONE)  // 138****5678
private String phone;

@Desensitize(type = DesensitizeType.EMAIL)  // z***@example.com
private String email;
```

### 5.5 数据权限接入

在「系统管理 → 数据权限配置」中新增配置：

| 字段 | 值 |
|------|-----|
| Mapper 方法 | `com.mdframe.forge.employee.mapper.EmployeeMapper.selectPage` |
| 表别名 | （单表查询留空） |
| 用户 ID 字段 | `create_by` |
| 组织 ID 字段 | `dept_id` |
| 租户 ID 字段 | `tenant_id` |

配置后，`selectPage` 查询会根据当前用户角色的数据权限范围自动追加 WHERE 条件。

![数据权限配置页面](https://gitee.com/ForgeLab/forge-admin/raw/main/images/%E6%95%B0%E6%8D%AE%E6%9D%83%E9%99%90%E9%85%8D%E7%BD%AE.png)

---

## 六、测试与验证

### 6.1 接口测试

使用 curl 或 Apifox 测试接口：

```bash
# 1. 登录获取 Token
TOKEN=$(curl -s -X POST http://localhost:8580/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}' | jq -r '.data.token')

# 2. 分页查询员工列表
curl -s http://localhost:8580/employee/page?pageNum=1\&pageSize=10 \
  -H "Authorization: Bearer $TOKEN" | jq .

# 3. 新增员工
curl -s -X POST http://localhost:8580/employee/add \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"empName":"张三","empNo":"EMP001","phone":"13800138000","status":1}' | jq .

# 4. 编辑员工
curl -s -X POST http://localhost:8580/employee/edit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"id":1,"empName":"张三丰","empNo":"EMP001","phone":"13800138001","status":1}' | jq .

# 5. 删除员工
curl -s -X POST http://localhost:8580/employee/remove/1 \
  -H "Authorization: Bearer $TOKEN" | jq .
```

### 6.2 页面验证

启动前后端后，使用 admin 账号登录系统：

1. 侧边栏应出现「人事管理 → 员工管理」菜单
2. 点击进入员工管理页面，可看到搜索栏、表格、新增按钮
3. 点击「新增」弹出表单，填写数据后提交
4. 表格中新增的数据应显示部门名称和状态标签（字典翻译生效）
5. 手机号应显示为 `138****5678`（脱敏生效）
6. 点击「编辑」可修改数据，点击「删除」可删除

![员工管理页面效果](https://gitee.com/ForgeLab/forge-admin/raw/main/images/user-management.png)

---

## 七、开发流程总结

```
建表 (SQL)
  │
  ├── 初始化菜单/权限资源 (sys_resource)
  │
  ▼
后端代码
  ├── Entity（实体类，继承 BaseEntity）
  ├── DTO（Query + DTO）
  ├── Mapper（继承 BaseMapper）
  ├── Service（接口 + 实现，@DictTranslate）
  └── Controller（REST 接口，@OperationLog / @ApiDecrypt / @ApiEncrypt）
  │
  ▼
前端代码
  ├── api/xxx.js（API 请求）
  └── views/xxx/index.vue（AiCrudPage 配置式页面）
  │
  ▼
权限配置
  ├── 角色管理：创建角色，分配资源权限
  ├── 数据权限：配置 Mapper 方法的过滤规则
  └── 用户管理：创建用户，分配角色
  │
  ▼
测试验证
  ├── 接口测试（curl / Apifox）
  └── 页面验证（登录 → 操作 → 检查）
```

---

## 八、常见问题

### Q1: 新增的菜单不显示在侧边栏？

1. 检查 `sys_resource` 中菜单的 `visible` 和 `menu_status` 是否为 1
2. 检查当前用户角色是否分配了该菜单资源
3. 清除浏览器缓存或重新登录（权限缓存刷新）
4. 检查菜单的 `component` 路径是否正确对应 `src/views/` 下的文件

### Q2: 接口返回 403 无权限？

1. 检查 `sys_resource` 中是否为该接口配置了 API 类型资源
2. 检查当前用户角色是否分配了该 API 资源
3. 如果是超级管理员仍 403，检查 `forge.auth.enable-api-permission` 是否开启

### Q3: 字典翻译不生效，xxxName 字段为空？

1. 确认 Entity 类上有 `@DictTrans` 注解
2. 确认字段上有 `@TransField(dictType = "xxx")`
3. 确认 Service 方法上有 `@DictTranslate` 注解
4. 确认字典数据已在 `sys_dict_type` 和 `sys_dict_data` 中配置

### Q4: 数据脱敏不生效？

1. 确认字段上有 `@Desensitize(type = ...)` 注解
2. 确认 `forge.crypto.desensitize.enabled` 配置为 true
3. 检查是否是超级管理员（超管可能不脱敏，取决于配置）

### Q5: 前端 AiCrudPage 页面空白？

1. 检查浏览器控制台是否有报错
2. 确认 `apiConfig` 中的接口地址与后端 Controller 路径一致
3. 确认 `src/views/` 下的文件路径与菜单 `component` 配置一致
4. 检查 `request` 工具是否正确导出

---

## 九、最佳实践

1. **代码生成器**：项目内置代码生成器（系统管理 → 代码生成），可自动生成 Entity/DTO/Mapper/Service/Controller/前端页面，大幅提升开发效率
2. **先建表再生成**：推荐先在数据库建好表，然后用代码生成器读取表结构生成代码
3. **幂等 SQL**：所有 seed 脚本使用 `WHERE NOT EXISTS` 保证可重复执行
4. **事务注解**：所有写操作（insert/update/delete）加 `@Transactional(rollbackFor = Exception.class)`
5. **日志记录**：关键写操作加 `@OperationLog`，便于审计追踪
6. **字段命名**：数据库用下划线（`emp_name`），Java 用驼峰（`empName`），MyBatis-Plus 自动映射
7. **字典优先**：状态、类型等枚举值优先使用字典管理，而非硬编码 if-else