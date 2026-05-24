# 低代码应用功能优化设计文档

**日期**: 2026-05-24  
**状态**: 已批准  
**优先级**: 高  
**影响范围**: 前端低代码搭建器、后端运行时配置生成器、前端 AiCrudPage 运行时组件  

---

## 1. 问题概述

本次优化涉及低代码应用的5个核心问题：

### 问题1：组织树字段在列表渲染时显示ID而非名称，编辑时无回显

**现象**: 真实运行页面（已发布应用）中，列表列显示组织ID而非组织名称，编辑表单中组织树字段没有回显已选值。

**影响**: 用户体验差，无法直观识别组织信息，编辑时无法确认已选组织。

### 问题2：主模型关联字段在渲染时显示ID而非关联模型名称

**现象**: 主模型有外键字段（如categoryId），关联到关联模型的ID字段，但在渲染时显示的是ID值而非关联模型的名称字段值。

**影响**: 用户无法直观理解关联关系，需要记忆ID对应的名称。

### 问题3：表单详情设计时组件需要标记所属模型

**现象**: 在设计器中，无法区分主模型字段和关联模型字段，容易混淆字段来源。

**影响**: 设计效率低，容易误用关联模型字段。

### 问题4：画布大小后面数字看不见

**现象**: CanvasFormDesigner 的画布宽度和高度输入框中数字显示不完整，被截断。

**影响**: 无法准确调整画布尺寸。

### 问题5：左树右边需要支持点击父级查询父级及其子集数据

**现象**: 左侧导航树和查询表单中的树形字段选择父节点时，只能查询该节点的直接子集，无法查询父节点及其所有子孙节点。

**影响**: 查询功能不完整，用户无法通过选择父节点查看完整层级数据。

---

## 2. 设计方案

### 方案选择

采用 **整体增强方案**：在现有架构上进行渐进式改进，风险最小，易于实现，支持分阶段交付。

**优先级排序**:
- 第一组（高优先级）：问题1-2（核心显示与回显问题）
- 第二组（中优先级）：问题3-5（设计器体验优化）

---

## 3. 第一组问题详细设计（问题1-2）

### 3.1 问题1：组织树字段显示与回显

#### 3.1.1 后端解决方案

**文件**: `LowcodeRuntimeConfigBuilder.java`

**扩展 transConfig 配置**:

当前系统已有 `transConfig` 用于字段值转换，需要扩展支持组织树字段的显示转换。

```java
private Map<String, Object> buildTransConfig(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
    Map<String, Object> transConfig = new LinkedHashMap<>();
    
    for (LowcodeFieldSchema field : modelSchema.getFields()) {
        if (!isOrgTreeField(field)) {
            continue;
        }
        
        Map<String, Object> fieldTrans = new LinkedHashMap<>();
        fieldTrans.put("type", "orgTree");
        fieldTrans.put("sourceField", field.getField());
        fieldTrans.put("displayField", "orgName");
        fieldTrans.put("valueField", "orgId");
        fieldTrans.put("queryApi", "/system/org/tree");
        
        transConfig.put(field.getField(), fieldTrans);
    }
    
    return transConfig;
}

private boolean isOrgTreeField(LowcodeFieldSchema field) {
    String componentType = field.getComponentType();
    return "orgTreeSelect".equals(componentType) 
        || "treeSelect".equals(componentType) && hasOrgTreeHint(field);
}
```

**新增 joinConfig 配置**:

```java
private List<Map<String, Object>> buildJoinConfig(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
    // ...现有逻辑
    
    // 新增：为组织树字段配置关联查询
    for (LowcodeFieldSchema field : modelSchema.getFields()) {
        if (!isOrgTreeField(field) || StringUtils.isBlank(field.getDictType())) {
            continue;
        }
        
        Map<String, Object> orgJoin = new LinkedHashMap<>();
        orgJoin.put("field", field.getField());
        orgJoin.put("joinTable", "sys_org");
        orgJoin.put("joinField", "org_id");
        orgJoin.put("displayField", "org_name");
        orgJoin.put("joinType", "LEFT");
        
        result.add(orgJoin);
    }
    
    return result;
}
```

