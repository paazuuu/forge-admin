<template>
  <AiCrudPage v-bind="mergedCrudProps" />
</template>

<script setup>
import { computed } from 'vue'
import AiCrudPage from '@/components/ai-form/AiCrudPage.vue'

const props = defineProps({
  crudProps: {
    type: Object,
    required: true,
  },
})

const mergedCrudProps = computed(() => ({
  ...props.crudProps,
  loadDetailOnEdit: true,
  modalWidth: props.crudProps.modalWidth || props.crudProps.options?.modalWidth || '1080px',
  childrenConfig: props.crudProps.childrenConfig || props.crudProps.options?.masterDetailConfig?.children || [],
}))
</script>
