# 代码应用元数据配置化

## 背景

采购审批这类代码实现的业务表单当前通过 `BusinessCodeFormProvider` 接入流程。以 `SamplePurchaseOrderCodeFormProvider` 为例，表单资产、字段目录、字段展示名称和组件类型都写在 Provider 代码里。应用管理里只能看到“业务流程配置”，业务管理员无法调整字段目录、表单资产名称、列表/详情字段展示。

待办审批页已经改为通过代码表单组件渲染，但自定义 Vue 表单只有显式使用 `useBusinessTaskFormContext` 返回的权限 API 时，字段显示/编辑权限才会生效。列表和详情页目前是业务页面硬编码列与表单项，平台没有统一配置入口控制显示隐藏。

2026-06-30 需求修正：代码应用不能在“业务流程配置”里新增一套“字段与视图”配置面板。应用管理左侧本来已有“表单设计”“列表设计”“详情设置”组件，代码应用必须复用这些组件和既有 `formDesignerSchema/viewSchema` 协议。

2026-06-30 缺陷修正：采购单示例发起流程时会调用 `ensureFlowModel()`，旧逻辑只要发现数据库中的 BPMN XML 与 `SamplePurchaseOrderFlowBpmn.build()` 不一致就更新模型并发布，导致流程设计器节点抽屉里配置的 `formFieldPermissions` 被示例代码回写覆盖。审批页同时存在只传 `taskId` 时不加载业务表单上下文、顶部采购单信息只按详情视图不按节点权限过滤的问题。

2026-06-30 兼容修正：代码应用进入表单设计、列表设计时必须先读取当前 Provider/业务表字段作为基准，再叠加用户保存的显示配置。旧实现一旦保存 `metadata.fields` 后就把它当成唯一来源，业务代码新增字段不会再进入设计器；表单设计删除/隐藏字段也没有反写到运行态可见性，导致代码实现的业务表单仍然全部展示。

2026-06-30 性能与展示修正：待办/已办/我发起列表和详情抽屉中的流程节点名称不能把 `dept_leader_approve` 这类技术编码展示给业务用户；待办列表业务摘要增强和审批表单上下文加载不能在同一请求内对同一业务对象重复解析 `ai_business_document_config`。

2026-06-30 审批表单加载性能修正：流程服务返回待办表单信息时，不能把 Flowable 的 UUID 型 `processDefinitionId` 当成业务流程 `model_key` 查询 `sys_flow_model`；业务代码表单 `sample_purchase_order_approval_form` 也不能被误判为动态表单去查 `sys_flow_form`。

2026-06-30 运行时校验修正：待办业务表单加载时，前端、流程任务表和 Flowable 运行时可能分别携带业务流程模型 Key、`key:version:id` 或历史 UUID 型流程定义标识。业务侧不能直接字符串比较这些表示，否则会误报“流程定义与当前任务不匹配”。

2026-06-30 待办详情首屏性能修正：待办审批详情打开时，父级抽屉已经请求了业务表单上下文，但业务代码表单组件仍会重复请求同一上下文、代码应用配置和采购单详情，导致采购审批表单首屏需要 7-8 秒才出现。父级加载结果应作为业务组件首屏数据源复用。

2026-06-30 流程定义校验复发修正：首屏性能优化后，待办抽屉并行请求业务表单上下文时仍可能从待办列表行带入未归一化的 `processDefKey`。列表行、任务表单信息和 Flowable 运行时的流程定义表示可能不同，业务上下文首个请求应只以 `taskId` 作为可信任务身份输入，由后端任务详情补齐流程实例、业务 Key、节点和归一化流程定义信息。

2026-06-30 审批详情性能复查：日志显示采购审批详情打开时，前端直接请求 `/api/flow/task/form/{taskId}`，业务表单上下文后台又通过 FlowClient 请求同一接口，导致同一个任务表单信息加载两次。单次 `getTaskFormInfo` 内部还存在流程模型和 BPMN 节点重复解析，并且业务表单场景无意义查询 `sys_flow_form_instance`。业务表单详情应以业务上下文为首屏主数据源，动态/外部表单才补查流程表单接口。