#### 3.1.2 前端解决方案

**文件**: `AiCrudPage.vue`

**列渲染逻辑增强**:

```vue
const tableColumns = computed(() => {
  const transConfig = schema.transConfig || {};
  
  return (schema.columnsSchema || []).map((col) => {
    if (!col.key || col.key === 'actions') {
      return col;
    }
    
    const fieldTrans = transConfig[col.key];
    if (fieldTrans && fieldTrans.type === 'orgTree') {
      return {
        ...col,
        render: (row) => {
          const rawValue = row[col.key];
          if (!rawValue) return '-';
          
          // 从缓存中获取组织名称
          const orgName = orgNameCache.value[rawValue];
          return orgName || rawValue;
        }
      };
    }
    
    return col;
  });
});

// 组织名称缓存
const orgNameCache = ref({});
const orgTreeData = ref([]);

// 加载组织树数据并构建名称映射
async function loadOrgTreeData() {
  try {
    const res = await request.get('/system/org/tree');
    orgTreeData.value = res.data || [];
    
    // 构建orgId -> orgName的映射
    const nameMap = {};
    flattenTree(orgTreeData.value).forEach(org => {
      nameMap[org.orgId] = org.orgName;
    });
    orgNameCache.value = nameMap;
  } catch (error) {
    console.error('加载组织树数据失败', error);
  }
}

function flattenTree(tree) {
  const result = [];
  function traverse(nodes) {
    nodes.forEach(node => {
      result.push(node);
      if (node.children) {
        traverse(node.children);
      }
    });
  }
  traverse(tree);
  return result;
}
```

**文件**: `AiFormItem.vue`

**组织树组件回显支持**:

```vue
<NTreeSelect
  v-else-if="field.type === 'orgTreeSelect'"
  :value="resolveOrgTreeValue(field, formData[field.field])"
  :options="orgTreeOptions"
  :key-field="field.valueField || 'orgId'"
  :label-field="field.displayField || 'orgName'"
  :children-field="field.childrenField || 'children'"
  clearable
  @update:value="handleOrgTreeChange(field, $event)"
/>
```

```javascript
// 处理组织树值回显
function resolveOrgTreeValue(field, rawValue) {
  if (!rawValue) return null;
  
  // 如果是字符串ID，需要转换为数值ID
  if (typeof rawValue === 'string') {
    return Number(rawValue);
  }
  
  return rawValue;
}

// 处理组织树选择变更
function handleOrgTreeChange(field, selectedValue) {
  formData[field.field] = selectedValue;
  
  // 同时保存组织名称用于显示（如果需要）
  const selectedOrg = findOrgById(selectedValue);
  if (selectedOrg) {
    formData[`${field.field}_name`] = selectedOrg.orgName;
  }
}

// 从组织树中查找节点
function findOrgById(orgId) {
  return flattenTree(orgTreeOptions.value).find(org => org.orgId === orgId);
}
```

---

### 3.2 问题2：关联字段值转换

#### 3.2.1 后端解决方案

**文件**: `LowcodeRuntimeConfigBuilder.java`

**扩展 joinConfig 配置**:

```java
private List<Map<String, Object>> buildJoinConfig(LowcodeModelSchema modelSchema, LowcodePageSchema pageSchema) {
    // ...现有逻辑
    
    // 新增：为关联字段配置显示转换
    for (LowcodeFieldSchema field : modelSchema.getFields()) {
        if (!isRelationDisplayField(field, pageSchema)) {
            continue;
        }
        
        LowcodeRelationSchema relation = findRelationForField(field, modelSchema);
        if (relation == null) {
            continue;
        }
        
        Map<String, Object> joinConfig = new LinkedHashMap<>();
        joinConfig.put("field", field.getField());
        joinConfig.put("joinTable", relation.getTargetTableName());
        joinConfig.put("joinField", relation.getTargetField());
        joinConfig.put("displayField", resolveDisplayField(field, relation));
        joinConfig.put("joinType", "LEFT");
        joinConfig.put("relationType", relation.getRelationType());
        
        result.add(joinConfig);
    }
    
    return result;
}

private boolean isRelationDisplayField(LowcodeFieldSchema field, LowcodePageSchema pageSchema) {
    // 判断字段是否需要显示关联模型的名称
    return field.getRelationDisplay() != null 
        && Boolean.TRUE.equals(field.getListVisible());
}

private String resolveDisplayField(LowcodeFieldSchema field, LowcodeRelationSchema relation) {
    // 优先使用字段配置的displayField
    if (StringUtils.isNotBlank(field.getRelationDisplay())) {
        return field.getRelationDisplay();
    }
    
    // 默认使用"name"字段
    return "name";
}
```

