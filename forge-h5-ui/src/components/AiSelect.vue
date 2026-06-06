<template>
  <view class="ai-select" @click="showPicker = true">
    <view class="ai-select-input">
      <text class="ai-select-text" :class="{ 'ai-select-placeholder': !selectedLabel }">
        {{ selectedLabel || placeholder }}
      </text>
    </view>
    
    <uni-popup ref="popup" type="bottom">
      <view class="ai-picker">
        <view class="ai-picker-header">
          <text class="ai-picker-cancel" @click="showPicker = false">取消</text>
          <text class="ai-picker-title">请选择</text>
          <text class="ai-picker-confirm" @click="confirmSelection">确定</text>
        </view>
        <picker-view :value="[tempIndex]" @change="onPickerChange" class="ai-picker-view">
          <picker-view-column>
            <view v-for="(option, index) in options" :key="index" class="ai-picker-item">
              <text>{{option.label}}</text>
            </view>
          </picker-view-column>
        </picker-view>
      </view>
    </uni-popup>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: ''
  },
  options: {
    type: Array,
    default: () => []
  },
  placeholder: {
    type: String,
    default: '请选择'
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const showPicker = ref(false)
const tempIndex = ref(0)

const selectedLabel = computed(() => {
  const selected = props.options.find(o => o.value === props.modelValue)
  return selected ? selected.label : ''
})

const onPickerChange = (e) => {
  tempIndex.value = e.detail.value[0]
}

const confirmSelection = () => {
  const selected = props.options[tempIndex.value]
  if (selected) {
    emit('update:modelValue', selected.value)
    emit('change', selected.value)
  }
  showPicker.value = false
}

// Watch for model changes
import { watch } from 'vue'
watch(() => props.modelValue, (val) => {
  const index = props.options.findIndex(o => o.value === val)
  if (index !== -1) {
    tempIndex.value = index
  }
}, { immediate: true })
</script>

<style lang="scss" scoped>
.ai-select {
  padding: 10px;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.ai-select-input {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ai-select-text {
  font-size: 14px;
  color: #333;
  
  &.ai-select-placeholder {
    color: #999;
  }
}

.ai-picker {
  background: #fff;
  border-radius: 12px 12px 0 0;
}

.ai-picker-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #eee;
}

.ai-picker-cancel,
.ai-picker-confirm {
  font-size: 14px;
}

.ai-picker-confirm {
  color: var(--primary-color, #0891b2);
}

.ai-picker-title {
  font-size: 16px;
  font-weight: 500;
}

.ai-picker-view {
  height: 200px;
}

.ai-picker-item {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px;
  font-size: 14px;
}
</style>