2026-06-30 应用中心业务单元维护缺口：低代码应用和代码应用进入应用中心后，业务单元只能新建、设计、启停和删除，缺少编辑基础信息与调整归属业务域的入口。后端虽然有业务对象更新接口，但跨业务域修改只更新对象本身会导致访问入口、设计版本、触发器和单据配置仍停留在旧业务域，形成配置割裂。

2026-06-30 业务单元显示字段配置收敛：创建业务单元时字段尚未设计完成，要求用户手输“显示字段”不符合使用场景。关联关系设计器已经提供“运行态显示字段 / 目标对象回显字段”的下拉配置，业务单元基础信息只保留后续对象设计器中的默认标题字段作为兜底。

2026-06-30 应用配置易误操作修正：业务表单资产配置只能新增不能移除，Provider 默认资产即使用户不想使用也会在刷新后重新出现；采购审批示例直接硬编码流程标题，未消费应用管理中的流程标题模板；业务单元和业务域的停用操作缺少二次确认，编辑抽屉中切换启用状态也可能绕过确认。

2026-06-30 采购审批驳回状态修正：采购审批节点点击驳回后，采购单状态仍停留在 `IN_PROCESS`。采购单状态机依赖流程 `TASK_COMPLETED` 回调里的 `approvalResult=reject`，但流程任务完成监听器在 Flowable 任务完成事件中读取流程变量存在时序风险，导致业务回调没有可靠拿到驳回动作变量。

2026-06-30 采购审批驳回状态二次修正：验证后仍出现采购单状态未从 `IN_PROCESS` 切到 `NEED_MODIFY`，但流程已经进入“申请人修改”节点，重新提交又因状态不是待修改被拦截。业务状态不能只依赖上一个审批任务完成事件的变量，还必须以“申请人修改任务创建”作为状态同步事实，并兼容已错过事件的存量待办在保存时自愈。

2026-06-30 采购审批重新提交状态修正：申请人修改后重新提交，流程已经进入下一轮审批节点，但采购单状态仍停留在 `NEED_MODIFY`。业务状态必须继续以当前实际任务节点兜底同步：进入任一审批节点时状态应为 `IN_PROCESS`，不能只依赖申请人修改任务完成事件。

2026-06-30 采购审批存量状态对账：已经错过流程事件的存量采购单，不会再次触发 `TASK_CREATED`。采购单列表/详情加载时也需要按当前活跃待办节点做轻量状态对账，让已进入审批节点但仍显示 `NEED_MODIFY` 的记录刷新后自动恢复为 `IN_PROCESS`。

## 目标

1. 代码应用在应用管理中复用左侧“表单设计 / 列表设计 / 详情设置 / 业务流程配置”入口，配置内容保存到现有业务流程绑定配置中。
2. Provider 继续作为默认模板和数据适配器，不再作为字段目录和视图显示的唯一事实来源。
3. 流程设计器节点抽屉继续负责审批节点的表单资产、字段可见/可编辑/必填权限，应用管理不复制节点级配置。
4. 待办审批上下文返回的 `fields/recordData` 必须按节点字段权限裁剪，自定义 Vue 表单通过统一上下文 API 生效。
5. 代码业务列表和详情可以消费应用管理里的视图配置，至少采购审批示例页要支持按配置显示/隐藏列表列和详情字段。
6. 示例流程初始化只能在模型缺失或 XML 缺失时种子初始化，不能覆盖用户在流程设计器里维护过的 BPMN 节点配置。
7. 采购审批示例要沉淀成复杂代码业务表单接入流程的真实样板：通用业务表单资产、字段展示、列表/详情展示和流程表单引用都通过应用构建/流程设计器可视化维护，业务代码只保留运行时适配和必要兜底。
8. 应用中心业务单元应支持编辑基础信息、启用状态、排序和所属业务域；调整业务域时同步迁移该业务单元的通用配置引用。