**模型字段配置扩展**:

需要在 `LowcodeFieldSchema` 中增加属性：
- `relationDisplay`: String - 关联显示字段名（如"name"、"categoryName"）
- `relationTargetModel`: String - 关联目标模型编码

#### 3.2.2 前端解决方案

**文件**: `AiCrudPage.vue`

**列渲染逻辑增强**:

```vue
const tableColumns = computed(() => {
  const joinConfig = schema.joinConfig || [];
  
  return (schema.columnsSchema || []).map((col) => {
    if (!col.key || col.key === 'actions') {
      return col;
    }
    
    // 查找是否配置了关联显示
    const join = joinConfig.find(j => j.field === col.key);
    if (join) {
      return {
        ...col,
        render: (row) => {
          // 使用join配置的displayField值
          const displayValue = row[`${col.key}_${join.displayField}`];
          return displayValue || row[col.key] || '-';
        }
      };
    }
    
    return col;
  });
});
```

**批量查询关联数据**:

```javascript
// 查询列表数据后，批量查询关联模型数据
async function fetchListData() {
  // ...现有查询逻辑
  
  // 后处理：批量查询关联数据
  await fetchRelationData(listData.value);
}

async function fetchRelationData(rows) {
  const joinConfig = schema.joinConfig || [];
  if (!joinConfig.length) return;
  
  // 收集需要查询的关联ID
  const relationQueries = {};
  joinConfig.forEach(join => {
    const ids = rows
      .map(row => row[join.field])
      .filter(id => id && !relationDataCache.value[join.joinTable]?.[id]);
    
    if (ids.length) {
      relationQueries[join.joinTable] = {
        ids,
        displayField: join.displayField,
        joinField: join.joinField,
      };
    }
  });
  
  // 批量查询关联数据
  for (const [table, query] of Object.entries(relationQueries)) {
    try {
      const res = await request.get(`/ai/crud/relation/${table}`, {
        params: {
          ids: query.ids.join(','),
          displayField: query.displayField,
        }
      });
      
      // 更新缓存
      relationDataCache.value[table] = {
        ...relationDataCache.value[table],
        ...res.data,
      };
      
      // 更新行数据
      rows.forEach(row => {
        const id = row[query.field];
        const displayValue = res.data[id]?.[query.displayField];
        if (displayValue) {
          row[`${query.field}_${query.displayField}`] = displayValue;
        }
      });
    } catch (error) {
      console.error(`查询关联表${table}失败`, error);
    }
  }
}
```

---

## 4. 第二组问题详细设计（问题3-5）

### 4.1 问题3：组件标记所属模型

#### 4.1.1 左侧字段列表显示模型

**文件**: `ComponentPalette.vue`

**模板优化**:

```vue
<div class="field-item">
  <span class="field-name">{{ field.label || field.field }}</span>
  <span class="field-model-tag">{{ resolveModelTag(field) }}</span>
  <span class="field-meta">{{ field.field }} · {{ resolveFieldType(field) }}</span>
</div>
```

```javascript
function resolveModelTag(field) {
  if (!field.modelCode || field.modelCode === primaryModelCode.value) {
    return '主模型';
  }
  return field.modelName || field.modelCode;
}
```

**样式设计**:

