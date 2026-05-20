<template>
  <n-select
    :value="value"
    :options="dataTypeOptions"
    size="small"
    @update:value="$emit('update:value', $event)"
  />
</template>

<script setup>
import { dataTypeOptions } from '../model/model-schema'

defineProps({
  value: {
    type: String,
    default: 'varchar',
  },
})

defineEmits(['update:value'])
</script>