## 非目标

1. 不把代码应用迁移成低代码对象，不要求用户在低代码表单设计器里重建采购审批页面。
2. 不新增流程节点权限的第二套配置入口。
3. 首期不新增数据库表，配置先复用 `ai_business_binding.binding_config.options.codeAppMetadata`。
4. 不在“业务流程配置”下新增“字段与视图”面板或自研字段配置 UI。

## 配置协议

`codeAppMetadata` 存储在流程绑定 `options` 下：

```json
{
  "codeAppMetadata": {
    "objectCode": "sample_purchase_order",
    "formAssets": [
      {
        "formKey": "sample_purchase_order_approval_form",
        "formName": "采购单审批表单",
        "formMode": "BUSINESS_CODE_FORM",
        "providerKey": "samplePurchaseOrder",
        "formUrl": "/business/purchase-order-test",
        "fields": []
      }
    ],
    "fields": [
      {
        "field": "title",
        "fieldCode": "title",
        "label": "采购主题",
        "componentType": "input",
        "visible": true,
        "readonly": false,
        "internal": false,
        "systemField": false
      }
    ],
    "formDesignerSchema": {
      "schemaVersion": "form-first-v2",
      "mode": "multi",
      "settings": { "formAssets": [] },
      "forms": []
    },
    "viewSchema": {
      "schemaVersion": "view-schema-v1",
      "search": { "fields": [], "settings": {} },
      "list": {
        "columns": [
          { "fieldCode": "orderNo", "label": "采购单号", "visible": true, "order": 0 },
          { "fieldCode": "title", "label": "采购主题", "visible": true, "order": 1 }
        ],
        "settings": {}
      },
      "detail": {
        "sections": [
          {
            "sectionKey": "basic",
            "title": "基础信息",
            "visible": true,
            "order": 0,
            "fields": [
              { "fieldCode": "orderNo", "label": "采购单号", "visible": true, "order": 0 },
              { "fieldCode": "title", "label": "采购主题", "visible": true, "order": 1 }
            ]
          }
        ],
        "settings": {}
      },
      "overrides": {}
    }
  }
}
```

## 需求

### R1 应用管理配置

- 代码应用的对象设计器左侧显示“表单设计”“列表设计”“业务流程配置”，表单页签内继续包含“详情设置”。
- 代码应用进入设计器时从后端返回的 Provider 默认资产初始化 `fields/modelSchema/formDesignerSchema/viewSchema/pageSchema`。
- “表单设计”复用 `BusinessFormDesigner`，保存输出 `formDesignerSchema` 和字段资产。
- “列表设计”复用 `BusinessListDesigner`，保存输出 `viewSchema.list.columns`。
- “详情设置”复用 `BusinessDetailDesigner`，保存输出 `viewSchema.detail.sections`。
- “业务流程配置”只负责流程绑定和打开流程设计器，不再出现“字段与视图”配置项。
- 保存时写入现有 `options.codeAppMetadata`，不能丢失已有流程模型、变量映射和业务绑定配置。

### R2 后端元数据合并

- `BusinessFlowService#getFormAssets(objectCode)` 返回配置化资产；无配置时返回 Provider 默认资产。
- 配置化字段必须合并 Provider 默认字段的运行时必要属性，例如 `providerKey/formUrl/formMode/supportsSave`。
- 配置中隐藏或标记为内部/系统的字段不能出现在默认公开字段目录中。
- `BusinessFlowAppConfigService` 返回 `options.codeAppMetadata`，保存代码应用配置时把 DTO options 写入流程绑定 options。
- 代码应用表单资产按现有 `formDesignerSchema.settings.formAssets` 兼容输出，不新增表单设计协议。

### R3 待办字段权限