```css
.field-item {
  display: grid;
  gap: 3px;
  min-height: 46px;
  padding: 7px 9px;
}

.field-name {
  color: #0f172a;
  font-size: 12px;
  font-weight: 700;
}

.field-model-tag {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 500;
  background: #f1f5f9;
  border-radius: 3px;
  padding: 1px 4px;
}

.field-meta {
  color: #94a3b8;
  font-size: 11px;
  overflow: hidden;
  text-overflow: ellipsis;
}
```

#### 4.1.2 画布组件标签显示模型

**文件**: `CanvasFormDesigner.vue`

**drawFormFieldPreview 方法优化**:

```javascript
function drawFormFieldPreview(api, group, item, width, height) {
  const field = fieldMap.value.get(item.fieldRef);
  
  // 显示格式："{模型名} · {字段名}"
  const displayLabel = item.modelName && item.modelName !== '主模型'
    ? `${item.modelName} · ${item.label || field?.label || '字段'}`
    : item.label || field?.label || '字段';
  
  // 根据模型类型设置颜色
  const strokeColor = item.modelName && item.modelName !== '主模型'
    ? '#059669'  // 绿色 - 关联模型
    : '#2563eb'; // 蓝色 - 主模型
  
  // ...绘制逻辑
}
```

### 4.2 问题4：画布数字显示修复

**文件**: `CanvasFormDesigner.vue`

**模板优化**:

```vue
<NInputNumber
  :value="canvas.width"
  :min="720"
  :max="2000"
  :step="40"
  size="small"
  class="size-input"
  placeholder="宽"
  @update:value="updateCanvasSize('width', $event)"
/>
<span class="size-divider">×</span>
<NInputNumber
  :value="canvas.height"
  :min="360"
  :max="1800"
  :step="40"
  size="small"
  class="size-input"
  placeholder="高"
  @update:value="updateCanvasSize('height', $event)"
/>
```

**样式调整**:

```css
.size-input {
  width: 80px;
  min-width: 80px;
}

.size-divider {
  margin: 0 4px;
  color: #94a3b8;
  font-size: 13px;
  font-weight: 600;
}
```

### 4.3 问题5：树节点父级查询

#### 4.3.1 左侧导航树父级查询

**文件**: `AiCrudPage.vue`

**树节点点击处理**:

```javascript
async function handleTreeNodeClick(node) {
  const treeConfig = schema.options?.treeConfig || {};
  const filterField = treeConfig.filterField || 'parentId';
  const targetField = treeConfig.targetField || 'id';
  
  // 判断是否是父节点（有子节点）
  const isParentNode = node.children && node.children.length > 0;
  
  // 构建查询参数
  const queryParams = {};
  
  if (isParentNode) {
    // 父节点：查询父节点及其所有子孙节点
    queryParams[filterField] = `${node[targetField]}ALL`;
  } else {
    // 叶子节点：精确查询
    queryParams[filterField] = node[targetField];
  }
  
  // 发起查询
  await fetchListData(queryParams);
}
```

#### 4.3.2 查询表单树形字段父级查询

**文件**: `AiFormItem.vue`

**树形选择组件优化**:

```vue
<NTreeSelect
  v-else-if="['treeSelect', 'orgTreeSelect', 'regionTreeSelect'].includes(field.type)"
  :value="formData[field.field]"
  :options="treeSelectOptions[field.field]"
  cascade
  checkable
  :cascade-check-strategy="resolveCascadeStrategy(field)"
  @update:value="handleTreeSelectChange(field, $event)"
/>
```

```javascript
function resolveCascadeStrategy(field) {
  // 如果配置了支持父级查询，使用all策略
  if (field.enableParentQuery) {
    return 'all';
  }
  return 'child';
}

function handleTreeSelectChange(field, selectedValues) {
  // 处理树形选择值
  const values = Array.isArray(selectedValues) ? selectedValues : [selectedValues];
  
  // 构建查询参数，支持ALL后缀
  const queryValue = values.map(v => {
    const node = findTreeNode(field, v);
    if (node && node.children && node.children.length > 0 && field.enableParentQuery) {
      return `${v}ALL`;
    }
    return v;
  }).join(',');
  
  formData[field.field] = queryValue;
}
```

