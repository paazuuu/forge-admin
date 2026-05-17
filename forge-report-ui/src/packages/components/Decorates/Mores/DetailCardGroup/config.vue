<template>
  <CollapseItem name="详情卡片组" :expanded="true">
    <SettingItemBox name="布局">
      <SettingItem name="列数">
        <n-input-number v-model:value="optionData.columns" size="small" :min="1" :max="6" />
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
</script>