- 代码表单待办上下文必须以 BPMN 节点 `formFieldPermissions` 为边界。
- 节点字段权限默认全部可见；未出现在权限列表中的字段保持可见但只读，只有显式 `readable=false` 的字段才隐藏。
- 自定义 Vue 业务表单通过 `useBusinessTaskFormContext.canShowField/canEditField/fieldPermission` 控制显示、禁用和校验。
- `BusinessFlowService` 读取节点权限时优先按 `taskId` 获取任务表单信息；当只有 `processInstanceId/businessKey/processDefKey/taskDefKey` 时，必须通过流程表单信息接口兜底读取 BPMN 节点配置。
- 采购审批页待办模式必须先加载业务表单上下文，再根据上下文的 `businessKey/recordId/taskDefKey` 渲染页面，不能因页面未传业务单 ID 而跳过字段权限加载。
- 待办页顶部“采购单信息”也必须叠加节点字段权限过滤，不能只按详情视图配置展示。

### R4 列表/详情视图配置

- 提供前端 composable/API 读取代码应用 `viewSchema`。
- 采购审批示例列表列按 `viewSchema.list.columns` 配置过滤和排序。
- 采购审批示例详情弹窗按 `viewSchema.detail.sections` 配置显示/隐藏。
- 没有配置时保持现有默认列和详情字段。

### R5 兼容性

- 未保存 `codeAppMetadata` 的代码应用仍使用现有 Provider 默认字段。
- 旧的流程绑定、节点表单配置和待办审批路径保持可用。
- 应用管理中节点权限仍通过“打开流程设计器”进入真实流程设计器维护。

### R6 示例流程初始化保护

- `SamplePurchaseOrderServiceImpl#ensureFlowModel` 只在流程模型不存在时创建默认 BPMN。
- 当模型已存在且 BPMN XML 非空时，初始化和发起流程不得调用 `updateModel` 覆盖已有 XML。
- 当模型已存在但未发布时，可以发布当前已有模型；当模型 XML 缺失时，才允许写入默认示例 XML。

### R7 代码应用字段基准与表单显隐

- 代码应用设计器加载时必须以当前 Provider 返回的字段为基准，`codeAppMetadata.fields/formAssets` 只作为标签、显隐、排序、组件显示属性的覆盖层。
- Provider 新增的公开字段在下一次进入应用管理时应自动补进表单设计、列表设计和详情设置；已显式隐藏的字段不能因为 Provider 仍返回而重新展示。
- 代码应用默认表单 schema 必须包含公开只读字段，用户可以通过既有表单设计器配置显示/隐藏；这只控制通用字段显隐，不要求控制代码表单的布局。
- 保存代码应用表单设计时，应把表单画布中字段组件的存在与 `visibility.hidden` 同步为 `fields[].formVisible`。
- 待办业务表单上下文必须识别 `fields[].formVisible=false` 并裁剪字段，采购审批自定义 Vue 表单也必须通过 `useCodeAppMetadata` 消费 FORM 显隐配置。

### R8 业务表单资产可视化配置

- 代码应用必须在应用构建中提供“业务表单资产”配置入口，用于维护 `appName/objectName/businessName/formKey/formName/formUrl/description/providerKey` 等资产信息。
- `formMode=BUSINESS_CODE_FORM`、`supportsSave`、字段目录等运行能力由 Provider 或平台推导；用户不需要手写 JSON。
- `providerKey` 应来自已注册 Provider 或当前 Provider 默认值；用户可以确认/选择，但不应靠业务代码里散落 `asset.put(...)` 作为主配置。
- `fields/fieldCatalog` 以当前业务表/Provider 字段目录为基准，应用管理只配置标题、排序、显示隐藏等通用属性。
- 保存时继续写入 `ai_business_binding.binding_config.options.codeAppMetadata.formAssets`，不新增数据库表；未保存配置时才使用 Provider 返回的默认资产兜底。
- 采购审批示例中的 Provider 可以保留默认资产兜底，但运行时和设计器应优先读取应用管理保存的 `codeAppMetadata.formAssets`。

