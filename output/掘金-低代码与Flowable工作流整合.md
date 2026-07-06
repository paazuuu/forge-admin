# 低代码表单怎么和 Flowable 工作流整合？Forge 的 3 种数据模式设计

> 低代码搭表单、Flowable 跑审批，这两件事单独做都不难。难的是合在一起：表单数据要不要落业务表？流程变量怎么传？审批中途数据存哪？这篇拆 Forge 的真实实现，看它怎么用 3 种数据模式解决这个整合难题。

做过审批系统的人都知道，低代码表单和工作流引擎之间有一道"缝"：

- 低代码表单负责**收集数据**（请假天数、报销金额、采购明细）
- 工作流引擎负责**驱动流程**（谁审批、怎么流转、什么时候结束）

但这两边的**数据怎么流动**，是最容易出问题的地方。

请假审批的表单数据，是只存在流程变量里？还是落到业务表？还是两边都存？不同选择，后续的查询、统计、数据权限完全不同。

Forge Admin 在这块做了一个很清晰的设计：**用 3 种数据模式（dataMode）来定义表单数据和流程的关系**。这篇就拆这个设计。

---

## 一、先看清楚：整合的 4 个核心实体

Forge 的流程和低代码整合，核心是 4 张表的关系。看懂这张图，后面的设计就清楚了：

```text
FlowForm（流程表单定义）
  │  formSchema: JSON 格式的表单协议（字段、类型、校验）
  │  formType: dynamic(动态表单) / external(外部表单) / builtin(内置表单)
  │
  ▼
FlowEntry（流程入口）
  │  modelKey:  绑定哪个流程模型（Flowable BPMN）
  │  formKey:   绑定哪个表单（FlowForm）
  │  dataMode:  数据模式（PROCESS_ONLY / BUSINESS_OBJECT / HYBRID）
  │  objectCode:业务对象编码（落哪张业务表）
  │
  ├──► FlowEntryFieldMapping（字段映射）
  │       formField   → 表单字段
  │       targetField → 业务对象字段
  │       flowVariable → 流程变量
  │
  ▼
FlowFormInstance（填报实例快照）
  │  schemaSnapshot: 提交时的表单 schema 快照
  │  formData:       用户填的数据
  │  processInstanceId: 关联的流程实例
  │  objectCode + recordId: 关联的业务记录
```

用一句话说清楚：

- **FlowForm** 定义"表单长什么样"（低代码表单设计器产出）
- **FlowEntry** 是"流程入口"，把表单和流程模型绑在一起，并决定数据怎么处理
- **FlowEntryFieldMapping** 定义"表单字段怎么映射到业务表和流程变量"
- **FlowFormInstance** 是"每次提交的快照"，保存当时填的数据和表单结构

---

## 二、核心设计：3 种数据模式

整合的难点不在"表单怎么渲染"，而在"表单数据提交后怎么处理"。Forge 定义了 3 种模式，对应 3 种业务场景。

### 模式一：PROCESS_ONLY（纯流程模式）

表单数据只存在流程变量里，不落业务表。

```text
用户填表单 → 表单数据作为流程变量 → 流程流转 → 结束
```

**适用场景**：简单的审批，不需要事后查询业务数据。

比如：请假审批。审批通过后，人事在考勤表里手动记录就行，不需要自动落一张"请假记录表"。

**优点**：简单，不用建业务表，不用写字段映射。

**缺点**：事后查数据只能查流程历史，不能像查业务表那样分页、筛选、统计。

### 模式二：BUSINESS_OBJECT（业务对象模式）

表单数据通过字段映射，落到业务表里，流程里不存表单数据。

```text
用户填表单 → 字段映射 → 落业务表 → 流程变量只存关键字段 → 流程流转
```

**适用场景**：审批完要当业务数据用的场景。

比如：采购审批。采购单审批通过后，要变成一条采购记录，后续要查询、统计、对账。这种场景必须落业务表。

**优点**：数据在业务表里，可以正常查询、分页、做数据权限、做统计看板。

**缺点**：要建业务表、配字段映射、实现业务对象适配器。

### 模式三：HYBRID（混合模式）

表单数据既落业务表，又留一份快照在流程侧。

```text
用户填表单 → 字段映射 → 落业务表
                    ↘ 同时存一份 FlowFormInstance 快照
流程变量存关键字段 → 流程流转
```

**适用场景**：既要业务表查询，又要追溯"提交时表单长什么样"。

