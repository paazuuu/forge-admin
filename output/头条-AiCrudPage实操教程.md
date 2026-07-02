# 一个 CRUD 页面从 2 天到 10 分钟：用 Forge Admin 的 AiCrudPage 实操

> 不讲架构，不讲原理，这篇就是手把手：怎么用 Forge Admin 10 分钟搭一个能跑的 CRUD 页面。

做过后台的都知道，一个"列表 + 搜索 + 新增 + 编辑 + 删除"的标准 CRUD 页面，要写多少东西：

- Controller（分页查询、详情、新增、修改、删除 5 个接口）
- Service + ServiceImpl（业务逻辑）
- Mapper + Mapper XML（SQL）
- Entity + DTO + VO（三层数据对象）
- Vue 页面（搜索表单、表格、弹窗表单、增删改查按钮、分页）

熟练的开发，两天能写完一个。不熟练的，三天起步。

但如果你的后台框架有一套协议驱动的 CRUD 组件，**10 分钟就能配出一个能跑的页面**。

这篇就用 Forge Admin 的 `AiCrudPage` 组件，从零搭一个"业务类型管理"页面，全程配置，几乎不写业务代码。

---

## 一、最终效果：我们要搭什么

目标：一个"消息业务类型管理"页面，包含：

- 顶部搜索：业务类型编码、名称、状态
- 表格列表：编码、名称、跳转方式、图标、排序、状态
- 新增/编辑弹窗：表单填写
- 删除确认

效果就是后台系统里最常见的那个页面。区别是：**别人写两天，我们配 10 分钟。**

---

## 二、第一步：准备后端接口

`AiCrudPage` 不生成接口，它只消费接口。所以后端要先准备好标准的 5 个 RESTful 接口：

| 操作 | 方法 | 路径 |
|------|------|------|
| 分页查询 | GET | `/api/message/bizType/page` |
| 详情查询 | GET | `/api/message/bizType/:id` |
| 新增 | POST | `/api/message/bizType` |
| 修改 | PUT | `/api/message/bizType` |
| 删除 | DELETE | `/api/message/bizType/:id` |

如果你用了 Forge 的代码生成器，这 5 个接口是一键生成的。这里假设你已经有了。

> 关键约定：分页参数前端传 `pageNum` + `pageSize`，后端必须用相同命名接收。URL 占位符用 `:id`（冒号），不是 `{id}`（花括号）。

---

## 三、第二步：写 apiConfig（告诉组件接口在哪）

`AiCrudPage` 的核心配置之一是 `apiConfig`，它告诉组件每个操作对应哪个接口：

```js
const apiConfig = {
  list: 'get@/api/message/bizType/page',
  detail: 'get@/api/message/bizType/:id',
  add: 'post@/api/message/bizType',
  update: 'put@/api/message/bizType',
  delete: 'delete@/api/message/bizType/:id',
}
```

格式是 `方法@路径`，就这一行一个接口，清晰明了。

- `list`：分页查询
- `detail`：详情（编辑时回显用）
- `add`：新增
- `update`：修改
- `delete`：删除

注意占位符是 `:id`，不是 `{id}`。这是组件的约定，写错了识别不了。

---

## 四、第三步：配置搜索表单（searchSchema）

搜索表单也是一个 JSON 数组，每个对象是一个搜索字段：

```js
const searchSchema = [
  {
    field: 'bizType',
    label: '业务类型编码',
    type: 'input',
    props: {
      placeholder: '请输入业务类型编码',
    },
  },
  {
    field: 'bizName',
    label: '业务类型名称',
    type: 'input',
    props: {
      placeholder: '请输入业务类型名称',
    },
  },
  {
    field: 'enabled',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '请选择状态',
      clearable: true,
      options: [
        { label: '启用', value: 1 },
        { label: '禁用', value: 0 },
      ],
    },
  },
]
```

`type` 支持的常用类型：

- `input`：文本输入
- `select`：下拉选择
- `datePicker`：日期选择
- `dict`：字典下拉（配合 `useDict` 用）

每个字段的 `props` 会透传给底层 Naive UI 组件，所以 Naive UI 支持的属性这里都能用。