### R9 流程全局表单引用应用表单资产

- 流程模型设计器进入业务对象上下文时，“更多设置 > 表单配置”不再引导用户重新设计动态表单。
- 有业务对象上下文时，流程全局表单配置应选择应用构建中的业务表单资产，保存 `formType=business` 以及 `formKey/formName/formMode/providerKey/formUrl/formRef` 等引用信息。
- 独立流程没有业务对象上下文时，保留原有 `dynamic/external/none` 动态表单设计能力。
- 审批节点默认继承流程全局业务表单；节点抽屉可以选择同一业务对象下的其他应用表单资产，并只维护字段可见/可写/必填权限。
- 运行时解析优先级为：节点表单引用 > 流程全局表单引用 > 应用默认表单资产 > Provider/系统兜底。
- 流程设计器字段权限目录必须来自当前选择的应用表单资产字段目录；没有单独节点表单时，使用流程全局表单字段目录。

### R10 业务单元编辑与业务域归属维护

- 应用总览业务单元卡片、业务域详情对象卡片、业务单元详情页都应提供“编辑业务单元”入口。
- 编辑内容包含所属业务域、业务单元名称、对象类型、图标、排序、启用状态和业务说明。
- 对象编码作为系统引用只读展示，避免修改后破坏运行配置、流程绑定和路由。
- 保存编辑时必须保留业务对象已有 `modelId/modelCode/options` 等隐藏配置。
- 业务单元跨业务域移动时，后端事务内同步迁移访问入口、设计版本、触发器和单据配置的 `suite_code`。
- 已配置对象关系的业务单元暂不允许跨业务域移动，避免单套件关系模型被拆断；后端需返回明确错误提示。
- 新建业务单元和基础信息编辑不展示“显示字段”；关联回显字段通过关联关系设计器维护，对象设计器只保留“默认标题字段”下拉作为兜底。

### R11 表单资产移除、标题模板与误操作确认

- 业务表单资产面板必须支持移除已配置资产，移除状态保存到 `codeAppMetadata.removedFormAssetKeys`。
- Provider 默认资产被移除后，后端合并 `formAssets` 时不得再次自动补回；用户通过新增重新添加时应清除对应移除标记。
- 采购审批示例发起流程时必须读取业务对象 FLOW/APPROVAL 绑定中的 `titleTemplate`，按业务数据替换 `${field}` 和 `{field}`；未配置时继续使用原有采购单号兜底标题。
- 业务单元和业务域的“停用”操作必须二次确认；删除操作继续保留确认框。
- 业务单元和业务域编辑抽屉中，如果把已启用记录保存为停用状态，也必须二次确认。

### R12 采购审批驳回状态

- 流程任务完成前应先把审批动作变量写入流程实例，确保任务完成事件和业务回调可以读取 `approvalResult/approved`。
- 采购审批业务回调不能只依赖单一 `approvalResult=reject` 字符串，应同时兼容 `approved=false`。
- 非申请人修改节点的审批驳回必须把采购单状态更新为 `NEED_MODIFY`，申请人修改节点选择终止/驳回时更新为 `REJECTED`。
- 申请人修改任务创建时，如果采购单仍为 `IN_PROCESS`，必须兜底同步为 `NEED_MODIFY`。
- 对已经进入申请人修改节点但状态仍为 `IN_PROCESS` 的存量待办，保存申请人修改节点字段时必须先自愈为 `NEED_MODIFY`，避免重新提交被状态机拦截。
- 申请人修改后重新提交并进入任一审批节点时，如果采购单仍为 `NEED_MODIFY`，必须兜底同步为 `IN_PROCESS`。
- 对已经进入审批节点但状态仍为 `NEED_MODIFY` 的存量待办，保存审批节点字段时必须先自愈为 `IN_PROCESS`，避免后续审批保存被状态机拦截。
- 采购单列表/详情加载时，应按当前活跃待办节点对 `IN_PROCESS/NEED_MODIFY` 两类流转中状态做对账，修复已经错过事件的存量记录。