比如：合同审批。合同审批通过后要落合同表（BUSINESS_OBJECT），但三个月后可能要查"当时提交审批时填了什么"——如果表单结构改过，纯业务表无法还原当时的填报内容。HYBRID 模式存了一份 schemaSnapshot + formData，可以完整还原。

**优点**：业务表可查 + 快照可追溯，最完整。

**缺点**：数据存两份，存储成本略高。

---

## 三、源码怎么实现的？

直接看 `FlowRuntimeServiceImpl.submitByMode()`，这是整合的核心入口：

```java
private FlowStartResultVO submitByMode(FlowEntryRuntimeVO runtime,
                                       List<FlowEntryFieldMapping> mappings,
                                       Map<String, Object> formData,
                                       FlowEntrySubmitDTO dto,
                                       String dataMode) {
    FlowEntry entry = runtime.getEntry();

    if (BUSINESS_OBJECT.equals(dataMode)) {
        return submitBusinessObject(entry, mappings, formData, dto, false);
    }
    if (HYBRID.equals(dataMode)) {
        return submitBusinessObject(entry, mappings, formData, dto, true);
    }
    return submitProcessOnly(runtime, mappings, formData, dto);
}
```

就 3 个分支，对应 3 种模式。BUSINESS_OBJECT 和 HYBRID 走同一个方法，区别在 `keepSnapshot` 参数。

### PROCESS_ONLY 怎么处理

纯流程模式最简单：把表单数据塞进流程变量，发起流程，完事。

```java
private FlowStartResultVO submitProcessOnly(...) {
    // 1. 构建流程变量
    Map<String, Object> variables = buildFlowVariables(formData, mappings, dto);

    // 2. 创建表单实例记录（只存基础信息，不存快照）
    FlowFormInstance instance = new FlowFormInstance();
    instance.setDataMode(PROCESS_ONLY);
    instance.setFormData(toJson(formData));
    formInstanceMapper.insert(instance);

    // 3. 发起流程
    String processInstanceId = flowInstanceService.startProcess(
        entry.getModelKey(), businessKey, ..., variables, ...);

    // 4. 关联回表单实例
    formInstanceMapper.updateProcessInstance(instanceId, processInstanceId, "RUNNING");

    return result;
}
```

### BUSINESS_OBJECT / HYBRID 怎么处理

这两种模式要落业务表，核心靠 `FlowBusinessObjectRuntimeAdapter` 这个扩展点：

```java
private FlowStartResultVO submitBusinessObject(FlowEntry entry,
                                               List<FlowEntryFieldMapping> mappings,
                                               Map<String, Object> formData,
                                               FlowEntrySubmitDTO dto,
                                               boolean keepSnapshot) {
    // 1. 调用业务侧适配器，把表单数据落成业务记录
    BusinessRecordCreateResult record =
            businessObjectRuntimeAdapter.createBusinessRecord(entry, mappings, formData);

    // 2. 构建流程变量（从字段映射里取）
    Map<String, Object> variables = buildFlowVariables(formData, mappings, dto);

    // 3. HYBRID 模式：额外存一份快照
    if (keepSnapshot) {
        FlowFormInstance instance = new FlowFormInstance();
        instance.setSchemaSnapshot(runtime.getFormVersion().getFormSchema());
        instance.setFieldRegistry(runtime.getFormVersion().getFieldRegistry());
        instance.setFormData(toJson(formData));
        instance.setDataMode(HYBRID);
        instance.setObjectCode(objectCode);
        instance.setRecordId(record.getRecordId());  // 关联业务记录
        formInstanceMapper.insert(instance);
        variables.put("flowFormInstanceId", String.valueOf(formInstanceId));
    }

    // 4. 发起流程
    String processInstanceId = flowInstanceService.startProcess(...);

    return result;
}
```

---

## 四、关键解耦：FlowBusinessObjectRuntimeAdapter

注意上面代码里的 `businessObjectRuntimeAdapter.createBusinessRecord()`。这是整个整合设计里最巧妙的一步。

**问题**：流程插件要把表单数据落业务表，但"业务表"是业务模块定义的，流程插件不知道你的 `biz_purchase` 表长什么样。

**如果让流程插件直接依赖业务模块**，架构就耦合了——流程插件要认识采购表、合同表、报销表……每加一个业务就要改流程插件。

**Forge 的解法**：流程插件定义一个扩展点接口，由业务侧实现：

