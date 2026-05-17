<template>
  <section class="detail-card-group" :style="rootStyle">
    <article v-for="card in option.cards" :key="card.label">
      <span>{{ card.label }}</span>
      <strong>{{ card.value }}<em>{{ card.unit }}</em></strong>
      <p>{{ card.desc }}</p>
    </article>
  </section>
</template>

<script setup lang="ts">
import { computed, PropType } from 'vue'
import { CreateComponentType } from '@/packages/index.d'
import type { option as defaultOption } from './config'

const props = defineProps({
  chartConfig: {
    type: Object as PropType<CreateComponentType & { option: typeof defaultOption }>,
    required: true
  }
})

const option = computed(() => props.chartConfig.option)
const rootStyle = computed(() => ({
  gridTemplateColumns: `repeat(${option.value.columns || 3}, minmax(0, 1fr))`,
  '--card-accent': option.value.style.accentColor,
  '--card-text': option.value.style.textColor,
  '--card-muted': option.value.style.mutedColor,
  '--card-panel': option.value.style.panelColor,
  '--card-border': option.value.style.borderColor
}))
</script>

<style scoped lang="scss">
.detail-card-group {
  display: grid;
  gap: 10px;
  width: 100%;
  height: 100%;
}

article {
  min-width: 0;
  padding: 14px;
  color: var(--card-text);
  border: 1px solid var(--card-border);
  border-radius: 8px;
  background: var(--card-panel);

  span,
  p {
    color: var(--card-muted);
    font-size: 12px;
  }

  strong {
    display: block;
    margin: 8px 0;
    color: var(--card-accent);
    font-size: 30px;
    line-height: 1;
  }

  em {
    margin-left: 4px;
    color: var(--card-muted);
    font-size: 13px;
    font-style: normal;
  }

  p {
    margin: 0;
  }
}
</style>