## 验收标准

1. 进入采购审批应用管理，左侧可以看到“表单设计”“列表设计”“业务流程配置”，表单页签内可以切到“详情设置”。
2. 打开“业务流程配置”，业务配置中心只显示“流程配置”，不再显示“字段与视图”。
3. 修改表单或列表/详情设计后保存，配置写入 `options.codeAppMetadata.formDesignerSchema/viewSchema`。
4. 采购审批列表列展示按 `viewSchema.list.columns` 变化，详情弹窗按 `viewSchema.detail.sections` 变化。
5. 流程设计器节点字段权限设置后，待办审批页未授权可写字段仍可见但只读；显式配置不可见的字段不展示。
6. 未配置元数据的新代码 Provider 仍按 Provider 默认字段进入流程设计器字段目录。
7. 修改采购流程节点字段权限后发起采购单，重新进入流程设计器配置不应被 `SamplePurchaseOrderCodeFormProvider` 或示例 BPMN 重置。
8. 待办审批页只传 `taskId` 或只传 `processInstanceId + taskDefKey` 时，仍能读取 BPMN `formFieldPermissions` 并按权限隐藏字段。
9. 进入采购审批代码应用的“业务流程配置”，可以维护业务表单资产名称、表单 key、表单 URL、说明和 Provider 引用；保存后重新进入仍回显。
10. 流程设计器从采购审批应用进入时，全局“表单配置”显示应用表单资产选择，不再展示“设计动态表单”作为主入口。
11. 流程节点未单独选择表单时继承全局业务表单；节点字段权限基于该表单资产字段目录配置。
12. 采购审批流程中任一审批节点驳回后，业务单状态应进入 `NEED_MODIFY`；申请人修改节点重新提交后回到 `IN_PROCESS`；申请人选择终止后进入 `REJECTED`。
13. 待办处理页展示“部门负责人审批”时不再追加 `dept_leader_approve`；列表批量处理错误提示、已办和我发起页面也使用相同节点展示规则。
14. 待办列表中同一业务对象的 `ai_business_document_config` 解析应在本次列表请求内复用，审批表单上下文加载不应先解析一次业务对象再重复查询运行配置和单据配置。
15. 审批单加载时 `sys_flow_model.model_key` 查询参数必须是业务流程模型 Key，不应出现 UUID 型 `processDefinitionId`；业务代码表单不应触发 `sys_flow_form.form_key=sample_purchase_order_approval_form` 的动态表单查询。
16. 待办业务表单校验流程定义时，`sample_purchase_order`、`sample_purchase_order:3:xxx` 和历史 UUID 型流程定义标识应按兼容规则处理，不应误报“流程定义与当前任务不匹配”。
17. 待办审批详情打开代码业务表单时，父级已加载的 `businessFormContext.recordData` 应直接传给业务表单组件用于首屏渲染，不应再重复请求 `/ai/business/flow/task-form-context`、代码应用配置和采购单详情接口。
18. 待办审批详情首个业务表单上下文请求应只传 `taskId`，不能把待办列表行里可能过期或未归一化的 `processDefKey` 带入任务身份校验。
19. 后端流程定义表示差异不应作为硬安全边界直接阻断业务表单加载；任务 ID、办理人、流程实例、业务 Key 和任务节点仍必须继续校验。
20. 采购审批这类业务表单详情首屏只应产生一次任务表单信息解析：前端不再直接请求 `/api/flow/task/form/{taskId}`，由 `/ai/business/flow/task-form-context` 后台解析后返回业务表单上下文和审批策略。
21. `FlowTaskServiceImpl#getTaskFormInfo` 单次调用内不应重复查询 `sys_flow_model` 或重复解析 BPMN 节点；业务表单不应查询 `sys_flow_form_instance` 快照。
22. 采购审批自定义业务表单内部的节点标签不得展示 `dept_leader_approve`、`engineering_manager_approve` 等技术编码，应展示业务可读节点名称。
23. 业务单元可从应用总览、业务域详情和业务单元详情进入编辑；修改基础信息或归属业务域保存后重新进入应回显，访问入口等关联配置应跟随新的业务域展示。
24. 业务表单资产面板可以移除资产，保存后重新进入不应因为 Provider 默认资产自动合并而重新出现；点击新增后可以重新添加默认资产。
25. 采购审批流程标题模板配置为包含 `{orderNo}`、`${title}` 等字段时，发起流程后的标题应按模板生成，而不是固定 `采购单审批-采购单号`。
26. 应用总览、业务域详情、业务单元详情和编辑抽屉中的业务域/业务单元停用操作都应出现二次确认；删除业务域/业务单元继续出现二次确认。
27. 采购审批任一审批节点点击“驳回”后，采购单状态应由 `IN_PROCESS` 更新为 `NEED_MODIFY`，不能继续显示“审批中”。

