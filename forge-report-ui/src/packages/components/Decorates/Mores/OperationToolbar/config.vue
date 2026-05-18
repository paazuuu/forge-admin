<template>
  <CollapseItem name="操作工具栏" :expanded="true">
    <SettingItemBox name="内容">
      <SettingItem name="标题">
        <n-input v-model:value="optionData.title" size="small" />
      </SettingItem>
      <SettingItem name="对齐">
        <n-select v-model:value="optionData.align" size="small" :options="alignOptions" />
      </SettingItem>
      <SettingItem name="按钮">
        <div class="setting-line">
          <n-button size="tiny" secondary type="primary" @click="editorVisible = true">配置</n-button>
          <span>{{ optionData.actions?.length || 0 }} 个</span>
        </div>
      </SettingItem>
    </SettingItemBox>
    <SettingItemBox name="样式">
      <SettingItem name="主色">
        <n-color-picker v-model:value="optionData.style.accentColor" size="small" :modes="['hex']" />
      </SettingItem>
    </SettingItemBox>
  </CollapseItem>

  <n-modal v-model:show="editorVisible" preset="card" title="配置工具栏按钮" :bordered="false" class="toolbar-modal">
    <div class="modal-toolbar">
      <span>配置刷新、跳转、弹窗、请求等页面级操作。</span>
      <n-button size="small" type="primary" @click="addAction">新增按钮</n-button>
    </div>
    <div class="action-list">
      <div v-for="(action, index) in optionData.actions" :key="index" class="action-card">
        <div class="action-head">
          <strong>按钮 {{ index + 1 }}</strong>
          <n-button size="tiny" quaternary type="error" @click="optionData.actions.splice(index, 1)">删除</n-button>
        </div>
        <n-grid :cols="2" :x-gap="12" :y-gap="10">
          <n-grid-item>
            <div class="field-label">按钮文字</div>
            <n-input v-model:value="action.label" size="small" />
          </n-grid-item>
          <n-grid-item>
            <div class="field-label">类型</div>
            <n-select v-model:value="action.type" size="small" :options="actionTypeOptions" />
          </n-grid-item>
          <n-grid-item v-if="action.type === 'goPage' || action.type === 'openModal'">
            <div class="field-label">{{ action.type === 'openModal' ? '目标弹窗' : '目标页面' }}</div>
            <PageTargetSelect v-model:value="action.targetPageId" :page-type="action.type === 'openModal' ? 'modal' : 'page'" />
          </n-grid-item>
          <n-grid-item v-if="action.type === 'request' || action.type === 'link'">
            <div class="field-label">{{ action.type === 'request' ? '接口地址' : '链接地址' }}</div>
            <n-input v-model:value="action.url" size="small" />
          </n-grid-item>
          <n-grid-item v-if="action.type === 'request'">
            <div class="field-label">请求方式</div>
            <n-select v-model:value="action.method" size="small" :options="methodOptions" />
          </n-grid-item>
          <n-grid-item>
            <div class="field-label">样式</div>
            <n-select v-model:value="action.style" size="small" :options="styleOptions" />
          </n-grid-item>
          <n-grid-item>
            <div class="field-label">确认</div>
            <n-switch v-model:value="action.confirm" size="small" />
          </n-grid-item>
          <n-grid-item v-if="action.confirm">
            <div class="field-label">确认文案</div>
            <n-input v-model:value="action.confirmText" size="small" />
          </n-grid-item>
        </n-grid>
      </div>
    </div>
  </n-modal>
</template>

<script setup lang="ts">
import { PropType, ref } from 'vue'
import { CollapseItem, SettingItemBox, SettingItem } from '@/components/Pages/ChartItemSetting'
import PageTargetSelect from '@/packages/components/common/PageTargetSelect.vue'
import { ensureArray, ensureObject } from '@/packages/components/common/configCompat'
import { option } from './config'

const props = defineProps({
  optionData: {
    type: Object as PropType<typeof option>,
    required: true
  }
})

ensureArray(props.optionData, 'actions', option.actions)
ensureObject(props.optionData, 'style', option.style)

const alignOptions = [
  { label: '居左', value: 'left' },
  { label: '居中', value: 'center' },
  { label: '居右', value: 'right' }
]

const editorVisible = ref(false)
const actionTypeOptions = [
  { label: '刷新', value: 'refresh' },
  { label: '跳转页面', value: 'goPage' },
  { label: '打开弹窗', value: 'openModal' },
  { label: '关闭弹窗', value: 'closeModal' },
  { label: '调用接口', value: 'request' },
  { label: '打开链接', value: 'link' }
]
const methodOptions = [
  { label: 'GET', value: 'get' },
  { label: 'POST', value: 'post' },
  { label: 'PUT', value: 'put' },
  { label: 'DELETE', value: 'delete' }
]
const styleOptions = [
  { label: '主要', value: 'primary' },
  { label: '成功', value: 'success' },
  { label: '警告', value: 'warning' },
  { label: '危险', value: 'error' },
  { label: '信息', value: 'info' }
]

const addAction = () => {
  props.optionData.actions.push({
    label: '刷新',
    type: 'refresh',
    style: 'primary'
  })
}
</script>

<style scoped lang="scss">
.setting-line,
.modal-toolbar,
.action-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

:deep(.toolbar-modal) {
  width: min(860px, 92vw);
}

.modal-toolbar {
  margin-bottom: 12px;
  color: rgba(255, 255, 255, 0.62);
  font-size: 13px;
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.action-card {
  padding: 12px;
  border: 1px solid rgba(120, 172, 255, 0.18);
  border-radius: 8px;
  background: rgba(13, 20, 38, 0.72);
}

.action-head {
  margin-bottom: 10px;
}

.field-label {
  margin-bottom: 5px;
  color: rgba(255, 255, 255, 0.58);
  font-size: 12px;
}
</style>
