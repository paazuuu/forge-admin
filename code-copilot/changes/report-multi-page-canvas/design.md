# 设计说明 — 报表多画布与下钻页面能力

## 1. 总体思路
本次把「一个报表项目」从单画布升级为多页面容器。项目仍然只有一个 `projectId`、一个发布地址、一个后端 `componentData` 字段；页面是项目 JSON 内部的逻辑单位。

```text
ReportProject
└── componentData
    ├── homePageId
    ├── activePageId
    ├── pages[]
    │   ├── 首页
    │   │   ├── editCanvasConfig
    │   │   ├── requestGlobalConfig
    │   │   └── componentList
    │   └── 区域详情
    │       ├── editCanvasConfig
    │       ├── requestGlobalConfig
    │       └── componentList
    └── sharedRequestGlobalConfig
```

## 2. 数据协议
### 2.1 类型草案
```ts
export type ReportPageTransition = 'none' | 'fade' | 'slide-left' | 'slide-right' | 'zoom'

export interface ReportCanvasPage {
  id: string
  name: string
  sort: number
  editCanvasConfig: EditCanvasConfigType
  requestGlobalConfig: RequestGlobalConfigType
  componentList: Array<CreateComponentType | CreateComponentGroupType>
}

export interface ReportMultiPageStorage {
  version: 2
  homePageId: string
  activePageId: string
  pageTransition?: ReportPageTransition
  pages: ReportCanvasPage[]
  sharedRequestGlobalConfig?: Partial<RequestGlobalConfigType>
}

export type ReportProjectStorage = ChartEditStorage | ReportMultiPageStorage
```

### 2.2 协议工具
集中新增协议工具，避免各入口散落判断：

```text
forge-report-ui/src/utils/reportPages.ts
```

关键函数：
```ts
export function isMultiPageStorage(storage: unknown): storage is ReportMultiPageStorage
export function normalizeProjectStorage(storage: unknown, fallbackName?: string): ReportMultiPageStorage
export function createDefaultPage(name?: string): ReportCanvasPage
export function extractPageStorage(project: ReportMultiPageStorage, pageId?: string): ChartEditStorage
export function updatePageStorage(project: ReportMultiPageStorage, pageId: string, storage: ChartEditStorage): ReportMultiPageStorage
export function removeInvalidPageActions(project: ReportMultiPageStorage, removedPageId: string): ReportMultiPageStorage
```

## 3. Store 设计
保留 `chartEditStore` 作为当前画布编辑 store，新增多页面管理字段和动作。

```ts
state: {
  projectPages: [],
  activePageId: '',
  homePageId: '',
  pageTransition: 'fade',
  runtimePageContext: {}
}
```

关键动作：
```ts
loadProjectStorage(storage: unknown): Promise<void>
getProjectStorageInfo(): ReportMultiPageStorage
flushCurrentPage(): void
switchPage(pageId: string): Promise<void>
createPage(copyFromPageId?: string): Promise<string>
renamePage(pageId: string, name: string): void
deletePage(pageId: string): void
duplicatePage(pageId: string): Promise<string>
setHomePage(pageId: string): void
setRuntimePageContext(context: Record<string, any>): void
```

原则：
- 编辑器和预览页都只渲染当前 `editCanvasConfig + requestGlobalConfig + componentList`。
- 多页面 store 负责在页面切换时把当前画布快照写回 `pages`。
- `getStorageInfo()` 可保留给单页兼容，但保存项目必须使用 `getProjectStorageInfo()`。

## 4. 编辑器 UI
首期建议新增一个轻量页面管理面板，不做 Figma 连线原型。

位置建议：
- 优先放在左侧图层面板附近，和「图层」属于同一编辑上下文。
- 如果左侧空间不足，可做底部页面条，类似 PPT 页面缩略栏。

能力：
- 页面列表：显示名称、首页标记、当前页高亮。
- 新增页面：创建空白画布。
- 复制页面：深拷贝当前页，所有组件生成新 ID。
- 删除页面：至少保留一个页面；删除时清理跳转到该页面的动作。
- 重命名页面：内联编辑或弹窗。
- 设置首页：发布默认打开页面。
- 拖拽排序：可第二阶段做，第一阶段可用上移/下移。

## 5. 预览与发布
预览入口：
```text
/chart/preview/:projectId
/chart/preview/:projectId?pageId=page-region
```

运行时状态：
```ts
activePageId
runtimePageContext
pageTransition
```

预览流程：
```text
读取 componentData
  ↓
normalizeProjectStorage()
  ↓
选择 query.pageId 或 homePageId
  ↓
extractPageStorage()
  ↓
回填 chartEditStore
  ↓
渲染 PreviewRenderList
```

页面跳转流程：
```text
组件触发 click
  ↓
读取 component.events.actions
  ↓
resolveDrillParams()
  ↓
flush/更新 runtimePageContext
  ↓
switchPreviewPage(targetPageId)
  ↓
过渡动画 + 重新初始化数据池
```

## 6. 组件跳转动作
新增结构化动作配置，与现有 JS 基础事件并存。

```ts
export interface ComponentAction {
  id: string
  trigger: 'click' | 'dblclick'
  type: 'goPage'
  targetPageId: string
  transition?: ReportPageTransition
  params?: DrillParamBinding[]
}

export interface DrillParamBinding {
  id: string
  targetKey: string
  source: 'static' | 'componentField' | 'pageContext' | 'userContext'
  sourceKey?: string
  value?: string | number | boolean | null
}
```

第一版重点支持：
- 点击/双击跳页面。
- 参数固定值。
- 从组件事件数据中取字段。
- 从当前页面上下文继承参数。

## 7. 下钻上下文
新增页面上下文来源，并接入现有动态参数机制。

示例：
```json
{
  "regionCode": "150100",
  "regionName": "呼和浩特市",
  "sourcePageId": "page-home"
}
```

动态请求参数可选择：
```text
source = pageContext
sourceKey = regionCode
target = Params
targetKey = regionCode
```

这样详情页组件不需要写代码，只要把请求参数绑定到页面上下文即可。

## 8. 兼容策略
- 老项目加载：自动包装成一个首页。
- 老项目保存：编辑保存后写入新协议。
- 旧组件事件：继续执行 `events.baseEvent` 中的 JS。
- 新动作字段不存在：按空数组处理。
- 代码编辑页和导入导出：必须支持新协议，但也接受旧协议导入。

## 9. 不纳入第一版
- Figma 式节点连线和可视化原型流。
- 页面缩略图实时截图。
- 页面级权限。
- 跨项目跳转。
- 弹层页面、抽屉页面、覆盖层页面。
- 页面历史栈的完整可视化管理；第一版只提供返回上一页动作的基础能力可选。