## 实现记录

| 任务 | 状态 | 说明 |
| --- | --- | --- |
| Task 1 | completed | SDD 文档与配置协议 |
| Task 2 | completed | 后端元数据保存与合并 |
| Task 3 | completed | 代码应用复用既有表单/列表/详情设计器 |
| Task 4 | completed | 采购审批列表/详情消费视图配置 |
| Task 5 | completed | 验证与执行日志 |
| Task 6 | completed | 修复示例 BPMN 覆盖和待办字段权限运行态 |
| Task 7 | completed | 修复代码应用字段基准合并、默认 schema 补齐和表单显隐运行态 |
| Task 8 | completed | 业务表单资产可视化配置、流程全局表单引用应用表单资产、采购审批样板收敛 |
| Task 9 | completed | 业务表单资产重复名称收敛、Provider 来源提示、左侧设计器空 schema 补齐 |
| Task 10 | completed | Provider Bean 目录下拉、表单 Key 系统引用、代码应用字段目录从 Provider 兜底 |
| Task 11 | completed | 已有业务对象的代码应用强制进入 Provider 字段设计器路径，并修复 metadata 保存 |
| Task 12 | completed | 待办节点展示去除技术编码，列表/审批表单业务上下文解析去重 |
| Task 13 | completed | 审批表单加载修正 processDefinitionId 误当 modelKey、业务代码表单误查动态表单和重复表单信息调用 |
| Task 14 | completed | 修正流程定义多种表示直接字符串比较导致的待办业务表单误拦截 |
| Task 15 | completed | 待办审批详情表单首屏复用父级业务上下文，减少串行和重复请求 |
| Task 16 | completed | 首个业务上下文请求只信任 taskId，流程定义表示差异不再硬失败 |
| Task 17 | completed | 审批详情业务表单去掉重复 task/form 请求，后端单次表单解析去重 |
| Task 18 | completed | 采购审批业务表单内部节点标签不再显示 taskDefKey 技术编码 |
| Task 19 | completed | 应用中心业务单元编辑入口、归属业务域修改和关联配置迁移 |
| Task 20 | completed | 新建/编辑业务单元移除显示字段手填，关联回显收敛到关联关系配置 |
| Task 21 | completed | 业务表单资产移除、采购审批流程标题模板生效、业务域/业务单元停用确认 |
| Task 22 | completed | 采购审批驳回后状态从审批中流转为待修改 |
| Task 23 | completed | 申请人修改任务创建同步待修改状态，并兼容存量待办保存前自愈 |
| Task 24 | completed | 申请人修改后重新提交同步审批中状态，并兼容存量审批待办保存前自愈 |
| Task 25 | completed | 采购单列表/详情按当前活跃待办节点对账修复存量状态 |
