<template>
  <div class="model-designer">
    <div class="model-main">
      <div class="model-toolbar">
        <n-form label-placement="top" size="small" class="model-form" :show-feedback="false">
          <div class="model-form-grid">
            <n-form-item label="应用类型">
              <n-select
                v-model:value="localModel.appType"
                :options="appTypeOptions"
                @update:value="handleAppTypeChange"
              />
            </n-form-item>
            <n-form-item label="建表方式">
              <n-select v-model:value="localModel.tableMode" :options="tableModeOptions" />
            </n-form-item>
            <n-form-item label="业务名称">
              <n-input v-model:value="localModel.businessName" placeholder="例如：合同管理" />
            </n-form-item>
            <n-form-item label="数据表">
              <n-input
                :value="localModel.tableName"
                placeholder="biz_contract"
                @update:value="localModel.tableName = normalizeTableName($event)"
              />
            </n-form-item>
            <div class="model-actions">
              <n-space justify="end">
                <n-button @click="addField">
                  <template #icon>
                    <n-icon><AddOutline /></n-icon>
                  </template>
                  新增字段
                </n-button>
                <n-button :loading="validating" @click="validateModel">
                  校验模型
                </n-button>
              </n-space>
            </div>
          </div>
        </n-form>
      </div>
      <ModelFieldTable
        :fields="localModel.fields"
        :selected-index="selectedIndex"
        @update:fields="handleFieldsUpdate"
        @select="selectedIndex = $event"
        @copy="copyField"
        @remove="removeField"
      />
    </div>
    <ModelFieldPropertyPanel
      :field="currentField"
      :fields="localModel.fields"
      class="model-side"
      @update:field="handleFieldUpdate"
    />
  </div>
</template>

<script setup>
import { AddOutline } from '@vicons/ionicons5'
import { computed, ref, watch } from 'vue'
import { lowcodeValidateModel } from '@/api/lowcode-crud'
import {
  appTypeOptions,
  cloneSchema,
  createDefaultField,
  createFieldFromIndex,
  isSameSchema,
  normalizeTableName,
  tableModeOptions,
} from './model-schema'
import ModelFieldPropertyPanel from './ModelFieldPropertyPanel.vue'
import ModelFieldTable from './ModelFieldTable.vue'

const props = defineProps({
  modelValue: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['update:modelValue', 'validated'])

const localModel = ref(cloneSchema(props.modelValue))
const selectedIndex = ref(0)
const validating = ref(false)

const currentField = computed(() => localModel.value.fields?.[selectedIndex.value] || null)

watch(
  () => props.modelValue,
  (value) => {
    if (isSameSchema(value, localModel.value))
      return
    localModel.value = cloneSchema(value)
    if (selectedIndex.value >= localModel.value.fields.length)
      selectedIndex.value = Math.max(localModel.value.fields.length - 1, 0)
  },
  { deep: true },
)

watch(
  localModel,
  (value) => {
    if (!isSameSchema(value, props.modelValue)) {
      emit('update:modelValue', cloneSchema(value))
    }
  },
  { deep: true },
)

function handleFieldsUpdate(fields) {
  localModel.value.fields = fields
}

function handleFieldUpdate(field) {
  if (selectedIndex.value < 0)
    return
  localModel.value.fields.splice(selectedIndex.value, 1, field)
}

function addField() {
  const next = createFieldFromIndex((localModel.value.fields?.length || 0) + 1)
  localModel.value.fields.push(next)
  selectedIndex.value = localModel.value.fields.length - 1
}

function handleAppTypeChange(value) {
  if (value !== 'TREE')
    return
  ensureTreeModel()
}

function ensureTreeModel() {
  const fields = localModel.value.fields || []
  if (!localModel.value.treeConfig) {
    localModel.value.treeConfig = {}
  }

  const parentField = localModel.value.treeConfig.parentField || 'parentId'
  if (!fields.some(field => field.field === parentField)) {
    fields.push({
      ...createDefaultField(parentField, '父级ID'),
      dataType: 'bigint',
      componentType: 'number',
      queryType: 'eq',
      searchable: false,
      listVisible: false,
      formVisible: false,
      width: 120,
    })
    localModel.value.fields = fields
  }

  const labelField = fields.find(field => field.field === 'name')?.field
    || fields.find(field => field.field !== parentField)?.field
    || 'name'
  localModel.value.treeConfig = {
    keyField: localModel.value.treeConfig.keyField || 'id',
    parentField,
    labelField: localModel.value.treeConfig.labelField || labelField,
    childrenField: localModel.value.treeConfig.childrenField || 'children',
    treeTitle: localModel.value.treeConfig.treeTitle || `${localModel.value.businessName || '业务'}树`,
  }
}

function copyField(index) {
  const source = localModel.value.fields[index]
  const copy = cloneSchema(source)
  copy.field = `${source.field}Copy`
  copy.columnName = `${source.columnName}_copy`
  copy.label = `${source.label}副本`
  localModel.value.fields.splice(index + 1, 0, copy)
  selectedIndex.value = index + 1
}

function removeField(index) {
  localModel.value.fields.splice(index, 1)
  selectedIndex.value = Math.max(Math.min(selectedIndex.value, localModel.value.fields.length - 1), 0)
}

async function validateModel() {
  validating.value = true
  try {
    await lowcodeValidateModel(localModel.value)
    window.$message?.success('模型校验通过')
    emit('validated')
  }
  catch (e) {
    window.$message?.error(e?.message || '模型校验失败')
  }
  finally {
    validating.value = false
  }
}
</script>

<style scoped>
.model-designer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 16px;
  min-height: 560px;
}

.model-main {
  min-width: 0;
}

.model-toolbar {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 12px;
}

.model-form {
  width: 100%;
}

.model-form-grid {
  display: grid;
  grid-template-columns: 150px 150px minmax(180px, 1fr) minmax(180px, 1fr) auto;
  gap: 12px;
  align-items: end;
}

.model-actions {
  min-width: 190px;
  padding-bottom: 1px;
}

.model-side {
  min-height: 560px;
}

@media (max-width: 1180px) {
  .model-designer {
    grid-template-columns: 1fr;
  }

  .model-form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .model-actions {
    grid-column: 1 / -1;
  }
}
</style>
