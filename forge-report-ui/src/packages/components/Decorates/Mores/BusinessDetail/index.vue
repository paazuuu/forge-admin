<template>
  <section class="business-detail" :style="rootStyle">
    <header class="detail-header">
      <span></span>
      <div>
        <h3>{{ option.title }}</h3>
        <p v-if="option.subtitle">{{ option.subtitle }}</p>
      </div>
    </header>
    <div class="detail-grid" :style="{ gridTemplateColumns: `repeat(${option.columns || 2}, minmax(0, 1fr))` }">
      <div
        v-for="field in option.fields"
        :key="field.key"
        class="detail-item"
        :style="{ gridColumn: `span ${Math.min(field.span || 1, option.columns || 2)}` }"
      >
        <div class="label">{{ field.label }}</div>
        <img v-if="field.type === 'image'" class="value-image" :src="getValue(field.key)" alt="" />
        <div v-else-if="field.type === 'tag'" class="value-tag" :style="{ color: field.color, borderColor: field.color }">
          {{ formatValue(field) }}
        </div>
        <div v-else class="value">{{ formatValue(field) }}</div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, inject, PropType, ref, unref } from 'vue'
import { CreateComponentType } from '@/packages/index.d'
import { PREVIEW_PAGE_CONTEXT_KEY } from '@/utils/requestDynamicParams'
import type { DetailField, option as defaultOption } from './config'

const props = defineProps({
  chartConfig: {
    type: Object as PropType<CreateComponentType & { option: typeof defaultOption }>,
    required: true
  }
})

const pageContext = inject(PREVIEW_PAGE_CONTEXT_KEY, ref({}))
const option = computed(() => props.chartConfig.option)
const data = computed(() => ({ ...(option.value.data || {}), ...(unref(pageContext) || {}) }))
const rootStyle = computed(() => ({
  '--detail-accent': option.value.style.accentColor,
  '--detail-text': option.value.style.textColor,
  '--detail-muted': option.value.style.mutedColor,
  '--detail-panel': option.value.style.panelColor,
  '--detail-border': option.value.style.borderColor,
  '--detail-radius': `${option.value.style.radius}px`
}))

const getByPath = (target: any, path: string) => path.split('.').reduce((current, key) => current?.[key], target)
const getValue = (key: string) => getByPath(data.value, key)
const formatValue = (field: DetailField) => {
  const value = getValue(field.key)
  if (field.type === 'money') return Number(value || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  if (field.type === 'date' && value) return String(value).slice(0, 10)
  return value ?? '-'
}
</script>

<style scoped lang="scss">
.business-detail {
  width: 100%;
  height: 100%;
  padding: 16px;
  overflow: hidden;
  color: var(--detail-text);
  border: 1px solid var(--detail-border);
  border-radius: var(--detail-radius);
  background: var(--detail-panel);
}

.detail-header {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 14px;

  span {
    width: 4px;
    height: 30px;
    border-radius: 4px;
    background: var(--detail-accent);
    box-shadow: 0 0 16px var(--detail-accent);
  }

  h3 {
    margin: 0;
    font-size: 18px;
    line-height: 1.2;
  }

  p {
    margin: 4px 0 0;
    color: var(--detail-muted);
    font-size: 12px;
  }
}

.detail-grid {
  display: grid;
  gap: 10px;
}

.detail-item {
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.035);
}

.label {
  margin-bottom: 6px;
  color: var(--detail-muted);
  font-size: 12px;
}

.value {
  overflow: hidden;
  font-size: 15px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.value-tag {
  display: inline-flex;
  height: 24px;
  align-items: center;
  padding: 0 10px;
  border: 1px solid;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
}

.value-image {
  width: 100%;
  max-height: 96px;
  object-fit: cover;
  border-radius: 6px;
}
</style>
