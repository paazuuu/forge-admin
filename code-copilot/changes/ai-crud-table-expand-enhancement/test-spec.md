# 单测 Spec — AiCrudPage 表格增强与通用展开面板
> status: propose
> created: 2026-06-27

## 0. 测试原则
- **Red/Green TDD**：新增核心纯函数测试时先确认失败再实现。
- **First Run the Tests**：开始实现前先跑前端现有可用检查，确认基线。
- **展示工作**：执行后把命令、结果、警告、跳过项追加到 `execution-log.md`。
- **增量复用**：开始 `/test`、阶段验证或归档前验收前，先读取 `code-copilot/rules/automated-testing-standard.md` 和本变更已有文档。

## 1. 测试框架
| 项目 | 值 |
|------|-----|
| 前端框架 | Vue 3 + Vite |
| 组件库 | Naive UI |
| 包管理 | pnpm，执行前使用 Node v20.19.0 |
| 单测框架 | 待按项目现有配置确认 |
| E2E | 可复用 pc-e2e-tester / Playwright 验证运行态页面 |

## 2. 覆盖范围

### P0 — 核心配置与数据加载（必须覆盖）
#### 模块: `expand-utils.js`
| 方法 | 场景 | 输入 | 预期结果 |
|------|------|------|----------|
| `buildExpandParams` | 从当前行映射参数 | `paramsMap={orderId:'row.id'}`，`row.id=1` | 输出 `{orderId:1}` |
| `buildExpandParams` | 支持多 source | `row/query/params/user` | 正确取值，不存在时返回空或默认值 |
| `extractExpandData` | records 包裹 | 响应 `{data:{records:[]}}` | 输出 records |
| `normalizeExpandConfig` | 单 panel | 传入单个 table 配置 | 归一化为 `panels[]` |
| `normalizeExpandConfig` | 未启用 | `enabled=false` 或空配置 | 不渲染展开列 |

### P1 — 组件渲染
| 组件 | 场景 | 预期 |
|------|------|------|
| `AiTable` | 开启 `resizable` | 表头可拖拽调整列宽 |
| `AiCrudRowExpand` | 首次展开 | 显示 loading，请求成功后渲染内容 |
| `AiCrudRowExpand` | 请求失败 | 展示错误和重试入口 |
| `ExpandTablePanel` | 子表数据 | 渲染内嵌表格，默认无工具栏 |
| `ExpandDescriptionsPanel` | 描述字段 | 按配置列数展示字段 |
| `ExpandTabsPanel` | 多面板 | Tab 切换不重复请求已缓存面板 |

### P2 — 入口层/设计器
| 场景 | 预期 |
|------|------|
| 设计器新增展开面板 | 选择子表后自动生成 table panel |
| 设计器配置描述面板 | 选择字段后生成 descriptions panel |
| 运行态预览 | `expandConfig` 被完整透传 |
| 未配置展开 | 老页面表格行为不变 |

### 不测试（明确列出原因）
- 后端数据权限逻辑：展开数据源复用已有接口，后端权限由已有机制保障。
- 子表编辑：第一期展开面板建议只读，编辑仍由详情/编辑弹窗里的 `ChildTableEditor` 负责。

## 3. 执行计划
- [ ] Step 1: 读取 `automated-testing-standard.md` 和当前变更文档。
- [ ] Step 2: 运行 `pnpm --dir forge-admin-ui build` 确认前端构建。
- [ ] Step 3: 若项目有前端单测框架，补 `expand-utils` 单测并确认 Red/Green。
- [ ] Step 4: 启动前端页面，使用 E2E/手工验证表格展开、子表、描述、Tabs、列宽拖拽。
- [ ] Step 5: 追加 `execution-log.md`，记录命令、结果、跳过项和服务清理。

## 4. 历史验证基线
| 时间 | 范围 | 命令 | 结果 | 备注 |
|------|------|------|------|------|
| 2026-06-27 | 方案阶段 | 未执行 | 跳过 | 本轮只记录方案，不改运行代码。 |

## 5. 本轮增量验证
| 时间 | 变更范围 | 必跑项 | 实际命令 | 结果 | 跳过/警告 |
|------|----------|--------|----------|------|-----------|
| 2026-06-27 | 文档方案 | 文档落盘检查 | `sed -n` 检查新增文档；`git status --short` 检查变更状态 | 通过 | 未进入实现阶段，未运行构建或测试。 |
| 2026-06-27 | 运行态展开实现 | 目标文件 ESLint | `pnpm --dir forge-admin-ui exec eslint ...` | 通过 | `AiTable.vue` 保留既有 `vue/no-required-prop-with-default` warning。 |
| 2026-06-27 | 前端整体构建 | Vite build | `pnpm --dir forge-admin-ui build` | 通过 | 有既有 CSS `//` 注释 warning、store 动静态导入 warning；新增 nested tabs 动态导入提示，不阻断构建。 |
| 2026-06-27 | 列表/表单设计器配置入口 | 目标文件 ESLint | `pnpm --dir forge-admin-ui exec eslint ...ListPageGridDesigner.vue ...ForgePropertyPanel.vue ...` | 通过 | `AiTable.vue` 既有 warning；`ListPageGridDesigner.vue` 保留既有单行按钮换行 warning。 |
| 2026-06-27 | 设计器入口整体验证 | Vite build | `pnpm --dir forge-admin-ui build` | 通过 | CSS `//` 注释、store 动静态导入、ExpandPanelRenderer chunk 提示均不阻断构建。 |

## 6. 执行证据
- `execution-log.md`：`code-copilot/changes/ai-crud-table-expand-enhancement/execution-log.md`
- 关键接口：复用各业务子表/详情接口，按实现阶段补充。
- 关键数据库检查：无数据库变更。
- 服务启动与停止：实现阶段按实际记录。
