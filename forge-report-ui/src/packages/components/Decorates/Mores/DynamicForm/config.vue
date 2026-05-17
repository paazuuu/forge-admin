<template>
  <CollapseItem name="动态表单" :expanded="true">
    <SettingItemBox name="接口">
      <SettingItem name="标题">
        <n-input v-model:value="optionData.title" size="small" />
      </SettingItem>
      <SettingItem name="提交地址">
        <n-input v-model:value="optionData.submitUrl" size="small" placeholder="/forge-report-api/xxx" />
      </SettingItem>
      <SettingItem name="请求方式">
        <n-select v-model:value="optionData.submitMethod" size="small" :options="methodOptions" />
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

const methodOptions = [
  { label: 'POST', value: 'post' },
  { label: 'PUT', value: 'put' }
]
</script>
