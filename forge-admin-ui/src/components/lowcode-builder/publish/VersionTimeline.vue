<template>
  <div class="version-timeline">
    <div class="panel-title">
      发布版本
    </div>
    <n-spin :show="loading">
      <n-empty v-if="!versions.length" description="暂无发布版本" />
      <n-timeline v-else>
        <n-timeline-item
          v-for="item in versions"
          :key="item.id"
          :type="item.versionType === 'rollback' ? 'warning' : 'success'"
          :title="`版本 ${item.versionNo}`"
          :content="item.remark || item.versionType"
          :time="formatTime(item.createTime)"
        >
          <template #footer>
            <n-popconfirm @positive-click="$emit('rollback', item.id)">
              <template #trigger>
                <n-button text size="tiny" class="text-warning">
                  回滚到此版本
                </n-button>
              </template>
              确认回滚到版本 {{ item.versionNo }}？
            </n-popconfirm>
          </template>
        </n-timeline-item>
      </n-timeline>
    </n-spin>
  </div>
</template>

<script setup>
defineProps({
  versions: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

defineEmits(['rollback'])

function formatTime(value) {
  if (!value)
    return ''
  return new Date(value).toLocaleString()
}
</script>

<style scoped>
.version-timeline {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.panel-title {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 12px;
}
</style>
