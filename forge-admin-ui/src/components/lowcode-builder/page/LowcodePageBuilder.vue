<template>
  <div class="page-builder-shell">
    <div class="builder-toolbar">
      <n-tabs v-model:value="builderTab" type="segment" size="small" class="builder-tabs">
        <n-tab name="list">
          列表页面
        </n-tab>
        <n-tab name="edit">
          编辑表单
        </n-tab>
      </n-tabs>
      <n-radio-group v-model:value="localSchema.layoutType" size="small">
        <n-radio-button
          v-for="option in layoutOptions"
          :key="option.value"
          :value="option.value"
        >
          {{ option.label }}
        </n-radio-button>
      </n-radio-group>
    </div>
    <div v-if="builderTab === 'list'" class="structured-builder">
      <StructuredListPageDesigner
        v-model="localSchema"
        :fields="fields"
        :layout-type="localSchema.layoutType"
      />
    </div>
    <div v-else class="form-create-builder">
      <FormCreateDesignerAdapter
        :zone="editZone"
        :fields="fields"
        @update:zone="handleZoneUpdate"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { cloneSchema, isSameSchema } from '../model/model-schema'
import FormCreateDesignerAdapter from './FormCreateDesignerAdapter.vue'
import { syncPageSchemaWithModel } from './page-schema'
import StructuredListPageDesigner from './StructuredListPageDesigner.vue'

const props = defineProps({
  modelValue: {
    type: Object,
    required: true,
  },
  modelSchema: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['update:modelValue'])

const localSchema = ref(syncPageSchemaWithModel(cloneSchema(props.modelValue), props.modelSchema))
const builderTab = ref('list')
const layoutOptions = [
  { label: '标准单表', value: 'simple-crud' },
  { label: '左树右表', value: 'tree-crud' },
]

const fields = computed(() => props.modelSchema?.fields || [])
const editZone = computed(() => localSchema.value.zones?.find(zone => zone.zoneKey === 'edit') || null)

watch(
  () => props.modelValue,
  (value) => {
    const next = syncPageSchemaWithModel(cloneSchema(value), props.modelSchema)
    if (!isSameSchema(next, localSchema.value)) {
      localSchema.value = next
    }
  },
  { deep: true },
)

watch(
  () => props.modelSchema,
  (value) => {
    const next = syncPageSchemaWithModel(localSchema.value, value)
    if (!isSameSchema(next, localSchema.value)) {
      localSchema.value = next
    }
  },
  { deep: true },
)

watch(
  localSchema,
  (value) => {
    if (!isSameSchema(value, props.modelValue)) {
      emit('update:modelValue', cloneSchema(value))
    }
  },
  { deep: true },
)

function handleZoneUpdate(zone) {
  localSchema.value = {
    ...localSchema.value,
    zones: (localSchema.value.zones || []).map(item => item.zoneKey === zone.zoneKey ? zone : item),
  }
}
</script>

<style scoped>
.page-builder-shell {
  display: grid;
  gap: 12px;
}

.builder-toolbar {
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.builder-tabs {
  width: 240px;
}

.structured-builder {
  min-height: 704px;
}

.form-create-builder {
  min-height: 704px;
}
</style>