```java
public interface FlowBusinessObjectRuntimeAdapter {

    BusinessRecordCreateResult createBusinessRecord(
        FlowEntry entry,                              // 流程入口配置
        List<FlowEntryFieldMapping> mappings,         // 字段映射规则
        Map<String, Object> formData                  // 用户填的表单数据
    );

    @Data
    class BusinessRecordCreateResult {
        private String objectCode;           // 业务对象编码
        private Long recordId;               // 业务记录ID
        private String businessKey;          // 业务键
        private Map<String, Object> variables; // 额外流程变量
    }
}
```

源码注释写得很明确：

> Flow 插件不直接依赖低代码 generator 插件；需要 BUSINESS_OBJECT/HYBRID 时，由业务侧提供实现，把表单数据映射为业务对象记录。

**业务侧怎么实现**：

```java
@Component
public class PurchaseFlowAdapter implements FlowBusinessObjectRuntimeAdapter {

    @Autowired
    private PurchaseMapper purchaseMapper;

    @Override
    public BusinessRecordCreateResult createBusinessRecord(
            FlowEntry entry,
            List<FlowEntryFieldMapping> mappings,
            Map<String, Object> formData) {

        // 1. 按字段映射，把表单数据组装成业务对象
        Purchase purchase = new Purchase();
        for (FlowEntryFieldMapping mapping : mappings) {
            Object value = formData.get(mapping.getFormField());
            // 反射或手动 set 到业务对象
            setFieldValue(purchase, mapping.getTargetField(), value);
        }

        // 2. 落业务表
        purchaseMapper.insert(purchase);

        // 3. 返回业务记录信息
        BusinessRecordCreateResult result = new BusinessRecordCreateResult();
        result.setObjectCode("purchase");
        result.setRecordId(purchase.getId());
        result.setBusinessKey("purchase:" + purchase.getId());
        return result;
    }
}
```

这样流程插件和业务模块完全解耦：

- 流程插件只管"调适配器、拿记录ID、发起流程"
- 业务模块只管"按映射规则把表单数据落表"
- 字段映射关系在 `FlowEntryFieldMapping` 表里配置，不用写代码

---

## 五、字段映射：表单字段怎么变成业务数据和流程变量

`FlowEntryFieldMapping` 是连接表单和流程/业务表的桥梁：

```java
public class FlowEntryFieldMapping {
    private String formField;      // 表单字段名
    private String targetType;     // 映射目标类型：业务字段 / 流程变量
    private String targetField;    // 目标字段名
    private String flowVariable;   // 流程变量名
    private Integer required;      // 是否必填
}
```

一个表单字段可以同时映射到两个地方：

```text
表单字段 "days"（请假天数）
  ├── targetType=BUSINESS_FIELD, targetField="leave_days"  → 落业务表
  └── targetType=FLOW_VARIABLE, flowVariable="days"         → 流程变量（条件分支用）
```

这样设计的好处是：

- 业务表存完整数据（查询、统计、数据权限用）
- 流程变量只存条件判断需要的关键字段（流程分支用，比如 `days > 3` 走老板审批）
- 映射关系可配置，不用写代码

---

## 六、表单类型：不只是动态表单

`FlowForm` 支持三种表单类型：

```java
private String formType;  // dynamic / external / builtin
```

| 类型 | 说明 | 适用场景 |
|------|------|----------|
| `dynamic` | 动态表单，formSchema 是 JSON 协议，运行时渲染 | 标准审批表单（请假、报销） |
| `external` | 外部表单，指向一个 URL | 已有业务页面接审批 |
| `builtin` | 内置表单，指定前端组件路径 | 定制化程度高的表单 |

**`dynamic` 类型就是低代码表单和流程整合的核心**：低代码表单设计器产出的 JSON Schema 存到 `formSchema` 字段，流程审批时运行时引擎按这个 Schema 渲染表单。

`external` 类型则给"已有业务页面"留了路：你的采购管理页面已经做好了，只需要在审批时跳过去填，不用重新用低代码搭一遍。

这种设计保证了**整合不是强制的**——你可以全用低代码动态表单，也可以对接已有页面，不用为了用流程引擎就把所有表单重做。

---

## 七、快照机制：为什么 HYBRID 要存两份？

HYBRID 模式下，`FlowFormInstance` 存了这些字段：