> 如果是状态、类型这种枚举，建议用字典组件 `DictSelect`，不要在页面里写死 `options`。这是项目规范。

---

## 五、第四步：配置表格列（columns）

表格列也是一个数组：

```js
const tableColumns = [
  { prop: 'bizType', label: '业务类型编码', width: 150 },
  { prop: 'bizName', label: '业务类型名称', width: 150 },
  { prop: 'jumpUrl', label: '跳转URL模板', ellipsis: { tooltip: true } },
  {
    prop: 'jumpTarget',
    label: '跳转方式',
    width: 100,
    render: (row) => {
      return h(NTag, { size: 'small' }, {
        default: () => row.jumpTarget === '_blank' ? '新窗口' : '当前页',
      })
    },
  },
  { prop: 'icon', label: '图标', width: 100 },
  { prop: 'sort', label: '排序', width: 80 },
]
```

几个常用属性：

- `prop`：字段名（对应后端返回的字段）
- `label`：列标题
- `width`：列宽
- `ellipsis`：超长省略，`tooltip: true` 鼠标悬停显示完整内容
- `render`：自定义渲染（用 `h` 函数渲染标签、按钮等）

需要渲染状态标签、操作按钮、图片的地方，用 `render` 写。不需要自定义的，填 `prop` + `label` 就行。

---

## 六、第五步：配置新增/编辑表单（editSchema）

弹窗里的表单也是 schema 配置：

```js
const editSchema = [
  {
    field: 'bizType',
    label: '业务类型编码',
    type: 'input',
    rules: { required: true, message: '请输入业务类型编码' },
  },
  {
    field: 'bizName',
    label: '业务类型名称',
    type: 'input',
    rules: { required: true, message: '请输入业务类型名称' },
  },
  {
    field: 'jumpUrl',
    label: '跳转URL模板',
    type: 'input',
  },
  {
    field: 'jumpTarget',
    label: '跳转方式',
    type: 'select',
    props: {
      options: [
        { label: '当前页', value: '_self' },
        { label: '新窗口', value: '_blank' },
      ],
    },
  },
  {
    field: 'sort',
    label: '排序',
    type: 'inputNumber',
    defaultValue: 0,
  },
  {
    field: 'enabled',
    label: '状态',
    type: 'switch',
    defaultValue: 1,
  },
]
```

`rules` 是表单校验规则，`defaultValue` 是新增时的默认值。

新增和编辑共用一份 schema。编辑时，组件会自动调 `detail` 接口回显数据（需要配 `:load-detail-on-edit="true"`）。

---

## 七、第六步：组装页面

把上面三份配置交给 `AiCrudPage`，页面就完成了：

```vue
<template>
  <div class="biz-type-page">
    <AiCrudPage
      ref="crudRef"
      :api-config="apiConfig"
      :search-schema="searchSchema"
      :columns="tableColumns"
      row-key="id"
      :edit-schema="editSchema"
      :load-detail-on-edit="true"
    />
  </div>
</template>

<script setup>
import { NTag } from 'naive-ui'
import { h } from 'vue'
import { AiCrudPage } from '@/components/ai-form'

defineOptions({ name: 'MessageBizType' })

const crudRef = ref(null)

const apiConfig = {
  list: 'get@/api/message/bizType/page',
  detail: 'get@/api/message/bizType/:id',
  add: 'post@/api/message/bizType',
  update: 'put@/api/message/bizType',
  delete: 'delete@/api/message/bizType/:id',
}

const searchSchema = [ /* 上面第四步的内容 */ ]
const tableColumns = [ /* 上面第五步的内容 */ ]
const editSchema = [ /* 上面第六步的内容 */ ]
</script>
```

就这么多。一个完整的 CRUD 页面，包含搜索、表格、分页、新增弹窗、编辑回显、删除确认，全部到位。

整个 Vue 文件大约 150 行，其中大部分是配置数据，几乎不写逻辑代码。

---

## 八、如果不想写代码：用低代码搭建器

上面是"写配置"的方式。如果你连配置都不想写，Forge 还提供了**可视化低代码搭建器**：

