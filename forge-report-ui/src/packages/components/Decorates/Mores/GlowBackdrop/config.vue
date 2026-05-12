<template>
  <CollapseItem name="发光背景" :expanded="true">
    <SettingItemBox name="样式">
      <SettingItem name="类型">
        <n-select v-model:value="optionData.variant" size="small" :options="variantOptions" />
      </SettingItem>
      <SettingItem name="透明度">
        <n-input-number v-model:value="optionData.opacity" size="small" :min="0" :max="1" :step="0.1" />
      </SettingItem>
      <SettingItem name="旋转">
        <n-input-number v-model:value="optionData.rotate" size="small" :min="0" :max="360" />
      </SettingItem>
    </SettingItemBox>
    <SettingItemBox name="颜色">
      <SettingItem name="主色">
        <n-color-picker v-model:value="optionData.accentColor" size="small" :modes="['hex']" />
      </SettingItem>
      <SettingItem name="辅色">
        <n-color-picker v-model:value="optionData.secondColor" size="small" :modes="['hex']" />
      </SettingItem>
    </SettingItemBox>
  </CollapseItem>
</template>

<script setup lang="ts">
import { PropType } from 'vue'
import { CollapseItem, SettingItemBox, SettingItem } from '@/components/Pages/ChartItemSetting'
import { option } from './config'

defineProps({
  optionData: {
    type: Object as PropType<typeof option>,
    required: true
  }
})

const variantOptions = [
  { label: '反应堆光环', value: 'reactor' },
  { label: '透视网格', value: 'grid' },
  { label: '能量翼', value: 'wing' },
  { label: '星门矩阵', value: 'stargate' },
  { label: '雷达裂变', value: 'radar' }
]
</script>