```java
private String schemaSnapshot;   // 提交时的表单 Schema 快照
private String fieldRegistry;    // 字段目录快照
private String formData;         // 用户填的数据
private String dataMode;         // HYBRID
private String objectCode;       // 业务对象编码
private Long recordId;           // 业务记录ID
```

为什么要存快照？因为**表单结构会变**。

假设 3 月你用 v1 版表单提交了采购审批，表单有"采购金额"字段。5 月业务方加了"采购用途"字段，表单变成 v2。

如果只存业务表数据，3 月的那条采购记录没有"采购用途"——这没问题。但如果你想在审批历史里**还原"3 月提交时表单长什么样"**，没有快照就做不到。

快照保存的是**提交那一刻的表单结构 + 数据**。无论表单后续怎么改，历史记录都能完整还原。

这对合规审计很重要——政务、金融场景经常要求"可追溯当时填了什么、表单是什么样的"。

---

## 八、完整链路：从填表到审批的 6 步

把整个整合链路串一遍：

```text
1. 管理员在低代码表单设计器里设计表单 → 存 FlowForm（formSchema）
2. 管理员创建 FlowEntry，绑定 modelKey + formKey + dataMode + 字段映射
3. 用户在待办页面点"发起审批" → 运行时加载 FlowEntry + FlowForm
4. 运行时引擎按 formSchema 渲染表单 → 用户填写
5. 提交 → FlowRuntimeServiceImpl.submitEntryForm()
   ├── PROCESS_ONLY：表单数据 → 流程变量 → 发起流程
   ├── BUSINESS_OBJECT：表单数据 → 字段映射 → 落业务表 → 发起流程
   └── HYBRID：落业务表 + 存快照 → 发起流程
6. 流程结束 → @FlowCallback 回调业务 → 更新业务状态
```

---

## 九、和"纯 Flowable 接入"的关系

上一篇《Spring Boot 接 Flowable：用 3 个注解搭一个请假审批流程》讲的是**业务侧怎么用 `@FlowStart` / `@FlowCallback` 接流程**——那适合"已有业务表，想加审批"的场景。

这篇讲的是**低代码表单和流程的深度整合**——适合"用低代码搭表单 + 走审批 + 数据落表"的场景。

两者的关系：

| 对比 | 注解化接入（@FlowStart） | 低代码整合（FlowEntry） |
|------|--------------------------|------------------------|
| 表单来源 | 业务模块自己的页面 | 低代码表单设计器 |
| 数据存储 | 业务模块自己管 | 由 dataMode 决定 |
| 字段映射 | 不需要（业务方法直接处理） | FlowEntryFieldMapping 配置 |
| 适用场景 | 已有业务系统加审批 | 从零搭审批表单 + 流程 + 落表 |
| 侵入性 | 加注解 | 配置化，几乎无侵入 |

两条路按场景选：已有业务表用注解，从零搭用低代码整合。

---

## 十、总结：整合的核心是"数据模式 + 字段映射 + 扩展点"

低代码和 Flowable 的整合，核心不是"表单怎么渲染"（那是低代码的事），而是**表单数据提交后怎么和流程、业务表三者协同**。

Forge 的设计可以总结为 3 层：

| 层 | 机制 | 解决什么 |
|----|------|----------|
| 数据模式 | PROCESS_ONLY / BUSINESS_OBJECT / HYBRID | 表单数据存哪 |
| 字段映射 | FlowEntryFieldMapping | 表单字段怎么变成业务数据和流程变量 |
| 扩展点 | FlowBusinessObjectRuntimeAdapter | 流程插件和业务模块解耦 |

这 3 层做到了：

- **不耦合**：流程插件不认识业务表，通过扩展点对接
- **可配置**：字段映射在后台配，不改代码
- **可追溯**：HYBRID 模式存快照，历史可还原
- **可选择**：3 种模式按场景选，不是一刀切

这才是低代码和工作流整合该有的设计——不是把两边硬拼在一起，而是用清晰的数据模式定义边界，用扩展点解耦，用字段映射做桥梁。

---

**源码自取**（本文所有代码片段均来自仓库，可对照核验）：

- Gitee：https://gitee.com/ForgeLab/forge-admin
- GitHub：https://github.com/yaomindong1996/forge-admin
- 在线演示：http://www.dlforgelab.com:8084/forge/login （admin / 123456）

> 你们的审批表单和工作流是怎么整合的？表单数据落业务表还是只存流程变量？评论区聊聊。

