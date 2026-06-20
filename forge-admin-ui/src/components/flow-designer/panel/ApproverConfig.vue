<script setup>
/**
 * ApproverConfig — 审批人节点完整配置
 *
 * 内部用 NTabs 切换：
 *   - basic        审批设置（assignee / candidates / form 引用）
 *   - multi        会签设置（multiInstance + completionCondition）
 *   - permissions  操作权限（7 个布尔开关）
 *   - form         表单字段权限（formFieldPermissions 列表）
 *   - listeners    任务监听器 / 执行监听器
 *
 * 字段 1:1 迁移自 NodePropertiesPanel.vue:1562-2200，但用更扁平的 emit('update:config') 通信。
 *
 * Props:
 *   - node      flowJson 节点
 *   - readonly
 *
 * Events:
 *   - update:config   增量 patch，外层 NodeConfigDrawer 合并到 draftNode.config
 */
import { computed, ref } from 'vue'
import ApproverAssigneeForm from './ApproverAssigneeForm.vue'
import BasicConfig from './BasicConfig.vue'
import FormPermissionConfig from './FormPermissionConfig.vue'
import ListenerConfig from './ListenerConfig.vue'
import MultiInstanceConfig from './MultiInstanceConfig.vue'
import PermissionConfig from './PermissionConfig.vue'

const props = defineProps({
  node: { type: Object, required: true },
  readonly: Boolean,
})

const emit = defineEmits(['update:config', 'update:node'])

const tab = ref('basic')

const config = computed(() => props.node?.config || {})

function patch(part) {
  emit('update:config', part)
}

function updateNode(node) {
  emit('update:node', node)
}
</script>

<template>
  <div class="approver-config">
    <n-tabs v-model:value="tab" type="line" size="large" animated>
      <n-tab-pane name="basic" tab="审批人设置">
        <BasicConfig
          :node="node"
          :readonly="readonly"
          @update:node="updateNode"
        />
        <ApproverAssigneeForm
          :config="config"
          :readonly="readonly"
          @update:config="patch"
        />
        <div class="config-section-block">
          <div class="config-section-title">
            多人审批方式
          </div>
          <MultiInstanceConfig
            :config="config"
            :readonly="readonly"
            @update:config="patch"
          />
        </div>
      </n-tab-pane>
      <n-tab-pane name="form" tab="表单权限">
        <FormPermissionConfig
          :config="config"
          :readonly="readonly"
          @update:config="patch"
        />
      </n-tab-pane>
      <n-tab-pane name="actions" tab="审批后操作">
        <div class="config-section-block">
          <div class="config-section-title">
            操作权限
          </div>
          <PermissionConfig
            :config="config"
            :readonly="readonly"
            @update:config="patch"
          />
        </div>
        <div class="config-section-block">
          <div class="config-section-title">
            监听器
          </div>
          <ListenerConfig
            :config="config"
            :readonly="readonly"
            @update:config="patch"
          />
        </div>
      </n-tab-pane>
    </n-tabs>
  </div>
</template>