1. 进入"AI 应用开发"页面
2. 点"AI 智能开发"，用自然语言描述需求："我要一个业务类型管理，有编码、名称、跳转方式、排序、状态"
3. AI 生成数据模型和页面配置
4. 在可视化编辑器里拖拽微调
5. 发布，菜单自动注册

这条路适合：

- 不想写任何代码的业务人员
- 快速原型验证
- 标准化程度高的 CRUD 业务

而 `AiCrudPage` 适合：

- 开发者想掌控细节
- 业务有自定义逻辑
- 需要下载代码二次开发

两条路，按需选。

---

## 九、和传统写法对比

| 对比项 | 传统手写 | AiCrudPage 配置 |
|--------|----------|-----------------|
| 开发时间 | 1-2 天 | 10 分钟 |
| 代码量 | 5-6 个文件，数百行 | 1 个 Vue 文件，约 150 行 |
| 改字段 | 改 Entity/DTO/VO/Mapper/Vue | 改 schema 配置 |
| 加搜索条件 | 改 Mapper XML + Vue | 加一个 schema 对象 |
| 改表格列 | 改 Vue | 改 columns 数组 |
| 一致性 | 看开发水平 | 组件统一保证 |
| 复杂业务 | 灵活 | 可下载代码包二次开发 |

这不是说传统写法没用。复杂业务逻辑、复杂联表查询、特殊交互，该手写还是手写。但 80% 的标准 CRUD 页面，用配置能省大量时间。

---

## 十、几个踩坑提醒

**1. 占位符用 `:id` 不是 `{id}`**

```js
// ✅ 对
detail: 'get@/api/message/bizType/:id'

// ❌ 错
detail: 'get@/api/message/bizType/{id}'
```

组件只识别 `includes(':id')`，写花括号会匹配不到。

**2. 分页参数叫 `pageNum` 不叫 `page`**

后端 Controller 必须用 `@RequestParam(defaultValue = "1") Integer pageNum`，不能用 `page`。前后端命名必须一致。

**3. 字典别写死 options**

状态、类型这种枚举值，不要在 schema 里写死 `options`。维护到 `sys_dict_type` + `sys_dict_data`，前端用 `useDict` 或 `DictSelect`。这样加一个枚举值不用改代码。

**4. schema 定义成 computed**

如果 schema 里用了字典（`useDict`），必须把 schema 定义成 `computed`，确保字典异步加载后响应式更新：

```js
const editSchema = computed(() => [
  {
    field: 'status',
    label: '状态',
    type: 'dict',
    dictType: 'sys_normal_disable',
  },
])
```

**5. 图片字段用 AuthImage**

如果表格里有图片列，用 `AuthImage` 组件渲染（自动带 Token），不要直接用 `NAvatar` 的 `src`。`imageUpload` 存的是 fileId 不是 URL。

---

## 十一、总结：什么时候用 AiCrudPage

**适合用：**

- 标准 CRUD 管理页面（用户、角色、字典、配置、业务类型……）
- 字段以表单输入、下拉、开关为主
- 搜索条件简单
- 团队想统一页面风格和交互

**不适合用：**

- 复杂多表联查的报表页面
- 有复杂交互逻辑的页面（拖拽排序、实时计算、复杂联动）
- 非标格式的页面（看板、日历、流程图）

**我的建议是：** 先用 `AiCrudPage` 把 80% 的标准页面快速搞定，省下的时间投入到那 20% 真正需要手写的复杂业务上。这才是低代码该有的价值——不是替代开发，是把重复劳动自动化。

---

## 上手体验

光看不练假把式，直接上演示站（账号 `admin` / `123456`）：

- 后台演示：http://www.dlforgelab.com:8084/forge/login
- 项目文档：http://www.dlforgelab.com:8084/forge-docs/
- Gitee：https://gitee.com/ForgeLab/forge-admin
- GitHub：https://github.com/yaomindong1996/forge-admin

> 你平时搭一个 CRUD 页面要多久？有没有试过用配置驱动的方式？评论区聊聊，觉得有用求个👍收藏。