#### 4.3.3 后端查询支持

**参考**: AGENTS.md 附录 A：行政区划查询规则

**MyBatis XML 查询逻辑**:

```xml
<if test="filterField != null and filterField != '' and filterField.contains('ALL')">
    AND (${filterFieldReplace} = REPLACE(#{filterField},'ALL','')
         OR ${filterFieldReplace} IN (SELECT ${targetField} FROM ${sourceTable} WHERE parent_field = REPLACE(#{filterField},'ALL','')))
</if>
<if test="filterField != null and filterField != '' and !filterField.contains('ALL')">
    AND ${filterFieldReplace} = #{filterField}
</if>
```

---

## 5. 实施计划

### 5.1 实施优先级

- **P0（核心功能）**: 问题1-2（组织树和关联字段显示转换）
- **P1（体验优化）**: 问题3-5（组件标记、画布样式、树查询）

### 5.2 实施阶段

**阶段一（P0功能）**:
1. 后端：扩展 `LowcodeRuntimeConfigBuilder.java` 的 `transConfig` 和 `joinConfig`
2. 后端：扩展 `LowcodeFieldSchema.java` 增加 `relationDisplay` 属性
3. 前端：`AiCrudPage.vue` 增强列渲染逻辑
4. 前端：`AiFormItem.vue` 增强组织树组件回显
5. 前端：增加组织树数据加载和名称映射逻辑

**阶段二（P1功能）**:
1. 前端：`ComponentPalette.vue` 增加模型标签显示
2. 前端：`CanvasFormDesigner.vue` 增强组件标签显示和颜色区分
3. 前端：修复画布输入框样式
4. 前端：`AiCrudPage.vue` 增强树节点查询支持
5. 后端：增加 ALL 后缀查询支持（如需）

### 5.3 验证方案

**问题1-2验证**:
- 创建包含组织树字段和关联字段的测试模型
- 发布应用后验证列表显示正确名称
- 打开编辑表单验证组织树字段正确回显

**问题3验证**:
- 在设计器中拖拽主模型和关联模型字段
- 验证左侧字段列表和画布组件标签正确显示模型信息

**问题4验证**:
- 在画布工具栏调整宽度和高度
- 验证数字完整显示

**问题5验证**:
- 发布树形应用，点击父节点验证查询包含子孙节点
- 在查询表单选择树形字段的父节点，验证查询包含子孙节点

---

## 6. 影响范围

### 6.1 后端文件

- `LowcodeRuntimeConfigBuilder.java` - 运行时配置生成器
- `LowcodeFieldSchema.java` - 字段配置（新增属性）
- MyBatis XML 查询文件 - 增加 ALL 后缀查询支持（如需）

### 6.2 前端文件

- `AiCrudPage.vue` - 核心运行时组件
- `AiFormItem.vue` - 表单组件
- `ComponentPalette.vue` - 字段列表组件
- `CanvasFormDesigner.vue` - 画布设计器

### 6.3 数据库影响

- 无数据库结构变更
- 现有 `transConfig` 和 `joinConfig` 配置扩展（JSON字段）

---

## 7. 风险评估

### 7.1 技术风险

**低风险**:
- 渐进式改进，不破坏现有架构
- 分阶段交付，易于验证和回滚
- 前端改动相对独立

**需注意**:
- 后端运行时配置变更影响所有低代码应用
- 需要兼容现有配置（向后兼容）

### 7.2 业务风险

- 不影响现有已发布应用（配置向后兼容）
- 新功能需要用户重新配置字段才能生效
- 可逐步推广，不强制要求

---

## 8. 后续优化方向

1. **性能优化**: 关联数据批量查询缓存策略优化
2. **配置增强**: 支持更多字段类型的值转换（如用户字段、区划字段）
3. **设计器增强**: 支持字段配置中的关联显示字段选择
4. **查询增强**: 支持更多查询条件组合和范围查询

---

**文档编写**: OpenCode AI  
**审核状态**: 待用户审核  
**下一步**: 用户审核后调用 writing-plans 技能创建实施计划